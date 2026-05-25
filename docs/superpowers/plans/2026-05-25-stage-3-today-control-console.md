# Stage 3 Today Control Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage 3 Today control console as an extensible, read-only section registry with independent section loading.

**Architecture:** Add Today contracts to `Pim.Core`, API-layer section providers that aggregate existing Calendar, PC Tracker, and Operations services, and `/api/v1/today/sections` endpoints. Refactor the Web Today page so it asks the server which sections exist, loads known sections individually, and keeps titles, ordering, layout, and visual treatment in React.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core InMemory tests, xUnit, React 19, TypeScript, TanStack Query, Node `assert` tests via `tsx`.

---

## File Structure

Create these backend files:

- `src/Pim.Core/Today/TodayDtos.cs`: shared Today DTOs, status constants, query, link, registry item, section response, and provider interface.
- `src/Pim.Api/Today/TodaySectionService.cs`: registry and per-section orchestration with date parsing, PC business date calculation, 404 behavior, and provider exception isolation.
- `src/Pim.Api/Today/TodaySectionProviders.cs`: concrete providers for `calendar.schedule`, `calendar.tasks`, `pc.activity`, `pc.quality`, `operations.health`, and `pc.classification_suggestions`.
- `src/Pim.Api/Endpoints/TodayEndpoints.cs`: minimal API group for `/api/v1/today/sections`.
- `tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs`: fast unit tests for registry shape, date handling, unknown section id, and provider failure isolation.
- `tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs`: provider tests with InMemory EF data and fake services.

Modify these backend files:

- `src/Pim.Api/Program.cs`: register Today services and map Today endpoints.

Create these frontend files:

- `src/client-web/src/api/today.ts`: API paths and `getTodaySectionRegistry`, `getTodaySection`.
- `src/client-web/src/components/today/TodaySectionHost.tsx`: shared host that loads and renders known section kinds.
- `src/client-web/src/components/today/TodayPcQualitySection.tsx`: compact PC quality section.
- `src/client-web/src/components/today/TodayHealthSection.tsx`: compact system health section.
- `src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx`: classification suggestions summary.
- `tests/client-web/todayApiPath.test.ts`: path and URL builder tests.
- `tests/client-web/todayTypes.test.ts`: TypeScript contract shape tests.
- `docs/operations/today-stage3-acceptance.md`: manual acceptance checklist.

Modify these frontend files:

- `src/client-web/src/types/index.ts`: Today registry and section types.
- `src/client-web/src/pages/TodayPage.tsx`: registry-first Today page; Web-owned section titles and layout.
- `src/client-web/src/components/today/TodayScheduleList.tsx`: accept server-filtered schedule section data.
- `src/client-web/src/components/today/TodayTaskColumn.tsx`: accept server section data shape for task attention.
- `src/client-web/src/components/today/TodayPcOverview.tsx`: accept `pc.activity` section data and keep existing visual summary.

Do not touch:

- `docs/plan.md` unless the user explicitly asks.
- Quick notes files or models.
- Plan-vs-reality matching files or models.

---

## Task 1: Add Today Core Contracts And Service

**Files:**
- Create: `src/Pim.Core/Today/TodayDtos.cs`
- Create: `src/Pim.Api/Today/TodaySectionService.cs`
- Create: `tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs`

- [ ] **Step 1: Write failing service tests**

Create `tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs`:

```csharp
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Api.Today;
using Pim.Core.Today;
using Xunit;

namespace Pim.UnitTests.Today;

public class TodaySectionServiceTests
{
    [Fact]
    public async Task GetRegistryAsync_ReturnsProviderMetadataWithoutUiFields()
    {
        var service = CreateService(new FakeProvider("calendar.schedule", "calendar.schedule"));

        var registry = await service.GetRegistryAsync("2026-05-25", CancellationToken.None);

        Assert.Equal("2026-05-25", registry.Date);
        Assert.Equal("2026-05-25", registry.PcBusinessDate);
        var section = Assert.Single(registry.Sections);
        Assert.Equal("calendar.schedule", section.Id);
        Assert.Equal("calendar.schedule", section.Kind);
        Assert.Equal(TodaySectionStatuses.Available, section.Status);
        Assert.DoesNotContain(section.Links, link => link.Rel == "details");
        Assert.Contains(section.Links, link =>
            link.Rel == TodayLinkRels.Self
            && link.Href == "/api/v1/today/sections/calendar.schedule?date=2026-05-25");
    }

    [Fact]
    public async Task GetRegistryAsync_UsesPreviousPcBusinessDateBeforeFourAm()
    {
        var service = CreateService(new FakeProvider("pc.activity", "pc.activity"));

        var registry = await service.GetRegistryAsync("2026-05-25T03:30:00", CancellationToken.None);

        Assert.Equal("2026-05-25", registry.Date);
        Assert.Equal("2026-05-24", registry.PcBusinessDate);
    }

    [Fact]
    public async Task GetSectionAsync_ReturnsProviderPayload()
    {
        var provider = new FakeProvider("operations.health", "operations.health");
        var service = CreateService(provider);

        var section = await service.GetSectionAsync("operations.health", "2026-05-25", CancellationToken.None);

        Assert.Equal("operations.health", section!.Id);
        Assert.Equal("operations.health", section.Kind);
        Assert.Equal(TodaySectionStatuses.Normal, section.Status);
        Assert.Equal(1, provider.BuildCount);
    }

    [Fact]
    public async Task GetSectionAsync_ReturnsNull_ForUnknownSection()
    {
        var service = CreateService(new FakeProvider("calendar.tasks", "calendar.tasks"));

        var section = await service.GetSectionAsync("missing.section", "2026-05-25", CancellationToken.None);

        Assert.Null(section);
    }

    [Fact]
    public async Task GetSectionAsync_ReturnsUnavailable_WhenProviderThrows()
    {
        var service = CreateService(new ThrowingProvider("pc.quality", "pc.quality"));

        var section = await service.GetSectionAsync("pc.quality", "2026-05-25", CancellationToken.None);

        Assert.NotNull(section);
        Assert.Equal("pc.quality", section!.Id);
        Assert.Equal(TodaySectionStatuses.Unavailable, section.Status);
        Assert.Equal("section_unavailable", section.Error!.Code);
        Assert.DoesNotContain("boom", section.Error.Message, StringComparison.OrdinalIgnoreCase);
    }

    private static TodaySectionService CreateService(params ITodaySectionProvider[] providers)
        => new(providers, NullLogger<TodaySectionService>.Instance);

    private sealed class FakeProvider : ITodaySectionProvider
    {
        public FakeProvider(string sectionId, string kind)
        {
            SectionId = sectionId;
            Kind = kind;
        }

        public string SectionId { get; }
        public string Kind { get; }
        public int BuildCount { get; private set; }

        public Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
        {
            BuildCount++;
            return Task.FromResult(new TodaySectionDto(
                SectionId,
                Kind,
                TodaySectionStatuses.Normal,
                DateTimeOffset.UtcNow,
                new { ok = true },
                new[] { new TodayLinkDto(TodayLinkRels.Details, "/status") },
                null));
        }
    }

    private sealed class ThrowingProvider : ITodaySectionProvider
    {
        public ThrowingProvider(string sectionId, string kind)
        {
            SectionId = sectionId;
            Kind = kind;
        }

        public string SectionId { get; }
        public string Kind { get; }

        public Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
            => throw new InvalidOperationException("boom");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodaySectionServiceTests
```

