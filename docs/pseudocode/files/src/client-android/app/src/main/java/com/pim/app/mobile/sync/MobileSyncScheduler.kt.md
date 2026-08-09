# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.mobile.sync
- 职责：基于 WorkManager 调度移动端同步：周期任务、立即一次任务、清理旧 unique work；网络约束随 TrackingSettings 变化。
- 主要依赖：WorkManager、TrackingSettingsStore、MobileSyncWorker、Hilt ApplicationContext
- 被谁使用：应用启动/设置变更处、立即同步入口；MobileSyncSchedulerTest

## 函数级结构化伪代码

### MobileSyncScheduler
#### ensurePeriodic()
- 输入：无（读 TrackingSettingsStore）
- 输出：无
- 副作用：enqueueUniquePeriodicWork(PERIODIC_NAME, UPDATE)
- 步骤：
  1. 读取 settings
  2. resolvePeriodicNetworkType
  3. buildPeriodicRequest
  4. WorkManager 以 UPDATE 策略入队唯一周期任务
- 调用：trackingSettingsStore.read、WorkManager.getInstance

#### enqueueNow(allowMeteredOnce = false)
- 输入：allowMeteredOnce
- 输出：无
- 副作用：enqueueUniqueWork(NOW_NAME, KEEP|REPLACE)
- 步骤：
  1. 读 settings → resolveImmediateNetworkType
  2. buildImmediateRequest（含 inputData allow_metered_once）
  3. resolveExistingWorkPolicy：计量网覆盖时 REPLACE，否则 KEEP
  4. 入队唯一一次性任务
- 分支：allowMeteredOnce 影响网络类型与 ExistingWorkPolicy

#### cancelOldWork()
- 输入：无
- 输出：无
- 副作用：取消历史 unique work 名（pim_upload / pim_location_upload / pim_mobile_background_sync / pim_endpoint_upload）

### companion
#### resolvePeriodicNetworkType(settings)
- 若 syncOnUnmeteredOnly → UNMETERED，否则 CONNECTED

#### resolveImmediateNetworkType(settings, allowMeteredOnce)
- 仅当 unmetered-only 且未允许计量一次 → UNMETERED，否则 CONNECTED

#### buildImmediateInputData(allowMeteredOnce) → workDataOf("allow_metered_once")

#### buildPeriodicRequest(networkType)
- Constraints + PeriodicWorkRequestBuilder<MobileSyncWorker>(15 min) + 指数退避 30s

#### buildImmediateRequest(networkType, allowMeteredOnce)
- Constraints + OneTimeWorkRequestBuilder<MobileSyncWorker> + inputData + 指数退避 30s

#### resolveExistingWorkPolicy(allowMeteredOnce)
- true → REPLACE；false → KEEP

## 近逐行中文伪代码

1. 声明 @Singleton MobileSyncScheduler，注入 Context 与 TrackingSettingsStore。
2. ensurePeriodic：读设置 → 解析周期网络类型 → 构建 15 分钟周期请求 → UPDATE 入队 PERIODIC_NAME。
3. enqueueNow：读设置 → 解析立即网络类型 → 构建一次性请求与 inputData → 按 allowMeteredOnce 选 KEEP/REPLACE → 入队 NOW_NAME。
4. cancelOldWork：逐一 cancelUniqueWork 四个旧任务名。
5. companion 常量 PERIODIC_NAME / NOW_NAME。
6. resolvePeriodicNetworkType：syncOnUnmeteredOnly 映射 UNMETERED/CONNECTED。
7. resolveImmediateNetworkType：unmetered 且不允许计量一次才 UNMETERED。
8. buildImmediateInputData：布尔键 allow_metered_once。
9. buildPeriodicRequest：网络约束、15 分钟间隔、指数退避 30 秒。
10. buildImmediateRequest：网络约束、inputData、指数退避 30 秒。
11. resolveExistingWorkPolicy：计量一次覆盖用 REPLACE，否则 KEEP。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt",
      "label": "MobileSyncScheduler",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt", "type": "depends_on" }
  ]
}
```
