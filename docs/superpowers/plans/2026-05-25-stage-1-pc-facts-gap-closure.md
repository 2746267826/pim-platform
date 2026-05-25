# Stage 1 PC Facts Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a server-owned PC facts quality summary, expose it through API and Web, and document the Stage 1 acceptance path without rebuilding the existing capture pipeline.

**Architecture:** Keep ActivityWatch and KeyStats raw tables as the source of truth. Add a focused `PcTrackerQualityService` that reads raw facts plus daemon heartbeat and returns structured health components, issues, and next steps; Web only displays this server interpretation. Keep the work scoped to quality visibility, tests, and acceptance documentation.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core with PostgreSQL/InMemory tests, React 19, TypeScript, TanStack Query, xUnit, Node `assert` tests via `tsx`.

---

## File Structure

Create:

- `src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs`: DTOs for `/api/v1/pc/quality`.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs`: service that evaluates AW bucket/event completeness, KeyStats continuity, daemon heartbeat, and interpreted timeline status.
- `tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`: backend tests for critical and warning quality states.
- `src/client-web/src/components/pc-tracker/PcQualitySummary.tsx`: compact quality panel used by PC Tracker and Status pages.
- `tests/client-web/pcQualityApiNormalization.test.ts`: TypeScript normalization tests for quality API responses.
- `docs/operations/pc-facts-stage1-acceptance.md`: Stage 1 acceptance matrix and manual verification runbook.

Modify:

- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: register the quality service and map `/api/v1/pc/quality`.
- `src/client-web/src/types/index.ts`: add PC quality response types.
- `src/client-web/src/api/pcTracker.ts`: add `getPcQuality` and `normalizePcQuality`.
- `src/client-web/src/pages/PcTrackerPage.tsx`: show today's PC facts quality summary.
- `src/client-web/src/pages/StatusPage.tsx`: show PC collection quality next to system status.
- `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`: improve empty state using quality issues for the selected range.

Do not modify:

- ActivityWatch collector upload flow.
- KeyStats collector sampling flow.
- Existing raw fact table shape, except if a later task discovers a compile-time mismatch.
- `docs/plan.md`, which is currently untracked user work.

---

### Task 1: Add Backend Quality DTOs And Bucket Checks

**Files:**

- Create: `src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs`
- Create: `src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs`
- Create: `tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`

- [ ] **Step 1: Write failing bucket quality tests**

Create `tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class PcTrackerQualityServiceTests
{
    [Fact]
    public async Task GetQualityAsync_ReturnsCritical_WhenWindowBucketIsMissing()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        AddRecentWindowsDaemon(db);
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddBucket(db, "aw-watcher-web-edge_DESKTOP", "web.tab.current");
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 10);
        AddKeyStatsSample(db, "2026-05-20T05:01:00+00:00", keys: 15);
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(
            date: new DateTime(2026, 5, 20),
            dateFrom: null,
            dateTo: null,
            CancellationToken.None);

        Assert.Equal(PimHealthStatus.Critical, quality.OverallStatus);
        Assert.Contains(quality.Issues, i => i.Code == "missing-aw-window-bucket" && i.Severity == PimHealthStatus.Critical);
        var buckets = Assert.Single(quality.Components, c => c.Key == "aw-buckets");
        Assert.Equal(PimHealthStatus.Critical, buckets.Status);
    }

    [Fact]
    public async Task GetQualityAsync_ReturnsWarning_WhenOnlyWebBucketIsMissing()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        AddRecentWindowsDaemon(db);
        AddBucket(db, "aw-watcher-window_DESKTOP", "currentwindow");
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddWindowEvent(db, "2026-05-20T05:00:00+00:00");
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 10);
        AddKeyStatsSample(db, "2026-05-20T05:01:00+00:00", keys: 15);
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(
            date: new DateTime(2026, 5, 20),
            dateFrom: null,
            dateTo: null,
            CancellationToken.None);

        Assert.Equal(PimHealthStatus.Warning, quality.OverallStatus);
        Assert.Contains(quality.Issues, i => i.Code == "missing-aw-web-bucket" && i.Severity == PimHealthStatus.Warning);
        Assert.DoesNotContain(quality.Issues, i => i.Code == "missing-aw-window-bucket");
    }

    private static void AddRecentWindowsDaemon(PimDbContext db)
    {
        db.DaemonHeartbeats.Add(new DaemonHeartbeatEntity
        {
            DeviceId = "DESKTOP",
            DaemonKind = "windows",
            Version = "1.0.0",
            ServerUrl = "http://127.0.0.1:5858",
            ActivityWatchState = DaemonSourceState.Available.ToString(),
            KeyStatsState = DaemonSourceState.Available.ToString(),
            StatusJson = "{}",
            ReceivedAt = DateTimeOffset.UtcNow
        });
    }

    private static void AddBucket(PimDbContext db, string bucketId, string bucketType)
    {
        db.Set<AwBucketEntity>().Add(new AwBucketEntity
        {
            PimDeviceId = "DESKTOP",
            BucketId = bucketId,
            BucketType = bucketType,
            Client = bucketId.Split('_')[0],
            Hostname = "DESKTOP",
            DataJson = "{}",
            SeenAt = DateTimeOffset.UtcNow,
            LastUpdatedSource = DateTimeOffset.UtcNow
        });
    }

    private static void AddWindowEvent(PimDbContext db, string timestamp)
    {
        db.Set<AwEventEntity>().Add(new AwEventEntity
        {
            DeviceId = "DESKTOP",
            Timestamp = DateTimeOffset.Parse(timestamp),
            Duration = 60,
            EventType = "window",
            AppName = "code.exe",
            AppNameNormalized = "code",
            WindowTitle = "PIM",
            BucketId = "aw-watcher-window_DESKTOP",
            BucketType = "currentwindow",
            SourceEventId = 100,
            DataJson = "{\"app\":\"code.exe\",\"title\":\"PIM\"}"
        });
    }

    private static void AddKeyStatsSample(PimDbContext db, string sampledAt, int keys)
    {
        db.Set<KeystatsSampleEntity>().Add(new KeystatsSampleEntity
        {
            PimDeviceId = "DESKTOP",
            SampledAtUtc = DateTimeOffset.Parse(sampledAt),
            StatsDate = new DateTime(2026, 5, 20),
            KeyPresses = keys,
            KeyCountsJson = "{\"A\":1}",
            AppStatsJson = "{}",
            RawJson = "{}"
        });
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerQualityServiceTests
```

Expected: FAIL with `CS0246` because `PcTrackerQualityService` does not exist.

- [ ] **Step 3: Add PC quality DTOs**

Create `src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs`:

```csharp
using Pim.Core.Operations;

namespace Pim.Module.PcTracker.DTOs;

public sealed record PcQualityResponse(
    PimHealthStatus OverallStatus,
    string Label,
    string Message,
    DateTimeOffset CheckedAt,
    IReadOnlyList<PcQualityComponentDto> Components,
    IReadOnlyList<PcQualityIssueDto> Issues,
    IReadOnlyList<string> NextSteps);

public sealed record PcQualityComponentDto(
    string Key,
    string Name,
    PimHealthStatus Status,
    string Message,
    IReadOnlyDictionary<string, string> Details);

public sealed record PcQualityIssueDto(
    string Code,
    PimHealthStatus Severity,
    string ComponentKey,
    string Message,
    string? NextStep);
```

- [ ] **Step 4: Add minimal quality service with bucket checks**

Create `src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Data.Entities;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public sealed class PcTrackerQualityService
{
    private static readonly TimeSpan WarningHeartbeatAge = TimeSpan.FromMinutes(10);
    private static readonly TimeSpan CriticalHeartbeatAge = TimeSpan.FromMinutes(60);
    private static readonly TimeSpan WarningBucketSeenAge = TimeSpan.FromMinutes(30);

    private readonly PimDbContext _db;

    public PcTrackerQualityService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<PcQualityResponse> GetQualityAsync(
        DateTime? date,
        DateTime? dateFrom,
        DateTime? dateTo,
        CancellationToken ct)
    {
        var checkedAt = DateTimeOffset.UtcNow;
        var (start, end) = GetRange(date, dateFrom, dateTo);
        var issues = new List<PcQualityIssueDto>();

        var buckets = await _db.Set<AwBucketEntity>()
            .AsNoTracking()
            .ToListAsync(ct);
        var awEvents = await _db.Set<AwEventEntity>()
            .AsNoTracking()
            .Where(e => e.Timestamp >= start && e.Timestamp < end)
            .ToListAsync(ct);
        var samples = await _db.Set<KeystatsSampleEntity>()
            .AsNoTracking()
            .Where(s => s.SampledAtUtc >= start && s.SampledAtUtc < end)
            .OrderBy(s => s.PimDeviceId)
            .ThenBy(s => s.SampledAtUtc)
            .ToListAsync(ct);
        var heartbeat = await _db.DaemonHeartbeats
            .AsNoTracking()
            .Where(h => h.DaemonKind == "windows")
            .OrderByDescending(h => h.ReceivedAt)
            .FirstOrDefaultAsync(ct);

        AddBucketIssues(buckets, checkedAt, issues);
        AddAwEventIssues(awEvents, issues);
        AddKeyStatsIssues(samples, issues);
        AddDaemonIssues(heartbeat, checkedAt, issues);

        var components = new List<PcQualityComponentDto>
        {
            BuildComponent(
                "aw-buckets",
                "ActivityWatch buckets",
                issues,
                new Dictionary<string, string>
                {
                    ["windowBuckets"] = buckets.Count(b => b.BucketType == "currentwindow").ToString(),
                    ["afkBuckets"] = buckets.Count(b => b.BucketType == "afkstatus").ToString(),
                    ["webBuckets"] = buckets.Count(b => b.BucketType == "web.tab.current").ToString()
                }),
            BuildComponent(
                "aw-events",
                "ActivityWatch raw events",
                issues,
                new Dictionary<string, string>
                {
                    ["windowEvents"] = awEvents.Count(e => e.EventType == "window").ToString(),
                    ["afkEvents"] = awEvents.Count(e => e.EventType == "afk").ToString(),
                    ["webEvents"] = awEvents.Count(e => e.EventType == "web" || e.BucketType == "web.tab.current").ToString()
                }),
            BuildComponent(
                "keystats-samples",
                "KeyStats minute samples",
                issues,
                new Dictionary<string, string>
                {
                    ["samples"] = samples.Count.ToString(),
                    ["devices"] = samples.Select(s => s.PimDeviceId).Distinct().Count().ToString(),
                    ["latestSampleAt"] = samples.OrderByDescending(s => s.SampledAtUtc).FirstOrDefault()?.SampledAtUtc.ToString("O") ?? ""
                }),
            BuildComponent(
                "daemon-upload",
                "Windows daemon upload",
                issues,
                new Dictionary<string, string>
                {
                    ["deviceId"] = heartbeat?.DeviceId ?? "",
                    ["receivedAt"] = heartbeat?.ReceivedAt.ToString("O") ?? "",
                    ["lastSuccessfulUploadAt"] = heartbeat?.LastSuccessfulUploadAt?.ToString("O") ?? "",
                    ["lastError"] = heartbeat?.LastError ?? "",
                    ["uploadQueueCount"] = heartbeat?.UploadQueueCount?.ToString() ?? ""
                }),
            BuildComponent(
                "interpreted-timeline",
                "Interpreted timeline",
                issues,
                new Dictionary<string, string>
                {
                    ["rawAwEvents"] = awEvents.Count.ToString(),
                    ["windowFallbackAvailable"] = awEvents.Any(e => e.EventType == "window").ToString()
                })
        };

        var overall = components
            .Select(c => c.Status)
            .OrderByDescending(GetSeverityRank)
            .FirstOrDefault(PimHealthStatus.Unknown);
        var nextSteps = issues
            .Select(i => i.NextStep)
            .Where(step => !string.IsNullOrWhiteSpace(step))
            .Distinct(StringComparer.Ordinal)
            .Select(step => step!)
            .ToList();

        return new PcQualityResponse(
            overall,
            GetLabel(overall),
            GetMessage(overall),
            checkedAt,
            components,
            issues,
            nextSteps);
    }

    private static void AddBucketIssues(
        List<AwBucketEntity> buckets,
        DateTimeOffset checkedAt,
        List<PcQualityIssueDto> issues)
    {
        if (!buckets.Any(b => b.BucketType == "currentwindow"))
        {
            issues.Add(new PcQualityIssueDto(
                "missing-aw-window-bucket",
                PimHealthStatus.Critical,
                "aw-buckets",
                "ActivityWatch window bucket has not been seen.",
                "Start ActivityWatch and confirm aw-watcher-window is running."));
        }

        if (!buckets.Any(b => b.BucketType == "afkstatus"))
        {
            issues.Add(new PcQualityIssueDto(
                "missing-aw-afk-bucket",
                PimHealthStatus.Warning,
                "aw-buckets",
                "ActivityWatch AFK bucket has not been seen.",
                "Start ActivityWatch and confirm aw-watcher-afk is running."));
        }

        if (!buckets.Any(b => b.BucketType == "web.tab.current"))
        {
            issues.Add(new PcQualityIssueDto(
                "missing-aw-web-bucket",
                PimHealthStatus.Warning,
                "aw-buckets",
                "ActivityWatch browser page bucket has not been seen.",
                "Install or enable the ActivityWatch browser extension if page-level history is needed."));
        }

        foreach (var bucket in buckets.Where(b => checkedAt - b.SeenAt > WarningBucketSeenAge))
        {
            issues.Add(new PcQualityIssueDto(
                "stale-aw-bucket",
                PimHealthStatus.Warning,
                "aw-buckets",
                $"ActivityWatch bucket {bucket.BucketId} has not been seen recently.",
                "Run the Windows daemon sync and confirm ActivityWatch is reachable."));
        }
    }

    private static void AddAwEventIssues(List<AwEventEntity> awEvents, List<PcQualityIssueDto> issues)
    {
        if (awEvents.Count == 0)
        {
            issues.Add(new PcQualityIssueDto(
                "no-aw-events-in-range",
                PimHealthStatus.Warning,
                "aw-events",
                "No ActivityWatch events were found in the selected range.",
                "Confirm the daemon is uploading ActivityWatch events or run ActivityWatch backfill."));
            return;
        }

        if (!awEvents.Any(e => e.EventType == "window"))
        {
            issues.Add(new PcQualityIssueDto(
                "no-window-events-in-range",
                PimHealthStatus.Warning,
                "aw-events",
                "No window events were found in the selected range.",
                "Confirm aw-watcher-window is running and trigger a daemon sync."));
        }

        var missingSourceIdCount = awEvents.Count(e => e.SourceEventId is null);
        if (missingSourceIdCount > 0)
        {
            var severity = missingSourceIdCount > awEvents.Count / 2
                ? PimHealthStatus.Critical
                : PimHealthStatus.Warning;
            issues.Add(new PcQualityIssueDto(
                "aw-events-missing-source-id",
                severity,
                "aw-events",
                $"{missingSourceIdCount} ActivityWatch events are missing source_event_id.",
                "Run ActivityWatch backfill so legacy rows can be replaced by source-event-id based facts."));
        }

        var invalidJsonCount = awEvents.Count(e => !HasUsableJson(e.DataJson));
        if (invalidJsonCount > 0)
        {
            var severity = invalidJsonCount > awEvents.Count / 2
                ? PimHealthStatus.Critical
                : PimHealthStatus.Warning;
            issues.Add(new PcQualityIssueDto(
                "aw-events-invalid-data-json",
                severity,
                "aw-events",
                $"{invalidJsonCount} ActivityWatch events have missing or invalid data_json.",
                "Inspect raw ActivityWatch uploads and rerun the daemon sync."));
        }
    }

    private static void AddKeyStatsIssues(List<KeystatsSampleEntity> samples, List<PcQualityIssueDto> issues)
    {
        if (samples.Count == 0)
        {
            issues.Add(new PcQualityIssueDto(
                "no-keystats-samples-in-range",
                PimHealthStatus.Critical,
                "keystats-samples",
                "No KeyStats minute samples were found in the selected range.",
                "Start KeyStats and wait at least two minutes for daemon sampling."));
            return;
        }

        var gapCount = 0;
        var resetCount = 0;
        foreach (var group in samples.GroupBy(s => s.PimDeviceId))
        {
            KeystatsSampleEntity? previous = null;
            foreach (var sample in group.OrderBy(s => s.SampledAtUtc))
            {
                if (previous is not null)
                {
                    var delta = KeystatsDeltaCalculator.Calculate(previous, sample);
                    if (delta.IsGap)
                        gapCount++;
                    if (delta.IsReset)
                        resetCount++;
                }

                previous = sample;
            }
        }

        if (gapCount > 0)
        {
            issues.Add(new PcQualityIssueDto(
                "keystats-sample-gap",
                PimHealthStatus.Warning,
                "keystats-samples",
                $"{gapCount} KeyStats sample gaps exceeded two minutes.",
                "Keep the daemon running and check whether KeyStats or the computer was paused."));
        }

        if (resetCount > 0)
        {
            issues.Add(new PcQualityIssueDto(
                "keystats-counter-reset",
                PimHealthStatus.Warning,
                "keystats-samples",
                $"{resetCount} KeyStats counter resets were detected.",
                "Treat the affected input-minute deltas as low quality for this range."));
        }
    }

    private static void AddDaemonIssues(
        DaemonHeartbeatEntity? heartbeat,
        DateTimeOffset checkedAt,
        List<PcQualityIssueDto> issues)
    {
        if (heartbeat is null)
        {
            issues.Add(new PcQualityIssueDto(
                "missing-windows-daemon-heartbeat",
                PimHealthStatus.Unknown,
                "daemon-upload",
                "Windows daemon heartbeat has not been received.",
                "Start and log in to the Windows daemon."));
            return;
        }

        var age = checkedAt - heartbeat.ReceivedAt;
        if (age >= CriticalHeartbeatAge)
        {
            issues.Add(new PcQualityIssueDto(
                "stale-windows-daemon-heartbeat",
                PimHealthStatus.Critical,
                "daemon-upload",
                "Windows daemon heartbeat is stale.",
                "Restart the Windows daemon and verify it can reach the local API."));
        }
        else if (age >= WarningHeartbeatAge)
        {
            issues.Add(new PcQualityIssueDto(
                "old-windows-daemon-heartbeat",
                PimHealthStatus.Warning,
                "daemon-upload",
                "Windows daemon heartbeat is old.",
                "Check whether the Windows daemon is still running."));
        }

        if (!string.IsNullOrWhiteSpace(heartbeat.LastError))
        {
            issues.Add(new PcQualityIssueDto(
                "windows-daemon-last-error",
                PimHealthStatus.Warning,
                "daemon-upload",
                $"Windows daemon reported an upload error: {heartbeat.LastError}",
                "Open the daemon status window and run a manual sync."));
        }

        if (heartbeat.UploadQueueCount is > 0)
        {
            issues.Add(new PcQualityIssueDto(
                "windows-daemon-upload-queue",
                PimHealthStatus.Warning,
                "daemon-upload",
                $"Windows daemon has {heartbeat.UploadQueueCount} queued upload items.",
                "Keep the daemon online until the queue drains."));
        }

        if (heartbeat.ActivityWatchState == DaemonSourceState.Unavailable.ToString())
        {
            issues.Add(new PcQualityIssueDto(
                "activitywatch-unavailable",
                PimHealthStatus.Warning,
                "daemon-upload",
                "Windows daemon reports ActivityWatch is unavailable.",
                "Start ActivityWatch on http://127.0.0.1:5600."));
        }

        if (heartbeat.KeyStatsState == DaemonSourceState.Unavailable.ToString())
        {
            issues.Add(new PcQualityIssueDto(
                "keystats-unavailable",
                PimHealthStatus.Warning,
                "daemon-upload",
                "Windows daemon reports KeyStats is unavailable.",
                "Start KeyStats on http://127.0.0.1:18080."));
        }
    }

    private static PcQualityComponentDto BuildComponent(
        string key,
        string name,
        List<PcQualityIssueDto> allIssues,
        IReadOnlyDictionary<string, string> details)
    {
        var componentIssues = allIssues
            .Where(i => i.ComponentKey == key)
            .ToList();
        var status = componentIssues.Count == 0
            ? PimHealthStatus.Healthy
            : componentIssues.Select(i => i.Severity).OrderByDescending(GetSeverityRank).First();
        var message = status == PimHealthStatus.Healthy
            ? $"{name} looks healthy."
            : componentIssues.OrderByDescending(i => GetSeverityRank(i.Severity)).First().Message;

        return new PcQualityComponentDto(key, name, status, message, details);
    }

    private static (DateTimeOffset Start, DateTimeOffset End) GetRange(DateTime? date, DateTime? dateFrom, DateTime? dateTo)
    {
        var startDate = (dateFrom ?? date ?? DateTime.Today).Date;
        var endDate = (dateTo ?? date ?? startDate).Date;
        if (endDate < startDate)
            (startDate, endDate) = (endDate, startDate);

        return (
            PcTrackerService.GetBusinessDayStartForQuery(startDate),
            PcTrackerService.GetBusinessDayStartForQuery(endDate).AddDays(1));
    }

    private static bool HasUsableJson(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
            return false;

        try
        {
            using var _ = System.Text.Json.JsonDocument.Parse(json);
            return true;
        }
        catch (System.Text.Json.JsonException)
        {
            return false;
        }
    }

    private static int GetSeverityRank(PimHealthStatus status)
        => status switch
        {
            PimHealthStatus.Healthy => 0,
            PimHealthStatus.Unknown => 1,
            PimHealthStatus.Warning => 2,
            PimHealthStatus.Critical => 3,
            _ => 1
        };

    private static string GetLabel(PimHealthStatus status)
        => status switch
        {
            PimHealthStatus.Healthy => "正常",
            PimHealthStatus.Warning => "有警告",
            PimHealthStatus.Critical => "故障",
            _ => "未知"
        };

    private static string GetMessage(PimHealthStatus status)
        => status switch
        {
            PimHealthStatus.Healthy => "PC facts look complete for the selected range.",
            PimHealthStatus.Warning => "PC facts are usable, but some collection quality issues need attention.",
            PimHealthStatus.Critical => "PC facts are not reliable enough for the selected range.",
            _ => "PC facts quality cannot be fully determined yet."
        };
}
```

- [ ] **Step 5: Run tests to verify bucket checks pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerQualityServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs
git commit -m "feat(pc): add facts quality service"
```

---

### Task 2: Cover KeyStats Gaps, Reset, Daemon, And Raw Completeness

**Files:**

- Modify: `tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs`

- [ ] **Step 1: Add failing quality issue tests**

Append these tests inside `PcTrackerQualityServiceTests` before the helper methods:

```csharp
    [Fact]
    public async Task GetQualityAsync_ReturnsWarning_ForKeyStatsGapAndReset()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        AddRecentWindowsDaemon(db);
        AddBucket(db, "aw-watcher-window_DESKTOP", "currentwindow");
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddBucket(db, "aw-watcher-web-edge_DESKTOP", "web.tab.current");
        AddWindowEvent(db, "2026-05-20T05:00:00+00:00");
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 20);
        AddKeyStatsSample(db, "2026-05-20T05:04:00+00:00", keys: 30);
        AddKeyStatsSample(db, "2026-05-20T05:05:00+00:00", keys: 10);
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(new DateTime(2026, 5, 20), null, null, CancellationToken.None);

        Assert.Equal(PimHealthStatus.Warning, quality.OverallStatus);
        Assert.Contains(quality.Issues, i => i.Code == "keystats-sample-gap");
        Assert.Contains(quality.Issues, i => i.Code == "keystats-counter-reset");
    }

    [Fact]
    public async Task GetQualityAsync_ReturnsCompletenessIssue_ForLegacyAwRows()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        AddRecentWindowsDaemon(db);
        AddBucket(db, "aw-watcher-window_DESKTOP", "currentwindow");
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddBucket(db, "aw-watcher-web-edge_DESKTOP", "web.tab.current");
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 10);
        AddKeyStatsSample(db, "2026-05-20T05:01:00+00:00", keys: 15);
        db.Set<AwEventEntity>().Add(new AwEventEntity
        {
            DeviceId = "DESKTOP",
            Timestamp = DateTimeOffset.Parse("2026-05-20T05:00:00+00:00"),
            Duration = 60,
            EventType = "window",
            AppName = "code.exe",
            WindowTitle = "Legacy",
            BucketId = "aw-watcher-window_DESKTOP",
            BucketType = "currentwindow",
            SourceEventId = null,
            DataJson = ""
        });
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(new DateTime(2026, 5, 20), null, null, CancellationToken.None);

        Assert.Contains(quality.Issues, i => i.Code == "aw-events-missing-source-id");
        Assert.Contains(quality.Issues, i => i.Code == "aw-events-invalid-data-json");
    }

    [Fact]
    public async Task GetQualityAsync_ReturnsCritical_WhenDaemonHeartbeatIsStale()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        db.DaemonHeartbeats.Add(new DaemonHeartbeatEntity
        {
            DeviceId = "DESKTOP",
            DaemonKind = "windows",
            Version = "1.0.0",
            ServerUrl = "http://127.0.0.1:5858",
            ActivityWatchState = DaemonSourceState.Available.ToString(),
            KeyStatsState = DaemonSourceState.Available.ToString(),
            StatusJson = "{}",
            ReceivedAt = DateTimeOffset.UtcNow.AddHours(-2)
        });
        AddBucket(db, "aw-watcher-window_DESKTOP", "currentwindow");
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddBucket(db, "aw-watcher-web-edge_DESKTOP", "web.tab.current");
        AddWindowEvent(db, "2026-05-20T05:00:00+00:00");
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 10);
        AddKeyStatsSample(db, "2026-05-20T05:01:00+00:00", keys: 15);
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(new DateTime(2026, 5, 20), null, null, CancellationToken.None);

        Assert.Equal(PimHealthStatus.Critical, quality.OverallStatus);
        Assert.Contains(quality.Issues, i => i.Code == "stale-windows-daemon-heartbeat");
    }
```

- [ ] **Step 2: Run tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerQualityServiceTests
```

Expected: PASS if Task 1 implementation already included the gap, reset, daemon, and raw completeness logic. If a test fails, update `PcTrackerQualityService` using the code from Task 1 Step 4 as the source of truth, then rerun.

- [ ] **Step 3: Add interpreted timeline test**

Append this test inside `PcTrackerQualityServiceTests` before the helper methods:

```csharp
    [Fact]
    public async Task GetQualityAsync_ReturnsHealthy_WhenFactsAreComplete()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        await using var db = new PimDbContext(options);
        AddRecentWindowsDaemon(db);
        AddBucket(db, "aw-watcher-window_DESKTOP", "currentwindow");
        AddBucket(db, "aw-watcher-afk_DESKTOP", "afkstatus");
        AddBucket(db, "aw-watcher-web-edge_DESKTOP", "web.tab.current");
        AddWindowEvent(db, "2026-05-20T05:00:00+00:00");
        db.Set<AwEventEntity>().Add(new AwEventEntity
        {
            DeviceId = "DESKTOP",
            Timestamp = DateTimeOffset.Parse("2026-05-20T05:01:00+00:00"),
            Duration = 60,
            EventType = "afk",
            AfkStatus = "not-afk",
            BucketId = "aw-watcher-afk_DESKTOP",
            BucketType = "afkstatus",
            SourceEventId = 200,
            DataJson = "{\"status\":\"not-afk\"}"
        });
        AddKeyStatsSample(db, "2026-05-20T05:00:00+00:00", keys: 10);
        AddKeyStatsSample(db, "2026-05-20T05:01:00+00:00", keys: 15);
        await db.SaveChangesAsync();

        var service = new PcTrackerQualityService(db);
        var quality = await service.GetQualityAsync(new DateTime(2026, 5, 20), null, null, CancellationToken.None);

        Assert.Equal(PimHealthStatus.Healthy, quality.OverallStatus);
        Assert.Empty(quality.Issues);
        Assert.Empty(quality.NextSteps);
        Assert.Contains(quality.Components, c => c.Key == "interpreted-timeline" && c.Status == PimHealthStatus.Healthy);
    }
