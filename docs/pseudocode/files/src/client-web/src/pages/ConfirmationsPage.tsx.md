# src/client-web/src/pages/ConfirmationsPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：确认中心：待确认列表、详情、二级/严格确认、拒绝、前后差异。
- 主要依赖：operations API、BeforeAfterDiff、StrictConfirmationPanel
- 被谁使用：路由确认中心

## 函数级结构化伪代码

### getConfirmActionState
- 二级确认需 arm 后才 final

### ConfirmationsPage
- 拉 pending 与 detail；选中首条
- confirm 分支 strict/secondLevel/普通；reject
- StrictConfirmationPanel + BeforeAfterDiff + 操作按钮

## 近逐行中文伪代码

1. 30s 刷新待确认列表。
2. 选中拉详情。
3. 二级确认先 arm 再确认。
4. 严格/L4 走 strict API。
5. 成功 invalidate 相关 keys。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/ConfirmationsPage.tsx",
      "label": "ConfirmationsPage",
      "path": "src/client-web/src/pages/ConfirmationsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/ConfirmationsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/ConfirmationsPage.tsx",
      "to": "src/client-web/src/api/operations.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/ConfirmationsPage.tsx",
      "to": "src/client-web/src/components/schedule/BeforeAfterDiff.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/ConfirmationsPage.tsx",
      "to": "src/client-web/src/components/schedule/StrictConfirmationPanel.tsx",
      "type": "depends_on"
    }
  ]
}
`
