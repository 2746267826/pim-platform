# src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：实现 `IModule`：注册速记模块 DI、映射 `/api/v1/quick-notes` 笔记与附件端点；提供路径常量。
- 主要依赖：`IServiceCollection`、`IConfiguration`、`IEndpointRouteBuilder`、`PimDbContext`、`QuickNoteService`、`QuickNoteAttachmentService`、`IQuickNoteObjectStorage`、`MinioQuickNoteObjectStorage`/`NullQuickNoteObjectStorage`、DTO、`ApiResponse`/`PagedResult`
- 被谁使用：`ModuleRegistry` / 主机启动加载模块；Web 与客户端调用对应 REST API

## 函数级结构化伪代码

### QuickNotesModule
#### string Name / string Version
- 输入：无
- 输出：模块名 `"quick-notes"`；版本 `"1.0.0"`
- 副作用：无
- 步骤：属性返回常量
- 分支与异常：无
- 调用：无

#### void RegisterServices(IServiceCollection services, IConfiguration configuration)
- 输入：服务集合、配置
- 输出：无
- 副作用：向 DI 注册程序集、对象存储实现、附件服务、笔记服务
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(当前程序集)`
  2. 若 `Minio:Endpoint` 非空：Scoped `IQuickNoteObjectStorage` → `MinioQuickNoteObjectStorage`
  3. 否则：Scoped → `NullQuickNoteObjectStorage`
  4. Scoped 注册 `QuickNoteAttachmentService`、`QuickNoteService`
- 分支与异常：Minio 配置有无决定存储实现
- 调用：`Assembly.GetExecutingAssembly`、`services.AddScoped`

#### void MapEndpoints(IEndpointRouteBuilder endpoints)
- 输入：端点路由构建器
- 输出：无
- 副作用：注册需授权的速记与附件 HTTP 路由
- 步骤：
  1. `MapGroup(QuickNoteEndpointPaths.Root).RequireAuthorization()`
  2. `GET ""`：`ListAsync`（status/search/page/pageSize，默认 page=1、pageSize=30）→ `ApiResponse.Ok` 分页列表
  3. `GET /{id}`：`GetAsync` → 详情
  4. `POST ""`：`CreateAsync` → `201 Created` + Location 路径
  5. `PUT /{id}`：`UpdateAsync` → 详情
  6. `POST /{id}/process|archive`：`ProcessAsync` / `ArchiveAsync`
  7. `POST /{id}/restore`：`RestoreAsync(request)`
  8. `DELETE /{id}`：`DeleteAsync` → `"已删除"`
  9. `POST /attachments`：校验 multipart；读 form；取 `file`；`UploadAsync` 流
  10. `GET /attachments/{id}/download`：`DownloadAsync` → `Results.File`
  11. `DELETE /attachments/{id}`：`DeleteAsync` → `"已删除"`
- 分支与异常：
  - 附件上传非 form → 400「需要 multipart/form-data」
  - `ReadFormAsync` 取消 → 重抛 `OperationCanceledException`
  - 无效 multipart（InvalidData/BadHttpRequest/IO）→ 400
  - 缺 `file` 字段 → 400
- 调用：`QuickNoteService.*`、`QuickNoteAttachmentService.*`、`Results.*`、`ApiResponse.*`

#### Task InitializeAsync(IServiceProvider serviceProvider)
- 输入：服务提供器
- 输出：已完成任务
- 副作用：无
- 步骤：`await Task.CompletedTask`
- 分支与异常：无
- 调用：无

### QuickNoteEndpointPaths
#### const Root / Attachments；Note(id)；AttachmentDownload(id)
- 输入：id 字符串（后两者）
- 输出：API 路径字符串
- 副作用：无
- 步骤：常量拼接 `/api/v1/quick-notes` 及附件子路径
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入反射、ASP.NET Core 构建/HTTP/MVC/路由、配置、DI、`Pim.Core`、`PimDbContext`、本模块 DTO 与 Services
2. 命名空间 `Pim.Module.QuickNotes`
3. 类 `QuickNotesModule` 实现 `IModule`：Name=`quick-notes`，Version=`1.0.0`
4. `RegisterServices`：注册模块程序集到 DbContext
5. 有 Minio Endpoint 则 Minio 存储，否则 Null 存储；注册附件与笔记服务
6. `MapEndpoints`：建授权 group 于 Root
7. 列表 GET：组装 `QuickNoteListQuery` 调 `ListAsync`，Ok 分页
8. 详情 GET：`GetAsync(id)` Ok
9. 创建 POST：`CreateAsync`，Created 到 Note 路径
10. 更新 PUT：`UpdateAsync` Ok
11. process/archive POST：对应服务方法 Ok
12. restore POST：带 body 调 `RestoreAsync`
13. DELETE 笔记：删后返回「已删除」
14. 附件 POST：必须 form；读 form 捕获取消/无效数据；取 file；上传流
15. 附件下载 GET：返回 File 流
16. 附件 DELETE：删后「已删除」
17. `InitializeAsync`：空完成
18. 静态类 `QuickNoteEndpointPaths`：Root、Attachments 常量；Note/AttachmentDownload 路径工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs",
      "label": "QuickNotesModule",
      "path": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs.md",
      "layer": "module.quicknotes",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/modules/Pim.Module.QuickNotes/DTOs/QuickNoteDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "type": "depends_on" }
  ]
}
```
