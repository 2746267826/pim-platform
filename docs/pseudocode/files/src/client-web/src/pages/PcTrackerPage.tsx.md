# src/client-web/src/pages/PcTrackerPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 活动总览：日期维度、热力/分析/时间线/键鼠/质量/复盘/分类确认与预览应用。
- 主要依赖：pcTracker+appKnowledge API、多 pc-tracker 组件
- 被谁使用：路由 PC Tracker

## 函数级结构化伪代码

### nextPcRoute3RequestId / isCurrentPcRoute3Request
- 预览/应用请求序号防竞态

### AnalysisCard
- 通用卡片壳

### PcTrackerPage
- 状态：日期/维度/分类应用筛选/对话框/预览
- 查询 summary/quality/suggestions/analysis/tree/heatmap/keystats 等
- reject/preview/apply mutations 带 requestId
- 组合 DateDimensionBar、热力、分析、时间线、键盘、质量、复盘、确认队列、效率面板

## 近逐行中文伪代码

1. 业务日默认 selectedDate。
2. 多 query 30s 刷新。
3. 分类建议预览与应用知识库。
4. 分析块选中与时间线对话框。
5. 大屏多面板编排。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/PcTrackerPage.tsx",
      "label": "PcTrackerPage",
      "path": "src/client-web/src/pages/PcTrackerPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/PcTrackerPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/PcTrackerPage.tsx",
      "to": "src/client-web/src/api/pcTracker.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/PcTrackerPage.tsx",
      "to": "src/client-web/src/api/appKnowledge.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/PcTrackerPage.tsx",
      "to": "src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/PcTrackerPage.tsx",
      "to": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx",
      "type": "depends_on"
    }
  ]
}
`
