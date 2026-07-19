# Outlook 日历分页修复设计

## 状态

已批准。2026-07-19。

**一句话目标**：消除 `IsAllowedNextLink` 对满足既定同源安全边界的 Microsoft Graph `@odata.nextLink` 的路径形状误拒绝，使多页日历同步可以完成全部页面，同时保持 SSRF 防护边界。

## 生产证据与根因追溯

### 已确认事实

- 生产 `master` 日志显示三个 binding 每五分钟同步失败一次，错误码均为 `invalid-next-link`。
- 对每个 binding，首次 `GET https://graph.microsoft.com/v1.0/me/calendars/{encoded-id}/calendarView` 返回 HTTP 200 且第一页事件被正常处理，之后在发出第二页 HTTP 请求前抛出 `invalid-next-link`。
- 三个数据库中的 Graph Calendar ID 均为 68 字符，URL 解码后不含原始斜杠，因此之前 raw-slash ImmutableId 补丁（`ceccf1c2`）不能解释本故障。
- 响应正文与被拒绝的 `@odata.nextLink` 未出现在日志中，具体路径形状未知。

### 补丁历史

| 提交 | 变更 | 效果 |
| --- | --- | --- |
| `e026f04c` | 新建 `GraphCalendarClient`，`IsAllowedNextLink` 按枚举路径形状 allowlist | 基础实现 |
| `e7a608cd` | 允许 `calendarView/` 尾部斜杠 | 部分缓解 |
| `ceccf1c2` | 添加 `MatchesMeCollectionLeaf`，允许多段 ImmutableId | 部分缓解 |

两次针对路径形状的补丁后，三个特定 binding 仍每五分钟失败，说明 **路径形状精确 allowlist 不可持续**。

### 未知项

- 具体被拒绝的 `@odata.nextLink` 路径（日志省略）。
- Graph Calendar ID 的具体值（不提供）。
- 具体 binding ID（不提供）。

## 考虑的方案

| 方案 | 描述 | 结论 |
| --- | --- | --- |
| **A：保留 origin/version 校验，移除路径形状 allowlist（批准）** | 只验证 scheme/https/host/port/userinfo/fragment/dot-traversal 和 `/v1.0/` 前缀，将剩余 `/v1.0/...` 路径视为 opaque。nextLink 原字符串传递到 `HttpRequestMessage`。 | 批准：消除一类误拒绝根本原因，保持 SSRF 信任边界。 |
| **B：扩展路径 allowlist 覆盖猜测的 Graph 路径** | 猜测生产三个 binding 的路径形状并添加例外。 | 拒绝：当前 unknown-unknown 问题；无法保证不会出现新的路径形状；不可持续。 |
| **C：仅发布诊断补丁** | 记录被拒绝的 nextLink 以调查，不改路径校验。 | 拒绝：下一个 5 分钟周期仍产生错误；推迟修复而不解决问题。 |

### 权衡

- **方案 A**：安全假设从“我知道 Graph 返回的所有合法路径形状”变为“我信任 Graph 在同源 HTTPS 下返回的 `/v1.0/` 路径”。Microsoft Graph authorization scope 已经在服务端执行访问控制。
- **方案 B**：需要不断监控和补丁，增加代码复杂度且不消除根因。
- **方案 C**：不改善用户体验，只推迟决策。

## 安全契约

```
输入: @odata.nextLink (string)
输出: bool (true = 允许请求)

接受条件（全部满足时 return true）:
├─ Uri.TryCreate(value, UriKind.Absolute) == true
├─ uri.Scheme == "https" (OrdinalIgnoreCase)
├─ uri.Host == "graph.microsoft.com" (OrdinalIgnoreCase)
├─ uri.IsDefaultPort == true
├─ string.IsNullOrEmpty(uri.UserInfo) == true
├─ string.IsNullOrEmpty(uri.Fragment) == true
├─ HasRawDotSegments(uri) == false   // 单/双编解码 ".." "."
├─ uri.AbsolutePath starts with "/v1.0/" (Ordinal)
├─ 剩余路径非空
└─ 不再校验剩余路径的具体形状

拒绝条件（任一满足时 return false）:
├─ 非 absolute URI
├─ 非 HTTPS
├─ 非 graph.microsoft.com 主机
├─ 非默认端口
├─ 包含 userinfo
├─ 包含 fragment
├─ 包含 raw 或编解码后的 "." / ".." 段
├─ 不以 /v1.0/ 开头
└─ /v1.0/ 后为空路径

安全说明:
- graph.microsoft.com 主机验证 + HTTPS 保证 nextLink 不能指向任意外部主机
- Graph OAuth 授权域已经约束请求可访问的数据范围
- 同源 /v1.0 路径被 Graph 自身控制，不构成 SSRF 向量
```

