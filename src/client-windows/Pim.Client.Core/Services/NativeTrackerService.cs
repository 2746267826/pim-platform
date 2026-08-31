using System.Collections.Concurrent;
using System.Net.Http;
using System.Threading.Channels;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class NativeTrackerService : IDisposable
{
    private readonly ApiClient _api;
    private readonly TrackerConfig _config;
    private readonly IWindowResolver _windowResolver;
    private readonly IIdleDetector _idleDetector;
    private readonly BrowserBridgeService _bridge;
    private readonly TrackerSessionManager _sessionManager;
    private readonly TrackerLogger _logger;
    private readonly CancellationTokenSource _cts = new();
    private readonly Channel<TrackerWindowInfo> _windowChannel = Channel.CreateUnbounded<TrackerWindowInfo>();
    private readonly ConcurrentQueue<TrackerEventForUpload> _uploadQueue = new();
    private readonly object _statsLock = new();
    private long _pollCount;
    private long _eventsUploaded;
    private long _uploadFailures;
    private string? _lastError;
    private bool _hookActive = true;
    private Task? _pollTask;
    private Task? _hookTask;
    private Task? _uploadTask;
    private Task? _healthTask;
    private Task? _browserTask;
    private DateTimeOffset _startedAt = DateTimeOffset.UtcNow;
    private DateTimeOffset _lastPollTime = DateTimeOffset.UtcNow;
    private TrackerWindowInfo? _lastWindow;
    private IntPtr _hookHandle = IntPtr.Zero;
    private Win32Hook.WinEventProc? _hookCallback;

    public Action<string>? Log { get; set; }

    public long PollCount { get { lock (_statsLock) return _pollCount; } }
    public long EventsUploaded { get { lock (_statsLock) return _eventsUploaded; } }
    public long UploadFailures { get { lock (_statsLock) return _uploadFailures; } }
    public string? LastError { get { lock (_statsLock) return _lastError; } }
    public bool HookActive { get { lock (_statsLock) return _hookActive; } }
    public long SessionsCreated => _sessionManager.SessionsCreated;
    public bool BrowserConnected => _bridge.IsConnected;
    public double? BrowserHeartbeatAgeSeconds
    {
        get
        {
            var hb = _bridge.LastHeartbeatTime;
            if (hb == DateTimeOffset.MinValue) return null;
            return (DateTimeOffset.UtcNow - hb).TotalSeconds;
        }
    }

    public NativeTrackerService(
        ApiClient api,
        TrackerConfig? config = null,
        IWindowResolver? windowResolver = null,
        IIdleDetector? idleDetector = null,
        BrowserBridgeService? bridge = null,
        TrackerLogger? logger = null)
    {
        _api = api;
        _config = config ?? new TrackerConfig();
        _windowResolver = windowResolver ?? new DefaultWindowResolver();
        _idleDetector = idleDetector ?? new WindowsIdleDetector();
        _logger = logger ?? new TrackerLogger(_config.LogRetentionDays);
        _bridge = bridge ?? new BrowserBridgeService(_config.BrowserBridgePort, _logger);
        _sessionManager = new TrackerSessionManager(_config, _logger);
        _sessionManager.SessionClosed += OnSessionClosed;
    }

    public void Start()
    {
        if (!_config.Enabled)
        {
            _logger.Info("Tracker", "Tracker disabled via config");
            return;
        }

        _startedAt = DateTimeOffset.UtcNow;
        _lastPollTime = _startedAt;
        _logger.Info("Tracker", $"Starting NativeTrackerService poll={_config.PollIntervalSeconds}s idle={_config.IdleThresholdSeconds}s gap={_config.GapThresholdSeconds}s port={_config.BrowserBridgePort}");

        try { _bridge.Start(); _logger.Info("Tracker", "BrowserBridge started"); } catch (Exception ex) { _logger.Error("Tracker", "BrowserBridge failed to start", ex); }

        _pollTask = Task.Run(() => PollLoopAsync(_cts.Token));
        _hookTask = Task.Run(() => HookLoopAsync(_cts.Token));
        _uploadTask = Task.Run(() => UploadLoopAsync(_cts.Token));
        _healthTask = Task.Run(() => HealthLoopAsync(_cts.Token));
        _browserTask = Task.Run(() => BrowserLoopAsync(_cts.Token));

        _logger.Info("Tracker", "NativeTrackerService started");
    }

    public void Stop()
    {
        _cts.Cancel();
        _bridge.Stop();
        var now = DateTimeOffset.UtcNow;
        _sessionManager.CloseCurrent(now);
        _logger.Info("Tracker", "NativeTrackerService stopped");
    }

    private void OnSessionClosed(TrackerSession session)
    {
        var evs = SessionToEvents(session, session.EndedAt ?? DateTimeOffset.UtcNow);
        foreach (var e in evs)
            _uploadQueue.Enqueue(e);
    }

    private List<TrackerEventForUpload> SessionToEvents(TrackerSession session, DateTimeOffset endedAt)
    {
        var duration = session.DurationSecs ?? (endedAt - session.StartedAt).TotalSeconds;
        if (duration <= 0) return new List<TrackerEventForUpload>();

        var eventType = session.IsIdle ? "idle" : "window";
        // Idle sessions are single event
        if (session.IsIdle)
        {
            return new List<TrackerEventForUpload>
            {
                new TrackerEventForUpload
                {
                    Timestamp = session.StartedAt.ToString("O"),
                    Duration = duration,
                    EventType = "idle",
                    ExePath = session.ExePath,
                    AppName = session.AppName,
                    DisplayName = session.AppName,
                    WindowTitle = session.WindowTitle,
                    IsIdle = true,
                    IsMediaActive = session.IsMediaActive,
                    Date = session.Date,
                    RawJson = new { sessionId = session.Id, isIdle = true }
                }
            };
        }

        // Normal window session: may have page visits with URLs
        // We create one window event covering whole session, plus page_visit info aggregated?
        // Spec says page_visit_count and page_visit_duration aggregate short visits
        var pageVisitCount = session.PageVisits.Count;
        var pageVisitDuration = session.PageVisits.Sum(v => v.DurationSecs ?? 0);

        var list = new List<TrackerEventForUpload>
        {
            new TrackerEventForUpload
            {
                Timestamp = session.StartedAt.ToString("O"),
                Duration = duration,
                EventType = "window",
                ExePath = session.ExePath,
                AppName = session.AppName,
                DisplayName = session.AppName,
                WindowTitle = session.WindowTitle,
                CommandLine = null,
                IsIdle = false,
                IsMediaActive = session.IsMediaActive,
                Date = session.Date,
                RawJson = new { sessionId = session.Id, pageVisits = session.PageVisits },
                PageVisitCount = pageVisitCount,
                PageVisitDuration = pageVisitDuration
            }
        };

        // If session has browser page visits with URLs, emit web-page events per distinct domain visit?
        // Simplified: each page visit with URL becomes a web-page event
        foreach (var pv in session.PageVisits.Where(p => !string.IsNullOrWhiteSpace(p.Url)))
        {
            var pvDuration = pv.DurationSecs ?? 0;
            if (pvDuration <= 0) continue;
            list.Add(new TrackerEventForUpload
            {
                Timestamp = pv.StartedAt.ToString("O"),
                Duration = pvDuration,
                EventType = "web-page",
                ExePath = session.ExePath,
                AppName = session.AppName,
                DisplayName = pv.Domain ?? session.AppName,
                WindowTitle = pv.WindowTitle,
                Url = pv.Url,
                Domain = pv.Domain,
                PagePath = null,
                Audible = _bridge.LastHeartbeat?.Audible,
                Incognito = _bridge.LastHeartbeat?.Incognito,
                TabCount = _bridge.LastHeartbeat?.TabCount,
                IsIdle = false,
                IsMediaActive = false,
                Date = pv.StartedAt.ToString("yyyy-MM-dd"),
                RawJson = new { sessionId = session.Id, pageVisit = pv }
            });
        }

        return list;
    }

    private async Task PollLoopAsync(CancellationToken ct)
    {
        var interval = TimeSpan.FromSeconds(_config.PollIntervalSeconds);
        using var timer = new PeriodicTimer(interval);
        // Initial check
        await DoPollAsync(ct).ConfigureAwait(false);

        while (await timer.WaitForNextTickAsync(ct).ConfigureAwait(false))
        {
            await DoPollAsync(ct).ConfigureAwait(false);
        }
    }

    private Task DoPollAsync(CancellationToken ct)
    {
        try
        {
            var now = DateTimeOffset.UtcNow;
            // Gap detection
            var elapsed = now - _lastPollTime;
            if (elapsed.TotalSeconds > _config.GapThresholdSeconds)
            {
                _logger.Info("Tracker", $"Gap detected: {elapsed.TotalSeconds:F1}s since last poll");
                _sessionManager.HandleGap(_lastPollTime, now);
                // Emit gap event
                _uploadQueue.Enqueue(new TrackerEventForUpload
                {
                    Timestamp = _lastPollTime.ToString("O"),
                    Duration = elapsed.TotalSeconds,
                    EventType = "gap",
                    IsIdle = false,
                    IsMediaActive = false,
                    Date = _lastPollTime.ToString("yyyy-MM-dd"),
                    RawJson = new { gapStart = _lastPollTime, gapEnd = now }
                });
            }
            _lastPollTime = now;
            lock (_statsLock) _pollCount++;

            // Idle detection
            var idleDuration = _idleDetector.GetIdleDuration();
            var isScreenOff = _idleDetector.IsScreenOff();
            if (isScreenOff)
            {
                if (!_sessionManager.IsIdle)
                    _sessionManager.HandleIdleStarted(now, idleDuration);
            }
            else if (idleDuration.TotalSeconds > _config.IdleThresholdSeconds)
            {
                // Check media active: if browser active + audible, extend threshold x3
                var effectiveThreshold = _config.IdleThresholdSeconds;
                if (_sessionManager.IsBrowserMediaActive)
                    effectiveThreshold *= 3;

                if (idleDuration.TotalSeconds > effectiveThreshold)
                {
                    if (!_sessionManager.IsIdle)
                        _sessionManager.HandleIdleStarted(now, idleDuration);
                }
            }
            else
            {
                if (_sessionManager.IsIdle)
                {
                    var window = _windowResolver.GetForegroundWindowInfo();
                    _sessionManager.HandleIdleEnded(now, window);
                }
            }

            // Window tracking (if not idle)
            if (!_sessionManager.IsIdle)
            {
                var window = _windowResolver.GetForegroundWindowInfo();
                if (window is not null)
                {
                    // Debounce: if same as last, skip but still check title?
                    bool shouldProcess = true;
                    if (_lastWindow is not null && _lastWindow.Hwnd == window.Hwnd && _lastWindow.AppName == window.AppName && _lastWindow.WindowTitle == window.WindowTitle)
                    {
                        // No change, avoid noisy duplicate processing polling vs hook
                        // But we still have Cooldown: don't skip entirely, but mark no change
                        // To avoid duplicate Hook event handling, we skip if recently processed via hook
                        // For simplicity allow poll to skip if window unchanged
                        shouldProcess = false;
                    }

                    if (shouldProcess)
                    {
                        _sessionManager.HandleWindowChange(window, now);
                        _lastWindow = window;
                        _logger.Debug("Tracker", $"Poll window: {window.AppName} title={window.WindowTitle}");
                    }
                }
            }

            // Hook health: if we haven't received hook event in a while, ensure poll covers
            // (hook loop sets _hookActive separately)
        }
        catch (Exception ex) when (!ct.IsCancellationRequested)
        {
            _logger.Error("Tracker", "Poll error", ex);
            lock (_statsLock) _lastError = ex.Message;
        }
        return Task.CompletedTask;
    }

    private async Task HookLoopAsync(CancellationToken ct)
    {
        // Hook using SetWinEventHook on Windows; fallback to no-op on other platforms or failure
        if (!OperatingSystem.IsWindows())
        {
            _logger.Warn("Tracker", "Hook not supported on non-Windows, using poll-only mode");
            lock (_statsLock) _hookActive = false;
            return;
        }

        try
        {
            _logger.Info("Tracker", "Registering Win32 hooks EVENT_SYSTEM_FOREGROUND and EVENT_OBJECT_NAMECHANGE");
            // P/Invoke setup simplified: use Win32 Hook in separate thread with message loop
            // For cross-platform testability, we simulate via polling fallback but mark hook active if succeeds
            // Real implementation would call SetWinEventHook; we abstract via try/catch

            // Hold delegate to prevent GC
            _hookCallback = OnWinEvent;
            // Attempt to register hook; if fails, fallback
            var hook1 = Win32Hook.TryRegister(0x0003, 0x0003, _hookCallback); // EVENT_SYSTEM_FOREGROUND
            var hook2 = Win32Hook.TryRegister(0x800C, 0x800C, _hookCallback); // EVENT_OBJECT_NAMECHANGE

            if (hook1 == IntPtr.Zero && hook2 == IntPtr.Zero)
            {
                _logger.Warn("Tracker", "Hook registration failed, fallback to poll");
                lock (_statsLock) _hookActive = false;
                return;
            }

            _hookHandle = hook1 != IntPtr.Zero ? hook1 : hook2;
            lock (_statsLock) _hookActive = true;
            _logger.Info("Tracker", "Hook registered successfully");

            // Message loop
            while (!ct.IsCancellationRequested)
            {
                Win32Hook.PumpMessages(100);
                await Task.Delay(100, ct).ConfigureAwait(false);
                // Hook health check: if hook lost (Win32Hook.IsLost), log warn and keep polling
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            _logger.Error("Tracker", "Hook loop error", ex);
            lock (_statsLock) { _hookActive = false; _lastError = ex.Message; }
        }
        finally
        {
            try { Win32Hook.Unhook(_hookHandle); } catch { }
        }
    }

    private DateTimeOffset _lastHookEventTime = DateTimeOffset.MinValue;
    private void OnWinEvent(IntPtr hook, uint eventType, IntPtr hwnd, int idObject, int idChild, uint thread, uint time)
    {
        try
        {
            if (hwnd == IntPtr.Zero) return;
            var now = DateTimeOffset.UtcNow;
            if ((now - _lastHookEventTime).TotalMilliseconds < 500) return;
            _lastHookEventTime = now;
            var window = _windowResolver.GetForegroundWindowInfo();
            if (window is null) return;
            _sessionManager.HandleWindowChange(window, now);
            _lastWindow = window;
            _logger.Debug("Tracker", $"Hook event {eventType:X} window {window.AppName} title {window.WindowTitle}");
        }
        catch (Exception ex)
        {
            _logger.Warn("Tracker", $"Hook callback error: {ex.Message}");
        }
    }

    private async Task UploadLoopAsync(CancellationToken ct)
    {
        var interval = TimeSpan.FromSeconds(_config.UploadIntervalSeconds);
        using var timer = new PeriodicTimer(interval);

        while (await timer.WaitForNextTickAsync(ct).ConfigureAwait(false))
        {
            try
            {
                if (_uploadQueue.IsEmpty) continue;
                var batch = new List<TrackerEventForUpload>();
                while (batch.Count < _config.UploadBatchSize && _uploadQueue.TryDequeue(out var ev))
                    batch.Add(ev);

                if (batch.Count == 0) continue;

                var req = new TrackerEventsUploadRequest
                {
                    DeviceId = Environment.MachineName,
                    Events = batch
                };

                var result = await _api.PostAsync<ApiResponse<int>>("/pc/tracker/upload", req, ct).ConfigureAwait(false);
                if (result is not null)
                {
                    lock (_statsLock) _eventsUploaded += batch.Count;
                    _logger.Info("Tracker", $"Uploaded {batch.Count} events -> {result.Data} saved");
                    Log?.Invoke($"[Tracker] Uploaded {batch.Count} events -> {result.Data} saved");
                    lock (_statsLock) _lastError = null;
                }
                else
                {
                    lock (_statsLock) { _uploadFailures++; _lastError = "Upload returned null response"; }
                    _logger.Warn("Tracker", "Upload returned null response");
                    // Re-queue for retry (simple: push back)
                    foreach (var ev in batch) _uploadQueue.Enqueue(ev);
                }
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { break; }
            catch (HttpRequestException ex)
            {
                lock (_statsLock) { _uploadFailures++; _lastError = ex.Message; }
                _logger.Error("Tracker", $"Upload Http error: {ex.Message}", ex);
                await Task.Delay(TimeSpan.FromSeconds(5), ct).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                lock (_statsLock) { _uploadFailures++; _lastError = ex.Message; }
                _logger.Error("Tracker", $"Upload error: {ex.Message}", ex);
            }
        }
    }

    private async Task HealthLoopAsync(CancellationToken ct)
    {
        var interval = TimeSpan.FromSeconds(_config.HealthReportIntervalSeconds);
        using var timer = new PeriodicTimer(interval);
        await Task.Delay(interval, ct).ConfigureAwait(false);
        while (await timer.WaitForNextTickAsync(ct).ConfigureAwait(false))
        {
            try
            {
                var req = new TrackerHealthRequest
                {
                    DeviceId = Environment.MachineName,
                    Status = _lastError is null ? "running" : "degraded",
                    UptimeSeconds = (DateTimeOffset.UtcNow - _startedAt).TotalSeconds,
                    HookActive = HookActive,
                    PollCount = PollCount,
                    SessionsCreated = SessionsCreated,
                    EventsUploaded = EventsUploaded,
                    UploadFailures = UploadFailures,
                    LastError = LastError,
                    BrowserConnected = BrowserConnected,
                    BrowserHeartbeatAgeSeconds = BrowserHeartbeatAgeSeconds
                };
                await _api.PostAsync<ApiResponse<string>>("/pc/tracker/health", req, ct).ConfigureAwait(false);
                _logger.Debug("Tracker", $"Health reported: hook={req.HookActive} polls={req.PollCount} sessions={req.SessionsCreated} uploaded={req.EventsUploaded}");
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested) { break; }
            catch (Exception ex)
            {
                _logger.Warn("Tracker", $"Health report failed: {ex.Message}");
            }
        }
    }

    private async Task BrowserLoopAsync(CancellationToken ct)
    {
        try
        {
            await foreach (var hb in _bridge.Reader.ReadAllAsync(ct).ConfigureAwait(false))
            {
                _sessionManager.UpdateBrowserHeartbeat(hb);
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            _logger.Warn("Tracker", $"Browser loop error: {ex.Message}");
        }
    }

    public void Dispose()
    {
        try { _cts.Cancel(); } catch { }
        _cts.Dispose();
        _bridge.Dispose();
        _logger.Dispose();
    }

    // Minimal Win32 hook abstraction for compilation on non-Windows
    private static class Win32Hook
    {
        public static IntPtr TryRegister(uint eventMin, uint eventMax, WinEventProc proc)
        {
            if (!OperatingSystem.IsWindows()) return IntPtr.Zero;
            try { return SetWinEventHook(eventMin, eventMax, IntPtr.Zero, proc, 0, 0, 0); } catch { return IntPtr.Zero; }
        }
        public static void Unhook(IntPtr h) { if (h != IntPtr.Zero) try { UnhookWinEvent(h); } catch { } }
        public static void PumpMessages(int timeoutMs)
        {
            try
            {
                MSG msg;
                while (PeekMessage(out msg, IntPtr.Zero, 0, 0, 1))
                {
                    TranslateMessage(ref msg);
                    DispatchMessage(ref msg);
                }
            }
            catch { }
        }
        public delegate void WinEventProc(IntPtr hWinEventHook, uint eventType, IntPtr hwnd, int idObject, int idChild, uint dwEventThread, uint dwmsEventTime);

        [System.Runtime.InteropServices.DllImport("user32.dll")] private static extern IntPtr SetWinEventHook(uint eventMin, uint eventMax, IntPtr hmodWinEventProc, WinEventProc lpfnWinEventProc, uint idProcess, uint idThread, uint dwFlags);
        [System.Runtime.InteropServices.DllImport("user32.dll")] private static extern bool UnhookWinEvent(IntPtr hWinEventHook);
        [System.Runtime.InteropServices.DllImport("user32.dll")] private static extern bool PeekMessage(out MSG lpMsg, IntPtr hWnd, uint wMsgFilterMin, uint wMsgFilterMax, uint wRemoveMsg);
        [System.Runtime.InteropServices.DllImport("user32.dll")] private static extern bool TranslateMessage(ref MSG lpMsg);
        [System.Runtime.InteropServices.DllImport("user32.dll")] private static extern IntPtr DispatchMessage(ref MSG lpMsg);
        [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)] private struct MSG { public IntPtr hwnd; public uint message; public IntPtr wParam; public IntPtr lParam; public uint time; public POINT pt; }
        [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)] private struct POINT { public int x; public int y; }
    }
}
