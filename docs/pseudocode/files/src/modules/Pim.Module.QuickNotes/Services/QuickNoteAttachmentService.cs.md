# src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：快速记录附件上传/下载/软删除，以及绑定前校验可绑定附件集合。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IQuickNoteObjectStorage`、`QuickNoteAttachmentEntity`、`QuickNoteAttachmentUploadDto`
- 被谁使用：QuickNotes 模块端点 / `QuickNoteService` 绑定附件时

## 函数级结构化伪代码

### QuickNoteAttachmentService
#### UserId (property)
- 输入：无（读 `currentUser.UserId`）
- 输出：`Guid`
- 副作用：未登录抛 `DomainException(1002)`
- 步骤：
  1. 取 `currentUser.UserId`；为 null 则抛未登录。
- 分支与异常：`DomainException(1002, "未登录")`
- 调用：`ICurrentUserService`

#### UploadAsync(Stream content, string fileName, string? contentType, long sizeBytes, CancellationToken ct)
- 输入：内容流、文件名、可选 MIME、字节大小
- 输出：`QuickNoteAttachmentUploadDto`
- 副作用：对象存储写入；DB 插入 `QuickNoteAttachmentEntity`
- 步骤：
  1. 取 `userId`；校验 `fileName` 非空白；`sizeBytes >= 0`。
  2. 生成 `id`；`Path.GetFileName` 得到 `safeName`，再校验非空。
  3. 规范化 `contentType`（空白→`application/octet-stream`）。
  4. 组装 `objectKey = quick-notes/{userId:N}/{id:N}/{safeName}`，`storage.StoreAsync`。
  5. 新建实体：`QuickNoteId=null`、`StorageProvider="minio"`、记录 ObjectKey/元数据/`CreatedAt=UtcNow`。
  6. `Add` + `SaveChangesAsync`；`MapUpload` 返回。
- 分支与异常：`4007` 文件名空；`4008` 大小负
- 调用：`IQuickNoteObjectStorage.StoreAsync`、`PimDbContext.SaveChangesAsync`、`MapUpload`

#### DownloadAsync(Guid id, CancellationToken ct)
- 输入：附件 id
- 输出：`(Stream Content, string ContentType, string FileName)`
- 副作用：打开对象存储读流
- 步骤：
  1. 按 `id + userId` `AsNoTracking` 查实体，不存在抛 `4006`。
  2. `storage.OpenReadAsync(ObjectKey)`，返回流与 ContentType/FileName。
- 分支与异常：`DomainException(4006, "附件不存在")`
- 调用：`OpenReadAsync`

#### DeleteAsync(Guid id, CancellationToken ct)
- 输入：附件 id
- 输出：无
- 副作用：软删除（写 `DeletedAt`）并保存
- 步骤：
  1. 按 `id + userId` 查询（跟踪），不存在抛 `4006`。
  2. `DeletedAt = UtcNow`；`SaveChangesAsync`。
- 分支与异常：`4006`
- 调用：`SaveChangesAsync`

#### LoadBindableAttachmentsAsync(IEnumerable<Guid> attachmentIds, Guid? targetNoteId, CancellationToken ct)
- 输入：待绑定 id 列表、目标笔记 id（可空）
- 输出：按请求顺序的 `IReadOnlyList<QuickNoteAttachmentEntity>`
- 副作用：无写库
- 步骤：
  1. `userId`；`attachmentIds.Distinct().ToList()`；空则返回空数组。
  2. 按 id 集合 + userId 查出全部。
  3. 数量不等 → `4005`（缺失/越权）。
  4. 任一附件已绑定到其他笔记（`QuickNoteId` 有值且 ≠ `targetNoteId`）→ `4005`。
  5. 按 `ids` 顺序 `Single` 重排返回。
- 分支与异常：`DomainException(4005, "附件不能绑定到这条快速记录")`
- 调用：EF `Where`/`ToListAsync`

#### MapUpload(QuickNoteAttachmentEntity attachment) [private static]
- 输入：实体
- 输出：`QuickNoteAttachmentUploadDto`
- 副作用：无
- 步骤：
  1. `downloadUrl = BuildDownloadUrl(id)`。
  2. `ContentType` 以 `image/` 开头则 `previewUrl=downloadUrl`，否则 null。
  3. 组装 DTO（Id/FileName/ContentType/SizeBytes/urls）。
- 分支与异常：无
- 调用：`BuildDownloadUrl`

#### BuildDownloadUrl(Guid id) [private static]
- 输入：附件 id
- 输出：相对路径字符串
- 副作用：无
- 步骤：返回 `/api/v1/quick-notes/attachments/{id}/download`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 主构造注入 `db`、`currentUser`、`storage`。
2. `UserId`：无用户抛 1002。
3. `UploadAsync`：校验文件名与大小；安全文件名；默认 MIME；拼 objectKey；存对象存储。
4. 新建未绑定笔记的附件实体，`StorageProvider=minio`，落库后 `MapUpload`。
5. `DownloadAsync`：用户范围查附件；打开对象读流返回三元组。
6. `DeleteAsync`：用户范围查附件；写 `DeletedAt` 软删。
7. `LoadBindableAttachmentsAsync`：去重 id；空则空列表；全量命中且未绑他笔记；按输入顺序返回。
8. `MapUpload`：下载 URL；图片可预览；映射上传 DTO。
9. `BuildDownloadUrl`：固定 API 下载路径模板。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs",
      "label": "QuickNoteAttachmentService",
      "path": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/Pim.Infrastructure/Auth", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "type": "depends_on" }
  ]
}
```
