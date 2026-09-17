# src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：从 Hangfire 监控 API 读取队列快照，映射为 `BackgroundJobSummaryDto` 健康状态
- 主要依赖：`Hangfire`、`Pim.Core.Operations`（`IBackgroundJobStatusService`、`BackgroundJobSummaryDto`、`PimHealthStatus`）
- 被谁使用：DI 注册为 `IBackgroundJobStatusService`；`SystemStatusService` 等聚合系统状态

## 函数级结构化伪代码

### HangfireMonitoringSnapshot
#### record HangfireMonitoringSnapshot(Processing, Enqueued, Scheduled, Failed)
- 输入：四类任务计数
- 输出：不可变监控快照
- 副作用：无
- 步骤：绑定四个 int 字段
- 分支与异常：无
- 调用：无

### IHangfireMonitoringClient
#### HangfireMonitoringSnapshot GetSnapshot()
- 输入：无
- 输出：当前 Hangfire 监控快照
- 副作用：读 Hangfire 存储监控 API
- 步骤：接口契约，由实现提供
- 分支与异常：由实现定义
- 调用：无

### HangfireMonitoringClient
#### HangfireMonitoringSnapshot GetSnapshot()
- 输入：无
- 输出：`HangfireMonitoringSnapshot`
- 副作用：访问 `JobStorage.Current.GetMonitoringApi()`
- 步骤：
  1. 取 monitoring API
  2. 读 Queues、ProcessingCount、ScheduledCount、FailedCount
  3. enqueued = 各队列 Length 之和
  4. 强转 int 后构造快照
- 分支与异常：JobStorage 未初始化等会向上抛
- 调用：`JobStorage.Current.GetMonitoringApi`、`Queues`/`ProcessingCount`/`ScheduledCount`/`FailedCount`、`Sum`

### HangfireJobStatusService
#### HangfireJobStatusService(IHangfireMonitoringClient monitoringClient)
- 输入：监控客户端
- 输出：服务实例
- 副作用：无
- 步骤：保存 `_monitoringClient`
- 分支与异常：无
- 调用：无

#### Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)
- 输入：可选取消令牌（本实现未使用）
- 输出：后台任务摘要 DTO（状态、四计数、检查时间、中文消息）
- 副作用：读取 Hangfire 监控
- 步骤：
  1. try：`GetSnapshot`
  2. 状态 = `MapFailedCountToStatus(Failed)`
  3. 构造 DTO：Processing/Enqueued/Scheduled/Failed、`UtcNow`、消息（有失败则「部分后台任务执行失败。」否则「后台任务正常。」）
  4. catch 任意异常：返回 Critical、全 0 计数、「后台任务状态不可用。」
- 分支与异常：任意异常吞掉并降级为 Critical 摘要
- 调用：`_monitoringClient.GetSnapshot`、`MapFailedCountToStatus`、`Task.FromResult`

#### static PimHealthStatus MapFailedCountToStatus(int failed)
- 输入：失败任务数
- 输出：`Warning`（failed>0）或 `Healthy`
- 副作用：无
- 步骤：三元判断
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Hangfire 与 `Pim.Core.Operations`
2. 命名空间 `Pim.Infrastructure.Operations`
3. 记录 `HangfireMonitoringSnapshot`：Processing/Enqueued/Scheduled/Failed
4. 接口 `IHangfireMonitoringClient.GetSnapshot`
5. `HangfireMonitoringClient.GetSnapshot`：取 MonitoringApi；汇总队列长度与三计数；返回快照
6. `HangfireJobStatusService` 实现 `IBackgroundJobStatusService`，构造注入监控客户端
7. `GetSummaryAsync` try：取快照 → 映射健康态 → 填 DTO 与中文说明 → `Task.FromResult`
8. catch：Critical + 零计数 + 「后台任务状态不可用。」
9. `MapFailedCountToStatus`：failed>0 → Warning，否则 Healthy

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs",
      "label": "HangfireJobStatusService",
      "path": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "to": "Hangfire.JobStorage", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "to": "Pim.Core.Operations.IBackgroundJobStatusService", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "type": "depends_on" }
  ]
}
```
