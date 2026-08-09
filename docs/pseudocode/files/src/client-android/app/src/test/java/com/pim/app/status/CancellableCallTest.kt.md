# src/client-android/app/src/test/java/com/pim/app/status/CancellableCallTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.status（client-android app test）
- 职责：验证 OkHttp `Call.awaitCancellableResponse` 在协程取消时会 cancel 底层 Call，且迟到的 Response body 会被关闭。
- 主要依赖：kotlinx-coroutines-test、okhttp3 Call/Response、被测扩展 `awaitCancellableResponse`
- 被谁使用：JUnit 测试运行器

## 函数级结构化伪代码

### CancellableCallTest
#### cancellingAwaitCancelsTheUnderlyingCall()
- 输入：可控 `ControllableCall`
- 输出：断言 `call.cancelled == true`
- 副作用：启动 async 等待响应后 cancelAndJoin
- 步骤：
  1. `async { call.awaitCancellableResponse() }`
  2. `runCurrent()` 让 enqueue 发生
  3. `awaiting.cancelAndJoin()`
  4. 断言底层 cancel 被调用
- 分支与异常：无
- 调用：`awaitCancellableResponse`、`cancelAndJoin`

#### responseArrivingAfterCancellationIsClosed()
- 输入：可控 Call + TrackingResponseBody
- 输出：断言 body.closed
- 副作用：取消后再 `call.respond(response)`
- 步骤：
  1. async 等待可取消响应
  2. 构造 200 Response 带 TrackingResponseBody
  3. cancelAndJoin 后 respond
  4. 断言 body 已 close
- 分支与异常：无
- 调用：`respond`、`awaitCancellableResponse`

### ControllableCall
#### Call 接口桩
- 输入：enqueue 回调、respond 注入响应
- 输出：模拟异步 Call
- 副作用：记录 cancelled、持有 Callback
- 步骤：
  1. enqueue 保存 callback 并 countDown latch
  2. cancel 置 AtomicBoolean
  3. respond 等待 latch 后 onResponse
- 分支与异常：execute 直接 error（不期望同步）
- 调用：okhttp Callback

### TrackingResponseBody
#### close 跟踪
- 输入：UTF-8 "body"
- 输出：关闭时 closed=true 的 ResponseBody
- 副作用：ForwardingSource.close 打标
- 步骤：包装 Buffer source，close 时 set closed
- 分支与异常：无
- 调用：okio ForwardingSource

## 近逐行中文伪代码

1. [L1-25] 包与导入 coroutines-test、okhttp、okio、JUnit
2. [L26-36] 测试：取消 await 会 cancel 底层 Call
3. [L38-56] 测试：取消后到达的 Response body 会被关闭
4. [L59-83] ControllableCall：enqueue/cancel/respond 桩
5. [L85-97] TrackingResponseBody：跟踪 close

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/status/CancellableCallTest.kt",
      "label": "CancellableCallTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/status/CancellableCallTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/status/CancellableCallTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/CancellableCallTest.kt", "to": "okhttp3.Call", "type": "tests" },
    { "from": "CancellableCallTest", "to": "awaitCancellableResponse", "type": "tests" }
  ]
}
```
