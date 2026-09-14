# tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：任务执行片段创建/List/Delete：时间校验、status/source 校验、UTC 归一、不覆盖已有规划、跨用户拒绝、软删。
- 主要依赖：`PlanningModelService`、TaskExecutionSegmentEntity
- 被谁使用：dotnet test

## 函数级结构化伪代码

### CreateSegmentAsync_RejectsEndsAtBeforeOrEqualStartsAt → 02024
### CreateSegmentAsync_RejectsInvalidStatusOrSource → 02026（空或超长）
### CreateSegmentAsync_KeepsTaskIdentityAndReturnsSegmentMetadata
- 步骤：+08 归一 UTC；出 inbox；首次写入 DtStart/PlannedEnd；不建 Event
### CreateSegmentAsync_DoesNotOverwriteExistingTaskPlanningRange
### CreateSegmentAsync_RejectsAnotherUsersTask → 02004
### ListSegmentsAsync_ReturnsSegmentsForTaskOrderedByStartsAt
### DeleteSegmentAsync_SoftDeletesSegmentWithoutDeletingTask

## 近逐行中文伪代码

1. [L17-37] 结束时间非法
2. [L39-63] status/source 校验
3. [L65-97] 创建元数据
4. [L99-124] 保留原规划
5. [L126-145] 跨用户
6. [L147-192] 列表排序
7. [L194-220] 软删
8. [L222-250] 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs",
      "label": "TaskExecutionSegmentServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs", "to": "src/Pim.Module.Calendar/Services/PlanningModelService.cs", "type": "tests" }
  ]
}
```
