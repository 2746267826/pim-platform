# tests/client-web/endpointShellPage.test.tsx

## 元信息
- 语言：TypeScript / node:assert + 源码字符串契约
- 程序集或包：tests/client-web
- 职责：确保 EndpointShell 页面、布局路由与 endpoints API 路径片段存在。
- 主要依赖：fs.readFileSync、EndpointShellPage/AppLayout/endpoints 源文件
- 被谁使用：测试运行器

## 函数级结构化伪代码

### assertSourceContains(path, snippets)
- 读 utf8 源码；每个 snippet 必须 includes，否则 assert 失败信息含 path/snippet

### 契约检查
#### EndpointShellPage.tsx
- 含组件名、listEndpointStatuses、getEndpointCollectionQuality、heartbeatEndpoint、handleEndpointNotificationAction、文案 collection quality / notification action / online-only boundary

#### AppLayout.tsx
- 含 EndpointShellPage 与路由 `/endpoint-shell`

#### api/endpoints.ts
- 含 `return '/endpoints'`、collection-quality、notification-actions

## 近逐行中文伪代码

1. 定义源码包含断言辅助函数。
2. 页面必须挂载端点状态/质量/心跳/通知动作能力。
3. 布局注册 endpoint-shell 路由。
4. API 模块保留 endpoints 与子路径片段。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/endpointShellPage.test.tsx",
      "label": "endpointShellPage.test",
      "path": "tests/client-web/endpointShellPage.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/endpointShellPage.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/endpointShellPage.test.tsx",
      "to": "src/client-web/src/pages/EndpointShellPage.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/endpointShellPage.test.tsx",
      "to": "src/client-web/src/layout/AppLayout.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/endpointShellPage.test.tsx",
      "to": "src/client-web/src/api/endpoints.ts",
      "type": "tests"
    }
  ]
}
```
