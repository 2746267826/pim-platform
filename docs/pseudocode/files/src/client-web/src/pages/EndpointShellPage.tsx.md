# src/client-web/src/pages/EndpointShellPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `EndpointShellPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/endpoints.ts`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`
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

### defaultDeviceId
#### defaultDeviceId(platform: EndpointPlatform)
- 输入：platform: EndpointPlatform
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `defaultDeviceId`
  2. 返回 platform === 'android' ? 'android-companion' : 'windows-companion'
- 分支与异常：无显著分支
- 调用：defaultDeviceId

### statusTone
#### statusTone(status: string)
- 输入：status: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `statusTone`
  2. 赋值 `normalized` = status.toLowerCase()
  3. 执行：if (normalized === 'healthy') return 'bg-emerald-50 text-emerald-700';
  4. 执行：if (normalized === 'warning') return 'bg-amber-50 text-amber-700';
  5. 执行：if (normalized === 'critical') return 'bg-rose-50 text-rose-700';
  6. 返回 'bg-slate-100 text-slate-600'
- 分支与异常：if (normalized === 'healthy') return 'bg-emerald-50 text-emerald-700';；if (normalized === 'warning') return 'bg-amber-50 text-amber-700';；if (normalized === 'critical') return 'bg-rose-50 text-rose-700';
- 调用：statusTone、status.toLowerCase

