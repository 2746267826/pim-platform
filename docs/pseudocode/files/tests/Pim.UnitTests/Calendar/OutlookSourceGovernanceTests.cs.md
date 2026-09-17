# tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Outlook 源治理——仅 Outlook 图层、停止同步 L4 严格确认、执行/拒绝、数据中心 Graph id。
- 主要依赖：`PlanningModelService`、`OutlookConflictService`、`OperationConfirmationService`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### OutlookOnlyCalendarLayersExcludeManualSources
### StopSyncPreviewUsesL4Risk
### ExecuteConfirmedStopSyncDetachesOutlookEventAndRecordsAudit
### ExecuteStopSyncRejectsNonStrictConfirmation
### DataCenterOutlookSourceIncludesGraphIds

## 近逐行中文伪代码

1. [L18-38] OutlookOnly 过滤 manual
2. [L40-58] L4+严格
3. [L60+] ConfirmStrict 后 detach+审计
4. 非严格拒绝；DataCenter 含 OutlookEventId

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs",
      "label": "OutlookSourceGovernanceTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs", "to": "src/Pim.Module.Calendar/Services/OutlookConflictService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs", "to": "src/Pim.Module.Calendar/Services/PlanningModelService.cs", "type": "tests" }
  ]
}
```
