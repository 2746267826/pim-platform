# tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：OutlookIcsService.Parse 全天/会议元数据/复发；ImportOutlookIcsAsync 去重、默认日历、批内重复、解析错误、缺日期、字段截断。
- 主要依赖：`OutlookIcsService`、`CalendarService`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Parse：全天 / 会议元数据 / 复发
- 步骤：IsAllDay；ExternalMetadataJson(method/organizer/attendees/sequence/outlookProperties/html)；RRule/ExDates/RecurrenceId

### Import：跳过活跃重复忽略已删；目标日历缺失回落默认；批内 duplicate_uid；parse_error；invalid_date；Title 截 255
- 步骤：报告 Imported/Skipped/SkippedReasons/Samples；IgnoreQueryFilters 计数

## 近逐行中文伪代码

1. [L14-36] 全天
2. [L38-81] 会议元数据 JSON
3. [L83-111] 复发字段
4. [L113-178] 活跃 vs 已删 duplicate
5. [L180-209] 默认日历
6. [L211-245] 批内重复
7. [L247-300] 解析错误与缺日期
8. [L302-330] 超长 Title
9. [L332-350] 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs",
      "label": "OutlookIcsServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs", "to": "src/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs", "to": "src/Pim.Module.Calendar/Services/CalendarService.cs", "type": "tests" }
  ]
}
```
