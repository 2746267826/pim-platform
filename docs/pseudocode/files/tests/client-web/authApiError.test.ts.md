# tests/client-web/authApiError.test.ts

## 元信息
- 语言：TypeScript (node:assert)
- 程序集或包：tests/client-web
- 职责：验证 `readAuthResponse` / `authFailureMessage` 对 401 登录与 409 注册冲突的中文错误映射。
- 主要依赖：`src/client-web/src/auth/authApi`
- 被谁使用：测试脚本运行

## 函数级结构化伪代码

### main
#### async main()
- 输入：无
- 输出：断言通过或进程失败
- 副作用：无
- 步骤：
  1. 构造 401 空 Response → readAuthResponse 得 null → authFailureMessage('login',...) 为「用户名或密码不正确」
  2. 构造 409 JSON（code 1003，message 用户名已存在）→ body.message 正确 → register 失败消息沿用 body.message
- 分支与异常：assert 失败抛错
- 调用：`readAuthResponse`、`authFailureMessage`

## 近逐行中文伪代码

1. [L1-L2] 导入 assert 与 authApi 两个函数
2. [L4] async main
3. [L5-L9] 401：body null，登录失败文案固定
4. [L11-L23] 409：解析 message，注册失败用服务端 message
5. [L26] void main() 启动

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/authApiError.test.ts",
      "label": "authApiError.test",
      "path": "tests/client-web/authApiError.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/authApiError.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/authApiError.test.ts",
      "to": "src/client-web/src/auth/authApi.ts",
      "type": "tests"
    }
  ]
}
```
