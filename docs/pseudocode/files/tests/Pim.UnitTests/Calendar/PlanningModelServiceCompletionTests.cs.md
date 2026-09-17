# tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：日历图层返回 events/segments/habits/availability/ai-placeholders；任务多非重叠片段。
- 主要依赖：PlanningModelService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### CalendarLayersReturnEventsSegmentsHabitsAvailabilityAndAiPlaceholders
### BasicTaskCanHaveMultipleNonOverlappingSegments

## 近逐行中文伪代码

1. 多种 layer 项与颜色/类型
2. 同一任务多 segment 无重叠

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs",
      "label": "PlanningModelServiceCompletionTests.cs",
      "path": "tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs","to":"src/Pim.Module.Calendar/Services/PlanningModelService.cs","type":"tests"}
}
```