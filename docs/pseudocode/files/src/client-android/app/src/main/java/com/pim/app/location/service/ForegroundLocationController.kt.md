# src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：封装对 ForegroundLocationService 的 start/pause/sync 意图与打开状态页 Intent。
- 主要依赖：ContextCompat、ForegroundLocationService、MainActivity
- 被谁使用：UI/通知动作

## 函数级结构化伪代码

### ForegroundLocationController
- start → startForegroundService ACTION_START_COLLECTION
- stop → startService ACTION_PAUSE_COLLECTION
- syncNow → startForegroundService ACTION_SYNC_NOW
- openStatusIntent → MainActivity EXTRA_OPEN_DESTINATION=status NEW_TASK
- serviceIntent(action) 工厂
- companion 动作常量

## 近逐行中文伪代码

1. Hilt Singleton 注入 ApplicationContext。
2. 三个控制方法发服务 Intent。
3. 打开状态用 MainActivity extra。
4. 常量定义 START/PAUSE/RESUME/SYNC/OPEN 与 extra 名。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt",
      "label": "ForegroundLocationController",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "to": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt", "type": "depends_on" }
  ]
}
```