Expected: FAIL because `Pim.Core.Today` and `Pim.Api.Today.TodaySectionService` do not exist.

- [ ] **Step 3: Add Today core DTOs**

Create `src/Pim.Core/Today/TodayDtos.cs`:

```csharp
namespace Pim.Core.Today;

public static class TodaySectionStatuses
{
    public const string Available = "available";
    public const string Normal = "normal";
    public const string Empty = "empty";
    public const string Warning = "warning";
    public const string Critical = "critical";
    public const string Unavailable = "unavailable";
}

public static class TodayLinkRels
{
    public const string Self = "self";
    public const string Details = "details";
    public const string Api = "api";
}

public sealed record TodayQuery(
    DateOnly Date,
    DateOnly PcBusinessDate);

public sealed record TodayLinkDto(
    string Rel,
    string Href);

public sealed record TodaySectionErrorDto(
    string Code,
    string Message);

public sealed record TodaySectionRegistryItemDto(
    string Id,
    string Kind,
    string Status,
    IReadOnlyList<TodayLinkDto> Links);

public sealed record TodaySectionRegistryDto(
    string Date,
    string PcBusinessDate,
    DateTimeOffset GeneratedAt,
    IReadOnlyList<TodaySectionRegistryItemDto> Sections);

public sealed record TodaySectionDto(
    string Id,
    string Kind,
    string Status,
    DateTimeOffset GeneratedAt,
    object Data,
    IReadOnlyList<TodayLinkDto> Links,
    TodaySectionErrorDto? Error);

public interface ITodaySectionProvider
{
    string SectionId { get; }
    string Kind { get; }
    Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct);
}
```

- [ ] **Step 4: Add Today section service**

Create `src/Pim.Api/Today/TodaySectionService.cs`:

```csharp
using Microsoft.Extensions.Logging;
using Pim.Core.Today;

namespace Pim.Api.Today;

public sealed class TodaySectionService
{
    private const int PcBusinessDayStartHour = 4;
    private readonly IReadOnlyList<ITodaySectionProvider> _providers;
    private readonly ILogger<TodaySectionService> _logger;

    public TodaySectionService(
        IEnumerable<ITodaySectionProvider> providers,
        ILogger<TodaySectionService> logger)
    {
        _providers = providers
            .GroupBy(p => p.SectionId, StringComparer.Ordinal)
            .Select(g => g.First())
            .OrderBy(p => p.SectionId, StringComparer.Ordinal)
            .ToList();
        _logger = logger;
    }

    public Task<TodaySectionRegistryDto> GetRegistryAsync(string? date, CancellationToken ct)
    {
        var query = BuildQuery(date);
        var dateText = FormatDate(query.Date);
        var sections = _providers
            .Select(provider => new TodaySectionRegistryItemDto(
                provider.SectionId,
                provider.Kind,
                TodaySectionStatuses.Available,
                new[]
                {
                    new TodayLinkDto(
                        TodayLinkRels.Self,
                        $"/api/v1/today/sections/{Uri.EscapeDataString(provider.SectionId)}?date={dateText}")
                }))
            .ToList();

        var registry = new TodaySectionRegistryDto(
            dateText,
            FormatDate(query.PcBusinessDate),
            DateTimeOffset.UtcNow,
            sections);

        return Task.FromResult(registry);
    }

    public async Task<TodaySectionDto?> GetSectionAsync(string sectionId, string? date, CancellationToken ct)
    {
        var provider = _providers.FirstOrDefault(p =>
            string.Equals(p.SectionId, sectionId, StringComparison.Ordinal));

        if (provider is null)
            return null;

        var query = BuildQuery(date);
        try
        {
            return await provider.BuildAsync(query, ct);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Today section {SectionId} failed to load.", sectionId);
            return new TodaySectionDto(
                provider.SectionId,
                provider.Kind,
                TodaySectionStatuses.Unavailable,
                DateTimeOffset.UtcNow,
                new { },
                Array.Empty<TodayLinkDto>(),
                new TodaySectionErrorDto(
                    "section_unavailable",
                    "This Today section is temporarily unavailable."));
        }
    }

    private static TodayQuery BuildQuery(string? date)
    {
        var (todayDate, localDateTime, hasExplicitTime) = ParseRequestedDate(date);
        var pcDate = hasExplicitTime && localDateTime.Hour < PcBusinessDayStartHour
            ? DateOnly.FromDateTime(localDateTime.AddDays(-1).Date)
            : todayDate;

        return new TodayQuery(
            todayDate,
            pcDate);
    }

    private static (DateOnly TodayDate, DateTime LocalDateTime, bool HasExplicitTime) ParseRequestedDate(string? date)
    {
        if (string.IsNullOrWhiteSpace(date))
        {
            var now = DateTime.Now;
            return (DateOnly.FromDateTime(now.Date), now, true);
        }

        if (DateOnly.TryParseExact(
            date,
            "yyyy-MM-dd",
            System.Globalization.CultureInfo.InvariantCulture,
            System.Globalization.DateTimeStyles.None,
            out var dateOnly))
        {
            return (dateOnly, dateOnly.ToDateTime(TimeOnly.MinValue), false);
        }

        if (DateTime.TryParse(date, out var parsed))
            return (DateOnly.FromDateTime(parsed.Date), parsed, true);

        throw new FormatException($"Invalid Today date '{date}'. Expected YYYY-MM-DD.");
    }

    private static string FormatDate(DateOnly date)
        => date.ToString("yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture);
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodaySectionServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/Pim.Core/Today/TodayDtos.cs src/Pim.Api/Today/TodaySectionService.cs tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs
git commit -m "feat(today): add section registry service"
```

---

## Task 2: Add Today Endpoints And Service Registration

**Files:**
- Create: `src/Pim.Api/Endpoints/TodayEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`
- Test: `tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs`

- [ ] **Step 1: Add endpoint path assertions to existing tests**

Append this test to `tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs`:

```csharp
[Fact]
public void TodayEndpointPaths_AreStable()
{
    Assert.Equal("/api/v1/today/sections", TodayEndpointPaths.Sections);
    Assert.Equal("/api/v1/today/sections/calendar.schedule", TodayEndpointPaths.Section("calendar.schedule"));
}
```

Add this using at the top if not already present:

```csharp
using Pim.Api.Endpoints;
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodayEndpointPaths_AreStable
```

Expected: FAIL because `TodayEndpointPaths` does not exist.

- [ ] **Step 3: Add Today endpoints**

Create `src/Pim.Api/Endpoints/TodayEndpoints.cs`:

```csharp
using Pim.Api.Today;
using Pim.Core.Common;
using Pim.Core.Today;

namespace Pim.Api.Endpoints;

public static class TodayEndpointPaths
{
    public const string Sections = "/api/v1/today/sections";

    public static string Section(string sectionId)
        => $"{Sections}/{sectionId}";
}

public static class TodayEndpoints
{
    public static void MapTodayEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/today").RequireAuthorization();

        group.MapGet("/sections", async (
            string? date,
            TodaySectionService today,
            CancellationToken ct) =>
        {
            var result = await today.GetRegistryAsync(date, ct);
            return Results.Ok(ApiResponse<TodaySectionRegistryDto>.Ok(result));
        });

        group.MapGet("/sections/{sectionId}", async (
            string sectionId,
            string? date,
            TodaySectionService today,
            CancellationToken ct) =>
        {
            var result = await today.GetSectionAsync(sectionId, date, ct);
            return result is null
                ? Results.NotFound(ApiResponse<string>.Error(404, "Today section not found."))
                : Results.Ok(ApiResponse<TodaySectionDto>.Ok(result));
        });
    }
}
```

