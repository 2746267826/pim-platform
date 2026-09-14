# src/client-web/src/components/today/TodaySectionHost.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `TodaySectionHost`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/api/today.ts`、`src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx`、`src/client-web/src/components/today/TodayHealthSection.tsx`、`src/client-web/src/components/today/TodayPcOverview.tsx`、`src/client-web/src/components/today/TodayPcQualitySection.tsx`、`src/client-web/src/components/today/TodayScheduleList.tsx`、`src/client-web/src/components/today/TodayTaskColumn.tsx`、`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### getTodaySectionTitle
#### getTodaySectionTitle(kind: TodaySectionKind | string)
- 输入：kind: TodaySectionKind | string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getTodaySectionTitle`
  2. 返回 isKnownTodaySectionKind(kind) ? todaySectionTitles[kind] : kind
- 分支与异常：无显著分支
- 调用：getTodaySectionTitle、isKnownTodaySectionKind

### isKnownTodaySectionKind
#### isKnownTodaySectionKind(kind: TodaySectionKind | string)
- 输入：kind: TodaySectionKind | string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `isKnownTodaySectionKind`
  2. 返回 todaySectionOrder.includes(kind as TodaySectionKind)
- 分支与异常：无显著分支
- 调用：isKnownTodaySectionKind、todaySectionOrder.includes

### SectionLoading
#### SectionLoading({ title }: { title: string })
- 输入：{ title }: { title: string }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `SectionLoading`
  2. 返回 JSX/结构
  3. 执行：<section className="pim-panel min-w-0 p-4">
  4. 执行：<div className="mb-3 flex items-center justify-between gap-3">
  5. 执行：<h2 className="font-semibold text-slate-900">{title}</h2>
  6. 执行：<EmptyState title="加载中" description="正在加载这个区块的数据。" />
  7. 执行：</section>
- 分支与异常：无显著分支
- 调用：SectionLoading

### SectionUnavailable
#### SectionUnavailable({ title, message }: { title: string; message?: string })
- 输入：{ title, message }: { title: string; message?: string }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `SectionUnavailable`
  2. 返回 JSX/结构
  3. 执行：<section className="pim-panel min-w-0 p-4">
  4. 执行：<div className="mb-3 flex items-center justify-between gap-3">
  5. 执行：<h2 className="font-semibold text-slate-900">{title}</h2>
  6. 执行：<EmptyState title="暂不可用" description={message || '这个区块暂时无法提供数据。'} />
  7. 执行：</section>
- 分支与异常：无显著分支
- 调用：SectionUnavailable

## 近逐行中文伪代码

1. [L5] 执行：CalendarScheduleTodayData,
2. [L6] 执行：CalendarTasksTodayData,
3. [L7] 执行：ClassificationSuggestionsTodayData,
4. [L8] 执行：OperationsHealthTodayData,
5. [L9] 执行：PcActivityTodayData,
6. [L10] 执行：PcQualityTodayData,
7. [L11] 执行：TodaySection,
8. [L12] 执行：TodaySectionKind,
9. [L13] 执行：TodaySectionRegistryItem,
10. [L14] 执行：TaskResponse,
11. [L24] 导出符号 `todaySectionOrder`
12. [L25] 执行：'calendar.schedule',
13. [L26] 执行：'pc.activity',
14. [L27] 执行：'calendar.tasks',
15. [L28] 执行：'operations.health',
16. [L29] 执行：'pc.quality',
17. [L30] 执行：'pc.classification_suggestions',
18. [L33] 执行：const todaySectionTitles: Record<TodaySectionKind, string> = {
19. [L34] 执行：'calendar.schedule': '今日安排',
20. [L35] 执行：'calendar.tasks': '任务关注',
21. [L36] 执行：'pc.activity': 'PC 记录概览',
22. [L37] 执行：'pc.quality': 'PC 数据质量',
23. [L38] 执行：'operations.health': '系统健康',
24. [L39] 执行：'pc.classification_suggestions': '分类建议',
25. [L42] 导出函数 `getTodaySectionTitle`
26. [L43] 返回 isKnownTodaySectionKind(kind) ? todaySectionTitles[kind] : kind
27. [L46] 导出函数 `isKnownTodaySectionKind`
28. [L47] 返回 todaySectionOrder.includes(kind as TodaySectionKind)
29. [L50] 定义函数 `SectionLoading`
30. [L51] 返回 JSX/结构
31. [L52] 执行：<section className="pim-panel min-w-0 p-4">
32. [L53] 执行：<div className="mb-3 flex items-center justify-between gap-3">
33. [L54] 执行：<h2 className="font-semibold text-slate-900">{title}</h2>
34. [L56] 执行：<EmptyState title="加载中" description="正在加载这个区块的数据。" />
35. [L57] 执行：</section>
36. [L61] 定义函数 `SectionUnavailable`
37. [L62] 返回 JSX/结构
38. [L63] 执行：<section className="pim-panel min-w-0 p-4">
39. [L64] 执行：<div className="mb-3 flex items-center justify-between gap-3">
40. [L65] 执行：<h2 className="font-semibold text-slate-900">{title}</h2>
41. [L67] 执行：<EmptyState title="暂不可用" description={message || '这个区块暂时无法提供数据。'} />
42. [L68] 执行：</section>
43. [L72] 默认导出函数 `TodaySectionHost`
44. [L75] 执行：todayPrefix,
45. [L76] 执行：onSelectScheduled,
46. [L77] 执行：onSelectTask,
47. [L79] 执行：item: TodaySectionRegistryItem;
48. [L80] 执行：date: string;
49. [L81] 执行：todayPrefix: string;
50. [L82] 执行：onSelectScheduled?: (item: ScheduledItem) => void;
51. [L83] 执行：onSelectTask?: (task: TaskResponse) => void;
52. [L85] 赋值 `known` = isKnownTodaySectionKind(item.kind)
53. [L86] 赋值 `title` = getTodaySectionTitle(item.kind)
54. [L87] 赋值 `query` = useQuery({
55. [L88] 执行：queryKey: ['today-section', item.id, date],
56. [L89] 执行：queryFn: () => getTodaySection(item.id, date),
57. [L90] 执行：enabled: known,
58. [L91] 执行：refetchInterval: item.kind.startsWith('pc.') || item.kind.startsWith('operations.') ? 30000 : false,
59. [L94] 若 (!known) 则
60. [L95] 返回 JSX/结构
61. [L98] 若 (query.isLoading) 则
62. [L99] 返回 JSX/结构
63. [L102] 若 (query.error) 则
64. [L103] 返回 JSX/结构
65. [L106] 赋值 `data` = query.data
66. [L107] 若 (!data?.data) 则
67. [L108] 返回 JSX/结构
68. [L111] 若 (data.status === 'unavailable') 则
69. [L112] 返回 JSX/结构
70. [L115] 按 `data.kind` 分支
71. [L116] 分支 case 'calendar.schedule'
72. [L117] 返回 JSX/结构
73. [L118] 执行：<TodayScheduleList
74. [L119] 执行：section={data as TodaySection<CalendarScheduleTodayData>}
75. [L120] 执行：onSelect={item => {
76. [L121] 若 (onSelectScheduled) 则
77. [L122] 执行：onSelectScheduled(item);
78. [L123] 返回（空）
79. [L125] 若 (item.type === 'task') 则
80. [L126] 执行：onSelectTask?.(item.task);
81. [L131] 分支 case 'calendar.tasks'
82. [L132] 返回 JSX/结构
83. [L133] 执行：<TodayTaskColumn
84. [L134] 执行：section={data as TodaySection<CalendarTasksTodayData>}
85. [L135] 执行：todayPrefix={todayPrefix}
86. [L136] 执行：onSelect={onSelectTask}
87. [L139] 分支 case 'pc.activity'
88. [L140] 返回 JSX/结构
89. [L141] 分支 case 'pc.quality'
90. [L142] 返回 JSX/结构
91. [L143] 分支 case 'operations.health'
92. [L144] 返回 JSX/结构
93. [L145] 分支 case 'pc.classification_suggestions'
94. [L146] 返回 JSX/结构
95. [L147] 执行：<TodayClassificationSuggestionsSection
96. [L148] 执行：section={data as TodaySection<ClassificationSuggestionsTodayData>}
97. [L151] 默认分支
98. [L152] 返回 JSX/结构

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "label": "TodaySectionHost",
      "path": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodaySectionHost.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/api/today.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayHealthSection.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayPcOverview.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodaySectionHost.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    }
  ]
}
```
