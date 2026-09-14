# src/modules/Pim.Module.Stats/StatsModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Stats
- 职责：Stats 模块入口：注册 EF 程序集与 `StatsService`；映射需授权的 `/api/v1/stats` 批量上传端点；空初始化。
- 主要依赖：`IModule`、`PimDbContext`、`StatsService`、`UploadBatch`、`ApiResponse`、ASP.NET Core Minimal APIs
- 被谁使用：`Pim.Api` 模块注册加载

## 函数级结构化伪代码

### StatsModule
#### string Name / string Version
- 输入：无
- 输出：`"stats"` / `"1.0.0"`
- 副作用：无
- 步骤：属性返回固定模块标识与版本
- 分支与异常：无
- 调用：无

#### void RegisterServices(IServiceCollection services, IConfiguration configuration)
- 输入：DI 容器、配置
- 输出：无
- 副作用：注册模块程序集到 `PimDbContext`；Scoped 注册 `StatsService`
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(当前程序集)`
  2. `services.AddScoped<StatsService>()`
- 分支与异常：无
- 调用：`PimDbContext.RegisterModuleAssembly`、`AddScoped`

#### void MapEndpoints(IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无
- 副作用：映射 POST `/api/v1/stats/upload`（RequireAuthorization）
- 步骤：
  1. 建 group `/api/v1/stats` 并要求授权
  2. MapPost `/upload`：绑定 `UploadBatch` 与 `StatsService`
  3. 若 `batch.Entries` 为空 → `Ok(ApiResponse<int>.Ok(0))`
  4. 否则 `svc.IngestBatchAsync` → `Ok(ApiResponse<int>.Ok(count))`
- 分支与异常：空批次短路返回 0
- 调用：`StatsService.IngestBatchAsync`、`ApiResponse.Ok`

#### Task InitializeAsync(IServiceProvider serviceProvider)
- 输入：根/作用域服务提供器
- 输出：已完成 Task
- 副作用：无
- 步骤：`await Task.CompletedTask`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Reflection、ASP.NET Core Builder/Http/Mvc/Routing、Configuration、DI、`Pim.Core.Common`、`Pim.Core.Modules`、`PimDbContext`、Stats DTOs/Services
2. 命名空间 `Pim.Module.Stats`
3. 类 `StatsModule` 实现 `IModule`
4. `Name` = `"stats"`；`Version` = `"1.0.0"`
5. `RegisterServices`：注册本程序集到 DbContext 模型；Scoped 添加 `StatsService`
6. `MapEndpoints`：MapGroup `/api/v1/stats` + RequireAuthorization
7.  POST `/upload`：空 Entries 返回 Ok(0)；否则 IngestBatchAsync 返回入库条数
8. `InitializeAsync`：立即完成，无启动逻辑

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Stats/StatsModule.cs",
      "label": "StatsModule",
      "path": "src/modules/Pim.Module.Stats/StatsModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Stats/StatsModule.cs.md",
      "layer": "module.stats",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/Pim.Core/Common", "type": "depends_on" },
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/modules/Pim.Module.Stats/StatsModule.cs", "type": "depends_on" }
  ]
}
```
