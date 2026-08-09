# src/client-web/src/api/calendar.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：日历/任务/回收站/数据中心/习惯/提醒/报告/Outlook 同步的 REST 客户端与路径工厂；ICS 导入导出走原生 fetch。
- 主要依赖：`./client`（`apiGet`/`apiPost`/`apiPut`/`apiDelete`）、`../types` 大量 DTO
- 被谁使用：`CalendarPage`、`TaskListPage`、`DataCenterPage`、`RecycleBinPage`、`RemindersPage`、`ReportsPage`、`SyncPage`、`HabitsPage`、`WorkbenchPage`、`TodayPage`、`Sidebar`、任务/事件对话框、日程组件等

## 函数级结构化伪代码

### 类型
#### TaskMutationData / RecycleBinParams / GetTasksParams / GetEventsParams
- 输入/输出：任务写模型字段；回收站筛选分页；任务/事件列表查询参数
- 副作用：无
- 步骤：类型声明
- 分支与异常：无
- 调用：无

### appendQuery(path, params)
- 输入：路径与键值（可 undefined）
- 输出：带 query 的路径或原 path
- 副作用：无
- 步骤：跳过 undefined；`URLSearchParams` 拼接
- 分支与异常：无 qs 则原 path
- 调用：`URLSearchParams`

### calendarApiPaths
- 输入：各工厂参数（id、type、layer 查询等）
- 输出：相对 API 路径字符串（前缀由 client 补 `/api/v1` 语义）
- 副作用：无
- 步骤：覆盖回收站、日历删除预览、事件批量删、任务 plan/segments、layers、数据中心 batch/restore/audit、projects/task-books/checklist、habits/reminders/actions、reports、outlook settings/device-code/sync、task batch
- 分支与异常：路径段 `encodeURIComponent`
- 调用：`appendQuery`、`encodeURIComponent`

### 日历 CRUD
#### getCalendars / createCalendar / updateCalendar / deleteCalendar
- 输入：kind 可选；创建 name/color/kind；更新 name/color；id
- 输出：列表/实体或 void
- 副作用：HTTP GET/POST/PUT/DELETE `/calendar/calendars`
- 步骤：调 api* 后取 `r.data`（delete 无返回体）
- 分支与异常：透传网络/API 错误
- 调用：`apiGet`/`apiPost`/`apiPut`/`apiDelete`

### 事件
#### getEvents / createEvent / updateEvent / deleteEvent / batchDeleteEvents / getEventsPaged
- 输入：时间窗或 Partial 实体或 ids 或分页筛选
- 输出：事件数组/实体/`CalendarOperationResult`/`PagedResult`
- 副作用：HTTP
- 步骤：标准 api 封装；batch 用 `eventBatchDelete` 路径
- 分支与异常：透传
- 调用：`apiGet`/`apiPost`/`apiPut`/`apiDelete`、`calendarApiPaths`

### 任务
#### buildTasksPath / getTasks / getTasksPaged / createTask / updateTask / moveTask / taskToMutationData / deleteTask / planTask / list|create|delete TaskExecutionSegment / batchDeleteTasks / batchUpdateTasks
- 输入：inbox 标志、GetTasksParams、TaskMutationData、move 载荷、plan 载荷、segment 载荷、批量 ids/字段
- 输出：任务列表/分页/实体/void/segment 列表等
- 副作用：HTTP
- 步骤：
  1. `buildTasksPath`：inbox 时 `?inbox=true`。
  2. `getTasksPaged` 组装多条件 query，默认 page=1 pageSize=50。
  3. `taskToMutationData` 从 `TaskResponse` 映射写模型并允许 overrides。
  4. 其余为对应路径的 api 调用。
- 分支与异常：透传
- 调用：`api*`、`calendarApiPaths`

