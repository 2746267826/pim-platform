using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public enum TrackerEventType
{
    AppSwitched,
    PageVisit,
    IdleStarted,
    IdleEnded,
    GapDetected,
    BrowserHeartbeat
}

public sealed class TrackerSessionManager
{
    private static long _globalId;
    private readonly TrackerLogger? _logger;
    private TrackerSession? _current;
    private BrowserHeartbeat? _lastHeartbeat;
    private DateTimeOffset _lastHeartbeatTime = DateTimeOffset.MinValue;
    private readonly TrackerConfig _config;
    private readonly object _lock = new();

    public TrackerSession? Current { get { lock (_lock) return _current; } }
    public bool IsIdle { get { lock (_lock) return _isIdle; } private set { lock (_lock) _isIdle = value; } }
    private bool _isIdle;
    public long SessionsCreated { get { lock (_lock) return _sessionsCreated; } private set { lock (_lock) _sessionsCreated = value; } }
    private long _sessionsCreated;

    public event Action<TrackerSession>? SessionClosed;

    public TrackerSessionManager(TrackerConfig config, TrackerLogger? logger = null)
    {
        _config = config;
        _logger = logger;
    }

    public void UpdateBrowserHeartbeat(BrowserHeartbeat hb)
    {
        lock (_lock)
        {
            _lastHeartbeat = hb;
            _lastHeartbeatTime = DateTimeOffset.UtcNow;
        }
        _logger?.Debug("SessionManager", $"Browser heartbeat: {hb.Domain} audible={hb.Audible}");
    }

    public BrowserHeartbeat? LastHeartbeat { get { lock (_lock) return _lastHeartbeat; } }
    public DateTimeOffset LastHeartbeatTime { get { lock (_lock) return _lastHeartbeatTime; } }
    public bool IsBrowserMediaActive
    {
        get
        {
            lock (_lock)
            {
                return _lastHeartbeat?.Audible == true && (DateTimeOffset.UtcNow - _lastHeartbeatTime).TotalSeconds < 60;
            }
        }
    }

    public TrackerSession? HandleWindowChange(TrackerWindowInfo? window, DateTimeOffset now)
    {
        if (window is null) return null;

        lock (_lock)
        {
            if (_config.ExcludedApps.Any(a => string.Equals(a, window.AppName, StringComparison.OrdinalIgnoreCase)))
            {
                _logger?.Debug("SessionManager", $"Excluded app {window.AppName}, ignoring");
                return null;
            }

            if (_current is null)
            {
                _current = CreateSession(window, now);
                _sessionsCreated++;
                _logger?.Info("SessionManager", $"Session opened: {window.AppName} ({window.WindowTitle}) at {now:O}");
                return _current;
            }

            if (_current.IsIdle)
            {
                CloseCurrentLocked(now);
                _current = CreateSession(window, now);
                _sessionsCreated++;
                _isIdle = false;
                _logger?.Info("SessionManager", $"Idle ended, new session {window.AppName} at {now:O}");
                return _current;
            }

            if (!string.Equals(_current.AppName, window.AppName, StringComparison.OrdinalIgnoreCase))
            {
                var old = CloseCurrentLocked(now);
                _current = CreateSession(window, now);
                _sessionsCreated++;
                _logger?.Info("SessionManager", $"App switched {_current.AppName} from {old?.AppName} at {now:O}");
                return _current;
            }

            if (!string.Equals(_current.WindowTitle, window.WindowTitle, StringComparison.Ordinal))
            {
                // Close previous page visit duration
                if (_current.PageVisits.Count > 0)
                {
                    var prev = _current.PageVisits[^1];
                    if (prev.EndedAt is null)
                    {
                        prev.EndedAt = now;
                        prev.DurationSecs = (now - prev.StartedAt).TotalSeconds;
                    }
                }

                var visit = new TrackerPageVisit
                {
                    WindowTitle = window.WindowTitle,
                    Url = _lastHeartbeat?.Url,
                    Domain = _lastHeartbeat?.Domain,
                    StartedAt = now
                };
                if (IsBrowserApp(window.AppName) && _lastHeartbeat is not null && (now - _lastHeartbeatTime).TotalSeconds < 30)
                {
                    visit.Url = _lastHeartbeat.Url;
                    visit.Domain = _lastHeartbeat.Domain;
                }
                _current.PageVisits.Add(visit);
                _current.WindowTitle = window.WindowTitle;
                _logger?.Debug("SessionManager", $"PageVisit in {window.AppName}: {window.WindowTitle}");
            }

            return null;
        }
    }

