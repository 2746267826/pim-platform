# src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端时间线块持久化实体，映射表 `mobile_timeline_blocks`，缓存聚合后的时间段用量块
- 主要依赖：`System.ComponentModel.DataAnnotations`、`MobileAnalyticsDefaults`、`MobileLifeCategories`
- 被谁使用：Mobile 时间线构建/查询服务、EF `PimDbContext`

## 函数级结构化伪代码

### MobileTimelineBlockEntity
#### 属性映射（无业务方法）
- 输入：无（POCO 属性读写）
- 输出：表行字段
- 副作用：无（由 EF 跟踪）
- 步骤：
  1. 主键 `Id`：Guid，默认 `NewGuid`
  2. 归属：`UserId`、`DeviceId`（≤128）
  3. 时间：`StartUtc`/`EndUtc`；`LocalDate`（≤10）；`Timezone` 默认 `MobileAnalyticsDefaults.DefaultTimezone`
  4. 分类：`LifeCategory` 默认 `MobileLifeCategories.Uncategorized`
  5. 聚合：`ForegroundSeconds`、`SessionCount`、`AppCount`
  6. JSONB：`TopAppsJson` 默认 `[]`；`SourceMixJson` 默认 `{}`；`QualityFlagsJson` 默认 `[]`
  7. 标志：`IncludesSystemNoise`、`IsStale`
  8. 时间戳：`GeneratedAt`/`CreatedAt`/`UpdatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：DTO 默认常量

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Mobile DTOs
2. 命名空间 `Pim.Module.Mobile.Entities`
3. 表名 `mobile_timeline_blocks`；sealed class
4. `id` Guid 主键默认新 Guid
5. `user_id`、`device_id`（MaxLength 128 默认空串）
6. `start_utc`、`end_utc`
7. `local_date` MaxLength 10；`timezone` MaxLength 64 默认 Asia/Shanghai
8. `life_category` MaxLength 128 默认「未分类」
9. `foreground_seconds` long；`session_count`/`app_count` int
10. `top_apps_json` jsonb 默认 `[]`；`source_mix_json` jsonb 默认 `{}`；`quality_flags_json` jsonb 默认 `[]`
11. `includes_system_noise`、`is_stale` bool
12. `generated_at`/`created_at`/`updated_at` 默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs",
      "label": "MobileTimelineBlockEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs", "type": "depends_on" }
  ]
}
```
