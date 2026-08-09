# src/client-android/app/src/main/java/com/pim/app/MainActivity.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：主 Activity：Compose 根屏，按 extra 打开 Status/Today；onStart 触发同步。
- 主要依赖：PimRootScreen、ForegroundLocationController、MobileSyncScheduler
- 被谁使用：启动入口

## 函数级结构化伪代码

### MainActivity.onCreate
- extra OPEN_DESTINATION=status → Status 否则 Today；setContent 根屏

### onStart
- mobileSyncScheduler.enqueueNow()

## 近逐行中文伪代码

1. Hilt 注入调度器。
2. 解析初始目的地。
3. 每次 onStart 立即同步。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt",
      "label": "MainActivity",
      "path": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/MainActivity.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/MainActivity.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt",
      "type": "depends_on"
    }
  ]
}
`
