# Mobile Records Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the redesigned `手机记录` analytics workbench: default last-7-days Beijing-time analytics, app classification and correction, heatmaps, charts, paginated drill-down timelines, quality flags, goals, anomalies, and GitHub Actions-verified master builds.

**Architecture:** Keep mobile raw facts as the source of truth and add derived analytics services, classification rules, aggregate projections, and paginated timeline blocks. Backend APIs provide range-aware analytics using `Asia/Shanghai` by default; the web client renders an analytics workbench over those APIs; Android strengthens app metadata upload so server-side classification has better inputs.

**Tech Stack:** .NET 8 Minimal API, EF Core, PostgreSQL migrations, xUnit, React 19, Vite, TanStack Query, TypeScript, Tailwind CSS, Kotlin Android, Room, Retrofit, GitHub Actions.

---

## Final Acceptance Goal For Goal Mode

Use this exact objective when starting Goal mode:

```text
Implement the mobile records analytics redesign on master: backend analytics APIs and aggregation, app classification overrides/rules/goals, Android metadata strengthening, web analytics workbench UI, tests, commits, push to origin/master, and wait until GitHub Actions Build API, Build Web Client, Build Android APK, and Build Windows Client succeed on master.
```

Goal mode is complete only when every item below is true:

- [ ] `手机记录` defaults to the last 7 Beijing-time days and exposes `今天 / 7天 / 30天 / 自定义`.
- [ ] Overview, heatmap, charts, anomalies, suggestions, goals, quality, and timeline blocks render on the first screen.
- [ ] The old first-500 raw timeline limitation is replaced by cursor-paginated timeline blocks with session and raw event drill-down.
- [ ] App display names and life categories resolve from Android metadata, user global overrides, user rules, built-in rules, and package-name fallback.
- [ ] User app corrections apply globally for that user across devices.
- [ ] Life categories are exactly: `社交沟通`, `短视频/娱乐`, `游戏`, `音乐/音频`, `阅读/资讯`, `学习`, `工作/生产力`, `工具/系统`, `浏览器/搜索`, `出行/地图`, `购物/外卖`, `金融/支付`, `健康/运动`, `相机/创作`, `生活服务`, `未分类`.
- [ ] System launchers, input methods, system UI, quick search, and 0-1 second events are hidden by default and visible with `显示系统与短事件`.
- [ ] Heatmap supports hourly overview and 15/30 minute drill-down.
- [ ] Charts include category share, Top App, daily trend, hour distribution, category trend, switch/pickup trend, comparisons, goals, anomalies, and suggestions.
- [ ] Quality flags explain fallback, missing metadata, stale aggregates, partial sync, timezone boundaries, and hidden noise.
- [ ] Old `/api/v1/mobile/summary` and `/api/v1/mobile/timeline` remain compatible.
- [ ] `dotnet test Pim.sln` passes locally.
- [ ] `npm --prefix src/client-web run build` passes locally.
- [ ] Android build is attempted locally with `cd src/client-android; .\gradlew.bat assembleDebug --no-daemon`; if local Java/Android SDK is missing, the exact blocker is recorded and GitHub Actions Android build is used as final APK build verification.
- [ ] Intentional source changes are committed on `master`.
- [ ] `git push origin master` succeeds.
- [ ] GitHub Actions runs for `Build API`, `Build Web Client`, `Build Android APK`, and `Build Windows Client` on `master`; all required runs finish successfully. If a path-filtered workflow does not trigger, use `workflow_dispatch` for that workflow and wait for success.

## Parallel Subagent Policy

The user requires simultaneous subagent work. Implementation must use parallel subagents whenever work can be isolated. Run at most 14 subagents at the same time（最多 14 个子代理同时工作）. There is no total subagent count limit; launch more waves after earlier agents finish.

Every implementation worker prompt must include:

```text
You are working in a repository shared by multiple agents. Do not revert changes you did not make. Stay inside your assigned write scope. If another agent has changed a file in your scope, inspect and adapt instead of overwriting. Follow TDD: write or update failing tests first, then implement, then run the assigned verification commands. Return changed file paths, commands run, pass/fail output, and blockers.
```

Recommended waves:

- **Wave 1, backend foundations, max 6 workers:** entities/migrations/contracts, classification service, aggregation service, timeline block service, insights/quality service, endpoint tests.
- **Wave 2, clients, max 6 workers:** frontend API/types, frontend analytics components, frontend catalog manager/tests, Android metadata collector, Android sync cleanup, CI helper/review.
- **Wave 3, integration/review, max 4 workers:** spec compliance review, code quality review, backend verification, frontend/Android verification.

When parallel workers edit code, give each worker a disjoint write scope. The coordinator integrates returned patches on `master`, resolves conflicts, runs full verification, commits, pushes, and waits for GitHub Actions.

## Files And Responsibilities

Backend mobile module:

- Create `src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs`: user-global app display/category/noise overrides.
- Create `src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs`: user-global classification rules.
- Create `src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs`: derived hour/day/app/category aggregate projection.
- Create `src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs`: readable timeline block projection.
- Create `src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs`: user-configurable total/category/app limits.
- Modify `src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs`: indexes, defaults, constraints for new entities.
- Create `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`: analytics request/response DTOs.
- Modify `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs`: only for compatibility helpers that old mobile endpoints need.
- Create `src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs`: resolves display/category/noise flags.
- Create `src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs`: catalog override, category rule, and stale-marking mutations.
- Create `src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs`: normalizes filters, Beijing-time windows, shared queries.
- Create `src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs`: computes aggregate buckets.
- Create `src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs`: builds and queries timeline blocks.
- Create `src/modules/Pim.Module.Mobile/Services/MobileUsageInsightService.cs`: comparisons, goals, anomalies, suggestions.
- Create `src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs`: reads and writes user goal/limit settings.
- Modify `src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs`: range quality flags or helper methods reused by analytics.
- Modify `src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs`: mark affected aggregates stale after ingest/session rebuild.
- Modify `src/modules/Pim.Module.Mobile/MobileModule.cs`: register services and map analytics/app catalog endpoints.
- Create migration under `src/Pim.Infrastructure/Data/Migrations/`: new mobile analytics tables.

Backend tests:

- Create or modify `tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileTimelineBlockServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileUsageInsightServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs`.
- Create `tests/Pim.UnitTests/Mobile/MobileAnalyticsEndpointContractTests.cs`.

Android:

- Modify `src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt`: keep the canonical metadata collector.
- Modify `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`: route metadata collection through `AppMetadataCollector` instead of duplicate local metadata logic.
- Modify `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`: keep DTO names aligned if backend DTO shape changes.

Web:

- Modify `src/client-web/src/api/mobile.ts`: analytics paths, DTOs, API functions.
- Modify `src/client-web/src/pages/MobileRecordsPage.tsx`: switch page to analytics queries and shared filters.
- Create `src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx`.
- Create `src/client-web/src/components/mobile/MobileInsightStrip.tsx`.
- Create `src/client-web/src/components/mobile/MobileUsageHeatmap.tsx`.
- Create `src/client-web/src/components/mobile/MobileChartsGrid.tsx`.
- Create `src/client-web/src/components/mobile/MobileTimelineBlocks.tsx`.
- Create `src/client-web/src/components/mobile/MobileAppCatalogManager.tsx`.
- Create `src/client-web/src/components/mobile/MobileAnomalyPanel.tsx`.
- Modify or replace existing `MobileMetricStrip.tsx`, `MobileAppRanking.tsx`, `MobileTimeline.tsx` only when the new components can reuse small formatting helpers.
- Modify `src/client-web/src/components/mobile/mobileFormatting.ts`: Beijing-time formatting and duration helpers.

Web tests:

- Modify `tests/client-web/mobileApiPath.test.ts`.
- Modify `tests/client-web/mobileTypes.test.ts`.
- Modify `tests/client-web/mobileComponents.test.tsx`.
- Add `tests/client-web/mobileAnalyticsComponents.test.tsx`.
- Add `tests/client-web/mobileAnalyticsInteractions.test.tsx`.
- Modify `tests/client-web/tsconfig.mobile.json` if new test files require inclusion.

Verification and CI:

- Use `.github/workflows/build-api.yml`: `Build API`.
- Use `.github/workflows/build-web.yml`: `Build Web Client`.
- Use `.github/workflows/build-android.yml`: `Build Android APK`.
- Use `.github/workflows/build-windows.yml`: `Build Windows Client`.
- Avoid committing generated `bin/`, `obj/`, `dist/`, `build/`, `publish/*`, `.superpowers/brainstorm/`, `src/Pim.Api/wwwroot/`, npm caches, and Android build outputs.

