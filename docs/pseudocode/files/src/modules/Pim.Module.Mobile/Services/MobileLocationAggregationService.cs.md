# src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：基于位置点聚合轨迹/分段/概览统计：按设备与 2h 间隙切 track，按停留半径/时长切 stay/move 段，Haversine 距离与质量标志。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`MobileLocationQueryService`、`TimeProvider`
  - `MobileLocationPointEntity`、位置分析 DTO、`MobileUserContext`、`MobileAnalyticsDefaults`
- 被谁使用：Mobile 位置分析相关端点

## 函数级结构化伪代码

### MobileLocationAggregationService
#### 常量
- `TrackGapThreshold` = 2h；`StayDurationThreshold` = 10min
- `StayRadiusThresholdMeters` = 150；`MoveDistanceThresholdMeters` = 30
- `EarthRadiusMeters` = 6371000

#### 构造 `(db, currentUser, queryService, timeProvider)`
- 注入依赖字段

#### `GetOverviewAsync(request, ct)`
- 输入：`MobileLocationQueryRequest`
- 输出：`MobileLocationAnalyticsOverviewResponse`
- 副作用：只读 DB
- 步骤：
  1. `queryService.Normalize` → context。
  2. `LoadRawPointsAsync`；过滤 `IsUsable` → usable。
  3. `CountLargeGaps`；`QualityFlags`；rejected = raw - usable。
  4. activeSpan：usable 首尾时间差秒（≤1 点则 0）。
  5. 组装：点数、距离（按设备求和）、停留段数、最长停留、均精度、质量问题数=拒绝+大间隙。
- 分支与异常：RequireUserId 失败由 MobileUserContext 抛
- 调用：Normalize、LoadRawPoints、CountStaySegments、LongestStaySeconds 等

#### `GetTracksAsync(request, ct)`
- 步骤：Normalize → `LoadTrackPointsAsync`（可含 rejected）→ `BuildTracks`
- 输出：`IReadOnlyList<MobileLocationTrackDto>`

#### `GetSegmentAsync(segmentId, request, ct)`
- 空 id → null；否则 GetTracks 扁平 Segments 按 Id 精确匹配

#### `GetSegmentPointsAsync(segmentId, request, ct)`
- 段不存在 → 空页；否则用段 Path 的 Guid 集合过滤 raw 点，按 Cursor Guid 分页

#### `LoadRawPointsAsync` / `LoadTrackPointsAsync` private
- 用户隔离 + 时间范围 + 可选 DeviceId；OrderBy RecordedAtUtc, Id
- Track 点：IncludeRejected 或 IsUsable

#### `BuildTracks` / `BuildTrack` / `BuildSegments` / `BuildSegment` static
- 按 DeviceId 分组；相邻点间隔 > 2h 开新 track
- 段：≤2 点单段；否则 FindStayEnd / FindNextStayStart 切 stay 与 move
- Kind：单点或半径≤150m 且时长≥10min → stay；距离≥30m → move 否则 stay
- StableId = `prefix_firstN_lastN`；本地时间标签；质量 flags

#### `FindStayEnd` / `FindNextStayStart`
- 从 start 扩展窗口满足时长与 MaxRadius；找到下一个 stay 起点

#### 质量与统计
- `SegmentQualityFlags`：single-point / low-accuracy / rejected-points
- `QualityFlags` 概览：low-accuracy-cluster / rejected-points / large-gap / no-usable-points
- `CountStaySegments` / `LongestStaySeconds`：临时全范围 context 再 BuildTracks

#### 几何与映射
- Haversine `DistanceMeters`；中心点 `MaxRadiusMeters`
- `IsUsable`：非 rejected 且精度 ≤ MaxAccuracyMeters
- MapPathPoint / MapPoint；Latitude/Longitude/Accuracy 自 decimal 转 double
- 时区：FindSystemTimeZoneById；默认时区失败回退 China Standard Time

## 近逐行中文伪代码

1. 注入 db、currentUser、queryService、timeProvider；定义间隙/停留/地球半径常量。
2. **概览**：规范化请求 → 加载原始点 → 可用点 → 大间隙与质量标志 → 汇总距离/停留/精度。
3. **轨迹**：加载（可含拒绝）点 → 按设备与 2h 间隙切 track → 每 track 建 segments。
4. **分段算法**：优先识别停留窗（≥10min 且半径≤150m）；其间为 move 或弱 stay。
5. **段点分页**：段 Path Id 过滤 raw 点，Cursor 为上一页最后点 Guid。
6. **距离**：逐点 Haversine 累加；概览按设备分别累加再求和。
7. **可用点**：Quality≠rejected 且水平精度≤阈值。
8. 辅助：稳定 Id、本地标签、时区回退、decimal 转 double。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs",
      "label": "MobileLocationAggregationService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileLocationAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" }
  ]
}
```
