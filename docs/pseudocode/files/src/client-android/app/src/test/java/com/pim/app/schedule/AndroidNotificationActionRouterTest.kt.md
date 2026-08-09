# src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.schedule
- 职责：验证 PimNotificationRouter 低/高风险路由，以及 EndpointNotificationActionDispatcher 调用契约。
- 主要依赖：PimNotificationRouter、NotificationRoute、EndpointNotificationActionDispatcher、DTO
- 被谁使用：Android 单元测试套件

## 函数级结构化伪代码

### AndroidNotificationActionRouterTest
#### lowRiskActionCanExecuteDirectly
- 输入：route("dismiss", "L1LowRiskAction")
- 输出：ExecuteOnline
- 调用：PimNotificationRouter.route

#### highRiskActionOpensDetail
- 输入：confirm + L3 + confirmationId
- 输出：OpenDetail，detailUrl=/confirmations/{id}
- 调用：PimNotificationRouter.route

#### lowRiskDispatcherCallsEndpointNotificationActionApi
- 输入：注入捕获 sender 的 dispatcher.execute
- 输出：deviceId/request 字段与 response.result 断言
- 副作用：runBlocking
- 调用：EndpointNotificationActionDispatcher.execute

## 近逐行中文伪代码

1. [L16-20] 低风险 → ExecuteOnline。
2. [L22-32] 高风险 → OpenDetail URL。
3. [L34-62] dispatcher 捕获 deviceId 与 DTO 字段，返回 Executed。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt",
      "label": "AndroidNotificationActionRouterTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt",
      "type": "tests"
    }
  ]
}
```
