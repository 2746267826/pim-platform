# src/client-web/src/pages/DataCenterPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `DataCenterPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/components/schedule/DataCenterBatchPreview.tsx`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatDateTime
#### formatDateTime(value?: string | null)
- 输入：value?: string | null
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatDateTime`
  2. 执行：if (!value) return '暂无';
  3. 赋值 `date` = new Date(value)
  4. 执行：if (Number.isNaN(date.getTime())) return value;
  5. 返回 date.toLocaleString()
- 分支与异常：if (!value) return '暂无';；if (Number.isNaN(date.getTime())) return value;
- 调用：formatDateTime、Date、Number.isNaN、date.getTime、date.toLocaleString

### readableObjectType
#### readableObjectType(value?: string | null)
- 输入：value?: string | null
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `readableObjectType`
  2. 执行：const labels: Record<string, string> = {
  3. 执行：event: '日程',
  4. 执行：task: '任务',
  5. 执行：'task-segment': '任务片段',
  6. 执行：habit: '习惯',
  7. 执行：reminder: '提醒',
  8. 执行：report: '报告',
  9. 执行：'sync-batch': '同步批次',
  10. 执行：'sync-conflict': '同步冲突',
  11. 执行：'audit-version': '审计版本',
  12. 返回 value ? labels[value] ?? value : '全部对象'
- 分支与异常：无显著分支
- 调用：readableObjectType

### DataCenterPage
#### DataCenterPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `DataCenterPage`
  2. 执行：const [search, setSearch] = useState('');
  3. 执行：const [objectType, setObjectType] = useState('');
  4. 执行：const [source, setSource] = useState('');
  5. 执行：const [pendingOnly, setPendingOnly] = useState(false);
  6. 执行：const [outlookOnly, setOutlookOnly] = useState(false);
  7. 执行：const [selectedObjectId, setSelectedObjectId] = useState<string | null>(null);
  8. Hook `useMemo` 绑定 `request`
  9. 执行：search: search.trim() || null,
  10. 执行：objectType: objectType || null,
  11. 执行：source: outlookOnly ? 'outlook' : source || null,
  12. 执行：pendingOnly,
  13. 执行：pageSize: 50,
  14. 赋值 `{ data, isLoading, isError, error }` = useQuery({
  15. 执行：queryKey: ['data-center-query', request],
  16. 执行：queryFn: () => queryDataCenter(request),
  17. 赋值 `exportMutation` = useMutation({
  18. 执行：mutationFn: getAuditExport,
  19. 赋值 `restorePreviewMutation` = useMutation({
  20. 执行：mutationFn: (auditVersionId: string) => previewDataCenterRestore(auditVersionId, '数据中心版本恢复预览'),
  21. 赋值 `items` = data?.items ?? []
  22. 赋值 `selected` = items.find(item => item.objectId === selectedObjectId) ?? items[0]
  23. 赋值 `selectedKey` = selected ? `${selected.objectType}-${selected.objectId}` : null
  24. 定义函数 `selectRow`
  25. 更新状态 setSelectedObjectId(item.objectId)
  26. 返回 JSX/结构
  27. 执行：<div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
  28. 执行：<PageHeader
  29. 执行：title="数据中心"
  30. 执行：subtitle="跨日程、任务、习惯、提醒、报告和同步来源进行全局治理、审计导出与版本恢复。"
- 分支与异常：无显著分支
- 调用：DataCenterPage、useState、useMemo、search.trim、useQuery、queryDataCenter、useMutation、previewDataCenterRestore、items.find、selectRow、setSelectedObjectId、exportMutation.mutate、minmax、setSearch、setObjectType

## 近逐行中文伪代码

1. [L5] 执行：getAuditExport,
2. [L6] 执行：previewDataCenterRestore,
3. [L7] 执行：queryDataCenter,
4. [L13] 定义函数 `formatDateTime`
5. [L14] 执行：if (!value) return '暂无';
6. [L15] 赋值 `date` = new Date(value)
7. [L16] 执行：if (Number.isNaN(date.getTime())) return value;
8. [L17] 返回 date.toLocaleString()
9. [L20] 定义函数 `readableObjectType`
10. [L21] 执行：const labels: Record<string, string> = {
11. [L22] 执行：event: '日程',
12. [L23] 执行：task: '任务',
13. [L24] 执行：'task-segment': '任务片段',
14. [L25] 执行：habit: '习惯',
15. [L26] 执行：reminder: '提醒',
16. [L27] 执行：report: '报告',
17. [L28] 执行：'sync-batch': '同步批次',
18. [L29] 执行：'sync-conflict': '同步冲突',
19. [L30] 执行：'audit-version': '审计版本',
20. [L33] 返回 value ? labels[value] ?? value : '全部对象'
21. [L36] 默认导出函数 `DataCenterPage`
22. [L37] 执行：const [search, setSearch] = useState('');
23. [L38] 执行：const [objectType, setObjectType] = useState('');
24. [L39] 执行：const [source, setSource] = useState('');
25. [L40] 执行：const [pendingOnly, setPendingOnly] = useState(false);
26. [L41] 执行：const [outlookOnly, setOutlookOnly] = useState(false);
27. [L42] 执行：const [selectedObjectId, setSelectedObjectId] = useState<string | null>(null);
28. [L44] Hook `useMemo` 绑定 `request`
29. [L45] 执行：search: search.trim() || null,
30. [L46] 执行：objectType: objectType || null,
31. [L47] 执行：source: outlookOnly ? 'outlook' : source || null,
32. [L48] 执行：pendingOnly,
33. [L50] 执行：pageSize: 50,
34. [L53] 赋值 `{ data, isLoading, isError, error }` = useQuery({
35. [L54] 执行：queryKey: ['data-center-query', request],
36. [L55] 执行：queryFn: () => queryDataCenter(request),
37. [L58] 赋值 `exportMutation` = useMutation({
38. [L59] 执行：mutationFn: getAuditExport,
39. [L62] 赋值 `restorePreviewMutation` = useMutation({
40. [L63] 执行：mutationFn: (auditVersionId: string) => previewDataCenterRestore(auditVersionId, '数据中心版本恢复预览'),
41. [L66] 赋值 `items` = data?.items ?? []
42. [L67] 赋值 `selected` = items.find(item => item.objectId === selectedObjectId) ?? items[0]
43. [L68] 赋值 `selectedKey` = selected ? `${selected.objectType}-${selected.objectId}` : null
44. [L70] 定义函数 `selectRow`
45. [L71] 更新状态 setSelectedObjectId(item.objectId)
46. [L74] 返回 JSX/结构
47. [L75] 执行：<div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
48. [L76] 执行：<PageHeader
49. [L77] 执行：title="数据中心"
50. [L78] 执行：subtitle="跨日程、任务、习惯、提醒、报告和同步来源进行全局治理、审计导出与版本恢复。"
51. [L79] 执行：actions={
52. [L81] 执行：type="button"
53. [L82] 执行：onClick={() => exportMutation.mutate()}
54. [L83] 执行：disabled={exportMutation.isPending}
55. [L84] 执行：className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
56. [L87] 执行：</button>
57. [L91] 执行：<section className="pim-panel p-4">
58. [L92] 执行：<div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(220px,1fr)_180px_180px_auto_auto]">
59. [L93] 执行：<label className="min-w-0">
60. [L94] 执行：<span className="text-xs font-semibold text-slate-500">全局搜索</span>
61. [L96] 执行：value={search}
62. [L97] 执行：onChange={event => setSearch(event.target.value)}
63. [L98] 执行：placeholder="标题、摘要、来源对象"
64. [L99] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
65. [L103] 执行：<span className="text-xs font-semibold text-slate-500">对象过滤</span>
66. [L105] 执行：value={objectType}
67. [L106] 执行：onChange={event => setObjectType(event.target.value)}
68. [L107] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
69. [L109] 执行：<option value="">全部对象</option>
70. [L110] 执行：<option value="event">日程</option>
71. [L111] 执行：<option value="task">任务</option>
72. [L112] 执行：<option value="task-segment">任务片段</option>
73. [L113] 执行：<option value="habit">习惯</option>
74. [L114] 执行：<option value="reminder">提醒</option>
75. [L115] 执行：<option value="report">报告</option>
76. [L116] 执行：<option value="sync-batch">同步批次</option>
77. [L117] 执行：<option value="sync-conflict">同步冲突</option>
78. [L118] 执行：<option value="audit-version">审计版本</option>
79. [L119] 执行：</select>
80. [L122] 执行：<span className="text-xs font-semibold text-slate-500">来源过滤</span>
81. [L124] 执行：value={source}
82. [L125] 执行：onChange={event => setSource(event.target.value)}
83. [L126] 执行：disabled={outlookOnly}
84. [L127] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border
85. [L129] 执行：<option value="">全部来源</option>
86. [L130] 执行：<option value="pim">PIM</option>
87. [L131] 执行：<option value="outlook">Outlook</option>
88. [L132] 执行：<option value="manual">手动</option>
89. [L133] 执行：<option value="ai">智能</option>
90. [L134] 执行：</select>
91. [L136] 执行：<label className="flex items-end gap-2 pb-2 text-sm font-medium text-slate-700">
92. [L138] 执行：type="checkbox"
93. [L139] 执行：checked={pendingOnly}
94. [L140] 执行：onChange={event => setPendingOnly(event.target.checked)}
95. [L141] 执行：className="h-4 w-4 rounded border-slate-300"
96. [L145] 执行：<label className="flex items-end gap-2 pb-2 text-sm font-medium text-slate-700">
97. [L147] 执行：type="checkbox"
98. [L148] 执行：checked={outlookOnly}
99. [L149] 执行：onChange={event => setOutlookOnly(event.target.checked)}
100. [L150] 执行：className="h-4 w-4 rounded border-slate-300"
101. [L152] 执行：Outlook-only
102. [L155] 执行：</section>
103. [L157] 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(340px,1fr)]">
104. [L158] 执行：<section className="pim-panel min-w-0 overflow-hidden">
105. [L159] 执行：<div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 px-4 py-3">
106. [L161] 执行：<h2 className="text-sm font-semibold text-slate-950">治理对象</h2>
107. [L162] 执行：<p className="mt-1 text-xs text-slate-500">
108. [L163] 执行：包含回收站、同步批次、审计时间线和待确认变更入口。
109. [L166] 执行：<span className="text-xs text-slate-500">{data?.totalCount ?? 0} 个对象</span>
110. [L169] 执行：{isLoading ? (
111. [L170] 执行：<p className="px-4 py-8 text-center text-sm text-slate-500">正在加载数据中心对象。</p>
112. [L171] 执行：) : isError ? (
113. [L172] 执行：<p className="px-4 py-8 text-center text-sm text-red-600">
114. [L173] 执行：{error instanceof Error ? error.message : '数据中心查询失败'}
115. [L175] 执行：) : items.length === 0 ? (
116. [L176] 执行：<p className="px-4 py-8 text-center text-sm text-slate-500">当前筛选下没有对象。</p>
117. [L178] 执行：<div className="overflow-auto">
118. [L179] 执行：<table className="min-w-full divide-y divide-slate-200 text-left text-sm">
119. [L180] 执行：<thead className="bg-slate-50 text-xs text-slate-500">
120. [L182] 执行：<th className="px-4 py-3 font-semibold">标题</th>
121. [L183] 执行：<th className="px-4 py-3 font-semibold">对象</th>
122. [L184] 执行：<th className="px-4 py-3 font-semibold">来源</th>
123. [L185] 执行：<th className="px-4 py-3 font-semibold">状态</th>
124. [L186] 执行：<th className="px-4 py-3 font-semibold">开始</th>
125. [L189] 执行：<tbody className="divide-y divide-slate-100">
126. [L190] 执行：{items.map(item => {
127. [L191] 赋值 `rowKey` = `${item.objectType}-${item.objectId}`
128. [L193] 返回 JSX/结构
129. [L195] 执行：key={rowKey}
130. [L196] 执行：onClick={() => selectRow(item)}
131. [L197] 执行：className={`cursor-pointer transition-colors hover:bg-blue-50 ${
132. [L198] 执行：selectedKey === rowKey ? 'bg-blue-50' : 'bg-white'
133. [L201] 执行：<td className="max-w-[300px] px-4 py-3">
134. [L202] 执行：<p className="truncate font-medium text-slate-800">{item.title}</p>
135. [L203] 执行：<p className="mt-1 truncate text-xs text-slate-500">{item.summary}</p>
136. [L205] 执行：<td className="px-4 py-3 text-slate-600">{readableObjectType(item.objectType)}</td>
137. [L206] 执行：<td className="px-4 py-3 text-slate-600">{item.source}</td>
138. [L207] 执行：<td className="px-4 py-3 text-slate-600">{item.status}</td>
139. [L208] 执行：<td className="whitespace-nowrap px-4 py-3 text-slate-500">{formatDateTime(item.startsAt)}</td>
140. [L216] 执行：</section>
141. [L218] 执行：<div className="space-y-4">
142. [L219] 执行：<section className="pim-panel min-w-0 p-4">
143. [L220] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
144. [L221] 执行：<h2 className="text-sm font-semibold text-slate-950">对象详情</h2>
145. [L222] 执行：{selected && (
146. [L224] 执行：to={`/audit/${encodeURIComponent(selected.objectType)}/${encodeURIComponent(selected.objectId)}`}
147. [L225] 执行：className="pim-button-secondary px-3 py-1.5 text-xs"
148. [L232] 执行：{selected ? (
149. [L233] 执行：<div className="mt-4 space-y-3 text-sm">
150. [L234] 执行：<dl className="grid grid-cols-1 gap-2">
151. [L236] 执行：['标题', selected.title],
152. [L237] 执行：['对象 ID', selected.objectId],
153. [L238] 执行：['对象类型', readableObjectType(selected.objectType)],
154. [L239] 执行：['来源', selected.source],
155. [L240] 执行：['状态', selected.status],
156. [L241] 执行：['开始', formatDateTime(selected.startsAt)],
157. [L242] 执行：['结束', formatDateTime(selected.endsAt)],
158. [L243] 执行：['摘要', selected.summary],
159. [L244] 执行：].map(([label, value]) => (
160. [L245] 执行：<div key={label} className="rounded-lg bg-slate-50 px-3 py-2">
161. [L246] 执行：<dt className="text-xs font-semibold text-slate-400">{label}</dt>
162. [L247] 执行：<dd className="mt-1 break-words text-slate-800">{value}</dd>
163. [L252] 执行：<div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
164. [L253] 执行：<button type="button" className="pim-button-secondary px-3 py-2 text-sm">
165. [L255] 执行：</button>
166. [L257] 执行：type="button"
167. [L258] 执行：onClick={() => restorePreviewMutation.mutate(selected.objectId)}
168. [L259] 执行：disabled={restorePreviewMutation.isPending}
169. [L260] 执行：className="pim-button-secondary px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
170. [L263] 执行：</button>
171. [L266] 执行：{restorePreviewMutation.data && (
172. [L267] 执行：<div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
173. [L268] 执行：<p className="font-semibold">恢复预览</p>
174. [L269] 执行：<p className="mt-1 text-xs leading-5">{restorePreviewMutation.data.summary}</p>
175. [L272] 执行：{exportMutation.data && (
176. [L273] 执行：<div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
177. [L274] 执行：已生成审计导出：{exportMutation.data.fileName}
178. [L279] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-8 text-center text-sm text-slate-5
179. [L280] 执行：选择一条记录以查看审计、恢复和批量治理入口。
180. [L283] 执行：</section>
181. [L285] 执行：<DataCenterBatchPreview selected={selected} />

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/DataCenterPage.tsx",
      "label": "DataCenterPage",
      "path": "src/client-web/src/pages/DataCenterPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/DataCenterPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/DataCenterPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/DataCenterPage.tsx",
      "to": "src/client-web/src/components/schedule/DataCenterBatchPreview.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/DataCenterPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/DataCenterPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
