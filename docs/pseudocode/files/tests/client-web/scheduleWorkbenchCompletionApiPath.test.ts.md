# tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：工作台收尾路径（项目/习惯/报告/二级确认/端点）与 fetch 契约。
- 主要依赖：calendar/operations/endpoints API
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 路径
- projects/task-books/checklist/habits/reminders/reports*/outlook poll/data-center batch|export
- confirm-second-level/strict、audit timeline、restore-preview
- endpoints list/heartbeat/quality/notification-actions

### main mock fetch
- 依次调用 21 个 API 函数，断言 /api/v1 前缀 URL 与部分 method

## 近逐行中文伪代码

1. [L1-L52] 静态路径
2. [L54-L126] mock 与 main 调用链
3. [L128-L131] catch exitCode

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts",
      "label": "scheduleWorkbenchCompletionApiPath.test",
      "path": "tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts", "to": "src/client-web/src/api/calendar.ts", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts", "to": "src/client-web/src/api/operations.ts", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts", "to": "src/client-web/src/api/endpoints.ts", "type": "tests" }
  ]
}
```
