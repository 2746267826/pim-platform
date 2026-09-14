# src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：快速记录模块的请求/响应 DTO 与列表查询参数；列表项含预览与附件计数，详情含 Markdown 全文与附件列表。
- 主要依赖：`Pim.Core.Common.PagedResult`
- 被谁使用：`QuickNoteService`、`QuickNotesModule` 端点、附件相关服务

## 函数级结构化伪代码

### QuickNoteListItemDto
#### record(Id, ContentPreview, Status, Source, AttachmentCount, CreatedAt, UpdatedAt, ArchivedAt)
- 输入：列表行字段
- 输出：不可变列表项
- 副作用：无
- 步骤：承载预览文案与未删除附件数
- 分支与异常：无
- 调用：无

### QuickNoteAttachmentDto
#### record(Id, FileName, ContentType, SizeBytes, DownloadUrl, PreviewUrl?, CreatedAt)
- 输入：附件元数据与下载/预览 URL
- 输出：详情中的附件项
- 副作用：无
- 步骤：图片类型通常 PreviewUrl=DownloadUrl
- 分支与异常：无
- 调用：无

### QuickNoteDetailDto
#### record(Id, ContentMarkdown, Status, Source, Attachments, MetadataJson, CreatedAt, UpdatedAt, ArchivedAt)
- 输入：全文 Markdown、状态、来源、附件集合、元数据 JSON
- 输出：详情 DTO
- 副作用：无
- 步骤：Attachments 为 `IReadOnlyList<QuickNoteAttachmentDto>`
- 分支与异常：无
- 调用：无

### CreateQuickNoteRequest
#### record(ContentMarkdown, Source?, AttachmentIds?)
- 输入：创建正文、可选来源、可选显式附件 Id
- 输出：创建请求
- 副作用：无
- 步骤：服务层还会从 Markdown 引用合并附件 Id
- 分支与异常：无
- 调用：无

### UpdateQuickNoteRequest
#### record(ContentMarkdown, Status?, AttachmentIds?)
- 输入：更新正文、可选状态、可选附件 Id 集合
- 输出：更新请求
- 副作用：无
- 步骤：Status 空则保留原状态
- 分支与异常：无
- 调用：无

### RestoreQuickNoteRequest
#### record(Status)
- 输入：恢复目标状态字符串
- 输出：恢复请求
- 副作用：无
- 步骤：由 `RestoreAsync` 校验
- 分支与异常：无
- 调用：无

### QuickNoteAttachmentUploadDto
#### record(Id, FileName, ContentType, SizeBytes, DownloadUrl, PreviewUrl?)
- 输入：上传完成后的附件元数据（无 CreatedAt）
- 输出：上传响应 DTO
- 副作用：无
- 步骤：与 `QuickNoteAttachmentDto` 字段接近但缺 CreatedAt
- 分支与异常：无
- 调用：无

### QuickNoteListQuery
#### record(Status?, Search?, Page, PageSize)
- 输入：状态过滤、全文搜索、分页
- 输出：列表查询参数
- 副作用：无
- 步骤：服务层 Page≥1、PageSize clamp 1..100
- 分支与异常：无
- 调用：无

### QuickNoteListResponse
#### record(Result: PagedResult\<QuickNoteListItemDto\>)
- 输入：分页结果包装
- 输出：列表响应外壳
- 副作用：无
- 步骤：包装 `PagedResult`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 `Pim.Core.Common`
2. 命名空间 `Pim.Module.QuickNotes.DTOs`
3. `QuickNoteListItemDto`：Id、ContentPreview、Status、Source、AttachmentCount、时间戳、ArchivedAt
4. `QuickNoteAttachmentDto`：Id、FileName、ContentType、SizeBytes、DownloadUrl、PreviewUrl、CreatedAt
5. `QuickNoteDetailDto`：全文 ContentMarkdown、Attachments 列表、MetadataJson、时间戳
6. `CreateQuickNoteRequest`：ContentMarkdown、可选 Source、可选 AttachmentIds
7. `UpdateQuickNoteRequest`：ContentMarkdown、可选 Status、可选 AttachmentIds
8. `RestoreQuickNoteRequest`：仅 Status
9. `QuickNoteAttachmentUploadDto`：上传结果字段（无 CreatedAt）
10. `QuickNoteListQuery`：Status/Search/Page/PageSize
11. `QuickNoteListResponse`：包装 `PagedResult<QuickNoteListItemDto>`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs",
      "label": "QuickNoteDtos",
      "path": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs.md",
      "layer": "module.quicknotes",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "to": "src/Pim.Core/Common", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "type": "depends_on" }
  ]
}
```
