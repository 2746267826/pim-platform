# tests/client-web/scheduleWorkbenchApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：锁定日程工作台路由源码片段与 calendar/operations API 路径及 fetch 契约。
- 主要依赖：`calendar` API、`operations` API、布局/页面源文件
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### assertSourceContains
- 读源文件，逐片段 includes

### 路径静态断言
- taskSegments、layers query 编码、data-center、outlook*、confirmations*

### async main（mock fetch）
- list/create/delete segments；layers GET；dataCenter POST；outlook settings/device-code/sync/batches；pending/detail/confirm/reject
- assertJsonBody 失败入 failures，末尾 AggregateError

## 近逐行中文伪代码

1. [L1-L35] 导入与源码断言 helper
2. [L37-L55] 布局/侧栏/页面片段
3. [L57-L75] 路径字符串
4. [L77-L91] fetch mock 与 body 辅助
5. [L93-L199] 14 次 API 调用契约
6. [L201-L204] main catch exitCode

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchApiPath.test.ts",
      "label": "scheduleWorkbenchApiPath.test",
      "path": "tests/client-web/scheduleWorkbenchApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchApiPath.test.ts", "to": "src/client-web/src/api/calendar.ts", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchApiPath.test.ts", "to": "src/client-web/src/api/operations.ts", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchApiPath.test.ts", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "tests" }
  ]
}
```
