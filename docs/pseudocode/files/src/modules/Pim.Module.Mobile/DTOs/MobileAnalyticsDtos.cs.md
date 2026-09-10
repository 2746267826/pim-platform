# src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端用量分析的默认常量、生活分类枚举值、查询/概览/热力图/图表/时间线/会话/目录覆盖/分类规则/用量目标等 DTO 契约
- 主要依赖：无外部类型依赖（纯 DTO 与静态常量）
- 被谁使用：Mobile 分析服务、端点、时间线实体默认值、用量目标服务等

## 函数级结构化伪代码

### MobileAnalyticsDefaults
#### 静态常量与 `LifeCategories`
- 输入：无
- 输出：默认时区 `Asia/Shanghai`、分页 50/最大 200、短事件阈值 1 秒；`LifeCategories` 转发 `MobileLifeCategories.All`
- 副作用：无
- 步骤：暴露分析查询与展示的默认配置
- 分支与异常：无
- 调用：`MobileLifeCategories.All`

### MobileLifeCategories
#### 分类常量与 `All` 数组
- 输入：无
- 输出：16 个中文生活分类字符串及完整列表
- 副作用：无
- 步骤：定义 Social…Uncategorized 常量；`All` 按固定顺序聚合
- 分支与异常：无
- 调用：无

### MobileAnalyticsRangeDto
#### record 时间范围
- 输入：RangeStartUtc、RangeEndUtc、Timezone、LocalStartDate、LocalEndDate
- 输出：分析窗口描述
- 副作用：无
- 步骤：绑定 UTC 与本地日期字符串
- 分支与异常：无
- 调用：无

### MobileAnalyticsQueryRequest
#### record 查询请求（均可选）
- 输入：时间窗、时区、设备、分类、包名、来源、是否含系统噪声、最短时长、粒度、游标、页码、页大小
- 输出：原始查询入参
- 副作用：无
- 步骤：构造可选过滤与分页字段
- 分支与异常：无
- 调用：无

### MobileAnalyticsQueryContext
#### record 已解析查询上下文
- 输入：Range、DeviceId、LifeCategory、PackageName、Source、IncludeSystemNoise、MinDurationSeconds、Granularity、Cursor、Page、PageSize
- 输出：服务层使用的规范化查询上下文
- 副作用：无
- 步骤：由 Request 解析后填充必填布尔/数值/分页
- 分支与异常：无
- 调用：`MobileAnalyticsRangeDto`

### MobileAnalyticsQualitySummaryDto
#### record 数据质量摘要
- 输入：覆盖率、回退占比、缺元数据应用数、系统噪声占比、短事件占比、失败/部分同步批次数、最后同步时间、质量标志列表
- 输出：质量面板数据
- 副作用：无
- 步骤：聚合同步与事件质量指标
- 分支与异常：无
- 调用：无

### MobileGoalProgressDto / MobileAnomalyDto / MobileSuggestionDto
#### record 目标进度、异常、建议
- 输入：Key/Label/限额与已用秒；Code/Severity/Title/Evidence/Drilldown；Code/Text/Drilldown
- 输出：概览附属结构
- 副作用：无
- 步骤：承载目标达成、异常告警与行动建议
- 分支与异常：无
- 调用：无

### MobileAnalyticsOverviewResponse
#### record 分析概览响应
- 输入：Range、GeneratedAt、IsStale、总前台秒、日均、环比变化、最高用量日、峰值小时、应用数、切换/拿起次数、完整度、Quality、GoalProgress、Anomalies、Suggestions
- 输出：概览 API 主体
- 副作用：无
- 步骤：组合统计核心与质量/目标/异常/建议
- 分支与异常：无
- 调用：Range、Quality、GoalProgress、Anomaly、Suggestion DTO

### MobileHeatmapBucketDto / MobileAnalyticsChartPointDto / MobileAnalyticsChartDto
#### record 热力桶与图表
- 输入：桶起止 UTC、本地日/时、分类、前台秒、质量标志；点 Key/Label/Value 及可选维度；图 Key/Title/ChartType/Unit/Points
- 输出：热力图与通用图表结构
- 副作用：无
- 步骤：定义可视化数据形状
- 分支与异常：无
- 调用：Chart 依赖 ChartPoint

