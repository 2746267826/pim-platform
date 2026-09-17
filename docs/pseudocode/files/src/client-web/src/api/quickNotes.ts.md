# src/client-web/src/api/quickNotes.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：速记 CRUD、状态流转（process/archive/restore）、附件上传与下载的前端 API。
- 主要依赖：`./client`（apiGet/Post/Put/Delete/Upload/DownloadBlob）、`../types` 速记相关类型
- 被谁使用：速记页面/组件

## 函数级结构化伪代码

### GetQuickNotesParams / buildQuery
- 输入：status/search/page/pageSize
- 输出：查询串或空
- 副作用：无
- 步骤：有值才 set；toString 后可选 `?`
- 分支与异常：无
- 调用：`URLSearchParams`

### quickNoteApiPaths
- list/detail/process/archive/restore/attachments/attachmentDownload 路径工厂

### getQuickNotes / getQuickNote / createQuickNote / updateQuickNote
- 输入：参数或 id + body
- 输出：分页列表项或详情 data
- 副作用：GET/POST/PUT
- 步骤：拼路径 → client 方法 → `.then(r => r.data)`
- 分支与异常：透传
- 调用：apiGet/Post/Put

### processQuickNote / archiveQuickNote / restoreQuickNote(id, status='inbox')
- 输入：id；restore 可带目标 status
- 输出：更新后详情
- 副作用：POST 状态动作
- 步骤：对应 path + body（restore 传 `{ status }`，其余 `{}`）
- 分支与异常：透传
- 调用：`apiPost`

### deleteQuickNote(id)
- 输入：id
- 输出：删除响应 data
- 副作用：DELETE
- 步骤：detail path
- 分支与异常：透传
- 调用：`apiDelete`

### uploadQuickNoteAttachment(file)
- 输入：File
- 输出：`QuickNoteAttachmentUpload`
- 副作用：multipart 上传
- 步骤：FormData 追加 `file` → `apiUpload(attachments)` → data
- 分支与异常：透传
- 调用：`apiUpload`

### downloadQuickNoteAttachmentBlob(id)
- 输入：附件 id
- 输出：Blob
- 副作用：下载
- 步骤：`apiDownloadBlob(attachmentDownload(id))`
- 分支与异常：透传
- 调用：`apiDownloadBlob`

## 近逐行中文伪代码

1. buildQuery 组装列表过滤参数
2. 路径表覆盖 list/detail/状态动作/附件
3. CRUD：list/get/create/update/delete 均解包 data
4. process/archive/restore 走 POST 动作
5. 附件：FormData 上传；blob 下载

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/quickNotes.ts",
      "label": "quickNotesApi",
      "path": "src/client-web/src/api/quickNotes.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/quickNotes.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/quickNotes.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/quickNotes.ts", "to": "/quick-notes", "type": "http" },
    { "from": "src/client-web/src/api/quickNotes.ts", "to": "/quick-notes/attachments", "type": "http" }
  ]
}
```
