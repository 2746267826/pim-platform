# src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：封装 WPF WebView2：初始化、按路由导航到本机 API 同源 Web、注入 accessToken、拦截新窗口到当前视图。
- 主要依赖：`Microsoft.Web.WebView2`、`ApiClient`、`AuthService`、`System.Text.Json`
- 被谁使用：`MainShellWindow`

## 函数级结构化伪代码

### EmbeddedWebViewHost
#### 构造 EmbeddedWebViewHost(ApiClient apiClient, AuthService authService)
- 输入：API 客户端、认证服务
- 输出：实例，`View` 为新 `WebView2`
- 副作用：创建 WebView2 控件
- 步骤：保存依赖；`View = new WebView2()`
- 分支与异常：无
- 调用：无

#### InitializeAsync()
- 输入：无
- 输出：`Task`
- 副作用：初始化 CoreWebView2、改设置、挂事件、注入 token
- 步骤：
  1. 若已 `_initialized` 则返回。
  2. `EnsureCoreWebView2Async()`。
  3. 若 Core 非空：开默认右键菜单与 DevTools；挂 `NewWindowRequested`；`InjectAuthTokenAsync()`。
  4. `_initialized = true`。
- 分支与异常：Core 为空时跳过设置但仍标记已初始化
- 调用：`EnsureCoreWebView2Async`、`InjectAuthTokenAsync`

#### NavigateAsync(string route)
- 输入：前端路由（如 `today`）
- 输出：`Task`
- 副作用：导航 WebView
- 步骤：`InitializeAsync` → `InjectAuthTokenAsync` → `CoreWebView2.Navigate(BuildWebUrl(route))`
- 分支与异常：Core 为空时 `?.` 不导航
- 调用：`InitializeAsync`、`InjectAuthTokenAsync`、`BuildWebUrl`

#### BuildWebUrl(string route)
- 输入：路由
- 输出：完整 URL 字符串
- 副作用：无
- 步骤：
  1. 取 `_apiClient.CurrentBaseUrl` 去尾 `/`。
  2. 若以 `/api/v1` 结尾则截掉该后缀，得到 Web 根。
  3. 路由空白则默认 `/today`。
  4. 返回 `{root}/{route.TrimStart('/')}`。
- 分支与异常：无
- 调用：无

#### InjectAuthTokenAsync()
- 输入：无
- 输出：`Task`
- 副作用：向页面注入/执行 localStorage 写 token 脚本
- 步骤：
  1. Core 为空或无 access token → 返回。
  2. 将 token JSON 序列化，拼 `localStorage.setItem('accessToken', ...)`。
  3. `AddScriptToExecuteOnDocumentCreatedAsync` + `ExecuteScriptAsync`。
- 分支与异常：无 token 早退
- 调用：`JsonSerializer.Serialize`、WebView2 脚本 API

#### OnNewWindowRequested(sender, e) [static private]
- 输入：新窗口请求事件
- 输出：无
- 副作用：在当前 WebView 内导航目标 URI
- 步骤：若 sender 为 `CoreWebView2`：`e.Handled=true`；`Navigate(e.Uri)`
- 分支与异常：sender 类型不符则忽略
- 调用：`Navigate`

## 近逐行中文伪代码

1. 字段：`ApiClient`、`AuthService`、`_initialized`；公开 `View`。
2. 构造时 new `WebView2`。
3. 初始化：Ensure Core → 开菜单/DevTools → 新窗口回本窗 → 注 token → 标记完成。
4. 导航：确保初始化与 token，再 Navigate 到 `BuildWebUrl`。
5. `BuildWebUrl`：API 基址去 `/api/v1`，空路由变 `/today`。
6. 注入：有 token 则 document-created 与立即执行写 `localStorage.accessToken`。
7. 新窗口：Handled 并在本 WebView 打开。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs",
      "label": "EmbeddedWebViewHost",
      "path": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs.md",
      "layer": "client-windows",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs", "type": "calls" }
  ]
}
```
