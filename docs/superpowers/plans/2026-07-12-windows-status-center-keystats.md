# Windows Status Center And KeyStats Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Windows 客户端收回「托盘守护 + 精简状态中心」主路径，修复 KeyStats 全 0/会话冲突，并只在健康时上传键鼠样本。

**Architecture:** 在 `Pim.Client.Core` 新增可单测的 KeyStats 健康探针与进程管理，采集器按健康门禁决定是否上传；`DaemonHeartbeatReporter` 写入真实 AW/KeyStats 状态；`StatusWindow` 升级为四区状态中心；`App`/`TrayIcon` 去掉 Companion Shell 主入口但保留 WebView2 源码。

**Tech Stack:** .NET 8, WPF + WinForms tray, xUnit, HttpClient, local KeyStats API `127.0.0.1:18080`, ActivityWatch `127.0.0.1:5600`, optional edits to `https://github.com/2746267826/keyStats`.

**Spec:** `docs/superpowers/specs/2026-07-12-windows-status-center-keystats-design.md`

---

## File Map

| File | Responsibility |
| --- | --- |
| `src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs` | 健康枚举、进程信息、探针结果、快照 DTO |
| `src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs` | 纯判定：进程/会话/API/增长 → 状态 |
| `src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs` | 枚举进程 Session、停止非目标实例、用户态启动/重启 |
| `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs` | 采样 + 健康门禁 + 条件上传 |
| `src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs` | 组装真实 heartbeat |
| `src/client-windows/Pim.Client.App/App.xaml.cs` | 启动不弹 Shell；用 process manager 拉起 KeyStats |
| `src/client-windows/Pim.Client.App/TrayIcon.cs` | 精简菜单；双击开状态中心；浏览器打开 Web |
| `src/client-windows/Pim.Client.App/StatusWindow.xaml(.cs)` | 四区状态中心 UI |
| `src/client-windows/Pim.Client.App/Startup.cs` | DI 注册新服务 |
| `tests/Pim.UnitTests/ClientWindows/*` | 健康、上传门禁、心跳、托盘/启动主路径测试 |
| `MainShellWindow.*` / `EmbeddedWebViewHost.cs` | **保留，不改职责**；仅从启动/托盘主路径移除 |

---

### Task 0: Branch And Workspace

**Files:** none (git only)

- [ ] **Step 1: Create implementation branch and worktree from latest master**

```powershell
git fetch origin master
git worktree add ..\pim-windows-status-center origin/master
cd ..\pim-windows-status-center
git checkout -b codex/windows-status-center-keystats
```

Expected: clean branch tracking new work from `origin/master`.

- [ ] **Step 2: Confirm design/spec available**

```powershell
Test-Path docs/superpowers/specs/2026-07-12-windows-status-center-keystats-design.md
Test-Path docs/superpowers/plans/2026-07-12-windows-status-center-keystats.md
```

If missing, copy from the design branch / this plan branch before coding.

- [ ] **Step 3: Commit nothing yet**

Only create workspace. No source changes in this task.

---

### Task 1: KeyStats Health Models And Pure Probe

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs`
- Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs`

- [ ] **Step 1: Write the failing tests**

Create `tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs`:

```csharp
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

public class KeyStatsHealthProbeTests
{
    [Fact]
    public void Evaluate_ReturnsMissingProcess_WhenNoProcesses()
    {
        var result = KeyStatsHealthProbe.Evaluate(
            processes: Array.Empty<KeyStatsProcessInfo>(),
            currentSessionId: 1,
            snapshot: null,
            previousSnapshot: null,
            apiError: null);

        Assert.Equal(KeyStatsDetailState.MissingProcess, result.DetailState);
        Assert.False(result.CanUpload);
        Assert.Equal("Unavailable", result.DaemonSourceState);
    }

    [Fact]
    public void Evaluate_ReturnsApiUnreachable_WhenApiError()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(100, 1, isCurrentUserSession: true)
        };

        var result = KeyStatsHealthProbe.Evaluate(
            processes,
            currentSessionId: 1,
            snapshot: null,
            previousSnapshot: null,
            apiError: "Connection refused");

        Assert.Equal(KeyStatsDetailState.ApiUnreachable, result.DetailState);
        Assert.False(result.CanUpload);
    }

    [Fact]
    public void Evaluate_ReturnsStaleZero_WhenAllCountersZeroAndNoGrowth()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(100, 1, isCurrentUserSession: true)
        };
        var snapshot = new KeyStatsCounterSnapshot(
            KeyPresses: 0,
            LeftClicks: 0,
            RightClicks: 0,
            MiddleClicks: 0,
            SideBackClicks: 0,
            SideForwardClicks: 0,
            MouseDistance: 0,
            ScrollDistance: 0);

        var result = KeyStatsHealthProbe.Evaluate(
            processes,
            currentSessionId: 1,
            snapshot,
            previousSnapshot: snapshot,
            apiError: null);

        Assert.Equal(KeyStatsDetailState.ApiOkButStaleZero, result.DetailState);
        Assert.False(result.CanUpload);
        Assert.Equal("stale-zero", result.SkipReason);
    }

    [Fact]
    public void Evaluate_ReturnsAvailable_WhenCountersNonZero()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(100, 1, isCurrentUserSession: true)
        };
        var snapshot = new KeyStatsCounterSnapshot(
            KeyPresses: 12,
            LeftClicks: 3,
            RightClicks: 0,
            MiddleClicks: 0,
            SideBackClicks: 0,
            SideForwardClicks: 0,
            MouseDistance: 100,
            ScrollDistance: 0);

        var result = KeyStatsHealthProbe.Evaluate(
            processes,
            currentSessionId: 1,
            snapshot,
            previousSnapshot: null,
            apiError: null);

        Assert.Equal(KeyStatsDetailState.Available, result.DetailState);
        Assert.True(result.CanUpload);
        Assert.Equal("Available", result.DaemonSourceState);
        Assert.Null(result.SkipReason);
    }

    [Fact]
    public void Evaluate_ReturnsAvailable_WhenCountersGrewFromPrevious()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(100, 1, isCurrentUserSession: true)
        };
        var previous = new KeyStatsCounterSnapshot(1, 0, 0, 0, 0, 0, 0, 0);
        var current = previous with { KeyPresses = 2 };

        var result = KeyStatsHealthProbe.Evaluate(
            processes,
            currentSessionId: 1,
            current,
            previous,
            apiError: null);

        Assert.Equal(KeyStatsDetailState.Available, result.DetailState);
        Assert.True(result.CanUpload);
    }

    [Fact]
    public void Evaluate_FlagsForeignSessionProcesses()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(100, 0, isCurrentUserSession: false),
            new KeyStatsProcessInfo(200, 1, isCurrentUserSession: true)
        };
        var snapshot = new KeyStatsCounterSnapshot(5, 1, 0, 0, 0, 0, 10, 0);

        var result = KeyStatsHealthProbe.Evaluate(
            processes,
            currentSessionId: 1,
            snapshot,
            previousSnapshot: null,
            apiError: null);

        Assert.True(result.HasForeignSessionProcess);
        Assert.Equal(2, result.ProcessCount);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsHealthProbeTests"
```

Expected: FAIL because types/methods do not exist.

- [ ] **Step 3: Implement models**

Create `src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs`:

```csharp
namespace Pim.Client.Core.Models;

public enum KeyStatsDetailState
{
    MissingProcess,
    ApiUnreachable,
    ApiOkButStaleZero,
    Available
}

public sealed record KeyStatsProcessInfo(
    int ProcessId,
    int SessionId,
    bool IsCurrentUserSession);

public sealed record KeyStatsCounterSnapshot(
    int KeyPresses,
    int LeftClicks,
    int RightClicks,
    int MiddleClicks,
    int SideBackClicks,
    int SideForwardClicks,
    double MouseDistance,
    double ScrollDistance)
{
    public int TotalClicks =>
        LeftClicks + RightClicks + MiddleClicks + SideBackClicks + SideForwardClicks;

    public bool HasAnyActivity =>
        KeyPresses > 0 ||
        TotalClicks > 0 ||
        MouseDistance > 0 ||
        ScrollDistance > 0;

    public bool GrewFrom(KeyStatsCounterSnapshot? previous)
    {
        if (previous is null)
        {
            return false;
        }

        return KeyPresses > previous.KeyPresses ||
               TotalClicks > previous.TotalClicks ||
               MouseDistance > previous.MouseDistance ||
               ScrollDistance > previous.ScrollDistance;
    }
}

public sealed record KeyStatsHealthResult(
    KeyStatsDetailState DetailState,
    string DaemonSourceState,
    bool CanUpload,
    string? SkipReason,
    int ProcessCount,
    bool HasForeignSessionProcess,
    KeyStatsCounterSnapshot? Snapshot,
    string SummaryZh);
```

- [ ] **Step 4: Implement probe**

Create `src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs`:

```csharp
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public static class KeyStatsHealthProbe
{
    public static KeyStatsHealthResult Evaluate(
        IReadOnlyList<KeyStatsProcessInfo> processes,
        int currentSessionId,
        KeyStatsCounterSnapshot? snapshot,
        KeyStatsCounterSnapshot? previousSnapshot,
        string? apiError)
    {
        var processCount = processes.Count;
        var hasForeign = processes.Any(p => !p.IsCurrentUserSession || p.SessionId != currentSessionId);

        if (processCount == 0)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.MissingProcess,
                "Unavailable",
                CanUpload: false,
                SkipReason: "missing-process",
                processCount,
                hasForeign,
                snapshot,
                "KeyStats 进程未运行");
        }

        if (!string.IsNullOrWhiteSpace(apiError) || snapshot is null)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.ApiUnreachable,
                "Unavailable",
                CanUpload: false,
                SkipReason: "api-unreachable",
                processCount,
                hasForeign,
                snapshot,
                $"KeyStats API 不可达：{apiError ?? "empty snapshot"}");
        }

        var available = snapshot.HasAnyActivity || snapshot.GrewFrom(previousSnapshot);
        if (!available)
        {
            return new KeyStatsHealthResult(
                KeyStatsDetailState.ApiOkButStaleZero,
                "Unavailable",
                CanUpload: false,
                SkipReason: "stale-zero",
                processCount,
                hasForeign,
                snapshot,
                hasForeign
                    ? "KeyStats API 可达但计数全 0，且存在非当前会话实例"
                    : "KeyStats API 可达但计数全 0 或不增长");
        }

        return new KeyStatsHealthResult(
            KeyStatsDetailState.Available,
            "Available",
            CanUpload: true,
            SkipReason: null,
            processCount,
            hasForeign,
            snapshot,
            hasForeign
                ? "KeyStats 可用，但存在额外会话实例"
                : "KeyStats 可用");
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsHealthProbeTests"
```

Expected: PASS

- [ ] **Step 6: Commit**

```powershell
git add src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs `
  src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs `
  tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs
git commit -m "feat(windows): add KeyStats health probe and models"
```

---

### Task 2: KeyStats Process Manager

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs`

Note: process kill/start is OS-bound. Unit-test pure selection logic via injectable process list + action callbacks, or test the selection helpers as static methods.

- [ ] **Step 1: Write failing tests for target selection**

```csharp
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

public class KeyStatsProcessManagerTests
{
    [Fact]
    public void SelectActions_KeepsOneCurrentSessionProcess_AndStopsOthers()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(10, 0, false),
            new KeyStatsProcessInfo(20, 1, true),
            new KeyStatsProcessInfo(30, 1, true)
        };

        var plan = KeyStatsProcessManager.BuildConvergencePlan(processes, currentSessionId: 1);

        Assert.Equal(new[] { 10, 30 }, plan.ProcessIdsToStop);
        Assert.False(plan.ShouldStart);
        Assert.Equal(20, plan.KeepProcessId);
    }

    [Fact]
    public void SelectActions_Starts_WhenNoCurrentSessionProcess()
    {
        var processes = new[]
        {
            new KeyStatsProcessInfo(10, 0, false)
        };

        var plan = KeyStatsProcessManager.BuildConvergencePlan(processes, currentSessionId: 1);

        Assert.Equal(new[] { 10 }, plan.ProcessIdsToStop);
        Assert.True(plan.ShouldStart);
        Assert.Null(plan.KeepProcessId);
    }
}
```

- [ ] **Step 2: Run to verify fail**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsProcessManagerTests"
```

Expected: FAIL

- [ ] **Step 3: Implement manager**

Create `src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs`:

```csharp
using System.Diagnostics;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed record KeyStatsConvergencePlan(
    int? KeepProcessId,
    IReadOnlyList<int> ProcessIdsToStop,
    bool ShouldStart);

public sealed class KeyStatsProcessManager
{
    public const string ProcessName = "KeyStats";
    public const string ExeFileName = "KeyStats.exe";

    public static KeyStatsConvergencePlan BuildConvergencePlan(
        IReadOnlyList<KeyStatsProcessInfo> processes,
        int currentSessionId)
    {
        var current = processes
            .Where(p => p.IsCurrentUserSession && p.SessionId == currentSessionId)
            .OrderBy(p => p.ProcessId)
            .ToList();
        var foreign = processes
            .Where(p => !(p.IsCurrentUserSession && p.SessionId == currentSessionId))
            .Select(p => p.ProcessId)
            .ToList();

        if (current.Count == 0)
        {
            return new KeyStatsConvergencePlan(null, foreign, ShouldStart: true);
        }

        var keep = current[0].ProcessId;
        var stopExtraCurrent = current.Skip(1).Select(p => p.ProcessId);
        var stop = foreign.Concat(stopExtraCurrent).Distinct().OrderBy(id => id).ToArray();
        return new KeyStatsConvergencePlan(keep, stop, ShouldStart: false);
    }

    public IReadOnlyList<KeyStatsProcessInfo> ListProcesses(int currentSessionId)
    {
        var result = new List<KeyStatsProcessInfo>();
        foreach (var process in Process.GetProcessesByName(ProcessName))
        {
            try
            {
                var sessionId = process.SessionId;
                result.Add(new KeyStatsProcessInfo(
                    process.Id,
                    sessionId,
                    sessionId == currentSessionId));
            }
            catch
            {
                // ignore processes that exit mid-enumeration
            }
            finally
            {
                process.Dispose();
            }
        }

        return result;
    }

    public KeyStatsConvergencePlan EnsureRunning(string keyStatsExePath, int currentSessionId)
    {
        var processes = ListProcesses(currentSessionId);
        var plan = BuildConvergencePlan(processes, currentSessionId);

        foreach (var pid in plan.ProcessIdsToStop)
        {
            TryStop(pid);
        }

        if (plan.ShouldStart)
        {
            StartInCurrentSession(keyStatsExePath);
        }

        return plan;
    }

    public void Restart(string keyStatsExePath, int currentSessionId)
    {
        foreach (var process in ListProcesses(currentSessionId))
        {
            TryStop(process.ProcessId);
        }

        StartInCurrentSession(keyStatsExePath);
    }

    private static void TryStop(int processId)
    {
        try
        {
            using var process = Process.GetProcessById(processId);
            process.Kill(entireProcessTree: true);
            process.WaitForExit(3000);
        }
        catch
        {
            // best effort
        }
    }

    private static void StartInCurrentSession(string keyStatsExePath)
    {
        if (!File.Exists(keyStatsExePath))
        {
            throw new FileNotFoundException("KeyStats.exe not found", keyStatsExePath);
        }

        Process.Start(new ProcessStartInfo
        {
            FileName = keyStatsExePath,
            UseShellExecute = true,
            WorkingDirectory = Path.GetDirectoryName(keyStatsExePath) ?? Environment.CurrentDirectory
        });
    }
}
```

