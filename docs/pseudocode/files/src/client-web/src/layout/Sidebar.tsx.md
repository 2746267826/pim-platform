# src/client-web/src/layout/Sidebar.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：`Sidebar`：见源文件职责（Sidebar.tsx）。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/auth/AuthContext.tsx`、`src/client-web/src/components/status/SidebarStatusIndicator.tsx`、`src/client-web/src/context/CalendarVisibilityContext.tsx`、`src/client-web/src/ui/ConfirmActionDialog.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### startRename
#### startRename(id: string, currentName: string)
- 输入：id: string, currentName: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `startRename`
  2. 更新状态 setEditingId(id)
  3. 更新状态 setEditName(currentName)
- 分支与异常：无显著分支
- 调用：startRename、setEditingId、setEditName

### submitRename
#### submitRename(id: string)
- 输入：id: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `submitRename`
  2. 执行：if (editName.trim()) updateMut.mutate({ id, data: { name: editName.trim() } });
- 分支与异常：if (editName.trim()) updateMut.mutate({ id, data: { name: editName.trim() } });
- 调用：submitRename、editName.trim、updateMut.mutate

### isActiveDeletePreviewRequest
#### isActiveDeletePreviewRequest(id: string, requestId: number)
- 输入：id: string, requestId: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `isActiveDeletePreviewRequest`
  2. 返回 activeDeletePreviewRequestRef.current?.deleteId === id
  3. 执行：&& activeDeletePreviewRequestRef.current.requestId === requestId;
- 分支与异常：无显著分支
- 调用：isActiveDeletePreviewRequest

