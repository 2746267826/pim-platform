# PIM 健康状态 planned offline（阶段 4）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** Windows 客户端在关机/休眠/注销/退出前尽力上报 planned_offline 事件，服务端以四态（在线/正常下线/异常离线/未接入）判定替换心跳新鲜度一刀切，消除「关机=不健康」误报。

**架构：** 服务端 `daemon_heartbeats` 加 `planned_offline_at`/`offline_reason` 两列 + 新端点 `POST /api/v1/daemon/planned-offline`；普通心跳清空 planned 标记；抽共享静态分类器 `DaemonLifecycleClassifier` 供 `SystemStatusService` 与 `PcTrackerQualityService.CheckDaemon` 共用（顺带收敛阈值常量）。客户端 `Pim.Client.Core` 新增 `PlannedOfflineReporter`（BuildRequest 纯函数 + ApiClient 上报），`Pim.Client.App` 挂 `SystemEvents.SessionEnding`（关机/注销）+ `PowerModeChanged.Suspend`（睡眠/休眠）+ `OnExit`（托盘退出/启动失败）三路监听，`Interlocked` 防重、2 秒超时、不重试。

**技术栈：** .NET 8（EF Core + Npgsql + Hangfire）、WPF（net8.0-windows，SystemEvents 托管 API 零 P/Invoke）、Xunit + EF InMemory + StubTimeProvider、node:test 前端零改动（四态落在既有 PimHealthStatus + 产品化 message）。

**需求文档：** `PIM展示层与分类体系改造需求文档_20260815_2317.md` §5（数据管道健康状态重新定义）。

**worktree：** `/workspace/pim-wt/planned-off`（分支 `opencode-linux/planned-offline`，基于 master 9f065c33）

---

## 0. 已锁定的设计决策（调查结论 + 需求 §5 综合）

### 0.1 四态判定（DaemonLifecycleClassifier，共享静态类）

| 状态 | 判定条件 | PimHealthStatus | message（产品化） | details.daemonState |
|---|---|---|---|---|
| 未接入 | 无任何 windows 心跳记录 | Unknown | 尚未收到 Windows 守护程序心跳。 | never-connected |
| 在线 | `now - received_at < 5min` | Healthy | Windows 守护程序在线。 | online |
| 正常下线 | `planned_offline_at` 非空且 `planned_offline_at >= received_at`（planned 之后无新普通心跳） | Healthy | 已关机/已休眠（正常）。 | planned-offline |
| 心跳偏旧（过渡态） | 无 planned 且 `5min <= age < 15min` | Warning | Windows 守护程序心跳偏旧。 | degraded |
| 异常离线 | 无 planned 且 `age >= 15min` | Warning | Windows 守护程序连接异常（可能崩溃/断网）。 | abnormal-offline |

- 阈值常量：`OnlineDaemonAge = 5min`、`DegradedDaemonAge = 5min`、`AbnormalDaemonAge = 15min`（替换现有 Warning 10min/Critical 60min 一刀切；需求 §5.3 明确 5/15 分钟口径）。旧阈值常量删除。
- 客户端恢复（开机）后普通心跳到达 → `DaemonHeartbeatService.Apply` 清空 `PlannedOfflineAt/OfflineReason` → 回到 online。
- 手机端（daemon_kind=android）**不参与**四态（需求 §5.4 暂不处理），`MobileQualityService` 不动。
- 展示细节（具体时间戳/reason）放 details（`plannedOfflineAt`/`offlineReason`），只进「状态信息」页；Today 面板只看 message/label。

### 0.2 存储与端点

- Migration `20260816XXXXXX_AddDaemonPlannedOffline`（EF API，非裸 SQL）：`daemon_heartbeats` 加
  - `planned_offline_at timestamptz NULL`
  - `offline_reason varchar(32) NULL`
  Down = DropColumn ×2；同步 `PimDbContextModelSnapshot`。
