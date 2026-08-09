# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.mobile.sync
- 职责：Robolectric + WorkManager 测试 MobileSyncScheduler 网络约束、inputData、退避、唯一任务与 ExistingWorkPolicy。
- 主要依赖：WorkManagerTestInitHelper、TrackingSettingsStore、MobileSyncScheduler、Robolectric、TestPimApp
- 被谁使用：测试运行器

## 函数级结构化伪代码

### MobileSyncSchedulerTest
#### setup
- 初始化 Test WorkManager；清空 scheduler_test SharedPreferences

#### 网络与 inputData 用例
- unmetered false/true → CONNECTED/UNMETERED（周期）
- buildImmediateInputData true/false 读写 allow_metered_once
- 立即网络：unmetered+override、unmetered 无 override、非 unmetered 任意 override

#### 唯一任务用例
- ensurePeriodic 三次 → ENQUEUED 仅 1
- enqueueNow 三次 → ENQUEUED 仅 1

#### 请求构建用例
- 周期：15 分钟间隔、网络约束、指数退避 30s
- 立即：网络约束、指数退避 30s
- resolveExistingWorkPolicy：false→KEEP，true→REPLACE

## 近逐行中文伪代码

1. Robolectric sdk=34、TestPimApp。
2. Before：WorkManager 测试初始化与 prefs 清空。
3. 断言周期/立即网络类型与 allow_metered_once 数据。
4. 连续 ensurePeriodic/enqueueNow 仅保留一个 ENQUEUED。
5. 断言 15 分钟周期、UNMETERED 约束、指数 30s 退避。
6. 断言 KEEP/REPLACE 策略映射。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt",
      "label": "MobileSyncSchedulerTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncSchedulerTest.kt", "to": "src/client-android/app/src/test/java/com/pim/app/TestPimApp.kt", "type": "depends_on" }
  ]
}
```
