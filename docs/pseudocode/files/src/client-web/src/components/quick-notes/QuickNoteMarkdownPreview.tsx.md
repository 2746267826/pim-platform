# src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：只读预览快速笔记 Markdown，解析附件 id 拉 blob URL 重写，并可下载附件列表。
- 主要依赖：downloadQuickNoteAttachmentBlob、QuickNoteEditor、quickNoteAttachmentBlobUrls
- 被谁使用：快速笔记详情/列表预览

## 函数级结构化伪代码

### QuickNoteMarkdownPreview
- extract 引用 id → 并行 download blob → createObjectURL Map
- rewrite markdown 内附件 URL；cleanup revoke
- readOnly QuickNoteEditor 显示；attachments 列表点击下载

## 近逐行中文伪代码

1. 从 markdown 提取附件 id。
2. effect 拉取 blob 建 objectUrl，取消时 revoke。
3. 重写后的 markdown 交给只读编辑器。
4. 附件名按钮触发浏览器下载。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx",
      "label": "QuickNoteMarkdownPreview",
      "path": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx", "to": "src/client-web/src/api/quickNotes.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx", "to": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/quick-notes/QuickNoteMarkdownPreview.tsx", "to": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts", "type": "depends_on" }
  ]
}
```