- 实体 `DaemonHeartbeatEntity` 加 `PlannedOfflineAt`、`OfflineReason` 两属性（列名 snake_case）。
- 新 DTO（`Pim.Core/Operations/DaemonHeartbeatDtos.cs`）：
  ```csharp
  public sealed record PlannedOfflineRequest(
      string DeviceId, string DaemonKind, string? Reason, DateTimeOffset? OccurredAt);
  ```
- `IDaemonHeartbeatService` 加 `RecordPlannedOfflineAsync(PlannedOfflineRequest request, CancellationToken ct)`；实现：按 `(DeviceId, DaemonKind)` 找行（不存在则建行仅 DeviceId/DaemonKind）→ `PlannedOfflineAt = request.OccurredAt ?? now`、`OfflineReason = Reason`（限制长度 32）、**不刷新 ReceivedAt**（received_at 语义 = 最近普通心跳）；SaveChanges 含既有 DbUpdateException 竞态重放。
- `DaemonHeartbeatService.Apply`：普通心跳时 `entity.PlannedOfflineAt = null; entity.OfflineReason = null;`
- 端点 `POST /api/v1/daemon/planned-offline`（DaemonEndpoints.cs，同组 `RequireAuthorization()`，与 heartbeat 一致），返回 `ApiResponse<DaemonHeartbeatDto>.Ok`（映射后带 planned 字段）。
- `DaemonHeartbeatDto` 加 `PlannedOfflineAt`/`OfflineReason`（前端未消费，仅契约完整性）。

### 0.3 服务端判定接线

- `SystemStatusService` 构造注入 `TimeProvider`（`AddAggregateResultCaching` 已全局注册 `TimeProvider.System`，零配置）；`checkedAt = _timeProvider.GetUtcNow()`；`BuildWindowsDaemonComponentAsync` 改用 `DaemonLifecycleClassifier.Classify(heartbeat, checkedAt)`（heartbeat null → never-connected）；details 加 `daemonState`/`plannedOfflineAt`（ToString("O")）/`offlineReason`；message 用分类器文案。查询异常仍 Critical。
- `PcTrackerQualityService.CheckDaemon`：心跳存在且分类器状态为 planned-offline 时，跳过 stale/old 新鲜度 issue（不产生误报），可加一条低危 info 类 issue（code `daemon-planned-offline`，Warning 或更低）带「已正常下线」文案；其余 last_error/队列/采集源检查保留。阈值改用共享常量。构造注入 `TimeProvider`。
- 前端零改动：`PimHealthStatus` 取值不变（0-3），TodayHealthSection 渲染 message/label 原样；StatusPage 的 details `<dl>` 自动展示新键。

### 0.4 客户端监听与上报

- 监听选型（调查结论排序）：**SystemEvents.SessionEnding**（关机/注销，`SessionEndingReason` 映射 reason=shutdown/logoff）+ **SystemEvents.PowerModeChanged**（`PowerModes.Suspend` → reason=suspend）+ **App.OnExit**（托盘退出/启动失败 → reason=exit）。不做 WM_POWERBROADCAST（需新建 HWND，PowerModeChanged 已覆盖睡眠）、不做 SetConsoleCtrlHandler（需 P/Invoke 无先例、SessionEnding 已覆盖）。
- `PlannedOfflineReporter`（**Pim.Client.Core**，纯 net8.0，测试项目只引用 Core）：
  - `BuildRequest(string deviceId, string reason, DateTimeOffset occurredAt)` 纯函数：`DaemonKind = "windows"`、DeviceId、Reason、OccurredAt。
  - `ReportAsync(request, ct)`：`_api.PostAsync<object>("daemon/planned-offline", request, ct)`（复用 ApiClient 的 /api/v1 前缀 + Bearer + 401 刷新）。
  - DI 注册进 `Startup.cs`（DaemonHeartbeatReporter 旁）。
