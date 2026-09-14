# src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历表 `calendars` 的 EF 实体；支持软删除与事件/任务导航集合。
- 主要依赖：`ISoftDeletable`、`DataAnnotations`/`Schema`、`EventEntity`、`TaskEntity`
- 被谁使用：Calendar 服务与 DbContext 模块注册；Event/Task 外键导航；EF 迁移与快照

## 函数级结构化伪代码

### CalendarEntity
#### 属性集（无行为方法）
- 输入：各属性由调用方/EF 赋值
- 输出：行状态
- 副作用：无（纯 POCO）；持久化由 DbContext 负责
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `UserId`：所属用户
  3. `Name`：名称，最长 100，默认空串
  4. `Color`：颜色，最长 7，默认 `#3B82F6`
  5. `Kind`：类型，最长 20，默认 `calendar`
  6. `IsDefault`：是否默认日历
  7. `CreatedAt`/`UpdatedAt`：时间戳，默认 UTC 现在
  8. `DeletedAt`：软删除时间（可空）
  9. `DeletedByOperationId`/`DeletedByOperationKind`：删除操作追溯（可空，kind 最长 64）
  10. `Events`/`Tasks`：导航集合，默认空列表
- 分支与异常：无运行时分支
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表注解 `calendars`；类实现 `ISoftDeletable`
4. 主键 id；user_id；name/color/kind 带 MaxLength 与默认值
5. is_default；created_at/updated_at 默认 UtcNow
6. deleted_at 与删除操作 id/kind 可选列
7. Events、Tasks 集合导航初始化为空 List

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs",
      "label": "CalendarEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "to": "src/Pim.Core/Data", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" }
  ]
}
```
