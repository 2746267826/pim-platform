# src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：Mobile 模块 API 请求/响应 DTO（设备注册、缺口查询、用量上传、定位点、摘要/时间线/质量诊断），并提供部分字段别名与兼容构造函数。
- 主要依赖：`Pim.Core.Operations.PimHealthStatus`
- 被谁使用：Mobile 端点、`MobileGapService`、`MobileUsageIngestService`、查询/质量服务与 Android 客户端契约

## 函数级结构化伪代码

### MobileDeviceRegisterRequest
#### 记录字段与别名
- 输入：DeviceId、AndroidIdHash、DisplayName、厂商/品牌/型号、AndroidVersion、SdkInt、AppVersion、MetadataJson
- 输出：不可变请求 + `DeviceHash`/`OsVersion`/`ApiLevel` 别名
- 副作用：无
- 步骤：主字段直接映射；别名分别回落到 AndroidIdHash、AndroidVersion、SdkInt
- 分支与异常：无
- 调用：无

### MobileDeviceDto
#### 主构造 + 兼容构造
- 输入：完整设备状态字段；或精简注册视图（registeredAt/lastSeen，心跳/同步为空、IsActive=true）
- 输出：设备 DTO；别名 `DeviceHash`/`OsVersion`/`ApiLevel`
- 副作用：无
- 步骤：兼容构造转发到主构造并填默认 LastHeartbeat/LastSync/IsActive
- 分支与异常：无
- 调用：无

### MobileGapRequest / MobileGapWindowDto / MobileGapResponse
#### 缺口查询契约
- 输入：DeviceId、RangeStart/End、CapabilityJson；窗口起止/Reason/SourcePreference；MaxBackfillStart + Windows
- 输出：请求别名 `CapabilitiesJson`；窗口别名 `SignalsJson`；响应为窗口列表
- 副作用：无
- 步骤：纯数据载体，供 `MobileGapService` 计算
- 分支与异常：无
- 调用：无

### MobileUsageEventsUploadRequest 及子 DTO
#### 用量上传批次
- 输入：DeviceId、ClientBatchId、源窗口、Apps/Events/FallbackSummaries
- 输出：别名 BatchId/WindowStart/End/Summaries；App/Event/Summary 元数据与 ClientItemKey
- 副作用：无
- 步骤：`MobileAppMetadataDto` 别名 Category/InstallerPackage；`MobileUsageSummaryDto.TotalTimeVisibleMs` 映射前台毫秒
- 分支与异常：无
- 调用：无

### MobileIngestItemResult / MobileUsageIngestResult
#### 摄取结果
- 输入：逐项 ClientItemKey/EntityType/Outcome/Code/Message；批次计数 + ItemResults
- 输出：结果 DTO；重载可省略 ItemResults 或仅传 accepted/failed
- 副作用：无
- 步骤：兼容构造将 skipped/rejected 置 0、ItemResults 空列表
- 分支与异常：无
- 调用：无

### MobileLocationPointRequest / MobileLocationPointDto
#### 定位点
- 输入：设备、时间、经纬度、精度、Provider、SourceKind、可选海拔/速度/方位、IsAutoSubmitted、RawJson
- 输出：Request 别名 Source，IsMock 恒 false；Dto 含 Quality/SubmittedAt 与精简兼容构造
- 副作用：无
- 步骤：精简构造填 SubmittedAt=recorded、可选字段 null、IsAutoSubmitted=false、RawJson="{}"
- 分支与异常：无
- 调用：无

### MobileSummaryQuery / MobileAppUsageSummaryDto / MobileSyncBatchSummaryDto / MobileUsageSummaryResponse
#### 日摘要查询与响应
- 输入：可选 DeviceId 与时间范围；应用排行与同步批次字段；汇总 TotalForeground/Completeness 等
- 输出：查询/排行/批次/汇总响应
- 副作用：无
- 步骤：纯展示与查询形状
- 分支与异常：无
- 调用：无

### MobileTimelineItemDto / MobileTimelineResponse / MobileLocationHistoryResponse
#### 时间线与定位历史
- 输入：会话/回退摘要条目；Date/Device/GeneratedAt + Sessions/FallbackSummaries/Items；历史点列表与精度上限
- 输出：对应响应
- 副作用：无
- 步骤：Items 可聚合 Sessions 与 Fallback
- 分支与异常：无
- 调用：无

### MobileQualityResponse / MobileQualityComponentDto / MobileQualityIssueDto
#### 质量诊断
- 输入：OverallStatus、Label/Message、Components/Issues/NextSteps；组件 Key/Name/Status/Details；问题 Code/Severity/ComponentKey
- 输出：质量响应；兼容构造提供中文默认 Label/Message 与空 NextSteps；组件默认 CheckedAt=UtcNow
- 副作用：无
- 步骤：依赖 `PimHealthStatus`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `Pim.Core.Operations`；命名空间 `Pim.Module.Mobile.DTOs`。
2. `MobileDeviceRegisterRequest`：设备注册字段 + DeviceHash/OsVersion/ApiLevel 别名。
3. `MobileDeviceDto`：完整设备状态；兼容构造映射注册视图并默认活跃。
4. `MobileGapRequest/Window/Response`：缺口查询与窗口原因/能力 JSON。
5. `MobileUsageEventsUploadRequest` + App/Event/Summary DTO 与批次别名。
6. `MobileIngestItemResult`/`MobileUsageIngestResult` 及计数兼容构造。
7. 定位点 Request/Dto（含精简构造与 Source 别名）。
8. 摘要查询、应用排行、同步批次、`MobileUsageSummaryResponse`。
9. 时间线条目/响应、定位历史响应。
10. 质量响应/组件/问题 DTO，默认中文文案与 `PimHealthStatus`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs",
      "label": "MobileDtos",
      "path": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs.md",
      "layer": "module.mobile",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" }
  ]
}
```
