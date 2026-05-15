# PIM 模块开发规范

本文档定义新模块的开发规范和接口契约。任何新增模块必须遵守本规范。

---

## 一、模块最小结构

### 1.1 服务端模块

```
src/modules/Pim.Module.{Name}/
├── Pim.Module.{Name}.csproj
├── {Name}Module.cs              # 实现 IModule
├── Controllers/
│   └── {Name}Controller.cs      # API 端点
├── Services/
│   └── {Name}Service.cs         # 业务逻辑
├── Entities/
│   └── *.cs                     # EF Core 实体
├── DTOs/
│   └── *.cs                     # 请求/响应 DTO
└── Migrations/                  # EF Core 迁移（由 Infrastructure 统一管理或模块自行管理）
```

### 1.2 最小 `.csproj`

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="..\..\Pim.Core\Pim.Core.csproj" />
  </ItemGroup>
</Project>
```

---

## 二、核心接口

### 2.1 `IModule` — 模块入口

```csharp
namespace Pim.Core.Modules;

public interface IModule
{
    /// <summary>模块唯一标识，用于路由前缀 /api/v1/{name}/</summary>
    string Name { get; }

    /// <summary>语义化版本号</summary>
    string Version { get; }

    /// <summary>注册模块的服务到 DI 容器</summary>
    void RegisterServices(IServiceCollection services, IConfiguration configuration);

    /// <summary>注册模块的 API 端点</summary>
    void MapEndpoints(IEndpointRouteBuilder endpoints);

    /// <summary>模块初始化（建表、种子数据、订阅事件等）</summary>
    Task InitializeAsync(IServiceProvider serviceProvider);
}
```

### 2.2 `ISearchProvider` — 搜索集成（可选）

```csharp
namespace Pim.Core.Modules;

public interface ISearchProvider
{
    /// <summary>模块标识，与 IModule.Name 一致</summary>
    string ModuleName { get; }

    /// <summary>搜索本模块数据</summary>
    /// <param name="query">搜索关键词</param>
    /// <param name="limit">最大返回条数</param>
    /// <param name="ct">取消令牌</param>
    Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct);
}

public record SearchResult(
    string ModuleName,
    string Type,         // 结果类型标识，如 "event"、"task"、"file"
    string Id,
    string Title,
    string Snippet,      // 匹配内容摘要，最多 200 字符
    string Url            // 客户端跳转路径，如 "/calendar/event/123"
);
```

如果模块需要被全局搜索索引，实现 `ISearchProvider` 并在 `RegisterServices` 中注册：
```csharp
services.AddSingleton<ISearchProvider, CalendarSearchProvider>();
```

`SearchController` 会通过 DI 自动发现所有 `ISearchProvider` 并聚合结果。

---

## 三、模块实现模板

```csharp
using Pim.Core.Modules;

namespace Pim.Module.Example;

public class ExampleModule : IModule
{
    public string Name => "example";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        // 注册业务服务
        services.AddScoped<ExampleService>();

        // 可选: 注册搜索提供者
        services.AddSingleton<ISearchProvider, ExampleSearchProvider>();
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/example")
            .RequireAuthorization();               // 需要认证

        group.MapGet("/items", async (
            [FromQuery] int page,
            [FromQuery] int pageSize,
            [FromServices] ExampleService service,
            CancellationToken ct) =>
        {
            var result = await service.GetItemsAsync(page, pageSize, ct);
            return Results.Ok(ApiResponse.Ok(result));
        });

        group.MapGet("/items/{id:guid}", async (
            Guid id,
            [FromServices] ExampleService service,
            CancellationToken ct) =>
        {
            var item = await service.GetItemAsync(id, ct);
            return item is null
                ? Results.NotFound(ApiResponse.Error(40401, "Item not found"))
                : Results.Ok(ApiResponse.Ok(item));
        });

        group.MapPost("/items", async (
            [FromBody] CreateExampleRequest request,
            [FromServices] ExampleService service,
            CancellationToken ct) =>
        {
            var item = await service.CreateItemAsync(request, ct);
            return Results.Created($"/api/v1/example/items/{item.Id}", ApiResponse.Ok(item));
        });

        group.MapPut("/items/{id:guid}", async (...) => { ... });
        group.MapDelete("/items/{id:guid}", async (...) => { ... });
    }

    public async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        // 自动迁移（开发环境）或验证表结构（生产环境）
        using var scope = serviceProvider.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<PimDbContext>();
        await db.Database.EnsureCreatedAsync();
    }
}
```

---

## 四、强制约定

### 4.1 API 路由

```
/api/v1/{module}/{resource}[/{id}][/{action}]
```

- `{module}`: 小写、复数形式（如 `calendar`、`files`、`activity`）
- `{resource}`: 小写、复数形式（如 `events`、`items`、`sessions`）
- 不使用动词作为资源名，动作通过 HTTP 方法表达
- 特殊操作使用 `POST /api/v1/{module}/{resource}/{action}` 格式

### 4.2 响应格式

所有 API 响应使用 `ApiResponse` 包装：

```csharp
// 成功
public static ApiResponse<T> Ok(T data)
    => new(0, "success", data, DateTimeOffset.UtcNow);

// 失败
public static ApiResponse Error(int code, string message)
    => new(code, message, null, DateTimeOffset.UtcNow);
```

分页使用 `PagedResult<T>`：

```csharp
public record PagedResult<T>(
    IReadOnlyList<T> Items,
    int Page,
    int PageSize,
    int TotalCount,
    int TotalPages
);
```

### 4.3 错误码

```
格式: {模块码}{错误序号}
模块码: 01=核心, 02=日历, 03=文件, 04=活动

