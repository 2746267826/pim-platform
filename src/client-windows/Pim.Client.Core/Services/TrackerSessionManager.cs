using Pim.Client.Core.Models;
using Pim.Client.Core.Utils;

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

/// <summary>
/// 会话/页面归属管理器。
///
/// 已知架构限制（#310 评审确认，非本 issue 引入、也不在本次范围内）：
/// 这里只保存**一份**「最近心跳」（<c>_lastHeartbeat</c>），不带 instanceId / HWND /
/// 进程身份。因此同一台机器上开两个浏览器或两个浏览器实例交替心跳时，URL 与
/// <c>Browser</c>/<c>InstanceId</c> 等元数据都可能来自另一个实例；两个同名浏览器窗口
/// 之间切换也不会形成会话边界（只比较 AppName + WindowTitle）。
///
/// 本次修复把归属收紧到「前台应用是浏览器 + 心跳新鲜 + 标题同源」，消除了 issue #310
/// 记录的全部假页面形态（非浏览器应用、浏览器原生窗口/对话框、静默后的陈旧心跳）。
/// 要彻底解决多实例归属，需要把心跳按 instanceId 分桶、并把产生页面访问时的元数据
/// 快照进 <c>TrackerPageVisit</c>；那是独立的结构性改动。
/// </summary>
public sealed class TrackerSessionManager
{
    public const int MaxContinuousSessionSeconds = 1800; // 30 minutes checkpoint (Rule T1b)

    /// <summary>
    /// 心跳新鲜度窗口。插件的心跳间隔是 60s（见 tracker-web config.heartbeat），
    /// 因此窗口必须显著大于一个周期，否则大部分合法心跳都会被判为过期；这里与
    /// <c>BrowserBridgeService.DisconnectAfterSeconds</c>（120s）对齐，保持「桥接认为
    /// 浏览器还活着」与「心跳可用于归属页面」两个口径一致。
    /// </summary>
    public const int HeartbeatFreshnessSeconds = 120;

    /// <summary>
    /// 前缀成立时，被完整包含的那段至少要这么长才可以不要求后面跟分隔符。
    /// 短于此长度的段只有在「后面紧跟分隔符」时才算独立单元，例如「首页」对上
    /// 「首页 - 我的博客」成立，而「新」对上「新标签页 - Google Chrome」不成立。
    /// </summary>
    private const int MinTitleMatchLength = 8;

    private static long _globalId;
    private readonly TrackerLogger? _logger;
    private TrackerSession? _current;
    private BrowserHeartbeat? _lastHeartbeat;
    private DateTimeOffset _lastHeartbeatTime = DateTimeOffset.MinValue;
    private DateTimeOffset? _lastEventEndTime;
    private readonly TrackerConfig _config;
    private readonly TimeProvider _timeProvider;
    private readonly object _lock = new();

    public TrackerSession? Current { get { lock (_lock) return _current; } }
    public bool IsIdle { get { lock (_lock) return _isIdle; } private set { lock (_lock) _isIdle = value; } }
    private bool _isIdle;
    public long SessionsCreated { get { lock (_lock) return _sessionsCreated; } private set { lock (_lock) _sessionsCreated = value; } }
    private long _sessionsCreated;

    public event Action<TrackerSession>? SessionClosed;

    public TrackerSessionManager(TrackerConfig config, TrackerLogger? logger = null, TimeProvider? timeProvider = null)
    {
        _config = config;
        _logger = logger;
        _timeProvider = timeProvider ?? TimeProvider.System;
    }

