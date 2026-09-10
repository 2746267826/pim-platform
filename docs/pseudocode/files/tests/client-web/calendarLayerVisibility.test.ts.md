# tests/client-web/calendarLayerVisibility.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：验证 `buildCalendarEvents` 按可见图层集合过滤 event/task/task-segment。
- 主要依赖：`CalendarPage.buildCalendarEvents`、client-web types、node:assert
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### (module top-level)
#### 构造样例并断言
- 输入：固定 EventResponse、TaskResponse、CalendarLayerItem
- 输出：断言 title 列表
- 副作用：无
- 步骤：
  1. 仅 `events` 图层 → 仅 Planning review
  2. `events`+`task-segments` → 事件 + 计划任务 + segment 三项
- 分支与异常：assert 失败即测试失败
- 调用：`buildCalendarEvents`

## 近逐行中文伪代码

1. [L1-3] 导入 assert、`buildCalendarEvents`、类型
2. [L5-14] 样例 event
3. [L16-24] 样例 plannedTask
4. [L26-38] task-segment 图层项
5. [L40-50] 仅 events 可见 → 单标题
6. [L52-61] events+task-segments → 三标题

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/calendarLayerVisibility.test.ts",
      "label": "calendarLayerVisibility.test",
      "path": "tests/client-web/calendarLayerVisibility.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/calendarLayerVisibility.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/calendarLayerVisibility.test.ts", "to": "src/client-web/src/pages/CalendarPage.tsx", "type": "tests" },
    { "from": "tests/client-web/calendarLayerVisibility.test.ts", "to": "src/client-web/src/types", "type": "depends_on" }
  ]
}
```
