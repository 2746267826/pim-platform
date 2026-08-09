# src/client-web/src/components/today/TodayPcOverview.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：`TodayPcOverview`：见源文件职责（TodayPcOverview.tsx）。
- 主要依赖：`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`、`src/client-web/src/ui/MetricCard.tsx`、`src/client-web/src/ui/StatusBadge.tsx`、`src/client-web/src/utils/pcBusinessDay.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatNumber
#### formatNumber(value: number | undefined)
- 输入：value: number | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatNumber`
  2. 返回 JSX/结构
- 分支与异常：无显著分支
- 调用：formatNumber、toLocaleString

### intensityClass
#### intensityClass(score: number, max: number)
- 输入：score: number, max: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `intensityClass`
  2. 执行：if (max <= 0 || score <= 0) return 'bg-slate-100';
  3. 赋值 `ratio` = score / max
  4. 执行：if (ratio > 0.75) return 'bg-teal-600';
  5. 执行：if (ratio > 0.5) return 'bg-teal-500';
  6. 执行：if (ratio > 0.25) return 'bg-teal-300';
  7. 返回 'bg-teal-100'
- 分支与异常：if (max <= 0 || score <= 0) return 'bg-slate-100';；if (ratio > 0.75) return 'bg-teal-600';；if (ratio > 0.5) return 'bg-teal-500';；if (ratio > 0.25) return 'bg-teal-300';
- 调用：intensityClass

