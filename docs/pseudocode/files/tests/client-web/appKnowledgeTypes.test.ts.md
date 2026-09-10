# tests/client-web/appKnowledgeTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：编译期+运行时断言 App 知识库类型（App/ContextPattern/SuggestionPreview）字段可构造且关键值正确。
- 主要依赖：src/client-web/src/api/appKnowledge 类型导出、node:assert/strict
- 被谁使用：client-web 类型/契约测试

## 函数级结构化伪代码

### 模块顶层构造与断言
#### 构造 AppKnowledgeApp / ContextPattern / SuggestionPreview 字面量
- 输入：示例字段（id、processName、patternType、preview 汇总等）
- 输出：通过类型检查的常量
- 副作用：无（纯类型与值断言）
- 步骤：
  1. 构造含 contextCount 的 app
  2. 构造 domain 型 context 与 appId=null 的 title 型 context
  3. 构造 suggestion preview（recommended + alternatives + preview 统计）
  4. assert 关键字段：contextCount、patternType、appId null、alternatives 长度、scopeSummary
- 分支与异常：assert 失败抛错
- 调用：assert.equal

## 近逐行中文伪代码

1. [L1-7] 导入 assert 与 appKnowledge 类型。
2. [L8-23] 构造 AppKnowledgeApp 示例。
3. [L25-57] 构造两条 ContextPattern（有/无 appId）。
4. [L59-72] 构造 SuggestionPreview 与 preview 汇总。
5. [L74-78] 断言核心字段。

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/appKnowledgeTypes.test.ts",
      "label": "appKnowledgeTypes.test",
      "path": "tests/client-web/appKnowledgeTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/appKnowledgeTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/appKnowledgeTypes.test.ts",
      "to": "src/client-web/src/api/appKnowledge",
      "type": "tests"
    }
  ]
}
```
