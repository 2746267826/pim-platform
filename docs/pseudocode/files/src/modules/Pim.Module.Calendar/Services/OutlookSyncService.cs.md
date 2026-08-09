# src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：Outlook 连接设置、设备码登录、Graph delta 同步、冲突确认、写回确认与订阅；批次元数据落库。
- 主要依赖：
  - `PimDbContext`、实体 Connection/SyncBatch/Event/Calendar/SyncConflict
  - `IHttpClientFactory`、`IOperationConfirmationService`
  - 可选 `OutlookTokenService`、`IMicrosoftGraphClient`（默认 `MicrosoftGraphDeviceCodeClient`）
  - `AuditVersionService`、DTO/Operations 枚举
- 被谁使用：
  - Calendar 模块 Outlook 相关端点
  - 冲突/治理服务间接依赖确认流

## 函数级结构化伪代码

### OutlookSyncService
#### 构造（两重载）
- 输入：db、httpFactory、confirmation；可选 tokenService、graphClient
- 输出：实例
- 副作用：三参重载委托五参（token/graph 为 null）
- 步骤：保存字段；常量 Provider/GraphBaseUrl/默认租户与 Scopes/状态
- 分支与异常：无
- 调用：无

#### `GetSettingsAsync` / `UpdateSettingsAsync`
- 输入：userId；（更新）UpdateOutlookSettingsRequest
- 输出：`OutlookSettingsResponse`
- 副作用：更新可创建 Connection；写 TenantId/ClientId/Scopes/UpdatedAt
- 步骤：
  - Get：AsNoTracking 查 Connection → MapSettings（缺省填默认）。
  - Update：无则 Add 新实体；规范化租户/Scopes；空 Status/TokenHealth 填 not-connected/missing；Save；Map。
- 分支与异常：无
- 调用：`NormalizeTenantId`/`NormalizeScopes`/`MapSettings`

#### `CreateDeviceCodeRequestAsync` / `PollDeviceCodeAsync`
- 输入：userId；（轮询）deviceCode
- 输出：设备码响应 / 设置响应
- 副作用：轮询成功 StoreTokens 并 Save
- 步骤：
  - Create：取 settings；无 ClientId 返回占位码 PIM-DEVICE-CODE；有则 Graph `RequestDeviceCodeAsync` 映射响应。
  - Poll：无 tokenService → 02036；无 ClientId → 02037；`PollDeviceCodeAsync` → StoreTokens → Save → MapSettings。
- 分支与异常：02036/02037；Graph 异常上抛
- 调用：`GraphClient`、`OutlookTokenService.StoreTokens`

#### `SyncAsync(userId, ct)`
- 输入：用户
- 输出：`OutlookSyncBatchResponse`
- 副作用：创建 SyncBatch；分页读 delta；新建 Event 或核心字段差异确认；更新 Connection；批步骤/错误 JSON
- 步骤：
  1. 新建 batch Status=running，Add+Save；steps/errors 列表。
  2. try：步骤「Load provider configuration」started；取 Connection 与 accessToken。
  3. 无连接/token：步骤 failed，batch failed，Save 返回。
  4. 完成配置与 Validate token；Read calendar delta started。
  5. nextUrl = DeltaLink 或 `BuildInitialDeltaUrl`（-30d..+180d）。
  6. while nextUrl：`GetDeltaPageAsync`；ReadCount+=；每事件：
     - 无本地 OutlookEventId 匹配 → MapOutlookEventAsync 插入，CreatedCount++。
     - 有：CreateOutlookCoreDiffConfirmationAsync；有确认则 Confirmation/Conflict++；否则只更新 ChangeKey/Etag。
  7. 有 NextLink 则继续；有 DeltaLink 写入 connection；否则结束循环。
  8. 更新 LastSyncedAt、Status=connected、TokenHealth 默认 healthy；batch completed；Save；MapBatch。
  9. catch：步骤 failed；batch failed；Save；MapBatch。
