# tests/client-web/scheduleWorkbenchCompletionTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：工作台收尾相关类型字面量可赋值（项目/习惯/报告/审计/冲突/端点等）。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

- 构造 DomainProject/TaskBook/Checklist/Habit/Reminder/Report/Audit/Conflict/Endpoint 与枚举字面量
- assert project.name；void 其余防未使用

## 近逐行中文伪代码

1. [L1-L44] 类型样例

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchCompletionTypes.test.ts",
      "label": "scheduleWorkbenchCompletionTypes.test",
      "path": "tests/client-web/scheduleWorkbenchCompletionTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchCompletionTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchCompletionTypes.test.ts", "to": "src/client-web/src/types/index.ts", "type": "tests" }
  ]
}
```
