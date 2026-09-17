# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.mobile.sync（client-android app）
- 职责：定义移动端同步结果枚举，将异常分类为 SUCCESS/RETRY/BLOCKED，并映射为 WorkManager `ListenableWorker.Result`。
- 主要依赖：`androidx.work.ListenableWorker`、`kotlinx.coroutines.CancellationException`、`retrofit2.HttpException`、`java.io.IOException` 及网络相关异常
- 被谁使用：移动同步 Worker / 测试 `MobileSyncOutcomeMappingTest`

## 函数级结构化伪代码

### MobileSyncOutcome
#### enum values
- 输入：无
- 输出：SUCCESS | RETRY | BLOCKED
- 副作用：无
- 步骤：
  1. 声明成功、可重试、不可重试（阻塞/失败）三种结局
- 分支与异常：无
- 调用：无

### MobileSyncErrorClassifier
#### classify(throwable: Throwable): MobileSyncOutcome
- 输入：`throwable` 同步过程中捕获的异常
- 输出：`MobileSyncOutcome`（RETRY 或 BLOCKED）；若为取消则重新抛出
- 副作用：对 `CancellationException` 重新抛出，不吞掉协程取消
- 步骤：
  1. 若 `throwable is CancellationException`，原样 `throw throwable`
  2. `when (throwable)` 分支：
     - `HttpException`：按 HTTP 状态码细分
       - 408、429、5xx → RETRY
       - 其它 4xx → BLOCKED
       - 其余码 → RETRY
     - 超时/连接/未知主机/Socket/IOException → RETRY
     - 其它异常 → BLOCKED
  3. 返回对应 outcome
- 分支与异常：取消异常向上抛；HTTP 与网络异常映射为 RETRY/BLOCKED
- 调用：`throwable.code()`（HttpException）

### mapOutcomeToWorkerResult
#### mapOutcomeToWorkerResult(outcome: MobileSyncOutcome): ListenableWorker.Result
- 输入：`outcome` 同步结局
- 输出：`Result.success()` / `Result.retry()` / `Result.failure()`
- 副作用：无
- 步骤：
  1. SUCCESS → `ListenableWorker.Result.success()`
  2. RETRY → `ListenableWorker.Result.retry()`
  3. BLOCKED → `ListenableWorker.Result.failure()`
- 分支与异常：`when (outcome)` 穷尽枚举
- 调用：`ListenableWorker.Result.success/retry/failure`

## 近逐行中文伪代码

1. [L1] 包声明 `com.pim.app.mobile.sync`
2. [L3-10] 导入 Work 结果、取消异常、HttpException、IO/网络异常类型
3. [L12-16] 枚举 `MobileSyncOutcome`：SUCCESS、RETRY、BLOCKED
4. [L18] 内部对象 `MobileSyncErrorClassifier`
5. [L19] 函数 `classify(throwable)`
6. [L20] 若为 `CancellationException` 则重新抛出
7. [L21] 按异常类型 `when`
8. [L22-27] HttpException：408/429/5xx→RETRY；其它4xx→BLOCKED；else→RETRY
9. [L29-31] SocketTimeout/Connect/UnknownHost/Socket/IOException → RETRY
10. [L32] 其它 → BLOCKED
11. [L37] 内部函数 `mapOutcomeToWorkerResult`
12. [L38-42] SUCCESS→success，RETRY→retry，BLOCKED→failure

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt",
      "label": "MobileSyncOutcome",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt", "to": "androidx.work.ListenableWorker", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt", "to": "retrofit2.HttpException", "type": "depends_on" }
  ]
}
```
