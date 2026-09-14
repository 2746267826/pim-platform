# src/client-web/src/api/client.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web（Vite/React）
- 职责：Web 前端统一 HTTP 客户端——内存+localStorage 令牌、401 自动 refresh、JSON CRUD/上传/Blob 下载、开发态耗时日志。
- 主要依赖：浏览器 `fetch`、`localStorage`、`performance`、`import.meta.env.DEV`
- 被谁使用：`auth/AuthContext` 与各 `api/*.ts` 领域客户端（calendar、files、mobile、ai 等）

## 函数级结构化伪代码

### 模块状态
#### accessToken / refreshToken / onAuthChange / BASE
- 输入：无
- 输出：模块级闭包状态；BASE=`/api/v1`
- 副作用：无
- 步骤：令牌与回调常驻
- 分支与异常：无
- 调用：无

### setTokens(access, refresh)
- 输入：access/refresh 字符串
- 输出：无
- 副作用：写内存与 localStorage
- 步骤：两键同步
- 分支与异常：无
- 调用：`localStorage.setItem`

### loadTokens(): boolean
- 输入：无
- 输出：是否存在 accessToken
- 副作用：从 localStorage 载入内存
- 步骤：读两键；返回 `!!accessToken`
- 分支与异常：无
- 调用：`localStorage.getItem`

### clearTokens()
- 输入：无
- 输出：无
- 副作用：清空内存与 localStorage
- 步骤：remove 两键
- 分支与异常：无
- 调用：`localStorage.removeItem`

### onTokensChanged(cb)
- 输入：无参回调
- 输出：无
- 副作用：注册全局 onAuthChange
- 步骤：赋值
- 分支与异常：无
- 调用：无

### refreshAccessToken(): Promise\<boolean\>
- 输入：无（用 refreshToken）
- 输出：是否刷新成功
- 副作用：成功则 setTokens
- 步骤：无 refresh → false；POST `/auth/refresh`；ok 则取 data 令牌
- 分支与异常：网络/非 ok → false
- 调用：`fetch`、`setTokens`

### apiGet / apiPost / apiPut / apiDelete
- 输入：path 与可选 body
- 输出：Promise\<T\>
- 副作用：带鉴权请求
- 步骤：转发 authedFetch；POST/PUT 序列化 body
- 分支与异常：见 apiFetchResponse
- 调用：`authedFetch`

### apiUpload(path, body: BodyInit)
- 输入：上传 body（不强制 JSON Content-Type）
- 输出：Promise\<T\>
- 副作用：POST 原始 body
- 步骤：`apiFetchRaw(..., includeJsonContentType=false)`
- 分支与异常：同上
- 调用：`apiFetchRaw`

### apiDownloadBlob(path)
- 输入：path
- 输出：Blob
- 副作用：GET 鉴权
- 步骤：apiFetchResponse → res.blob()
- 分支与异常：同上
- 调用：`apiFetchResponse`

### logApi(method, path, duration, status?)
- 输入：方法、路径、毫秒、状态
- 输出：无
- 副作用：DEV 时 console.log
- 步骤：仅 DEV 打印
- 分支与异常：无
- 调用：`console.log`

### authedFetch → apiFetchRaw → apiFetchResponse
- 输入：path、RequestInit、是否加 JSON Content-Type
- 输出：解析后的 T 或 Response
- 副作用：Bearer；401 刷新重试；失败 clearTokens+onAuthChange
- 步骤：
  1. 计时；Headers；可选 Content-Type；有 access 则 Authorization
  2. fetch BASE+path
  3. 401 且有 refresh：refresh 成功则改 Bearer 重试；失败 clear 并抛「登录已过期」
  4. logApi；!ok 则 json.message 或 HTTP 状态抛 Error
  5. Raw：204/空 body → undefined as T；否则 res.json()
- 分支与异常：过期/HTTP 错误抛 Error
- 调用：`refreshAccessToken`、`clearTokens`、`fetch`

## 近逐行中文伪代码

1. BASE=`/api/v1`；模块级 access/refresh/onAuthChange
2. setTokens/loadTokens/clearTokens 同步 localStorage
3. onTokensChanged 注册回调
4. refreshAccessToken：POST refresh；写新令牌
5. apiGet/Post/Put/Delete 包装 authedFetch
6. apiUpload 不强制 JSON；apiDownloadBlob 返回 Blob
7. apiFetchResponse：Bearer；401 刷新或清令牌；失败抛 message
8. apiFetchRaw：空响应 undefined，否则 json
9. DEV 下 logApi 打印耗时与状态

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/client.ts",
      "label": "api/client",
      "path": "src/client-web/src/api/client.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/client.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/client.ts", "to": "/api/v1/auth/refresh", "type": "http" },
    { "from": "src/client-web/src/auth/AuthContext.tsx", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/calendar.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/files.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/mobile.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" }
  ]
}
```
