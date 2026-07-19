# 日程可靠性修复与统一 Event 模型设计

## 1. 背景与目标

### 1.1 现状

PIM 日程系统已具备基础功能：日历管理、事件 CRUD、ICS 导入导出、任务日历规划层、Outlook Graph 同步（含新版轻量同步 Job 和写回服务）。`EventEntity` 已携带部分 Outlook 元数据字段，`OutlookEventMapper` 可映射 Graph DTO 到领域模型。Web 前端有 `EventEditorDialog`、`TaskEditorDialog` 和月视图/周视图/时间轴视图。

### 1.2 已证实故障根因

以下故障均经人工测试确认，本文档直接引用为规范依据，不再重新猜测：

1. **时间偏移显示问题**：事件时间带 UTC offset，直接赋值给 `datetime-local` 输入框后浏览器拒绝显示，详情/编辑框显示占位符。
2. **月视图硬编码折叠**：月视图 `dayMaxEvents={3}` 硬编码，即使容器高度充足也折叠事件。
3. **事件色块异常**：FullCalendar 事件输入设置内联纯色背景，配合紧凑自定义内容形成过大色块。
4. **默认日历空 ID**：多日历时"默认日历"保留空 ID，前端无法识别实际默认日历的 Outlook 绑定，绕过写回流程，后端返回领域错误 02009/HTTP 400。
5. **时区/UTC 不一致**：本地 datetime 字符串反序列化为 `+08:00`，创建/更新未统一为 UTC；Npgsql 8.0.6 拒绝向 `timestamptz` 写入非零 offset 的 `DateTimeOffset`，导致任务创建 HTTP 500。
6. **结束时间校验缺失**：客户端和服务端都没有保证日程/任务结束时间严格晚于开始时间。
7. **预估时长格式不友好**：任务预估时长直接暴露 ISO 8601 持续时间格式，人工输入不清晰。
8. **HTML 描述暴露源码**：Outlook HTML 描述被直接作为纯文本显示，暴露 HTML 标签。
9. **Graph 同步字段遗漏**：`ExternalMetadataJson` 已存在于实体中但未被 Graph 同步填充，大量 Graph 字段未映射。

### 1.3 目标

- PR1：修复上述人工测试故障，统一 UTC 规范化，改进任务时长输入和日历 UI 可靠性。不新增 Event 字段、不扩展 Graph 字段映射。
- PR2：建立统一 Event 领域模型——只有一套 Event 类型、编辑器和 API。Outlook 日程 = 原生 Event 数据 + Outlook 来源/绑定/远端同步元数据，不得存在两套日程系统。新增原生可编辑字段、扩展 Graph 映射、完整接入富文本编辑器、`ExternalMetadataJson` 填充。
- PR3：重复日程主模型——系列主事件 + 规范重复规则作为权威数据，occurrence 按规则生成，例外/取消独立持久化，并提供从当前展开模型迁移的策略。

### 1.4 非目标（贯穿三阶段）

- 不把 Outlook Event 建成第二套领域模型。
- 不删除已有 EventEntity 列（仅新增或调整写入规范）。
- 不实现本地通知调度（属于后续独立子系统）。
- 不自动复制/上传 Outlook 附件二进制文件到 PIM（附件引用存储是 PR2 统一能力，二进制同步延后）。
- 不引入实时 coauthoring 或 WebSocket 推送。
- 不实现跨 Outlook 日历移动事件的 PIM 操作。
- 不改写已存在的旧 `OutlookSyncService` 运行注册（新链路已经过轻量设计独立验证，本规范仅调整 Event 层面的一致性）。

## 2. 统一 Event 领域模型

### 2.1 领域边界

**核心信条**：全系统只有一套 `Event` 类型。Outlook 来源事件、原生日程事件、ICS 导入事件共享同一领域模型 `EventEntity`。差异仅体现在：
- `Source` 字段（`"manual"` / `"outlook"` / `"outlook-ics"`）
- `OutlookCalendarBindingId` 等外部身份字段
- 写回路径（Outlook 来源走 `OutlookEventWriteService`，原生日程直接更新数据库）

**架构层级**：

```
EventEntity (DB / 领域模型)
    ↑↓
EventResponse / CreateEventRequest / UpdateEventRequest (API DTO)
    ↑↓
EventEditorDialog (前端编辑器，统一入口)
    ↑↓ 按来源分支
    ├── Source == manual  → CalendarService (直接 DB)
    └── Outlook binding   → OutlookEventWriteService (Graph 写回)
```

**来源/绑定/远端元数据关系**：

```
EventEntity
├── Source: "manual" | "outlook" | "outlook-ics"
├── CalendarId → CalendarEntity
│   ├── Source: "manual"
│   └── OutlookCalendarBindingId → OutlookCalendarBindingEntity
│       ├── ConnectionId → OutlookConnectionEntity
│       ├── GraphCalendarId
│       ├── CanEdit
│       └── RemoteState
├── OutlookEventId (Graph immutable ID)
├── OutlookConnectionId
├── OutlookCalendarBindingId
├── OutlookChangeKey / OutlookEtag
├── OutlookSeriesMasterId / OutlookEventType
├── OriginalStartTimeZone / OriginalEndTimeZone
├── GraphRecurrenceJson
└── ExternalMetadataJson ← 无损保留未知/扩展字段
```

### 2.2 字段分类表

#### 原生可编辑字段（所有 Event 共有，PR2 新增字段均为可选 nullable）