    public TrackerSession? HandleIdleStarted(DateTimeOffset now, TimeSpan idleDuration)
    {
        lock (_lock)
        {
            if (_isIdle) return null;

            var grace = TimeSpan.FromSeconds(_config.IdleThresholdSeconds);
            var idleStart = now - grace;

            _isIdle = true;
            var closed = CloseCurrentLocked(idleStart);
            _current = new TrackerSession
            {
                Id = System.Threading.Interlocked.Increment(ref _globalId),
                DeviceId = Environment.MachineName,
                ExePath = "__IDLE__",
                AppName = "__IDLE__",
                WindowTitle = "Idle",
                StartedAt = idleStart,
                IsIdle = true,
                IsMediaActive = IsBrowserMediaActive
            };
            _sessionsCreated++;
            _logger?.Info("SessionManager", $"Idle started at {idleStart:O} (grace {grace.TotalSeconds}s), duration {idleDuration.TotalSeconds}s");
            return closed;
        }
    }

    public TrackerSession? HandleIdleEnded(DateTimeOffset now, TrackerWindowInfo? window)
    {
        lock (_lock)
        {
            if (!_isIdle) return null;
            _isIdle = false;
            var closed = CloseCurrentLocked(now);
            _logger?.Info("SessionManager", $"Idle ended at {now:O}");

            if (window is not null)
            {
                _current = CreateSession(window, now);
                _sessionsCreated++;
            }
            else
            {
                _current = null;
            }
            return closed;
        }
    }

    public TrackerSession? HandleGap(DateTimeOffset gapStart, DateTimeOffset now)
    {
        lock (_lock)
        {
            _logger?.Info("SessionManager", $"Gap detected from {gapStart:O} to {now:O}, duration {(now - gapStart).TotalSeconds}s");
            var closed = CloseCurrentLocked(gapStart);
            _current = null;
            _isIdle = false;
            return closed;
        }
    }

    public TrackerSession? CloseCurrent(DateTimeOffset endedAt)
    {
        lock (_lock)
        {
            return CloseCurrentLocked(endedAt);
        }
    }

    private TrackerSession? CloseCurrentLocked(DateTimeOffset endedAt)
    {
        if (_current is null) return null;
        // finalize last page visit if open
        if (_current.PageVisits.Count > 0)
        {
            var last = _current.PageVisits[^1];
            if (last.EndedAt is null)
            {
                last.EndedAt = endedAt;
                last.DurationSecs = (endedAt - last.StartedAt).TotalSeconds;
                if (last.DurationSecs < 0) last.DurationSecs = 0;
            }
        }
        _current.EndedAt = endedAt;
        _current.DurationSecs = (endedAt - _current.StartedAt).TotalSeconds;
        if (_current.DurationSecs < 0) _current.DurationSecs = 0;
        var closed = _current;
        _current = null;
        // invoke outside lock to avoid deadlock
        Task.Run(() => SessionClosed?.Invoke(closed));
        _logger?.Info("SessionManager", $"Session closed: {closed.AppName} duration {closed.DurationSecs:F1}s pageVisits={closed.PageVisits.Count} idle={closed.IsIdle}");
        return closed;
    }

    public TrackerSession? Flush(DateTimeOffset now)
    {
        lock (_lock)
        {
            if (_current is null) return null;
            // close open page visit snapshot
            var visits = new List<TrackerPageVisit>();
            foreach (var v in _current.PageVisits)
            {
                var copy = new TrackerPageVisit
                {
                    Id = v.Id,
                    SessionId = v.SessionId,
                    WindowTitle = v.WindowTitle,
                    Url = v.Url,
                    Domain = v.Domain,
                    StartedAt = v.StartedAt,
                    EndedAt = v.EndedAt ?? now,
                    DurationSecs = v.DurationSecs ?? (now - v.StartedAt).TotalSeconds
                };
                visits.Add(copy);
            }
            var snapshot = new TrackerSession
            {
                Id = _current.Id,
                DeviceId = _current.DeviceId,
                ExePath = _current.ExePath,
                AppName = _current.AppName,
                WindowTitle = _current.WindowTitle,
                StartedAt = _current.StartedAt,
                EndedAt = now,
                DurationSecs = (now - _current.StartedAt).TotalSeconds,
                IsIdle = _current.IsIdle,
                IsMediaActive = _current.IsMediaActive,
                PageVisits = visits
            };
            return snapshot;
        }
    }

    private static TrackerSession CreateSession(TrackerWindowInfo window, DateTimeOffset now)
    {
        return new TrackerSession
        {
            Id = System.Threading.Interlocked.Increment(ref _globalId),
            DeviceId = Environment.MachineName,
            ExePath = window.ExePath,
            AppName = window.AppName,
            WindowTitle = window.WindowTitle,
            StartedAt = now,
            IsIdle = false,
            IsMediaActive = false
        };
    }

    private static bool IsBrowserApp(string appName)
    {
        var n = appName.ToLowerInvariant();
        return n is "chrome" or "msedge" or "firefox" or "brave" or "opera";
    }
}
