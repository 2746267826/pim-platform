# src/client-web/src/pages/RecycleBinPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `RecycleBinPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`、`src/client-web/src/ui/OperationResultBanner.tsx`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### typeLabel
#### typeLabel(type: string)
- 输入：type: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `typeLabel`
  2. 执行：if (type === 'event') return '日程';
  3. 执行：if (type === 'task') return '任务';
  4. 执行：if (type === 'calendar' || type === 'calendar-book') return '日历本';
  5. 执行：if (type === 'task-book') return '任务本';
  6. 返回 type || '未知'
- 分支与异常：if (type === 'event') return '日程';；if (type === 'task') return '任务';；if (type === 'calendar' || type === 'calendar-book') return '日历本';；if (type === 'task-book') return '任务本';
- 调用：typeLabel

### canRestoreAsCopy
#### canRestoreAsCopy(type: string)
- 输入：type: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `canRestoreAsCopy`
  2. 返回 type === 'event' || type === 'task'
- 分支与异常：无显著分支
- 调用：canRestoreAsCopy

### recycleItemKey
#### recycleItemKey(item: Pick<CalendarRecycleBinItem, 'type' | 'id'>)
- 输入：item: Pick<CalendarRecycleBinItem, 'type' | 'id'>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `recycleItemKey`
  2. 返回 `${item.type}:${item.id}`
- 分支与异常：无显著分支
- 调用：recycleItemKey

