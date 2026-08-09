# src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：今日区块：分类建议摘要最多 3 条 + 跳转 PC Tracker。
- 主要依赖：EmptyState、StatusBadge、TodaySection
- 被谁使用：TodaySectionHost

## 函数级结构化伪代码

### formatMinutes
- 秒转整分钟

### TodayClassificationSuggestionsSection
- pendingCount badge；0 则 EmptyState；否则 slice(0,3)；Link /pc-tracker

## 近逐行中文伪代码

1. 读 section.data 待处理数与列表。
2. 展示前三条建议摘要。
3. 链到 PC Tracker。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx",
      "label": "TodayClassificationSuggestionsSection",
      "path": "src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    }
  ]
}
`
