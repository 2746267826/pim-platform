# src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：将 ActivityWatch 事件解释为浏览器页面时间线：短页合并、浏览器窗口关联、分类、输出 `PcDetailRecord` 列表
- 主要依赖：`System.Text.Json`；`AwEventEntity`、`ActivityCategoryRuleEntity`、`PcDetailRecord`；`ActivityClassifier`、`AppNameNormalizer`
- 被谁使用：PcTracker 详情/时间线构建管线

## 函数级结构化伪代码

### BrowserPageTimelineBuilder（static）
#### 常量与字典
- ShortPageThresholdSeconds = 5；MaxShortPageMergeGapSeconds = 30
- BrowserAppNames：msedge/chrome/firefox/brave/opera（忽略大小写）
- BrowserBucketTokens：edge/msedge→msedge，chrome/firefox/brave/opera 映射

#### `public static List<PcDetailRecord> BuildInterpretedAwRecords(awEvents, rules)`
- 输入：AW 事件列表、分类规则
- 输出：按 Start 字符串序排序的解释后明细记录
- 副作用：无（纯计算）
- 步骤：
  1. 按 DeviceId 分组
  2. 组内按 Timestamp、SourceEventId/Id 排序
  3. 筛 web 事件再排序；`BuildWebPageClusters` → `ToDetailPage` 过滤 null
  4. 收集已解释的 BrowserWindow 到引用相等 HashSet
  5. 非 web 且非已解释浏览器窗口 → `ToRawAwRecord`
  6. 合并 web-page 记录与 nonWeb 记录；全局 OrderBy Start
- 分支与异常：ToDetailPage 可因竞争窗口所有权返回 null
- 调用：`IsWebEvent`、`BuildWebPageClusters`、`ToRawAwRecord`

#### `public static PcDetailRecord ToRawAwRecord(e, rules)`
- 输入：单条 AW 事件、规则
- 输出：原始解释 `PcDetailRecord`
- 副作用：无
- 步骤：
  1. 规范化 AppName；web 则 ParseWebData
  2. recordType = web 或 e.EventType
  3. ActivityClassifier.Classify（web 上下文含 domain/path/title 等）
  4. 构造 PcDetailRecord：起止 O 格式 UTC、Duration、设备、应用、分类、窗口标题、DataJson 对象、URL 字段、SourceWeb/Window Ids、分类元数据、BucketType、SourceBucketIds、SourceType（无 SourceEventId 或空 BucketId → fallback 否则 aw）、InterpretationVersion=interpreted-aw-v1
- 分支与异常：无
- 调用：`AppNameNormalizer`、`ParseWebData`、`ActivityClassifier`、`FormatUtc`、`SourceIds`、`SourceBucketIds`

#### `private static BuildWebPageClusters(webEvents, rules)`
- 输入：有序 web 事件
- 输出：`WebPageCluster` 列表
- 副作用：无
- 步骤：
  1. 遍历：Duration≤5 的短事件进入 pending
  2. 若 pending 非空且与上一条不够近（>30s 间隙）→ AttachTrailing 到已有 clusters 并清空 pending
  3. 长事件：TakeAdjacentShortSuffix 作为 leading；剩余 pending AttachTrailing；新建 cluster(Primary=长事件, leading, empty trailing)
  4. 循环结束：pending 有且已有 cluster → AttachTrailing；仅有 pending → FromShortEvents（以最后一条为 Primary）
- 分支与异常：无
- 调用：`AttachTrailingShortEvents`、`TakeAdjacentShortSuffix`、`IsNearEnoughToMerge`

#### `AttachTrailingShortEvents` / `TakeAdjacentShortSuffix` / `ClusterEnd` / `EventEnd` / `IsNearEnoughToMerge`
- Attach：cluster 空或 short 空则 return；与上一 cluster 尾不够近 return；否则 with 合并 Trailing（DistinctBy 引用、按时间排序）
- TakeAdjacent：从 short 末尾向前，与 cursor 够近则纳入并 cursor=短事件开始；反转返回
- ClusterEnd：leading+primary+trailing 的最大 EventEnd
- EventEnd：Timestamp+Duration 秒
- IsNearEnough：nextStart≤previousEnd 或间隙秒 ≤30

