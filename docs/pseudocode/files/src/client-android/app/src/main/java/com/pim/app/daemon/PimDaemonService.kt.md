# src/client-android/app/src/main/java/com/pim/app/daemon/PimDaemonService.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：服务 `PimDaemonService`：后台或系统服务逻辑。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### PimDaemonService
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L9 声明 `PimDaemonService`
- 分支与异常：无
- 调用：无

### onBind
#### onBind(intent: Intent?)
- 输入：intent: Intent?
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 覆盖方法 `onBind`
- 分支与异常：无显著分支
- 调用：onBind

### onCreate
#### onCreate(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 覆盖方法 `onCreate`
  2. 执行：super.onCreate()
  3. 执行：startForeground(NOTIFICATION_ID, buildNotification())
- 分支与异常：无显著分支
- 调用：onCreate、super.onCreate、startForeground、buildNotification

### onStartCommand
#### onStartCommand(intent: Intent?, flags: Int, startId: Int)
- 输入：intent: Intent?, flags: Int, startId: Int
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 覆盖方法 `onStartCommand`
  2. 返回 START_STICKY
- 分支与异常：无显著分支
- 调用：onStartCommand

### buildNotification
#### buildNotification(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun buildNotification(): Notification {
  2. 执行：val channelId = "pim_daemon"
  3. 若 (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 则
  4. 执行：val channel = NotificationChannel(channelId, "PIM 数据采集",
  5. 执行：NotificationManager.IMPORTANCE_LOW)
  6. 执行：getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  7. 执行：val pendingIntent = PendingIntent.getActivity(
  8. 执行：Intent(this, StatusActivity::class.java),
  9. 执行：PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  10. 返回 NotificationCompat.Builder(this, channelId)
  11. 执行：.setContentTitle("PIM 数据采集")
  12. 执行：.setContentText("采集运行中")
  13. 执行：.setSmallIcon(android.R.drawable.ic_menu_manage)
  14. 执行：.setOngoing(true)
  15. 执行：.setContentIntent(pendingIntent)
- 分支与异常：if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
- 调用：buildNotification、NotificationChannel、getSystemService、createNotificationChannel、PendingIntent.getActivity、Intent、NotificationCompat.Builder、setContentTitle、setContentText、setSmallIcon、setOngoing、setContentIntent、build

## 近逐行中文伪代码

1. [L9] 定义类 `PimDaemonService`
2. [L10] 覆盖方法 `onBind`
3. [L12] 覆盖方法 `onCreate`
4. [L13] 执行：super.onCreate()
5. [L14] 执行：startForeground(NOTIFICATION_ID, buildNotification())
6. [L17] 覆盖方法 `onStartCommand`
7. [L18] 返回 START_STICKY
8. [L21] 执行：private fun buildNotification(): Notification {
9. [L22] 执行：val channelId = "pim_daemon"
10. [L23] 若 (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 则
11. [L24] 执行：val channel = NotificationChannel(channelId, "PIM 数据采集",
12. [L25] 执行：NotificationManager.IMPORTANCE_LOW)
13. [L26] 执行：getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
14. [L29] 执行：val pendingIntent = PendingIntent.getActivity(
15. [L31] 执行：Intent(this, StatusActivity::class.java),
16. [L32] 执行：PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
17. [L35] 返回 NotificationCompat.Builder(this, channelId)
18. [L36] 执行：.setContentTitle("PIM 数据采集")
19. [L37] 执行：.setContentText("采集运行中")
20. [L38] 执行：.setSmallIcon(android.R.drawable.ic_menu_manage)
21. [L39] 执行：.setOngoing(true)
22. [L40] 执行：.setContentIntent(pendingIntent)
23. [L44] 执行：companion object {
24. [L45] 执行：const val NOTIFICATION_ID = 1001

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/daemon/PimDaemonService.kt",
      "label": "PimDaemonService",
      "path": "src/client-android/app/src/main/java/com/pim/app/daemon/PimDaemonService.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/daemon/PimDaemonService.kt.md",
      "layer": "client-android",
      "kind": "entrypoint"
    }
  ],
  "edges": []
}
```