### 回收站与删除预览
#### getRecycleBin / previewRecycleRestore / restoreRecycleItem / previewCalendarDelete
- 输入：分页筛选；type+id；restoreAsCopy；calendar id
- 输出：分页项/预览/操作结果/删除预览
- 副作用：HTTP（多为 POST 预览/恢复）
- 步骤：走 `calendarApiPaths` 对应路径
- 分支与异常：透传
- 调用：`apiGet`/`apiPost`

### 图层与数据中心
#### getCalendarLayers / queryDataCenter / preview|requestConfirmation|execute DataCenterBatch / getAuditExport / preview|requestConfirmation DataCenterRestore
- 输入：图层查询；查询/批量请求体；confirmationId；auditVersionId+reason
- 输出：图层响应、查询/预览/执行结果、审计导出、确认票据
- 副作用：HTTP
- 步骤：路径工厂 + POST/GET
- 分支与异常：透传
- 调用：`apiGet`/`apiPost`、`calendarApiPaths`

### 项目/任务本/清单/习惯/提醒/报告
#### get|create Projects/TaskBooks；addTaskChecklistItem；get|create Habits；get|create|snooze|dismiss|handleAction Reminders；getReminderDeliveryLog；get|getOne|generate|archive Reports；requestReportSuggestionAction
- 输入：各 Create* 请求、id、action、scheduledAt 可选
- 输出：对应领域 DTO / OperationConfirmation
- 副作用：HTTP
- 步骤：路径工厂；snooze 可选 query `scheduledAt`
- 分支与异常：透传
- 调用：`apiGet`/`apiPost`、`calendarApiPaths`

### Outlook
#### get|update OutlookSettings；createOutlookDeviceCode；pollOutlookDeviceCode；runOutlookSync；getOutlookSyncBatches
- 输入：设置体；deviceCode
- 输出：设置/设备码响应/同步批次
- 副作用：HTTP
- 步骤：对应 outlook 路径
- 分支与异常：透传
- 调用：`apiGet`/`apiPost`/`apiPut`

### ICS
#### exportIcs(ids?, start?, end?)
- 输入：可选 id 列表与时间窗
- 输出：void（触发浏览器下载）
- 副作用：`fetch` 带 Bearer；blob 下载 `pim-events.ics`
- 步骤：拼 query → fetch `/api/v1/calendar/export-ics` → 非 ok 抛「导出失败」→ blob URL → a.click → revoke
- 分支与异常：`!resp.ok` throw
- 调用：`fetch`、`localStorage.getItem('accessToken')`

#### importIcs(file, calendarId?)
- 输入：File、可选日历 id
- 输出：`ImportReport`
- 副作用：multipart POST `/api/v1/calendar/import-ics`
- 步骤：FormData 填 file/calendarId；Bearer；非 ok 抛「导入失败」；解析 `ApiResponse` 取 data
- 分支与异常：`!resp.ok` throw
- 调用：`fetch`

## 近逐行中文伪代码

1. 导入 client 与 types；导出写模型与查询参数类型。
2. `appendQuery` 过滤 undefined 拼 query。
3. `calendarApiPaths` 集中声明日历域路径。
4. 日历/事件/任务 CRUD 与分页、批量、移动、计划、执行段。
5. 回收站预览恢复、日历删除预览。
6. 图层、数据中心查询/批量确认执行/审计/恢复确认。
7. 项目、任务本、清单、习惯、提醒动作与投递日志。
8. 报告生成归档与建议动作确认。
9. Outlook 设置、设备码、同步与批次。
10. ICS 导入导出绕过 api 封装，直接 fetch + token。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/calendar.ts",
      "label": "calendar",
      "path": "src/client-web/src/api/calendar.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/calendar.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/calendar.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/calendars", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/events", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/tasks", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/layers", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/data-center", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/outlook", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/export-ics", "type": "http" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "/calendar/import-ics", "type": "http" },
    { "from": "src/client-web/src/pages/CalendarPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TaskListPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/DataCenterPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/layout/Sidebar.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" }
  ]
}
```
