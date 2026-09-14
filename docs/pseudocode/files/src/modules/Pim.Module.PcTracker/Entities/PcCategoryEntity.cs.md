# src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类树节点实体，支持父子层级、颜色图标、生产力标签与排序。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`System.ComponentModel.DataAnnotations.Schema`
- 被谁使用：PcTracker 分类管理/统计服务、EF 映射与迁移

## 函数级结构化伪代码

### PcCategoryEntity
#### 属性组与导航（EF 实体 POCO，无业务方法）
- 输入：服务层赋值后由 EF 持久化
- 输出：PC 分类表一行及可选父子导航
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 主键 `Id`；可选 `ParentId` 外键指向父分类。
  2. `Name` 最长 64；`Color` 最长 7 默认 `#64748b`。
  3. `Icon` 可空最长 32；`Productivity` 最长 16 默认 `"neutral"`。
  4. `SortOrder`；`IsBuiltin`。
  5. `CreatedAt`/`UpdatedAt` 默认 `DateTime.UtcNow`。
  6. 导航：`Parent`（ForeignKey ParentId）；`Children` 集合默认空列表。
- 分支与异常：本类型无校验逻辑
- 调用：被 PcTracker 服务读写；自引用树

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema。
2. 命名空间 `Pim.Module.PcTracker.Entities`；类 `PcCategoryEntity`。
3. `Id` 主键；`ParentId` 可空。
4. `Name`/`Color`/`Icon`/`Productivity` 字符串字段与默认值。
5. `SortOrder`、`IsBuiltin`、时间戳。
6. `Parent` 外键导航；`Children` 子节点集合初始化为空 List。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs",
      "label": "PcCategoryEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "type": "depends_on" }
  ]
}
```
