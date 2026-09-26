# src/client-web/src/components/pc-tracker/EventTimelineDialog.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `EventTimelineDialog`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### getBadge
#### getBadge(category: string)
- 输入：category: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getBadge`
  2. 执行：if (PRODUCTIVE_CATS.some(c => category.includes(c))) return { label: 'P', className: 'bg-emerald-400' };
  3. 执行：if (DISTRACTING_CATS.some(c => category.includes(c))) return { label: 'D', className: 'bg-rose-400' };
  4. 返回 JSX/结构
- 分支与异常：if (PRODUCTIVE_CATS.some(c => category.includes(c))) return { label: 'P', className: 'bg-emerald-400' };；if (DISTRACTING_CATS.some(c => category.includes(c))) return { label: 'D', className: 'bg-rose-400' };
- 调用：getBadge、PRODUCTIVE_CATS.some、category.includes、DISTRACTING_CATS.some

### EventTimelineDialog
#### EventTimelineDialog({ open, timeline, dateStr, onClose }: Props)
- 输入：{ open, timeline, dateStr, onClose }: Props
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `EventTimelineDialog`
  2. Hook `useRef` 绑定 `dialogRef`
  3. 赋值 `titleId` = useId()
  4. Hook `usedRef` 绑定 `previouslyFocusedRef`
  5. 注册 `useEffect` 副作用
  6. 执行：if (!open) return;
  7. 执行：previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
  8. 执行：? document.activeElement
  9. 赋值 `dialog` = dialogRef.current
  10. 执行：dialog?.focus();
  11. 返回 JSX/结构
  12. 执行：previouslyFocusedRef.current?.focus();
  13. 执行：previouslyFocusedRef.current = null;
  14. 定义函数 `handleKey`
  15. 执行：if (e.key === 'Escape') onClose();
  16. 执行：document.addEventListener('keydown', handleKey);
  17. 使用 `useMemo` 缓存计算结果
  18. 执行：if (!timeline.length) return { entries: [], totalMinutes: 0, productivePercent: 0 };
  19. 赋值 `parsed` = timeline
  20. 执行：.filter(item => item.start && item.end)
  21. 执行：.map(item => ({
  22. 执行：start: new Date(item.start),
  23. 执行：end: new Date(item.end),
  24. 执行：appName: item.appName || '',
  25. 执行：windowTitle: item.windowTitle,
  26. 执行：categoryName: item.categoryName || '其他',
  27. 执行：categoryColor: item.categoryColor || '#64748b',
  28. 执行：durationMinutes: item.durationMinutes || (new Date(item.end).getTime() - new Date(item.start).getTime()) / 600
  29. 执行：.sort((a, b) => a.start.getTime() - b.start.getTime());
  30. 赋值 `total` = parsed.reduce((s, e) => s + e.durationMinutes, 0)
- 分支与异常：if (!open) return;；if (e.key === 'Escape') onClose();；if (!timeline.length) return { entries: [], totalMinutes: 0, productivePercent: 0 };；if (!open) return null;
- 调用：EventTimelineDialog、useId、useEffect、focus、handleKey、onClose、document.addEventListener、document.removeEventListener、useMemo、filter、map、Date、getTime、sort、a.start.getTime

## 近逐行中文伪代码

1. [L5] 赋值 `PRODUCTIVE_CATS` = ['工作', '编程', '文档', '学习', '邮件', '终端']
2. [L6] 赋值 `DISTRACTING_CATS` = ['游戏', '视频', '娱乐', '社交']
3. [L8] 定义函数 `getBadge`
4. [L9] 执行：if (PRODUCTIVE_CATS.some(c => category.includes(c))) return { label: 'P', className: 'bg-emerald-400' };
5. [L10] 执行：if (DISTRACTING_CATS.some(c => category.includes(c))) return { label: 'D', className: 'bg-rose-400' };
6. [L11] 返回 JSX/结构
7. [L14] 定义类型 `Props`
8. [L15] 执行：open: boolean;
9. [L16] 执行：timeline: TimelineItem[];
10. [L17] 执行：dateStr: string;
11. [L18] 执行：onClose: () => void;
12. [L21] 默认导出函数 `EventTimelineDialog`
13. [L22] Hook `useRef` 绑定 `dialogRef`
14. [L23] 赋值 `titleId` = useId()
15. [L24] Hook `usedRef` 绑定 `previouslyFocusedRef`
16. [L26] 注册 `useEffect` 副作用
17. [L27] 执行：if (!open) return;
18. [L28] 执行：previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
19. [L29] 执行：? document.activeElement
20. [L31] 赋值 `dialog` = dialogRef.current
21. [L32] 执行：dialog?.focus();
22. [L33] 返回 JSX/结构
23. [L34] 执行：previouslyFocusedRef.current?.focus();
24. [L35] 执行：previouslyFocusedRef.current = null;
25. [L39] 注册 `useEffect` 副作用
26. [L40] 执行：if (!open) return;
27. [L41] 定义函数 `handleKey`
28. [L42] 执行：if (e.key === 'Escape') onClose();
29. [L44] 执行：document.addEventListener('keydown', handleKey);
30. [L45] 返回 JSX/结构
31. [L48] 使用 `useMemo` 缓存计算结果
32. [L49] 执行：if (!timeline.length) return { entries: [], totalMinutes: 0, productivePercent: 0 };
33. [L51] 赋值 `parsed` = timeline
34. [L52] 执行：.filter(item => item.start && item.end)
35. [L53] 执行：.map(item => ({
36. [L54] 执行：start: new Date(item.start),
37. [L55] 执行：end: new Date(item.end),
38. [L56] 执行：appName: item.appName || '',
39. [L57] 执行：windowTitle: item.windowTitle,
40. [L58] 执行：categoryName: item.categoryName || '其他',
41. [L59] 执行：categoryColor: item.categoryColor || '#64748b',
42. [L60] 执行：durationMinutes: item.durationMinutes || (new Date(item.end).getTime() - new Date(item.start).getTime()) / 600
43. [L62] 执行：.sort((a, b) => a.start.getTime() - b.start.getTime());
44. [L64] 赋值 `total` = parsed.reduce((s, e) => s + e.durationMinutes, 0)
45. [L65] 赋值 `prod` = parsed
46. [L66] 执行：.filter(e => PRODUCTIVE_CATS.some(c => e.categoryName.includes(c)))
47. [L67] 执行：.reduce((s, e) => s + e.durationMinutes, 0);
48. [L69] 返回 JSX/结构
49. [L72] Hook `useMemo` 绑定 `maxDuration`
50. [L73] 执行：() => Math.max(...entries.map(e => e.durationMinutes), 1),
51. [L74] 执行：[entries]
52. [L77] 执行：if (!open) return null;
53. [L79] 返回 JSX/结构
54. [L80] 执行：<div className="fixed inset-0 z-50 flex items-start justify-center px-4 py-8">
55. [L81] 执行：<div className="fixed inset-0 bg-slate-950/40 backdrop-blur-sm" onClick={onClose} />
56. [L83] 执行：ref={dialogRef}
57. [L84] 执行：role="dialog"
58. [L85] 执行：aria-modal="true"
59. [L86] 执行：aria-labelledby={titleId}
60. [L87] 执行：tabIndex={-1}
61. [L88] 执行：className="relative flex max-h-full w-full max-w-[640px] flex-col overflow-hidden rounded-2xl border border-sl
62. [L90] 执行：{/* Header */}
63. [L91] 执行：<header className="flex shrink-0 items-center justify-between border-b border-slate-100 px-5 py-4">
64. [L92] 执行：<h3 id={titleId} className="text-sm font-semibold text-slate-900">
65. [L93] 执行：详细时间线 · {format(new Date(dateStr), 'M月d日 EEEE')}
66. [L96] 执行：type="button"
67. [L97] 执行：onClick={onClose}
68. [L98] 执行：className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate
69. [L101] 执行：</button>
70. [L102] 执行：</header>
71. [L104] 执行：{/* Body */}
72. [L105] 执行：<div className="overflow-y-auto px-5 py-4">
73. [L106] 执行：{/* Summary */}
74. [L107] 执行：<div className="mb-4 flex flex-wrap items-center justify-between gap-2 text-xs">
75. [L108] 执行：<div className="flex items-center gap-3">
76. [L109] 执行：<span className="font-semibold text-slate-700">
77. [L110] 执行：共计 {totalMinutes} 分钟
78. [L112] 执行：<span className="text-slate-400">
79. [L113] 执行：{entries.length} 条事件
80. [L115] 执行：<span className="font-medium text-emerald-600">
81. [L116] 执行：生产性 {productivePercent}%
82. [L119] 执行：<div className="flex items-center gap-2 text-[10px] text-slate-400">
83. [L120] 执行：<span className="flex items-center gap-1">
84. [L121] 执行：<span className="inline-block h-2 w-2 rounded-full bg-emerald-400" /> 生产性
85. [L123] 执行：<span className="flex items-center gap-1">
86. [L124] 执行：<span className="inline-block h-2 w-2 rounded-full bg-slate-300" /> 中性
87. [L126] 执行：<span className="flex items-center gap-1">
88. [L127] 执行：<span className="inline-block h-2 w-2 rounded-full bg-rose-400" /> 分心
89. [L132] 执行：{/* Entries */}
90. [L133] 执行：{entries.length === 0 ? (
91. [L134] 执行：<div className="py-10 text-center text-sm text-slate-400">暂无时间线数据</div>
92. [L136] 执行：<div className="space-y-0.5">
93. [L137] 执行：{entries.map((entry, i) => {
94. [L138] 赋值 `badge` = getBadge(entry.categoryName)
95. [L139] 赋值 `barWidth` = Math.max((entry.durationMinutes / maxDuration) * 100, 3)
96. [L140] 返回 JSX/结构
97. [L143] 执行：className="group flex items-start gap-2.5 rounded-lg px-2 py-2 transition-colors hover:bg-slate-50"
98. [L145] 执行：{/* Time column */}
99. [L146] 执行：<div className="w-[66px] shrink-0 pt-0.5 text-right">
100. [L147] 执行：<span className="text-[11px] font-medium text-slate-600">
101. [L148] 执行：{format(entry.start, 'HH:mm')}
102. [L150] 执行：<span className="mx-0.5 text-[10px] text-slate-400">-</span>
103. [L151] 执行：<span className="text-[11px] text-slate-500">
104. [L152] 执行：{format(entry.end, 'HH:mm')}
105. [L156] 执行：{/* Dot + connector */}
106. [L157] 执行：<div className="flex shrink-0 flex-col items-center pt-1">
107. [L159] 执行：className="h-2.5 w-2.5 rounded-full border-2 border-white shadow-sm"
108. [L160] 执行：style={{ backgroundColor: entry.categoryColor }}
109. [L163] 执行：className="mt-0.5 w-px flex-1 bg-slate-200"
110. [L164] 执行：style={{ minHeight: i < entries.length - 1 ? '100%' : '0' }}
111. [L168] 执行：{/* Content */}
112. [L169] 执行：<div className="min-w-0 flex-1">
113. [L170] 执行：<div className="flex items-center gap-1.5">
114. [L171] 执行：<span className="truncate text-xs font-semibold text-slate-800">
115. [L172] 执行：{entry.appName}
116. [L175] 执行：className={`inline-flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full text-[7px] font-bold tex
117. [L177] 执行：{badge.label}
118. [L179] 执行：<span className="shrink-0 text-[10px] text-slate-400">
119. [L180] 执行：{entry.durationMinutes.toFixed(0)}m
120. [L183] 执行：{entry.windowTitle && (
121. [L184] 执行：<div className="mt-0.5 truncate text-[10px] leading-tight text-slate-400">
122. [L185] 执行：{entry.windowTitle}
123. [L188] 执行：<div className="mt-1 h-1 max-w-[200px] overflow-hidden rounded-full bg-slate-100">
124. [L190] 执行：className="h-full rounded-full transition-all duration-200"
125. [L191] 执行：style={{ width: `${barWidth}%`, backgroundColor: entry.categoryColor }}
126. [L201] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/EventTimelineDialog.tsx",
      "label": "EventTimelineDialog",
      "path": "src/client-web/src/components/pc-tracker/EventTimelineDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/EventTimelineDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/EventTimelineDialog.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
