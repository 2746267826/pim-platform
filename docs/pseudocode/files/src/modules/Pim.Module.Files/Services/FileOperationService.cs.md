# src/modules/Pim.Module.Files/Services/FileOperationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：当前用户文件操作：列表/详情、移动重命名删除、上传下载、回收站、提供方同步、版本列表/下载/恢复与预览、打开链接、建议列表与接受/驳回；同步元数据到 DB 并写审计。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IAuditLogService`、`FileProviderBindingService`、`IFileProviderAdapter`、Files 实体与 DTO
- 被谁使用：Files 模块 HTTP 端点

## 函数级结构化伪代码

### FileOperationService
#### 构造与 `UserId` 属性
- 输入：db、currentUser、auditLog、providerBindings、adapter
- 输出：服务实例；`UserId` 取自当前用户
- 副作用：无
- 步骤：主构造函数注入；`UserId` 为 null 时抛 `DomainException(1002,"未登录")`
- 分支与异常：未登录 1002
- 调用：`currentUser.UserId`

#### `Task<PagedResult<FileItemDto>> ListItemsAsync(FileListQuery query, ct)`
- 输入：父路径查询
- 输出：单页 `PagedResult`（Page=1，全量子项）
- 副作用：只读查询
- 步骤：
  1. 规范化 parentPath；前缀 `/` 或 `{path}/`
  2. 查当前用户、未删除、Path 以前缀开头的项（含 IndexJobs）
  3. 内存过滤直接子路径；文件夹优先、名忽略大小写、Id
  4. Map 为 DTO 填 PagedResult
- 分支与异常：无匹配则空列表 TotalPages=0
- 调用：`NormalizePath`、`IsDirectChildPath`、`MapFileItem`

#### `Task<FileItemDto> GetItemAsync(id, ct)`
- 输入：文件项 Id
- 输出：DTO
- 副作用：只读
- 步骤：`LoadItemAsync` → `MapFileItem`
- 分支与异常：不存在 5300
- 调用：`LoadItemAsync`

#### `Task<FileItemDto> MoveAsync(id, MoveFileRequest, ct)`
- 输入：Id、目标路径
- 输出：更新后 DTO
- 副作用：提供方 Move；DB 路径/子树；版本；审计 `files.move`
- 步骤：
  1. Load 项；目标规范化，根 `/` → 5301
  2. adapter.Move；ApplyProviderItem；文件夹则 UpdateDescendantPaths
  3. 文件则 UpsertCurrentVersion；Save；审计
- 分支与异常：5301 目标无名；提供方失败向上
- 调用：adapter、`ApplyProviderItem`、`UpdateDescendantPathsAsync`、`UpsertCurrentVersionAsync`、`RecordAuditAsync`

#### `Task<FileItemDto> RenameAsync(id, RenameFileRequest, ct)`
- 输入：Id、新名
- 输出：DTO
- 副作用：提供方 Rename；子树路径；版本；审计 `files.rename`
- 步骤：NormalizeRenameName → adapter.Rename → 同 Move 的元数据/子树/版本/审计
- 分支与异常：5302 非法文件名
- 调用：同 Move 模式

#### `Task DeleteAsync(id, ct)`
- 输入：Id
- 输出：无
- 副作用：提供方进回收站；本地 IsDeleted；子项标记删除；审计 `files.delete_to_trash`
- 步骤：Load → DeleteToTrash → 标记自身与 MarkDescendantsDeleted → Save → 审计
- 分支与异常：5300
- 调用：adapter、`MarkDescendantsDeletedAsync`

#### `Task<FileItemDto> UploadAsync(providerId, destinationPath, content, contentType, ct)`
- 输入：提供方、目标路径、流、MIME
- 输出：DTO
- 副作用：adapter.Upload；新建或更新 FileItem；版本；provider.UpdatedAt；审计 `files.upload`
- 步骤：
  1. 路径须含文件名否则 5301；校验提供方属用户否则 5104
  2. Upload；按 ExternalFileId 找或建项；ApplyProviderItem
  3. 文件：新项先 Save 再 UpsertCurrentVersion；再 Save 与审计
- 分支与异常：5301/5104
- 调用：adapter、`UpsertCurrentVersionAsync`

#### `Task<ProviderDownload> DownloadAsync(id, ct)`
- 输入：Id
- 输出：提供方下载结果
- 副作用：只读下载流
- 步骤：Load；folder → 5303；adapter.Download
- 分支与异常：5303 文件夹
- 调用：adapter.DownloadAsync

#### `Task<IReadOnlyList<ProviderTrashItem>> ListTrashAsync(ct)`
- 输入：无
- 输出：各提供方回收站合并列表
- 副作用：只读远程
- 步骤：枚举用户 providerId（CreatedAt/Id 序）；逐个 ListTrash 累加
- 分支与异常：无
- 调用：adapter.ListTrashAsync

#### `Task RestoreTrashAsync(providerId, trashId, ct)`
- 输入：提供方与回收站项 Id
- 输出：无
- 副作用：远程恢复；审计 `files.trash_restore`（资源类型 file_provider）
- 步骤：GetConnection → RestoreTrash → RecordAudit
- 分支与异常：绑定失败向上
- 调用：adapter、`RecordAuditAsync`

#### `Task<IReadOnlyList<FileItemDto>> SyncProviderAsync(providerId, ct)`
- 输入：提供方 Id
- 输出：同步后根下条目 DTO 列表
- 副作用：ListFolder `/`；upsert 项；根下未见则软删；LastSyncAt；文件版本二次 Save
- 步骤：
  1. 校验提供方；ListFolder 根；加载该提供方全部项字典
  2. 按 ExternalFileId 去重 upsert ApplyProviderItem
  3. 根直接子且未见 → IsDeleted；更新 provider 时间；Save
  4. 对文件 UpsertCurrentVersion 再 Save；排序 Map
