# src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.location（client-android app 测试）
- 职责：覆盖定位捕获仓库相关纯函数：提交状态文案、`enqueueThenSchedule` 契约、`resolveAutoSubmittedState`。
- 主要依赖：`formatSubmitStatus`、`enqueueThenSchedule`、`resolveAutoSubmittedState`（同包生产代码）、JUnit、Robolectric、coroutines-test
- 被谁使用：测试运行器

## 函数级结构化伪代码

### LocationCaptureRepositoryTest
#### enqueueSuccessShowsQueuedMessage / enqueueFailure*
- 输入：enqueued 与 error
- 输出：断言文案
- 副作用：无
- 步骤：
  1. 成功 → 「已加入上传队列」
  2. 失败+error → 「加入上传队列失败：{error}」
  3. 失败+null → 「…未知错误」
- 分支与异常：无
- 调用：`formatSubmitStatus`

#### enqueueThenSchedule 三用例
- 输入：mock enqueue/schedule
- 输出：Result 与调用次数
- 副作用：无
- 步骤：
  1. 成功：enqueue+schedule 各一次，isSuccess
  2. enqueue 抛错：不 schedule，isFailure
  3. CancellationException 原样抛出
- 分支与异常：取消异常不可吞
- 调用：`enqueueThenSchedule`

#### resolveAutoSubmittedState 七用例
- 输入：current / isAutoSubmit / success
- 输出：布尔
- 副作用：无
- 步骤：仅「自动提交且成功」从 false→true；current 已 true 则保持 true；手动或失败不置 true
- 分支与异常：无
- 调用：`resolveAutoSubmittedState`

## 近逐行中文伪代码

1. [L1-12] 测试包与断言/Robolectric 导入
2. [L13-32] 三类 `formatSubmitStatus` 文案断言
3. [L37-77] `enqueueThenSchedule`：成功、失败不调度、取消重抛
4. [L82-114] `resolveAutoSubmittedState` 真值表覆盖

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt",
      "label": "LocationCaptureRepositoryTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt", "type": "tests" }
  ]
}
```
