# src/client-web/src/pages/QuickNotesPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：快速笔记页：状态筛选列表、创建草稿、详情编辑/预览、处理/归档/恢复/删除。
- 主要依赖：quickNotes API、QuickNoteEditor/Preview、PageHeader
- 被谁使用：路由快速笔记

## 函数级结构化伪代码

### formatDateTime / noteTitle / 本地 StatusBadge
- 列表标题与时间

### QuickNotesPage
- list/detail queries；create/update/process/archive/restore/delete mutations
- 乐观 deletedIds 过滤；选中同步 editMarkdown
- 左列表右编辑/预览

## 近逐行中文伪代码

1. inbox/processed/archived 过滤 + 搜索。
2. 新建草稿 createQuickNote。
3. 选中拉详情编辑保存。
4. 处理/归档/恢复/删除操作刷缓存。
5. Markdown 预览含附件。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/QuickNotesPage.tsx",
      "label": "QuickNotesPage",
      "path": "src/client-web/src/pages/QuickNotesPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/QuickNotesPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/QuickNotesPage.tsx",
      "to": "src/client-web/src/api/quickNotes.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/QuickNotesPage.tsx",
      "to": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/QuickNotesPage.tsx",
      "to": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx",
      "type": "depends_on"
    }
  ]
}
`
