# src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：领域项目（domain project）EF 实体，表 `domain_projects`，支持软删除，并导航到 TaskBook/Task 集合。
- 主要依赖：
  - `System.ComponentModel.DataAnnotations` / Schema
  - `Pim.Core.Data.ISoftDeletable`
  - `TaskBookEntity`、`TaskEntity`（导航）
- 被谁使用：`PimDbContext` 映射；日历/规划相关服务与查询

## 函数级结构化伪代码

### DomainProjectEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：持久化字段与导航
- 副作用：无
- 步骤：
  1. 表名 `domain_projects`；实现 `ISoftDeletable`。
  2. `Id` 默认 `Guid.NewGuid()`；`UserId` 归属用户。
  3. `Name` MaxLength 255；`Description` 可空；`Status` 默认 `"Active"` MaxLength 40。
  4. `CreatedAt`/`UpdatedAt` 默认 UtcNow；`DeletedAt` 可空软删。
  5. 导航：`TaskBooks`、`Tasks` 初始化空列表。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`。
2. 命名空间 `Pim.Module.Calendar.Entities`；`[Table("domain_projects")]`。
3. 类实现 `ISoftDeletable`；主键 id、user_id、name、description、status。
4. 时间戳 created_at/updated_at/deleted_at。
5. 集合导航 TaskBooks 与 Tasks。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs",
      "label": "DomainProjectEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs", "type": "depends_on" }
  ]
}
```
