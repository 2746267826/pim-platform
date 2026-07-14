# KeyStats 一键修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Windows 状态中心数据源页为 KeyStats 增加中文修复建议与「一键修复」：普通权限收敛进程，失败时经确认仅提权独立脚本清理 Session 0，再两阶段复检并展示结果。

**Architecture:** 纯逻辑放在 `Pim.Client.Core`：`KeyStatsFixAdvisor`（建议文案）、`KeyStatsProcessManager` 扩展（可报告 stop 成败）、`KeyStatsOneClickFixService`（编排 + 可注入 elevate/probe 缝）。`StatusWindow` 只负责 UI 与调用。提权脚本 `scripts/fix-keystats-session.ps1` 只做强制清理，启动回客户端普通权限。

**Tech Stack:** .NET 8、WPF（App）、xUnit、PowerShell、`HttpClient` 本机 `http://127.0.0.1:18080/api/stats/`

**Spec:** `docs/superpowers/specs/2026-07-14-keystats-one-click-fix-design.md`  
**Branch:** `codex/keystats-one-click-fix`

**并行约束（强制）：** 实现与审查阶段**同一时刻至少开 4 个子代理**并行工作。推荐分组：

| 波次 | 并行子代理（≥4） | 任务 |
|------|------------------|------|
| Wave A | 4 个 implementer | Task 1 ProcessManager stop 结果；Task 2 Advisor；Task 3 LocalStats；Task 5 修复脚本（互不依赖） |
| Wave B | 1 implementer + 3 reviewer/tester | Task 4 OneClickFixService 实现；另 3 个分别审查 Wave A 的 stop/advisor/script |
| Wave C | 2 implementer + 2 reviewer | Task 6 UI + Task 7 打包；2 个审查 Task 4/5 与 UI 契约 |
| Wave D | 2 tester + 1 docs + 1 PR/CI watcher | Task 8 回归测试、运维文档、push/PR、等 Actions |

禁止单线程串行做完整计划；仅 Task 内强依赖步骤（红→绿→commit）可在同一子代理内顺序执行。

---

## File map

| Path | Responsibility |
|------|----------------|
| Create: `src/client-windows/Pim.Client.Core/Models/KeyStatsFixModels.cs` | `KeyStatsStopResult`, `KeyStatsFixOutcome`, `KeyStatsFixResult`, `KeyStatsFixSuggestion` |
| Modify: `src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs` | `TryStop` 返回结果；`StopProcesses` / `StartInCurrentSession` 可复用；`EnsureRunning`/`Restart` 聚合 stop 结果（Restart 对外行为保持「尽力重启」） |
| Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsFixAdvisor.cs` | 健康状态 → 中文建议 |
| Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsLocalStatsClient.cs` | 只读 GET `/api/stats/` → `KeyStatsCounterSnapshot`（复检用，不上传） |
| Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsOneClickFixService.cs` | 一键修复编排 |
| Create: `scripts/fix-keystats-session.ps1` | 提权清理脚本 |
| Modify: `src/client-windows/Pim.Client.App/Startup.cs` | 注册 `KeyStatsOneClickFixService`（若用 DI） |
| Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml` | 建议区、一键修复按钮、结果区 |
| Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs` | `OnOneClickFixKeyStats`、刷新建议文案 |
| Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj` | 发布时复制 ps1 |
| Modify: `build-daemon.ps1` | publish 后拷贝 ps1 到 `PimDaemon/` |
| Modify: `.github/workflows/build-windows.yml` | CI publish 后拷贝 ps1（**仅因打包必需**） |
| Modify: `docs/operations/windows-keystats-session-fix.md` | 指向一键修复 |
| Create: `tests/Pim.UnitTests/ClientWindows/KeyStatsFixAdvisorTests.cs` | 文案映射 |
| Create: `tests/Pim.UnitTests/ClientWindows/KeyStatsOneClickFixServiceTests.cs` | 编排/提权决策 |
| Modify: `tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs` | stop 结果聚合 |
| Modify: `tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs` | UI 契约 |

---

### Task 1: Stop 结果模型 + ProcessManager 可报告 kill 成败

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Models/KeyStatsFixModels.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs`
- Modify: `tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs`

- [ ] **Step 1: 写失败测试 — stop 结果与 NeedsElevation 纯逻辑**

在 `KeyStatsProcessManagerTests.cs` 追加：

```csharp
[Fact]
public void StopResult_NeedsElevation_WhenAnyAccessDenied()
{
    var results = new[]
    {
        new KeyStatsStopResult(10, Succeeded: true, Error: null),
        new KeyStatsStopResult(20, Succeeded: false, Error: "access-denied")
    };

    Assert.True(KeyStatsProcessManager.NeedsElevation(results));
    Assert.Equal(new[] { 20 }, KeyStatsProcessManager.FailedStopIds(results));
}