    public void UpdateBrowserHeartbeat(BrowserHeartbeat hb)
    {
        var now = _timeProvider.GetUtcNow();
        lock (_lock)
        {
            _lastHeartbeat = hb;
            _lastHeartbeatTime = now;

            // 浏览器心跳只能归属给「同一个标签页」的窗口。若前台窗口是浏览器的原生
            // 窗口/对话框，或应用根本不是浏览器，心跳只用于判活，不写页面记录（#310）。
            if (_current != null
                && !_current.IsIdle
                && IsBrowserApp(_current.AppName)
                && !string.IsNullOrWhiteSpace(hb.Url)
                && HeartbeatMatchesWindowTitle(hb, _current.WindowTitle))
            {
                if (_current.PageVisits.Count == 0)
                {
                    _current.PageVisits.Add(new TrackerPageVisit
                    {
                        Id = System.Threading.Interlocked.Increment(ref _globalId),
                        SessionId = _current.Id,
                        WindowTitle = _current.WindowTitle,
                        Url = hb.Url,
                        Domain = hb.Domain,
                        StartedAt = _current.StartedAt
                    });
                }
                else
                {
                    var last = _current.PageVisits[^1];
                    if (!string.Equals(last.Url, hb.Url, StringComparison.OrdinalIgnoreCase))
                    {
                        if (last.EndedAt is null && string.IsNullOrWhiteSpace(last.Url))
                        {
                            // 标题变化先留下「无 URL 占位」，扩展随后上报新页面；就地
                            // 补齐占位（而不是关闭后再开一条）才能让页面时间线连续，
                            // 否则每次标题变化都会把占位段切成碎片。
                            last.Url = hb.Url;
                            last.Domain = hb.Domain;
                            last.WindowTitle = _current.WindowTitle;
                        }
                        else
                        {
                            if (last.EndedAt is null)
                            {
                                last.EndedAt = now;
                                last.DurationSecs = (now - last.StartedAt).TotalSeconds;
                                if (last.DurationSecs < 0) last.DurationSecs = 0;
                            }

                            _current.PageVisits.Add(new TrackerPageVisit
                            {
                                Id = System.Threading.Interlocked.Increment(ref _globalId),
                                SessionId = _current.Id,
                                WindowTitle = _current.WindowTitle,
                                Url = hb.Url,
                                Domain = hb.Domain,
                                StartedAt = now
                            });
                        }
                    }
                }
            }
        }
        _logger?.Debug("SessionManager", $"Browser heartbeat: {hb.Domain} audible={hb.Audible}");
    }

    /// <summary>
    /// 浏览器心跳能否归属到某个窗口标题。
    ///
    /// 插件上报的是「浏览器当前激活标签页」，而前台窗口可能是浏览器的原生窗口
    /// （附加组件管理器、文件选择对话框…）或另一个应用。这类窗口的标题与标签页
    /// 无关，绝不能借心跳把浏览器标签的 URL 挂上去——#310 的假页面记录正来源于此。
    ///
    /// 判定规则（按顺序）：
    /// 1. 两边都非空；
    /// 2. 完全相等 → 同源；
    /// 3. 去掉 Windows 的截断省略号（尾部 <c>…</c> / <c>...</c>）；
    /// 4. 要求一方是另一方的前缀（心跳标题是窗口标题前缀，或反之——后者对应窗口
    ///    标题被截断），否则不同源；
    /// 5. 前缀成立后，被完整包含的那一方要么本身够长（≥ <see cref="MinTitleMatchLength"/>），
    ///    要么紧跟其后的字符是**分隔符**（空格、<c>-</c>、<c>|</c>、<c>·</c> 等）。
    ///    这条同时挡住两个方向的误判：
    ///    - 短标题「新」对上「新标签页 - Google Chrome」：下一个字符是「标」不是分隔符
    ///      → 拒绝（前缀相同但并非同一标签页）；
    ///    - 短标题「首页」对上「首页 - 我的博客」：下一个字符是空格 → 接受（同一标签页）。
    ///
    /// 注意：这里只能做到「标题同源」这一层证据。真正的多实例/多窗口精确关联需要
    /// 按 instanceId + HWND 分别保存心跳，属于既有架构限制（本 issue 未要求），
    /// 见 TrackerSessionManager 顶部关于心跳来源的说明。
    /// </summary>
    public static bool HeartbeatMatchesWindowTitle(BrowserHeartbeat hb, string? windowTitle)
    {
        if (string.IsNullOrWhiteSpace(hb.Title) || string.IsNullOrWhiteSpace(windowTitle))
            return false;

        var heartbeatTitle = hb.Title.Trim();
        var title = StripTruncationMarker(windowTitle.Trim());

        if (string.Equals(heartbeatTitle, title, StringComparison.OrdinalIgnoreCase))
            return true;

        // 心跳标题是窗口标题前缀（窗口标题通常为 "<标签页标题> - <浏览器名>"）。
        if (title.StartsWith(heartbeatTitle, StringComparison.OrdinalIgnoreCase))
            return IsCompleteToken(heartbeatTitle.Length, title);

        // 窗口标题是心跳标题前缀（窗口标题被截断，或标签页标题更长）。
        if (heartbeatTitle.StartsWith(title, StringComparison.OrdinalIgnoreCase))
            return IsCompleteToken(title.Length, heartbeatTitle);

        return false;
    }

