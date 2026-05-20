# PC Tracker Complete Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist every field available from ActivityWatch and KeyStats, sample KeyStats once per minute, and make PC detail queries explain window, AFK, input, app, and key activity at minute-level granularity.

**Architecture:** Add a raw capture layer for ActivityWatch buckets/events and KeyStats samples, keep existing daily tables as compatibility caches, and build detail/query responses from raw events plus computed minute deltas. Use idempotent database schema initialization because this project currently calls `EnsureCreated()` rather than EF migrations.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core with PostgreSQL/Npgsql, WPF Windows daemon, React 19 + TypeScript + TanStack Query, xUnit.

---

## Reference Inputs

- Spec: `docs/superpowers/specs/2026-05-20-pc-tracker-complete-capture-design.md`
- Existing module: `src/modules/Pim.Module.PcTracker`
- Existing Windows collectors: `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`, `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`
- Existing detail page: `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`

## File Structure

Create:

- `src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs`: stores ActivityWatch bucket metadata.
- `src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs`: stores one complete KeyStats raw snapshot per device per minute.
- `src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs`: normalizes `.exe`, casing, and display variants for classification and joins.
- `src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs`: computes minute deltas from consecutive KeyStats samples.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`: applies idempotent SQL schema upgrades on startup.
- `tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs`: verifies app normalization.
- `tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs`: verifies minute delta rules.
- `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`: verifies AW upsert and KeyStats sample storage behavior.

Modify:

- `src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs`: add complete raw AW fields while preserving existing columns.
- `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`: configure new entities, indexes, and unique constraints.
- `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`: add complete upload contracts and typed detail records.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`: add complete capture writes, delta-based detail queries, and compatibility fallbacks.
- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: register schema initializer and new upload endpoint.
- `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`: upload AW info, bucket metadata, source event ids, and full data JSON.
- `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`: upload complete one-minute samples including raw JSON and formatted fields.
- `src/client-web/src/types/index.ts`: add typed PC detail records and event-type filters.
- `src/client-web/src/api/pcTracker.ts`: support expanded query params.
- `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`: render mixed detail records with filters and raw JSON expansion.

## Task 1: Add App Normalization Helper

**Files:**

- Create: `src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs`
- Create: `tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs`

- [ ] **Step 1: Write failing tests**

Create `tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs`:

```csharp
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class AppNameNormalizerTests
{
    [Theory]
    [InlineData("msedge.exe", "msedge")]
    [InlineData("msedge", "msedge")]
    [InlineData("Codex.exe", "codex")]
    [InlineData("Google Chrome", "google chrome")]
    [InlineData(" PowerToys.Peek.UI.exe ", "powertoys.peek.ui")]
    public void Normalize_ReturnsStableLowercaseAppKey(string input, string expected)
    {
        Assert.Equal(expected, AppNameNormalizer.Normalize(input));
    }

    [Fact]
    public void Normalize_ReturnsUnknownForBlankInput()
    {
        Assert.Equal("unknown", AppNameNormalizer.Normalize(""));
        Assert.Equal("unknown", AppNameNormalizer.Normalize(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~AppNameNormalizerTests
```

Expected: FAIL with `CS0103` or `CS0246` because `AppNameNormalizer` does not exist.

- [ ] **Step 3: Implement normalizer**

Create `src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs`:

```csharp
namespace Pim.Module.PcTracker.Services;

public static class AppNameNormalizer
{
    public static string Normalize(string? appName)
    {
        if (string.IsNullOrWhiteSpace(appName))
            return "unknown";

        var normalized = appName.Trim().ToLowerInvariant();
        return normalized.EndsWith(".exe", StringComparison.Ordinal)
            ? normalized[..^4]
            : normalized;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~AppNameNormalizerTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs tests/Pim.UnitTests/Services/AppNameNormalizerTests.cs
git commit -m "feat(pc): normalize tracked app names"
```

## Task 2: Add Complete Capture Entities and Schema Initializer

**Files:**

- Create: `src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs`
- Create: `src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs`
- Create: `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`

- [ ] **Step 1: Add entity model test**

Append to `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.Entities;
using Xunit;

namespace Pim.UnitTests.Services;

public class PcTrackerCompleteCaptureTests
{
    [Fact]
    public void Model_IncludesCompleteCaptureEntities()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwBucketEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        using var db = new PimDbContext(options);

        Assert.NotNull(db.Model.FindEntityType(typeof(AwBucketEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(KeystatsSampleEntity)));
    }
}
```

- [ ] **Step 2: Add EF InMemory package if needed**

If the test project does not already reference `Microsoft.EntityFrameworkCore.InMemory`, add this package reference to `tests/Pim.UnitTests/Pim.UnitTests.csproj`:

```xml
<PackageReference Include="Microsoft.EntityFrameworkCore.InMemory" Version="8.0.0" />
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Model_IncludesCompleteCaptureEntities
```

Expected: FAIL because `AwBucketEntity` and `KeystatsSampleEntity` do not exist.

- [ ] **Step 4: Create AW bucket entity**

