# src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core tests / com.pim.core.network
- 职责：MockWebServer 验证 AuthInterceptor + AuthRefreshCoordinator：401 单次刷新重试、拒绝清会话、匿名/异源不带凭据、过期预刷、Forced 轮换、并发单飞。
- 主要依赖：AuthInterceptor、AuthRefreshCoordinator、FakeAuthSessionStore、MockWebServer、OkHttp
- 被谁使用：core 单元测试套件

## 函数级结构化伪代码

### AuthInterceptorTest（关键用例）
#### server401RefreshesOnceAndRetriesWithRotatedAccessToken
- 步骤：401 后 200；刷新 1 次；Authorization 从 token-a 变为 token-b

#### rejectedRefreshClearsOnceAndReturnsOriginal401WithoutRetry
- 步骤：刷新 Rejected → clear 1 次 → 仍 401 → 仅 1 次网络请求

#### first401BodyIsClosedBeforeRefreshIsAttempted
- 步骤：EventListener responseBodyEnd 后才进入 refresh

#### missingRefreshTokenClearsOnceAndReturnsOriginal401
- 步骤：无 refresh → 不调 refreshOperation；clear 一次

#### anonymousRequestOmitsAuthorizationAndNeverRefreshes
- 步骤：AuthMode.Anonymous 去掉 Authorization；不刷新

#### requestToDifferentOriginNeverSendsOrRefreshesBoundCredentials
- 步骤：store 绑定其它 origin → 不发 Authorization、不刷新

#### expiredSessionRefreshesBeforeFirstNetworkRequest
- 步骤：expires 过期 → 先刷新再请求，仅用新 token

#### forcedRefreshRejectsSuccessWithoutAccessTokenRotation / RejectsExpiryOnlyChangeWithSameFailedAccessToken
- 步骤：Forced 刷新 access 未变 → false 并 clear

#### expiryRefreshRejectsNoOpSuccessWithExpiredSession / RequiresExpiryStrictlyAfterCurrentTime / AcceptsNonblankTokenWithFutureExpiry
- 步骤：过期刷新要求新 expires > now 且 token 有效

#### refreshIfExpiredReturnsFalseWhenSessionIsNull
- 步骤：空会话 false，不 clear

#### expiredRefreshWithInvalidPayloadDoesNotClearConcurrentNewLogin
- 步骤：刷新过程中会话变为新登录 → 接受新会话且不 clear

#### concurrentRefreshSeesNewerSessionAndSkipsProbe / concurrent401ResponsesCauseOneRefresh / concurrentExpiredRequestsRefreshOnceBeforeSending
- 步骤：Mutex 单飞；并发 401 只刷新一次；过期并发只预刷一次

#### second401IsReturnedWithoutAnotherRefreshOrRetry
- 步骤：刷新后仍 401 → 不再二次刷新

### 测试夹具
#### authenticatedClient / RecordingRefresh / FakeAuthSessionStore / ControlledConcurrentAuthSessionStore
- 输入：store、refresh 操作、时钟
- 输出：OkHttpClient 或假 store
- 步骤：绑定 serverIdentity；记录 refresh/clear；并发门闩控制 snapshot 时序
- 调用：AuthRefreshCoordinator、AuthInterceptor

## 近逐行中文伪代码

1. [L33-45] MockWebServer 启停。
2. [L47-178] 401 刷新/拒绝/关 body/无 refresh/匿名/异源/过期预刷。
3. [L180-280] Coordinator 单元：Forced 轮换、过期边界、空会话。
4. [L282-422] 并发：新登录保护、双 401 单刷、双过期单预刷。
5. [L424-440] 二次 401 不连环刷新。
6. [L442-590] 夹具：client 工厂、RecordingRefresh、Fake/Controlled store。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt",
      "label": "AuthInterceptorTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "type": "tests"
    }
  ]
}
```