```

- [ ] **Step 4: Run tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerQualityServiceTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs
git commit -m "test(pc): cover facts quality issues"
```

---

### Task 3: Expose `/api/v1/pc/quality`

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs`

- [ ] **Step 1: Add endpoint path contract test**

Append this test inside `PcTrackerQualityServiceTests` before the helper methods:

```csharp
    [Fact]
    public void PcTrackerModule_ExposesQualityEndpointInSource()
    {
        var source = File.ReadAllText(Path.Combine(
            AppContext.BaseDirectory,
            "..",
            "..",
            "..",
            "..",
            "..",
            "src",
            "modules",
            "Pim.Module.PcTracker",
            "PcTrackerModule.cs"));

        Assert.Contains("MapGet(\"/quality\"", source);
        Assert.Contains("PcTrackerQualityService", source);
    }
```

- [ ] **Step 2: Run endpoint contract test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerModule_ExposesQualityEndpointInSource
```

Expected: FAIL because `PcTrackerModule.cs` does not yet reference `PcTrackerQualityService` or `/quality`.

- [ ] **Step 3: Register quality service**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, inside `RegisterServices`, add this line after `services.AddScoped<PcTrackerService>();`:

```csharp
services.AddScoped<PcTrackerQualityService>();
```

- [ ] **Step 4: Map the quality endpoint**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, add this endpoint after the existing `/detail` endpoint:

