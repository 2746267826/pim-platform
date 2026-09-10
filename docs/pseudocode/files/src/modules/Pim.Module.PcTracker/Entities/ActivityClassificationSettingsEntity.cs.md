# src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类全局设置实体（表 `pc_activity_classification_settings`），如推荐最小分类时长。
- 主要依赖：
  - System.ComponentModel.DataAnnotations / Schema
- 被谁使用：PcTracker 分类配置服务、`PimDbContext`

## 函数级结构化伪代码

### ActivityClassificationSettingsEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 主键 `Id`（Guid）。
  2. `SettingsKey` 默认 `"default"`，MaxLength 64。
  3. `RecommendedMinimumClassificationDurationMinutes` 默认 5。
  4. `CreatedAt` / `UpdatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`；表 `pc_activity_classification_settings`。
2. id、settings_key、recommended_minimum_classification_duration_minutes、created_at、updated_at。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs",
      "label": "ActivityClassificationSettingsEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs", "type": "depends_on" }
  ]
}
```