| 字段 | 类型 | 对应 Prop | 新增阶段 | 说明 |
|------|------|-----------|----------|------|
| Id | Guid | id | 已有 | 系统内部标识 |
| CalendarId | Guid | calendarId | 已有 | 所属 PIM 日历 |
| Uid | string | uid | 已有 | 日历唯一标识符 |
| Title | string(255) | title | 已有 | 标题，必填 |
| Description | string? | description | 已有 | 富文本/纯文本内容 |
| DescriptionFormat | string? | descriptionFormat | PR2 | `"html"` / `"plain"`；Outlook `body.contentType` 映射到该字段 |
| Location | string(500)? | location | 已有 | 地点 |
| DtStart | DateTimeOffset (UTC) | dtStart | 已有 | 开始时间，规范化到 UTC |
| DtEnd | DateTimeOffset (UTC) | dtEnd | 已有 | 结束时间，规范化到 UTC |
| IsAllDay | bool | isAllDay | 已有 | 全天标记 |
| AllDayStartDate | DateOnly? | allDayStartDate | 已有 | 全天开始日期，不参与 UTC 偏移 |
| AllDayEndDateExclusive | DateOnly? | allDayEndDateExclusive | 已有 | 全天排他结束日期，不参与 UTC 偏移 |
| TimeZoneId | string(100)? | timeZoneId | 已有 | 事件的规范时区 ID；Graph 原始 Windows 时区另存来源元数据 |
| Status | string(20) | status | 已有 | `"CONFIRMED"` / `"TENTATIVE"` / `"CANCELLED"` |
| ShowAs | string(20)? | showAs | PR2 | `"free"` / `"tentative"` / `"busy"` / `"oof"` / `"workingElsewhere"` |
| Importance | string(20)? | importance | PR2 | `"low"` / `"normal"` / `"high"` |
| Sensitivity | string(20)? | sensitivity | PR2 | `"normal"` / `"personal"` / `"private"` / `"confidential"` |
| Categories | string[] | categories | PR2 | jsonb 存数组 |
| IsReminderOn | bool | isReminderOn | PR2 | 提醒开关 |
| ReminderMinutesBeforeStart | int? | reminderMinutesBeforeStart | PR2 | 提醒提前分钟数；isReminderOn=false 时可为 null |
| Organizer | string (jsonb) | organizer | PR2 结构化迁移 | 目标为结构化组织者对象：`{ "name", "email" }`；当前 legacy string 通过扩展列、回填、切换的兼容迁移升级，不原地破坏存量值 |
| Attendees | string (jsonb) | attendees | PR2 | 结构化参会者列表 JSON：`[{ "name", "email", "type": "required"\|"optional"\|"resource" }]` |
| IsOnlineMeeting | bool | isOnlineMeeting | PR2 | 是否为在线会议 |
| OnlineMeetingProvider | string(50)? | onlineMeetingProvider | PR2 | `"teams"` / `"zoom"` / `"meet"` / `"other"` |
| OnlineMeetingUrl | string? | onlineMeetingUrl | PR2 | 会议加入链接 |
| ExternalLink | string? | externalLink | PR2 | 关联的外部 URL（如 Outlook Web 链接） |
| AttachmentReferences | string (jsonb) | attachmentReferences | PR2 | 统一附件引用；原生日程关联 PIM 文件 ID，Outlook 保留附件 ID、名称、类型、大小和可下载能力 |
| RRule | string? | rrule | 已有 | 重复规则（RFC 5545） |
| ExDatesJson | string (jsonb) | exDatesJson | 已有 | 排除日期 |
| RecurrenceMetadataJson | string (jsonb) | recurrenceMetadataJson | 已有，PR3 规范化 | 重复规则辅助元数据；系列身份使用独立列，不在 JSON 中重复存储 |

所有 PR2/PR3 新增列在对应迁移中添加，默认 null 或 false。Organizer 使用兼容扩展/回填/切换迁移；任何 schema 变更都不得破坏已有数据。

#### 来源元数据字段（自动填充，不可手动编辑）

| 字段 | 用途 |
|------|------|
| Source | `"manual"` / `"outlook"` / `"outlook-ics"` |
| OutlookEventId | Graph immutable event ID |
| OutlookConnectionId | 所属 Outlook 连接 FK |
| OutlookCalendarBindingId | Outlook 日历绑定 FK |
| OutlookSeriesMasterId | 所属系列主事件 ID |
| OutlookEventType | `"seriesMaster"` / `"occurrence"` / `"exception"` / `"singleInstance"` |
| OriginalStartTimeZone | Graph 返回的原始开始时区 |
| OriginalEndTimeZone | Graph 返回的原始结束时区 |
| OutlookChangeKey | Graph changeKey（同步元数据） |
| OutlookEtag | HTTP ETag（条件写回） |
| GraphRecurrenceJson | 原始 Graph recurrence JSON |
| SourceUid | iCalUId |
| SourceTimeZoneId | 导入来源的原始时区 ID |
| LastSeenSyncGeneration | 最后同步批次 ID |
| OutlookSyncState | 同步状态 |

#### Outlook 保留但前端只读的字段（不做原生可编辑语义）

以下字段来自 Graph，PIM 存储并在"Outlook 附加信息"区域展示，不作为原生 Event 的可编辑属性：

- `ResponseRequested`
- `AllowNewTimeProposals`
- `HideAttendees`
- 所有 Graph extended properties / open extensions

#### 后续阶段能力（PR2 存储结构到位，功能延后）

- `AttachmentReferences`：PR2 统一附件引用存储和编辑入口就绪。原生日程可关联 PIM 文件 ID；Outlook 保留稳定附件 ID、名称、类型、大小和由服务端代理的下载入口，不把短期签名 URL 作为权威数据。自动复制/上传二进制文件延后。
- `SchedulePlanId`：AI 排程计划连接。已在实体中。

#### 原生与 Outlook 一致性原则

| 方面 | 规则 |
|------|------|
| 字段定义 | 所有原生可编辑字段在 `CreateEventRequest` / `UpdateEventRequest` 中均有对应属性 |
| 校验规则 | 结束 > 开始、标题非空等校验对全部 Event 一致执行 |
| 时区处理 | 所有 DtStart/DtEnd 在写入 `timestamptz` 前规范化到 UTC，无论来源 |
| 编辑器 | 同一 `EventEditorDialog` 处理所有 Event，仅 Outlook 来源增加写回差异确认步骤 |
| API 路径 | `POST/GET/PUT /calendar/events` 是统一端点；Outlook 来源的写操作路由到 `OutlookEventWriteService` |
| DTO/存储 | manual、Outlook、ICS 等来源共用同一 CreateEventRequest/UpdateEventRequest/EventResponse DTO 和 EventEntity 存储 |

Provider 能力只影响某个字段在当前来源上是否可写，不影响字段是否属于统一 Event。例如 Graph 不允许修改的 organizer 仍使用同一个 Organizer 字段和编辑器位置，但控件只读并说明来源限制；其值继续参与展示、审计和无损保留，不建立 Outlook 专用替代字段。

### 2.3 隐私与敏感事件脱敏

- `importance` 支持排序、筛选和轻量视觉强调（高重要性事件加左侧强调色条加粗）。
- `sensitivity` 为 `private` 或 `confidential` 时，在非所有者私密上下文或未明确授权的信任上下文中：
  - 标题替换为"私人事件"。
  - 描述、地点、参会者、会议链接和附件均隐藏。
  - 时间块仍显示（仅起止时间）。
- `free` 语义：时间块标记为空闲，不占用排程可用性计算。
- `tentative` 语义：软占用，排程算法可建议替代时间但需用户确认。

## 3. PR1：UI 可靠性修复

### 3.1 时间格式转换

**根因**：事件时间 `DateTimeOffset` 被序列化为 ISO 8601 带 offset 字符串（如 `2026-07-20T14:00:00+08:00`），直接赋值到 `<input type="datetime-local">` 后浏览器拒绝渲染。

**修复规则**：
- API/DTO `EventResponse.dtStart` / `dtEnd` 继续使用 `DateTimeOffset` 序列化为 ISO 8601（如 `2026-07-20T06:00:00Z` 或 `2026-07-20T14:00:00+08:00`）。**不得返回无 offset 的裸本地时间字符串**。
- 前端使用共享工具函数 `isoToDatetimeLocal(iso: string, timeZoneId?: string): string` 将 UTC/带 offset 的 ISO 字符串转换为 `datetime-local` 控件可接受的无 offset 字符串。优先使用事件 `TimeZoneId`，缺失时才使用用户偏好时区，不能硬编码 `Asia/Shanghai`。
- 提交时使用共享工具函数 `datetimeLocalToIso(local: string, timeZoneId?: string): string` 转换为带明确 offset 的 ISO 字符串或 UTC。
- `TimeZoneId` 已存在于当前 Event DTO。PR1 复用已有值完成转换，不新增时区选择控件；PR2 再把时区作为统一编辑字段完善。历史缺失时区使用用户偏好时区。
- `CalendarPage` 的 FullCalendar `timeZone` 改为用户偏好时区（或浏览器 local 配置），移除当前硬编码 `Asia/Shanghai`。日历总览按用户选择的查看时区渲染；单个事件 `TimeZoneId` 用于编辑墙上时间和往返转换。
- 后端 `CalendarService.CreateEventAsync` / `UpdateEventAsync` 在持久化前将 DtStart/DtEnd 规范化到 UTC（`.ToUniversalTime()`）。

