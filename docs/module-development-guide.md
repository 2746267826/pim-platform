# PIM 并行模块开发指南

## 文档目的

这份文档是 PIM 新模块并行开发的通用契约。

它面向所有未来模块，不只面向健康数据或定位记录。适用范围包括健康、定位、复盘、文件资料、外部系统集成、Android 数据源、AI 建议，以及任何后续会长期留在 PIM 里的能力。

这份指南追求的是可落地的模块化：

- 新模块可以由不同开发者或不同 agent 并行推进。
- 早期输入和输出可以不完全统一，因为领域模型还在探索。
- 每个模块必须留下稳定边界，方便后期融合。
- 服务端始终是事实来源和业务规则所有者。
- Web 始终是主要交互端。
- Windows 和 Android daemon 是传感器与轻量执行器。
- 未来 MCP 工具应该包装现有服务端 API，而不是重写模块业务逻辑。

## 当前仓库结构

新模块优先贴合当前仓库，不另起一套架构。

- 后端模块放在 `src/modules/Pim.Module.{Name}`。
- 后端共享契约放在 `src/Pim.Core`。
- 基础设施、持久化、认证、审计、存储、健康状态、后台任务放在 `src/Pim.Infrastructure`。
- API 宿主放在 `src/Pim.Api`。
- Web 前端放在 `src/client-web`。
- Windows daemon/client 放在 `src/client-windows`。
- Android 代码放在 `src/client-android`。
- 后端单元测试当前放在 `tests/Pim.UnitTests`。
- Web 类型和 API 路径测试当前放在 `tests/client-web`。

除非现有结构真的阻碍模块开发，否则不要为单个模块创造第二套工程结构。

## 核心原则

### 服务端拥有业务状态

长期存在的业务规则放在服务端，包括：

- 数据模型
- 状态流转
- 校验
- 权限
- 审计
- 后台任务
- 派生数据
- 冲突检查
- 导入和上传归一化
- 面向未来 MCP 的结构化操作结果

Web 负责展示、编辑、筛选和确认。Web 不应该复制分类、健康解释、匹配、排程、数据质量判断等业务逻辑。

daemon 负责采集、缓存、上传、上报状态和执行简单服务端命令。daemon 不负责长期业务含义判断。

### 原始数据优先

带外部来源的模块先保存原始事实，再做派生解释。

示例：

- 健康模块：先保存心率、睡眠、步数、运动、来源设备元数据。
- 定位模块：先保存坐标、精度、来源 provider、运动提示、原始 payload。
- 文件模块：先保存 provider 元数据和版本 id，再做摘要和向量索引。
- 复盘模块：先保存证据和统计口径，再生成 AI 总结。

派生数据可以存在，但必须可解释，并且尽量能从原始数据重算。

### 稳定边界优先于过早统一

并行模块不要求第一天就和所有模块 DTO 完全统一，但必须具备：

- 稳定 API 前缀
- 稳定 ID
- 明确所属模块
- 必要的来源和设备字段
- 必要的上传幂等策略
- 原始数据与派生数据分层说明
- 清楚的后期融合点
- 覆盖公共边界的测试

如果模块暂时不接入 Today、搜索、状态页或 MCP，也要在模块说明中明确“不接入的原因”和“后续最小接入点”。

## 后端模块结构

默认使用下面的结构：

```text
src/modules/Pim.Module.{Name}/
|-- Pim.Module.{Name}.csproj
|-- {Name}Module.cs
|-- DTOs/
|   `-- {Name}Dtos.cs
|-- Entities/
|   `-- {Name}Entity.cs
|-- Services/
|   `-- {Name}Service.cs
|-- Search/
|   `-- {Name}SearchProvider.cs
|-- Jobs/
|   `-- {Name}RefreshJob.cs
|-- ExternalSources/
|   `-- {SourceName}Normalizer.cs
`-- README.md
```

只创建当前模块真正需要的目录。服务类要保持聚焦；如果一个 service 同时承担 API 编排、上传、查询和派生计算，通常应该拆分。

最小 `.csproj`：

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="..\..\Pim.Core\Pim.Core.csproj" />
    <ProjectReference Include="..\..\Pim.Infrastructure\Pim.Infrastructure.csproj" />
  </ItemGroup>
</Project>
```

模块使用 `PimDbContext`、当前用户、审计、存储或后台任务时引用 `Pim.Infrastructure`。纯领域 helper 如果只需要 `Pim.Core`，就保持轻依赖。

