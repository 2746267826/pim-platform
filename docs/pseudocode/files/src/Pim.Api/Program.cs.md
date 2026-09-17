# src/Pim.Api/Program.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：ASP.NET Core 宿主入口：配置 Serilog、基础设施与鉴权、模块发现、Today 区块提供器、中间件管道、DB 迁移采纳、各 API 端点映射、Hangfire 周期任务与 SPA 回退。
- 主要依赖：
  - Hangfire、EF Core、Serilog
  - `Pim.Infrastructure.Extensions`（`AddPimInfrastructure`/`AddPimAuth`）
  - `ModuleRegistry`、`TodaySectionService` 及各 `ITodaySectionProvider`
  - 中间件 `CorrelationIdMiddleware`/`ExceptionMiddleware`、`HangfireAuthorizationFilter`
  - 端点映射：Auth/Search/Status/Daemon/Endpoint/Operations/Today/Ai/Version
  - `PimMigrationAdoptionService`、`PimDbContext`、`Stage0DiagnosticJob`
- 被谁使用：进程入口（`dotnet run` / 容器启动）

## 函数级结构化伪代码

### 顶层宿主脚本（top-level statements）
#### 启动主流程
- 输入：`args`/`configuration`（隐式）
- 输出：长期运行的 Web 主机
- 副作用：日志、DI、中间件、路由、迁移、Hangfire 任务、监听 HTTP
- 步骤：
  1. 配置全局 `Log.Logger`：Debug、LogContext、Service=pim-api、Console+文件 Compact JSON（`/data/pim/logs/pim-api-.jsonl`，日滚动保留 30）。
  2. `WebApplication.CreateBuilder`；`UseSerilog`。
  3. DI：`AddPimInfrastructure`、`AddPimAuth`；CORS 全开。
  4. `ModuleRegistry.DiscoverModules`；失败仅 Warning，继续。
  5. 注册 `TodaySectionService` 与十余个 `ITodaySectionProvider`（日历/运维/Outlook/提醒/报告/端点/PC 活动与质量/分类建议等）。
  6. `builder.Build()` → 管道：CorrelationId → Serilog 请求日志 → Exception → Cors → AuthN → AuthZ → Hangfire Dashboard `/hangfire` → DefaultFiles/StaticFiles。
  7. 作用域内：`PimMigrationAdoptionService.AdoptExistingSchemaAsync` 后 `db.Database.MigrateAsync`；失败 Warning 仍启动。
  8. 匿名 `GET /health`；映射 Version/Auth/Search/Status/Daemon/Endpoint/Operations/Today/Ai。
  9. `moduleRegistry.MapAllEndpoints`；`InitializeAllAsync`。
  10. Hangfire 注册小时级 `Stage0DiagnosticJob`；失败 Warning。
  11. 匿名 SPA `MapFallbackToFile("index.html")`；`app.Run()`。
- 分支与异常：模块发现、迁移、Hangfire 注册均 catch 后 Warning 降级
- 调用：基础设施扩展、ModuleRegistry、各 Map*Endpoints、EF Migrate、RecurringJob

## 近逐行中文伪代码

1. 引入 Hangfire、EF、Api 命名空间、Infrastructure 扩展、Operations、Serilog。
2. 建 Serilog：Debug + Compact JSON 控制台与滚动文件。
3. CreateBuilder + UseSerilog。
4. AddPimInfrastructure / AddPimAuth；CORS AllowAny*。
5. ModuleRegistry 发现模块；异常 Warning。
6. 注册 TodaySectionService 与全部 ITodaySectionProvider 实现。
7. Build 后装 CorrelationId、请求日志模板、ExceptionMiddleware、Cors、Authentication、Authorization、Hangfire 面板。
8. UseDefaultFiles + UseStaticFiles（wwwroot SPA）。
9. try：AdoptExistingSchema → Migrate；catch Warning。
10. MapGet /health 匿名；MapVersion/Auth/Search/Status/Daemon/Endpoint/Operations/Today/Ai。
11. MapAllEndpoints + InitializeAllAsync。
12. RecurringJob 小时诊断；catch Warning。
13. MapFallbackToFile index.html 匿名；Run。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Program.cs",
      "label": "Program",
      "path": "src/Pim.Api/Program.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Program.cs.md",
      "layer": "api",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/ModuleRegistry.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Middleware/CorrelationIdMiddleware.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/DaemonEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/AiEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Today/TodaySectionService.cs", "type": "depends_on" }
  ]
}
```
