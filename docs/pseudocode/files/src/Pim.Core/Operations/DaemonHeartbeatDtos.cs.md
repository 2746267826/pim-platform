# src/Pim.Core/Operations/DaemonHeartbeatDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：Windows 守护进程心跳上报的请求/响应 DTO 与服务契约
- 主要依赖：`DaemonSourceState`（`src/Pim.Core/Operations/OperationEnums.cs`）
- 被谁使用：`DaemonEndpoints` 入参/出参；`DaemonHeartbeatService` 实现 `IDaemonHeartbeatService`；Windows `DaemonHeartbeatReporter` 构造并上报请求

## 函数级结构化伪代码

### DaemonHeartbeatRequest
#### 记录主构造 DaemonHeartbeatRequest(...)
- 输入：设备与守护元数据（`DeviceId`、`DaemonKind`、`Version`、`ServerUrl`）、上传时间与错误（`LastSuccessfulUploadAt`、`LastAttemptedUploadAt`、`LastError`）、队列与源状态（`UploadQueueCount`、`ActivityWatchState`、`KeyStatsState`）、`CollectionPaused`、`StatusJson`
- 输出：不可变请求记录
- 副作用：无
- 步骤：
  1. 以位置参数保存全部心跳字段（无服务端接收时间）
- 分支与异常：无
- 调用：无

### DaemonHeartbeatDto
#### 记录主构造 DaemonHeartbeatDto(...)
- 输入：与请求相同的心跳字段，外加 `ReceivedAt`（服务端接收时间）
- 输出：不可变响应/查询 DTO
- 副作用：无
- 步骤：
  1. 保存客户端上报字段与 `ReceivedAt`
- 分支与异常：无
- 调用：无

### IDaemonHeartbeatService
#### UpsertAsync(DaemonHeartbeatRequest request, CancellationToken ct = default)
- 输入：心跳请求；可选取消令牌
- 输出：`Task<DaemonHeartbeatDto>` 最新持久化后的心跳视图
- 副作用：由实现方写入存储（契约层无实现）
- 步骤：
  1. 契约声明：按设备 upsert 心跳并返回 DTO
- 分支与异常：由实现定义
- 调用：实现见 `DaemonHeartbeatService.UpsertAsync`

#### GetLatestAsync(string deviceId, CancellationToken ct = default)
- 输入：`deviceId`；可选取消令牌
- 输出：`Task<DaemonHeartbeatDto?>` 该设备最新心跳，无则 null
- 副作用：由实现方只读查询
- 步骤：
  1. 契约声明：按设备查最新心跳
- 分支与异常：由实现定义
- 调用：实现见 `DaemonHeartbeatService.GetLatestAsync`

#### GetLatestWindowsAsync(CancellationToken ct = default)
- 输入：可选取消令牌
- 输出：`Task<DaemonHeartbeatDto?>` Windows 类守护最新心跳，无则 null
- 副作用：由实现方只读查询
- 步骤：
  1. 契约声明：取 Windows 守护进程最新心跳
- 分支与异常：由实现定义
- 调用：实现见 `DaemonHeartbeatService.GetLatestWindowsAsync`

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Operations`
2. 密封记录 `DaemonHeartbeatRequest`：设备 ID、守护类型、版本、服务端 URL
3. 可选字段：最近成功/尝试上传时间、最近错误、上传队列数
4. 源状态：`ActivityWatchState`、`KeyStatsState`（`DaemonSourceState`）
5. 布尔 `CollectionPaused` 与自由文本 `StatusJson`
6. 密封记录 `DaemonHeartbeatDto`：字段同请求，并追加 `ReceivedAt`
7. 接口 `IDaemonHeartbeatService`：
8.   `UpsertAsync`：写入/更新心跳，返回 DTO
9.   `GetLatestAsync(deviceId)`：按设备取最新，可空
10.  `GetLatestWindowsAsync`：取 Windows 守护最新，可空

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs",
      "label": "DaemonHeartbeatDtos",
      "path": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/DaemonHeartbeatDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/DaemonHeartbeatReporter.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/DaemonHeartbeatReporterTests.cs", "to": "src/Pim.Core/Operations/DaemonHeartbeatDtos.cs", "type": "tests" }
  ]
}
```