    /// <summary>去掉 Windows 截断省略号，避免它破坏前缀比较。</summary>
    private static string StripTruncationMarker(string title)
    {
        if (title.EndsWith('…'))
            return title[..^1].TrimEnd();
        if (title.EndsWith("...", StringComparison.Ordinal))
            return title[..^3].TrimEnd();
        return title;
    }

    /// <summary>
    /// 被完整包含的那段能否视作独立单元：足够长，或它在更长字符串中的下一个字符是分隔符。
    /// </summary>
    private static bool IsCompleteToken(int matchedLength, string longer)
    {
        // longer 一定比 matchedLength 长（相等已在调用方短路），索引必然有效。
        return matchedLength >= MinTitleMatchLength || IsTitleSeparator(longer[matchedLength]);
    }

    private static bool IsTitleSeparator(char c)
        => c is ' ' or '-' or '–' or '—' or '|' or '·' or ':' or '/' or '\t' or '[' or '(';

    /// <summary>
    /// 当前窗口标题对应的心跳是否仍然新鲜（#310：陈旧心跳不得被归属到新窗口）。
    /// </summary>
    private bool IsHeartbeatFreshFor(DateTimeOffset now, string? windowTitle)
        => _lastHeartbeat is not null
            && (now - _lastHeartbeatTime).TotalSeconds < HeartbeatFreshnessSeconds
            && HeartbeatMatchesWindowTitle(_lastHeartbeat, windowTitle);

    /// <summary>
    /// 为「前台应用 + 窗口标题」构造一条可归属的页面访问记录；无法归属时返回 null。
    ///
    /// 归属成立需要同时满足三条（#310）：
    /// 1. 前台应用是浏览器 —— 否则该窗口本就不该产出页面记录；
    /// 2. 心跳仍然新鲜 —— 静默后的陈旧 URL 与当前窗口无关；
    /// 3. 心跳标题与窗口标题对得上 —— 排除浏览器的原生窗口/对话框（它们带着另一个
    ///    标题，却会继承当前标签页的心跳）。
    /// </summary>
    private TrackerPageVisit? AttachHeartbeatIfAttributable(string appName, DateTimeOffset now, string? windowTitle)
    {
        if (!IsBrowserApp(appName))
            return null;
        if (_lastHeartbeat is null || string.IsNullOrWhiteSpace(_lastHeartbeat.Url))
            return null;
        if (!IsHeartbeatFreshFor(now, windowTitle))
            return null;

        return new TrackerPageVisit
        {
            Id = System.Threading.Interlocked.Increment(ref _globalId),
            SessionId = _current!.Id,
            WindowTitle = windowTitle,
            Url = _lastHeartbeat.Url,
            Domain = _lastHeartbeat.Domain,
            StartedAt = now,
        };
    }

    public BrowserHeartbeat? LastHeartbeat { get { lock (_lock) return _lastHeartbeat; } }
    public DateTimeOffset LastHeartbeatTime { get { lock (_lock) return _lastHeartbeatTime; } }
    public bool IsBrowserMediaActive
    {
        get
        {
            lock (_lock)
            {
                return _lastHeartbeat?.Audible == true
                    && (_timeProvider.GetUtcNow() - _lastHeartbeatTime).TotalSeconds < 60;
            }
        }
    }

