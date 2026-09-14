# src/client-web/src/pages/CalendarPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：FullCalendar 日历页：时间轴/月视图、图层切换、事件/任务编辑、外部任务拖放 planTask。
- 主要依赖：FullCalendar 插件、calendar API、CalendarVisibilityContext、LayerToolbar、编辑对话框
- 被谁使用：路由日历

## 函数级结构化伪代码

### CalendarPage
- URL view 同步 mode；Draggable 绑定 .js-draggable-task
- 查询 events(范围)、tasks、calendar-layers(启用层+outlookOnly)
- 过滤 hiddenCalendarIds；buildCalendarEvents
- planTaskMutation 拖放安排任务
- 交互：选区新建事件、点击打开编辑、图层 toggle、前后/今天导航
- FullCalendar 配置 droppable/selectable

### 辅助（同文件）
- normalizeMode、rangeForDate、buildCalendarEvents、renderCalendarEvent、时间格式等

## 近逐行中文伪代码

1. 状态：范围、编辑器、启用图层、outlookOnly。
2. 拉事件/任务/图层；合并为 FC events。
3. 工具栏图层与分段视图。
4. 拖任务→planTask；点事件/任务开对话框。
5. 选时间段开新建日程。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/CalendarPage.tsx",
      "label": "CalendarPage",
      "path": "src/client-web/src/pages/CalendarPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/CalendarPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/context/CalendarVisibilityContext.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/dialogs/EventEditorDialog.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/dialogs/TaskEditorDialog.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/components/schedule/CalendarLayerToolbar.tsx", "type": "depends_on" }
  ]
}
```
