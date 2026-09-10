# src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `ContextConfirmationPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatDuration
#### formatDuration(seconds: number)
- 输入：seconds: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatDuration`
  2. 赋值 `minutes` = Math.round(seconds / 60)
  3. 若 (minutes < 60) 则
  4. 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
  5. 赋值 `hours` = Math.floor(minutes / 60)
  6. 赋值 `remainingMinutes` = minutes % 60
  7. 返回 remainingMinutes > 0
  8. 执行：? `${hours.toLocaleString('zh-CN')}h ${remainingMinutes}m`
  9. 执行：: `${hours.toLocaleString('zh-CN')}h`;
- 分支与异常：if (minutes < 60) {
- 调用：formatDuration、Math.round、minutes.toLocaleString、Math.floor、hours.toLocaleString

### displayName
#### displayName(suggestion: ActivityClassificationSuggestion)
- 输入：suggestion: ActivityClassificationSuggestion
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `displayName`
  2. 返回 suggestion.appDisplayName || suggestion.clusterKey || '未识别上下文'
- 分支与异常：无显著分支
- 调用：displayName

### targetText
#### targetText(suggestion: ActivityClassificationSuggestion)
- 输入：suggestion: ActivityClassificationSuggestion
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `targetText`
  2. 赋值 `category` = suggestion.suggestedCategory || '保持分类'
  3. 返回 suggestion.suggestedProjectTag
  4. 执行：? `${category} · ${suggestion.suggestedProjectTag}`
  5. 执行：: category;
- 分支与异常：无显著分支
- 调用：targetText

## 近逐行中文伪代码

1. [L3] 定义类型 `Props`
2. [L4] 执行：suggestions: ActivityClassificationSuggestion[];
3. [L5] 执行：isLoading: boolean;
4. [L6] 执行：onPreview: (suggestion: ActivityClassificationSuggestion) => void;
5. [L7] 执行：onReject: (suggestion: ActivityClassificationSuggestion) => void;
6. [L10] 定义函数 `formatDuration`
7. [L11] 赋值 `minutes` = Math.round(seconds / 60)
8. [L12] 若 (minutes < 60) 则
9. [L13] 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
10. [L16] 赋值 `hours` = Math.floor(minutes / 60)
11. [L17] 赋值 `remainingMinutes` = minutes % 60
12. [L18] 返回 remainingMinutes > 0
13. [L19] 执行：? `${hours.toLocaleString('zh-CN')}h ${remainingMinutes}m`
14. [L20] 执行：: `${hours.toLocaleString('zh-CN')}h`;
15. [L23] 定义函数 `displayName`
16. [L24] 返回 suggestion.appDisplayName || suggestion.clusterKey || '未识别上下文'
17. [L27] 定义函数 `targetText`
18. [L28] 赋值 `category` = suggestion.suggestedCategory || '保持分类'
19. [L29] 返回 suggestion.suggestedProjectTag
20. [L30] 执行：? `${category} · ${suggestion.suggestedProjectTag}`
21. [L31] 执行：: category;
22. [L34] 默认导出函数 `ContextConfirmationPanel`
23. [L35] 执行：suggestions,
24. [L36] 执行：isLoading,
25. [L37] 执行：onPreview,
26. [L38] 执行：onReject,
27. [L40] 若 (isLoading) 则
28. [L41] 返回 JSX/结构
29. [L42] 执行：<section className="pim-panel min-w-0 p-4">
30. [L43] 执行：<h2 className="text-sm font-semibold text-slate-950">待确认上下文</h2>
31. [L44] 执行：<div className="mt-4 rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate
32. [L45] 执行：正在加载需要确认的上下文...
33. [L47] 执行：</section>
34. [L51] 赋值 `visibleSuggestions` = suggestions.slice(0, 6)
35. [L53] 返回 JSX/结构
36. [L54] 执行：<section className="pim-panel min-w-0 p-4">
37. [L55] 执行：<div className="flex items-start justify-between gap-3">
38. [L56] 执行：<div className="min-w-0">
39. [L57] 执行：<h2 className="text-sm font-semibold text-slate-950">待确认上下文</h2>
40. [L58] 执行：<p className="mt-1 text-xs text-slate-500">
41. [L59] 执行：预览高置信度活动上下文，确认后写入 App 知识库。
42. [L62] 执行：<span className="rounded-full bg-cyan-50 px-2 py-1 text-xs font-medium text-cyan-700">
43. [L63] 执行：{suggestions.length.toLocaleString('zh-CN')} 项
44. [L67] 执行：{visibleSuggestions.length === 0 ? (
45. [L68] 执行：<div className="mt-4 rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate
46. [L69] 执行：暂无待确认上下文，App 知识库不需要新的写入。
47. [L72] 执行：<div className="mt-4 space-y-2">
48. [L73] 执行：{visibleSuggestions.map(suggestion => (
49. [L75] 执行：key={suggestion.id}
50. [L76] 执行：className="rounded-lg border border-slate-200 bg-white px-3 py-3"
51. [L78] 执行：<div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-start md:justify-between">
52. [L79] 执行：<div className="min-w-0">
53. [L80] 执行：<div className="flex min-w-0 flex-wrap items-center gap-2">
54. [L81] 执行：<h3 className="min-w-0 break-words text-sm font-semibold text-slate-950">
55. [L82] 执行：{displayName(suggestion)}
56. [L84] 执行：<span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
57. [L85] 执行：待写入 App 知识库
58. [L89] 执行：<p className="mt-1 text-xs text-slate-600">
59. [L90] 执行：{suggestion.sampleCount.toLocaleString('zh-CN')} 个样本 ·{' '}
60. [L91] 执行：{formatDuration(suggestion.totalDurationSeconds)}
61. [L92] 执行：{suggestion.currentCategory ? ` · 当前 ${suggestion.currentCategory}` : ''}
62. [L95] 执行：<p className="mt-1 text-xs text-cyan-700">
63. [L96] 执行：建议上下文：{targetText(suggestion)}
64. [L100] 执行：<div className="flex shrink-0 flex-wrap gap-2 md:justify-end">
65. [L102] 执行：type="button"
66. [L103] 执行：onClick={() => onPreview(suggestion)}
67. [L104] 执行：className="pim-button-primary min-h-8 px-3 py-1.5 text-xs font-medium"
68. [L107] 执行：</button>
69. [L109] 执行：type="button"
70. [L110] 执行：onClick={() => onReject(suggestion)}
71. [L111] 执行：className="pim-button-secondary min-h-8 px-3 py-1.5 text-xs font-medium"
72. [L114] 执行：</button>
73. [L117] 执行：</article>
74. [L120] 执行：{suggestions.length > visibleSuggestions.length && (
75. [L121] 执行：<p className="px-1 text-xs text-slate-500">
76. [L122] 执行：还有 {(suggestions.length - visibleSuggestions.length).toLocaleString('zh-CN')} 项待确认上下文。
77. [L127] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx",
      "label": "ContextConfirmationPanel",
      "path": "src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