## 模块入口

每个后端模块都实现 `Pim.Core.Modules.IModule`。

```csharp
public sealed class ExampleModule : IModule
{
    public string Name => "example";
    public string Version => "1.0.0";

    public void RegisterServices(IServiceCollection services, IConfiguration configuration)
    {
        PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
        services.AddScoped<ExampleService>();
    }

    public void MapEndpoints(IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/example")
            .RequireAuthorization();

        group.MapGet("/items", async (
            [FromServices] ExampleService service,
            CancellationToken ct) =>
            Results.Ok(ApiResponse<IReadOnlyList<ExampleItemDto>>.Ok(
                await service.ListAsync(ct))));
    }

    public Task InitializeAsync(IServiceProvider serviceProvider)
        => Task.CompletedTask;
}
```

模块入口要求：

- `Name` 稳定、小写、适合做 API 路径。
- `Version` 在公共行为有明显变化时更新。
- `RegisterServices` 只注册本模块依赖和 provider。
- 有 EF 配置时，在 `RegisterServices` 调用 `PimDbContext.RegisterModuleAssembly(...)`。
- `MapEndpoints` 在 `/api/v1/{module}` 下暴露公共 API。
- `InitializeAsync` 只做轻量启动检查或特殊 schema helper，不替代常规 EF migration。

要启用模块，需要在 `src/Pim.Api/Pim.Api.csproj` 添加模块项目引用。API 宿主启动时会发现编译输出里的 `Pim.Module.*.dll`。

## API 契约

### 路径结构

使用：

```text
/api/v1/{module}/{resources}
/api/v1/{module}/{resources}/{id}
/api/v1/{module}/{resources}/{id}/{action}
```

示例：

- `/api/v1/health/samples`
- `/api/v1/health/sources`
- `/api/v1/location/points`
- `/api/v1/location/trips`
- `/api/v1/files/items`
- `/api/v1/review/daily`

资源名优先使用名词。只有状态流转或不适合 CRUD 的操作才使用动作后缀，例如：

- `preview`
- `restore`
- `archive`
- `sync`
- `upload`
- `recompute`
- `accept`
- `reject`

### 响应结构

所有 JSON API 响应使用 `Pim.Core.Common.ApiResponse<T>`。

分页使用 `PagedResult<T>`。

写操作响应不要只返回 `"ok"`，应该返回有用事实：

- 变更对象 id
- 变更对象类型
- 变更数量
- 跳过数量
- 冲突列表
- 警告
- 是否需要后续确认
- 有用的下一步建议

这样未来 MCP 工具才能直接包装 API，而不是重新猜测业务结果。

### 读、写、上传、操作接口

多数模块应该区分四类 API：

- 读接口：给 Web 和未来 MCP 查询归一化数据。
- 写接口：用户主动 CRUD 或状态流转。
- 上传接口：daemon 或外部来源写入原始事实。
- 操作接口：dry-run、preview、recompute、batch update、import、export。

示例：

```text
GET  /api/v1/location/points?from=&to=&source=&page=&pageSize=
POST /api/v1/location/points/upload
GET  /api/v1/location/quality?from=&to=
POST /api/v1/location/trips/recompute-preview
POST /api/v1/location/trips/recompute
```

### 认证

业务接口默认 `.RequireAuthorization()`。

匿名接口应该很少，并且要有明确理由，例如 API 宿主已有的 `/health`。不要让 daemon 上传匿名，除非已经设计了明确的设备 token 或绑定机制。

### 错误处理

用户可预期错误使用领域异常或显式 endpoint result。

预期冲突不是普通 500。接口应该返回结构化数据，让 Web 能展示清晰选择：

- 来源数据重复
- 恢复冲突
- 时间范围非法
- 导入格式不支持
- 外部来源不可用
- 权限不足

## 数据模型规则

### 实体基础

默认约定：

- 主键使用 `Guid`，由应用代码生成。
- 时间点使用 `DateTimeOffset`。
- 只有真正不带时间的日历日期才使用 `DateOnly`。
- 使用 `CreatedAt`、`UpdatedAt` 和可选 `DeletedAt`。
- 用户可见且应可恢复的数据实现 `ISoftDeletable`。
- EF 配置放在 `Entities/` 下的显式 configuration 类中。

### 用户、设备、来源和时间

存储个人数据的模块应该建模所有权和来源。

常见字段：

