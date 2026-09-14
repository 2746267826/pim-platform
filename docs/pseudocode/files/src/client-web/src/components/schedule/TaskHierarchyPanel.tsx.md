# src/client-web/src/components/schedule/TaskHierarchyPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `TaskHierarchyPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### (file)
#### 模块顶层
- 输入：见导入与导出
- 输出：导出符号
- 副作用：见近逐行
- 步骤：
  1. 定义类型 `TaskHierarchyPanelProps`
  2. 执行：tasks: TaskResponse[];
  3. 执行：selectedTaskId?: string;
  4. 执行：onSelectTask: (task: TaskResponse) => void;
  5. 默认导出函数 `TaskHierarchyPanel`
  6. 执行：selectedTaskId,
  7. 执行：onSelectTask,
  8. 返回 JSX/结构
  9. 执行：<aside className="pim-panel min-w-0 p-4" aria-label="任务层级">
  10. 执行：<div className="flex items-center justify-between gap-2">
  11. 执行：<h2 className="text-sm font-semibold text-slate-950">项目与任务本</h2>
  12. 执行：<span data-contract="Checklist" className="rounded-full bg-slate-100 px-2 py-1 text-[11px] font-semibold text-
  13. 执行：<div className="mt-3 space-y-2">
  14. 执行：{tasks.map(task => (
  15. 执行：<TaskNode
  16. 执行：key={task.id}
  17. 执行：task={task}
  18. 执行：depth={0}
  19. 执行：selectedTaskId={selectedTaskId}
  20. 执行：onSelectTask={onSelectTask}
  21. 执行：{tasks.length === 0 && (
  22. 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
  23. 执行：当前筛选下没有任务。
  24. 定义函数 `TaskNode`
  25. 执行：task: TaskResponse;
- 分支与异常：无显著分支
- 调用：TaskHierarchyPanel、tasks.map、TaskNode、onSelectTask、childTasks.map

## 近逐行中文伪代码

1. [L3] 定义类型 `TaskHierarchyPanelProps`
2. [L4] 执行：tasks: TaskResponse[];
3. [L5] 执行：selectedTaskId?: string;
4. [L6] 执行：onSelectTask: (task: TaskResponse) => void;
5. [L9] 默认导出函数 `TaskHierarchyPanel`
6. [L11] 执行：selectedTaskId,
7. [L12] 执行：onSelectTask,
8. [L14] 返回 JSX/结构
9. [L15] 执行：<aside className="pim-panel min-w-0 p-4" aria-label="任务层级">
10. [L16] 执行：<div className="flex items-center justify-between gap-2">
11. [L17] 执行：<h2 className="text-sm font-semibold text-slate-950">项目与任务本</h2>
12. [L18] 执行：<span data-contract="Checklist" className="rounded-full bg-slate-100 px-2 py-1 text-[11px] font-semibold text-
13. [L22] 执行：<div className="mt-3 space-y-2">
14. [L23] 执行：{tasks.map(task => (
15. [L24] 执行：<TaskNode
16. [L25] 执行：key={task.id}
17. [L26] 执行：task={task}
18. [L27] 执行：depth={0}
19. [L28] 执行：selectedTaskId={selectedTaskId}
20. [L29] 执行：onSelectTask={onSelectTask}
21. [L32] 执行：{tasks.length === 0 && (
22. [L33] 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-500">
23. [L34] 执行：当前筛选下没有任务。
24. [L42] 定义函数 `TaskNode`
25. [L45] 执行：selectedTaskId,
26. [L46] 执行：onSelectTask,
27. [L48] 执行：task: TaskResponse;
28. [L49] 执行：depth: number;
29. [L50] 执行：selectedTaskId?: string;
30. [L51] 执行：onSelectTask: (task: TaskResponse) => void;
31. [L53] 赋值 `selected` = task.id === selectedTaskId
32. [L54] 赋值 `childTasks` = task.subTasks ?? []
33. [L56] 返回 JSX/结构
34. [L59] 执行：type="button"
35. [L60] 执行：onClick={() => onSelectTask(task)}
36. [L61] 执行：className={`flex w-full items-center justify-between gap-2 rounded-lg border px-3 py-2 text-left text-sm trans
37. [L63] 执行：? 'border-blue-200 bg-blue-50 text-blue-700'
38. [L64] 执行：: 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
39. [L66] 执行：style={{ paddingLeft: `${12 + depth * 14}px` }}
40. [L68] 执行：<span className="min-w-0 truncate">{task.title}</span>
41. [L69] 执行：<span className="shrink-0 text-[11px] font-semibold text-slate-400">{childTasks.length}</span>
42. [L70] 执行：</button>
43. [L71] 执行：{childTasks.length > 0 && (
44. [L72] 执行：<div className="mt-1 space-y-1">
45. [L73] 执行：{childTasks.map(child => (
46. [L74] 执行：<TaskNode
47. [L75] 执行：key={child.id}
48. [L76] 执行：task={child}
49. [L77] 执行：depth={depth + 1}
50. [L78] 执行：selectedTaskId={selectedTaskId}
51. [L79] 执行：onSelectTask={onSelectTask}

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/TaskHierarchyPanel.tsx",
      "label": "TaskHierarchyPanel",
      "path": "src/client-web/src/components/schedule/TaskHierarchyPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/TaskHierarchyPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/schedule/TaskHierarchyPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
