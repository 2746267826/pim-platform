# src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：任务表 `tasks` 实体：归属用户/日历/项目/任务本、优先级与时长、计划与截止、软删除与操作溯源、父子任务与清单导航。
- 主要依赖：`ISoftDeletable`、`CalendarEntity`/`DomainProjectEntity`/`TaskBookEntity`/`TaskChecklistItemEntity`
- 被谁使用：`CalendarService`、调度与执行段服务、`PimDbContext`

## 函数级结构化伪代码

### TaskEntity
#### 属性与导航（表 `tasks`，实现 `ISoftDeletable`）
- 输入：属性赋值
- 输出：实体状态
- 副作用：无
- 步骤：
  1. 标识：`Id` 默认 NewGuid；`UserId`；可选 `CalendarId`/`DomainProjectId`/`TaskBookId`
  2. 内容：`Uid`/`Title`/`Description`；`Source` 默认 `"manual"`；`Priority`
  3. 时间：`EstimatedDuration`/`MinimumSegment`/`DtStart`/`PlannedEnd`/`Due`/`CompletedAt`
  4. 状态：`Status` 默认 `"NEEDS-ACTION"`；`StateReason`/`ReviewOutcome`/`PercentComplete`
  5. 结构：`ParentTaskId`、`IsInbox` 默认 true、`SortOrder`、`SchedulePlanId`
  6. 软删：`DeletedAt`；`DeletedByOperationId`/`DeletedByOperationKind`
  7. 审计：`CreatedAt`/`UpdatedAt` 默认 UtcNow
  8. 导航：Calendar、DomainProject、TaskBook、ParentTask、SubTasks、ChecklistItems
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`Pim.Core.Data`
2. 表 `tasks`；类实现 `ISoftDeletable`
3. 主键与用户/日历/项目/任务本外键列
4. Uid、Title、Description、Source、Priority、时长与时间字段
5. 删除操作 Id/Kind；Due、CompletedAt、Status 等
6. 父子任务、收件箱与排序、SchedulePlanId
7. Created/Updated/Deleted 时间戳
8. FK 导航与 SubTasks、ChecklistItems 集合初始化

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs",
      "label": "TaskEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" }
  ]
}
```
