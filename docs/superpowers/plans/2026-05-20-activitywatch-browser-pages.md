# ActivityWatch Browser Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ActivityWatch browser page capture and an interpreted PC timeline that explains browser time with concrete page records while leaving KeyStats/input behavior unchanged.

**Architecture:** Keep ActivityWatch raw events as the source of truth, extend upload classification to store `web.tab.current` events as raw `web` records, and build interpreted `web-page` detail records at query time. The Windows daemon discovers supported ActivityWatch buckets by metadata and explicitly excludes `aw-watcher-input` / `os.hid.input`. The frontend adds browser page filters and displays page title/domain by default while preserving raw JSON drilldown.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core, WPF Windows daemon core services, React 19 + TypeScript + TanStack Query, xUnit.

---

## Current Workspace Notes

- The branch is `codex/pc-tracker-complete-capture`.
- There are existing uncommitted PC tracker changes in source and tests. Treat them as baseline work in progress. Do not revert them.
- This plan is browser-page focused only. Do not modify KeyStats collection, KeyStats upload contracts, keyboard heatmap behavior, or input-minute/app-input/key-input calculations except where a method signature must pass existing values through unchanged.

## Reference Documents

- Spec: `docs/superpowers/specs/2026-05-20-activitywatch-browser-pages-design.md`
- Existing plan that created the complete capture baseline: `docs/superpowers/plans/2026-05-20-pc-tracker-complete-capture.md`

## File Structure

Create:

- `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`: pure backend timeline synthesis helper for raw web/window events.
- `tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs`: daemon bucket selection tests.
- `src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs`: daemon-side supported bucket filtering.

Modify:

- `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`: add browser query fields and optional page fields to `PcDetailRecord`.
- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: bind browser query fields from `/api/v1/pc/detail`.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`: classify raw `web.tab.current` events, compose interpreted detail records, support raw view, and use interpreted timeline records for summary/timeline.
- `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`: discover supported buckets, upload web buckets, exclude input buckets, and track cursor per bucket.
- `src/client-web/src/types/index.ts`: add browser detail fields and query params.
- `src/client-web/src/api/pcTracker.ts`: query string already serializes arbitrary params; keep it unchanged unless TypeScript needs imports adjusted.
- `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`: add browser filters, Chinese event labels, web-page display fields, and raw JSON details.
- `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`: add backend browser storage and synthesis tests.

## Task 1: Store ActivityWatch Web Events as Raw Web Records

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Write the failing web upload test**

Append this test to `PcTrackerCompleteCaptureTests` before `MakeDetailQuery()`:

```csharp
[Fact]
public async Task UploadCompleteAwEventsAsync_StoresWebTabCurrentAsWebEvent()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    var service = new PcTrackerService(db);
    var request = new CompleteAwUploadRequest(
        "DESKTOP",
        new AwInfoDto("DESKTOP", "v0.13.2", false, "aw-device"),
        new AwBucketDto(
            "aw-watcher-web-edge_DESKTOP",
            null,
            "web.tab.current",
            "aw-client-web",
            "DESKTOP",
            "2026-05-20T00:00:00+00:00",
            "2026-05-20T05:00:00+00:00",
            new Dictionary<string, object>()),
        new List<CompleteAwEventEntry>
        {
            new(200, "2026-05-20T05:00:00+00:00", 8.0, new Dictionary<string, object>
            {
                ["url"] = "https://docs.activitywatch.net/en/latest/api/rest.html",
                ["title"] = "REST API",
                ["audible"] = false,
                ["incognito"] = false,
                ["tabCount"] = 12
            })
        });

    Assert.Equal(1, await service.UploadCompleteAwEventsAsync(request, CancellationToken.None));

    var saved = Assert.Single(db.Set<AwEventEntity>());
    Assert.Equal("web", saved.EventType);
    Assert.Equal("web.tab.current", saved.BucketType);
    Assert.Null(saved.AppName);
    Assert.Equal("REST API", saved.WindowTitle);
    Assert.Contains("docs.activitywatch.net", saved.DataJson);
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UploadCompleteAwEventsAsync_StoresWebTabCurrentAsWebEvent
```

Expected: FAIL because `EventType` is currently stored as `window` for non-AFK buckets.

- [ ] **Step 3: Add event type classification**

In `PcTrackerService.UploadCompleteAwEventsCoreAsync`, replace the assignment:

```csharp
entity.EventType = req.Bucket.Type == "afkstatus" ? "afk" : "window";
```

with:

```csharp
entity.EventType = ClassifyAwEventType(req.Bucket.Type);
```

Add this helper near `GetString`:

```csharp
private static string ClassifyAwEventType(string bucketType)
{
    return bucketType switch
    {
        "afkstatus" => "afk",
        "web.tab.current" => "web",
        _ => "window"
    };
}
```

Keep the existing assignments:

```csharp
var app = GetString(data, "app");
var title = GetString(data, "title");
var status = GetString(data, "status");
```

For web events, this stores the page title in `WindowTitle` and the full page payload in `DataJson`.

- [ ] **Step 4: Run the web upload test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UploadCompleteAwEventsAsync_StoresWebTabCurrentAsWebEvent
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): store activitywatch browser page events"
```

## Task 2: Discover Supported ActivityWatch Buckets and Exclude Input

**Files:**

- Create: `src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs`

- [ ] **Step 1: Write bucket selection tests**

Create `tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs`:

```csharp
using Pim.Client.Core.Services;
using Xunit;

namespace Pim.UnitTests.ClientWindows;

public class AwBucketSelectionTests
{
    [Fact]
    public void IsSupportedUploadBucket_IncludesWindowAfkAndBrowserPages()
    {
        Assert.True(AwBucketSelection.IsSupportedUploadBucket("aw-watcher-window_DESKTOP", "currentwindow", "aw-watcher-window"));
        Assert.True(AwBucketSelection.IsSupportedUploadBucket("aw-watcher-afk_DESKTOP", "afkstatus", "aw-watcher-afk"));
        Assert.True(AwBucketSelection.IsSupportedUploadBucket("aw-watcher-web-edge_DESKTOP", "web.tab.current", "aw-client-web"));
    }

    [Fact]
    public void IsSupportedUploadBucket_ExcludesInputBuckets()
    {
        Assert.False(AwBucketSelection.IsSupportedUploadBucket("aw-watcher-input_DESKTOP", "os.hid.input", "aw-watcher-input"));
        Assert.False(AwBucketSelection.IsSupportedUploadBucket("aw-watcher-input_DESKTOP", "currentwindow", "aw-watcher-input"));
    }

    [Fact]
    public void DescribeBucketKind_ReturnsStableLogLabels()
    {
        Assert.Equal("window", AwBucketSelection.DescribeBucketKind("currentwindow"));
        Assert.Equal("afk", AwBucketSelection.DescribeBucketKind("afkstatus"));
        Assert.Equal("web", AwBucketSelection.DescribeBucketKind("web.tab.current"));
        Assert.Equal("unknown", AwBucketSelection.DescribeBucketKind("other"));
    }
}
```