## Shared Contracts

All workers must reuse these names instead of inventing alternatives.

```csharp
public static class MobileAnalyticsDefaults
{
    public const string DefaultTimezone = "Asia/Shanghai";
    public const int DefaultPageSize = 50;
    public const int MaxPageSize = 200;
    public const int DefaultShortEventThresholdSeconds = 1;
}

public static class MobileLifeCategories
{
    public const string Social = "社交沟通";
    public const string ShortVideoEntertainment = "短视频/娱乐";
    public const string Game = "游戏";
    public const string MusicAudio = "音乐/音频";
    public const string ReadingNews = "阅读/资讯";
    public const string Learning = "学习";
    public const string WorkProductivity = "工作/生产力";
    public const string ToolsSystem = "工具/系统";
    public const string BrowserSearch = "浏览器/搜索";
    public const string TravelMaps = "出行/地图";
    public const string ShoppingFood = "购物/外卖";
    public const string FinancePayment = "金融/支付";
    public const string HealthFitness = "健康/运动";
    public const string CameraCreation = "相机/创作";
    public const string LifeServices = "生活服务";
    public const string Uncategorized = "未分类";
}
```

```ts
export const MOBILE_DEFAULT_TIMEZONE = 'Asia/Shanghai';
export const MOBILE_LIFE_CATEGORIES = [
  '社交沟通',
  '短视频/娱乐',
  '游戏',
  '音乐/音频',
  '阅读/资讯',
  '学习',
  '工作/生产力',
  '工具/系统',
  '浏览器/搜索',
  '出行/地图',
  '购物/外卖',
  '金融/支付',
  '健康/运动',
  '相机/创作',
  '生活服务',
  '未分类',
] as const;
```

## Task 0: Plan Commit And Execution Setup

**Files:**
- Create: `docs/superpowers/plans/2026-07-07-mobile-records-analytics.md`

- [ ] **Step 1: Confirm branch state**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git status --short --branch
```

Expected: branch is `master`; if `master` is behind `origin/master`, run `git pull --ff-only` before implementation. Existing dirty files must be listed and preserved.

- [ ] **Step 2: Commit this plan**

Run:

```powershell
git add docs/superpowers/plans/2026-07-07-mobile-records-analytics.md
git diff --cached --check
git commit -m "docs: plan mobile records analytics"
```

Expected: commit succeeds with only the plan file.

## Task 1: Backend Entity And DTO Contracts

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileUsageAggregateEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileTimelineBlockEntity.cs`
- Create: `src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs`
- Modify: `src/modules/Pim.Module.Mobile/Entities/MobileEntityConfigurations.cs`
- Create: `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileAnalyticsContractTests.cs`

- [ ] **Step 1: Write failing DTO and entity contract tests**

Create `tests/Pim.UnitTests/Mobile/MobileAnalyticsContractTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileAnalyticsContractTests
{
    [Fact]
    public void LifeCategories_ExposeConfirmedChineseCategorySet()
    {
        Assert.Equal(new[]
        {
            "社交沟通",
            "短视频/娱乐",
            "游戏",
            "音乐/音频",
            "阅读/资讯",
            "学习",
            "工作/生产力",
            "工具/系统",
            "浏览器/搜索",
            "出行/地图",
            "购物/外卖",
            "金融/支付",
            "健康/运动",
            "相机/创作",
            "生活服务",
            "未分类"
        }, MobileLifeCategories.All);
    }

    [Fact]
    public void AnalyticsDefaults_UseBeijingTimeAndStablePaging()
    {
        Assert.Equal("Asia/Shanghai", MobileAnalyticsDefaults.DefaultTimezone);
        Assert.Equal(50, MobileAnalyticsDefaults.DefaultPageSize);
        Assert.Equal(200, MobileAnalyticsDefaults.MaxPageSize);
        Assert.Equal(1, MobileAnalyticsDefaults.DefaultShortEventThresholdSeconds);
    }

    [Fact]
    public void OverrideEntity_IsUserGlobalByPackageName()
    {
        var entity = new MobileAppCatalogOverrideEntity
        {
            UserId = MobileTestHelpers.UserId,
            PackageName = "com.tencent.mobileqq",
            DisplayNameOverride = "QQ",
            LifeCategory = MobileLifeCategories.Social,
            IsSystemNoise = false,
            HideShortEvents = false
        };

        Assert.Equal("com.tencent.mobileqq", entity.PackageName);
        Assert.Equal("QQ", entity.DisplayNameOverride);
        Assert.Equal("社交沟通", entity.LifeCategory);
    }

    [Fact]
    public void OverviewResponse_CarriesQualityGoalAnomalyAndStaleState()
    {
        var response = new MobileAnalyticsOverviewResponse(
            new MobileAnalyticsRangeDto(
                DateTimeOffset.Parse("2026-07-01T16:00:00Z"),
                DateTimeOffset.Parse("2026-07-08T16:00:00Z"),
                "Asia/Shanghai",
                "2026-07-02",
                "2026-07-08"),
            DateTimeOffset.Parse("2026-07-08T10:00:00Z"),
            false,
            3600,
            600,
            0.25,
            "2026-07-08",
            21,
            12,
            42,
            0.94,
            new MobileAnalyticsQualitySummaryDto(0.92, 0.08, 1, 0.03, 0.02, 0, DateTimeOffset.Parse("2026-07-08T09:59:00Z"), Array.Empty<string>()),
            new MobileGoalProgressDto("total-daily", "每日手机总时长", 14400, 3600, false, 10800),
            new[] { new MobileAnomalyDto("night-use", "Warning", "夜间使用偏高", "22:00 后使用增加", "heatmap:night") },
            new[] { new MobileSuggestionDto("short-video-night", "短视频/娱乐集中在 22:00 后", "category:短视频/娱乐") });

        Assert.Equal("Asia/Shanghai", response.Range.Timezone);
        Assert.False(response.IsStale);
        Assert.Single(response.Anomalies);
        Assert.Single(response.Suggestions);
    }
}
```

- [ ] **Step 2: Run the failing backend contract test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileAnalyticsContractTests
```

Expected before implementation: compile fails because `MobileLifeCategories`, new entities, and analytics DTOs do not exist.

- [ ] **Step 3: Add constants, entities, and DTOs**

Add entity files and create `MobileAnalyticsDtos.cs` with these names and shapes:

```csharp
public static class MobileAnalyticsDefaults
{
    public const string DefaultTimezone = "Asia/Shanghai";
    public const int DefaultPageSize = 50;
    public const int MaxPageSize = 200;
    public const int DefaultShortEventThresholdSeconds = 1;
}

public static class MobileLifeCategories
{
    public const string Social = "社交沟通";
    public const string ShortVideoEntertainment = "短视频/娱乐";
    public const string Game = "游戏";
    public const string MusicAudio = "音乐/音频";
    public const string ReadingNews = "阅读/资讯";
    public const string Learning = "学习";
    public const string WorkProductivity = "工作/生产力";
    public const string ToolsSystem = "工具/系统";
    public const string BrowserSearch = "浏览器/搜索";
    public const string TravelMaps = "出行/地图";
    public const string ShoppingFood = "购物/外卖";
    public const string FinancePayment = "金融/支付";
    public const string HealthFitness = "健康/运动";
    public const string CameraCreation = "相机/创作";
    public const string LifeServices = "生活服务";
    public const string Uncategorized = "未分类";

    public static readonly string[] All =
    {
        Social,
        ShortVideoEntertainment,
        Game,
        MusicAudio,
        ReadingNews,
        Learning,
        WorkProductivity,
        ToolsSystem,
        BrowserSearch,
        TravelMaps,
        ShoppingFood,
        FinancePayment,
        HealthFitness,
        CameraCreation,
        LifeServices,
        Uncategorized
    };
}

public sealed record MobileAnalyticsRangeDto(
    DateTimeOffset RangeStartUtc,
    DateTimeOffset RangeEndUtc,
    string Timezone,
    string LocalStartDate,
    string LocalEndDate);

public sealed record MobileAnalyticsQualitySummaryDto(
    double UsageEventsCoverage,
    double FallbackShare,
    int MissingMetadataAppCount,
    double SystemNoiseShare,
    double ShortEventShare,
    int FailedOrPartialSyncBatchCount,
    DateTimeOffset? LastSyncAt,
    IReadOnlyList<string> QualityFlags);