Important behavior change vs current `EnsureKeyStatsRunning`:
- Do **not** prefer `schtasks /run /tn PimKeyStats` as the only path (it can land in Session 0).
- Prefer direct user-session start.
- Stop foreign-session processes when converging.

- [ ] **Step 4: Pass tests and commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsProcessManagerTests"
git add src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs `
  tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs
git commit -m "feat(windows): converge KeyStats to single user-session process"
```

---

### Task 3: Collector Upload Gate (Skip Stale Zero)

**Files:**
- Modify: `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs`

The current collector always uploads any successful `/api/stats/` JSON. Change it to:

1. Fetch snapshot
2. Evaluate health with previous counters
3. If `!CanUpload`, set `LastUploadError`/`LastSkipReason` and return without POSTing
4. If `CanUpload`, POST samples + legacy as today
5. Expose latest `KeyStatsHealthResult` for UI/heartbeat

Because `KeyStatsCollectorService` uses real `HttpClient`/`ApiClient`, extract a pure gate helper to keep unit tests simple.

- [ ] **Step 1: Write failing gate tests**

```csharp
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

public class KeyStatsCollectorUploadGateTests
{
    [Fact]
    public void ShouldUpload_IsFalse_ForStaleZero()
    {
        var health = KeyStatsHealthProbe.Evaluate(
            new[] { new KeyStatsProcessInfo(1, 1, true) },
            1,
            new KeyStatsCounterSnapshot(0, 0, 0, 0, 0, 0, 0, 0),
            new KeyStatsCounterSnapshot(0, 0, 0, 0, 0, 0, 0, 0),
            null);

        Assert.False(KeyStatsCollectorService.ShouldUpload(health));
    }

    [Fact]
    public void ShouldUpload_IsTrue_ForAvailable()
    {
        var health = KeyStatsHealthProbe.Evaluate(
            new[] { new KeyStatsProcessInfo(1, 1, true) },
            1,
            new KeyStatsCounterSnapshot(9, 1, 0, 0, 0, 0, 1, 0),
            null,
            null);

        Assert.True(KeyStatsCollectorService.ShouldUpload(health));
    }
}
```

- [ ] **Step 2: Run fail, then implement gate + collector changes**

In `KeyStatsCollectorService`:

```csharp
public static bool ShouldUpload(KeyStatsHealthResult health) => health.CanUpload;

// fields
private KeyStatsCounterSnapshot? _previousSnapshot;
private KeyStatsHealthResult? _lastHealth;
private string? _lastSkipReason;
private readonly KeyStatsProcessManager _processManager;

public KeyStatsHealthResult? LastHealth { get { lock (LockObj) return _lastHealth; } }
public string? LastSkipReason { get { lock (LockObj) return _lastSkipReason; } }

// in CollectAndUploadAsync after fetching stats JSON:
var processes = _processManager.ListProcesses(Process.GetCurrentProcess().SessionId);
var snapshot = new KeyStatsCounterSnapshot(
    stats.KeyPresses,
    stats.LeftClicks,
    stats.RightClicks,
    stats.MiddleClicks,
    stats.SideBackClicks,
    stats.SideForwardClicks,
    stats.MouseDistance,
    stats.ScrollDistance);
var health = KeyStatsHealthProbe.Evaluate(
    processes,
    Process.GetCurrentProcess().SessionId,
    snapshot,
    _previousSnapshot,
    apiError: null);
_previousSnapshot = snapshot;
lock (LockObj)
{
    _lastHealth = health;
    _lastSkipReason = health.SkipReason;
}

if (!ShouldUpload(health))
{
    lock (LockObj) { _lastUploadError = health.SummaryZh; }
    Log?.Invoke($"[KeyStatsCollector] Skip upload: {health.SkipReason} ({health.SummaryZh})");
    return;
}

// existing upload path...
```