- [ ] **Step 4: Register Today services in Program**

Modify `src/Pim.Api/Program.cs`.

Add this using:

```csharp
using Pim.Api.Today;
using Pim.Core.Today;
```

After `moduleRegistry.DiscoverModules(builder.Services, builder.Configuration);`, add:

```csharp
builder.Services.AddScoped<TodaySectionService>();
```

After `app.MapOperationsEndpoints();`, add:

```csharp
app.MapTodayEndpoints();
```

- [ ] **Step 5: Run path tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodayEndpointPaths_AreStable
```

Expected: PASS.

- [ ] **Step 6: Run all Today service tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodaySectionServiceTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/Pim.Api/Endpoints/TodayEndpoints.cs src/Pim.Api/Program.cs tests/Pim.UnitTests/Today/TodaySectionServiceTests.cs
git commit -m "feat(today): expose section endpoints"
```

---

## Task 3: Add Today Section Providers

**Files:**
- Create: `src/Pim.Api/Today/TodaySectionProviders.cs`
- Modify: `src/Pim.Api/Program.cs`
- Create: `tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs`

- [ ] **Step 1: Write failing provider tests**

Create `tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Api.Today;
using Pim.Core.Operations;
using Pim.Core.Today;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Today;

public class TodaySectionProviderTests
{
    [Fact]
    public async Task CalendarScheduleProvider_ReturnsEventsAndScheduledTasks()
    {
        var (db, userId) = CreateDb();
        var calendarService = CreateCalendarService(db, userId);
        var calendar = await calendarService.CreateCalendarAsync(
            new CreateCalendarRequest("Default", "#2563eb", "calendar"),
            CancellationToken.None);
        var start = new DateTimeOffset(2026, 5, 25, 9, 0, 0, TimeSpan.Zero);
        var evt = await calendarService.CreateEventAsync(
            new CreateEventRequest(calendar.Id, "Planning", null, null, start, start.AddHours(1), null),
            CancellationToken.None);
        var task = await calendarService.CreateTaskAsync(
            new CreateTaskRequest(null, "Scheduled task", null, 2, null, null, null, start.AddHours(2)),
            CancellationToken.None);

        var provider = new CalendarScheduleTodaySectionProvider(calendarService);
        var section = await provider.BuildAsync(Query(), CancellationToken.None);

        Assert.Equal("calendar.schedule", section.Id);
        Assert.Equal(TodaySectionStatuses.Normal, section.Status);
        var data = Assert.IsType<CalendarScheduleTodayData>(section.Data);
        Assert.Contains(data.Events, x => x.Id == evt.Id);
        Assert.Contains(data.ScheduledTasks, x => x.Id == task.Id);
    }

    [Fact]
    public async Task CalendarTasksProvider_ReturnsWarning_WhenOverdueTasksExist()
    {
        var (db, userId) = CreateDb();
        var calendarService = CreateCalendarService(db, userId);
        await calendarService.CreateTaskAsync(
            new CreateTaskRequest(null, "Overdue", null, 1, null, null, new DateTimeOffset(2026, 5, 24, 10, 0, 0, TimeSpan.Zero), null),
            CancellationToken.None);

        var provider = new CalendarTasksTodaySectionProvider(calendarService);
        var section = await provider.BuildAsync(Query(), CancellationToken.None);

        Assert.Equal(TodaySectionStatuses.Warning, section.Status);
        var data = Assert.IsType<CalendarTasksTodayData>(section.Data);
        Assert.Equal(1, data.OverdueTasks.Count);
        Assert.Equal(1, data.IncompleteCount);
    }

    [Fact]
    public async Task PcQualityProvider_UsesQualityService()
    {
        var (db, _) = CreateDb(registerPc: true);
        var provider = new PcQualityTodaySectionProvider(new PcTrackerQualityService(db));

        var section = await provider.BuildAsync(Query(), CancellationToken.None);

        Assert.Equal("pc.quality", section.Id);
        var data = Assert.IsType<PcQualityTodayData>(section.Data);
        Assert.True(data.IssueCount >= 1);
        Assert.Contains(section.Links, link => link.Href == "/pc-tracker");
    }

    [Fact]
    public async Task OperationsHealthProvider_ReturnsHealthSummary()
    {
        var status = new FakeSystemStatusService(PimHealthStatus.Warning);
        var provider = new OperationsHealthTodaySectionProvider(status);

        var section = await provider.BuildAsync(Query(), CancellationToken.None);

        Assert.Equal(TodaySectionStatuses.Warning, section.Status);
        var data = Assert.IsType<OperationsHealthTodayData>(section.Data);
        Assert.Equal(PimHealthStatus.Warning, data.Summary.Status);
        Assert.Contains(section.Links, link => link.Href == "/status");
    }

    [Fact]
    public async Task ClassificationSuggestionsProvider_ReturnsWarning_WhenPendingSuggestionsExist()
    {
        var (db, _) = CreateDb(registerPc: true);
        db.Set<ActivityClassificationSuggestionEntity>().Add(new ActivityClassificationSuggestionEntity
        {
            Id = Guid.NewGuid(),
            ClusterKey = "app:unknown",
            SampleCount = 2,
            TotalDurationSeconds = 600,
            SampleRecordsJson = "[]",
            SanitizedContextJson = "{}",
            Status = "pending",
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        });
        await db.SaveChangesAsync();

        var provider = new ClassificationSuggestionsTodaySectionProvider(new ActivitySuggestionService(db));
        var section = await provider.BuildAsync(Query(), CancellationToken.None);

        Assert.Equal(TodaySectionStatuses.Warning, section.Status);
        var data = Assert.IsType<ClassificationSuggestionsTodayData>(section.Data);
        Assert.Equal(1, data.PendingCount);
    }

    private static TodayQuery Query()
        => new(new DateOnly(2026, 5, 25), new DateOnly(2026, 5, 25));

    private static (PimDbContext Db, Guid UserId) CreateDb(bool registerPc = false)
    {
        PimDbContext.RegisterModuleAssembly(typeof(CalendarEntity).Assembly);
        if (registerPc)
            PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);

        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return (new PimDbContext(options), Guid.NewGuid());
    }

    private static CalendarService CreateCalendarService(PimDbContext db, Guid userId)
        => new(
            db,
            new FixedCurrentUserService(userId),
            new RecurrenceService(NullLogger<RecurrenceService>.Instance));

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }

    private sealed class FakeSystemStatusService(PimHealthStatus status) : ISystemStatusService
    {
        public Task<SystemStatusSummaryDto> GetSummaryAsync(CancellationToken ct = default)
            => Task.FromResult(new SystemStatusSummaryDto(status, "label", "message", DateTimeOffset.UtcNow));

        public Task<SystemStatusDetailDto> GetDetailAsync(CancellationToken ct = default)
            => Task.FromResult(new SystemStatusDetailDto(
                new SystemStatusSummaryDto(status, "label", "message", DateTimeOffset.UtcNow),
                Array.Empty<StatusComponentDto>(),
                Array.Empty<string>()));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodaySectionProviderTests
```

