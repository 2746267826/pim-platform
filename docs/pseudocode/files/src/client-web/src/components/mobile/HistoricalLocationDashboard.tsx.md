# src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：历史位置仪表盘展示组件——时间快捷/自定义范围、设备与质量过滤、指标条、地图+片段详情、停留时间线与原始点表；状态由父页注入。
- 主要依赖：`../../api/mobile` 类型、`mobileFormatting.MobileRangeShortcut`、`LocationHistoryMap`、`LocationMetricStrip`、`LocationRawPointTable`、`LocationSegmentDetail`、`LocationStayMoveTimeline`
- 被谁使用：`pages/HistoricalLocationPage.tsx`

## 函数级结构化伪代码

### HistoricalLocationDashboardProps
#### 字段
- 输入：rangeShortcut/起止日、selectedDeviceId、devices、maxAccuracyMeters、includeRejected、overview、tracks、选中 segment/point、points、loading/fetching/error、各类 on* 回调
- 输出：Props 接口
- 副作用：无
- 步骤：纯受控
- 分支与异常：无
- 调用：无

### deviceLabel(device)
- 输入：MobileDevice
- 输出：显示名
- 副作用：无
- 步骤：displayName || model || deviceId
- 分支与异常：无
- 调用：无

### HistoricalLocationDashboard(props) 默认导出
- 输入：完整 Props
- 输出：JSX
- 副作用：用户操作触发回调（不直接请求 API）
- 步骤：
  1. 标题区「历史位置」+ 快捷 today/7d/30d/custom + 北京时间 + 刷新
  2. 过滤网格：设备、日期范围、最大误差、展示模式（固定轨迹+停留）、隐藏已拒绝点、搜索占位
  3. 摘要 chips：日期区间、误差、拒绝点策略
  4. errorMessage 红框
  5. LocationMetricStrip(overview)
  6. loading → 加载文案；否则地图+SegmentDetail、StayMoveTimeline+RawPointTable
- 分支与异常：isLoading 切换主体；error 仅展示
- 调用：子组件与 onShortcutChange/onRefresh 等

## 近逐行中文伪代码

1. 引入 mobile 类型与 Location* 子组件
2. 定义 Props 与 shortcuts 中文标签
3. deviceLabel 三选一
4. 渲染过滤条与刷新；快捷按钮高亮当前 shortcut
5. 日期 input 改 custom range；checkbox 反转 includeRejected
6. 展示搜索框（当前无 onChange 逻辑）
7. 指标条；非 loading 时双栏地图/详情与时间线/点表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx",
      "label": "HistoricalLocationDashboard",
      "path": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/components/mobile/LocationHistoryMap.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/components/mobile/LocationMetricStrip.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/components/mobile/LocationSegmentDetail.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "to": "src/client-web/src/components/mobile/LocationRawPointTable.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/HistoricalLocationPage.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "type": "calls" }
  ]
}
```
