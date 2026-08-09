# tests/client-web/appKnowledgeComponents.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：App 知识库相关 UI 组件与页面文案契约测试（中文导航、分类树、上下文列表、禁止英文残留）。
- 主要依赖：`AppKnowledgeTabs`、`AppKnowledgeContextList`、`react-dom/server`、`MemoryRouter`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### test 辅助
- 同步执行回调；`readClientSource` 读 `src/client-web/src/*` 源码字符串

### category tree secondary navigation uses app knowledge language
- 渲染 `AppKnowledgeTabs active=categories`；含「App 列表」「分类树」，不含「分类管理」

### category tree page uses app knowledge category tree language and tabs
- 源码断言 `CategoryTreePage` 标题/副标题/Tabs 与禁用旧文案

### app knowledge base page keeps app tab active with updated subtitle
- `AppKnowledgeBasePage` 标题、副标题、API 调用与 ContextList 挂载

### context list renders context knowledge pattern details
- 静态渲染带样例 context；断言域名/学习/影响条数时长/标签

### app knowledge UI copy does not expose English remnants
- 合并多文件源码，遍历禁止英文 UI 串列表均不出现

## 近逐行中文伪代码

1. 用 client-web 的 require 加载 React/router。
2. 注入 `globalThis.React` 供 JSX 运行时。
3. 组件级 static markup 断言中文文案。
4. 页面级读源码断言结构与 API 使用。
5. 黑名单扫描防英文 UI 回潮。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/appKnowledgeComponents.test.tsx",
      "label": "appKnowledgeComponents.test",
      "path": "tests/client-web/appKnowledgeComponents.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/appKnowledgeComponents.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/appKnowledgeComponents.test.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/appKnowledgeComponents.test.tsx",
      "to": "src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/appKnowledgeComponents.test.tsx",
      "to": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "type": "tests"
    },
    {
      "from": "tests/client-web/appKnowledgeComponents.test.tsx",
      "to": "src/client-web/src/pages/CategoryTreePage.tsx",
      "type": "tests"
    }
  ]
}
```
