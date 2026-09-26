# tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Stage5 事件元数据、软删追踪、Update/Move 保留字段、响应不暴露 ICS 原文。
- 主要依赖：Calendar 实体与 CalendarService 更新路径
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Event 持久化 Outlook 全天/时区/ICS/复发元数据
2. Calendar/Task/Event 软删字段；默认过滤隐藏、IgnoreQueryFilters 可见
3. UpdateEvent 省略字段保留 Stage5；显式 IsAllDay=false 生效
4. UpdateTask 省略 PlannedEnd 保留
5. MoveTask 推导 PlannedEnd 仍可见
6. EventResponse 无 SourceIcsComponent

## 近逐行中文伪代码

1. 七 Fact 覆盖上列场景与服务调用

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs",
      "label": "CalendarStage5ModelTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs", "to": "src/modules/Pim.Module.Calendar/Entities", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs", "to": "src/modules/Pim.Module.Calendar/Services", "type": "tests" }
  ]
}
```
