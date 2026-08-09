# tests/client-web/statusApiNormalization.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：系统状态 summary/detail 数值状态归一与中文标签。
- 主要依赖：`normalizeStatusSummary`/`normalizeStatusDetail`/`getHealthStatusLabel`/`getComponentKindLabel`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

- summary status 1→Healthy/正常
- detail：Warning/Critical/Unknown；kind 空；details 转字符串；nextSteps 转字符串
- 标签 Critical/Unknown/Api

## 近逐行中文伪代码

1. [L1-L18] summary
2. [L20-L61] detail 与 label

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/statusApiNormalization.test.ts",
      "label": "statusApiNormalization.test",
      "path": "tests/client-web/statusApiNormalization.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/statusApiNormalization.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/statusApiNormalization.test.ts", "to": "src/client-web/src/api/status.ts", "type": "tests" }
  ]
}
```
