# src/client-web/src/components/schedule/TaskSegmentEditor.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：为任务新增执行时间段并列出已有 segments。
- 主要依赖：react-query、`createTaskExecutionSegment`、`listTaskExecutionSegments`
- 被谁使用：日程/任务编辑相关 UI

## 函数级结构化伪代码

### TaskSegmentEditor
- 输入：task 可空、onClose
- 输出：null 或面板
- 副作用：查询/创建时间段并失效相关 query
- 步骤：
  1. task 变化时填 startsAt/endsAt（dtStart、plannedEnd|due 截 16 位），清空 reason
  2. enabled 有 task 时 list segments
  3. createMutation：status Planned、source manual；成功 invalidate task-segments/calendar-layers/today-sections
  4. 无 task → null
  5. 表单：开始/结束 datetime-local、原因、添加按钮、segment 列表

## 近逐行中文伪代码

1. 本地状态三项输入。
2. 随 task 重置默认起止。
3. 拉已有时间段。
4. 添加 mutation 写后端并刷缓存。
5. 无任务不渲染；有则展示标题任务名、输入与列表。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/TaskSegmentEditor.tsx",
      "label": "TaskSegmentEditor",
      "path": "src/client-web/src/components/schedule/TaskSegmentEditor.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/TaskSegmentEditor.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/schedule/TaskSegmentEditor.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/schedule/TaskSegmentEditor.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