- App 接线（Pim.Client.App，net8.0-windows 开箱可用 SystemEvents）：
  - 一次性防重标志 `private int _plannedOfflineSent;`，三路统一走 `TryReportPlannedOffline(string reason)`：
    ```csharp
    private void TryReportPlannedOffline(string reason)
    {
        if (Interlocked.Exchange(ref _plannedOfflineSent, 1) == 1) return;
        var reporter = Services?.GetService<PlannedOfflineReporter>();
        if (reporter is null) return;
        var request = PlannedOfflineReporter.BuildRequest(Environment.MachineName, reason, DateTimeOffset.UtcNow);
        _ = Task.Run(async () =>
        {
            try
            {
                using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(2));
                await reporter.ReportAsync(request, cts.Token);
                Logger.Info($"Planned offline reported ({reason})");
            }
            catch (Exception ex)
            {
                Logger.Warn($"Planned offline report failed ({reason}): {ex.Message}");
            }
        });
    }
    ```
  - `OnStartup` 挂 `SystemEvents.SessionEnding += (_, e) => TryReportPlannedOffline(e.Reason == SessionEndingReason.Shutdown ? "shutdown" : "logoff");` 与 `SystemEvents.PowerModeChanged += (_, e) => { if (e.Mode == PowerModes.Suspend) TryReportPlannedOffline("suspend"); };`
  - `OnExit`（在 `base.OnExit(e)` 之前）调 `TryReportPlannedOffline("exit")`；OnExit 是同步的、返回后进程退出，fire-and-forget 的 Task 有被截断风险 → OnExit 路径改用**有界同步等待**：`TryReportPlannedOffline("exit", wait: true)`（`Task.Run(...).Wait(TimeSpan.FromSeconds(2))`），其余两路 fire-and-forget。实现时把等待参数化。
- 需求 §5.2 的 `reason: "shutdown|suspend|logoff"` 取值 + 补 "exit"。

### 0.5 测试策略

- **后端**（tests/Pim.UnitTests/Operations/）：
  - 新 `DaemonLifecycleClassifierTests.cs`：五态判定 + 边界（4:59/5:00/14:59/15:00）+ planned >= received_at 判定 + null heartbeat。
  - `SystemStatusServiceTests` 改造：注入 `StubTimeProvider`（Calendar/OutlookGraphTestDoubles 模式）替换相对偏移写法；新增 planned-offline 用例（Healthy + message「已关机/已休眠」+ details.daemonState）。
  - `DaemonHeartbeatServiceTests` 追加：普通心跳清空 planned 字段；`RecordPlannedOfflineAsync` 建行/更新/不刷新 ReceivedAt/竞态重放。
  - 模型快照测试（Stage0PersistenceTests 或 PimPcTrackerModelTests 模式）确认新列在快照中。
- **客户端**（tests/Pim.UnitTests/ClientWindows/）：
  - 新 `PlannedOfflineReporterTests.cs`：BuildRequest 纯测（deviceId=Environment.MachineName、daemon_kind="windows"、reason/occurredAt 透传）+ 与服务端 `PlannedOfflineRequest` 的 JSON 往返契约测试（照 DaemonHeartbeatReporterTests L91-114 模式）。
  - App 接线文本断言：新 `WindowsPlannedOfflineWiringTests.cs`（照 WindowsCompanionShellTests 模式）断言 App.xaml.cs 源码含 `SessionEnding`、`PowerModeChanged`、`TryReportPlannedOffline`、`Interlocked`、2s 超时字样；csproj 无新依赖。
  - 前端：无改动无测试。

---

## 任务 1：服务端存储 + planned-offline 端点 + 心跳清理

**文件：**
- 修改：`src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs`
- 创建：`src/Pim.Infrastructure/Data/Migrations/20260816XXXXXX_AddDaemonPlannedOffline.cs`（+ 同步 `PimDbContextModelSnapshot.cs`）
- 修改：`src/Pim.Core/Operations/DaemonHeartbeatDtos.cs`
- 修改：`src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs`（接口 + 实现 + Apply 清理）
- 修改：`src/Pim.Api/Endpoints/DaemonEndpoints.cs`
- 测试：`tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`（追加）、模型快照断言

- [x] **步骤 1：写失败测试**（DaemonHeartbeatServiceTests 追加）

