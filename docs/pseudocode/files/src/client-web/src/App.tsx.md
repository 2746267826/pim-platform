# src/client-web/src/App.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：Web 根组件：Auth 上下文包裹路由表（登录、默认重定向今日、其余交给 AppLayout）。
- 主要依赖：`react-router-dom`、`AuthProvider`、`LoginPage`、`AppLayout`
- 被谁使用：`main` 入口挂载

## 函数级结构化伪代码

### App (default export)
- 输入：无 props
- 输出：React 节点
- 副作用：无（子树负责鉴权与布局）
- 步骤：
  1. 渲染 `<AuthProvider>`。
  2. 内嵌 `<Routes>`：
     - `/login` → `LoginPage`
     - `/` → `Navigate` 到 `/today`（replace）
     - `/*` → `AppLayout`
  3. 结束 AuthProvider。
- 分支与异常：无
- 调用：路由组件

## 近逐行中文伪代码

1. 导入 Routes/Route/Navigate、AuthProvider、LoginPage、AppLayout。
2. 默认导出 App：AuthProvider 包住三路由。
3. 登录页独立；根路径跳今日；其余通配进主布局。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/App.tsx",
      "label": "App",
      "path": "src/client-web/src/App.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/App.tsx.md",
      "layer": "client-web",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/App.tsx", "to": "src/client-web/src/auth/AuthContext.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/App.tsx", "to": "src/client-web/src/auth/LoginPage.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/App.tsx", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "depends_on" }
  ]
}
```
