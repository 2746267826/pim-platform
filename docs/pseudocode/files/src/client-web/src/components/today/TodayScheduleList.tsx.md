# src/client-web/src/components/today/TodayScheduleList.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `TodayScheduleList`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`、`src/client-web/src/ui/StatusBadge.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### ScheduledItem
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L5 声明 `ScheduledItem`
- 分支与异常：无
- 调用：无

### safeTime
#### safeTime(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `safeTime`
  2. 执行：if (!value) return null;
  3. 赋值 `parsed` = new Date(value)
  4. 执行：if (Number.isNaN(parsed.getTime())) return null;
  5. 返回 parsed
- 分支与异常：if (!value) return null;；if (Number.isNaN(parsed.getTime())) return null;
- 调用：safeTime、Date、Number.isNaN、parsed.getTime

### formatTime
#### formatTime(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatTime`
  2. 赋值 `parsed` = safeTime(value)
  3. 返回 parsed
  4. 执行：? parsed.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  5. 执行：: '时间未知';
- 分支与异常：无显著分支
- 调用：formatTime、safeTime、parsed.toLocaleTimeString

### priorityBorder
#### priorityBorder(priority: number)
- 输入：priority: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `priorityBorder`
  2. 执行：if (priority === 1) return 'border-l-red-500';
  3. 执行：if (priority === 3) return 'border-l-teal-500';
  4. 返回 'border-l-amber-500'
- 分支与异常：if (priority === 1) return 'border-l-red-500';；if (priority === 3) return 'border-l-teal-500';
- 调用：priorityBorder

### priorityLabel
#### priorityLabel(priority: number)
- 输入：priority: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `priorityLabel`
  2. 执行：if (priority === 1) return '高优先级';
  3. 执行：if (priority === 3) return '低优先级';
  4. 返回 '普通优先级'
- 分支与异常：if (priority === 1) return '高优先级';；if (priority === 3) return '低优先级';
- 调用：priorityLabel

## 近逐行中文伪代码

