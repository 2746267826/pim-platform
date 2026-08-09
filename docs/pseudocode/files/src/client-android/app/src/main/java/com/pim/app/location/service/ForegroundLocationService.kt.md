# src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：前台定位 Service：策略引擎调度间隔、质量门、入队、通知与手动同步。
- 主要依赖：LocationQueueRepository、MotionSignalRepository、LocationPolicyEngine、AltitudeWaitCoordinator、MobileSyncScheduler、TrackingSettingsStore、ScheduleWindowRepository、LocationNotificationRenderer
- 被谁使用：ForegroundLocationController 启动

## 函数级结构化伪代码

### onCreate / onStartCommand / onDestroy
- 初始化 LocationManager；按 ACTION 启动/暂停/同步；销毁停采集

### startCollection / stopCollection
- 权限失败关连续采集；否则加载设置/日程窗、注册 listener、前台通知
- stop 移除 updates

### 位置处理路径
- onLocationChanged → 策略输入（运动/日程/上次点）→ decide 间隔
- 质量协调器 accept → enqueueAccepted + 可选 sync；drop 记诊断
- 更新 runtime StateFlow 与通知文案

### runManualSync / publishRuntimeState
- enqueueNow；发布 isRunning/policy/pending 等

## 近逐行中文伪代码

1. Hilt 注入仓储与设置。
2. Intent 动作驱动采集生命周期。
3. 策略决定 requestInterval，动态 re-register。
4. 接受点写队列 JSON；拒绝写 dropped。
5. 通知栏反映状态；START_STICKY 保活。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt",
      "label": "ForegroundLocationService",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "type": "depends_on" }
  ]
}
```
