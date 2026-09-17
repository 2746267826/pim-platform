# src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端应用分类规则持久化实体，按用户配置 package 匹配模式、生活分类、展示名覆盖与系统噪声标记。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`Pim.Module.Mobile.DTOs.MobileLifeCategories`
- 被谁使用：Mobile 分类/规则服务、`PimDbContext` 映射表 `mobile_app_category_rules`

## 函数级结构化伪代码

### MobileAppCategoryRuleEntity
#### 属性与默认值
- 输入：无（EF 实体属性）
- 输出：表行字段
- 副作用：无
- 步骤：
  1. 映射表 `mobile_app_category_rules`
  2. `Id` 主键 Guid，默认 `NewGuid()`
  3. `UserId` 所属用户
  4. `RuleType` 最长 64，默认 `"package-exact"`
  5. `Pattern` 最长 512，匹配模式字符串
  6. `LifeCategory` 最长 128，默认 `MobileLifeCategories.Uncategorized`
  7. `DisplayNameOverride` 可选展示名覆盖
  8. `IsSystemNoise` 可选系统噪声标记
  9. `Priority` 默认 100
  10. `IsEnabled` 默认 true
  11. `CreatedAt` / `UpdatedAt` 默认 UTC 现在
- 分支与异常：无运行时逻辑
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema 与 Mobile DTOs
2. 命名空间 `Pim.Module.Mobile.Entities`
3. sealed 类映射 `mobile_app_category_rules`
4. Id、UserId、RuleType、Pattern、LifeCategory
5. DisplayNameOverride、IsSystemNoise、Priority、IsEnabled
6. CreatedAt、UpdatedAt 时间戳

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs",
      "label": "MobileAppCategoryRuleEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs", "type": "depends_on" }
  ]
}
```
