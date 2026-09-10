# src/client-windows/Pim.Client.App/Startup.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：组装守护进程 DI：核心 API/认证/采集/心跳/通知路由/托盘均为 Singleton。
- 主要依赖：`Microsoft.Extensions.DependencyInjection`、`Pim.Client.Core.Services`、本程序集 `TrayIcon`/`NotificationActionRouter`
- 被谁使用：`App.OnStartup` → `ConfigureServices`

## 函数级结构化伪代码

### Startup
#### static IServiceProvider ConfigureServices()
- 输入：无
- 输出：已构建的 `ServiceProvider`
- 副作用：注册服务图
- 步骤：
  1. `new ServiceCollection()`
  2. Singleton：`ApiClient`、`AuthService`、`AwCollectorService`、`KeyStatsProcessManager`、`KeyStatsCollectorService`、`DaemonHeartbeatReporter`
  3. Singleton：`Pim.Client.Core.Services.NotificationActionRouter` 与 App 层 `NotificationActionRouter`、`EndpointCollectionBoundaryService`、`TrayIcon`
  4. `BuildServiceProvider` 返回
- 分支与异常：无
- 调用：DI 扩展方法

## 近逐行中文伪代码

1. 新建 ServiceCollection
2. 注册 Core 单例：API、认证、AW 采集、KeyStats 进程与采集、心跳上报
3. 注册 Core 与 App 两套 NotificationActionRouter、端点边界服务、托盘
4. Build 并返回 IServiceProvider

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/Startup.cs",
      "label": "Startup",
      "path": "src/client-windows/Pim.Client.App/Startup.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/Startup.cs.md",
      "layer": "client-windows",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/Startup.cs", "type": "calls" }
  ]
}
```