- `UserId`：数据所属用户。
- `DeviceId`：daemon 来源数据的稳定设备 id。
- `Source`：粗粒度来源，例如 `android-daemon`、`windows-daemon`、`manual`、`import`、`provider`。
- `SourceProvider`：外部 provider，例如 `google-fit`、`apple-health-export`、`gps`、`network`、`activitywatch`。
- `SourceRecordId`：provider 或 daemon 提供的记录 id。
- `ObservedAt`：事实发生时间。
- `ReceivedAt`：API 接收时间。
- `RawJson`：暂未归一化但后续可能有用的原始字段。

上传接口必须定义幂等键。可以是自然唯一键，例如：

```text
UserId + DeviceId + Source + SourceRecordId
```

也可以是：

```text
UserId + DeviceId + Source + ObservedAt + MetricType
```

不要假设客户端重试永远完美。上传接口必须能容忍重复 batch。

### 原始表和派生表

当派生结果可能变化时，原始事实和派生解释分表保存。

推荐模式：

- `LocationPointEntity`：原始定位点。
- `LocationStayEntity`：派生停留点或地点。
- `LocationTripEntity`：派生行程。
- `HealthSampleEntity`：原始健康测量。
- `HealthDailySummaryEntity`：派生日汇总。

派生数据要保留足够来源信息，便于解释或重算：

- 来源原始记录 id 或时间范围
- 算法版本
- 生成时间
- 置信度或质量标记

### EF Core 和迁移

模块实体配置通过 `PimDbContext.RegisterModuleAssembly(...)` 接入主 DbContext。

迁移当前统一放在 `src/Pim.Infrastructure/Data/Migrations`。模块改动持久化 schema 时：

1. 在 `{Name}Module.RegisterServices` 注册模块 assembly。
2. 添加或更新实体配置。
3. 针对 `PimDbContext` 生成 EF migration。
4. 保持 migration 聚焦在本模块 schema。
5. 风险不低时，为模型注册或服务行为添加测试。

`InitializeAsync` 不应该静默创建普通业务表。普通表属于 migration。`InitializeAsync` 可用于可选分表、来源初始化、轻量启动校验等特殊场景。

## 外部输入和 daemon 预留

有些模块依赖尚未完成的数据来源。定位可能依赖未来 Android daemon，健康可能依赖手机集成或导入源。这是允许的。

但服务端边界现在就要设计好。

### 上传 DTO 最小要求

daemon 类来源的上传请求应包含：

- `deviceId`
- `source`
- `sourceProvider`
- `batchId` 或上传尝试 id
- batch 型数据的 `capturedFrom` 和 `capturedTo`
- records 列表
- 每条记录的 observed time
- 每条记录的 source id 或幂等字段
- 必要的原始 metadata

上传响应应包含：

- accepted count
- inserted count
- updated count
- skipped duplicate count
- rejected count
- warnings
- server receive time
- next upload cursor，如果需要

### 质量或状态接口

任何带外部采集的模块都应该尽早暴露质量接口：

```text
GET /api/v1/{module}/quality
GET /api/v1/{module}/sources
```

质量接口回答：

- 是否收到数据
- 最新事实发生时间
- 最新接收时间
- 来源和设备覆盖情况
- 明显数据缺口
- 最近上传错误，如果有
- Web 应显示 normal、warning、critical、empty 还是 unavailable

系统级健康状态优先复用现有 operations/status 能力，但模块自身的数据质量仍放在模块 API 下。

### Android 尚未完成时

如果 Android daemon 尚未实现，不要阻塞服务端模块。

应该先做：

- 定义上传 DTO
- 定义服务端 ingestion service
- 必要时提供手动 seed 或 import
- 在模块 `README.md` 记录未来 Android sender 行为
- 用 fake device id 测试上传幂等
- Web 空状态明确说明数据源尚未上报

## Web 模块结构

使用当前 React/Vite 结构：

```text
src/client-web/src/
|-- api/{module}.ts
|-- pages/{Module}Page.tsx
|-- components/{module}/
|   `-- {Module}Panel.tsx
`-- types/index.ts
```

大模块可以拆出本地类型文件，但要保证 API-facing 类型容易找到。

Web API 文件应该：

- 集中定义可测试的路径 builder
- 统一解包 `ApiResponse<T>`
- 只做传输兼容性归一化
- 避免服务端应该拥有的业务判断

页面接入位置：

- `src/client-web/src/layout/Sidebar.tsx`
- `src/client-web/src/layout/AppLayout.tsx`

