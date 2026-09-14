# src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动位置分析 API 的请求/上下文/响应 DTO（概览、轨迹、分段、路径点分页）。
- 主要依赖：
  - `MobileAnalyticsRangeDto`（定义于其他 Mobile DTO 文件）
  - `MobileLocationPointDto`（定义于 `MobileDtos.cs`）
- 被谁使用：`MobileLocationAggregationService`、`MobileLocationQueryService` 及 Mobile 位置分析端点

## 函数级结构化伪代码

### MobileLocationQueryRequest（record）
#### 主构造
- 输入：可选 RangeStart/EndUtc、Timezone、DeviceId、MaxAccuracyMeters、IncludeRejected、Cursor、PageSize
- 输出：不可变查询请求
- 副作用：无
- 步骤：1. 全部字段默认 null/false 语义由规范化层处理
- 分支与异常：无
- 调用：无

### MobileLocationQueryContext（record）
#### 主构造
- 输入：已规范化的 Range、DeviceId、MaxAccuracyMeters、IncludeRejected、Cursor、PageSize
- 输出：服务内部查询上下文
- 副作用：无
- 步骤：1. 由 `MobileLocationQueryService.Normalize` 从 Request 生成
- 分支与异常：无
- 调用：无

### MobileGeoBoundsDto（record）
#### 主构造
- 输入：Min/Max Latitude/Longitude
- 输出：地理包围盒
- 副作用：无

### MobileLocationAnalyticsOverviewResponse（record）
#### 主构造
- 输入：Range、GeneratedAt、点计数/可用/拒绝、ActiveSpanSeconds、DistanceMeters、StayCount、LongestStaySeconds、AverageAccuracyMeters、QualityIssueCount、QualityFlags
- 输出：位置分析概览响应
- 副作用：无

### MobileLocationPathPointDto（record）
#### 主构造
- 输入：Id、RecordedAtUtc、经纬度、HorizontalAccuracyMeters、Quality
- 输出：轨迹路径上的精简点

### MobileLocationSegmentDto（record）
#### 主构造
- 输入：Id、TrackId、DeviceId、Kind、起止 UTC/本地标签、时长、距离、点数、均速、均/最大精度、Quality、QualityFlags、Bounds、Path
- 输出：停留/移动分段

### MobileLocationTrackDto（record）
#### 主构造
- 输入：Id、DeviceId、起止、距离、时长、点数、段数、Bounds、QualityFlags、Segments
- 输出：按设备与时间间隙切分的轨迹

### MobileLocationSegmentPointPageDto（record）
#### 主构造
- 输入：Items（完整 `MobileLocationPointDto`）、NextCursor、HasMore
- 输出：分段内点分页

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Mobile.DTOs`；全部为 sealed record。
2. `MobileLocationQueryRequest`：原始查询参数（时间范围、时区、设备、精度上限、是否含拒绝点、游标、页大小）。
3. `MobileLocationQueryContext`：规范化后的内部上下文（含必填 MaxAccuracyMeters/PageSize）。
4. `MobileGeoBoundsDto`：四点包围盒。
5. `MobileLocationAnalyticsOverviewResponse`：概览统计与质量标志。
6. `MobileLocationPathPointDto`：段路径精简点。
7. `MobileLocationSegmentDto`：段元数据 + Path。
8. `MobileLocationTrackDto`：轨迹元数据 + Segments。
9. `MobileLocationSegmentPointPageDto`：段内点游标分页。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs",
      "label": "MobileLocationAnalyticsDtos",
      "path": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs.md",
      "layer": "module.mobile",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs", "type": "depends_on" }
  ]
}
```
