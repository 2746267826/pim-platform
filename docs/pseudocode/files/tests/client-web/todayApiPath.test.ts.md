# tests/client-web/todayApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言 `todayApiPaths` 区块列表与单区块路径拼接。
- 主要依赖：`src/client-web/src/api/today` 的 `todayApiPaths`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：
  1. sections(date) → `/today/sections?date=...`
  2. section(key, date) → `/today/sections/{key}?date=...`（含 calendar.schedule、pc.classification_suggestions）

## 近逐行中文伪代码

1. [L1-2] 导入 assert 与 todayApiPaths
2. [L4] sections 日期 query
3. [L5-12] 两个 section key 路径 equal

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/todayApiPath.test.ts",
      "label": "todayApiPath.test",
      "path": "tests/client-web/todayApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/todayApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/todayApiPath.test.ts",
      "to": "src/client-web/src/api/today.ts",
      "type": "tests"
    }
  ]
}
```
