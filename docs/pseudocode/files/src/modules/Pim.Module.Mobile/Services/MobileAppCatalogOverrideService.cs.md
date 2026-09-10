# src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：当前用户的 App 目录覆盖（package 级）与分类规则 CRUD；在覆盖变更后将相关 usage 聚合与时间线块标为 stale。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、`MobileUserContext`、Mobile 实体/DTO、`MobileAppClassificationService` 规则类型常量、`MobileLifeCategories`
- 被谁使用：`MobileModule` 端点；可选注入 `MobileUsageIngestService`

## 函数级结构化伪代码

### MobileAppCatalogOverrideService
#### 构造注入
- 输入：db、currentUser、timeProvider
- 输出：实例
- 副作用：无
- 步骤：捕获依赖；静态 `SupportedRuleTypes` 含 package-exact/prefix/keyword、display-keyword、package-keyword
- 分支与异常：无
- 调用：无

#### ListOverridesAsync / UpsertOverrideAsync / DeleteOverrideAsync / ClearOverridesAsync
- 输入：可选 Upsert 请求或 packageName
- 输出：覆盖 DTO 列表/单条；删除返回 bool；清空返回删除数
- 副作用：读写 `MobileAppCatalogOverrideEntity` + SaveChanges
- 步骤：
  1. `RequireUserId`；List 按 PackageName 排序投影 ToDto。
  2. Upsert：规范化 package/lifeCategory；按 user+package 查或新建；写 DisplayNameOverride/LifeCategory/IsSystemNoise/HideShortEvents/UpdatedAt。
  3. Delete：规范化 package 后 Remove 或 false。
  4. Clear：拉用户全部 RemoveRange，返回 Count。
- 分支与异常：未登录；空 package/非法 lifeCategory 抛 ArgumentException
- 调用：EF、Normalize*、ToDto

#### ListCategoryRulesAsync / CreateCategoryRuleAsync / UpdateCategoryRuleAsync / DeleteCategoryRuleAsync
- 输入：规则 Upsert 请求或规则 id 字符串
- 输出：规则 DTO 列表/单条；删除 bool
- 副作用：读写 `MobileAppCategoryRuleEntity`
- 步骤：
  1. List：用户规则按 Priority/CreatedAt/Pattern/Id 排序。
  2. Create：规范化 RuleType/Pattern/LifeCategory；写 Priority/IsEnabled/DisplayNameOverride/IsSystemNoise；Add+Save。
  3. Update：ParseRuleId；找不到 KeyNotFoundException；覆盖字段并 UpdatedAt。
  4. Delete：找不到 false，否则 Remove。
- 分支与异常：非法 GUID/规则类型/pattern/category；规则不存在
- 调用：EF、Normalize*

#### MarkAnalyticsStaleAsync(packageName, rangeStartUtc, rangeEndUtc)
- 输入：包名与时间范围
- 输出：`MobileAnalyticsStaleMarkResult(AggregatesMarked, TimelineBlocksMarked)`
- 副作用：将重叠且未 stale 的 `MobileUsageAggregateEntity` 与 `MobileTimelineBlockEntity` 标 IsStale
- 步骤：
  1. end<=start → ArgumentException。
  2. 查 package 与时间窗重叠的聚合，非 stale 则置 true。
  3. 查时间窗重叠的 timeline block；`TopAppsJson` 含 package（忽略大小写）且非 stale 则标记。
  4. 有变更则 Save；返回计数。
- 分支与异常：范围非法；未登录
- 调用：EF

#### 私有 ToDto / ParseRuleId / Normalize* / NullIfBlank
- 输入：实体或字符串
- 输出：DTO 或规范化字符串
- 副作用：无
- 步骤：映射字段；GUID 解析；trim+lower package/pattern；规则类型必须在 SupportedRuleTypes；lifeCategory 必须在 MobileLifeCategories.All；空白字符串变 null
- 分支与异常：ArgumentException
- 调用：无

### MobileAnalyticsStaleMarkResult
#### record(AggregatesMarked, TimelineBlocksMarked)
- 输入/输出：两个 int 计数
- 副作用：无
- 步骤：结果载体
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 静态支持规则类型集合；构造注入 db/currentUser/timeProvider。
2. 覆盖：List 投影；Upsert 按 user+package 新建或更新；Delete/Clear 删除。
3. 分类规则：List 排序；Create/Update 规范化后写库；Delete 按 id。
4. MarkAnalyticsStale：校验范围；标聚合与 TopAppsJson 命中的时间线块。
5. ToDto 与 NormalizePackageName/Pattern/RuleType/LifeCategory；ParseRuleId；NullIfBlank。
6. 文件末尾 record 返回 stale 计数。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs",
      "label": "MobileAppCatalogOverrideService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "to": "src/Pim.Infrastructure/Auth", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "to": "src/modules/Pim.Module.Mobile/Entities", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" }
  ]
}
```