大页面或低频页面优先 lazy load。

## 横向能力接入

### Today

Today section 通过 `ITodaySectionProvider` 注册，目前注册点在 `src/Pim.Api/Program.cs`。

模块有每日注意力价值时再接入 Today，例如：

- 今日健康摘要
- 最新定位异常
- 待处理导入冲突
- daemon 上传失败
- 复盘建议

Today section 规则：

- provider 生成服务端拥有的摘要。
- Web 只按返回 DTO 渲染。
- 状态使用现有 `TodaySectionStatuses`。
- links 指向模块页面或 API 详情。

模块没准备好 Today 时不要硬加空卡片。

### 全局搜索

用户应该能从全局搜索找到模块对象时，实现 `ISearchProvider`。

搜索结果包含：

- module name
- result type
- stable id
- title
- short snippet
- Web URL

除非确实有检查价值并且安全，不要把隐私性强的原始记录暴露到全局搜索。

### 状态和运维

跨模块能力优先复用基础设施：

- `IAuditLogService`：重要写操作和风险操作。
- `IOperationConfirmationService`：preview/confirm 流程。
- `IBackgroundJobStatusService` 或 Hangfire：后台任务。
- `ISystemStatusService`：系统状态组合。

有自身质量模型的模块仍应暴露 `/api/v1/{module}/quality`；系统状态页可以后续聚合它。

### 审计和确认

建议审计：

- 删除
- 恢复
- 导入
- 敏感导出
- 大范围重算
- 接受 AI 建议
- 写回外部系统
- 批量更新

建议 preview/confirm：

- 破坏性或高影响变更
- 写外部系统
- 接受会影响历史数据的规则
- 批量清理定位或健康数据
- AI 生成的大范围修改

### 后台任务

以下场景优先使用后台任务：

- 耗时导入
- 重算
- 汇总生成
- 外部 provider 同步
- embedding 或文本提取
- 数据质量扫描

任务结果要可查询。不要要求用户看日志才能知道任务是否成功。

## 跨模块依赖

避免模块之间直接耦合。

推荐：

- 稳定平台能力放到 shared/core 接口后再被模块调用。
- 模块 A 只调用模块 B 明确暴露的公共 API 或稳定 service。
- 跨模块引用先保存 `{moduleName, objectType, objectId}`，为后续绑定留空间。

风险做法：

- 模块 A 直接查询模块 B 的内部 entity。
- 模块 A 复制模块 B 的业务规则。
- 模块 A 为了局部方便修改共享基础设施。

如果两个模块后续需要协作，等两个边界稳定后再添加 adapter 或 integration service。

## 模块 README

较完整的新模块应包含 `src/modules/Pim.Module.{Name}/README.md`。

README 保持短而实用：

- 模块目的
- API 前缀
- 原始数据表
- 派生数据表
- 外部来源
- 幂等规则
- Web 页面路由
- Today/search/status 接入状态
- 已知缺口
- 验证命令

并行开发时，另一个开发者应该不用读完所有代码就能理解模块边界。

## 健康模块示例

健康模块应该先作为事实和汇总模块，不要一开始就做医疗建议引擎。

可能的 API：

```text
POST /api/v1/health/samples/upload
GET  /api/v1/health/samples?from=&to=&type=&page=&pageSize=
GET  /api/v1/health/daily?from=&to=
GET  /api/v1/health/quality
```

原始事实：

- metric type，例如 heart rate、sleep、steps、workout
- value 和 unit
- observed time range
- source provider
- device id
- source record id
- raw json

派生数据：

- 日汇总
- 趋势标记
- 数据质量缺口

早期版本不要诊断健康状况。除非后续设计明确扩大范围，否则文案只描述数据可用性、趋势和用户自有记录。

## 定位模块示例

定位模块应该在 Android daemon 尚未完成前就预留上传边界。

可能的 API：

```text
POST /api/v1/location/points/upload
GET  /api/v1/location/points?from=&to=&page=&pageSize=
GET  /api/v1/location/stays?from=&to=
GET  /api/v1/location/trips?from=&to=
GET  /api/v1/location/quality
POST /api/v1/location/stays/recompute-preview
POST /api/v1/location/stays/recompute
```

原始事实：

- latitude
- longitude
- accuracy meters
- altitude，如果有
- speed，如果有
- provider，例如 gps、network、fused
- observed time
- received time
- device id
- source record id
- raw json

