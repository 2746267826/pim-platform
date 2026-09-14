# src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：将多条 `MobileLocationTrack` 的 segments 展平排序，渲染可点击的停留/移动时间线列表。
- 主要依赖：`MobileLocationTrack`（mobile API 类型）、`locationFormatting` 格式化函数
- 被谁使用：移动端历史位置仪表盘等

## 函数级结构化伪代码

### segmentsFromTracks(tracks)
- 输入：轨迹数组
- 输出：按 `startUtc` 升序的 segment 列表
- 副作用：无
- 步骤：`flatMap(segments)` + `localeCompare` 排序
- 分支与异常：无
- 调用：无

### LocationStayMoveTimeline(props)
- 输入：`tracks`、可选 `selectedSegmentId`、`onSelectSegment`
- 输出：React section
- 副作用：点击回调
- 步骤：
  1. 展平排序 segments。
  2. 标题区固定文案。
  3. 空列表提示；否则 map 按钮：时间窗、kind 标签、距离/时长/精度、点数。
  4. 选中项蓝底边框；`onClick` → `onSelectSegment?.(id)`。
- 分支与异常：空态 / 选中态样式
- 调用：`segmentKindLabel`、`formatDistanceMeters`、`formatDurationSeconds`、`formatAccuracyLabel`

## 近逐行中文伪代码

1. 从 tracks 展平 segments，按 startUtc 排序。
2. 区块标题“停留与移动时间线”。
3. 无片段显示空文案；有则网格按钮列表。
4. 显示本地起止时刻、kind、距离/时长/误差、点数；支持选中高亮与回调。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx",
      "label": "LocationStayMoveTimeline",
      "path": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "calls" }
  ]
}
```
