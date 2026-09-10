# tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：确认请求携带前后对比与严格/审计元数据；Outlook 二级确认；基本确认无法绕过。
- 主要依赖：ScheduleFactConfirmation / OperationConfirmationService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ConfirmationRequestCarriesBeforeAfterStrictAndAuditMetadata
### OutlookOriginCoreFactChangeCanRequireSecondLevelConfirmation
### BasicConfirmCannotBypassSecondLevelOrStrictConfirmation

## 近逐行中文伪代码

1. 请求元数据
2. Outlook 二级
3. 基本 Confirm 绕不过二级/严格

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs",
      "label": "ScheduleFactConfirmationGateTests.cs",
      "path": "tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs","to":"src/Pim.Module.Calendar/Services","type":"tests"}
}
```