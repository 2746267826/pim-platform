# src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `DailyActivityPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### CompactStat
#### CompactStat({ label, value }: { label: string; value: string | number })
- 输入：{ label, value }: { label: string; value: string | number }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `CompactStat`
  2. 返回 JSX/结构
  3. 执行：<div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
  4. 执行：<div className="text-[11px] text-slate-500">{label}</div>
  5. 执行：<div className="mt-1 min-w-0 break-words text-sm font-semibold text-slate-950">{value}</div>
- 分支与异常：无显著分支
- 调用：CompactStat

## 近逐行中文伪代码

1. [L3] 定义类型 `Props`
2. [L4] 执行：metrics: DerivedMetrics | null;
3. [L5] 执行：categories: CategorySummary[];
4. [L6] 执行：appRanking: AppRankingItem[];
5. [L7] 执行：selectedCategory: string | null;
6. [L8] 执行：onSelectCategory: (cat: string | null) => void;
7. [L9] 执行：selectedApp: string | null;
8. [L10] 执行：onSelectApp: (app: string | null) => void;
9. [L13] 定义函数 `CompactStat`
10. [L14] 返回 JSX/结构
11. [L15] 执行：<div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
12. [L16] 执行：<div className="text-[11px] text-slate-500">{label}</div>
13. [L17] 执行：<div className="mt-1 min-w-0 break-words text-sm font-semibold text-slate-950">{value}</div>
14. [L22] 默认导出函数 `DailyActivityPanel`
15. [L24] 执行：categories,
16. [L25] 执行：appRanking,
17. [L26] 执行：selectedCategory,
18. [L27] 执行：onSelectCategory,
19. [L28] 执行：selectedApp,
20. [L29] 执行：onSelectApp,
21. [L31] 执行：if (!metrics) return <div className="rounded-xl border border-slate-200 bg-slate-50 py-10 text-center text-sm 
22. [L33] 赋值 `top5Categories` = categories.slice(0, 5)
23. [L34] 赋值 `top5Apps` = appRanking.slice(0, 5)
24. [L35] 赋值 `totalInput` = top5Apps.reduce((sum, app) => sum + app.keyPresses + app.totalClicks, 0) || 1
25. [L37] 返回 JSX/结构
26. [L38] 执行：<div className="space-y-4">
27. [L39] 执行：<div className="grid grid-cols-2 gap-3">
28. [L40] 执行：<CompactStat label="工作会话" value={`${metrics.sessionCount} 个`} />
29. [L41] 执行：<CompactStat label="应用切换" value={`${metrics.appSwitchCount} 次`} />
30. [L42] 执行：<CompactStat label="最专注应用" value={metrics.mostFocusedApp || '-'} />
31. [L43] 执行：<CompactStat label="按键/点击比" value={`${metrics.keyClickRatio}:1`} />
32. [L46] 执行：<div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
33. [L47] 执行：<div className="mb-3 flex items-center justify-between">
34. [L48] 执行：<div className="text-xs font-semibold text-slate-700">分类排行</div>
35. [L49] 执行：{selectedCategory && (
36. [L50] 执行：<button type="button" className="text-xs text-blue-600 hover:text-blue-700" onClick={() => onSelectCategory(nu
37. [L52] 执行：</button>
38. [L55] 执行：<div className="space-y-2">
39. [L56] 执行：{top5Categories.length === 0 ? (
40. [L57] 执行：<p className="py-3 text-center text-xs text-slate-400">暂无分类数据</p>
41. [L58] 执行：) : top5Categories.map(category => (
42. [L60] 执行：key={category.categoryName}
43. [L61] 执行：type="button"
44. [L62] 执行：className={`w-full rounded-lg border px-3 py-2 text-left transition-colors ${
45. [L63] 执行：selectedCategory === category.categoryName
46. [L64] 执行：? 'border-blue-300 bg-blue-50'
47. [L65] 执行：: 'border-slate-200 bg-white hover:border-blue-200'
48. [L67] 执行：onClick={() => onSelectCategory(selectedCategory === category.categoryName ? null : category.categoryName)}
49. [L69] 执行：<div className="flex items-center justify-between gap-3 text-xs">
50. [L70] 执行：<span className="flex min-w-0 items-center gap-2 font-medium text-slate-800">
51. [L71] 执行：<span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: category.color }} />
52. [L72] 执行：<span className="truncate">{category.categoryName}</span>
53. [L74] 执行：<span className="shrink-0 text-slate-500">{category.share}%</span>
54. [L76] 执行：</button>
55. [L81] 执行：<div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
56. [L82] 执行：<div className="mb-3 flex items-center justify-between">
57. [L83] 执行：<div className="text-xs font-semibold text-slate-700">应用排行</div>
58. [L84] 执行：{selectedApp && (
59. [L85] 执行：<button type="button" className="text-xs text-blue-600 hover:text-blue-700" onClick={() => onSelectApp(null)}>
60. [L87] 执行：</button>
61. [L90] 执行：<div className="space-y-2">
62. [L91] 执行：{top5Apps.length === 0 ? (
63. [L92] 执行：<p className="py-3 text-center text-xs text-slate-400">暂无应用数据</p>
64. [L93] 执行：) : top5Apps.map(app => {
65. [L94] 赋值 `inputCount` = app.keyPresses + app.totalClicks
66. [L95] 赋值 `share` = Math.round((inputCount / totalInput) * 100)
67. [L96] 返回 JSX/结构
68. [L98] 执行：key={app.appName}
69. [L99] 执行：type="button"
70. [L100] 执行：className={`w-full rounded-lg border px-3 py-2 text-left transition-colors ${
71. [L101] 执行：selectedApp === app.appName
72. [L102] 执行：? 'border-teal-300 bg-teal-50'
73. [L103] 执行：: 'border-slate-200 bg-white hover:border-teal-200'
74. [L105] 执行：onClick={() => onSelectApp(selectedApp === app.appName ? null : app.appName)}
75. [L107] 执行：<div className="mb-1 flex items-center justify-between gap-3 text-xs">
76. [L108] 执行：<span className="min-w-0 truncate font-medium text-slate-800">{app.displayName || app.appName}</span>
77. [L109] 执行：<span className="shrink-0 text-slate-500">{share}%</span>
78. [L111] 执行：<div className="h-1.5 overflow-hidden rounded-full bg-slate-200">
79. [L112] 执行：<div className="h-full rounded-full bg-teal-500" style={{ width: `${share}%` }} />
80. [L114] 执行：</button>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx",
      "label": "DailyActivityPanel",
      "path": "src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
