# src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：可拖拽快速记录浮层：草稿 localStorage、创建笔记、失焦/缩放夹紧位置。
- 主要依赖：react-query、`createQuickNote`、`QuickNoteEditor`、`quickNoteFloatingState`
- 被谁使用：快速记录浮动入口

## 函数级结构化伪代码

### getViewportSize
- 无 window 时默认 1024x768，否则 innerWidth/Height

### loadDraft
- 读 QUICK_NOTE_DRAFT_KEY，失败返回空串

### QuickNoteFloatingPanel
- 输入：onClose
- 状态：markdown、position、error、positionRef、dragRef
- 副作用：localStorage 读写、pointer 拖拽、API 创建
- 步骤：
  1. 初始化草稿与面板位置（viewport+PANEL_SIZE）
  2. effect 同步 positionRef；挂载时 clamp 一次
  3. markdown 变化 best-effort 持久化草稿
  4. resize 时 clamp 并 savePanelPosition
  5. saveMutation：createQuickNote source=web-floating；成功清空草稿并 invalidate quick-notes
  6. pointer down/move/up/lostCapture 实现拖拽与落点保存
  7. handleSave：trim 空或 pending 则返回；否则 mutate
  8. UI：标题栏拖拽+关闭；编辑器；错误；保存按钮

## 近逐行中文伪代码

1. 固定面板 380x460。
2. 读视口与草稿。
3. 挂载夹紧位置；内容变更写草稿。
4. 窗口缩放重新夹紧并持久化坐标。
5. 保存 mutation 成功清状态与缓存。
6. 指针捕获拖动，松手写位置。
7. 保存需非空且非进行中。
8. 固定定位 section 渲染编辑与按钮。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx",
      "label": "QuickNoteFloatingPanel",
      "path": "src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx", "to": "src/client-web/src/api/quickNotes.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx", "to": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/quick-notes/QuickNoteFloatingPanel.tsx", "to": "src/client-web/src/components/quick-notes/quickNoteFloatingState.ts", "type": "depends_on" }
  ]
}
```