- [ ] **Step 2: Run the failing selection tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~AwBucketSelectionTests
```

Expected: FAIL because `AwBucketSelection` does not exist.

- [ ] **Step 3: Add the selection helper**

Create `src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs`:

```csharp
namespace Pim.Client.Core.Services;

public static class AwBucketSelection
{
    private static readonly HashSet<string> SupportedTypes = new(StringComparer.Ordinal)
    {
        "currentwindow",
        "afkstatus",
        "web.tab.current"
    };

    public static bool IsSupportedUploadBucket(string bucketId, string bucketType, string client)
    {
        if (string.Equals(bucketType, "os.hid.input", StringComparison.Ordinal))
            return false;

        if (string.Equals(client, "aw-watcher-input", StringComparison.Ordinal))
            return false;

        if (bucketId.StartsWith("aw-watcher-input_", StringComparison.Ordinal))
            return false;

        return SupportedTypes.Contains(bucketType);
    }

    public static string DescribeBucketKind(string bucketType)
    {
        return bucketType switch
        {
            "currentwindow" => "window",
            "afkstatus" => "afk",
            "web.tab.current" => "web",
            _ => "unknown"
        };
    }
}
```

- [ ] **Step 4: Run the selection tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~AwBucketSelectionTests
```

Expected: PASS.

- [ ] **Step 5: Update the collector cursor to support many buckets**

In `AwCollectorService`, replace the two fixed bucket constants:

```csharp
private static readonly string BucketId = $"aw-watcher-window_{Environment.MachineName}";
private static readonly string AfkBucketId = $"aw-watcher-afk_{Environment.MachineName}";
```

with no constants. Add this method:

```csharp
private async Task<List<AwBucketPayload>> FetchSupportedBucketsAsync()
{
    try
    {
        var buckets = await _aw.GetFromJsonAsync<Dictionary<string, AwBucketPayload>>("/api/0/buckets/", _cts.Token)
            ?? new Dictionary<string, AwBucketPayload>();

        var normalizedBuckets = buckets
            .Select(kv => EnsureBucketId(kv.Key, kv.Value))
            .ToList();

        foreach (var bucket in normalizedBuckets)
            _bucketCache[bucket.Id] = bucket;

        return normalizedBuckets
            .Where(b => AwBucketSelection.IsSupportedUploadBucket(b.Id, b.Type, b.Client))
            .OrderBy(b => b.Id, StringComparer.Ordinal)
            .ToList();
    }
    catch (Exception ex)
    {
        Log?.Invoke($"[AwCollector] Bucket discovery failed: {ex.Message}");
        return new List<AwBucketPayload>();
    }
}
```

Add this helper near `FetchSupportedBucketsAsync`:

```csharp
private static AwBucketPayload EnsureBucketId(string bucketId, AwBucketPayload bucket)
{
    return string.IsNullOrWhiteSpace(bucket.Id)
        ? new AwBucketPayload(
            bucketId,
            bucket.Name,
            bucket.Type,
            bucket.Client,
            bucket.Hostname,
            bucket.Created,
            bucket.LastUpdated,
            bucket.Data)
        : bucket;
}
```

Replace `AwCollectorCursorState` with:

```csharp
public sealed class AwCollectorCursorState
{
    private readonly Dictionary<string, long> _committed = new(StringComparer.Ordinal);
    private readonly Dictionary<string, long> _pending = new(StringComparer.Ordinal);

    public long LastForBucket(string bucketId)
    {
        return _committed.GetValueOrDefault(bucketId);
    }

    public void RecordFetched(string bucketId, long lastId)
    {
        _pending[bucketId] = Math.Max(_pending.GetValueOrDefault(bucketId), lastId);
    }

    public void CommitFetched()
    {
        foreach (var (bucketId, lastId) in _pending)
            _committed[bucketId] = Math.Max(_committed.GetValueOrDefault(bucketId), lastId);

        _pending.Clear();
    }
}
```

- [ ] **Step 6: Update normal collection to iterate supported buckets**

Replace `CollectAndUploadAsync` body after `_awInfo ??= await FetchAwInfoAsync();` with:

```csharp
var buckets = await FetchSupportedBucketsAsync();
var outcomes = new List<AwBucketUploadOutcome>();

foreach (var bucket in buckets)
{
    outcomes.Add(await CollectBucketAndUploadAsync(bucket));
}

var pending = outcomes.Sum(o => Math.Max(0, o.Fetched - o.Uploaded));
var healthMessage = BuildUploadHealthMessage(outcomes);

lock (_lock) { _queueCount = pending; }

if (outcomes.Sum(o => o.Uploaded) > 0)
{
    lock (_lock)
    {
        _lastUploadTime = DateTime.Now;
        _lastUploadError = healthMessage;
    }
}
```

Change `CollectBucketAndUploadAsync` signature to:

```csharp
private async Task<AwBucketUploadOutcome> CollectBucketAndUploadAsync(AwBucketPayload bucket)
```

Inside it, replace `bucketId` uses with `bucket.Id`, replace `isAfk` logging with:

```csharp
var kind = AwBucketSelection.DescribeBucketKind(bucket.Type);
```

Use the cursor with:

```csharp
var lastId = _cursorState.LastForBucket(bucket.Id);
var rawEvents = FetchNewEvents(bucket.Id, lastId, out var pendingLastId);
```

After successful upload, replace the old `RecordFetched` branch with:

```csharp
_cursorState.RecordFetched(bucket.Id, pendingLastId);
_cursorState.CommitFetched();
```

Update the upload log to:

```csharp
Log?.Invoke($"[AwCollector] Uploaded {events.Count} complete {kind} events -> {result.Data} saved");
```

