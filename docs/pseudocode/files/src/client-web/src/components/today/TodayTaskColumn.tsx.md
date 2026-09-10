# src/client-web/src/components/today/TodayTaskColumn.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `TodayTaskColumn`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`、`src/client-web/src/ui/StatusBadge.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### validTimestamp
#### validTimestamp(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `validTimestamp`
  2. 执行：if (!value) return Number.POSITIVE_INFINITY;
  3. 赋值 `time` = new Date(value).getTime()
  4. 返回 Number.isNaN(time) ? Number.POSITIVE_INFINITY : time
- 分支与异常：if (!value) return Number.POSITIVE_INFINITY;
- 调用：validTimestamp、Date、getTime、Number.isNaN

### priorityDot
#### priorityDot(priority: number)
- 输入：priority: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `priorityDot`
  2. 执行：if (priority === 1) return 'bg-red-500';
  3. 执行：if (priority === 3) return 'bg-teal-500';
  4. 返回 'bg-amber-500'
- 分支与异常：if (priority === 1) return 'bg-red-500';；if (priority === 3) return 'bg-teal-500';
- 调用：priorityDot

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

### dueTone
#### dueTone(task: TaskResponse, todayPrefix: string)
- 输入：task: TaskResponse, todayPrefix: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `dueTone`
  2. 执行：if (!task.due) return 'neutral';
  3. 赋值 `dueTime` = new Date(task.due).getTime()
  4. 执行：if (Number.isNaN(dueTime)) return 'neutral';
  5. 执行：if (task.due.startsWith(todayPrefix)) return 'warning';
  6. 返回 dueTime < new Date(`${todayPrefix}T00:00:00`).getTime() ? 'danger' : 'neutral'
- 分支与异常：if (!task.due) return 'neutral';；if (Number.isNaN(dueTime)) return 'neutral';；if (task.due.startsWith(todayPrefix)) return 'warning';
- 调用：dueTone、Date、getTime、Number.isNaN、task.due.startsWith

