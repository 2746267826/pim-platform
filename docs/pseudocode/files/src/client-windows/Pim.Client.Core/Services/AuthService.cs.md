# src/client-windows/Pim.Client.Core/Services/AuthService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 客户端认证：登录/注册/刷新/登出；将 access/refresh token 与用户信息持久化到 `%LocalAppData%/PIM/token.json`；与 `ApiClient` 同步 BaseUrl 与 Bearer token。
- 主要依赖：`ApiClient`、`AuthResponse`/`ApiResponse` 模型、`System.Text.Json`、文件系统
- 被谁使用：登录窗、启动恢复会话、需鉴权的 Core 服务

## 函数级结构化伪代码

### AuthService
#### AuthService(ApiClient apiClient)
- 输入：HTTP API 客户端
- 输出：实例
- 副作用：订阅 `RequestTiming` 写 Debug 计时日志
- 步骤：保存 `_apiClient`；绑定计时回调
- 分支与异常：无
- 调用：`ApiClient.RequestTiming`

#### bool IsAuthenticated
- 输入：无
- 输出：accessToken 非空且未过期（UTC 比较）
- 副作用：无
- 步骤：检查 `_accessToken` 与 `_accessTokenExpiry`
- 分支与异常：无
- 调用：无

#### string? CurrentUserId / CurrentUsername / CurrentDisplayName / CurrentAccessToken
- 输入：无
- 输出：当前会话用户字段或 token
- 副作用：无（属性读取）
- 步骤：暴露私有字段/属性
- 分支与异常：无
- 调用：无

#### string ServerUrl { get; set; }
- 输入：set 时为新 BaseUrl
- 输出：get 为 `_apiClient.CurrentBaseUrl`
- 副作用：set 调用 `SetBaseUrl`
- 步骤：代理到 ApiClient
- 分支与异常：无
- 调用：`ApiClient.SetBaseUrl` / `CurrentBaseUrl`

#### Task<bool> LoginAsync(string username, string password)
- 输入：用户名、密码
- 输出：成功 true / 失败 false
- 副作用：成功则 ApplyTokens + SaveToken
- 步骤：
  1. POST `/auth/login` 匿名体
  2. `result?.Data` 空 → false
  3. `ApplyTokens`；`SaveToken`；true
- 分支与异常：网络/反序列化失败表现为 null 结果
- 调用：`ApiClient.PostAsync`、`ApplyTokens`、`SaveToken`

#### Task<string?> RegisterAsync(string username, string email, string password, string? displayName)
- 输入：注册字段
- 输出：null 表示成功；否则错误文案
- 副作用：成功则写 token 与用户信息
- 步骤：
  1. POST `/auth/register`
  2. Code==0 且 Data 非空 → ApplyTokens/SaveToken/return null
  3. result 空 → `"服务器无响应"`；否则 `错误码 {Code}: {Message}`
- 分支与异常：见返回文案
- 调用：`ApiClient.PostAsync`、`ApplyTokens`、`SaveToken`

#### Task<bool> TryRestoreTokenAsync()
- 输入：无（读 TokenPath）
- 输出：会话是否可用
- 副作用：内存装载 token；可能 Refresh；设置 ApiClient token 与 OnUnauthorized
- 步骤：
  1. try：文件不存在 → false
  2. 读 JSON 反序列化为 `PersistedToken`；失败 null → false
  3. 恢复字段；`SetAccessToken`；`OnUnauthorized = RefreshAsync`
  4. 若未认证 → `RefreshAsync`；否则 true
  5. catch → false
- 分支与异常：任何异常吞掉返回 false
- 调用：`File` IO、`JsonSerializer`、`RefreshAsync`、`ApiClient`

#### void SaveToken()
- 输入：无（读内存字段）
- 输出：无
- 副作用：写 token.json（尽力而为）
- 步骤：CreateDirectory；构造 PersistedToken；Serialize 写文件；catch 忽略
- 分支与异常：磁盘错误静默
- 调用：`Directory`/`File`/`JsonSerializer`

#### void ApplyTokens(AuthResponse data)
- 输入：认证响应
- 输出：无
- 副作用：更新内存 token/用户；设置 ApiClient token 与 OnUnauthorized
- 步骤：写 access/refresh/expiry；有 UserInfo 则写用户三字段；SetAccessToken；OnUnauthorized=RefreshAsync
- 分支与异常：UserInfo 可空
- 调用：`ApiClient.SetAccessToken`

#### Task<bool> RefreshAsync()
- 输入：无（用 `_refreshToken`）
- 输出：刷新是否成功
- 副作用：更新 token 并 SaveToken
- 步骤：
  1. refresh 空 → false
  2. POST `/auth/refresh` 带 refreshToken
  3. Data 空 → false
  4. 更新 access/refresh/expiry；SetAccessToken；SaveToken；true
- 分支与异常：失败 false（不清理会话）
- 调用：`ApiClient.PostAsync`、`SaveToken`

#### void Logout()
- 输入：无
- 输出：无
- 副作用：清空内存会话；ClearAccessToken；尽力删除 token 文件
- 步骤：置空 token/用户；ClearAccessToken；try Delete TokenPath
- 分支与异常：删除失败忽略
- 调用：`ApiClient.ClearAccessToken`、`File.Delete`

### PersistedToken（私有）
#### 属性 AccessToken / RefreshToken / ExpiresAt / UserId / Username / DisplayName
- 输入：序列化字段
- 输出：磁盘 JSON 形状
- 副作用：无
- 步骤：POCO 属性
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、`Pim.Client.Core.Models`
2. 命名空间 `Pim.Client.Core.Services`；类 `AuthService`
3. 字段：`_apiClient`、access/refresh token、过期时间
4. 静态 `TokenDir`=`%LocalAppData%/PIM`；`TokenPath`=token.json
5. 构造：保存 ApiClient；订阅 RequestTiming 打 Debug
6. `IsAuthenticated`：token 非空且 UTC 未过期
7. 暴露 CurrentUser* 与 CurrentAccessToken
8. `ServerUrl` 代理 ApiClient BaseUrl
9. `LoginAsync`：POST login → ApplyTokens+SaveToken 或 false
10. `RegisterAsync`：POST register；成功 null，失败中文/错误码消息
11. `TryRestoreTokenAsync`：读盘恢复；挂 OnUnauthorized；过期则 Refresh
12. `SaveToken`：写 PersistedToken JSON，失败吞掉
13. `ApplyTokens`：同步内存与 ApiClient；OnUnauthorized=RefreshAsync
14. `RefreshAsync`：POST refresh；成功更新并落盘
15. `Logout`：清空内存、清 token、删文件
16. 私有 `PersistedToken` 承载落盘字段

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/AuthService.cs",
      "label": "AuthService",
      "path": "src/client-windows/Pim.Client.Core/Services/AuthService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/AuthService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "/auth/login", "type": "http" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "/auth/register", "type": "http" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "/auth/refresh", "type": "http" }
  ]
}
```
