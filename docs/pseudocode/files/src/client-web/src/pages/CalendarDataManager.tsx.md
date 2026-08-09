# src/client-web/src/pages/CalendarDataManager.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：日程数据管理：分页列表、筛选、ICS 导入导出、批量删除确认、详情弹层。
- 主要依赖：calendar API、OperationResultBanner、ConfirmActionDialog
- 被谁使用：设置入口「管理日程数据」

## 函数级结构化伪代码

### pruneSelectedIds / hasStaleSelection
- 过滤不在当前页的选中 ID

### CalendarDataManager
- 筛选：search/calendarId/dateRange/custom；分页 page
- 查询 calendars 与 events-paged
- 选中态 microtask 清理 stale
- importMut / deleteMut：成功写 result 并 invalidate
- handleBatchDelete：originalEventId 去重 → ConfirmActionDialog
- 导出选中/全部；导入选文件 .ics
- 表格 + 分页 + 详情 modal + 确认删除

## 近逐行中文伪代码

1. 状态：筛选、选中、详情、操作结果/错误、删除确认。
2. 按 dateRange 算 start/end。
3. 拉日历与分页事件。
4. 页变清理无效选中。
5. 导入/删除 mutation 与 banner。
6. 批量删走确认；导出 ICS；导入点文件。
7. 渲染工具栏、筛选条、表、分页、详情、确认框。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/CalendarDataManager.tsx",
      "label": "CalendarDataManager",
      "path": "src/client-web/src/pages/CalendarDataManager.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/CalendarDataManager.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/CalendarDataManager.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarDataManager.tsx", "to": "src/client-web/src/ui/OperationResultBanner.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/CalendarDataManager.tsx", "to": "src/client-web/src/ui/ConfirmActionDialog.tsx", "type": "depends_on" }
  ]
}
```
