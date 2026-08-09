# src/modules/Pim.Module.Files/DTOs/FileDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：Files 模块 API 请求/响应 DTO 与查询模型（提供方、条目、版本、AI 结果、搜索、索引任务等）。
- 主要依赖：`Pim.Core.Common.PagedResult`
- 被谁使用：`FilesModule` 端点与 Files 服务层映射；Web 客户端类型镜像

## 函数级结构化伪代码

### FileDtos（文件内 sealed record 集合）
#### 属性/形状一览（无行为方法）
- 输入：构造参数即字段
- 输出：不可变 DTO 实例
- 副作用：无
- 步骤：
  1. `FileProviderDto`：提供方 Id、类型、BaseUrl/InternalBaseUrl、用户名、状态、同步时间/错误、时间戳
  2. `BindNextcloudProviderRequest`：绑定 Nextcloud 的 URL/用户名/AppPassword
  3. `FileProviderTestDto`：连通性测试结果
  4. `FileItemDto`：文件/目录元数据、版本指针、索引状态、可选 `FileAiResultDto`
  5. `FileVersionDto`：版本 Id、etag/size、来源、是否当前
  6. `FileAiResultDto`：摘要/标签/语言/敏感度/模型/证据 chunk
  7. `FileSuggestionDto`：建议类型、置信度、payload JSON、状态
  8. `FileListQuery`/`FileSearchQuery`：列表路径与搜索词/模式
  9. `FileSearchResultDto`/`FileChunkSearchHitDto`：条目 + chunk 命中
  10. `MoveFileRequest`/`RenameFileRequest`/`FileSuggestionStatusRequest`：写操作请求
  11. `FileOpenLinkDto`：打开链接 URL 与模式
  12. `VersionRestorePreviewDto`：恢复预览与是否需确认
  13. `FileIndexJobDto`：索引任务状态/阶段/重试/错误
  14. `FileListResponse`：包装 `PagedResult<FileItemDto>`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `PagedResult`；命名空间 `Pim.Module.Files.DTOs`
2. 定义提供方绑定/测试 DTO
3. 定义文件项、版本、AI 结果、建议 DTO
4. 定义列表/搜索查询与搜索结果、chunk 命中
5. 定义移动/重命名/打开链接/版本恢复预览/索引任务/建议状态请求
6. `FileListResponse` 承载分页结果

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/DTOs/FileDtos.cs",
      "label": "FileDtos",
      "path": "src/modules/Pim.Module.Files/DTOs/FileDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/DTOs/FileDtos.cs.md",
      "layer": "module.files",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/DTOs/FileDtos.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/DTOs/FileDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files", "to": "src/modules/Pim.Module.Files/DTOs/FileDtos.cs", "type": "depends_on" }
  ]
}
```
