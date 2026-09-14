# tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 CalendarService 任务规划 PlanTask、分页筛选、批量更新（状态/日历/边界）。
- 主要依赖：`CalendarService`、`TaskEntity`/`EventEntity`/`CalendarEntity`、InMemory `PimDbContext`、FixedCurrentUserService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PlanTaskAsync_SetsPlannedRangeWithoutCreatingEvent
- 步骤：inbox 任务规划时间段与 PT1H30M；出 inbox；EstimatedDuration=01:30:00；无 Event

### PlanTaskAsync_NormalizesOffsetPlannedRangeToUtc
- 步骤：+08:00 输入归一为 UTC offset=0

### PlanTaskAsync_PreservesExistingEstimatedDurationWhenOmitted
- 步骤：请求不传 duration 时保留已有 2h

### GetTasksPagedAsync_FiltersSearchStatusAndPriority
- 步骤：search/status/priority 仅命中 Alpha

### BatchUpdateTasksAsync_UpdatesStatusForRequestedTasksOnly
- 步骤：仅更新指定 id 状态为 COMPLETED

### BatchUpdateTasksAsync_RejectsAnotherUsersCalendar
- 步骤：DomainException 02003「日历不存在」；不改 CalendarId/IsInbox

### BatchUpdateTasksAsync_AssignsCurrentUsersCalendarAndClearsInbox
- 步骤：绑定本人日历；IsInbox=false；Samples.BookName

### BatchUpdateTasksAsync_ReturnsNoTasksUpdatedWhenNoTasksMatch
- 步骤：AffectedCount=0；Message「没有更新任务」

### BatchUpdateTasksAsync_ReturnsNoTasksUpdatedWhenNoMutationFields
- 步骤：全 null 字段不改 UpdatedAt

### CreateDb / CreateCalendarService / FixedCurrentUserService
- 步骤：注册模块程序集；InMemory；固定 UserId

## 近逐行中文伪代码

1. [L15] 固定 UserId
2. [L17-37] Plan 时间段不建事件
3. [L39-58] 时区归一 UTC
4. [L60-83] 省略 duration 保留原值
5. [L85-111] 分页筛选
6. [L113-128] 批量状态
7. [L130-154] 他人日历拒绝
8. [L156-179] 本人日历绑定
9. [L181-218] 无匹配/无字段
10. [L220-236] 工厂与假当前用户

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs",
      "label": "CalendarTaskPlanningTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs", "to": "src/Pim.Module.Calendar/Services/CalendarService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs", "to": "src/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" }
  ]
}
```
