# tests/client-web/calendarApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：校验 calendar API 路径构建与 fetch 请求契约（tasks 列表/筛选/plan/batch-update/delete-preview）；并静态检查 TaskListPage 源码关键交互符号。
- 主要依赖：`src/client-web/src/api/calendar`、`node:assert`、`node:fs`、全局 `fetch` mock
- 被谁使用：Node 测试脚本直接执行

## 函数级结构化伪代码

### 模块顶层
#### 路径常量断言
- 步骤：`buildTasksPath` / `calendarApiPaths.*` 与期望字符串 equal

#### fetch mock
- 步骤：拦截 fetch 记录 url/init，抛 `requestCaptured` 使调用方 rejects

### main()
#### getTasksPaged / planTask / batchUpdateTasks / previewCalendarDelete
- 输入：各 API 参数组合
- 输出：无（断言后可能 AggregateError）
- 副作用：填充 `requests` 数组
- 步骤：
  1. 默认分页 GET `/api/v1/calendar/tasks?page=1&pageSize=50`
  2. inbox=true 带 query
  3. 全筛选参数 URL 编码
  4. planTask POST body 含 plannedStart/End/estimatedDuration
  5. batchUpdateTasks POST ids/status/priority/calendarId
  6. previewCalendarDelete POST 空 body
  7. 读 TaskListPage 源：不匹配 sortTasksByDue；匹配 pendingDeleteIds/pruneSelectedIds/batchDeleteTasks/显示前 N 项
- 分支与异常：JSON 解析失败收集到 failures；最终 AggregateError
- 调用：calendar API 导出函数、assert.*

## 近逐行中文伪代码

1. [L1-15] 导入 calendar API 与 TaskListPage 源码文本
2. [L17-24] 同步路径字符串断言
3. [L26-32] mock fetch 记录并抛错
4. [L34-70] getTasksPaged 三组 URL
5. [L71-120] plan/batch-update/delete-preview 方法与 body
6. [L122-131] failures 汇总；TaskListPage 源码契约
7. [L133-136] main catch 设 exitCode=1

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/calendarApiPath.test.ts",
      "label": "calendarApiPath.test",
      "path": "tests/client-web/calendarApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/calendarApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/calendarApiPath.test.ts", "to": "src/client-web/src/api/calendar.ts", "type": "tests" },
    { "from": "tests/client-web/calendarApiPath.test.ts", "to": "src/client-web/src/pages/TaskListPage.tsx", "type": "tests" }
  ]
}
```
