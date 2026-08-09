# src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端用量聚合桶实体（按设备/粒度/时间桶/包名），表 `mobile_usage_aggregates`，支撑分析查询与陈旧标记。
- 主要依赖：`MobileAnalyticsDefaults`、`MobileLifeCategories`（DTO 常量）
- 被谁使用：`MobileUsageAggregationService`、分析查询、`PimDbContext` / `MobileEntityConfigurations`

## 函数级结构化伪代码

### MobileUsageAggregateEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 表名 `mobile_usage_aggregates`；`Id` 默认 NewGuid。
  2. `UserId`、`DeviceId`(128)、`Granularity` 默认 `"hour"`。
  3. `BucketStartUtc`/`BucketEndUtc`；`Timezone` 默认 `MobileAnalyticsDefaults.DefaultTimezone`。
  4. `PackageName`/`DisplayName`；`LifeCategory` 默认 `MobileLifeCategories.Uncategorized`。
  5. `Source` 默认 `"events"`；`ForegroundSeconds`、`SessionCount`、`LaunchCount`、`SwitchOrPickupCount`。
  6. `IsSystemNoise`；`ShortEventSeconds`；`QualityFlagsJson` jsonb 默认 `"[]"`。
  7. `IsStale`；`GeneratedAt`/`CreatedAt`/`UpdatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema 与 Mobile DTOs 常量。
2. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类映射 `mobile_usage_aggregates`。
3. 主键 Id；用户与设备；粒度与 UTC 桶边界；时区默认值。
4. 包名/显示名/生活分类；来源 events；前台秒数与会话/启动/切换计数。
5. 系统噪声、短事件秒数、质量标志 JSON、陈旧位与时间戳。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs",
      "label": "MobileUsageAggregateEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs", "type": "depends_on" }
  ]
}
```