- 分支与异常：5104
- 调用：adapter.ListFolderAsync、`ApplyProviderItem`、`UpsertCurrentVersionAsync`

#### `Task<IReadOnlyList<FileVersionDto>> ListVersionsAsync(id, ct)`
- 输入：文件项 Id
- 输出：版本 DTO（ExternalVersionId 降序）
- 副作用：从提供方同步版本行、当前版本标记
- 步骤：adapter.ListVersions；按 ExternalVersionId upsert；当前版本清其它 IsCurrent 并设 item.CurrentVersionId；Save；再查询 Map
- 分支与异常：5300
- 调用：adapter、`MapVersion`、`NormalizeVersionSource`

#### `Task<ProviderDownload> DownloadVersionAsync(id, versionId, ct)`
- 输入：文件与版本 Id
- 输出：历史版本下载
- 副作用：远程读
- 步骤：Load 项与版本 → adapter.DownloadVersion
- 分支与异常：5300/5304
- 调用：adapter.DownloadVersionAsync

#### `Task RestoreVersionAsync(id, versionId, ct)`
- 输入：文件与版本 Id
- 输出：无
- 副作用：远程恢复版本；本地当前版本与元数据；审计 `files.version_restore`
- 步骤：
  1. adapter.RestoreVersion
  2. 清其它 IsCurrent（必要时中间 Save）；设本版本当前并回写 item 的 Etag/Size/ModifiedAt
  3. Save + 审计
- 分支与异常：5300/5304
- 调用：adapter、`RecordAuditAsync`

#### `Task<VersionRestorePreviewDto> RestoreVersionPreviewAsync(id, versionId, ct)`
- 输入：文件与版本 Id
- 输出：预览（当前/目标标签、RequiresConfirmation=true、中文摘要）
- 副作用：只读
- 步骤：Load 项与版本；可选当前版本；FormatVersionLabel 拼文案
- 分支与异常：5304
- 调用：`FormatVersionLabel`

#### `Task<FileOpenLinkDto> BuildOpenLinkAsync(id, mode, ct)`
- 输入：Id、mode 默认 view
- 输出：Url + Mode
- 副作用：无（适配器构造链接）
- 步骤：Load → GetConnection → adapter.BuildOpenLink
- 分支与异常：5300
- 调用：adapter.BuildOpenLink

#### `Task<IReadOnlyList<FileSuggestionDto>> ListSuggestionsAsync(ct)`
- 输入：无
- 输出：当前用户文件相关建议 DTO
- 副作用：只读
- 步骤：FileSuggestion 经 FileItem.Provider.UserId 过滤；UpdatedAt/CreatedAt 降序 Map
- 分支与异常：无
- 调用：`MapSuggestion`

#### `Task<FileSuggestionDto> DismissSuggestionAsync` / `AcceptSuggestionAsync`
- 输入：建议 Id
- 输出：更新后 DTO
- 副作用：Status=dismissed|accepted；审计 suggestion_dismiss/accept
- 步骤：LoadSuggestion → 改状态与 UpdatedAt → Save → 审计（资源为 FileItemId）
- 分支与异常：5305
- 调用：`LoadSuggestionAsync`、`RecordAuditAsync`

#### 私有加载与映射/路径辅助
- 输入：Id 或路径字符串
- 输出：实体/DTO/规范化路径
- 副作用：Load 只读或带跟踪；审计写
- 步骤：
  1. LoadItem：用户+未删除+Provider；null→5300
  2. LoadSuggestion：含 FileItem.Provider；null→5305
  3. LoadVersion：null→5304
  4. ApplyProviderItem：路径/名/类型/MIME/大小/Etag/权限；清除删除；刷新同步时间
  5. UpsertCurrentVersion：externalId=`current:{etag|ExternalFileId}`；唯一 IsCurrent
  6. UpdateDescendantPaths / MarkDescendantsDeleted：仅文件夹
  7. RecordAudit：User 角色、source=files、Success
  8. IsDirectChildPath / NormalizePath / 重命名安全校验 / LatestIndexStatus
- 分支与异常：见上错误码
- 调用：EF、`IAuditLogService.RecordAsync`

## 近逐行中文伪代码

1. 注入 Db、当前用户、审计、绑定服务、提供方适配器；常量 resource=file、source=files
2. UserId 未登录抛 1002
3. ListItems：前缀查库 → 直接子过滤 → 文件夹优先排序 → 假分页
4. GetItem：Load + Map
5. Move/Rename：远程操作 → 应用元数据 → 子路径/版本 → Save → 审计
6. Delete：远程回收站 → 软删自身与子孙 → 审计
7. Upload：校验路径与提供方 → 远程上传 → upsert 项与版本 → 审计
8. Download：拒文件夹 → 远程下载
9. ListTrash/RestoreTrash：遍历提供方；恢复写审计
10. SyncProvider：根列表同步 upsert；未见根子项软删；文件补版本
11. ListVersions/DownloadVersion/RestoreVersion/Preview：版本同步与恢复预览
12. BuildOpenLink：适配器生成链接
13. 建议列表与 dismiss/accept
14. 私有：Load*、ApplyProviderItem、UpsertCurrentVersion、子树路径/删除、审计、路径与版本标签工具

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/FileOperationService.cs",
      "label": "FileOperationService",
      "path": "src/modules/Pim.Module.Files/Services/FileOperationService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/FileOperationService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "IFileProviderAdapter", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Endpoints", "to": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "type": "calls" }
  ]
}
```
