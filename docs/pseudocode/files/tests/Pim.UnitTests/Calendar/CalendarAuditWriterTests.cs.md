# tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 `CalendarAuditWriter.RecordSuccessAsync` 写入审计日志与元数据。
- 主要依赖：`CalendarAuditWriter`、`AuditLogService`、`PimDbContext`
- 被谁使用：xUnit

## 函数级结构化伪代码

### CalendarAuditWriterTests
#### RecordSuccessAsync_WritesCalendarAuditWithMetadata
- 输入：userId、action、resourceType、resourceId、metadata 字典
- 输出：AuditLogs 单行
- 步骤：创建 writer → RecordSuccessAsync → 断言 UserId/Action/Resource/Source=calendar 与 MetadataJson 含 title 与 affectedCount
#### CreateDb
- InMemory 数据库

## 近逐行中文伪代码

1. [L1-L10] using 与类
2. [L11-L39] 成功写入并断言字段与 metadata
3. [L41-L47] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs",
      "label": "CalendarAuditWriterTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "depends_on" }
  ]
}
```