### requestDeletePreview
#### requestDeletePreview(id: string)
- 输入：id: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `requestDeletePreview`
  2. 赋值 `requestId` = nextDeletePreviewRequestIdRef.current + 1
  3. 执行：nextDeletePreviewRequestIdRef.current = requestId;
  4. 执行：activeDeletePreviewRequestRef.current = { deleteId: id, requestId };
  5. 更新状态 setDeleteId(id)
  6. 更新状态 setDeleteInput(null)
  7. 更新状态 setDeleteError(null)
  8. 执行：previewDeleteMut.mutate(id, {
  9. 执行：onSuccess: preview => {
  10. 若 (isActiveDeletePreviewRequest(id, requestId)) 则
  11. 执行：setDeleteInput({
  12. 执行：targetType: preview.targetType,
  13. 执行：title: preview.title,
  14. 执行：affectedCount: Math.max(1, preview.affectedCount),
  15. 执行：samples: preview.samples,
  16. 执行：onError: () => {
  17. 执行：activeDeletePreviewRequestRef.current = null;
  18. 更新状态 setDeleteId(null)
  19. 更新状态 setDeleteError('删除预览失败，请稍后重试。')
- 分支与异常：if (isActiveDeletePreviewRequest(id, requestId)) {
- 调用：requestDeletePreview、setDeleteId、setDeleteInput、setDeleteError、previewDeleteMut.mutate、isActiveDeletePreviewRequest、Math.max

### cancelDelete
#### cancelDelete(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `cancelDelete`
  2. 执行：activeDeletePreviewRequestRef.current = null;
  3. 更新状态 setDeleteInput(null)
  4. 更新状态 setDeleteId(null)
- 分支与异常：无显著分支
- 调用：cancelDelete、setDeleteInput、setDeleteId

### confirmDelete
#### confirmDelete(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `confirmDelete`
  2. 执行：if (deleteId) deleteMut.mutate(deleteId);
- 分支与异常：if (deleteId) deleteMut.mutate(deleteId);
- 调用：confirmDelete、deleteMut.mutate

### Sidebar
#### Sidebar(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `Sidebar`
  2. 赋值 `navigate` = useNavigate()
  3. 赋值 `location` = useLocation()
  4. 赋值 `{ logout, username }` = useAuth()
  5. 赋值 `{ data: calendars = [] }` = useQuery({
  6. 执行：queryKey: ['calendars', 'calendar'],
  7. 执行：queryFn: () => getCalendars('calendar')
  8. 赋值 `{ data: taskBooks = [] }` = useQuery({
  9. 执行：queryKey: ['calendars', 'task'],
  10. 执行：queryFn: () => getCalendars('task')
  11. 返回 JSX/结构
  12. 执行：<aside className="flex h-full w-[220px] flex-col border-r border-slate-200/80 bg-white/90">
  13. 执行：<div className="px-4 py-5">
  14. 执行：<p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">PIM</p>
  15. 执行：<p className="mt-1 text-lg font-semibold text-slate-950">个人中枢</p>
  16. 执行：<SidebarStatusIndicator />
  17. 执行：<nav className="flex-1 space-y-1 overflow-auto px-3 pb-3">
  18. 执行：{primaryNavItems.map(item => {
  19. 赋值 `active` = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`)
  20. 执行：key={item.path}
  21. 执行：onClick={() => navigate(item.path)}
  22. 执行：aria-current={active ? 'page' : undefined}
  23. 执行：className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition-col
  24. 执行：? 'bg-blue-50 text-blue-700 shadow-[inset_0_0_0_1px_rgba(37,99,235,0.12)]'
  25. 执行：: 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
  26. 执行：<span aria-hidden="true" className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font
  27. 执行：active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'
  28. 执行：{item.short}
  29. 执行：<span>{item.label}</span>
  30. 执行：</button>
- 分支与异常：无显著分支
- 调用：Sidebar、useNavigate、useLocation、useAuth、useQuery、getCalendars、primaryNavItems.map、location.pathname.startsWith、navigate、inset_0_0_0_1px_rgba

## 近逐行中文伪代码

1. [L10] 导出符号 `primaryNavItems`
2. [L11] 执行：{ label: '今日', path: '/today', short: '今' },
3. [L12] 执行：{ label: '日历', path: '/calendar', short: '历' },
4. [L13] 执行：{ label: '工作台', path: '/workbench', short: '工' },
5. [L14] 执行：{ label: '确认', path: '/confirmations', short: '确' },
6. [L15] 执行：{ label: '数据中心', path: '/data-center', short: '数' },
7. [L16] 执行：{ label: '提醒', path: '/reminders', short: '提' },
8. [L17] 执行：{ label: '报告', path: '/reports', short: '报' },
9. [L18] 执行：{ label: '习惯', path: '/habits', short: '习' },
10. [L19] 执行：{ label: '快速记录', path: '/quick-notes', short: '记' },
11. [L20] 执行：{ label: '文件', path: '/files', short: '文' },
12. [L21] 执行：{ label: '任务', path: '/tasks', short: '任' },
13. [L22] 执行：{ label: '电脑记录', path: '/pc-tracker', short: '电' },
14. [L23] 执行：{ label: '手机记录', path: '/mobile-records', short: '机' },
15. [L24] 执行：{ label: '历史位置', path: '/location-history', short: '位' },
16. [L25] 执行：{ label: '应用知识库', path: '/app-knowledge-base', short: '库' },
17. [L26] 执行：{ label: '状态信息', path: '/status', short: '态' },
18. [L27] 执行：{ label: '设置', path: '/settings', short: '设' },
19. [L30] 定义函数 `CalendarBookSection`
20. [L33] 执行：queryKey,
21. [L36] 执行：title: string;
22. [L37] 执行：books: Array<{ id: string; name: string; color: string }>;
23. [L38] 执行：queryKey: string[];
24. [L39] 执行：kind: string;
25. [L41] 赋值 `queryClient` = useQueryClient()
26. [L42] 执行：const [editingId, setEditingId] = useState<string | null>(null);
27. [L43] 执行：const [editName, setEditName] = useState('');
28. [L44] 执行：const [newName, setNewName] = useState('');
29. [L45] 执行：const [showNew, setShowNew] = useState(false);
30. [L46] 执行：const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
31. [L47] 执行：const [deleteId, setDeleteId] = useState<string | null>(null);
32. [L48] 执行：const [deleteError, setDeleteError] = useState<string | null>(null);
33. [L49] Hook `useRef` 绑定 `activeDeletePreviewRequestRef`
34. [L50] Hook `useRef` 绑定 `nextDeletePreviewRequestIdRef`
35. [L51] 赋值 `{ hiddenCalendarIds, toggleCalendar }` = useCalendarVisibility()
36. [L53] 赋值 `createMut` = useMutation({
37. [L54] 执行：mutationFn: (data: { name: string; color?: string; kind?: string }) => createCalendar(data),
38. [L55] 执行：onSuccess: () => {
39. [L56] 执行：queryClient.invalidateQueries({ queryKey });
40. [L57] 更新状态 setNewName('')
41. [L58] 更新状态 setShowNew(false)
42. [L62] 赋值 `updateMut` = useMutation({
43. [L63] 执行：mutationFn: ({ id, data }: { id: string; data: { name?: string; color?: string } }) => updateCalendar(id, data
44. [L64] 执行：onSuccess: () => {
45. [L65] 执行：queryClient.invalidateQueries({ queryKey });
46. [L66] 更新状态 setEditingId(null)
47. [L70] 赋值 `deleteMut` = useMutation({
48. [L71] 执行：mutationFn: deleteCalendar,
49. [L72] 执行：onSuccess: () => {
50. [L73] 执行：const affectedQueryKeys: string[][] = [
51. [L74] 执行：queryKey,
52. [L75] 执行：['calendars'],
53. [L76] 执行：['calendar-recycle-bin'],
54. [L77] 执行：['events'],
55. [L78] 执行：['events-paged'],
56. [L79] 执行：['tasks'],
57. [L80] 执行：['today-sections'],
58. [L81] 执行：['today-section'],
59. [L84] 执行：affectedQueryKeys.forEach(key => {
60. [L85] 执行：queryClient.invalidateQueries({ queryKey: key });
61. [L88] 执行：activeDeletePreviewRequestRef.current = null;
62. [L89] 更新状态 setDeleteInput(null)
63. [L90] 更新状态 setDeleteId(null)
64. [L91] 更新状态 setDeleteError(null)
65. [L93] 执行：onError: () => {
66. [L94] 执行：activeDeletePreviewRequestRef.current = null;
67. [L95] 更新状态 setDeleteInput(null)
68. [L96] 更新状态 setDeleteId(null)
69. [L97] 更新状态 setDeleteError('删除失败，请稍后重试。')
70. [L101] 赋值 `previewDeleteMut` = useMutation({
71. [L102] 执行：mutationFn: previewCalendarDelete,
72. [L105] 定义函数 `startRename`
73. [L106] 更新状态 setEditingId(id)
74. [L107] 更新状态 setEditName(currentName)
75. [L110] 定义函数 `submitRename`
76. [L111] 执行：if (editName.trim()) updateMut.mutate({ id, data: { name: editName.trim() } });
77. [L114] 定义函数 `isActiveDeletePreviewRequest`
78. [L115] 返回 activeDeletePreviewRequestRef.current?.deleteId === id
79. [L116] 执行：&& activeDeletePreviewRequestRef.current.requestId === requestId;
80. [L119] 定义函数 `requestDeletePreview`
81. [L120] 赋值 `requestId` = nextDeletePreviewRequestIdRef.current + 1
82. [L122] 执行：nextDeletePreviewRequestIdRef.current = requestId;
83. [L123] 执行：activeDeletePreviewRequestRef.current = { deleteId: id, requestId };
84. [L124] 更新状态 setDeleteId(id)
85. [L125] 更新状态 setDeleteInput(null)
86. [L126] 更新状态 setDeleteError(null)
87. [L127] 执行：previewDeleteMut.mutate(id, {
88. [L128] 执行：onSuccess: preview => {
89. [L129] 若 (isActiveDeletePreviewRequest(id, requestId)) 则
90. [L130] 执行：setDeleteInput({
91. [L131] 执行：targetType: preview.targetType,
92. [L132] 执行：title: preview.title,
93. [L133] 执行：affectedCount: Math.max(1, preview.affectedCount),
94. [L134] 执行：samples: preview.samples,
95. [L138] 执行：onError: () => {
96. [L139] 若 (isActiveDeletePreviewRequest(id, requestId)) 则
97. [L140] 执行：activeDeletePreviewRequestRef.current = null;
98. [L141] 更新状态 setDeleteInput(null)
99. [L142] 更新状态 setDeleteId(null)
100. [L143] 更新状态 setDeleteError('删除预览失败，请稍后重试。')
101. [L149] 定义函数 `cancelDelete`
102. [L150] 执行：activeDeletePreviewRequestRef.current = null;
103. [L151] 更新状态 setDeleteInput(null)
104. [L152] 更新状态 setDeleteId(null)
105. [L155] 定义函数 `confirmDelete`
106. [L156] 执行：if (deleteId) deleteMut.mutate(deleteId);
107. [L159] 返回 JSX/结构
108. [L160] 执行：<div className="mt-4 border-t border-slate-200/80 pt-4">
109. [L161] 执行：<div className="mb-2 flex items-center justify-between px-2">
110. [L162] 执行：<p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{title}</p>
111. [L164] 执行：onClick={() => setShowNew(!showNew)}
112. [L165] 执行：className="h-6 w-6 rounded-full text-sm leading-none text-slate-400 transition-colors hover:bg-blue-50 hover:t
113. [L166] 执行：aria-label={`新建${title}`}
114. [L169] 执行：</button>
115. [L172] 执行：{showNew && (
116. [L173] 执行：<div className="px-2 mb-2 flex gap-1">
117. [L175] 执行：type="text" placeholder={`${title}名称`}
118. [L176] 执行：value={newName}
119. [L177] 执行：onChange={e => setNewName(e.target.value)}
120. [L178] 执行：onKeyDown={e => { if (e.key === 'Enter' && newName.trim()) createMut.mutate({ name: newName.trim(), kind }); }
121. [L179] 执行：className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 outline
122. [L180] 执行：autoFocus
123. [L183] 执行：onClick={() => newName.trim() && createMut.mutate({ name: newName.trim(), kind })}
124. [L184] 执行：disabled={createMut.isPending}
125. [L185] 执行：className="rounded-lg bg-blue-600 px-2 py-1 text-xs text-white transition-colors hover:bg-blue-700 disabled:op
126. [L188] 执行：</button>
127. [L192] 执行：{deleteError && (
128. [L193] 执行：<p className="px-2 pb-1 text-xs text-red-500">{deleteError}</p>
129. [L196] 执行：{books?.map(book => {
130. [L197] 赋值 `hidden` = hiddenCalendarIds.has(book.id)
131. [L198] 赋值 `deleteDisabled` = previewDeleteMut.isPending || deleteMut.isPending
132. [L199] 返回 JSX/结构
133. [L200] 执行：<div key={book.id} className={`group flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg
134. [L202] 执行：onClick={() => toggleCalendar(book.id)}
135. [L203] 执行：className="h-5 w-5 rounded-full border border-slate-200 text-[10px] leading-none text-slate-400 transition-col
136. [L204] 执行：title={hidden ? '显示' : '隐藏'}
137. [L206] 执行：{hidden ? '○' : '●'}
138. [L207] 执行：</button>
139. [L208] 执行：<span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: book.color }} />
140. [L209] 执行：{editingId === book.id ? (
141. [L211] 执行：type="text" value={editName}
142. [L212] 执行：onChange={e => setEditName(e.target.value)}
143. [L213] 执行：onKeyDown={e => { if (e.key === 'Enter') submitRename(book.id); if (e.key === 'Escape') setEditingId(null); }}
144. [L214] 执行：onBlur={() => submitRename(book.id)}
145. [L215] 执行：className="min-w-0 flex-1 rounded border border-slate-200 bg-white px-1 py-0.5 text-xs text-slate-700 outline-
146. [L216] 执行：autoFocus
147. [L220] 执行：className="flex-1 truncate text-xs text-slate-600 cursor-pointer"
148. [L221] 执行：onDoubleClick={() => startRename(book.id, book.name)}
149. [L222] 执行：title="双击重命名"
150. [L224] 执行：{book.name}
151. [L227] 执行：<div className="hidden group-hover:flex items-center gap-0.5">
152. [L229] 执行：onClick={() => startRename(book.id, book.name)}
153. [L230] 执行：className="rounded px-1 text-xs leading-none text-slate-400 hover:bg-blue-50 hover:text-blue-600"
154. [L231] 执行：title="重命名"
155. [L234] 执行：</button>
156. [L236] 执行：onClick={() => requestDeletePreview(book.id)}
157. [L237] 执行：disabled={deleteDisabled}
158. [L238] 执行：className="rounded px-1 text-xs leading-none text-slate-400 hover:bg-red-50 hover:text-red-500 disabled:cursor
159. [L239] 执行：title="删除"
160. [L242] 执行：</button>
161. [L248] 执行：{(!books || books.length === 0) && !showNew && (
162. [L249] 执行：<p className="px-2 py-1 text-xs text-slate-400">暂无{title}，点击 + 创建</p>
163. [L252] 执行：<ConfirmActionDialog
164. [L253] 执行：open={deleteInput !== null}
165. [L254] 执行：input={deleteInput}
166. [L255] 执行：isPending={deleteMut.isPending}
167. [L256] 执行：onCancel={cancelDelete}
168. [L257] 执行：onConfirm={confirmDelete}
169. [L263] 默认导出函数 `Sidebar`
170. [L264] 赋值 `navigate` = useNavigate()
171. [L265] 赋值 `location` = useLocation()
172. [L266] 赋值 `{ logout, username }` = useAuth()
173. [L268] 赋值 `{ data: calendars = [] }` = useQuery({
174. [L269] 执行：queryKey: ['calendars', 'calendar'],
175. [L270] 执行：queryFn: () => getCalendars('calendar')
176. [L273] 赋值 `{ data: taskBooks = [] }` = useQuery({
177. [L274] 执行：queryKey: ['calendars', 'task'],
178. [L275] 执行：queryFn: () => getCalendars('task')
179. [L278] 返回 JSX/结构
180. [L279] 执行：<aside className="flex h-full w-[220px] flex-col border-r border-slate-200/80 bg-white/90">
181. [L280] 执行：<div className="px-4 py-5">
182. [L281] 执行：<p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">PIM</p>
183. [L282] 执行：<p className="mt-1 text-lg font-semibold text-slate-950">个人中枢</p>
184. [L284] 执行：<SidebarStatusIndicator />
185. [L286] 执行：<nav className="flex-1 space-y-1 overflow-auto px-3 pb-3">
186. [L287] 执行：{primaryNavItems.map(item => {
187. [L288] 赋值 `active` = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`)
188. [L290] 返回 JSX/结构
189. [L292] 执行：key={item.path}
190. [L293] 执行：onClick={() => navigate(item.path)}
191. [L294] 执行：aria-current={active ? 'page' : undefined}
192. [L295] 执行：className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition-col
193. [L297] 执行：? 'bg-blue-50 text-blue-700 shadow-[inset_0_0_0_1px_rgba(37,99,235,0.12)]'
194. [L298] 执行：: 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
195. [L301] 执行：<span aria-hidden="true" className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font
196. [L302] 执行：active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'
197. [L304] 执行：{item.short}
198. [L306] 执行：<span>{item.label}</span>
199. [L307] 执行：</button>
200. [L311] 执行：<CalendarBookSection
201. [L312] 执行：title="日历本"
202. [L313] 执行：books={calendars}
203. [L314] 执行：queryKey={['calendars']}
204. [L315] 执行：kind="calendar"
205. [L318] 执行：<CalendarBookSection
206. [L319] 执行：title="任务本"
207. [L320] 执行：books={taskBooks}
208. [L321] 执行：queryKey={['calendars']}
209. [L322] 执行：kind="task"
210. [L326] 执行：<div className="flex items-center justify-between border-t border-slate-200/80 p-3">
211. [L327] 执行：<span className="truncate text-xs text-slate-500">{username}</span>
212. [L328] 执行：<button onClick={logout} className="rounded-lg px-2 py-1 text-xs text-slate-500 hover:bg-red-50 hover:text-red

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/layout/Sidebar.tsx",
      "label": "Sidebar",
      "path": "src/client-web/src/layout/Sidebar.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/layout/Sidebar.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/layout/Sidebar.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/Sidebar.tsx",
      "to": "src/client-web/src/auth/AuthContext.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/Sidebar.tsx",
      "to": "src/client-web/src/components/status/SidebarStatusIndicator.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/Sidebar.tsx",
      "to": "src/client-web/src/context/CalendarVisibilityContext.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/Sidebar.tsx",
      "to": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "type": "depends_on"
    }
  ]
}
```