1. [L5] 导出类型 `ScheduledItem`
2. [L7] 执行：type: 'event';
3. [L8] 执行：id: string;
4. [L9] 执行：event: EventResponse;
5. [L10] 执行：title: string;
6. [L11] 执行：start: string;
7. [L12] 执行：end?: string;
8. [L13] 执行：meta?: string;
9. [L14] 执行：color?: string;
10. [L17] 执行：type: 'task';
11. [L18] 执行：id: string;
12. [L19] 执行：task: TaskResponse;
13. [L20] 执行：title: string;
14. [L21] 执行：start: string;
15. [L22] 执行：end?: string;
16. [L23] 执行：meta?: string;
17. [L24] 执行：priority: number;
18. [L27] 定义函数 `safeTime`
19. [L28] 执行：if (!value) return null;
20. [L29] 赋值 `parsed` = new Date(value)
21. [L30] 执行：if (Number.isNaN(parsed.getTime())) return null;
22. [L31] 返回 parsed
23. [L34] 定义函数 `formatTime`
24. [L35] 赋值 `parsed` = safeTime(value)
25. [L36] 返回 parsed
26. [L37] 执行：? parsed.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
27. [L38] 执行：: '时间未知';
28. [L41] 定义函数 `priorityBorder`
29. [L42] 执行：if (priority === 1) return 'border-l-red-500';
30. [L43] 执行：if (priority === 3) return 'border-l-teal-500';
31. [L44] 返回 'border-l-amber-500'
32. [L47] 定义函数 `priorityLabel`
33. [L48] 执行：if (priority === 1) return '高优先级';
34. [L49] 执行：if (priority === 3) return '低优先级';
35. [L50] 返回 '普通优先级'
36. [L53] 导出函数 `buildScheduledItems`
37. [L54] 执行：events: EventResponse[],
38. [L55] 执行：tasks: TaskResponse[],
39. [L56] 执行：datePrefix: string,
40. [L57] 执行：): ScheduledItem[] {
41. [L58] 执行：const eventItems: ScheduledItem[] = events.map(event => ({
42. [L59] 执行：type: 'event',
43. [L60] 执行：id: event.id,
44. [L62] 执行：title: event.title,
45. [L63] 执行：start: event.dtStart,
46. [L64] 执行：end: event.dtEnd,
47. [L65] 执行：meta: event.location || event.description || '日程',
48. [L68] 执行：const taskItems: ScheduledItem[] = tasks
49. [L69] 执行：.filter(task => task.dtStart?.startsWith(datePrefix))
50. [L70] 执行：.map(task => ({
51. [L71] 执行：type: 'task',
52. [L72] 执行：id: task.id,
53. [L74] 执行：title: task.title,
54. [L75] 执行：start: task.dtStart!,
55. [L76] 执行：meta: task.description || '已排程任务',
56. [L77] 执行：priority: task.priority,
57. [L80] 返回 [...eventItems, ...taskItems].sort((a, b) => {
58. [L81] 赋值 `aTime` = safeTime(a.start)?.getTime() ?? Number.POSITIVE_INFINITY
59. [L82] 赋值 `bTime` = safeTime(b.start)?.getTime() ?? Number.POSITIVE_INFINITY
60. [L83] 返回 aTime - bTime
61. [L87] 默认导出函数 `TodayScheduleList`
62. [L89] 执行：onSelect,
63. [L91] 执行：section: TodaySection<CalendarScheduleTodayData>;
64. [L92] 执行：onSelect?: (item: ScheduledItem) => void;
65. [L94] 执行：const eventItems: ScheduledItem[] = section.data.events.map(event => ({
66. [L95] 执行：type: 'event',
67. [L96] 执行：id: event.id,
68. [L98] 执行：title: event.title,
69. [L99] 执行：start: event.dtStart,
70. [L100] 执行：end: event.dtEnd,
71. [L101] 执行：meta: event.location || event.description || '日程',
72. [L103] 执行：const taskItems: ScheduledItem[] = section.data.scheduledTasks.map(task => ({
73. [L104] 执行：type: 'task',
74. [L105] 执行：id: task.id,
75. [L107] 执行：title: task.title,
76. [L108] 执行：start: task.dtStart!,
77. [L109] 执行：meta: task.description || '已排程任务',
78. [L110] 执行：priority: task.priority,
79. [L112] 赋值 `items` = [...eventItems, ...taskItems].sort((a, b) => {
80. [L113] 赋值 `aTime` = safeTime(a.start)?.getTime() ?? Number.POSITIVE_INFINITY
81. [L114] 赋值 `bTime` = safeTime(b.start)?.getTime() ?? Number.POSITIVE_INFINITY
82. [L115] 返回 aTime - bTime
83. [L118] 返回 JSX/结构
84. [L119] 执行：<section className="pim-panel min-w-0 p-4">
85. [L120] 执行：<div className="mb-3 flex items-center justify-between gap-3">
86. [L121] 执行：<h2 className="font-semibold text-slate-900">今日安排</h2>
87. [L122] 执行：<StatusBadge tone="neutral">{items.length} 项</StatusBadge>
88. [L125] 执行：{items.length === 0 ? (
89. [L126] 执行：<EmptyState title="今天还没有安排" description="日程和已排程任务会显示在这里。" />
90. [L128] 执行：<div className="space-y-2">
91. [L129] 执行：{items.map(item => {
92. [L130] 赋值 `itemLabel` = item.type === 'task' ? `任务，${priorityLabel(item.priority)}` : '日程'
93. [L131] 赋值 `canSelect` = Boolean(onSelect)
94. [L132] 赋值 `interactionClass` = canSelect
95. [L133] 执行：? 'cursor-pointer transition-colors hover:bg-white focus:outline-none focus:ring-2 focus:ring-blue-200'
96. [L134] 执行：: 'cursor-default opacity-90';
97. [L136] 返回 JSX/结构
98. [L138] 执行：key={`${item.type}-${item.id}`}
99. [L139] 执行：type="button"
100. [L140] 执行：onClick={() => {
101. [L141] 执行：if (canSelect) onSelect?.(item);
102. [L143] 执行：aria-label={`${itemLabel}：${item.title}，${formatTime(item.start)}`}
103. [L144] 执行：disabled={!canSelect}
104. [L145] 执行：className={`w-full rounded-xl border border-slate-200 border-l-4 bg-slate-50 p-3 text-left ${
105. [L146] 执行：item.type === 'task' ? priorityBorder(item.priority) : 'border-l-blue-500'
106. [L149] 执行：<div className="flex items-start justify-between gap-3">
107. [L150] 执行：<div className="min-w-0">
108. [L151] 执行：<p className="truncate text-sm font-medium text-slate-900">{item.title}</p>
109. [L152] 执行：{item.meta && <p className="mt-1 truncate text-xs text-slate-500">{item.meta}</p>}
110. [L154] 执行：<StatusBadge tone={item.type === 'task' ? 'activity' : 'primary'}>
111. [L155] 执行：{item.type === 'task' ? '任务' : '日程'}
112. [L156] 执行：</StatusBadge>
113. [L158] 执行：{item.type === 'task' && <span className="sr-only">{priorityLabel(item.priority)}</span>}
114. [L159] 执行：<p className="mt-3 text-xs font-medium text-slate-600">
115. [L160] 执行：{formatTime(item.start)}
116. [L161] 执行：{item.end ? ` - ${formatTime(item.end)}` : ''}
117. [L163] 执行：</button>
118. [L168] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "label": "TodayScheduleList",
      "path": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayScheduleList.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayScheduleList.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    }
  ]
}
```
