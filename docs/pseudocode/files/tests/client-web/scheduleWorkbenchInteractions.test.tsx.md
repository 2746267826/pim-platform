# tests/client-web/scheduleWorkbenchInteractions.test.tsx

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态检查 Today/Calendar/TaskList/Habits 交互符号。
- 主要依赖：各页面源码
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### assertPageSourceContains
- Today：工作台/待确认/同步/提醒/报告
- Calendar：LayerToolbar/outlookOnly/ai-placeholders
- TaskList：Hierarchy/Segment/Checklist
- Habits：RoutineEditor/完成历史/投射

## 近逐行中文伪代码

1. 辅助读文件 includes
2. 四页 snippet 断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchInteractions.test.tsx",
      "label": "scheduleWorkbenchInteractions.test.tsx",
      "path": "tests/client-web/scheduleWorkbenchInteractions.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchInteractions.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/client-web/scheduleWorkbenchInteractions.test.tsx","to":"src/client-web/src/pages/TodayPage.tsx","type":"tests"},{"from":"tests/client-web/scheduleWorkbenchInteractions.test.tsx","to":"src/client-web/src/pages/CalendarPage.tsx","type":"tests"}]
}
```