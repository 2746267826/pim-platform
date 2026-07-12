# Microsoft 日历同步轻量设计

## 最终目标

不删减本轮确认的用户功能，实现适合个人项目，并最大化复用已完成工作。本文取代 `2026-07-10` 旧设计作为后续实施计划的权威输入，但保留旧文档作历史参照；旧设计中用户明确排除的可选功能不再进入实施范围。

## 目标与原则

- 每个 PIM 用户只连接一个 Microsoft 账号，一个账号绑定任意数量日历。
- 普通用户只填写 Client ID；tenant 固定 `common`，scopes 固定 `Calendars.ReadWrite` + `User.Read`。
- 认证使用 MSAL Device Code Flow，加密持久化 MSAL cache，静默续期。
- Outlook -> PIM 的新增/修改/移动/删除自动应用，不需要确认。
- PIM -> Outlook 新建/修改/删除在事件编辑器内预览后直接调用 Graph，不经过确认中心或后台写回队列。
- 不删除已实现的基础设施；新链路仅新增 GraphCalendarClient、OutlookCalendarSyncService（新版）、OutlookEventWriteService、OutlookCalendarSyncJob、OutlookEventMapper。
- 保留旧列与旧服务代码以避免无收益迁移；新运行链路不得依赖旧 token/delta/outbox/conflict 状态。

## 功能范围表

| 功能 | 范围 |
|------|------|
| Entra 分步引导 | 应用注册、个人+组织账号类型、公共客户端流、Graph 委托权限 |
| Device Code 授权 | MSAL Device Code；显示网址/授权码/复制/打开/倒计时/取消；前端自动轮询 |
| 日历发现 | 分页 calendarGroups、组内 calendars、根 calendars；去重；覆盖默认/课程表/未分组；每个远端日历映射独立 PIM 日历 |
| 日历选择 | 首次默认全选；按组或单个暂停/恢复；暂停只停止同步并隐藏，不删除 |
| 只读日历 | 可同步/查看但不可写回；不提供"复制为 PIM 日程" |
| 远端日历消失 | 保留本地数据和 binding，标记 remote-missing，不静默删除 |
| 普通同步 | 启动、Hangfire 每 5 分钟、手动触发；统一 `/me/calendars/{id}/calendarView`；窗口 -90/+365 |
| 深度同步 | full-resources（`/events`，只 upsert）；range-instances（用户指定范围 calendarView）；均分页去重、显示进度、可取消 |
| 事件删除推断 | 逐页成功即幂等 upsert；全部页成功后才 missing verification；对窗口内缺失去向先 GET 单事件，404 才软删 |
| 部分失败与重试 | 单个日历失败不阻断其他日历；失败日历可人工重试并生成关联原批次的新批次 |
| 写回（新建） | 事件编辑器识别 Outlook 来源 → 预览 before/after/账号/日历 → 用户确认后直接 Graph POST（transactionId） |
| 写回（修改/删除） | 同上流程，使用 If-Match；412 时停止覆盖，显示最新 Outlook 内容供用户重新编辑；不做透明重试 |
| 重复日程 | 可读取/展示/修改实例或系列的普通字段；删除实例或系列；不新建/修改 recurrence 规则 |
| 跨日历移动 | PIM 中不支持一次操作跨 Outlook 日历移动；用户在目标日历新建后到原日历删除 |
| 同步历史 | 永久保留；记录批次摘要、逐日历步骤、created/updated/deleted/conflict/failed 事件 ID+标题；不保存正文/token/完整 payload |
| 用户状态 | 未配置/等待授权/已连接/需重连/失败；设置页显示账号、已选日历、同步状态、最近错误 |
| 检查连接 | MSAL silent token → `/me` → 日历列表权限；不建设遥测平台 |
| 暂停/断开 | 默认保留数据；另有"移除本地 Microsoft 数据"危险操作，软删除本地 bindings/calendars/events |
| 安全 | token cache Data Protection 加密；用户隔离；nextLink/Graph URL 白名单；不记录 access/refresh token/cache/Authorization/device code |
| 历史复用 | 复用 `outlook_sync_batches` 表；PerCalendarJson/StepsJson/ErrorsJson 存结构化详情 |
| 连接级锁 | 轻量锁、逐日历顺序执行；自动任务遇到运行中批次则跳过；重启旧 running 批次标 interrupted 并新建批次 |
| 旧数据兼容 | 仅按可靠 Graph ID 或 iCalUId 重绑；无法唯一判断的数据保持可见并标 legacy-unbound |

