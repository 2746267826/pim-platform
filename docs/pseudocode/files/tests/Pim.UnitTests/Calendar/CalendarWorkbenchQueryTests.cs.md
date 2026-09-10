# tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：数据中心合并查询与筛选分页；日历图层查询；路径常量稳定。
- 主要依赖：`DataCenterQueryService`、`PlanningModelService`、Calendar 实体、OperationConfirmation
- 被谁使用：dotnet test

## 函数级结构化伪代码

### DataCenterQueryAsync_MergesActiveItemsConfirmationsAndRecycleBin
- 步骤：含 event/task/task-segment/confirmation/recycle-bin；不含已删 event 作 active

### DataCenterQueryAsync_AppliesSearchObjectTypeSourcePendingAndPaging
- 步骤：search focus；objectType confirmation；source outlook-ics；pendingOnly；page=2 pageSize=2

### GetCalendarLayersAsync_ReturnsRequestedLayersAndFiltersOutlookOnly
- 步骤：events+task-segments 颜色；OutlookOnly 仅 outlook 源

### CalendarWorkbenchEndpointPaths_AreStable
- 步骤：layers 与 data-center/query 路径

### SeedWorkbench / CreateDb
- 步骤：播种活动/删除事件、任务、片段、确认

## 近逐行中文伪代码

1. [L26-45] 合并查询
2. [L47-89] 多维筛选分页
3. [L91-125] 图层与 OutlookOnly
4. [L127-132] 路径
5. [L134-228] 种子与工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs",
      "label": "CalendarWorkbenchQueryTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs", "to": "src/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs", "to": "src/Pim.Module.Calendar/Services/PlanningModelService.cs", "type": "tests" }
  ]
}
```
