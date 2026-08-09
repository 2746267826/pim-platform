# src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：终端设备状态服务——心跳 upsert、状态列表、采集质量、通知动作风险分流与动作审计落库；判断操作是否可离线缓存。
- 主要依赖：`PimDbContext`；`ICurrentUserService`；`Pim.Core.Endpoints` DTO；`DomainException`；EF Core
- 被谁使用：DI `AddScoped`；`EndpointEndpoints`；`EndpointsStatusTodaySectionProvider`

## 函数级结构化伪代码

### EndpointStatusService
#### 静态字段 `OfflineCacheableKinds`
- 输入：无
- 输出：忽略大小写的操作类型集合
- 副作用：无
- 步骤：
  1. 预置可离线缓存 kind：`pc-activity`、`android-location`、`collection-upload`、`window-context`、`browser-context`、`input-activity`、`device-state`、`upload-retry`、`mobile-sensor`、`location-sample`
- 分支与异常：无
- 调用：无

#### 构造 `EndpointStatusService(PimDbContext db, ICurrentUserService currentUser)`
- 输入：DbContext、当前用户
- 输出：实例
- 副作用：保存字段
- 步骤：赋值 `_db`、`_currentUser`
- 分支与异常：无
- 调用：无

#### 属性 `UserId`
- 输入：无
- 输出：当前用户 Guid
- 副作用：无登录时抛错
- 步骤：取 `_currentUser.UserId`，空则 `DomainException(01002, "Login required")`
- 分支与异常：未登录 → 01002
- 调用：无

#### `bool CanCacheOffline(string operationKind)`
- 输入：操作类型字符串
- 输出：是否允许离线缓存
- 副作用：无
- 步骤：对 `operationKind` 做 null→空串、`Trim` 后查 `OfflineCacheableKinds`
- 分支与异常：无
- 调用：`HashSet.Contains`

#### `Task<IReadOnlyList<EndpointStatusDto>> ListAsync(CancellationToken ct)`
- 输入：取消令牌
- 输出：当前用户终端状态列表
- 副作用：读库
- 步骤：
  1. 取 `UserId`
  2. 无跟踪查询该用户全部 `EndpointStatuses`
  3. 按 `LastHeartbeatAt` 降序、`DeviceId` 升序排序
  4. `MapStatus` 映射后返回列表
- 分支与异常：未登录 → 01002
- 调用：EF `Where`/`ToListAsync`；`MapStatus`

#### `Task<EndpointStatusDto> UpsertHeartbeatAsync(string deviceId, EndpointHeartbeatRequest request, CancellationToken ct)`
- 输入：设备 id、心跳请求、取消令牌
- 输出：更新后的状态 DTO
- 副作用：插入或更新 `EndpointStatusEntity` 并 `SaveChanges`
- 步骤：
  1. 规范化 `deviceId`；记录 `now` 与 `userId`
  2. 查找用户+设备；不存在则新建（`CreatedAt=now`）并 Add
  3. `ApplyHeartbeat` 写平台/版本/上传状态/缓存数/心跳时间
  4. 保存并 `MapStatus` 返回
- 分支与异常：空 deviceId → `ArgumentException`；未登录 → 01002
- 调用：`NormalizeDeviceId`、`ApplyHeartbeat`、`SaveChangesAsync`

#### `Task<EndpointCollectionQualityDto> GetCollectionQualityAsync(string deviceId, CancellationToken ct)`
- 输入：设备 id、取消令牌
- 输出：采集质量 DTO（含 issueCount）
- 副作用：可能创建默认状态行
- 步骤：
  1. 规范化设备 id；`GetOrCreateStateAsync`（平台由设备 id 推断）
  2. `UploadStatus` 既非 Healthy 也非 Unknown → issueCount++
  3. `CollectionCacheCount > 0` → issueCount++
  4. 返回 DeviceId/Platform/UploadStatus/issueCount/当前 UTC
- 分支与异常：空 deviceId / 未登录
- 调用：`GetOrCreateStateAsync`、`InferPlatform`

#### `Task<EndpointNotificationActionResponse> HandleNotificationActionAsync(string deviceId, EndpointNotificationActionRequest request, CancellationToken ct)`
- 输入：设备 id、通知动作请求、取消令牌
- 输出：动作结果（Executed / OpenDetailRequired / Rejected）
- 副作用：可能增加 `OnlineOnlyBlockedCount`；写 `EndpointNotificationActions`；SaveChanges
- 步骤：
  1. 规范化设备；`GetOrCreateStateAsync`
  2. Action 空白 → Rejected + 记录后返回
  3. 若 `CanExecuteDirectly(RiskLevel)` → Executed 文案
  4. 否则：`OnlineOnlyBlockedCount++`、更新 `UpdatedAt`，返回 OpenDetailRequired + `BuildDetailUrl`
  5. `RecordNotificationActionAsync` 落库后返回
- 分支与异常：空 deviceId / 未登录
- 调用：`CanExecuteDirectly`、`BuildDetailUrl`、`RecordNotificationActionAsync`