public sealed record MobileGoalProgressDto(
    string Key,
    string Label,
    long LimitSeconds,
    long UsedSeconds,
    bool IsOverLimit,
    long RemainingSeconds);

public sealed record MobileAnomalyDto(
    string Code,
    string Severity,
    string Title,
    string Evidence,
    string DrilldownTarget);

public sealed record MobileSuggestionDto(
    string Code,
    string Text,
    string DrilldownTarget);

public sealed record MobileAnalyticsOverviewResponse(
    MobileAnalyticsRangeDto Range,
    DateTimeOffset GeneratedAt,
    bool IsStale,
    long TotalForegroundSeconds,
    long DailyAverageSeconds,
    double PreviousPeriodChange,
    string? HighestUseLocalDate,
    int? PeakLocalHour,
    int AppCount,
    int SwitchOrPickupCount,
    double Completeness,
    MobileAnalyticsQualitySummaryDto Quality,
    MobileGoalProgressDto? GoalProgress,
    IReadOnlyList<MobileAnomalyDto> Anomalies,
    IReadOnlyList<MobileSuggestionDto> Suggestions);

public sealed record MobileUsageGoalDto(
    Guid Id,
    string Scope,
    string? PackageName,
    string? LifeCategory,
    string Label,
    long LimitSeconds,
    bool IsEnabled,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);
```

Use standard `System.ComponentModel.DataAnnotations` and `System.ComponentModel.DataAnnotations.Schema` entity attributes, matching existing mobile entities.

- [ ] **Step 4: Configure EF indexes and defaults**

In `MobileEntityConfigurations.cs`, add configurations:

```csharp
public sealed class MobileAppCatalogOverrideEntityConfiguration : IEntityTypeConfiguration<MobileAppCatalogOverrideEntity>
{
    public void Configure(EntityTypeBuilder<MobileAppCatalogOverrideEntity> builder)
    {
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.UpdatedAt).HasDefaultValueSql("now()");
        builder.HasIndex(e => new { e.UserId, e.PackageName }).IsUnique();
        builder.HasIndex(e => new { e.UserId, e.LifeCategory });
    }
}

public sealed class MobileAppCategoryRuleEntityConfiguration : IEntityTypeConfiguration<MobileAppCategoryRuleEntity>
{
    public void Configure(EntityTypeBuilder<MobileAppCategoryRuleEntity> builder)
    {
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("now()");
        builder.Property(e => e.UpdatedAt).HasDefaultValueSql("now()");
        builder.HasIndex(e => new { e.UserId, e.IsEnabled, e.Priority });
        builder.HasIndex(e => new { e.UserId, e.RuleType, e.Pattern });
    }
}
```

Also configure aggregate and timeline block indexes by user, timezone, window start, device, category, and package. Configure `MobileUsageGoalEntity` with indexes on `{ UserId, IsEnabled }`, `{ UserId, Scope, PackageName }`, and `{ UserId, Scope, LifeCategory }`.

- [ ] **Step 5: Run contract test and full mobile unit tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Mobile"
```

Expected: mobile tests pass.

- [ ] **Step 6: Commit backend contracts**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/Entities src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs tests/Pim.UnitTests/Mobile/MobileAnalyticsContractTests.cs
git commit -m "feat: add mobile analytics contracts"
```

## Task 2: Backend Classification Service

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs`
- Modify: `src/modules/Pim.Module.Mobile/MobileModule.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs`

- [ ] **Step 1: Write failing classification tests**

Create `MobileAppClassificationServiceTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileAppClassificationServiceTests
{
    [Fact]
    public async Task ResolveAsync_UserOverrideWinsAcrossDevices()
    {
        await using var db = MobileTestHelpers.CreateDb();
        db.Set<MobileAppCatalogOverrideEntity>().Add(new MobileAppCatalogOverrideEntity
        {
            UserId = MobileTestHelpers.UserId,
            PackageName = "com.tencent.mobileqq",
            DisplayNameOverride = "QQ",
            LifeCategory = MobileLifeCategories.Social,
            IsSystemNoise = false,
            HideShortEvents = false
        });
        db.Set<MobileAppCatalogEntity>().Add(new MobileAppCatalogEntity
        {
            UserId = MobileTestHelpers.UserId,
            DeviceId = "phone-a",
            PackageName = "com.tencent.mobileqq",
            DisplayName = "Mobile QQ",
            Category = "SOCIAL",
            RawJson = "{}"
        });
        await db.SaveChangesAsync();

        var service = new MobileAppClassificationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));

        var resolved = await service.ResolveAsync("phone-b", new[] { "com.tencent.mobileqq" }, CancellationToken.None);

        Assert.Equal("QQ", resolved["com.tencent.mobileqq"].DisplayName);
        Assert.Equal("社交沟通", resolved["com.tencent.mobileqq"].LifeCategory);
    }

    [Fact]
    public async Task ResolveAsync_BuiltInRulesClassifyLauncherAsSystemNoise()
    {
        await using var db = MobileTestHelpers.CreateDb();
        var service = new MobileAppClassificationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));

        var resolved = await service.ResolveAsync("phone-a", new[] { "com.android.launcher" }, CancellationToken.None);

        Assert.Equal("工具/系统", resolved["com.android.launcher"].LifeCategory);
        Assert.True(resolved["com.android.launcher"].IsSystemNoise);
        Assert.True(resolved["com.android.launcher"].HideShortEvents);
    }
}
```

- [ ] **Step 2: Run failing tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileAppClassificationServiceTests
```

Expected before implementation: compile fails because `MobileAppClassificationService` is missing.

- [ ] **Step 3: Implement classification service**

Create a service that exposes:

```csharp
public sealed record ResolvedMobileApp(
    string PackageName,
    string DisplayName,
    string LifeCategory,
    string? AndroidCategory,
    bool IsSystemApp,
    bool IsSystemNoise,
    bool HideShortEvents,
    IReadOnlyList<string> QualityFlags);

public sealed class MobileAppClassificationService
{
    public Task<IReadOnlyDictionary<string, ResolvedMobileApp>> ResolveAsync(
        string? deviceId,
        IReadOnlyCollection<string> packageNames,
        CancellationToken ct = default)
    {
        // Load user overrides, user rules, and latest app catalog rows.
        // Apply precedence from the design spec and return one result per package.
    }
}
```

Implement built-in rules for at least:

```csharp
private static readonly string[] SystemNoisePrefixes =
{
    "com.android.",
    "com.google.android.inputmethod",
    "com.heytap.quicksearchbox"
};

private static readonly Dictionary<string, string> BuiltInExactCategories = new(StringComparer.Ordinal)
{
    ["com.tencent.mobileqq"] = MobileLifeCategories.Social,
    ["com.tencent.mm"] = MobileLifeCategories.Social,
    ["com.android.launcher"] = MobileLifeCategories.ToolsSystem
};
```

- [ ] **Step 4: Register service**

In `MobileModule.RegisterServices`, add:

```csharp
services.AddScoped<MobileAppClassificationService>();
services.AddScoped<MobileAppCatalogOverrideService>();
```

- [ ] **Step 5: Run classification tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileAppClassificationServiceTests
```

Expected: tests pass.

- [ ] **Step 6: Commit classification**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs src/modules/Pim.Module.Mobile/MobileModule.cs tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs
git commit -m "feat: classify mobile apps"
```

## Task 3: Backend Analytics Query And Aggregation

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs`

- [ ] **Step 1: Write failing Beijing-time query tests**

Create `MobileAnalyticsQueryServiceTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileAnalyticsQueryServiceTests
{
    [Fact]
    public void BuildRange_DefaultLastSevenDaysUsesBeijingCalendar()
    {
        var now = DateTimeOffset.Parse("2026-07-07T13:30:00Z");
        var range = MobileAnalyticsQueryService.BuildRange(null, null, "Asia/Shanghai", now, "7d");

        Assert.Equal("Asia/Shanghai", range.Timezone);
        Assert.Equal("2026-07-01", range.LocalStartDate);
        Assert.Equal("2026-07-07", range.LocalEndDate);
        Assert.Equal(DateTimeOffset.Parse("2026-06-30T16:00:00Z"), range.RangeStartUtc);
        Assert.Equal(DateTimeOffset.Parse("2026-07-07T16:00:00Z"), range.RangeEndUtc);
    }

    [Fact]
    public void NormalizePageSize_ClampsToConfiguredMaximum()
    {
        Assert.Equal(50, MobileAnalyticsQueryService.NormalizePageSize(null));
        Assert.Equal(200, MobileAnalyticsQueryService.NormalizePageSize(500));
        Assert.Equal(1, MobileAnalyticsQueryService.NormalizePageSize(0));
    }
}
```