[Fact]
public void StopResult_DoesNotNeedElevation_WhenAllSucceeded()
{
    var results = new[]
    {
        new KeyStatsStopResult(10, Succeeded: true, Error: null)
    };

    Assert.False(KeyStatsProcessManager.NeedsElevation(results));
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsProcessManagerTests"
```

Expected: FAIL（类型/方法不存在）

- [ ] **Step 3: 实现模型与 ProcessManager 扩展**

`KeyStatsFixModels.cs`（本 Task 至少包含 stop 相关；后续 Task 可同文件追加 Fix 类型）：

```csharp
namespace Pim.Client.Core.Models;

public sealed record KeyStatsStopResult(
    int ProcessId,
    bool Succeeded,
    string? Error);
```

在 `KeyStatsProcessManager` 中：

```csharp
public static bool NeedsElevation(IReadOnlyList<KeyStatsStopResult> stopResults)
    => stopResults.Any(r => !r.Succeeded && string.Equals(r.Error, "access-denied", StringComparison.OrdinalIgnoreCase));

public static IReadOnlyList<int> FailedStopIds(IReadOnlyList<KeyStatsStopResult> stopResults)
    => stopResults.Where(r => !r.Succeeded).Select(r => r.ProcessId).ToArray();

public KeyStatsStopResult TryStop(int processId)
{
    try
    {
        using var process = Process.GetProcessById(processId);
        process.Kill(entireProcessTree: true);
        if (!process.WaitForExit(3000) && !process.HasExited)
            return new KeyStatsStopResult(processId, false, "timeout");
        return new KeyStatsStopResult(processId, true, null);
    }
    catch (ArgumentException)
    {
        // already exited
        return new KeyStatsStopResult(processId, true, null);
    }
    catch (System.ComponentModel.Win32Exception)
    {
        return new KeyStatsStopResult(processId, false, "access-denied");
    }
    catch (UnauthorizedAccessException)
    {
        return new KeyStatsStopResult(processId, false, "access-denied");
    }
    catch (Exception ex)
    {
        return new KeyStatsStopResult(processId, false, ex.GetType().Name);
    }
}

public IReadOnlyList<KeyStatsStopResult> StopProcesses(IEnumerable<int> processIds)
    => processIds.Distinct().Select(TryStop).ToArray();

public void StartInCurrentSession(string keyStatsExePath)
{
    // move existing private StartInCurrentSession body here as public
}
```

更新 `EnsureRunning` / `Restart` 使用 `StopProcesses`（忽略失败细节亦可，保持尽力行为）。**不要**改变 `Restart` 的对外语义（仍尽力杀+启）。

- [ ] **Step 4: 运行测试确认通过**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsProcessManagerTests|FullyQualifiedName~KeyStatsHealthProbeTests"
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add src/client-windows/Pim.Client.Core/Models/KeyStatsFixModels.cs src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs
git commit -m "feat: report KeyStats process stop failures for elevate decisions"
```

---

### Task 2: KeyStatsFixAdvisor 中文建议映射

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsFixAdvisor.cs`
- Modify: `src/client-windows/Pim.Client.Core/Models/KeyStatsFixModels.cs`（追加 `KeyStatsFixSuggestion`）
- Create: `tests/Pim.UnitTests/ClientWindows/KeyStatsFixAdvisorTests.cs`

- [ ] **Step 1: 写失败测试**

```csharp
using Pim.Client.Core.Models;
using Pim.Client.Core.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

public class KeyStatsFixAdvisorTests
{
    [Fact]
    public void BuildSuggestion_StaleZeroWithForeign_MentionsSessionAndOneClick()
    {
        var health = new KeyStatsHealthResult(
            KeyStatsDetailState.ApiOkButStaleZero,
            "Unavailable",
            CanUpload: false,
            SkipReason: "stale-zero",
            ProcessCount: 2,
            HasForeignSessionProcess: true,
            Snapshot: null,
            SummaryZh: "x");

        var s = KeyStatsFixAdvisor.BuildSuggestion(health);
        Assert.Contains("Session", s.MessageZh);
        Assert.Contains("一键修复", s.MessageZh);
        Assert.True(s.ShowActionHint);
    }

    [Fact]
    public void BuildSuggestion_Healthy_SaysNormal()
    {
        var health = new KeyStatsHealthResult(
            KeyStatsDetailState.Available,
            "Available",
            CanUpload: true,
            SkipReason: null,
            ProcessCount: 1,
            HasForeignSessionProcess: false,
            Snapshot: null,
            SummaryZh: "KeyStats 可用");

        var s = KeyStatsFixAdvisor.BuildSuggestion(health);
        Assert.Contains("运行正常", s.MessageZh);
        Assert.False(s.ShowActionHint);
    }

    [Theory]
    [InlineData(KeyStatsDetailState.MissingProcess, "missing-process", false, "未运行")]
    [InlineData(KeyStatsDetailState.ApiUnreachable, "api-unreachable", false, "不可达")]
    [InlineData(KeyStatsDetailState.ApiOkButStaleZero, "stale-zero", false, "计数")]
    public void BuildSuggestion_CoversCommonSkipReasons(
        KeyStatsDetailState state, string skip, bool foreign, string needle)
    {
        var health = new KeyStatsHealthResult(
            state, "Unavailable", false, skip, 1, foreign, null, "x");
        var s = KeyStatsFixAdvisor.BuildSuggestion(health);
        Assert.Contains(needle, s.MessageZh);
        Assert.True(s.ShowActionHint);
    }

    [Fact]
    public void BuildSuggestion_AvailableWithForeign_SuggestsConverge()
    {
        var health = new KeyStatsHealthResult(
            KeyStatsDetailState.Available, "Available", true, null, 2, true, null, "x");
        var s = KeyStatsFixAdvisor.BuildSuggestion(health);
        Assert.Contains("额外会话", s.MessageZh);
        Assert.True(s.ShowActionHint);
    }

    [Fact]
    public void BuildSuggestion_NullHealth_SafeDefault()
    {
        var s = KeyStatsFixAdvisor.BuildSuggestion(null);
        Assert.False(string.IsNullOrWhiteSpace(s.MessageZh));
    }
}
```

- [ ] **Step 2: 运行确认失败**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsFixAdvisorTests"
```

- [ ] **Step 3: 实现 Advisor**

```csharp
namespace Pim.Client.Core.Models;

public sealed record KeyStatsFixSuggestion(bool ShowActionHint, string MessageZh);
```

```csharp
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public static class KeyStatsFixAdvisor
{
    public static KeyStatsFixSuggestion BuildSuggestion(KeyStatsHealthResult? health)
    {
        if (health is null)
            return new KeyStatsFixSuggestion(true, "尚无 KeyStats 健康探测结果。可尝试「一键修复」启动并复检。");

        if (health.DetailState == KeyStatsDetailState.Available && !health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(false, "运行正常，无需修复。");

        if (health.DetailState == KeyStatsDetailState.Available && health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(true,
                "KeyStats 可用，但存在额外会话实例。建议使用「一键修复」收敛为当前会话单实例。");

        if (string.Equals(health.SkipReason, "stale-zero", StringComparison.OrdinalIgnoreCase)
            && health.HasForeignSessionProcess)
            return new KeyStatsFixSuggestion(true,
                "检测到非当前会话（常为 Session 0）实例可能占用本地 API。建议使用「一键修复」：结束非当前会话实例 → 在当前会话重启 KeyStats → 自动复检。");

        if (string.Equals(health.SkipReason, "stale-zero", StringComparison.OrdinalIgnoreCase))
            return new KeyStatsFixSuggestion(true,
                "API 可达但计数全 0 或不增长。建议「一键修复」重启后，操作键鼠再刷新；若仍为 0，请复制诊断。");

        if (string.Equals(health.SkipReason, "missing-process", StringComparison.OrdinalIgnoreCase)
            || health.DetailState == KeyStatsDetailState.MissingProcess)
            return new KeyStatsFixSuggestion(true,
                "KeyStats 进程未运行。一键修复将在当前会话启动 KeyStats。");

        if (string.Equals(health.SkipReason, "api-unreachable", StringComparison.OrdinalIgnoreCase)
            || health.DetailState == KeyStatsDetailState.ApiUnreachable)
            return new KeyStatsFixSuggestion(true,
                "KeyStats API 不可达。一键修复将收敛进程并重启；若仍失败，请复制诊断。");

        return new KeyStatsFixSuggestion(true,
            $"{health.SummaryZh} 可尝试「一键修复」。");
    }
}
```

- [ ] **Step 4: 测试通过并 commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsFixAdvisorTests"
git add src/client-windows/Pim.Client.Core tests/Pim.UnitTests/ClientWindows/KeyStatsFixAdvisorTests.cs
git commit -m "feat: add KeyStats fix advisor Chinese suggestions"
```

---

### Task 3: 本地 Stats 只读客户端 + FixResult 模型

**Files:**
- Modify: `src/client-windows/Pim.Client.Core/Models/KeyStatsFixModels.cs`
- Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsLocalStatsClient.cs`
- Create: `tests/Pim.UnitTests/ClientWindows/KeyStatsLocalStatsClientTests.cs`（仅测映射/增长谓词辅助，不依赖真实端口）

- [ ] **Step 1: 追加模型**

```csharp
public enum KeyStatsFixOutcome
{
    Succeeded,
    Partial,
    Failed,
    Cancelled
}

public sealed record KeyStatsFixResult(
    KeyStatsFixOutcome Outcome,
    string Phase1MessageZh,
    string Phase2MessageZh,
    IReadOnlyList<int> StoppedProcessIds,
    IReadOnlyList<int> FailedStopProcessIds,
    bool ElevatedUsed,
    int? ScriptExitCode,
    string? ScriptOutputExcerpt,
    bool ApiReachable,
    bool CountersGrew);
```

- [ ] **Step 2: 实现 LocalStatsClient**

```csharp
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Pim.Client.Core.Models;

namespace Pim.Client.Core.Services;

public sealed class KeyStatsLocalStatsClient : IDisposable
{
    public static string ResolveBaseUrl()
        => Environment.GetEnvironmentVariable("KEYSTATS_BASE_URL") ?? "http://127.0.0.1:18080";

    private readonly HttpClient _http;

    public KeyStatsLocalStatsClient(HttpClient? http = null)
    {
        _http = http ?? new HttpClient
        {
            BaseAddress = new Uri(ResolveBaseUrl()),
            Timeout = TimeSpan.FromSeconds(5)
        };
    }

    public async Task<(KeyStatsCounterSnapshot? Snapshot, string? Error)> GetSnapshotAsync(
        CancellationToken ct = default)
    {
        try
        {
            var dto = await _http.GetFromJsonAsync<StatsDto>("/api/stats/", ct);
            if (dto is null) return (null, "empty snapshot");
            return (new KeyStatsCounterSnapshot(
                dto.KeyPresses, dto.LeftClicks, dto.RightClicks, dto.MiddleClicks,
                dto.SideBackClicks, dto.SideForwardClicks, dto.MouseDistance, dto.ScrollDistance), null);
        }
        catch (Exception ex)
        {
            return (null, ex.Message);
        }
    }

    public static bool CountersIndicateRecovery(
        KeyStatsCounterSnapshot? before, KeyStatsCounterSnapshot? after)
    {
        if (after is null) return false;
        return after.HasAnyActivity || after.GrewFrom(before);
    }

    public void Dispose() => _http.Dispose();

    private sealed class StatsDto
    {
        [JsonPropertyName("keyPresses")] public int KeyPresses { get; set; }
        [JsonPropertyName("leftClicks")] public int LeftClicks { get; set; }
        [JsonPropertyName("rightClicks")] public int RightClicks { get; set; }
        [JsonPropertyName("middleClicks")] public int MiddleClicks { get; set; }
        [JsonPropertyName("sideBackClicks")] public int SideBackClicks { get; set; }
        [JsonPropertyName("sideForwardClicks")] public int SideForwardClicks { get; set; }
        [JsonPropertyName("mouseDistance")] public double MouseDistance { get; set; }
        [JsonPropertyName("scrollDistance")] public double ScrollDistance { get; set; }
    }
}
```

> JSON 属性名以 `KeyStatsCollectorService` 内 `KeyStatsSnapshot` 的实际 `[JsonPropertyName]` 为准；实现时对照 collector 私有 DTO，保持一致。

- [ ] **Step 3: 单测增长谓词**

```csharp
[Fact]
public void CountersIndicateRecovery_True_WhenGrew()
{
    var before = new KeyStatsCounterSnapshot(0,0,0,0,0,0,0,0);
    var after = before with { KeyPresses = 3 };
    Assert.True(KeyStatsLocalStatsClient.CountersIndicateRecovery(before, after));
}

[Fact]
public void CountersIndicateRecovery_False_WhenStillZero()
{
    var before = new KeyStatsCounterSnapshot(0,0,0,0,0,0,0,0);
    var after = new KeyStatsCounterSnapshot(0,0,0,0,0,0,0,0);
    Assert.False(KeyStatsLocalStatsClient.CountersIndicateRecovery(before, after));
}
```

- [ ] **Step 4: 测试通过并 commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsLocalStatsClientTests"
git add src/client-windows/Pim.Client.Core tests/Pim.UnitTests/ClientWindows/KeyStatsLocalStatsClientTests.cs
git commit -m "feat: add KeyStats local stats client for fix recheck"
```

---

### Task 4: KeyStatsOneClickFixService 编排（可注入缝）

**Files:**
- Create: `src/client-windows/Pim.Client.Core/Services/KeyStatsOneClickFixService.cs`
- Create: `tests/Pim.UnitTests/ClientWindows/KeyStatsOneClickFixServiceTests.cs`

设计缝（构造注入，单测不真 kill/UAC）：

```csharp
public sealed class KeyStatsOneClickFixService
{
    public const string FixScriptFileName = "fix-keystats-session.ps1";

    private readonly KeyStatsProcessManager _processes;
    private readonly KeyStatsLocalStatsClient _stats;
    private readonly Func<IReadOnlyList<int>, IReadOnlyList<KeyStatsStopResult>> _stop;
    private readonly Action<string> _start;
    private readonly Func<string, string, Task<(int ExitCode, string Output, bool Cancelled)>> _runElevatedScript;
    private readonly Func<Task> _delayPhase1;
    private readonly Func<Task> _delayPhase2;

    public KeyStatsOneClickFixService(
        KeyStatsProcessManager processes,
        KeyStatsLocalStatsClient stats,
        Func<IReadOnlyList<int>, IReadOnlyList<KeyStatsStopResult>>? stop = null,
        Action<string>? start = null,
        Func<string, string, Task<(int ExitCode, string Output, bool Cancelled)>>? runElevatedScript = null,
        Func<Task>? delayPhase1 = null,
        Func<Task>? delayPhase2 = null)
    {
        _processes = processes;
        _stats = stats;
        _stop = stop ?? (ids => processes.StopProcesses(ids));
        _start = start ?? processes.StartInCurrentSession;
        _runElevatedScript = runElevatedScript ?? DefaultRunElevatedAsync;
        _delayPhase1 = delayPhase1 ?? (() => Task.Delay(1500));
        _delayPhase2 = delayPhase2 ?? (() => Task.Delay(8000));
    }

    public async Task<KeyStatsFixResult> RunAsync(
        string keyStatsExePath,
        string fixScriptPath,
        int currentSessionId,
        Func<string, bool> confirmElevation,
        CancellationToken ct = default)
    {
        // 1) missing exe
        // 2) plan = BuildConvergencePlan(ListProcesses, session)
        // 3) stopResults = _stop(plan.ProcessIdsToStop)
        // 4) if ShouldStart or no current after stop -> _start(exe)
        // 5) re-list; if NeedsElevation(stopResults) OR still foreign:
        //      if !File.Exists(script) -> Failed
        //      if !confirmElevation(message) -> Cancelled
        //      elevated = await _runElevatedScript(script, exe)
        //      if Cancelled -> Cancelled
        //      if ExitCode != 0 -> Failed with excerpt
        //      _start(exe) again (user session)
        // 6) await phase1 delay; recheck processes + GetSnapshotAsync
        // 7) await phase2; before/after snapshots; CountersIndicateRecovery
        // 8) Outcome: Failed / Cancelled / Succeeded / Partial
    }
}
```

Elevate 默认实现（App 可用；Core 内用 Process + runas）：

```csharp
private static Task<(int ExitCode, string Output, bool Cancelled)> DefaultRunElevatedAsync(
    string scriptPath, string keyStatsExePath)
{
    // powershell -NoProfile -ExecutionPolicy Bypass -File script -KeyStatsExe path
    // Verb=runas, RedirectStandardOutput if possible; on Win32 cancel -> Cancelled=true
}
```

> 若 `runas` 难重定向 stdout：写临时 log 文件 `-LogPath`，脚本写文件，客户端读回。实现时二选一，优先 **脚本写 `%TEMP%\pim-keystats-fix-last.log`**，避免 runas 管道问题。

- [ ] **Step 1: 写编排单测（全 mock）**

```csharp
[Fact]
public async Task RunAsync_NoElevation_WhenStopsSucceedAndNoForeign()
{
    var mgr = new KeyStatsProcessManager();
    // inject stop success, start no-op, stats returns growing counters, delays no-op
    // assert Outcome Succeeded or Partial, ElevatedUsed false
}

[Fact]
public async Task RunAsync_RequestsElevation_WhenAccessDenied()
{
    // stop returns access-denied for foreign pid
    // confirmElevation returns true
    // runElevated returns 0
    // assert ElevatedUsed true
}

[Fact]
public async Task RunAsync_Cancelled_WhenUserRejectsUacPrompt()
{
    // confirmElevation false -> Cancelled
}

[Fact]
public async Task RunAsync_Partial_WhenApiOkButCountersStillZero()
{
    // phase2 both zero -> Partial, Phase2MessageZh contains 键盘
}
```

- [ ] **Step 2: 实现服务直到测试绿**

- [ ] **Step 3: Commit**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~KeyStatsOneClickFixServiceTests"
git add src/client-windows/Pim.Client.Core tests/Pim.UnitTests/ClientWindows/KeyStatsOneClickFixServiceTests.cs
git commit -m "feat: add KeyStats one-click fix orchestration service"
```

---

### Task 5: `fix-keystats-session.ps1`

**Files:**
- Create: `scripts/fix-keystats-session.ps1`

- [ ] **Step 1: 写脚本**

```powershell
#Requires -Version 5.1
param(
    [Parameter(Mandatory = $true)]
    [string]$KeyStatsExe,
    [string]$LogPath = $(Join-Path $env:TEMP "pim-keystats-fix-last.log")
)

$ErrorActionPreference = "Continue"
$lines = New-Object System.Collections.Generic.List[string]
function Log([string]$m) { $lines.Add(("[ {0} ] {1}" -f (Get-Date -Format o), $m)) }

Log "KeyStatsExe=$KeyStatsExe"
Log "Starting forced cleanup of KeyStats.exe"

& taskkill.exe /F /IM KeyStats.exe /T 2>&1 | ForEach-Object { Log $_ }

Start-Sleep -Milliseconds 500

$remaining = @(Get-Process -Name KeyStats -ErrorAction SilentlyContinue)
if ($remaining.Count -gt 0) {
    foreach ($p in $remaining) {
        Log ("REMAINING pid={0} session={1}" -f $p.Id, $p.SessionId)
    }
    Log "FAIL: KeyStats processes still present"
    $lines | Set-Content -Path $LogPath -Encoding UTF8
    exit 2
}

Log "OK: no KeyStats processes remain"
$lines | Set-Content -Path $LogPath -Encoding UTF8
# 故意不在此 Start-Process：由客户端普通权限在当前用户会话启动
exit 0
```

- [ ] **Step 2: 本地语法检查**

```powershell
powershell -NoProfile -Command "& { $null = [System.Management.Automation.Language.Parser]::ParseFile('scripts/fix-keystats-session.ps1', [ref]$null, [ref]$errs); if ($errs) { $errs; exit 1 } else { 'parse-ok' } }"
```

Expected: `parse-ok`

- [ ] **Step 3: Commit**

```powershell
git add scripts/fix-keystats-session.ps1
git commit -m "feat: add elevated KeyStats session cleanup script"
```

---

### Task 6: StatusWindow UI + 一键修复 handler

**Files:**
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`
- Modify: `src/client-windows/Pim.Client.App/Startup.cs`（注册服务，可选：也可 `new` 组装）
- Modify: `tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs`

- [ ] **Step 1: 扩展 UI 契约测试（先红）**

在 `StatusWindow_DeclaresFourSectionsAndKeyStatsActions` 追加：

```csharp
Assert.Contains("一键修复", xaml);
Assert.Contains("修复建议", xaml);
Assert.Contains("修复结果", xaml);
Assert.Contains("KeyStatsOneClickFixButton", xaml);
Assert.Contains("KeyStatsFixSuggestionText", xaml);
Assert.Contains("KeyStatsFixResultText", xaml);
Assert.Contains("OnOneClickFixKeyStats", code);
Assert.Contains("fix-keystats-session.ps1", code);
```

- [ ] **Step 2: 改 XAML（KeyStats 段）**

在 `KeyStatsDetailText` 后、按钮行前插入：

```xml
<TextBlock Text="修复建议"
           FontWeight="SemiBold"
           Foreground="{StaticResource PimTextBrush}"/>
<TextBlock x:Name="KeyStatsFixSuggestionText"
           Text="—"
           TextWrapping="Wrap"
           Margin="0,4,0,12"
           Foreground="{StaticResource PimMutedTextBrush}"/>
```

按钮行：

```xml
<StackPanel Orientation="Horizontal">
  <Button x:Name="KeyStatsOneClickFixButton"
          Content="一键修复"
          Style="{StaticResource PimPrimaryButton}"
          Margin="0,0,8,0"
          Click="OnOneClickFixKeyStats"/>
  <Button Content="重启 KeyStats"
          Style="{StaticResource PimSecondaryButton}"
          Margin="0,0,8,0"
          Click="OnRestartKeyStats"/>
  ...
</StackPanel>
```

按钮后：

```xml
<TextBlock Text="修复结果"
           FontWeight="SemiBold"
           Margin="0,16,0,0"
           Foreground="{StaticResource PimTextBrush}"/>
<TextBox x:Name="KeyStatsFixResultText"
         IsReadOnly="True"
         TextWrapping="Wrap"
         BorderThickness="0"
         Background="{StaticResource PimMutedSurfaceBrush}"
         Foreground="{StaticResource PimTextBrush}"
         FontFamily="Consolas"
         FontSize="11"
         Padding="8"
         Margin="0,4,0,0"
         MinHeight="48"
         Text="—"/>
```

- [ ] **Step 3: code-behind**

- 字段：`KeyStatsOneClickFixService _fixService`（DI 或 ctor 组装）
- `RefreshStatusAsync` 末尾：

```csharp
var suggestion = KeyStatsFixAdvisor.BuildSuggestion(health);
KeyStatsFixSuggestionText.Text = suggestion.MessageZh;
```

- Handler（对齐 `OnManualSync`）：

```csharp
private async void OnOneClickFixKeyStats(object sender, RoutedEventArgs e)
{
    KeyStatsOneClickFixButton.IsEnabled = false;
    KeyStatsOneClickFixButton.Content = "修复中...";
    KeyStatsFixResultText.Text = "修复中…";
    try
    {
        var exe = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsProcessManager.ExeFileName);
        var script = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, KeyStatsOneClickFixService.FixScriptFileName);
        var result = await _fixService.RunAsync(
            exe,
            script,
            Process.GetCurrentProcess().SessionId,
            confirmElevation: msg =>
                MessageBox.Show(msg, "PIM", MessageBoxButton.YesNo, MessageBoxImage.Warning) == MessageBoxResult.Yes);

        KeyStatsFixResultText.Text =
            $"阶段1：{result.Phase1MessageZh}\n阶段2：{result.Phase2MessageZh}";
        await RefreshStatusAsync();

        if (result.Outcome == KeyStatsFixOutcome.Failed)
            MessageBox.Show($"KeyStats 修复失败：\n{result.Phase1MessageZh}\n{result.Phase2MessageZh}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
        else if (result.Outcome == KeyStatsFixOutcome.Cancelled)
            MessageBox.Show("已取消管理员授权，未完成跨会话清理。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
        else if (result.Outcome == KeyStatsFixOutcome.Partial)
            MessageBox.Show("进程与 API 已处理，但计数仍为 0。请敲几下键盘后点「刷新」。", "PIM", MessageBoxButton.OK, MessageBoxImage.Warning);
        else
            MessageBox.Show("KeyStats 修复已完成。", "PIM", MessageBoxButton.OK, MessageBoxImage.Information);
    }
    catch (Exception ex)
    {
        KeyStatsFixResultText.Text = $"失败：{ex.Message}";
        MessageBox.Show($"KeyStats 修复失败：{ex.Message}", "PIM", MessageBoxButton.OK, MessageBoxImage.Error);
    }
    finally
    {
        KeyStatsOneClickFixButton.IsEnabled = true;
        KeyStatsOneClickFixButton.Content = "一键修复";
    }
}
```

确认框文案示例：

`普通权限无法结束部分 KeyStats 进程（可能位于 Session 0）。是否以管理员权限运行修复脚本？仅提升该脚本权限，不会提权整个 PIM 客户端。`

- [ ] **Step 4: 测试**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsStatusCenterTests|FullyQualifiedName~KeyStats"
```

- [ ] **Step 5: Commit**

```powershell
git add src/client-windows/Pim.Client.App tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs
git commit -m "feat: add KeyStats one-click fix UI in status center"
```

---

### Task 7: 打包发布脚本

**Files:**
- Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`
- Modify: `build-daemon.ps1`
- Modify: `.github/workflows/build-windows.yml`（**仅复制一行脚本到 publish 根；不改触发条件/权限**）

- [ ] **Step 1: csproj Content**

```xml
<ItemGroup>
  <Content Include="..\..\..\scripts\fix-keystats-session.ps1"
           Link="fix-keystats-session.ps1"
           CopyToOutputDirectory="PreserveNewest"
           CopyToPublishDirectory="PreserveNewest" />
</ItemGroup>
```

- [ ] **Step 2: build-daemon.ps1** 在 publish 成功、打 zip 前：

```powershell
$fixScript = Join-Path $projectDir "scripts\fix-keystats-session.ps1"
if (Test-Path $fixScript) {
    Copy-Item -LiteralPath $fixScript -Destination (Join-Path $daemonDir "fix-keystats-session.ps1") -Force
} else {
    Write-Host "WARNING: fix-keystats-session.ps1 missing" -ForegroundColor Yellow
}
```

- [ ] **Step 3: build-windows.yml** 在 KeyStats 拷贝步骤后增加：

```yaml
- name: Include KeyStats session fix script
  shell: pwsh
  run: |
    Copy-Item -LiteralPath "${{ github.workspace }}/scripts/fix-keystats-session.ps1" `
      -Destination "src/client-windows/publish/fix-keystats-session.ps1" -Force
```

（路径以 workflow 当前 `working-directory` / publish 目录为准，实现时对照现有 copy KeyStats 步骤。）

- [ ] **Step 4: Commit**

```powershell
git add src/client-windows/Pim.Client.App/Pim.Client.App.csproj build-daemon.ps1 .github/workflows/build-windows.yml
git commit -m "chore: ship KeyStats fix script with Windows client publish"
```

---

### Task 8: 运维文档 + 全量回归

**Files:**
- Modify: `docs/operations/windows-keystats-session-fix.md`

- [ ] **Step 1: 文档增加「状态中心一键修复」为首选路径**

在 Operator recovery 前插入：

```markdown
## Preferred recovery (Status Center)

1. Open **PIM 状态中心 → 数据源**
2. Read **修复建议**
3. Click **一键修复**
4. If prompted, approve UAC for `fix-keystats-session.ps1` only
5. Wait for phase-1/phase-2 result; if counters still 0, type a few keys and click **刷新**

Script path (install dir): `fix-keystats-session.ps1` next to `KeyStats.exe` and `Pim.Client.App.exe`.
```

保留原手工 PowerShell 作为后备。

- [ ] **Step 2: 全量 ClientWindows 测试**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ClientWindows"
```

Expected: PASS

- [ ] **Step 3: Commit + push + PR**

```powershell
git add docs/operations/windows-keystats-session-fix.md
git commit -m "docs: point KeyStats session recovery to one-click fix"
git push -u origin codex/keystats-one-click-fix
gh pr create --title "feat: KeyStats one-click fix in Windows status center" --body "..."
```

- [ ] **Step 4: 等 GitHub Actions（含 build-windows）通过后再称完成**

---

## 手动验收清单

1. 复现 Session 0 + Session 1 stale-zero  
2. 打开状态中心：见修复建议含 Session / 一键修复  
3. 点一键修复：普通权限可清则无 UAC；不可清则确认后 UAC 仅脚本  
4. 结果区两阶段文案正确；取消 UAC 有中文说明  
5. 「重启 KeyStats」仍可用且不弹 UAC 编排  
6. 安装目录存在 `fix-keystats-session.ps1`

---

## Spec coverage self-check

| Spec requirement | Task |
|------------------|------|
| 中文修复建议映射 | Task 2 |
| 一键修复按钮 + 结果区 | Task 6 |
| 普通权限收敛 + stop 失败可见 | Task 1 + 4 |
| 确认后仅脚本提权 | Task 4 + 5 |
| 脚本只清理不启动 | Task 5 |
| 客户端脚本后普通权限启动 | Task 4 |
| 两阶段复检（进程/API + 计数） | Task 3 + 4 |
| 阶段2 仍0 = Partial 黄提示 | Task 4 + 6 |
| 重启 KeyStats 不变 | Task 6（Secondary，原逻辑） |
| 打包脚本同目录 | Task 7 |
| 运维文档 | Task 8 |
| 单测 + UI 契约 | Task 1–4, 6, 8 |
| 不提权 WPF / 不改心跳 / 不改上游 | 全程遵守 |

## Placeholder scan

无 TBD/TODO；脚本日志路径与 elevate 输出约定已写死为 `%TEMP%\pim-keystats-fix-last.log`。

## Type consistency

- `KeyStatsStopResult` / `KeyStatsFixResult` / `KeyStatsFixOutcome` / `KeyStatsFixSuggestion` 全计划统一  
- 脚本文件名常量 `KeyStatsOneClickFixService.FixScriptFileName = "fix-keystats-session.ps1"`  
- 按钮/handler：`KeyStatsOneClickFixButton` / `OnOneClickFixKeyStats`
