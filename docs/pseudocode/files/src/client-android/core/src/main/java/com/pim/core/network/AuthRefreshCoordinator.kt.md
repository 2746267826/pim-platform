# src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.network
- 职责：单飞 Mutex 协调 token 刷新：过期预刷与 401 强制刷；校验 server 绑定与 access token 轮换。
- 主要依赖：AuthSessionStore、AuthRefreshOperation、AuthRefreshResult、AuthTokens
- 被谁使用：AuthInterceptor、AuthInterceptorTest

## 函数级结构化伪代码

### AuthRefreshCoordinator
#### refreshIfExpired(serverIdentity?: String): Boolean
- 输入：可选要求绑定的 serverIdentity
- 输出：会话是否有效（未过期或刷新成功）
- 副作用：可能调用 refresh；可能 clear
- 步骤：
  1. 无 token → false
  2. 与 required server 不绑定 → false
  3. 未过期 → true
  4. Mutex 内双重检查后 refreshLocked(Expiry)
- 调用：refreshLocked、isExpired、isBoundToRequiredServer

#### refreshAfterUnauthorized(failedAccessToken?, serverIdentity?): Boolean
- 输入：失败时的 access token、可选 serverIdentity
- 输出：是否获得可用新会话（access 必须轮换）
- 副作用：刷新或 clear
- 步骤：
  1. Forced 需求；绑定检查
  2. 若已有有效完成刷新（access 已变）→ true
  3. Mutex 内再检查后 refreshLocked(Forced)
- 调用：isValidCompletedRefresh、refreshLocked

#### refreshLocked(beforeRefresh, requirement, serverIdentity)
- 输入：刷新前快照、需求类型、server
- 输出：Boolean
- 步骤：
  1. 无 refreshToken → clear 后判定
  2. 无 serverIdentity → clearAndReject
  3. 重读若 refresh/server 已变 → 按新会话判定（保护并发登录）
  4. refreshOperation.refresh：
     - Rejected：若会话仍是旧 refresh 则 clearAndReject；否则接受新会话
     - Success：若会话已变则接受新会话；否则校验 tokens（Forced 要求 access 轮换）；save
- 分支：并发新登录不 clear
- 调用：refreshOperation.refresh、sessionStore.save/clear

#### isValidRefreshTokens / isValidSession / isExpired / isBoundToRequiredServer
- 输入：tokens、requirement、serverIdentity
- 输出：Boolean
- 步骤：
  - 有效会话：非空 access/refresh 且 expires > now
  - Forced：failedAccessToken 为空或与当前 access 不同
  - 绑定：无 token 或未要求 server 则 true；否则 identity 相等
- 调用：nowMillis

## 近逐行中文伪代码

1. [L11-16] 注入 sessionStore、refreshOperation、时钟；Mutex。
2. [L18-31] refreshIfExpired：观察 → 绑定 → 过期 → 锁内刷新。
3. [L33-48] refreshAfterUnauthorized：Forced 需求与锁内刷新。
4. [L50-99] refreshLocked：缺 refresh 清会话；并发会话变化跳过 clear；Rejected/Success 分支。
5. [L101-149] clearAndReject、完成判定、有效性/过期/绑定辅助。
6. [L151-156] nonblank 与 RefreshRequirement 密封类型。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "label": "AuthRefreshCoordinator",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthRefreshOperation",
      "type": "depends_on"
    }
  ]
}
```