## 当前状态与复用边界

以下资产**已完成且有专项测试证据**，直接复用，不为减行数而重写：

- **持久化模型**：`OutlookConnectionEntity`、`OutlookAuthorizationSessionEntity`、`OutlookCalendarBindingEntity`、`OutlookSyncBatchEntity`、`OutlookOperationExecutionEntity`，以及 `CalendarEntity`、`EventEntity`、`SyncConflictEntity` 的 Microsoft 同步字段、EF 配置和两条 Microsoft 迁移。
- **认证基础**：`OutlookTokenCacheLock`、`OutlookTokenCacheStore`、`MsalPublicClientAdapter`、`MsalOutlookAuthCoordinator`、`OutlookAuthorizationSessionRunner`。
- **直接复用的专项测试**：`OutlookPersistenceModelTests`、`OutlookMsalAuthenticationTests`、`OutlookAuthorizationSessionTests` 和相关 migration target-model 测试。旧 Graph/delta/writeback/conflict 测试描述的是旧运行路径，只复用测试场景，不把它们视为新链路已完成证据。
- **旧结构保留**：旧 access/refresh token、token expiry、connection delta、subscription、outbox 和 conflict 字段或表仍在数据库中。新链路不依赖它们；`OutlookOperationExecutionEntity` 和 `SyncConflictEntity` 保留但不参与新写回流程。

以下**是已完成资产但标记为旧路径**，端到端验证后退役其运行注册与 API 依赖：`OutlookSyncService`、`OutlookTokenService`、`MicrosoftGraphDeviceCodeClient`、`OutlookGraphModels`、`OutlookConflictService`。

以下**尚未实现**（待设计指导实施）：`GraphCalendarClient`、`OutlookCalendarSyncService`（新版）、`OutlookEventWriteService`、`OutlookCalendarSyncJob`、`OutlookEventMapper`，以及现有 Web UI/API 到新运行链路的接线和本设计新增交互。旧 UI/API 已存在，不从零重建。

## 轻量架构

```
OutlookCalendarSyncJob (Hangfire) → OutlookCalendarSyncService
User HTTP 请求 → OutlookCalendarSyncService (手动同步)
User HTTP 请求 → OutlookEventWriteService (写回)
GraphCalendarClient → Graph REST API
OutlookCalendarSyncService → GraphCalendarClient → DB (EventEntity 绑定 upsert)
OutlookEventWriteService → GraphCalendarClient → DB (EventEntity 更新/软删 + 审计)
```

### 组件职责

**GraphCalendarClient**（待实现）
- 职责：封装 Graph REST 请求、分页、nextLink 白名单校验、Prefer header 注入。只实现需要的端点：`/me`、`/me/calendarGroups`、`/me/calendars`、`/me/calendars/{id}/calendarView`、`/me/calendars/{id}/events`、单事件 GET/POST/PATCH/DELETE。
- 调用方：`OutlookCalendarSyncService`、`OutlookEventWriteService`。
- 依赖：`MsalOutlookAuthCoordinator` 和现有命名 `HttpClient`。30 秒超时、有限重试和 401 强制刷新在这个边界内集中实现，不新增 transport service。
- 不承担业务逻辑；不保存状态。

**OutlookCalendarSyncService**（待实现，取代旧 `OutlookSyncService` 的运行注册）
- 职责：发现日历、普通同步、深度同步、同步历史记录。对外暴露 `DiscoverAsync`、`SyncAsync`（普通）、`FullResourcesSyncAsync`、`RangeInstancesSyncAsync`。内部管理连接级锁、逐日历顺序执行、分页 upsert 和 missing verification。
- 调用方：`OutlookCalendarSyncJob`（Hangfire）、手动同步 API endpoint。
- 依赖：`GraphCalendarClient`、`PimDbContext`、`OutlookEventMapper`。连接级同步锁是服务内部的轻量内存锁，不复用 token cache 锁。
- 不直接发 HTTP；不写回事件。

**OutlookEventWriteService**（待实现）
- 职责：处理用户发起的 PIM -> Outlook 新建、修改、删除。新建使用 `transactionId`。修改/删除使用 `If-Match` 条件请求。Graph 成功后更新本地事件并写审计。412 时不覆盖，返回最新 Outlook 内容供刷新。
- 调用方：事件编辑器 API endpoint（用户请求内同步调用，不进 Hangfire）。
- 依赖：`GraphCalendarClient`、`PimDbContext`、`OutlookEventMapper`。

