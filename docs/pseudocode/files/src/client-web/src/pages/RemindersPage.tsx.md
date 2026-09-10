# src/client-web/src/pages/RemindersPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `RemindersPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`、`src/client-web/src/ui/SegmentedControl.tsx`
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

### isSameBusinessDate
#### isSameBusinessDate(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `isSameBusinessDate`
  2. 执行：if (!value) return false;
  3. 赋值 `date` = new Date(value)
  4. 赋值 `now` = new Date()
  5. 返回 date.getFullYear() === now.getFullYear()
  6. 执行：&& date.getMonth() === now.getMonth()
  7. 执行：&& date.getDate() === now.getDate();
- 分支与异常：if (!value) return false;
- 调用：isSameBusinessDate、Date、date.getFullYear、now.getFullYear、date.getMonth、now.getMonth、date.getDate、now.getDate

### isWithinWeek
#### isWithinWeek(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `isWithinWeek`
  2. 执行：if (!value) return false;
  3. 赋值 `date` = new Date(value)
  4. 赋值 `now` = new Date()
  5. 赋值 `end` = new Date(now)
  6. 执行：end.setDate(now.getDate() + 7);
  7. 返回 date >= now && date <= end
- 分支与异常：if (!value) return false;
- 调用：isWithinWeek、Date、end.setDate、now.getDate

### isOverdue
#### isOverdue(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `isOverdue`
  2. 执行：if (!value) return false;
  3. 返回 new Date(value).getTime() < Date.now()
- 分支与异常：if (!value) return false;
- 调用：isOverdue、Date、getTime、Date.now

### deliveryMatchesReminder
#### deliveryMatchesReminder(delivery: ReminderDelivery, reminder: ReminderSummary)
- 输入：delivery: ReminderDelivery, reminder: ReminderSummary
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `deliveryMatchesReminder`
  2. 返回 delivery.reminderId === reminder.id
- 分支与异常：无显著分支
- 调用：deliveryMatchesReminder