- [ ] **Step 2: Write failing aggregation tests**

Create `MobileUsageAggregationServiceTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileUsageAggregationServiceTests
{
    [Fact]
    public async Task BuildHourlyBucketsAsync_GroupsSessionsByBeijingHourAndCategory()
    {
        await using var db = MobileTestHelpers.CreateDb();
        db.Set<MobileUsageSessionEntity>().Add(new MobileUsageSessionEntity
        {
            UserId = MobileTestHelpers.UserId,
            DeviceId = "phone-a",
            PackageName = "com.tencent.mobileqq",
            StartUtc = DateTimeOffset.Parse("2026-07-06T13:05:00Z"),
            EndUtc = DateTimeOffset.Parse("2026-07-06T13:35:00Z"),
            DurationMs = 30 * 60 * 1000
        });
        await db.SaveChangesAsync();

        var classifier = new MobileAppClassificationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));
        var service = new MobileUsageAggregationService(db, MobileTestHelpers.CurrentUser(), classifier, MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));

        var buckets = await service.GetHeatmapAsync(
            new MobileAnalyticsFilter(
                DateTimeOffset.Parse("2026-07-06T00:00:00Z"),
                DateTimeOffset.Parse("2026-07-07T00:00:00Z"),
                "Asia/Shanghai",
                null,
                null,
                null,
                null,
                false,
                1,
                "hour"),
            CancellationToken.None);

        var bucket = Assert.Single(buckets);
        Assert.Equal(21, bucket.LocalHour);
        Assert.Equal("社交沟通", bucket.LifeCategory);
        Assert.Equal(1800, bucket.ForegroundSeconds);
    }
}
```

- [ ] **Step 3: Run failing tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "MobileAnalyticsQueryServiceTests|MobileUsageAggregationServiceTests"
```

Expected before implementation: compile fails for missing services and DTOs.

- [ ] **Step 4: Implement filter, range, and aggregation DTOs**

Add these DTOs to `MobileAnalyticsDtos.cs`:

```csharp
public sealed record MobileAnalyticsFilter(
    DateTimeOffset RangeStartUtc,
    DateTimeOffset RangeEndUtc,
    string Timezone,
    string? DeviceId,
    string? Category,
    string? PackageName,
    string? Source,
    bool IncludeSystemNoise,
    int MinDurationSeconds,
    string Granularity);

public sealed record MobileHeatmapBucketDto(
    DateTimeOffset BucketStartUtc,
    DateTimeOffset BucketEndUtc,
    string LocalDate,
    int LocalHour,
    string LifeCategory,
    long ForegroundSeconds,
    IReadOnlyList<string> QualityFlags);
```

- [ ] **Step 5: Implement range and page helpers**

`MobileAnalyticsQueryService` must include deterministic static methods:

```csharp
public static MobileAnalyticsRangeDto BuildRange(
    DateTimeOffset? rangeStartUtc,
    DateTimeOffset? rangeEndUtc,
    string? timezone,
    DateTimeOffset nowUtc,
    string shortcut = "7d")
{
    var zone = TimeZoneInfo.FindSystemTimeZoneById(string.IsNullOrWhiteSpace(timezone)
        ? MobileAnalyticsDefaults.DefaultTimezone
        : timezone);
    var localNow = TimeZoneInfo.ConvertTime(nowUtc, zone);
    var localEndDate = DateOnly.FromDateTime(localNow.DateTime);
    var localStartDate = shortcut switch
    {
        "today" => localEndDate,
        "30d" => localEndDate.AddDays(-29),
        _ => localEndDate.AddDays(-6)
    };

    var start = rangeStartUtc ?? LocalDateStartUtc(localStartDate, zone);
    var end = rangeEndUtc ?? LocalDateStartUtc(localEndDate.AddDays(1), zone);
    return new MobileAnalyticsRangeDto(start, end, zone.Id, localStartDate.ToString("yyyy-MM-dd"), localEndDate.ToString("yyyy-MM-dd"));
}

public static int NormalizePageSize(int? pageSize)
    => Math.Clamp(pageSize ?? MobileAnalyticsDefaults.DefaultPageSize, 1, MobileAnalyticsDefaults.MaxPageSize);
```

- [ ] **Step 6: Implement heatmap aggregation**

`MobileUsageAggregationService.GetHeatmapAsync` should query sessions in range, classify package names with `MobileAppClassificationService`, hide system/short sessions when requested, group by local hour and category, and return `MobileHeatmapBucketDto`.

- [ ] **Step 7: Mark aggregates stale after ingest**

In `MobileUsageIngestService.IngestAsync`, after `RebuildSessionsAsync(...)`, call a new aggregation stale marker:

```csharp
await _usageAggregation.MarkStaleAsync(
    userId,
    request.DeviceId,
    request.WindowStartUtc,
    request.WindowEndUtc,
    ct);
```

Inject `MobileUsageAggregationService` into `MobileUsageIngestService`. The first implementation of `MarkStaleAsync` marks matching aggregate rows `IsStale = true`; if no aggregate rows exist yet it returns without error.

- [ ] **Step 8: Run aggregation tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "MobileAnalyticsQueryServiceTests|MobileUsageAggregationServiceTests"
```

Expected: tests pass.

- [ ] **Step 9: Commit query and aggregation**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs
git commit -m "feat: aggregate mobile analytics"
```

## Task 4: Backend Timeline Blocks

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileTimelineBlockServiceTests.cs`

- [ ] **Step 1: Write failing timeline block tests**

Create `MobileTimelineBlockServiceTests.cs`:

```csharp
using Pim.Module.Mobile.DTOs;
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileTimelineBlockServiceTests
{
    [Fact]
    public async Task GetBlocksAsync_ReturnsCategoryBlocksInsteadOfRawSessionWall()
    {
        await using var db = MobileTestHelpers.CreateDb();
        db.Set<MobileUsageSessionEntity>().AddRange(
            Session("com.tencent.mobileqq", "2026-07-06T12:00:00Z", "2026-07-06T12:10:00Z"),
            Session("com.tencent.mm", "2026-07-06T12:12:00Z", "2026-07-06T12:20:00Z"));
        await db.SaveChangesAsync();

        var classifier = new MobileAppClassificationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));
        var service = new MobileTimelineBlockService(db, MobileTestHelpers.CurrentUser(), classifier, MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));

        var page = await service.GetBlocksAsync(new MobileTimelineBlockQuery(
            DateTimeOffset.Parse("2026-07-06T00:00:00Z"),
            DateTimeOffset.Parse("2026-07-07T00:00:00Z"),
            "Asia/Shanghai",
            null,
            null,
            null,
            null,
            false,
            1,
            null,
            50), CancellationToken.None);

        var block = Assert.Single(page.Items);
        Assert.Equal("社交沟通", block.LifeCategory);
        Assert.Equal(1080, block.ForegroundSeconds);
        Assert.Equal(2, block.SessionCount);
    }

    private static MobileUsageSessionEntity Session(string packageName, string start, string end)
        => new()
        {
            UserId = MobileTestHelpers.UserId,
            DeviceId = "phone-a",
            PackageName = packageName,
            StartUtc = DateTimeOffset.Parse(start),
            EndUtc = DateTimeOffset.Parse(end),
            DurationMs = Convert.ToInt64((DateTimeOffset.Parse(end) - DateTimeOffset.Parse(start)).TotalMilliseconds)
        };
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileTimelineBlockServiceTests
```

Expected before implementation: compile fails for missing query/service/DTOs.

- [ ] **Step 3: Add timeline DTOs**

Add to `MobileAnalyticsDtos.cs`:

```csharp
public sealed record MobileTimelineBlockQuery(
    DateTimeOffset RangeStartUtc,
    DateTimeOffset RangeEndUtc,
    string Timezone,
    string? DeviceId,
    string? Category,
    string? PackageName,
    string? Source,
    bool IncludeSystemNoise,
    int MinDurationSeconds,
    string? Cursor,
    int PageSize);

public sealed record MobileTimelineBlockDto(
    string Id,
    DateTimeOffset StartUtc,
    DateTimeOffset EndUtc,
    string LocalStart,
    string LocalEnd,
    string LifeCategory,
    long ForegroundSeconds,
    int SessionCount,
    int AppCount,
    IReadOnlyList<MobileTimelineBlockAppDto> TopApps,
    IReadOnlyList<string> QualityFlags);

public sealed record MobileTimelineBlockAppDto(
    string PackageName,
    string DisplayName,
    long ForegroundSeconds);

public sealed record MobileTimelineBlockPageDto(
    IReadOnlyList<MobileTimelineBlockDto> Items,
    string? NextCursor,
    bool HasMore);
```

- [ ] **Step 4: Implement block grouping and pagination**

Group adjacent sessions with the same dominant life category when the gap is 15 minutes or less. Sort descending by start UTC for page responses. Generate stable block ids with package/category/start/end content, for example SHA-256 shortened to 24 chars.

- [ ] **Step 5: Run timeline tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileTimelineBlockServiceTests
```

Expected: tests pass.

- [ ] **Step 6: Commit timeline blocks**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs tests/Pim.UnitTests/Mobile/MobileTimelineBlockServiceTests.cs
git commit -m "feat: add mobile timeline blocks"
```

## Task 5: Backend Insights, Quality, And Goals

**Files:**
- Create: `src/modules/Pim.Module.Mobile/Services/MobileUsageInsightService.cs`
- Create: `src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileUsageInsightServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs`

- [ ] **Step 1: Write failing insight tests**

Create `MobileUsageInsightServiceTests.cs`:

```csharp
using Pim.Module.Mobile.Entities;
using Pim.Module.Mobile.Services;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileUsageInsightServiceTests
{
    [Fact]
    public async Task GetOverviewAsync_ReportsPreviousPeriodChangeGoalAndNightAnomaly()
    {
        await using var db = MobileTestHelpers.CreateDb();
        db.Set<MobileUsageSessionEntity>().AddRange(
            Session("com.video.app", "2026-07-06T14:00:00Z", "2026-07-06T15:30:00Z"),
            Session("com.video.app", "2026-06-29T14:00:00Z", "2026-06-29T14:30:00Z"));
        await db.SaveChangesAsync();

        var classifier = new MobileAppClassificationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));
        var aggregation = new MobileUsageAggregationService(db, MobileTestHelpers.CurrentUser(), classifier, MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));
        var service = new MobileUsageInsightService(db, MobileTestHelpers.CurrentUser(), aggregation, classifier, MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-07T00:00:00Z")));

        var overview = await service.GetOverviewAsync(DateTimeOffset.Parse("2026-07-06T00:00:00Z"), DateTimeOffset.Parse("2026-07-07T00:00:00Z"), "Asia/Shanghai", CancellationToken.None);

        Assert.True(overview.TotalForegroundSeconds > overview.DailyAverageSeconds / 2);
        Assert.NotNull(overview.GoalProgress);
        Assert.Contains(overview.Anomalies, anomaly => anomaly.Code == "night-use");
    }

    private static MobileUsageSessionEntity Session(string packageName, string start, string end)
        => new()
        {
            UserId = MobileTestHelpers.UserId,
            DeviceId = "phone-a",
            PackageName = packageName,
            StartUtc = DateTimeOffset.Parse(start),
            EndUtc = DateTimeOffset.Parse(end),
            DurationMs = Convert.ToInt64((DateTimeOffset.Parse(end) - DateTimeOffset.Parse(start)).TotalMilliseconds)
        };
}
```

- [ ] **Step 2: Run failing insight test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileUsageInsightServiceTests
```

Expected before implementation: compile fails for missing service.

- [ ] **Step 3: Implement deterministic insight rules**

Implement:

```csharp
public Task<MobileAnalyticsOverviewResponse> GetOverviewAsync(
    DateTimeOffset rangeStartUtc,
    DateTimeOffset rangeEndUtc,
    string timezone,
    CancellationToken ct = default)
```

Use these initial constants:

```csharp
private const long DefaultDailyGoalSeconds = 4 * 60 * 60;
private const double SharpChangeThreshold = 0.35;
private const int NightStartHour = 22;
private const long LongSessionThresholdSeconds = 90 * 60;
```

Return evidence-based anomalies with stable codes: `duration-change`, `category-spike`, `night-use`, `long-session`, `high-switching`, `new-top-app`, `data-gap`.

- [ ] **Step 4: Implement persistent goal settings**

Create `MobileUsageGoalService` with:

```csharp
public Task<IReadOnlyList<MobileUsageGoalDto>> ListAsync(CancellationToken ct = default);
public Task<MobileUsageGoalDto> UpsertAsync(MobileUsageGoalUpsertRequest request, CancellationToken ct = default);
```

Add `MobileUsageGoalUpsertRequest` to `MobileAnalyticsDtos.cs`:

```csharp
public sealed record MobileUsageGoalUpsertRequest(
    string Scope,
    string? PackageName,
    string? LifeCategory,
    string Label,
    long LimitSeconds,
    bool IsEnabled);
```

`MobileUsageInsightService` uses the enabled `total-daily` goal when present. If none exists, it uses the default 4-hour total daily goal so the overview always has goal progress.

- [ ] **Step 5: Run insight tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileUsageInsightServiceTests
```

Expected: tests pass.

- [ ] **Step 6: Commit insights and goals**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs src/modules/Pim.Module.Mobile/Services/MobileUsageInsightService.cs src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs tests/Pim.UnitTests/Mobile/MobileUsageInsightServiceTests.cs tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs
git commit -m "feat: add mobile usage insights"
```

## Task 6: Backend Analytics Endpoints And App Rule Endpoints

**Files:**
- Modify: `src/modules/Pim.Module.Mobile/MobileModule.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileAnalyticsEndpointContractTests.cs`

- [ ] **Step 1: Write endpoint path contract tests**

Create `MobileAnalyticsEndpointContractTests.cs`:

```csharp
using Pim.Module.Mobile;
using Xunit;

namespace Pim.UnitTests.Mobile;

public sealed class MobileAnalyticsEndpointContractTests
{
    [Fact]
    public void EndpointPaths_ExposeAnalyticsAndCatalogRoutes()
    {
        Assert.Equal("/api/v1/mobile/analytics/overview", MobileEndpointPaths.AnalyticsOverview);
        Assert.Equal("/api/v1/mobile/analytics/heatmap", MobileEndpointPaths.AnalyticsHeatmap);
        Assert.Equal("/api/v1/mobile/analytics/charts", MobileEndpointPaths.AnalyticsCharts);
        Assert.Equal("/api/v1/mobile/analytics/timeline-blocks", MobileEndpointPaths.AnalyticsTimelineBlocks);
        Assert.Equal("/api/v1/mobile/analytics/goals", MobileEndpointPaths.AnalyticsGoals);
        Assert.Equal("/api/v1/mobile/apps/catalog-overrides", MobileEndpointPaths.AppCatalogOverrides);
        Assert.Equal("/api/v1/mobile/apps/category-rules", MobileEndpointPaths.AppCategoryRules);
    }
}
```

- [ ] **Step 2: Run failing endpoint test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter MobileAnalyticsEndpointContractTests
```

Expected before implementation: compile fails because path constants are missing.

- [ ] **Step 3: Register analytics services**

In `MobileModule.RegisterServices`:

```csharp
services.AddScoped<MobileAnalyticsQueryService>();
services.AddScoped<MobileUsageAggregationService>();
services.AddScoped<MobileTimelineBlockService>();
services.AddScoped<MobileUsageInsightService>();
services.AddScoped<MobileUsageGoalService>();
```

- [ ] **Step 4: Map analytics endpoints**

In `MobileModule.MapEndpoints`, add:

```csharp
group.MapGet("/analytics/overview", async (
    [FromQuery] DateTimeOffset? rangeStartUtc,
    [FromQuery] DateTimeOffset? rangeEndUtc,
    [FromQuery] string? timezone,
    [FromServices] MobileUsageInsightService service,
    CancellationToken ct) =>
{
    var effective = MobileAnalyticsQueryService.BuildRange(rangeStartUtc, rangeEndUtc, timezone, TimeProvider.System.GetUtcNow());
    return Results.Ok(ApiResponse<MobileAnalyticsOverviewResponse>.Ok(await service.GetOverviewAsync(
        effective.RangeStartUtc,
        effective.RangeEndUtc,
        effective.Timezone,
        ct)));
});

group.MapGet("/analytics/heatmap", async (
    [AsParameters] MobileAnalyticsEndpointQuery query,
    [FromServices] MobileUsageAggregationService service,
    CancellationToken ct) =>
{
    var filter = MobileAnalyticsQueryService.BuildFilter(query);
    return Results.Ok(ApiResponse<IReadOnlyList<MobileHeatmapBucketDto>>.Ok(await service.GetHeatmapAsync(filter, ct)));
});
```

