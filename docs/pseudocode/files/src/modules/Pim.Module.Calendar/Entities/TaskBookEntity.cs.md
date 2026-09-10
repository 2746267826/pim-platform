# src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：任务本（task book）持久化实体，映射表 `task_books`，可挂域项目与任务集合，支持软删除。
- 主要依赖：`ISoftDeletable`；DataAnnotations/Schema；`DomainProjectEntity`、`TaskEntity`
- 被谁使用：日历/任务规划服务、EF 迁移与 DbContext 配置

## 函数级结构化伪代码

### TaskBookEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层赋值后由 EF 持久化
- 输出：表 `task_books` 一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表 `task_books`；实现 `ISoftDeletable`。
  2. `Id` 主键；`UserId`；可选 `DomainProjectId` + FK 导航 `DomainProject`。
  3. `Name` MaxLength 255；`Kind` 默认 `"task"`；`Status` 默认 `"Active"`。
  4. `CreatedAt`/`UpdatedAt` 默认 UtcNow；`DeletedAt` 可空。
  5. 导航 `Tasks` → `TaskEntity` 集合。
- 分支与异常：本类型无校验逻辑
- 调用：被任务本/任务服务读写

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`。
2. 命名空间 `Pim.Module.Calendar.Entities`；`[Table("task_books")]`。
3. 类实现 `ISoftDeletable`。
4. Id/UserId/DomainProjectId/Name/Kind/Status/时间戳/DeletedAt。
5. FK DomainProject；集合 Tasks 初始化为空 List。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs",
      "label": "TaskBookEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" }
  ]
}
```
