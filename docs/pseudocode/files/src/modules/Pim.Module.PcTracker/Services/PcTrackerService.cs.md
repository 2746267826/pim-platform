# src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 追踪主服务——Keystats/ActivityWatch 上传幂等入库、业务日摘要/时间线/热力图、完整明细查询、遗留 App 分类与活动分类规则门面。
- 主要依赖：`PimDbContext`、`ActivityClassificationSnapshotService`、`ActivityClassificationSettingsService`、`ActivityTimelineSmoothingService`、`ActivityClassificationRuleService`、`BrowserPageTimelineBuilder`、`AppNameNormalizer`、`KeystatsDeltaCalculator`、PcTracker Entities/DTOs
- 被谁使用：PcTracker 模块 HTTP 端点 / API 控制器

## 函数级结构化伪代码

### PcTrackerService
#### 构造(db, classificationSnapshots, classificationSettings, timelineSmoothing, classificationRules?)
- 输入：Db 与分类相关服务；rules 可空则 `new ActivityClassificationRuleService(db)`
- 输出：实例；缓存字段 `_cachedLegacyRules` / `_cachedActivityRules` 初始 null
- 副作用：无
- 调用：可选 new RuleService

#### Task UpsertKeystatsAsync(KeystatsUploadRequest, ct)
- 输入：日汇总上传
- 输出：无
- 副作用：按 DeviceId+SnapshotDate 删旧含子表后整行插入
- 步骤：解析 Date；Include KeyCounts/AppBreakdowns 找既有→RemoveRange+Remove；Add 新 Daily 与子项；Save
- 调用：EF

#### Task\<int\> UploadAwEventsAsync(AwEventsUploadRequest, ct)
- 输入：简化 AW 事件列表
- 输出：新插入条数
- 副作用：去重后 AddRange
- 步骤：映射实体；时间窗内已有键 `MakeAwEventKey`；只插入新键；Save
- 分支：空列表返回 0

#### Task\<int\> UploadCompleteAwEventsAsync(CompleteAwUploadRequest, ct)
- 输入：完整 bucket+events（上限 500）
- 输出：新插入条数
- 分支：超限 ArgumentException；唯一冲突由 Core 重试
- 调用：`UploadCompleteAwEventsCoreAsync(attempt:0)`

#### Task UpsertKeystatsSampleAsync(KeystatsSampleUploadRequest, ct)
- 输入：分钟级采样
- 输出：无
- 步骤：解析 SampledAt/Date；TruncateToMinute；Core upsert
- 分支：时间戳非法抛 ArgumentException
- 调用：`UpsertKeystatsSampleCoreAsync`

#### UpsertKeystatsSampleCoreAsync(req, sampledAtUtc, statsDate, offsetMinutes, ct, attempt)
- 副作用：按 (PimDeviceId, SampledAtUtc) upsert JSON 字段
- 步骤：找既有或 new；写指标与 KeyCountsJson/AppStatsJson/RawJson；Save
- 分支：attempt==0 且唯一冲突 → Clear ChangeTracker 后 attempt=1 重试
- 调用：`IsUniqueViolation`、`ToJson`/`ToApiJson`

#### UploadCompleteAwEventsCoreAsync(req, ct, attempt)
- 副作用：upsert AwBucket；按 SourceEventId 更新/插入 AwEvent
- 步骤：
  1. 找/建 bucket，更新元数据 SeenAt。
  2. 加载同 device+bucket 已有 sourceIds。
  3. 每事件：解析 timestamp；抽 app/title/status；无则新建并 inserted++；写 DataJson、EventType=ClassifyAwEventType、Normalized app 等。
  4. Save；唯一冲突重试一次。
- 调用：`AppNameNormalizer.Normalize`、`ClassifyAwEventType`、`GetString`

#### GetSummaryAsync(date, ct)
- 输入：业务日 date
- 输出：`PcSummaryResponse`（keystats、热力、App 排行、时间线、会话、派生指标、分类汇总）
- 步骤：
  1. 业务日 [4:00 local→UTC, +1d)；LatestKeystats 或 Sample 回退。
  2. 拉日窗 AW 事件；热力用 window；时间线=解释后记录→过滤→ToTimeline→Normalize→Smooth。
  3. 组装 summary。
- 调用：`BuildInterpretedAwDetailRecordsAsync`、`_timelineSmoothing.Smooth`、`GetCategorySummariesAsync`

#### GetTimelineAsync / GetHeatmapAsync / GetKeystatsRangeAsync
- 输入：日期或区间
- 输出：时间线项 / 多日热力桶 / 日 keystats 列表
- 步骤：与 summary 类似的解释+平滑；按日叠加小时热力；Include 子表范围查询
- 调用：同上辅助

#### GetCategorySummariesAsync(date, ct)
- 输入：日期
- 输出：Top5 分类占比（基于 keystats AppBreakdowns + 遗留规则）
- 步骤：无 breakdown 返回空；ClassifyApp 累计 keys+clicks；算百分比
- 调用：`GetLegacyCategoryRulesAsync`

#### QueryDetailAsync(DetailQueryParams, ct)
- 输入：分页/过滤/排序
- 输出：日级 Keystats 字典项分页
- 步骤：过滤 DateFrom/To/DeviceId/AppName/KeyName；ApplyDetailSort；Skip/Take；投影字典
- 调用：EF

