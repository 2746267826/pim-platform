# src/client-web/src/components/mobile/MobileRecordsDashboard.tsx

## 元信息
- 语言：TypeScript / React
- 程序集或包：client-web
- 职责：手机记录仪表盘：日期/设备筛选、指标条、时间线、App 排行、同步批次、质量面板。
- 主要依赖：`../../api/mobile` 类型；`MobileAppRanking`、`MobileMetricStrip`、`MobileQualityPanel`、`MobileTimeline`；`mobileFormatting`
- 被谁使用：手机记录路由/页面容器

## 函数级结构化伪代码

### SyncBatchPanel({ summary })
- 输入：可选 `MobileSummary`
- 输出：同步批次 section
- 副作用：无
- 步骤：
  1. `batches = summary?.syncBatches ?? []`。
  2. 标题与批次数徽章。
  3. 空 →「暂无同步批次」；否则最多展示 4 批：clientBatchId、提交时间、statusLabel、接受/跳过事件与定位计数、errorMessage。
- 分支与异常：无
- 调用：`formatDateTime`、`statusLabel`

### MobileRecordsDashboard(props)
- 输入：date、selectedDeviceId、devices、summary/timeline/quality、加载/错误与 onDateChange/onDeviceChange/onRefresh
- 输出：仪表盘布局 JSX
- 副作用：无（回调由父组件驱动数据）
- 步骤：
  1. `timelineItems`、`appRanking` 从 props 取默认空数组。
  2. 顶栏：标题说明、刷新按钮（isFetching 文案）。
  3. 日期 input + 设备 select（空值=全部设备）。
  4. 有 errorMessage 显示红框。
  5. isLoading → 加载文案；否则：
     - `MobileMetricStrip` 传汇总指标（质量问题数回退到 quality.issues.length）。
     - 左 `MobileTimeline`；右排行 + SyncBatchPanel + MobileQualityPanel。
- 分支与异常：缺 summary 用 0/空
- 调用：子组件与格式化

## 近逐行中文伪代码

1. 引入 mobile 类型与子组件。
2. SyncBatchPanel：最多 4 条批次卡片与计数网格。
3. Dashboard：筛选栏 + 错误 + 加载/内容两态。
4. 内容区 MetricStrip + Timeline + Ranking + 批次 + Quality。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx",
      "label": "MobileRecordsDashboard",
      "path": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileRecordsDashboard.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileAppRanking.tsx", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileMetricStrip.tsx", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileQualityPanel.tsx", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileTimeline.tsx", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" }
  ]
}
```
