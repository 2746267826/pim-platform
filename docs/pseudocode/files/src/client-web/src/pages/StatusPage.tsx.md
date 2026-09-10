# src/client-web/src/pages/StatusPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `StatusPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/mobile.ts`、`src/client-web/src/api/pcTracker.ts`、`src/client-web/src/api/status.ts`、`src/client-web/src/components/pc-tracker/PcQualitySummary.tsx`、`src/client-web/src/components/status/MobileDiagnosticsPanel.tsx`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatCheckedAt
#### formatCheckedAt(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatCheckedAt`
  2. 执行：if (!value) return '未知';
  3. 赋值 `date` = new Date(value)
  4. 执行：if (Number.isNaN(date.getTime())) return value;
  5. 返回 date.toLocaleString('zh-CN')
- 分支与异常：if (!value) return '未知';；if (Number.isNaN(date.getTime())) return value;
- 调用：formatCheckedAt、Date、Number.isNaN、date.getTime、date.toLocaleString

### StatusPill
#### StatusPill({ status, label }: { status: PimHealthStatus; label: string })
- 输入：{ status, label }: { status: PimHealthStatus; label: string }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `StatusPill`
  2. 赋值 `styles` = statusStyles[status]
  3. 返回 JSX/结构
  4. 执行：<span className={`inline-flex max-w-full items-center gap-2 rounded-full border px-2.5 py-1 text-xs font-semib
  5. 执行：<span className={`h-2 w-2 shrink-0 rounded-full ${styles.dot}`} aria-hidden="true" />
  6. 执行：<span className="truncate">{label}</span>
- 分支与异常：无显著分支
- 调用：StatusPill

### ComponentCard
#### ComponentCard({ component }: { component: StatusComponent })
- 输入：{ component }: { component: StatusComponent }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `ComponentCard`
  2. 赋值 `detailEntries` = Object.entries(component.details || {})
  3. 赋值 `kindLabel` = getComponentKindLabel(component.kind)
  4. 返回 JSX/结构
  5. 执行：<section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4">
  6. 执行：<div className="flex flex-wrap items-start justify-between gap-3">
  7. 执行：<div className="min-w-0">
  8. 执行：<h2 className="truncate text-sm font-semibold text-slate-950">{component.name}</h2>
  9. 执行：{kindLabel && <p className="mt-1 truncate text-xs text-slate-500">{kindLabel}</p>}
  10. 执行：<StatusPill status={component.status} label={getHealthStatusLabel(component.status)} />
  11. 执行：<p className="mt-3 text-sm text-slate-600">{component.message || '系统状态暂不可用'}</p>
  12. 执行：<p className="mt-2 text-xs text-slate-400">检查时间：{formatCheckedAt(component.checkedAt)}</p>
  13. 执行：{detailEntries.length > 0 && (
  14. 执行：<dl className="mt-4 grid grid-cols-1 gap-2 border-t border-slate-100 pt-3 sm:grid-cols-2">
  15. 执行：{detailEntries.map(([key, value]) => (
  16. 执行：<div key={key} className="min-w-0">
  17. 执行：<dt className="truncate text-[11px] font-medium uppercase text-slate-400">{key}</dt>
  18. 执行：<dd className="mt-0.5 break-words text-xs text-slate-700">{value}</dd>
  19. 执行：</section>
- 分支与异常：无显著分支
- 调用：ComponentCard、Object.entries、getComponentKindLabel、getHealthStatusLabel、formatCheckedAt、detailEntries.map

### StatusPage
#### StatusPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `StatusPage`
  2. 赋值 `{ data, isLoading, isError, refetch: ref` = useQuery({
  3. 执行：queryKey: ['status-detail'],
  4. 执行：queryFn: getStatusDetail,
  5. 执行：refetchInterval: 60_000,
  6. 执行：data: pcQuality,
  7. 执行：isLoading: pcQualityLoading,
  8. 执行：error: pcQualityError,
  9. 执行：refetch: refetchPcQuality,
  10. 执行：isFetching: pcQualityFetching,
  11. 执行：queryKey: ['status-pc-quality'],
  12. 执行：queryFn: () => getPcQuality(),
  13. 执行：data: mobileQuality,
  14. 执行：isLoading: mobileQualityLoading,
  15. 执行：error: mobileQualityError,
  16. 执行：refetch: refetchMobileQuality,
  17. 执行：isFetching: mobileQualityFetching,
  18. 执行：queryKey: ['status-mobile-quality'],
  19. 执行：queryFn: () => getMobileQuality(),
  20. 赋值 `summary` = data?.summary
  21. 赋值 `summaryStatus` = summary?.status ?? 'Unknown'
  22. 返回 JSX/结构
  23. 执行：<div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
  24. 执行：<PageHeader
  25. 执行：title="状态信息"
  26. 执行：subtitle="查看 API、数据库、daemon、采集源和后台任务状态。"
  27. 执行：actions={
  28. 执行：type="button"
  29. 执行：onClick={() => {
  30. 执行：void refetchStatus();
- 分支与异常：无显著分支
- 调用：StatusPage、useQuery、getPcQuality、getMobileQuality、refetchStatus、refetchPcQuality、refetchMobileQuality、getHealthStatusLabel、formatCheckedAt、data.nextSteps.map、data.components.map

## 近逐行中文伪代码

1. [L10] 执行：const statusStyles: Record<PimHealthStatus, { text: string; bg: string; border: string; dot: string }> = {
2. [L11] 执行：Healthy: {
3. [L12] 执行：text: 'text-emerald-700',
4. [L13] 执行：bg: 'bg-emerald-50',
5. [L14] 执行：border: 'border-emerald-200',
6. [L15] 执行：dot: 'bg-emerald-500',
7. [L17] 执行：Warning: {
8. [L18] 执行：text: 'text-amber-700',
9. [L19] 执行：bg: 'bg-amber-50',
10. [L20] 执行：border: 'border-amber-200',
11. [L21] 执行：dot: 'bg-amber-500',
12. [L23] 执行：Critical: {
13. [L24] 执行：text: 'text-red-700',
14. [L25] 执行：bg: 'bg-red-50',
15. [L26] 执行：border: 'border-red-200',
16. [L27] 执行：dot: 'bg-red-500',
17. [L29] 执行：Unknown: {
18. [L30] 执行：text: 'text-slate-600',
19. [L31] 执行：bg: 'bg-slate-50',
20. [L32] 执行：border: 'border-slate-200',
21. [L33] 执行：dot: 'bg-slate-400',
22. [L37] 定义函数 `formatCheckedAt`
23. [L38] 执行：if (!value) return '未知';
24. [L39] 赋值 `date` = new Date(value)
25. [L40] 执行：if (Number.isNaN(date.getTime())) return value;
26. [L41] 返回 date.toLocaleString('zh-CN')
27. [L44] 定义函数 `StatusPill`
28. [L45] 赋值 `styles` = statusStyles[status]
29. [L46] 返回 JSX/结构
30. [L47] 执行：<span className={`inline-flex max-w-full items-center gap-2 rounded-full border px-2.5 py-1 text-xs font-semib
31. [L48] 执行：<span className={`h-2 w-2 shrink-0 rounded-full ${styles.dot}`} aria-hidden="true" />
32. [L49] 执行：<span className="truncate">{label}</span>
33. [L54] 定义函数 `ComponentCard`
34. [L55] 赋值 `detailEntries` = Object.entries(component.details || {})
35. [L56] 赋值 `kindLabel` = getComponentKindLabel(component.kind)
36. [L58] 返回 JSX/结构
37. [L59] 执行：<section className="min-w-0 rounded-lg border border-slate-200 bg-white p-4">
38. [L60] 执行：<div className="flex flex-wrap items-start justify-between gap-3">
39. [L61] 执行：<div className="min-w-0">
40. [L62] 执行：<h2 className="truncate text-sm font-semibold text-slate-950">{component.name}</h2>
41. [L63] 执行：{kindLabel && <p className="mt-1 truncate text-xs text-slate-500">{kindLabel}</p>}
42. [L65] 执行：<StatusPill status={component.status} label={getHealthStatusLabel(component.status)} />
43. [L68] 执行：<p className="mt-3 text-sm text-slate-600">{component.message || '系统状态暂不可用'}</p>
44. [L69] 执行：<p className="mt-2 text-xs text-slate-400">检查时间：{formatCheckedAt(component.checkedAt)}</p>
45. [L71] 执行：{detailEntries.length > 0 && (
46. [L72] 执行：<dl className="mt-4 grid grid-cols-1 gap-2 border-t border-slate-100 pt-3 sm:grid-cols-2">
47. [L73] 执行：{detailEntries.map(([key, value]) => (
48. [L74] 执行：<div key={key} className="min-w-0">
49. [L75] 执行：<dt className="truncate text-[11px] font-medium uppercase text-slate-400">{key}</dt>
50. [L76] 执行：<dd className="mt-0.5 break-words text-xs text-slate-700">{value}</dd>
51. [L81] 执行：</section>
52. [L85] 默认导出函数 `StatusPage`
53. [L86] 赋值 `{ data, isLoading, isError, refetch: ref` = useQuery({
54. [L87] 执行：queryKey: ['status-detail'],
55. [L88] 执行：queryFn: getStatusDetail,
56. [L89] 执行：refetchInterval: 60_000,
57. [L93] 执行：data: pcQuality,
58. [L94] 执行：isLoading: pcQualityLoading,
59. [L95] 执行：error: pcQualityError,
60. [L96] 执行：refetch: refetchPcQuality,
61. [L97] 执行：isFetching: pcQualityFetching,
62. [L99] 执行：queryKey: ['status-pc-quality'],
63. [L100] 执行：queryFn: () => getPcQuality(),
64. [L101] 执行：refetchInterval: 60_000,
65. [L105] 执行：data: mobileQuality,
66. [L106] 执行：isLoading: mobileQualityLoading,
67. [L107] 执行：error: mobileQualityError,
68. [L108] 执行：refetch: refetchMobileQuality,
69. [L109] 执行：isFetching: mobileQualityFetching,
70. [L111] 执行：queryKey: ['status-mobile-quality'],
71. [L112] 执行：queryFn: () => getMobileQuality(),
72. [L113] 执行：refetchInterval: 60_000,
73. [L116] 赋值 `summary` = data?.summary
74. [L117] 赋值 `summaryStatus` = summary?.status ?? 'Unknown'
75. [L119] 返回 JSX/结构
76. [L120] 执行：<div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
77. [L121] 执行：<PageHeader
78. [L122] 执行：title="状态信息"
79. [L123] 执行：subtitle="查看 API、数据库、daemon、采集源和后台任务状态。"
80. [L124] 执行：actions={
81. [L126] 执行：type="button"
82. [L127] 执行：onClick={() => {
83. [L128] 执行：void refetchStatus();
84. [L129] 执行：void refetchPcQuality();
85. [L130] 执行：void refetchMobileQuality();
86. [L132] 执行：disabled={statusFetching || pcQualityFetching || mobileQualityFetching}
87. [L133] 执行：className="pim-button-secondary px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
88. [L136] 执行：</button>
89. [L140] 执行：{isLoading && (
90. [L141] 执行：<section className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-500">
91. [L142] 执行：正在检查系统状态...
92. [L143] 执行：</section>
93. [L146] 执行：{isError && (
94. [L147] 执行：<section className="rounded-lg border border-red-200 bg-red-50 p-6">
95. [L148] 执行：<p className="text-sm font-semibold text-red-700">系统状态暂不可用</p>
96. [L149] 执行：<p className="mt-1 text-sm text-red-600">请稍后刷新重试。</p>
97. [L150] 执行：</section>
98. [L153] 执行：{!isLoading && !isError && summary && (
99. [L155] 执行：<section className="rounded-lg border border-slate-200 bg-white p-5">
100. [L156] 执行：<div className="flex flex-wrap items-start justify-between gap-4">
101. [L157] 执行：<div className="min-w-0">
102. [L158] 执行：<p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">总体状态</p>
103. [L159] 执行：<h2 className="mt-2 text-2xl font-semibold text-slate-950">{summary.label || '未知'}</h2>
104. [L160] 执行：<p className="mt-2 text-sm text-slate-600">{summary.message || '系统状态暂不可用'}</p>
105. [L162] 执行：<StatusPill status={summaryStatus} label={getHealthStatusLabel(summaryStatus)} />
106. [L164] 执行：<p className="mt-4 text-xs text-slate-400">检查时间：{formatCheckedAt(summary.checkedAt)}</p>
107. [L165] 执行：</section>
108. [L167] 执行：<PcQualitySummary
109. [L168] 执行：quality={pcQuality}
110. [L169] 执行：isLoading={pcQualityLoading}
111. [L170] 执行：error={pcQualityError}
112. [L174] 执行：<MobileDiagnosticsPanel
113. [L175] 执行：quality={mobileQuality as MobileQualityDiagnosticsData | undefined}
114. [L176] 执行：isLoading={mobileQualityLoading}
115. [L177] 执行：error={mobileQualityError}
116. [L180] 执行：{data.nextSteps.length > 0 && (
117. [L181] 执行：<section className="rounded-lg border border-amber-200 bg-amber-50 p-4">
118. [L182] 执行：<h2 className="text-sm font-semibold text-amber-800">需要关注</h2>
119. [L183] 执行：<ul className="mt-2 space-y-1">
120. [L184] 执行：{data.nextSteps.map((step, index) => (
121. [L185] 执行：<li key={`${step}-${index}`} className="text-sm text-amber-800">
122. [L190] 执行：</section>
123. [L193] 执行：<div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
124. [L194] 执行：{data.components.map(component => (
125. [L195] 执行：<ComponentCard key={component.key} component={component} />

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/StatusPage.tsx",
      "label": "StatusPage",
      "path": "src/client-web/src/pages/StatusPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/StatusPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/api/mobile.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/api/pcTracker.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/api/status.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/components/pc-tracker/PcQualitySummary.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/StatusPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
