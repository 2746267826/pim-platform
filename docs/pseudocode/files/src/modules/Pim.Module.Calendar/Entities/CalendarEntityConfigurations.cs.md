# src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：为 Calendar 模块各实体提供 EF Core `IEntityTypeConfiguration`：软删除查询过滤、默认值、索引与导航关系。
- 主要依赖：
  - `Microsoft.EntityFrameworkCore` / `Metadata.Builders`
  - 同模块实体类型（`CalendarEntity`、`EventEntity`、`TaskEntity` 等）
- 被谁使用：
  - `PimDbContext` / 模块模型注册（`ApplyConfigurationsFromAssembly` 或显式 Apply）
  - 迁移与快照生成

## 函数级结构化伪代码

### CalendarEntityConfiguration
#### `void Configure(EntityTypeBuilder<CalendarEntity> builder)`
- 输入：实体构建器
- 输出：void
- 副作用：全局查询过滤 `DeletedAt == null`；索引 `UserId`、`(UserId,DeletedAt)`、`DeletedByOperationId`
- 步骤：1. HasQueryFilter；2–4. 建索引。
- 分支与异常：无
- 调用：EF Fluent API

### EventEntityConfiguration
#### `void Configure(EntityTypeBuilder<EventEntity> builder)`
- 输入：构建器
- 输出：void
- 副作用：软删过滤；JSON 默认值；多字段索引；与 Calendar 一对多
- 步骤：
  1. QueryFilter `DeletedAt == null`。
  2. `ExternalMetadataJson`/`ExDatesJson`/`RecurrenceMetadataJson` 默认 `{}`/`[]`/`{}`。
  3. 索引：CalendarId、Uid、SourceUid、OutlookEventId、OutlookChangeKey、`(DeletedAt,DtStart)`、DeletedByOperationId。
  4. `HasOne(Calendar).WithMany(Events).HasForeignKey(CalendarId)`。
- 分支与异常：无
- 调用：EF Fluent API

### TaskEntityConfiguration
#### `void Configure(EntityTypeBuilder<TaskEntity> builder)`
- 输入：构建器
- 输出：void
- 副作用：软删过滤；用户/日历/项目/任务本/状态/时间索引；FK 到 Calendar、DomainProject、TaskBook、ParentTask
- 步骤：
  1. QueryFilter。
  2. 索引 UserId、组合索引、Status、DeletedByOperationId。
  3. HasOne Calendar/DomainProject/TaskBook/ParentTask（SubTasks 自引用）。
- 分支与异常：无
- 调用：EF Fluent API

### DomainProjectEntityConfiguration / TaskBookEntityConfiguration / TaskChecklistItemEntityConfiguration
#### 各 `Configure`
- 输入：对应 EntityTypeBuilder
- 输出：void
- 副作用：
  - DomainProject：软删；唯一 `(UserId,Name)`；`(UserId,Status)`。
  - TaskBook：软删；`(UserId,Name,DomainProjectId)` 与 Status 索引；FK DomainProject。
  - Checklist：软删；UserId 与 `(TaskId,SortOrder)`；FK Task。
- 步骤：Filter → Index →（可选）关系
- 分支与异常：无
- 调用：EF Fluent API

### HabitRoutineEntityConfiguration / HabitOccurrenceEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：软删；Routine 的 RuleJson 默认 `{}` 与 Status/Cadence 索引；Occurrence 索引与 FK HabitRoutine
- 步骤：Filter → 默认值/索引 → FK
- 分支与异常：无
- 调用：EF Fluent API

### AvailabilityWindowEntityConfiguration / AiPlanningPlaceholderEntityConfiguration / TaskExecutionSegmentEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：均软删；时间窗/状态/确认 Id 索引；Segment 另 FK Task（WithMany 无导航集合）
- 步骤：Filter → Index →（Segment）HasOne Task
- 分支与异常：无
- 调用：EF Fluent API

### PendingConfirmationEntityConfiguration / SchedulingFeedbackEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：无软删过滤；仅 UserId/Status 索引
- 步骤：HasIndex
- 分支与异常：无
- 调用：EF Fluent API

