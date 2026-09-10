# src/client-web/src/pages/ReportsPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `ReportsPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`、`src/client-web/src/ui/SegmentedControl.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### todayDate
#### todayDate(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `todayDate`
  2. 返回 new Date().toISOString().slice(0, 10)
- 分支与异常：无显著分支
- 调用：todayDate、Date、toISOString、slice

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

### reportTitle
#### reportTitle(report: ReportArtifact)
- 输入：report: ReportArtifact
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `reportTitle`
  2. 返回 report.title || `${report.kind} 报告`
- 分支与异常：无显著分支
- 调用：reportTitle

### parseMetrics
#### parseMetrics(report: ReportArtifact)
- 输入：report: ReportArtifact
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `parseMetrics`
  2. 执行：if (!report.metricsJson) return {};
  3. 进入 try
  4. 赋值 `parsed` = JSON.parse(report.metricsJson) as Record<string, unknown>
  5. 返回 parsed && typeof parsed === 'object' ? parsed : {}
  6. 返回 JSX/结构
- 分支与异常：if (!report.metricsJson) return {};；try {
- 调用：parseMetrics、JSON.parse

### suggestionStatus
#### suggestionStatus(suggestion: ReportSuggestion)
- 输入：suggestion: ReportSuggestion
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `suggestionStatus`
  2. 执行：if (suggestion.confirmationId) return `后续确认：${suggestion.confirmationId}`;
  3. 返回 `后续确认：${suggestion.status}`
- 分支与异常：if (suggestion.confirmationId) return `后续确认：${suggestion.confirmationId}`;
- 调用：suggestionStatus

### ReportsPage
#### ReportsPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `ReportsPage`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [tab, setTab] = useState<ReportKind>('Daily');
  4. 执行：const [date, setDate] = useState(todayDate);
  5. 执行：const [status, setStatus] = useState('all');
  6. 赋值 `{ data: reports = [], isLoading }` = useQuery({
  7. 执行：queryKey: ['reports'],
  8. 执行：queryFn: getReports,
  9. 执行：refetchInterval: 60_000,
  10. 赋值 `generateMutation` = useMutation({
  11. 执行：mutationFn: (request: GenerateReportRequest) => generateReport(request),
  12. 执行：onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports'] }),
  13. 赋值 `suggestionMutation` = useMutation({
  14. 执行：mutationFn: requestReportSuggestionAction,
  15. 执行：onSuccess: () => {
  16. 执行：queryClient.invalidateQueries({ queryKey: ['reports'] });
  17. 执行：queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
  18. Hook `useMemo` 绑定 `visibleReports`
  19. 赋值 `kindMatches` = report.kind.toLowerCase() === tab.toLowerCase()
  20. 赋值 `statusMatches` = status === 'all' || (report.status ?? '').toLowerCase() === status
  21. 返回 kindMatches && statusMatches
  22. 赋值 `latestReport` = visibleReports[0]
  23. 赋值 `metrics` = latestReport ? parseMetrics(latestReport) : {}
  24. 赋值 `suggestions` = latestReport?.suggestions ?? []
  25. 定义函数 `submitGenerate`
  26. 执行：generateMutation.mutate({
  27. 执行：kind: tab,
  28. 执行：projectId: null,
  29. 返回 JSX/结构
  30. 执行：<div className="mx-auto w-full max-w-[1300px] space-y-4 pb-8">
- 分支与异常：无显著分支
- 调用：ReportsPage、useQueryClient、useState、useQuery、useMutation、generateReport、queryClient.invalidateQueries、useMemo、reports.filter、report.kind.toLowerCase、tab.toLowerCase、toLowerCase、parseMetrics、submitGenerate、generateMutation.mutate

## 近逐行中文伪代码

1. [L4] 执行：generateReport,
2. [L5] 执行：getReports,
3. [L6] 执行：requestReportSuggestionAction,
4. [L12] 定义类型 `ReportKind`
5. [L14] 执行：const reportTabs: Array<{ value: ReportKind; label: string }> = [
6. [L15] 执行：{ value: 'Daily', label: '日报' },
7. [L16] 执行：{ value: 'Weekly', label: '周报' },
8. [L17] 执行：{ value: 'Monthly', label: '月报' },
9. [L18] 执行：{ value: 'Project', label: '项目报告' },
10. [L21] 定义函数 `todayDate`
11. [L22] 返回 new Date().toISOString().slice(0, 10)
12. [L25] 定义函数 `formatDateTime`
13. [L26] 执行：if (!value) return '暂无';
14. [L27] 赋值 `date` = new Date(value)
15. [L28] 执行：if (Number.isNaN(date.getTime())) return value;
16. [L29] 返回 date.toLocaleString()
17. [L32] 定义函数 `reportTitle`
18. [L33] 返回 report.title || `${report.kind} 报告`
19. [L36] 定义函数 `parseMetrics`
20. [L37] 执行：if (!report.metricsJson) return {};
21. [L39] 进入 try
22. [L40] 赋值 `parsed` = JSON.parse(report.metricsJson) as Record<string, unknown>
23. [L41] 返回 parsed && typeof parsed === 'object' ? parsed : {}
24. [L43] 返回 JSX/结构
25. [L47] 定义函数 `suggestionStatus`
26. [L48] 执行：if (suggestion.confirmationId) return `后续确认：${suggestion.confirmationId}`;
27. [L49] 返回 `后续确认：${suggestion.status}`
28. [L52] 默认导出函数 `ReportsPage`
29. [L53] 赋值 `queryClient` = useQueryClient()
30. [L54] 执行：const [tab, setTab] = useState<ReportKind>('Daily');
31. [L55] 执行：const [date, setDate] = useState(todayDate);
32. [L56] 执行：const [status, setStatus] = useState('all');
33. [L58] 赋值 `{ data: reports = [], isLoading }` = useQuery({
34. [L59] 执行：queryKey: ['reports'],
35. [L60] 执行：queryFn: getReports,
36. [L61] 执行：refetchInterval: 60_000,
37. [L64] 赋值 `generateMutation` = useMutation({
38. [L65] 执行：mutationFn: (request: GenerateReportRequest) => generateReport(request),
39. [L66] 执行：onSuccess: () => queryClient.invalidateQueries({ queryKey: ['reports'] }),
40. [L69] 赋值 `suggestionMutation` = useMutation({
41. [L70] 执行：mutationFn: requestReportSuggestionAction,
42. [L71] 执行：onSuccess: () => {
43. [L72] 执行：queryClient.invalidateQueries({ queryKey: ['reports'] });
44. [L73] 执行：queryClient.invalidateQueries({ queryKey: ['pending-confirmations'] });
45. [L77] Hook `useMemo` 绑定 `visibleReports`
46. [L78] 赋值 `kindMatches` = report.kind.toLowerCase() === tab.toLowerCase()
47. [L79] 赋值 `statusMatches` = status === 'all' || (report.status ?? '').toLowerCase() === status
48. [L80] 返回 kindMatches && statusMatches
49. [L83] 赋值 `latestReport` = visibleReports[0]
50. [L84] 赋值 `metrics` = latestReport ? parseMetrics(latestReport) : {}
51. [L85] 赋值 `suggestions` = latestReport?.suggestions ?? []
52. [L87] 定义函数 `submitGenerate`
53. [L88] 执行：generateMutation.mutate({
54. [L89] 执行：kind: tab,
55. [L91] 执行：projectId: null,
56. [L95] 返回 JSX/结构
57. [L96] 执行：<div className="mx-auto w-full max-w-[1300px] space-y-4 pb-8">
58. [L97] 执行：<PageHeader
59. [L98] 执行：title="报告中心"
60. [L99] 执行：subtitle="生成日报、周报、月报和项目报告，查看指标、正文、建议与后续确认结果。"
61. [L100] 执行：beforeActions={<SegmentedControl value={tab} options={reportTabs} onChange={setTab} ariaLabel="报告类型" />}
62. [L101] 执行：actions={
63. [L103] 执行：type="button"
64. [L104] 执行：onClick={submitGenerate}
65. [L105] 执行：disabled={generateMutation.isPending}
66. [L106] 执行：className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
67. [L108] 执行：{generateMutation.isPending ? '生成中' : `生成${reportTabs.find(item => item.value === tab)?.label ?? '报告'}`}
68. [L109] 执行：</button>
69. [L113] 执行：<section className="pim-panel p-4">
70. [L114] 执行：<div className="grid grid-cols-1 gap-3 md:grid-cols-2">
71. [L116] 执行：<span className="text-xs font-semibold text-slate-500">报告日期</span>
72. [L118] 执行：type="date"
73. [L119] 执行：value={date}
74. [L120] 执行：onChange={event => setDate(event.target.value)}
75. [L121] 执行：className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
76. [L125] 执行：<span className="text-xs font-semibold text-slate-500">状态</span>
77. [L126] 执行：<select value={status} onChange={event => setStatus(event.target.value)} className="mt-1 w-full rounded-lg bor
78. [L127] 执行：<option value="all">全部状态</option>
79. [L128] 执行：<option value="draft">草稿</option>
80. [L129] 执行：<option value="published">已发布</option>
81. [L130] 执行：<option value="archived">已归档</option>
82. [L131] 执行：</select>
83. [L134] 执行：</section>
84. [L136] 执行：<div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
85. [L138] 执行：['报告数量', visibleReports.length],
86. [L139] 执行：['后续建议', suggestions.length],
87. [L140] 执行：['待后续确认', suggestions.filter(item => item.status.toLowerCase() !== 'done').length],
88. [L141] 执行：].map(([label, value]) => (
89. [L142] 执行：<section key={label} className="pim-card p-4">
90. [L143] 执行：<p className="text-[11px] font-semibold text-slate-400">{label}</p>
91. [L144] 执行：<p className="mt-2 text-2xl font-semibold text-slate-950">{String(value)}</p>
92. [L145] 执行：<p className="mt-1 text-xs text-slate-500">{tab} / {status}</p>
93. [L146] 执行：</section>
94. [L150] 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,0.8fr)]">
95. [L151] 执行：<section className="pim-panel p-4">
96. [L152] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
97. [L153] 执行：<h2 className="text-sm font-semibold text-slate-950">
98. [L154] 执行：{reportTabs.find(item => item.value === tab)?.label}内容
99. [L156] 执行：{latestReport && (
100. [L157] 执行：<span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
101. [L158] 执行：{latestReport.status ?? '未标记'} · {formatDateTime(latestReport.generatedAt)}
102. [L163] 执行：{isLoading ? (
103. [L164] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
104. [L167] 执行：) : !latestReport ? (
105. [L168] 执行：<p className="mt-4 rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-
106. [L169] 执行：当前没有{reportTabs.find(item => item.value === tab)?.label}。
107. [L172] 执行：<article className="mt-4 space-y-4">
108. [L173] 执行：<div className="rounded-lg border border-slate-200 bg-white p-3">
109. [L174] 执行：<h3 className="text-sm font-semibold text-slate-950">{reportTitle(latestReport)}</h3>
110. [L175] 执行：<p className="mt-1 text-xs text-slate-500">风险：{latestReport.riskLevel}</p>
111. [L177] 执行：<div className="rounded-lg bg-slate-50 p-3">
112. [L178] 执行：<p className="whitespace-pre-wrap text-sm leading-6 text-slate-700">
113. [L179] 执行：{latestReport.contentMarkdown || '报告正文尚未生成。'}
114. [L182] 执行：</article>
115. [L184] 执行：</section>
116. [L186] 执行：<div className="space-y-4">
117. [L187] 执行：<section className="pim-panel p-4">
118. [L188] 执行：<h2 className="text-sm font-semibold text-slate-950">指标</h2>
119. [L189] 执行：<div className="mt-3 grid gap-2">
120. [L190] 执行：{Object.entries(metrics).slice(0, 8).map(([key, value]) => (
121. [L191] 执行：<div key={key} className="rounded-lg bg-slate-50 px-3 py-2">
122. [L192] 执行：<p className="text-xs font-semibold text-slate-400">{key}</p>
123. [L193] 执行：<p className="mt-1 break-words text-sm text-slate-700">{String(value)}</p>
124. [L196] 执行：{Object.keys(metrics).length === 0 && (
125. [L197] 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
126. [L202] 执行：</section>
127. [L204] 执行：<section className="pim-panel p-4">
128. [L205] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
129. [L206] 执行：<h2 className="text-sm font-semibold text-slate-950">后续确认</h2>
130. [L207] 执行：<span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700">
131. [L208] 执行：{suggestions.length} 条建议
132. [L211] 执行：<div className="mt-3 grid gap-2">
133. [L212] 执行：{suggestions.map(suggestion => (
134. [L213] 执行：<article key={suggestion.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
135. [L214] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
136. [L215] 执行：<h3 className="text-sm font-semibold text-slate-900">{suggestion.action}</h3>
137. [L216] 执行：<span className="text-xs text-slate-500">{suggestion.status}</span>
138. [L218] 执行：<p className="mt-2 text-xs leading-5 text-slate-500">{suggestion.summary}</p>
139. [L219] 执行：<div className="mt-3 flex flex-wrap items-center gap-2">
140. [L220] 执行：<span className="text-xs text-slate-500">{suggestionStatus(suggestion)}</span>
141. [L222] 执行：type="button"
142. [L223] 执行：onClick={() => suggestionMutation.mutate(suggestion.id)}
143. [L224] 执行：disabled={suggestionMutation.isPending}
144. [L225] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-60"
145. [L228] 执行：</button>
146. [L230] 执行：</article>
147. [L232] 执行：{suggestions.length === 0 && (
148. [L233] 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
149. [L234] 执行：暂无需要后续确认的建议。
150. [L238] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/ReportsPage.tsx",
      "label": "ReportsPage",
      "path": "src/client-web/src/pages/ReportsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/ReportsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/ReportsPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/ReportsPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/ReportsPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/ReportsPage.tsx",
      "to": "src/client-web/src/ui/SegmentedControl.tsx",
      "type": "depends_on"
    }
  ]
}
```
