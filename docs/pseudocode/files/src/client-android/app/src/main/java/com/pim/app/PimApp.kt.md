# src/client-android/app/src/main/java/com/pim/app/PimApp.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app
- 职责：Hilt Application 入口；配置 WorkManager 自定义 WorkerFactory；进程启动时清理旧 Work 并确保周期同步任务。
- 主要依赖：HiltAndroidApp、PimWorkerFactory、MobileSyncScheduler、WorkManager Configuration
- 被谁使用：AndroidManifest Application 名

## 函数级结构化伪代码

### PimApp
#### onCreate()
- 输入：无
- 输出：无
- 副作用：取消旧 Work；注册/确保周期 MobileSync
- 步骤：
  1. super.onCreate()
  2. mobileSyncScheduler.cancelOldWork()
  3. mobileSyncScheduler.ensurePeriodic()
- 分支与异常：依赖 Hilt 已注入 workerFactory / mobileSyncScheduler
- 调用：MobileSyncScheduler.cancelOldWork、ensurePeriodic

#### workManagerConfiguration (getter)
- 输入：无
- 输出：Configuration
- 副作用：无
- 步骤：
  1. Configuration.Builder().setWorkerFactory(workerFactory).build()
- 分支与异常：无
- 调用：PimWorkerFactory

## 近逐行中文伪代码

1. @HiltAndroidApp 标记 Application。
2. 实现 Configuration.Provider 以注入自定义 WorkerFactory。
3. @Inject lateinit workerFactory、mobileSyncScheduler。
4. onCreate：超类初始化后 cancelOldWork + ensurePeriodic。
5. workManagerConfiguration：Builder 设置 workerFactory 并 build。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/PimApp.kt",
      "label": "PimApp",
      "path": "src/client-android/app/src/main/java/com/pim/app/PimApp.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/PimApp.kt.md",
      "layer": "client-android",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/PimApp.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/PimApp.kt", "to": "src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt", "type": "depends_on" }
  ]
}
```
