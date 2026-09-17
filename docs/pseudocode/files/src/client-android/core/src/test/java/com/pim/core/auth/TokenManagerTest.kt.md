# src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt

## 元信息
- 语言：Kotlin / JUnit
- 程序集或包：client-android core tests
- 职责：TokenManager 与 AuthRefreshCoordinator 并发/会话绑定/安全存储失败闭包行为测试；内含内存 SharedPreferences 假实现。
- 主要依赖：TokenManager、AuthRefreshCoordinator、AuthRefreshOperation、SecurePreferencesFactory
- 被谁使用：测试运行器

## 函数级结构化伪代码

### TokenManagerTest
#### invalidLoginTokensNeverOverwriteCurrentSession
- 已有有效会话；空 access/refresh、非法 expires、已过期 saveTokens 全 false，snapshot 不变

#### boundSessionCanOnlyBeReadForItsTrustedOrigin
- 按 serverUrl 绑定 identity；仅同源 getAccessTokenForServer 有值

#### legacyEncryptedSessionWithoutServerIdentityFailsClosed
- 旧 prefs 无 server_identity → access/refresh 均 null

#### inFlightRefreshCannotRestoreSessionAfterLogout
- 刷新进行中 clear()；完成后仍保持登出，refresh 返回 false

#### inFlightRefreshCannotOverwriteNewLoginSession
- 刷新中新登录；完成后保留新登录 tokens

#### rejectedOldRefreshCannotClearNewLoginSession
- 旧 refresh Rejected 时已新登录 → 不清新会话，协调器仍 true

#### concurrentRefreshIfExpiredSecondReturnsFalseWhenFirstRejectedAndCleared
- 并发 refreshIfExpired：仅一次网络；Rejected 清会话；两者 false

#### secureStorageOpenFailureFailsClosedWithoutTokens
- open 抛 SecureStorageUnavailableException → 无 token 且 error 回调含 initialization failed

#### saveFailureAfterSuccessfulInitClearsInMemoryState
- commit 失败的 prefs → save false 且内存 tokens null

#### clearFailureClearsInMemoryState
- clear commit 失败仍清空内存态

### 辅助类型
- FakeSecurePreferencesFactory、InMemorySharedPreferences、TrivialFailingSharedPreferences、CommittingSharedPreferences：模拟 prefs 成功/失败路径

## 近逐行中文伪代码

1. 无效 token 写入不覆盖现会话。
2. 会话绑定 trusted origin，跨服读 token 为 null。
3. 无 server_identity 的遗留会话失败闭包。
4. 飞行中 refresh 与 logout/新登录竞态：以最新会话为准，旧 refresh 不得复活/覆盖。
5. 并发过期刷新合并为单次调用。
6. 安全存储不可用或 commit 失败均 fail-closed。
7. 测试夹具实现完整 SharedPreferences 内存版与失败 Editor。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt",
      "label": "TokenManagerTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/auth/TokenManagerTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "type": "tests"
    }
  ]
}
```