Expected: FAIL because provider and section data types do not exist.

- [ ] **Step 3: Add provider implementations**

Create `src/Pim.Api/Today/TodaySectionProviders.cs`:

```csharp
using Pim.Core.Operations;
using Pim.Core.Today;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Services;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Services;

namespace Pim.Api.Today;

public sealed record CalendarScheduleTodayData(
    IReadOnlyList<EventResponse> Events,
    IReadOnlyList<TaskResponse> ScheduledTasks);

public sealed record CalendarTasksTodayData(
    int IncompleteCount,
    IReadOnlyList<TaskResponse> DueTodayTasks,
    IReadOnlyList<TaskResponse> OverdueTasks,
    IReadOnlyList<TaskResponse> UnscheduledTasks);

public sealed record PcActivityTodayData(PcSummaryResponse Summary);

public sealed record PcQualityTodayData(
    PcQualityResponse Quality,
    int IssueCount);

public sealed record OperationsHealthTodayData(SystemStatusDetailDto Detail)
{
    public SystemStatusSummaryDto Summary => Detail.Summary;
}

public sealed record ClassificationSuggestionsTodayData(
    int PendingCount,
    IReadOnlyList<ActivityClassificationSuggestionDto> Suggestions);

public sealed class CalendarScheduleTodaySectionProvider : ITodaySectionProvider
{
    private readonly CalendarService _calendar;

    public CalendarScheduleTodaySectionProvider(CalendarService calendar)
    {
        _calendar = calendar;
    }

    public string SectionId => "calendar.schedule";
    public string Kind => "calendar.schedule";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var start = new DateTimeOffset(query.Date.ToDateTime(TimeOnly.MinValue), TimeSpan.Zero);
        var end = start.AddDays(1);
        var events = await _calendar.GetEventsAsync(start, end, ct);
        var tasks = await _calendar.GetTasksAsync(null, ct);
        var scheduledTasks = tasks
            .Where(t => t.DtStart is not null && DateOnly.FromDateTime(t.DtStart.Value.Date) == query.Date)
            .OrderBy(t => t.DtStart)
            .ToList();

        var status = events.Count == 0 && scheduledTasks.Count == 0
            ? TodaySectionStatuses.Empty
            : TodaySectionStatuses.Normal;

        return new TodaySectionDto(
            SectionId,
            Kind,
            status,
            DateTimeOffset.UtcNow,
            new CalendarScheduleTodayData(events, scheduledTasks),
            new[] { new TodayLinkDto(TodayLinkRels.Details, "/calendar") },
            null);
    }
}

public sealed class CalendarTasksTodaySectionProvider : ITodaySectionProvider
{
    private readonly CalendarService _calendar;

    public CalendarTasksTodaySectionProvider(CalendarService calendar)
    {
        _calendar = calendar;
    }

    public string SectionId => "calendar.tasks";
    public string Kind => "calendar.tasks";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var tasks = await _calendar.GetTasksAsync(null, ct);
        var incomplete = tasks
            .Where(t => !string.Equals(t.Status, "COMPLETED", StringComparison.OrdinalIgnoreCase))
            .ToList();
        var dueToday = incomplete
            .Where(t => t.Due is not null && DateOnly.FromDateTime(t.Due.Value.Date) == query.Date)
            .OrderBy(t => t.Due)
            .ToList();
        var overdue = incomplete
            .Where(t => t.Due is not null && DateOnly.FromDateTime(t.Due.Value.Date) < query.Date)
            .OrderBy(t => t.Due)
            .ToList();
        var unscheduled = incomplete
            .Where(t => t.DtStart is null)
            .OrderBy(t => t.SortOrder)
            .ToList();

        var status = overdue.Count > 0 || dueToday.Count > 0
            ? TodaySectionStatuses.Warning
            : incomplete.Count == 0
                ? TodaySectionStatuses.Empty
                : TodaySectionStatuses.Normal;

        return new TodaySectionDto(
            SectionId,
            Kind,
            status,
            DateTimeOffset.UtcNow,
            new CalendarTasksTodayData(incomplete.Count, dueToday, overdue, unscheduled),
            new[]
            {
                new TodayLinkDto(TodayLinkRels.Details, "/tasks"),
                new TodayLinkDto(TodayLinkRels.Details, "/calendar")
            },
            null);
    }
}

public sealed class PcActivityTodaySectionProvider : ITodaySectionProvider
{
    private readonly PcTrackerService _pcTracker;

    public PcActivityTodaySectionProvider(PcTrackerService pcTracker)
    {
        _pcTracker = pcTracker;
    }

    public string SectionId => "pc.activity";
    public string Kind => "pc.activity";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var summary = await _pcTracker.GetSummaryAsync(query.PcBusinessDate.ToDateTime(TimeOnly.MinValue), ct);
        var hasData = summary.Heatmap.Any(h => h.TotalEvents > 0)
            || summary.AppRanking.Count > 0
            || summary.Metrics is not null
            || summary.Keystats is not null;

        return new TodaySectionDto(
            SectionId,
            Kind,
            hasData ? TodaySectionStatuses.Normal : TodaySectionStatuses.Empty,
            DateTimeOffset.UtcNow,
            new PcActivityTodayData(summary),
            new[] { new TodayLinkDto(TodayLinkRels.Details, "/pc-tracker") },
            null);
    }
}

public sealed class PcQualityTodaySectionProvider : ITodaySectionProvider
{
    private readonly PcTrackerQualityService _quality;

    public PcQualityTodaySectionProvider(PcTrackerQualityService quality)
    {
        _quality = quality;
    }

    public string SectionId => "pc.quality";
    public string Kind => "pc.quality";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var quality = await _quality.GetQualityAsync(
            query.PcBusinessDate.ToDateTime(TimeOnly.MinValue),
            null,
            null,
            ct);

        return new TodaySectionDto(
            SectionId,
            Kind,
            MapHealthStatus(quality.OverallStatus),
            DateTimeOffset.UtcNow,
            new PcQualityTodayData(quality, quality.Issues.Count),
            new[] { new TodayLinkDto(TodayLinkRels.Details, "/pc-tracker") },
            null);
    }

    private static string MapHealthStatus(PimHealthStatus status)
        => status switch
        {
            PimHealthStatus.Healthy => TodaySectionStatuses.Normal,
            PimHealthStatus.Warning => TodaySectionStatuses.Warning,
            PimHealthStatus.Critical => TodaySectionStatuses.Critical,
            _ => TodaySectionStatuses.Unavailable
        };
}

public sealed class OperationsHealthTodaySectionProvider : ITodaySectionProvider
{
    private readonly ISystemStatusService _status;

    public OperationsHealthTodaySectionProvider(ISystemStatusService status)
    {
        _status = status;
    }

    public string SectionId => "operations.health";
    public string Kind => "operations.health";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var detail = await _status.GetDetailAsync(ct);
        return new TodaySectionDto(
            SectionId,
            Kind,
            MapHealthStatus(detail.Summary.Status),
            DateTimeOffset.UtcNow,
            new OperationsHealthTodayData(detail),
            new[] { new TodayLinkDto(TodayLinkRels.Details, "/status") },
            null);
    }

    private static string MapHealthStatus(PimHealthStatus status)
        => status switch
        {
            PimHealthStatus.Healthy => TodaySectionStatuses.Normal,
            PimHealthStatus.Warning => TodaySectionStatuses.Warning,
            PimHealthStatus.Critical => TodaySectionStatuses.Critical,
            _ => TodaySectionStatuses.Unavailable
        };
}

public sealed class ClassificationSuggestionsTodaySectionProvider : ITodaySectionProvider
{
    private readonly ActivitySuggestionService _suggestions;

    public ClassificationSuggestionsTodaySectionProvider(ActivitySuggestionService suggestions)
    {
        _suggestions = suggestions;
    }

    public string SectionId => "pc.classification_suggestions";
    public string Kind => "pc.classification_suggestions";

    public async Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
    {
        var suggestions = await _suggestions.GetSuggestionsAsync(ct);
        return new TodaySectionDto(
            SectionId,
            Kind,
            suggestions.Count > 0 ? TodaySectionStatuses.Warning : TodaySectionStatuses.Empty,
            DateTimeOffset.UtcNow,
            new ClassificationSuggestionsTodayData(suggestions.Count, suggestions.Take(5).ToList()),
            new[] { new TodayLinkDto(TodayLinkRels.Details, "/pc-tracker") },
            null);
    }
}
```