#### QueryCompleteDetailAsync(DetailQueryParams, ct)
- 输入：完整明细查询
- 输出：`TypedDetailQueryResponse`（PcDetailRecord 分页）
- 步骤：
  1. 业务日范围拉 AW + KeystatsSample；加载活动规则。
  2. raw/web 视图：ToRawAwRecord；否则 BuildInterpretedAwRecords + ToInputMinuteRecords。
  3. 预分类过滤 EventType → EnsureClassificationsAsync → 完整过滤/排序 → 分页。
- 调用：`BrowserPageTimelineBuilder`、`_classificationSnapshots`、`GetActivityCategoryRulesAsync`

#### GetAllCategoriesAsync / SaveCategoryAsync / DeleteCategoryAsync
- 输入：遗留 AppCategory 规则
- 输出：列表/保存结果/是否删除
- 副作用：upsert AppCategoryEntity；删非 builtin；清规则缓存
- 调用：EF

#### GetActivityClassificationRulesAsync / SaveActivityClassificationRuleAsync
- 输入：无 / Save 请求
- 输出：DTO 列表或新建 DTO
- 副作用：Save 清 `_cachedActivityRules`
- 调用：`_classificationRules.ListAsync` / `SaveAsync`

#### GetHeatmapGridAsync(start, end, dimension, ct)
- 输入：区间与 dimension（hour|其它）
- 输出：网格热力与 maxKeyCount
- 步骤：hour 维按 24 小时分摊 keyPresses；否则按日拼周行
- 调用：KeystatsDaily + window 事件

#### LatestKeystatsForDate / LatestKeystatsSampleForDate
- 步骤：按日取最新 CreatedAt / SampledAtUtc

#### GetBusinessDayStartForQuery / BusinessDayStart
- 步骤：本地 date 04:00 转 UTC（BusinessDayStartHour=4）

#### ApplyDetailSort / GetDetailQueryRange
- 步骤：keyPresses/totalClicks/date 排序；DateFrom/To 默认 Today 转业务日窗

#### ToAwDetailRecord / ToInputMinuteRecords
- 步骤：遗留规则分类；相邻 sample 算 KeystatsDelta 与键计数差生成 input-minute 记录
- 调用：`KeystatsDeltaCalculator`、`ClassifyApp`

#### ApplyCompleteDetailFilters / ApplyPreClassification... / ApplyCompleteDetailSort
- 步骤：EventType/Device/App/Category/Key/Domain/Title/Url 过滤；按 Start 排序

#### ParseJsonObject / ParseKeyCounts / ParseAppStats / CalculateKeyCountDeltas / ContainsIgnoreCase
- 步骤：安全反序列化；键计数 delta 非负

#### BuildKeystatsSummary / FromSample / BuildAppRanking / FromSample / BuildHourlyHeatmap
- 步骤：汇总点击/峰值/Top 键；App 排行强度；每小时 activeMinutes→intensity 0-5

#### BuildInterpretedAwDetailRecordsAsync
- 步骤：活动规则 → BuildInterpretedAwRecords → EnsureClassificationsAsync

#### IsSummaryTimelineRecord / ToTimelineItem / NormalizeTimelineItems
- 步骤：仅 web-page 与有 App 的 window；映射 TimelineItem；重叠时裁剪前一段 end

#### GetLegacyCategoryRulesAsync / GetActivityCategoryRulesAsync
- 步骤：内存缓存；分别 GetAllCategories / LoadActiveAsync

#### ClassifyApp / GetCategoryColor / BuildSessions / MakeSession
- 步骤：精确 AppPattern 匹配否则 Other；>15 分钟间隙切会话；主 App 为计数最大；时长>=5 分钟保留

#### ComputeDerivedMetrics / FormatDuration
- 步骤：记录/输入/空闲时长、会话数、App 切换率、最长 App、键鼠比

#### MakeAwEventKey / ParseOptionalOffset / TryParseTimestamp / TruncateToMinute / TryParseLocalDateOffset
- 步骤：去重键；UTC 解析；截到分钟

#### IsUniqueViolation / EnumerateExceptions / GetStringProperty
- 步骤：反射 SqlState/SQLState == 23505

#### ToJson / ToApiJson / GetString / ClassifyAwEventType / TotalClicks 重载
- 步骤：序列化；字典取值；bucketType→afk/web/window；各实体点击合计

## 近逐行中文伪代码

1. 常量：业务日起 4 点、完整上传 500 条、PG 唯一冲突码。
2. 构造注入快照/设置/平滑/规则服务；规则可内联 new。
3. UpsertKeystats：整日替换；UploadAw 内容键去重；Complete 与 Sample Core 冲突重试。
4. Complete 路径：upsert bucket，按 SourceEventId 幂等写事件并规范化 App。
5. Summary/Timeline：业务日窗解释 AW→分类快照→裁剪重叠→时长平滑。
6. Heatmap/KeystatsRange/CategorySummaries：聚合展示。
7. QueryDetail 日表分页；QueryComplete 解释+input-minute+分类过滤分页。
8. 遗留 AppCategory CRUD 与活动规则 List/Save 门面。
9. HeatmapGrid hour/日周布局。
10. 大量私有辅助：解析、过滤、会话、派生指标、JSON、唯一冲突检测。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs",
      "label": "PcTrackerService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