**验证标准**：
- 创建带 offset 的日程，详情和编辑框显示正确时间，不显示占位符。
- 编辑保存后重新打开，时间不变。
- API 响应的 dtStart/dtEnd 始终是合法 ISO 8601（带 offset 或 Z），从不返回裸 `"2026-07-20T14:00:00"`。

### 3.2 月视图动态折叠

**根因**：月视图 `dayMaxEvents={3}` 硬编码。

**修复规则**：
- 移除固定 `dayMaxEvents`，使用 FullCalendar 原生 `dayMaxEvents={true}` 让 FullCalendar 根据单元格可用高度自动折叠。
- 使用 FullCalendar 原生的 more 链接和 Popover（`dayPopoverFormat` 控制标题格式），不自行实现 Popover。
- 容器 resize 时 FullCalendar 自动重算（`window.resize` 触发 calendar render，不需要手算 slotHeight）。

**验证标准**：
- 月视图在屏幕空间足够时显示全部事件条目，不进行不必要的折叠。
- 屏幕空间不足时自动折叠，显示 "+N more" 链接。
- 点击 "+N more" 展开 FullCalendar 原生 Popover 显示全部事件列表。
- 改变窗口大小时自动重新计算。

### 3.3 时间轴事件视觉

**根因**：FullCalendar event 设内联 `backgroundColor` 全填充，自定义紧凑内容配合高饱和色形成异常色块。

**修复规则**：
- 时间轴事件渲染改为：**浅色持续时间块 + 左侧 3px 强调线**。避免整块高饱和纯色。
- 背景色使用日历主题色 15% 透明度（`<color>26` hex），强调线使用日历主题色全不透明。不设置内联 `backgroundColor`，仅在 `eventContent` 自定义 DOM 中应用样式。
- 紧凑自定义内容区域使用 8px 内边距，字体 12px，行高 1.3。

**时间轴信息优先级**（空间不足时从最低优先级向上隐藏）：

1. **标题 + 时间**（始终显示）
2. 地点
3. 日历名称 / 来源标签
4. 描述摘要（最多一行，截断 60 字符 + "…"）
5. 重复/提醒/重要性状态图标

当 FullCalendar 事件高度 >= 32px 时从第 2 优先级开始逐层叠加；< 32px 只显示第 1 优先级。

**验证标准**：
- 时间轴事件显示为浅色块 + 左侧彩色强调线。
- 不同优先级在充足空间下完整显示。
- 空间压缩时按优先级顺序隐藏内容。

### 3.4 默认日历解析

**根因**：多日历时前端缓存"默认日历"空 ID，无法关联实际默认日历的 Outlook 绑定，写回时绕过了 Outlook 校验，后端返回 02009 领域错误。

**修复规则**：
- 前端 `CalendarDataManager` / `CalendarPage` 在加载日历列表后，将 `IsDefault` 日历的 `Id` 解析为稳定 GUID，不再保留空 ID。
- 事件编辑器 `EventEditorDialog` 初始化时优先使用传入的 `calendarId`，若无则使用解析后的默认日历 ID。
- **无默认日历标记时的 fallback 策略**：使用确定性 fallback——按 API 顺序选择首个 `CanEdit=true` 且可见的日历。若没有任何可写日历，禁用保存按钮并提示"没有可用的可写日历，请先在设置中添加或启用日历"。
- fallback 只负责前端选择；保存请求必须始终发送该显式非空 `CalendarId`，服务端不得在同一次创建中再选择另一个默认日历。后端的默认日历创建/查询逻辑继续用于日历初始化，不替代事件请求中的明确选择。
- Outlook 绑定日历的选择器在下拉中明确标注 `(Outlook)` 后缀，默认选中默认日历（若默认日历有 Outlook 绑定则自动使用写回路径）。
- 后端 `CalendarService` 在日历 ID 为空时拒绝请求并返回明确错误消息（现有行为正确，仅前端需修复）。

**验证标准**：
- 存在多个 PIM 日历（含 Outlook 绑定）时，新建事件默认选中正确的默认日历。
- 默认日历有 Outlook 绑定时，写回路径正确触发。
- 无默认日历标记时使用首个可写可见日历，若无可写日历则禁用保存并提示。

### 3.5 UTC 规范化

**根因**：客户端传 `+08:00` 的 `DateTimeOffset`，Npgsql 8.0.6 拒绝向 `timestamptz` 写入非零 offset。

**修复规则**：
- `CalendarService.CreateEventAsync` 第 1 步：`request.DtStart.ToUniversalTime()` / `request.DtEnd.ToUniversalTime()`。
- `CalendarService.UpdateEventAsync` 同理。
- `OutlookEventMapper` Graph → 实体映射时：把 Graph 日期时间与其时区语义解析成 `DateTimeOffset`，再调用 `.ToUniversalTime()`；禁止用 `DateTime.SpecifyKind` 给未转换的墙上时间强行贴 UTC 标签。
- 任务创建/更新/排程等所有写 `DateTimeOffset` 到 `timestamptz` 的路径统一 UTC 规范化。
- 前端任务编辑器中的 `EstimatedDuration` 不再直接显示 ISO 8601，改为两个数字输入框（见 3.6）。

**验证标准**：
- 通过 API 创建 `+08:00` 事件，数据库存储为 UTC。
- 通过 Web 编辑保存，不触发 Npgsql 8.0.6 `timestamptz` 异常。
- 创建任务不因时间 offset 产生 HTTP 500。

### 3.6 任务时长双输入

**根因**：`EstimatedDuration` 暴露 ISO 8601 格式（如 `PT1H30M`），人工输入不友好。

**修复规则**：
- `EstimatedDuration` 属于 `CreateTaskRequest` / `UpdateTaskRequest` / `TaskResponse` 和 `TaskEditorDialog.tsx`。**不属于 Event 相关 DTO 或 `EventEditorDialog`**。
- 任务 API 路由是 `/api/v1/calendar/tasks`。
- 前端 `TaskEditorDialog` 中 `EstimatedDuration` 的输入控件改为两个数字输入框：
  - **时**：非负整数，默认 0。
  - **分钟**：整数 0-59，默认 30。
- 至少有一项 > 0，否则显示错误"请至少设置 1 分钟"。
- 保存时前端组合为 ISO 8601 格式（如 `PT1H30M`）发送给 API。
- API 继续使用 ISO 8601 字符串格式（`TaskResponse.EstimatedDuration`），仅前端展示/编辑层做格式转换。
- 后端 `CalendarService` 验证 `EstimatedDuration` 是否符合 ISO 8601 且正值（总时长至少 1 分钟）。

