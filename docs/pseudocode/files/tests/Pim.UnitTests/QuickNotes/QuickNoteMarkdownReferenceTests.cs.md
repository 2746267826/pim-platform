# tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：从 Markdown 提取本地附件 download URL 中的 attachment Guid。
- 主要依赖：`QuickNoteMarkdownReferences.ExtractAttachmentIds`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ExtractAttachmentIds_ReturnsIdsFromImageAndFileLinks
### ExtractAttachmentIds_IgnoresDuplicatesAndInvalidUrls
### ExtractAttachmentIds_IgnoresExternalAbsoluteUrlsWithLocalAttachmentPath
### ExtractAttachmentIds_IgnoresUrlsWithDownloadSuffixes
### ExtractAttachmentIds_ReturnsEmptyForBlankMarkdown

## 近逐行中文伪代码

1. [L8-21] 图片与文件链接
2. [L23-38] 去重与非法
3. [L40-54] 绝对外链忽略
4. [L56-70] download-extra 后缀忽略
5. [L72-81] 空白

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs",
      "label": "QuickNoteMarkdownReferenceTests",
      "path": "tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteMarkdownReferenceTests.cs", "to": "src/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs", "type": "tests" }
  ]
}
```