### RemindersPage
#### RemindersPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `RemindersPage`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [tab, setTab] = useState<ReminderTab>('due');
  4. 执行：const [horizon, setHorizon] = useState('today');
  5. 执行：const [channel, setChannel] = useState('all');
  6. 执行：const [status, setStatus] = useState('open');
  7. 赋值 `{ data: reminders = [], isLoading }` = useQuery({
  8. 执行：queryKey: ['reminders'],
  9. 执行：queryFn: getReminders,
  10. 执行：refetchInterval: 30_000,
  11. 赋值 `{ data: deliveryLog = [] }` = useQuery({
  12. 执行：queryKey: ['reminder-delivery-log'],
  13. 执行：queryFn: getReminderDeliveryLog,
  14. 赋值 `actionMutation` = useMutation({
  15. 执行：mutationFn: ({ id, action }: { id: string; action: string }) => handleReminderAction(id, action),
  16. 执行：onSuccess: () => {
  17. 执行：queryClient.invalidateQueries({ queryKey: ['reminders'] });
  18. 执行：queryClient.invalidateQueries({ queryKey: ['reminder-delivery-log'] });
  19. 赋值 `snoozeMutation` = useMutation({
  20. 执行：mutationFn: (id: string) => snoozeReminder(id),
  21. 执行：onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reminders'] }),
  22. 赋值 `dismissMutation` = useMutation({
  23. 执行：mutationFn: dismissReminder,
  24. Hook `useMemo` 绑定 `filteredReminders`
  25. 赋值 `horizonMatches` = horizon === 'today'
  26. 执行：? isSameBusinessDate(reminder.scheduledAt)
  27. 执行：: horizon === 'week'
  28. 执行：? isWithinWeek(reminder.scheduledAt)
  29. 执行：: horizon === 'overdue'
  30. 执行：? isOverdue(reminder.scheduledAt)
- 分支与异常：无显著分支
- 调用：RemindersPage、useQueryClient、useState、useQuery、useMutation、handleReminderAction、queryClient.invalidateQueries、snoozeReminder、useMemo、reminders.filter、isSameBusinessDate、isWithinWeek、isOverdue、reminder.channels.some、item.toLowerCase

## 近逐行中文伪代码

1. [L5] 执行：dismissReminder,
2. [L6] 执行：getReminderDeliveryLog,
3. [L7] 执行：getReminders,
4. [L8] 执行：handleReminderAction,
5. [L9] 执行：snoozeReminder,
6. [L15] 定义类型 `ReminderTab`
7. [L17] 执行：const reminderTabs: Array<{ value: ReminderTab; label: string }> = [
8. [L18] 执行：{ value: 'due', label: '待提醒' },
9. [L19] 执行：{ value: 'rules', label: '规则' },
10. [L20] 执行：{ value: 'delivery', label: '发送历史' },
11. [L23] 定义函数 `formatDateTime`
12. [L24] 执行：if (!value) return '暂无';
13. [L25] 赋值 `date` = new Date(value)
14. [L26] 执行：if (Number.isNaN(date.getTime())) return value;
15. [L27] 返回 date.toLocaleString()
16. [L30] 定义函数 `isSameBusinessDate`
17. [L31] 执行：if (!value) return false;
18. [L32] 赋值 `date` = new Date(value)
19. [L33] 赋值 `now` = new Date()
20. [L34] 返回 date.getFullYear() === now.getFullYear()
21. [L35] 执行：&& date.getMonth() === now.getMonth()
22. [L36] 执行：&& date.getDate() === now.getDate();
23. [L39] 定义函数 `isWithinWeek`
24. [L40] 执行：if (!value) return false;
25. [L41] 赋值 `date` = new Date(value)
26. [L42] 赋值 `now` = new Date()
27. [L43] 赋值 `end` = new Date(now)
28. [L44] 执行：end.setDate(now.getDate() + 7);
29. [L45] 返回 date >= now && date <= end
30. [L48] 定义函数 `isOverdue`
31. [L49] 执行：if (!value) return false;
32. [L50] 返回 new Date(value).getTime() < Date.now()
33. [L53] 定义函数 `deliveryMatchesReminder`
34. [L54] 返回 delivery.reminderId === reminder.id
35. [L57] 默认导出函数 `RemindersPage`
36. [L58] 赋值 `queryClient` = useQueryClient()
37. [L59] 执行：const [tab, setTab] = useState<ReminderTab>('due');
38. [L60] 执行：const [horizon, setHorizon] = useState('today');
39. [L61] 执行：const [channel, setChannel] = useState('all');
40. [L62] 执行：const [status, setStatus] = useState('open');
41. [L64] 赋值 `{ data: reminders = [], isLoading }` = useQuery({
42. [L65] 执行：queryKey: ['reminders'],
43. [L66] 执行：queryFn: getReminders,
44. [L67] 执行：refetchInterval: 30_000,
45. [L70] 赋值 `{ data: deliveryLog = [] }` = useQuery({
46. [L71] 执行：queryKey: ['reminder-delivery-log'],
47. [L72] 执行：queryFn: getReminderDeliveryLog,
48. [L73] 执行：refetchInterval: 30_000,
49. [L76] 赋值 `actionMutation` = useMutation({
50. [L77] 执行：mutationFn: ({ id, action }: { id: string; action: string }) => handleReminderAction(id, action),
51. [L78] 执行：onSuccess: () => {
52. [L79] 执行：queryClient.invalidateQueries({ queryKey: ['reminders'] });
53. [L80] 执行：queryClient.invalidateQueries({ queryKey: ['reminder-delivery-log'] });
54. [L84] 赋值 `snoozeMutation` = useMutation({
55. [L85] 执行：mutationFn: (id: string) => snoozeReminder(id),
56. [L86] 执行：onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reminders'] }),
57. [L89] 赋值 `dismissMutation` = useMutation({
58. [L90] 执行：mutationFn: dismissReminder,
59. [L91] 执行：onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reminders'] }),
60. [L94] Hook `useMemo` 绑定 `filteredReminders`
61. [L95] 赋值 `horizonMatches` = horizon === 'today'
62. [L96] 执行：? isSameBusinessDate(reminder.scheduledAt)
63. [L97] 执行：: horizon === 'week'
64. [L98] 执行：? isWithinWeek(reminder.scheduledAt)
65. [L99] 执行：: horizon === 'overdue'
66. [L100] 执行：? isOverdue(reminder.scheduledAt)
67. [L102] 赋值 `channelMatches` = channel === 'all'
68. [L103] 执行：|| reminder.channels.some(item => item.toLowerCase() === channel);
69. [L104] 赋值 `statusMatches` = status === 'all'
70. [L105] 执行：|| reminder.status.toLowerCase() === status;
71. [L107] 返回 horizonMatches && channelMatches && statusMatches
72. [L110] 赋值 `responseLog` = deliveryLog.filter(item => item.respondedAt)
73. [L112] 返回 JSX/结构
74. [L113] 执行：<div className="mx-auto w-full max-w-[1300px] space-y-4 pb-8">
75. [L114] 执行：<PageHeader
76. [L115] 执行：title="提醒中心"
77. [L116] 执行：subtitle="统一处理日程、任务、确认和报告的提醒触发原因、通知渠道、DND 与发送历史。"
78. [L117] 执行：actions={<SegmentedControl value={tab} options={reminderTabs} onChange={setTab} ariaLabel="提醒视图" />}
79. [L120] 执行：<section className="pim-panel p-4">
80. [L121] 执行：<div className="grid grid-cols-1 gap-3 md:grid-cols-3">
81. [L123] 执行：<span className="text-xs font-semibold text-slate-500">时间范围</span>
82. [L124] 执行：<select value={horizon} onChange={event => setHorizon(event.target.value)} className="mt-1 w-full rounded-lg b
83. [L125] 执行：<option value="today">今天</option>
84. [L126] 执行：<option value="week">未来 7 天</option>
85. [L127] 执行：<option value="overdue">已过期</option>
86. [L128] 执行：<option value="all">全部</option>
87. [L129] 执行：</select>
88. [L132] 执行：<span className="text-xs font-semibold text-slate-500">通知渠道</span>
89. [L133] 执行：<select value={channel} onChange={event => setChannel(event.target.value)} className="mt-1 w-full rounded-lg b
90. [L134] 执行：<option value="all">全部渠道</option>
91. [L135] 执行：<option value="desktop">桌面</option>
92. [L136] 执行：<option value="email">邮件</option>
93. [L137] 执行：<option value="web">Web</option>
94. [L138] 执行：<option value="android">Android</option>
95. [L139] 执行：</select>
96. [L142] 执行：<span className="text-xs font-semibold text-slate-500">状态</span>
97. [L143] 执行：<select value={status} onChange={event => setStatus(event.target.value)} className="mt-1 w-full rounded-lg bor
98. [L144] 执行：<option value="open">待处理</option>
99. [L145] 执行：<option value="snoozed">已稍后提醒</option>
100. [L146] 执行：<option value="sent">已发送</option>
101. [L147] 执行：<option value="dismissed">已忽略</option>
102. [L148] 执行：<option value="all">全部状态</option>
103. [L149] 执行：</select>
104. [L152] 执行：</section>
105. [L154] 执行：{tab === 'delivery' ? (
106. [L155] 执行：<section className="pim-panel p-4">
107. [L156] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
108. [L157] 执行：<h2 className="text-sm font-semibold text-slate-950">发送历史</h2>
109. [L158] 执行：<span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
110. [L159] 执行：{deliveryLog.length} 条记录
111. [L162] 执行：<div className="mt-4 grid gap-2">
112. [L163] 执行：{deliveryLog.map(delivery => (
113. [L164] 执行：<article key={delivery.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
114. [L165] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
115. [L166] 执行：<h3 className="text-sm font-semibold text-slate-900">{delivery.channel} / {delivery.status}</h3>
116. [L167] 执行：<span className="text-xs text-slate-500">{formatDateTime(delivery.createdAt)}</span>
117. [L169] 执行：<p className="mt-2 break-words text-xs text-slate-500">{delivery.payloadJson || '无发送载荷'}</p>
118. [L170] 执行：{delivery.respondedAt && (
119. [L171] 执行：<p className="mt-2 text-xs font-semibold text-emerald-700">响应历史：{formatDateTime(delivery.respondedAt)}</p>
120. [L173] 执行：</article>
121. [L175] 执行：{deliveryLog.length === 0 && (
122. [L176] 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
123. [L181] 执行：</section>
124. [L183] 执行：<section className="pim-panel p-4">
125. [L184] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
126. [L185] 执行：<h2 className="text-sm font-semibold text-slate-950">{tab === 'due' ? '提醒队列' : '提醒规则'}</h2>
127. [L186] 执行：<span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
128. [L187] 执行：{horizon} / {channel} / {status}
129. [L191] 执行：{isLoading ? (
130. [L192] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
131. [L195] 执行：) : filteredReminders.length === 0 ? (
132. [L196] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
133. [L197] 执行：当前筛选下没有提醒记录。
134. [L200] 执行：<div className="mt-4 grid gap-3">
135. [L201] 执行：{filteredReminders.map(reminder => {
136. [L202] 赋值 `deliveries` = [
137. [L203] 执行：...(reminder.deliveryHistory ?? []),
138. [L204] 执行：...deliveryLog.filter(delivery => deliveryMatchesReminder(delivery, reminder)),
139. [L206] 赋值 `responses` = [
140. [L207] 执行：...(reminder.responseHistory ?? []),
141. [L208] 执行：...deliveries.filter(delivery => delivery.respondedAt),
142. [L210] 赋值 `relatedUrl` = reminder.relatedObjectType && reminder.relatedObjectId
143. [L211] 执行：? `/audit/${encodeURIComponent(reminder.relatedObjectType)}/${encodeURIComponent(reminder.relatedObjectId)}`
144. [L214] 返回 JSX/结构
145. [L215] 执行：<article key={reminder.id} className="rounded-lg border border-slate-200 bg-white p-4">
146. [L216] 执行：<div className="flex flex-wrap items-start justify-between gap-3">
147. [L217] 执行：<div className="min-w-0">
148. [L218] 执行：<h3 className="truncate text-sm font-semibold text-slate-950">{reminder.title}</h3>
149. [L219] 执行：<p className="mt-1 text-xs leading-5 text-slate-500">{reminder.body || '无正文'}</p>
150. [L221] 执行：<span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
151. [L222] 执行：{reminder.riskLevel}
152. [L226] 执行：<div className="mt-3 grid grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-4">
153. [L227] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
154. [L228] 执行：<p className="text-xs font-semibold text-slate-400">触发原因</p>
155. [L229] 执行：<p className="mt-1 text-sm text-slate-700">{reminder.triggerReason || '规则触发'}</p>
156. [L231] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
157. [L232] 执行：<p className="text-xs font-semibold text-slate-400">通知渠道</p>
158. [L233] 执行：<p className="mt-1 text-sm text-slate-700">{reminder.channels.join(' / ') || '未配置'}</p>
159. [L235] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
160. [L236] 执行：<p className="text-xs font-semibold text-slate-400">DND</p>
161. [L237] 执行：<p className="mt-1 text-sm text-slate-700">
162. [L238] 执行：{reminder.doNotDisturbStart || reminder.doNotDisturbEnd
163. [L239] 执行：? `${reminder.doNotDisturbStart ?? '开始'} - ${reminder.doNotDisturbEnd ?? '结束'}`
164. [L243] 执行：<div className="rounded-lg bg-slate-50 px-3 py-2">
165. [L244] 执行：<p className="text-xs font-semibold text-slate-400">升级策略</p>
166. [L245] 执行：<p className="mt-1 text-sm text-slate-700">{reminder.escalationPolicy || '高风险打开确认详情'}</p>
167. [L249] 执行：<div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500">
168. [L250] 执行：<span>计划时间：{formatDateTime(reminder.scheduledAt)}</span>
169. [L251] 执行：<span>状态：{reminder.status}</span>
170. [L252] 执行：{relatedUrl && (
171. [L253] 执行：<Link to={relatedUrl} className="font-semibold text-blue-600 hover:text-blue-700">
172. [L259] 执行：<div className="mt-3 grid grid-cols-1 gap-3 lg:grid-cols-2">
173. [L260] 执行：<div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
174. [L261] 执行：<p className="text-xs font-semibold text-slate-500">发送历史</p>
175. [L262] 执行：<p className="mt-1 text-xs text-slate-500">
176. [L263] 执行：{deliveries.length > 0
177. [L264] 执行：? deliveries.slice(0, 3).map(item => `${item.channel}:${item.status}`).join(' / ')
178. [L265] 执行：: '暂无发送记录'}
179. [L268] 执行：<div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
180. [L269] 执行：<p className="text-xs font-semibold text-slate-500">响应历史</p>
181. [L270] 执行：<p className="mt-1 text-xs text-slate-500">
182. [L271] 执行：{responses.length > 0
183. [L272] 执行：? responses.slice(0, 3).map(item => formatDateTime(item.respondedAt)).join(' / ')
184. [L273] 执行：: '暂无用户响应'}
185. [L278] 执行：<div className="mt-3 flex flex-wrap items-center gap-2">
186. [L279] 执行：<span className="text-xs font-semibold text-slate-500">操作按钮</span>
187. [L281] 执行：type="button"
188. [L282] 执行：onClick={() => actionMutation.mutate({ id: reminder.id, action: 'open' })}
189. [L283] 执行：disabled={actionMutation.isPending}
190. [L284] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
191. [L287] 执行：</button>
192. [L289] 执行：type="button"
193. [L290] 执行：onClick={() => snoozeMutation.mutate(reminder.id)}
194. [L291] 执行：disabled={snoozeMutation.isPending}
195. [L292] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
196. [L295] 执行：</button>
197. [L297] 执行：type="button"
198. [L298] 执行：onClick={() => dismissMutation.mutate(reminder.id)}
199. [L299] 执行：disabled={dismissMutation.isPending}
200. [L300] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
201. [L303] 执行：</button>
202. [L305] 执行：</article>
203. [L310] 执行：</section>
204. [L313] 执行：{responseLog.length > 0 && (
205. [L314] 执行：<section className="pim-panel p-4">
206. [L315] 执行：<h2 className="text-sm font-semibold text-slate-950">用户响应历史</h2>
207. [L316] 执行：<div className="mt-3 flex flex-wrap gap-2">
208. [L317] 执行：{responseLog.slice(0, 8).map(item => (
209. [L318] 执行：<span key={item.id} className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
210. [L319] 执行：{item.channel} · {formatDateTime(item.respondedAt)}
211. [L323] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/RemindersPage.tsx",
      "label": "RemindersPage",
      "path": "src/client-web/src/pages/RemindersPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/RemindersPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/RemindersPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RemindersPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RemindersPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/RemindersPage.tsx",
      "to": "src/client-web/src/ui/SegmentedControl.tsx",
      "type": "depends_on"
    }
  ]
}
```
