# src/client-web/src/pages/CategoryTreePage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：分类树管理：展开选择、增改删、种子数据、AppKnowledgeTabs。
- 主要依赖：pcTracker API category tree、AppKnowledgeTabs、PageHeader
- 被谁使用：路由分类树

## 函数级结构化伪代码

### TreeNode
- 递归渲染展开/色点/生产力标签/内置标记

### CategoryTreePage
- 查 getCategoryTree；save/delete/seed mutations
- 表单新建/编辑 CategorySaveRequest
- 选中节点详情与删除；种子按钮

## 近逐行中文伪代码

1. 拉树并递归 TreeNode。
2. 选中填充表单。
3. 保存/删除/种子后 invalidate。
4. 附带知识库 tabs。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/CategoryTreePage.tsx",
      "label": "CategoryTreePage",
      "path": "src/client-web/src/pages/CategoryTreePage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/CategoryTreePage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/CategoryTreePage.tsx",
      "to": "src/client-web/src/api/pcTracker.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/CategoryTreePage.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "type": "depends_on"
    }
  ]
}
`