## 组件与数据流

影响范围仅包括：

- `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs` 中的 `GraphCalendarClient.IsAllowedNextLink` 及不再使用的 `MatchesMeCollectionLeaf`。
- `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs` 中的 nextLink 校验和分页回归测试。

### 删除

- `MatchesMeCollectionLeaf` 方法——不再需要对集合-叶子路径做精确匹配。
- `IsAllowedNextLink` 中 `segments is [...]` 模式匹配分支（calendarGroups、calendars、calendarView、events 精确形状）——全部由统一 opaque 路径校验替代。

### 保留不变

- `GetPagesAsync` —— 只调 `IsAllowedNextLink`，不感知路径内容。
- `OutlookCalendarSyncService` —— `MapSafeSyncError`、`invalid-next-link` 错误码、`GetCalendarViewAsync`、`GetEventsAsync` 调用均不修改。
- `IsAllowedNextLink` 签名 `public static bool IsAllowedNextLink(string value)` —— 保持静态。
- `HasRawDotSegments` —— dot-traversal 安全检查保留。
- scheme/host/port/userinfo/fragment/prefix 单项校验保留。
- 所有写入方法（`CreateEventAsync`、`UpdateEventAsync`、`DeleteEventAsync`）不涉及 nextLink，不修改。

### 数据流（修改后）

```
@odata.nextLink (来自 Graph JSON)
  │
  ▼
IsAllowedNextLink(value)
  ▲── Uri.TryCreate → absolute? → false
  ▲── Scheme == "https"? → false
  ▲── Host == "graph.microsoft.com"? → false
  ▲── DefaultPort? → false
  ▲── Empty UserInfo? → false
  ▲── Empty Fragment? → false
  ▲── HasRawDotSegments? → true
  ▲── StartsWith "/v1.0/"? → false
  ▲── Non-empty after "/v1.0/"? → false
  │
  true ──► HttpRequestMessage(RequestUri = value) ──► HttpClient.SendAsync
```

## 错误处理与敏感数据规则

### 错误码

| 条件 | 异常/错误码 | 不变？ |
| --- | --- | --- |
| IsAllowedNextLink 返回 false | `InvalidOperationException("Invalid nextLink rejected")` → `MapSafeSyncError` 映射为 `("invalid-next-link", "分页链接校验失败")` | 不变 |
| HTTP 失败 | `GraphRequestException` / `OutlookReauthenticationRequiredException` | 不变 |

### 敏感数据

- `GetPagesAsync` 在以 nextLink 构建 `HttpRequestMessage` 时，**不记录、不持久化 nextLink**。
- `MapSafeSyncError` 将 `InvalidOperationException` 的消息映射为安全中文字符串，原始消息不出现在 API 响应或数据库错误列中。
- `OutlookCalendarSyncServiceTests.cs:2565-2596` 验证敏感数据不泄露的安全测试继续通过。

## TDD 测试设计

**先改/加测试，使当前代码对于受信任的 opaque `/v1.0/` 路径 RED（失败），然后修改 `IsAllowedNextLink` 使测试 GREEN。**

### 当前 RED（修改后 `IsAllowedNextLink` 应通过）

所有已通过测试中，属于合法 `/v1.0` 路径且当前为 `true` 的保持 `true`，包括多段 ImmutableId。但以下新场景当前应为 `false`，修改后应为 `true`：

| 输入 | 当前 | 目标 | 理由 |
| --- | --- | --- | --- |
| `https://graph.microsoft.com/v1.0/users/{id}/calendars/{calendar-id}/calendarView?$skiptoken=a` | `false` | `true` | 同源 `/v1.0` 路径，安全；仅作为 opaque 路径代表，不声称它就是生产中的未知路径 |
| `https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView/e1` | `false` | `true` | 同源 `/v1.0` 路径，安全 |
| `https://graph.microsoft.com/v1.0/me/drive/root` | `false` | `true` | 证明校验器不再解释同源 `/v1.0` 资源路径；实际数据访问仍受 Graph scope 约束 |

