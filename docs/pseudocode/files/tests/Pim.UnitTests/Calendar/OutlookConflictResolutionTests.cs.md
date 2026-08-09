# tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：人工冲突动作生成预期风险等级确认。
- 主要依赖：OutlookConflictService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ManualConflictActionsCreateExpectedConfirmationRisk
- Theory 各 action 对应风险

## 近逐行中文伪代码

1. 播种冲突
2. 请求动作确认
3. 断言 RiskLevel

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs",
      "label": "OutlookConflictResolutionTests.cs",
      "path": "tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs","to":"src/Pim.Module.Calendar/Services/OutlookConflictService.cs","type":"tests"}]
}
```