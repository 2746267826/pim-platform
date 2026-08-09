# src/modules/Pim.Module.Calendar/Services/ReminderService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：提醒 CRUD 动作（创建/列表/延后/关闭/处理动作）、通知载荷构建与投递日志；高风险级别限制动作必须先 open 详情。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`
  - `ReminderEntity`、`ReminderDeliveryEntity`
  - Calendar DTOs（Create/Response/Action/Notification/Delivery）
  - `DomainException`、`System.Text.Json`
- 被谁使用：Calendar 提醒端点；`RemindersQueueTodaySectionProvider` 等

## 函数级结构化伪代码

### ReminderService
#### 构造与 `UserId`
- 输入：db、currentUser
- 输出：实例
- 副作用：未登录抛 `DomainException(01002)`
- 步骤：字段赋值；UserId 属性取值
- 分支与异常：未登录
- 调用：`ICurrentUserService`

#### `async Task<ReminderResponse> CreateAsync(CreateReminderRequest request, ct)`
- 输入：创建请求
- 输出：映射后的 ReminderResponse
- 副作用：插入 ReminderEntity 并 Save
- 步骤：
  1. 校验 Title 1–255。
  2. 建实体：UserId、关联对象、Title trim、Body/TriggerReason、RiskLevel 默认 L1、Channels 去重 JSON、DND 时间、ScheduledAt UTC、Status=Open、时间戳。
  3. Add + Save；Map 返回。
- 分支与异常：校验失败 02042
- 调用：`ValidateRequired`、`Normalize`、JsonSerializer、`Map`

#### `async Task<IReadOnlyList<ReminderResponse>> ListAsync(ct)`
- 输入：无
- 输出：当前用户提醒列表按 ScheduledAt 升序
- 副作用：只读查询
- 步骤：Where UserId → OrderBy ScheduledAt → Select Map
- 分支与异常：未登录
- 调用：EF、`Map`

#### `async Task<ReminderResponse> SnoozeAsync(Guid id, DateTimeOffset scheduledAt, ct)`
- 输入：Id 与新计划时间
- 输出：更新后 Response
- 副作用：ScheduledAt UTC、Status=Snoozed、UpdatedAt、Save
- 步骤：Load → 赋值 → Save → Map
- 分支与异常：不存在 02041
- 调用：`LoadAsync`

#### `async Task<ReminderResponse> DismissAsync(Guid id, ct)`
- 输入：Id
- 输出：Response
- 副作用：Status=Dismissed、Save
- 步骤：Load → 赋值 → Save → Map
- 分支与异常：02041
- 调用：`LoadAsync`

#### `async Task<ReminderActionResponse> HandleActionAsync(Guid id, string action, ct)`
- 输入：Id、动作字符串
- 输出：`ReminderActionResponse`（结果码、状态、详情 URL）
- 副作用：可能改状态；写投递记录；Save
- 步骤：
  1. Load；Normalize action 默认 open。
  2. 若 RiskLevel 在 HighRisk 且 action 不是 open/snooze/dismiss → 记投递 OpenDetailRequired，返回需打开详情。
  3. dismiss → Dismissed；snooze → Snoozed 且 +15 分钟。
  4. UpdatedAt；记投递 Executed；Save；返回 Executed。
- 分支与异常：高风险限制；02041
- 调用：`RecordDeliveryAsync`、`DetailUrl`

#### `async Task<ReminderNotificationPayloadDto> BuildNotificationPayloadAsync(Guid id, string channel, ct)`
- 输入：Id、渠道
- 输出：通知载荷 DTO
- 副作用：记投递 Created + Save
- 步骤：Load → 建 payload（含 open/snooze/dismiss 动作）→ RecordDelivery → Save
- 分支与异常：02041
- 调用：`RecordDeliveryAsync`

#### `async Task<IReadOnlyList<ReminderDeliveryDto>> GetDeliveryLogAsync(ct)`
- 输入：无
- 输出：最近 100 条投递（CreatedAt 降序）
- 副作用：只读
- 步骤：Where UserId → OrderByDescending → Take 100 → 投影 DTO
- 分支与异常：未登录
- 调用：EF

#### 私有 `LoadAsync` / `RecordDeliveryAsync` / `Map` / `ReadChannels` / `DetailUrl` / `ValidateRequired` / `Normalize`
- Load：Id+UserId 找不到抛 02041。
- RecordDelivery：默认 payload；Add ReminderDeliveryEntity（Channel 默认 Web；有 action 则 RespondedAt=UtcNow）。
- Map：实体 → ReminderResponse（Channels 反序列化）。
- ReadChannels：JSON 失败返回空列表。
- DetailUrl：关联 confirmation 则 `/confirmations/{id}` 否则 `/reminders/{id}`。
- ValidateRequired：空白或超长 02042。
- Normalize：空白用 fallback，否则 Trim。

## 近逐行中文伪代码

1. 引入 Json、EF、DomainException、Auth、Data、DTOs、Entities。
2. 静态 Web JsonOptions；HighRiskLevels 集合（L2/L3/L4 治理类）。
3. 构造注入 db 与 currentUser；UserId 未登录 01002。
4. Create：校验标题 → 填实体默认 Open → 存库 → Map。
5. List：当前用户按计划时间排序映射。
6. Snooze/Dismiss：Load 改状态与时间后保存。
7. HandleAction：高风险非 open/snooze/dismiss 强制 OpenDetailRequired；否则 dismiss/snooze（+15m）并记 Executed 投递。
8. BuildNotificationPayload：建载荷、记 Created 投递。
9. GetDeliveryLog：最近 100 条。
10. 辅助：Load、RecordDelivery、Map、读渠道 JSON、详情 URL、校验与 Normalize。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs",
      "label": "ReminderService",
      "path": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/ReminderService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "type": "calls" }
  ]
}
```
