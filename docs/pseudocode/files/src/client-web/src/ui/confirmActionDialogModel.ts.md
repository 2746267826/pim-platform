# src/client-web/src/ui/confirmActionDialogModel.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：UI 组件 `getDeleteTargetTypeLabel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### DeleteConfirmationInput
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L3 声明 `DeleteConfirmationInput`
- 分支与异常：无
- 调用：无

### DeleteConfirmationCopy
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L10 声明 `DeleteConfirmationCopy`
- 分支与异常：无
- 调用：无

### getDeleteTargetTypeLabel
#### getDeleteTargetTypeLabel(targetType: string)
- 输入：targetType: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getDeleteTargetTypeLabel`
  2. 执行：if (targetType === 'calendar' || targetType === 'calendar-book') return '日历本';
  3. 执行：if (targetType === 'task-book') return '任务本';
  4. 执行：if (targetType === 'task') return '任务';
  5. 返回 '日程'
- 分支与异常：if (targetType === 'calendar' || targetType === 'calendar-book') return '日历本';；if (targetType === 'task-book') return '任务本';；if (targetType === 'task') return '任务';
- 调用：getDeleteTargetTypeLabel

### getOperationSampleTypeLabel
#### getOperationSampleTypeLabel(type: string)
- 输入：type: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getOperationSampleTypeLabel`
  2. 执行：if (type === 'calendar' || type === 'calendar-book') return '日历本';
  3. 执行：if (type === 'task-book') return '任务本';
  4. 执行：if (type === 'task') return '任务';
  5. 返回 '日程'
- 分支与异常：if (type === 'calendar' || type === 'calendar-book') return '日历本';；if (type === 'task-book') return '任务本';；if (type === 'task') return '任务';
- 调用：getOperationSampleTypeLabel

### buildDeleteConfirmationCopy
#### buildDeleteConfirmationCopy(input: DeleteConfirmationInput)
- 输入：input: DeleteConfirmationInput
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `buildDeleteConfirmationCopy`
  2. 赋值 `typeLabel` = getDeleteTargetTypeLabel(input.targetType)
  3. 若 (input.affectedCount <= 1) 则
  4. 返回 JSX/结构
  5. 执行：title: `删除${typeLabel}`,
  6. 执行：description: `${input.title} 将移动到回收站，可以在设置中恢复。`,
  7. 执行：confirmLabel: '移动到回收站',
  8. 执行：samples: input.samples,
  9. 执行：description: `${input.title} 和 ${input.affectedCount} 个关联项目将一起移动到回收站。`,
  10. 执行：confirmLabel: `确认移动 ${input.affectedCount} 项`,
- 分支与异常：if (input.affectedCount <= 1) {
- 调用：buildDeleteConfirmationCopy、getDeleteTargetTypeLabel

## 近逐行中文伪代码

1. [L3] 导出类型 `DeleteConfirmationInput`
2. [L4] 执行：targetType: string;
3. [L5] 执行：title: string;
4. [L6] 执行：affectedCount: number;
5. [L7] 执行：samples: CalendarOperationSample[];
6. [L10] 导出类型 `DeleteConfirmationCopy`
7. [L11] 执行：title: string;
8. [L12] 执行：description: string;
9. [L13] 执行：confirmLabel: string;
10. [L14] 执行：samples: CalendarOperationSample[];
11. [L17] 导出函数 `getDeleteTargetTypeLabel`
12. [L18] 执行：if (targetType === 'calendar' || targetType === 'calendar-book') return '日历本';
13. [L19] 执行：if (targetType === 'task-book') return '任务本';
14. [L20] 执行：if (targetType === 'task') return '任务';
15. [L21] 返回 '日程'
16. [L24] 导出函数 `getOperationSampleTypeLabel`
17. [L25] 执行：if (type === 'calendar' || type === 'calendar-book') return '日历本';
18. [L26] 执行：if (type === 'task-book') return '任务本';
19. [L27] 执行：if (type === 'task') return '任务';
20. [L28] 返回 '日程'
21. [L31] 导出函数 `buildDeleteConfirmationCopy`
22. [L32] 赋值 `typeLabel` = getDeleteTargetTypeLabel(input.targetType)
23. [L34] 若 (input.affectedCount <= 1) 则
24. [L35] 返回 JSX/结构
25. [L36] 执行：title: `删除${typeLabel}`,
26. [L37] 执行：description: `${input.title} 将移动到回收站，可以在设置中恢复。`,
27. [L38] 执行：confirmLabel: '移动到回收站',
28. [L39] 执行：samples: input.samples,
29. [L43] 返回 JSX/结构
30. [L44] 执行：title: `删除${typeLabel}`,
31. [L45] 执行：description: `${input.title} 和 ${input.affectedCount} 个关联项目将一起移动到回收站。`,
32. [L46] 执行：confirmLabel: `确认移动 ${input.affectedCount} 项`,
33. [L47] 执行：samples: input.samples,

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/confirmActionDialogModel.ts",
      "label": "getDeleteTargetTypeLabel",
      "path": "src/client-web/src/ui/confirmActionDialogModel.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/confirmActionDialogModel.ts.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/ui/confirmActionDialogModel.ts",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