### 安全拒绝场景（实现前后均保持 GREEN）

`NextLinkScenarios` 中以下条目必须继续返回 `false`：

| 条目 | 理由 |
| --- | --- |
| `https://evil.com/v1.0/me/calendarGroups` | 非 Graph 主机 |
| `/v1.0/me/calendarGroups` | 非 absolute |
| `http://graph.microsoft.com/v1.0/me/calendarGroups` | 非 HTTPS |
| `https://graph.microsoft.com:8080/v1.0/me/calendarGroups` | 非默认端口 |
| `https://user:pass@graph.microsoft.com/v1.0/me/calendarGroups` | 含 userinfo |
| `https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView?s=s#frag` | 含 fragment |
| `https://graph.microsoft.com/beta/me/calendarGroups` | 非 `/v1.0/` 前缀 |
| `https://graph.microsoft.com/v1.0/me/calendarGroups/../me/calendarGroups` | dot traversal |
| `https://graph.microsoft.com/v1.0/me/calendarGroups/%2e%2e/me/calendarGroups` | 编码 dot traversal |
| `https://graph.microsoft.com/v1.0/me/calendarGroups/%252e%252e/me/calendarGroups` | 双重编码 dot traversal |
| `https://graph.microsoft.com/v1.0/me/calendars/./calendarView` | 单点 dot traversal |
| `https://graph.microsoft.us/v1.0/me/calendars` | 非 `graph.microsoft.com` |
| `https://graph.microsoft.com/v1.0` | 缺少 `/v1.0/` 前缀后的非空路径 |

### 集成测试

**`Pagination_FollowsValidNextLink`**、**`Pagination_TrailingSlashCalendarViewNextLink`**、**`Pagination_UnencodedSlashImmutableIdCalendarViewNextLink`** 保持绿色——修改不改变合法 nextLink 的跟随行为。

**新增 `Pagination_FollowsTrustedOpaqueNextLinkWithoutReconstruction`**：
- 第一页返回 canonical 测试链接 `"@odata.nextLink":"https://graph.microsoft.com/v1.0/users/u/calendars/c/calendarView?$skiptoken=abc&marker=keep"`。
- 当前实现因资源路径形状不匹配而在第二次 HTTP 请求前失败，形成生产故障对应的 RED 边界。
- 修改后验证第二次请求的 `RequestUri!.AbsoluteUri` 与该 canonical 测试链接相同，证明代码没有重建路径、删除或追加查询参数。这里不承诺绕过 `HttpRequestMessage` 自身的标准 URI 解析或规范化。

### 预期结果

| 步骤 | 状态 | 说明 |
| --- | --- | --- |
| 先改测试（新 RED 场景加期望 `true`） | RED | `IsAllowedNextLink` 拒绝 `/v1.0/users/...`、未知同源资源路径和新增分页集成场景 |
| 修改 `IsAllowedNextLink` 移除路径 allowlist | GREEN | 剩余安全检查通过，拒绝条目仍拒绝，新路径通过 |
| `dotnet test Pim.sln` | GREEN | 全部测试通过 |

## 验证与生产验收

### 本地验证

```powershell
dotnet test Pim.sln
```

### 生产验收

1. 合并 PR 后，从包含修复提交的最新 `master` 构建并部署。
2. 观察三个之前失败 binding 的首个同步周期完成页面 2+。
3. 确认 `invalid-next-link` 错误不再出现。
4. 确认安全拒绝测试有效：恶意主机、非 HTTPS、非默认端口、userinfo、fragment、dot traversal、beta 路径、相对 URI 仍被拒绝。
5. 无需提供部署凭据或生产主机特有命令。

## 非目标

- 不添加 diagnostics、custom exceptions、fallback URL reconstruction、relative-link 支持、beta 支持、sovereign-cloud 主机或 config 开关。
- 不记录或持久化完整 nextLink。
- 不重构 `GetPagesAsync`、`OutlookCalendarSyncService` 或 `MapSafeSyncError`。
- 不改变写入方法的任何行为。
- 不涉及与分页无关的模块。

### 回滚

若生产出现意外的同源 `/v1.0/` 路径安全问题，只回滚本次功能修复提交并恢复当前路径 allowlist；不撤销 `e7a608cd` 的安全错误映射或 `ceccf1c2` 的既有测试历史。回滚后生产会重新出现已知分页失败，因此应同时暂停自动同步或部署后续修正。