Create `src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_aw_buckets")]
public class AwBucketEntity
{
    [Key][Column("id")] public long Id { get; set; }
    [Column("pim_device_id")][MaxLength(64)] public string PimDeviceId { get; set; } = string.Empty;
    [Column("aw_device_id")][MaxLength(128)] public string? AwDeviceId { get; set; }
    [Column("bucket_id")][MaxLength(256)] public string BucketId { get; set; } = string.Empty;
    [Column("name")][MaxLength(256)] public string? Name { get; set; }
    [Column("type")][MaxLength(64)] public string BucketType { get; set; } = string.Empty;
    [Column("client")][MaxLength(128)] public string Client { get; set; } = string.Empty;
    [Column("hostname")][MaxLength(128)] public string Hostname { get; set; } = string.Empty;
    [Column("created_at_source")] public DateTimeOffset? CreatedAtSource { get; set; }
    [Column("last_updated_source")] public DateTimeOffset? LastUpdatedSource { get; set; }
    [Column("data_json", TypeName = "jsonb")] public string DataJson { get; set; } = "{}";
    [Column("seen_at")] public DateTimeOffset SeenAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 5: Create KeyStats sample entity**

Create `src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_keystats_samples")]
public class KeystatsSampleEntity
{
    [Key][Column("id")] public long Id { get; set; }
    [Column("pim_device_id")][MaxLength(64)] public string PimDeviceId { get; set; } = string.Empty;
    [Column("sampled_at_utc")] public DateTimeOffset SampledAtUtc { get; set; }
    [Column("stats_date", TypeName = "date")] public DateTime StatsDate { get; set; }
    [Column("stats_timezone_offset_minutes")] public int StatsTimezoneOffsetMinutes { get; set; }
    [Column("key_presses")] public int KeyPresses { get; set; }
    [Column("left_clicks")] public int LeftClicks { get; set; }
    [Column("right_clicks")] public int RightClicks { get; set; }
    [Column("middle_clicks")] public int MiddleClicks { get; set; }
    [Column("side_back_clicks")] public int SideBackClicks { get; set; }
    [Column("side_forward_clicks")] public int SideForwardClicks { get; set; }
    [Column("mouse_distance")] public double MouseDistance { get; set; }
    [Column("scroll_distance")] public double ScrollDistance { get; set; }
    [Column("peak_kps")] public int PeakKps { get; set; }
    [Column("peak_cps")] public int PeakCps { get; set; }
    [Column("formatted_mouse_distance")][MaxLength(64)] public string? FormattedMouseDistance { get; set; }
    [Column("formatted_scroll_distance")][MaxLength(64)] public string? FormattedScrollDistance { get; set; }
    [Column("key_counts_json", TypeName = "jsonb")] public string KeyCountsJson { get; set; } = "{}";
    [Column("app_stats_json", TypeName = "jsonb")] public string AppStatsJson { get; set; } = "{}";
    [Column("raw_json", TypeName = "jsonb")] public string RawJson { get; set; } = "{}";
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 6: Expand AW event entity**

Modify `src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs` so the class contains these properties while keeping existing `device_id`, `timestamp`, `duration`, `event_type`, `app_name`, `window_title`, and `afk_status` columns:

```csharp
[Column("aw_device_id")][MaxLength(128)] public string? AwDeviceId { get; set; }
[Column("aw_hostname")][MaxLength(128)] public string? AwHostname { get; set; }
[Column("bucket_id")][MaxLength(256)] public string? BucketId { get; set; }
[Column("bucket_type")][MaxLength(64)] public string? BucketType { get; set; }
[Column("bucket_client")][MaxLength(128)] public string? BucketClient { get; set; }
[Column("source_event_id")] public long? SourceEventId { get; set; }
[Column("data_json", TypeName = "jsonb")] public string DataJson { get; set; } = "{}";
[Column("app_name_normalized")][MaxLength(256)] public string? AppNameNormalized { get; set; }
[Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
```

- [ ] **Step 7: Configure indexes and unique constraints**

Modify `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`:

```csharp
public class AwBucketEntityConfiguration : IEntityTypeConfiguration<AwBucketEntity>
{
    public void Configure(EntityTypeBuilder<AwBucketEntity> builder)
    {
        builder.HasIndex(e => new { e.PimDeviceId, e.BucketId }).IsUnique();
        builder.HasIndex(e => e.BucketType);
        builder.HasIndex(e => e.SeenAt);
    }
}

public class KeystatsSampleEntityConfiguration : IEntityTypeConfiguration<KeystatsSampleEntity>
{
    public void Configure(EntityTypeBuilder<KeystatsSampleEntity> builder)
    {
        builder.HasIndex(e => new { e.PimDeviceId, e.SampledAtUtc }).IsUnique();
        builder.HasIndex(e => e.StatsDate);
    }
}
```

Also update `AwEventEntityConfiguration.Configure`:

```csharp
builder.HasIndex(e => e.BucketId);
builder.HasIndex(e => e.SourceEventId);
builder.HasIndex(e => e.AppNameNormalized);
builder.HasIndex(e => new { e.DeviceId, e.BucketId, e.SourceEventId }).IsUnique();
```

- [ ] **Step 8: Add idempotent schema initializer**

Create `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;

namespace Pim.Module.PcTracker.Services;

public sealed class PcTrackerSchemaInitializer
{
    private readonly PimDbContext _db;

    public PcTrackerSchemaInitializer(PimDbContext db)
    {
        _db = db;
    }

    public async Task InitializeAsync(CancellationToken ct = default)
    {
        await _db.Database.ExecuteSqlRawAsync("""
CREATE TABLE IF NOT EXISTS pc_aw_buckets (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    pim_device_id VARCHAR(64) NOT NULL,
    aw_device_id VARCHAR(128),
    bucket_id VARCHAR(256) NOT NULL,
    name VARCHAR(256),
    type VARCHAR(64) NOT NULL,
    client VARCHAR(128) NOT NULL,
    hostname VARCHAR(128) NOT NULL,
    created_at_source TIMESTAMPTZ,
    last_updated_source TIMESTAMPTZ,
    data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_aw_buckets_device_bucket ON pc_aw_buckets (pim_device_id, bucket_id);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS aw_device_id VARCHAR(128);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS aw_hostname VARCHAR(128);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS bucket_id VARCHAR(256);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS bucket_type VARCHAR(64);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS bucket_client VARCHAR(128);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS source_event_id BIGINT;
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS data_json JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS app_name_normalized VARCHAR(256);
ALTER TABLE pc_aw_events ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_aw_events_source ON pc_aw_events (device_id, bucket_id, source_event_id) WHERE bucket_id IS NOT NULL AND source_event_id IS NOT NULL;
CREATE TABLE IF NOT EXISTS pc_keystats_samples (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    pim_device_id VARCHAR(64) NOT NULL,
    sampled_at_utc TIMESTAMPTZ NOT NULL,
    stats_date DATE NOT NULL,
    stats_timezone_offset_minutes INT NOT NULL,
    key_presses INT NOT NULL DEFAULT 0,
    left_clicks INT NOT NULL DEFAULT 0,
    right_clicks INT NOT NULL DEFAULT 0,
    middle_clicks INT NOT NULL DEFAULT 0,
    side_back_clicks INT NOT NULL DEFAULT 0,
    side_forward_clicks INT NOT NULL DEFAULT 0,
    mouse_distance DOUBLE PRECISION NOT NULL DEFAULT 0,
    scroll_distance DOUBLE PRECISION NOT NULL DEFAULT 0,
    peak_kps INT NOT NULL DEFAULT 0,
    peak_cps INT NOT NULL DEFAULT 0,
    formatted_mouse_distance VARCHAR(64),
    formatted_scroll_distance VARCHAR(64),
    key_counts_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    app_stats_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_keystats_samples_device_minute ON pc_keystats_samples (pim_device_id, sampled_at_utc);
CREATE INDEX IF NOT EXISTS ix_pc_keystats_samples_stats_date ON pc_keystats_samples (stats_date);
""", ct);
    }
}
```

- [ ] **Step 9: Run initializer at module startup**

Modify `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`:

```csharp
services.AddScoped<PcTrackerSchemaInitializer>();
```

Then update `InitializeAsync`:

```csharp
public async Task InitializeAsync(IServiceProvider serviceProvider)
{
    using var scope = serviceProvider.CreateScope();
    var initializer = scope.ServiceProvider.GetRequiredService<PcTrackerSchemaInitializer>();
    await initializer.InitializeAsync();
}
```

- [ ] **Step 10: Run model test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~Model_IncludesCompleteCaptureEntities
```

Expected: PASS.

- [ ] **Step 11: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Entities src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests tests/Pim.UnitTests/Pim.UnitTests.csproj
git commit -m "feat(pc): add complete capture schema"
```

## Task 3: Add Complete Upload DTOs

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`

- [ ] **Step 1: Add server DTOs**

Append these records to `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`:

```csharp
public record AwInfoDto(
    string? Hostname,
    string? Version,
    bool Testing,
    string? DeviceId
);

public record AwBucketDto(
    string Id,
    string? Name,
    string Type,
    string Client,
    string Hostname,
    string? Created,
    string? LastUpdated,
    Dictionary<string, object>? Data
);

public record CompleteAwEventEntry(
    long SourceEventId,
    string Timestamp,
    double Duration,
    Dictionary<string, object>? Data
);

public record CompleteAwUploadRequest(
    string PimDeviceId,
    AwInfoDto? AwInfo,
    AwBucketDto Bucket,
    List<CompleteAwEventEntry> Events
);

public record KeystatsSampleUploadRequest(
    string PimDeviceId,
    string SampledAt,
    string Date,
    int KeyPresses,
    Dictionary<string, int>? KeyPressCounts,
    int LeftClicks,
    int RightClicks,
    int MiddleClicks,
    int SideBackClicks,
    int SideForwardClicks,
    double MouseDistance,
    double ScrollDistance,
    int PeakKps,
    int PeakCps,
    string? FormattedMouseDistance,
    string? FormattedScrollDistance,
    Dictionary<string, AppStatEntry>? AppStats
);
```

- [ ] **Step 2: Add client-side serializable request shapes**

Inside `AwCollectorService.cs`, replace anonymous AW upload payloads with private records:

```csharp
private sealed record AwInfoPayload(string? Hostname, string? Version, bool Testing, string? DeviceId);
private sealed record AwBucketPayload(string Id, string? Name, string Type, string Client, string Hostname, string? Created, string? LastUpdated, Dictionary<string, object>? Data);
private sealed record AwEventPayload(long SourceEventId, string Timestamp, double Duration, Dictionary<string, object>? Data);
private sealed record CompleteAwUploadPayload(string PimDeviceId, AwInfoPayload? AwInfo, AwBucketPayload Bucket, List<AwEventPayload> Events);
```

Inside `KeyStatsCollectorService.cs`, add:

```csharp
private sealed record KeystatsSampleUploadPayload(
    string PimDeviceId,
    string SampledAt,
    string Date,
    int KeyPresses,
    Dictionary<string, int>? KeyPressCounts,
    int LeftClicks,
    int RightClicks,
    int MiddleClicks,
    int SideBackClicks,
    int SideForwardClicks,
    double MouseDistance,
    double ScrollDistance,
    int PeakKps,
    int PeakCps,
    string? FormattedMouseDistance,
    string? FormattedScrollDistance,
    Dictionary<string, KeyStatsAppStats>? AppStats
);
```

- [ ] **Step 3: Build to verify DTOs compile**

Run:

```powershell
dotnet build src/modules/Pim.Module.PcTracker/Pim.Module.PcTracker.csproj
dotnet build src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj
```

Expected: both builds succeed.

- [ ] **Step 4: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs
git commit -m "feat(pc): define complete capture upload contracts"
```

## Task 4: Implement ActivityWatch Idempotent Upsert

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Write failing upsert test**

Append to `PcTrackerCompleteCaptureTests`:

```csharp
[Fact]
public async Task UploadCompleteAwEventsAsync_UpsertsByBucketAndSourceEventId()
{
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    var service = new PcTrackerService(db);

    var bucket = new AwBucketDto(
        "aw-watcher-window_DESKTOP",
        null,
        "currentwindow",
        "aw-watcher-window",
        "DESKTOP",
        "2026-05-20T00:00:00+00:00",
        "2026-05-20T05:00:00+00:00",
        new Dictionary<string, object>());

    var first = new CompleteAwUploadRequest(
        "DESKTOP",
        new AwInfoDto("DESKTOP", "v0.13.2", false, "aw-device"),
        bucket,
        new List<CompleteAwEventEntry>
        {
            new(100, "2026-05-20T05:00:00+00:00", 1.0, new Dictionary<string, object>
            {
                ["app"] = "msedge.exe",
                ["title"] = "First"
            })
        });

    var second = first with
    {
        Events = new List<CompleteAwEventEntry>
        {
            new(100, "2026-05-20T05:00:00+00:00", 42.0, new Dictionary<string, object>
            {
                ["app"] = "msedge.exe",
                ["title"] = "First"
            })
        }
    };

    Assert.Equal(1, await service.UploadCompleteAwEventsAsync(first, CancellationToken.None));
    Assert.Equal(0, await service.UploadCompleteAwEventsAsync(second, CancellationToken.None));

    var saved = Assert.Single(db.Set<AwEventEntity>());
    Assert.Equal(42.0, saved.Duration);
    Assert.Equal("aw-watcher-window_DESKTOP", saved.BucketId);
    Assert.Equal(100, saved.SourceEventId);
    Assert.Equal("msedge", saved.AppNameNormalized);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UploadCompleteAwEventsAsync_UpsertsByBucketAndSourceEventId
```

Expected: FAIL because `UploadCompleteAwEventsAsync` does not exist.

- [ ] **Step 3: Implement `UploadCompleteAwEventsAsync`**

Add to `PcTrackerService.cs`:

```csharp
public async Task<int> UploadCompleteAwEventsAsync(CompleteAwUploadRequest req, CancellationToken ct)
{
    var now = DateTimeOffset.UtcNow;
    var bucket = await _db.Set<AwBucketEntity>()
        .FirstOrDefaultAsync(x => x.PimDeviceId == req.PimDeviceId && x.BucketId == req.Bucket.Id, ct);

    if (bucket is null)
    {
        bucket = new AwBucketEntity
        {
            PimDeviceId = req.PimDeviceId,
            AwDeviceId = req.AwInfo?.DeviceId,
            BucketId = req.Bucket.Id,
            Name = req.Bucket.Name,
            BucketType = req.Bucket.Type,
            Client = req.Bucket.Client,
            Hostname = req.Bucket.Hostname,
            CreatedAtSource = ParseOptionalOffset(req.Bucket.Created),
            LastUpdatedSource = ParseOptionalOffset(req.Bucket.LastUpdated),
            DataJson = ToJson(req.Bucket.Data),
            SeenAt = now
        };
        _db.Set<AwBucketEntity>().Add(bucket);
    }
    else
    {
        bucket.AwDeviceId = req.AwInfo?.DeviceId;
        bucket.Name = req.Bucket.Name;
        bucket.BucketType = req.Bucket.Type;
        bucket.Client = req.Bucket.Client;
        bucket.Hostname = req.Bucket.Hostname;
        bucket.LastUpdatedSource = ParseOptionalOffset(req.Bucket.LastUpdated);
        bucket.DataJson = ToJson(req.Bucket.Data);
        bucket.SeenAt = now;
    }

    var sourceIds = req.Events.Select(e => e.SourceEventId).ToList();
    var existing = await _db.Set<AwEventEntity>()
        .Where(e => e.DeviceId == req.PimDeviceId && e.BucketId == req.Bucket.Id && e.SourceEventId != null && sourceIds.Contains(e.SourceEventId.Value))
        .ToDictionaryAsync(e => e.SourceEventId!.Value, ct);

    var inserted = 0;
    foreach (var incoming in req.Events)
    {
        var data = incoming.Data ?? new Dictionary<string, object>();
        var app = GetString(data, "app");
        var title = GetString(data, "title");
        var status = GetString(data, "status");
        var timestamp = DateTimeOffset.Parse(incoming.Timestamp).ToUniversalTime();

        if (!existing.TryGetValue(incoming.SourceEventId, out var entity))
        {
            entity = new AwEventEntity
            {
                DeviceId = req.PimDeviceId,
                CreatedAt = now
            };
            _db.Set<AwEventEntity>().Add(entity);
            inserted++;
        }

        entity.AwDeviceId = req.AwInfo?.DeviceId;
        entity.AwHostname = req.AwInfo?.Hostname;
        entity.BucketId = req.Bucket.Id;
        entity.BucketType = req.Bucket.Type;
        entity.BucketClient = req.Bucket.Client;
        entity.SourceEventId = incoming.SourceEventId;
        entity.Timestamp = timestamp;
        entity.Duration = incoming.Duration;
        entity.DataJson = ToJson(data);
        entity.EventType = req.Bucket.Type == "afkstatus" ? "afk" : "window";
        entity.AppName = app;
        entity.AppNameNormalized = AppNameNormalizer.Normalize(app);
        entity.WindowTitle = title;
        entity.AfkStatus = status;
        entity.UpdatedAt = now;
    }

    await _db.SaveChangesAsync(ct);
    return inserted;
}
```

Also add helpers to `PcTrackerService.cs`:

```csharp
private static DateTimeOffset? ParseOptionalOffset(string? value)
{
    return DateTimeOffset.TryParse(value, out var parsed) ? parsed.ToUniversalTime() : null;
}

private static string ToJson(object? value)
{
    return System.Text.Json.JsonSerializer.Serialize(value ?? new { });
}

private static string? GetString(Dictionary<string, object> data, string key)
{
    if (!data.TryGetValue(key, out var value) || value is null)
        return null;
    return value is System.Text.Json.JsonElement element
        ? element.ValueKind == System.Text.Json.JsonValueKind.String ? element.GetString() : element.ToString()
        : value.ToString();
}
```

- [ ] **Step 4: Add endpoint**

Modify `PcTrackerModule.MapEndpoints`:

```csharp
writeGroup.MapPost("/aw/upload-complete", async (
    [FromBody] CompleteAwUploadRequest req,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var count = await svc.UploadCompleteAwEventsAsync(req, ct);
    return Results.Ok(ApiResponse<int>.Ok(count));
});
```

- [ ] **Step 5: Run test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UploadCompleteAwEventsAsync_UpsertsByBucketAndSourceEventId
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): upsert complete activitywatch events"
```

## Task 5: Implement KeyStats One-Minute Sample Storage

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Write failing sample upsert test**

Append to `PcTrackerCompleteCaptureTests`:

```csharp
[Fact]
public async Task UpsertKeystatsSampleAsync_StoresRawMinuteSnapshot()
{
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    var service = new PcTrackerService(db);

    var req = new KeystatsSampleUploadRequest(
        "DESKTOP",
        "2026-05-20T05:55:33+00:00",
        "2026-05-20T00:00:00+08:00",
        10,
        new Dictionary<string, int> { ["Space"] = 4 },
        1,
        2,
        3,
        4,
        5,
        123.4,
        56.7,
        8,
        9,
        "1.2 m",
        "57 px",
        new Dictionary<string, AppStatEntry>
        {
            ["msedge"] = new("msedge", "Microsoft Edge", 10, 1, 2, 0, 0, 0, 56.7)
        });

    await service.UpsertKeystatsSampleAsync(req, CancellationToken.None);
    await service.UpsertKeystatsSampleAsync(req with { KeyPresses = 12 }, CancellationToken.None);

    var saved = Assert.Single(db.Set<KeystatsSampleEntity>());
    Assert.Equal(new DateTimeOffset(2026, 5, 20, 5, 55, 0, TimeSpan.Zero), saved.SampledAtUtc);
    Assert.Equal(12, saved.KeyPresses);
    Assert.Contains("Space", saved.KeyCountsJson);
    Assert.Contains("msedge", saved.AppStatsJson);
    Assert.Contains("FormattedMouseDistance", saved.RawJson);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UpsertKeystatsSampleAsync_StoresRawMinuteSnapshot
```

Expected: FAIL because `UpsertKeystatsSampleAsync` does not exist.

- [ ] **Step 3: Implement sample upsert**

Add to `PcTrackerService.cs`:

```csharp
public async Task UpsertKeystatsSampleAsync(KeystatsSampleUploadRequest req, CancellationToken ct)
{
    var sampledAt = DateTimeOffset.Parse(req.SampledAt).ToUniversalTime();
    sampledAt = new DateTimeOffset(sampledAt.Year, sampledAt.Month, sampledAt.Day, sampledAt.Hour, sampledAt.Minute, 0, TimeSpan.Zero);
    var statsDate = DateTimeOffset.Parse(req.Date);

    var entity = await _db.Set<KeystatsSampleEntity>()
        .FirstOrDefaultAsync(x => x.PimDeviceId == req.PimDeviceId && x.SampledAtUtc == sampledAt, ct);

    if (entity is null)
    {
        entity = new KeystatsSampleEntity
        {
            PimDeviceId = req.PimDeviceId,
            SampledAtUtc = sampledAt,
            CreatedAt = DateTimeOffset.UtcNow
        };
        _db.Set<KeystatsSampleEntity>().Add(entity);
    }

    entity.StatsDate = statsDate.Date;
    entity.StatsTimezoneOffsetMinutes = (int)statsDate.Offset.TotalMinutes;
    entity.KeyPresses = req.KeyPresses;
    entity.LeftClicks = req.LeftClicks;
    entity.RightClicks = req.RightClicks;
    entity.MiddleClicks = req.MiddleClicks;
    entity.SideBackClicks = req.SideBackClicks;
    entity.SideForwardClicks = req.SideForwardClicks;
    entity.MouseDistance = req.MouseDistance;
    entity.ScrollDistance = req.ScrollDistance;
    entity.PeakKps = req.PeakKps;
    entity.PeakCps = req.PeakCps;
    entity.FormattedMouseDistance = req.FormattedMouseDistance;
    entity.FormattedScrollDistance = req.FormattedScrollDistance;
    entity.KeyCountsJson = ToJson(req.KeyPressCounts ?? new Dictionary<string, int>());
    entity.AppStatsJson = ToJson(req.AppStats ?? new Dictionary<string, AppStatEntry>());
    entity.RawJson = ToJson(req);

    await _db.SaveChangesAsync(ct);
}
```

- [ ] **Step 4: Add endpoint**

Modify `PcTrackerModule.MapEndpoints`:

```csharp
writeGroup.MapPost("/keystats/samples", async (
    [FromBody] KeystatsSampleUploadRequest req,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    await svc.UpsertKeystatsSampleAsync(req, ct);
    return Results.Ok(ApiResponse<string>.Ok("ok"));
});
```

- [ ] **Step 5: Run test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~UpsertKeystatsSampleAsync_StoresRawMinuteSnapshot
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): store one-minute keystats samples"
```

## Task 6: Compute KeyStats Minute Deltas

**Files:**

- Create: `src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs`
- Test: `tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs`

- [ ] **Step 1: Write failing delta tests**

Create `tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs`:

```csharp
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class KeystatsDeltaCalculatorTests
{
    [Fact]
    public void Calculate_ReturnsDifferenceBetweenConsecutiveSamples()
    {
        var previous = Sample("2026-05-20T05:55:00+00:00", 10, 2);
        var current = Sample("2026-05-20T05:56:00+00:00", 17, 5);

        var delta = KeystatsDeltaCalculator.Calculate(previous, current);

        Assert.False(delta.IsGap);
        Assert.False(delta.IsReset);
        Assert.Equal(7, delta.KeyPresses);
        Assert.Equal(3, delta.TotalClicks);
    }

    [Fact]
    public void Calculate_MarksResetWhenCountersDecrease()
    {
        var previous = Sample("2026-05-20T05:55:00+00:00", 10, 5);
        var current = Sample("2026-05-20T05:56:00+00:00", 2, 1);

        var delta = KeystatsDeltaCalculator.Calculate(previous, current);

        Assert.True(delta.IsReset);
        Assert.Equal(0, delta.KeyPresses);
        Assert.Equal(0, delta.TotalClicks);
    }

    private static KeystatsSampleEntity Sample(string sampledAt, int keys, int leftClicks)
    {
        return new KeystatsSampleEntity
        {
            PimDeviceId = "DESKTOP",
            SampledAtUtc = DateTimeOffset.Parse(sampledAt),
            StatsDate = new DateTime(2026, 5, 20),
            KeyPresses = keys,
            LeftClicks = leftClicks
        };
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~KeystatsDeltaCalculatorTests
```

Expected: FAIL because `KeystatsDeltaCalculator` does not exist.

- [ ] **Step 3: Implement delta calculator**

Create `src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs`:

```csharp
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public record KeystatsMinuteDelta(
    string DeviceId,
    DateTimeOffset MinuteStartUtc,
    int KeyPresses,
    int TotalClicks,
    double MouseDistance,
    double ScrollDistance,
    bool IsGap,
    bool IsReset
);

public static class KeystatsDeltaCalculator
{
    public static KeystatsMinuteDelta Calculate(KeystatsSampleEntity? previous, KeystatsSampleEntity current)
    {
        if (previous is null || previous.StatsDate != current.StatsDate)
        {
            return new KeystatsMinuteDelta(
                current.PimDeviceId,
                current.SampledAtUtc,
                current.KeyPresses,
                TotalClicks(current),
                current.MouseDistance,
                current.ScrollDistance,
                true,
                false);
        }

        var keyDelta = current.KeyPresses - previous.KeyPresses;
        var clickDelta = TotalClicks(current) - TotalClicks(previous);
        var mouseDelta = current.MouseDistance - previous.MouseDistance;
        var scrollDelta = current.ScrollDistance - previous.ScrollDistance;
        var isReset = keyDelta < 0 || clickDelta < 0 || mouseDelta < 0 || scrollDelta < 0;

        return new KeystatsMinuteDelta(
            current.PimDeviceId,
            current.SampledAtUtc,
            isReset ? 0 : keyDelta,
            isReset ? 0 : clickDelta,
            isReset ? 0 : mouseDelta,
            isReset ? 0 : scrollDelta,
            (current.SampledAtUtc - previous.SampledAtUtc).TotalMinutes > 2,
            isReset);
    }

    private static int TotalClicks(KeystatsSampleEntity sample)
    {
        return sample.LeftClicks + sample.RightClicks + sample.MiddleClicks + sample.SideBackClicks + sample.SideForwardClicks;
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~KeystatsDeltaCalculatorTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs tests/Pim.UnitTests/Services/KeystatsDeltaCalculatorTests.cs
git commit -m "feat(pc): compute keystats minute deltas"
```

## Task 7: Update Windows Collectors to Upload Complete Data

**Files:**

- Modify: `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs`

- [ ] **Step 1: Extend KeyStats snapshot model**

In `KeyStatsCollectorService.KeyStatsSnapshot`, add:

```csharp
[JsonPropertyName("FormattedMouseDistance")]
public string? FormattedMouseDistance { get; set; }

[JsonPropertyName("FormattedScrollDistance")]
public string? FormattedScrollDistance { get; set; }
```

- [ ] **Step 2: Post KeyStats samples to new endpoint**

In `KeyStatsCollectorService.CollectAndUploadAsync`, after successfully reading `stats`, build and post:

```csharp
var sample = new KeystatsSampleUploadPayload(
    stats.DeviceId,
    DateTimeOffset.UtcNow.ToString("O"),
    stats.Date,
    stats.KeyPresses,
    stats.KeyPressCounts,
    stats.LeftClicks,
    stats.RightClicks,
    stats.MiddleClicks,
    stats.SideBackClicks,
    stats.SideForwardClicks,
    stats.MouseDistance,
    stats.ScrollDistance,
    stats.PeakKps,
    stats.PeakCps,
    stats.FormattedMouseDistance,
    stats.FormattedScrollDistance,
    stats.AppStats);

var result = await _api.PostAsync<ApiResponse<string>>("/pc/keystats/samples", sample, _cts.Token);
```

Keep the old `/pc/keystats/upload` call for one release if summary still depends on `pc_keystats_daily`; remove it only after summary reads from samples.

- [ ] **Step 3: Fetch AW info and bucket metadata**

In `AwCollectorService`, add fields:

```csharp
private AwInfoPayload? _awInfo;
private readonly Dictionary<string, AwBucketPayload> _bucketCache = new();
```

Add methods:

```csharp
private async Task<AwInfoPayload?> FetchAwInfoAsync()
{
    try
    {
        return await _aw.GetFromJsonAsync<AwInfoPayload>("/api/0/info", _cts.Token);
    }
    catch
    {
        return null;
    }
}

private async Task<AwBucketPayload?> FetchBucketAsync(string bucketId)
{
    if (_bucketCache.TryGetValue(bucketId, out var cached))
        return cached;

    try
    {
        var bucket = await _aw.GetFromJsonAsync<AwBucketPayload>($"/api/0/buckets/{Uri.EscapeDataString(bucketId)}", _cts.Token);
        if (bucket is not null)
            _bucketCache[bucketId] = bucket;
        return bucket;
    }
    catch
    {
        return null;
    }
}
```

- [ ] **Step 4: Upload AW complete events per bucket**

Change AW event collection so each bucket posts separately to `/pc/aw/upload-complete`:

```csharp
_awInfo ??= await FetchAwInfoAsync();
await CollectBucketAndUploadAsync(BucketId, "window");
await CollectBucketAndUploadAsync(AfkBucketId, "afk");
```

Implement:

```csharp
private async Task CollectBucketAndUploadAsync(string bucketId, string fallbackKind)
{
    var lastId = fallbackKind == "afk" ? _cursorState.LastAfkId : _cursorState.LastWindowId;
    var rawEvents = FetchNewEvents(bucketId, lastId, out var pendingLastId);
    if (rawEvents.Count == 0) return;

    var bucket = await FetchBucketAsync(bucketId);
    if (bucket is null) return;

    var events = rawEvents.Select(e => new AwEventPayload(e.Id, e.Timestamp, e.Duration, e.Data.ToDictionary(kv => kv.Key, kv => (object)kv.Value))).ToList();
    var payload = new CompleteAwUploadPayload(Environment.MachineName, _awInfo, bucket, events);
    var result = await _api.PostAsync<ApiResponse<int>>("/pc/aw/upload-complete", payload, _cts.Token);
    if (result is not null)
    {
        if (fallbackKind == "afk")
            _cursorState.RecordFetched(_cursorState.LastWindowId, pendingLastId);
        else
            _cursorState.RecordFetched(pendingLastId, _cursorState.LastAfkId);
        _cursorState.CommitFetched();
        Log?.Invoke($"[AwCollector] Uploaded {events.Count} {fallbackKind} events -> {result.Data} inserted/updated");
    }
}
```

- [ ] **Step 5: Build client core**

Run:

```powershell
dotnet build src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj
```

Expected: build succeeds.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs
git commit -m "feat(pc): upload complete local tracker samples"
```

## Task 8: Implement Mixed PC Detail Query

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Add typed detail DTOs**

In `PcTrackerDtos.cs`, add:

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
    object? Raw
);

public record TypedDetailQueryResponse(
    List<PcDetailRecord> Items,
    int Page,
    int PageSize,
    int TotalCount,
    int TotalPages
);
```

- [ ] **Step 2: Write failing detail test**

Append to `PcTrackerCompleteCaptureTests`:

```csharp
[Fact]
public async Task QueryCompleteDetailAsync_ReturnsWindowAndInputMinuteRecords()
{
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().Add(new AwEventEntity
    {
        DeviceId = "DESKTOP",
        Timestamp = DateTimeOffset.Parse("2026-05-20T05:55:00+00:00"),
        Duration = 30,
        EventType = "window",
        AppName = "msedge.exe",
        AppNameNormalized = "msedge",
        WindowTitle = "Example",
        DataJson = "{\"app\":\"msedge.exe\",\"title\":\"Example\"}"
    });
    db.Set<KeystatsSampleEntity>().AddRange(
        new KeystatsSampleEntity { PimDeviceId = "DESKTOP", SampledAtUtc = DateTimeOffset.Parse("2026-05-20T05:55:00+00:00"), StatsDate = new DateTime(2026, 5, 20), KeyPresses = 10 },
        new KeystatsSampleEntity { PimDeviceId = "DESKTOP", SampledAtUtc = DateTimeOffset.Parse("2026-05-20T05:56:00+00:00"), StatsDate = new DateTime(2026, 5, 20), KeyPresses = 15 });
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var result = await service.QueryCompleteDetailAsync(
        new DetailQueryParams("2026-05-20", "2026-05-20", null, null, null, null, null, null, null, null, 1, 20),
        CancellationToken.None);

    Assert.Contains(result.Items, x => x.RecordType == "window" && x.Title == "Example");
    Assert.Contains(result.Items, x => x.RecordType == "input-minute" && x.KeyPresses == 5);
}
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QueryCompleteDetailAsync_ReturnsWindowAndInputMinuteRecords
```

Expected: FAIL because `QueryCompleteDetailAsync` does not exist.

- [ ] **Step 4: Implement query method**

Add `QueryCompleteDetailAsync` to `PcTrackerService.cs`. It must:

- Convert `dateFrom/dateTo` to PC business-day UTC bounds using existing `BusinessDayStart`.
- Query `AwEventEntity` where `Timestamp` is inside the range.
- Query `KeystatsSampleEntity` inside the range ordered by `PimDeviceId`, `SampledAtUtc`.
- Convert consecutive samples to `input-minute` records using `KeystatsDeltaCalculator`.
- Apply filters:
  - `eventType`: record type exact match.
  - `deviceId`: `DeviceId` or `PimDeviceId`.
  - `appName`: raw or normalized app contains value.
  - `keyName`: only records with key counts containing the key.
  - `categoryName`: use existing category rules after app normalization.
- Sort by `start` descending by default.
- Return page and total counts.

Use this record creation shape:

```csharp
private static PcDetailRecord ToWindowRecord(AwEventEntity e)
{
    return new PcDetailRecord(
        e.EventType,
        e.Timestamp.ToString("O"),
        e.Timestamp.AddSeconds(e.Duration).ToString("O"),
        e.Duration,
        e.DeviceId,
        e.AppName,
        e.AppName,
        null,
        e.WindowTitle,
        null,
        null,
        null,
        null,
        null,
        e.DataJson);
}
```

- [ ] **Step 5: Update endpoint to return typed records**

In `PcTrackerModule.MapEndpoints`, change `/detail` to call:

```csharp
var result = await svc.QueryCompleteDetailAsync(q, ct);
return Results.Ok(ApiResponse<TypedDetailQueryResponse>.Ok(result));
```

- [ ] **Step 6: Run detail test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~QueryCompleteDetailAsync_ReturnsWindowAndInputMinuteRecords
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): query mixed tracker detail records"
```

## Task 9: Make Summary Prefer Complete Capture Data

**Files:**

- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Test: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Write summary preference test**

Append a test that inserts two `KeystatsSampleEntity` rows for the selected day and asserts `GetSummaryAsync` returns the latest sample values when no `KeystatsDailyEntity` exists:

```csharp
[Fact]
public async Task GetSummaryAsync_UsesLatestKeystatsSampleWhenDailySnapshotMissing()
{
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<KeystatsSampleEntity>().Add(new KeystatsSampleEntity
    {
        PimDeviceId = "DESKTOP",
        SampledAtUtc = DateTimeOffset.Parse("2026-05-20T05:56:00+00:00"),
        StatsDate = new DateTime(2026, 5, 20),
        KeyPresses = 99,
        LeftClicks = 7,
        KeyCountsJson = "{\"Space\":9}",
        AppStatsJson = "{}",
        RawJson = "{}"
    });
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var summary = await service.GetSummaryAsync(new DateTime(2026, 5, 20), CancellationToken.None);

    Assert.NotNull(summary.Keystats);
    Assert.Equal(99, summary.Keystats!.KeyPresses);
    Assert.Equal(7, summary.Keystats.TotalClicks);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GetSummaryAsync_UsesLatestKeystatsSampleWhenDailySnapshotMissing
```

Expected: FAIL because `GetSummaryAsync` only reads daily snapshot.

- [ ] **Step 3: Implement latest-sample fallback**

Add a helper that converts `KeystatsSampleEntity` into `KeystatsSummary` and app ranking:

```csharp
private static KeystatsSummary BuildKeystatsSummaryFromSample(KeystatsSampleEntity sample)
{
    var keyCounts = System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, int>>(sample.KeyCountsJson) ?? new();
    return new KeystatsSummary(
        sample.StatsDate.ToString("yyyy-MM-dd"),
        sample.KeyPresses,
        sample.LeftClicks + sample.RightClicks + sample.MiddleClicks + sample.SideBackClicks + sample.SideForwardClicks,
        sample.LeftClicks,
        sample.RightClicks,
        sample.MiddleClicks,
        sample.SideBackClicks,
        sample.SideForwardClicks,
        sample.MouseDistance,
        sample.ScrollDistance,
        sample.PeakKps,
        sample.PeakCps,
        keyCounts.OrderByDescending(kv => kv.Value).Take(10)
            .Select(kv => new KeyCountItem(kv.Key, kv.Value, sample.KeyPresses > 0 ? (double)kv.Value / sample.KeyPresses : 0))
            .ToList());
}
```

Update `GetSummaryAsync` to use latest sample if daily snapshot is null.

- [ ] **Step 4: Run summary test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~GetSummaryAsync_UsesLatestKeystatsSampleWhenDailySnapshotMissing
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat(pc): summarize from complete samples"
```

## Task 10: Update Frontend Detail Types and Panel

**Files:**

- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/pcTracker.ts`
- Modify: `src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx`

- [ ] **Step 1: Add typed detail interfaces**

In `src/client-web/src/types/index.ts`, replace `DetailQueryResponse.items: Record<string, unknown>[]` with:

```ts
export type PcDetailRecordType = 'window' | 'afk' | 'input-minute' | 'app-input' | 'key-input';

export interface PcDetailRecord {
  recordType: PcDetailRecordType | string;
  start: string;
  end: string | null;
  durationSeconds: number | null;
  deviceId: string;
  appName: string | null;
  displayName: string | null;
  categoryName: string | null;
  title: string | null;
  keyPresses: number | null;
  totalClicks: number | null;
  mouseDistance: number | null;
  scrollDistance: number | null;
  keyCounts: Record<string, number> | null;
  raw: unknown;
}

export interface DetailQueryResponse {
  items: PcDetailRecord[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}
```

- [ ] **Step 2: Add event type filter options**

In `PcDetailQueryPanel.tsx`, add event-type select values:

```tsx
<select className="w-full border rounded-lg px-3 py-2 text-sm" onChange={e => update('eventType', e.target.value)}>
  <option value="">全部</option>
  <option value="window">窗口记录</option>
  <option value="afk">空闲记录</option>
  <option value="input-minute">分钟输入</option>
  <option value="app-input">应用输入</option>
  <option value="key-input">按键明细</option>
</select>
```

- [ ] **Step 3: Render typed columns**

Replace dynamic `Object.keys(data.items[0])` table rendering with fixed columns:

```tsx
const columns = ['类型', '开始', '结束', '设备', '应用', '标题', '按键', '点击', '滚动', '时长'];
```

Render each row:

```tsx
<td>{row.recordType}</td>
<td>{new Date(row.start).toLocaleString('zh-CN')}</td>
<td>{row.end ? new Date(row.end).toLocaleString('zh-CN') : '-'}</td>
<td>{row.deviceId}</td>
<td>{row.displayName || row.appName || '-'}</td>
<td className="max-w-[320px] truncate">{row.title || '-'}</td>
<td>{row.keyPresses ?? '-'}</td>
<td>{row.totalClicks ?? '-'}</td>
<td>{row.scrollDistance ?? '-'}</td>
<td>{row.durationSeconds ? `${Math.round(row.durationSeconds)}s` : '-'}</td>
```

- [ ] **Step 4: Keep export support**

Change `downloadCSV` signature to:

```ts
function downloadCSV(items: PcDetailRecord[], filename: string) {
```

Serialize `raw` with `JSON.stringify(row.raw ?? '')` when exporting.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: TypeScript and Vite build succeed.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/pcTracker.ts src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx
git commit -m "feat(pc): render complete detail records"
```

## Task 11: Add ActivityWatch Backfill Command Path

**Files:**

- Modify: `src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs`
- Modify: `src/client-windows/Pim.Client.App/TrayIcon.cs`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`

- [ ] **Step 1: Add backfill method**

Add to `AwCollectorService`:

```csharp
public async Task BackfillAsync(DateTimeOffset startUtc, DateTimeOffset endUtc)
{
    _awInfo ??= await FetchAwInfoAsync();
    await BackfillBucketAsync(BucketId, startUtc, endUtc);
    await BackfillBucketAsync(AfkBucketId, startUtc, endUtc);
}

private async Task BackfillBucketAsync(string bucketId, DateTimeOffset startUtc, DateTimeOffset endUtc)
{
    var bucket = await FetchBucketAsync(bucketId);
    if (bucket is null) return;

    var url = $"/api/0/buckets/{Uri.EscapeDataString(bucketId)}/events?start={Uri.EscapeDataString(startUtc.ToString("O"))}&end={Uri.EscapeDataString(endUtc.ToString("O"))}";
    var events = await _aw.GetFromJsonAsync<List<RawAwEvent>>(url, _cts.Token) ?? new();
    foreach (var batch in events.Chunk(200))
    {
        var payload = new CompleteAwUploadPayload(
            Environment.MachineName,
            _awInfo,
            bucket,
            batch.Select(e => new AwEventPayload(e.Id, e.Timestamp, e.Duration, e.Data.ToDictionary(kv => kv.Key, kv => (object)kv.Value))).ToList());
        await _api.PostAsync<ApiResponse<int>>("/pc/aw/upload-complete", payload, _cts.Token);
    }
}
```

- [ ] **Step 2: Add manual backfill trigger**

Add a tray/status action that calls:

```csharp
await awCollector.BackfillAsync(DateTimeOffset.UtcNow.AddDays(-14), DateTimeOffset.UtcNow);
```

Label it `回填最近 14 天 ActivityWatch` in the UI.

- [ ] **Step 3: Build Windows app**

Run:

```powershell
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Debug
```

Expected: build succeeds. If the daemon executable is locked, stop `Pim.Client.App` first and rerun the build.

- [ ] **Step 4: Commit**

Run:

```powershell
git add src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs src/client-windows/Pim.Client.App/TrayIcon.cs src/client-windows/Pim.Client.App/StatusWindow.xaml.cs
git commit -m "feat(pc): add activitywatch backfill"
```

## Task 12: End-to-End Verification

**Files:**

- Verify only unless a previous task fails.

- [ ] **Step 1: Run backend unit tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
```

Expected: all tests pass.

- [ ] **Step 2: Build API image and start services**

Run:

```powershell
docker compose up -d --build pim-api
```

Expected: `project-pim-api-1` is healthy.

- [ ] **Step 3: Verify schema exists**

Run:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "\dt pc_aw_buckets"
docker compose exec -T postgres psql -U pim -d pim -c "\dt pc_keystats_samples"
docker compose exec -T postgres psql -U pim -d pim -c "\d pc_aw_events"
```

Expected: new tables exist and `pc_aw_events` includes `bucket_id`, `source_event_id`, `data_json`, and `app_name_normalized`.

- [ ] **Step 4: Restart daemon and wait two minutes**

Run:

```powershell
Get-Process -Name Pim.Client.App -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Process -FilePath (Resolve-Path 'src/client-windows/Pim.Client.App/bin/Debug/net8.0-windows/Pim.Client.App.exe') -WindowStyle Hidden
Start-Sleep -Seconds 130
```

Expected: daemon log shows posts to `/pc/keystats/samples` and `/pc/aw/upload-complete`.

- [ ] **Step 5: Verify stored data counts**

Run:

```powershell
docker compose exec -T postgres psql -U pim -d pim -c "select count(*) from pc_aw_buckets;"
docker compose exec -T postgres psql -U pim -d pim -c "select count(*), max(sampled_at_utc) from pc_keystats_samples;"
docker compose exec -T postgres psql -U pim -d pim -c "select count(*), count(distinct source_event_id) from pc_aw_events where bucket_id is not null;"
```

Expected: bucket count is at least 2, sample count increases after two minutes, AW event rows have source ids.

- [ ] **Step 6: Verify APIs**

Run:

```powershell
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/health"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/summary?date=2026-05-20"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=2026-05-20&dateTo=2026-05-20&pageSize=20"
Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:5858/api/v1/pc/detail?dateFrom=2026-05-20&dateTo=2026-05-20&eventType=input-minute&pageSize=20"
```

Expected: all return HTTP 200; detail includes `window` records and, after two samples, `input-minute` records.

- [ ] **Step 7: Build frontend**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: build succeeds.

- [ ] **Step 8: Handle verification fixes if any**

If verification fails, stop and return to the specific task that introduced the failing behavior. Make the fix there, rerun that task's verification command, and use that task's commit step with its exact file list. Do not create a generic verification-fixes commit.

```powershell
git status --short
```

If no fixes are required, do not create an empty commit.

## Self-Review Checklist

- Spec coverage: AW raw info, bucket metadata, source event ids, `data_json`, KeyStats 1-minute samples, raw JSON, formatted fields, app normalization, detail query, and backfill are covered.
- Completeness scan: This plan avoids unresolved work markers and names concrete files, methods, commands, and expected outcomes.
- Type consistency: Server DTO names are `CompleteAwUploadRequest`, `KeystatsSampleUploadRequest`, `PcDetailRecord`, and `TypedDetailQueryResponse`; client payload names mirror the server contracts.
- Scope control: The plan keeps existing daily tables and endpoints as compatibility paths while adding complete capture as the new source of truth.
