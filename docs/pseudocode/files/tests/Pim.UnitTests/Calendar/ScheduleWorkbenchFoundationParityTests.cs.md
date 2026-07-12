# tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：地基风险等级与关键实体仍在模型中。
- 主要依赖：`OperationRiskLevel`、TaskExecutionSegment/OutlookSyncBatch 实体
- 被谁使用：xUnit

## 函数级结构化伪代码

- L0–L4 枚举存在；InMemory 模型含 TaskExecutionSegmentEntity 与 OutlookSyncBatchEntity

## 近逐行中文伪代码

1. [L1-L29] 单 Fact

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs",
      "label": "ScheduleWorkbenchFoundationParityTests",
      "path": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs", "to": "src/Pim.Core/Operations", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs", "to": "src/modules/Pim.Module.Calendar/Entities", "type": "tests" }
  ]
}
```
