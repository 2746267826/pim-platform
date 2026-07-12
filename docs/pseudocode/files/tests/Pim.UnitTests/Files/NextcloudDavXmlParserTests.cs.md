# tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：解析 Nextcloud WebDAV multistatus XML 为文件项；解码 href；缺 fileid 抛错；多 propstat 取成功项。
- 主要依赖：`NextcloudDavXmlParser`、`DomainException`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ParseItems_MapsStableIdsPathsEtagsAndFolders
- 步骤：文件夹与文件映射 ExternalFileId/Path/Name/ItemType/Permissions/Size/Etag；子项 ParentExternalFileId

### ParseItems_UrlDecodesHrefAndNormalizesRootPath
- 步骤：根 `/`；`Q1%20Report.docx` 解码；父 id 挂到根 collection

### ParseItems_ThrowsWhenNormalFileEntryDoesNotIncludeFileId
- 步骤：DomainException 5201「Nextcloud 响应未包含文件 ID」

### ParseItems_UsesSuccessfulPropstatWhenErrorPropstatAppearsFirst
- 步骤：404 propstat 在前时仍取 200 的 fileid/size/etag

## 近逐行中文伪代码

1. [L9-42] 完整 multistatus 双 response
2. [L44-67] URL 解码与根路径
3. [L69-87] 缺 oc:fileid
4. [L89-114] 双 propstat 容错

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs",
      "label": "NextcloudDavXmlParserTests",
      "path": "tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/Pim.UnitTests/Files/NextcloudDavXmlParserTests.cs",
      "to": "src/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs",
      "type": "tests"
    }
  ]
}
```
