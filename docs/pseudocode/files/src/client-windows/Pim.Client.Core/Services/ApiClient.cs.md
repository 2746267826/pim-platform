# src/client-windows/Pim.Client.Core/Services/ApiClient.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 客户端 HTTP API 封装：基址、Bearer、401 刷新重试、计时事件、端点状态/采集质量/通知动作便捷方法。
- 主要依赖：`HttpClient`、`ClientDefaults`、`Pim.Client.Core.Models`、System.Net.Http.Json
- 被谁使用：AuthService、心跳、采集上报、状态中心等 Core/App 服务

## 函数级结构化伪代码

### ApiClient
#### ApiClient()
- 输入：无
- 输出：实例
- 副作用：创建禁用代理的 HttpClient，BaseAddress=`DefaultServerUrl/api/v1/`
- 步骤：HttpClientHandler UseProxy=false；new HttpClient
- 分支与异常：无
- 调用：`ClientDefaults.DefaultServerUrl`

#### void SetBaseUrl(string baseUrl)
- 输入：服务器根 URL
- 输出：无
- 副作用：规范化 URL；新建 HttpClient 并原子替换旧实例（Dispose 旧）
- 步骤：
  1. `NormalizeServerUrl`；拼 `/api/v1/`
  2. 新 Handler+Client；复制旧 Authorization
  3. `Interlocked.Exchange` 换出旧 client 并 Dispose
- 分支与异常：无
- 调用：`NormalizeServerUrl`、`Interlocked.Exchange`

#### string CurrentBaseUrl
- 输入：无
- 输出：当前 BaseAddress 去尾斜杠，或空串
- 副作用：无
- 步骤：读 `_httpClient.BaseAddress`
- 分支与异常：无
- 调用：无

#### string Resolve(string endpoint)
- 输入：相对路径
- 输出：去前导 `/` 的相对路径（相对 BaseAddress）
- 副作用：无
- 步骤：`TrimStart('/')`
- 分支与异常：无
- 调用：无

#### static string NormalizeServerUrl(string baseUrl)
- 输入：用户输入 URL
- 输出：规范化字符串；`localhost` 主机改为 `127.0.0.1`
- 副作用：无
- 步骤：Trim 尾 `/`；非绝对 URI 原样返回；绝对且 Host=localhost 则替换 Host
- 分支与异常：解析失败返回 trimmed
- 调用：`Uri.TryCreate`、`UriBuilder`

#### void SetAccessToken / ClearAccessToken
- 输入：token 或无
- 输出：无
- 副作用：设置/清空 DefaultRequestHeaders.Authorization Bearer
- 步骤：赋值或 null
- 分支与异常：无
- 调用：无

#### GetEndpointStatusesAsync / GetEndpointCollectionQualityAsync / SendEndpointNotificationActionAsync
- 输入：可选 deviceId、请求 DTO、ct
- 输出：对应 `ApiResponse<T>?`
- 副作用：HTTP 请求
- 步骤：委托 Get/Post 到 `/endpoints...`
- 分支与异常：见 SendWithAuthRetry
- 调用：`GetAsync`/`PostAsync`、`Uri.EscapeDataString`

#### static string BuildConfirmationDetailPath(string confirmationId)
- 输入：确认单 Id
- 输出：`/confirmations/{id}` 路径
- 副作用：无
- 步骤：Escape 后拼接
- 分支与异常：无
- 调用：`Uri.EscapeDataString`

#### Task<T?> GetAsync / PostAsync / PutAsync；Task DeleteAsync；Task<T?> PostStringAsync
- 输入：endpoint、body/content、ct
- 输出：反序列化 T 或 void（Delete 用 IgnoreResult）
- 副作用：HTTP；可能 401 刷新重试
- 步骤：包装 `SendWithAuthRetryAsync`；PostString 用 text/calendar 内容
- 分支与异常：见下
- 调用：`HttpClient` Get/PostAsJson/PutAsJson/Delete/Post

#### Task<T?> SendWithAuthRetryAsync(Func request, ct)
- 输入：发送委托、取消令牌
- 输出：JSON 反序列化结果或 default
- 副作用：HTTP；触发 `RequestTiming`；可能调用 `OnUnauthorized` 刷新后重发
- 步骤：
  1. 计时发第一跳
  2. 若 401 且有 OnUnauthorized 且非刷新中：置 `_isRefreshing`；调 OnUnauthorized；成功则重发并计时「after refresh」；finally 清标志
  3. 否则触发 RequestTiming 第一跳耗时
  4. `EnsureSuccessStatusCode`
  5. 若 T 为 IgnoreResult → default；否则 `ReadFromJsonAsync<T>`
- 分支与异常：非成功状态抛；刷新失败仍 Ensure 原响应
- 调用：`OnUnauthorized`、`RequestTiming`、`ReadFromJsonAsync`

### IgnoreResult（私有）
- 空密封类，供 Delete 忽略 body

## 近逐行中文伪代码

1. 引入 Diagnostics、Net、Headers、Http.Json、ClientDefaults、Models
2. 类 `ApiClient`：字段 HttpClient、volatile `_isRefreshing`；委托 OnUnauthorized；事件 RequestTiming
3. 构造：禁用代理，Base=`DefaultServerUrl/api/v1/`
4. `SetBaseUrl`：规范化；新 Client 拷贝 Auth；原子交换并 Dispose 旧
5. CurrentBaseUrl / Resolve 工具
6. NormalizeServerUrl：localhost→127.0.0.1
7. Set/Clear Bearer
8. 端点状态/质量/通知动作便捷 API
9. BuildConfirmationDetailPath 静态路径
10. Get/Post/Put/Delete/PostString 均走 SendWithAuthRetry
11. SendWithAuthRetry：401 且可刷新则重试；EnsureSuccess；IgnoreResult 不读 body
12. 私有 IgnoreResult 标记类型

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs",
      "label": "ApiClient",
      "path": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/ApiClient.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "to": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "to": "System.Net.Http.HttpClient", "type": "depends_on" }
  ]
}
```