- [ ] **Step 4: Register providers**

Modify `src/Pim.Api/Program.cs`.

After `builder.Services.AddScoped<TodaySectionService>();`, add:

```csharp
builder.Services.AddScoped<ITodaySectionProvider, CalendarScheduleTodaySectionProvider>();
builder.Services.AddScoped<ITodaySectionProvider, CalendarTasksTodaySectionProvider>();
builder.Services.AddScoped<ITodaySectionProvider, PcActivityTodaySectionProvider>();
builder.Services.AddScoped<ITodaySectionProvider, PcQualityTodaySectionProvider>();
builder.Services.AddScoped<ITodaySectionProvider, OperationsHealthTodaySectionProvider>();
builder.Services.AddScoped<ITodaySectionProvider, ClassificationSuggestionsTodaySectionProvider>();
```

- [ ] **Step 5: Run provider tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TodaySectionProviderTests
```

Expected: PASS.

- [ ] **Step 6: Run all Today tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Today
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/Pim.Api/Today/TodaySectionProviders.cs src/Pim.Api/Program.cs tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs
git commit -m "feat(today): add section providers"
```

---

## Task 4: Add Frontend Today Types And API Client

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Create: `src/client-web/src/api/today.ts`
- Create: `tests/client-web/todayApiPath.test.ts`
- Create: `tests/client-web/todayTypes.test.ts`

- [ ] **Step 1: Write failing API path test**

Create `tests/client-web/todayApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { todayApiPaths } from '../../src/client-web/src/api/today';

assert.equal(todayApiPaths.sections('2026-05-25'), '/today/sections?date=2026-05-25');
assert.equal(
  todayApiPaths.section('calendar.schedule', '2026-05-25'),
  '/today/sections/calendar.schedule?date=2026-05-25',
);
assert.equal(
  todayApiPaths.section('pc.classification_suggestions', '2026-05-25'),
  '/today/sections/pc.classification_suggestions?date=2026-05-25',
);
```

- [ ] **Step 2: Write failing type test**

Create `tests/client-web/todayTypes.test.ts`:

```ts
import assert from 'node:assert/strict';
import type {
  TodaySectionRegistry,
  TodaySection,
  TodaySectionStatus,
  CalendarTasksTodayData,
} from '../../src/client-web/src/types';

const status: TodaySectionStatus = 'warning';

const registry: TodaySectionRegistry = {
  date: '2026-05-25',
  pcBusinessDate: '2026-05-25',
  generatedAt: '2026-05-25T00:00:00Z',
  sections: [
    {
      id: 'calendar.tasks',
      kind: 'calendar.tasks',
      status: 'available',
      links: [{ rel: 'self', href: '/api/v1/today/sections/calendar.tasks?date=2026-05-25' }],
    },
  ],
};

const tasksData: CalendarTasksTodayData = {
  incompleteCount: 1,
  dueTodayTasks: [],
  overdueTasks: [],
  unscheduledTasks: [],
};

const section: TodaySection<CalendarTasksTodayData> = {
  id: 'calendar.tasks',
  kind: 'calendar.tasks',
  status,
  generatedAt: '2026-05-25T00:00:00Z',
  data: tasksData,
  links: [{ rel: 'details', href: '/tasks' }],
  error: null,
};

assert.equal(registry.sections[0].kind, 'calendar.tasks');
assert.equal(section.data.incompleteCount, 1);
assert.equal(section.error, null);
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayApiPath.test.ts
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayTypes.test.ts
```

Expected: FAIL because `src/client-web/src/api/today.ts` and Today types do not exist.

- [ ] **Step 4: Add Today types**

Append to `src/client-web/src/types/index.ts`:

```ts
export type TodaySectionStatus =
  | 'available'
  | 'normal'
  | 'empty'
  | 'warning'
  | 'critical'
  | 'unavailable';

export interface TodayLink {
  rel: 'self' | 'details' | 'api' | string;
  href: string;
}

export interface TodaySectionError {
  code: string;
  message: string;
}

export interface TodaySectionRegistryItem {
  id: string;
  kind: TodaySectionKind | string;
  status: TodaySectionStatus;
  links: TodayLink[];
}

export interface TodaySectionRegistry {
  date: string;
  pcBusinessDate: string;
  generatedAt: string;
  sections: TodaySectionRegistryItem[];
}

export interface TodaySection<TData = unknown> {
  id: string;
  kind: TodaySectionKind | string;
  status: TodaySectionStatus;
  generatedAt: string;
  data: TData;
  links: TodayLink[];
  error: TodaySectionError | null;
}

export type TodaySectionKind =
  | 'calendar.schedule'
  | 'calendar.tasks'
  | 'pc.activity'
  | 'pc.quality'
  | 'operations.health'
  | 'pc.classification_suggestions';

export interface CalendarScheduleTodayData {
  events: EventResponse[];
  scheduledTasks: TaskResponse[];
}

export interface CalendarTasksTodayData {
  incompleteCount: number;
  dueTodayTasks: TaskResponse[];
  overdueTasks: TaskResponse[];
  unscheduledTasks: TaskResponse[];
}

export interface PcActivityTodayData {
  summary: PcSummaryResponse;
}

export interface PcQualityTodayData {
  quality: PcQualityResponse;
  issueCount: number;
}

export interface OperationsHealthTodayData {
  detail: SystemStatusDetail;
  summary: SystemStatusSummary;
}

export interface ClassificationSuggestionsTodayData {
  pendingCount: number;
  suggestions: ActivityClassificationSuggestion[];
}
```

- [ ] **Step 5: Add Today API client**

Create `src/client-web/src/api/today.ts`:

