# src/Pim.Core/Endpoints/EndpointDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义终端（Endpoint）状态、心跳、采集质量与通知动作的契约 DTO，供 API / Infrastructure / 客户端序列化与边界传输。
- 主要依赖：无（纯数据记录类型；`System` 基础类型）
- 被谁使用：
  - `src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs`（映射实体、处理心跳/质量/通知动作）
  - `src/Pim.Api/Endpoints/EndpointEndpoints.cs`（HTTP 请求/响应体）
  - `src/Pim.Api/Today/TodaySectionProviders.cs`（今日视图引用状态列表）
  - `tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs`（边界测试）

## 函数级结构化伪代码

### EndpointStatusDto
#### EndpointStatusDto(DeviceId, Platform, AppVersion, UploadStatus, CollectionCacheCount, OnlineOnlyBlockedCount, LastHeartbeatAt)
- 输入：设备标识、平台、可选应用版本、上传状态、采集缓存条数、仅在线阻塞条数、最近心跳时间
- 输出：不可变状态快照记录
- 副作用：无
- 步骤：
  1. 以位置参数构造密封 `record`，承载终端当前运行态字段
- 分支与异常：无
- 调用：无

### EndpointHeartbeatRequest
#### EndpointHeartbeatRequest(Platform, AppVersion = null, UploadStatus = null, CollectionCacheCount = null)
- 输入：平台（必填）；应用版本、上传状态、采集缓存条数（可选）
- 输出：心跳上报请求体
- 副作用：无
- 步骤：
  1. 构造客户端向服务端上报心跳时使用的请求 DTO
  2. 可选字段缺省为 `null`，由服务端决定是否覆盖既有状态
- 分支与异常：无
- 调用：无

### EndpointCollectionQualityDto
#### EndpointCollectionQualityDto(DeviceId, Platform, UploadStatus, IssueCount, CheckedAt)
- 输入：设备标识、平台、上传状态、问题计数、检查时间
- 输出：采集质量结果快照
- 副作用：无
- 步骤：
  1. 构造表示某设备采集链路健康/问题统计的响应 DTO
- 分支与异常：无
- 调用：无

### EndpointNotificationActionRequest
#### EndpointNotificationActionRequest(Action, RiskLevel, ConfirmationId = null, RelatedObjectType = null, RelatedObjectId = null)
- 输入：动作名、风险等级；可选确认 ID、关联对象类型与 ID
- 输出：通知动作请求体
- 副作用：无
- 步骤：
  1. 构造客户端对系统通知执行 dismiss/confirm 等动作时的请求 DTO
  2. 高风险动作可通过 `ConfirmationId` 与确认流关联
- 分支与异常：无
- 调用：无

### EndpointNotificationActionResponse
#### EndpointNotificationActionResponse(Result, DetailUrl = null, Message = null)
- 输入：处理结果字符串；可选详情 URL、消息
- 输出：通知动作响应体
- 副作用：无
- 步骤：
  1. 构造服务端处理通知动作后的结果 DTO，供客户端跳转或展示
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Endpoints`
2. 定义密封记录 `EndpointStatusDto`：字段为 DeviceId、Platform、可空 AppVersion、UploadStatus、CollectionCacheCount、OnlineOnlyBlockedCount、可空 LastHeartbeatAt
3. 定义密封记录 `EndpointHeartbeatRequest`：必填 Platform；可选 AppVersion、UploadStatus、CollectionCacheCount（默认 null）
4. 定义密封记录 `EndpointCollectionQualityDto`：DeviceId、Platform、UploadStatus、IssueCount、CheckedAt
5. 定义密封记录 `EndpointNotificationActionRequest`：必填 Action、RiskLevel；可选 ConfirmationId、RelatedObjectType、RelatedObjectId
6. 定义密封记录 `EndpointNotificationActionResponse`：必填 Result；可选 DetailUrl、Message
7. 文件结束；无方法体、无校验逻辑，纯契约载体

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Endpoints/EndpointDtos.cs",
      "label": "EndpointDtos",
      "path": "src/Pim.Core/Endpoints/EndpointDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Endpoints/EndpointDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/EndpointEndpoints.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs", "to": "src/Pim.Core/Endpoints/EndpointDtos.cs", "type": "tests" }
  ]
}
```
