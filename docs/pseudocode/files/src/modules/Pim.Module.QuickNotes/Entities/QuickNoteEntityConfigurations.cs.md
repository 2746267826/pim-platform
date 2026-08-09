# src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：EF Core 实体类型配置——速记主体与附件表的默认值、软删除过滤、索引与一对多关系。
- 主要依赖：`Microsoft.EntityFrameworkCore`、`QuickNoteEntity`、`QuickNoteAttachmentEntity`、`QuickNoteStatuses`、`QuickNoteSources`
- 被谁使用：模块注册/DbContext `ApplyConfigurationsFromAssembly` 或显式 `ApplyConfiguration`

## 函数级结构化伪代码

### QuickNoteEntityConfiguration
#### `void Configure(EntityTypeBuilder<QuickNoteEntity> builder)`
- 输入：`QuickNoteEntity` 的 `EntityTypeBuilder`
- 输出：无（就地配置模型）
- 副作用：改写 EF 元模型（过滤、默认值、索引、关系）
- 步骤：
  1. `HasQueryFilter`：`DeletedAt == null`（全局软删除过滤）。
  2. 属性默认值：`ContentMarkdown`→`""`；`Status`→`Inbox`；`Source`→`WebPage`；`MetadataJson`→`"{}"`。
  3. 时间默认：`CreatedAt`/`UpdatedAt` 使用 SQL `now()`。
  4. 索引：`(UserId, Status, UpdatedAt)`；`(UserId, CreatedAt)`。
  5. 关系：`HasMany(Attachments).WithOne(QuickNote).HasForeignKey(QuickNoteId).OnDelete(SetNull)`。
- 分支与异常：无运行时分支
- 调用：EF Fluent API

### QuickNoteAttachmentEntityConfiguration
#### `void Configure(EntityTypeBuilder<QuickNoteAttachmentEntity> builder)`
- 输入：附件实体 builder
- 输出：无
- 副作用：过滤/默认/索引
- 步骤：
  1. 软删除过滤 `DeletedAt == null`。
  2. 默认：`StorageProvider`→`"minio"`；`ContentType`→`"application/octet-stream"`；`MetadataJson`→`"{}"`；`CreatedAt`→`now()`。
  3. 索引：`QuickNoteId`；`(UserId, CreatedAt)`；`(UserId, DeletedAt)`。
- 分支与异常：无
- 调用：EF Fluent API

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.QuickNotes.Entities`；引用 EF Core 与 Builders。
2. `QuickNoteEntityConfiguration` 实现 `IEntityTypeConfiguration<QuickNoteEntity>`。
3. Configure：查询过滤未删除；Markdown/状态/来源/元数据 JSON 默认值；创建/更新时间 SQL now。
4. 用户+状态+更新时间、用户+创建时间复合索引。
5. 一对多附件，外键 `QuickNoteId`，删除行为 SetNull。
6. `QuickNoteAttachmentEntityConfiguration` 同理：软删除过滤。
7. 存储提供商 minio、内容类型 octet-stream、元数据 {}、CreatedAt now。
8. 按笔记 Id、用户+创建、用户+删除时间建索引。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs",
      "label": "QuickNoteEntityConfigurations",
      "path": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs.md",
      "layer": "module.quicknotes",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntityConfigurations.cs", "type": "depends_on" }
  ]
}
```
