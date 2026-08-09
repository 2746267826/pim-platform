# src/Pim.Core/Data/ISoftDeletable.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义软删除契约，要求实体暴露可空的 `DeletedAt` 时间戳。
- 主要依赖：无（仅 BCL `DateTimeOffset`）
- 被谁使用：`UserEntity`、QuickNotes/Calendar 等模块实体；EF 全局查询过滤与业务软删逻辑

## 函数级结构化伪代码

### ISoftDeletable
#### 属性 `DateTimeOffset? DeletedAt { get; set; }`
- 输入：无（读写属性）
- 输出：`null` 表示未删除；非空表示软删除发生时刻
- 副作用：由实现类型的 setter 写入实体状态
- 步骤：
  1. 实现类型提供 `DeletedAt` 存储与访问
  2. 查询层通常过滤 `DeletedAt == null` 的活动行
- 分支与异常：无控制流
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Data`
2. 声明公共接口 `ISoftDeletable`
3. 要求实现方提供可空 `DateTimeOffset` 属性 `DeletedAt`（get/set）
4. （文件结束；无方法体）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Data/ISoftDeletable.cs",
      "label": "ISoftDeletable",
      "path": "src/Pim.Core/Data/ISoftDeletable.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Data/ISoftDeletable.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" }
  ]
}
```
