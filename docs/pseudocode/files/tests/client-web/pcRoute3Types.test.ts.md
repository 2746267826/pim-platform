# tests/client-web/pcRoute3Types.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：锁定 Route3 预览与活动分析类型形状。
- 主要依赖：types 中 ActivityClassificationSuggestionPreview、PcActivityAnalysisResponse
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

- 构造 preview.rule/preview 与 analysis.blocks
- assert scope=activity、pendingClassificationCount=1

## 近逐行中文伪代码

1. [L1-L47] 类型样例与断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcRoute3Types.test.ts",
      "label": "pcRoute3Types.test",
      "path": "tests/client-web/pcRoute3Types.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/pcRoute3Types.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/pcRoute3Types.test.ts", "to": "src/client-web/src/types/index.ts", "type": "tests" }
  ]
}
```
