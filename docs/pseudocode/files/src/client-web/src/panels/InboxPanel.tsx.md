# src/client-web/src/panels/InboxPanel.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：侧栏收集箱：未排程任务列表，可选拖到日历，点击编辑，新建任务/日程菜单。
- 主要依赖：getTasks、Task/EventEditorDialog
- 被谁使用：日历/布局侧栏

## 函数级结构化伪代码

### InboxPanel
- 过滤 isInbox 或无 dtStart
- draggable 时 setData task id/title；拖后短时忽略 click
- 菜单新建任务/日程；编辑对话框

## 近逐行中文伪代码

1. 拉全部任务筛未排程。
2. 可拖任务到 FullCalendar。
3. 点击开任务编辑（拖后防误点）。
4. 加号菜单新建。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/panels/InboxPanel.tsx",
      "label": "InboxPanel",
      "path": "src/client-web/src/panels/InboxPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/panels/InboxPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/panels/InboxPanel.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/panels/InboxPanel.tsx",
      "to": "src/client-web/src/dialogs/TaskEditorDialog.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/panels/InboxPanel.tsx",
      "to": "src/client-web/src/dialogs/EventEditorDialog.tsx",
      "type": "depends_on"
    }
  ]
}
`
