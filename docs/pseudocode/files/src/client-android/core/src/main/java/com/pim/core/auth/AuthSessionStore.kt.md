# src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.auth
- 职责：认证会话抽象：令牌快照、按服务器身份读写、刷新结果类型与刷新操作接口。
- 主要依赖：无（纯模型/接口）
- 被谁使用：`TokenManager`、登录协调器、Auth 拦截器/刷新管线

## 函数级结构化伪代码

### AuthMode
- `Required` | `Anonymous`

### AuthTokens / AuthSessionSnapshot
- tokens + 可选 serverIdentity

### AuthSessionStore（interface）
#### snapshot() / save(...) / clear()
- 读写/清空会话；save 需 serverIdentity

#### accessToken / refreshToken / expiresAtUtcMillis
- 默认从 snapshot 取

#### accessTokenForServerIdentity(serverIdentity)
- 仅当当前 session 的 serverIdentity 匹配时返回 accessToken

### AuthRefreshResult
- Success(tokens) | Rejected

### AuthRefreshOperation
- suspend `refresh(refreshToken, serverIdentity) -> AuthRefreshResult`

## 近逐行中文伪代码

1. 枚举认证模式。
2. 数据类描述 access/refresh/过期时间。
3. 快照绑定可选服务器身份。
4. 接口规定 snapshot/save/clear 与便捷 token 读取。
5. `accessTokenForServerIdentity` 防止跨服务器误用令牌。
6. 刷新结果密封类 + 函数式刷新操作接口。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt",
      "label": "AuthSessionStore",
      "path": "src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
