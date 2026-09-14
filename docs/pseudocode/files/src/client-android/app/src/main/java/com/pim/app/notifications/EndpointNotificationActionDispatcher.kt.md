# src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.notifications
- 职责：将端点通知动作（dismiss/confirm 等）封装为对 API `sendEndpointNotificationAction` 的挂起调用，返回响应 data。
- 主要依赖：ApiService、EndpointNotificationActionRequestDto、EndpointNotificationActionResponseDto、ApiResponse
- 被谁使用：NotificationActionReceiver / 通知动作路由、AndroidNotificationActionRouterTest

## 函数级结构化伪代码

### EndpointNotificationActionDispatcher
#### constructor(sender)
- 输入：`sender: suspend (String, EndpointNotificationActionRequestDto) -> ApiResponse<EndpointNotificationActionResponseDto>`
- 输出：实例
- 副作用：无
- 步骤：
  1. 保存可注入的 sender 函数，便于测试替换
- 分支与异常：无
- 调用：无

#### constructor(apiService: ApiService)
- 输入：ApiService
- 输出：实例
- 副作用：无
- 步骤：
  1. 将 `apiService::sendEndpointNotificationAction` 作为 sender 委托主构造
- 分支与异常：无
- 调用：sendEndpointNotificationAction

#### execute(deviceId, action, riskLevel, confirmationId, relatedObjectType, relatedObjectId)
- 输入：设备 ID、动作名、风险等级、可选确认 ID、关联对象类型/ID
- 输出：`EndpointNotificationActionResponseDto?`（取 ApiResponse.data）
- 副作用：网络请求
- 步骤：
  1. 组装 `EndpointNotificationActionRequestDto`
  2. 调用 `sender(deviceId, request)`
  3. 返回 `response.data`
- 分支与异常：网络异常由上层协程/调用方处理；data 可为 null
- 调用：sender

## 近逐行中文伪代码

1. [L8] 定义类 `EndpointNotificationActionDispatcher`，主构造接收可挂起 sender。
2. [L14] 次构造：用 `ApiService::sendEndpointNotificationAction` 绑定真实 API。
3. [L16-23] `execute`：接收 deviceId、action、riskLevel 及可选关联字段。
4. [L24-33] 构造请求 DTO 并调用 sender。
5. [L34] 返回 `response.data`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt",
      "label": "EndpointNotificationActionDispatcher",
      "path": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt",
      "type": "depends_on"
    }
  ]
}
```
