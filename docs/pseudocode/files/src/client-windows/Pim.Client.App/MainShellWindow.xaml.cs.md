# src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs

## 元信息
- 语言：C# / WPF
- 程序集或包：Pim.Client.App
- 职责：Windows 客户端主壳窗口：嵌入 WebView 路由导航、保存服务器地址、展示账户/上传状态、打开状态窗。
- 主要依赖：`ApiClient`、`AuthService`、`AwCollectorService`、`KeyStatsCollectorService`、`EmbeddedWebViewHost`、`DaemonConfig`、`StatusWindow`
- 被谁使用：WPF 应用入口/托盘导航（`OpenRoute`）

## 函数级结构化伪代码

### MainShellWindow
#### MainShellWindow()
- 输入：无
- 输出：已初始化主壳
- 副作用：DI 解析服务；创建 WebHost 并放入 `WebHostSlot`；加载配置填充服务器 URL；刷新壳状态
- 步骤：
  1. `InitializeComponent`。
  2. 从 `App.Services` 取 `ApiClient`/`AuthService`/`AwCollectorService`/`KeyStatsCollectorService`。
  3. 构造 `EmbeddedWebViewHost`，将 `View` 赋给 `WebHostSlot.Content`。
  4. `DaemonConfig.Load()` 写 `ServerUrlBox`；`RefreshShellState`。
- 分支与异常：无显式
- 调用：`DaemonConfig.Load`、`RefreshShellState`

#### OnLoaded(sender, e)
- 输入：Loaded 事件
- 输出：无
- 副作用：导航到当前路由
- 步骤：1. `await NavigateToAsync(_currentRoute)`（默认 `/today`）。
- 分支与异常：异步异常由调用方/框架处理
- 调用：`NavigateToAsync`

#### OpenRoute(route)
- 输入：路由字符串
- 输出：无（fire-and-forget）
- 副作用：异步导航；失败时写 Logger 警告
- 步骤：
  1. 启动 `NavigateToAsync(route)`。
  2. `ContinueWith` 仅在 faulted 时 `Logger.Warn` 基异常消息。
- 分支与异常：导航失败仅日志
- 调用：`NavigateToAsync`、`Services.Logger.Warn`

#### OnNavigateRoute(sender, e)
- 输入：按钮点击
- 输出：无
- 副作用：若 `Tag` 为 string route 则导航
- 步骤：检查 `sender` 为 `Button` 且 `Tag` 为 string，则 `NavigateToAsync`。
- 分支与异常：Tag 非 string 忽略
- 调用：`NavigateToAsync`

#### OnRefresh(sender, e)
- 输入：刷新按钮
- 输出：无
- 副作用：重导航当前路由并刷新壳状态文本
- 步骤：`NavigateToAsync(_currentRoute)`；`RefreshShellState`。
- 分支与异常：无
- 调用：`NavigateToAsync`、`RefreshShellState`

#### NavigateToAsync(route)
- 输入：route
- 输出：Task
- 副作用：更新 `_currentRoute`、UI 文本、WebHost 导航
- 步骤：
  1. 保存 `_currentRoute`。
  2. `CurrentRouteText.Text = route`。
  3. `await _webHost.NavigateAsync(route)`。
- 分支与异常：透传 WebHost 异常
- 调用：`EmbeddedWebViewHost.NavigateAsync`

#### OnSaveServerUrl(sender, e)
- 输入：保存按钮
- 输出：无
- 副作用：校验 URL；更新 ApiClient/AuthService 基址；持久化 DaemonConfig；刷新状态
- 步骤：
  1. `NormalizeServerUrl(ServerUrlBox.Text.Trim())`。
  2. 若非绝对 http/https → MessageBox 警告并 return。
  3. `_apiClient.SetBaseUrl`；`_authService.ServerUrl = normalized`。
  4. 加载配置写 `ServerUrl` 并 `Save`；回写文本框；`RefreshShellState`。
- 分支与异常：非法 URL 提前返回
- 调用：`ApiClient.NormalizeServerUrl`/`SetBaseUrl`、`DaemonConfig.Load`/`Save`、`RefreshShellState`

#### OnOpenStatusWindow(sender, e)
- 输入：打开状态窗
- 输出：无
- 副作用：激活已有 `StatusWindow` 或新建 Show
- 步骤：
  1. 在 `Application.Current.Windows` 中找首个 `StatusWindow`。
  2. 存在则 `Activate`；否则 `new StatusWindow().Show()`。
- 分支与异常：无
- 调用：`StatusWindow`

#### RefreshShellState()
- 输入：无
- 输出：无
- 副作用：更新账户与上传状态文案
- 步骤：
  1. 已登录：显示用户名；否则「未登录」。
  2. 拼接 AW 队列数、AW/KeyStats 最后上传错误（null 显示「无」）。
- 分支与异常：无
- 调用：`AuthService.IsAuthenticated`/`CurrentUsername`、采集器 `QueueCount`/`LastUploadError`

## 近逐行中文伪代码

1. 引入 WPF、DI、`Pim.Client.Core.Services`。
2. 字段：ApiClient、Auth、Aw/KeyStats 采集器、WebHost、当前路由默认 `/today`。
3. 构造：DI 解析；建 EmbeddedWebViewHost 塞入槽；读 DaemonConfig 填 URL；刷新状态。
4. Loaded → 导航当前路由。
5. `OpenRoute` 异步导航，失败只打 Warn。
6. 导航按钮读 Tag 路由；刷新重导航并刷新状态。
7. `NavigateToAsync` 记路由、更新 UI、WebHost.NavigateAsync。
8. 保存服务器：规范化并校验 http(s)，写 Api/Auth/配置。
9. 状态窗单例激活或新建。
10. 壳状态：登录文案 + AW 队列与两边上传错误。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs",
      "label": "MainShellWindow",
      "path": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs.md",
      "layer": "client-windows",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/DaemonConfig.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "type": "calls" }
  ]
}
```
