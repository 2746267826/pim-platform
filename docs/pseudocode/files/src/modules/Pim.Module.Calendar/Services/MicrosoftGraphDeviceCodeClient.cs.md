# src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：实现 `IMicrosoftGraphClient`：设备码 OAuth、刷新令牌、Graph 日历 delta 分页与事件 PATCH。
- 主要依赖：`IHttpClientFactory`（命名客户端 `"outlook"`）、login.microsoftonline.com、graph.microsoft.com/v1.0、System.Text.Json
- 被谁使用：Outlook 连接/同步服务 DI 注入

## 函数级结构化伪代码

### MicrosoftGraphDeviceCodeClient
#### 构造 `MicrosoftGraphDeviceCodeClient(IHttpClientFactory httpClientFactory)`
- 输入：HttpClient 工厂
- 输出：实例
- 副作用：保存工厂引用
- 步骤：赋值 `_httpClientFactory`
- 分支与异常：无
- 调用：无

#### `Task<DeviceCodeResult> RequestDeviceCodeAsync(tenant, clientId, scopes, ct)`
- 输入：租户、应用 Id、scopes
- 输出：`DeviceCodeResult`（device_code/user_code/uri/message/expires_in）
- 副作用：HTTP POST 设备码端点
- 步骤：
  1. Form：client_id、scope
  2. POST `TokenEndpoint(tenant,"devicecode")`
  3. EnsureSuccess；解析 JSON 字段（uri 缺省 microsoft link，expires 缺省 900）
- 分支与异常：非成功状态码抛 Http 异常
- 调用：`Http.PostAsync`、`ReadFromJsonAsync`、`ReadString`/`ReadInt`

#### `Task<TokenResult> PollDeviceCodeAsync(tenant, clientId, deviceCode, ct)`
- 输入：租户、clientId、deviceCode
- 输出：`TokenResult`
- 副作用：POST token 端点
- 步骤：grant_type=device_code 表单 → `RequestTokenAsync`
- 分支与异常：同 token 请求
- 调用：`RequestTokenAsync`

#### `Task<TokenResult> RefreshAsync(tenant, clientId, refreshToken, scopes, ct)`
- 输入：刷新令牌与 scopes
- 输出：`TokenResult`
- 副作用：POST token
- 步骤：grant_type=refresh_token 表单 → `RequestTokenAsync`
- 分支与异常：同 token 请求
- 调用：`RequestTokenAsync`

#### `Task<GraphDeltaPage> GetDeltaPageAsync(accessToken, url, ct)`
- 输入：Bearer token、绝对或相对 Graph URL
- 输出：事件列表 + nextLink/deltaLink
- 副作用：GET Graph
- 步骤：
  1. 规范化 URL；Authorization Bearer
  2. 成功后读 `value` 数组，逐项 `ReadGraphEvent`
  3. 读 `@odata.nextLink` / `@odata.deltaLink`
- 分支与异常：无 value 则空列表；HTTP 失败抛
- 调用：`NormalizeGraphUrl`、`ReadGraphEvent`、`ReadNullableString`

#### `Task<GraphEvent> PatchEventAsync(accessToken, eventId, changeKey, patch, ct)`
- 输入：token、事件 Id、If-Match changeKey、patch 对象
- 输出：更新后 `GraphEvent`
- 副作用：PATCH `/me/events/{id}`
- 步骤：序列化 patch JSON；EnsureSuccess；`ReadGraphEvent`
- 分支与异常：HTTP 失败抛（含 ETag 冲突）
- 调用：`Http.SendAsync`、`JsonSerializer.Serialize`

#### 私有 `Http` / `RequestTokenAsync` / `TokenEndpoint` / `NormalizeGraphUrl` / JSON 读取辅助
- 输入：tenant/content 或 JsonElement 字段名
- 输出：HttpClient、TokenResult、URL、字符串/int/Graph 结构
- 副作用：token POST
- 步骤：
  1. `CreateClient("outlook")`
  2. POST oauth2/v2.0/token → access/refresh/expires_in/scope
  3. 绝对 URL 原样，否则拼 GraphBaseUrl
  4. `ReadGraphEvent` 映射 id/subject/bodyPreview/start/end/修改时间/iCalUId/changeKey/etag/location/webLink
  5. `ReadDateTimeTimeZone` 缺省 MinValue+UTC
- 分支与异常：属性缺失用 fallback/null
- 调用：JsonElement API

## 近逐行中文伪代码

1. 引入 Http/Json 命名空间；类实现 `IMicrosoftGraphClient`
2. 常量 Provider=`outlook`、GraphBaseUrl；工厂与 Web JsonOptions
3. 构造保存 `IHttpClientFactory`
4. RequestDeviceCode：POST devicecode，解析 device/user code 与 uri/message/expires
5. PollDeviceCode：device_code grant → RequestToken
6. Refresh：refresh_token grant → RequestToken
7. GetDeltaPage：Bearer GET，解析 value 与 odata 链接
8. PatchEvent：PATCH 事件 + If-Match，返回 GraphEvent
9. Http 属性按 Provider 建客户端；RequestToken 解析四字段
10. TokenEndpoint 拼 login.microsoftonline.com；NormalizeGraphUrl 相对路径补前缀
11. ReadGraphEvent/ReadDateTimeTimeZone 与 ReadString/Nullable/Int 辅助

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs",
      "label": "MicrosoftGraphDeviceCodeClient",
      "path": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "to": "IMicrosoftGraphClient", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "to": "IHttpClientFactory", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "to": "https://login.microsoftonline.com", "type": "http" },
    { "from": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "to": "https://graph.microsoft.com/v1.0", "type": "http" },
    { "from": "src/modules/Pim.Module.Calendar", "to": "src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs", "type": "depends_on" }
  ]
}
```
