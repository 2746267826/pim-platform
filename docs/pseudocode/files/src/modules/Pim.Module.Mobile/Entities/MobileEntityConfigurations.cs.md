# src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：为 Mobile 模块全部 EF 实体提供 `IEntityTypeConfiguration`：默认值、精度、唯一/查询索引。
- 主要依赖：`Microsoft.EntityFrameworkCore`、`EntityTypeBuilder`、`Pim.Module.Mobile.DTOs`（`MobileLifeCategories`、`MobileAnalyticsDefaults`）
- 被谁使用：EF 模型构建时自动应用（`ApplyConfigurationsFromAssembly` 或模块注册）；对应实体表由 `PimDbContext` 暴露

## 函数级结构化伪代码

### MobileDeviceEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileDeviceEntity\> builder)
- 输入：设备实体类型构建器
- 输出：无
- 副作用：配置默认值与索引
- 步骤：
  1. `MetadataJson` 默认 `"{}"`
  2. `CreatedAt`/`UpdatedAt` 默认 SQL `now()`
  3. 唯一索引 `(UserId, DeviceId)`；查询索引 `(UserId, LastSeenAtUtc)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileAppCatalogEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileAppCatalogEntity\> builder)
- 输入：应用目录实体构建器
- 输出：无
- 副作用：配置默认值与唯一索引
- 步骤：
  1. `RawJson` 默认 `"{}"`；时间戳 `now()`
  2. 唯一索引 `(UserId, DeviceId, PackageName)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileUsageEventEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileUsageEventEntity\> builder)
- 输入：使用事件实体构建器
- 输出：无
- 副作用：配置默认值、唯一与时间索引
- 步骤：
  1. `RawJson` 默认 `"{}"`；`QualityFlagsJson` 默认 `"[]"`；`CreatedAt`=`now()`
  2. 唯一索引 `(UserId, DeviceId, PackageName, EventType, EventTimestampUtc, ClassName)`
  3. 查询索引 `(UserId, DeviceId, EventTimestampUtc)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileUsageSummaryEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileUsageSummaryEntity\> builder)
- 输入：使用摘要实体构建器
- 输出：无
- 副作用：配置默认值与索引
- 步骤：
  1. `RawJson`/`QualityFlagsJson` 默认；`CreatedAt`/`UpdatedAt`=`now()`
  2. 唯一索引 `(UserId, DeviceId, PackageName, WindowStartUtc, WindowEndUtc, SourceKind)`
  3. 查询索引 `(UserId, DeviceId, WindowStartUtc)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileUsageSessionEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileUsageSessionEntity\> builder)
- 输入：使用会话实体构建器
- 输出：无
- 副作用：配置默认值与双查询索引
- 步骤：
  1. `QualityFlagsJson` 默认 `"[]"`；`CreatedAt`=`now()`
  2. 索引 `(UserId, DeviceId, StartUtc)` 与 `(UserId, PackageName, StartUtc)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileLocationPointEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileLocationPointEntity\> builder)
- 输入：定位点实体构建器
- 输出：无
- 副作用：配置数值精度、默认质量与索引
- 步骤：
  1. 经纬度/精度/速度/方位等字段设 `HasPrecision`
  2. `Quality` 默认 `"usable"`；`RawJson` 默认 `"{}"`；`CreatedAt`=`now()`
  3. 索引 `(UserId, DeviceId, RecordedAtUtc)` 与 `(UserId, Quality, RecordedAtUtc)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileSyncBatchEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileSyncBatchEntity\> builder)
- 输入：同步批次实体构建器
- 输出：无
- 副作用：配置状态默认与唯一批次索引
- 步骤：
  1. `Status` 默认 `"completed"`；`ErrorJson` 默认 `"{}"`；`CreatedAt`=`now()`
  2. 唯一索引 `(UserId, DeviceId, BatchId)`；查询索引 `(UserId, DeviceId, CreatedAt)`
- 分支与异常：无
- 调用：EF Fluent API

### MobileAppCatalogOverrideEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileAppCatalogOverrideEntity\> builder)
- 输入：应用目录覆盖实体构建器
- 输出：无
- 副作用：配置分类默认与索引
- 步骤：
  1. `LifeCategory` 默认 `MobileLifeCategories.Uncategorized`；时间戳 `now()`
  2. 唯一 `(UserId, PackageName)`；索引按 `LifeCategory`、`IsSystemNoise`