```csharp
        readGroup.MapGet("/quality", async (
            [FromQuery] string? date,
            [FromQuery] string? dateFrom,
            [FromQuery] string? dateTo,
            [FromServices] PcTrackerQualityService svc,
            CancellationToken ct) =>
        {
            var result = await svc.GetQualityAsync(
                TryParseDate(date),
                TryParseDate(dateFrom),
                TryParseDate(dateTo),
                ct);
            return Results.Ok(ApiResponse<PcQualityResponse>.Ok(result));
        });
```

Then add this private helper near the bottom of `PcTrackerModule` before `NeedsClassificationSuggestion`:

```csharp
    private static DateTime? TryParseDate(string? value)
    {
        return DateTime.TryParse(value, out var parsed)
            ? parsed.Date
            : null;
    }
```

If the compiler cannot find `PcQualityResponse`, confirm that `PcTrackerModule.cs` already has `using Pim.Module.PcTracker.DTOs;`. It does in the current code.

- [ ] **Step 5: Run endpoint contract test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerModule_ExposesQualityEndpointInSource
```

Expected: PASS.

- [ ] **Step 6: Run PC tracker service tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTracker
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs
git commit -m "feat(pc): expose facts quality endpoint"
```

---

### Task 4: Add Frontend Quality API Types And Normalization