```csharp
[Fact]
public async Task RecordPlannedOfflineAsync_CreatesRowWhenMissing()
{
    await using var db = CreateDb();
    var service = new DaemonHeartbeatService(db, StubClock(now));
    var result = await service.RecordPlannedOfflineAsync(
        new PlannedOfflineRequest("PC-1", "windows", "shutdown", FixedNow), CancellationToken.None);
    var row = await db.DaemonHeartbeats.SingleAsync();
    Assert.Equal(FixedNow, row.PlannedOfflineAt);
    Assert.Equal("shutdown", row.OfflineReason);
    Assert.NotEqual(FixedNow.AddMinutes(1), row.ReceivedAt); // received_at 不被刷新（保持初始/默认语义——断言它不等于 occurredAt+1 即可，具体见实现注释）
}

[Fact]
public async Task RecordPlannedOfflineAsync_UpdatesExistingRowWithoutTouchingReceivedAt()
{
    await using var db = CreateDb();
    var existing = new DaemonHeartbeatEntity { DeviceId = "PC-1", DaemonKind = "windows", ReceivedAt = FixedNow.AddMinutes(-30) };
    db.DaemonHeartbeats.Add(existing);
    await db.SaveChangesAsync();
    var service = new DaemonHeartbeatService(db, StubClock(now));
    var result = await service.RecordPlannedOfflineAsync(
        new PlannedOfflineRequest("PC-1", "windows", "suspend", FixedNow), CancellationToken.None);
    Assert.Equal(FixedNow, existing.PlannedOfflineAt);
    Assert.Equal("suspend", existing.OfflineReason);
    Assert.Equal(FixedNow.AddMinutes(-30), existing.ReceivedAt);
}

[Fact]
public async Task UpsertAsync_ClearsPlannedOfflineOnRegularHeartbeat()
{
    await using var db = CreateDb();
    db.DaemonHeartbeats.Add(new DaemonHeartbeatEntity
    {
        DeviceId = "PC-1", DaemonKind = "windows",
        PlannedOfflineAt = FixedNow.AddMinutes(-5), OfflineReason = "suspend",
        ReceivedAt = FixedNow.AddMinutes(-10)
    });
    await db.SaveChangesAsync();
    var service = new DaemonHeartbeatService(db, StubClock(now));
    await service.UpsertAsync(HeartbeatRequest("PC-1"), CancellationToken.None);
    var row = await db.DaemonHeartbeats.SingleAsync();
    Assert.Null(row.PlannedOfflineAt);
    Assert.Null(row.OfflineReason);
}
```

（`CreateDb`/`HeartbeatRequest`/Stub 时钟辅助按现有 DaemonHeartbeatServiceTests 模式；`FixedNow` 用固定 DateTimeOffset。既有测试若构造签名变化同步适配。）

- [x] **步骤 2：确认失败 → 步骤 3：实现**（migration + entity + DTO + service + endpoint 按 §0.2；migration 时间戳取执行时刻）

- [x] **步骤 4：测试通过 + 全量**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~DaemonHeartbeat" --no-restore` → PASS；`dotnet test Pim.sln --no-restore` 全量绿。

- [x] **步骤 5：Commit**

```bash
git commit -m "feat: planned offline storage, endpoint and heartbeat clearing / 计划离线存储、上报端点与心跳清理"
```

---

## 任务 2：四态判定接线（SystemStatusService + PcTrackerQualityService）

**文件：**
- 创建：`src/Pim.Infrastructure/Operations/DaemonLifecycleClassifier.cs`
- 修改：`src/Pim.Infrastructure/Operations/SystemStatusService.cs`
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs`
- 测试：`tests/Pim.UnitTests/Operations/DaemonLifecycleClassifierTests.cs`（新建）、`tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs`（改造）、`tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`（追加）

- [x] **步骤 1：写失败测试**

```csharp
// DaemonLifecycleClassifierTests.cs 骨架
public sealed record DaemonLifecycleState(string State, PimHealthStatus Status, string Message);