Constructor: accept optional `KeyStatsProcessManager` or new it internally for DI simplicity:

```csharp
public KeyStatsCollectorService(ApiClient api, KeyStatsProcessManager? processManager = null)
{
    _api = api;
    _processManager = processManager ?? new KeyStatsProcessManager();
    ...
}
```

Register singleton in `Startup.cs`:

```csharp
services.AddSingleton<KeyStatsProcessManager>();
services.AddSingleton<KeyStatsCollectorService>();
```

- [ ] **Step 3: Pass tests and commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsCollectorUploadGateTests|FullyQualifiedName~KeyStatsHealthProbeTests"
git add src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs `
  src/client-windows/Pim.Client.App/Startup.cs `
  tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs
git commit -m "fix(windows): skip KeyStats upload when stale zero or unhealthy"
```

---

### Task 4: Real Heartbeat Source States

**Files:**
- Modify: `src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs`
- Modify: `src/client-windows/Pim.Client.App/App.xaml.cs`
- Modify: `tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs`

- [ ] **Step 1: Update/extend tests**

Replace the assumption that states are always Unknown. Add:

```csharp
[Fact]
public void BuildHeartbeat_UsesProvidedSourceStatesAndStatusDetails()
{
    var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
        deviceId: "device-1",
        version: "1.0.0",
        serverUrl: ClientDefaults.DefaultServerUrl,
        lastSuccessfulUploadAt: DateTimeOffset.Parse("2026-05-24T00:00:00Z"),
        lastAttemptedUploadAt: DateTimeOffset.Parse("2026-05-24T00:01:00Z"),
        lastError: null,
        uploadQueueCount: 3,
        activityWatchState: "Available",
        keyStatsState: "Unavailable",
        statusDetails: new
        {
            keyStatsDetailState = "ApiOkButStaleZero",
            keyStatsProcessCount = 2,
            keyStatsSkipReason = "stale-zero",
            awQueueCount = 3
        });

    Assert.Equal("Available", heartbeat.ActivityWatchState);
    Assert.Equal("Unavailable", heartbeat.KeyStatsState);
    Assert.Equal(3, heartbeat.UploadQueueCount);

    using var status = JsonDocument.Parse(heartbeat.StatusJson);
    Assert.Equal("ApiOkButStaleZero", status.RootElement.GetProperty("keyStatsDetailState").GetString());
    Assert.Equal(2, status.RootElement.GetProperty("keyStatsProcessCount").GetInt32());
    Assert.Equal("stale-zero", status.RootElement.GetProperty("keyStatsSkipReason").GetString());
}
```

Update existing deserialize test to pass explicit states and assert those values instead of Unknown.

- [ ] **Step 2: Change BuildHeartbeat signature**

```csharp
public static DaemonHeartbeatRequest BuildHeartbeat(
    string deviceId,
    string version,
    string serverUrl,
    DateTimeOffset? lastSuccessfulUploadAt,
    DateTimeOffset? lastAttemptedUploadAt,
    string? lastError,
    int? uploadQueueCount = null,
    string activityWatchState = "Unknown",
    string keyStatsState = "Unknown",
    object? statusDetails = null)
{
    var normalizedServerUrl = ApiClient.NormalizeServerUrl(
        string.IsNullOrWhiteSpace(serverUrl)
            ? ClientDefaults.DefaultServerUrl
            : serverUrl);

    var statusPayload = new Dictionary<string, object?>
    {
        ["machine"] = Environment.MachineName,
        ["process"] = "pim-windows-daemon"
    };

    if (statusDetails is not null)
    {
        foreach (var prop in statusDetails.GetType().GetProperties())
        {
            statusPayload[prop.Name] = prop.GetValue(statusDetails);
        }
    }

    return new DaemonHeartbeatRequest(
        deviceId,
        "windows",
        string.IsNullOrWhiteSpace(version)
            ? Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? "unknown"
            : version,
        normalizedServerUrl,
        lastSuccessfulUploadAt,
        lastAttemptedUploadAt,
        lastError,
        uploadQueueCount,
        activityWatchState,
        keyStatsState,
        false,
        JsonSerializer.Serialize(statusPayload));
}
```

- [ ] **Step 3: Wire App heartbeat loop with real collector facts**

In `App.ReportHeartbeatOnceAsync`:

```csharp
var aw = Services.GetRequiredService<AwCollectorService>();
var ks = Services.GetRequiredService<KeyStatsCollectorService>();
var awState = aw.LastUploadError is null && aw.LastUploadTime is not null
    ? "Available"
    : aw.LastUploadError is null ? "Unknown" : "Unavailable";