### TodayPcOverview
#### TodayPcOverview({ section }: { section: TodaySection<PcActivityTodayData> })
- 输入：{ section }: { section: TodaySection<PcActivityTodayData> }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `TodayPcOverview`
  2. 赋值 `summary` = section.data.summary
  3. 赋值 `metrics` = summary.metrics
  4. 赋值 `keystats` = summary.keystats
  5. 赋值 `heatmap` = PC_BUSINESS_HOURS.map(hour => {
  6. 赋值 `bucket` = summary.heatmap.find(item => item.hour === hour)
  7. 返回 JSX/结构
  8. 执行：activeMinutes: bucket?.activeMinutes ?? 0,
  9. 执行：totalEvents: bucket?.totalEvents ?? 0,
  10. 执行：intensityScore: bucket?.intensityScore ?? 0,
  11. 赋值 `maxIntensity` = Math.max(...heatmap.map(item => item.intensityScore), 0)
  12. 执行：<section className="pim-panel min-w-0 p-4">
  13. 执行：<div className="mb-4 flex items-center justify-between gap-3">
  14. 执行：<h2 className="font-semibold text-slate-900">PC 记录概览</h2>
  15. 执行：<p className="mt-1 text-xs text-slate-500">输入、应用活跃度与 24 小时热力分布</p>
  16. 执行：<StatusBadge tone={section.status === 'empty' ? 'neutral' : 'activity'}>
  17. 执行：{section.status === 'empty' ? '暂无数据' : '今日'}
  18. 执行：</StatusBadge>
  19. 执行：{section.status === 'empty' ? (
  20. 执行：<EmptyState title="暂无 PC 记录" description="守护程序同步后会显示今天的使用概览。" />
  21. 执行：<div className="space-y-4">
  22. 执行：<div className="grid grid-cols-2 gap-3">
  23. 执行：<MetricCard
  24. 执行：label="记录时长"
  25. 执行：value={metrics?.totalRecordedDuration ?? '-'}
  26. 执行：helper={metrics?.mostFocusedApp ? `最专注：${metrics.mostFocusedApp}` : '等待同步'}
  27. 执行：tone="primary"
  28. 执行：label="活跃输入"
  29. 执行：value={metrics?.activeInputDuration ?? '-'}
  30. 执行：helper={`会话 ${metrics?.sessionCount ?? 0} 次`}
- 分支与异常：无显著分支
- 调用：TodayPcOverview、PC_BUSINESS_HOURS.map、summary.heatmap.find、Math.max、heatmap.map、formatNumber、intensityClass、pcHourLabel、summary.appRanking.slice、map、Math.round

## 近逐行中文伪代码

1. [L7] 定义函数 `formatNumber`
2. [L8] 返回 JSX/结构
3. [L11] 定义函数 `intensityClass`
4. [L12] 执行：if (max <= 0 || score <= 0) return 'bg-slate-100';
5. [L13] 赋值 `ratio` = score / max
6. [L14] 执行：if (ratio > 0.75) return 'bg-teal-600';
7. [L15] 执行：if (ratio > 0.5) return 'bg-teal-500';
8. [L16] 执行：if (ratio > 0.25) return 'bg-teal-300';
9. [L17] 返回 'bg-teal-100'
10. [L20] 默认导出函数 `TodayPcOverview`
11. [L21] 赋值 `summary` = section.data.summary
12. [L22] 赋值 `metrics` = summary.metrics
13. [L23] 赋值 `keystats` = summary.keystats
14. [L24] 赋值 `heatmap` = PC_BUSINESS_HOURS.map(hour => {
15. [L25] 赋值 `bucket` = summary.heatmap.find(item => item.hour === hour)
16. [L26] 返回 JSX/结构
17. [L28] 执行：activeMinutes: bucket?.activeMinutes ?? 0,
18. [L29] 执行：totalEvents: bucket?.totalEvents ?? 0,
19. [L30] 执行：intensityScore: bucket?.intensityScore ?? 0,
20. [L33] 赋值 `maxIntensity` = Math.max(...heatmap.map(item => item.intensityScore), 0)
21. [L35] 返回 JSX/结构
22. [L36] 执行：<section className="pim-panel min-w-0 p-4">
23. [L37] 执行：<div className="mb-4 flex items-center justify-between gap-3">
24. [L39] 执行：<h2 className="font-semibold text-slate-900">PC 记录概览</h2>
25. [L40] 执行：<p className="mt-1 text-xs text-slate-500">输入、应用活跃度与 24 小时热力分布</p>
26. [L42] 执行：<StatusBadge tone={section.status === 'empty' ? 'neutral' : 'activity'}>
27. [L43] 执行：{section.status === 'empty' ? '暂无数据' : '今日'}
28. [L44] 执行：</StatusBadge>
29. [L47] 执行：{section.status === 'empty' ? (
30. [L48] 执行：<EmptyState title="暂无 PC 记录" description="守护程序同步后会显示今天的使用概览。" />
31. [L50] 执行：<div className="space-y-4">
32. [L51] 执行：<div className="grid grid-cols-2 gap-3">
33. [L52] 执行：<MetricCard
34. [L53] 执行：label="记录时长"
35. [L54] 执行：value={metrics?.totalRecordedDuration ?? '-'}
36. [L55] 执行：helper={metrics?.mostFocusedApp ? `最专注：${metrics.mostFocusedApp}` : '等待同步'}
37. [L56] 执行：tone="primary"
38. [L58] 执行：<MetricCard
39. [L59] 执行：label="活跃输入"
40. [L60] 执行：value={metrics?.activeInputDuration ?? '-'}
41. [L61] 执行：helper={`会话 ${metrics?.sessionCount ?? 0} 次`}
42. [L62] 执行：tone="activity"
43. [L64] 执行：<MetricCard
44. [L65] 执行：label="按键"
45. [L66] 执行：value={formatNumber(keystats?.keyPresses ?? metrics?.totalKeyPresses)}
46. [L67] 执行：helper={`峰值 ${keystats?.peakKps ?? 0} KPS`}
47. [L68] 执行：tone="neutral"
48. [L70] 执行：<MetricCard
49. [L71] 执行：label="点击"
50. [L72] 执行：value={formatNumber(keystats?.totalClicks ?? metrics?.totalClicks)}
51. [L73] 执行：helper={`应用 ${metrics?.activeAppCount ?? 0} 个`}
52. [L74] 执行：tone="warning"
53. [L78] 执行：<div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
54. [L79] 执行：<div className="mb-3 flex items-center justify-between">
55. [L80] 执行：<p className="text-sm font-medium text-slate-800">24 小时热力图</p>
56. [L81] 执行：<p className="text-xs text-slate-500">04:00 起算，越深表示活动越集中</p>
57. [L83] 执行：<div className="grid grid-cols-12 gap-1.5" role="img" aria-label="今日 24 小时 PC 活跃热力图">
58. [L84] 执行：{heatmap.map(item => (
59. [L86] 执行：key={item.hour}
60. [L87] 执行：className={`h-8 rounded-md ${intensityClass(item.intensityScore, maxIntensity)}`}
61. [L88] 执行：title={`${pcHourLabel(item.hour)}，活跃 ${item.activeMinutes} 分钟，事件 ${item.totalEvents} 次`}
62. [L92] 执行：<div className="mt-2 grid grid-cols-4 text-xs text-slate-400">
63. [L93] 执行：<span>04:00</span>
64. [L94] 执行：<span className="text-center">10:00</span>
65. [L95] 执行：<span className="text-center">16:00</span>
66. [L96] 执行：<span className="text-right">22:00</span>
67. [L100] 执行：{summary.appRanking?.length ? (
68. [L101] 执行：<div className="space-y-2">
69. [L102] 执行：<p className="text-sm font-medium text-slate-800">主要应用</p>
70. [L103] 执行：{summary.appRanking.slice(0, 4).map(app => (
71. [L104] 执行：<div key={app.appName} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2">
72. [L105] 执行：<span className="min-w-0 truncate text-sm text-slate-700">{app.displayName || app.appName}</span>
73. [L106] 执行：<span className="text-xs font-medium text-slate-500">{Math.round(app.share * 100)}%</span>
74. [L110] 执行：) : null}
75. [L113] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "label": "TodayPcOverview",
      "path": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayPcOverview.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "to": "src/client-web/src/ui/MetricCard.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "to": "src/client-web/src/utils/pcBusinessDay.ts",
      "type": "depends_on"
    }
  ]
}
```
