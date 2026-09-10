# tests/Pim.UnitTests/Calendar/ReportServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：报告生成不改事实任务，风险 L0，无确认。
- 主要依赖：`ReportService`、`OperationConfirmationService`
- 被谁使用：xUnit

## 函数级结构化伪代码

### GeneratesReportArtifactWithoutMutatingFacts (Theory Daily/Weekly/Monthly/Project)
- GenerateAsync → Kind/L0/ContentMarkdown；Task 计数不变；无 OperationConfirmations

## 近逐行中文伪代码

1. [L1-L40] Theory 生成
2. [L42-L58] helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ReportServiceTests.cs",
      "label": "ReportServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/ReportServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ReportServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ReportServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "type": "tests" }
  ]
}
```
