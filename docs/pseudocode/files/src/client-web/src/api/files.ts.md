# src/client-web/src/api/files.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Files 模块前端 API 路径常量与 HTTP 封装（提供商、列表、上传下载、回收站、版本、索引、搜索、建议、打开链接）。
- 主要依赖：
  - `./client`（apiGet/Post/Delete/Upload/DownloadBlob）
  - `../types` 中 File* 类型
- 被谁使用：文件管理 UI 页面与组件

## 函数级结构化伪代码

### fileApiPaths
#### 路径工厂对象
- 输入：各资源 id/path/query 参数
- 输出：REST 路径字符串
- 副作用：无
- 步骤：
  1. providers / nextcloud 绑定 / test / sync。
  2. items 列表（query path）、单 item CRUD 相关路径。
  3. upload、download、move、rename、trash、versions、index、search、suggestions、open-link。
- 分支与异常：无
- 调用：`URLSearchParams`

### 导出 API 函数（统一模式）
#### `getFileProviders` / `bindNextcloudProvider` / `testFileProvider` / `syncFileProvider`
- 输入：绑定请求或 provider id
- 输出：Promise 解包后的 `data`
- 副作用：HTTP
- 步骤：对应 apiGet/apiPost → `.then(r => r.data)`
- 调用：`apiGet` / `apiPost`

#### `getFileItems` / `getFileItem` / `uploadFile` / `downloadFileBlob`
- 输入：path 或 id；上传为 providerId+path+File
- 输出：列表/项/Blob
- 副作用：HTTP；upload 组 FormData
- 步骤：
  1. upload：FormData 附加 providerId、path、file → apiUpload。
  2. download：apiDownloadBlob。
- 调用：`apiGet` / `apiUpload` / `apiDownloadBlob`

#### `moveFile` / `renameFile` / `deleteFile`
- 输入：id + 请求体（move/rename）
- 输出：FileItem 或 string
- 副作用：HTTP
- 调用：`apiPost` / `apiDelete`

#### `getFileTrash` / `restoreFileTrash`
- 输入：restore 需 providerId+trashId
- 输出：回收站列表或结果字符串
- 调用：`apiGet` / `apiPost`

#### `getFileVersions` / `downloadFileVersionBlob` / `restoreFileVersionPreview` / `restoreFileVersion`
- 输入：file id、version id
- 输出：版本列表、Blob、预览、结果
- 调用：`apiGet` / `apiDownloadBlob` / `apiPost`

#### `indexFile` / `searchFiles` / `getFileSuggestions` / `dismissFileSuggestion` / `acceptFileSuggestion` / `getFileOpenLink`
- 输入：id、查询串与 mode、建议 id、打开模式
- 输出：对应 DTO
- 副作用：HTTP
- 调用：`apiGet` / `apiPost`

## 近逐行中文伪代码

1. 集中定义 `/files/*` 路径工厂。
2. 每个业务函数：拼路径 → client HTTP → 返回 `r.data`（Blob 直接返回）。
3. 上传用 multipart FormData；下载用 blob API。
4. 覆盖提供商绑定/测试/同步、目录浏览、改名移动删除、回收站、版本恢复、索引搜索与建议、打开链接。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/files.ts",
      "label": "files",
      "path": "src/client-web/src/api/files.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/files.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/files.ts", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/files.ts", "to": "src/client-web/src/types", "type": "depends_on" }
  ]
}
```