**OutlookCalendarSyncJob**（待实现）
- 职责：Hangfire 调用的薄 wrapper，5 分钟扫描已连接且有选中日历的 connection 并调用 `OutlookCalendarSyncService.SyncAsync`。
- 约 20 行，不含同步业务逻辑。

**OutlookEventMapper**（待实现）
- 职责：Graph DTO 与 PIM `EventEntity` 之间的字段映射（UTC 时区、全天日期、原始时区、recurrence JSON、ETag/changeKey）。纯静态辅助类，不注入，不访问数据库。

### 不新增的组件

- 不新增 facade、coordinator、per-calendar worker、diagnostics service、durable handler、transactional outbox 或多实例恢复。
- 写回是用户请求内同步 Graph 调用，不进 Hangfire。

## 配置授权

### Entra 引导（Web UI 实现）

分步展示：
1. 打开 Microsoft Entra 管理中心的"应用注册"。
2. 新建应用，名称如 `PIM Calendar Sync`。
3. 账户类型选择"任何组织目录中的账户和个人 Microsoft 账户"。
4. 在"身份验证 → 高级设置"中启用"允许公共客户端流"。
5. Device Code Flow 不要求填写重定向 URI。
6. 添加 Microsoft Graph 委托权限 `Calendars.ReadWrite` 和 `User.Read`。
7. 从"概述"页复制 Client ID。

界面醒目标明：Client ID 不是密码；不提供 Secret 输入框。

### 配置字段

只收集 `ClientId`（UUID 格式）。Tenant 固定 `common`，scopes 固定在后端，普通界面不展示或允许修改。

### Device Code 流程（复用 `OutlookAuthorizationSessionRunner`）

1. 前端调用授权启动 API，后端创建 session 并调用 MSAL `AcquireTokenWithDeviceCode`。
2. 前端显示 `user_code`、`verification_uri`、过期倒计时、复制按钮；自动轮询授权状态 API。
3. MSAL 内部轮询 Microsoft Identity；成功后加密保存完整 MSAL cache 到 `OutlookConnectionEntity.MsalCacheEncrypted`。
4. session 状态复用现有值：`starting` → `waiting-for-user` → `connected`/`expired`/`canceled`/`failed`；成功后 connection 状态同样为 `connected`。
5. 错误按 Client ID 无效、公共客户端流未开启、用户取消、过期、管理员同意受限、网络失败、cache 损坏分别给出修复步骤。

### 后续 token 获取（复用 `MsalOutlookAuthCoordinator`）

所有 Graph 操作只调用 `AcquireTokenSilent`。当 MSAL 抛出需要交互的异常时：connection 标 `reauth-required`、自动同步暂停、界面显示重新连接入口、不清空旧 cache。

## 发现选择

### 发现流程（`OutlookCalendarSyncService.DiscoverAsync`）

1. 调用 `/me/calendarGroups`，跟随全部 `@odata.nextLink`。
2. 对每个分组调用 `/me/calendarGroups/{id}/calendars`，跟随全部分页。
3. 再调用 `/me/calendars` 做兜底。
4. 按 Graph Calendar ID 去重，结果与 `OutlookCalendarBindingEntity` upsert。
5. 每个 binding 创建或复用一个独立 `CalendarEntity`；请求使用 `$select` 限定必要字段，保存分组名称、日历颜色、默认标记、canEdit、owner。
6. 只有全部分页成功后，才把本次未出现的旧 binding 标 `remote-missing`。

### 选择规则

- 首次发现后默认全选（`OutlookCalendarBindingEntity.IsSelected = true`）。
- 用户可以按分组全选、逐日历取消、重新发现。
- 取消选择只暂停同步 + 隐藏对应 PIM 日历层，不删除已导入事件。
- 远端日历消失（remote-missing）保留本地数据，界面提示用户处理。

## 读取同步

### 普通同步

- 所有选中日历统一使用 `/me/calendars/{id}/calendarView?startDateTime=...&endDateTime=...`。
- 批次开始时只读取一次当前 UTC 时间，计算并持久化固定窗口：当前时间 -90 天至 +365 天。该批次的全部日历、分页与缺失验证复用同一组边界。
- 事件请求发送 `Prefer: IdType="ImmutableId"` 和 `Prefer: outlook.timezone="UTC"`。
- 每次完成全部分页对账；成功任务生成 sync generation（`Guid`）写入 `LastSeenSyncGeneration`。
- 不使用 delta 游标。
- 远端新增、修改、移动和删除自动写入 PIM，不创建确认项。

