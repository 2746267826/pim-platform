# src/modules/Pim.Module.Calendar/Services/CalendarService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：当前用户日历/事件/任务的 CRUD、分页查询、重复展开、Outlook ICS 导入、任务计划与批量更新。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`RecurrenceService`、`OutlookIcsService`、Calendar 实体与 DTO、`DomainException`
- 被谁使用：Calendar 模块 HTTP 端点与相关编排服务

## 函数级结构化伪代码

### CalendarService
#### 构造 CalendarService(PimDbContext db, ICurrentUserService currentUser, RecurrenceService recurrence)
- 输入：DbContext、当前用户、重复展开服务
- 输出：实例
- 副作用：保存 `_db`/`_currentUser`/`_recurrence`
- 步骤：赋值字段
- 分支与异常：无
- 调用：无

#### Guid UserId（属性）
- 输入：无
- 输出：当前用户 Id
- 副作用：无
- 步骤：`_currentUser.UserId` 为空则 `DomainException(01002, "未登录")`
- 分支与异常：未登录抛错
- 调用：无

#### Task\<List\<CalendarResponse\>\> GetCalendarsAsync(string? kind, CancellationToken ct)
- 输入：可选 kind 过滤
- 输出：日历列表（含事件数）
- 副作用：查询 DB
- 步骤：按 UserId 过滤；可选 Kind；投影 Id/Name/Color/Kind/IsDefault/Events.Count
- 分支与异常：无
- 调用：EF ToListAsync

#### Task\<CalendarResponse\> CreateCalendarAsync(CreateCalendarRequest request, CancellationToken ct)
- 输入：创建请求
- 输出：新建日历响应
- 副作用：插入 `CalendarEntity`
- 步骤：
  1. kind 默认 `calendar`
  2. 构造实体：UserId、Name、Color 默认 `#3B82F6`、Kind
  3. IsDefault = 该用户该 kind 尚无任何日历
  4. Add + SaveChanges；返回 Count=0 的响应
- 分支与异常：无
- 调用：SaveChangesAsync

#### Task\<CalendarResponse\> UpdateCalendarAsync(Guid id, CreateCalendarRequest request, CancellationToken ct)
- 输入：日历 id、请求
- 输出：更新后响应
- 副作用：改名/颜色
- 步骤：按 id+UserId 查找，不存在 02002；更新 Name/Color/UpdatedAt；保存
- 分支与异常：不存在抛 DomainException
- 调用：SaveChangesAsync

#### Task DeleteCalendarAsync(Guid id, CancellationToken ct)
- 输入：日历 id
- 输出：无
- 副作用：软删除（DeletedAt=UtcNow）
- 步骤：查找；不存在 02002；设 DeletedAt；保存
- 分支与异常：不存在
- 调用：SaveChangesAsync

#### Task\<List\<EventResponse\>\> GetEventsAsync(DateTimeOffset start, DateTimeOffset end, CancellationToken ct)
- 输入：时间窗
- 输出：展开后的事件响应列表
- 副作用：查询+内存展开
- 步骤：
  1. minValidDate = MinValue+100年，过滤异常历史日期
  2. 取当前用户全部有效事件 AsNoTracking
  3. `_recurrence.ExpandEvents` 到 [start,end]
  4. 按 OccurrenceStart 排序并 MapExpandedEvent
- 分支与异常：无
- 调用：RecurrenceService.ExpandEvents

#### Task\<PagedResult\<EventResponse\>\> GetEventsPagedAsync(...)
- 输入：search/calendarId/start/end/page/pageSize
- 输出：分页展开结果
- 副作用：查询+展开+内存分页
- 步骤：用户过滤+有效日期；可选标题 Contains、calendarId；展开范围默认 Min~Max；OrderByDescending OccurrenceStart 后 Skip/Take
- 分支与异常：无
- 调用：ExpandEvents

#### Task\<EventResponse\> CreateEventAsync(CreateEventRequest request, CancellationToken ct)
- 输入：创建事件请求
- 输出：事件响应
- 副作用：插入 EventEntity
- 步骤：
  1. CalendarId 为空 Guid → GetOrCreateDefaultCalendarAsync("calendar")
  2. 否则查日历，不存在 02003
  3. 填 Uid/Title/时间/RRule/全天/时区等；Add+Save；MapEvent
- 分支与异常：日历不存在
- 调用：GetOrCreateDefaultCalendarAsync

#### Task\<ImportReport\> ImportOutlookIcsAsync(string icsContent, Guid? targetCalendarId, OutlookIcsService outlookIcs, CancellationToken ct)
- 输入：ICS 文本、目标日历、解析服务
- 输出：导入报告（imported/skipped/原因计数/样本）
- 副作用：可能批量插入事件
- 步骤：
  1. Parse；有 ErrorReason → 全跳过报告
  2. 解析目标日历或默认日历
  3. 对每条：InvalidReason / MinValue 日期 → 跳过
  4. FindActiveDuplicateReasonAsync 与批次内 FindAcceptedDuplicateReason
  5. 通过则映射字段 Source=outlook-ics，截断字符串，加入 accepted
  6. imported>0 时 SaveChanges；返回 ImportReport
