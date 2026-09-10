# tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 `AuditVersionService` 记录前后快照与时间线查询。
- 主要依赖：`AuditVersionService`、`PimDbContext`
- 被谁使用：xUnit

## 函数级结构化伪代码

### AuditVersionServiceTests
#### RecordAsyncWritesBeforeAfterAuditVersion
- RecordAsync(event, before/after, changedFields, confirmationId)
- GetTimelineAsync 单条：Id/ConfirmationId/BeforeJson/AfterJson/ChangedFieldsJson
#### CreateDb：InMemory

## 近逐行中文伪代码

1. [L1-L9] using 与类
2. [L10-L35] 记录与时间线断言
3. [L37-L44] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs",
      "label": "AuditVersionServiceTests",
      "path": "tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "tests" }
  ]
}
```