### 逐页 upsert 与 missing verification

- 每页成功即幂等 upsert `EventEntity`，按 `(OutlookCalendarBindingId, OutlookEventId)` 唯一约束。
- 某页失败时保留此前页面已成功 upsert 的结果，将该日历标为失败或部分失败，并继续处理其他日历。
- 只有某日历全部分页成功后，才对仍与窗口相交但未出现的事件执行 missing verification。
- Missing verification：逐个 GET 单事件 `/me/calendars/{id}/events/{eventId}`。
- 404 → 软删除（`deleted_at`）。仍存在或权限错误 → 按远端值更新/保留。
- 分页取消/超时/权限失败绝不推断删除。

### 自动调度

- API 启动后为已连接 connection 入队一次同步。
- Hangfire 每 5 分钟扫描已连接且有选中日历的 connection，调用 `OutlookCalendarSyncJob`。
- 连接级轻量锁：自动任务遇到正在运行的批次则跳过；手动操作返回当前批次进度。
- 重启将旧 running 批次标 `interrupted` 并新建批次，不恢复执行现场。
- 失败日历提供人工重试；重试仅处理所选日历，并新建一个在 JSON 中引用原批次 ID 的批次，不进入后台重试队列。

### 手动深度同步

**full-resources**：对指定日历分页 `/me/calendars/{id}/events`，只 upsert，不凭缺失推断删除。

**range-instances**：用户选择起止日期 → 最多 180 天分片 → 各分片调用 `calendarView` → 按 immutable event ID 去重 → 展示可取消的逐日历进度。

### 定时事件、全天事件与重复日程

- 定时事件：Graph 响应强制为 UTC，数据库存 `DateTimeOffset` UTC，Web 按 `Asia/Shanghai` 显示。保存 `OriginalStartTimeZone`/`OriginalEndTimeZone`。
- 全天事件：保存 `AllDayStartDate`（开始日期）和 `AllDayEndDateExclusive`（排他结束日期），不做 UTC 偏移。
- 重复日程：series master 保存 `GraphRecurrenceJson`（原始 JSON），master 与 occurrence 不互相覆盖。PIM 不新建/修改 recurrence 规则。

### 同账号跨 binding 移动

同一个 Microsoft 账号下，在新建投影前按 `OutlookEventId`（Immutable Event ID）检查其他 binding。若命中旧投影，则更新其 binding 和移动状态，避免重复行；这些本地变更在一次 `SaveChangesAsync` 中提交，使用 EF Core 隐式事务，不新增事务服务。

## 写回冲突

### 编辑流程（`OutlookEventWriteService`）

1. 事件编辑器识别事件存在 `OutlookCalendarBindingId` → 显示 Microsoft 账号、日历名、只读标记（若 `CanEdit=false`）。
2. 用户编辑后点"保存" → 前端展示 before/after 对比、操作类型（新建/修改/删除）、实例/系列范围预览。
3. 用户再次确认 → 同一 API 请求调用 Graph。
4. 新建使用 `transactionId` 降重。
5. 修改/删除使用 `If-Match`（`OutlookEtag`）；`OutlookChangeKey` 作为同步元数据保存，不代替 HTTP ETag。

### 成功与失败

- Graph 成功 → 更新/软删本地 `EventEntity` + 写审计日志。
- 用户确认删除后，Graph DELETE 返回 404 视为幂等成功：本地软删除并记录审计，不要求用户重复操作。
- Graph 失败 → 本地不变；编辑内容保留（前端缓存），用户可重试。
- 412（ETag 冲突）→ 返回 409 Conflict，包含最新 Outlook 事件内容。用户刷新后根据最新版本重新编辑。不提供强制覆盖、四选一冲突恢复或逐字段合并。

### 不可执行的操作

- 只读日历（`CanEdit = false`）不显示写回命令。
- 不提供"复制为 PIM 日程"功能。
- 不支持跨 Outlook 日历移动事件（用户在目标日历新建后到原日历删除）。
- 不支持新建或修改 recurrence pattern/range；删除单个实例或整个系列仍受支持。

### 旧事件重绑

旧事件（迁移前导入）优先按 Graph ID 重绑，其次按 `iCalUId`。不能唯一判断的事件保持可见并标 `legacy-unbound`，禁止回写。不使用标题/时间模糊合并。