**验证标准**：
- 任务编辑器中时长显示为"时"和"分钟"两个输入框。
- 修改后保存、刷新，时长值保持一致。
- 输入"0 小时 0 分钟"时拒绝保存并显示客户端错误。

### 3.7 时间校验

**客户端即时校验**：
- `EventEditorDialog` 中 `dtEnd` 选项的 min 值动态设为 `dtStart` 的当前值 + 1 分钟。
- 提交前：`DtEnd <= DtStart` 时阻止并显示"结束时间必须晚于开始时间"。
- `TaskEditorDialog`：仅在 `DtStart` 与 `PlannedEnd` 同时存在时校验 `PlannedEnd > DtStart`。无 `PlannedEnd` 或 `DtStart` 时跳过此校验。

**服务端权威校验**：
- `CalendarService.CreateEventAsync` 中：`if (request.DtEnd <= request.DtStart)` → 返回 `4xx` 领域错误（错误码 `02010`），消息"结束时间必须晚于开始时间"。
- `CalendarService.UpdateEventAsync` 同样校验。
- `CreateTaskRequest` / `UpdateTaskRequest`：仅在 `PlannedEnd` 与 `DtStart` 同时存在时校验 `PlannedEnd > DtStart`。`PlanTaskRequest` 使用其真实字段校验 `PlannedEnd > PlannedStart`。不满足时返回 `400` + `02010`。
- 以上所有路径在写入前统一 UTC 规范化。
- 服务端始终返回 `400 Bad Request` 而非 `500 Internal Server Error`。

**验证标准**：
- 提交结束 <= 开始的事件时，客户端立即显示错误。
- 通过 API 直接提交（绕开前端校验）时，服务端返回 400 和可理解的错误消息。
- 后端日志记录该错误码，不记录堆栈。
- 任务校验不影响无 PlannedEnd 的简单任务创建。

### 3.8 HTML 描述安全

**规则（PR1 范围）**：
- 前端 `EventEditorDialog` / `TaskEditorDialog` 在展示现有描述时：若内容包含 HTML 标签，使用 `DOMPurify.sanitize()` 安全清洗后渲染。若为纯文本，直接文本显示（不解析标签）。
- PR1 **不新增 `DescriptionFormat` 数据列**，不更改存储格式。服务端描述字段保持原样。
- PR1 对现有描述采用最佳推测渲染：检测是否包含 HTML 标签，按安全方式渲染。
- `DOMPurify` 配置禁止 `<script>`、`<iframe>`、`<object>`、`<embed>`、`on*` 事件属性。
- Outlook HTML 描述在 PR1 显示为安全的只读渲染预览，不把源码塞入纯文本输入框，也不在缺少格式感知编辑器时改写原始内容；原生纯文本描述继续使用现有文本编辑。
- PR1 服务端把手工 CRUD 的描述按纯文本语义处理，并拒绝 `<script>`、`<iframe>`、`<object>`、`<embed>` 和 `on*` 属性等可执行构造；Outlook 同步原始 HTML 不走手工 CRUD 过滤链，保持原值并只通过清洗后的预览展示。
- 阻止危险 HTML 注入，但不改变编辑器为富文本编辑器（富文本编辑器属于 PR2）。

**PR2 范围**（此处提前说明以确保一致性）：
- PR2 新增 `DescriptionFormat` 列，编辑器替换为安全富文本编辑器（Quill 或 Tiptap）。
- 同步来的原始 Outlook `body.contentType` / `body.content` 在受保护的来源元数据中无损保留。普通 UI 不显示原始 HTML 源码。
- 用户编辑后只写回安全规范化 HTML。原始提供方正文（`ExternalMetadataJson` 或专用元数据字段）在保护区域内保留，不用于默认渲染。

**验证标准（PR1）**：
- Outlook 同步的 HTML 描述显示为格式化文本，不暴露 `<div>`、`<span>` 等标签。
- 包含 `<script>` 的恶意描述不会执行脚本（`DOMPurify` 过滤）。
- 原生日程纯文本描述显示不变。
- 编辑器不产生危险 HTML。

## 4. API 契约

### 4.1 事件端点

保留现有路由前缀 `/api/v1/calendar/events`。

| 方法 | 路径 | 用途 | 变更 |
|------|------|------|------|
| `GET` | `/` | 按日历/时间查询 | 无变化 |
| `GET` | `/{id}` | 单个事件详情 | PR2 返回新增字段 |
| `POST` | `/` | 创建事件 | UTC 规范化；结束时间校验 |
| `PUT` | `/{id}` | 更新事件 | UTC 规范化；结束时间校验 |
| `DELETE` | `/{id}` | 软删除 | 无变化 |

**PR2 完成后的目标契约**：以下请求体不是 PR1 契约；PR1 不发送这些新增字段。

**`POST /` 请求体（PR2 扩展后）**：

```json
{
  "calendarId": "guid",
  "title": "string",
  "description": "string?",
  "descriptionFormat": "html|plain?",
  "location": "string?",
  "dtStart": "2026-07-20T06:00:00Z",
  "dtEnd": "2026-07-20T07:00:00Z",
  "isAllDay": false,
  "allDayStartDate": "2026-07-20?",
  "allDayEndDateExclusive": "2026-07-21?",
  "timeZoneId": "Asia/Shanghai?",
  "rrule": "string?",
  "status": "CONFIRMED?",
  "showAs": "busy?",
  "importance": "normal?",
  "sensitivity": "normal?",
  "categories": ["string"]?,
  "isReminderOn": false,
  "reminderMinutesBeforeStart": 15?,
  "organizer": { "name": "string", "email": "string" }?,
  "attendees": [{ "name": "string", "email": "string", "type": "required|optional|resource" }]?,
  "isOnlineMeeting": false,
  "onlineMeetingProvider": "teams|zoom|meet|other?",
  "onlineMeetingUrl": "string?",
  "externalLink": "string?",
  "attachmentReferences": [{ "kind": "pimFile|outlook", "id": "string", "name": "string", "contentType": "string?", "size": 0 }]?
}
```

所有新增字段在 PR2 迁移中变为可选。PR1 只修复现有字段行为，不要求前端发送新字段。

**dtStart/dtEnd 格式**：API 出站 DTO 始终是合法 ISO 8601 `DateTimeOffset`（如 `"2026-07-20T06:00:00Z"` 或 `"2026-07-20T14:00:00+08:00"`）。从不返回裸无 offset 本地时间字符串。

### 4.2 任务端点

| 方法 | 路径 | 用途 | 变更 |
|------|------|------|------|
| `POST` | `/api/v1/calendar/tasks` | 创建任务 | `EstimatedDuration` 前端双输入 → API ISO 8601；UTC 规范化 |
| `PUT` | `/api/v1/calendar/tasks/{id}` | 更新任务 | UTC 规范化 |
| `POST` | `/api/v1/calendar/tasks/{id}/plan` | 排程 | 结束时间校验（`PlannedEnd > PlannedStart`） |

### 4.3 错误码补充

| 错误码 | HTTP 状态 | 消息 |
|--------|-----------|------|
| `02010` | 400 | 结束时间必须晚于开始时间 |
| `02011` | 400 | 时长必须为正值 |
| `02012` | 400 | 标题不能为空 |