### OutlookConnectionEntityConfiguration / OutlookSyncBatchEntityConfiguration / SyncConflictEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：
  - Connection：Provider/TenantId/Scopes/Status/TokenHealth 默认值；UserId 唯一索引。
  - SyncBatch：Provider/Status/StepsJson/ErrorsJson 默认；StartedAt `now()`；UserId 与时间索引。
  - SyncConflict：Provider/ObjectType/Status/快照 JSON 默认；CreatedAt `now()`；多维索引含 GraphEventId、ResolvedConfirmationId。
- 步骤：Property 默认值 → Index
- 分支与异常：无
- 调用：EF Fluent API

### ReminderEntityConfiguration / ReminderDeliveryEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：Reminder 软删 + ChannelsJson/Status/CreatedAt 默认与索引；Delivery 无软删，FK Reminder.Deliveries
- 步骤：Filter/默认值/索引/关系
- 分支与异常：无
- 调用：EF Fluent API

### ReportArtifactEntityConfiguration / ReportSuggestionEntityConfiguration
#### 各 `Configure`
- 输入：构建器
- 输出：void
- 副作用：
  - Artifact：软删；RiskLevel/InputsJson/MetricsJson/Status/GeneratedAt 默认；索引 `(UserId,Kind,GeneratedAt)` 与 `(UserId,ProjectId)`。
  - Suggestion：ChangedFieldsJson/PayloadJson/Status/CreatedAt 默认；索引；FK Report.Suggestions。
- 步骤：Filter（仅 Artifact）→ 默认值 → 索引 → 关系
- 分支与异常：无
- 调用：EF Fluent API

## 近逐行中文伪代码

1. 引入 EF Core 与 Builders；命名空间 `Pim.Module.Calendar.Entities`。
2. `CalendarEntityConfiguration`：QueryFilter 未删；三索引。
3. `EventEntityConfiguration`：Filter；三 JSON 默认；七索引；Calendar FK。
4. `TaskEntityConfiguration`：Filter；多组合索引；Calendar/DomainProject/TaskBook/ParentTask FK。
5. `DomainProjectEntityConfiguration`：Filter；唯一名；状态索引。
6. `TaskBookEntityConfiguration`：Filter；名/项目索引；DomainProject FK。
7. `TaskChecklistItemEntityConfiguration`：Filter；UserId 与排序索引；Task FK。
8. `HabitRoutineEntityConfiguration`：Filter；RuleJson 默认；Status/Cadence 索引。
9. `HabitOccurrenceEntityConfiguration`：Filter；Routine/时间/Confirmation 索引；Routine FK。
10. `AvailabilityWindowEntityConfiguration`：Filter；时间与 Kind 索引。
11. `AiPlanningPlaceholderEntityConfiguration`：Filter；时间/确认/状态索引。
12. `TaskExecutionSegmentEntityConfiguration`：Filter；User/Task/StartsAt/Confirmation 索引；Task FK。
13. `PendingConfirmationEntityConfiguration`：UserId、Status 索引。
14. `SchedulingFeedbackEntityConfiguration`：UserId 索引。
15. `OutlookConnectionEntityConfiguration`：五默认值；UserId 唯一。
16. `OutlookSyncBatchEntityConfiguration`：默认与 `now()`；三索引。
17. `SyncConflictEntityConfiguration`：默认与快照；四索引。
18. `ReminderEntityConfiguration`：Filter；Channels/Status/CreatedAt；两索引。
19. `ReminderDeliveryEntityConfiguration`：Payload/Status/CreatedAt；索引；Reminder FK。
20. `ReportArtifactEntityConfiguration`：Filter；Risk/Inputs/Metrics/Status/GeneratedAt；两索引。
21. `ReportSuggestionEntityConfiguration`：ChangedFields/Payload/Status/CreatedAt；索引；Report FK。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs",
      "label": "CalendarEntityConfigurations",
      "path": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs", "type": "depends_on" }
  ]
}
```