// Better: probe AW health lightly or use last known. Minimal acceptable:
// if queue/error suggests AW path alive use Available when no hard failure.

var ksHealth = ks.LastHealth;
var ksState = ksHealth?.DaemonSourceState ?? "Unknown";

var lastSuccess = MaxTime(aw.LastUploadTime, ks.LastUploadTime);
var lastError = aw.LastUploadError ?? ks.LastUploadError;

var heartbeat = DaemonHeartbeatReporter.BuildHeartbeat(
    Environment.MachineName,
    version,
    config.ServerUrl,
    lastSuccess is DateTime dt ? new DateTimeOffset(dt) : null,
    DateTimeOffset.UtcNow,
    lastError,
    aw.QueueCount,
    awState,
    ksState,
    new
    {
        keyStatsDetailState = ksHealth?.DetailState.ToString(),
        keyStatsProcessCount = ksHealth?.ProcessCount,
        keyStatsSkipReason = ks.LastSkipReason,
        awQueueCount = aw.QueueCount
    });
```

Also replace `EnsureKeyStatsRunning` body to use `KeyStatsProcessManager`:

```csharp
var manager = Services.GetRequiredService<KeyStatsProcessManager>();
var exe = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsProcessManager.ExeFileName);
manager.EnsureRunning(exe, Process.GetCurrentProcess().SessionId);
```

Remove schtasks-only launch as primary path.

- [ ] **Step 4: Pass tests and commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~DaemonHeartbeatReporterTests"
git add src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs `
  src/client-windows/Pim.Client.App/App.xaml.cs `
  tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs
git commit -m "feat(windows): report real AW and KeyStats heartbeat states"
```

---

### Task 5: Demote Companion Shell; Restore Tray Daemon Primary Path

**Files:**
- Modify: `src/client-windows/Pim.Client.App/App.xaml.cs`
- Modify: `src/client-windows/Pim.Client.App/TrayIcon.cs`
- Modify: `tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs`
- Create: `tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs`

- [ ] **Step 1: Rewrite shell/path tests**

Replace `WindowsCompanionShellTests` assertions so they no longer require shell routes in tray/startup.

```csharp
public class WindowsCompanionShellTests
{
    [Fact]
    public void CompanionShellCodeRemainsAvailableButIsNotPrimaryPath()
    {
        var projectFile = File.ReadAllText(RepoPath("src", "client-windows", "Pim.Client.App", "Pim.Client.App.csproj"));
        var appStartup = File.ReadAllText(RepoPath("src", "client-windows", "Pim.Client.App", "App.xaml.cs"));
        var trayCode = File.ReadAllText(RepoPath("src", "client-windows", "Pim.Client.App", "TrayIcon.cs"));
        var hostCode = File.ReadAllText(RepoPath("src", "client-windows", "Pim.Client.App", "EmbeddedWebViewHost.cs"));

        Assert.Contains("WebView2", projectFile);
        Assert.Contains("EmbeddedWebViewHost", hostCode);
        Assert.Contains("MainShellWindow", File.ReadAllText(RepoPath("src", "client-windows", "Pim.Client.App", "MainShellWindow.xaml.cs")));

        Assert.DoesNotContain("ShowMainShellWindow();", appStartup.Replace(" ", string.Empty));
        Assert.DoesNotContain("OpenShell(\"/today\")", trayCode);
        Assert.Contains("ShowStatusWindow", trayCode);
        Assert.Contains("在浏览器打开 Web 工作台", trayCode);
    }
}
```

Add `WindowsDaemonPrimaryPathTests`:

```csharp
[Fact]
public void TrayMenu_IsDaemonFocused()
{
    var trayCode = File.ReadAllText(...TrayIcon.cs);
    foreach (var banned in new[] { "任务 / 日历", "报告中心", "Outlook 同步", "Data Center", "审计中心", "通知中心" })
    {
        Assert.DoesNotContain(banned, trayCode);
    }

    Assert.Contains("打开状态中心", trayCode);
    Assert.Contains("立即同步", trayCode);
    Assert.Contains("回填最近 14 天 ActivityWatch", trayCode);
    Assert.Contains("在浏览器打开 Web 工作台", trayCode);
}
```

- [ ] **Step 2: Run fail, then modify App/Tray**

`App.xaml.cs`:
- Delete/disable `ShowMainShellWindow();` during startup.
- Keep `ShowMainShellWindow` method for optional future use.

`TrayIcon.cs` menu:

