# tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证活动分类规则保存：scope 归一、已知分类、重名（含 trim）。
- 主要依赖：`ActivityClassificationRuleService`、`PcCategoryEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. SaveAsync：Scope app→activity；分类存在
2. 未知分类 ArgumentException 中文消息
3. 重名 InvalidOperationException
4. 仅空白差异的重名同样拒绝

## 近逐行中文伪代码

1. [L1-L24] 归一与已知分类
2. [L26-L36] 未知分类
3. [L38-L78] 重名两场景
4. [L80-L99] NewRule/CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs",
      "label": "ActivityClassificationRuleServiceTests",
      "path": "tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "type": "tests" }
  ]
}
```
