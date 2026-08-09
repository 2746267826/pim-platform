# src/client-web/src/dialogs/TaskEditorDialog.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：任务新建/编辑抽屉：字段、完成切换、moveTask 确认日程、删除确认。
- 主要依赖：calendar API、EditorDrawer、ConfirmActionDialog、Field
- 被谁使用：TaskListPage、CalendarPage、TodayPage

## 函数级结构化伪代码

### TaskEditorDialog
- formKey remount

### invalidateTaskRelatedQueries
- 失效 tasks/tasks-paged/today*

### TaskEditorForm
- state 标题/优先级/起止/due/时长/任务本
- create/update(可先 moveTask)/delete mutations
- 校验：不可清空 plannedEnd；submit 创建或更新
- 删除 ConfirmActionDialog；完成状态切换

## 近逐行中文伪代码

1. open 时拉 task 日历列表。
2. 提交 create 或 update（变更开始可 moveTask）。
3. 删除二次确认进回收站缓存失效。
4. 抽屉 footer 删除/完成/取消/保存。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "label": "TaskEditorDialog",
      "path": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/dialogs/TaskEditorDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "to": "src/client-web/src/ui/EditorDrawer.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "to": "src/client-web/src/ui/ConfirmActionDialog.tsx",
      "type": "depends_on"
    }
  ]
}
`
