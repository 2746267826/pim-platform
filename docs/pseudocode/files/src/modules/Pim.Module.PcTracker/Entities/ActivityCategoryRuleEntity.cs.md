# src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类规则实体，表 `pc_activity_category_rules`，存储规则名、范围、分类/项目标签、条件 JSON 与优先级。
- 主要依赖：DataAnnotations / Schema
- 被谁使用：`ActivityClassificationRuleService`、`PcTrackerService`、`ActivityCategoryRuleEntityConfiguration`、`PimDbContext`

## 函数级结构化伪代码

### ActivityCategoryRuleEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 表 `pc_activity_category_rules`；`Id` 主键 Guid。
  2. `RuleName` 最长 128；`Scope` 默认 `"activity"`。
  3. `CategoryName`/`ProjectTag` 可选；`Color` 默认 `#64748b`。
  4. `Priority`；`Source` 默认 `"user"`；`Status` 默认 `"active"`。
  5. `ConditionsJson` jsonb 默认 `"{}"`；`Confidence` 默认 1。
  6. `Explanation` 可空；`CreatedAt`/`UpdatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema。
2. 命名空间 `Pim.Module.PcTracker.Entities`；类映射 `pc_activity_category_rules`。
3. Id、RuleName、Scope、CategoryName、ProjectTag、Color。
4. Priority、Source、Status、ConditionsJson、Confidence、Explanation。
5. CreatedAt、UpdatedAt。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs",
      "label": "ActivityCategoryRuleEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" }
  ]
}
```
