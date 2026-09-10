# src/modules/Pim.Module.Mobile/MobileModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：Mobile 模块入口：注册 DI 服务与 `/api/v1/mobile` 下全部 HTTP 端点；附带路径常量与分析/位置查询参数 record。
- 主要依赖：`IModule`、`PimDbContext.RegisterModuleAssembly`、Mobile Services/DTOs、`ApiResponse`、ASP.NET Minimal APIs
- 被谁使用：主机模块加载器扫描 `IModule` 实现

## 函数级结构化伪代码

### MobileModule
#### string Name / string Version
- 输入：无
- 输出：`"mobile"` / `"1.0.0"`
- 副作用：无
- 步骤：模块元数据
- 分支与异常：无
- 调用：无

#### void RegisterServices(IServiceCollection, IConfiguration)
- 输入：services、configuration（未读配置）
- 输出：无
- 副作用：注册程序集实体 + Scoped/Singleton 服务
- 步骤：
  1. `PimDbContext.RegisterModuleAssembly(ExecutingAssembly)`
  2. `TryAddSingleton(TimeProvider.System)`
  3. Scoped：Device/Gap/UsageIngest/SessionInterpreter/Location/LocationQuery/LocationAggregation/UsageQuery/Quality/AnalyticsQuery/AppClassification/AppCatalogOverride/UsageGoal/UsageAggregation/TimelineBlock
- 分支与异常：无
- 调用：DI 扩展

#### void MapEndpoints(IEndpointRouteBuilder)
- 输入：endpoints
- 输出：无
- 副作用：映射需授权的一组路由
- 步骤：
  1. `MapGroup(MobileEndpointPaths.Root).RequireAuthorization()`
  2. 设备：GET `/devices`、POST `/devices/register`
  3. 同步/上报：POST `/sync/gaps`、`/usage/events`、`/location/points`
  4. 汇总：GET `/summary`、`/timeline`（`BuildSummaryQuery`）
  5. 位置历史：GET `/location/history`（start/end 或 range*；默认 maxAccuracy 50）
  6. 位置分析：overview/tracks/segments/{id}/points
  7. 质量：GET `/quality`
  8. 用量分析：overview/heatmap/charts/timeline-blocks 及 block sessions、session events
  9. 目标：GET/POST `/analytics/goals`、DELETE `/{goalId}`
  10. 应用目录覆盖与分类规则 CRUD
  11. 统一 `ApiResponse.Ok` 包装；segment 未找到 404
- 分支与异常：segment 404；其余由服务抛出
- 调用：各 Mobile*Service

#### Task InitializeAsync(IServiceProvider)
- 输入：sp
- 输出：CompletedTask
- 副作用：无
- 步骤：空初始化
- 分支与异常：无
- 调用：无

#### static MobileSummaryQuery BuildSummaryQuery(deviceId, date, rangeStartUtc, rangeEndUtc)
- 输入：可选设备、日期串、UTC 范围
- 输出：`MobileSummaryQuery`
- 副作用：无
- 步骤：
  1. date 空白 → 直接用 range 参数
  2. `DateOnly.TryParse` 失败 → 回退 range
  3. 成功 → 当日 00:00 UTC 到 +1 天
- 分支与异常：解析失败静默回退
- 调用：CultureInfo.InvariantCulture

### MobileEndpointPaths
#### 常量
- 输入：无
- 输出：Root=`/api/v1/mobile` 及 devices/register/gaps/usage/location/summary/timeline/quality 路径
- 副作用：无
- 步骤：字符串常量组合
- 分支与异常：无
- 调用：无

### MobileAnalyticsEndpointQuery / MobileLocationEndpointQuery
#### ToRequest()
- 输入：query 字段（范围、时区、设备、过滤、分页等）
- 输出：`MobileAnalyticsQueryRequest` / `MobileLocationQueryRequest`
- 副作用：无
- 步骤：按位置透传构造 request record
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Reflection、Globalization、ASP.NET、DI、Core.Common/Modules、Infrastructure.Data、Mobile DTOs/Services
2. `MobileModule : IModule`：Name=mobile，Version=1.0.0
3. `RegisterServices`：注册模块程序集 + TimeProvider + 15 个 Scoped 服务
4. `MapEndpoints`：`/api/v1/mobile` 需登录；设备/间隙/用量/位置/汇总/时间线/质量/分析/目标/应用覆盖与规则
5. `InitializeAsync` 空完成
6. `BuildSummaryQuery`：date 优先解析为 UTC 日区间，否则用 range*
7. `MobileEndpointPaths` 公开路径常量
8. 两个 EndpointQuery record 映射到服务层 Request

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/MobileModule.cs",
      "label": "MobileModule",
      "path": "src/modules/Pim.Module.Mobile/MobileModule.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/MobileModule.cs.md",
      "layer": "module.mobile",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "/api/v1/mobile", "type": "http" }
  ]
}
```
