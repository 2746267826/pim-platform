# src/Pim.Infrastructure/Operations/SystemStatusService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `ISystemStatusService`，聚合 API/数据库/Windows 守护/后台任务组件健康，输出摘要与详情。
- 主要依赖：`PimDbContext`、`IBackgroundJobStatusService`、`DaemonHeartbeatEntity`、`Pim.Core.Operations` 状态 DTO/枚举、EF Core
- 被谁使用：DI 注册为 `ISystemStatusService`；`StatusEndpoints`；`OperationsHealthTodaySectionProvider`

## 函数级结构化伪代码

### SystemStatusService
#### 构造 SystemStatusService(PimDbContext db, IBackgroundJobStatusService backgroundJobs)
- 输入：DbContext、后台任务状态服务
- 输出：服务实例
- 副作用：保存字段
- 步骤：赋值 `_db`、`_backgroundJobs`
- 分支与异常：无
- 调用：无

#### Task<SystemStatusSummaryDto> GetSummaryAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：详情中的 `Summary`
- 副作用：同 `GetDetailAsync` 的探测
- 步骤：
  1. `await GetDetailAsync(ct)`
  2. 返回 `detail.Summary`
- 分支与异常：透传
- 调用：`GetDetailAsync`

#### Task<SystemStatusDetailDto> GetDetailAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：`SystemStatusDetailDto`（摘要 + 组件列表 + NextSteps）
- 副作用：探测 DB、读心跳、查后台任务摘要
- 步骤：
  1. `checkedAt = UtcNow`
  2. 组件列表先放 API：固定 Healthy、「API 进程正在运行。」
  3. 追加 `BuildDatabaseComponentAsync`
  4. 追加 `BuildWindowsDaemonComponentAsync`
  5. 追加 `BuildBackgroundJobsComponentAsync`
  6. 整体状态 = 组件中按 `GetSeverityRank` 最高者
  7. 摘要：`status` + `GetLabel` + `GetMessage` + `checkedAt`
  8. NextSteps = 状态为 Warning/Critical 的组件 Message 列表
  9. 返回 `SystemStatusDetailDto`
- 分支与异常：子构建方法内部捕获 DB 异常
- 调用：三个 Build*、`GetSeverityRank`、`GetLabel`、`GetMessage`

#### Task<StatusComponentDto> BuildDatabaseComponentAsync(DateTimeOffset checkedAt, CancellationToken ct)
- 输入：检查时间、取消令牌
- 输出：database 组件
- 副作用：非 InMemory 时执行 `SELECT 1`
- 步骤：
  1. try：若 Provider 不是 InMemory → `ExecuteSqlRawAsync("SELECT 1")`；返回 Healthy「数据库可访问。」
  2. catch：Critical「数据库不可用。」，Details 含 `error=ex.Message`
- 分支与异常：见上
- 调用：`_db.Database.ExecuteSqlRawAsync`

#### Task<StatusComponentDto> BuildWindowsDaemonComponentAsync(DateTimeOffset checkedAt, CancellationToken ct)
- 输入：检查时间、取消令牌
- 输出：windows-daemon 组件
- 副作用：查询 `DaemonHeartbeats`
- 步骤：
  1. try：查 `DaemonKind == "windows"` 最新 `ReceivedAt` 一条（AsNoTracking）
  2. catch：Critical「心跳状态不可用」，Details.error
  3. 无记录：Unknown「尚未收到心跳」
  4. `age = checkedAt - ReceivedAt`
  5. age ≥ 60 分钟 → Critical；≥ 10 分钟 → Warning；否则 Healthy
  6. 按状态选中文消息；Details 含 deviceId/version/receivedAt/activityWatch/keyStats
- 分支与异常：查询失败 vs 无数据 vs 按年龄分级
- 调用：EF `FirstOrDefaultAsync`；常量 `WarningDaemonAge`/`CriticalDaemonAge`

#### Task<StatusComponentDto> BuildBackgroundJobsComponentAsync(CancellationToken ct)
- 输入：取消令牌
- 输出：background-jobs 组件
- 副作用：委托后台任务服务
- 步骤：
  1. `summary = await _backgroundJobs.GetSummaryAsync(ct)`
  2. 映射 Key/Name/Kind/Status/Message/CheckedAt；Details 含 processing/enqueued/scheduled/failed
- 分支与异常：由 `_backgroundJobs` 定义
- 调用：`IBackgroundJobStatusService.GetSummaryAsync`

#### static string GetLabel / int GetSeverityRank / string GetMessage(PimHealthStatus status)
- 输入：健康枚举
- 输出：中文标签 / 严重度秩 0–3 / 中文总览消息
- 副作用：无
- 步骤：switch 映射 Healthy/Warning/Critical/默认 Unknown
- 分支与异常：默认分支
- 调用：无

## 近逐行中文伪代码

1. 引入 EF Core、`Pim.Core.Operations`、`Pim.Infrastructure.Data`、Entities
2. 命名空间 `Pim.Infrastructure.Operations`
3. 密封类实现 `ISystemStatusService`
4. 静态阈值：警告 10 分钟、严重 60 分钟
5. 注入 `_db`、`_backgroundJobs`
6. `GetSummaryAsync`：取详情摘要
7. `GetDetailAsync`：记 checkedAt；组件列表含 API
8. 追加数据库、Windows 守护、后台任务组件
9. 按严重度排序取最差状态为总状态
10. 构造 Summary；NextSteps 收集告警组件消息
11. `BuildDatabaseComponentAsync`：InMemory 跳过 SQL；否则 SELECT 1；异常 Critical
12. `BuildWindowsDaemonComponentAsync`：查 windows 最新心跳；异常/空/年龄分支
13. Details 写入 deviceId、version、receivedAt、activityWatch、keyStats
14. `BuildBackgroundJobsComponentAsync`：转发后台任务摘要计数
15. `GetLabel`：正常/有警告/故障/未知
16. `GetSeverityRank`：Healthy0 Unknown1 Warning2 Critical3
17. `GetMessage`：全正常 / 需关注 / 故障中 / 未知

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/SystemStatusService.cs",
      "label": "SystemStatusService",
      "path": "src/Pim.Infrastructure/Operations/SystemStatusService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/SystemStatusService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "depends_on" }
  ]
}
```
