# src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：App 知识库二级导航：App 列表 / 分类树 Tab 链接，高亮当前页。
- 主要依赖：`react-router-dom` 的 `Link`
- 被谁使用：`AppKnowledgeBasePage`、`CategoryTreePage`

## 函数级结构化伪代码

### AppKnowledgeTabs({ active })
- 输入：`active: 'apps' | 'categories'`
- 输出：导航 React 节点
- 副作用：无
- 步骤：
  1. 静态 `tabs`：apps → `/app-knowledge-base`；categories → `/app-knowledge-base/categories`。
  2. 渲染 `nav`（aria-label「App 知识库导航」）。
  3. map 每个 tab 为 `Link`：匹配 active 时 `aria-current=page` 且蓝底白字，否则边框灰底。
- 分支与异常：仅样式/aria 分支
- 调用：`Link`

## 近逐行中文伪代码

1. Props 仅 `active`。
2. 两 tab：App 列表、分类树及路径。
3. flex 导航；当前页高亮并设 aria-current。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "label": "AppKnowledgeTabs",
      "path": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx", "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CategoryTreePage.tsx", "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx", "type": "depends_on" }
  ]
}
```
