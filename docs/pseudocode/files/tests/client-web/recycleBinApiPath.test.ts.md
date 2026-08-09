# tests/client-web/recycleBinApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言日历回收站路径拼接稳定，并捕获 preview/restore 的 fetch 契约。
- 主要依赖：`calendarApiPaths`、`previewRecycleRestore`、`restoreRecycleItem`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 路径断言
#### calendarApiPaths.recycle*
- 输入：可选查询参数、type/id
- 输出：相对路径字符串
- 步骤：无参列表、带 query 列表、restore-preview、restore、task plan/batch 路径

### async main
#### main()
- 输入：mock fetch 抛 `request captured`
- 输出：断言通过或 AggregateError
- 副作用：覆盖 globalThis.fetch
- 步骤：
  1. previewRecycleRestore → 捕获 URL 与 POST
  2. restoreRecycleItem(true) → URL/POST/body restoreAsCopy
  3. 汇总 failures 后抛 AggregateError
- 调用：`previewRecycleRestore`、`restoreRecycleItem`

## 近逐行中文伪代码

1. [L1-L6] 导入 assert 与 calendar API
2. [L8] failures 数组
3. [L10-L33] 路径与 query 顺序断言（乱序 query 失败入 failures）
4. [L35-L40] mock fetch 记录请求后抛错
5. [L42-L63] main：两次 API 调用契约 + failures 汇总
6. [L65-L68] main().catch 设 exitCode=1

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/recycleBinApiPath.test.ts",
      "label": "recycleBinApiPath.test",
      "path": "tests/client-web/recycleBinApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/recycleBinApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/recycleBinApiPath.test.ts", "to": "src/client-web/src/api/calendar.ts", "type": "tests" }
  ]
}
```
