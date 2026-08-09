# tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：ICS 导出含 UID；Outlook ICS 导入预览聚合 duplicate 原因。
- 主要依赖：`IcsService`、`OutlookIcsService`、`CalendarService`、InMemory Db
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ExportSelectedObjectsIncludesUid
- 步骤：IcsService.ExportEvents 含 `UID:` 与 `SUMMARY:`

### ImportPreviewAggregatesDuplicateReason
- 步骤：库中已有同 UID 事件；导入预览 SkippedReasons["duplicate"]=1；Samples.Reason 以 duplicate 开头

### CreateDb / CreateCalendarService / FixedCurrentUserService
- 步骤：InMemory + 固定用户

## 近逐行中文伪代码

1. [L14] UserId
2. [L16-31] 导出 UID/SUMMARY
3. [L33-71] 预置事件 + ICS 文本 → ImportOutlookIcsAsync 聚合 duplicate
4. [L73-89] 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs",
      "label": "OutlookIcsCompletionTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs", "to": "src/Pim.Module.Calendar/Services/IcsService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs", "to": "src/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs", "to": "src/Pim.Module.Calendar/Services/CalendarService.cs", "type": "tests" }
  ]
}
```
