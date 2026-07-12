# src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：`PimWorkerFactory`：见源文件职责（PimWorkerFactory.kt）。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### PimWorkerFactory
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L12 声明 `PimWorkerFactory`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L11] 注解 @Singleton
2. [L12] 定义类 `PimWorkerFactory`
3. [L13] 执行：private val mobileSyncWorkerFactory: MobileSyncWorker.Factory
4. [L14] 执行：) : WorkerFactory() {
5. [L16] 覆盖方法 `createWorker`
6. [L17] 执行：appContext: Context,
7. [L18] 执行：workerClassName: String,
8. [L19] 执行：workerParameters: WorkerParameters
9. [L20] 执行：): ListenableWorker? {
10. [L21] 返回 when (workerClassName) {
11. [L22] 分支臂：MobileSyncWorker::class.java.name ->
12. [L23] 执行：mobileSyncWorkerFactory.create(appContext, workerParameters)
13. [L24] when 默认 else

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt",
      "label": "PimWorkerFactory",
      "path": "src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt",
      "type": "depends_on"
    }
  ]
}
```
