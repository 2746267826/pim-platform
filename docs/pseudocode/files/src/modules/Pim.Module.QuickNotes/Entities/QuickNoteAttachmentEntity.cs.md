# src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：速记附件表 `quick_note_attachments` 实体，记录对象存储键、文件元数据与软删除，可选关联 `QuickNoteEntity`。
- 主要依赖：`ISoftDeletable`（Pim.Core.Data）、`QuickNoteEntity`（外键导航）
- 被谁使用：QuickNotes 服务/端点、`PimDbContext`

## 函数级结构化伪代码

### QuickNoteAttachmentEntity
#### 属性与导航（无自定义方法）
- 输入：无（POCO / EF 实体）
- 输出：字段与导航
- 副作用：无
- 步骤：
  1. 映射表 `quick_note_attachments`；实现 `ISoftDeletable`。
  2. `Id`：Guid 主键，默认 `Guid.NewGuid()`。
  3. `QuickNoteId`：可空 Guid，所属速记；`UserId`：所属用户。
  4. `StorageProvider`：默认 `"minio"`，最长 32。
  5. `ObjectKey`、`FileName`：存储对象键与原始文件名。
  6. `ContentType`：默认 `application/octet-stream`；`SizeBytes`；可选 `ContentHash`。
  7. `MetadataJson`：jsonb，默认 `"{}"`。
  8. `CreatedAt` 默认 UtcNow；`DeletedAt` 可空实现软删除。
  9. 导航 `QuickNote` 经 `QuickNoteId` 外键。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema 与 `Pim.Core.Data`。
2. 命名空间 `Pim.Module.QuickNotes.Entities`；表 `quick_note_attachments`。
3. 类实现 `ISoftDeletable`。
4. `Id` 主键 Guid 默认新 Guid。
5. `QuickNoteId` 可空；`UserId` 必填。
6. `StorageProvider` 默认 minio；`ObjectKey`/`FileName` 空串默认。
7. `ContentType` 默认 octet-stream；`SizeBytes`；可选 `ContentHash`。
8. `MetadataJson` jsonb 默认 `{}`；`CreatedAt`；`DeletedAt`。
9. 外键导航到 `QuickNoteEntity`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs",
      "label": "QuickNoteAttachmentEntity",
      "path": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs.md",
      "layer": "module.quicknotes",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "to": "src/Pim.Core/Data", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "type": "depends_on" }
  ]
}
```