## 5. 存储与规范化

### 5.1 PostgreSQL

- `timestamptz` 列（`dtstart`、`dtend`、`created_at`、`updated_at`、`deleted_at`、`dtstamp`）只接收 offset 为零的 UTC `DateTimeOffset`。
- `CalendarService` 和 `OutlookEventMapper` 在写入前统一 `.ToUniversalTime()`。
- 新增字段使用 EF Core 迁移逐 PR 添加，不合并迁移。

### 5.2 时区展示规则

| 场景 | 规则 |
|------|------|
| API 返回 | `DateTimeOffset` ISO 8601（带 offset 或 Z） |
| Web 显示 | 优先按事件 `TimeZoneId`，缺失时按用户偏好时区显示本地时间 |
| 全天事件 | 使用 `AllDayStartDate` / `AllDayEndDateExclusive` 的日期语义，不从 UTC 时刻反推日期 |
| 编辑框 | 前端共享工具 `isoToDatetimeLocal` / `datetimeLocalToIso` 完成转换 |
| 历史无时区事件 | 使用用户偏好时区展示，**不能把服务器 `Asia/Shanghai` 当作所有用户的永久语义** |

### 5.3 HTML 安全存储

**PR1**：
- 不新增 `DescriptionFormat` 列。描述字段保持原样存储。
- 渲染时 `DOMPurify.sanitize()` 清洗后展示。服务端对手工 CRUD 拒绝可执行 HTML 构造，但不清洗或重写 Outlook 同步原文。

**PR2**：
- 新增 `DescriptionFormat` 列，持久化格式标记。
- 用户编辑后只写回安全规范化 HTML。
- 同步来的原始 Outlook `body.content` / `body.contentType` 以 `ExternalMetadataJson.sourceSnapshot.body` 保留独立副本；该保留发生在 typed mapping 之前，即使用户以后编辑规范化 Description，也不会覆盖来源快照。
- 普通 UI 不显示原始 HTML 源码。原始数据仅供技术诊断或迁移使用，且需权限控制。

### 5.4 Graph 未知字段保留边界

- PR2 必须在 Graph JSON 反序列化边界保留原始 payload/property bag，再执行 typed DTO mapping。
- 实现方式：在 `GraphCalendarClient` 响应处理层（或 `OutlookEventMapper` 入口）先将完整 Graph JSON 解析为 `JsonDocument` / `Dictionary<string, object?>`，取出已映射字段后剩余全部键值对存入 `ExternalMetadataJson`。
- **允许调整 `GraphCalendarClient`** 的响应投影/反序列化策略以支持原始 payload 保留，但不改变其认证、分页、重试和 HTTP 传输策略。
- Graph extended properties / open extensions（`singleValueExtendedProperties`、`multiValueExtendedProperties`）同样保留在此 JSON 中。
- 这条边界确保 typed DTO mapping 不会丢失 Graph 返回的未知键。
- 标准 `EventResponse` 不直接下发原始 `ExternalMetadataJson`；它只返回经过 allowlist、脱敏和长度限制的 `outlookAdditionalInfo` 摘要。原始 JSON 仅由受权限控制的诊断端点按需返回。

## 6. PR1 交互与视觉细则

### 6.1 事件编辑器

- 保留现有 `EventEditorDialog` 单列布局。
- 标题、全天复选框、开始/结束时间（`datetime-local` 控件，由共享工具进行 ISO ↔ local 转换）、地点、描述（PR1 保持纯文本 + 安全渲染预览模式）。
- 保存按钮文案：原生日程为"保存"，Outlook 来源为"预览写回"（PR2 行为，PR1 仅修复非 Outlook 路径的可靠性）。
- 删除按钮：原生日程直接删除，Outlook 来源确认后删除。

### 6.2 错误反馈

- 所有领域错误（02009、02010 等）在前端显示为中文本地化消息，不暴露错误码给普通用户（错误码隐藏在可展开技术详情中）。
- 网络超时/服务不可用时显示"网络连接异常，请检查后重试"。
- 表单校验错误使用内联红色提示，出现在相应字段下方。

### 6.3 折叠/展开

- 月视图 "+N more" Popover 使用 FullCalendar 原生控件，宽度 320px，最大高度 400px，可滚动。
- 弹出后点击 Popover 外部关闭。
- 窗口 resize 时 FullCalendar 自动重算。

## 7. PR2：统一字段与 Outlook 双向映射

### 7.1 Graph 映射扩展

`OutlookEventMapper` 当前映射的项目保持不变。PR2 新增映射如下：

| Graph 字段 | EventEntity 字段 | 类别 |
|------------|-----------------|------|
| `importance` | Importance | 原生可编辑 |
| `sensitivity` | Sensitivity | 原生可编辑 |
| `categories` | Categories (jsonb) | 原生可编辑 |
| `showAs` | ShowAs | 原生可编辑 |
| `isReminderOn` | IsReminderOn | 原生可编辑。`isReminderOn=false` 映射为 `IsReminderOn=false` + `ReminderMinutesBeforeStart=null`。两个字段独立存储。开关语义不丢失。 |
| `reminderMinutesBeforeStart` | ReminderMinutesBeforeStart | 原生可编辑 |
| `organizer.emailAddress` | Organizer | 原生可编辑 |
| `attendees` | Attendees (JSON) | 原生可编辑 |
| `isOnlineMeeting` | IsOnlineMeeting | 原生可编辑 |
| `onlineMeetingProvider` | OnlineMeetingProvider | 原生可编辑 |
| `onlineMeetingUrl` / `joinUrl` | OnlineMeetingUrl | 原生可编辑 |
| `webLink` | ExternalLink | 原生可编辑（Outlook Web 链接） |
| `body.contentType` | DescriptionFormat | 原生格式字段；原始 `body.content` / `contentType` 同时复制到 `ExternalMetadataJson.sourceSnapshot.body` 无损保留 |
| `responseRequested` | ExternalMetadataJson | Outlook 只读 |
| `allowNewTimeProposals` | ExternalMetadataJson | Outlook 只读 |
| `hideAttendees` | ExternalMetadataJson | Outlook 只读 |

### 7.2 未知字段无损保留

- `ExternalMetadataJson`（jsonb 列）保存 Graph 响应中所有未映射到实体字段的键值对，并在保留区 `sourceSnapshot` 存放需要精确往返的已映射来源值（包括原始 `body.content` / `contentType`）。
- 映射流程：
  1. Graph 响应 JSON 到达 `GraphCalendarClient` 或 `OutlookEventMapper` 入口层。
  2. 反序列化为 `JsonDocument` 或 `Dictionary<string, object?>` 保留完整属性包。
  3. 执行 typed DTO mapping 提取已知字段。
  4. 从原始属性包中移除已映射键，剩余全部存入 `ExternalMetadataJson`。
- 此机制确保 typed mapping 不丢失未知键。**如果修改 GraphCalendarClient 响应投影/反序列化策略对于实现此流程是必要的，则允许修改**，但不改变认证、分页、重试和 HTTP 传输策略。
- 回写时 `ExternalMetadataJson` 不参与写回 payload，Graph 只发送已映射的原生字段。
- PR2 上线前已被旧映射丢弃的字段不能从本地自动恢复；PR2 必须通过重新同步 Graph 对仍存在的远端事件执行回填，此后新旧已回填事件都按无损规则保存。