```csharp
_notifyIcon.ContextMenuStrip.Items.Add("打开状态中心", null, (_, _) => ShowStatusWindow());
_notifyIcon.ContextMenuStrip.Items.Add(new ToolStripSeparator());
_notifyIcon.ContextMenuStrip.Items.Add("立即同步", null, async (_, _) => await TriggerSyncAsync());
_notifyIcon.ContextMenuStrip.Items.Add("回填最近 14 天 ActivityWatch", null, async (_, _) => await TriggerAwBackfillAsync());
_notifyIcon.ContextMenuStrip.Items.Add("在浏览器打开 Web 工作台", null, (_, _) => OpenWebWorkbench());
_notifyIcon.ContextMenuStrip.Items.Add(new ToolStripSeparator());
_notifyIcon.ContextMenuStrip.Items.Add("登录...", null, (_, _) => ShowLogin());
_notifyIcon.ContextMenuStrip.Items.Add("退出", null, (_, _) => ConfirmAndExit());

_notifyIcon.DoubleClick += (_, _) => ShowStatusWindow();
```

Implement `OpenWebWorkbench`:

```csharp
private static void OpenWebWorkbench()
{
    var api = App.Services.GetRequiredService<ApiClient>();
    var root = api.CurrentBaseUrl.TrimEnd('/');
    if (root.EndsWith("/api/v1", StringComparison.OrdinalIgnoreCase))
        root = root[..^"/api/v1".Length];
    var url = string.IsNullOrWhiteSpace(root) ? ClientDefaults.DefaultServerUrl.TrimEnd('/') + "/today" : root + "/today";
    Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
}
```

- [ ] **Step 3: Pass tests and commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsCompanionShellTests|FullyQualifiedName~WindowsDaemonPrimaryPathTests"
git add src/client-windows/Pim.Client.App/App.xaml.cs `
  src/client-windows/Pim.Client.App/TrayIcon.cs `
  tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs `
  tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs
git commit -m "refactor(windows): restore tray status center as primary path"
```

---

### Task 6: Status Center UI (4 Sections)

**Files:**
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs` (source-level presence tests + pure summary helper if extracted)

- [ ] **Step 1: Write source/contract tests**

```csharp
[Fact]
public void StatusWindow_DeclaresFourSectionsAndKeyStatsActions()
{
    var xaml = File.ReadAllText(...StatusWindow.xaml);
    var code = File.ReadAllText(...StatusWindow.xaml.cs);

    Assert.Contains("概览", xaml);
    Assert.Contains("数据源", xaml);
    Assert.Contains("上传", xaml);
    Assert.Contains("设置", xaml);
    Assert.Contains("重启 KeyStats", xaml);
    Assert.Contains("复制诊断", xaml);
    Assert.Contains("在浏览器打开 Web", xaml);
    Assert.Contains("KeyStatsProcessManager", code);
}
```

If extracting overview rating helper, unit-test:

```csharp
Assert.Equal("正常", StatusCenterEvaluator.Rate(... all available ...));
Assert.Equal("部分异常", StatusCenterEvaluator.Rate(... stale zero ...));
Assert.Equal("不可用", StatusCenterEvaluator.Rate(... missing process + unauthenticated ...));
```

- [ ] **Step 2: Redesign XAML**

Use a `TabControl` with 4 tabs or a single scroll page with 4 section headers. Required controls:

**Overview**
- Account text
- API connectivity text
- Overall health text (`正常` / `部分异常` / `不可用`)
- Manual sync button

**Sources**
- AW summary + detail
- KeyStats summary + detail (process count, sessions, counters, growth)
- Buttons: `重启 KeyStats`, `打开安装目录`, `复制诊断`

**Upload**
- AW queue
- KS last upload / skip reason
- last errors

**Settings**
- Server URL + save
- Auto-start checkbox
- Open logs
- Open web in browser
- Login button

Keep title: `PIM 状态中心` (or keep `PIM 守护程序状态` if less churn is preferred; prefer `PIM 状态中心`).

- [ ] **Step 3: Implement code-behind refresh**

Refresh loop/on-load:

1. Account diagnostic
2. Probe PIM API `/health`
3. Probe AW buckets endpoint
4. Read `KeyStatsCollectorService.LastHealth` or run live process list + `/api/stats/`
5. Build section view models / text blocks
6. Wire actions:
   - Restart => `KeyStatsProcessManager.Restart(exe, sessionId)` then re-probe
   - Open install dir => `Process.Start("explorer.exe", dir)`
   - Copy diagnostics => `Clipboard.SetText(report)`

Do not open MainShell from status center primary buttons. Browser only.

- [ ] **Step 4: Pass tests, build app, commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsStatusCenterTests|FullyQualifiedName~ClientWindows"
dotnet build src/client-windows/Pim.Client.Windows.slnx
git add src/client-windows/Pim.Client.App/StatusWindow.xaml `
  src/client-windows/Pim.Client.App/StatusWindow.xaml.cs `
  tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs
git commit -m "feat(windows): upgrade status window into four-section status center"
```

