# src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `ClassificationSuggestionPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatMinutes
#### formatMinutes(seconds: number)
- 输入：seconds: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatMinutes`
  2. 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
  3. 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
- 分支与异常：无显著分支
- 调用：formatMinutes、Math.round、minutes.toLocaleString

### getEmojiForApp
#### getEmojiForApp(appIcon?: string | null, clusterKey?: string)
- 输入：appIcon?: string | null, clusterKey?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getEmojiForApp`
  2. 执行：if (appIcon) return appIcon;
  3. 执行：if (!clusterKey) return '❓';
  4. 执行：if (clusterKey.startsWith('web:')) return '🌐';
  5. 执行：if (clusterKey.startsWith('app:')) return '📱';
  6. 返回 '❓'
- 分支与异常：if (appIcon) return appIcon;；if (!clusterKey) return '❓';；if (clusterKey.startsWith('web:')) return '🌐';；if (clusterKey.startsWith('app:')) return '📱';
- 调用：getEmojiForApp、clusterKey.startsWith

### getRecognitionBadge
#### getRecognitionBadge(recognitionSource?: string | null)
- 输入：recognitionSource?: string | null
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getRecognitionBadge`
  2. 若 (recognitionSource === 'builtin' || recognitionSource === 'manual') 则
  3. 返回 JSX/结构
  4. 执行：<span className="inline-block rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
  5. 执行：<span className="inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
- 分支与异常：if (recognitionSource === 'builtin' || recognitionSource === 'manual') {
- 调用：getRecognitionBadge

### toggleSelect
#### toggleSelect(id: string)
- 输入：id: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义箭头函数 `toggleSelect`
  2. 执行：setSelectedIds(prev => {
  3. 赋值 `next` = new Set(prev)
  4. 执行：if (next.has(id)) next.delete(id);
  5. 执行：else next.add(id);
  6. 返回 next
- 分支与异常：if (next.has(id)) next.delete(id);；else next.add(id);
- 调用：setSelectedIds、Set、next.has、next.delete、next.add

### toggleSelectAll
#### toggleSelectAll(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义箭头函数 `toggleSelectAll`
  2. 若 (selectedIds.size === visibleSuggestions.length) 则
  3. 更新状态 setSelectedIds(new Set())
  4. 更新状态 setSelectedIds(new Set(visibleSuggestions.map(s => s.id)))
- 分支与异常：if (selectedIds.size === visibleSuggestions.length) {
- 调用：setSelectedIds、Set、visibleSuggestions.map

## 近逐行中文伪代码

1. [L4] 定义类型 `Props`
2. [L5] 执行：suggestions: ActivityClassificationSuggestion[];
3. [L6] 执行：isLoading: boolean;
4. [L7] 执行：onAccept: (suggestion: ActivityClassificationSuggestion) => void;
5. [L8] 执行：onCorrect: (suggestion: ActivityClassificationSuggestion) => void;
6. [L9] 执行：onReject: (suggestion: ActivityClassificationSuggestion) => void;
7. [L10] 执行：onBatchAccept: (ids: string[]) => void;
8. [L11] 执行：onBatchReject: (ids: string[]) => void;
9. [L14] 定义函数 `formatMinutes`
10. [L15] 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
11. [L16] 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
12. [L19] 定义函数 `getEmojiForApp`
13. [L20] 执行：if (appIcon) return appIcon;
14. [L21] 执行：if (!clusterKey) return '❓';
15. [L22] 执行：if (clusterKey.startsWith('web:')) return '🌐';
16. [L23] 执行：if (clusterKey.startsWith('app:')) return '📱';
17. [L24] 返回 '❓'
18. [L27] 定义函数 `getRecognitionBadge`
19. [L28] 若 (recognitionSource === 'builtin' || recognitionSource === 'manual') 则
20. [L29] 返回 JSX/结构
21. [L30] 执行：<span className="inline-block rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
22. [L35] 返回 JSX/结构
23. [L36] 执行：<span className="inline-block rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
24. [L42] 默认导出函数 `ClassificationSuggestionPanel`
25. [L43] 执行：suggestions,
26. [L44] 执行：isLoading,
27. [L45] 执行：onAccept,
28. [L46] 执行：onCorrect,
29. [L47] 执行：onReject,
30. [L48] 执行：onBatchAccept,
31. [L49] 执行：onBatchReject,
32. [L51] 执行：const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
33. [L53] 若 (isLoading) 则
34. [L54] 返回 JSX/结构
35. [L55] 执行：<div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500"
36. [L56] 执行：正在加载分类建议...
37. [L61] 赋值 `visibleSuggestions` = suggestions.slice(0, 10)
38. [L63] 若 (visibleSuggestions.length === 0) 则
39. [L64] 返回 JSX/结构
40. [L65] 执行：<div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500"
41. [L66] 执行：暂无需要处理的分类建议。
42. [L71] 定义箭头函数 `toggleSelect`
43. [L72] 执行：setSelectedIds(prev => {
44. [L73] 赋值 `next` = new Set(prev)
45. [L74] 执行：if (next.has(id)) next.delete(id);
46. [L75] 执行：else next.add(id);
47. [L76] 返回 next
48. [L80] 定义箭头函数 `toggleSelectAll`
49. [L81] 若 (selectedIds.size === visibleSuggestions.length) 则
50. [L82] 更新状态 setSelectedIds(new Set())
51. [L84] 更新状态 setSelectedIds(new Set(visibleSuggestions.map(s => s.id)))
52. [L88] 返回 JSX/结构
53. [L89] 执行：<div className="space-y-2">
54. [L90] 执行：{visibleSuggestions.map(suggestion => {
55. [L91] 赋值 `displayName` = suggestion.appDisplayName || suggestion.clusterKey
56. [L92] 赋值 `icon` = getEmojiForApp(suggestion.appIcon, suggestion.clusterKey)
57. [L93] 赋值 `isSelected` = selectedIds.has(suggestion.id)
58. [L94] 赋值 `appName` = suggestion.clusterKey?.startsWith('app:')
59. [L95] 执行：? suggestion.clusterKey.slice(4)
60. [L98] 返回 JSX/结构
61. [L100] 执行：key={suggestion.id}
62. [L101] 执行：className={`flex min-w-0 flex-col gap-3 rounded-lg border px-3 py-3 transition-colors md:flex-row md:items-sta
63. [L102] 执行：isSelected
64. [L103] 执行：? 'border-blue-300 bg-blue-50'
65. [L104] 执行：: 'border-slate-200 bg-white'
66. [L107] 执行：<div className="flex min-w-0 items-start gap-3">
67. [L108] 执行：{/* Checkbox for batch */}
68. [L110] 执行：type="checkbox"
69. [L111] 执行：className="mt-1 h-4 w-4 shrink-0 accent-blue-600"
70. [L112] 执行：checked={isSelected}
71. [L113] 执行：onChange={() => toggleSelect(suggestion.id)}
72. [L116] 执行：{/* App icon */}
73. [L117] 执行：<span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-lg">
74. [L121] 执行：<div className="min-w-0 flex-1">
75. [L122] 执行：{/* App display name + recognition badge */}
76. [L123] 执行：<div className="flex flex-wrap items-center gap-2">
77. [L124] 执行：<span className="truncate text-sm font-semibold text-slate-950">
78. [L125] 执行：{displayName}
79. [L127] 执行：{getRecognitionBadge(suggestion.recognitionSource)}
80. [L128] 执行：{appName && displayName !== appName && (
81. [L129] 执行：<span className="truncate text-xs text-slate-400">
82. [L130] 执行：{appName}
83. [L135] 执行：{/* Stats row */}
84. [L136] 执行：<div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
85. [L138] 执行：样本 <strong className="text-slate-700">{suggestion.sampleCount.toLocaleString('zh-CN')}</strong>
86. [L141] 执行：时长 <strong className="text-slate-700">{formatMinutes(suggestion.totalDurationSeconds)}</strong>
87. [L143] 执行：{suggestion.currentCategory && (
88. [L145] 执行：当前 <strong className="text-slate-700">{suggestion.currentCategory}</strong>
89. [L150] 执行：{/* Suggested category */}
90. [L151] 执行：{suggestion.suggestedCategory && (
91. [L152] 执行：<div className="mt-1.5 text-xs text-blue-600">
92. [L153] 执行：建议 → <span className="font-medium">{suggestion.suggestedCategory}</span>
93. [L154] 执行：<span className="ml-1 text-green-600">99% 置信</span>
94. [L160] 执行：{/* Action buttons */}
95. [L161] 执行：<div className="flex shrink-0 gap-2 pl-9 md:pl-0">
96. [L163] 执行：type="button"
97. [L164] 执行：onClick={() => onAccept(suggestion)}
98. [L165] 执行：className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-7
99. [L168] 执行：</button>
100. [L170] 执行：type="button"
101. [L171] 执行：onClick={() => onCorrect(suggestion)}
102. [L172] 执行：className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 transiti
103. [L175] 执行：</button>
104. [L177] 执行：type="button"
105. [L178] 执行：onClick={() => onReject(suggestion)}
106. [L179] 执行：className="rounded-lg border border-red-200 bg-white px-3 py-1.5 text-xs font-medium text-red-500 transition-c
107. [L182] 执行：</button>
108. [L188] 执行：{/* Batch action bar */}
109. [L189] 执行：{visibleSuggestions.length > 0 && (
110. [L190] 执行：<div className="flex items-center gap-3 border-t border-slate-100 px-1 pt-3">
111. [L192] 执行：type="checkbox"
112. [L193] 执行：className="h-4 w-4 accent-blue-600"
113. [L194] 执行：checked={selectedIds.size === visibleSuggestions.length}
114. [L195] 执行：onChange={toggleSelectAll}
115. [L197] 执行：<span className="text-xs text-slate-500">
116. [L198] 执行：{selectedIds.size > 0
117. [L199] 执行：? `已选 ${selectedIds.size} 项`
118. [L202] 执行：{selectedIds.size > 0 && (
119. [L205] 执行：onClick={() => {
120. [L206] 执行：onBatchAccept([...selectedIds]);
121. [L207] 更新状态 setSelectedIds(new Set())
122. [L209] 执行：className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-7
123. [L212] 执行：</button>
124. [L214] 执行：onClick={() => {
125. [L215] 执行：onBatchReject([...selectedIds]);
126. [L216] 更新状态 setSelectedIds(new Set())
127. [L218] 执行：className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 transiti
128. [L221] 执行：</button>
129. [L222] 执行：<span className="ml-auto text-xs text-slate-400">
130. [L223] 执行：{visibleSuggestions.length < suggestions.length
131. [L224] 执行：? `还有 ${suggestions.length - visibleSuggestions.length} 条未显示`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx",
      "label": "ClassificationSuggestionPanel",
      "path": "src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