### 7.3 写回差异确认

**Outlook 事件编辑流程（PR2）**：

1. 编辑器加载事件，显示当前值。
2. 用户修改后点击"预览写回"。
3. 前端显示 before/after 差异对比（高亮修改字段、新增字段、删除字段）。
4. 差异面板包含：操作类型（修改/删除/取消）、目标日历账号、日历名称。
5. 用户点击"确认写回"后，调用 `OutlookEventWriteService` 执行 Graph PATCH。
6. 成功 → 更新本地 `EventEntity` 并写入审计。
7. `412 Precondition Failed`（ETag 冲突）→ 显示"远端已变更"、加载最新 Outlook 内容、用户重新编辑。
8. 其他 Graph 失败 → 显示错误消息，保留编辑内容，用户可重试。

**原生日程编辑流程**（无 Outlook 绑定）：
- 直接保存，无差异确认步骤。
- 按钮文案为"保存"。

**差异确认 UI 原则**：
- 差异对比使用左（之前）/ 右（之后）列布局。
- 未变更字段灰显。
- 变更字段高亮（新增绿色、删除红色、修改黄色）。
- 焦点自动定位到变更区域顶部。

### 7.4 "Outlook 附加信息"生成原则

- 仅 Outlook 来源事件显示此区域。
- 内容来源：`ExternalMetadataJson`、`OutlookChangeKey`、`OutlookEtag`、`OriginalStartTimeZone`、`OriginalEndTimeZone`、`OutlookEventId`、`OutlookEventType`、`LastSeenSyncGeneration`。
- 普通 UI **不提供右键查看完整原始 JSON**。只显示经过 allowlist/脱敏的人类可读只读摘要，按逻辑分组展示（如"同步元数据"、"权限"、"扩展属性"）。
- 脱敏规则：隐藏 token、邮件正文、内部标识等敏感值。长字符串截断 200 字符，递归深度 ≤ 3。
- 技术诊断中的原始 JSON 仅在所有者私密上下文或显式授权的受信任诊断上下文可见，且继续遵守脱敏规则；普通管理员身份本身不自动获得完整正文权限。
- 默认折叠为卡片，不自动展开。

### 7.5 编辑器分组布局（PR2）

- 采用单列分组、渐进展开布局。
- **常用基础字段**（默认可见）：标题、描述（富文本编辑器）、地点、开始/结束时间、全天、时区、日历选择。
- **高级**（可展开）：`showAs`、`importance`、`sensitivity`、`categories`、提醒开关和提前分钟数。
- **协作**（可展开）：组织者、参会者列表。
- **会议**（可展开）：在线会议开关、提供方、链接。
- **重复**（可展开）：PR2 显示现有规则的只读人类可读摘要；PR3 在同一位置接入完整编辑控件，普通用户不直接编辑原始 RRule 字符串。
- **Outlook 附加信息**（可展开，仅 Outlook 来源）：只读摘要。

## 8. PR3：重复日程主模型

### 8.1 当前问题

- 重复规则（`RRule`）存在但无系列主事件概念。
- `RecurrenceService.ExpandEvents()` 在查询时即时展开 occurrence，未持久化 occurrence 实例。
- 缺少例外修改和取消实例的稳定持久化机制。
- Outlook 同步还保存 series master、occurrence、exception 行，形成双路径问题：原生日程由 `RecurrenceService` 查询时展开，而 Outlook 同步将 occurrence 保存为独立 `EventEntity` 行。

### 8.2 主模型定义

**权威数据**：
- 系列主事件 = `EventEntity` 其中 `RRule` 非空 + `IsSeriesMaster = true`。
- 规范重复规则 = `RRule` + `ExDatesJson`。
- 普通 occurrence **不持久化为独立 `EventEntity`**，按规则即时生成。

**例外/取消持久化**（最小字段集）：
- `SeriesMasterId`（Guid?）：指向系列主事件。
- `RecurrenceId`（DateTimeOffset?）：原始发生时间（UTC）。
- `IsSeriesMaster`（bool）：标记系列主事件。
- `IsException`（bool）：标记例外或取消实例。
- 规范重复规则（`RRule` + `ExDatesJson`）：仅系列主事件持有。
- 例外 occurrence = `EventEntity` 其中 `SeriesMasterId` 指向主事件 + `RecurrenceId` 为原始发生时间 + `IsException = true`。
- 取消 occurrence = `EventEntity` 同上 + `Status = "CANCELLED"` + `IsException = true`。
- 例外与取消使用同表 `events`，通过 `IsException` 和 `Status` 区分。
- 唯一约束：`(SeriesMasterId, RecurrenceId)` 对例外表生效。

**PR3 迁移必须解决双路径问题**：
- 识别并停止把普通 Outlook occurrence 当权威记录保存。迁移/去重后只保留 master + exception/cancellation。
- 兼容窗口内旧 occurrence 行标记为只读，不用于写回和编辑。
- 迁移脚本幂等可重跑，不删除旧数据。

**outlook 关联**：
- Graph `seriesMasterId` ↔ PIM `SeriesMasterId`（双向）。
- Graph `type=exception` ↔ PIM `IsException=true` + `RecurrenceId` = 原始开始时间。
- 取消的 occurrence 在 Graph 中可能是 `type=exception` + `isCancelled=true`。

### 8.3 Occurrence 生成

- `RecurrenceService.OccurrenceGenerator` 使用 `RRule` + `ExDatesJson` 生成指定时间窗口内的 occurrence 列表。
- 支持规则：不重复、每日、每周、每月、每年、间隔和结束条件。
- 每次生成时先获取该系列的全部例外（`IsException=true, SeriesMasterId=masterId`），用例外实例替换对应 `RecurrenceId` 的生成值。
- 取消实例在列表中标记为 `isCancelled`，前端灰显。
- 无 RRule 的单次事件直接返回自身，不走生成器。
- RDATE 不作为当前必需能力。

### 8.4 编辑规则

| 操作 | 规则 |
|------|------|
| 编辑单个 occurrence | 创建一个例外 `EventEntity`（`IsException=true, SeriesMasterId, RecurrenceId`），修改字段在该例外中存储 |
| 取消单个 occurrence | 创建一个取消例外（`IsException=true, Status=CANCELLED`） |
| 编辑整个系列 | 更新系列主事件字段；例外中未覆盖的字段按主事件新值生成 |
| 删除单个 occurrence | 创建取消例外 |
| 删除整个系列 | 软删除系列主事件 + 关联全部例外 |

### 8.5 重复编辑 UI（PR3）

- 人类可用的重复规则编辑区域：至少支持不重复、每日、每周、每月、每年、间隔和结束条件。
- 编辑器将用户选择转换为规范 RRule 字符串。
- Outlook 绑定事件通过同一编辑器编辑，编辑后经差异确认流程后通过 Graph recurrence mapping 写回。
- 不要限制为"仅 RRule 字符串编辑"或"Graph recurrence 永远不新建/修改"。PR3 应提供规则的图形化编辑并支持写回。

