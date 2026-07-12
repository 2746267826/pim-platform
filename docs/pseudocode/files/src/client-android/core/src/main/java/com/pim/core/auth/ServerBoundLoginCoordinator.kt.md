# src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.auth
- 职责：服务器绑定登录：用当前 baseUrl 解析身份 → transport 登录 → 仅当服务器仍当前时提交 token。
- 主要依赖：`ServerSettingsStore`、`TokenManager`、`PimServerEndpoints`、`LoginRequest`/`AuthResponse`/`ApiResponse`
- 被谁使用：设置登录 UI、SettingsViewModel

## 函数级结构化伪代码

### ServerBoundLoginTransport
#### suspend login(serverIdentity, request): ApiResponse\<AuthResponse\>
- 输入：服务器身份与 LoginRequest
- 输出：API 包装
- 副作用：网络（实现侧）
- 步骤：1. 函数接口契约
- 分支与异常：实现侧
- 调用：实现方

### ServerBoundLoginResult
#### sealed Success / StaleServer / SessionSaveFailed / Failure(error)
- 输入：可选 Throwable
- 输出：登录结果代数类型
- 副作用：无
- 步骤：1. 区分成功、服务器已变、会话保存失败、其它失败
- 分支与异常：无
- 调用：无

### ServerBoundLoginCoordinator
#### login(username, password): ServerBoundLoginResult (suspend)
- 输入：用户名密码
- 输出：`ServerBoundLoginResult`
- 副作用：读 baseUrl；网络登录；可能写 token
- 步骤：
  1. `getBaseUrl()`；`PimServerEndpoints.from` 取 trustedOrigin，失败 → Failure
  2. `transport.login(identity, LoginRequest(trim username, password))`
  3. CancellationException 上抛；其它 Exception → Failure
  4. code!=0 或 data null → Failure(message)
  5. `commitSessionIfCurrentServer(identity) { tokenManager.saveTokens(...) }`
  6. 映射：Committed→Success；ServerChanged→StaleServer；SaveFailed→SessionSaveFailed；InvalidServer→Failure
- 分支与异常：取消上抛；网络/解析失败 Failure
- 调用：`serverSettingsStore`、`tokenManager.saveTokens`、`transport.login`

## 近逐行中文伪代码

1. [L13-18] 定义 `ServerBoundLoginTransport`
2. [L20-25] sealed 结果类型
3. [L27-32] 注入 store、tokenManager、transport
4. [L33-39] 读 URL 并解析 identity
5. [L40-49] 调 transport；捕获取消与异常
6. [L50-55] 校验 response code/data
7. [L57-73] commitSessionIfCurrentServer + saveTokens 映射结果

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "label": "ServerBoundLoginCoordinator",
      "path": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    }
  ]
}
```
