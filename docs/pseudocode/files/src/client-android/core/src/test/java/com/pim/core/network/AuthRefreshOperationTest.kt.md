# src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt

## 元信息
- 语言：Kotlin (JUnit + MockWebServer)
- 程序集或包：client-android-core test / com.pim.core.network
- 职责：验证 refresh 传输与 `RetrofitAuthRefreshOperation`/`AuthRefreshCoordinator`：401 可检查、拒绝清会话、成功轮换、5xx/超时/取消不 clear、非法 payload 拒绝、并发切服时 pin 到捕获服务器。
- 主要依赖：`ApiService`、`RetrofitAuthRefreshOperation`、`AuthRefreshCoordinator`、MockWebServer
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AuthRefreshOperationTest
#### setUp / tearDown
- 步骤：启动/关闭 MockWebServer

#### refreshHttp401IsReturnedWithoutThrowingHttpException()
- 步骤：enqueue 401 → refresh 不抛 → Response code=401 → 路径 `/api/v1/auth/refresh` 且无 Authorization 头

#### realRefreshOperationMapsHttp401ToRejectedAndCoordinatorClears()
- 步骤：401 → refreshAfterUnauthorized 返回 false 且 clearCalls=1

#### realRefreshOperationReturnsValidatedRotatedSession()
- 步骤：200 合法 JSON → Success tokens 与 expires 解析正确

#### serverFailureRemainsAnHttpException()
- 步骤：503 → 抛 HttpException(503)，clearCalls=0

#### transportFailureIsPropagated()
- 步骤：拦截器抛 SocketTimeoutException → 上抛，不 clear

#### cancellationIsPropagated()
- 步骤：refreshCall 抛 CancellationException → 上抛，不 clear

#### invalidRefreshPayloadsAreRejectedBeforeSessionCommit()
- 步骤：空 access/refresh、非法/过期 expires → 均为 Rejected

#### capturedServerIdentityPinsRefreshAcrossConcurrentSettingsSwitch()
- 步骤：refresh 进入后模拟设置切到 B；仍只打 serverA，serverB 请求数 0

#### apiService / RecordingSessionStore
- 步骤：Retrofit 绑 mock baseUrl；内存会话实现 AuthSessionStore 记录 clear

## 近逐行中文伪代码

1. [L33-L45] 测试类 + MockWebServer 生命周期
2. [L47-L63] 401 可检查且无 Authorization
3. [L65-L82] 401 → coordinator 清会话
4. [L84-L107] 200 成功轮换 token
5. [L109-L143] 503/超时不 clear
6. [L145-L157] 取消传播
7. [L159-L186] 非法 payload → Rejected
8. [L188-L249] 并发切服 pin serverA
9. [L251-L295] 辅助 apiService 与 RecordingSessionStore

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt",
      "label": "AuthRefreshOperationTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/network/AuthRefreshOperationTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "type": "tests"
    }
  ]
}
```
