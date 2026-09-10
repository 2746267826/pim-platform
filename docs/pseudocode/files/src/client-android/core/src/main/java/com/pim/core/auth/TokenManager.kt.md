# src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.auth
- 职责：加密 SharedPreferences 持久化 access/refresh token 与 serverIdentity；实现 AuthSessionStore。
- 主要依赖：EncryptedSharedPreferences、MasterKeys、PimServerEndpoints、AuthSessionStore
- 被谁使用：AuthInterceptor、登录协调器、AuthRefreshCoordinator、TokenManagerTest

## 函数级结构化伪代码

### SecurePreferencesFactory / 异常类型
#### open(): SharedPreferences
- 输入：无
- 输出：SharedPreferences
- 副作用：打开加密存储
- 步骤：接口由 AndroidSecurePreferencesFactory 实现
- 分支：Master key 失败 → SecureStorageUnavailableException；打开失败 → SecureStorageCorruptionException

### AndroidSecurePreferencesFactory.open
- 输入：Context
- 输出：EncryptedSharedPreferences（pim_auth）
- 副作用：创建/打开加密 prefs
- 步骤：getOrCreate master key → EncryptedSharedPreferences.create AES256
- 调用：MasterKeys、EncryptedSharedPreferences

### TokenManager
#### init
- 输入：factory
- 输出：内存 snapshot
- 副作用：打开 prefs；失败则 prefs=null、snapshot 空，并 reportStorageError
- 步骤：try open + readSession；catch 降级
- 调用：readSession

#### saveTokens(access, refresh, expiresAt, serverUrl): Boolean
- 输入：ISO 过期时间字符串与 serverUrl
- 输出：是否保存成功
- 步骤：解析 Instant → expiresAtUtcMillis；解析 trustedOrigin；调 save
- 分支：解析失败返回 false
- 调用：PimServerEndpoints.from、save

#### snapshot / save / clear（AuthSessionStore）
- 输入：token 四元组或无
- 输出：AuthSessionSnapshot 或 Boolean
- 副作用：同步写 prefs；失败清空内存并 null prefs
- 步骤：
  1. save：校验 token 未过期；normalizeTrustedOrigin；commit 四字段；更新 currentSnapshot
  2. clear：edit().clear().commit；snapshot 置空
  3. 任一步 commit 失败或异常 → reportStorageError 并返回 false
- 调用：commit、normalizeTrustedOrigin

#### getAccessToken / getAccessTokenForServer / isExpiredForServer / isExpired
- 输入：可选 serverUrl
- 输出：token 或是否过期
- 步骤：委托 AuthSessionStore 扩展 accessToken / accessTokenForServerIdentity；比对 serverIdentity 与 now
- 调用：PimServerEndpoints.from、snapshot

#### SharedPreferences.readSession
- 输入：prefs
- 输出：StoredSession?
- 步骤：读 access/refresh/server_identity/expires_at；缺任一字段返回 null；normalize origin
- 调用：normalizeTrustedOrigin

#### AuthTokens.isValidAt
- 输入：now
- 输出：Boolean
- 步骤：非空 token 且 expiresAtUtcMillis > now

## 近逐行中文伪代码

1. [L11-24] 工厂接口与安全存储异常。
2. [L25-50] Android 工厂：MasterKeys + EncryptedSharedPreferences 名 pim_auth。
3. [L52-80] TokenManager 初始化：打开 prefs 读会话，失败降级。
4. [L82-96] saveTokens：解析过期时间与 trustedOrigin 后 save。
5. [L98-134] save：校验、归一化 identity、commit、更新 snapshot。
6. [L136-151] clear：清空 prefs 与内存。
7. [L153-174] 按服务器取 token / 判断过期。
8. [L176-196] readSession 与 isValidAt。
9. [L198-205] StoredSession 与 TAG。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "label": "TokenManager",
      "path": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt",
      "type": "implements"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    }
  ]
}
```