[Theory]
// 4:59 / 5:00 / 14:59 / 15:00 边界
[InlineData(-4.9, "online", "Healthy")]      // 4.9 分钟前心跳
[InlineData(-5.0, "degraded", "Warning")]
[InlineData(-14.9, "degraded", "Warning")]
[InlineData(-15.0, "abnormal-offline", "Warning")]
public void Classify_NoPlanned_ByAge(double minutesAgo, string state, string status)
{
    var heartbeat = Heartbeat(receivedAt: FixedNow.AddMinutes(minutesAgo));
    var result = DaemonLifecycleClassifier.Classify(heartbeat, FixedNow);
    Assert.Equal(state, result.State);
    Assert.Equal(status, result.Status.ToString());
}

[Fact]
public void Classify_PlannedOffline_BeatsAge()
{
    var heartbeat = Heartbeat(receivedAt: FixedNow.AddHours(-3), plannedAt: FixedNow.AddMinutes(-1));
    var result = DaemonLifecycleClassifier.Classify(heartbeat, FixedNow);
    Assert.Equal("planned-offline", result.State);
    Assert.Equal(PimHealthStatus.Healthy, result.Status);
    Assert.Equal("已关机/已休眠（正常）。", result.Message);
}

[Fact]
public void Classify_StalePlanned_AfterNewerHeartbeat_TreatedByAge()
{
    // planned_offline_at 早于最近心跳 → 计划离线已过期，回到年龄判定
    var heartbeat = Heartbeat(receivedAt: FixedNow.AddMinutes(-3), plannedAt: FixedNow.AddHours(-2));
    var result = DaemonLifecycleClassifier.Classify(heartbeat, FixedNow);
    Assert.Equal("online", result.State);
}

[Fact]
public void Classify_NullHeartbeat_NeverConnected()
{
    var result = DaemonLifecycleClassifier.Classify(null, FixedNow);
    Assert.Equal("never-connected", result.State);
    Assert.Equal(PimHealthStatus.Unknown, result.Status);
    Assert.Equal("尚未收到 Windows 守护程序心跳。", result.Message);
}
```

SystemStatusServiceTests 追加：planned-offline 心跳 → GetDetail 的 windows-daemon 组件 status=Healthy、message「已关机/已休眠（正常）。」、details["daemonState"]=="planned-offline"、details["offlineReason"]=="shutdown"；在线新心跳 → daemonState=="online"。PcTrackerQualityServiceTests 追加：planned-offline 心跳不产生 stale-windows-daemon-heartbeat 与 old-daemon-heartbeat issue。

- [x] **步骤 2：确认失败 → 步骤 3：实现**（§0.1/§0.3；`DaemonLifecycleClassifier` 签名：`Classify(DaemonHeartbeatEntity? heartbeat, DateTimeOffset checkedAt)` 返回 `DaemonLifecycleState(State, PimHealthStatus, Message, string? PlannedOfflineAt, string? OfflineReason)`——record；SystemStatusService/PcTrackerQualityService 注入 TimeProvider，现有测试全部改用 StubTimeProvider）

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: four-state daemon lifecycle classification replaces age-only health / 四态生命周期判定替换心跳一刀切"
```

---

## 任务 3：Windows 客户端监听与上报

**文件：**
- 创建：`src/client-windows/Pim.Client.Core/Services/PlannedOfflineReporter.cs`
- 创建：`src/client-windows/Pim.Client.Core/Models/PlannedOfflineDtos.cs`（客户端 request record）
- 修改：`src/client-windows/Pim.Client.App/Startup.cs`（DI）
- 修改：`src/client-windows/Pim.Client.App/App.xaml.cs`（三路监听 + 防重 + 2s 超时 + OnExit 有界等待）
- 测试：`tests/Pim.UnitTests/ClientWindows/PlannedOfflineReporterTests.cs`（新建）、`tests/Pim.UnitTests/ClientWindows/WindowsPlannedOfflineWiringTests.cs`（新建）

- [x] **步骤 1：写失败测试**