## 历史状态诊断

### 同步批次历史

- 复用 `outlook_sync_batches` 表。
- 普通字段：复用现有批次摘要字段（`Status`、`Mode`、`RequestedWindowStart`、`RequestedWindowEnd`、`ReadCount`、`CreatedCount`、`UpdatedCount`、`ConflictCount`、`FailureCount`、`StartedAt`、`FinishedAt`）。
- `ConfirmationCount` 是旧确认中心链路遗留字段；新同步和写回链路保持为 `0`，不新增替代字段或数据库迁移。
- `PerCalendarJson`（jsonb）：逐日历步骤摘要（日历名、读取/新增/修改/删除/失败计数、变化或失败事件的 ID 与标题、原批次 ID）。
- `StepsJson`（jsonb）：逐日历详细步骤（阶段名、耗时、计数）。
- `ErrorsJson`（jsonb）：错误列表（错误码、消息、是否重试成功）。
- 不保存事件正文、token、完整请求/响应。
- 人工重试新建批次，在 `PerCalendarJson` 中记录原批次 ID。
- 写回成功和失败同样记录为 `Mode = writeback` 的历史批次，并继续使用现有事件审计记录本地变化。
- 服务端分页；永不自动清理。

### 用户状态

连接持久化状态复用现有 `not-connected`、`connected`、`reauth-required`，不引入 `not-configured` 或 `waiting-auth` 数据库状态。界面按连接与最新授权 session 组合显示：

| 界面文案 | 判定 |
|----------|------|
| 未配置 | `ClientId` 为空；connection 仍存为初始值 `not-connected` |
| 等待授权 | 最新 session 为 `starting` 或 `waiting-for-user` |
| 已连接 | connection 为 `connected` |
| 需重连 | connection 为 `reauth-required` |
| 失败 | 最新授权或同步操作失败，同时展示可执行的修复说明 |

`TokenHealth` 只保留为内部诊断字段。新 MSAL 链路沿用 `missing`、`healthy`、`interaction-required`、`cache-corrupted`；旧服务可能留下 `expired`、`refresh-failed` 等历史值。普通设置页只显示上述用户状态和修复动作，不直接暴露 `TokenHealth` 技术值。

### 批次状态

复用现有成功状态 `completed`：`running` / `completed` / `partial` / `failed` / `canceled` / `interrupted`。历史批次中的 `completed` 无需迁移或改写。

### 设置页展示

连接账号、已选日历列表、上次同步时间、当前同步进度、最近错误、操作按钮（重新连接、断开、刷新日历列表、立即同步）。

### 断开连接与本地数据清理

- 断开连接清除授权 cache 并停止后续同步，但默认保留已导入事件、日历和永久历史。
- “移除本地 Microsoft 数据”先显示影响数量并要求一次明确确认，再软删除已导入事件和 PIM 日历、移除 binding 与授权 cache；不修改 Outlook 云端，且保留同步历史。

## 错误安全

### HTTP 超时与重试

- 每次 HTTP 尝试 30 秒超时。
- 最多 3 次总尝试（首次 + 最多 2 次重试）。
- 只重试：网络瞬断、HTTP 408、429、5xx。
- 429 优先遵守 `Retry-After`（有界）。
- 401 → 强制静默刷新一次并重放请求；再次 401 标记 `reauth-required`。
- 普通 4xx 不重试。

### 写请求

POST 使用稳定 `transactionId` 降低人工重试导致的重复创建。PATCH/DELETE 不使用透明 HTTP 重试。若请求结果未知或 Graph 已成功但本地保存失败，返回明确错误并保持本地未提交状态，不重复外部副作用；用户刷新或下一次普通同步从 Graph 对账后再决定是否重试。

### 日志安全

不记录：access token、refresh token、MSAL cache 明文、Authorization header、device code、user code、事件正文、完整请求/响应 payload。

允许记录：connection ID、binding ID、sync batch ID、event ID（不含正文）、标题、Graph request-id、状态码、耗时、重试次数、错误码与消息。

### 用户隔离

所有操作按当前 PIM `UserId` 过滤，跨用户 ID 拒绝。

## 旧数据迁移

### 阶段 1：复用（与当前状态一致）

已有实体、字段、迁移、认证基础设施均保持不删。旧 `OutlookSyncService`/`OutlookTokenService`/`MicrosoftGraphDeviceCodeClient` 运行注册保留。

### 阶段 2：新链路接入

