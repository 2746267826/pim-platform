# src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 客户端与端点状态/通知动作相关的 JSON DTO。
- 主要依赖：`System.Text.Json.Serialization`
- 被谁使用：`ApiClient`、`NotificationActionRouter`、状态中心/心跳上报路径

## 函数级结构化伪代码

### EndpointStatusDto
#### 属性（无方法）
- 输入：无
- 输出：字段
- 副作用：无
- 步骤：
  1. `DeviceId`；`Platform` 默认 windows。
  2. `UploadStatus` 默认 Unknown；缓存与 online-only 阻塞计数。
  3. `LastHeartbeatAt` 可空。
- 分支与异常：无
- 调用：无

### EndpointCollectionQualityDto
#### 属性（无方法）
- 输入：无
- 输出：字段
- 副作用：无
- 步骤：
  1. 设备/平台/上传状态。
  2. `IssueCount`、`CheckedAt`。
- 分支与异常：无
- 调用：无

### EndpointNotificationActionRequestDto
#### 属性（无方法）
- 输入：无
- 输出：字段
- 副作用：无
- 步骤：
  1. `Action`、`RiskLevel`。
  2. 可选 `ConfirmationId`、`RelatedObjectType`、`RelatedObjectId`。
- 分支与异常：无
- 调用：无

### EndpointNotificationActionResponseDto
#### 属性（无方法）
- 输入：无
- 输出：字段
- 副作用：无
- 步骤：
  1. `Result`；可选 `DetailUrl`、`Message`。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 四个 sealed DTO，JSON 属性名 camelCase。
2. 状态 DTO：设备、平台、上传状态、缓存计数、心跳时间。
3. 采集质量 DTO：问题数与检查时间。
4. 通知动作请求/响应：action、risk、确认与关联对象；结果 URL 与消息。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs",
      "label": "EndpointDtos",
      "path": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs.md",
      "layer": "client-windows",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "to": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/NotificationActionRouter.cs", "to": "src/client-windows/Pim.Client.Core/Models/EndpointDtos.cs", "type": "depends_on" }
  ]
}
```
