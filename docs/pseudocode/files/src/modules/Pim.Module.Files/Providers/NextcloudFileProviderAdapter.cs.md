# src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：实现 `IFileProviderAdapter`，通过 Nextcloud WebDAV（PROPFIND/PUT/GET/MOVE/DELETE）完成连接测试、目录列举、元数据、上传下载、移动重命名、回收站与版本管理，并构造 Web 打开链接；内含路径安全校验与响应流包装。
- 主要依赖：
  - `HttpClient`、WebDAV XML（`NextcloudDavXmlParser`）
  - `FileProviderConnection` 与 Provider DTO
  - `DomainException`（路径不安全 5202）
- 被谁使用：Files 模块在 Provider=Nextcloud 时的文件操作编排

## 函数级结构化伪代码

### NextcloudFileProviderAdapter
#### 构造 / 静态 HttpMethod
- 输入：HttpClient
- 输出：适配器实例
- 副作用：无
- 步骤：保存 `_httpClient`；定义 PROPFIND、MOVE 方法常量。
- 分支与异常：无
- 调用：无

#### `TestConnectionAsync`
- 输入：connection、ct
- 输出：`FileProviderTestResult`
- 副作用：对根路径 PROPFIND
- 步骤：`GetMetadataAsync("/",)` 成功 → (true, connected)；取消原样抛；其他异常 → (false, error, message)。
- 分支与异常：见上
- 调用：GetMetadataAsync

#### `ListFolderAsync` / `GetMetadataAsync`
- 输入：connection、path
- 输出：列表或单项 ProviderFileItem
- 副作用：HTTP PROPFIND（depth 1/0）
- 步骤：
  1. 建 PropFind 请求；Send；EnsureSuccess。
  2. 解析 XML；List 过滤掉与请求路径相同的自身项；Get 取 First。
- 分支与异常：HTTP 失败抛 HttpRequestException
- 调用：CreatePropFindRequest、NextcloudDavXmlParser

#### `UploadAsync` / `DownloadAsync`
- 输入：连接、路径/流/contentType 或 path
- 输出：上传后元数据 / ProviderDownload
- 副作用：PUT 或 GET
- 步骤：
  1. Upload：PUT StreamContent + ContentType → 成功后 GetMetadata。
  2. Download：GET ResponseHeadersRead → 包装 `ResponseDisposingStream`、content-type、文件名。
- 分支与异常：EnsureSuccess
- 调用：CreateRequest、GetMetadataAsync

#### `MoveAsync` / `RenameAsync` / `DeleteToTrashAsync`
- 输入：源/目标路径或新名
- 输出：移动后元数据 / void
- 副作用：MOVE 或 DELETE
- 步骤：
  1. Move：Destination 头、Overwrite=F。
  2. Rename：校验文件名安全 → Parent+name → Move。
  3. Delete：DELETE 文件 URL（依赖服务端进 trashbin）。
- 分支与异常：非法名 5202
- 调用：CreateMoveRequest、ValidateRenameName

#### 回收站与版本
- `ListTrashAsync`：PROPFIND trash 根 depth1 → ParseTrashItems。
- `RestoreTrashAsync`：MOVE trash/{id} → restore。
- `ListVersionsAsync` / `DownloadVersionAsync` / `RestoreVersionAsync`：versions 路径 PROPFIND/GET/MOVE restore。
- 分支与异常：路径段校验 5202；HTTP 失败
- 调用：EscapeSinglePathSegment、Create*

#### `BuildOpenLink`
- 输入：connection、path、mode、optional externalFileId
- 输出：`ProviderOpenLink`
- 副作用：无
- 步骤：`/apps/files/files?dir=parent&mode=`；可选 openfile=校验后的 id。
- 分支与异常：externalFileId 非法 5202
- 调用：ParentPath、ValidateSinglePathSegment

#### HTTP 与 URL 辅助
- CreatePropFind/Move/Request：Basic Auth（Username:AppPassword Base64）；Depth；Destination。
- EnsureSuccess：2xx 或 MultiStatus；否则读 body 抛 HttpRequestException 并 Dispose response。
- Dav/Files/Trash/Versions 根 URL：优先 InternalBaseUrl。
- EscapePath：分段 Escape + 禁止 `.`/`..`/空；Validate* 抛 5202。
- ParentPath/CombinePath/NormalizePath/FileNameFromResponse/PropfindBody。

#### 嵌套 `ResponseDisposingStream`
- 包装 inner Stream + HttpResponseMessage；读写委托 inner；Dispose/DisposeAsync 同时释放 response。

## 近逐行中文伪代码

1. 实现 IFileProviderAdapter；注入 HttpClient。
2. 测连：根元数据成功即 connected。
3. 列目录/元数据：PROPFIND + XML 解析。
4. 上传 PUT 后回读元数据；下载 GET 流 + 释放响应包装。
5. MOVE 改路径；Rename 拼父路径；Delete 调 WebDAV DELETE。
6. trashbin/versions 的列举、恢复、下载版本。
7. 打开链接指向 Nextcloud files 应用。
8. Basic Auth；路径段安全校验（5202）；MultiStatus 视为成功。
9. ResponseDisposingStream 保证流结束时释放 HttpResponse。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs",
      "label": "NextcloudFileProviderAdapter",
      "path": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "to": "src/modules/Pim.Module.Files/Providers/NextcloudDavXmlParser.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" }
  ]
}
```
