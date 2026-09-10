# src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (core)
- 职责：实现 `AuthRefreshOperation`：经 Retrofit 调用刷新接口，校验 HTTP/业务码/令牌/过期时间后返回 `AuthRefreshResult`。
- 主要依赖：`AuthRefreshOperation`、`AuthRefreshResult`、`AuthTokens`、`RefreshRequest`、`AuthResponse`、`ApiResponse`、Retrofit `Response`/`HttpException`、`java.time.Instant`
- 被谁使用：认证拦截器 / Token 刷新管线

## 函数级结构化伪代码

### RetrofitAuthRefreshOperation
#### refresh(refreshToken: String, serverIdentity: String): AuthRefreshResult
- 输入：刷新令牌、服务器身份
- 输出：`Success(AuthTokens)` 或 `Rejected`；非 401 失败抛 `HttpException`
- 副作用：网络 refresh 调用
- 步骤：
  1. `refreshCall(serverIdentity, RefreshRequest(refreshToken))`
  2. HTTP 401 → `Rejected`
  3. 非 successful → throw `HttpException`
  4. body 空或 data 空或 `envelope.code != 0` → `Rejected`
  5. 解析 `expiresAt` 为 epoch 毫秒，失败 → `Rejected`
  6. access/refresh 空白或已过期 → `Rejected`
  7. 返回 `Success(AuthTokens(...))`
- 分支与异常：401 与业务拒绝；其它 HTTP 异常；Instant 解析失败
- 调用：`refreshCall`、`Instant.parse`、`nowMillis`

## 近逐行中文伪代码

1. [L13-19] 类持有挂起 `refreshCall` 与 `nowMillis` 时钟
2. [L20-24] 调用 refreshCall 得到 Response
3. [L25-26] 401 Rejected；非成功抛 HttpException
4. [L28-30] 校验 envelope/data/code
5. [L31-33] 解析 expiresAt
6. [L34-40] 空白令牌或过期 → Rejected
7. [L42-44] Success 包装 AuthTokens

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt",
      "label": "RetrofitAuthRefreshOperation",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthRefreshOperation.kt", "type": "implements" },
    { "from": "src/client-android/core/src/main/java/com/pim/core/network/RetrofitAuthRefreshOperation.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt", "type": "depends_on" }
  ]
}
```
