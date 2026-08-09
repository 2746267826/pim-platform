# tests/Pim.UnitTests/Operations/ScheduleFactConfirmationPolicyTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：日程事实变更风险分级 L2/L3/L4。
- 主要依赖：ScheduleFactConfirmationPolicy
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PimCoreFactChangesRequireL2
### OutlookCoreFactChangesRequireL3AndSecondLevelConfirmation
### DestructiveGovernanceRequiresL4AndStrictConfirmation

## 近逐行中文伪代码

1. pim 核心字段 L2
2. outlook L3+二级
3. stop-sync/batch-delete 等 L4+严格

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/ScheduleFactConfirmationPolicyTests.cs",
      "label": "ScheduleFactConfirmationPolicyTests.cs",
      "path": "tests/Pim.UnitTests/Operations/ScheduleFactConfirmationPolicyTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/ScheduleFactConfirmationPolicyTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Operations/ScheduleFactConfirmationPolicyTests.cs","to":"src/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs","type":"tests"}]
}
```