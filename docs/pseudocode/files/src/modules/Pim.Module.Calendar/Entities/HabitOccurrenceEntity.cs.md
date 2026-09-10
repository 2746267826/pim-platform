# src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：习惯例行（HabitRoutine）单次发生记录的 EF 实体，映射表 `habit_occurrences`，支持软删除。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`Pim.Core.Data.ISoftDeletable`、`HabitRoutineEntity`
- 被谁使用：Calendar 规划/习惯服务、`PimDbContext` 配置、相关迁移

## 函数级结构化伪代码

### HabitOccurrenceEntity : ISoftDeletable
#### 属性（无方法）
- 输入/输出：持久化字段读写
- 副作用：无（纯实体）
- 步骤（字段语义）：
  1. `Id`：主键 Guid，默认 NewGuid。
  2. `HabitRoutineId` / 导航 `HabitRoutine`：所属习惯例行。
  3. `UserId`：所属用户。
  4. `StartsAt` / `EndsAt`：发生时间窗。
  5. `Status`：默认 `"Planned"`，MaxLength 40。
  6. `Source`：默认 `"manual"`，MaxLength 40。
  7. `ConfirmationId`：可选关联运维确认。
  8. `CreatedAt` / `UpdatedAt`：默认 UtcNow。
  9. `DeletedAt`：软删除时间，实现 `ISoftDeletable`。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema 与 `ISoftDeletable`。
2. 命名空间 `Pim.Module.Calendar.Entities`。
3. 表名 `habit_occurrences`；类实现软删除接口。
4. 列映射：id、habit_routine_id、user_id、starts_at、ends_at、status、source、confirmation_id、created_at、updated_at、deleted_at。
5. FK 导航到 `HabitRoutineEntity`（null! 由 EF 填充）。
6. 默认值：Status=Planned，Source=manual，时间戳 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs",
      "label": "HabitOccurrenceEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs", "type": "depends_on" }
  ]
}
```