```ts
import { apiGet } from './client';
import type { ApiResponse, TodaySection, TodaySectionRegistry } from '../types';

export const todayApiPaths = {
  sections: (date: string) => `/today/sections?date=${encodeURIComponent(date)}`,
  section: (sectionId: string, date: string) =>
    `/today/sections/${encodeURIComponent(sectionId)}?date=${encodeURIComponent(date)}`,
} as const;

export function getTodaySectionRegistry(date: string) {
  return apiGet<ApiResponse<TodaySectionRegistry>>(todayApiPaths.sections(date)).then(r => r.data);
}

export function getTodaySection<TData = unknown>(sectionId: string, date: string) {
  return apiGet<ApiResponse<TodaySection<TData>>>(todayApiPaths.section(sectionId, date)).then(r => r.data);
}
```

- [ ] **Step 6: Run Today client tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayApiPath.test.ts
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayTypes.test.ts
```

Expected: PASS.

- [ ] **Step 7: Run TypeScript build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/today.ts tests/client-web/todayApiPath.test.ts tests/client-web/todayTypes.test.ts
git commit -m "feat(web): add today section api client"
```

---

## Task 5: Refactor Today Page To Registry And Section Loading

**Files:**
- Create: `src/client-web/src/components/today/TodaySectionHost.tsx`
- Create: `src/client-web/src/components/today/TodayPcQualitySection.tsx`
- Create: `src/client-web/src/components/today/TodayHealthSection.tsx`
- Create: `src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx`
- Modify: `src/client-web/src/components/today/TodayScheduleList.tsx`
- Modify: `src/client-web/src/components/today/TodayTaskColumn.tsx`
- Modify: `src/client-web/src/components/today/TodayPcOverview.tsx`
- Modify: `src/client-web/src/pages/TodayPage.tsx`

- [ ] **Step 1: Add section host component**

Create `src/client-web/src/components/today/TodaySectionHost.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query';
import { getTodaySection } from '../../api/today';
import type {
  CalendarScheduleTodayData,
  CalendarTasksTodayData,
  ClassificationSuggestionsTodayData,
  OperationsHealthTodayData,
  PcActivityTodayData,
  PcQualityTodayData,
  TodaySection,
  TodaySectionRegistryItem,
} from '../../types';
import EmptyState from '../../ui/EmptyState';
import TodayScheduleList from './TodayScheduleList';
import TodayTaskColumn from './TodayTaskColumn';
import TodayPcOverview from './TodayPcOverview';
import TodayPcQualitySection from './TodayPcQualitySection';
import TodayHealthSection from './TodayHealthSection';
import TodayClassificationSuggestionsSection from './TodayClassificationSuggestionsSection';

function SectionLoading({ title }: { title: string }) {
  return (
    <section className="pim-panel min-h-[220px] p-4">
      <div className="h-4 w-28 rounded bg-slate-100" aria-label={`${title}加载中`} />
      <div className="mt-4 space-y-2">
        <div className="h-12 rounded-xl bg-slate-50" />
        <div className="h-12 rounded-xl bg-slate-50" />
      </div>
    </section>
  );
}

function SectionUnavailable({ title, message }: { title: string; message?: string }) {
  return (
    <section className="pim-panel p-4">
      <h2 className="font-semibold text-slate-900">{title}</h2>
      <div className="mt-3 rounded-xl border border-red-100 bg-red-50 px-3 py-3 text-sm text-red-700" role="alert">
        {message || '这个栏目暂时不可用。'}
      </div>
    </section>
  );
}

const sectionTitles: Record<string, string> = {
  'calendar.schedule': '今日安排',
  'calendar.tasks': '任务关注',
  'pc.activity': 'PC 记录概览',
  'pc.quality': 'PC 数据质量',
  'operations.health': '系统健康',
  'pc.classification_suggestions': '分类建议',
};

export const todaySectionOrder = [
  'calendar.schedule',
  'pc.activity',
  'calendar.tasks',
  'operations.health',
  'pc.quality',
  'pc.classification_suggestions',
];

export function getTodaySectionTitle(kind: string) {
  return sectionTitles[kind] ?? '未知栏目';
}

export function isKnownTodaySectionKind(kind: string) {
  return Object.prototype.hasOwnProperty.call(sectionTitles, kind);
}

export default function TodaySectionHost({
  item,
  date,
  todayPrefix,
  onSelectScheduled,
  onSelectTask,
}: {
  item: TodaySectionRegistryItem;
  date: string;
  todayPrefix: string;
  onSelectScheduled?: (item: { type: 'event' | 'task'; id: string }) => void;
  onSelectTask?: (taskId: string) => void;
}) {
  const title = getTodaySectionTitle(item.kind);
  const { data, isLoading, error } = useQuery({
    queryKey: ['today-section', item.id, date],
    queryFn: () => getTodaySection(item.id, date),
    enabled: isKnownTodaySectionKind(item.kind),
    refetchInterval: item.kind.startsWith('pc.') || item.kind.startsWith('operations.') ? 30000 : false,
  });

  if (!isKnownTodaySectionKind(item.kind)) {
    return (
      <section className="pim-panel p-4">
        <EmptyState title="暂不支持的栏目" description={item.kind} />
      </section>
    );
  }

  if (isLoading) return <SectionLoading title={title} />;
  if (error) return <SectionUnavailable title={title} message={(error as Error).message} />;
  if (!data) return <SectionUnavailable title={title} />;
  if (data.status === 'unavailable') {
    return <SectionUnavailable title={title} message={data.error?.message} />;
  }

  switch (data.kind) {
    case 'calendar.schedule':
      return (
        <TodayScheduleList
          section={data as TodaySection<CalendarScheduleTodayData>}
          onSelect={onSelectScheduled}
        />
      );
    case 'calendar.tasks':
      return (
        <TodayTaskColumn
          section={data as TodaySection<CalendarTasksTodayData>}
          todayPrefix={todayPrefix}
          onSelect={onSelectTask}
        />
      );
    case 'pc.activity':
      return <TodayPcOverview section={data as TodaySection<PcActivityTodayData>} />;
    case 'pc.quality':
      return <TodayPcQualitySection section={data as TodaySection<PcQualityTodayData>} />;
    case 'operations.health':
      return <TodayHealthSection section={data as TodaySection<OperationsHealthTodayData>} />;
    case 'pc.classification_suggestions':
      return (
        <TodayClassificationSuggestionsSection
          section={data as TodaySection<ClassificationSuggestionsTodayData>}
        />
      );
    default:
      return (
        <section className="pim-panel p-4">
          <EmptyState title="暂不支持的栏目" description={data.kind} />
        </section>
      );
  }
}
```

- [ ] **Step 2: Add compact quality, health, and suggestion components**

Create `src/client-web/src/components/today/TodayPcQualitySection.tsx`:

```tsx
import { Link } from 'react-router-dom';
import StatusBadge from '../../ui/StatusBadge';
import type { PcQualityTodayData, TodaySection } from '../../types';

function statusTone(status: string) {
  if (status === 'critical' || status === 'Critical') return 'danger';
  if (status === 'warning' || status === 'Warning') return 'warning';
  if (status === 'normal' || status === 'Healthy') return 'activity';
  return 'neutral';
}

export default function TodayPcQualitySection({
  section,
}: {
  section: TodaySection<PcQualityTodayData>;
}) {
  const { quality, issueCount } = section.data;

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">PC 数据质量</h2>
        <StatusBadge tone={statusTone(section.status)}>{quality.label}</StatusBadge>
      </div>
      <p className="text-sm leading-6 text-slate-600">{quality.message}</p>
      <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div className="rounded-xl bg-slate-50 px-3 py-2">
          <p className="text-xs text-slate-500">问题数</p>
          <p className="mt-1 text-lg font-semibold text-slate-950">{issueCount}</p>
        </div>
        <div className="rounded-xl bg-slate-50 px-3 py-2">
          <p className="text-xs text-slate-500">组件</p>
          <p className="mt-1 text-lg font-semibold text-slate-950">{quality.components.length}</p>
        </div>
      </div>
      {quality.nextSteps[0] && (
        <p className="mt-3 rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
          {quality.nextSteps[0]}
        </p>
      )}
      <Link className="mt-4 inline-flex text-sm font-medium text-blue-600 hover:text-blue-700" to="/pc-tracker">
        查看 PC 记录
      </Link>
    </section>
  );
}
```

Create `src/client-web/src/components/today/TodayHealthSection.tsx`:

```tsx
import { Link } from 'react-router-dom';
import StatusBadge from '../../ui/StatusBadge';
import type { OperationsHealthTodayData, TodaySection } from '../../types';

function statusTone(status: string) {
  if (status === 'critical' || status === 'Critical') return 'danger';
  if (status === 'warning' || status === 'Warning') return 'warning';
  if (status === 'normal' || status === 'Healthy') return 'activity';
  return 'neutral';
}

export default function TodayHealthSection({
  section,
}: {
  section: TodaySection<OperationsHealthTodayData>;
}) {
  const { detail, summary } = section.data;
  const daemon = detail.components.find(component => component.key === 'windows-daemon');

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">系统健康</h2>
        <StatusBadge tone={statusTone(section.status)}>{summary.label}</StatusBadge>
      </div>
      <p className="text-sm leading-6 text-slate-600">{summary.message}</p>
      {daemon && (
        <div className="mt-4 rounded-xl bg-slate-50 px-3 py-2 text-sm">
          <div className="flex items-center justify-between gap-3">
            <span className="font-medium text-slate-800">Windows daemon</span>
            <StatusBadge tone={statusTone(daemon.status)}>{daemon.status}</StatusBadge>
          </div>
          <p className="mt-1 text-xs leading-5 text-slate-500">{daemon.message}</p>
        </div>
      )}
      {detail.nextSteps[0] && (
        <p className="mt-3 rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
          {detail.nextSteps[0]}
        </p>
      )}
      <Link className="mt-4 inline-flex text-sm font-medium text-blue-600 hover:text-blue-700" to="/status">
        查看状态信息
      </Link>
    </section>
  );
}
```

Create `src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx`:

```tsx
import { Link } from 'react-router-dom';
import StatusBadge from '../../ui/StatusBadge';
import EmptyState from '../../ui/EmptyState';
import type { ClassificationSuggestionsTodayData, TodaySection } from '../../types';

export default function TodayClassificationSuggestionsSection({
  section,
}: {
  section: TodaySection<ClassificationSuggestionsTodayData>;
}) {
  const { pendingCount, suggestions } = section.data;

  return (
    <section className="pim-panel min-w-0 p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="font-semibold text-slate-900">分类建议</h2>
        <StatusBadge tone={pendingCount > 0 ? 'warning' : 'neutral'}>{pendingCount} 条</StatusBadge>
      </div>
      {pendingCount === 0 ? (
        <EmptyState title="暂无待处理建议" description="PC 活动分类暂时不需要你处理。" />
      ) : (
        <div className="space-y-2">
          {suggestions.slice(0, 3).map(suggestion => (
            <div key={suggestion.id} className="rounded-xl bg-slate-50 px-3 py-2">
              <p className="truncate text-sm font-medium text-slate-900">
                {suggestion.suggestedCategory || suggestion.currentCategory || suggestion.clusterKey}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {suggestion.sampleCount} 条记录 · {Math.round(suggestion.totalDurationSeconds / 60)} 分钟
              </p>
            </div>
          ))}
        </div>
      )}
      <Link className="mt-4 inline-flex text-sm font-medium text-blue-600 hover:text-blue-700" to="/pc-tracker">
        处理分类建议
      </Link>
    </section>
  );
}
```

- [ ] **Step 3: Adapt existing Today components to section data**

Modify `src/client-web/src/components/today/TodayScheduleList.tsx`.

Keep `ScheduledItem`, `formatTime`, and helper functions. Stop using `buildScheduledItems` inside this component because the server already filters the schedule section to today's events and scheduled tasks. Change the component signature and first lines to:

```tsx
import type { CalendarScheduleTodayData, EventResponse, TaskResponse, TodaySection } from '../../types';
```

Replace the default export signature with:

```tsx
export default function TodayScheduleList({
  section,
  onSelect,
}: {
  section: TodaySection<CalendarScheduleTodayData>;
  onSelect?: (item: ScheduledItem) => void;
}) {
  const items: ScheduledItem[] = [
    ...section.data.events.map(event => ({
      type: 'event' as const,
      id: event.id,
      title: event.title,
      start: event.dtStart,
      end: event.dtEnd,
      meta: event.location || event.description || '日程',
    })),
    ...section.data.scheduledTasks.map(task => ({
      type: 'task' as const,
      id: task.id,
      title: task.title,
      start: task.dtStart!,
      meta: task.description || '已排程任务',
      priority: task.priority,
    })),
  ].sort((a, b) => {
    const aTime = safeTime(a.start)?.getTime() ?? Number.POSITIVE_INFINITY;
    const bTime = safeTime(b.start)?.getTime() ?? Number.POSITIVE_INFINITY;
    return aTime - bTime;
  });
```

Leave the existing rendered markup below that point, using the local `items`.

Modify `src/client-web/src/components/today/TodayTaskColumn.tsx`.

Change imports:

```tsx
import type { CalendarTasksTodayData, TaskResponse, TodaySection } from '../../types';
```

Replace the component signature and incomplete task calculation with:

```tsx
export default function TodayTaskColumn({
  section,
  todayPrefix,
  onSelect,
}: {
  section: TodaySection<CalendarTasksTodayData>;
  todayPrefix: string;
  onSelect?: (taskId: string) => void;
}) {
  const incompleteTasks = sortTasksByDue([
    ...section.data.overdueTasks,
    ...section.data.dueTodayTasks,
    ...section.data.unscheduledTasks,
  ].filter((task, index, source) => source.findIndex(item => item.id === task.id) === index));
```

Change button click from:

```tsx
onClick={() => onSelect?.(task)}
```

to:

```tsx
onClick={() => onSelect?.(task.id)}
```

Modify `src/client-web/src/components/today/TodayPcOverview.tsx`.

Change imports:

```tsx
import type { PcActivityTodayData, TodaySection } from '../../types';
```

Replace the component signature:

```tsx
export default function TodayPcOverview({
  section,
}: {
  section: TodaySection<PcActivityTodayData>;
}) {
  const summary = section.data.summary;
  const metrics = summary.metrics;
  const keystats = summary.keystats;
```

Remove the `isLoading` and `error` branches from this component. Keep the no-summary/empty branch as:

