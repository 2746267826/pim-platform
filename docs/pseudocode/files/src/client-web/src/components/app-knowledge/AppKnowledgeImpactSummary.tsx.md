# src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：展示应用知识变更影响摘要：影响记录数、影响时长（分钟）、可选待确认上下文数。
- 主要依赖：无（纯展示 props）
- 被谁使用：应用知识相关页面

## 函数级结构化伪代码

### formatMinutes(seconds: number)
- 输入：秒
- 输出：本地化分钟数字符串
- 副作用：无
- 步骤：`Math.round(seconds/60)` 后 `toLocaleString()`
- 分支与异常：无
- 调用：无

### AppKnowledgeImpactSummary(props)
- 输入：`affectedRecordCount`、`affectedDurationSeconds`、可选 `pendingContextCount`
- 输出：React 节点（若干 badge）
- 副作用：无
- 步骤：
  1. 渲染“影响 N 条记录”。
  2. 渲染“影响时长 M 分钟”（formatMinutes）。
  3. 若 `pendingContextCount` 为 number，再渲染琥珀色“待确认上下文 K 项”。
- 分支与异常：仅可选第三徽章
- 调用：`formatMinutes`

## 近逐行中文伪代码

1. Props：记录数、时长秒、可选待确认数。
2. 秒转整分钟并本地化显示。
3. 横排 flex 徽章；有 pending 时额外 amber 徽章。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx",
      "label": "AppKnowledgeImpactSummary",
      "path": "src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/app-knowledge", "to": "src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx", "type": "depends_on" }
  ]
}
```
