# src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：可用时间窗实体，映射表 `availability_windows`，记录用户某段时间的可用/不可用状态及来源。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`Pim.Core.Data.ISoftDeletable`
- 被谁使用：EF `PimDbContext` 映射；日历调度/可用性相关服务读写

## 函数级结构化伪代码

### AvailabilityWindowEntity
#### 属性集（实体字段）
- 输入：无（POCO 属性读写）
- 输出：各列对应 CLR 属性
- 副作用：由 EF 持久化到 `availability_windows`
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid()`
  2. `UserId`：所属用户
  3. `Title`：标题，最长 255
  4. `StartsAt` / `EndsAt`：时间窗起止（`DateTimeOffset`）
  5. `Kind`：种类，默认 `"available"`，最长 40
  6. `Source`：来源，默认 `"manual"`，最长 40
  7. `CreatedAt` / `UpdatedAt`：创建/更新时间，默认 UTC 现在
  8. `DeletedAt`：软删除时间戳（实现 `ISoftDeletable`）
- 分支与异常：无方法体控制流
- 调用：无

## 近逐行中文伪代码

1. 引用数据注解与 `Pim.Core.Data`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表映射 `[Table("availability_windows")]`
4. 类 `AvailabilityWindowEntity` 实现 `ISoftDeletable`
5. `Id` 主键列 `id`，默认新 Guid
6. `UserId` 列 `user_id`
7. `Title` 列 `title`，MaxLength 255，默认空串
8. `StartsAt` 列 `starts_at`；`EndsAt` 列 `ends_at`
9. `Kind` 列 `kind`，默认 `"available"`
10. `Source` 列 `source`，默认 `"manual"`
11. `CreatedAt`/`UpdatedAt` 默认 `UtcNow`
12. `DeletedAt` 可空，支持软删
13. （无导航属性/方法）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs",
      "label": "AvailabilityWindowEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs", "type": "depends_on" }
  ]
}
```
