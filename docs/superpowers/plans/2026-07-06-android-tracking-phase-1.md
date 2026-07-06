# Android Tracking Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成安卓端一期追踪闭环：打开 App 后展示中文 UI、自动同步最近 14 天内服务器确认缺失的应用使用数据、支持手动 GNSS 定位上传，并在服务端和 Web UI 查询历史记录与诊断状态。

**Architecture:** 新增 `Pim.Module.Mobile`，保留 PC tracker 的独立性；Android 通过新的 mobile API 上传使用事件、fallback 汇总、应用元数据、定位点，并继续用现有 daemon heartbeat 上报 `daemonKind=android` 状态；Web 新增移动记录与历史位置页面，状态页读取 mobile quality 展示诊断。服务端保存原始事实，派生移动会话与质量摘要，Android 只做可见前台 UI 同步，不做后台保活和后台定位。

**Tech Stack:** .NET 8 Minimal API, EF Core, xUnit, Kotlin Android, Hilt, Room, Retrofit, Jetpack Compose, React 19, Vite, TanStack Query, Leaflet/OpenStreetMap, GitHub Actions.

---

## 最终验收目标

目标模式可直接使用以下清单作为完成判定：

- [ ] `dotnet test Pim.sln` 在本地通过。
- [ ] `dotnet build src/Pim.Api/Pim.Api.csproj` 在本地通过。
- [ ] `npm --prefix src/client-web run build` 在本地通过。
- [ ] `cd src/client-android; .\gradlew.bat assembleDebug --no-daemon` 在本地通过；若本机缺少 Java/Android SDK，则记录阻塞原因，并以 GitHub Actions 的 Android 构建结果作为 APK 构建验收。
- [ ] Android App 打开后不再立即 `finish()`，默认显示中文状态界面，包含服务器、登录、权限、同步进度、队列、最近日志和操作入口。
- [ ] Android App 登录后打开会自动请求 `/api/v1/mobile/sync/gaps`，只采集服务器返回窗口，最大回补到最近 14 天。
- [ ] Android App 定位页实时显示经纬度、水平误差、来源、海拔、速度、方向、时间、等待时长和提交状态；误差 `<=10m` 自动提交，`<=50m` 可手动提交，`>50m` 禁止提交。
- [ ] 服务端新增 mobile 数据表和 API，定位点 `50m` 被接受，`>50m` 被拒绝，使用事件上传幂等。
- [ ] Web 新增“手机记录”和“历史位置”中文页面；历史位置包含可用地图，移动记录能区分事件会话和 fallback 汇总。
- [ ] 状态页包含 Android heartbeat、移动同步、移动定位和移动应用元数据诊断。
- [ ] `git push origin master` 成功。
- [ ] GitHub Actions 中后端/API、Web、Android 工作流均在 `master` 上成功；若路径过滤未自动触发，手动 dispatch 对应工作流并等待成功。

## 并行子代理执行图

最多同时运行 14 个子代理。本计划推荐每波并发不超过 8 个，所有写入范围互斥；主代理负责整合、冲突处理、最终验证、提交和推送。

### Wave 1：后端与 CI 并行

- Worker B1：后端 mobile DTO、实体、EF configuration、模块骨架。写入范围：`src/modules/Pim.Module.Mobile/**` 的 DTO/Entities/Module 文件，`Pim.sln`，`src/Pim.Api/Pim.Api.csproj`，`tests/Pim.UnitTests/Pim.UnitTests.csproj`。
- Worker B2：后端 mobile 业务服务与服务测试。写入范围：`src/modules/Pim.Module.Mobile/Services/**`，`tests/Pim.UnitTests/Mobile/*ServiceTests.cs`。
- Worker B3：后端 endpoint、endpoint path 测试、heartbeat Android 测试。写入范围：`src/modules/Pim.Module.Mobile/MobileModule.cs` endpoint 段，`tests/Pim.UnitTests/Mobile/*Endpoint*`，`tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`。
- Worker C1：GitHub Actions 后端/API workflow。写入范围：`.github/workflows/build-api.yml`。

### Wave 2：Android 并行

- Worker A1：Android core 网络、服务端设置、登录和 API models。写入范围：`src/client-android/core/**`。
- Worker A2：Android Room 数据、使用事件采集、应用元数据采集。写入范围：`src/client-android/app/src/main/java/com/pim/app/data/**`，`src/client-android/app/src/main/java/com/pim/app/mobile/usage/**`。
- Worker A3：Android 同步协调、heartbeat、结构化日志。写入范围：`src/client-android/app/src/main/java/com/pim/app/mobile/sync/**`，`src/client-android/app/src/main/java/com/pim/app/mobile/logs/**`，`src/client-android/app/src/main/java/com/pim/app/daemon/**`。
- Worker A4：Android Compose UI、导航、定位页和 `MainActivity`。写入范围：`src/client-android/app/src/main/java/com/pim/app/ui/**`，`src/client-android/app/src/main/java/com/pim/app/location/**`，`src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`，`src/client-android/app/src/main/AndroidManifest.xml`。

### Wave 3：Web 并行

