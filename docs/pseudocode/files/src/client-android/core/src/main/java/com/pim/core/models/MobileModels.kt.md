# src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.models
- 职责：移动端 API 序列化 DTO：设备注册、缺口回填、使用事件上传/摄取结果、定位点、时间线、质量、轨迹分析等。
- 主要依赖：kotlinx.serialization.Serializable
- 被谁使用：ApiService、同步/上传/分析调用方与反序列化

## 函数级结构化伪代码

### 设备与缺口
- MobileDeviceRegisterRequest / MobileDeviceDto：设备身份与心跳/同步时间
- MobileGapRequest / MobileGapResponse / MobileGapWindowDto：缺口查询与窗口

### 使用上传与摄取
- MobileUsageEventsUploadRequest：apps + events + fallbackSummaries
- MobileAppMetadataDto / MobileUsageEventDto / MobileUsageSummaryDto
- MobileIngestItemResult / MobileIngestResponse：按 clientItemKey 的 outcome 计数

### 定位
- MobileLocationPointRequest / MobileLocationPointDto：精度/速度/方位/自动提交/quality
- MobileLocationHistoryResponse：时间窗 + maxAccuracy + points

### 摘要与时间线
- MobileAppUsageSummaryDto / MobileSyncBatchSummaryDto
- MobileUsageSummaryResponse：当日汇总与 ranking
- MobileTimelineItemDto / MobileTimelineResponse：session/fallback/items

### 质量
- MobileQualityResponse + Component/Issue DTO

### 分析与轨迹
- MobileAnalyticsRangeDto / MobileGeoBoundsDto
- MobileLocationAnalyticsOverviewResponse
- MobileLocationPathPointDto / SegmentDto / TrackDto
- MobileLocationSegmentPointPageDto：分页 cursor

## 近逐行中文伪代码

1. 全部 @Serializable data class，无行为逻辑。
2. 设备注册与 DTO 含硬件/版本/元数据与时间戳。
3. Gap 请求带 capabilityJson；响应 windows 列表。
4. 使用事件批次含 apps/events/fallback；摄取返回 itemResults。
5. 定位请求/DTO 含水平精度、provider、sourceKind、可选传感器字段。
6. 使用摘要、同步批次、时间线、质量组件/问题。
7. 分析范围、地理边界、轨迹/片段/路径点与分页。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt",
      "label": "MobileModels",
      "path": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt", "type": "depends_on" }
  ]
}
```
