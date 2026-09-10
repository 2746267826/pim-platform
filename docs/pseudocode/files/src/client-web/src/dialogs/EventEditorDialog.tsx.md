# src/client-web/src/dialogs/EventEditorDialog.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：日程新建/编辑抽屉：字段表单、创建/更新/删除（确认框）。
- 主要依赖：calendar API、EditorDrawer、ConfirmActionDialog、Field
- 被谁使用：CalendarPage、TodayPage 等

## 函数级结构化伪代码

### EventEditorDialog
- 用 open/event/defaults 拼 formKey remount Form

### EventEditorForm
- 本地 state 标题/描述/地点/起止/全天/calendarId
- open 时拉 calendars；单日历默认选中
- create/update/delete mutations 成功 invalidate 并关闭
- handleDelete → ConfirmActionDialog；submit 分支 create/update
- EditorDrawer footer：删除/取消/保存

## 近逐行中文伪代码

1. key 重置表单避免脏状态。
2. 表单字段与日历下拉。
3. 提交走 create 或 update。
4. 删除二次确认后 deleteEvent。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/dialogs/EventEditorDialog.tsx",
      "label": "EventEditorDialog",
      "path": "src/client-web/src/dialogs/EventEditorDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/dialogs/EventEditorDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/dialogs/EventEditorDialog.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/dialogs/EventEditorDialog.tsx", "to": "src/client-web/src/ui/EditorDrawer.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/dialogs/EventEditorDialog.tsx", "to": "src/client-web/src/ui/ConfirmActionDialog.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/dialogs/EventEditorDialog.tsx", "to": "src/client-web/src/dialogs/common.tsx", "type": "depends_on" }
  ]
}
```
