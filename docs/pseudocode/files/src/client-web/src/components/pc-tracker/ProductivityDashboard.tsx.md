# src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：今日效率面板：环形分数、生产/中性/分心小时、目标、本周柱状趋势。
- 主要依赖：getProductivityDashboard
- 被谁使用：PcTrackerPage

## 函数级结构化伪代码

### CircularScore
- SVG 环按 score 着色 70/50 阈值

### ProductivityDashboardPanel
- 查今日 dashboard；loading 占位；goalMet 徽章；小时列表；weeklyTrend 柱

## 近逐行中文伪代码

1. 今日 date 拉效率数据。
2. 环图显示 todayScore。
3. 三色小时与目标。
4. 周趋势柱按 max 归一。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx",
      "label": "ProductivityDashboard",
      "path": "src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/ProductivityDashboard.tsx",
      "to": "src/client-web/src/api/pcTracker.ts",
      "type": "depends_on"
    }
  ]
}
`
