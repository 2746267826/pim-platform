# src/client-web/src/components/quick-notes/QuickNoteEditor.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `QuickNoteEditor`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/api/quickNotes.ts`、`src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### QuickNoteEditorProps
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L27 声明 `QuickNoteEditorProps`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L3] 执行：BlockTypeSelect,
2. [L4] 执行：BoldItalicUnderlineToggles,
3. [L5] 执行：CreateLink,
4. [L6] 执行：headingsPlugin,
5. [L7] 执行：imagePlugin,
6. [L8] 执行：InsertImage,
7. [L9] 执行：InsertThematicBreak,
8. [L10] 执行：linkPlugin,
9. [L11] 执行：listsPlugin,
10. [L12] 执行：ListsToggle,
11. [L13] 执行：markdownShortcutPlugin,
12. [L14] 执行：MDXEditor,
13. [L15] 定义类型 `MDXEditorMethods`
14. [L16] 执行：quotePlugin,
15. [L17] 执行：Separator,
16. [L18] 执行：thematicBreakPlugin,
17. [L19] 执行：toolbarPlugin,
18. [L20] 执行：UndoRedo,
19. [L27] 导出类型 `QuickNoteEditorProps`
20. [L28] 执行：value: string;
21. [L29] 执行：onChange?: (value: string) => void;
22. [L30] 执行：minHeight?: number;
23. [L31] 执行：readOnly?: boolean;
24. [L34] 默认导出函数 `QuickNoteEditor`
25. [L36] 执行：onChange,
26. [L37] 执行：minHeight = 220,
27. [L38] 执行：readOnly = false,
28. [L40] Hook `useRef` 绑定 `editorRef`
29. [L41] Hook `useRef` 绑定 `previewObjectUrls`
30. [L43] 注册 `useEffect` 副作用
31. [L44] 赋值 `editor` = editorRef.current
32. [L45] 若 (editor && editor.getMarkdown() !== value) 则
33. [L46] 执行：editor.setMarkdown(value);
34. [L50] 注册 `useEffect` 副作用
35. [L51] 执行：previewObjectUrls.current.forEach(url => URL.revokeObjectURL(url));
36. [L52] 执行：previewObjectUrls.current.clear();
37. [L55] Hook `useMemo` 绑定 `plugins`
38. [L56] 赋值 `sharedPlugins` = [
39. [L57] 执行：headingsPlugin(),
40. [L58] 执行：listsPlugin(),
41. [L59] 执行：quotePlugin(),
42. [L60] 执行：thematicBreakPlugin(),
43. [L61] 执行：linkPlugin(),
44. [L62] 执行：imagePlugin({
45. [L63] 执行：imageUploadHandler: async file => {
46. [L64] 等待 `uploadQuickNoteAttachment(file)` 赋给 `uploaded`
47. [L65] 返回 uploaded.downloadUrl
48. [L67] 执行：imagePreviewHandler: async imageSource => {
49. [L68] 赋值 `attachmentId` = getQuickNoteAttachmentIdFromDownloadUrl(imageSource)
50. [L69] 若 (!attachmentId) 则
51. [L70] 返回 imageSource
52. [L73] 赋值 `cachedObjectUrl` = previewObjectUrls.current.get(attachmentId)
53. [L74] 若 (cachedObjectUrl) 则
54. [L75] 返回 cachedObjectUrl
55. [L78] 等待 `downloadQuickNoteAttachmentBlob(attachmentId)` 赋给 `blob`
56. [L79] 赋值 `objectUrl` = URL.createObjectURL(blob)
57. [L80] 执行：previewObjectUrls.current.set(attachmentId, objectUrl);
58. [L81] 返回 objectUrl
59. [L84] 执行：markdownShortcutPlugin(),
60. [L87] 若 (readOnly) 则
61. [L88] 返回 sharedPlugins
62. [L91] 返回 [
63. [L92] 执行：...sharedPlugins,
64. [L93] 执行：toolbarPlugin({
65. [L94] 执行：toolbarContents: () => (
66. [L96] 执行：<UndoRedo />
67. [L97] 执行：<Separator />
68. [L98] 执行：<BlockTypeSelect />
69. [L99] 执行：<BoldItalicUnderlineToggles />
70. [L100] 执行：<ListsToggle />
71. [L101] 执行：<Separator />
72. [L102] 执行：<CreateLink />
73. [L103] 执行：<InsertImage />
74. [L104] 执行：<InsertThematicBreak />
75. [L107] 执行：toolbarClassName: 'quick-note-editor-toolbar',
76. [L112] 返回 JSX/结构
77. [L114] 执行：className={`quick-note-editor overflow-hidden rounded-lg border border-slate-200 bg-white text-sm text-slate-8
78. [L115] 执行：readOnly ? 'quick-note-editor-readonly border-transparent bg-transparent' : ''
79. [L117] 执行：style={{ minHeight }}
80. [L119] 执行：<MDXEditor
81. [L120] 执行：ref={editorRef}
82. [L121] 执行：markdown={value}
83. [L122] 执行：onChange={nextValue => onChange?.(nextValue)}
84. [L123] 执行：plugins={plugins}
85. [L124] 执行：readOnly={readOnly}
86. [L125] 执行：contentEditableClassName="quick-note-editor-content min-h-[inherit] px-3 py-2 leading-6 outline-none"
87. [L126] 执行：className="h-full"

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx",
      "label": "QuickNoteEditor",
      "path": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/QuickNoteEditor.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx",
      "to": "src/client-web/src/api/quickNotes.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/quick-notes/QuickNoteEditor.tsx",
      "to": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts",
      "type": "depends_on"
    }
  ]
}
```