- Worker W1：Web mobile API types、API path tests、navigation tests。写入范围：`src/client-web/src/api/mobile.ts`，`tests/client-web/mobile*.test.ts*`，`tests/client-web/tsconfig.mobile.json`。
- Worker W2：Web 手机记录页、移动组件、历史位置页和地图依赖。写入范围：`src/client-web/src/pages/MobileRecordsPage.tsx`，`src/client-web/src/pages/HistoricalLocationPage.tsx`，`src/client-web/src/components/mobile/**`，`src/client-web/package.json`，`src/client-web/package-lock.json`。
- Worker W3：Web 路由、侧边栏、状态页移动诊断。写入范围：`src/client-web/src/layout/AppLayout.tsx`，`src/client-web/src/layout/Sidebar.tsx`，`src/client-web/src/pages/StatusPage.tsx`，`src/client-web/src/components/status/**`。

### Wave 4：集成与复核

- Worker R1：跨模块规格复核。只读，输出缺口清单。
- Worker R2：代码质量复核。只读，输出风险清单。
- 主代理：修复复核发现的问题，运行最终验收命令，提交、推送、等待 GA。

所有 worker 提示必须包含：当前仓库多人并行工作，不要 revert 他人改动；只能修改分配范围；遵循 TDD，生产代码前先写失败测试；返回改动文件、验证命令、阻塞项。

## 公共接口契约

后端和客户端都以这些名字对齐，避免各 worker 自创字段：

```csharp
public sealed record MobileDeviceRegisterRequest(
    string DeviceId,
    string? AndroidIdHash,
    string DisplayName,
    string Manufacturer,
    string Brand,
    string Model,
    string AndroidVersion,
    int SdkInt,
    string AppVersion,
    string MetadataJson);

public sealed record MobileGapRequest(
    string DeviceId,
    DateTimeOffset RangeStartUtc,
    DateTimeOffset RangeEndUtc,
    string CapabilityJson);

public sealed record MobileGapWindowDto(
    DateTimeOffset WindowStartUtc,
    DateTimeOffset WindowEndUtc,
    string Reason,
    string SourcePreference);

public sealed record MobileUsageEventsUploadRequest(
    string DeviceId,
    string ClientBatchId,
    DateTimeOffset SourceWindowStartUtc,
    DateTimeOffset SourceWindowEndUtc,
    IReadOnlyList<MobileAppMetadataDto> Apps,
    IReadOnlyList<MobileUsageEventDto> Events,
    IReadOnlyList<MobileUsageSummaryDto> FallbackSummaries);

public sealed record MobileLocationPointRequest(
    string DeviceId,
    DateTimeOffset RecordedAtUtc,
    double Latitude,
    double Longitude,
    double HorizontalAccuracyMeters,
    string Provider,
    string SourceKind,
    double? AltitudeMeters,
    double? VerticalAccuracyMeters,
    double? SpeedMetersPerSecond,
    double? SpeedAccuracyMetersPerSecond,
    double? BearingDegrees,
    double? BearingAccuracyDegrees,
    bool IsAutoSubmitted,
    string RawJson);
```

Android Retrofit path names must match:

```kotlin
@POST("mobile/devices/register")
suspend fun registerMobileDevice(@Body request: MobileDeviceRegisterRequest): ApiResponse<MobileDeviceDto>

@POST("mobile/sync/gaps")
suspend fun getMobileGaps(@Body request: MobileGapRequest): ApiResponse<MobileGapResponse>

@POST("mobile/usage/events")
suspend fun uploadMobileUsage(@Body request: MobileUsageEventsUploadRequest): ApiResponse<MobileIngestResponse>

@POST("mobile/location/points")
suspend fun uploadMobileLocation(@Body request: MobileLocationPointRequest): ApiResponse<MobileLocationPointDto>

@POST("daemon/heartbeat")
suspend fun sendHeartbeat(@Body request: DaemonHeartbeatRequest): ApiResponse<DaemonHeartbeatDto>
```

Web API paths must match:

```ts
export const mobileApiPaths = {
  devices: '/mobile/devices',
  summary: (date: string, deviceId?: string) => `/mobile/summary?date=${encodeURIComponent(date)}${deviceId ? `&deviceId=${encodeURIComponent(deviceId)}` : ''}`,
  timeline: (date: string, deviceId?: string) => `/mobile/timeline?date=${encodeURIComponent(date)}${deviceId ? `&deviceId=${encodeURIComponent(deviceId)}` : ''}`,
  locations: (start: string, end: string, deviceId?: string, maxAccuracyMeters = 50) =>
    `/mobile/location/history?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}&maxAccuracyMeters=${maxAccuracyMeters}${deviceId ? `&deviceId=${encodeURIComponent(deviceId)}` : ''}`,
  quality: (date?: string, deviceId?: string) =>
    `/mobile/quality${date || deviceId ? '?' : ''}${[
      date ? `date=${encodeURIComponent(date)}` : '',
      deviceId ? `deviceId=${encodeURIComponent(deviceId)}` : '',
    ].filter(Boolean).join('&')}`,
};
```

## Task 0：计划提交

**Files:**
- Create: `docs/superpowers/plans/2026-07-06-android-tracking-phase-1.md`

