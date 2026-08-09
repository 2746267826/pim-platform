# tests/client-web/outlookSyncInvalidation.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言同步成功后 React Query 失效键集合完整。
- 主要依赖：`SyncPage` 的 `outlookSyncInvalidationKeys`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：将 keys 序列化入 Set；逐条检查 batches/confirmations/layers/data-center 相关 key

## 近逐行中文伪代码

1. [L1-4] 导入并 Set 化
2. [L6-21] 九个 expectedKey 必须存在

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/outlookSyncInvalidation.test.ts",
      "label": "outlookSyncInvalidation.test",
      "path": "tests/client-web/outlookSyncInvalidation.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/outlookSyncInvalidation.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/outlookSyncInvalidation.test.ts", "to": "src/client-web/src/pages/SyncPage.tsx", "type": "tests" }
  ]
}
```
