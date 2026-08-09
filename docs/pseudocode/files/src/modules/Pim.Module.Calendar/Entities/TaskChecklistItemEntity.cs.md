# src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：任务检查项实体，支持软删除与排序。
- 主要依赖：`ISoftDeletable`、`TaskEntity` 导航、DataAnnotations
- 被谁使用：`PlanningModelService`（增删查检查项）、`PimDbContext`

## 函数级结构化伪代码

### TaskChecklistItemEntity
#### 属性模型（实现 ISoftDeletable）
- 输入：ORM 字段
- 输出：表 `task_checklist_items` 行
- 副作用：无运行时逻辑
- 步骤：
  1. 主键 `Id`；外键 `TaskId` + 导航 `Task`
  2. 归属 `UserId`；`Title` 最长 255
  3. `IsDone`、`SortOrder`
  4. `CreatedAt`/`UpdatedAt` 默认 UtcNow；`DeletedAt` 可空（软删）
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 `Pim.Core.Data`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表 `task_checklist_items`，类实现 `ISoftDeletable`
4. 字段：Id、TaskId、UserId、Title、IsDone、SortOrder、时间戳、DeletedAt
5. `ForeignKey(TaskId)` 导航到 `TaskEntity`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs",
      "label": "TaskChecklistItemEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs", "to": "Pim.Core.Data.ISoftDeletable", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs", "type": "depends_on" }
  ]
}
```