- [ ] **Step 1: 检查仓库状态**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git status --short --branch
```

Expected: `master...origin/master [ahead 1]` 或显示仅包含本计划文档的未提交变更；若 `master` 落后，先 `git pull --ff-only`。

- [ ] **Step 2: 保存计划后做占位词扫描**

Run:

```powershell
rg -n "T[B]D|T[O]DO|implement [l]ater|fill in [d]etails|Similar to [T]ask|appropriate error [h]andling" docs\superpowers\plans\2026-07-06-android-tracking-phase-1.md
```

Expected: 无匹配。

- [ ] **Step 3: 提交计划文档**

Run:

```powershell
git add docs\superpowers\plans\2026-07-06-android-tracking-phase-1.md
git commit -m "docs: plan android tracking phase 1"
```

Expected: 生成一个 `docs:` commit。

## Task 1：后端 Mobile 模块骨架、DTO、实体与 EF 配置

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Pim.Module.Mobile.csproj`
- Create: `src/modules/Pim.Module.Mobile/MobileModule.cs`
- Create: `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileUsageEventEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileLocationPointEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs`
- Modify: `Pim.sln`
- Modify: `src/Pim.Api/Pim.Api.csproj`
- Modify: `tests/Pim.UnitTests/Pim.UnitTests.csproj`
- Test: `tests/Pim.UnitTests/Mobile/MobileModelTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs`

- [ ] **Step 1: 写失败测试，证明 mobile module 会注册表结构与项目引用**

Create `tests/Pim.UnitTests/Mobile/MobileModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Mobile;
using Pim.Module.Mobile.Entities;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileModelTests
{
    [Fact]
    public void MobileModuleRegistersExpectedEntities()
    {
        new MobileModule().RegisterServices(new ServiceCollection(), new ConfigurationBuilder().Build());
        using var db = new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options);

        var entityNames = db.Model.GetEntityTypes().Select(e => e.ClrType).ToHashSet();

        Assert.Contains(typeof(MobileDeviceEntity), entityNames);
        Assert.Contains(typeof(MobileAppCatalogEntity), entityNames);
        Assert.Contains(typeof(MobileUsageEventEntity), entityNames);
        Assert.Contains(typeof(MobileUsageSummaryEntity), entityNames);
        Assert.Contains(typeof(MobileUsageSessionEntity), entityNames);
        Assert.Contains(typeof(MobileLocationPointEntity), entityNames);
        Assert.Contains(typeof(MobileSyncBatchEntity), entityNames);
    }

    [Fact]
    public void MobileLocationAccuracyUsesDecimalPrecision()
    {
        new MobileModule().RegisterServices(new ServiceCollection(), new ConfigurationBuilder().Build());
        using var db = new PimDbContext(new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options);

        var accuracy = db.Model.FindEntityType(typeof(MobileLocationPointEntity))!
            .FindProperty(nameof(MobileLocationPointEntity.HorizontalAccuracyMeters))!;

        Assert.Equal(9, accuracy.GetPrecision());
        Assert.Equal(2, accuracy.GetScale());
    }
}
```

Create `tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs`:

```csharp
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileModuleProjectReferenceTests
{
    [Fact]
    public void ApiProjectReferencesMobileModule()
    {
        var csproj = File.ReadAllText(Path.Combine("..", "..", "..", "..", "src", "Pim.Api", "Pim.Api.csproj"));
        Assert.Contains(@"..\modules\Pim.Module.Mobile\Pim.Module.Mobile.csproj", csproj);
    }

    [Fact]
    public void UnitTestsReferenceMobileModule()
    {
        var csproj = File.ReadAllText(Path.Combine("..", "..", "..", "Pim.UnitTests.csproj"));
        Assert.Contains(@"..\..\src\modules\Pim.Module.Mobile\Pim.Module.Mobile.csproj", csproj);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileModel|FullyQualifiedName~MobileModuleProjectReference"
```

Expected: FAIL，原因是 `Pim.Module.Mobile` 命名空间或项目引用不存在。

- [ ] **Step 3: 新建 module csproj 与 DTO/实体/配置**

Create `src/modules/Pim.Module.Mobile/Pim.Module.Mobile.csproj`:

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <ItemGroup>
    <ProjectReference Include="..\..\Pim.Core\Pim.Core.csproj" />
    <ProjectReference Include="..\..\Pim.Infrastructure\Pim.Infrastructure.csproj" />
  </ItemGroup>
  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>
