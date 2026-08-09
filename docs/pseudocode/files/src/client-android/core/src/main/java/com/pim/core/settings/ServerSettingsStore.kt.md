# src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.settings（client-android core）
- 职责：单例持久化服务器 Base URL；校验 URL；切换服务器时清理绑定到其它 origin 的会话；仅在当前服务器身份匹配时提交会话。
- 主要依赖：Android SharedPreferences、`AuthSessionStore`、`ServerUrlValidator`、`PimServerEndpoints`、Hilt
- 被谁使用：登录/设置/API 客户端配置

## 函数级结构化伪代码

### ServerSessionCommitResult
#### 枚举
- 输入：无
- 输出：Committed / ServerChanged / SaveFailed / InvalidServer
- 副作用：无
- 步骤：表达会话提交结果
- 分支与异常：无
- 调用：无

### ServerSettingsStore
#### getBaseUrl(): String
- 输入：prefs 中 KEY_SERVER_BASE_URL
- 输出：规范化 base URL（默认 DEFAULT_BASE_URL 空串）
- 副作用：读 SharedPreferences
- 步骤：`normalizeBaseUrl(prefs.getString(...))`
- 分支与异常：无
- 调用：`normalizeBaseUrl`

#### setBaseUrl(baseUrl: String): String
- 输入：用户输入 URL
- 输出：规范化后的 URL
- 副作用：写 prefs；可能清除旧服务器会话
- 步骤：
  1. `ServerUrlValidator.validate`；无效则 require 失败
  2. 取 normalizedUrl 与 `PimServerEndpoints.from(...).trustedOrigin`
  3. `invalidateSessionBoundToAnotherServer`
  4. prefs commit 写入 KEY_SERVER_BASE_URL；失败抛 IllegalStateException
  5. 返回 normalized
- 分支与异常：校验失败 / 持久化失败
- 调用：`ServerUrlValidator`、`PimServerEndpoints`、`authSessionStore`

#### saveSessionIfCurrentServer / commitSessionIfCurrentServer
- 输入：expectedServerIdentity、saveSession 回调
- 输出：Boolean 或 `ServerSessionCommitResult`
- 副作用：可能调用 saveSession 写会话
- 步骤：
  1. 规范化 expected identity；失败 → InvalidServer
  2. 从当前 getBaseUrl 推导 trustedOrigin；失败 → InvalidServer
  3. 身份不等 → ServerChanged
  4. saveSession() 真 → Committed 否则 SaveFailed
  5. saveSessionIfCurrentServer 仅当 Committed 返回 true
- 分支与异常：runCatching 吞解析异常映射为 InvalidServer
- 调用：`PimServerEndpoints.normalizeTrustedOrigin`、`from`、`saveSession`

#### invalidateSessionBoundToAnotherServer(serverIdentity)
- 输入：新服务器 identity
- 输出：Unit
- 副作用：必要时 clear AuthSessionStore
- 步骤：
  1. snapshot 当前会话
  2. 无 tokens 或 identity 已相同则 return
  3. clear 失败则抛 IllegalStateException
- 分支与异常：clear 失败
- 调用：`authSessionStore.snapshot/clear`

#### companion.normalizeBaseUrl
- 输入：可空字符串
- 输出：validator.normalizedUrl
- 副作用：无
- 步骤：`ServerUrlValidator.validate(value).normalizedUrl`
- 分支与异常：无
- 调用：`ServerUrlValidator.validate`

## 近逐行中文伪代码

1. [L1-8] 包与导入 Context/Prefs/AuthSessionStore/Hilt
2. [L10-15] 枚举 `ServerSessionCommitResult`
3. [L17-22] `@Singleton` 注入 Context 与 AuthSessionStore，打开 prefs
4. [L24-27] `getBaseUrl` 读并规范化
5. [L29-45] `setBaseUrl` 校验、切服清会话、commit
6. [L47-54] `saveSessionIfCurrentServer` 包装 commit 结果
7. [L56-75] `commitSessionIfCurrentServer` 身份比对后保存
8. [L77-83] 切服时清理旧会话
9. [L85-93] companion 常量与 normalizeBaseUrl

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "label": "ServerSettingsStore",
      "path": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "to": "com.pim.core.auth.AuthSessionStore", "type": "depends_on" },
    { "from": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "to": "com.pim.core.settings.ServerUrlValidator", "type": "calls" },
    { "from": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "to": "com.pim.core.settings.PimServerEndpoints", "type": "calls" }
  ]
}
```
