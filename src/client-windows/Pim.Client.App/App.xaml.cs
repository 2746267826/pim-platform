using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Windows;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Win32;
using Pim.Client.App.Services;
using Pim.Client.Core.Services;

namespace Pim.Client.App;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;
    private TrayIcon? _trayIcon;
    private readonly CancellationTokenSource _shutdown = new();
    private readonly PeriodicTimer _heartbeatTimer = new(TimeSpan.FromMinutes(2));
    private Task? _heartbeatTask;
    private int _plannedOfflineSent;
    private Task? _plannedOfflineTask;
    private readonly object _plannedOfflineLock = new();
    private readonly SemaphoreSlim _reportSemaphore = new(1, 1);

    protected override async void OnStartup(StartupEventArgs e)
    {
        Logger.Initialize();

        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            Logger.Error("UnhandledException", args.ExceptionObject as Exception);
        };

        try
        {
            Logger.Info("Daemon starting");
            Services = Pim.Client.App.Startup.ConfigureServices();
            Logger.Info("DI configured");

            // Fire-once best-effort report of planned offline before shutdown/suspend/logoff.
            SystemEvents.SessionEnding += (_, e) =>
            {
                // 停心跳并等待在途心跳结束，避免在途心跳在 planned 请求之后到达服务端把标记清掉；Cancel 幂等。
                StopHeartbeatLoopAndWait();
                TryReportPlannedOffline(e.Reason == SessionEndReasons.SystemShutdown ? "shutdown" : "logoff", wait: true);
            };
            SystemEvents.PowerModeChanged += (_, e) =>
            {
                if (e.Mode == PowerModes.Suspend)
                {
                    TryReportPlannedOffline("suspend");
                }
                else if (e.Mode == PowerModes.Resume)
                {
                    // 等待在途 suspend 上报 ≤2s 结束或超时，再重置防重并立即心跳清服务端 planned 标记。
                    lock (_plannedOfflineLock)
                    {
                        if (_plannedOfflineTask is { } t)
                        {
                            try
                            {
                                t.Wait(TimeSpan.FromSeconds(3));
                            }
                            catch (AggregateException)
                            {
                            }
                        }

                        Interlocked.Exchange(ref _plannedOfflineSent, 0);
                        _plannedOfflineTask = null;
                    }

                    Task.Run(async () =>
                    {
                        try
                        {
                            await ReportHeartbeatOnceAsync(CancellationToken.None);
                        }
                        catch (Exception ex)
                        {
                            Logger.Warn($"Resume heartbeat failed: {ex.Message}");
                        }
                    });
                }
            };

            var config = DaemonConfig.Load();
            // Apply auto-start setting (synchronizes registry with config at every boot)
            AutoStartManager.Set(config.AutoStart);
            var apiClient = Services.GetRequiredService<ApiClient>();
            var authService = Services.GetRequiredService<AuthService>();
            if (!string.IsNullOrEmpty(config.ServerUrl))
            {
                var serverUrl = ApiClient.NormalizeServerUrl(config.ServerUrl);
                if (!string.Equals(config.ServerUrl, serverUrl, StringComparison.Ordinal))
                {
                    config.ServerUrl = serverUrl;
                    config.Save();
                }

                apiClient.SetBaseUrl(serverUrl);
                authService.ServerUrl = serverUrl;
                Logger.Info($"Server URL: {serverUrl}");
            }

            ShutdownMode = ShutdownMode.OnExplicitShutdown;

            apiClient.RequestTiming += (desc, ms) =>
                Logger.Info($"[ApiTiming] {desc} took {ms}ms");

            _trayIcon = Services.GetRequiredService<TrayIcon>();
            _trayIcon.Show();
            Logger.Info("Tray icon shown");

            var restored = await authService.TryRestoreTokenAsync();
            if (restored)
            {
                Logger.Info($"Authenticated as {authService.CurrentUsername} (token restored)");
            }
            else
            {
                Logger.Info("No saved token; showing login window");
                var loginWindow = new LoginWindow();
                var result = loginWindow.ShowDialog();
                if (result == true)
                    Logger.Info($"Authenticated as {authService.CurrentUsername}");
                else
                    Logger.Warn("Login skipped; daemon running without API access, uploads will fail");
            }

            var tracker = Services.GetRequiredService<NativeTrackerService>();
            tracker.Log = msg => Logger.Info(msg);
            tracker.Start();
            Logger.Info("NativeTrackerService started");

            var bridge = Services.GetRequiredService<BrowserBridgeService>();
            // Bridge already started inside tracker, but ensure standalone start if needed
            try { bridge.Start(); } catch { }

            EnsureKeyStatsRunning();

            var keyStatsCollector = Services.GetRequiredService<KeyStatsCollectorService>();
            keyStatsCollector.Log = msg => Logger.Info(msg);
            keyStatsCollector.Start();
            Logger.Info("KeyStats collector started");

            _heartbeatTask = Task.Run(() => RunHeartbeatLoopAsync(_shutdown.Token));
            Logger.Info("Daemon heartbeat loop started");
        }
        catch (Exception ex)
        {
            Logger.Error("Fatal daemon startup error", ex);
            Shutdown();
        }
    }

    internal static void ShowMainShellWindow(string? route = null)
    {
        var existing = Current.Windows.OfType<MainShellWindow>().FirstOrDefault();
        if (existing is not null)
        {
            if (!string.IsNullOrWhiteSpace(route))
            {
                existing.OpenRoute(route);
            }

            existing.Activate();
            return;
        }

        var shell = new MainShellWindow();
        shell.Show();
        if (!string.IsNullOrWhiteSpace(route))
        {
            shell.OpenRoute(route);
        }
    }

    private async Task RunHeartbeatLoopAsync(CancellationToken ct)
    {
        try
        {
            await ReportHeartbeatOnceAsync(ct);

            while (await _heartbeatTimer.WaitForNextTickAsync(ct))
            {
                await ReportHeartbeatOnceAsync(ct);
            }
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
        }
    }

    private static async Task ReportHeartbeatOnceAsync(CancellationToken ct)
    {
        var app = (App)Current;
        await app._reportSemaphore.WaitAsync(ct);
        try
        {
            var reporter = Services.GetRequiredService<DaemonHeartbeatReporter>();
            var config = DaemonConfig.Load();
            var tracker = Services.GetService<NativeTrackerService>();
            var ks = Services.GetRequiredService<KeyStatsCollectorService>();

            var trackerState = tracker is null ? "Unknown"
                : tracker.LastError is null && tracker.EventsUploaded > 0 ? "Available"
                : tracker.LastError is null ? "Unknown" : "Unavailable";
            var ksHealth = ks.LastHealth;
            var ksState = ksHealth?.DaemonSourceState ?? "Unknown";

            var lastSuccess = MaxTime(tracker?.EventsUploaded > 0 ? DateTime.Now : null, ks.LastUploadTime);
            var lastError = tracker?.LastError ?? ks.LastUploadError;
            var version = typeof(App).Assembly
                .GetCustomAttributes(false)
                .OfType<System.Reflection.AssemblyInformationalVersionAttribute>()
                .FirstOrDefault()?.InformationalVersion ?? "0.0.0(unknown)";

            var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
                Environment.MachineName,
                version,
                config.ServerUrl,
                lastSuccess is DateTime dt ? new DateTimeOffset(dt) : null,
                DateTimeOffset.UtcNow,
                lastError,
                tracker is null ? 0 : 0,
                trackerState,
                ksState,
                new
                {
                    keyStatsDetailState = ksHealth?.DetailState.ToString(),
                    keyStatsProcessCount = ksHealth?.ProcessCount,
                    keyStatsSkipReason = ks.LastSkipReason,
                    trackerPollCount = tracker?.PollCount ?? 0,
                    trackerSessionsCreated = tracker?.SessionsCreated ?? 0,
                    trackerEventsUploaded = tracker?.EventsUploaded ?? 0,
                    trackerHookActive = tracker?.HookActive ?? false,
                    trackerBrowserConnected = tracker?.BrowserConnected ?? false
                });
            await reporter.ReportAsync(heartbeat, ct);
            Logger.Info("Daemon heartbeat reported");
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            Logger.Warn($"Daemon heartbeat failed: {ex.Message}");
        }
        finally
        {
            app._reportSemaphore.Release();
        }
    }

    private static DateTime? MaxTime(DateTime? a, DateTime? b)
    {
        if (a is null) return b;
        if (b is null) return a;
        return a > b ? a : b;
    }

    private void TryReportPlannedOffline(string reason, bool wait = false)
    {
        // 置位、创建 task、保存 _plannedOfflineTask、等待逻辑全部在锁内，保证防重语义严格。
        // 调用点都在 UI 线程串行，锁内等待可接受；CTS 在 Task.Run 之前创建，生命周期从创建起 2 秒有界。
        lock (_plannedOfflineLock)
        {
            if (Interlocked.Exchange(ref _plannedOfflineSent, 1) == 1)
            {
                // 已在途：等待既有上报，不重发。
                if (wait && _plannedOfflineTask is not null)
                {
                    try
                    {
                        _plannedOfflineTask.Wait(TimeSpan.FromSeconds(3));
                    }
                    catch (AggregateException)
                    {
                        // 任务内部已 catch，等待超时/异常不抛出。
                    }
                }

                return;
            }

            var reporter = Services?.GetService<PlannedOfflineReporter>();
            if (reporter is null)
            {
                return;
            }

            var request = PlannedOfflineReporter.BuildRequest(Environment.MachineName, reason, DateTimeOffset.UtcNow);
            var cts = new CancellationTokenSource(TimeSpan.FromSeconds(2));
            var task = Task.Run(async () =>
            {
                try
                {
                    await _reportSemaphore.WaitAsync(cts.Token);
                    try
                    {
                        await reporter.ReportAsync(request, cts.Token);
                        Logger.Info($"Planned offline reported ({reason})");
                    }
                    finally
                    {
                        _reportSemaphore.Release();
                    }
                }
                catch (Exception ex)
                {
                    Logger.Warn($"Planned offline report failed ({reason}): {ex.Message}");
                }
                finally
                {
                    cts.Dispose();
                }
            });

            _plannedOfflineTask = task;

            if (wait)
            {
                try
                {
                    task.Wait(TimeSpan.FromSeconds(3));
                }
                catch (AggregateException)
                {
                    // 任务内部已 catch，等待超时/异常不抛出。
                }
            }
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        // 先停心跳并等待在途心跳结束（避免在途心跳清掉 planned 标记），再上报；Cancel 幂等，多次调用安全。
        StopHeartbeatLoopAndWait();
        TryReportPlannedOffline("exit", wait: true);
        try { Services.GetService<NativeTrackerService>()?.Stop(); } catch { }
        try { Services.GetService<BrowserBridgeService>()?.Stop(); } catch { }
        _heartbeatTimer.Dispose();
        _heartbeatTask?.ContinueWith(
            task => Logger.Warn($"Daemon heartbeat loop faulted: {task.Exception?.GetBaseException().Message ?? "unknown error"}"),
            TaskContinuationOptions.OnlyOnFaulted);
        _shutdown.Dispose();
        _trayIcon?.Dispose();
        Logger.Info("Daemon exiting");
        base.OnExit(e);
    }

    private void StopHeartbeatLoopAndWait()
    {
        _shutdown.Cancel();
        if (_heartbeatTask is { } hb)
        {
            try
            {
                hb.Wait(TimeSpan.FromSeconds(2));
            }
            catch (AggregateException)
            {
                // 心跳循环内部已处理。
            }
        }
    }

    /// <summary>
    /// 确保 KeyStats 作为子进程运行（继承守护程序 HIGHEST 权限），替代旧的独立计划任务模式。
    /// </summary>
    private static void EnsureKeyStatsRunning()
    {
        try
        {
            var manager = Services.GetRequiredService<KeyStatsProcessManager>();
            var exe = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsProcessManager.ExeFileName);
            if (!File.Exists(exe))
            {
                Logger.Warn($"KeyStats.exe not found at {exe}");
                return;
            }

            var sessionId = Process.GetCurrentProcess().SessionId;
            var processes = manager.ListProcesses(sessionId);
            var plan = KeyStatsProcessManager.BuildConvergencePlan(processes, sessionId);
            var stopResults = manager.StopProcesses(plan.ProcessIdsToStop);
            if (plan.ProcessIdsToStop.Count > 0)
            {
                Logger.Info($"KeyStats ensure-running stopped {plan.ProcessIdsToStop.Count} extra process(es)");
            }

            if (plan.ShouldStart)
            {
                try
                {
                    // 直接作为子进程拉起，继承父进程 HIGHEST 权限，无需独立计划任务
                    manager.StartInCurrentSession(exe);
                    Logger.Info("KeyStats ensure-running started as child process (inherited HIGHEST)");
                }
                catch (Exception ex)
                {
                    Logger.Warn($"Failed to start KeyStats as child process: {ex.Message}");
                    throw;
                }
            }
            else if (plan.KeepProcessId is int keepPid)
            {
                Logger.Info($"KeyStats ensure-running kept process {keepPid}");
            }
            else
            {
                Logger.Info("KeyStats ensure-running completed with no keep process");
            }
        }
        catch (Exception ex)
        {
            Logger.Error("Failed to ensure KeyStats is running", ex);
        }
    }
}