</Project>
```

Implement DTOs using the public contract section exactly. Entity names must expose public settable properties matching table fields. `MobileEntityConfigurations.cs` must map:

```csharp
builder.Entity<MobileDeviceEntity>(e =>
{
    e.ToTable("mobile_devices");
    e.HasKey(x => x.Id);
    e.HasIndex(x => new { x.UserId, x.DeviceId }).IsUnique();
    e.Property(x => x.MetadataJson).HasColumnType("jsonb").HasDefaultValue("{}");
    e.Property(x => x.FirstSeenAt).HasDefaultValueSql("now()");
    e.Property(x => x.LastSeenAt).HasDefaultValueSql("now()");
});
```

Repeat the same style for app catalog, usage events, usage summaries, usage sessions, location points, and sync batches, including the unique keys and indexes from the design spec. `MobileLocationPointEntity.HorizontalAccuracyMeters` must use `.HasPrecision(9, 2)` and latitude/longitude `.HasPrecision(10, 7)`.

- [ ] **Step 4: 注册 module 并补项目引用**

`MobileModule.RegisterServices` must call:

```csharp
PimDbContext.RegisterModuleAssembly(Assembly.GetExecutingAssembly());
services.AddScoped<MobileDeviceService>();
services.AddScoped<MobileGapService>();
services.AddScoped<MobileUsageIngestService>();
services.AddScoped<MobileSessionInterpreter>();
services.AddScoped<MobileLocationService>();
services.AddScoped<MobileQueryService>();
services.AddScoped<MobileQualityService>();
```

Add `<ProjectReference Include="..\modules\Pim.Module.Mobile\Pim.Module.Mobile.csproj" />` to `src/Pim.Api/Pim.Api.csproj`.

Add `<ProjectReference Include="..\..\src\modules\Pim.Module.Mobile\Pim.Module.Mobile.csproj" />` to `tests/Pim.UnitTests/Pim.UnitTests.csproj`.

Add `Pim.Module.Mobile` to `Pim.sln` under the `modules` solution folder with a stable GUID.

- [ ] **Step 5: 运行测试确认通过**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileModel|FullyQualifiedName~MobileModuleProjectReference"
```

Expected: PASS。

## Task 2：后端 Mobile 业务服务、幂等 ingest、14 天 gap、定位校验与质量摘要

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileGapService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileQueryService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileDeviceServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileQualityServiceTests.cs`

- [ ] **Step 1: 写 device upsert 失败测试**

Create a test that calls `MobileDeviceService.RegisterAsync(userId, request, ct)` twice with the same `DeviceId`, changes `Model`, and asserts one row remains and `LastSeenAt >= FirstSeenAt`.

Expected service signature:

```csharp
public Task<MobileDeviceDto> RegisterAsync(Guid userId, MobileDeviceRegisterRequest request, CancellationToken ct = default)
```

- [ ] **Step 2: 写 gap 失败测试**

Create tests:

```csharp
[Fact]
public async Task GapsNeverStartBeforeFourteenDayLimit()
```

Use fixed now `2026-07-06T12:00:00Z`, request `RangeStartUtc` 30 days earlier, assert every window starts at or after `2026-06-22T00:00:00Z`.

```csharp
[Fact]
public async Task ExistingEventDayDoesNotReturnDuplicateFullDayGap()
```

Seed one `MobileUsageEventEntity` inside a day and assert that day is not returned as a full missing day.

Inject clock through:

```csharp
public interface IMobileClock { DateTimeOffset UtcNow { get; } }
```

- [ ] **Step 3: 写 usage ingest 幂等失败测试**

Upload the same `MobileUsageEventsUploadRequest` twice. Assert first response has accepted count `1`, second has skipped count `1`, and only one `MobileUsageEventEntity` exists. Also assert app metadata upserts into one `MobileAppCatalogEntity`.

- [ ] **Step 4: 写 session interpreter 失败测试**

Given foreground event at `10:00`, background event at `10:05`, assert one session with duration `300000`. Given foreground App A at `10:00`, foreground App B at `10:03` without App A background, assert App A closes at `10:03` and has a quality flag containing `closed-by-next-foreground`.

- [ ] **Step 5: 写 location 失败测试**

Create two tests:

```csharp
[Theory]
[InlineData(50, true)]
[InlineData(50.01, false)]
public async Task LocationAccuracyBoundaryIsEnforced(double accuracy, bool accepted)
```

Assert `50` saves and `50.01` throws `DomainException`.

Add coordinate validation tests for latitude outside `[-90,90]` and longitude outside `[-180,180]`.

- [ ] **Step 6: 写 quality 失败测试**

Seed a stale Android heartbeat, one failed sync batch, one fallback summary, and one rejected location batch. Assert `MobileQualityService.GetQualityAsync` returns components with keys `android-heartbeat`, `mobile-sync`, `mobile-usage-coverage`, `mobile-location`.

- [ ] **Step 7: 运行 mobile service 测试确认失败**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~Mobile"
```

Expected: FAIL，原因是 services 尚未实现。

- [ ] **Step 8: 实现 services 到测试通过**

Implementation rules:

- `MobileDeviceService` uses `userId + deviceId` upsert.
- `MobileGapService` clamps request range to `UtcNow.AddDays(-14)` and returns UTC day windows with reason `missing-day`, `missing-tail`, or `fallback-only`.
- `MobileUsageIngestService` upserts app catalog first, inserts events idempotently by unique key, inserts fallback summaries separately, records one `MobileSyncBatchEntity`, and calls `MobileSessionInterpreter` after accepted events.
- `MobileSessionInterpreter` rebuilds sessions for the affected device/window by deleting existing sessions in that window and writing derived rows.
- `MobileLocationService` rejects invalid coordinates and `HorizontalAccuracyMeters > 50`, assigns quality `high` for `<=10`, `usable` for `<=50`.
- `MobileQueryService` returns daily summary, timeline sessions/fallback blocks, devices, and location history.
- `MobileQualityService` reads latest android heartbeat through `PimDbContext.DaemonHeartbeats` and mobile tables; do not add direct mobile references inside `SystemStatusService`.