### formatDue
#### formatDue(value?: string)
- 输入：value?: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatDue`
  2. 执行：if (!value) return '无截止';
  3. 赋值 `parsed` = new Date(value)
  4. 执行：if (Number.isNaN(parsed.getTime())) return '截止时间无效';
  5. 返回 parsed.toLocaleString('zh-CN', {
  6. 执行：month: '2-digit',
  7. 执行：day: '2-digit',
  8. 执行：hour: '2-digit',
  9. 执行：minute: '2-digit',
- 分支与异常：if (!value) return '无截止';；if (Number.isNaN(parsed.getTime())) return '截止时间无效';
- 调用：formatDue、Date、Number.isNaN、parsed.getTime、parsed.toLocaleString

### sortTasksByDue
#### sortTasksByDue(tasks: TaskResponse[])
- 输入：tasks: TaskResponse[]
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `sortTasksByDue`
  2. 返回 [...tasks].sort((a, b) => {
  3. 赋值 `dueDelta` = validTimestamp(a.due) - validTimestamp(b.due)
  4. 执行：if (dueDelta !== 0) return dueDelta;
  5. 返回 a.title.localeCompare(b.title, 'zh-CN')
- 分支与异常：if (dueDelta !== 0) return dueDelta;
- 调用：sortTasksByDue、sort、validTimestamp、a.title.localeCompare

## 近逐行中文伪代码

1. [L5] 定义函数 `validTimestamp`
2. [L6] 执行：if (!value) return Number.POSITIVE_INFINITY;
3. [L7] 赋值 `time` = new Date(value).getTime()
4. [L8] 返回 Number.isNaN(time) ? Number.POSITIVE_INFINITY : time
5. [L11] 定义函数 `priorityDot`
6. [L12] 执行：if (priority === 1) return 'bg-red-500';
7. [L13] 执行：if (priority === 3) return 'bg-teal-500';
8. [L14] 返回 'bg-amber-500'
9. [L17] 定义函数 `priorityLabel`
10. [L18] 执行：if (priority === 1) return '高优先级';
11. [L19] 执行：if (priority === 3) return '低优先级';
12. [L20] 返回 '普通优先级'
13. [L23] 定义函数 `dueTone`
14. [L24] 执行：if (!task.due) return 'neutral';
15. [L25] 赋值 `dueTime` = new Date(task.due).getTime()
16. [L26] 执行：if (Number.isNaN(dueTime)) return 'neutral';
17. [L27] 执行：if (task.due.startsWith(todayPrefix)) return 'warning';
18. [L28] 返回 dueTime < new Date(`${todayPrefix}T00:00:00`).getTime() ? 'danger' : 'neutral'
19. [L31] 定义函数 `formatDue`
20. [L32] 执行：if (!value) return '无截止';
21. [L33] 赋值 `parsed` = new Date(value)
22. [L34] 执行：if (Number.isNaN(parsed.getTime())) return '截止时间无效';
23. [L35] 返回 parsed.toLocaleString('zh-CN', {
24. [L36] 执行：month: '2-digit',
25. [L37] 执行：day: '2-digit',
26. [L38] 执行：hour: '2-digit',
27. [L39] 执行：minute: '2-digit',
28. [L43] 导出函数 `sortTasksByDue`
29. [L44] 返回 [...tasks].sort((a, b) => {
30. [L45] 赋值 `dueDelta` = validTimestamp(a.due) - validTimestamp(b.due)
31. [L46] 执行：if (dueDelta !== 0) return dueDelta;
32. [L47] 返回 a.title.localeCompare(b.title, 'zh-CN')
33. [L51] 默认导出函数 `TodayTaskColumn`
34. [L53] 执行：todayPrefix,
35. [L54] 执行：onSelect,
36. [L56] 执行：section: TodaySection<CalendarTasksTodayData>;
37. [L57] 执行：todayPrefix: string;
38. [L58] 执行：onSelect?: (task: TaskResponse) => void;
39. [L60] 赋值 `incompleteTasks` = sortTasksByDue(
40. [L61] 执行：Array.from(
41. [L64] 执行：...section.data.overdueTasks,
42. [L65] 执行：...section.data.dueTodayTasks,
43. [L66] 执行：...section.data.unscheduledTasks,
44. [L67] 执行：].map(task => [task.id, task]),
45. [L68] 执行：).values(),
46. [L72] 返回 JSX/结构
47. [L73] 执行：<section className="pim-panel min-w-0 p-4">
48. [L74] 执行：<div className="mb-3 flex items-center justify-between gap-3">
49. [L75] 执行：<h2 className="font-semibold text-slate-900">待办任务</h2>
50. [L76] 执行：<StatusBadge tone="neutral">{incompleteTasks.length} 项</StatusBadge>
51. [L79] 执行：{incompleteTasks.length === 0 ? (
52. [L80] 执行：<EmptyState title="没有未完成任务" description="可以新建任务，或打开日历安排今天要推进的工作。" />
53. [L82] 执行：<div className="space-y-2">
54. [L83] 执行：{incompleteTasks.map(task => (
55. [L85] 执行：key={task.id}
56. [L86] 执行：type="button"
57. [L87] 执行：onClick={() => onSelect?.(task)}
58. [L88] 执行：aria-label={`任务：${task.title}，${priorityLabel(task.priority)}，${formatDue(task.due)}`}
59. [L89] 执行：className="w-full rounded-xl border border-slate-200 bg-white p-3 text-left transition-colors hover:border-blu
60. [L91] 执行：<div className="flex items-start gap-2">
61. [L93] 执行：className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${priorityDot(task.priority)}`}
62. [L94] 执行：aria-hidden="true"
63. [L96] 执行：<span className="sr-only">{priorityLabel(task.priority)}</span>
64. [L97] 执行：<div className="min-w-0 flex-1">
65. [L98] 执行：<p className="truncate text-sm font-medium text-slate-900">{task.title}</p>
66. [L99] 执行：{task.description && (
67. [L100] 执行：<p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">{task.description}</p>
68. [L104] 执行：<div className="mt-3 flex flex-wrap items-center gap-2">
69. [L105] 执行：<StatusBadge tone={dueTone(task, todayPrefix)}>{formatDue(task.due)}</StatusBadge>
70. [L106] 执行：{task.dtStart && <StatusBadge tone="activity">已排程</StatusBadge>}
71. [L107] 执行：{task.plannedEnd && <StatusBadge tone="neutral">计划至 {formatDue(task.plannedEnd)}</StatusBadge>}
72. [L109] 执行：</button>
73. [L113] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "label": "TodayTaskColumn",
      "path": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayTaskColumn.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayTaskColumn.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    }
  ]
}
```