### 8.6 迁移策略

**从当前模型到主模型**：

阶段 A：并列运行（PR3 交付时）
- 新增 `IsSeriesMaster`、`IsException`、`SeriesMasterId`（已有）、`RecurrenceId`（已有）列。
- 已有 RRule 事件自动标记 `IsSeriesMaster=true`。
- 新创建重复事件写入主模型。
- 旧查询路径（`ExpandEvents`）保留并兼容新旧数据。
- 停止把普通 Outlook occurrence 作为权威记录保存。

阶段 B：迁移窗口（PR3 交付后至少一个发布周期）
- 后台迁移脚本：扫描已有 `RRule` 事件，生成反向兼容的 `ExDatesJson`、标记 `IsSeriesMaster`。
- 例外 detection：扫描已有 `OutlookSeriesMasterId` 非空且 `OutlookEventType=exception` 的事件，标记 `IsException=true`。
- 旧普通 occurrence 标记为只读，不参与编辑和写回。
- 新生成路径启用后，普通查询排除这些 legacy occurrence 行，避免与按主规则生成的实例重复显示；受权限控制的迁移诊断仍可查询旧行。

阶段 C：只读旧格式（下一大版本）
- 旧格式数据标记为 `legacy-recurrence`。
- 编辑器禁止修改旧格式重复事件，提示用户转换为新模型。
- 不删除旧数据。

**回滚**：
- 阶段 A 完全可回滚：删除新增列即可。
- 阶段 B 迁移脚本幂等可重跑。
- 阶段 C 不自动转换，保留原数据。

### 8.7 已知限制

- PR3 不实现 `RDATE` 手动添加。
- 不实现基本模式之外的高级规则，例如工作日集合、每月倒数工作日和自定义 RDATE 集合。
- 不实现例外只修改部分字段（例外存储完整覆盖快照）。

## 9. 测试策略

### 9.1 PR1 测试

**单元测试**：
- `CalendarService` 创建/更新事件时 UTC 规范化验证。
- `CalendarService` 结束时间校验（02010 错误码）。
- 任务创建/排程时 `DateTimeOffset` UTC 规范化。
- 前端 `isoToDatetimeLocal` / `datetimeLocalToIso` 工具函数测试（时区无关、边界情况）。

**UI/集成测试**：
- 月视图 `dayMaxEvents={true}` 验证（空间足够显示全部、空间不足折叠）。
- 时间轴事件按优先级渲染验证。
- 事件编辑器保存后时间值不丢失。
- `datetime-local` 控件正常显示无占位符。
- 默认日历 fallback 选择逻辑验证。
- 任务时长双输入验证（0 小时 0 分钟拒绝保存）。

**API 测试**：
- `POST /api/v1/calendar/events` 传 `+08:00` → DB 读回为 UTC。
- `POST /api/v1/calendar/events` 传 `DtEnd <= DtStart` → `400 02010`。
- `POST /api/v1/calendar/tasks` 传 `+08:00` → DB 读回为 UTC。
- `POST /api/v1/calendar/tasks/{id}/plan` 传 `PlannedEnd <= PlannedStart` → `400 02010`。

### 9.2 PR2 测试

**后端测试**：
- `OutlookEventMapper` 新增字段映射：`importance`、`sensitivity`、`showAs`、`categories`、`isReminderOn`、`reminderMinutesBeforeStart`、`attendees`、`isOnlineMeeting`、`body.contentType`。
- `isReminderOn=false` → DB 中 `IsReminderOn=false` + `ReminderMinutesBeforeStart=null`（不丢失开关语义）。
- 原始属性包在反序列化边界保留，未知字段存入 `ExternalMetadataJson`。
- 写回差异确认流程（preview → confirm → Graph PATCH → DB update → audit）。
- ETag 冲突（412）处理。
- `CanEdit=false` 日历禁止写回。

**前端测试**：
- 编辑器显示所有字段组（基础、高级、协作、会议、重复、Outlook 附加信息）。
- 差异对比 UI 正确渲染 before/after。
- Outlook 附加信息折叠/展开，不显示敏感值。
- `DOMPurify` HTML 安全清洗（含富文本编辑器的完整接入）。

**映射完整性测试**：
- 使用真实 Graph 响应 fixture 验证全量 typed mapping + 全部未知字段进入 `ExternalMetadataJson`。
- 至少一个 fixture 包含 extended properties。

### 9.3 PR3 测试

**后端测试**：
- Occurrence 生成器使用 `RRule` + `ExDatesJson` 生成正确实例列表（不重复、每日、每周、每月、每年、间隔、结束条件）。
- 例外替换：例外实例覆盖生成值。
- 取消实例：`Status=CANCELLED` 在列表中标记。
- 系列编辑 → 主事件更新 + 例外保留。
- 单个 occurrence 编辑 → 创建例外。
- 整个系列删除 → 软删除主 + 全部例外。
- Outlook occurrence 同步不再创建权威记录（仅保留 master + exception/cancellation）。

**迁移测试**：
- 已有 `RRule` 事件正确标记 `IsSeriesMaster`。
- 已有 Outlook exception 事件正确标记 `IsException`。
- 普通 Outlook occurrence 正确标记为只读。
- 迁移脚本幂等。
- 回滚脚本恢复原状。

**前端测试**：
- 重复事件显示"重复"标记。
- 单个 occurrence 上下文菜单显示"编辑此实例" / "编辑系列"。
- 取消实例在日历中灰显。
- 重复规则编辑器正确生成 RRule（不重复/每日/每周/每月/每年）。

### 9.4 跨阶段持续测试

- `dotnet test Pim.sln` 全部后端测试通过。
- `npm --prefix src/client-web run build` Web 构建无错误。
- `npm --prefix src/client-web run lint` 无新增 warning。
- `git diff --check` 无空白错误。

## 10. 交付顺序与风险控制

### 10.1 三阶段总览

| 阶段 | 范围 | 依赖 | 可独立验证 | 可回滚 |
|------|------|------|-----------|--------|
| PR1 | 时间格式、月视图折叠、色块修复、默认日历解析、UTC 规范化、任务时长双输入、时间校验、HTML 安全渲染 | 无 | 是 | 是 |
| PR2 | 统一字段、编辑器分组、Graph 映射扩展 + `ExternalMetadataJson` 填充、富文本编辑器、差异确认、Outlook 附加信息 | 无（新增列均可为空） | 是 | 是 |
| PR3 | 重复主模型、occurrence 生成器、例外/取消持久化、重复规则 UI、迁移脚本 | 无（并列运行） | 是 | 是（阶段 A/B 均可回滚） |

### 10.2 PR1 交付边界

**包含**：
- 前端共享工具 `isoToDatetimeLocal` / `datetimeLocalToIso` 转换。
- 月视图 `dayMaxEvents={true}` 原生折叠。
- 时间轴事件渲染改为浅色背景 + 左侧强调线。
- 默认日历解析（前端 `CalendarDataManager`）：含无默认标记的 fallback 选择。
- `CalendarService` UTC 规范化（事件和任务所有写路径）。
- `TaskEditorDialog` 时长双输入。
- 前后端结束时间校验（事件 `DtEnd > DtStart`；任务仅当 `PlannedEnd` + `DtStart` 同时存在时校验）。
- HTML 描述 `DOMPurify` 安全渲染（不新增 `DescriptionFormat` 列）。

