# src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历本/日程/任务的软删除与删除预览；统一写 `DeletedAt`/`DeletedByOperationId`/`DeletedByOperationKind`；审计成功操作。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`CalendarAuditWriter`
  - 实体 `CalendarEntity`/`EventEntity`/`TaskEntity`
  - DTO `CalendarDeletePreviewResponse`/`CalendarOperationResult`/`CalendarOperationSample`
  - `DomainException`
- 被谁使用：
  - Calendar 模块端点（删除/批量删除/预览）
  - DI 注册于 Calendar 模块

## 函数级结构化伪代码

### CalendarDeleteService
#### 构造 `CalendarDeleteService(PimDbContext, ICurrentUserService, CalendarAuditWriter)`
- 输入：db、当前用户、审计
- 输出：实例
- 副作用：保存字段
- 步骤：赋值 `_db`/`_currentUser`/`_audit`
- 分支与异常：无
- 调用：无

#### 属性 `UserId`
- 输入：无
- 输出：Guid
- 副作用：无
- 步骤：取 `_currentUser.UserId`，null → `DomainException(1002,"未登录")`
- 分支与异常：1002
- 调用：`ICurrentUserService`

#### `Task<CalendarDeletePreviewResponse> PreviewCalendarDeleteAsync(Guid calendarId, ct)`
- 输入：日历 Id
- 输出：预览 DTO（类型、名称、操作种、影响数、样例、文案、可执行）
- 副作用：只读查询
- 步骤：
  1. `LoadCalendarAsync`。
  2. `operationKind = CalendarOperationKind`（task→task-book 否则 calendar-book）。
  3. 样例最多 5；`CountCalendarChildrenAsync`。
  4. 构造 Preview：对象类型 task-book/calendar-book；消息含名称与活跃子项数；`CanExecute=true`。
- 分支与异常：日历不存在 02002
- 调用：Load/LoadSamples/Count

#### `Task<CalendarOperationResult> DeleteCalendarAsync(Guid calendarId, ct)`
- 输入：日历 Id
- 输出：操作结果（operation、operationId、影响 Id 列表、样例、消息）
- 副作用：软删日历及子任务或子日程；SaveChanges；审计 `calendar.books.delete`
- 步骤：
  1. 加载日历；生成 operationId；记录 deletedAt；取样例；affectedIds 先含日历。
  2. 标记日历 Deleted* 与 UpdatedAt。
  3. Kind==task：查用户下该日历 Tasks，逐个 MarkDeleted 并收集 Id。
  4. 否则：查该日历 Events（含 Calendar 过滤 UserId），MarkDeleted。
  5. SaveChanges；审计；返回 Result。
- 分支与异常：02002
- 调用：MarkDeleted、`_audit.RecordSuccessAsync`

#### `DeleteEventAsync` / `BatchDeleteEventsAsync`
- 输入：单 Id 或 Id 枚举
- 输出：`CalendarOperationResult`
- 副作用：软删事件；审计 single/batch
- 步骤：
  - 单删：LoadEvent → MarkDeleted → Save → 审计 `calendar.events.delete` → Result。
  - 批删：NormalizeIds；空则 EmptyResult；查询用户事件 OrderBy DtStart；空 Empty；统一 operationKind batch-event；Save；审计 objectId=operationId。
- 分支与异常：02001 单删不存在；批删无匹配不抛
- 调用：LoadEvent/NormalizeIds/MarkDeleted/Result/EmptyResult

#### `DeleteTaskAsync` / `BatchDeleteTasksAsync`
- 输入：单 Id 或 Id 枚举
- 输出：`CalendarOperationResult`
- 副作用：软删任务；审计
- 步骤：与事件对称；operation `calendar.tasks.delete` / `batch_delete`；operationKind single-task/batch-task；任务按 Title 排序
- 分支与异常：02004 单删；批删空集 Empty
- 调用：LoadTask 等

#### 私有加载/计数/样例
- `LoadCalendarAsync`：Id+UserId 否则 02002。
- `LoadEventAsync`：Include Calendar，UserId 匹配否则 02001。
- `LoadTaskAsync`：Include Calendar，UserId 否则 02004。
- `CountCalendarChildrenAsync`：task 计 Tasks 否则 Events。
- `LoadCalendarChildSamplesAsync`：取 take 条映射 `CalendarOperationSample`。
- 调用：EF

#### 静态辅助
- `CalendarOperationKind`：task→task-book 否则 calendar-book。
- `MarkDeleted(Event|Task)`：写 DeletedAt/ByOperationId/Kind、UpdatedAt。
- `Result` 两重载：从 Events/Tasks 建 affectedIds 与最多 5 样例。
- `EmptyResult`：count=0 空列表。
- `NormalizeIds`：去 Empty、Distinct。
- `Metadata`：operationId/kind/affectedCount，可选 title。

## 近逐行中文伪代码

1. 引入 EF、DomainException、Auth、Data、DTOs、Entities。
2. sealed 类注入 db、currentUser、audit。
3. UserId：null 抛 1002。
4. Preview：加载日历→操作种→样例/计数→构造预览响应。
5. DeleteCalendar：生成 operationId；软删本；task 删任务 else 删日程；Save；审计 books.delete。
6. DeleteEvent：加载→Mark→Save→审计 events.delete→Result。
7. BatchDeleteEvents：规范化 Id；查询；Mark；Save；审计 batch_delete。
8. DeleteTask/BatchDeleteTasks：对称任务路径。
9. Load*：FirstOrDefault 否则业务码异常。
10. Count/Samples：按 Kind 分支 Task/Event。
11. MarkDeleted 写四字段；Result/Empty/Normalize/Metadata 纯组装。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs",
      "label": "CalendarDeleteService",
      "path": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs", "type": "depends_on" }
  ]
}
```
