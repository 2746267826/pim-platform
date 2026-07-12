# src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：习惯例程持久化实体，映射表 `habit_routines`，支持软删除与发生记录导航。
- 主要依赖：`ISoftDeletable`（`Pim.Core.Data`）；DataAnnotations/Schema；`HabitOccurrenceEntity`
- 被谁使用：`DataCenterQueryService`、规划/习惯相关服务、EF 迁移与 `PimDbContext` 配置

## 函数级结构化伪代码

### HabitRoutineEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层赋值后由 EF 持久化
- 输出：表 `habit_routines` 一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表 `habit_routines`；实现 `ISoftDeletable`。
  2. `Id` 主键 Guid 默认 NewGuid；`UserId` 归属用户。
  3. `Title` MaxLength 255；`Description` 可空。
  4. `Cadence` 默认 `"Daily"`；`Source` 默认 `"manual"`；`Status` 默认 `"Active"`（均 MaxLength 40）。
  5. `RuleJson` 默认 `"{}"`。
  6. `CreatedAt`/`UpdatedAt` 默认 UtcNow；`DeletedAt` 可空软删。
  7. 导航 `Occurrences` → `HabitOccurrenceEntity` 集合。
- 分支与异常：本类型无校验逻辑
- 调用：被查询服务投影为 DataCenter 的 habit 项

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`。
2. 命名空间 `Pim.Module.Calendar.Entities`；`[Table("habit_routines")]`。
3. 类实现 `ISoftDeletable`。
4. Id/UserId/Title/Description/Cadence/Source/Status/RuleJson/时间戳与 DeletedAt。
5. 集合导航 Occurrences 初始化为空 List。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs",
      "label": "HabitRoutineEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs", "type": "depends_on" }
  ]
}
```
