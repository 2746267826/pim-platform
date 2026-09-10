# src/modules/Pim.Module.Files/FilesModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件模块入口：注册 DI 服务、映射 `/api/v1/files` 全套端点（提供商/条目/回收站/版本/索引搜索/建议/打开链接），启动时注册 AI Schema。
- 主要依赖：`IModule`、`PimDbContext`、`FileProviderBindingService`、`FileOperationService`、`FileIndexingService`、`FileAiService`、`HashingFileEmbeddingService`、`TikaFileTextExtractionService`、`NextcloudFileProviderAdapter`、`QdrantFileVectorStore`、`ApiResponse`、`DomainException`
- 被谁使用：宿主模块发现/加载；Web 客户端调用文件 API

## 函数级结构化伪代码

### FilesModule
#### string Name / string Version
- 输入：无
- 输出：`"files"` / `"1.0.0"`
- 副作用：无
- 步骤：返回固定元数据
- 分支与异常：无
- 调用：无

#### void RegisterServices(IServiceCollection services, IConfiguration configuration)
- 输入：DI 容器与配置
- 输出：无
- 副作用：注册程序集实体、Scoped/Singleton 服务与 HttpClient
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(当前程序集)`
  2. Scoped：`FileProviderBindingService`、`FileOperationService`、`FileIndexingService`、`FileAiService`
  3. Singleton：`IFileEmbeddingService` → `HashingFileEmbeddingService`
  4. Scoped：`IFileTextExtractionService` → `TikaFileTextExtractionService`
  5. HttpClient：`NextcloudFileProviderAdapter`、`QdrantFileVectorStore`
  6. Scoped 接口绑定：`IFileVectorStore`→Qdrant；`IFileProviderAdapter`→Nextcloud
- 分支与异常：无
- 调用：`services.AddScoped/AddSingleton/AddHttpClient`

#### void MapEndpoints(IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无
- 副作用：在 `FileEndpointPaths.Root` 组上映射全部文件 API，要求授权
- 步骤：
  1. `MapGroup(Root).RequireAuthorization()`
  2. 提供商：list、bind nextcloud、test、sync
  3. 条目：list/get/upload/download/move/rename/delete
  4. 回收站：list、restore
  5. 版本：list/download/restore-preview/restore
  6. 索引与搜索：index、search
  7. 建议：list/dismiss/accept
  8. 打开链接：open-link
- 分支与异常：无（处理在各 handler）
- 调用：各静态 handler 方法

#### Task InitializeAsync(IServiceProvider serviceProvider)
- 输入：根服务提供者
- 输出：已完成 Task
- 副作用：若存在 `IAiSchemaRegistry` 则注册文件 AI Schema
- 步骤：
  1. 尝试解析 `IAiSchemaRegistry`
  2. 非 null 时 `FileAiService.RegisterSchemas(registry)`
  3. 返回 `CompletedTask`
- 分支与异常：无 registry 则跳过
- 调用：`FileAiService.RegisterSchemas`

#### 各端点 Handler（节选逻辑）
- **UploadItemAsync**：要求 multipart；解析 `providerId`/`path`/`file`；校验失败抛 DomainException 5306–5309；流式上传
- **DownloadItemAsync / DownloadVersionAsync**：服务取流后 `Results.File`
- **DeleteItemAsync / RestoreVersionAsync / RestoreTrashAsync**：成功返回中文消息 ApiResponse
- **RestoreTrashAsync**：缺 `trashId` 抛 5310
- **SearchAsync**：查询参数 `q`/`mode` 转 `FileSearchQuery`
- **NotImplemented**：501 占位（当前主路径未使用）
- 副作用：委托对应 Service；部分写库/远端
- 调用：`FileOperationService` / `FileIndexingService` / `FileProviderBindingService`

### FileEndpointPaths
#### 路径常量与辅助
- 输入：id 字符串
- 输出：根路径与子路径字符串
- 副作用：无
- 步骤：
  1. `Root = /api/v1/files`
  2. 组合 Providers、Nextcloud、Item、Download、VersionRestore 等路径
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 Reflection、ASP.NET Core 最小 API、Core/Infra/Files 命名空间
2. `FilesModule` 实现 `IModule`：Name=files，Version=1.0.0
3. `RegisterServices`：注册程序集 + 绑定/操作/索引/AI 服务 + 哈希嵌入 + Tika 抽取 + Nextcloud/Qdrant HttpClient 与接口映射
4. `MapEndpoints`：组路径 Root，RequireAuthorization，绑定 providers/items/trash/versions/index/search/suggestions/open-link
5. `InitializeAsync`：可选注册 AI Schema
6. 各 private static handler：解析参数 → 调 service → `ApiResponse` 或 `Results.File`；上传/恢复校验 DomainException
7. `NotImplemented` 返回 501
8. 静态类 `FileEndpointPaths` 定义路径常量与工厂方法

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/FilesModule.cs",
      "label": "FilesModule",
      "path": "src/modules/Pim.Module.Files/FilesModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/FilesModule.cs.md",
      "layer": "module.files",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "/api/v1/files", "type": "http" }
  ]
}
```
