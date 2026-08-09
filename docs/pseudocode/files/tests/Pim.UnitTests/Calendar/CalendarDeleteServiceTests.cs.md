# tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖日历/事件/任务软删除、批量 operationId 与空/已删幂等。
- 主要依赖：`CalendarDeleteService`、`CalendarAuditWriter`、`AuditLogService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. DeleteEventAsync：软删+审计 calendar.events.delete
2. DeleteCalendarBook：仅活跃子项同 operationId；预览 AffectedCount
3. BatchDeleteTasks：两任务同一 operationId/batch-task
4. BatchDeleteEvents 空/null：0 影响无审计
5. BatchDeleteTasks 未知/已删：0 且不重标

## 近逐行中文伪代码

1. [L1-L34] 单事件软删
2. [L36-L63] 日历簿级联
3. [L65-L80] 批量任务
4. [L82-L145] 边界幂等
5. [L147-L206] 装配与种子

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs",
      "label": "CalendarDeleteServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "type": "tests" }
  ]
}
```
