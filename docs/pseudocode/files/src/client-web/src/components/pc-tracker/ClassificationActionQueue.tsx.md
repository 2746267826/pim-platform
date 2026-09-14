# src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：展示最多 10 条待处理活动分类建议，支持预览/稍后/拒绝。
- 主要依赖：ActivityClassificationSuggestion
- 被谁使用：PcTrackerPage 上下文确认区

## 函数级结构化伪代码

### formatMinutes / displayName / suggestionBadge
- 秒转分钟；展示名优先 displayName/clusterKey；按 recognitionSource 打徽章

### ClassificationActionQueue
- loading/空态；map 建议卡：样本数/时长/当前与建议分类；按钮 onPreview/onLater/onReject

## 近逐行中文伪代码

1. 加载中与空队列虚线占位。
2. 仅展示前 10 条建议。
3. 卡片显示名称、徽章、样本与建议。
4. 主按钮处理并预览；可选稍后；拒绝。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx",
      "label": "ClassificationActionQueue",
      "path": "src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx",
      "to": "src/client-web/src/types/index.ts",
      "type": "depends_on"
    }
  ]
}
`