Add matching endpoint methods for charts, timeline blocks, block sessions, session events, goals, catalog overrides, and category rules. Keep response envelope `ApiResponse<T>.Ok(...)`.

- [ ] **Step 5: Add endpoint path constants**

Add:

```csharp
public const string AnalyticsOverview = $"{Root}/analytics/overview";
public const string AnalyticsHeatmap = $"{Root}/analytics/heatmap";
public const string AnalyticsCharts = $"{Root}/analytics/charts";
public const string AnalyticsTimelineBlocks = $"{Root}/analytics/timeline-blocks";
public const string AnalyticsGoals = $"{Root}/analytics/goals";
public const string AppCatalogOverrides = $"{Root}/apps/catalog-overrides";
public const string AppCategoryRules = $"{Root}/apps/category-rules";
```

- [ ] **Step 6: Run endpoint tests and mobile tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "MobileAnalyticsEndpointContractTests|FullyQualifiedName~Mobile"
```

Expected: tests pass.

- [ ] **Step 7: Commit endpoints**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/MobileModule.cs src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs tests/Pim.UnitTests/Mobile/MobileAnalyticsEndpointContractTests.cs
git commit -m "feat: expose mobile analytics endpoints"
```

## Task 7: EF Migration And Backfill Hook

**Files:**
- Create: `src/Pim.Infrastructure/Data/Migrations/20260707120000_AddMobileAnalytics.cs`
- Create: `src/Pim.Infrastructure/Data/Migrations/20260707120000_AddMobileAnalytics.Designer.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`
- Test: existing `dotnet test Pim.sln`

- [ ] **Step 1: Ensure module registration before migration**

Run:

```powershell
dotnet build src/Pim.Api/Pim.Api.csproj
```

Expected: build succeeds before generating migration.

- [ ] **Step 2: Generate migration**

Run the repository's existing EF migration pattern. If `dotnet ef` is available:

```powershell
dotnet ef migrations add AddMobileAnalytics --project src/Pim.Infrastructure --startup-project src/Pim.Api --context PimDbContext
```

Expected: migration files are created under `src/Pim.Infrastructure/Data/Migrations`.
If EF generates a different timestamp, rename the two generated files and migration id to `20260707120000_AddMobileAnalytics` so the plan uses stable file names. Update the designer metadata and snapshot references to the same id.

- [ ] **Step 3: Inspect migration**

Confirm migration creates tables for:

```text
mobile_app_catalog_overrides
mobile_app_category_rules
mobile_usage_aggregates
mobile_timeline_blocks
mobile_usage_goals
```

Confirm indexes include:

```text
(user_id, package_name) unique on mobile_app_catalog_overrides
(user_id, is_enabled, priority) on mobile_app_category_rules
(user_id, timezone, window_start_utc) on mobile_usage_aggregates
(user_id, device_id, start_utc) on mobile_timeline_blocks
(user_id, is_enabled) on mobile_usage_goals
```

- [ ] **Step 4: Run backend tests**

Run:

```powershell
dotnet test Pim.sln
dotnet build src\Pim.Api\Pim.Api.csproj
```

Expected: all tests pass and API builds.

- [ ] **Step 5: Commit migration**

Run:

```powershell
git add src/Pim.Infrastructure/Data/Migrations
git commit -m "feat: add mobile analytics schema"
```

## Task 8: Android Metadata Strengthening

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt` only if DTO names change

- [ ] **Step 1: Inspect duplicate metadata paths**

Run:

```powershell
rg -n "collectAppMetadata|AppMetadataCollector|MobileAppMetadataDto|installerPackageName|loadLabel" src/client-android
```

Expected: `AppMetadataCollector.kt` and the duplicate local `collectAppMetadata` inside `MobileSyncCoordinator.kt` are visible.

- [ ] **Step 2: Route sync through `AppMetadataCollector`**

Inject `AppMetadataCollector` into `MobileSyncCoordinator`:

```kotlin
class MobileSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val tokenManager: TokenManager,
    private val usageAccessChecker: UsageAccessChecker,
    private val usageEventCollector: UsageEventCollector,
    private val appMetadataCollector: AppMetadataCollector,
    private val database: AppDatabase,
    private val logs: StructuredLogRepository,
    private val heartbeatReporter: MobileHeartbeatReporter,
    private val serverSettingsStore: ServerSettingsStore
)
```

Replace local collection:

```kotlin
val appMetadata = appMetadataCollector.collectForPackages(packageNames)
```

Delete the duplicate private `collectAppMetadata`, `appMetadataJson`, and installer/category helpers from `MobileSyncCoordinator` when no longer used.

- [ ] **Step 3: Ensure raw metadata includes label and installer**

In `AppMetadataCollector.kt`, keep raw JSON fields:

```kotlin
JSONObject()
    .put("packageName", packageName)
    .put("label", label)
    .put("versionName", packageInfo.versionName ?: JSONObject.NULL)
    .put("versionCode", versionCode)
    .put("firstInstallTimeUtc", packageInfo.firstInstallTime)
    .put("lastUpdateTimeUtc", packageInfo.lastUpdateTime)
    .put("isSystemApp", isSystemApp)
    .put("category", category ?: JSONObject.NULL)
    .put("installerPackageName", installerPackageName ?: JSONObject.NULL)
    .put("collectedAtUtc", collectedAtUtc)
    .toString()
```

- [ ] **Step 4: Build Android debug APK locally**

Run:

```powershell
cd src\client-android
.\gradlew.bat assembleDebug --no-daemon
cd ..\..
```

Expected: build succeeds. If local Java/Android SDK is missing, record the exact command output and rely on GitHub Actions `Build Android APK` after push.

- [ ] **Step 5: Commit Android metadata**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt
git commit -m "fix: strengthen android app metadata upload"
```

## Task 9: Web API Types And Query Functions

**Files:**
- Modify: `src/client-web/src/api/mobile.ts`
- Modify: `tests/client-web/mobileApiPath.test.ts`
- Modify: `tests/client-web/mobileTypes.test.ts`

- [ ] **Step 1: Write failing API path tests**

Extend `tests/client-web/mobileApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { mobileApiPaths } from '../../src/client-web/src/api/mobile';

assert.equal(
  mobileApiPaths.analyticsOverview({ rangeStartUtc: '2026-07-01T16:00:00Z', rangeEndUtc: '2026-07-08T16:00:00Z', timezone: 'Asia/Shanghai' }),
  '/mobile/analytics/overview?rangeStartUtc=2026-07-01T16%3A00%3A00Z&rangeEndUtc=2026-07-08T16%3A00%3A00Z&timezone=Asia%2FShanghai',
);
assert.equal(
  mobileApiPaths.analyticsTimelineBlocks({ timezone: 'Asia/Shanghai', includeSystemNoise: false, pageSize: 50 }),
  '/mobile/analytics/timeline-blocks?timezone=Asia%2FShanghai&includeSystemNoise=false&pageSize=50',
);
assert.equal(mobileApiPaths.appCatalogOverrides(), '/mobile/apps/catalog-overrides');
assert.equal(mobileApiPaths.appCategoryRules(), '/mobile/apps/category-rules');
```

- [ ] **Step 2: Write failing type tests**

Extend `tests/client-web/mobileTypes.test.ts` with:

```ts
import type {
  MobileAnalyticsOverview,
  MobileHeatmapBucket,
  MobileTimelineBlockPage,
  MobileAppCatalogOverride,
  MobileAppCategoryRule,
  MobileUsageGoal,
} from '../../src/client-web/src/api/mobile';

const overview: MobileAnalyticsOverview = {
  range: {
    rangeStartUtc: '2026-07-01T16:00:00Z',
    rangeEndUtc: '2026-07-08T16:00:00Z',
    timezone: 'Asia/Shanghai',
    localStartDate: '2026-07-02',
    localEndDate: '2026-07-08',
  },
  generatedAt: '2026-07-08T10:00:00Z',
  isStale: false,
  totalForegroundSeconds: 3600,
  dailyAverageSeconds: 600,
  previousPeriodChange: 0.25,
  highestUseLocalDate: '2026-07-08',
  peakLocalHour: 21,
  appCount: 12,
  switchOrPickupCount: 42,
  completeness: 0.94,
  quality: {
    usageEventsCoverage: 0.92,
    fallbackShare: 0.08,
    missingMetadataAppCount: 1,
    systemNoiseShare: 0.03,
    shortEventShare: 0.02,
    failedOrPartialSyncBatchCount: 0,
    lastSyncAt: '2026-07-08T09:59:00Z',
    qualityFlags: [],
  },
  goalProgress: {
    key: 'total-daily',
    label: '每日手机总时长',
    limitSeconds: 14400,
    usedSeconds: 3600,
    isOverLimit: false,
    remainingSeconds: 10800,
  },
  anomalies: [{ code: 'night-use', severity: 'Warning', title: '夜间使用偏高', evidence: '22:00 后使用增加', drilldownTarget: 'heatmap:night' }],
  suggestions: [{ code: 'short-video-night', text: '短视频/娱乐集中在 22:00 后', drilldownTarget: 'category:短视频/娱乐' }],
};

const heatmapBucket: MobileHeatmapBucket = {
  bucketStartUtc: '2026-07-06T13:00:00Z',
  bucketEndUtc: '2026-07-06T14:00:00Z',
  localDate: '2026-07-06',
  localHour: 21,
  lifeCategory: '社交沟通',
  foregroundSeconds: 1800,
  qualityFlags: [],
};

const page: MobileTimelineBlockPage = { items: [], nextCursor: null, hasMore: false };
const override: MobileAppCatalogOverride = { packageName: 'com.tencent.mobileqq', displayNameOverride: 'QQ', lifeCategory: '社交沟通', isSystemNoise: false, hideShortEvents: false };
const rule: MobileAppCategoryRule = { id: 'rule-1', ruleType: 'package-prefix', pattern: 'com.tencent.', lifeCategory: '社交沟通', priority: 100, isEnabled: true };
const goal: MobileUsageGoal = { id: 'goal-1', scope: 'total-daily', packageName: null, lifeCategory: null, label: '每日手机总时长', limitSeconds: 14400, isEnabled: true, createdAt: '2026-07-08T10:00:00Z', updatedAt: '2026-07-08T10:00:00Z' };

assert.equal(overview.range.timezone, 'Asia/Shanghai');
assert.equal(heatmapBucket.lifeCategory, '社交沟通');
assert.equal(page.hasMore, false);
assert.equal(override.displayNameOverride, 'QQ');
assert.equal(rule.priority, 100);
assert.equal(goal.scope, 'total-daily');
```

- [ ] **Step 3: Run failing web mobile type tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
npm --prefix src/client-web exec tsx -- tests/client-web/mobileTypes.test.ts
```

Expected before implementation: TypeScript compile fails for missing paths/types.

- [ ] **Step 4: Implement paths, types, and functions**

In `src/client-web/src/api/mobile.ts`, add interfaces matching backend DTOs and functions:

```ts
export interface MobileAnalyticsQuery {
  rangeStartUtc?: string;
  rangeEndUtc?: string;
  timezone?: string;
  deviceId?: string;
  category?: string;
  packageName?: string;
  source?: string;
  includeSystemNoise?: boolean;
  minDurationSeconds?: number;
  granularity?: 'hour' | '30m' | '15m' | 'day';
  cursor?: string;
  pageSize?: number;
}

export function getMobileAnalyticsOverview(query: MobileAnalyticsQuery): Promise<MobileAnalyticsOverview> {
  return apiGet<ApiResponse<MobileAnalyticsOverview>>(mobileApiPaths.analyticsOverview(query)).then(r => r.data);
}

export function getMobileHeatmap(query: MobileAnalyticsQuery): Promise<MobileHeatmapBucket[]> {
  return apiGet<ApiResponse<MobileHeatmapBucket[]>>(mobileApiPaths.analyticsHeatmap(query)).then(r => r.data);
}
```

Also add chart, timeline block, override, rule, and goal functions using `apiGet`, `apiPost`, `apiPut`, and `apiDelete`.

- [ ] **Step 5: Run web type tests**

Run the same three commands from Step 3.

Expected: tests pass.

- [ ] **Step 6: Commit web API contracts**

Run:

```powershell
git add src/client-web/src/api/mobile.ts tests/client-web/mobileApiPath.test.ts tests/client-web/mobileTypes.test.ts
git commit -m "feat: add mobile analytics web api"
```

## Task 10: Web Analytics Workbench UI

**Files:**
- Modify: `src/client-web/src/pages/MobileRecordsPage.tsx`
- Create: `src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx`
- Create: `src/client-web/src/components/mobile/MobileInsightStrip.tsx`
- Create: `src/client-web/src/components/mobile/MobileUsageHeatmap.tsx`
- Create: `src/client-web/src/components/mobile/MobileChartsGrid.tsx`
- Create: `src/client-web/src/components/mobile/MobileTimelineBlocks.tsx`
- Create: `src/client-web/src/components/mobile/MobileAnomalyPanel.tsx`
- Modify: `src/client-web/src/components/mobile/mobileFormatting.ts`
- Test: `tests/client-web/mobileAnalyticsComponents.test.tsx`
- Test: `tests/client-web/mobileAnalyticsInteractions.test.tsx`

- [ ] **Step 1: Write failing component smoke test**

Create `tests/client-web/mobileAnalyticsComponents.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import path from 'node:path';
import type { MobileAnalyticsOverview, MobileHeatmapBucket, MobileTimelineBlockPage } from '../../src/client-web/src/api/mobile';

const requireFromClient = createRequire(path.join(process.cwd(), 'src/client-web/package.json'));
const React = requireFromClient('react') as typeof import('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server') as typeof import('react-dom/server');
(globalThis as typeof globalThis & { React: typeof React }).React = React;

async function main() {
  const { default: MobileInsightStrip } = await import('../../src/client-web/src/components/mobile/MobileInsightStrip');
  const { default: MobileUsageHeatmap } = await import('../../src/client-web/src/components/mobile/MobileUsageHeatmap');
  const { default: MobileTimelineBlocks } = await import('../../src/client-web/src/components/mobile/MobileTimelineBlocks');

  const overview: MobileAnalyticsOverview = {
    range: { rangeStartUtc: '2026-07-01T16:00:00Z', rangeEndUtc: '2026-07-08T16:00:00Z', timezone: 'Asia/Shanghai', localStartDate: '2026-07-02', localEndDate: '2026-07-08' },
    generatedAt: '2026-07-08T10:00:00Z',
    isStale: false,
    totalForegroundSeconds: 3600,
    dailyAverageSeconds: 600,
    previousPeriodChange: 0.25,
    highestUseLocalDate: '2026-07-08',
    peakLocalHour: 21,
    appCount: 12,
    switchOrPickupCount: 42,
    completeness: 0.94,
    quality: { usageEventsCoverage: 0.92, fallbackShare: 0.08, missingMetadataAppCount: 1, systemNoiseShare: 0.03, shortEventShare: 0.02, failedOrPartialSyncBatchCount: 0, lastSyncAt: '2026-07-08T09:59:00Z', qualityFlags: [] },
    goalProgress: { key: 'total-daily', label: '每日手机总时长', limitSeconds: 14400, usedSeconds: 3600, isOverLimit: false, remainingSeconds: 10800 },
    anomalies: [],
    suggestions: [],
  };
  const heatmap: MobileHeatmapBucket[] = [{ bucketStartUtc: '2026-07-06T13:00:00Z', bucketEndUtc: '2026-07-06T14:00:00Z', localDate: '2026-07-06', localHour: 21, lifeCategory: '社交沟通', foregroundSeconds: 1800, qualityFlags: [] }];
  const page: MobileTimelineBlockPage = { items: [{ id: 'block-1', startUtc: '2026-07-06T13:00:00Z', endUtc: '2026-07-06T14:00:00Z', localStart: '21:00', localEnd: '22:00', lifeCategory: '社交沟通', foregroundSeconds: 1800, sessionCount: 2, appCount: 2, topApps: [], qualityFlags: [] }], nextCursor: null, hasMore: false };

  const html = [
    renderToStaticMarkup(React.createElement(MobileInsightStrip, { overview })),
    renderToStaticMarkup(React.createElement(MobileUsageHeatmap, { buckets: heatmap, onSelectBucket: () => undefined })),
    renderToStaticMarkup(React.createElement(MobileTimelineBlocks, { page, onLoadMore: () => undefined, onExpandBlock: () => undefined })),
  ].join('\n');

  for (const text of ['总使用时长', '日均', '目标', '热力图', '社交沟通', '时间块']) {
    assert.equal(html.includes(text), true, `expected ${text}`);
  }
}

