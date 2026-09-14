# src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端使用时长目标实体（按用户、范围、包名或生活分类设定日限额）。
- 主要依赖：`Pim.Module.Mobile.DTOs.MobileAnalyticsDefaults`、DataAnnotations
- 被谁使用：Mobile 分析/目标相关服务、EF 映射与迁移

## 函数级结构化伪代码

### MobileUsageGoalEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层赋值后由 EF 持久化
- 输出：`mobile_usage_goals` 表一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表名 `mobile_usage_goals`；主键 `Id` 默认 NewGuid。
  2. `UserId` 归属用户。
  3. `Scope` 默认 `"total-daily"`（最长 64）。
  4. 可选限定：`PackageName`、`LifeCategory`。
  5. `Label` 默认 `"每日手机总时长"`；`LimitSeconds` 限额秒数。
  6. `Timezone` 默认 `MobileAnalyticsDefaults.DefaultTimezone`。
  7. `IsEnabled` 默认 true；`CreatedAt`/`UpdatedAt` 默认 UtcNow。
- 分支与异常：本类型无校验逻辑
- 调用：被 Mobile 目标/分析服务读写

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`Pim.Module.Mobile.DTOs`。
2. sealed 类映射 `mobile_usage_goals`。
3. Id、UserId、Scope 默认 total-daily。
4. PackageName、LifeCategory 可空；Label 默认中文总时长文案。
5. LimitSeconds；Timezone 取 MobileAnalyticsDefaults.DefaultTimezone。
6. IsEnabled 默认 true；CreatedAt/UpdatedAt 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs",
      "label": "MobileUsageGoalEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs", "type": "depends_on" }
  ]
}
```
