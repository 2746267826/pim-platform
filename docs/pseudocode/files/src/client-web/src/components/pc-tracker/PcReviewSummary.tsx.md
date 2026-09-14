# src/client-web/src/components/pc-tracker/PcReviewSummary.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：今日复盘六指标卡：时长、输入、主分类、切换、待确认、活跃度。
- 主要依赖：PcSummaryResponse、ActivityClassificationSuggestion
- 被谁使用：PcTrackerPage

## 函数级结构化伪代码

### formatCount / mainCategory
- 本地化数字；首分类名

### PcReviewSummary
- 从 summary.metrics 组装 6 卡；可选 mostFocusedApp 徽章

## 近逐行中文伪代码

1. 汇总 key/click 得输入活跃度。
2. 六格网格渲染 label/value。
3. 有最聚焦应用则蓝底提示。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/PcReviewSummary.tsx",
      "label": "PcReviewSummary",
      "path": "src/client-web/src/components/pc-tracker/PcReviewSummary.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/PcReviewSummary.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/PcReviewSummary.tsx",
      "to": "src/client-web/src/types/index.ts",
      "type": "depends_on"
    }
  ]
}
`
