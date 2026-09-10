# src/client-web/src/pages/AppKnowledgeBasePage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `AppKnowledgeBasePage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/appKnowledge.ts`、`src/client-web/src/api/appSignatures.ts`、`src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx`、`src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatRecentDuration
#### formatRecentDuration(seconds: number)
- 输入：seconds: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatRecentDuration`
  2. 赋值 `minutes` = Math.round(seconds / 60)
  3. 返回 `${minutes.toLocaleString()} 分钟`
- 分支与异常：无显著分支
- 调用：formatRecentDuration、Math.round、minutes.toLocaleString

### getSourceLabel
#### getSourceLabel(source: string)
- 输入：source: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getSourceLabel`
  2. 若 (source === 'builtin') 则
  3. 返回 '内置'
  4. 若 (source === 'learned') 则
  5. 返回 '学习'
  6. 返回 '自定义'
- 分支与异常：if (source === 'builtin') {；if (source === 'learned') {
- 调用：getSourceLabel

### AppKnowledgeBasePage
#### AppKnowledgeBasePage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `AppKnowledgeBasePage`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [search, setSearch] = useState('');
  4. 执行：const [selectedAppId, setSelectedAppId] = useState<string | null>(null);
  5. 执行：const [showAddForm, setShowAddForm] = useState(false);
  6. 执行：const [form, setForm] = useState({
  7. 执行：processName: '',
  8. 执行：displayName: '',
  9. 执行：categoryPath: '',
  10. 执行：productivity: 'neutral',
  11. 执行：icon: '',
  12. 执行：description: '',
  13. 赋值 `{ data: apps = [], isLoading }` = useQuery({
  14. 执行：queryKey: ['app-knowledge-apps', search],
  15. 执行：queryFn: () => getAppKnowledgeApps(search || undefined),
  16. 赋值 `selectedApp` = apps.find(app => app.id === selectedAppId) ?? null
  17. 注册 `useEffect` 副作用
  18. 若 (apps.length === 0) 则
  19. 若 (selectedAppId !== null) 则
  20. 更新状态 setSelectedAppId(null)
  21. 返回（空）
  22. 若 (!selectedAppId || !apps.some(app => app.id === selectedAppId)) 则
  23. 更新状态 setSelectedAppId(apps[0].id)
  24. 赋值 `{ data: contexts = [], isLoading: contex` = useQuery({
  25. 执行：queryKey: ['app-knowledge-contexts', selectedAppId],
  26. 执行：queryFn: () => selectedAppId ? getAppKnowledgeContexts(selectedAppId) : Promise.resolve([]),
  27. 执行：enabled: selectedAppId !== null,
  28. 赋值 `createMut` = useMutation({
  29. 执行：mutationFn: () => createAppSignature({
  30. 执行：processName: form.processName.trim(),
- 分支与异常：if (apps.length === 0) {；if (selectedAppId !== null) {；if (!selectedAppId || !apps.some(app => app.id === selectedAppId)) {；if (selectedAppId === id) {；if (app.source === 'builtin') {；if (confirm(`确定删除「${app.displayName}」？`)) {；if (confirm('确认删除这个上下文知识模式？')) {
- 调用：AppKnowledgeBasePage、useQueryClient、useState、useQuery、getAppKnowledgeApps、apps.find、useEffect、setSelectedAppId、apps.some、getAppKnowledgeContexts、Promise.resolve、useMutation、createAppSignature、form.processName.trim、form.displayName.trim

## 近逐行中文伪代码

1. [L4] 执行：deleteAppKnowledgeContext,
2. [L5] 执行：getAppKnowledgeApps,
3. [L6] 执行：getAppKnowledgeContexts,
4. [L13] 赋值 `productivities` = [
5. [L14] 执行：{ value: 'productive', label: '✅ 高效率', color: 'text-green-600 bg-green-50' },
6. [L15] 执行：{ value: 'neutral', label: '➖ 中性', color: 'text-slate-600 bg-slate-50' },
7. [L16] 执行：{ value: 'distracting', label: '❌ 分散精力', color: 'text-red-600 bg-red-50' },
8. [L19] 定义函数 `formatRecentDuration`
9. [L20] 赋值 `minutes` = Math.round(seconds / 60)
10. [L21] 返回 `${minutes.toLocaleString()} 分钟`
11. [L24] 定义函数 `getSourceLabel`
12. [L25] 若 (source === 'builtin') 则
13. [L26] 返回 '内置'
14. [L29] 若 (source === 'learned') 则
15. [L30] 返回 '学习'
16. [L33] 返回 '自定义'
17. [L36] 默认导出函数 `AppKnowledgeBasePage`
18. [L37] 赋值 `queryClient` = useQueryClient()
19. [L38] 执行：const [search, setSearch] = useState('');
20. [L39] 执行：const [selectedAppId, setSelectedAppId] = useState<string | null>(null);
21. [L40] 执行：const [showAddForm, setShowAddForm] = useState(false);
22. [L41] 执行：const [form, setForm] = useState({
23. [L42] 执行：processName: '',
24. [L43] 执行：displayName: '',
25. [L44] 执行：categoryPath: '',
26. [L45] 执行：productivity: 'neutral',
27. [L46] 执行：icon: '',
28. [L47] 执行：description: '',
29. [L50] 赋值 `{ data: apps = [], isLoading }` = useQuery({
30. [L51] 执行：queryKey: ['app-knowledge-apps', search],
31. [L52] 执行：queryFn: () => getAppKnowledgeApps(search || undefined),
32. [L55] 赋值 `selectedApp` = apps.find(app => app.id === selectedAppId) ?? null
33. [L57] 注册 `useEffect` 副作用
34. [L58] 若 (apps.length === 0) 则
35. [L59] 若 (selectedAppId !== null) 则
36. [L60] 更新状态 setSelectedAppId(null)
37. [L62] 返回（空）
38. [L65] 若 (!selectedAppId || !apps.some(app => app.id === selectedAppId)) 则
39. [L66] 更新状态 setSelectedAppId(apps[0].id)
40. [L70] 赋值 `{ data: contexts = [], isLoading: contex` = useQuery({
41. [L71] 执行：queryKey: ['app-knowledge-contexts', selectedAppId],
42. [L72] 执行：queryFn: () => selectedAppId ? getAppKnowledgeContexts(selectedAppId) : Promise.resolve([]),
43. [L73] 执行：enabled: selectedAppId !== null,
44. [L76] 赋值 `createMut` = useMutation({
45. [L77] 执行：mutationFn: () => createAppSignature({
46. [L78] 执行：processName: form.processName.trim(),
47. [L79] 执行：displayName: form.displayName.trim(),
48. [L80] 执行：categoryPath: form.categoryPath.trim() || undefined,
49. [L81] 执行：productivity: form.productivity,
50. [L82] 执行：icon: form.icon.trim() || undefined,
51. [L83] 执行：description: form.description.trim() || undefined,
52. [L85] 执行：onSuccess: () => {
53. [L86] 执行：queryClient.invalidateQueries({ queryKey: ['app-knowledge-apps'] });
54. [L87] 执行：queryClient.invalidateQueries({ queryKey: ['app-signatures'] });
55. [L88] 更新状态 setShowAddForm(false)
56. [L89] 更新状态 setForm({ processName: '', displayName: '', categoryPath: '', productivity: 'neutral', i)
57. [L93] 赋值 `deleteMut` = useMutation({
58. [L94] 执行：mutationFn: (id: string) => deleteAppSignature(id),
59. [L95] 执行：onSuccess: (_result, id) => {
60. [L96] 执行：queryClient.invalidateQueries({ queryKey: ['app-knowledge-apps'] });
61. [L97] 执行：queryClient.invalidateQueries({ queryKey: ['app-knowledge-contexts'] });
62. [L98] 执行：queryClient.invalidateQueries({ queryKey: ['app-signatures'] });
63. [L99] 若 (selectedAppId === id) 则
64. [L100] 更新状态 setSelectedAppId(null)
65. [L105] 赋值 `contextDeleteMut` = useMutation({
66. [L106] 执行：mutationFn: (id: string) => deleteAppKnowledgeContext(id),
67. [L107] 执行：onSuccess: () => {
68. [L108] 执行：queryClient.invalidateQueries({ queryKey: ['app-knowledge-apps'] });
69. [L109] 执行：queryClient.invalidateQueries({ queryKey: ['app-knowledge-contexts'] });
70. [L113] 返回 JSX/结构
71. [L114] 执行：<div className="space-y-4">
72. [L115] 执行：<PageHeader
73. [L116] 执行：title="App 知识库"
74. [L117] 执行：subtitle="管理应用、域名、标题模式和分类归属知识"
75. [L119] 执行：<AppKnowledgeTabs active="apps" />
76. [L121] 执行：{/* Search + Add toolbar */}
77. [L122] 执行：<div className="flex flex-wrap items-center gap-3">
78. [L124] 执行：type="text"
79. [L125] 执行：placeholder="搜索应用名称或进程名..."
80. [L126] 执行：value={search}
81. [L127] 执行：onChange={e => setSearch(e.target.value)}
82. [L128] 执行：className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 outline
83. [L131] 执行：type="button"
84. [L132] 执行：onClick={() => setShowAddForm(!showAddForm)}
85. [L133] 执行：className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700
86. [L135] 执行：{showAddForm ? '取消' : '+ 添加应用'}
87. [L136] 执行：</button>
88. [L139] 执行：{/* Add form */}
89. [L140] 执行：{showAddForm && (
90. [L141] 执行：<div className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
91. [L142] 执行：<div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
92. [L144] 执行：<label className="text-xs font-medium text-slate-500">进程名 *</label>
93. [L146] 执行：type="text"
94. [L147] 执行：value={form.processName}
95. [L148] 执行：onChange={e => setForm(f => ({ ...f, processName: e.target.value }))}
96. [L149] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
97. [L153] 执行：<label className="text-xs font-medium text-slate-500">显示名称 *</label>
98. [L155] 执行：type="text"
99. [L156] 执行：value={form.displayName}
100. [L157] 执行：onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))}
101. [L158] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
102. [L162] 执行：<label className="text-xs font-medium text-slate-500">分类路径（如 工作·编程）</label>
103. [L164] 执行：type="text"
104. [L165] 执行：value={form.categoryPath}
105. [L166] 执行：onChange={e => setForm(f => ({ ...f, categoryPath: e.target.value }))}
106. [L167] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
107. [L171] 执行：<label className="text-xs font-medium text-slate-500">效率评分</label>
108. [L173] 执行：value={form.productivity}
109. [L174] 执行：onChange={e => setForm(f => ({ ...f, productivity: e.target.value }))}
110. [L175] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
111. [L177] 执行：{productivities.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
112. [L178] 执行：</select>
113. [L181] 执行：<label className="text-xs font-medium text-slate-500">Emoji 图标</label>
114. [L183] 执行：type="text"
115. [L184] 执行：value={form.icon}
116. [L185] 执行：onChange={e => setForm(f => ({ ...f, icon: e.target.value }))}
117. [L186] 执行：placeholder="🎮"
118. [L187] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
119. [L191] 执行：<label className="text-xs font-medium text-slate-500">描述</label>
120. [L193] 执行：type="text"
121. [L194] 执行：value={form.description}
122. [L195] 执行：onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
123. [L196] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
124. [L200] 执行：<div className="flex justify-end">
125. [L202] 执行：type="button"
126. [L203] 执行：onClick={() => createMut.mutate()}
127. [L204] 执行：disabled={!form.processName.trim() || !form.displayName.trim() || createMut.isPending}
128. [L205] 执行：className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700
129. [L207] 执行：{createMut.isPending ? '提交中...' : '保存'}
130. [L208] 执行：</button>
131. [L213] 执行：<div className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
132. [L214] 执行：<section className="min-w-0">
133. [L215] 执行：{isLoading ? (
134. [L216] 执行：<div className="py-8 text-center text-sm text-slate-500">加载中...</div>
135. [L217] 执行：) : apps.length === 0 ? (
136. [L218] 执行：<div className="py-8 text-center text-sm text-slate-500">
137. [L219] 执行：{search ? '未找到匹配的应用' : '知识库为空，点击"+ 添加应用"开始添加'}
138. [L222] 执行：<div className="overflow-x-auto rounded-lg border border-slate-200">
139. [L223] 执行：<table className="w-full text-sm">
140. [L225] 执行：<tr className="border-b border-slate-200 bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider 
141. [L226] 执行：<th className="px-3 py-2">图标</th>
142. [L227] 执行：<th className="px-3 py-2">显示名称</th>
143. [L228] 执行：<th className="px-3 py-2">进程名</th>
144. [L229] 执行：<th className="px-3 py-2">分类路径</th>
145. [L230] 执行：<th className="px-3 py-2">效率</th>
146. [L231] 执行：<th className="px-3 py-2">上下文</th>
147. [L232] 执行：<th className="px-3 py-2">来源</th>
148. [L233] 执行：<th className="px-3 py-2">操作</th>
149. [L237] 执行：{apps.map(app => {
150. [L238] 赋值 `productivity` = productivities.find(p => p.value === app.productivity)
151. [L239] 赋值 `isSelected` = app.id === selectedAppId
152. [L241] 返回 JSX/结构
153. [L243] 执行：key={app.id}
154. [L244] 执行：aria-selected={isSelected}
155. [L245] 执行：onClick={() => setSelectedAppId(app.id)}
156. [L246] 执行：className={`cursor-pointer border-b border-slate-100 transition-colors last:border-b-0 ${
157. [L247] 执行：isSelected ? 'bg-blue-50 ring-1 ring-inset ring-blue-200' : 'hover:bg-slate-50'
158. [L250] 执行：<td className="px-3 py-2.5 text-lg">{app.icon || '❓'}</td>
159. [L251] 执行：<td className="px-3 py-2.5 font-medium text-slate-900">
160. [L252] 执行：<div className="flex flex-col">
161. [L253] 执行：<span>{app.displayName}</span>
162. [L254] 执行：{isSelected && <span className="text-xs font-normal text-blue-600">正在查看上下文</span>}
163. [L257] 执行：<td className="px-3 py-2.5 font-mono text-xs text-slate-500">{app.processName}</td>
164. [L258] 执行：<td className="px-3 py-2.5 text-slate-600">{app.categoryPath || '-'}</td>
165. [L259] 执行：<td className="px-3 py-2.5">
166. [L260] 执行：{productivity ? (
167. [L261] 执行：<span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${productivity.color}`}>
168. [L262] 执行：{productivity.label}
169. [L266] 执行：<td className="px-3 py-2.5 text-slate-600">
170. [L267] 执行：<div className="flex flex-col text-xs">
171. [L268] 执行：<span>{app.contextCount.toLocaleString()} 个模式</span>
172. [L269] 执行：<span className={app.pendingContextCount > 0 ? 'text-amber-600' : 'text-slate-400'}>
173. [L270] 执行：{app.pendingContextCount.toLocaleString()} 项待确认
174. [L274] 执行：<td className="px-3 py-2.5">
175. [L275] 执行：<span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
176. [L276] 执行：app.source === 'builtin' ? 'bg-slate-100 text-slate-600' : 'bg-blue-50 text-blue-700'
177. [L279] 执行：{getSourceLabel(app.source)}
178. [L282] 执行：<td className="px-3 py-2.5">
179. [L284] 执行：type="button"
180. [L285] 执行：onClick={event => {
181. [L286] 执行：event.stopPropagation();
182. [L287] 若 (app.source === 'builtin') 则
183. [L288] 执行：alert('内置项不可删除');
184. [L289] 返回（空）
185. [L291] 若 (confirm(`确定删除「${app.displayName}」？`)) 则
186. [L292] 执行：deleteMut.mutate(app.id);
187. [L295] 执行：disabled={app.source === 'builtin' || deleteMut.isPending}
188. [L296] 执行：className={`rounded px-2 py-1 text-xs font-medium transition-colors ${
189. [L297] 执行：app.source === 'builtin'
190. [L298] 执行：? 'cursor-not-allowed text-slate-300'
191. [L299] 执行：: 'text-red-500 hover:bg-red-50'
192. [L303] 执行：</button>
193. [L312] 执行：</section>
194. [L314] 执行：<aside className="min-w-0 space-y-3 rounded-lg border border-slate-200 bg-white p-4">
195. [L315] 执行：<div className="space-y-3">
196. [L317] 执行：<p className="text-xs font-semibold uppercase tracking-wide text-slate-500">上下文模式</p>
197. [L318] 执行：<h2 className="mt-1 text-base font-semibold text-slate-900">
198. [L319] 执行：{selectedApp ? selectedApp.displayName : '选择应用'}
199. [L321] 执行：<p className="mt-1 text-xs text-slate-500">
200. [L322] 执行：{selectedApp
201. [L323] 执行：? `${selectedApp.processName} · ${formatRecentDuration(selectedApp.recentAffectedDurationSeconds)} 近期影响`
202. [L324] 执行：: '选择一行查看上下文知识模式。'}
203. [L328] 执行：{selectedApp && (
204. [L329] 执行：<div className="flex flex-wrap gap-2 text-xs text-slate-600">
205. [L330] 执行：<span className="rounded border border-slate-200 bg-slate-50 px-2 py-1">
206. [L331] 执行：{selectedApp.contextCount.toLocaleString()} 个上下文模式
207. [L333] 执行：<span className="rounded border border-slate-200 bg-slate-50 px-2 py-1">
208. [L334] 执行：{formatRecentDuration(selectedApp.recentAffectedDurationSeconds)} 近期影响
209. [L336] 执行：{selectedApp.pendingContextCount > 0 && (
210. [L337] 执行：<span className="rounded border border-amber-200 bg-amber-50 px-2 py-1 text-amber-700">
211. [L338] 执行：{selectedApp.pendingContextCount.toLocaleString()} 项待确认上下文
212. [L345] 执行：{selectedAppId ? (
213. [L346] 执行：<AppKnowledgeContextList
214. [L347] 执行：contexts={contexts}
215. [L348] 执行：isLoading={contextsLoading}
216. [L349] 执行：onDelete={id => {
217. [L350] 若 (confirm('确认删除这个上下文知识模式？')) 则
218. [L351] 执行：contextDeleteMut.mutate(id);
219. [L356] 执行：<div className="rounded border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500">
220. [L357] 执行：选择应用行以查看上下文知识。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "label": "AppKnowledgeBasePage",
      "path": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/AppKnowledgeBasePage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "to": "src/client-web/src/api/appKnowledge.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "to": "src/client-web/src/api/appSignatures.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