- [ ] **Step 9: 运行 mobile service 测试确认通过**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~Mobile"
```

Expected: PASS。

## Task 3：后端 API endpoints、migration、heartbeat Android 兼容与 API CI

**Files:**
- Modify: `src/modules/Pim.Module.Mobile/MobileModule.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/*_AddMobileModule.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/*_AddMobileModule.Designer.cs`
- Modify: `tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileEndpointPathTests.cs`
- Create: `.github/workflows/build-api.yml`

- [ ] **Step 1: 写 endpoint path 失败测试**

Create `MobileEndpointPathTests.cs` that asserts constants:

```csharp
Assert.Equal("/api/v1/mobile", MobileEndpointPaths.Root);
Assert.Equal("/api/v1/mobile/devices/register", MobileEndpointPaths.RegisterDevice);
Assert.Equal("/api/v1/mobile/sync/gaps", MobileEndpointPaths.SyncGaps);
Assert.Equal("/api/v1/mobile/usage/events", MobileEndpointPaths.UsageEvents);
Assert.Equal("/api/v1/mobile/location/points", MobileEndpointPaths.LocationPoints);
```

- [ ] **Step 2: 写 Android heartbeat 失败测试**

Extend `DaemonHeartbeatServiceTests` with a request:

```csharp
new DaemonHeartbeatRequest(
    "android-device-1",
    "android",
    "1.0.0",
    "http://127.0.0.1:5858",
    now,
    now,
    null,
    3,
    DaemonSourceState.Unknown,
    DaemonSourceState.Unknown,
    false,
    """{"usagePermission":true,"preciseLocationPermission":true}""");
```

Assert it is stored independently from a Windows heartbeat with the same device id and `DaemonKind == "android"`.

- [ ] **Step 3: 运行 endpoint/heartbeat 测试确认失败**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileEndpointPath|FullyQualifiedName~DaemonHeartbeat"
```

Expected: Mobile endpoint constants missing or heartbeat test fails before implementation.

- [ ] **Step 4: 实现 mobile endpoint group**

`MobileModule.MapEndpoints` must create:

```csharp
var group = endpoints.MapGroup(MobileEndpointPaths.Root).RequireAuthorization();
group.MapGet("/devices", ListDevicesAsync);
group.MapPost("/devices/register", RegisterDeviceAsync);
group.MapPost("/sync/gaps", GetGapsAsync);
group.MapPost("/usage/events", UploadUsageEventsAsync);
group.MapPost("/location/points", UploadLocationPointAsync);
group.MapGet("/summary", GetSummaryAsync);
group.MapGet("/timeline", GetTimelineAsync);
group.MapGet("/location/history", GetLocationHistoryAsync);
group.MapGet("/quality", GetQualityAsync);
```

Each endpoint returns `Results.Ok(ApiResponse<T>.Ok(result))`. Parse `date`, `start`, and `end` with `DateTime.Parse(..., CultureInfo.InvariantCulture)` and use current user id from the existing current user service pattern. If current user service is unavailable in minimal endpoint tests, keep endpoint path tests static and service tests cover behavior.

- [ ] **Step 5: 生成 EF migration**

Run:

```powershell
dotnet ef migrations add AddMobileModule --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

Expected: migration creates all `mobile_*` tables, indexes, unique constraints, and JSON defaults.

- [ ] **Step 6: 新增 API GitHub Actions**

Create `.github/workflows/build-api.yml`:

```yaml
name: Build API

on:
  push:
    branches: [ master ]
    paths:
      - 'src/Pim.Api/**'
      - 'src/Pim.Core/**'
      - 'src/Pim.Infrastructure/**'
      - 'src/modules/**'
      - 'tests/Pim.UnitTests/**'
      - 'Pim.sln'
      - '.github/workflows/build-api.yml'
  workflow_dispatch:

jobs:
  build-api:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-dotnet@v4
        with:
          dotnet-version: '8.0.x'
      - name: Restore
        run: dotnet restore Pim.sln
      - name: Test
        run: dotnet test Pim.sln --configuration Release --no-restore
      - name: Build API
        run: dotnet build src/Pim.Api/Pim.Api.csproj --configuration Release --no-restore
```

- [ ] **Step 7: 运行后端验证**

Run:

```powershell
dotnet test tests\Pim.UnitTests\Pim.UnitTests.csproj --filter "FullyQualifiedName~Mobile|FullyQualifiedName~DaemonHeartbeat"
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: PASS。

## Task 4：Android core 网络、设置、登录和 API models

**Files:**
- Modify: `src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt`

- [ ] **Step 1: 写 Kotlin 单元测试或 compile-check 场景**

Add tests if the Android test source set exists; otherwise add a pure Kotlin compile target by referencing the new models from `ApiService`. Minimum assertions:

```kotlin
check(ServerSettingsStore.DEFAULT_BASE_URL == "http://127.0.0.1:5858/api/v1/")
```

and compile verifies `registerMobileDevice`, `getMobileGaps`, `uploadMobileUsage`, `uploadMobileLocation`, `sendHeartbeat`.

- [ ] **Step 2: 运行 Android core 测试或 assemble 确认失败**

Run:

```powershell
cd src\client-android
.\gradlew.bat :core:testDebugUnitTest --no-daemon
```

If no unit test task is configured, run:

```powershell
cd src\client-android
.\gradlew.bat :core:assembleDebug --no-daemon
```

Expected: FAIL because new types or provider do not exist.

- [ ] **Step 3: 实现 server settings 和动态 Retrofit**

`ServerSettingsStore` must persist base URL in SharedPreferences key `server_base_url`, defaulting to `http://127.0.0.1:5858/api/v1/`, and normalize missing trailing slash.

`ApiClientProvider` must expose:

```kotlin
@Singleton
class ApiClientProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val settingsStore: ServerSettingsStore,
) {
    fun apiService(): ApiService
}
```

It should rebuild Retrofit when base URL changes. `CoreModule` must remove hardcoded `http://39.105.78.130:5858/api/v1/`.

- [ ] **Step 4: 实现 models 和 ApiService endpoints**

Create serializable Kotlin data classes mirroring the public contract. `DaemonHeartbeatRequest` must include `daemonKind = "android"` usage through caller, and legacy source states should use `"Unknown"`.

- [ ] **Step 5: 运行 Android core 验证**

Run the same command as Step 2. Expected: PASS or progress to unrelated environment issue; record exact blocker if Java/SDK missing.

## Task 5：Android Room 数据、使用事件采集、应用元数据

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt`

- [ ] **Step 1: 写数据层失败测试或 compile-check**

Add DAO methods that must compile:

```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertUsageEvents(events: List<MobileUsageEventEntity>): List<Long>

@Query("SELECT COUNT(*) FROM mobile_usage_events WHERE syncStatus = :status")
suspend fun countUsageEventsByStatus(status: String): Int
```

Compile should fail before entities are added.

- [ ] **Step 2: 运行 app compile 确认失败**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: FAIL because Room entities and collectors do not exist.

- [ ] **Step 3: 实现 Room entities**

Create tables:

- `mobile_usage_events`
- `mobile_usage_summaries`
- `mobile_app_metadata`
- `mobile_location_points`
- `mobile_sync_batches`
- `mobile_logs`
- `mobile_device_profile`

Every syncable row must have `syncStatus`, `lastError`, `createdAtUtc`, `updatedAtUtc`. Increment `AppDatabase` version and define a destructive migration only if current project already uses it; otherwise add a Room migration from current version to new version.

- [ ] **Step 4: 实现使用权限和采集器**

`UsageAccessChecker.hasUsageAccess()` must query `UsageStatsManager.queryUsageStats` over a short recent window and return false for empty results.

`UsageEventCollector.collect(startUtc, endUtc)` must query `UsageStatsManager.queryEvents`, preserve raw fields, and fallback to `queryUsageStats` when no events are returned.

`AppMetadataCollector.collect(packageNames)` must read label, versionName, longVersionCode/versionCode, firstInstallTime, lastUpdateTime, system app flag, category, installer package name when available.

- [ ] **Step 5: 运行 app compile**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: PASS or progress to unrelated environment issue.

## Task 6：Android 同步协调、heartbeat、日志与打开即同步

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/daemon/UploadWorker.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/daemon/DataCollector.kt`

- [ ] **Step 1: 写 sync coordinator 失败测试或 compile-check**

Expected public method:

```kotlin
suspend fun syncOnOpen(): MobileSyncState
```

Expected state fields: `phase`, `progressText`, `acceptedCount`, `skippedCount`, `rejectedCount`, `failedCount`, `lastError`, `pendingQueueCount`.

- [ ] **Step 2: 运行 Android app compile 确认失败**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: FAIL before coordinator exists.

- [ ] **Step 3: 实现日志与 sync coordinator**

`syncOnOpen()` order:

1. Check token and server URL.
2. Check usage access; log and skip usage if missing.
3. Register device.
4. Request gaps with `[now-14d, now]`.
5. For each returned window, collect events/app metadata/fallback summaries.
6. Upload batches, mark accepted rows synced, retain retryable failures.
7. Send Android heartbeat with status JSON.

Write structured logs for every phase. Do not schedule closed-app keepalive in this phase.

- [ ] **Step 4: 更新旧 daemon 入口**

Keep existing background classes compiling, but do not let `MainActivity` depend on starting foreground service to be usable. `UploadWorker` can call `MobileSyncCoordinator` when invoked, but no new background keepalive behavior is introduced.

- [ ] **Step 5: 运行 Android app compile**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: PASS or documented environment blocker.

## Task 7：Android Compose 中文 UI、状态、设置、定位

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/NavRoutes.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/usage/UsageScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/location/LocationScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/location/LocationViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt`

- [ ] **Step 1: 写 UI 行为 compile-check**

`MainActivity` must call `setContent { PimAppScaffold() }` and must not call `finish()` in `onCreate`. `LocationViewModel` must expose `canManualSubmit`, `autoSubmitTriggered`, and `inlineReason`.

- [ ] **Step 2: 运行 Android app compile 确认失败**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: FAIL before UI classes exist.

- [ ] **Step 3: 实现 Manifest 权限**

Ensure:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Do not add `ACCESS_BACKGROUND_LOCATION`.

- [ ] **Step 4: 实现状态和设置 UI**

中文 UI tab：`状态`、`使用`、`定位`、`设置`。状态页显示服务器、登录、使用权限、精确定位权限、同步阶段、进度、队列、最后尝试、最后成功、最后错误、最近日志、立即同步按钮、定位入口。设置页可编辑服务器 URL 并登录。

- [ ] **Step 5: 实现定位 capture**

Use `LocationManager` GNSS/GPS provider first and keep source. `LocationCaptureRepository` emits `LocationFixState` with latitude, longitude, horizontal accuracy, provider, altitude, speed, bearing, timestamp, elapsed wait, submission state.

Rules:

- `accuracy <= 10` triggers auto submit once.
- `accuracy <= 50` enables manual submit.
- `accuracy > 50` disables submit and shows inline reason `误差大于 50 米，不能提交`。
- UI must not use modal dialog for location quality.

- [ ] **Step 6: 运行 Android app build**

Run:

```powershell
cd src\client-android
.\gradlew.bat assembleDebug --no-daemon
```

Expected: APK builds or exact local SDK/JDK blocker is documented.

## Task 8：Web mobile API、类型、路由测试与 Leaflet 依赖

**Files:**
- Create: `src/client-web/src/api/mobile.ts`
- Modify: `src/client-web/package.json`
- Modify: `src/client-web/package-lock.json`
- Create: `tests/client-web/mobileApiPath.test.ts`
- Create: `tests/client-web/mobileTypes.test.ts`
- Create: `tests/client-web/mobileNavigation.test.tsx`
- Create: `tests/client-web/tsconfig.mobile.json`

- [ ] **Step 1: 写 API path 失败测试**

`mobileApiPath.test.ts`:

```ts
import { mobileApiPaths } from '../../src/client-web/src/api/mobile';

if (mobileApiPaths.devices !== '/mobile/devices') throw new Error('devices path mismatch');
if (mobileApiPaths.summary('2026-07-06') !== '/mobile/summary?date=2026-07-06') throw new Error('summary path mismatch');
if (!mobileApiPaths.locations('2026-07-06T00:00:00Z', '2026-07-06T23:59:59Z').includes('maxAccuracyMeters=50')) throw new Error('location filter missing');
```

- [ ] **Step 2: 写 navigation 失败测试**

Assert `primaryNavItems` contains labels `手机记录` with `/mobile-records` and `历史位置` with `/location-history`.

- [ ] **Step 3: 安装地图依赖**

Run:

```powershell
npm --prefix src\client-web install leaflet react-leaflet @types/leaflet
```

Expected: `package.json` and `package-lock.json` updated.

- [ ] **Step 4: 运行 Web mobile tests 确认失败**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileNavigation.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
```

Expected: FAIL before API/routes exist.

- [ ] **Step 5: 实现 API types**

`src/client-web/src/api/mobile.ts` must export `mobileApiPaths` and functions:

```ts
export function getMobileDevices(): Promise<MobileDevice[]>
export function getMobileSummary(date: string, deviceId?: string): Promise<MobileSummary>
export function getMobileTimeline(date: string, deviceId?: string): Promise<MobileTimeline>
export function getMobileLocationHistory(params: MobileLocationHistoryParams): Promise<MobileLocationHistory>
export function getMobileQuality(date?: string, deviceId?: string): Promise<MobileQuality>
```

Use existing `apiGet<T>` and unwrap server `ApiResponse` consistently with nearby API modules.

- [ ] **Step 6: 运行 Web mobile tests 确认通过**

Run the same commands as Step 4. Expected: PASS。

## Task 9：Web 手机记录页、历史位置地图、状态页移动诊断

**Files:**
- Create: `src/client-web/src/pages/MobileRecordsPage.tsx`
- Create: `src/client-web/src/pages/HistoricalLocationPage.tsx`
- Create: `src/client-web/src/components/mobile/MobileMetricStrip.tsx`
- Create: `src/client-web/src/components/mobile/MobileTimeline.tsx`
- Create: `src/client-web/src/components/mobile/MobileAppRanking.tsx`
- Create: `src/client-web/src/components/mobile/MobileQualityPanel.tsx`
- Create: `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
- Create: `src/client-web/src/components/mobile/LocationPointList.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/pages/StatusPage.tsx`
- Test: `tests/client-web/mobileComponents.test.tsx`

- [ ] **Step 1: 写组件 smoke 失败测试**

`mobileComponents.test.tsx` should import the new page/components and assert exported functions exist:

```ts
import MobileRecordsPage from '../../src/client-web/src/pages/MobileRecordsPage';
import HistoricalLocationPage from '../../src/client-web/src/pages/HistoricalLocationPage';
import { formatAccuracyLabel } from '../../src/client-web/src/components/mobile/LocationPointList';

if (typeof MobileRecordsPage !== 'function') throw new Error('MobileRecordsPage missing');
if (typeof HistoricalLocationPage !== 'function') throw new Error('HistoricalLocationPage missing');
if (formatAccuracyLabel(9.4) !== '9.4 m') throw new Error('accuracy label mismatch');
```

- [ ] **Step 2: 运行组件测试确认失败**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileComponents.test.tsx
```

Expected: FAIL before pages/components exist.

- [ ] **Step 3: 实现手机记录页**

Layout:

- Header title `手机记录`。
- Controls: date input, device selector, refresh button.
- Metrics: total foreground time, app switch count, apps used, completeness, quality issue count, last upload.
- Panels: mobile timeline, app ranking, sync batch status, quality warnings.
- Fallback summary blocks must show label `fallback` or `汇总数据` and use visually different border/background from event sessions.

- [ ] **Step 4: 实现历史位置页**

Use `react-leaflet` with OpenStreetMap tile layer. Controls: start datetime, end datetime, device selector, max accuracy select/input, auto/manual filter, refresh. Map shows markers and optional line connecting chronological points. List shows recorded time, submitted time, accuracy, provider/source, auto/manual, coordinate, quality.

- [ ] **Step 5: 实现路由、侧边栏和状态页**

`AppLayout` routes:

```tsx
<Route path="/mobile-records" element={<MobileRecordsPage />} />
<Route path="/location-history" element={<HistoricalLocationPage />} />
```

`primaryNavItems` labels:

```ts
{ label: '手机记录', path: '/mobile-records', short: '机' }
{ label: '历史位置', path: '/location-history', short: '位' }
```

`StatusPage` adds `getMobileQuality()` query and a mobile diagnostics panel showing Android heartbeat, usage collection, sync batches, location capture, app metadata.

- [ ] **Step 6: 运行 Web tests 和 build**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
npm --prefix src/client-web run build
```

Expected: PASS。

## Task 10：中文文案修正、集成、复核与提交

**Files:**
- Modify only files touched by Tasks 1-9 when fixing integration issues.

- [ ] **Step 1: 全仓中文可见文案检查**

Run:

```powershell
rg -n "Mobile Records|Historical Location|Usage|Location|Settings|Status|Submit|Refresh|fallback" src\client-web\src src\client-android\app\src\main\java
```

Expected: 用户可见位置不出现英文主文案；技术字段、类型名、CSS 类、API 字段允许英文。

- [ ] **Step 2: 派发只读规格复核子代理**

Prompt must ask reviewer to compare implementation against `docs/superpowers/specs/2026-07-06-android-tracking-phase-1-design.md` and this plan, and return missing requirements with file paths.

- [ ] **Step 3: 派发只读代码质量复核子代理**

Prompt must ask reviewer to inspect changed backend, Android, Web, and CI files for correctness risks, migration hazards, broken build paths, security/privacy issues, and generated output accidentally staged.

- [ ] **Step 4: 修复复核发现的问题**

For each accepted finding, write or adjust a test first, run it red if feasible, implement the fix, and rerun the relevant verification.

- [ ] **Step 5: 本地最终验证**

Run:

```powershell
dotnet test Pim.sln
dotnet build src\Pim.Api\Pim.Api.csproj
npm --prefix src/client-web run build
cd src\client-android
.\gradlew.bat assembleDebug --no-daemon
```

Expected: all pass, except Android local build may be replaced by documented environment blocker plus GA pass.

- [ ] **Step 6: 检查 generated outputs 未被提交**

Run:

```powershell
git status --short
git diff --name-only --cached
```

Expected: staged files do not include `bin/`, `obj/`, `build/`, `dist/`, `publish/PimDaemon/`, `publish/*.zip`, `.dotnet-*`, `.superpowers/brainstorm/`, npm caches, or `src/Pim.Api/wwwroot` build artifacts.

- [ ] **Step 7: 提交实现**

Run:

```powershell
git add Pim.sln .github\workflows\build-api.yml src\Pim.Api\Pim.Api.csproj src\Pim.Infrastructure\Data\Migrations src\modules\Pim.Module.Mobile tests\Pim.UnitTests src\client-android src\client-web\src tests\client-web src\client-web\package.json src\client-web\package-lock.json
git commit -m "feat: add android mobile tracking phase 1"
```

Expected: one focused feature commit.

- [ ] **Step 8: 推送 master 并等待 GA**

Run:

```powershell
git push origin master
gh run list --branch master --limit 10
```

Identify runs for API, Web, Android, and Windows if triggered. For each required run:

```powershell
gh run watch <run-id> --exit-status
```

If a workflow is skipped by path filters, trigger it:

```powershell
gh workflow run "Build API" --ref master
gh workflow run "Build Web" --ref master
gh workflow run "Build Android APK" --ref master
```

Expected: required GitHub Actions complete successfully. If a run fails, fetch logs with `gh run view <run-id> --log-failed`, fix locally with tests, commit, push, and watch again.

## 自审记录

- Spec coverage: 后端 mobile module、Android 可见 UI、打开即同步、14 天 gap、UsageEvents/fallback、应用元数据、手动定位精度规则、Web 手机记录、Web 历史位置地图、状态诊断、无后台保活均映射到 Tasks 1-10。
- Placeholder scan: 已规避常见占位表达式，并把扫描命令写成不自我命中的形式。
- Type consistency: C# records、Kotlin Retrofit 方法、Web API path 名称在公共接口契约中固定，后续任务引用同一命名。