**Files:**

- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/pcTracker.ts`
- Create: `tests/client-web/pcQualityApiNormalization.test.ts`

- [ ] **Step 1: Write failing frontend normalization test**

Create `tests/client-web/pcQualityApiNormalization.test.ts`:

```ts
import assert from 'node:assert/strict';
import { normalizePcQuality } from '../../src/client-web/src/api/pcTracker';

const quality = normalizePcQuality({
  overallStatus: 2,
  label: 'Warning',
  message: 'needs attention',
  checkedAt: '2026-05-25T00:00:00Z',
  components: [
    {
      key: 'aw-buckets',
      name: 'ActivityWatch buckets',
      status: 3,
      message: 'missing window bucket',
      details: { windowBuckets: 0, webBuckets: 1 },
    },
    {
      key: 'unknown',
      name: null,
      status: 99,
      message: null,
      details: null,
    },
  ],
  issues: [
    {
      code: 'missing-aw-window-bucket',
      severity: 3,
      componentKey: 'aw-buckets',
      message: 'missing',
      nextStep: 'Start ActivityWatch',
    },
  ],
  nextSteps: ['Start ActivityWatch', 123],
});

assert.equal(quality.overallStatus, 'Warning');
assert.equal(quality.label, '有警告');
assert.equal(quality.components[0].status, 'Critical');
assert.equal(quality.components[1].status, 'Unknown');
assert.equal(quality.components[0].details.windowBuckets, '0');
assert.equal(quality.components[0].details.webBuckets, '1');
assert.deepEqual(quality.components[1].details, {});
assert.equal(quality.issues[0].severity, 'Critical');
assert.deepEqual(quality.nextSteps, ['Start ActivityWatch', '123']);
```

- [ ] **Step 2: Run frontend normalization test to verify it fails**

Run from the repository root:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\pcQualityApiNormalization.test.ts
```

