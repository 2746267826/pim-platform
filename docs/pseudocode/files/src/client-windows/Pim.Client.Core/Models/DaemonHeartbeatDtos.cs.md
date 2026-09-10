# src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 守护进程心跳请求体 DTO，汇总设备、版本、上传时间/错误、队列与采集子系统状态。
- 主要依赖：无
- 被谁使用：Daemon 心跳上报服务 / API 客户端

## 函数级结构化伪代码

### DaemonHeartbeatRequest
#### record 构造（位置参数）
- 输入：
  - `DeviceId`、`DaemonKind`、`Version`、`ServerUrl`
  - `LastSuccessfulUploadAt`、`LastAttemptedUploadAt`（可空）
  - `LastError`、`UploadQueueCount`（可空）
  - `ActivityWatchState`、`KeyStatsState`
  - `CollectionPaused`、`StatusJson`
- 输出：不可变记录
- 副作用：无
- 步骤：
  1. 作为一次心跳 payload 携带采集与上传健康快照。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.Core.Models`。
2. sealed record `DaemonHeartbeatRequest`：设备/守护种类/版本/服务器。
3. 最近成功/尝试上传时间；错误与队列长度。
4. AW 与 KeyStats 状态字符串；是否暂停采集；完整 `StatusJson`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs",
      "label": "DaemonHeartbeatRequest",
      "path": "src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs.md",
      "layer": "client-windows",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core", "to": "src/client-windows/Pim.Client.Core/Models/DaemonHeartbeatDtos.cs", "type": "depends_on" }
  ]
}
```
