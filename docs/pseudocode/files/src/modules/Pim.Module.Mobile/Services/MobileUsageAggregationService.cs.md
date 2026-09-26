# src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：从 session/summary 装载用量行，做分类过滤、时区分桶与图表/概览/热力图聚合，并汇总质量与目标进度。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`MobileAnalyticsQueryService`、`MobileUsageGoalService`、`MobileAppClassificationService`、`TimeProvider`、Mobile 实体与 DTO
- 被谁使用：Mobile Analytics API / 查询端点

## 函数级结构化伪代码

### MobileUsageAggregationService
#### 构造(...)
- 输入：db、currentUser、queryService、goalService、timeProvider、可选 classificationService
- 输出：服务实例
- 副作用：无
- 步骤：保存依赖；classification 可空（无则走内置/override 回退路径）
- 调用：无

#### Task\<MobileAnalyticsOverviewResponse\> GetOverviewAsync(request, ct)
- 输入：分析查询请求
- 输出：概览 DTO（总量、日均、峰值日/时、质量、目标、异常、建议）
- 副作用：读 session/summary/batch/goal
- 步骤：
  1. Normalize 上下文；解析时区；LoadRows。
  2. 若未含系统噪声，另加载含噪声质量行用于 quality。
  3. 日/小时分桶，找峰值本地日与小时。
  4. FirstGoalProgress；BuildAnomalies/Suggestions。
  5. 计算 fallback 占比、systemNoise 占比、qualityFlags（含 hidden-system-noise）。
  6. 组装 Overview：含 QualitySummary（FailedBatchCount、LastSyncAt）。
- 调用：`LoadRowsAsync`、`SplitRowsIntoBuckets`、`FailedBatchCountAsync`、`LastSyncAtAsync`

#### Task\<IReadOnlyList\<MobileHeatmapBucketDto\>\> GetHeatmapAsync(request, ct)
- 输入：查询（含 Granularity）
- 输出：按桶+分类聚合的热力点
- 步骤：Normalize；按 15m/30m/day/hour 选 bucketSize；LoadRows 分桶；按 BucketStart/End、LocalDate/Hour、LifeCategory 分组求和
- 调用：`SplitRowsIntoBuckets`

#### Task\<IReadOnlyList\<MobileAnalyticsChartDto\>\> GetChartsAsync(request, ct)
- 输入：查询
- 输出：分类占比、Top App、每日/小时、分类趋势、切换趋势等图表（comparison/goal-marker 空占位）
- 步骤：LoadRows；分组算 ChartPoint；switch-trend 按本地日计行数
- 调用：`ChartPoint`、`SplitRowsIntoBuckets`

#### LoadRowsAsync(context, ct)
- 输入：规范化查询上下文
- 输出：`UsageRow` 列表（events + fallback）
- 副作用：读 session、summary、分类
- 步骤：
  1. 查与时间窗相交的 session；可选 DeviceId/PackageName 过滤。
  2. WhereFallbackSummaries 加载 summary 并同样过滤。
  3. 收集 package；`LoadClassificationsAsync`。
  4. session：裁剪到 range，算秒数，过滤 MinDuration；Source=`events`。
  5. summary：ProratedSeconds；Source=`fallback`；附加 fallback-only 质量标志。
  6. MatchesClassification（噪声/生活分类过滤）。
- 调用：`MobileUsageQueryService.WhereFallbackSummaries`、`QualityFlags`

#### SplitRowsIntoBuckets / SplitRowIntoBuckets / FloorLocalBucket / NextLocalBucket / LocalBucketUtc
- 输入：行、时区、桶大小、粒度
- 输出：按本地桶切分并按重叠毫秒比例分配秒数（末桶吃剩余）
- 分支：非正时长或 end<=start 跳过；无重叠跳过
- 调用：时区 ConvertTime / ConvertTimeToUtc

#### LoadClassificationsAsync
- 输入：userId、包名集合
- 输出：package→Classification 字典
- 步骤：有 classificationService 则 ClassifyAsync；否则 override + catalog + BuiltIn + MapAndroidCategory
- 调用：`MobileAppClassificationService.ClassifyAsync` 或 EF

#### FirstGoalProgressAsync / FailedBatchCountAsync / LastSyncAtAsync
- 输入：已用秒数或查询上下文
- 输出：目标进度 DTO；失败批次数；最近同步时间
- 步骤：List 目标取 total-daily 优先；batch 按 FailedCount/Status 与时间窗统计；CompletedAtUtc ?? CreatedAt 降序
- 调用：`MobileUsageGoalService.ListAsync`、`MobileSyncBatchEntity`

#### MatchesClassification / BuildAnomalies / BuildSuggestions / ChartPoint / QualityFlags
- 输入：行与上下文
- 输出：过滤布尔；异常/建议列表；图表点；质量标志
- 步骤：夜间(>=22 中国时区)、总时长>6h；顶部分类建议；json 含 partial/stale；缺元数据标志
- 调用：`ChinaTimeZone`

#### BuiltIn / MapAndroidCategory / ProratedSeconds / Max/Min / CountLocalDays / LocalDate / FirstNonBlank
- 输入：包名或时间窗/可见毫秒
- 输出：内置分类、Android 映射、按比例秒数、日期计数
- 分支：sourceMs<=0 时直接 totalVisibleMs/1000

### 内部 record：UsageRow / UsageBucketRow / Classification
- 输入/输出：聚合中间结构；Default 未分类

## 近逐行中文伪代码

1. 构造注入查询/目标/时间/可选分类服务。
2. GetOverview：规范化、装行、质量行、分桶峰值、目标/异常/建议、质量摘要与同步批次。
3. GetHeatmap：按粒度分桶后分组 LifeCategory。
4. GetCharts：分类占比、Top10、日趋势、小时分布、分类趋势、切换计数；两空图表占位。
5. LoadRows：session+fallback summary 裁剪过滤，挂分类与质量标志。
6. 本地时区分桶并按重叠比例分配秒数。
7. 分类加载：优先 ClassificationService，否则 override/catalog/内置。
8. 目标进度、失败批次、最近同步；异常与建议启发式。
9. 工具：时区、比例秒、BuiltIn 映射、内部 Usage 行结构。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs",
      "label": "MobileUsageAggregationService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" }
  ]
}
```
