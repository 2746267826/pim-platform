# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.mobile.sync
- 职责：WorkManager 周期后台 Worker，在应用未前台打开时调用 `MobileSyncCoordinator.syncOnOpen()` 上传队列中的使用与定位数据。
- 主要依赖：CoroutineWorker、AssistedInject、MobileSyncCoordinator、MobileSyncErrorClassifier、mapOutcomeToWorkerResult
- 被谁使用：PimWorkerFactory / MobileSyncScheduler 调度

## 函数级结构化伪代码

### MobileSyncWorker
#### constructor(context, params, syncCoordinator)
- 输入：Assisted Context、WorkerParameters；注入 MobileSyncCoordinator
- 输出：Worker 实例
- 副作用：无
- 步骤：
  1. 继承 CoroutineWorker(context, params)
  2. 保存 syncCoordinator
- 分支与异常：无
- 调用：CoroutineWorker 构造

#### Factory.create(context, params)
- 输入：Context、WorkerParameters
- 输出：MobileSyncWorker
- 副作用：无
- 步骤：
  1. AssistedFactory 由 Hilt 生成实现
- 分支与异常：无
- 调用：无

#### doWork()
- 输入：无（挂起）
- 输出：ListenableWorker.Result
- 副作用：触发一次移动端同步（上传队列）
- 步骤：
  1. try：调用 syncCoordinator.syncOnOpen() 得到 state
  2. 将 state.outcome 映射为 Worker Result 并返回
  3. catch CancellationException：原样 rethrow（不吞取消）
  4. catch 其他 Exception：用 MobileSyncErrorClassifier.classify(ex) 得 outcome，再 mapOutcomeToWorkerResult
- 分支与异常：取消向上抛；业务/网络异常分类为 RETRY/BLOCKED 等
- 调用：MobileSyncCoordinator.syncOnOpen、MobileSyncErrorClassifier.classify、mapOutcomeToWorkerResult

## 近逐行中文伪代码

1. 包声明 mobile.sync。
2. 导入 Context、CoroutineWorker、WorkerParameters、Assisted 注入与 CancellationException。
3. 注释说明：周期 Worker 跑 syncOnOpen，替代旧 UploadWorker/废弃 stats 端点。
4. 类 MobileSyncWorker：AssistedInject 构造，持有 syncCoordinator。
5. 内部接口 Factory：AssistedFactory，create(context, params)。
6. 重写 doWork：
7.   尝试 syncOnOpen，outcome 映射为 Result。
8.   CancellationException 继续抛出。
9.   其他异常 classify 后再映射 Result。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt",
      "label": "MobileSyncWorker",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt", "type": "depends_on" }
  ]
}
```
