# src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：`IBackgroundJobStatusService` 的空实现：未配置后台作业时返回 Unknown 汇总与说明文案。
- 主要依赖：`Pim.Core.Operations`（`IBackgroundJobStatusService`、`BackgroundJobSummaryDto`、`PimHealthStatus`）
- 被谁使用：可作为占位注册；生产 DI 默认注册 `HangfireJobStatusService`；`SystemStatusService` 依赖接口

## 函数级结构化伪代码

### NoopBackgroundJobStatusService
#### `Task<BackgroundJobSummaryDto> GetSummaryAsync(CancellationToken ct = default)`
- 输入：可选取消令牌（未使用）
- 输出：已完成任务，值为固定 DTO
- 副作用：无 I/O
- 步骤：
  1. 构造 `BackgroundJobSummaryDto(PimHealthStatus.Unknown, 0, 0, 0, 0, DateTimeOffset.UtcNow, "Background jobs are not configured yet.")`
  2. `Task.FromResult` 包装返回
- 分支与异常：无
- 调用：无外部服务

## 近逐行中文伪代码

1. 引入 `Pim.Core.Operations`
2. 命名空间 `Pim.Infrastructure.Operations`
3. 密封类实现 `IBackgroundJobStatusService`
4. `GetSummaryAsync`：忽略 ct，立即返回 Unknown 健康、四计数为 0、当前 UTC 时间、英文提示「尚未配置后台作业」

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs",
      "label": "NoopBackgroundJobStatusService",
      "path": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/NoopBackgroundJobStatusService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/BackgroundJobDtos.cs", "type": "calls" }
  ]
}
```