- 分支与异常：无
- 调用：EF Fluent API、`MobileLifeCategories`

### MobileAppCategoryRuleEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileAppCategoryRuleEntity\> builder)
- 输入：应用分类规则实体构建器
- 输出：无
- 副作用：配置规则默认值与索引
- 步骤：
  1. `RuleType` 默认 `"package-exact"`；`LifeCategory` 未分类；`Priority`=100；`IsEnabled`=true
  2. 唯一 `(UserId, RuleType, Pattern)`；索引 `(UserId, IsEnabled, Priority)` 与按分类
- 分支与异常：无
- 调用：EF Fluent API、`MobileLifeCategories`

### MobileUsageAggregateEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileUsageAggregateEntity\> builder)
- 输入：使用聚合实体构建器
- 输出：无
- 副作用：配置分析默认值与多维唯一/查询索引
- 步骤：
  1. 默认：`DeviceId`/`PackageName`/`DisplayName` 空串；`Granularity`=`"hour"`；时区默认；分类未分类；`Source`=`"events"`；`QualityFlagsJson`=`"[]"`；生成/创建/更新时间 `now()`
  2. 唯一索引：用户+设备+粒度+桶起止+包名+生活分类
  3. 查询索引：按桶开始、分类、包名、`IsStale`
- 分支与异常：无
- 调用：EF Fluent API、`MobileAnalyticsDefaults`、`MobileLifeCategories`

### MobileTimelineBlockEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileTimelineBlockEntity\> builder)
- 输入：时间线块实体构建器
- 输出：无
- 副作用：配置 JSON/时区默认与索引
- 步骤：
  1. 时区默认；分类未分类；`TopAppsJson`=`"[]"`；`SourceMixJson`=`"{}"`；质量标志/时间戳默认
  2. 索引：设备+StartUtc、分类+StartUtc、LocalDate、IsStale
- 分支与异常：无
- 调用：EF Fluent API、`MobileAnalyticsDefaults`、`MobileLifeCategories`

### MobileUsageGoalEntityConfiguration
#### void Configure(EntityTypeBuilder\<MobileUsageGoalEntity\> builder)
- 输入：使用目标实体构建器
- 输出：无
- 副作用：配置目标默认与唯一作用域
- 步骤：
  1. `Scope` 默认 `"total-daily"`；`Label` 默认中文「每日手机总时长」；时区默认；`IsEnabled`=true
  2. 唯一 `(UserId, Scope, PackageName, LifeCategory)`；索引启用态/分类/包名
- 分支与异常：无
- 调用：EF Fluent API、`MobileAnalyticsDefaults`

## 近逐行中文伪代码

1. 引入 EF Core 与 Mobile DTOs 常量
2. 命名空间 `Pim.Module.Mobile.Entities`
3. `MobileDeviceEntityConfiguration`：MetadataJson/`now()`、唯一设备、LastSeen 索引
4. `MobileAppCatalogEntityConfiguration`：RawJson/时间、唯一包名
5. `MobileUsageEventEntityConfiguration`：Raw/质量 JSON、六元组唯一、时间索引
6. `MobileUsageSummaryEntityConfiguration`：窗口+SourceKind 唯一
7. `MobileUsageSessionEntityConfiguration`：设备/包名两条 Start 索引
8. `MobileLocationPointEntityConfiguration`：经纬度等精度、Quality=usable
9. `MobileSyncBatchEntityConfiguration`：Status=completed、BatchId 唯一
10. `MobileAppCatalogOverrideEntityConfiguration`：覆盖分类与系统噪声索引
11. `MobileAppCategoryRuleEntityConfiguration`：package-exact、Priority 100
12. `MobileUsageAggregateEntityConfiguration`：小时粒度聚合唯一键与多查询索引
13. `MobileTimelineBlockEntityConfiguration`：时间线块 JSON 默认与索引
14. `MobileUsageGoalEntityConfiguration`：每日总时长目标默认与唯一 Scope
15. （各配置类均仅实现 `Configure`，无运行时业务逻辑）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs",
      "label": "MobileEntityConfigurations",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs", "to": "Microsoft.EntityFrameworkCore.IEntityTypeConfiguration", "type": "implements" },
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs", "type": "depends_on" }
  ]
}
```