### MobileTimelineBlockAppDto / MobileTimelineBlockDto / MobileTimelineBlockPageDto
#### record 时间线块与分页
- 输入：包名/显示名/秒；块 Id/起止/本地时间/分类/会话数/应用数/TopApps/质量/来源混合/系统噪声；Items+游标+分页元数据
- 输出：时间线块列表与分页包装
- 副作用：无
- 步骤：块内 TopApps；分页含 NextCursor/HasMore/TotalCount/TotalPages
- 分支与异常：无
- 调用：Block 依赖 AppDto；Page 依赖 BlockDto

### MobileTimelineBlockSessionDto / MobileSessionEventDto
#### record 块内会话与会话事件
- 输入：会话 Id/设备/包名/显示名/起止/时长/分类/来源/置信度/质量；事件 Id/SessionId/设备/包名/类型/时间/类名/RawJson
- 输出：下钻会话与原始事件
- 副作用：无
- 步骤：会话级与事件级明细契约
- 分支与异常：无
- 调用：无

### MobileAppCatalogOverrideDto / UpsertRequest
#### record 应用目录覆盖
- 输入：PackageName、DisplayNameOverride、LifeCategory、IsSystemNoise、HideShortEvents、可选时间戳
- 输出：覆盖读写 DTO
- 副作用：无
- 步骤：Upsert 不含时间戳；Dto 可带 CreatedAt/UpdatedAt
- 分支与异常：无
- 调用：无

### MobileAppCategoryRuleDto / UpsertRequest
#### record 分类规则
- 输入：Id/RuleType/Pattern/LifeCategory/Priority/IsEnabled/可选 DisplayNameOverride/IsSystemNoise/时间戳
- 输出：规则列表与写入请求
- 副作用：无
- 步骤：规则匹配配置契约
- 分支与异常：无
- 调用：无

### MobileUsageGoalDto / MobileUsageGoalUpsertRequest
#### record 用量目标
- 输入：Id/Scope/PackageName/LifeCategory/Label/LimitSeconds/IsEnabled/时间戳；Upsert 无 Id/时间
- 输出：目标 CRUD 契约
- 副作用：无
- 步骤：按 Scope+包名+分类维度定义限额
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Mobile.DTOs`
2. `MobileAnalyticsDefaults`：DefaultTimezone=Asia/Shanghai；DefaultPageSize=50；MaxPageSize=200；DefaultShortEventThresholdSeconds=1；LifeCategories 指向分类 All
3. `MobileLifeCategories`：社交通讯、短视频/娱乐、游戏、音乐/音频、阅读/资讯、学习、工作/生产力、工具/系统、浏览器/搜索、出行/地图、购物/外卖、金融/支付、健康/运动、相机/创作、生活服务、未分类；All 数组按此顺序
4. `MobileAnalyticsRangeDto`：UTC 起止 + 时区 + 本地起止日期字符串
5. `MobileAnalyticsQueryRequest`：可选时间窗/过滤/粒度/游标/分页
6. `MobileAnalyticsQueryContext`：解析后 Range 必填；IncludeSystemNoise/MinDurationSeconds/Granularity/Page/PageSize 具体化
7. `MobileAnalyticsQualitySummaryDto`：覆盖、回退、缺元数据、噪声、短事件、同步失败批、LastSyncAt、QualityFlags
8. `MobileGoalProgressDto`：Key/Label/Limit/Used/IsOverLimit/Remaining
9. `MobileAnomalyDto`：Code/Severity/Title/Evidence/DrilldownTarget
10. `MobileSuggestionDto`：Code/Text/DrilldownTarget
11. `MobileAnalyticsOverviewResponse`：概览核心指标 + Quality + 可选 GoalProgress + Anomalies + Suggestions
12. `MobileHeatmapBucketDto`：桶时间、本地日/时、分类、前台秒、质量标志
13. `MobileAnalyticsChartPointDto` / `MobileAnalyticsChartDto`：通用图表点与图
14. `MobileTimelineBlockAppDto` / `MobileTimelineBlockDto` / `MobileTimelineBlockPageDto`：时间线块、TopApps、游标分页
15. `MobileTimelineBlockSessionDto` / `MobileSessionEventDto`：会话与事件下钻
16. `MobileAppCatalogOverride*` / `MobileAppCategoryRule*`：应用覆盖与分类规则读写
17. `MobileUsageGoalDto` / `MobileUsageGoalUpsertRequest`：用量目标读写

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs",
      "label": "MobileAnalyticsDtos",
      "path": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs.md",
      "layer": "module.mobile",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" }
  ]
}
```
