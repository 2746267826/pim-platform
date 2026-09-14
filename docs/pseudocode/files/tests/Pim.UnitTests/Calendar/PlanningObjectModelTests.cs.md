# tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：规划对象模型实体齐全；Task 层级与复盘元数据属性。
- 主要依赖：Calendar 规划实体
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ModelContainsAllApprovedPlanningObjects
### TaskHasProjectBookHierarchyStateAndReviewMetadata

## 近逐行中文伪代码

1. DomainProject/TaskBook/Checklist/Habit/Availability/AiPlaceholder
2. Task 属性 DomainProjectId/TaskBookId/Parent/StateReason/Review/Source

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs",
      "label": "PlanningObjectModelTests.cs",
      "path": "tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs","to":"src/Pim.Module.Calendar/Entities","type":"tests"}]
}
```