---

### Task 7: Local KeyStats Fix Validation (And Optional keyStats Source Patch)

**Files:**
- Possibly external repo `https://github.com/2746267826/keyStats`
- Possibly `.github/workflows/build-windows.yml` if KeyStats packaging needs health-aware binary

- [ ] **Step 1: Publish local daemon**

```powershell
dotnet publish src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Release -o publish/PimDaemon -r win-x64 --self-contained true
```

- [ ] **Step 2: Stop existing KeyStats dual instances and run new daemon**

```powershell
Get-Process KeyStats -ErrorAction SilentlyContinue | Stop-Process -Force
# install/run from publish output or C:\ProgramLocal\PIM after copying
```

- [ ] **Step 3: Verify process convergence**

```powershell
Get-Process KeyStats | ForEach-Object { [PSCustomObject]@{Id=$_.Id; SessionId=$_.SessionId} }
```

Expected: only current interactive session (usually Session 1), count = 1.

- [ ] **Step 4: Verify counters grow after real keyboard/mouse activity**

```powershell
Invoke-RestMethod http://127.0.0.1:18080/api/stats/ | ConvertTo-Json -Depth 4
# type / click for 30-60s
Invoke-RestMethod http://127.0.0.1:18080/api/stats/ | ConvertTo-Json -Depth 4
```

Expected: `keyPresses` or clicks increase.

- [ ] **Step 5: If still stuck at zero, patch keyStats source**

Clone and patch `https://github.com/2746267826/keyStats`:

Minimum patches:
1. Named mutex single-instance
2. Surface hook install failure in logs/API
3. Optional API fields: `hookActive`, `sessionId`, `lastInputAt`

Then rebuild KeyStats, copy `KeyStats.exe` + deps beside daemon, retest Step 3–4.

- [ ] **Step 6: Verify upload gate**

While stale zero: daemon logs contain `Skip upload: stale-zero` and no new sample rows.
After growth: samples upload succeeds.

- [ ] **Step 7: Commit any PIM-side packaging/docs notes if needed**

```powershell
git add ...
git commit -m "fix(windows): harden KeyStats launch and local validation notes"
```

If keyStats source changed, commit/PR in that repository separately and record SHA in PIM PR description.

---

### Task 8: Full Verification And PR

**Files:** none required beyond accumulated changes

- [ ] **Step 1: Run focused + broad tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ClientWindows"
dotnet test Pim.sln
```

Expected: PASS (or document unrelated failures with evidence).

- [ ] **Step 2: Publish daemon**

```powershell
dotnet publish src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Release -o publish/PimDaemon -r win-x64 --self-contained true
```

- [ ] **Step 3: Manual acceptance checklist**

1. Start daemon → tray only, no MainShell popup
2. Double-click tray → status center
3. Four sections visible and Chinese labels correct
4. KeyStats single user-session process
5. After input, stats grow and health becomes Available
6. Stale zero does not upload
7. Browser open Web works
8. Heartbeat reports non-Unknown states when probes have data

- [ ] **Step 4: Push and open PR**

```powershell
git status --short --branch
git push -u origin codex/windows-status-center-keystats
gh pr create --title "feat(windows): status center + KeyStats reliability" --body "$(@'
## Summary
- Restore tray + status center as Windows daemon primary path
- Keep WebView2 shell code but remove it from startup/tray primary entry
- Add KeyStats health probe, user-session process convergence, skip stale-zero uploads
- Report real AW/KeyStats states in daemon heartbeat

## Spec
- docs/superpowers/specs/2026-07-12-windows-status-center-keystats-design.md

## Test plan
- [x] ClientWindows unit tests
- [x] local KeyStats process/session check
- [x] local /api/stats growth after input
- [x] no MainShell on startup
- [ ] GitHub Actions
'@)"
```

- [ ] **Step 5: Wait for GitHub Actions and fix failures**

Do not claim complete until checks pass or path filters prove no workflow ran.

---

## Spec Coverage Self-Review

| Spec requirement | Task |
| --- | --- |
| B+D product shape | Task 5, 6 |
| Keep WebView2 code, not primary | Task 5 |
| Four-section status center | Task 6 |
| KeyStats dual-session / single instance | Task 2, 7 |
| Health grades Missing/Unreachable/StaleZero/Available | Task 1 |
| Skip full-zero upload | Task 3 |
| Real heartbeat states | Task 4 |
| Optional keyStats source fix | Task 7 |
| Tests + publish + PR | Task 8 |
| Branch/worktree from master | Task 0 |

## Placeholder Scan

No TBD/TODO left. Observation window is explicit: first non-zero activity counts as Available; otherwise compare with previous collector snapshot (minute cadence). Service enum remains coarse (`Available`/`Unavailable`/`Unknown`); detail stays in `statusJson`.
