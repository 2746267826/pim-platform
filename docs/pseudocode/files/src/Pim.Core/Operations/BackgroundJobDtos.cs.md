# src/Pim.Core/Operations/BackgroundJobDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义后台任务队列健康摘要 DTO 与状态查询契约。
- 主要依赖：`PimHealthStatus`（`OperationEnums.cs`）
- 被谁使用：`HangfireJobStatusService` / `NoopBackgroundJobStatusService` 实现；`SystemStatusService` 聚合调用；DI 注册为 `IBackgroundJobStatusService`

## 函数级结构化伪代码

### BackgroundJobSummaryDto
#### record BackgroundJobSummaryDto(Status, Processing, Enqueued, Scheduled, Failed, CheckedAt, Message)
- 输入：构造参数——健康枚举、处理中/排队/计划/失败计数、检查时间、说明文案
- 输出：不可变摘要记录实例
- 副作用：无
- 步骤：
  1. 作为值对象承载一次后台任务系统快照
  2. 字段语义：`Processing`/`Enqueued`/`Scheduled`/`Failed` 对应队列各状态数量
- 分支与异常：无
- 调用：由 `IBackgroundJobStatusService` 实现构造并返回

### IBackgroundJobStatusService
#### Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)
- 输入：`ct` 取消令牌
- 输出：`BackgroundJobSummaryDto`
- 副作用：无（契约层）；实现可能读 Hangfire 存储
- 步骤：
  1. 实现读取后台作业监控数据
  2. 映射失败数等到 `PimHealthStatus`
  3. 返回摘要 DTO
- 分支与异常：未配置实现（Noop）返回 `Unknown`；Hangfire 异常可返回 `Critical`
- 调用：被 `SystemStatusService` 聚合系统状态时调用

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Operations`
2. 声明密封记录 `BackgroundJobSummaryDto`
3. 字段 `Status`：整体健康（`PimHealthStatus`）
4. 字段 `Processing`：正在处理的作业数
5. 字段 `Enqueued`：已入队待执行数
6. 字段 `Scheduled`：计划中作业数
7. 字段 `Failed`：失败作业数
8. 字段 `CheckedAt`：本快照采集时间
9. 字段 `Message`：人类可读说明
10. 声明接口 `IBackgroundJobStatusService`
11. 方法 `GetSummaryAsync`：异步返回上述摘要，支持取消

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/BackgroundJobDtos.cs",
      "label": "BackgroundJobDtos",
      "path": "src/Pim.Core/Operations/BackgroundJobDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/BackgroundJobDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "calls" }
  ]
}
```
