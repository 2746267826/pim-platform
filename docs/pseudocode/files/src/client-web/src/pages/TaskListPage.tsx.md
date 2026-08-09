# src/client-web/src/pages/TaskListPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：任务列表：筛选/搜索/任务本、多选删除确认、完成切换、层级面板、时间段编辑、任务编辑对话框。
- 主要依赖：calendar API、TaskHierarchyPanel、TaskSegmentEditor、TaskEditorDialog、ConfirmActionDialog
- 被谁使用：路由任务列表

## 函数级结构化伪代码

### useLocalDate / buildTaskQuery / 展示辅助
- 跨午夜刷新今日串；按 filter 组装 GetTasksParams；优先级/状态/截止格式

### TaskListPage
- 查询 task books 与 tasks-paged
- toggleMutation 完成状态；deleteMutation 批量删
- 选中 stale 清理；筛选变更 clearSelection
- requestDeleteSelected → ConfirmActionDialog
- 列表卡：勾选、打开编辑、标记完成、时间段
- 侧栏 Hierarchy + SegmentEditor；Editor/Confirm 对话框

## 近逐行中文伪代码

1. 定义 filter pills 与 invalidate keys。
2. 构建查询含 inbox/high/completed/planned/today。
3. 加载任务与任务本。
4. 完成/删除 mutation 刷缓存。
5. 工具区筛选搜索全选删除。
6. 层级+时间段；列表项交互；空态 EmptyState。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/TaskListPage.tsx",
      "label": "TaskListPage",
      "path": "src/client-web/src/pages/TaskListPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/TaskListPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/components/schedule/TaskHierarchyPanel.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/components/schedule/TaskSegmentEditor.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/dialogs/TaskEditorDialog.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/ui/ConfirmActionDialog.tsx", "type": "depends_on" }
  ]
}
```