- 分支与异常：解析失败、重复、无效日期
- 调用：outlookIcs.Parse、FindActiveDuplicateReasonAsync、Truncate

#### Task\<string?\> FindActiveDuplicateReasonAsync / static FindAcceptedDuplicateReason / static Truncate
- 输入：解析事件与已接受列表 / 字符串与 maxLength
- 输出：重复原因码或截断字符串
- 副作用：DB AnyAsync 查询（active）
- 步骤：依次检查 Uid、SourceUid、Title+时间；批次内 Uid 与 Title+时间
- 分支与异常：无
- 调用：EF AnyAsync

#### Task\<CalendarEntity\> GetOrCreateDefaultCalendarAsync(string kind, CancellationToken ct)
- 输入：kind
- 输出：默认或首个同 kind 日历；无则创建
- 副作用：可能插入默认日历
- 步骤：先 IsDefault 再任意 kind；否则新建 Name 按 task/calendar 中文默认名
- 分支与异常：无
- 调用：SaveChangesAsync

#### Task\<EventResponse\> UpdateEventAsync / Task\<List\<EventEntity\>\> GetEventEntitiesAsync / Task DeleteEventAsync / Task\<int\> DeleteEventsAsync
- 输入：id 或时间窗或 id 集合
- 输出：更新响应 / 实体列表 / 无 / 删除数量
- 副作用：更新字段或软删
- 步骤：用户隔离查询；Update 写字段；GetEventEntities 额外时间重叠过滤；Delete 设 DeletedAt；批量统计保存
- 分支与异常：02001 日程不存在
- 调用：SaveChangesAsync

#### Task\<List\<TaskResponse\>\> GetTasksAsync / GetTasksPagedAsync / CreateTaskAsync / UpdateTaskAsync / PlanTaskAsync
- 输入：inbox/筛选/创建更新/计划请求
- 输出：任务列表或分页或单条
- 副作用：读写 TaskEntity
- 步骤：
  1. Get：按 UserId、可选 inbox、SortOrder
  2. Paged：page/pageSize 夹紧；多条件过滤；排序 未完成优先、Due、SortOrder
  3. Create：解析时长 ISO8601；无日历且无 DtStart 则 IsInbox
  4. Update：状态 COMPLETED 写 CompletedAt
  5. Plan：写 PlannedStart/End、清 IsInbox
- 分支与异常：02004 任务不存在；02009 时长格式
- 调用：ParseDuration、MapTask

#### Task\<CalendarOperationResult\> BatchUpdateTasksAsync(BatchTaskUpdateRequest request, CancellationToken ct)
- 输入：批量 ids 与可选 Status/Priority/CalendarId
- 输出：操作结果（kind、operationId、计数、样本）
- 副作用：批量更新任务
- 步骤：规范化 ids；空 ids 或无变更字段 → 零结果；校验目标日历；加载任务 Include Calendar；循环应用字段；保存；返回样本最多 5 条
- 分支与异常：02003 日历不存在
- 调用：SaveChangesAsync

#### Task MoveTaskAsync / Task DeleteTaskAsync
- 输入：id 与移动请求 / id
- 输出：无
- 副作用：改排期/排序或软删
- 步骤：Find/过滤用户；写 ScheduledStart、SortOrder、PlannedEnd 或由 Duration 推算；Delete 设 DeletedAt
- 分支与异常：02004（Move 用 Find 不校验 UserId）
- 调用：SaveChangesAsync

#### static MapEvent / MapExpandedEvent / FormatDuration / ParseDuration / MapTask
- 输入：实体或 ExpandedEvent 或时长字符串
- 输出：DTO / TimeSpan?
- 副作用：无（Parse 失败抛 02009）
- 步骤：字段映射；XmlConvert.ToTimeSpan；子任务递归 MapTask
- 分支与异常：时长非法
- 调用：无

## 近逐行中文伪代码

1. 注入 Db、当前用户、RecurrenceService
2. UserId 未登录抛 01002
3. 日历：列表/创建/更新/软删，创建时判定 IsDefault
4. 事件查询：过滤过早无效日期；Recurrence 展开后映射
5. 分页事件：过滤后展开再内存分页
6. 创建事件：空 CalendarId 走默认日历
7. ICS 导入：解析失败短路；逐条校验与三层去重；批量保存
8. 去重：库内 uid/source_uid/title_time；批次内 uid/title_time
9. 默认日历：找 default 或任意 kind，否则中文名新建
10. 更新/删除事件：用户隔离；批量软删返回计数
11. 任务列表与分页：多维筛选与排序
12. 创建/更新/计划任务：时长 ISO8601；计划后出收件箱
13. 批量更新：校验日历；写状态/优先级/日历
14. 移动任务：排期与排序；删除软删
15. 映射与时长解析辅助函数

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs",
      "label": "CalendarService",
      "path": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/CalendarService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/Pim.Core/Exceptions", "type": "depends_on" }
  ]
}
```
