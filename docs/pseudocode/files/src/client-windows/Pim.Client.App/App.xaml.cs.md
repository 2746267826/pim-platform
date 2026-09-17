# src/client-windows/Pim.Client.App/App.xaml.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 守护进程 WPF 入口：日志/DI、自启同步、托盘、登录恢复、AW/KeyStats 采集、心跳循环与退出清理。
- 主要依赖：`Startup`、`Logger`、`DaemonConfig`、`ApiClient`、`AuthService`、`TrayIcon`、`AwCollectorService`、`KeyStatsCollectorService`、`KeyStatsProcessManager`、`DaemonHeartbeatReporter`、`LoginWindow`、`MainShellWindow`、`AutoStartManager`
- 被谁使用：WPF 应用启动（`App.xaml`）

## 函数级结构化伪代码

### App
#### 字段
- `Services`：静态 `IServiceProvider`
- `_trayIcon`、`_shutdown` CTS、2 分钟 `PeriodicTimer`、`_heartbeatTask`

#### OnStartup(StartupEventArgs e) — async void
- 输入：启动参数
- 输出：无
- 副作用：初始化守护进程全生命周期
- 步骤：
  1. `Logger.Initialize`；订阅 `UnhandledException` 写 Error
  2. try：`Startup.ConfigureServices` → `Services`
  3. `DaemonConfig.Load`；`AutoStartManager.Set(config.AutoStart)`
  4. 取 `ApiClient`/`AuthService`；若 `ServerUrl` 非空则规范化、必要时 `Save`、设 BaseUrl 与 Auth ServerUrl
  5. `ShutdownMode = OnExplicitShutdown`；订阅 `RequestTiming` 打日志
  6. 解析并 `Show` `TrayIcon`
  7. `TryRestoreTokenAsync`：成功记日志；失败弹 `LoginWindow.ShowDialog`，取消则 Warn 无 API
  8. 启动 `AwCollectorService`（注入 Log）
  9. `EnsureKeyStatsRunning`；启动 `KeyStatsCollectorService`
  10. `Task.Run(RunHeartbeatLoopAsync)`
  11. catch：Error + `Shutdown`
- 分支与异常：启动致命错误关闭进程；登录可跳过
- 调用：DI 解析、配置、采集器、登录窗

#### ShowMainShellWindow(route?)
- 输入：可选路由
- 输出：无
- 副作用：显示/激活主壳
- 步骤：已有 `MainShellWindow` 则可选 `OpenRoute` + `Activate`；否则新建 `Show` 再路由
- 分支与异常：无
- 调用：`MainShellWindow`

#### RunHeartbeatLoopAsync(ct)
- 输入：取消令牌
- 输出：无
- 副作用：周期上报
- 步骤：先 `ReportHeartbeatOnceAsync`；while `WaitForNextTickAsync` 再上报；取消吞掉 `OperationCanceledException`
- 分支与异常：仅取消时退出
- 调用：`ReportHeartbeatOnceAsync`

#### ReportHeartbeatOnceAsync(ct)
- 输入：取消令牌
- 输出：无
- 副作用：HTTP 心跳
- 步骤：
  1. 取 reporter/config/aw/ks
  2. awState：有成功上传且无错 → Available；无错无时间 → Unknown；有错 → Unavailable
  3. ksState 取 `LastHealth.DaemonSourceState` 或 Unknown
  4. lastSuccess = Max(aw/ks LastUploadTime)；lastError 优先 aw
  5. version 取 AssemblyInformationalVersion 或默认
  6. `BuildHeartbeat`（机器名、版本、ServerUrl、成功/当前时间、错误、队列、状态、扩展匿名对象）
  7. `ReportAsync`；Info 成功；非取消异常 Warn
- 分支与异常：取消 rethrow；其它记 Warn
- 调用：`DaemonHeartbeatReporter`

#### MaxTime(a, b)
- 输入：两个可空 DateTime
- 输出：较晚者或非空一方
- 副作用：无
- 步骤：null 处理后比较 `>`
- 分支与异常：无
- 调用：无

#### OnExit(ExitEventArgs e)
- 输入：退出事件
- 输出：无
- 副作用：取消心跳、释放资源
- 步骤：Cancel/Dispose timer；心跳 Task OnlyOnFaulted 记 Warn；Dispose CTS/托盘；Info 退出；base
- 分支与异常：无
- 调用：base.OnExit

#### EnsureKeyStatsRunning()
- 输入：无
- 输出：无
- 副作用：单实例收敛 KeyStats 进程
- 步骤：取 manager；拼 BaseDirectory+ExeFileName；不存在 Warn 返回；`EnsureRunning(exe, SessionId)` 按 plan 打日志（启动/保留 pid/无 keep/停多余）
- 分支与异常：catch Error 日志不抛
- 调用：`KeyStatsProcessManager.EnsureRunning`

## 近逐行中文伪代码

1. 静态 Services + 托盘/关闭 CTS/2 分钟定时器/心跳 Task 字段
2. OnStartup：日志 → DI → 配置与自启 → 规范化 ServerUrl → 托盘 → 恢复或登录 → AW 采集 → 确保 KeyStats → KeyStats 采集 → 心跳循环；失败则 Shutdown
3. ShowMainShellWindow：复用或新建主壳并可选 OpenRoute
4. 心跳：立即一次 + 每 2 分钟；聚合 AW/KS 状态与队列上报
5. OnExit：取消并释放；EnsureKeyStatsRunning 收敛用户会话单实例

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/App.xaml.cs",
      "label": "App",
      "path": "src/client-windows/Pim.Client.App/App.xaml.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/App.xaml.cs.md",
      "layer": "client-windows",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/Startup.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/AutoStartManager.cs", "type": "calls" }
  ]
}
```
