# src/client-web/src/pages/WorkbenchPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：工作台仪表：今日图层计数、待确认、Outlook 设置/批次、密度模式与快捷链。
- 主要依赖：calendar/operations API、PageHeader、SegmentedControl
- 被谁使用：路由工作台

## 函数级结构化伪代码

### todayRange / format* / DashboardMetric
- 今日 ISO 范围；状态中文；指标卡

### WorkbenchPage
- 查 layers/confirmations/settings/syncBatches
- 聚合 layerCounts；密度影响列表条数
- 指标网格 + 各列表 + 深链

## 近逐行中文伪代码

1. 固定今日时间窗拉图层。
2. 30-60s 刷新确认与同步。
3. 密度切换展示条数。
4. 链到日历/同步/确认等页。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/WorkbenchPage.tsx",
      "label": "WorkbenchPage",
      "path": "src/client-web/src/pages/WorkbenchPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/WorkbenchPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/WorkbenchPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/WorkbenchPage.tsx",
      "to": "src/client-web/src/api/operations.ts",
      "type": "depends_on"
    }
  ]
}
`