#### `Task<EndpointStatusEntity> GetOrCreateStateAsync(string deviceId, string platform, CancellationToken ct)`
- 输入：已规范化设备 id、平台、取消令牌
- 输出：实体（存在即返回，否则新建并保存）
- 副作用：可能 Insert + SaveChanges
- 步骤：
  1. 按 userId+deviceId 查找
  2. 命中直接返回
  3. 否则新建：Platform、UploadStatus=Unknown、时间戳，Add 并保存
- 分支与异常：未登录
- 调用：EF 查询与保存

#### `Task RecordNotificationActionAsync(state, request, response, ct)`
- 输入：状态实体、请求、响应、取消令牌
- 输出：无
- 副作用：插入 `EndpointNotificationActionEntity` 并保存
- 步骤：填充 UserId/DeviceId/Action/RiskLevel/Result/DetailUrl/Message/ConfirmationId/Related* /CreatedAt 后 SaveChanges
- 分支与异常：无额外
- 调用：`SaveChangesAsync`

#### `static bool CanExecuteDirectly(string riskLevel)`
- 输入：风险级别
- 输出：是否可直接在线执行
- 副作用：无
- 步骤：等于 `Low` 或 `L0AutomaticArtifact` 或 `L1LowRiskAction`（忽略大小写）
- 分支与异常：无
- 调用：`string.Equals`

#### `static string BuildDetailUrl(EndpointNotificationActionRequest request)`
- 输入：请求
- 输出：详情路径
- 副作用：无
- 步骤：
  1. 有 ConfirmationId → `/confirmations/{escaped}`
  2. 否则有 RelatedObjectType+Id → `/audit/{type}/{id}`
  3. 否则 `/confirmations`
- 分支与异常：无
- 调用：`Uri.EscapeDataString`

#### `static EndpointStatusDto MapStatus(EndpointStatusEntity state)`
- 输入：实体
- 输出：DTO（设备、平台、版本、上传状态、缓存数、在线阻断计数、最后心跳）
- 副作用：无
- 步骤：构造 `EndpointStatusDto` 记录
- 分支与异常：无
- 调用：无

#### `static string NormalizeDeviceId(string deviceId)`
- 输入：原始设备 id
- 输出：Trim 后字符串
- 副作用：空白抛 `ArgumentException`
- 步骤：IsNullOrWhiteSpace → 抛错；否则 Trim
- 分支与异常：必填校验
- 调用：无

#### `static string NormalizePlatform(string? platform)`
- 输入：平台字符串
- 输出：`android` | `windows`（默认 windows）
- 副作用：无
- 步骤：Trim+ToLowerInvariant；仅允许 android/windows，否则 windows
- 分支与异常：无
- 调用：无

#### `static string InferPlatform(string deviceId)`
- 输入：设备 id
- 输出：含 "android"（忽略大小写）则 android，否则 windows
- 副作用：无
- 步骤：`Contains("android")` 判断
- 分支与异常：无
- 调用：无

#### `static void ApplyHeartbeat(state, request, now)`
- 输入：实体、心跳请求、当前时间
- 输出：无
- 副作用：就地改实体字段
- 步骤：写 Platform（规范化）、AppVersion、UploadStatus（空→Unknown）、CollectionCacheCount（null→0 且 ≥0）、LastHeartbeatAt/UpdatedAt=now
- 分支与异常：无
- 调用：`NormalizePlatform`、`NormalizeUploadStatus`

#### `static string NormalizeUploadStatus(string? uploadStatus)`
- 输入：上传状态
- 输出：Trim 或 `Unknown`
- 副作用：无
- 步骤：空白 → Unknown，否则 Trim
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、Core.Endpoints、DomainException、Auth、Data
2. 密封类 `EndpointStatusService`
3. 静态集合定义可离线缓存的 operation kind 列表
4. 注入 `_db` 与 `_currentUser`；`UserId` 未登录抛 01002
5. `CanCacheOffline`：Trim 后查集合
6. `ListAsync`：查用户状态 → 心跳时间降序 → Map 列表
7. `UpsertHeartbeatAsync`：规范化设备 → 查或建状态 → ApplyHeartbeat → 保存 → Map
8. `GetCollectionQualityAsync`：GetOrCreate → 按上传状态与缓存数计 issue → 返回质量 DTO
9. `HandleNotificationActionAsync`：空 Action 拒绝；低风险 Executed；高风险累计阻断并要求打开详情 URL；写动作审计
10. `GetOrCreateStateAsync`：无则默认 Unknown 上传状态并保存
11. `RecordNotificationActionAsync`：组装动作实体并 SaveChanges
12. `CanExecuteDirectly` / `BuildDetailUrl` / `MapStatus` / 各类 Normalize/Infer/Apply 辅助方法
13. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs",
      "label": "EndpointStatusService",
      "path": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Infrastructure/Auth/ICurrentUserService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "calls" }
  ]
}
```
