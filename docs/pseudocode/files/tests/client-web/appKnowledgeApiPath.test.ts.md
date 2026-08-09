# tests/client-web/appKnowledgeApiPath.test.ts

## 元信息
- 语言：TypeScript / node:assert
- 程序集或包：tests/client-web
- 职责：锁定 appKnowledgeApiPaths 路径字符串契约。
- 主要依赖：appKnowledgeApiPaths（src/client-web/src/api/appKnowledge）
- 被谁使用：测试运行器

## 函数级结构化伪代码

### 顶层断言
- apps() → `/pc/app-knowledge/apps`
- apps('code') → 带 `?search=code`
- appContexts(appId) → `/pc/app-knowledge/apps/{id}/contexts`
- suggestionPreview(id) → `.../suggestions/{id}/preview`
- suggestionApply(id) → `.../suggestions/{id}/apply`

## 近逐行中文伪代码

1. 导入 appKnowledgeApiPaths。
2. 逐项 assert.equal 期望 REST 路径。
3. 搜索参数与 id 插值必须稳定。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/appKnowledgeApiPath.test.ts",
      "label": "appKnowledgeApiPath.test",
      "path": "tests/client-web/appKnowledgeApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/appKnowledgeApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/appKnowledgeApiPath.test.ts",
      "to": "src/client-web/src/api/appKnowledge.ts",
      "type": "tests"
    }
  ]
}
```
