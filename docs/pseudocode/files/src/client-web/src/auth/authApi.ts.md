# src/client-web/src/auth/authApi.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：认证响应解析与失败文案映射——安全读 JSON body、按 HTTP 状态与业务 message 生成中文错误提示。
- 主要依赖：`../types` 中 `ApiResponse`、`AuthResponse`；浏览器 `Response`
- 被谁使用：当前仓库内仅本文件导出（供登录/注册 UI 或测试接入；`AuthContext` 现走 `api/client`）

## 函数级结构化伪代码

### 类型 AuthAction / AuthApiResponse
#### AuthAction = 'login' | 'register'
#### AuthApiResponse = ApiResponse\<AuthResponse\>
- 输入：无
- 输出：类型别名
- 副作用：无
- 步骤：无
- 分支与异常：无
- 调用：无

### readAuthResponse(response): Promise\<AuthApiResponse | null\>
- 输入：fetch Response
- 输出：解析后的认证包装或 null
- 副作用：读 body 一次
- 步骤：204 或 content-length=0 → null；try json 否则 null
- 分支与异常：JSON 失败 → null
- 调用：`response.json`

### authFailureMessage(action, response, body): string
- 输入：动作、Response、可选 body
- 输出：用户可见中文错误
- 副作用：无
- 步骤：
  1. body.message 优先
  2. 401 → 用户名或密码不正确
  3. 429 → 登录尝试过多
  4. 否则 register/login 默认失败文案
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 从 `../types` 引入 ApiResponse、AuthResponse
2. 导出 AuthAction、AuthApiResponse
3. readAuthResponse：空 body 或解析失败返回 null
4. authFailureMessage：message → 401 → 429 → 默认注册/登录失败

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/auth/authApi.ts",
      "label": "authApi",
      "path": "src/client-web/src/auth/authApi.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/auth/authApi.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/auth/authApi.ts", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
