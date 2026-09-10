# src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：解析 Nextcloud/WebDAV PROPFIND XML，产出文件项、回收站项与版本列表；规范化路径与父 fileId。
- 主要依赖：
  - `System.Xml.Linq`
  - `Pim.Core.Exceptions.DomainException`
  - 提供方 DTO：`ProviderFileItem`/`ProviderTrashItem`/`ProviderFileVersion`
- 被谁使用：
  - `NextcloudFileProviderAdapter`（列表/回收站/版本）

## 函数级结构化伪代码

### NextcloudDavXmlParser（static）
#### 命名空间常量
- 输入：无
- 输出：XNamespace Dav/OwnCloud/Nextcloud
- 副作用：无
- 步骤：固定 DAV 与 owncloud/nextcloud NS
- 分支与异常：无
- 调用：无

#### `ParseItems(xml, hrefPrefix, requestedPath)`
- 输入：PROPFIND XML、href 前缀、请求路径（参数保留，实现未用 requestedPath 过滤）
- 输出：`IReadOnlyList<ProviderFileItem>`（含 ParentExternalFileId）
- 副作用：无 fileid 抛 5201
- 步骤：
  1. `ResponseProperties` → 每项 `ParseItem`。
  2. 建 byPath 字典（OrdinalIgnoreCase）。
  3. 投影 with ParentExternalFileId = ParentFileId(path, byPath)。
- 分支与异常：5201
- 调用：ParseItem/ParentFileId

#### `ParseTrashItems` / `ParseVersions`
- 输入：xml、hrefPrefix
- 输出：回收站/版本列表（null 过滤；版本按 ModifiedAt 降序）
- 副作用：无
- 步骤：ResponseProperties → ParseTrashItem/ParseVersion → Where not null → Cast
- 分支与异常：无
- 调用：Parse*

#### `ParseItem` / `ParseTrashItem` / `ParseVersion`（private）
- 输入：href、prop、prefix
- 输出：对应 DTO 或 null
- 副作用：无 fileid → 5201
- 步骤：
  - Item：NormalizePath(RemovePrefix(DecodeHref))；fileid 必填；collection→folder 否则 file；取 contenttype/length/etag/permissions/lastmodified。
  - Trash：trashId 空 null；文件名/原路径/删除时间 owncloud 或 nextcloud 元素；Unix 时间。
  - Version：versionId 空 null；etag/length/lastmodified；source nextcloud；isCurrent=false。
- 分支与异常：5201
- 调用：ElementValue/ParseHttpDate/ParseLong/ParseUnixTime

#### `ResponseProperties` / `SelectPropstat` / `IsSuccessfulPropstat`
- 输入：xml 或 response 元素
- 输出：成功 prop 的 (Href, Prop) 序列
- 步骤：
  1. XDocument.Parse；Descendants response；取 href 与 SelectPropstat 的 prop。
  2. SelectPropstat：优先 status 成功(2xx 或含 " 200 ") 的 propstat，否则无 status，否则第一个。
- 分支与异常：XML 非法抛 XDocument 异常
- 调用：ElementValue

#### 路径与解析辅助
- `DecodeHref` Unescape；`RemovePrefix` 去前缀；`NormalizePath` 斜杠与去尾；`NameFromPath`；`ParentPath`/`ParentFileId`；`ElementValue` trim；`ParseLong`；`ParseHttpDate` 失败 UnixEpoch；`ParseUnixTime` 失败 UnixEpoch。

## 近逐行中文伪代码

1. 引入 CultureInfo、XLinq、DomainException。
2. static 类；三命名空间常量。
3. ParseItems：解析→字典→补 ParentExternalFileId。
4. ParseTrashItems/ParseVersions：过滤 null；版本排序。
5. ParseItem：路径与 fileid；类型 folder/file；组装 ProviderFileItem。
6. ParseTrashItem：trashId/name/location/deletion；可 null。
7. ParseVersion：externalVersionId 与元数据。
8. ResponseProperties：遍历 response/propstat 成功属性。
9. IsSuccessfulPropstat：status 含 200 或 2xx 数字。
10. 路径 Decode/去前缀/规范化；父路径查 fileId；日期与长整型解析。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs",
      "label": "NextcloudDavXmlParser",
      "path": "src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs.md",
      "layer": "module.files",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "to": "src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs", "type": "calls" }
  ]
}
```