Expected: FAIL because `normalizePcQuality` does not exist.

- [ ] **Step 3: Add PC quality types**

In `src/client-web/src/types/index.ts`, add these interfaces after `SystemStatusDetail`:

```ts
export interface PcQualityComponent {
  key: string;
  name: string;
  status: PimHealthStatus;
  message: string;
  details: Record<string, string>;
}

export interface PcQualityIssue {
  code: string;
  severity: PimHealthStatus;
  componentKey: string;
  message: string;
  nextStep: string | null;
}

export interface PcQualityResponse {
  overallStatus: PimHealthStatus;
  label: string;
  message: string;
  checkedAt: string;
  components: PcQualityComponent[];
  issues: PcQualityIssue[];
  nextSteps: string[];
}

export interface PcQualityQueryParams {
  date?: string;
  dateFrom?: string;
  dateTo?: string;
}
```

- [ ] **Step 4: Add quality API client and normalization**

Modify the import block in `src/client-web/src/api/pcTracker.ts` so it includes the new types:

```ts
import type {
  PcSummaryResponse, TimelineItem, HeatmapBucket,
  DetailQueryParams, DetailQueryResponse,
  AppCategoryRule, HeatmapGridResponse,
  ActivityClassificationRule, ActivityClassificationSuggestion,
  PcQualityResponse, PcQualityQueryParams, PcQualityComponent,
  PcQualityIssue, PimHealthStatus
} from '../types';
```

