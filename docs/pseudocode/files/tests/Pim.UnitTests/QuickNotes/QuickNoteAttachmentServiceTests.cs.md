# tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：速记附件上传、绑定 markdown/显式 Id、更新解绑、跨用户/已删拒绝、下载门禁。
- 主要依赖：`QuickNoteAttachmentService`、`QuickNoteService`、FakeObjectStorage
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Upload 临时附件+对象存储路径
2. Create 绑定 markdown 与显式 Id
3. 仅显式 Id 无 markdown 亦可绑定
4. Update null attachmentIds 软删未再引用
5. 拒绝他用户/已删附件绑定与下载

## 近逐行中文伪代码

1. 多 Fact 覆盖上传绑定与权限
2. FakeObjectStorage 与 Create* helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs",
      "label": "QuickNoteAttachmentServiceTests",
      "path": "tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteAttachmentServiceTests.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "type": "tests" }
  ]
}
```