新链路端到端通过后：
- 从 `CalendarModule.cs` 注销旧服务的运行注册与 API 路由。
- 对应旧服务代码文件保留在源码中（不删除），标记为 legacy。
- 不删除数据库旧列（此设计阶段不要求）。

### 阶段 3：重绑

旧 `EventEntity` 优先按已有 `outlook_event_id` 重绑，其次使用能够唯一匹配的 `iCalUId`。不能可靠重绑的事件标 `legacy-unbound`，保持可见，禁止回写。不按标题/时间模糊合并。

## 测试验收

### 单元测试

- 认证：device code 状态转换、cache 加密/并发/损坏、silent auth force refresh 与重新授权、错误映射。
- Graph client：endpoint 白名单、nextLink host 校验、Prefer header、30 秒超时、3 次尝试、Retry-After、401 force refresh、不可重试 4xx、用户取消传播。
- 发现：calendarGroups、分组 calendars、根 calendars 全部分页与去重；远端日历消失只标 `remote-missing`。
- 同步：逐日历顺序执行、连接级锁、running 跳过、重启标 interrupted 并新建批次；逐页 upsert；单日历失败继续；人工重试关联原批次；full-resources 只 upsert；range-instances 分片去重进度；missing verification 逻辑（404 软删、仍存在更新、分页失败不删）。
- 普通同步窗口：使用可控时钟验证每个批次只计算一次 -90/+365 边界，所有日历、分页、持久化批次字段和缺失验证使用完全相同的起止值。
- 写回：新建 transactionId 降重、基于 ETag 的 If-Match 修改/删除、412 冲突不覆盖、只读日历禁止写回；普通编辑不能绕过 Outlook 检查。
- 写回删除：用户确认后的 Graph 404 按幂等成功处理，本地软删除且审计只记录一次。
- 映射：UTC、Asia/Shanghai、全天日期边界、原始时区、recurrence master/occurrence。
- 生命周期：断开保留数据；“移除本地 Microsoft 数据”不影响 Outlook 且保留历史。
- 旧事件重绑：已有 outlook_event_id 匹配、iCalUId 唯一匹配、不可靠事件标 legacy-unbound。

### API 集成测试

使用 fake MSAL adapter 和可编程 Graph `HttpMessageHandler`。验证 settings、auth session、discovery、selection、sync run、writeback 契约和用户隔离。

### Web 测试

Entra 分步向导、Client ID UUID 校验、无 Secret/Tenant/Scopes 编辑字段、设备码自动轮询/复制/倒计时/取消/过期恢复、日历分组默认全选/只读标记/取消不删除、同步进度与取消、失败日历重试、写回 before/after/二次确认/412 冲突展示，以及本地数据清理确认。

### 真实账号 E2E

使用真实 Microsoft 账号执行：仅按界面完成 Entra 注册和 Device Code 授权；发现默认、分组、课程表和未分组日历；验证普通同步、两种深度同步、UTC/全天/重复实例；验证 Outlook 到 PIM 的新增/修改/移动/删除；验证 PIM 到 Outlook 的新建/修改/删除、二次确认与 ETag 冲突；验证 token 续期、取消、部分失败、人工重试、永久历史和本地数据清理不影响 Outlook。

## 明确非目标

- 只读 Outlook 事件复制为 PIM 日程（用户明确不要）。
- 新建或编辑 recurrence 规则。
- 一次 PIM 操作跨 Outlook 日历移动。
- Teams 接受/拒绝/暂定/参会与组织者工作流。
- 公网 webhook/subscription。
- delta 游标、每日基线重建。
- 确认中心式写回、outbox、durable execution、自动字段合并、强制覆盖、多实例恢复平台。
- 同步历史自动清理或 90 天滚动删除（永久保留）。

## 完成标准

1. 后端新增服务（GraphCalendarClient、OutlookCalendarSyncService、OutlookEventWriteService、OutlookCalendarSyncJob、OutlookEventMapper）实现并注册。
2. 实体、认证、发现、同步、写回对应的单元测试通过。
3. API 集成测试（fake Graph）覆盖全部契约。
4. Web 构建、lint 和 Outlook 相关测试通过。
5. 旧服务运行注册在新链路端到端通过后可退役。
6. 至少一个真实 Microsoft 账号手工验收记录完成。
7. `dotnet test Pim.sln` 与 `npm --prefix src/client-web run build` 通过，且不存在空按钮、假数据、占位页面或被推迟的已确认功能。
