# src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：快速记录 CRUD/状态流转（inbox/processed/archived）、软删除、附件绑定同步、列表分页搜索、审计写入、Markdown 预览与附件 URL 映射。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`IAuditLogService`、`QuickNoteAttachmentService`、`QuickNoteEntity`/`QuickNoteAttachmentEntity`、`QuickNoteDtos`、`DomainException`、`PagedResult`
- 被谁使用：`QuickNotesModule` HTTP 端点（List/Get/Create/Update/Process/Archive/Restore/Delete）

## 函数级结构化伪代码

### QuickNoteService
#### QuickNoteService(db, currentUser, auditLog, attachments)
- 输入：DbContext、当前用户、审计、附件服务
- 输出：实例
- 副作用：无
- 步骤：保存四依赖
- 分支与异常：无
- 调用：无

#### Guid UserId（属性）
- 输入：无
- 输出：当前用户 Guid
- 副作用：未登录抛 DomainException(1002)
- 步骤：`_currentUser.UserId ?? throw`
- 分支与异常：未登录 → 1002
- 调用：`ICurrentUserService`

#### Task\<PagedResult\<QuickNoteListItemDto\>\> ListAsync(query, ct)
- 输入：Status/Search/Page/PageSize
- 输出：分页列表项
- 副作用：只读查询
- 步骤：
  1. UserId；page=max(1)；pageSize=clamp(1..100)
  2. 过滤当前用户笔记
  3. 有 Status → ValidateStatus 后过滤
  4. 有 Search → ContentMarkdown.Contains
  5. Count；算 totalPages
  6. AsNoTracking 按 UpdatedAt 降序 Skip/Take；投影含未删附件数
  7. BuildPreview 生成列表项；返回 PagedResult
- 分支与异常：非法 Status → 4003
- 调用：EF、`ValidateStatus`、`BuildPreview`

#### Task\<QuickNoteDetailDto\> GetAsync(id, ct)
- 输入：笔记 Id
- 输出：详情
- 副作用：读库
- 步骤：LoadNoteAsync → MapDetail
- 分支与异常：不存在 → 4004
- 调用：`LoadNoteAsync`、`MapDetail`

#### Task\<QuickNoteDetailDto\> CreateAsync(request, ct)
- 输入：Create 请求
- 输出：新建详情
- 副作用：插入笔记、绑定附件、Save、审计 create
- 步骤：
  1. UserId、now；MergeAttachmentIds（显式+Markdown）
  2. LoadBindableAttachmentsAsync(ids, null)
  3. new QuickNoteEntity：Inbox、NormalizeSource、时间戳
  4. Add；foreach 附件设 QuickNoteId 并加入导航
  5. Save；RecordAudit create；MapDetail
- 分支与异常：未登录/附件服务异常向上抛
- 调用：`_attachments`、EF、`RecordAuditAsync`

#### Task\<QuickNoteDetailDto\> UpdateAsync(id, request, ct)
- 输入：Id、Update 请求
- 输出：更新后详情
- 副作用：改内容/状态/归档时间、软删未再绑定附件、绑定新附件、审计 update
- 步骤：
  1. LoadNote；可选 Status 校验并设 ArchivedAt
  2. 写 ContentMarkdown、UpdatedAt
  3. MergeAttachmentIds；LoadBindable；不在集合内的附件 DeletedAt=now
  4. 可绑定附件设 QuickNoteId 并补入导航
  5. Save；审计；MapDetail
- 分支与异常：不存在/非法状态
- 调用：附件服务、EF、审计

#### Task\<QuickNoteDetailDto\> ProcessAsync(id, ct)
- 输入：Id
- 输出：详情
- 副作用：Status=Processed、ArchivedAt=null、审计 process
- 步骤：Load → 改状态 → Save → 审计 → MapDetail
- 分支与异常：不存在
- 调用：Load/Save/审计

#### Task\<QuickNoteDetailDto\> ArchiveAsync(id, ct)
- 输入：Id
- 输出：详情
- 副作用：Status=Archived、ArchivedAt=now、审计 archive
- 步骤：同 Process 模式
- 分支与异常：不存在
- 调用：同上

#### Task\<QuickNoteDetailDto\> RestoreAsync(id, request, ct) / RestoreAsync(id, status, ct)
- 输入：Id、目标 Status（可空则 Inbox）
- 输出：详情
- 副作用：改状态与 ArchivedAt、审计 restore
- 步骤：重载转发；ValidateStatus；Load；设状态；Save；审计
- 分支与异常：非法状态/不存在
- 调用：ValidateStatus、Load、审计

#### Task DeleteAsync(id, ct)
- 输入：Id
- 输出：无
- 副作用：笔记与未删附件软删、审计 delete
- 步骤：Load；DeletedAt/UpdatedAt=now；附件未删则 DeletedAt；Save；审计
- 分支与异常：不存在
- 调用：Load、审计

#### LoadNoteAsync(id, ct)（私有）
- 输入：Id
- 输出：含 Attachments 的实体
- 副作用：读库
- 步骤：UserId；Include Attachments；Id+UserId 匹配否则 4004
- 分支与异常：4004
- 调用：EF

#### RecordAuditAsync(action, noteId, userId, ct)（私有）
- 输入：动作名、资源 Id、用户
- 输出：无
- 副作用：写审计
- 步骤：CreateAuditLogRequest(resourceType=quick_note, source=quick-notes, Success)
- 调用：`IAuditLogService.RecordAsync`

#### ValidateStatus / NormalizeSource / BuildPreview / MergeAttachmentIds / MapDetail / MapAttachment（静态私有）
- ValidateStatus：IsValid 否则 4003
- NormalizeSource：空 → WebPage
- BuildPreview：去换行与 `#*_`\`，截断 140
- MergeAttachmentIds：显式 Id 去重 + Markdown 引用 Id
- MapDetail：未删附件按 CreatedAt/Id 排序后 MapAttachment
- MapAttachment：DownloadUrl 固定路径；image/* 则 PreviewUrl=下载 URL
- 调用：`QuickNoteStatuses`、`QuickNoteSources`、`QuickNoteMarkdownReferences`

## 近逐行中文伪代码

1. 引用 EF、Core.Common/Exceptions/Operations、Auth、Data、DTOs、Entities
2. 常量 ResourceType=`quick_note`、AuditSource=`quick-notes`
3. 字段：_db、_currentUser、_auditLog、_attachments；构造注入
4. UserId：未登录 DomainException 1002
5. ListAsync：分页夹紧；用户过滤；Status/Search；Count；投影 AttachmentCount；BuildPreview；PagedResult
6. GetAsync：LoadNote → MapDetail
7. CreateAsync：合并附件 Id → 可绑定附件 → Inbox 实体 → 绑定 → Save → 审计 create
8. UpdateAsync：可选状态与 ArchivedAt → 内容 → 同步附件软删/绑定 → Save → 审计
9. Process/Archive：改状态与 ArchivedAt → Save → 审计
10. Restore 两重载：默认 Inbox；校验状态；清/设 ArchivedAt
11. DeleteAsync：软删笔记与附件 → 审计
12. LoadNote：Include 附件；用户隔离；否则 4004
13. RecordAudit：统一 CreateAuditLogRequest
14. ValidateStatus / NormalizeSource / BuildPreview / MergeAttachmentIds
15. MapDetail / MapAttachment：生成 `/api/v1/quick-notes/attachments/{id}/download` 与图片预览

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs",
      "label": "QuickNoteService",
      "path": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "type": "calls" }
  ]
}
```