#### 嵌套 `WebPageCluster` record
##### `FromShortEvents`
- 以 shortEvents 最后一条为 Primary，全部作 Leading，Trailing 空

##### `ToDetailPage(awEvents)`
- 输入：同设备全部 awEvents
- 输出：`WebPageDetail?`（Record + BrowserWindow）
- 步骤：
  1. 合并 leading/primary/trailing 去重排序 → start/end
  2. ParseWebData(Primary)；InferBrowserName
  3. 找与时间重叠的浏览器 window 事件；优先匹配 browserName 规范化名，按重叠秒降序再时间升序；否则任意重叠窗口
  4. displayName = Domain 或本地文件「文件」
  5. 若无 browserWindow 且存在非浏览器 window 重叠 → return null（竞争所有权）
  6. 统计吸收的短事件数与时长和
  7. Classify recordType=web-page
  8. 构造 PcDetailRecord（web-page、起止、时长、设备、displayName、分类、标题、URL 字段、浏览器 App/标题、audible/incognito/tabCount、短事件统计、SourceIds、分类字段、SourceType=aw、interpreted-aw-v1）
  9. 返回 WebPageDetail
- 调用：`ParseWebData`、`InferBrowserName`、`IsBrowserWindowEvent`、`Overlaps`、`OverlapSeconds`、`HasCompetingWindowOwnership`、`ActivityClassifier`

#### 判定与推断
- `IsWebEvent`：EventType==web 或 BucketType==web.tab.current
- `IsBrowserWindowEvent`：EventType==window 且规范化 App 在 BrowserAppNames
- `HasCompetingWindowOwnership`：存在非浏览器、有 AppName 的 window 与 [start,end] 重叠秒>0
- `InferBrowserName`：从事件集合/单事件 BucketId 或 BucketClient 子串匹配 BrowserBucketTokens

#### 时间几何与 JSON
- Overlaps：半开式区间相交；OverlapSeconds：相交长度秒
- ParseWebData：DataJson → url/title/audible/incognito/tabCount；Uri 解析 Host/Path；本地文件 Domain null、Path=LocalPath
- TryParseJson/GetString/GetBool/GetInt：容错解析
- SourceIds：非空 SourceEventId 列表；SourceBucketIds：非空 BucketId 去重排序
- FormatUtc：UniversalTime "O"
- ParseRecordTime：存在但本文件未在公开路径调用

#### 嵌套 `WebPageData` / `WebPageDetail`
- WebPageData：Url/Domain/Path/IsLocalFile/Title/Audible/Incognito/TabCount
- WebPageDetail：Record + 可选 BrowserWindow

## 近逐行中文伪代码

1. 引入 Json、PcTracker DTOs/Entities；static 类
2. 短页阈值 5s、合并间隙 30s；浏览器应用名与 bucket 词元映射
3. BuildInterpretedAwRecords：按设备分组排序 → 建网页簇 → 解释页 → 收集已解释浏览器窗 → 其余 raw → 合并排序
4. ToRawAwRecord：规范化应用、解析 web 数据、分类、填 PcDetailRecord（含 fallback/aw SourceType）
5. BuildWebPageClusters：短事件 pending 合并；长事件带 leading 短事件成簇；尾部 short 挂前簇或自成簇
6. AttachTrailing/TakeAdjacent/ClusterEnd/EventEnd/IsNearEnough：短事件时间邻接合并
7. WebPageCluster.ToDetailPage：算起止、推断浏览器、选重叠 window、竞争 window 则丢弃、分类、输出 web-page 记录
8. IsWeb/IsBrowserWindow/HasCompeting/InferBrowserName：事件类型与浏览器识别
9. Overlaps/OverlapSeconds：时间重叠
10. ParseWebData 与 JSON 辅助：url/domain/path/本地文件/标题/可听/无痕/标签数
11. SourceIds/SourceBucketIds/FormatUtc：源追踪与时间格式
12. WebPageData/WebPageDetail 内部 record

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs",
      "label": "BrowserPageTimelineBuilder",
      "path": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs", "type": "calls" }
  ]
}
```