- [ ] **Step 7: Update backfill to iterate supported buckets**

In `BackfillAsync`, replace the two fixed bucket backfills with:

```csharp
var buckets = await FetchSupportedBucketsAsync();
var outcomes = new List<AwBucketUploadOutcome>();

foreach (var bucket in buckets)
{
    outcomes.Add(await BackfillBucketAsync(bucket, startUtc, endUtc));
}

var error = BuildUploadHealthMessage(outcomes);
var errorDetails = outcomes
    .Select(o => o.Error)
    .Prepend(error)
    .Where(e => !string.IsNullOrWhiteSpace(e));
var backfillError = string.Join("; ", errorDetails);

if (outcomes.Sum(o => o.Uploaded) > 0)
{
    lock (_lock)
    {
        _lastUploadTime = DateTime.Now;
        _lastUploadError = string.IsNullOrWhiteSpace(backfillError) ? null : backfillError;
    }
}

Log?.Invoke($"[AwCollector] Backfill finished: {outcomes.Sum(o => o.Uploaded)}/{outcomes.Sum(o => o.Fetched)} supported AW events uploaded");
```

Change `BackfillBucketAsync` signature to:

```csharp
private async Task<AwBucketUploadOutcome> BackfillBucketAsync(AwBucketPayload bucket, DateTimeOffset startUtc, DateTimeOffset endUtc)
```

Inside it, use `bucket.Id` in URLs and logs.

Replace `BuildUploadHealthMessage(int windowFetched, int windowUploaded, int afkFetched, int afkUploaded)` with:

```csharp
private static string? BuildUploadHealthMessage(IEnumerable<AwBucketUploadOutcome> outcomes)
{
    var pending = outcomes.Sum(o => Math.Max(0, o.Fetched - o.Uploaded));
    return pending == 0 ? null : $"Partial AW upload failure: pending {pending} events";
}
```

- [ ] **Step 8: Build client core and run tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~AwBucketSelectionTests
dotnet build src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj
```

Expected: tests pass and client core builds.

- [ ] **Step 9: Commit**

Run:

```powershell
git add src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs
git commit -m "feat(pc): collect activitywatch browser buckets"
```

## Task 3: Add Browser Page Timeline Synthesis

**Files:**

- Create: `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Extend detail query and record DTOs**

In `PcTrackerDtos.cs`, replace `DetailQueryParams` with:

```csharp
public record DetailQueryParams(
    string? DateFrom,
    string? DateTo,
    string? Dimension,
    string? DeviceId,
    string? AppName,
    string? CategoryName,
    string? KeyName,
    string? EventType,
    string? SortBy,
    string? SortDir,
    int Page,
    int PageSize,
    string? Domain = null,
    string? Title = null,
    string? Url = null,
    string? View = null
);
```

Replace `PcDetailRecord` with:

```csharp
public record PcDetailRecord(
    string RecordType,
    string Start,
    string? End,
    double? DurationSeconds,
    string DeviceId,
    string? AppName,
    string? DisplayName,
    string? CategoryName,
    string? Title,
    int? KeyPresses,
    int? TotalClicks,
    double? MouseDistance,
    double? ScrollDistance,
    Dictionary<string, int>? KeyCounts,
    object? Raw,
    string? Url = null,
    string? Domain = null,
    string? Path = null,
    bool IsLocalFile = false,
    string? BrowserAppName = null,
    string? BrowserWindowTitle = null,
    bool? Audible = null,
    bool? Incognito = null,
    int? TabCount = null,
    int AbsorbedShortEventsCount = 0,
    double AbsorbedDurationSeconds = 0,
    List<long>? SourceWebEventIds = null,
    List<long>? SourceWindowEventIds = null
);
```

In `PcTrackerModule.MapEndpoints`, add query parameters to `/detail`:

```csharp
[FromQuery] string? domain,
[FromQuery] string? title,
[FromQuery] string? url,
[FromQuery] string? view,
```

and create params with:

```csharp
var q = new DetailQueryParams(dateFrom, dateTo, dimension, deviceId,
    appName, categoryName, keyName, eventType, sortBy, sortDir, page, pageSize,
    domain, title, url, view);
```

- [ ] **Step 2: Write synthesis tests**

Append these tests to `PcTrackerCompleteCaptureTests` before `MakeDetailQuery()`:

```csharp
[Fact]
public async Task QueryCompleteDetailAsync_MergesShortWebPagesIntoNextValidPage()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().AddRange(
        WebEvent(1, "2026-05-20T05:00:00+00:00", 300, "https://example.com/a", "A"),
        WebEvent(2, "2026-05-20T05:05:00+00:00", 2, "https://example.com/b", "B"),
        WebEvent(3, "2026-05-20T05:05:02+00:00", 3, "https://example.com/c", "C"),
        WebEvent(4, "2026-05-20T05:05:05+00:00", 6, "https://example.com/d", "D"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(MakeDetailQuery(), CancellationToken.None);

    var pages = result.Items.Where(x => x.RecordType == "web-page").OrderBy(x => x.Start).ToList();
    Assert.Collection(
        pages,
        first =>
        {
            Assert.Equal("A", first.Title);
            Assert.Equal(300, first.DurationSeconds);
        },
        second =>
        {
            Assert.Equal("D", second.Title);
            Assert.Equal("2026-05-20T05:05:00.0000000+00:00", second.Start);
            Assert.Equal("2026-05-20T05:05:11.0000000+00:00", second.End);
            Assert.Equal(11, second.DurationSeconds);
            Assert.Equal(2, second.AbsorbedShortEventsCount);
            Assert.Equal(5, second.AbsorbedDurationSeconds);
            Assert.Equal(new List<long> { 2, 3, 4 }, second.SourceWebEventIds);
        });
}

[Fact]
public async Task QueryCompleteDetailAsync_MergesTrailingShortWebPageIntoPreviousValidPage()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().AddRange(
        WebEvent(1, "2026-05-20T05:00:00+00:00", 300, "https://example.com/a", "A"),
        WebEvent(2, "2026-05-20T05:05:00+00:00", 3, "https://example.com/b", "B"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(MakeDetailQuery(), CancellationToken.None);

    var page = Assert.Single(result.Items, x => x.RecordType == "web-page");
    Assert.Equal("A", page.Title);
    Assert.Equal("2026-05-20T05:00:00.0000000+00:00", page.Start);
    Assert.Equal("2026-05-20T05:05:03.0000000+00:00", page.End);
    Assert.Equal(303, page.DurationSeconds);
    Assert.Equal(1, page.AbsorbedShortEventsCount);
    Assert.Equal(3, page.AbsorbedDurationSeconds);
}

[Fact]
public async Task QueryCompleteDetailAsync_HidesBrowserWindowWhenWebPageExplainsIt()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().AddRange(
        WindowEvent("2026-05-20T05:00:00+00:00", 60, "msedge.exe", "Edge window"),
        WindowEvent("2026-05-20T05:02:00+00:00", 60, "notepad.exe", "Notes"),
        WebEvent(1, "2026-05-20T05:00:10+00:00", 10, "https://docs.activitywatch.net/", "Docs"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(MakeDetailQuery(), CancellationToken.None);

    Assert.DoesNotContain(result.Items, x => x.RecordType == "window" && x.AppName == "msedge.exe");
    Assert.Contains(result.Items, x => x.RecordType == "window" && x.AppName == "notepad.exe");
    var page = Assert.Single(result.Items, x => x.RecordType == "web-page");
    Assert.Equal("msedge.exe", page.BrowserAppName);
    Assert.Equal("Edge window", page.BrowserWindowTitle);
}

[Fact]
public async Task QueryCompleteDetailAsync_ReturnsBrowserWindowWhenNoWebPageExplainsIt()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().Add(WindowEvent("2026-05-20T05:00:00+00:00", 60, "msedge.exe", "Edge window"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(MakeDetailQuery(), CancellationToken.None);

    var window = Assert.Single(result.Items, x => x.RecordType == "window");
    Assert.Equal("msedge.exe", window.AppName);
}

[Fact]
public async Task QueryCompleteDetailAsync_CanReturnRawWebEvents()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().Add(WebEvent(1, "2026-05-20T05:00:00+00:00", 3, "https://example.com/b", "B"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(MakeDetailQuery() with { EventType = "web" }, CancellationToken.None);

    var raw = Assert.Single(result.Items);
    Assert.Equal("web", raw.RecordType);
    Assert.Equal("B", raw.Title);
}
```

Add these helpers near `MakeDetailQuery()`:

```csharp
private static AwEventEntity WebEvent(long sourceId, string timestamp, double duration, string url, string title)
{
    return new AwEventEntity
    {
        DeviceId = "DESKTOP",
        Timestamp = DateTimeOffset.Parse(timestamp),
        Duration = duration,
        EventType = "web",
        BucketId = "aw-watcher-web-edge_DESKTOP",
        BucketType = "web.tab.current",
        SourceEventId = sourceId,
        WindowTitle = title,
        DataJson = JsonSerializer.Serialize(new Dictionary<string, object>
        {
            ["url"] = url,
            ["title"] = title,
            ["audible"] = false,
            ["incognito"] = false,
            ["tabCount"] = 7
        })
    };
}

private static AwEventEntity WindowEvent(string timestamp, double duration, string app, string title)
{
    return new AwEventEntity
    {
        DeviceId = "DESKTOP",
        Timestamp = DateTimeOffset.Parse(timestamp),
        Duration = duration,
        EventType = "window",
        AppName = app,
        AppNameNormalized = AppNameNormalizer.Normalize(app),
        WindowTitle = title,
        DataJson = JsonSerializer.Serialize(new Dictionary<string, object>
        {
            ["app"] = app,
            ["title"] = title
        })
    };
}
```