Then add this code after `queryPcDetail`:

```ts
const healthStatusByNumber: Record<number, PimHealthStatus> = {
  0: 'Unknown',
  1: 'Healthy',
  2: 'Warning',
  3: 'Critical',
};

const healthStatusNames = new Set<PimHealthStatus>(['Unknown', 'Healthy', 'Warning', 'Critical']);

const pcQualityStatusLabels: Record<PimHealthStatus, string> = {
  Unknown: '未知',
  Healthy: '正常',
  Warning: '有警告',
  Critical: '故障',
};

type RawPcQualityComponent = Omit<PcQualityComponent, 'status' | 'name' | 'message' | 'details'> & {
  status: unknown;
  name: unknown;
  message: unknown;
  details: unknown;
};

type RawPcQualityIssue = Omit<PcQualityIssue, 'severity' | 'message' | 'nextStep'> & {
  severity: unknown;
  message: unknown;
  nextStep: unknown;
};

type RawPcQuality = {
  overallStatus?: unknown;
  label?: unknown;
  message?: unknown;
  checkedAt?: unknown;
  components?: unknown;
  issues?: unknown;
  nextSteps?: unknown;
};

function normalizePcHealthStatus(value: unknown): PimHealthStatus {
  if (typeof value === 'number') return healthStatusByNumber[value] ?? 'Unknown';
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^\d+$/.test(trimmed)) return healthStatusByNumber[Number(trimmed)] ?? 'Unknown';
    if (healthStatusNames.has(trimmed as PimHealthStatus)) return trimmed as PimHealthStatus;
  }
  return 'Unknown';
}

function textOrEmpty(value: unknown): string {
  if (value === null || value === undefined) return '';
  return String(value);
}

function normalizeDetails(details: unknown): Record<string, string> {
  if (!details || typeof details !== 'object' || Array.isArray(details)) return {};
  return Object.fromEntries(
    Object.entries(details).map(([key, value]) => [key, textOrEmpty(value)])
  );
}

function normalizeQualityComponent(raw: unknown): PcQualityComponent {
  const component = (raw && typeof raw === 'object' ? raw : {}) as Partial<RawPcQualityComponent>;
  return {
    key: textOrEmpty(component.key),
    name: textOrEmpty(component.name),
    status: normalizePcHealthStatus(component.status),
    message: textOrEmpty(component.message),
    details: normalizeDetails(component.details),
  };
}

function normalizeQualityIssue(raw: unknown): PcQualityIssue {
  const issue = (raw && typeof raw === 'object' ? raw : {}) as Partial<RawPcQualityIssue>;
  return {
    code: textOrEmpty(issue.code),
    severity: normalizePcHealthStatus(issue.severity),
    componentKey: textOrEmpty(issue.componentKey),
    message: textOrEmpty(issue.message),
    nextStep: textOrEmpty(issue.nextStep) || null,
  };
}

export function normalizePcQuality(raw: unknown): PcQualityResponse {
  const quality = (raw && typeof raw === 'object' ? raw : {}) as RawPcQuality;
  const overallStatus = normalizePcHealthStatus(quality.overallStatus);
  const rawLabel = textOrEmpty(quality.label).trim();
  const label = healthStatusNames.has(rawLabel as PimHealthStatus)
    ? pcQualityStatusLabels[overallStatus]
    : rawLabel || pcQualityStatusLabels[overallStatus];

  return {
    overallStatus,
    label,
    message: textOrEmpty(quality.message),
    checkedAt: textOrEmpty(quality.checkedAt),
    components: Array.isArray(quality.components)
      ? quality.components.map(normalizeQualityComponent)
      : [],
    issues: Array.isArray(quality.issues)
      ? quality.issues.map(normalizeQualityIssue)
      : [],
    nextSteps: Array.isArray(quality.nextSteps)
      ? quality.nextSteps.map(textOrEmpty).filter(Boolean)
      : [],
  };
}

export function getPcQuality(params: PcQualityQueryParams = {}) {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') searchParams.set(key, String(value));
  });
  const query = searchParams.toString();
  const path = query ? `/pc/quality?${query}` : '/pc/quality';
  return apiGet<ApiResponse<unknown>>(path).then(r => normalizePcQuality(r.data));
}
```

- [ ] **Step 5: Run frontend normalization test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\pcQualityApiNormalization.test.ts
```

Expected: PASS.

- [ ] **Step 6: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/pcTracker.ts tests/client-web/pcQualityApiNormalization.test.ts
git commit -m "feat(web): add pc quality api client"
```

---

### Task 5: Add PC Quality Summary UI

**Files:**

- Create: `src/client-web/src/components/pc-tracker/PcQualitySummary.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Modify: `src/client-web/src/pages/StatusPage.tsx`

- [ ] **Step 1: Create the reusable summary component**

Create `src/client-web/src/components/pc-tracker/PcQualitySummary.tsx`:

```tsx
import type { PcQualityResponse, PimHealthStatus } from '../../types';
import StatusBadge from '../../ui/StatusBadge';

const toneByStatus: Record<PimHealthStatus, 'neutral' | 'primary' | 'warning' | 'danger'> = {
  Unknown: 'neutral',
  Healthy: 'primary',
  Warning: 'warning',
  Critical: 'danger',
};

function formatCheckedAt(value: string) {
  if (!value) return '未知';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
}

