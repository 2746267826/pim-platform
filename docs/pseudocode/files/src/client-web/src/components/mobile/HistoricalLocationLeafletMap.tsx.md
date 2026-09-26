# src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx

## 元信息
- 语言：TypeScript / React
- 程序集或包：client-web
- 职责：历史定位 Leaflet 地图：按轨迹段画折线、点标记与 Popup，支持段/点选中回调。
- 主要依赖：leaflet、react-leaflet；`MobileLocationTrack`/`MobileLocationPathPoint`；`mobileFormatting`、`locationFormatting`
- 被谁使用：手机定位/历史轨迹相关页面

## 函数级结构化伪代码

### 模块级常量与工具
#### markerIcon / selectedMarkerIcon
- 输入：无
- 输出：L.divIcon 实例（选中更大）
- 副作用：无
- 步骤：className 区分普通/选中；html 空 span；设定 size/anchor。
- 分支与异常：无
- 调用：`L.divIcon`

#### allSegments(tracks)
- 输入：轨迹数组
- 输出：所有 segment 扁平列表
- 步骤：`tracks.flatMap(t => t.segments)`。
- 调用：无

#### pathPosition(point)
- 输入：路径点
- 输出：`[lat, lng]`
- 步骤：取 latitude/longitude。
- 调用：无

#### firstPosition(tracks)
- 输入：轨迹
- 输出：中心坐标；无点时默认上海 [31.2304, 121.4737]
- 步骤：取第一段第一点；无则默认。
- 调用：`allSegments`、`pathPosition`

#### segmentColor(kind, selected)
- 输入：段类型、是否选中
- 输出：颜色 hex
- 步骤：选中红；move 蓝；否则青绿。
- 调用：无

### HistoricalLocationLeafletMap(props)
- 输入：tracks、selectedSegmentId/PointId、onSelectSegment/Point
- 输出：MapContainer JSX
- 副作用：地图渲染；点击回调
- 步骤：
  1. 汇总 segments。
  2. MapContainer：center=firstPosition，有段 zoom 13 否则 5。
  3. OSM TileLayer。
  4. 每段 Polyline：颜色/线宽/虚线(非 move)；click → onSelectSegment。
  5. 每点 Marker：id 缺省用 `segmentId-point-index`；选中换 icon/opacity；click 同时选段与点；Popup 展示类型、时间、坐标、精度、里程。
- 分支与异常：无点用默认中心；point.id 可空
- 调用：`formatDateTime`、`segmentKindLabel`、`formatCoordinate`、`formatAccuracyLabel`、`formatDistanceMeters`

## 近逐行中文伪代码

1. 引入 leaflet CSS/库与 react-leaflet 组件。
2. 定义 props：轨迹与选中/回调。
3. 普通与选中 divIcon。
4. 扁平段、点坐标、默认中心上海、段颜色规则。
5. 渲染地图：瓦片 + 折线 + 标记 Popup。
6. 点击折线/标记回传选中 id。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx",
      "label": "HistoricalLocationLeafletMap",
      "path": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "calls" }
  ]
}
```
