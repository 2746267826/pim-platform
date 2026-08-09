# src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：映射表 `task_execution_segments` 的任务执行/排程片段实体（时段、状态、来源、规划原因、软删除，FK 到 `TaskEntity`）。
- 主要依赖：DataAnnotations/Schema；`ISoftDeletable`；导航 `TaskEntity`
- 被谁使用：日程工作台与任务执行段服务；相关单元测试与 EF 模型

## 函数级结构化伪代码

### TaskExecutionSegmentEntity
#### 属性集（无行为方法）
- 输入：各属性赋值
- 输出：行状态
- 副作用：无；`ISoftDeletable` 支持软删除过滤
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `TaskId`：所属任务 Id；`[ForeignKey]` 导航 `Task`
  3. `UserId`：所属用户
  4. `StartsAt` / `EndsAt`：片段起止
  5. `Status`：默认 `"planned"`，最长 40
  6. `Source`：默认 `"manual"`，最长 40
  7. `PlanningReason`：可选规划原因
  8. `ConfirmationId`：可选确认 Id
  9. `CreatedAt` / `UpdatedAt`：默认 UTC 现在
  10. `DeletedAt`：软删除时间
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表 `task_execution_segments`；实现 `ISoftDeletable`
4. 主键、`task_id`、`user_id`、起止时间
5. 状态默认 planned、来源默认 manual
6. 可选规划原因与确认 Id；时间戳与软删除
7. 导航属性 `Task` → `TaskEntity`（FK TaskId）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs",
      "label": "TaskExecutionSegmentEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs", "type": "depends_on" }
  ]
}
```