### getErrorMessage
#### getErrorMessage(error: unknown)
- 输入：error: unknown
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getErrorMessage`
  2. 执行：if (error instanceof Error && error.message) return error.message;
  3. 返回 '操作失败，请稍后再试。'
- 分支与异常：if (error instanceof Error && error.message) return error.message;
- 调用：getErrorMessage

### formatDateTime
#### formatDateTime(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatDateTime`
  2. 执行：if (!value) return '-';
  3. 赋值 `parsed` = new Date(value)
  4. 执行：if (Number.isNaN(parsed.getTime())) return value;
  5. 返回 parsed.toLocaleString('zh-CN', {
  6. 执行：year: 'numeric',
  7. 执行：month: '2-digit',
  8. 执行：day: '2-digit',
  9. 执行：hour: '2-digit',
  10. 执行：minute: '2-digit',
- 分支与异常：if (!value) return '-';；if (Number.isNaN(parsed.getTime())) return value;
- 调用：formatDateTime、Date、Number.isNaN、parsed.getTime、parsed.toLocaleString

### formatSampleTime
#### formatSampleTime(sample: CalendarOperationSample)
- 输入：sample: CalendarOperationSample
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatSampleTime`
  2. 执行：if (sample.start && sample.end) return `${formatDateTime(sample.start)} - ${formatDateTime(sample.end)}`;
  3. 返回 formatDateTime(sample.start || sample.end)
- 分支与异常：if (sample.start && sample.end) return `${formatDateTime(sample.start)} - ${formatDateTime(sample.end)}`;
- 调用：formatSampleTime、formatDateTime

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

### RecycleBinPage
#### RecycleBinPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `RecycleBinPage`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [type, setType] = useState<RecycleType>('all');
  4. 执行：const [search, setSearch] = useState('');
  5. 执行：const [selectedItem, setSelectedItem] = useState<CalendarRecycleBinItem | null>(null);
  6. 执行：const [preview, setPreview] = useState<CalendarRestorePreviewResponse | null>(null);
  7. 执行：const [previewError, setPreviewError] = useState<unknown>(null);
  8. 执行：const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  9. 执行：const [result, setResult] = useState<CalendarOperationResult | null>(null);
  10. Hook `useRef` 绑定 `selectedItemKeyRef`
  11. Hook `useRef` 绑定 `activePreviewRequestRef`
  12. Hook `useRef` 绑定 `nextPreviewRequestIdRef`
  13. 赋值 `normalizedSearch` = search.trim()
  14. 赋值 `listQuery` = useQuery({
  15. 执行：queryKey: ['calendar-recycle-bin', type, normalizedSearch],
  16. 执行：queryFn: () =>
  17. 执行：getRecycleBin({
  18. 执行：search: normalizedSearch || undefined,
  19. 执行：pageSize: 50,
  20. 赋值 `previewMutation` = useMutation({
  21. 执行：mutationFn: (item: CalendarRecycleBinItem) => previewRecycleRestore(item.type, item.id),
  22. 赋值 `restoreMutation` = useMutation({
  23. 执行：mutationFn: ({ item, restoreAsCopy }: { item: CalendarRecycleBinItem; restoreAsCopy: boolean }) =>
  24. 执行：restoreRecycleItem(item.type, item.id, restoreAsCopy),
  25. 执行：onSuccess: data => {
  26. 更新状态 setResult(data)
  27. 执行：selectedItemKeyRef.current = null;
  28. 执行：activePreviewRequestRef.current = null;
  29. 更新状态 setSelectedItem(null)
  30. 更新状态 setPreview(null)
- 分支与异常：if (isActivePreviewRequest(variables, requestId)) {；if (!selectedItem) return;
- 调用：RecycleBinPage、useQueryClient、useState、useRef、search.trim、useQuery、getRecycleBin、useMutation、previewRecycleRestore、restoreRecycleItem、setResult、setSelectedItem、setPreview、setPreviewError、setIsPreviewLoading

## 近逐行中文伪代码

1. [L10] 执行：CalendarOperationResult,
2. [L11] 执行：CalendarOperationSample,
3. [L12] 执行：CalendarRecycleBinItem,
4. [L13] 执行：CalendarRestorePreviewResponse,
5. [L16] 定义类型 `RecycleType`
6. [L18] 执行：const typeOptions: { value: RecycleType; label: string }[] = [
7. [L19] 执行：{ value: 'all', label: '全部' },
8. [L20] 执行：{ value: 'event', label: '日程' },
9. [L21] 执行：{ value: 'task', label: '任务' },
10. [L22] 执行：{ value: 'calendar', label: '日历本' },
11. [L23] 执行：{ value: 'task-book', label: '任务本' },
12. [L26] 赋值 `invalidateAfterRestoreKeys` = [
13. [L27] 执行：['calendar-recycle-bin'],
14. [L28] 执行：['events'],
15. [L29] 执行：['events-paged'],
16. [L30] 执行：['tasks'],
17. [L31] 执行：['calendars'],
18. [L32] 执行：['today-sections'],
19. [L33] 执行：['today-section'],
20. [L34] 执行：] as const;
21. [L36] 定义函数 `typeLabel`
22. [L37] 执行：if (type === 'event') return '日程';
23. [L38] 执行：if (type === 'task') return '任务';
24. [L39] 执行：if (type === 'calendar' || type === 'calendar-book') return '日历本';
25. [L40] 执行：if (type === 'task-book') return '任务本';
26. [L41] 返回 type || '未知'
27. [L44] 定义函数 `canRestoreAsCopy`
28. [L45] 返回 type === 'event' || type === 'task'
29. [L48] 定义函数 `recycleItemKey`
30. [L49] 返回 `${item.type}:${item.id}`
31. [L52] 定义函数 `getErrorMessage`
32. [L53] 执行：if (error instanceof Error && error.message) return error.message;
33. [L54] 返回 '操作失败，请稍后再试。'
34. [L57] 定义函数 `formatDateTime`
35. [L58] 执行：if (!value) return '-';
36. [L60] 赋值 `parsed` = new Date(value)
37. [L61] 执行：if (Number.isNaN(parsed.getTime())) return value;
38. [L63] 返回 parsed.toLocaleString('zh-CN', {
39. [L64] 执行：year: 'numeric',
40. [L65] 执行：month: '2-digit',
41. [L66] 执行：day: '2-digit',
42. [L67] 执行：hour: '2-digit',
43. [L68] 执行：minute: '2-digit',
44. [L72] 定义函数 `formatSampleTime`
45. [L73] 执行：if (sample.start && sample.end) return `${formatDateTime(sample.start)} - ${formatDateTime(sample.end)}`;
46. [L74] 返回 formatDateTime(sample.start || sample.end)
47. [L77] 定义类型 `RestorePreviewDialogProps`
48. [L78] 执行：item: CalendarRecycleBinItem;
49. [L79] 执行：preview: CalendarRestorePreviewResponse | null;
50. [L80] 执行：isLoading: boolean;
51. [L81] 执行：previewError: unknown;
52. [L82] 执行：restoreError: unknown;
53. [L83] 执行：isRestoring: boolean;
54. [L84] 执行：onCancel: () => void;
55. [L85] 执行：onRetryPreview: () => void;
56. [L86] 执行：onRestore: (restoreAsCopy: boolean) => void;
57. [L89] 定义函数 `RestorePreviewDialog`
58. [L92] 执行：isLoading,
59. [L93] 执行：previewError,
60. [L94] 执行：restoreError,
61. [L95] 执行：isRestoring,
62. [L96] 执行：onCancel,
63. [L97] 执行：onRetryPreview,
64. [L98] 执行：onRestore,
65. [L100] Hook `useRef` 绑定 `dialogRef`
66. [L101] Hook `usedRef` 绑定 `previouslyFocusedRef`
67. [L102] 赋值 `titleId` = useId()
68. [L103] 赋值 `hasConflicts` = (preview?.conflicts.length ?? 0) > 0
69. [L104] 赋值 `copyAllowed` = canRestoreAsCopy(item.type)
70. [L105] 赋值 `canRestoreNormally` = Boolean(preview?.canRestoreWithoutConflict)
71. [L107] 注册 `useEffect` 副作用
72. [L108] 执行：previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
73. [L109] 执行：? document.activeElement
74. [L112] 执行：dialogRef.current?.focus();
75. [L114] 返回 JSX/结构
76. [L115] 执行：previouslyFocusedRef.current?.focus();
77. [L116] 执行：previouslyFocusedRef.current = null;
78. [L120] 定义函数 `getFocusableElements`
79. [L121] 赋值 `dialog` = dialogRef.current
80. [L122] 执行：if (!dialog) return [];
81. [L124] 返回 Array.from(
82. [L125] 执行：dialog.querySelectorAll<HTMLElement>(
83. [L126] 执行：'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [ta
84. [L128] 执行：).filter(element => !element.hasAttribute('aria-hidden'));
85. [L131] 定义函数 `handleKeyDown`
86. [L132] 若 (e.key === 'Escape') 则
87. [L133] 执行：e.stopPropagation();
88. [L134] 执行：onCancel();
89. [L135] 返回（空）
90. [L138] 执行：if (e.key !== 'Tab') return;
91. [L140] 赋值 `focusableElements` = getFocusableElements()
92. [L141] 若 (focusableElements.length === 0) 则
93. [L142] 执行：e.preventDefault();
94. [L143] 执行：dialogRef.current?.focus();
95. [L144] 返回（空）
96. [L147] 赋值 `firstElement` = focusableElements[0]
97. [L148] 赋值 `lastElement` = focusableElements[focusableElements.length - 1]
98. [L149] 赋值 `activeElement` = document.activeElement
99. [L151] 若 (e.shiftKey && (activeElement === firstElement || activeElement === dialogRef.current)) 则
100. [L152] 执行：e.preventDefault();
101. [L153] 执行：lastElement.focus();
102. [L155] 执行：e.preventDefault();
103. [L156] 执行：firstElement.focus();
104. [L160] 返回 JSX/结构
105. [L161] 执行：<div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6">
106. [L163] 执行：ref={dialogRef}
107. [L164] 执行：role="dialog"
108. [L165] 执行：aria-modal="true"
109. [L166] 执行：aria-labelledby={titleId}
110. [L167] 执行：tabIndex={-1}
111. [L168] 执行：onKeyDown={handleKeyDown}
112. [L169] 执行：className="w-full max-w-2xl rounded-lg border border-slate-200 bg-white shadow-2xl"
113. [L171] 执行：<header className="border-b border-slate-200 px-5 py-4">
114. [L172] 执行：<p className="text-xs font-semibold uppercase text-blue-600">恢复预览</p>
115. [L173] 执行：<h2 id={titleId} className="mt-1 text-base font-semibold text-slate-950">
116. [L174] 执行：恢复“{item.title}”
117. [L176] 执行：<p className="mt-2 text-sm text-slate-600">
118. [L177] 执行：{typeLabel(item.type)}
119. [L178] 执行：{item.bookName ? ` · 原本所属：${item.bookName}` : ''}
120. [L180] 执行：</header>
121. [L182] 执行：<section className="max-h-[60vh] overflow-auto px-5 py-4">
122. [L183] 执行：{isLoading && (
123. [L184] 执行：<div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-6 text-center text-sm text-slate-500">
124. [L185] 执行：正在检查恢复影响...
125. [L189] 执行：{!isLoading && Boolean(previewError) && (
126. [L190] 执行：<div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
127. [L191] 执行：<p className="font-medium">恢复预览加载失败</p>
128. [L192] 执行：<p className="mt-1">{getErrorMessage(previewError)}</p>
129. [L194] 执行：type="button"
130. [L195] 执行：onClick={onRetryPreview}
131. [L196] 执行：className="mt-3 rounded-md border border-red-200 bg-white px-3 py-1.5 text-xs font-medium text-red-700 hover:b
132. [L199] 执行：</button>
133. [L203] 执行：{!isLoading && preview && (
134. [L204] 执行：<div className="space-y-4">
135. [L206] 执行：className={`rounded-lg border px-4 py-3 text-sm ${
136. [L207] 执行：hasConflicts
137. [L208] 执行：? 'border-amber-200 bg-amber-50 text-amber-900'
138. [L209] 执行：: 'border-teal-200 bg-teal-50 text-teal-900'
139. [L212] 执行：{hasConflicts ? (
140. [L214] 执行：<p className="font-medium">发现 {preview.conflicts.length} 个冲突，不能直接恢复。</p>
141. [L215] 执行：<p className="mt-1">
142. [L216] 执行：{copyAllowed
143. [L217] 执行：? '可以恢复为副本，避免覆盖或合并现有项目。'
144. [L218] 执行：: '日历本和任务本暂不支持恢复为副本，请先处理冲突后再恢复。'}
145. [L222] 执行：<p className="font-medium">未发现冲突，可恢复 {preview.restoreCount} 项。</p>
146. [L226] 执行：{preview.samples.length > 0 && (
147. [L228] 执行：<div className="mb-2 flex items-center justify-between gap-3">
148. [L229] 执行：<h3 className="text-sm font-medium text-slate-800">将恢复的项目</h3>
149. [L230] 执行：<span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">
150. [L231] 执行：共 {preview.restoreCount} 项
151. [L234] 执行：<ul className="space-y-2">
152. [L235] 执行：{preview.samples.map(sample => (
153. [L236] 执行：<li key={`${sample.type}:${sample.id}`} className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
154. [L237] 执行：<div className="flex flex-wrap items-start justify-between gap-2">
155. [L238] 执行：<div className="min-w-0">
156. [L239] 执行：<p className="truncate text-sm font-medium text-slate-900">{sample.title}</p>
157. [L240] 执行：<p className="mt-0.5 text-xs text-slate-500">
158. [L241] 执行：{typeLabel(sample.type)}
159. [L242] 执行：{sample.bookName ? ` · ${sample.bookName}` : ''}
160. [L245] 执行：{(sample.start || sample.end) && (
161. [L246] 执行：<span className="shrink-0 text-xs text-slate-500">{formatSampleTime(sample)}</span>
162. [L255] 执行：{hasConflicts && (
163. [L257] 执行：<h3 className="mb-2 text-sm font-medium text-slate-800">冲突详情</h3>
164. [L258] 执行：<ul className="space-y-2">
165. [L259] 执行：{preview.conflicts.map(conflict => (
166. [L261] 执行：key={`${conflict.deletedType}:${conflict.deletedId}:${conflict.activeId}`}
167. [L262] 执行：className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900"
168. [L264] 执行：<p className="font-medium">{conflict.title}</p>
169. [L265] 执行：<p className="mt-1 text-xs">
170. [L266] 执行：{typeLabel(conflict.deletedType)} 与现有 {typeLabel(conflict.activeType)} 冲突：{conflict.reason}
171. [L274] 执行：{Boolean(restoreError) && (
172. [L275] 执行：<div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
173. [L276] 执行：{getErrorMessage(restoreError)}
174. [L281] 执行：</section>
175. [L283] 执行：<footer className="flex flex-wrap items-center justify-end gap-2 border-t border-slate-200 px-5 py-4">
176. [L285] 执行：type="button"
177. [L286] 执行：onClick={onCancel}
178. [L287] 执行：className="pim-button-secondary px-4 py-2 text-sm"
179. [L290] 执行：</button>
180. [L291] 执行：{preview && hasConflicts && copyAllowed && (
181. [L293] 执行：type="button"
182. [L294] 执行：onClick={() => onRestore(true)}
183. [L295] 执行：disabled={isRestoring}
184. [L296] 执行：className="pim-button-secondary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
185. [L298] 执行：{isRestoring ? '恢复中...' : '恢复为副本'}
186. [L299] 执行：</button>
187. [L302] 执行：type="button"
188. [L303] 执行：onClick={() => onRestore(false)}
189. [L304] 执行：disabled={!preview || !canRestoreNormally || isRestoring}
190. [L305] 执行：className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
191. [L307] 执行：{isRestoring ? '恢复中...' : '恢复'}
192. [L308] 执行：</button>
193. [L309] 执行：</footer>
194. [L315] 默认导出函数 `RecycleBinPage`
195. [L316] 赋值 `queryClient` = useQueryClient()
196. [L317] 执行：const [type, setType] = useState<RecycleType>('all');
197. [L318] 执行：const [search, setSearch] = useState('');
198. [L319] 执行：const [selectedItem, setSelectedItem] = useState<CalendarRecycleBinItem | null>(null);
199. [L320] 执行：const [preview, setPreview] = useState<CalendarRestorePreviewResponse | null>(null);
200. [L321] 执行：const [previewError, setPreviewError] = useState<unknown>(null);
201. [L322] 执行：const [isPreviewLoading, setIsPreviewLoading] = useState(false);
202. [L323] 执行：const [result, setResult] = useState<CalendarOperationResult | null>(null);
203. [L324] Hook `useRef` 绑定 `selectedItemKeyRef`
204. [L325] Hook `useRef` 绑定 `activePreviewRequestRef`
205. [L326] Hook `useRef` 绑定 `nextPreviewRequestIdRef`
206. [L328] 赋值 `normalizedSearch` = search.trim()
207. [L330] 赋值 `listQuery` = useQuery({
208. [L331] 执行：queryKey: ['calendar-recycle-bin', type, normalizedSearch],
209. [L332] 执行：queryFn: () =>
210. [L333] 执行：getRecycleBin({
211. [L335] 执行：search: normalizedSearch || undefined,
212. [L337] 执行：pageSize: 50,
213. [L341] 赋值 `previewMutation` = useMutation({
214. [L342] 执行：mutationFn: (item: CalendarRecycleBinItem) => previewRecycleRestore(item.type, item.id),
215. [L345] 赋值 `restoreMutation` = useMutation({
216. [L346] 执行：mutationFn: ({ item, restoreAsCopy }: { item: CalendarRecycleBinItem; restoreAsCopy: boolean }) =>
217. [L347] 执行：restoreRecycleItem(item.type, item.id, restoreAsCopy),
218. [L348] 执行：onSuccess: data => {
219. [L349] 更新状态 setResult(data)
220. [L350] 执行：selectedItemKeyRef.current = null;
221. [L351] 执行：activePreviewRequestRef.current = null;
222. [L352] 更新状态 setSelectedItem(null)
223. [L353] 更新状态 setPreview(null)
224. [L354] 更新状态 setPreviewError(null)
225. [L355] 更新状态 setIsPreviewLoading(false)
226. [L356] 循环 for (const queryKey of invalidateAfterRestoreKeys)
227. [L357] 执行：void queryClient.invalidateQueries({ queryKey });
228. [L362] 赋值 `items` = listQuery.data?.items ?? []
229. [L364] 定义函数 `isActivePreviewRequest`
230. [L365] 赋值 `itemKey` = recycleItemKey(item)
231. [L366] 返回 selectedItemKeyRef.current === itemKey
232. [L367] 执行：&& activePreviewRequestRef.current?.itemKey === itemKey
233. [L368] 执行：&& activePreviewRequestRef.current.requestId === requestId;
234. [L371] 定义函数 `startPreviewRequest`
235. [L372] 赋值 `itemKey` = recycleItemKey(item)
236. [L373] 赋值 `requestId` = nextPreviewRequestIdRef.current + 1
237. [L375] 执行：nextPreviewRequestIdRef.current = requestId;
238. [L376] 执行：selectedItemKeyRef.current = itemKey;
239. [L377] 执行：activePreviewRequestRef.current = { itemKey, requestId };
240. [L378] 更新状态 setPreview(null)
241. [L379] 更新状态 setPreviewError(null)
242. [L380] 更新状态 setIsPreviewLoading(true)
243. [L381] 执行：previewMutation.reset();
244. [L382] 执行：previewMutation.mutate(item, {
245. [L383] 执行：onSuccess: (data, variables) => {
246. [L384] 若 (isActivePreviewRequest(variables, requestId)) 则
247. [L385] 更新状态 setPreview(data)
248. [L388] 执行：onError: (error, variables) => {
249. [L389] 若 (isActivePreviewRequest(variables, requestId)) 则
250. [L390] 更新状态 setPreviewError(error)
251. [L393] 执行：onSettled: (_data, _error, variables) => {
252. [L394] 若 (isActivePreviewRequest(variables, requestId)) 则
253. [L395] 更新状态 setIsPreviewLoading(false)
254. [L401] 定义函数 `openRestorePreview`
255. [L402] 更新状态 setResult(null)
256. [L403] 更新状态 setSelectedItem(item)
257. [L404] 执行：restoreMutation.reset();
258. [L405] 执行：startPreviewRequest(item);
259. [L408] 定义函数 `closeRestorePreview`
260. [L409] 执行：selectedItemKeyRef.current = null;
261. [L410] 执行：activePreviewRequestRef.current = null;
262. [L411] 更新状态 setSelectedItem(null)
263. [L412] 更新状态 setPreview(null)
264. [L413] 更新状态 setPreviewError(null)
265. [L414] 更新状态 setIsPreviewLoading(false)
266. [L415] 执行：previewMutation.reset();
267. [L416] 执行：restoreMutation.reset();
268. [L419] 定义函数 `retryRestorePreview`
269. [L420] 执行：if (!selectedItem) return;
270. [L422] 执行：startPreviewRequest(selectedItem);
271. [L425] 定义函数 `restoreSelected`
272. [L426] 执行：if (!selectedItem) return;
273. [L427] 执行：restoreMutation.mutate({ item: selectedItem, restoreAsCopy });
274. [L430] 返回 JSX/结构
275. [L431] 执行：<div className="mx-auto max-w-6xl space-y-4 pb-8">
276. [L432] 执行：<PageHeader
277. [L433] 执行：title="回收站"
278. [L434] 执行：subtitle="恢复已删除的日程、任务、日历本和任务本"
279. [L435] 执行：actions={
280. [L436] 执行：<Link to="/settings" className="pim-button-secondary px-3 py-1.5 text-sm">
281. [L442] 执行：<OperationResultBanner result={result} onDismiss={() => setResult(null)} />
282. [L444] 执行：<section className="pim-panel flex flex-wrap items-center gap-3 p-4">
283. [L445] 执行：<label className="flex min-w-44 flex-col gap-1 text-sm">
284. [L446] 执行：<span className="text-xs font-medium text-slate-500">类型</span>
285. [L448] 执行：value={type}
286. [L449] 执行：onChange={event => setType(event.target.value as RecycleType)}
287. [L450] 执行：className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:bor
288. [L452] 执行：{typeOptions.map(option => (
289. [L453] 执行：<option key={option.value} value={option.value}>
290. [L454] 执行：{option.label}
291. [L455] 执行：</option>
292. [L457] 执行：</select>
293. [L460] 执行：<label className="flex min-w-64 flex-1 flex-col gap-1 text-sm">
294. [L461] 执行：<span className="text-xs font-medium text-slate-500">搜索</span>
295. [L463] 执行：type="search"
296. [L464] 执行：value={search}
297. [L465] 执行：onChange={event => setSearch(event.target.value)}
298. [L466] 执行：placeholder="搜索标题或原本所属"
299. [L467] 执行：className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transitio
300. [L471] 执行：<div className="ml-auto self-end text-sm text-slate-500">
301. [L472] 执行：{listQuery.isFetching ? '正在刷新...' : `共 ${listQuery.data?.totalCount ?? 0} 项`}
302. [L474] 执行：</section>
303. [L476] 执行：<section className="pim-panel overflow-hidden">
304. [L477] 执行：{listQuery.isLoading ? (
305. [L478] 执行：<div className="px-4 py-10 text-center text-sm text-slate-500">正在加载回收站...</div>
306. [L479] 执行：) : listQuery.isError ? (
307. [L480] 执行：<div className="p-4">
308. [L481] 执行：<EmptyState
309. [L482] 执行：title="回收站加载失败"
310. [L483] 执行：description={getErrorMessage(listQuery.error)}
311. [L486] 执行：type="button"
312. [L487] 执行：onClick={() => void listQuery.refetch()}
313. [L488] 执行：className="pim-button-secondary px-3 py-1.5 text-sm"
314. [L491] 执行：</button>
315. [L495] 执行：) : items.length === 0 ? (
316. [L496] 执行：<div className="p-4">
317. [L497] 执行：<EmptyState title="回收站为空" description="删除的日程、任务、日历本和任务本会显示在这里。" />
318. [L500] 执行：<div className="overflow-x-auto">
319. [L501] 执行：<table className="min-w-full text-left text-sm">
320. [L502] 执行：<thead className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase text-slate-500">
321. [L504] 执行：<th className="px-4 py-3">类型</th>
322. [L505] 执行：<th className="px-4 py-3">标题</th>
323. [L506] 执行：<th className="px-4 py-3">原本所属</th>
324. [L507] 执行：<th className="px-4 py-3">删除时间</th>
325. [L508] 执行：<th className="px-4 py-3 text-right">操作</th>
326. [L511] 执行：<tbody className="divide-y divide-slate-100">
327. [L512] 执行：{items.map(item => (
328. [L513] 执行：<tr key={`${item.type}:${item.id}`} className="transition-colors hover:bg-slate-50">
329. [L514] 执行：<td className="whitespace-nowrap px-4 py-3 text-slate-600">{typeLabel(item.type)}</td>
330. [L515] 执行：<td className="min-w-56 px-4 py-3">
331. [L516] 执行：<p className="font-medium text-slate-950">{item.title || '未命名项目'}</p>
332. [L517] 执行：{(item.start || item.end) && (
333. [L518] 执行：<p className="mt-1 text-xs text-slate-500">{formatDateTime(item.start || item.end)}</p>
334. [L521] 执行：<td className="whitespace-nowrap px-4 py-3 text-slate-600">{item.bookName || '-'}</td>
335. [L522] 执行：<td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatDateTime(item.deletedAt)}</td>
336. [L523] 执行：<td className="whitespace-nowrap px-4 py-3 text-right">
337. [L525] 执行：type="button"
338. [L526] 执行：onClick={() => openRestorePreview(item)}
339. [L527] 执行：disabled={previewMutation.isPending || restoreMutation.isPending}
340. [L528] 执行：className="pim-button-secondary px-3 py-1.5 text-sm disabled:cursor-not-allowed disabled:opacity-60"
341. [L531] 执行：</button>
342. [L539] 执行：</section>
343. [L541] 执行：{selectedItem && (
344. [L542] 执行：<RestorePreviewDialog
345. [L543] 执行：item={selectedItem}
346. [L544] 执行：preview={preview}
347. [L545] 执行：isLoading={isPreviewLoading}
348. [L546] 执行：previewError={previewError}
349. [L547] 执行：restoreError={restoreMutation.error}
350. [L548] 执行：isRestoring={restoreMutation.isPending}
351. … 其余约 3 条有效逻辑行同序压缩（源文件共 556 行）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/RecycleBinPage.tsx",
      "label": "RecycleBinPage",
      "path": "src/client-web/src/pages/RecycleBinPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/RecycleBinPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/RecycleBinPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RecycleBinPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RecycleBinPage.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RecycleBinPage.tsx",
      "to": "src/client-web/src/ui/OperationResultBanner.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RecycleBinPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
