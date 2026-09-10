# tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：位置聚合总览与轨迹：距离/精度、长间隙切段、跨设备隔离、稳定 segment id、stay/move/stay。
- 主要依赖：`MobileLocationAggregationService`、`MobileLocationQueryService`、`MobileTestHelpers`、点实体
- 被谁使用：dotnet test

## 函数级结构化伪代码

### GetOverviewAsync_ReturnsAcceptedMetricInputs
- 步骤：3 可用点 → PointCount/Usable=3；DistanceMeters>1000；AverageAccuracy>0

### GetTracksAsync_SplitsLongGapsAndReturnsMoveSegments
- 步骤：时间大间隔 → tracks≥2；含 kind=move

### GetTracksAsync_DoesNotConnectDifferentDevices
- 步骤：两设备各一条 track；每 track 单 segment

### GetOverviewAsync_DoesNotReportLargeGapAcrossDifferentDevices
- 步骤：跨设备不报 large-gap；QualityIssueCount=0

### GetOverviewAsync_DoesNotAddDistanceAcrossDifferentDevices
- 步骤：跨设备 DistanceMeters=0

### GetTracksAsync_ReturnsStableUrlSafeSegmentIdsAcrossFilters
- 步骤：过滤 DeviceId 前后 segment.Id 一致；匹配 URL-safe 字符

### GetTracksAsync_SplitsStayMoveStayWithinContinuousTrack
- 步骤：近点停留→远移→再停留 → kinds [stay,move,stay]

### Service / SeedPoint
- 步骤：组装服务；写入 MobileLocationPointEntity

## 近逐行中文伪代码

1. [L11-29] overview 指标
2. [L31-47] 长间隙切轨
3. [L49-64] 设备隔离 tracks
4. [L66-81] 跨设备无 large-gap
5. [L83-97] 跨设备无距离
6. [L99-125] 稳定 segment id
7. [L127-144] stay/move/stay
8. [L146-178] 工厂与 SeedPoint

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs",
      "label": "MobileLocationAggregationServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileLocationAggregationServiceTests.cs", "to": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "type": "depends_on" }
  ]
}
```