- 分支与异常：外层 catch 吞异常记入 batch
- 调用：Graph、MapOutlookEvent、CreateOutlookCoreDiff、SaveBatchAsync

#### `ListBatchesAsync`
- 输入：userId
- 输出：最近 20 批 MapBatch 列表
- 副作用：只读
- 步骤：Where UserId OrderByDescending StartedAt Take 20
- 分支与异常：无
- 调用：`MapBatch`

#### `CreateOutlookWritebackConfirmationAsync` / `WriteToOutlookAsync`
- 输入：userId、Event、action（Write 固定 write_to_outlook）
- 输出：确认 DTO
- 副作用：创建 L3 确认 `outlook.writeback`
- 步骤：序列化 payload/preview；CreateAsync 含 allowedActions review/write_to_outlook/skip
- 分支与异常：确认服务异常
- 调用：`_confirmationService.CreateAsync`

#### `CreateOutlookSubscriptionAsync`
- 输入：userId、notificationUrl
- 输出：void
- 副作用：POST Graph /subscriptions；写 SubscriptionId/ExpiresAt
- 步骤：查 Connection 否则 02005；CreateGraphClient；PostAsJson；EnsureSuccess；解析 id/expiration；Save
- 分支与异常：02005；HTTP 失败 Ensure
- 调用：HttpClient

#### `ExecuteConfirmedWriteAsync(confirmationId, ct)`
- 输入：确认 Id
- 输出：void
- 副作用：Graph Patch 事件；AuditVersion；MarkExecuted
- 步骤：
  1. Get 确认；非 Confirmed → 02007。
  2. 解析 payload eventId/action。
  3. 若 action 为 write_to_outlook 或 operationType 为 writeback/keep_pim/merge：
     取 user/connection/token/event；无 OutlookEventId → 02038。
     构造 patch subject/body/location/start/end；PatchEventAsync。
     更新 ChangeKey/Etag；AuditVersionService.RecordAsync；MarkExecuted。
- 分支与异常：02006/02007/02005/02001/02038
- 调用：Graph、AuditVersionService、confirmation

#### 私有核心
- `CreateGraphClient`：解密 access token 设 Bearer。
- `MapOutlookEventAsync`：默认日历否则 02008；映射 GraphEvent→EventEntity。
- `CreateOutlookCoreDiffConfirmationAsync`：比 title/description/location/dt；无差异 null；有则确认 + UpsertCoreDiffConflict。
- `UpsertCoreDiffConflictAsync`：未 resolved 的 core-diff 冲突更新或新建。
- `GraphClient`：注入或 new DeviceCodeClient。
- `GetOrCreateConnectionAsync`：无则默认连接 Save。
- `GetAccessTokenAsync`：tokenService 刷新或明文解码。
- `BuildInitialDeltaUrl`/`ParseGraphDateTime`/`MapSettings`/`MapBatch`/`SaveBatchAsync`/`AddStep`/`DeserializeSteps`/`HasUsableAccessToken`/`Normalize*`/`BuildDeviceCodeEndpoint`/`GetStringProperty`。

## 近逐行中文伪代码

1. 引入 Http/Json/EF/Exceptions/Operations/Audit/Data/DTOs/Entities。
2. 类字段：db、httpFactory、confirmation、可选 token/graph；常量与 JsonOptions。
3. 两构造：三参转五参。
4. GetSettings：查连接 Map；UpdateSettings：upsert 字段 Save。
5. CreateDeviceCode：无 ClientId 占位；有则 Graph 设备码。
6. PollDeviceCode：校验服务与 ClientId；轮询存 token。
7. SyncAsync：建 batch；无 token 失败返回；delta 循环增/冲突确认；写 deltaLink；完成或 catch 失败。
8. ListBatches：Top 20 映射。
9. Writeback 确认与 CreateSubscription、ExecuteConfirmedWrite（Patch+审计）。
10. 私有：Map 事件、核心差异确认与冲突 upsert、token/URL/JSON 辅助。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs",
      "label": "OutlookSyncService",
      "path": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "depends_on" }
  ]
}
```