main().catch(error => { throw error; });
```

- [ ] **Step 2: Run failing component test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
```

Expected before implementation: import fails because components do not exist.

- [ ] **Step 3: Implement analytics components with stable dimensions**

Use existing Tailwind style conventions. Do not add a chart library for the first pass; implement charts with semantic HTML/CSS bars and grids. `MobileUsageHeatmap` should use a stable 24-column grid on desktop and horizontal scroll on narrow screens:

```tsx
export default function MobileUsageHeatmap({
  buckets,
  onSelectBucket,
}: {
  buckets: MobileHeatmapBucket[];
  onSelectBucket: (bucket: MobileHeatmapBucket) => void;
}) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-slate-950">热力图</h2>
        <span className="text-xs text-slate-500">北京时间</span>
      </div>
      <div className="mt-4 overflow-x-auto">
        <div className="grid min-w-[720px] grid-cols-24 gap-1">
          {buckets.map(bucket => (
            <button
              key={`${bucket.bucketStartUtc}-${bucket.lifeCategory}`}
              type="button"
              onClick={() => onSelectBucket(bucket)}
              className="h-8 rounded border border-slate-100 text-[10px] text-slate-700"
              title={`${bucket.localDate} ${bucket.localHour}:00 ${bucket.lifeCategory}`}
            >
              {bucket.localHour}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
```

If Tailwind does not provide `grid-cols-24`, use inline `style={{ gridTemplateColumns: 'repeat(24, minmax(24px, 1fr))' }}`.

- [ ] **Step 4: Switch `MobileRecordsPage` to analytics queries**

Keep TanStack Query. Compute default range in Beijing time:

```ts
const DEFAULT_TIMEZONE = 'Asia/Shanghai';

function beijingDateInput(offsetDays: number) {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: DEFAULT_TIMEZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  return formatter.format(date);
}
```

Use range shortcuts: `today`, `7d`, `30d`, `custom`. Query overview, heatmap, charts, and timeline blocks with shared filter state.

- [ ] **Step 5: Run component tests and build**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
npm --prefix src/client-web run build
```

Expected: both pass.

- [ ] **Step 6: Commit web analytics UI**

Run:

```powershell
git add src/client-web/src/pages/MobileRecordsPage.tsx src/client-web/src/components/mobile tests/client-web/mobileAnalyticsComponents.test.tsx tests/client-web/mobileAnalyticsInteractions.test.tsx
git commit -m "feat: add mobile analytics workbench"
```

## Task 11: Web App Catalog Manager

**Files:**
- Create: `src/client-web/src/components/mobile/MobileAppCatalogManager.tsx`
- Modify: `tests/client-web/mobileAnalyticsComponents.test.tsx`

- [ ] **Step 1: Add failing catalog manager render test**

Extend `mobileAnalyticsComponents.test.tsx` to import `MobileAppCatalogManager` and assert labels:

```tsx
const { default: MobileAppCatalogManager } = await import('../../src/client-web/src/components/mobile/MobileAppCatalogManager');
const catalogHtml = renderToStaticMarkup(React.createElement(MobileAppCatalogManager, {
  overrides: [{ packageName: 'com.tencent.mobileqq', displayNameOverride: 'QQ', lifeCategory: '社交沟通', isSystemNoise: false, hideShortEvents: false }],
  rules: [{ id: 'rule-1', ruleType: 'package-prefix', pattern: 'com.tencent.', lifeCategory: '社交沟通', priority: 100, isEnabled: true }],
  onSaveOverride: () => undefined,
  onSaveRule: () => undefined,
  onDeleteRule: () => undefined,
}));
assert.equal(catalogHtml.includes('应用管理'), true);
assert.equal(catalogHtml.includes('QQ'), true);
assert.equal(catalogHtml.includes('批量规则'), true);
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
```

Expected before implementation: import fails.

- [ ] **Step 3: Implement manager**

Build a compact management panel with:

```tsx
export interface MobileAppCatalogManagerProps {
  overrides: MobileAppCatalogOverride[];
  rules: MobileAppCategoryRule[];
  onSaveOverride: (override: MobileAppCatalogOverride) => void;
  onSaveRule: (rule: MobileAppCategoryRule) => void;
  onDeleteRule: (id: string) => void;
}
```

Use a select for life category, checkbox for system noise, checkbox for hide short events, and a small rule table. Do not use nested cards.

- [ ] **Step 4: Run tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileAnalyticsComponents.test.tsx
npm --prefix src/client-web run build
```

Expected: pass.

- [ ] **Step 5: Commit catalog UI**

Run:

```powershell
git add src/client-web/src/components/mobile/MobileAppCatalogManager.tsx tests/client-web/mobileAnalyticsComponents.test.tsx
git commit -m "feat: manage mobile app categories"
```

## Task 12: Integration Verification, Push, And GitHub Actions

**Files:**
- No source files unless fixing verification failures.

- [ ] **Step 1: Re-run status and inspect intentional changes**

Run:

```powershell
git status --short --branch
git log --oneline -12
```

Expected: on `master`; only intentional uncommitted changes exist before final fix commits.

- [ ] **Step 2: Run backend verification**

Run:

```powershell
dotnet test Pim.sln
```

Expected: all tests pass.

- [ ] **Step 3: Run web verification**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/mobileApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/mobileNavigation.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/mobileComponents.test.tsx
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.mobile.json
npm --prefix src/client-web run build
```

Expected: focused mobile tests, TypeScript, and Vite build pass.

- [ ] **Step 4: Attempt Android local verification**

Run:

```powershell
Push-Location src\client-android
.\gradlew.bat assembleDebug --no-daemon
Pop-Location
```

Expected: debug APK builds. If blocked by local Java/Android SDK, write the exact failure into the final implementation summary and rely on GitHub Actions `Build Android APK` for final Android build verification.

- [ ] **Step 5: Commit any verification fixes**

If Step 2-4 required fixes:

```powershell
git add -- src/modules/Pim.Module.Mobile src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Mobile src/client-web/src/api/mobile.ts src/client-web/src/pages/MobileRecordsPage.tsx src/client-web/src/components/mobile tests/client-web src/client-android/app/src/main/java/com/pim/app/mobile src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt
git diff --cached --check
git commit -m "fix: stabilize mobile analytics verification"
```

Expected: no generated outputs are staged.

- [ ] **Step 6: Push master**

Run:

```powershell
git status --short --branch
git push origin master
```

Expected: push succeeds.

- [ ] **Step 7: Wait for GitHub Actions**

Use GitHub UI or CLI. With GitHub CLI available, run:

```powershell
gh run list --branch master --limit 20
```

Wait until these workflows triggered by the pushed commit are successful:

```text
Build API
Build Web Client
Build Android APK
Build Windows Client
```

If a workflow does not trigger because path filters did not match, dispatch it manually:

```powershell
gh workflow run build-api.yml --ref master
gh workflow run build-web.yml --ref master
gh workflow run build-android.yml --ref master
gh workflow run build-windows.yml --ref master
```

Then wait:

```powershell
$workflowNames = @('Build API', 'Build Web Client', 'Build Android APK', 'Build Windows Client')
$runs = gh run list --branch master --limit 30 --json databaseId,name,headSha,status,conclusion | ConvertFrom-Json
$runs |
  Where-Object { $workflowNames -contains $_.name } |
  Sort-Object name -Unique |
  ForEach-Object { gh run watch $_.databaseId --exit-status }
```

Expected: all four workflows finish successfully on `master`.

- [ ] **Step 8: Final status**

Run:

```powershell
git status --short --branch
```

Expected: `master...origin/master` with no uncommitted source changes. Ignored generated files may exist but are not staged.

## Plan Self-Review Checklist

Before executing implementation, the coordinator must verify:

- [ ] Every design spec acceptance criterion maps to one or more tasks above.
- [ ] The implementation uses parallel subagents with at most 14 running simultaneously.
- [ ] The plan includes the final Goal mode objective.
- [ ] Backend, Android, Web, tests, push, and GitHub Actions waiting are all covered.
- [ ] No task asks workers to commit generated build artifacts.
- [ ] Old mobile endpoints remain compatible.
- [ ] Verification commands are explicit.
