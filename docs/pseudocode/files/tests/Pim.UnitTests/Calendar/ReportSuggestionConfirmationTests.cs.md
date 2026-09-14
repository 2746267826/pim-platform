# tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：报告可操作建议创建 L2 确认而非直接改事实。
- 主要依赖：`ReportService.RequestSuggestionActionAsync`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ActionableReportSuggestionCreatesConfirmationInsteadOfChangingFacts
- 种子 Report+Suggestion(move-task-segment)
- RequestSuggestionAction → L2、ChangedFields 含 startsAt；TaskExecutionSegment 仍 0

## 近逐行中文伪代码

1. [L1-L51] 主测试
2. [L53-L66] CreateDb/FixedUser

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs",
      "label": "ReportSuggestionConfirmationTests",
      "path": "tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "type": "tests" }
  ]
}
```