派生数据：

- 停留点
- 行程
- 常去地点
- 数据质量缺口

不要等 Android 完成才做服务端模块。先用 fake device id 和测试 batch 把 ingestion 契约稳定下来，再让 Android 后续接入。

## 测试要求

测试范围按风险缩放。

后端测试覆盖：

- service 行为
- 上传幂等
- 查询过滤
- 空状态
- 重复数据处理
- 重要状态流转
- quality/status 汇总
- 风险操作 preview/confirm

Web 测试覆盖：

- API path builder
- 必要 DTO normalization
- 关键 UI 状态 helper
- React 渲染外的空状态和错误状态 helper

推荐验证：

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

只运行和改动面相关的命令。后端和 Web 都动了，就两个都跑。

## 并行开发规则

开发新模块时：

1. 先写模块 README 或简短设计说明。
2. 写 endpoint 前先确定稳定 module name 和 API prefix。
3. 模块专属代码留在模块目录，除非确实需要共享能力。
4. 通过 `Pim.Api.csproj` 注册模块；除 Today section 等横向 provider 外，不要在 `Program.cs` 硬编码模块启动逻辑。
5. migration 保持聚焦，检查 model snapshot 是否有无关变化。
6. 除非任务明确要求，不要修改 daemon 默认地址或 API base URL。
7. 不把生成产物放进提交。
8. 不为方便本模块而静默重塑其他模块 DTO。
9. 记录临时不一致和计划融合点。
10. 为公共契约添加聚焦测试。

## 新模块检查清单

开工前：

- [ ] 一段话写清模块目的。
- [ ] 确定模块名和 `/api/v1/{module}` 前缀。
- [ ] 区分原始事实和派生数据。
- [ ] 命名当前外部来源和未来缺失来源。
- [ ] 明确上传或导入的幂等策略。
- [ ] 如果需要 Web 页面，明确路由和空状态。

后端：

- [ ] 创建 `src/modules/Pim.Module.{Name}`。
- [ ] `{Name}Module` 实现 `IModule`。
- [ ] 在 `RegisterServices` 注册模块服务。
- [ ] 有 EF entity 时注册模块 assembly。
- [ ] 在 `src/Pim.Api/Pim.Api.csproj` 引用模块。
- [ ] endpoint 使用 `ApiResponse<T>`。
- [ ] 分页列表使用 `PagedResult<T>`。
- [ ] 相关上传接口具备幂等性。
- [ ] 外部来源模块有 quality/status endpoint。
- [ ] 相关重要写操作有审计。
- [ ] 相关风险操作有 preview/confirmation。

数据：

- [ ] entity 使用 `Guid` id。
- [ ] 时间点字段使用 `DateTimeOffset`。
- [ ] 必要时建模用户所有权。
- [ ] 必要时建模设备和来源。
- [ ] 来源 shape 可能变化时保留 raw payload。
- [ ] EF configuration 明确。
- [ ] migration 只包含有意 schema 变化。

Web：

- [ ] Web 调用模块时创建 `src/client-web/src/api/{module}.ts`。
- [ ] API path 集中定义且可测试。
- [ ] 有页面时在 `AppLayout` 注册 route。
- [ ] 面向用户的页面在 Sidebar 注册入口。
- [ ] Web 展示服务端状态和空状态，不发明业务含义。
- [ ] 大页面按需 lazy load。

横向能力：

- [ ] 只有具备每日注意力价值时才添加 Today section。
- [ ] 只有用户需要全局查找对象时才添加 `ISearchProvider`。
- [ ] 后台任务状态可查询。
- [ ] 未来 MCP 可以包装 API，不需要重写业务逻辑。

验证：

- [ ] 后端改动通过相关后端测试。
- [ ] Web 改动通过 Web build。
- [ ] `git status --short --branch` 只显示有意变化。
- [ ] 模块 README 或文档记录已知缺口和未来融合点。

## 完成标准

一个模块准备合入时，另一个开发者应该能只看公共边界就回答：

- 模块拥有什么能力？
- API 前缀是什么？
- 保存哪些原始事实？
- 哪些派生数据可以重算？
- 上传或导入如何去重？
- Web 展示什么，服务端决定什么？
- 当前或未来依赖什么外部来源？
- 模块如何报告健康状态或数据质量？
- 接入了哪些横向能力？
- 哪些测试证明公共边界可用？

这就是并行开发的标准：每个模块可以独立前进，但不会变成孤岛。