**不包含（明确排除）**：
- 新增 EventEntity 字段（Importance、ShowAs、DescriptionFormat 等）。
- 编辑器分组重构。
- Outlook 差异确认。
- 重复主模型。
- 通知调度。
- 附件引用存储。
- `ExternalMetadataJson` 填充（保留现有字段映射行为）。
- Graph 字段映射扩展。
- 富文本编辑器。

### 10.3 PR2 交付边界

**包含**：
- EventEntity schema 迁移：新增 `IsReminderOn`、`ReminderMinutesBeforeStart`、`Attendees`、`DescriptionFormat`、`AttachmentReferences` 等字段，并对现有 `Organizer` string 执行兼容结构化迁移。
- `CreateEventRequest` / `UpdateEventRequest` / `EventResponse` DTO 扩展。
- `OutlookEventMapper` 全面映射 + 原始属性包保留 + `ExternalMetadataJson` 填充。
- 编辑器统一单列分组布局（基础/高级/协作/会议/重复/Outlook 附加信息）。
- 安全富文本编辑器替换纯文本输入（完整 `DOMPurify` + `DescriptionFormat` 持久化）。
- 差异确认流程（全链路 from preview → confirm → writeback → audit）。
- "Outlook 附加信息"只读区域（allowlist/脱敏摘要）。
- 原始 Graph body/contentType 在受保护来源元数据中无损保留。

**不包含（明确排除）**：
- 重复主模型。
- 本地通知调度。
- 附件二进制同步（`AttachmentReferences` 只做统一引用存储、Outlook 元数据和下载入口）。
- RDATE 支持。

### 10.4 PR3 交付边界

**包含**：
- 新增 `IsSeriesMaster`、`IsException` 列。
- OccurrenceGenerator 支持基本规则（不重复/每日/每周/每月/每年/间隔/结束条件）。
- 例外/取消持久化路径。
- 重复规则编辑器 UI（人类可用，非纯 RRule 字符串）。
- API 系列/实例编辑路由。
- Graph recurrence mapping 写回（通过差异确认流程）。
- 阶段 A 迁移（主模型新增 + 旧数据共存 + 停止普通 occurrence 权威记录）。
- 阶段 B 迁移脚本（扫描 + 标记存量数据 + 旧 occurrence 只读化）。

**不包含（明确排除）**：
- RDATE 手动添加。
- 例外只修改部分字段。
- 旧格式只读化（阶段 C 不在此 PR）。

### 10.5 风险控制

| 风险 | 缓解 |
|------|------|
| 现有数据库迁移冲突 | 每个 PR 独立迁移文件，不合并。PR1 不动实体列，仅修改写入逻辑 |
| 月份视图重算性能 | FullCalendar 原生 `dayMaxEvents={true}` 自动处理，不需要手算，无性能风险 |
| 重复迁移数据丢失 | 阶段 A 并列运行，不删除旧数据；迁移脚本幂等可重跑。旧 occurrence 只读标记不删除 |
| 前端与后端不同步 | 前端先适配新 DTO 格式（忽略未知字段），后端在 PR2 统一发布时开启新字段映射 |
| Npgsql 版本兼容 | 已在 8.0.6 确认问题并修复为所有写入 UTC，不依赖驱动版本变化 |
| Outlook 同步在 PR2 之前产生未映射字段丢失 | PR2 在 Graph JSON 反序列化边界开始无损保留，并安排对仍存在的远端事件重新同步回填；无法再从 Graph 获取的历史字段明确标记为不可恢复，不伪造数据 |

## 11. 当前模块高影响区域

以下列出主要受影响的代码模块/文件（高层定位，非逐行实施计划）：

- `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`：UTC 规范化、结束校验、PR2 新增字段写入门禁。
- `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`：PR2 Graph 映射扩展、原始属性包保留、`ExternalMetadataJson` 填充、`DescriptionFormat` 映射。
- `src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs`：PR3 Occurrence 生成器、例外替换、查询适配。
- `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`：PR2 允许调整响应投影/反序列化以支持原始 payload 保留，不改变认证/分页/重试/HTTP 传输策略。
- `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`：PR2 新增列、PR3 新增列。
- `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`：DTO 扩展、字段分类。
- `src/client-web/src/dialogs/EventEditorDialog.tsx`：PR1 时间格式 + HTML 安全渲染；PR2 分组布局 + 差异确认 + 富文本编辑器 + Outlook 附加信息。
- `src/client-web/src/dialogs/TaskEditorDialog.tsx`：PR1 时长双输入 + 结束时间校验。
- `src/client-web/src/pages/CalendarPage.tsx` + `CalendarView.tsx`：月视图折叠、时间轴事件视觉。
- `src/client-web/src/api/client.ts`：API 调用层适配新 DTO。
- `src/client-web/src/types/index.ts`：TypeScript 类型扩展。
- `src/client-web/src/utils/dateUtils.ts`（PR1 建议新增）：`isoToDatetimeLocal` / `datetimeLocalToIso` 共享工具函数，优先尊重事件时区。

**不修改的文件**：
- `OutlookCalendarSyncService`（PR1/PR2/PR3 均不改变同步调度和批量读取逻辑）。
- 已存在的旧 `OutlookSyncService` / `OutlookTokenService`（不删除，不改写）。

## 12. 字段演进总结

| 阶段 | EventEntity schema 变更 | DTO 变更 |
|------|------------------|----------|
| PR1 | 无（仅修改写入规范和前端展示逻辑） | 无 |
| PR2 | `DescriptionFormat`, `ShowAs`, `Importance`, `Sensitivity`, `Categories` (jsonb), `IsReminderOn`, `ReminderMinutesBeforeStart`, `Organizer` 结构化兼容迁移, `Attendees` (jsonb), `IsOnlineMeeting`, `OnlineMeetingProvider`, `OnlineMeetingUrl`, `ExternalLink`, `AttachmentReferences` (jsonb) | 对应 DTO 字段全部可选 |
| PR3 | `IsSeriesMaster`, `IsException` | 查询参数 `expandOccurrences`，编辑体 `scope: "this" | "series"` |

所有新增列必须 nullable 或有合理默认值，已有行不受影响。

## 13. 隐私与安全约束汇总

- `private` / `confidential` 事件在非所有者私密上下文或未明确授权的信任上下文中脱敏：标题替换、描述/地点/参会者/会议链接/附件隐藏。
- 所有 Graph token、device code 永不返回给前端。
- `ExternalMetadataJson` 在前端只显示 allowlist/脱敏摘要，原始 JSON 需权限控制且隐藏敏感值。
- `DOMPurify` 是前端安全的必要但不充分条件——PR2 服务端也必须拒绝 `<script>` 等危险标签。
- 写入 `timestamptz` 的值必须全部为 UTC，不信任客户端的时区声明（客户端时区仅用于前端展示）。
- 审计日志记录 event ID 和字段变化摘要，不记录完整请求体和 token。
