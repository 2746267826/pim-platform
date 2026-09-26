# src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历模块全部 API/服务边界 DTO：日历与事件 CRUD、任务与调度、回收站/批量、分层视图、数据中心、Outlook 同步与冲突、提醒、报告等请求/响应记录类型。
- 主要依赖：`System.ComponentModel.DataAnnotations`（Required/MaxLength）
- 被谁使用：Calendar 端点与 `CalendarService`/`Outlook*`/`Reminder*`/`Report*` 等服务

## 函数级结构化伪代码

### 日历与事件
#### `CreateCalendarRequest` / `CalendarResponse`
- 输入：创建名、颜色、Kind；响应对应 Id/Name/Color/Kind/IsDefault/EventCount
- 输出：record 实例
- 副作用：无
- 步骤：校验 Name 必填≤100、Color≤7
- 分支与异常：校验由框架触发
- 调用：无

#### `CreateEventRequest` / `UpdateEventRequest` / `EventResponse`
- 输入：CalendarId、Title、Description、Location、DtStart/DtEnd、RRule、Uid、全日与时区等
- 输出：事件读写 DTO；`EventResponse` 另含 Status/Source/OriginalEventId/ExternalMetadataJson/Recurrence 元数据
- 副作用：无
- 步骤：Title 必填≤255；起止时间必填；Update 的 IsAllDay 可空
- 分支与异常：无
- 调用：无

### 任务与调度
#### `CreateTaskRequest` / `UpdateTaskRequest` / `TaskResponse` / `MoveTaskRequest` / `ScheduleRequest` / `SchedulePlanResponse` / `ScheduledTaskSlot` / `PlanTaskRequest` / `CreateTaskExecutionSegmentRequest` / `TaskExecutionSegmentResponse`
- 输入：任务字段（优先级、时长、Due、状态、规划端点）；调度任务 Id 列表；执行段起止/Status/Source
- 输出：任务树（含 SubTasks）、排程计划与槽位、执行段响应
- 副作用：无
- 步骤：Title 必填；Move 可调 ScheduledStart/Duration/SortOrder/PlannedEnd
- 分支与异常：无
- 调用：无

### 批量与回收站
#### `ImportResult` / `BatchDeleteRequest` / `BatchDeleteResult` / `CalendarOperationSample` / `CalendarDeletePreviewResponse` / `CalendarOperationResult` / `CalendarRestoreConflict` / `CalendarRestorePreviewResponse` / `CalendarRestoreRequest` / `CalendarRecycleBinItem` / `CalendarRecycleBinDetail` / `BatchIdsRequest` / `BatchTaskUpdateRequest`
- 输入：Id 列表、恢复是否副本、批量改 Status/Priority/CalendarId
- 输出：删除预览/结果、冲突列表、回收站条目与详情
- 副作用：无
- 步骤：操作样例含 Type/Title/时间/BookName；预览含 RequiresStrictConfirmation
- 分支与异常：无
- 调用：无

### 领域对象与分层
#### `CreateDomainProjectRequest` / `CreateTaskBookRequest` / `AddTaskChecklistItemRequest` / `CreateHabitRequest` / `CreateHabitOccurrenceRequest` / `CreateAvailabilityWindowRequest` / `CreateAiPlanningPlaceholderRequest`
- 输入：项目/任务本/清单项/习惯/可用窗/AI 占位创建字段
- 输出：对应请求 record
- 副作用：无
- 步骤：名称/标题必填与 MaxLength；可选 Status/Source/Kind/RuleJson
- 分支与异常：无
- 调用：无

#### `CalendarLayerQuery` / `CalendarLayerItem` / `CalendarLayerResponse`
- 输入：时间窗、Layers、OutlookOnly
- 输出：分层条目（Layer/ObjectType/颜色/RequiresConfirmation）与响应
- 副作用：无
- 步骤：按区间聚合多层日历对象
- 分支与异常：无
- 调用：无

### 数据中心
#### `DataCenterQueryRequest` / `DataCenterItem` / `DataCenterQueryResponse` / `DataCenterObjectRef` / `DataCenterBatchOperationRequest` / `DataCenterBatchPreviewResponse` / `DataCenterBatchExecutionResponse` / `DataCenterExecuteBatchRequest` / `DataCenterRestoreRequest`
- 输入：搜索/类型/来源/分页；批量 Action+Objects；ConfirmationId；AuditVersionId
- 输出：分页列表、风险预览、执行结果
- 副作用：无
- 步骤：PendingOnly 过滤；默认 Page=1 PageSize=50
- 分支与异常：无
- 调用：无

### 导入与 Outlook
#### `ImportSkippedItem` / `ImportReport` / `OutlookSettingsResponse` / `UpdateOutlookSettingsRequest` / `OutlookDeviceCodeRequestResponse` / `OutlookDeviceCodePollRequest` / `OutlookSyncStep` / `OutlookSyncBatchResponse` / `ConflictResolutionRequest` / `OutlookStopSyncExecuteRequest` / `SyncConflictDetailDto`
- 输入：租户/ClientId/Scopes；DeviceCode；冲突 Action/MergedFields；停止同步 ConfirmationId
- 输出：设置与 token 健康、设备码、同步批次计数与步骤、冲突快照
- 副作用：无
- 步骤：同步批次含 Read/Created/Updated/Conflict/Confirmation/Failure 计数
- 分支与异常：无
- 调用：无

### 提醒与报告
#### `CreateReminderRequest` / `ReminderResponse` / `ReminderActionResponse` / `ReminderNotificationPayloadDto` / `ReminderDeliveryDto` / `GenerateReportRequest` / `ReportArtifactDto` / `ReportSuggestionDto`
- 输入：关联对象、渠道、勿扰、ScheduledAt；报告 Kind/Date/ProjectId
- 输出：提醒状态、投递与 payload、报告 Markdown/Metrics、建议 Action
- 副作用：无
- 步骤：Channels 为字符串列表；报告含 RiskLevel/Status
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations；命名空间 `Pim.Module.Calendar.DTOs`
2. 日历：`CreateCalendarRequest`、`CalendarResponse`
3. 事件：创建/更新请求 + `EventResponse`（含复发与外部元数据 JSON 默认值）
4. 任务：创建/更新/`TaskResponse`（SubTasks 列表）、`MoveTaskRequest`、`ScheduleRequest`/`SchedulePlanResponse`/`ScheduledTaskSlot`
5. 导入/批量删除：`ImportResult`、`BatchDeleteRequest`/`BatchDeleteResult`
6. 操作预览与结果、恢复冲突与回收站 Item/Detail、`BatchIdsRequest`/`BatchTaskUpdateRequest`
7. `PlanTaskRequest`、任务执行段创建/响应
8. 领域项目、任务本、清单项、习惯及 occurrence、可用窗、AI 占位请求
9. 日历分层 Query/Item/Response
10. 数据中心查询/批量/执行/恢复 DTO
11. 导入报告与跳过样例；Outlook 设置、设备码、同步步骤/批次、冲突解决与停止同步
12. 提醒创建/响应/动作/通知/投递；报告生成与 Artifact/Suggestion
13. 全部为 `record`，无运行时逻辑

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs",
      "label": "CalendarDtos",
      "path": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs.md",
      "layer": "module.calendar",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Endpoints", "to": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs", "type": "depends_on" }
  ]
}
```
