# src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app)
- 职责：通知动作 `BroadcastReceiver`：处理前台定位控制广播与端点通知动作（在线执行 / 打开详情 / 离线重试引导）。
- 主要依赖：`ForegroundLocationController`、`PimShellActivity`、`ApiClientProvider`、`PimNotificationRouter`、`EndpointNotificationActionDispatcher`、Hilt
- 被谁使用：AndroidManifest 注册；通知 PendingIntent 发送广播

## 函数级结构化伪代码

### NotificationActionReceiver
#### onReceive(context: Context, intent: Intent)
- 输入：`context` 应用上下文；`intent` 通知/控制动作
- 输出：无（副作用：停/启采集、同步、启动 Activity、HTTP 派发）
- 副作用：控制前台定位；异步调用 API；启动 shell Activity
- 步骤：
  1. 按 `intent.action` 匹配定位控制动作
  2. `ACTION_PAUSE_COLLECTION` → `foregroundLocationController.stop()` 并 return
  3. `ACTION_RESUME_COLLECTION` → `start()` 并 return
  4. `ACTION_SYNC_NOW` → `syncNow()` 并 return
  5. `ACTION_OPEN_STATUS` → `startActivity(openStatusIntent())` 并 return
  6. 否则读取 extras：`EXTRA_ACTION`、`EXTRA_RISK_LEVEL`、`EXTRA_CONFIRMATION_ID`、`EXTRA_RELATED_OBJECT_TYPE`、`EXTRA_RELATED_OBJECT_ID`、`EXTRA_ONLINE`
  7. 调用 `PimNotificationRouter().route(...)` 得到 `NotificationRoute`
  8. `ExecuteOnline`：`goAsync()` + IO 协程调用 `EndpointNotificationActionDispatcher.execute(...)`，finally `pending.finish()`
  9. `OpenDetail`：`PimShellActivity.intentFor(context, route.detailUrl)` + `FLAG_ACTIVITY_NEW_TASK`
  10. `RetryWhenOnline`：打开 `/endpoint-shell`
- 分支与异常：定位控制四分支优先；路由三分支；协程 try/finally 保证 finish
- 调用：`foregroundLocationController.*`、`PimNotificationRouter.route`、`EndpointNotificationActionDispatcher.execute`、`PimShellActivity.intentFor`

### companion object
#### notificationDeviceId(intent: Intent): String
- 输入：intent
- 输出：设备 ID 字符串
- 副作用：无
- 步骤：
  1. 读 `EXTRA_DEVICE_ID`
  2. 非空白则返回，否则默认 `"android-companion"`
- 分支与异常：blank 回退默认
- 调用：`getStringExtra`、`takeIf`

## 近逐行中文伪代码

1. [L1] package `com.pim.app.notifications`
2. [L15-16] `@AndroidEntryPoint` 类 `NotificationActionReceiver` 继承 `BroadcastReceiver`
3. [L17-18] 注入 `ApiClientProvider`、`ForegroundLocationController`
4. [L20] 覆盖 `onReceive`
5. [L21-38] when action：暂停/恢复采集、立即同步、打开状态页
6. [L40-51] 读取 action/risk/confirmation/related/online extras，调用 router
7. [L54-70] `ExecuteOnline`：goAsync + IO 协程 dispatch 端点动作
8. [L72-76] `OpenDetail`：启动 shell 打开 detailUrl
9. [L78-83] `RetryWhenOnline`：打开 endpoint-shell
10. [L87-99] companion 常量 extras 与 `notificationDeviceId` 默认值

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt",
      "label": "NotificationActionReceiver",
      "path": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt.md",
      "layer": "client-android",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt", "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt", "to": "src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt", "to": "src/client-android/app/src/main/java/com/pim/app/notifications/EndpointNotificationActionDispatcher.kt", "type": "calls" }
  ]
}
```
