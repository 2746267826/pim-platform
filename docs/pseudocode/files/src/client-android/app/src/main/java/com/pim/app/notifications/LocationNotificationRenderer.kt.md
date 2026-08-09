# src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.notifications
- 职责：根据定位策略与队列状态渲染前台持续定位通知（折叠/展开文案、渠道、操作按钮）。
- 主要依赖：`LocationPolicyMode`、`ForegroundLocationController`、`NotificationActionReceiver`、`MainActivity`、Android Notification API
- 被谁使用：前台定位服务在更新通知时调用 `build` / 文案方法

## 函数级结构化伪代码

### LocationNotificationState
#### data class LocationNotificationState(...)
- 输入：mode、nextExpectedLocationText、lastAcceptedLocationText、lastAccuracyText、pendingUploadCount、apiState、lastDroppedReason
- 输出：不可变状态快照
- 副作用：无
- 步骤：
  1. 聚合通知渲染所需的定位策略与队列/API 文案字段
- 分支与异常：无
- 调用：无

### LocationNotificationRenderer
#### collapsedText(state: LocationNotificationState): String
- 输入：state
- 输出：单行折叠通知正文
- 副作用：无
- 步骤：
  1. 组装列表：策略标签、下次定位文案、精度、待上传数、API 状态
  2. 用 ` · ` 连接并返回
- 分支与异常：无
- 调用：`modeLabel`

#### expandedText(state: LocationNotificationState): String
- 输入：state
- 输出：多行展开正文
- 副作用：无
- 步骤：
  1. 构建多行：策略、下次定位、最近位置+精度、待上传+API
  2. 若 `lastDroppedReason` 非空则追加「最近丢弃」行
  3. 用换行连接
- 分支与异常：`lastDroppedReason` 可空
- 调用：`modeLabel`

#### build(context: Context, state: LocationNotificationState): Notification
- 输入：context、state
- 输出：`Notification` 实例
- 副作用：确保通知渠道存在
- 步骤：
  1. `ensureChannel(context)`
  2. 用 `NotificationCompat.Builder` 设置图标、标题「PIM 持续定位」、折叠文案、BigText 展开样式
  3. `setOngoing(true)`、`setOnlyAlertOnce(true)`
  4. 点击打开状态页 PendingIntent
  5. 添加操作：暂停 / 同步 / 状态（广播到 `NotificationActionReceiver`）
  6. `build()` 返回
- 分支与异常：无
- 调用：`ensureChannel`、`collapsedText`、`expandedText`、`openStatusPendingIntent`、`receiverPendingIntent`

#### modeLabel(mode: LocationPolicyMode): String
- 输入：mode
- 输出：中文策略标签
- 副作用：无
- 步骤：
  1. when 映射 Off/省电/日程低频/运动观察/移动恢复/同步兜底
- 分支与异常：穷尽 `LocationPolicyMode`
- 调用：无

#### ensureChannel(context: Context)
- 输入：context
- 输出：Unit
- 副作用：创建通知渠道（O+）
- 步骤：
  1. SDK < O 则直接返回
  2. 创建 IMPORTANCE_LOW 渠道 `pim_location_collection`
  3. `createNotificationChannel`
- 分支与异常：API 级别分支
- 调用：`NotificationManager.createNotificationChannel`

#### openStatusPendingIntent(context: Context): PendingIntent
- 输入：context
- 输出：打开 MainActivity 并带 status 目的地的 Activity PendingIntent
- 副作用：无（构造 Intent）
- 步骤：
  1. Intent → MainActivity，extra `EXTRA_OPEN_DESTINATION=status`
  2. `getActivity` requestCode=20
- 分支与异常：无
- 调用：`pendingIntentFlags`

#### receiverPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent
- 输入：context、action、requestCode
- 输出：广播 PendingIntent
- 副作用：无
- 步骤：
  1. Intent → NotificationActionReceiver，setAction(action)
  2. `getBroadcast`
- 分支与异常：无
- 调用：`pendingIntentFlags`

#### pendingIntentFlags(): Int
- 输入：无
- 输出：FLAG_UPDATE_CURRENT | (M+ 时 FLAG_IMMUTABLE)
- 副作用：无
- 步骤：按 SDK 拼 flags
- 分支与异常：API 级别
- 调用：无

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.notifications`
2. [L3-L13] 导入通知 API、MainActivity、LocationPolicyMode、ForegroundLocationController
3. [L15-L23] 定义数据类 `LocationNotificationState`：策略、下次/最近位置文案、精度、待上传数、API 状态、最近丢弃原因
4. [L25] 单例 `LocationNotificationRenderer`
5. [L26-L27] 常量 CHANNEL_ID=`pim_location_collection`，NOTIFICATION_ID=7101
6. [L29-L37] `collapsedText`：策略标签 · 下次定位 · 精度 · 待上传 · API 状态
7. [L39-L47] `expandedText`：多行策略/下次/最近位置/待上传；有丢弃原因则追加
8. [L49-L63] `build`：确保渠道 → Builder 设图标标题折叠/展开 → 常驻且只响一次 → 内容点击与三按钮
9. [L65-L72] `modeLabel`：各 `LocationPolicyMode` 映射中文
10. [L74-L85] `ensureChannel`：O 以下跳过；否则 LOW 重要性渠道并注册
11. [L87-L91] `openStatusPendingIntent`：MainActivity + status 目的地
12. [L93-L96] `receiverPendingIntent`：广播到 NotificationActionReceiver
13. [L98-L101] `pendingIntentFlags`：UPDATE_CURRENT，M+ 加 IMMUTABLE

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "label": "LocationNotificationRenderer",
      "path": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt",
      "type": "depends_on"
    }
  ]
}
```
