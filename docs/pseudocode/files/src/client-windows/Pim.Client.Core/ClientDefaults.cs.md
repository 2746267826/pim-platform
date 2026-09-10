# src/client-windows/Pim.Client.Core/ClientDefaults.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 客户端共享默认常量（本地 API 根 URL）。
- 主要依赖：无
- 被谁使用：`ApiClient`、`DaemonConfig`、`DaemonHeartbeatReporter`、`TrayIcon`、`StatusWindow`

## 函数级结构化伪代码

### ClientDefaults
#### const DefaultServerUrl
- 输入：无
- 输出：字符串常量 `"http://127.0.0.1:5858"`
- 副作用：无
- 步骤：编译期常量
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.Core`。
2. 静态类 `ClientDefaults`。
3. 常量 `DefaultServerUrl = http://127.0.0.1:5858`（与本地 API 默认端口对齐）。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/ClientDefaults.cs",
      "label": "ClientDefaults",
      "path": "src/client-windows/Pim.Client.Core/ClientDefaults.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/ClientDefaults.cs.md",
      "layer": "client-windows",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/DaemonConfig.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" }
  ]
}
```