export default function PcQualitySummary({
  quality,
  isLoading,
  error,
  compact = false,
}: {
  quality: PcQualityResponse | undefined;
  isLoading?: boolean;
  error?: unknown;
  compact?: boolean;
}) {
  if (isLoading) {
    return (
      <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        正在检查 PC 数据质量...
      </section>
    );
  }

  if (error) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-4">
        <div className="text-sm font-semibold text-red-700">PC 数据质量暂不可用</div>
        <div className="mt-1 text-sm text-red-600">请稍后刷新重试。</div>
      </section>
    );
  }

  if (!quality) {
    return (
      <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-500">
        PC 数据质量暂无结果
      </section>
    );
  }

  const visibleIssues = quality.issues.slice(0, compact ? 2 : 4);
  const visibleNextSteps = quality.nextSteps.slice(0, compact ? 2 : 3);

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-sm font-semibold text-slate-950">PC 数据质量</h2>
            <StatusBadge tone={toneByStatus[quality.overallStatus]}>{quality.label}</StatusBadge>
          </div>
          <p className="mt-2 text-sm text-slate-600">{quality.message || '暂无质量说明'}</p>
          <p className="mt-1 text-xs text-slate-400">检查时间：{formatCheckedAt(quality.checkedAt)}</p>
        </div>
        <div className="shrink-0 text-right text-xs text-slate-500">
          <div>{quality.issues.length} 个问题</div>
          <div>{quality.components.length} 个组件</div>
        </div>
      </div>

      {visibleIssues.length > 0 && (
        <div className="mt-4 grid gap-2 md:grid-cols-2">
          {visibleIssues.map(issue => (
            <div key={`${issue.code}-${issue.componentKey}`} className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2">
              <div className="text-xs font-semibold text-amber-800">{issue.message}</div>
              {issue.nextStep && <div className="mt-1 text-xs text-amber-700">{issue.nextStep}</div>}
            </div>
          ))}
        </div>
      )}

      {visibleNextSteps.length > 0 && (
        <ul className="mt-3 space-y-1 text-xs text-slate-600">
          {visibleNextSteps.map((step, index) => (
            <li key={`${step}-${index}`}>{step}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
```

- [ ] **Step 2: Wire PC Tracker page**

Modify imports in `src/client-web/src/pages/PcTrackerPage.tsx`:

```tsx
import { getPcSummary, getPcHeatmapGrid, getPcQuality } from '../api/pcTracker';
import PcQualitySummary from '../components/pc-tracker/PcQualitySummary';
```

Add this query after the existing `pc-summary` query:

```tsx
  const {
    data: quality,
    isLoading: qualityLoading,
    error: qualityError,
  } = useQuery({
    queryKey: ['pc-quality', dateStr],
    queryFn: () => getPcQuality({ date: dateStr }),
    refetchInterval: 30000,
  });
```

Add this component immediately after the `PageHeader`:

```tsx
      <PcQualitySummary quality={quality} isLoading={qualityLoading} error={qualityError} />
```

- [ ] **Step 3: Wire Status page**

Modify imports in `src/client-web/src/pages/StatusPage.tsx`:

```tsx
import { getPcQuality } from '../api/pcTracker';
import PcQualitySummary from '../components/pc-tracker/PcQualitySummary';
```

Inside `StatusPage`, after the existing `useQuery` call for `status-detail`, add:

```tsx
  const {
    data: pcQuality,
    isLoading: pcQualityLoading,
    error: pcQualityError,
  } = useQuery({
    queryKey: ['status-pc-quality'],
    queryFn: () => getPcQuality(),
    refetchInterval: 60_000,
  });
```

Inside the rendered `<>...</>` block after the overall status section and before `data.nextSteps.length > 0`, add:

```tsx
          <PcQualitySummary
            quality={pcQuality}
            isLoading={pcQualityLoading}
            error={pcQualityError}
            compact
          />
```

- [ ] **Step 4: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/components/pc-tracker/PcQualitySummary.tsx src/client-web/src/pages/PcTrackerPage.tsx src/client-web/src/pages/StatusPage.tsx
git commit -m "feat(web): show pc facts quality"
```

---

### Task 6: Improve Detail Empty State With Quality Reasons

**Files:**

- Modify: `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`

- [ ] **Step 1: Add quality imports**

Modify imports at the top of `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`:

```tsx
import { queryPcDetail, getPcQuality } from '../../api/pcTracker';
import type { DetailQueryParams, PcDetailRecord, PcQualityResponse } from '../../types';
```

- [ ] **Step 2: Add empty state helper**

Add this function above `export default function PcDetailQueryPanel()`:

```tsx
function getEmptyStateText(quality: PcQualityResponse | undefined) {
  if (!quality) return '暂无数据';

  const codes = new Set(quality.issues.map(issue => issue.code));
  if (codes.has('no-keystats-samples-in-range')) return '查询范围内没有 KeyStats 分钟样本';
  if (codes.has('no-aw-events-in-range')) return '查询范围内没有 ActivityWatch 原始事件';
  if (codes.has('missing-aw-window-bucket')) return 'ActivityWatch 窗口采集源不可用';
  if (codes.has('missing-windows-daemon-heartbeat')) return 'Windows daemon 尚未上报状态';
  if (quality.overallStatus === 'Unknown') return '新环境可能仍在等待采样';
  return '暂无数据';
}
```

- [ ] **Step 3: Fetch quality for empty-state ranges**

Inside `PcDetailQueryPanel`, after the existing detail `useQuery`, add:

```tsx
  const { data: quality } = useQuery({
    queryKey: ['pc-detail-quality', params.dateFrom, params.dateTo],
    queryFn: () => getPcQuality({ dateFrom: params.dateFrom, dateTo: params.dateTo }),
    enabled: !data || data.items.length === 0,
  });
```

- [ ] **Step 4: Replace the empty state text**

In the table area, replace:

```tsx
<div className="py-8 text-center text-gray-400">暂无数据</div>
```

with:

```tsx
<div className="py-8 text-center text-gray-400">{getEmptyStateText(quality)}</div>
```

- [ ] **Step 5: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx
git commit -m "feat(web): explain empty pc detail results"
```

---

### Task 7: Add Stage 1 Acceptance Matrix And Runbook

**Files:**

- Create: `docs/operations/pc-facts-stage1-acceptance.md`

- [ ] **Step 1: Create acceptance document**

Create `docs/operations/pc-facts-stage1-acceptance.md`:

```markdown
# PC Facts Stage 1 Acceptance

This document verifies the Stage 1 PC facts layer. It checks whether PIM can reliably preserve what happened on the computer before classification, AI, review, or scheduling logic depends on it.

## Acceptance Matrix

| Roadmap requirement | Current evidence | Status | Stage 1 gap closure |
| --- | --- | --- | --- |
| Save ActivityWatch bucket metadata | `pc_aw_buckets`, `AwBucketEntity`, `/api/v1/pc/aw/upload-complete` | Satisfied | Quality API checks missing or stale buckets. |
| Save ActivityWatch window events | `pc_aw_events`, `AwEventEntity.EventType = "window"` | Satisfied | Quality API checks window events in range. |
| Save ActivityWatch afk events | `pc_aw_events`, `AwEventEntity.EventType = "afk"` | Satisfied | Quality API checks AFK bucket visibility. |
| Save browser page events | `web.tab.current` bucket support and `EventType = "web"` | Satisfied | Missing web bucket is warning, not failure. |
| Save source event id | `AwEventEntity.SourceEventId` and unique index | Satisfied for complete uploads | Legacy rows without source id are reported as completeness issues. |
| Save raw data JSON | `AwEventEntity.DataJson`, `KeystatsSampleEntity.RawJson` | Satisfied | Invalid or missing AW JSON is reported as quality issue. |
| Save KeyStats daily compatibility data | `/api/v1/pc/keystats/upload` and daily entities | Satisfied | Kept as compatibility path. |
| Save KeyStats minute snapshots | `pc_keystats_samples`, `/api/v1/pc/keystats/samples` | Satisfied | Quality API checks sample presence and latest sample time. |
| Calculate or query KeyStats minute delta | `KeystatsDeltaCalculator` and detail `input-minute` records | Satisfied | Quality API reports gaps and resets. |
| Detect collection gaps | Existing delta flags plus new quality service | Satisfied after this stage | `/api/v1/pc/quality` exposes gap issues. |
| Support ActivityWatch backfill | Windows daemon `BackfillAsync` path | Satisfied | Manual runbook includes recent 14 day backfill check. |
| Provide raw data query | `/api/v1/pc/detail?view=raw` | Satisfied | Runbook includes raw detail query. |
| Provide interpreted timeline query | `/api/v1/pc/detail` and `/api/v1/pc/aw/timeline` | Satisfied | Quality API includes interpreted timeline component. |
| Avoid browser window/page double counting | `BrowserPageTimelineBuilder` tests | Satisfied | Existing tests remain part of verification. |
| Show data quality status | `/api/v1/pc/quality`, PC page, Status page | Satisfied after this stage | Web displays server-owned quality summary. |
| Report daemon upload health | `daemon_heartbeats` and status page | Satisfied | Quality API reads daemon heartbeat details. |

## Local Verification Commands

Run backend tests:

```powershell
dotnet test Pim.sln
```

Build the web client:

```powershell
npm --prefix src/client-web run build
```

Check current git state before pushing:

```powershell
git status --short --branch
```

## Manual Runtime Checks

Start the API and supporting services:

```powershell
docker compose up -d postgres minio tika
dotnet run --project src/Pim.Api/Pim.Api.csproj
```

Start the web client:

```powershell
npm --prefix src/client-web run dev
```

Start or restart the Windows daemon from a built Debug output, or launch it from the IDE. Confirm it is configured for:

```text
http://127.0.0.1:5858
```

Check liveness:

```powershell
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/health"
```

Check PC facts quality for today:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/quality?date=$today"
```

Check interpreted detail records:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=$today&dateTo=$today&pageSize=20"
```

Check raw detail records:

```powershell
$today = Get-Date -Format "yyyy-MM-dd"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=$today&dateTo=$today&view=raw&pageSize=20"
```

Wait at least two minutes after starting the daemon, then check that KeyStats samples exist:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "select count(*), max(sampled_at_utc) from pc_keystats_samples;"
```

Check ActivityWatch bucket and event persistence:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "select bucket_id, type, seen_at from pc_aw_buckets order by seen_at desc;"
docker compose exec -T postgres psql -U pim -d pim -c "select event_type, count(*) from pc_aw_events group by event_type order by event_type;"
docker compose exec -T postgres psql -U pim -d pim -c "select count(*) as rows, count(source_event_id) as rows_with_source_id from pc_aw_events;"
```

Trigger ActivityWatch recent backfill from the daemon UI if recent history is missing. After backfill, rerun the quality and raw detail checks.

## Web Checks

Open the web client and verify:

- PC 记录 page shows the PC 数据质量 panel.
- 状态信息 page shows the PC 数据质量 panel.
- PC detailed data page distinguishes empty data from unavailable collection sources.
- Raw view can show original window, web, and afk records.
- Interpreted view does not double count browser pages and browser windows.

## Common Failure Handling

ActivityWatch unavailable:

- Start ActivityWatch.
- Confirm `http://127.0.0.1:5600/api/0/buckets/` opens locally.
- Run daemon manual sync.

Browser page bucket missing:

- Install or enable the ActivityWatch browser extension.
- Keep using window records as fallback until the bucket appears.

KeyStats unavailable:

- Start KeyStats.
- Confirm `http://127.0.0.1:18080/api/stats/` opens locally.
- Wait at least two minutes for minute samples.

Windows daemon heartbeat missing:

- Start the daemon.
- Confirm login token is valid.
- Confirm server URL is `http://127.0.0.1:5858`.

Upload failures:

- Open daemon status.
- Run manual sync.
- Check `/api/v1/status` and `/api/v1/pc/quality`.
```

- [ ] **Step 2: Run markdown sanity check**

Run:

```powershell
$pattern = ('TB' + 'D|TO' + 'DO|待' + '定|占' + '位|FIX' + 'ME')
rg -n $pattern docs/operations/pc-facts-stage1-acceptance.md
```

Expected: no output and exit code 1.

- [ ] **Step 3: Commit**

```powershell
git add docs/operations/pc-facts-stage1-acceptance.md
git commit -m "docs: add pc facts stage 1 acceptance"
```

---

### Task 8: Final Verification

**Files:**

- Verify only unless a command fails.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
dotnet test Pim.sln
```

Expected: all tests pass.

- [ ] **Step 2: Run frontend normalization tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\statusApiNormalization.test.ts
npm --prefix src/client-web exec tsx -- ..\..\tests\client-web\pcQualityApiNormalization.test.ts
```

Expected: both commands pass with no assertion errors.

- [ ] **Step 3: Build frontend**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: TypeScript and Vite build pass.

- [ ] **Step 4: Check generated-output hygiene**

Run:

```powershell
git status --short --branch
```

Expected: only intentional source, test, and docs changes are present. Generated `src/Pim.Api/wwwroot` build output, `bin`, `obj`, `dist`, `build`, and publish artifacts must not be staged.

- [ ] **Step 5: Inspect staged files before any push**

Run:

```powershell
git diff --stat HEAD
git diff --check
```

Expected: changed files match the task commits, and `git diff --check` reports no whitespace errors.

## Self-Review Checklist

- Spec coverage: Tasks cover quality DTOs, service, API, Web PC page, Web Status page, detail empty states, backend tests, frontend normalization tests, and acceptance documentation.
- Scope control: The plan does not rebuild ActivityWatch or KeyStats collection and does not enter Stage 2 classification or LLM work.
- Type consistency: Backend uses `PcQualityResponse`, `PcQualityComponentDto`, `PcQualityIssueDto`, and `PcTrackerQualityService`; frontend uses `PcQualityResponse`, `PcQualityComponent`, `PcQualityIssue`, and `getPcQuality`.
- Verification: Backend tests, frontend normalization tests, frontend build, and final git hygiene checks are included.
