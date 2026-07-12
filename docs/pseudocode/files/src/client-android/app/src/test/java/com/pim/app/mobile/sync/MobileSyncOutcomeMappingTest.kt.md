# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.mobile.sync（client-android app test）
- 职责：覆盖同步 outcome→Worker Result、异常分类、MobileSyncState.merge 优先级与计数、LocationUploadStatusUpdates、ingest toState。
- 主要依赖：`MobileSyncOutcome`/`MobileSyncErrorClassifier`/`mapOutcomeToWorkerResult`、`MobileSyncState`、`LocationUploadStatusUpdates`、`MobileIngestResponse`、Robolectric、FakeHttpException
- 被谁使用：Robolectric JUnit 运行器

## 函数级结构化伪代码

### MobileSyncOutcomeMappingTest
#### mapOutcomeToWorkerResult 三用例
- 输入：SUCCESS/RETRY/BLOCKED
- 输出：分别断言 Result.Success / Retry / Failure
- 副作用：无
- 步骤：调用 `mapOutcomeToWorkerResult` 并 `assertTrue(result is ...)`
- 分支与异常：无
- 调用：`mapOutcomeToWorkerResult`

#### classify 系列
- 输入：CancellationException 及各类网络/HTTP 异常
- 输出：取消应 rethrow；网络与 408/429/5xx→RETRY；401/403/400/404→BLOCKED
- 副作用：无
- 步骤：
  1. Cancellation 用 `@Test(expected=...)`
  2. 网络异常直接 classify 断言 RETRY
  3. `FakeHttpException(code)` 模拟 HttpException
- 分支与异常：见 classify 规则
- 调用：`MobileSyncErrorClassifier.classify`

#### MobileSyncState.merge 优先级与计数
- 输入：两份 state 不同 outcome/计数/batch 字段
- 输出：RETRY 优先于 BLOCKED/SUCCESS；BLOCKED 优先于 SUCCESS；计数相加；lastBatch* 取 other
- 副作用：无
- 步骤：构造 a/b，调用 `a.merge(b)` 断言
- 分支与异常：无
- 调用：`MobileSyncState.merge`

#### LocationUploadStatusUpdates / toState
- 输入：带 perItemErrors 的 updates；`MobileIngestResponse` 不同 failed/rejected 计数
- 输出：retryableFirstError 取首个可重试错误；failedCount>0→RETRY，仅 rejected→SUCCESS
- 副作用：无
- 步骤：调用 `retryableFirstError` / `toState` 断言 outcome
- 分支与异常：无
- 调用：`LocationUploadStatusUpdates.retryableFirstError`、`MobileIngestResponse.toState`

### FakeHttpException
#### createFakeResponse / 构造
- 输入：HTTP code
- 输出：可 classify 的 HttpException 子类
- 副作用：无
- 步骤：`Response.error(code, empty body)` 包装
- 分支与异常：无
- 调用：okhttp3/retrofit Response API

## 近逐行中文伪代码

1. [L1-17] 包与导入 Work Result、异常、Robolectric、JUnit
2. [L18-39] SUCCESS/RETRY/BLOCKED 映射 Worker Result
3. [L43-71] Cancellation rethrow + 网络异常 RETRY
4. [L75-121] FakeHttpException 覆盖 408/429/5xx RETRY 与 4xx BLOCKED
5. [L125-165] merge 优先级：RETRY > BLOCKED > SUCCESS
6. [L169-204] merge 累加 accepted/skipped/rejected/failed；保留 other 的 batch 字段
7. [L208-232] retryableFirstError 有/无重试项
8. [L234-276] toState：failed→RETRY；仅 rejected 或全零/skipped→SUCCESS
9. [L279-285] FakeHttpException 内部辅助类

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt",
      "label": "MobileSyncOutcomeMappingTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncOutcome.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncOutcomeMappingTest.kt", "to": "com.pim.core.models.MobileIngestResponse", "type": "tests" }
  ]
}
```