- [ ] **Step 3: Run synthesis tests to verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~QueryCompleteDetailAsync_MergesShortWebPagesIntoNextValidPage|FullyQualifiedName~QueryCompleteDetailAsync_MergesTrailingShortWebPageIntoPreviousValidPage|FullyQualifiedName~QueryCompleteDetailAsync_HidesBrowserWindowWhenWebPageExplainsIt|FullyQualifiedName~QueryCompleteDetailAsync_ReturnsBrowserWindowWhenNoWebPageExplainsIt|FullyQualifiedName~QueryCompleteDetailAsync_CanReturnRawWebEvents"
```

Expected: FAIL because `web-page` synthesis and raw web filtering are not implemented.

- [ ] **Step 4: Add the browser page builder**

Create `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`:

```csharp
using System.Text.Json;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public static class BrowserPageTimelineBuilder
{
    private const double MinimumPageDurationSeconds = 5;
    private static readonly HashSet<string> BrowserApps = new(StringComparer.OrdinalIgnoreCase)
    {
        "msedge",
        "chrome",
        "firefox",
        "brave",
        "opera"
    };

    public static List<PcDetailRecord> BuildInterpretedAwRecords(
        List<AwEventEntity> awEvents,
        List<AppCategoryRule> rules)
    {
        var webEvents = awEvents
            .Where(IsWebEvent)
            .OrderBy(e => e.Timestamp)
            .ThenBy(e => e.SourceEventId ?? e.Id)
            .ToList();
        var windowEvents = awEvents
            .Where(e => e.EventType == "window")
            .OrderBy(e => e.Timestamp)
            .ToList();
        var pageRecords = BuildWebPageRecords(webEvents, windowEvents);
        var explainedBrowserWindows = FindExplainedBrowserWindowIds(windowEvents, pageRecords);

        var records = new List<PcDetailRecord>();
        records.AddRange(pageRecords);
        records.AddRange(awEvents
            .Where(e => e.EventType != "web")
            .Where(e => e.EventType != "window" || !explainedBrowserWindows.Contains(e.Id))
            .Select(e => ToRawAwRecord(e, rules)));

        return records;
    }

    public static PcDetailRecord ToRawAwRecord(AwEventEntity e, List<AppCategoryRule> rules)
    {
        var normalizedApp = AppNameNormalizer.Normalize(e.AppNameNormalized ?? e.AppName);
        var category = ClassifyApp(normalizedApp, rules);

        return new PcDetailRecord(
            e.EventType,
            FormatUtc(e.Timestamp),
            FormatUtc(e.Timestamp.AddSeconds(e.Duration)),
            e.Duration,
            e.DeviceId,
            e.AppName,
            normalizedApp,
            category,
            e.WindowTitle,
            null,
            null,
            null,
            null,
            null,
            ParseJsonObject(e.DataJson),
            Url: GetDataString(e.DataJson, "url"),
            Domain: ExtractDomain(GetDataString(e.DataJson, "url")),
            Path: ExtractPath(GetDataString(e.DataJson, "url")),
            IsLocalFile: IsLocalFileUrl(GetDataString(e.DataJson, "url")),
            Audible: GetDataBool(e.DataJson, "audible"),
            Incognito: GetDataBool(e.DataJson, "incognito"),
            TabCount: GetDataInt(e.DataJson, "tabCount"),
            SourceWebEventIds: e.EventType == "web" && e.SourceEventId is not null ? new List<long> { e.SourceEventId.Value } : null,
            SourceWindowEventIds: e.EventType == "window" && e.SourceEventId is not null ? new List<long> { e.SourceEventId.Value } : null);
    }

    private static List<PcDetailRecord> BuildWebPageRecords(List<AwEventEntity> webEvents, List<AwEventEntity> windowEvents)
    {
        var records = new List<MutablePageRecord>();
        var pendingShort = new List<AwEventEntity>();

        foreach (var webEvent in webEvents)
        {
            if (webEvent.Duration < MinimumPageDurationSeconds)
            {
                pendingShort.Add(webEvent);
                continue;
            }

            var record = MutablePageRecord.From(webEvent);
            if (pendingShort.Count > 0)
            {
                record.AbsorbBefore(pendingShort);
                pendingShort.Clear();
            }

            records.Add(record);
        }

        if (pendingShort.Count > 0 && records.Count > 0)
            records[^1].AbsorbAfter(pendingShort);

        return records
            .Select(r => r.ToDetailRecord(windowEvents))
            .ToList();
    }

    private static HashSet<long> FindExplainedBrowserWindowIds(List<AwEventEntity> windowEvents, List<PcDetailRecord> pageRecords)
    {
        return windowEvents
            .Where(IsBrowserWindow)
            .Where(window => pageRecords.Any(page => Overlaps(window.Timestamp, window.Timestamp.AddSeconds(window.Duration), DateTimeOffset.Parse(page.Start), DateTimeOffset.Parse(page.End!))))
            .Select(window => window.Id)
            .ToHashSet();
    }

    private static bool IsWebEvent(AwEventEntity e)
    {
        return e.EventType == "web" || e.BucketType == "web.tab.current";
    }

    private static bool IsBrowserWindow(AwEventEntity e)
    {
        return e.EventType == "window" && BrowserApps.Contains(AppNameNormalizer.Normalize(e.AppNameNormalized ?? e.AppName));
    }

    private static bool Overlaps(DateTimeOffset aStart, DateTimeOffset aEnd, DateTimeOffset bStart, DateTimeOffset bEnd)
    {
        return aStart < bEnd && bStart < aEnd;
    }

    private static string FormatUtc(DateTimeOffset timestamp)
    {
        return timestamp.ToUniversalTime().ToString("O");
    }

    private static string ClassifyApp(string appName, List<AppCategoryRule> rules)
    {
        foreach (var rule in rules)
        {
            if (string.Equals(appName, rule.AppPattern, StringComparison.OrdinalIgnoreCase))
                return rule.CategoryName;
        }
        return "Other";
    }

    private static object? ParseJsonObject(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
            return null;

        try
        {
            return JsonSerializer.Deserialize<JsonElement>(json);
        }
        catch (JsonException)
        {
            return null;
        }
    }

    private static string? GetDataString(string json, string key)
    {
        using var document = JsonDocument.Parse(string.IsNullOrWhiteSpace(json) ? "{}" : json);
        return document.RootElement.TryGetProperty(key, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private static bool? GetDataBool(string json, string key)
    {
        using var document = JsonDocument.Parse(string.IsNullOrWhiteSpace(json) ? "{}" : json);
        return document.RootElement.TryGetProperty(key, out var value) && value.ValueKind is JsonValueKind.True or JsonValueKind.False
            ? value.GetBoolean()
            : null;
    }

    private static int? GetDataInt(string json, string key)
    {
        using var document = JsonDocument.Parse(string.IsNullOrWhiteSpace(json) ? "{}" : json);
        return document.RootElement.TryGetProperty(key, out var value) && value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var number)
            ? number
            : null;
    }

    private static string? ExtractDomain(string? url)
    {
        if (string.IsNullOrWhiteSpace(url))
            return null;

        if (Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return uri.IsFile ? null : uri.Host;

        return null;
    }

    private static string? ExtractPath(string? url)
    {
        if (string.IsNullOrWhiteSpace(url))
            return null;

        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return null;

        return uri.IsFile ? Uri.UnescapeDataString(uri.LocalPath) : uri.AbsolutePath;
    }

    private static bool IsLocalFileUrl(string? url)
    {
        return Uri.TryCreate(url, UriKind.Absolute, out var uri) && uri.IsFile;
    }

    private sealed class MutablePageRecord
    {
        private readonly AwEventEntity _primary;
        private readonly List<long> _sourceWebEventIds = new();
        private DateTimeOffset _start;
        private DateTimeOffset _end;
        private int _absorbedShortEventsCount;
        private double _absorbedDurationSeconds;

        private MutablePageRecord(AwEventEntity primary)
        {
            _primary = primary;
            _start = primary.Timestamp;
            _end = primary.Timestamp.AddSeconds(primary.Duration);
            if (primary.SourceEventId is not null)
                _sourceWebEventIds.Add(primary.SourceEventId.Value);
        }

        public static MutablePageRecord From(AwEventEntity primary) => new(primary);

        public void AbsorbBefore(List<AwEventEntity> shortEvents)
        {
            _start = shortEvents.Min(e => e.Timestamp);
            Absorb(shortEvents);
        }

        public void AbsorbAfter(List<AwEventEntity> shortEvents)
        {
            _end = shortEvents.Max(e => e.Timestamp.AddSeconds(e.Duration));
            Absorb(shortEvents, insertBeforePrimary: false);
        }

        public PcDetailRecord ToDetailRecord(List<AwEventEntity> windowEvents)
        {
            var url = GetDataString(_primary.DataJson, "url");
            var overlappingWindow = windowEvents
                .Where(IsBrowserWindow)
                .Where(window => Overlaps(window.Timestamp, window.Timestamp.AddSeconds(window.Duration), _start, _end))
                .OrderByDescending(window => OverlapSeconds(window, _start, _end))
                .FirstOrDefault();
            var sourceWindowIds = overlappingWindow?.SourceEventId is null
                ? null
                : new List<long> { overlappingWindow.SourceEventId.Value };

            return new PcDetailRecord(
                "web-page",
                FormatUtc(_start),
                FormatUtc(_end),
                (_end - _start).TotalSeconds,
                _primary.DeviceId,
                null,
                ExtractDomain(url) ?? (IsLocalFileUrl(url) ? "文件" : null),
                null,
                GetDataString(_primary.DataJson, "title") ?? _primary.WindowTitle,
                null,
                null,
                null,
                null,
                null,
                ParseJsonObject(_primary.DataJson),
                Url: url,
                Domain: ExtractDomain(url),
                Path: ExtractPath(url),
                IsLocalFile: IsLocalFileUrl(url),
                BrowserAppName: overlappingWindow?.AppName,
                BrowserWindowTitle: overlappingWindow?.WindowTitle,
                Audible: GetDataBool(_primary.DataJson, "audible"),
                Incognito: GetDataBool(_primary.DataJson, "incognito"),
                TabCount: GetDataInt(_primary.DataJson, "tabCount"),
                AbsorbedShortEventsCount: _absorbedShortEventsCount,
                AbsorbedDurationSeconds: _absorbedDurationSeconds,
                SourceWebEventIds: _sourceWebEventIds,
                SourceWindowEventIds: sourceWindowIds);
        }

        private void Absorb(List<AwEventEntity> shortEvents, bool insertBeforePrimary = true)
        {
            _absorbedShortEventsCount += shortEvents.Count;
            _absorbedDurationSeconds += shortEvents.Sum(e => e.Duration);
            var sourceIds = shortEvents
                .Select(e => e.SourceEventId)
                .Where(id => id is not null)
                .Select(id => id!.Value);

            if (insertBeforePrimary)
            {
                foreach (var sourceId in sourceIds)
                    _sourceWebEventIds.Insert(Math.Max(0, _sourceWebEventIds.Count - 1), sourceId);
            }
            else
            {
                _sourceWebEventIds.AddRange(sourceIds);
            }
        }

        private static double OverlapSeconds(AwEventEntity window, DateTimeOffset pageStart, DateTimeOffset pageEnd)
        {
            var start = window.Timestamp > pageStart ? window.Timestamp : pageStart;
            var windowEnd = window.Timestamp.AddSeconds(window.Duration);
            var end = windowEnd < pageEnd ? windowEnd : pageEnd;
            return Math.Max(0, (end - start).TotalSeconds);
        }
    }
}
```

- [ ] **Step 5: Use the builder in detail queries**

In `PcTrackerService.QueryCompleteDetailAsync`, replace:

```csharp
var records = new List<PcDetailRecord>();
records.AddRange(awEvents.Select(e => ToAwDetailRecord(e, rules)));
records.AddRange(ToInputMinuteRecords(samples));
```

with:

```csharp
var records = new List<PcDetailRecord>();
var rawMode = string.Equals(q.View, "raw", StringComparison.OrdinalIgnoreCase)
    || string.Equals(q.EventType, "web", StringComparison.Ordinal);
records.AddRange(rawMode
    ? awEvents.Select(e => BrowserPageTimelineBuilder.ToRawAwRecord(e, rules))
    : BrowserPageTimelineBuilder.BuildInterpretedAwRecords(awEvents, rules));

if (!rawMode)
    records.AddRange(ToInputMinuteRecords(samples));
```

In `ApplyCompleteDetailFilters`, after the key filter block, add:

```csharp
if (!string.IsNullOrWhiteSpace(q.Domain))
{
    records = records.Where(r => ContainsIgnoreCase(r.Domain, q.Domain));
}

if (!string.IsNullOrWhiteSpace(q.Title))
{
    records = records.Where(r => ContainsIgnoreCase(r.Title, q.Title));
}

if (!string.IsNullOrWhiteSpace(q.Url))
{
    records = records.Where(r => ContainsIgnoreCase(r.Url, q.Url));
}
```

Replace `ToAwDetailRecord` usages with `BrowserPageTimelineBuilder.ToRawAwRecord` or remove the private helper after all call sites are gone.

- [ ] **Step 6: Run synthesis tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~QueryCompleteDetailAsync_MergesShortWebPagesIntoNextValidPage|FullyQualifiedName~QueryCompleteDetailAsync_MergesTrailingShortWebPageIntoPreviousValidPage|FullyQualifiedName~QueryCompleteDetailAsync_HidesBrowserWindowWhenWebPageExplainsIt|FullyQualifiedName~QueryCompleteDetailAsync_ReturnsBrowserWindowWhenNoWebPageExplainsIt|FullyQualifiedName~QueryCompleteDetailAsync_CanReturnRawWebEvents"
```

Expected: PASS.

- [ ] **Step 7: Run existing complete detail tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PcTrackerCompleteCaptureTests
```

Expected: PASS. Existing input-minute assertions must still pass.

- [ ] **Step 8: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): synthesize browser page timeline"
```

## Task 4: Use Browser Pages in Summary and Timeline APIs

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Add summary timeline test**

Append this test to `PcTrackerCompleteCaptureTests`:

```csharp
[Fact]
public async Task GetSummaryAsync_UsesBrowserPageRecordsInTimeline()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().AddRange(
        WindowEvent("2026-05-20T05:00:00+00:00", 60, "msedge.exe", "Edge window"),
        WebEvent(1, "2026-05-20T05:00:10+00:00", 10, "https://docs.activitywatch.net/en/latest/api/rest.html", "REST API"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var summary = await service.GetSummaryAsync(new DateTime(2026, 5, 20), CancellationToken.None);

    var item = Assert.Single(summary.Timeline);
    Assert.Equal("docs.activitywatch.net", item.AppName);
    Assert.Equal("REST API", item.WindowTitle);
    Assert.Equal(10.0 / 60.0, item.DurationMinutes);
}
```

- [ ] **Step 2: Run the failing summary test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GetSummaryAsync_UsesBrowserPageRecordsInTimeline
```

Expected: FAIL because summary timeline still uses raw window events.

- [ ] **Step 3: Add timeline conversion helpers**

In `PcTrackerService`, add:

```csharp
private async Task<List<PcDetailRecord>> BuildInterpretedAwDetailRecordsAsync(List<AwEventEntity> awEvents, CancellationToken ct)
{
    var rules = await GetCategoryRulesAsync(ct);
    return BrowserPageTimelineBuilder.BuildInterpretedAwRecords(awEvents, rules);
}

private static TimelineItem ToTimelineItem(PcDetailRecord record)
{
    var durationMinutes = record.DurationSeconds.GetValueOrDefault() / 60.0;
    return new TimelineItem(
        record.Start,
        record.End ?? record.Start,
        durationMinutes,
        record.RecordType == "web-page"
            ? record.Domain ?? (record.IsLocalFile ? "文件" : "web-page")
            : record.AppName ?? "unknown",
        record.Title);
}
```

Update `GetSummaryAsync` so it loads all AW event types for the day:

```csharp
var awEvents = await _db.Set<AwEventEntity>()
    .Where(e => e.Timestamp >= dayStart && e.Timestamp < dayEnd)
    .OrderBy(e => e.Timestamp)
    .ToListAsync(ct);
var windowEvents = awEvents.Where(e => e.EventType == "window").ToList();
var nonWebEvents = awEvents.Where(e => e.EventType != "web").ToList();
```

Keep existing heatmap/session/metrics behavior on non-web or window records:

```csharp
var heatmap = BuildHourlyHeatmap(dayStart, windowEvents);
```

Use `windowEvents` for `BuildSessions(...)` and `nonWebEvents` for `ComputeDerivedMetrics(...)`.

Then update `GetSummaryAsync` timeline creation:

```csharp
var interpretedAwRecords = await BuildInterpretedAwDetailRecordsAsync(awEvents, ct);
var timeline = interpretedAwRecords
    .Where(r => (r.RecordType is "window" or "web-page") && r.DurationSeconds.GetValueOrDefault() > 0)
    .Select(ToTimelineItem)
    .ToList();
```

Pass `timeline` into `PcSummaryResponse`.

Update `GetTimelineAsync` to use the same interpreted records:

```csharp
var events = await _db.Set<AwEventEntity>()
    .Where(e => e.Timestamp >= dayStart && e.Timestamp < dayEnd)
    .OrderBy(e => e.Timestamp)
    .ToListAsync(ct);

var interpretedAwRecords = await BuildInterpretedAwDetailRecordsAsync(events, ct);
return interpretedAwRecords
    .Where(r => (r.RecordType is "window" or "web-page") && r.DurationSeconds.GetValueOrDefault() > 0)
    .Select(ToTimelineItem)
    .ToList();
```

- [ ] **Step 4: Run the summary timeline test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GetSummaryAsync_UsesBrowserPageRecordsInTimeline
```

Expected: PASS.

- [ ] **Step 5: Run summary and detail tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GetSummaryAsync|FullyQualifiedName~GetTimelineAsync|FullyQualifiedName~QueryCompleteDetailAsync"
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): show browser pages in pc timeline"
```

## Task 5: Add Browser Page Filters and Detail Display to Web Client

**Files:**

- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`
- Verify: `src/client-web/src/api/pcTracker.ts`

- [ ] **Step 1: Update frontend types**

In `src/client-web/src/types/index.ts`, replace `DetailQueryParams` with:

```ts
export interface DetailQueryParams {
  dateFrom?: string;
  dateTo?: string;
  dimension?: 'hour' | 'day' | 'month' | 'year';
  deviceId?: string;
  appName?: string;
  categoryName?: string;
  keyName?: string;
  eventType?: string;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
  domain?: string;
  title?: string;
  url?: string;
  view?: 'interpreted' | 'raw';
}
```

Replace `PcDetailRecordType` with:

```ts
export type PcDetailRecordType = 'window' | 'afk' | 'web' | 'web-page' | 'input-minute' | 'app-input' | 'key-input';
```

Add these fields to `PcDetailRecord`:

```ts
  url: string | null;
  domain: string | null;
  path: string | null;
  isLocalFile: boolean;
  browserAppName: string | null;
  browserWindowTitle: string | null;
  audible: boolean | null;
  incognito: boolean | null;
  tabCount: number | null;
  absorbedShortEventsCount: number;
  absorbedDurationSeconds: number;
  sourceWebEventIds: number[] | null;
  sourceWindowEventIds: number[] | null;
```

- [ ] **Step 2: Update CSV columns**

In `PcDetailQueryPanel.tsx`, add these columns to `detailCsvColumns` after `title`:

```tsx
  { key: 'url', label: 'url' },
  { key: 'domain', label: 'domain' },
  { key: 'path', label: 'path' },
  { key: 'browserAppName', label: 'browserAppName' },
  { key: 'browserWindowTitle', label: 'browserWindowTitle' },
  { key: 'absorbedShortEventsCount', label: 'absorbedShortEventsCount' },
  { key: 'absorbedDurationSeconds', label: 'absorbedDurationSeconds' },
```

Update `formatCsvValue`:

```tsx
function formatCsvValue(row: PcDetailRecord, key: keyof PcDetailRecord) {
  if (key === 'raw') return row.raw == null ? '' : JSON.stringify(row.raw);
  if (key === 'keyCounts') return row.keyCounts == null ? '' : JSON.stringify(row.keyCounts);
  if (key === 'sourceWebEventIds') return row.sourceWebEventIds == null ? '' : JSON.stringify(row.sourceWebEventIds);
  if (key === 'sourceWindowEventIds') return row.sourceWindowEventIds == null ? '' : JSON.stringify(row.sourceWindowEventIds);
  return row[key] ?? '';
}
```

- [ ] **Step 3: Add Chinese type labels and browser display helpers**

Add near the formatter helpers:

```tsx
const recordTypeLabels: Record<string, string> = {
  window: '窗口',
  afk: '空闲',
  web: '原始页面',
  'web-page': '页面',
  'input-minute': '分钟输入',
  'app-input': '应用输入',
  'key-input': '按键明细',
};

function labelRecordType(type: string) {
  return recordTypeLabels[type] ?? type;
}

function formatPageSource(row: PcDetailRecord) {
  if (row.recordType !== 'web-page' && row.recordType !== 'web') return row.displayName || row.appName || '-';
  if (row.isLocalFile) return row.path ? `文件：${row.path.split(/[\\\\/]/).pop()}` : '文件';
  return row.domain || row.displayName || row.appName || '-';
}

function formatAbsorbed(row: PcDetailRecord) {
  if (!row.absorbedShortEventsCount) return '';
  return `吸收 ${row.absorbedShortEventsCount} 个短页面，${Math.round(row.absorbedDurationSeconds)}s`;
}
```

- [ ] **Step 4: Add browser filters**

In the filter grid, change the event type options to:

```tsx
<option value="">全部</option>
<option value="web-page">页面</option>
<option value="web">原始页面</option>
<option value="window">窗口</option>
<option value="afk">空闲</option>
<option value="input-minute">分钟输入</option>
<option value="app-input">应用输入</option>
<option value="key-input">按键明细</option>
```

Add these filter controls after the key name filter:

```tsx
<div>
  <label className="text-xs text-gray-500">域名</label>
  <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="如 docs.activitywatch.net"
    onChange={e => update('domain', e.target.value)} />
</div>
<div>
  <label className="text-xs text-gray-500">页面标题</label>
  <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="标题关键词"
    onChange={e => update('title', e.target.value)} />
</div>
<div>
  <label className="text-xs text-gray-500">URL</label>
  <input type="text" className="w-full border rounded-lg px-3 py-2 text-sm" placeholder="URL 关键词"
    onChange={e => update('url', e.target.value)} />
</div>
<div>
  <label className="text-xs text-gray-500">视图</label>
  <select className="w-full border rounded-lg px-3 py-2 text-sm"
    onChange={e => update('view', e.target.value || undefined)}>
    <option value="">解释时间线</option>
    <option value="raw">原始事件</option>
  </select>
</div>
```

- [ ] **Step 5: Update table rendering**

Change table headers to Chinese:

```tsx
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">类型</th>
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">开始</th>
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">结束</th>
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">设备</th>
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">来源</th>
<th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">标题</th>
<th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">按键</th>
<th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">点击</th>
<th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">滚动</th>
<th className="text-right px-3 py-2 text-xs text-gray-500 font-medium">时长</th>
```

Replace row mapping with a fragment that includes details:

```tsx
{data.items.map((row, i) => (
  <tr key={`${row.recordType}-${row.start}-${i}`} className="border-b hover:bg-gray-50 align-top">
    <td className="px-3 py-2 text-xs text-gray-700">{labelRecordType(row.recordType)}</td>
    <td className="px-3 py-2 text-xs text-gray-700 whitespace-nowrap">{formatDate(row.start)}</td>
    <td className="px-3 py-2 text-xs text-gray-700 whitespace-nowrap">{formatDate(row.end)}</td>
    <td className="px-3 py-2 text-xs text-gray-700">{row.deviceId || '-'}</td>
    <td className="px-3 py-2 text-xs text-gray-700 max-w-[180px] truncate" title={formatPageSource(row)}>
      {formatPageSource(row)}
    </td>
    <td className="px-3 py-2 text-xs text-gray-700 max-w-[320px]">
      <div className="truncate" title={row.title ?? undefined}>{row.title || '-'}</div>
      {(row.url || row.raw != null) && (
        <details className="mt-1 text-[11px] text-gray-500">
          <summary className="cursor-pointer select-none">详情</summary>
          <div className="mt-1 space-y-1 break-all">
            {row.url && <div>URL：{row.url}</div>}
            {row.browserAppName && <div>关联窗口：{row.browserAppName} / {row.browserWindowTitle || '-'}</div>}
            {formatAbsorbed(row) && <div>{formatAbsorbed(row)}</div>}
            <pre className="max-h-40 overflow-auto rounded bg-gray-50 p-2">{JSON.stringify(row.raw, null, 2)}</pre>
          </div>
        </details>
      )}
    </td>
    <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.keyPresses)}</td>
    <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.totalClicks)}</td>
    <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatNumber(row.scrollDistance)}</td>
    <td className="px-3 py-2 text-xs text-gray-700 text-right">{formatDurationSeconds(row.durationSeconds)}</td>
  </tr>
))}
```

- [ ] **Step 6: Build frontend**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx src/client-web/src/api/pcTracker.ts
git commit -m "feat(pc): show browser page detail records"
```

## Task 6: End-to-End Verification

**Files:**

- Verify only unless a preceding task fails.

- [ ] **Step 1: Run backend unit tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
```

Expected: PASS.

- [ ] **Step 2: Build Windows daemon core and app**

Run:

```powershell
dotnet build src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Debug
```

Expected: PASS. If the app executable is locked, stop `Pim.Client.App` and rerun the second command.

- [ ] **Step 3: Build frontend**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 4: Optionally verify local ActivityWatch buckets**

Run:

```powershell
Invoke-RestMethod -Uri 'http://localhost:5600/api/0/buckets/' |
  Select-Object -ExpandProperty PSObject |
  Select-Object -ExpandProperty Properties |
  ForEach-Object { [pscustomobject]@{ Id=$_.Name; Type=$_.Value.type; Client=$_.Value.client } } |
  Format-Table -AutoSize
```

Expected: output includes `web.tab.current`; if `aw-watcher-input` appears, it is not selected by `AwBucketSelection`.

- [ ] **Step 5: Optionally verify API responses after daemon sync**

After API and daemon are running, run:

```powershell
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=2026-05-20&dateTo=2026-05-20&eventType=web-page&pageSize=20"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=2026-05-20&dateTo=2026-05-20&eventType=web&pageSize=20"
```

Expected: both return HTTP 200. `web-page` contains interpreted page records when browser plugin data exists. `web` contains raw page records, including short pages.

- [ ] **Step 6: Confirm git status**

Run:

```powershell
git status --short --branch
```

Expected: only intentional changes remain. Existing unrelated dirty files from before this plan may still be present if they were not part of the browser-page tasks.

## Self-Review Checklist

- Spec coverage: raw web upload, input bucket exclusion, KeyStats unchanged, short-page merge, window hide/fallback, raw web view, domain/title/url filters, frontend display, and verification are covered.
- Placeholder scan: the plan contains no unresolved markers or unspecified implementation steps.
- Type consistency: backend `DetailQueryParams` fields match frontend `DetailQueryParams`; backend `PcDetailRecord` optional fields match frontend `PcDetailRecord`.
- Scope control: tasks do not change `KeyStatsCollectorService`, `KeystatsDeltaCalculator`, keyboard heatmap behavior, or input-minute math.