### EndpointShellPage
#### EndpointShellPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `EndpointShellPage`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');
  4. 执行：const [manualDeviceId, setManualDeviceId] = useState(defaultDeviceId('windows'));
  5. 执行：const [manualPlatform, setManualPlatform] = useState<EndpointPlatform>('windows');
  6. 赋值 `{ data: endpoints = [], isLoading }` = useQuery({
  7. 执行：queryKey: ['endpoint-statuses'],
  8. 执行：queryFn: listEndpointStatuses,
  9. 执行：refetchInterval: 30_000,
  10. 注册 `useEffect` 副作用
  11. 若 (!selectedDeviceId && endpoints.length > 0) 则
  12. 更新状态 setSelectedDeviceId(endpoints[0].deviceId)
  13. Hook `useMemo` 绑定 `selectedEndpoint`
  14. 执行：endpoints.find(endpoint => endpoint.deviceId === selectedDeviceId) ?? endpoints[0]
  15. 执行：), [endpoints, selectedDeviceId]);
  16. 赋值 `qualityDeviceId` = selectedEndpoint?.deviceId || manualDeviceId
  17. 赋值 `{ data: quality }` = useQuery({
  18. 执行：queryKey: ['endpoint-collection-quality', qualityDeviceId],
  19. 执行：queryFn: () => getEndpointCollectionQuality(qualityDeviceId),
  20. 执行：enabled: Boolean(qualityDeviceId),
  21. 赋值 `heartbeatMutation` = useMutation({
  22. 执行：mutationFn: () => heartbeatEndpoint(manualDeviceId, {
  23. 执行：platform: manualPlatform,
  24. 执行：appVersion: 'web-shell',
  25. 执行：uploadStatus: 'Healthy',
  26. 执行：collectionCacheCount: selectedEndpoint?.collectionCacheCount ?? 0,
  27. 执行：onSuccess: endpoint => {
  28. 更新状态 setSelectedDeviceId(endpoint.deviceId)
  29. 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-statuses'] });
  30. 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-collection-quality', endpoint.deviceId] });
- 分支与异常：if (!selectedDeviceId && endpoints.length > 0) {
- 调用：EndpointShellPage、useQueryClient、useState、defaultDeviceId、useQuery、useEffect、setSelectedDeviceId、endpoints.find、getEndpointCollectionQuality、Boolean、useMutation、heartbeatEndpoint、queryClient.invalidateQueries、handleEndpointNotificationAction、endpoints.reduce

## 近逐行中文伪代码

1. [L4] 执行：getEndpointCollectionQuality,
2. [L5] 执行：handleEndpointNotificationAction,
3. [L6] 执行：heartbeatEndpoint,
4. [L7] 执行：listEndpointStatuses,
5. [L12] 定义函数 `formatDateTime`
6. [L13] 执行：if (!value) return '暂无';
7. [L14] 赋值 `date` = new Date(value)
8. [L15] 执行：if (Number.isNaN(date.getTime())) return value;
9. [L16] 返回 date.toLocaleString()
10. [L19] 定义函数 `defaultDeviceId`
11. [L20] 返回 platform === 'android' ? 'android-companion' : 'windows-companion'
12. [L23] 定义函数 `statusTone`
13. [L24] 赋值 `normalized` = status.toLowerCase()
14. [L25] 执行：if (normalized === 'healthy') return 'bg-emerald-50 text-emerald-700';
15. [L26] 执行：if (normalized === 'warning') return 'bg-amber-50 text-amber-700';
16. [L27] 执行：if (normalized === 'critical') return 'bg-rose-50 text-rose-700';
17. [L28] 返回 'bg-slate-100 text-slate-600'
18. [L31] 默认导出函数 `EndpointShellPage`
19. [L32] 赋值 `queryClient` = useQueryClient()
20. [L33] 执行：const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');
21. [L34] 执行：const [manualDeviceId, setManualDeviceId] = useState(defaultDeviceId('windows'));
22. [L35] 执行：const [manualPlatform, setManualPlatform] = useState<EndpointPlatform>('windows');
23. [L37] 赋值 `{ data: endpoints = [], isLoading }` = useQuery({
24. [L38] 执行：queryKey: ['endpoint-statuses'],
25. [L39] 执行：queryFn: listEndpointStatuses,
26. [L40] 执行：refetchInterval: 30_000,
27. [L43] 注册 `useEffect` 副作用
28. [L44] 若 (!selectedDeviceId && endpoints.length > 0) 则
29. [L45] 更新状态 setSelectedDeviceId(endpoints[0].deviceId)
30. [L49] Hook `useMemo` 绑定 `selectedEndpoint`
31. [L50] 执行：endpoints.find(endpoint => endpoint.deviceId === selectedDeviceId) ?? endpoints[0]
32. [L51] 执行：), [endpoints, selectedDeviceId]);
33. [L53] 赋值 `qualityDeviceId` = selectedEndpoint?.deviceId || manualDeviceId
34. [L54] 赋值 `{ data: quality }` = useQuery({
35. [L55] 执行：queryKey: ['endpoint-collection-quality', qualityDeviceId],
36. [L56] 执行：queryFn: () => getEndpointCollectionQuality(qualityDeviceId),
37. [L57] 执行：enabled: Boolean(qualityDeviceId),
38. [L58] 执行：refetchInterval: 30_000,
39. [L61] 赋值 `heartbeatMutation` = useMutation({
40. [L62] 执行：mutationFn: () => heartbeatEndpoint(manualDeviceId, {
41. [L63] 执行：platform: manualPlatform,
42. [L64] 执行：appVersion: 'web-shell',
43. [L65] 执行：uploadStatus: 'Healthy',
44. [L66] 执行：collectionCacheCount: selectedEndpoint?.collectionCacheCount ?? 0,
45. [L68] 执行：onSuccess: endpoint => {
46. [L69] 更新状态 setSelectedDeviceId(endpoint.deviceId)
47. [L70] 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-statuses'] });
48. [L71] 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-collection-quality', endpoint.deviceId] });
49. [L75] 赋值 `notificationActionMutation` = useMutation({
50. [L76] 执行：mutationFn: (riskLevel: OperationRiskLevel) => handleEndpointNotificationAction(qualityDeviceId, {
51. [L77] 执行：action: riskLevel === 'L1LowRiskAction' ? 'dismiss' : 'open-confirmation',
52. [L78] 执行：riskLevel,
53. [L79] 执行：confirmationId: riskLevel === 'L1LowRiskAction' ? null : 'pending-confirmation',
54. [L80] 执行：relatedObjectType: 'task',
55. [L81] 执行：relatedObjectId: 'endpoint-shell-preview',
56. [L83] 执行：onSuccess: () => {
57. [L84] 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-statuses'] });
58. [L85] 执行：queryClient.invalidateQueries({ queryKey: ['endpoint-collection-quality', qualityDeviceId] });
59. [L89] 赋值 `totalBlocked` = endpoints.reduce((sum, endpoint) => sum + endpoint.onlineOnlyBlockedCount, 0)
60. [L91] 返回 JSX/结构
61. [L92] 执行：<div className="mx-auto w-full max-w-[1300px] space-y-4 pb-8" data-page="EndpointShellPage">
62. [L93] 执行：<PageHeader
63. [L94] 执行：title="端点外壳"
64. [L95] 执行：subtitle="Windows 与 Android 只缓存采集上传，复杂事实变更统一回到 Web 确认。"
65. [L96] 执行：actions={
66. [L98] 执行：type="button"
67. [L99] 执行：onClick={() => heartbeatMutation.mutate()}
68. [L100] 执行：disabled={heartbeatMutation.isPending || !manualDeviceId.trim()}
69. [L101] 执行：className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
70. [L104] 执行：</button>
71. [L108] 执行：<section className="pim-panel p-4" data-contract="online-only boundary">
72. [L109] 执行：<div className="grid grid-cols-1 gap-3 md:grid-cols-[minmax(0,1fr)_180px_auto]">
73. [L111] 执行：<span className="text-xs font-semibold text-slate-500">设备 ID</span>
74. [L113] 执行：value={manualDeviceId}
75. [L114] 执行：onChange={event => setManualDeviceId(event.target.value)}
76. [L115] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
77. [L119] 执行：<span className="text-xs font-semibold text-slate-500">平台</span>
78. [L121] 执行：value={manualPlatform}
79. [L122] 执行：onChange={event => {
80. [L123] 赋值 `next` = event.target.value as EndpointPlatform
81. [L124] 更新状态 setManualPlatform(next)
82. [L125] 更新状态 setManualDeviceId(defaultDeviceId(next))
83. [L127] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
84. [L129] 执行：<option value="windows">Windows</option>
85. [L130] 执行：<option value="android">Android</option>
86. [L131] 执行：</select>
87. [L133] 执行：<div className="flex items-end">
88. [L134] 执行：<span className="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-600">
89. [L135] 执行：在线专属拦截 {totalBlocked}
90. [L139] 执行：</section>
91. [L141] 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
92. [L142] 执行：<section className="pim-panel p-4">
93. [L143] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
94. [L144] 执行：<h2 className="text-sm font-semibold text-slate-950">端点状态</h2>
95. [L145] 执行：<span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
96. [L146] 执行：{endpoints.length} 台设备
97. [L150] 执行：{isLoading ? (
98. [L151] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
99. [L152] 执行：正在加载端点状态。
100. [L154] 执行：) : endpoints.length === 0 ? (
101. [L155] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
102. [L156] 执行：暂无端点心跳，可先发送一次本机心跳。
103. [L159] 执行：<div className="mt-4 grid gap-3">
104. [L160] 执行：{endpoints.map(endpoint => (
105. [L162] 执行：type="button"
106. [L163] 执行：key={endpoint.deviceId}
107. [L164] 执行：onClick={() => setSelectedDeviceId(endpoint.deviceId)}
108. [L165] 执行：className={`rounded-lg border p-3 text-left transition-colors ${
109. [L166] 执行：endpoint.deviceId === qualityDeviceId
110. [L167] 执行：? 'border-blue-300 bg-blue-50'
111. [L168] 执行：: 'border-slate-200 bg-white hover:border-blue-200'
112. [L171] 执行：<div className="flex flex-wrap items-start justify-between gap-2">
113. [L172] 执行：<div className="min-w-0">
114. [L173] 执行：<h3 className="truncate text-sm font-semibold text-slate-950">{endpoint.deviceId}</h3>
115. [L174] 执行：<p className="mt-1 text-xs text-slate-500">
116. [L175] 执行：{endpoint.platform} / {formatDateTime(endpoint.lastHeartbeatAt)}
117. [L178] 执行：<span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusTone(endpoint.uploadStatus)}`}>
118. [L179] 执行：{endpoint.uploadStatus}
119. [L182] 执行：<div className="mt-3 grid grid-cols-2 gap-2">
120. [L183] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
121. [L184] 执行：<p className="text-xs font-semibold text-slate-400">采集缓存</p>
122. [L185] 执行：<p className="mt-1 text-sm text-slate-700">{endpoint.collectionCacheCount}</p>
123. [L187] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
124. [L188] 执行：<p className="text-xs font-semibold text-slate-400">在线处理</p>
125. [L189] 执行：<p className="mt-1 text-sm text-slate-700">{endpoint.onlineOnlyBlockedCount}</p>
126. [L192] 执行：</button>
127. [L196] 执行：</section>
128. [L198] 执行：<div className="space-y-4">
129. [L199] 执行：<section className="pim-panel p-4" data-contract="collection quality">
130. [L200] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
131. [L201] 执行：<h2 className="text-sm font-semibold text-slate-950">采集质量</h2>
132. [L202] 执行：{quality && (
133. [L203] 执行：<span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusTone(quality.uploadStatus)}`}>
134. [L204] 执行：{quality.uploadStatus}
135. [L208] 执行：<div className="mt-3 grid gap-2">
136. [L209] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
137. [L210] 执行：<p className="text-xs font-semibold text-slate-400">检查设备</p>
138. [L211] 执行：<p className="mt-1 break-words text-sm text-slate-700">{quality?.deviceId ?? qualityDeviceId}</p>
139. [L213] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
140. [L214] 执行：<p className="text-xs font-semibold text-slate-400">问题数量</p>
141. [L215] 执行：<p className="mt-1 text-sm text-slate-700">{quality?.issueCount ?? 0}</p>
142. [L217] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
143. [L218] 执行：<p className="text-xs font-semibold text-slate-400">检查时间</p>
144. [L219] 执行：<p className="mt-1 text-sm text-slate-700">{formatDateTime(quality?.checkedAt)}</p>
145. [L222] 执行：</section>
146. [L224] 执行：<section className="pim-panel p-4" data-contract="notification action">
147. [L225] 执行：<h2 className="text-sm font-semibold text-slate-950">通知动作</h2>
148. [L226] 执行：<p className="mt-1 text-xs leading-5 text-slate-500">
149. [L227] 执行：低风险动作可在线执行，高风险动作打开确认或审计详情。
150. [L229] 执行：<div className="mt-3 flex flex-wrap gap-2">
151. [L231] 执行：type="button"
152. [L232] 执行：onClick={() => notificationActionMutation.mutate('L1LowRiskAction')}
153. [L233] 执行：disabled={notificationActionMutation.isPending}
154. [L234] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
155. [L237] 执行：</button>
156. [L239] 执行：type="button"
157. [L240] 执行：onClick={() => notificationActionMutation.mutate('L3ExternalSourceOrWriteback')}
158. [L241] 执行：disabled={notificationActionMutation.isPending}
159. [L242] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
160. [L245] 执行：</button>
161. [L247] 执行：{notificationActionMutation.data && (
162. [L248] 执行：<div className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
163. [L249] 执行：<p>结果：{notificationActionMutation.data.result}</p>
164. [L250] 执行：{notificationActionMutation.data.detailUrl && (
165. [L251] 执行：<p className="mt-1 break-words">详情：{notificationActionMutation.data.detailUrl}</p>
166. [L255] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/EndpointShellPage.tsx",
      "label": "EndpointShellPage",
      "path": "src/client-web/src/pages/EndpointShellPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/EndpointShellPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/EndpointShellPage.tsx",
      "to": "src/client-web/src/api/endpoints.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/EndpointShellPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/EndpointShellPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
