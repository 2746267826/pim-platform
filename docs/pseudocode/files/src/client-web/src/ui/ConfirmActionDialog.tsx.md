# src/client-web/src/ui/ConfirmActionDialog.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `ConfirmActionDialog`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`、`src/client-web/src/ui/confirmActionDialogModel.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatSampleTime
#### formatSampleTime(sample: CalendarOperationSample)
- 输入：sample: CalendarOperationSample
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatSampleTime`
  2. 执行：if (sample.start && sample.end) return `${sample.start} - ${sample.end}`;
  3. 返回 sample.start || sample.end || null
- 分支与异常：if (sample.start && sample.end) return `${sample.start} - ${sample.end}`;
- 调用：formatSampleTime

### getFocusableElements
#### getFocusableElements(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getFocusableElements`
  2. 赋值 `dialog` = dialogRef.current
  3. 执行：if (!dialog) return [];
  4. 返回 Array.from(
  5. 执行：dialog.querySelectorAll<HTMLElement>(
  6. 执行：'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [ta
  7. 执行：).filter(element => !element.hasAttribute('aria-hidden'));
- 分支与异常：if (!dialog) return [];
- 调用：getFocusableElements、Array.from、not、filter、element.hasAttribute

### handleKeyDown
#### handleKeyDown(e: KeyboardEvent<HTMLDivElement>)
- 输入：e: KeyboardEvent<HTMLDivElement>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `handleKeyDown`
  2. 若 (e.key === 'Escape') 则
  3. 执行：e.stopPropagation();
  4. 执行：onCancel();
  5. 返回（空）
  6. 执行：if (e.key !== 'Tab') return;
  7. 赋值 `focusableElements` = getFocusableElements()
  8. 若 (focusableElements.length === 0) 则
  9. 执行：e.preventDefault();
  10. 执行：dialogRef.current?.focus();
  11. 赋值 `firstElement` = focusableElements[0]
  12. 赋值 `lastElement` = focusableElements[focusableElements.length - 1]
  13. 赋值 `activeElement` = document.activeElement
  14. 若 (e.shiftKey && (activeElement === firstElement || activeElement === dialogRef.current)) 则
  15. 执行：lastElement.focus();
  16. 执行：firstElement.focus();
- 分支与异常：if (e.key === 'Escape') {；if (e.key !== 'Tab') return;；if (focusableElements.length === 0) {；if (e.shiftKey && (activeElement === firstElement || activeElement === dialogRef.current)) {
- 调用：handleKeyDown、e.stopPropagation、onCancel、getFocusableElements、e.preventDefault、focus、lastElement.focus、firstElement.focus

## 近逐行中文伪代码

1. [L4] 执行：buildDeleteConfirmationCopy,
2. [L5] 执行：getOperationSampleTypeLabel,
3. [L6] 定义类型 `DeleteConfirmationInput`
4. [L10] 定义类型 `DeleteConfirmationCopy`
5. [L11] 定义类型 `DeleteConfirmationInput`
6. [L14] 执行：export { buildDeleteConfirmationCopy } from './confirmActionDialogModel';
7. [L16] 定义类型 `ConfirmActionDialogProps`
8. [L17] 执行：open: boolean;
9. [L18] 执行：input: DeleteConfirmationInput | null;
10. [L19] 执行：isPending?: boolean;
11. [L20] 执行：onCancel: () => void;
12. [L21] 执行：onConfirm: () => void;
13. [L24] 定义函数 `formatSampleTime`
14. [L25] 执行：if (sample.start && sample.end) return `${sample.start} - ${sample.end}`;
15. [L26] 返回 sample.start || sample.end || null
16. [L29] 默认导出函数 `ConfirmActionDialog`
17. [L32] 执行：isPending = false,
18. [L33] 执行：onCancel,
19. [L34] 执行：onConfirm,
20. [L36] Hook `useRef` 绑定 `dialogRef`
21. [L37] Hook `usedRef` 绑定 `previouslyFocusedRef`
22. [L38] 赋值 `titleId` = useId()
23. [L40] 注册 `useEffect` 副作用
24. [L41] 执行：if (!open || !input) return;
25. [L43] 执行：previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
26. [L44] 执行：? document.activeElement
27. [L47] 赋值 `dialog` = dialogRef.current
28. [L48] 执行：dialog?.focus();
29. [L50] 返回 JSX/结构
30. [L51] 执行：previouslyFocusedRef.current?.focus();
31. [L52] 执行：previouslyFocusedRef.current = null;
32. [L56] 执行：if (!open || !input) return null;
33. [L58] 定义函数 `getFocusableElements`
34. [L59] 赋值 `dialog` = dialogRef.current
35. [L60] 执行：if (!dialog) return [];
36. [L62] 返回 Array.from(
37. [L63] 执行：dialog.querySelectorAll<HTMLElement>(
38. [L64] 执行：'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [ta
39. [L66] 执行：).filter(element => !element.hasAttribute('aria-hidden'));
40. [L69] 定义函数 `handleKeyDown`
41. [L70] 若 (e.key === 'Escape') 则
42. [L71] 执行：e.stopPropagation();
43. [L72] 执行：onCancel();
44. [L73] 返回（空）
45. [L76] 执行：if (e.key !== 'Tab') return;
46. [L78] 赋值 `focusableElements` = getFocusableElements()
47. [L79] 若 (focusableElements.length === 0) 则
48. [L80] 执行：e.preventDefault();
49. [L81] 执行：dialogRef.current?.focus();
50. [L82] 返回（空）
51. [L85] 赋值 `firstElement` = focusableElements[0]
52. [L86] 赋值 `lastElement` = focusableElements[focusableElements.length - 1]
53. [L87] 赋值 `activeElement` = document.activeElement
54. [L89] 若 (e.shiftKey && (activeElement === firstElement || activeElement === dialogRef.current)) 则
55. [L90] 执行：e.preventDefault();
56. [L91] 执行：lastElement.focus();
57. [L93] 执行：e.preventDefault();
58. [L94] 执行：firstElement.focus();
59. [L98] 赋值 `copy` = buildDeleteConfirmationCopy(input)
60. [L100] 返回 JSX/结构
61. [L101] 执行：<div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6">
62. [L103] 执行：ref={dialogRef}
63. [L104] 执行：role="dialog"
64. [L105] 执行：aria-modal="true"
65. [L106] 执行：aria-labelledby={titleId}
66. [L107] 执行：tabIndex={-1}
67. [L108] 执行：onKeyDown={handleKeyDown}
68. [L109] 执行：className="w-full max-w-lg rounded-lg border border-slate-200 bg-white shadow-2xl"
69. [L111] 执行：<header className="border-b border-slate-200 px-5 py-4">
70. [L112] 执行：<p className="text-xs font-semibold uppercase tracking-wide text-red-600">严格确认</p>
71. [L113] 执行：<h2 id={titleId} className="mt-1 text-base font-semibold text-slate-950">
72. [L114] 执行：{copy.title}
73. [L116] 执行：<p className="mt-2 text-sm leading-6 text-slate-600">{copy.description}</p>
74. [L117] 执行：</header>
75. [L119] 执行：<section className="px-5 py-4">
76. [L120] 执行：<div className="flex items-center justify-between gap-3">
77. [L121] 执行：<h3 className="text-sm font-medium text-slate-800">受影响样例</h3>
78. [L122] 执行：<span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">
79. [L123] 执行：共 {input.affectedCount} 项
80. [L127] 执行：{copy.samples.length > 0 ? (
81. [L128] 执行：<ul className="mt-3 max-h-56 space-y-2 overflow-auto">
82. [L129] 执行：{copy.samples.map(sample => {
83. [L130] 赋值 `sampleTime` = formatSampleTime(sample)
84. [L132] 返回 JSX/结构
85. [L133] 执行：<li key={`${sample.type}:${sample.id}`} className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
86. [L134] 执行：<div className="flex items-start justify-between gap-3">
87. [L135] 执行：<div className="min-w-0">
88. [L136] 执行：<p className="truncate text-sm font-medium text-slate-900">{sample.title}</p>
89. [L137] 执行：<p className="mt-0.5 text-xs text-slate-500">
90. [L138] 执行：{getOperationSampleTypeLabel(sample.type)}
91. [L139] 执行：{sample.bookName ? ` · ${sample.bookName}` : ''}
92. [L142] 执行：{sampleTime && <span className="shrink-0 text-xs text-slate-500">{sampleTime}</span>}
93. [L149] 执行：<p className="mt-3 rounded-md border border-dashed border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-5
94. [L153] 执行：</section>
95. [L155] 执行：<footer className="flex items-center justify-end gap-2 border-t border-slate-200 px-5 py-4">
96. [L157] 执行：type="button"
97. [L158] 执行：onClick={onCancel}
98. [L159] 执行：className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-s
99. [L162] 执行：</button>
100. [L164] 执行：type="button"
101. [L165] 执行：onClick={onConfirm}
102. [L166] 执行：disabled={isPending}
103. [L167] 执行：className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:cursor-not
104. [L169] 执行：{isPending ? '处理中' : copy.confirmLabel}
105. [L170] 执行：</button>
106. [L171] 执行：</footer>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "label": "ConfirmActionDialog",
      "path": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/ConfirmActionDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "to": "src/client-web/src/ui/confirmActionDialogModel.ts",
      "type": "depends_on"
    }
  ]
}
```
