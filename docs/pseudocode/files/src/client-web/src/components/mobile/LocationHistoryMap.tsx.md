# src/client-web/src/components/mobile/LocationHistoryMap.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：位置历史轨迹地图壳层——汇总指标、动态 import Leaflet 地图、段/点选择回调。
- 主要依赖：`MobileLocationTrack`、`locationFormatting`、懒加载 `HistoricalLocationLeafletMap`
- 被谁使用：历史位置 Dashboard 等页面

## 函数级结构化伪代码

### `allSegments(tracks)`
- 输入：轨迹数组
- 输出：扁平化所有 segment
- 步骤：`flatMap(track => track.segments)`

### `LocationHistoryMap(props)`
- 输入：tracks、selectedSegmentId/PointId、onSelectSegment/Point
- 输出：地图 section UI
- 副作用：useEffect 动态 import Leaflet 组件；卸载置 mounted=false
- 步骤：
  1. state 存 Leaflet 组件类型或 null。
  2. 汇总 segments；选中段 = 按 id 找或取第一段。
  3. totalDistance 累加各 track.distanceMeters。
  4. averageAccuracy：无段则 0，否则段平均误差均值。
  5. useEffect：仅浏览器环境；import HistoricalLocationLeafletMap；mounted 时 setLeafletMap。
  6. 渲染标题、轨迹数/总距离/平均误差徽章。
  7. 有 LeafletMap 则传入 tracks 与选择 props；否则占位文案 + 当前片段摘要。
- 分支与异常：SSR（无 window）跳过 import；卸载后不 setState
- 调用：formatAccuracyLabel、formatDistanceMeters、segmentKindLabel、动态 import

## 近逐行中文伪代码

1. 引入 React hooks、MobileLocationTrack、格式化工具、Leaflet 地图 props 类型。
2. allSegments 扁平化轨迹段。
3. 组件 state 懒加载 LeafletMap。
4. 算选中段、总距离、平均精度。
5. useEffect 动态 import 地图模块，清理 mounted。
6. 头部展示轨迹数/距离/平均误差。
7. 地图区：已加载则渲染 LeafletMap；否则占位与当前段 kind/距离/点数。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationHistoryMap.tsx",
      "label": "LocationHistoryMap",
      "path": "src/client-web/src/components/mobile/LocationHistoryMap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationHistoryMap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationHistoryMap.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationHistoryMap.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationHistoryMap.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx", "type": "calls" }
  ]
}
```
