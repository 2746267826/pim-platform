# tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：速记默认 inbox 与软删过滤；附件可先临时无 note。
- 主要依赖：QuickNote/Attachment 实体、PimDbContext
- 被谁使用：dotnet test

## 函数级结构化伪代码

### QuickNote_DefaultsToInboxAndFiltersSoftDeletedRows
### QuickNoteAttachment_CanBeTemporaryBeforeNoteSave

## 近逐行中文伪代码

1. [L10-39] 软删过滤 + Status/Metadata 默认
2. [L41-63] 临时附件 QuickNoteId null
3. [L65-71] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs",
      "label": "QuickNoteModelTests",
      "path": "tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteModelTests.cs", "to": "src/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "type": "tests" }
  ]
}
```
