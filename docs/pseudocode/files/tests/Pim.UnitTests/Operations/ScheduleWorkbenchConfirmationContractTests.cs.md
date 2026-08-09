# tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：锁定日程工作台确认契约：风险等级枚举、DTO 差分字段、未知风险回落、持久化 pending 视图。
- 主要依赖：`OperationRiskLevel`、`OperationConfirmationDto`、`OperationConfirmationService`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ScheduleWorkbenchConfirmationContractTests
#### RiskLevelsExposeWorkbenchScaleAndLegacyValues
- L0–L4 与 Low/Medium/High 均 IsDefined
#### ConfirmationDtoCarriesDiffAndSecondLevelMetadata
- 构造 L3 DTO，断言 ChangedFields/AllowedActions/ObjectType/RequiresSecondLevel
#### ConfirmationServiceFallsBackToMediumForUnknownRiskValues
- 实体 RiskLevel=FutureRiskValue → GetAsync 得 Medium
#### ConfirmationServicePersistsDiffMetadataForPendingViews
- CreateAsync 带差分元数据 → ListPendingForUserAsync 回显

## 近逐行中文伪代码

1. [L1-L11] using 与类
2. [L12-L23] 风险枚举存在性
3. [L25-L54] DTO 字段
4. [L56-L80] 未知风险回落
5. [L82-L114] pending 列表持久化
6. [L116-L122] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs",
      "label": "ScheduleWorkbenchConfirmationContractTests",
      "path": "tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" }
  ]
}
```