示例:
  01001 核心-未知错误
  01002 核心-认证失败
  02001 日历-事件不存在
  02002 日历-排程无可行解
  03001 文件-文件不存在
  03002 文件-上传失败
  04001 活动-会话不存在
```

### 4.4 实体规范

- 主键统一使用 `Guid`（`UUID`），由应用层生成（`Guid.NewGuid()`）
- `created_at` / `updated_at` / `deleted_at` 使用 `DateTimeOffset` (TIMESTAMPTZ)
- 软删除使用 `deleted_at` 字段，查询时全局过滤 `ISoftDeletable` 接口
- 全文搜索字段使用 `NpgsqlTsVector` 类型，在实体中声明为 `SearchVector`

```csharp
public class EventEntity : ISoftDeletable
{
    public Guid Id { get; set; }
    // ...
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public DateTimeOffset? DeletedAt { get; set; }
}
```

### 4.5 DTO 与共享契约

服务端 DTO 放在 `modules/Pim.Module.{Name}/DTOs/` 目录。

客户端共享的 DTO 和 API 路径常量放入 `shared/Pim.Shared.Contracts/`：

```csharp
// Pim.Shared.Contracts/Calendar/CalendarEndpoints.cs
public static class CalendarEndpoints
{
    public const string Base = "/api/v1/calendar";
    public const string Events = Base + "/events";
    public const string Tasks = Base + "/tasks";
    public const string Schedule = Base + "/schedule";
}
```

两端客户端各自引用或复制 DTO 定义，字段和类型必须保持一致。

### 4.6 认证与授权

- 所有业务端点默认 `.RequireAuthorization()`
- 用户数据隔离在 DbContext 层面自动注入 `user_id` 过滤
- 模块内需要 `currentUserId` 时从 `IHttpContextAccessor` 获取

### 4.7 依赖注入注册

每个模块的 `RegisterServices` 方法只注册本模块的依赖。基础设施无关的通用服务（DbContext, JWT, MinIO Client）由 `Pim.Infrastructure` 的扩展方法注册。

---

## 五、客户端模块规范

### 5.1 Windows 客户端 (WPF)

```
Pim.Client.Modules/Pim.Client.{Name}/
├── Pim.Client.{Name}.csproj
├── Services/
│   └── {Name}ApiService.cs      # 封装此模块的 HTTP 请求
├── ViewModels/
│   └── {Name}ViewModel.cs
└── Views/
    └── {Name}View.xaml
```

注册模式：

```csharp
// Pim.Client.App/Startup.cs
services.AddSingleton<CalendarApiService>();
services.AddTransient<CalendarViewModel>();
// 导航注册
navigationService.Register("calendar", typeof(CalendarView));
```

### 5.2 Android 客户端

```
features/{name}/
├── ui/
│   └── {Name}Screen.kt
├── viewmodel/
│   └── {Name}ViewModel.kt
└── data/
    ├── {Name}ApiService.kt
    └── {Name}Repository.kt
```

在 Gradle 中以 dynamic feature module 方式注册（如果需要按需下载），或以 library module 方式直接依赖。

---

## 六、数据库迁移

模块的实体变更通过 EF Core Migration 管理：

- **开发期**: 每个模块可使用独立 `DbContext` 或通过主 `PimDbContext` 统一管理
- **生产期**: 迁移脚本由 CI/CD 自动执行，模块的 `InitializeAsync` 中只做 `EnsureCreated`（用于首次部署）

分表策略（如活动记录的事件表）需要手动创建而非通过 EF Migration。模块需在 `InitializeAsync` 中检查并创建当月表：

```csharp
public async Task InitializeAsync(IServiceProvider sp)
{
    var db = sp.GetRequiredService<PimDbContext>();
    var tableName = $"keyboard_events_{DateTimeOffset.UtcNow:yyyyMM}";
    var sql = $@"
        CREATE TABLE IF NOT EXISTS {tableName} (
            LIKE keyboard_events_template INCLUDING ALL
        )";
    await db.Database.ExecuteSqlRawAsync(sql);
}
```

---

## 七、测试要求

每个模块至少覆盖：

- **单元测试** (xUnit): 业务服务逻辑，Mock 依赖
- **集成测试**: 至少一个端到端 API 测试（使用 WebApplicationFactory）
- **客户端**: 至少一个 ViewModel 单元测试

```
tests/
├── Pim.Module.{Name}.Tests/
│   ├── Services/
│   │   └── {Name}ServiceTests.cs
│   └── Integration/
│       └── {Name}ApiTests.cs
```

---

## 八、添加新模块检查清单

1. [ ] 创建服务端项目 `dotnet new classlib -n Pim.Module.{Name}`，放到 `src/modules/` 下
2. [ ] 添加 `Pim.Core` 项目引用
3. [ ] 实现 `IModule` 接口（`Name`、`Version`、`RegisterServices`、`MapEndpoints`、`InitializeAsync`）
4. [ ] 定义 Entities，放置在 `Entities/` 目录
5. [ ] 定义 DTOs（Request/Response），放置在 `DTOs/` 目录
6. [ ] 实现 Service 层业务逻辑
7. [ ] 实现 Controller / Minimal API 端点
8. [ ] 如需被搜索：实现 `ISearchProvider`，在 `RegisterServices` 中注册
9. [ ] 在 `Pim.Api.csproj` 中添加 `<ProjectReference>`
10. [ ] 在 `Pim.Shared.Contracts` 中添加 API 路径常量和客户端 DTO
11. [ ] 编写单元测试和集成测试
12. [ ] 创建 WPF 客户端模块项目，实现 View/ViewModel/Service
13. [ ] 创建 Android 客户端 feature 模块，实现 Screen/ViewModel/Data
14. [ ] 更新整体部署文档（如有新依赖容器）
15. [ ] 更新全局搜索类型筛选参数（`/api/v1/search?type=` 文档）
