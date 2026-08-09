# src/client-web/src/auth/AuthContext.tsx

## 元信息
- 语言：TypeScript / React
- 程序集或包：client-web
- 职责：全局认证上下文：登录/注册/登出、token 持久化与失效监听、暴露 `useAuth`。
- 主要依赖：`../api/client`（load/set/clearTokens、onTokensChanged、apiPost）；`../types` Auth/ApiResponse
- 被谁使用：路由守卫、登录页、需要用户信息的组件

## 函数级结构化伪代码

### AuthProvider({ children })
- 输入：子树 ReactNode
- 输出：带 Provider 的 JSX
- 副作用：本地 state；token 读写；HTTP 登录/注册
- 步骤：
  1. `isAuth` 初始为 `loadTokens()` 布尔；`username` 初 null。
  2. effect：注册 `onTokensChanged` → 清空 auth 与 username；卸载时用空回调“清理”（见源码）。
  3. `login`：POST `/auth/login`；code≠0 或无 data → 返回 message；否则 setTokens、设用户名与 isAuth，返回 null。
  4. `register`：POST `/auth/register`（含 email/displayName）；成功路径同 login。
  5. `logout`：clearTokens；isAuth false；username null。
  6. Provider value：`isAuthenticated`、`username`、`login`、`register`、`logout`。
- 分支与异常：catch 返回 Error.message 或默认「登录/注册失败」
- 调用：`apiPost`、`setTokens`/`clearTokens`/`loadTokens`/`onTokensChanged`

### useAuth()
- 输入：无（须在 Provider 内）
- 输出：`AuthState`
- 副作用：无
- 步骤：`useContext(AuthContext)`。
- 分支与异常：Provider 外为 null! 未防御
- 调用：`useContext`

## 近逐行中文伪代码

1. 创建 AuthContext；定义 AuthState 接口。
2. AuthProvider：token 初始加载；监听 token 变更清空状态。
3. login → /auth/login，写 token 与用户名。
4. register → /auth/register，同样写 token。
5. logout 清 token 与状态。
6. 导出 useAuth 读上下文。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/auth/AuthContext.tsx",
      "label": "AuthContext",
      "path": "src/client-web/src/auth/AuthContext.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/auth/AuthContext.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/auth/AuthContext.tsx", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/auth/AuthContext.tsx", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/auth/AuthContext.tsx", "to": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "type": "http" }
  ]
}
```
