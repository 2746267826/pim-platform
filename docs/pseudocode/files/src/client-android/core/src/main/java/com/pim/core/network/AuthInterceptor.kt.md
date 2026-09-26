# src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.network
- 职责：OkHttp 拦截器：按 AuthMode 附加 Bearer；过期预刷新；401 后刷新重试。
- 主要依赖：`AuthSessionStore`、`AuthRefreshCoordinator`、`PimServerEndpoints`、`AuthMode`
- 被谁使用：OkHttpClient 配置 / DI

## 函数级结构化伪代码

### AuthInterceptor
#### intercept(chain): Response
- 输入：Interceptor.Chain
- 输出：Response
- 副作用：可能阻塞刷新 token；读 session；二次 proceed
- 步骤：
  1. 读 request tag `AuthMode`，默认 Required
  2. Anonymous：去掉 Authorization 后 proceed 并返回
  3. 由 URL 得 `trustedOrigin` serverIdentity
  4. `runBlocking { refreshIfExpired(identity) }`
  5. 取 accessToken，带 Bearer proceed
  6. 非 401 直接返回
  7. 401：用空 body 克隆 unauthorized 响应并 close 原 body
  8. `refreshAfterUnauthorized(oldToken, identity)`；失败返回 unauthorized
  9. 成功则 close unauthorized，用新 token 再 proceed
- 分支与异常：401 分支；刷新失败返回 401 空体
- 调用：`refreshCoordinator`、`sessionStore`、`withAccessToken`

#### withAccessToken(accessToken): Request
- 步骤：remove Authorization；token 非空则 `Bearer`
- 调用：`newBuilder`

#### nonblank(): String?
- 步骤：blank → null
- 调用：`takeIf`

## 近逐行中文伪代码

1. [L11-14] 构造注入 sessionStore 与 refreshCoordinator
2. [L15-24] Anonymous 短路
3. [L26-31] 预刷新 + 首次请求
4. [L32] 非 401 返回
5. [L34-41] 401 空体 + refreshAfterUnauthorized
6. [L41-47] 刷新成功后二次请求
7. [L50-59] withAccessToken / nonblank 辅助
8. [L61-63] AUTHORIZATION 常量

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "label": "AuthInterceptor",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt.md",
      "layer": "client-android",
      "kind": "middleware"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    }
  ]
}
```
