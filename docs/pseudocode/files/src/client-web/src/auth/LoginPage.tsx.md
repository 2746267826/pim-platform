# src/client-web/src/auth/LoginPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：登录/注册表单页；已登录则重定向；提交调用 AuthContext。
- 主要依赖：
  - `react-router-dom`（Navigate/useLocation/useNavigate）
  - `./AuthContext`（useAuth）
- 被谁使用：路由表登录入口

## 函数级结构化伪代码

### LoginPage（default export）
#### 组件主体
- 输入：无 props；读 location.state.from 作回跳
- 输出：JSX
- 副作用：登录/注册 API（经 AuthContext）
- 步骤：
  1. 取 login/register/isAuthenticated；算 redirectTarget（from 路径或 `/today`）。
  2. 本地 state：username/password/email、isRegister、error、loading。
  3. 已认证 → `<Navigate to={redirectTarget} replace />`。
  4. 渲染居中表单：标题、错误区、用户名、注册时邮箱、密码、提交按钮、切换登录/注册。
- 分支与异常：见 handleSubmit
- 调用：`useAuth`、`useNavigate`、`useLocation`

#### `handleSubmit(e)`
- 输入：FormEvent
- 输出：Promise void
- 副作用：register 或 login；导航
- 步骤：
  1. preventDefault；清 error；loading=true。
  2. isRegister → register(username,email,password)；否则 login(username,password)。
  3. 返回 err 字符串 → setError；否则 navigate(redirectTarget, replace)。
  4. catch → 网络失败文案；finally loading=false。
- 分支与异常：业务 err vs 网络异常
- 调用：`register` / `login` / `navigate`

## 近逐行中文伪代码

1. 从路由 state 恢复登录后回跳地址，默认 /today。
2. 已登录直接 Navigate。
3. 表单提交：注册或登录；有错误展示；成功 replace 导航。
4. 切换模式清空错误；loading 时禁用提交按钮。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/auth/LoginPage.tsx",
      "label": "LoginPage",
      "path": "src/client-web/src/auth/LoginPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/auth/LoginPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/auth/LoginPage.tsx", "to": "src/client-web/src/auth/AuthContext.tsx", "type": "calls" },
    { "from": "src/client-web/src/auth/LoginPage.tsx", "to": "react-router-dom", "type": "depends_on" }
  ]
}
```
