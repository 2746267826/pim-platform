# src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：组装守护进程心跳请求并通过 `ApiClient` POST `daemon/heartbeat`。
- 主要依赖：`ApiClient`、`DaemonHeartbeatRequest`、`ClientDefaults`、`System.Text.Json`、`Assembly`
- 被谁使用：Windows 守护主循环/状态上报路径

## 函数级结构化伪代码

### DaemonHeartbeatReporter
#### 构造函数(ApiClient api)
- 输入：`ApiClient`
- 输出：实例
- 副作用：保存 `_api`
- 步骤：赋值字段
- 分支与异常：无
- 调用：无

#### ReportAsync(DaemonHeartbeatRequest heartbeat, CancellationToken ct)
- 输入：心跳 DTO
- 输出：Task
- 副作用：HTTP POST
- 步骤：`_api.PostAsync<object>("daemon/heartbeat", heartbeat, ct)`
- 分支与异常：透传 ApiClient 异常
- 调用：`ApiClient.PostAsync`

#### BuildHeartbeat(...) [static]
- 输入：deviceId、version、serverUrl、上下传时间、lastError、队列数、AW/KeyStats 状态、可选 statusDetails
- 输出：`DaemonHeartbeatRequest`
- 副作用：无（纯构造）
- 步骤：
  1. `NormalizeServerUrl`（空白用默认）。
  2. `statusPayload` 固定 machine/process；若有 statusDetails 反射属性合并进字典。
  3. version 空白则用执行程序集版本或 `"unknown"`。
  4. 构造请求：platform=`"windows"`，`isPaused=false`，`statusJson=Serialize(statusPayload)`。
- 分支与异常：无
- 调用：`ApiClient.NormalizeServerUrl`、`JsonSerializer.Serialize`

## 近逐行中文伪代码

1. 持有 `ApiClient`。
2. `ReportAsync` 固定路径 POST 心跳。
3. `BuildHeartbeat` 规范化 ServerUrl；拼 status 字典（机器名 + 进程名 + 可选反射字段）。
4. 版本回退程序集；平台 windows；序列化 statusJson 返回 `DaemonHeartbeatRequest`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs",
      "label": "DaemonHeartbeatReporter",
      "path": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "daemon/heartbeat", "type": "http" }
  ]
}
```