```tsx
{section.status === 'empty' ? (
  <EmptyState title="暂无 PC 记录" description="守护程序同步后会显示今天的使用概览。" />
) : (
  <div className="space-y-4">
    ...
  </div>
)}
```

Set the badge text from `section.status`:

```tsx
<StatusBadge tone={section.status === 'empty' ? 'neutral' : 'activity'}>
  {section.status === 'empty' ? '暂无数据' : '今日'}
</StatusBadge>
```

- [ ] **Step 4: Refactor TodayPage to registry-first loading**

Replace `src/client-web/src/pages/TodayPage.tsx` with:

```tsx
import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getTasks } from '../api/calendar';
import { getTodaySectionRegistry } from '../api/today';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import PageHeader from '../ui/PageHeader';
import EmptyState from '../ui/EmptyState';
import TodaySectionHost, {
  isKnownTodaySectionKind,
  todaySectionOrder,
} from '../components/today/TodaySectionHost';
import type { TaskResponse, TodaySectionRegistryItem } from '../types';

function useTodayDate() {
  const [today, setToday] = useState(() => new Date());

  useEffect(() => {
    const now = new Date();
    const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const delayMs = nextMidnight.getTime() - now.getTime() + 1000;
    const timerId = window.setTimeout(() => setToday(new Date()), delayMs);

    return () => window.clearTimeout(timerId);
  }, [today]);

  return today;
}

function sortSections(sections: TodaySectionRegistryItem[]) {
  return [...sections]
    .filter(section => isKnownTodaySectionKind(section.kind))
    .sort((a, b) => {
      const aIndex = todaySectionOrder.indexOf(a.kind);
      const bIndex = todaySectionOrder.indexOf(b.kind);
      return (aIndex === -1 ? 999 : aIndex) - (bIndex === -1 ? 999 : bIndex);
    });
}

export default function TodayPage() {
  const today = useTodayDate();
  const dateStr = format(today, 'yyyy-MM-dd');
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();

  const {
    data: registry,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['today-sections', dateStr],
    queryFn: () => getTodaySectionRegistry(dateStr),
    refetchInterval: 30000,
  });

  const {
    data: tasks = [],
  } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks(),
  });

  const sections = useMemo(
    () => sortSections(registry?.sections ?? []),
    [registry],
  );

  function openTaskById(taskId: string) {
    const task = tasks.find(item => item.id === taskId);
    if (!task) return;
    setEditingTask(task);
    setTaskEditorOpen(true);
  }

  return (
    <div className="mx-auto max-w-[1500px] space-y-4 pb-8">
      <PageHeader
        title="今日工作台"
        subtitle={`${dateStr} · 计划、活动、健康和待处理事项`}
        actions={
          <button
            type="button"
            onClick={() => {
              setEditingTask(undefined);
              setTaskEditorOpen(true);
            }}
            className="pim-button-primary px-4 py-2 text-sm"
          >
            新建任务
          </button>
        }
      />

      {error && (
        <section className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          <p className="font-medium">Today 栏目加载失败</p>
          <p className="mt-1 text-xs leading-5">{(error as Error).message || '请稍后重试。'}</p>
        </section>
      )}

      {isLoading ? (
        <section className="pim-panel p-6">
          <EmptyState title="正在加载 Today" description="正在询问服务端今天有哪些栏目。" />
        </section>
      ) : (
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-4">
          {sections.map(section => (
            <div
              key={section.id}
              className={section.kind === 'pc.activity' ? 'xl:col-span-2' : undefined}
            >
              <TodaySectionHost
                item={section}
                date={dateStr}
                todayPrefix={dateStr}
                onSelectTask={openTaskById}
              />
            </div>
          ))}
        </div>
      )}

      <TaskEditorDialog
        open={taskEditorOpen}
        onClose={() => setTaskEditorOpen(false)}
        task={editingTask}
      />
    </div>
  );
}
```

This stage intentionally removes event editor opening from Today. Users can enter event detail through the Calendar link in the schedule section. Keep task editor because the page still has a "新建任务" action and task cards can open known tasks when `getTasks()` has loaded.

- [ ] **Step 5: Run TypeScript build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-web/src/components/today src/client-web/src/pages/TodayPage.tsx
git commit -m "feat(web): render today sections"
```

---

## Task 6: Add Manual Acceptance Documentation

**Files:**
- Create: `docs/operations/today-stage3-acceptance.md`

- [ ] **Step 1: Create acceptance checklist**

Create `docs/operations/today-stage3-acceptance.md`:

```markdown
# Today Stage 3 Acceptance

## Scope

Stage 3 turns `/today` into a server-backed section surface.

This stage does not implement:

- Quick notes.
- Plan-vs-reality deviation analysis.
- Daily review or scheduling suggestions.

## API Checks

- `GET /api/v1/today/sections?date=YYYY-MM-DD` returns the section registry.
- `GET /api/v1/today/sections/calendar.schedule?date=YYYY-MM-DD` returns the schedule section.
- `GET /api/v1/today/sections/calendar.tasks?date=YYYY-MM-DD` returns the task section.
- `GET /api/v1/today/sections/pc.activity?date=YYYY-MM-DD` returns the PC activity section.
- `GET /api/v1/today/sections/pc.quality?date=YYYY-MM-DD` returns the PC quality section.
- `GET /api/v1/today/sections/operations.health?date=YYYY-MM-DD` returns the health section.
- `GET /api/v1/today/sections/pc.classification_suggestions?date=YYYY-MM-DD` returns the classification suggestion section.
- An unknown section id returns 404.

The registry and section responses must not contain server-owned UI title, layout, order, priority, column, or card-size fields.

## Web Checks

- Open `/today`.
- Confirm the page shows schedule, task attention, PC activity, PC quality, system health, and classification suggestion sections.
- Confirm Web controls Chinese titles and layout.
- Confirm a section failure is shown inside that section without breaking the rest of the page.
- Confirm the schedule section links to `/calendar`.
- Confirm the task section links to `/tasks` or `/calendar`.
- Confirm PC sections link to `/pc-tracker`.
- Confirm health links to `/status`.

## Data State Checks

- Use a day with no PC data and confirm PC activity or quality shows an empty or unavailable state.
- Stop or stale the Windows daemon heartbeat and confirm the health section calls attention to it.
- Create a pending classification suggestion and confirm the suggestion count appears on Today.
- Complete all tasks and confirm the task section does not show overdue or due warning pressure.

## Verification Commands

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayApiPath.test.ts
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayTypes.test.ts
```
```

- [ ] **Step 2: Commit**

Run:

```powershell
git add docs/operations/today-stage3-acceptance.md
git commit -m "docs: add today stage 3 acceptance"
```

---

## Task 7: Final Verification

**Files:**
- No source edits expected.

- [ ] **Step 1: Run backend verification**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 2: Run frontend verification**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 3: Run focused frontend contract tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayApiPath.test.ts
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\todayTypes.test.ts
```

Expected: PASS.

- [ ] **Step 4: Check git status**

Run:

```powershell
git status --short --branch
```

Expected: only intentional files are changed or the working tree is clean except pre-existing `docs/plan.md`.

- [ ] **Step 5: Commit any final fixes**

If verification required small fixes, commit them:

```powershell
git add <fixed-files>
git commit -m "fix(today): finalize stage 3 verification"
```

If no fixes were needed, do not create an empty commit.