```csharp
// PlannedOfflineReporterTests.cs
[Fact]
public void BuildRequest_FillsDeviceKindAndReason()
{
    var at = new DateTimeOffset(2026, 8, 16, 12, 0, 0, TimeSpan.Zero);
    var req = PlannedOfflineReporter.BuildRequest(Environment.MachineName, "shutdown", at);
    Assert.Equal(Environment.MachineName, req.DeviceId);
    Assert.Equal("windows", req.DaemonKind);
    Assert.Equal("shutdown", req.Reason);
    Assert.Equal(at, req.OccurredAt);
}

[Fact]
public void BuildRequest_RoundTripsToServerDto()
{
    var at = new DateTimeOffset(2026, 8, 16, 12, 0, 0, TimeSpan.Zero);
    var client = PlannedOfflineReporter.BuildRequest("PC-1", "suspend", at);
    var json = JsonSerializer.Serialize(client, new JsonSerializerOptions(JsonSerializerDefaults.Web));
    var server = JsonSerializer.Deserialize<PlannedOfflineRequest>(json, new JsonSerializerOptions(JsonSerializerDefaults.Web));
    Assert.NotNull(server);
    Assert.Equal("PC-1", server.DeviceId);
    Assert.Equal("windows", server.DaemonKind);
    Assert.Equal("suspend", server.Reason);
    Assert.Equal(at, server.OccurredAt);
}
```

```csharp
// WindowsPlannedOfflineWiringTests.cs（文本断言，照 WindowsCompanionShellTests 模式）
[Fact]
public void AppWiresPlannedOfflineListeners()
{
    var source = File.ReadAllText(Path.Combine(RepoPath, "src/client-windows/Pim.Client.App/App.xaml.cs"));
    Assert.Contains("SystemEvents.SessionEnding", source);
    Assert.Contains("PowerModeChanged", source);
    Assert.Contains("TryReportPlannedOffline", source);
    Assert.Contains("Interlocked", source);
    Assert.Contains("TimeSpan.FromSeconds(2)", source);
    Assert.Contains("PlannedOfflineReporter", source);
    Assert.Contains("\"shutdown\"", source);
    Assert.Contains("\"suspend\"", source);
    Assert.Contains("\"exit\"", source);
}
```

- [x] **步骤 2：确认失败 → 步骤 3：实现**（§0.4 全流程；App 侧接线时确认 `OnStartup` 的位置在 `Services = ConfigureServices()` 之后挂事件、OnExit 在 base.OnExit 前调用；Startup.cs 加 `services.AddSingleton<PlannedOfflineReporter>()`）

- [x] **步骤 4：测试通过 + 全量 + Commit**

```bash
git commit -m "feat: windows daemon planned offline reporting on shutdown/suspend/logoff / Windows 守护程序关机休眠注销前上报计划离线"
```

---

## 任务 4：收尾（全量门禁 + PR + 三视角 review + 合并清理）

- [ ] **步骤 1：全量门禁**

```bash
dotnet test Pim.sln --no-restore          # 期望 1342+ 通过
npm --prefix src/client-web run test:schedule-workbench-complete   # 前端零改动，回归确认
npm --prefix src/client-web run build
git diff --check origin/master
```

- [ ] **步骤 2：push + PR**（四节双语：技术修改/功能变化/如何体验/测试）

- [ ] **步骤 3：CI 门禁**（gh pr checks --watch；注意 build-windows 会触发——client-windows 改动在路径过滤内，必须全绿）

- [ ] **步骤 4：三视角 review**（sol/terra/flash 并行，Important+ 清零循环）

- [ ] **步骤 5：合并后清理**（worktree remove + branch -d + master fast-forward）

---

## 明确不做（阶段 4 边界）

- 手机端健康状态（需求 §5.4：OPPO 后台留存未解决，另行设计）。
- WM_POWERBROADCAST/HwndSource 消息窗口与 SetConsoleCtrlHandler P/Invoke（SystemEvents 托管路径已覆盖关机/注销/睡眠三场景，见 §0.4 选型）。
- 心跳上报通道的通用事件总线改造（本阶段只加一个专用端点）。
- 前端 UI 改动（四态经既有 PimHealthStatus + 产品化 message 表达，StatusPage details 自动展示新键）。
- EndpointStatusService（endpoint_statuses 另一套心跳）不动——与本需求无关。