    public TrackerSession? HandleWindowChange(TrackerWindowInfo? window, DateTimeOffset now)
    {
        if (window is null) return null;

        TrackerSession? closed = null;
        TrackerSession? result = null;
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
                result = _current;
            }
            else if (_current.IsIdle)
            {
                closed = CloseCurrentLocked(now);
                _current = CreateSession(window, now);
                _sessionsCreated++;
                _isIdle = false;
                _logger?.Info("SessionManager", $"Idle ended, new session {window.AppName} at {now:O}");
                result = _current;
            }
            else if (!string.Equals(_current.AppName, window.AppName, StringComparison.OrdinalIgnoreCase))
            {
                closed = CloseCurrentLocked(now);
                _current = CreateSession(window, now);
                _sessionsCreated++;
                _logger?.Info("SessionManager", $"App switched {_current.AppName} from {closed?.AppName} at {now:O}");
                result = _current;
            }
            else if (!string.Equals(_current.WindowTitle, window.WindowTitle, StringComparison.Ordinal))
            {
                if (_current.PageVisits.Count > 0)
                {
                    var prev = _current.PageVisits[^1];
                    if (prev.EndedAt is null)
                    {
                        prev.EndedAt = now;
                        prev.DurationSecs = (now - prev.StartedAt).TotalSeconds;
                    }
                }

                // #310：窗口标题变化本身不是页面证据。只有前台应用确实是浏览器、
                // 且心跳与这个窗口标题对得上（同一标签页）时，才能把 URL 归属到这里；
                // 否则只留一条无 URL 的占位 visit，由后续心跳就地补齐。
                var visit = AttachHeartbeatIfAttributable(window.AppName, now, window.WindowTitle)
                    ?? new TrackerPageVisit
                    {
                        Id = System.Threading.Interlocked.Increment(ref _globalId),
                        SessionId = _current.Id,
                        WindowTitle = window.WindowTitle,
                        Url = null,
                        Domain = null,
                        StartedAt = now
                    };

                _current.PageVisits.Add(visit);
                _current.WindowTitle = window.WindowTitle;
                _logger?.Debug("SessionManager", $"PageVisit in {window.AppName}: {window.WindowTitle}");
            }
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return result;
    }

    public TrackerSession? HandleIdleStarted(DateTimeOffset now, TimeSpan idleDuration)
    {
        TrackerSession? closed = null;
        lock (_lock)
        {
            if (_isIdle) return null;

            var grace = TimeSpan.FromSeconds(_config.IdleThresholdSeconds);
            var idleStart = now - grace;

            // Clamp idleStart so it NEVER backtracks into previous events or gaps
            if (_lastEventEndTime.HasValue && idleStart < _lastEventEndTime.Value)
            {
                idleStart = _lastEventEndTime.Value;
            }
            if (_current != null && idleStart < _current.StartedAt)
            {
                idleStart = _current.StartedAt;
            }

            _isIdle = true;
            closed = CloseCurrentLocked(idleStart);
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
            _lastEventEndTime = idleStart;
            _logger?.Info("SessionManager", $"Idle started at {idleStart:O} (grace {grace.TotalSeconds}s), duration {idleDuration.TotalSeconds}s");
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return closed;
    }

    public TrackerSession? HandleScreenOff(DateTimeOffset now)
    {
        TrackerSession? closed = null;
        lock (_lock)
        {
            if (_isIdle) return null;

            var idleStart = now;
            if (_lastEventEndTime.HasValue && idleStart < _lastEventEndTime.Value)
            {
                idleStart = _lastEventEndTime.Value;
            }
            if (_current != null && idleStart < _current.StartedAt)
            {
                idleStart = _current.StartedAt;
            }

            _isIdle = true;
            closed = CloseCurrentLocked(idleStart);
            _current = new TrackerSession
            {
                Id = System.Threading.Interlocked.Increment(ref _globalId),
                DeviceId = Environment.MachineName,
                ExePath = "__IDLE__",
                AppName = "__IDLE__",
                WindowTitle = "ScreenOff",
                StartedAt = idleStart,
                IsIdle = true,
                IsMediaActive = false
            };
            _sessionsCreated++;
            _lastEventEndTime = idleStart;
            _logger?.Info("SessionManager", $"Screen off detected, idle started at {idleStart:O}");
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return closed;
    }

    public TrackerSession? HandleIdleEnded(DateTimeOffset now, TrackerWindowInfo? window)
    {
        TrackerSession? closed = null;
        lock (_lock)
        {
            if (!_isIdle) return null;
            _isIdle = false;
            closed = CloseCurrentLocked(now);
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
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return closed;
    }

    public TrackerSession? CheckpointIfNeeded(DateTimeOffset now, TrackerWindowInfo? currentWindow)
    {
        TrackerSession? closed = null;
        lock (_lock)
        {
            if (_current is null) return null;

            var duration = (now - _current.StartedAt).TotalSeconds;
            var crossedDay = BusinessDayUtils.GetBusinessDate(now) != BusinessDayUtils.GetBusinessDate(_current.StartedAt);

            if (duration >= MaxContinuousSessionSeconds || crossedDay)
            {
                var isIdle = _current.IsIdle;
                var appName = _current.AppName;
                var exePath = _current.ExePath;
                var windowTitle = _current.WindowTitle;
                var isMediaActive = _current.IsMediaActive;

                closed = CloseCurrentLocked(now);

                if (isIdle)
                {
                    _current = new TrackerSession
                    {
                        Id = System.Threading.Interlocked.Increment(ref _globalId),
                        DeviceId = Environment.MachineName,
                        ExePath = exePath,
                        AppName = appName,
                        WindowTitle = windowTitle,
                        StartedAt = now,
                        IsIdle = true,
                        IsMediaActive = IsBrowserMediaActive
                    };
                }
                else if (currentWindow is not null)
                {
                    _current = CreateSession(currentWindow, now);
                }
                else
                {
                    _current = new TrackerSession
                    {
                        Id = System.Threading.Interlocked.Increment(ref _globalId),
                        DeviceId = Environment.MachineName,
                        ExePath = exePath,
                        AppName = appName,
                        WindowTitle = windowTitle,
                        StartedAt = now,
                        IsIdle = false,
                        IsMediaActive = isMediaActive
                    };
                }
                _sessionsCreated++;
                _lastEventEndTime = now;
                _logger?.Info("SessionManager", $"Session checkpointed at {now:O} for {appName} (dur={duration:F0}s, crossedDay={crossedDay})");
            }
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return closed;
    }

    public TrackerSession? HandleGap(DateTimeOffset gapStart, DateTimeOffset now)
    {
        TrackerSession? closed = null;
        lock (_lock)
        {
            _logger?.Info("SessionManager", $"Gap detected from {gapStart:O} to {now:O}, duration {(now - gapStart).TotalSeconds}s");
            closed = CloseCurrentLocked(gapStart);
            _current = null;
            _isIdle = false;
            _lastEventEndTime = now;
        }
        if (closed is not null) RaiseSessionClosed(closed);
        return closed;
    }

    public TrackerSession? CloseCurrent(DateTimeOffset endedAt)
    {
        TrackerSession? closed;
        lock (_lock)
        {
            closed = CloseCurrentLocked(endedAt);
        }
        if (closed is not null)
            RaiseSessionClosed(closed);
        return closed;
    }

    private TrackerSession? CloseCurrentLocked(DateTimeOffset endedAt)
    {
        if (_current is null) return null;
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
        _lastEventEndTime = endedAt;
        _logger?.Info("SessionManager", $"Session closed: {closed.AppName} duration {closed.DurationSecs:F1}s pageVisits={closed.PageVisits.Count} idle={closed.IsIdle}");
        return closed;
    }

    private void RaiseSessionClosed(TrackerSession session)
    {
        try
        {
            SessionClosed?.Invoke(session);
        }
        catch (Exception ex)
        {
            _logger?.Warn("SessionManager", $"SessionClosed invocation error: {ex.Message}");
        }
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

    /// <summary>
    /// 前台应用是否为可产出页面记录的浏览器。与 <c>BrowserBridgeService</c> 认可的
    /// 浏览器类型保持一致（chrome/edge/firefox/safari/other 的心跳）；这里按进程名判断。
    /// </summary>
    public static bool IsBrowserApp(string? appName)
    {
        if (string.IsNullOrWhiteSpace(appName))
            return false;

        var n = appName.ToLowerInvariant();
        return n is "chrome" or "msedge" or "firefox" or "brave" or "opera";
    }
}
