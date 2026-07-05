# PC Records Route 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Route 3 so PC classification corrections use one preview/apply/recompute/audit loop, stable record identity protects user corrections, and the full PC records page gains new analysis modules while preserving the existing category timeline, detail panel, and keyboard/mouse heatmap.

**Architecture:** Backend work adds a stable key service, metadata-backed classification snapshots, category/scope validation, suggestion draft/preview/apply endpoints, real recompute, and an activity analysis endpoint. Frontend work adds API contracts, a new activity analysis heatmap, a new classification action queue, and preview/apply UI while leaving `CategoryTimeline`, `EventTimelineDialog`, and `KeyboardHeatmap` behavior intact.

**Tech Stack:** .NET 9, ASP.NET Minimal APIs, EF Core, PostgreSQL-compatible schema initializer, xUnit, React 19, TypeScript, TanStack Query, Vite, GitHub Actions through `gh`.

---

## Goal-Mode Objective

Use this objective when starting goal mode:

> Implement PC Records Route 3 from `docs/superpowers/specs/2026-07-05-pc-records-understanding-display-design.md`: stable record keys and source metadata, unified suggestion/manual rule preview-apply-recompute-audit flow, authoritative category-tree validation, new activity analysis heatmap and classification action queue on the complete PC records page, preservation of the existing category timeline/detail panel/keyboard-mouse heatmap, local verification, commit/push, and GitHub Actions confirmation.

## Recommended Subagent Strategy

Use `superpowers:subagent-driven-development` for execution. Up to 19 subagents can run at once after the foundation tasks land.

Suggested maximum parallel assignment:

1. **Key/schema agent:** Tasks 1-2.
2. **Rule contract agent:** Task 3.
3. **Suggestion flow agent:** Task 4.
4. **Recompute/audit agent:** Task 5.
5. **Activity analysis API agent:** Task 6.
6. **Frontend API/types agent:** Task 7.
7. **Action queue/dialog agent:** Task 8.
8. **Full PC page integration agent:** Task 9.
9. **Verification/GA agent:** Task 10.

Additional subagents up to the 19-agent cap should be reserved for independent code reviews, focused failure investigations, migration review, frontend visual review, and GitHub Actions log triage. Do not dispatch multiple implementation subagents against the same unresolved write set.

Dependency guardrails:

- Tasks 1-2 must finish before Tasks 4-6.
- Task 3 must finish before Tasks 4-5.
- Task 7 must finish before Tasks 8-9.
- Task 10 runs after all code tasks merge.

## File Structure

Backend files to create:

- `src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs`: one service for stable record keys, key versions, source metadata, and stability level.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs`: validates scopes, rule names, conditions JSON, and target category names; owns rule persistence.
- `src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs`: builds server-owned draft rules from suggestions and manual correction input.
- `src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs`: groups interpreted PC records into time blocks for the new activity analysis heatmap.
- `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs`: PC-specific audit table for classification apply/recompute details.
- `tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs`
- `tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs`
- `tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs`

Backend files to modify:

- `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`: append optional source identity and key metadata fields to `PcDetailRecord`.
- `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`: add suggestion preview/apply, activity analysis, recompute, and key metadata DTOs.
- `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`: add key version, key stability, source type, source bucket ids, and interpretation version columns.
- `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`: configure new classification metadata and audit entity indexes.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`: add SQL for new columns and audit table.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs`: keep a compatibility wrapper over the new key service.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs`: use the key service and persist source metadata.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`: delegate validation/persistence to rule service, support real recompute, and support suggestion apply.
- `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`: remove direct accept persistence and add status helpers used by the apply service.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`: treat `scope = app` as activity-classifying for compatibility.
- `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`: propagate source bucket ids and interpretation version into `PcDetailRecord`.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`: propagate source metadata for legacy raw AW records and route rule save/list through rule service where practical.
- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: register new services and map new endpoints.
- `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`

Frontend files to create:

- `src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx`: new activity-state heatmap, separate from keyboard/mouse heatmap.
- `src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx`: new pending classification work queue.
- `src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx`: preview/apply dialog driven by server drafts.
- `src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx`: reusable preview summary panel.
- `tests/client-web/pcRoute3ApiPath.test.ts`
- `tests/client-web/pcRoute3Types.test.ts`
- `tests/client-web/pcRoute3Components.test.tsx`

Frontend files to modify:

- `src/client-web/src/api/pcTracker.ts`: add route 3 API functions and remove frontend direct accept usage from exported page flow.
- `src/client-web/src/types/index.ts`: add route 3 DTOs and remove `all` from `ActivityClassificationApplyRange.mode`.
- `src/client-web/src/pages/PcTrackerPage.tsx`: integrate new modules into the full page; preserve existing modules.
- `src/client-web/src/components/pc-tracker/ClassificationSuggestionPanel.tsx`: leave file available, but do not use the direct accept path from the PC records page.
- `tests/client-web/pcClassificationApiPath.test.ts`
- `tests/client-web/pcClassificationTypes.test.ts`

Generated files:

- New EF migration under `src/Pim.Infrastructure/Data/Migrations/` after entity changes.

Do not commit `.superpowers/brainstorm/` visual mockups.

---

### Task 0: Preflight And Branch Setup

**Files:**
- Read: `AGENTS.md`
- Read: `docs/superpowers/specs/2026-07-05-pc-records-understanding-display-design.md`
- Read: `docs/operations/migrations.md`

- [ ] **Step 1: Confirm git state**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git status --short --branch
```

Expected: current branch is known, uncommitted files are understood, and unrelated dirty files are not modified.

- [ ] **Step 2: Create an implementation branch**

Run:

```powershell
git checkout -b feat/pc-records-route-3
```

Expected: branch changes to `feat/pc-records-route-3`.

- [ ] **Step 3: Run baseline verification**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassification|ActivitySuggestion|PcTracker"
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationTypes.test.ts
```

Expected: either PASS, or failures are recorded before feature changes. If `ActivitySuggestionServiceTests` fails because the constructor now requires `AppSignatureService`, fix the test helper in Task 4 before relying on those tests.

---

### Task 1: Stable Record Key Service

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`

- [ ] **Step 1: Write failing key service tests**

Create `tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs`:

```csharp
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class PcActivityRecordKeyServiceTests
{
    [Fact]
    public void Build_PrefersAwBucketAndSourceEventId()
    {
        var record = NewRecord() with
        {
            SourceBucketIds = ["aw-watcher-window_device-1"],
            SourceWindowEventIds = [42]
        };

        var result = PcActivityRecordKeyService.Build(record);

        Assert.Equal("pc-aw-v1:aw-watcher-window_device-1:42", result.RecordKey);
        Assert.Equal("pc-aw-v1", result.KeyVersion);
        Assert.Equal("aw", result.SourceType);
        Assert.Equal("stable", result.Stability);
        Assert.Equal("[42]", result.SourceEventIdsJson);
        Assert.Equal("[\"aw-watcher-window_device-1\"]", result.SourceBucketIdsJson);
    }

    [Fact]
    public void Build_UsesSortedSourceIdsForMergedWebPage()
    {
        var record = NewRecord() with
        {
            RecordType = "web-page",
            SourceBucketIds = ["aw-watcher-web-edge_device-1"],
            SourceWebEventIds = [9, 7, 8]
        };

        var result = PcActivityRecordKeyService.Build(record);

        Assert.Equal("pc-aw-v1:aw-watcher-web-edge_device-1:7-8-9", result.RecordKey);
        Assert.Equal("stable", result.Stability);
    }

    [Fact]
    public void Build_FallsBackWithExplicitLowerStability()
    {
        var record = NewRecord() with
        {
            SourceBucketIds = null,
            SourceWebEventIds = null,
            SourceWindowEventIds = null
        };

        var result = PcActivityRecordKeyService.Build(record);

        Assert.StartsWith("pc-fallback-v1:", result.RecordKey);
        Assert.Equal("pc-fallback-v1", result.KeyVersion);
        Assert.Equal("fallback", result.SourceType);
        Assert.Equal("low", result.Stability);
    }

    [Fact]
    public void Build_FallbackIgnoresClassificationFields()
    {
        var first = NewRecord() with { CategoryName = "Other", ClassificationExplanation = "first" };
        var second = first with { CategoryName = "Learning", ClassificationExplanation = "second" };

        Assert.Equal(
            PcActivityRecordKeyService.Build(first).RecordKey,
            PcActivityRecordKeyService.Build(second).RecordKey);
    }

    private static PcDetailRecord NewRecord() =>
        new(
            "window",
            "2026-07-05T01:00:00Z",
            "2026-07-05T01:10:00Z",
            600,
            "device-1",
            "Code.exe",
            "code",
            "Other",
            "Program.cs",
            null,
            null,
            null,
            null,
            null,
            null);
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PcActivityRecordKeyServiceTests
```

Expected: FAIL because the service and DTO fields do not exist.

- [ ] **Step 2: Append key metadata to `PcDetailRecord`**

Modify `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs` by appending these optional parameters to the end of `PcDetailRecord`, after `string? BucketType = null`:

```csharp
,
    string? RecordKey = null,
    string? RecordKeyVersion = null,
    string? RecordKeyStability = null,
    List<string>? SourceBucketIds = null,
    string? SourceType = null,
    string? InterpretationVersion = null
```

Keep the existing parameter order unchanged before these new fields so current constructors still compile.

- [ ] **Step 3: Add the key service**

Create `src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs`:

```csharp
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Pim.Module.PcTracker.DTOs;

namespace Pim.Module.PcTracker.Services;

public sealed record PcActivityRecordKeyResult(
    string RecordKey,
    string KeyVersion,
    string Stability,
    string SourceType,
    string SourceEventIdsJson,
    string SourceBucketIdsJson);

public sealed class PcActivityRecordKeyService
{
    public PcActivityRecordKeyResult BuildKey(PcDetailRecord record) => Build(record);

    public static PcActivityRecordKeyResult Build(PcDetailRecord record)
    {
        ArgumentNullException.ThrowIfNull(record);

        var sourceIds = SourceEventIds(record);
        var bucketIds = SourceBucketIds(record);
        if (sourceIds.Count > 0 && bucketIds.Count > 0)
        {
            var eventPart = string.Join('-', sourceIds);
            var bucketPart = bucketIds.Count == 1
                ? bucketIds[0]
                : HashPart(string.Join('|', bucketIds));

            return new PcActivityRecordKeyResult(
                $"pc-aw-v1:{bucketPart}:{eventPart}",
                "pc-aw-v1",
                "stable",
                "aw",
                JsonSerializer.Serialize(sourceIds),
                JsonSerializer.Serialize(bucketIds));
        }

        var fallbackPayload = string.Join(
            "\n",
            record.RecordType ?? string.Empty,
            record.DeviceId ?? string.Empty,
            record.Start ?? string.Empty,
            record.End ?? record.Start ?? string.Empty,
            record.AppName ?? record.BrowserAppName ?? string.Empty,
            record.Domain ?? string.Empty,
            record.Path ?? string.Empty,
            record.Title ?? record.BrowserWindowTitle ?? string.Empty);

        return new PcActivityRecordKeyResult(
            $"pc-fallback-v1:{HashPart(fallbackPayload)}",
            "pc-fallback-v1",
            "low",
            "fallback",
            JsonSerializer.Serialize(sourceIds),
            JsonSerializer.Serialize(bucketIds));
    }

    public static IReadOnlyList<long> SourceEventIds(PcDetailRecord record)
    {
        var ids = record.SourceWebEventIds is { Count: > 0 }
            ? record.SourceWebEventIds
            : record.SourceWindowEventIds is { Count: > 0 }
                ? record.SourceWindowEventIds
                : [];

        return ids
            .Distinct()
            .OrderBy(id => id)
            .ToList();
    }

    public static IReadOnlyList<string> SourceBucketIds(PcDetailRecord record)
    {
        return (record.SourceBucketIds ?? [])
            .Where(value => !string.IsNullOrWhiteSpace(value))
            .Select(value => value.Trim())
            .Distinct(StringComparer.Ordinal)
            .Order(StringComparer.Ordinal)
            .ToList();
    }

    private static string HashPart(string payload)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(payload));
        return Convert.ToHexString(hash).ToLowerInvariant()[..32];
    }
}
```

- [ ] **Step 4: Keep the existing static wrapper compatible**

Modify `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs` so existing callers still work:

```csharp
using Pim.Module.PcTracker.DTOs;
using System.Text.Json;

namespace Pim.Module.PcTracker.Services;

public static class ActivityClassificationRecordKey
{
    public static string FromRecord(PcDetailRecord record) =>
        PcActivityRecordKeyService.Build(record).RecordKey;

    public static string SourceEventIdsJson(PcDetailRecord record) =>
        PcActivityRecordKeyService.Build(record).SourceEventIdsJson;

    public static string SourceBucketIdsJson(PcDetailRecord record) =>
        PcActivityRecordKeyService.Build(record).SourceBucketIdsJson;

    public static string KeyVersion(PcDetailRecord record) =>
        PcActivityRecordKeyService.Build(record).KeyVersion;

    public static string KeyStability(PcDetailRecord record) =>
        PcActivityRecordKeyService.Build(record).Stability;
}
```

If `System.Text.Json` is unused after editing, remove that using.

- [ ] **Step 5: Register the service**

Modify `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs` in `RegisterServices`:

```csharp
services.AddScoped<PcActivityRecordKeyService>();
```

- [ ] **Step 6: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "PcActivityRecordKeyServiceTests|ActivityClassificationSnapshotServiceTests"
```

Expected: key service tests PASS; snapshot tests may fail until Task 2 persists metadata.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs
git commit -m "feat: add stable pc activity record keys"
```

---

### Task 2: Persist Source Identity Metadata

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`
- Create: `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`
- Test: `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`
- Generated: `src/Pim.Infrastructure/Data/Migrations/*AddPcRoute3ClassificationMetadata*`

- [ ] **Step 1: Add failing snapshot metadata test**

Append to `tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs`:

```csharp
[Fact]
public async Task EnsureClassificationsAsync_PersistsKeyVersionSourceBucketsAndStability()
{
    using var db = CreateDb();
    var service = new ActivityClassificationSnapshotService(db, NullLogger<ActivityClassificationSnapshotService>.Instance);
    var record = NewRecord("Code.exe", "Program.cs") with
    {
        SourceBucketIds = ["aw-watcher-window_device-1"],
        SourceWindowEventIds = [123],
        InterpretationVersion = "interpreted-aw-v1"
    };

    await service.EnsureClassificationsAsync(
        [record],
        [NewRule("Code is programming", "Programming")],
        null,
        CancellationToken.None);

    var snapshot = await db.Set<ActivityClassificationEntity>().SingleAsync();
    Assert.Equal("pc-aw-v1", snapshot.RecordKeyVersion);
    Assert.Equal("stable", snapshot.RecordKeyStability);
    Assert.Equal("aw", snapshot.SourceType);
    Assert.Equal("[\"aw-watcher-window_device-1\"]", snapshot.SourceBucketIdsJson);
    Assert.Equal("interpreted-aw-v1", snapshot.InterpretationVersion);
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter EnsureClassificationsAsync_PersistsKeyVersionSourceBucketsAndStability
```

Expected: FAIL because entity fields do not exist.

- [ ] **Step 2: Add classification metadata fields**

Modify `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs`:

```csharp
[Column("record_key_version")]
[MaxLength(32)]
public string RecordKeyVersion { get; set; } = "pc-fallback-v1";

[Column("record_key_stability")]
[MaxLength(16)]
public string RecordKeyStability { get; set; } = "low";

[Column("source_type")]
[MaxLength(32)]
public string SourceType { get; set; } = "fallback";

[Column("source_bucket_ids", TypeName = "jsonb")]
public string SourceBucketIdsJson { get; set; } = "[]";

[Column("interpretation_version")]
[MaxLength(32)]
public string InterpretationVersion { get; set; } = "interpreted-aw-v1";
```

- [ ] **Step 3: Add PC-specific audit entity**

Create `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationAuditEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_activity_classification_audits")]
public class ActivityClassificationAuditEntity
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; }

    [Column("operation")]
    [MaxLength(64)]
    public string Operation { get; set; } = string.Empty;

    [Column("rule_id")]
    public Guid? RuleId { get; set; }

    [Column("suggestion_id")]
    public Guid? SuggestionId { get; set; }

    [Column("range_mode")]
    [MaxLength(16)]
    public string RangeMode { get; set; } = string.Empty;

    [Column("date_from")]
    [MaxLength(16)]
    public string? DateFrom { get; set; }

    [Column("date_to")]
    [MaxLength(16)]
    public string? DateTo { get; set; }

    [Column("affected_record_count")]
    public int AffectedRecordCount { get; set; }

    [Column("affected_duration_seconds")]
    public double AffectedDurationSeconds { get; set; }

    [Column("affected_record_keys", TypeName = "jsonb")]
    public string AffectedRecordKeysJson { get; set; } = "[]";

    [Column("created_by_user_id")]
    public Guid? CreatedByUserId { get; set; }

    [Column("created_at")]
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 4: Configure entity metadata**

Modify `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs` inside `ActivityClassificationEntityConfiguration.Configure`:

```csharp
builder.Property(e => e.RecordKeyVersion).HasDefaultValue("pc-fallback-v1");
builder.Property(e => e.RecordKeyStability).HasDefaultValue("low");
builder.Property(e => e.SourceType).HasDefaultValue("fallback");
builder.Property(e => e.SourceBucketIdsJson).HasDefaultValueSql("'[]'::jsonb");
builder.Property(e => e.InterpretationVersion).HasDefaultValue("interpreted-aw-v1");
builder.HasIndex(e => e.RecordKeyVersion)
    .HasDatabaseName("ix_pc_activity_classifications_record_key_version");
builder.HasIndex(e => e.SourceType)
    .HasDatabaseName("ix_pc_activity_classifications_source_type");
```

Add a new configuration class:

```csharp
public class ActivityClassificationAuditEntityConfiguration : IEntityTypeConfiguration<ActivityClassificationAuditEntity>
{
    public void Configure(EntityTypeBuilder<ActivityClassificationAuditEntity> builder)
    {
        builder.ToTable("pc_activity_classification_audits");
        builder.Property(e => e.Id).HasDefaultValueSql("gen_random_uuid()");
        builder.Property(e => e.AffectedRecordKeysJson).HasDefaultValueSql("'[]'::jsonb");
        builder.Property(e => e.CreatedAt).HasDefaultValueSql("NOW()");
        builder.HasIndex(e => e.RuleId).HasDatabaseName("ix_pc_activity_classification_audits_rule_id");
        builder.HasIndex(e => e.SuggestionId).HasDatabaseName("ix_pc_activity_classification_audits_suggestion_id");
        builder.HasIndex(e => e.CreatedAt).HasDatabaseName("ix_pc_activity_classification_audits_created_at");
    }
}
```

- [ ] **Step 5: Persist metadata in snapshots**

Modify `ActivityClassificationSnapshotService.ApplySnapshot`:

```csharp
var key = PcActivityRecordKeyService.Build(record);
snapshot.RecordKeyVersion = key.KeyVersion;
snapshot.RecordKeyStability = key.Stability;
snapshot.SourceType = key.SourceType;
snapshot.SourceEventIdsJson = key.SourceEventIdsJson;
snapshot.SourceBucketIdsJson = key.SourceBucketIdsJson;
snapshot.InterpretationVersion = record.InterpretationVersion ?? "interpreted-aw-v1";
```

Keep existing category, confidence, and audit assignment intact.

- [ ] **Step 6: Propagate source bucket ids from ActivityWatch records**

In `BrowserPageTimelineBuilder`, add a helper:

```csharp
private static List<string> SourceBucketIds(IEnumerable<AwEventEntity> events)
{
    return events
        .Select(e => e.BucketId)
        .Where(id => !string.IsNullOrWhiteSpace(id))
        .Select(id => id!)
        .Distinct(StringComparer.Ordinal)
        .Order(StringComparer.Ordinal)
        .ToList();
}
```

In `ToRawAwRecord`, pass:

```csharp
SourceBucketIds: SourceBucketIds(new[] { e }),
SourceType: e.SourceEventId is null || string.IsNullOrWhiteSpace(e.BucketId) ? "fallback" : "aw",
InterpretationVersion: "interpreted-aw-v1"
```

In `WebPageCluster.ToDetailPage`, pass:

```csharp
SourceBucketIds: SourceBucketIds(browserWindow is null ? allWebEvents : allWebEvents.Append(browserWindow)),
SourceType: "aw",
InterpretationVersion: "interpreted-aw-v1"
```

In `PcTrackerService.ToAwDetailRecord`, pass:

```csharp
SourceBucketIds: string.IsNullOrWhiteSpace(e.BucketId) ? null : [e.BucketId],
SourceType: e.SourceEventId is null || string.IsNullOrWhiteSpace(e.BucketId) ? "fallback" : "aw",
InterpretationVersion: "raw-aw-v1"
```

- [ ] **Step 7: Update schema initializer SQL**

Modify `PcTrackerSchemaInitializer.SchemaSql` after the `pc_activity_classifications` table:

```sql
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS record_key_version VARCHAR(32) NOT NULL DEFAULT 'pc-fallback-v1';
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS record_key_stability VARCHAR(16) NOT NULL DEFAULT 'low';
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'fallback';
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS source_bucket_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS interpretation_version VARCHAR(32) NOT NULL DEFAULT 'interpreted-aw-v1';
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_record_key_version
    ON pc_activity_classifications (record_key_version);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classifications_source_type
    ON pc_activity_classifications (source_type);

CREATE TABLE IF NOT EXISTS pc_activity_classification_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation VARCHAR(64) NOT NULL,
    rule_id UUID,
    suggestion_id UUID,
    range_mode VARCHAR(16) NOT NULL,
    date_from VARCHAR(16),
    date_to VARCHAR(16),
    affected_record_count INT NOT NULL DEFAULT 0,
    affected_duration_seconds DOUBLE PRECISION NOT NULL DEFAULT 0,
    affected_record_keys JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_by_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_audits_rule_id
    ON pc_activity_classification_audits (rule_id);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_audits_suggestion_id
    ON pc_activity_classification_audits (suggestion_id);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_audits_created_at
    ON pc_activity_classification_audits (created_at);
```

- [ ] **Step 8: Add EF migration**

Run:

```powershell
dotnet ef migrations add AddPcRoute3ClassificationMetadata --project src\Pim.Infrastructure --startup-project src\Pim.Api --context PimDbContext --output-dir Data\Migrations
```

Expected: migration adds the new columns and `pc_activity_classification_audits`.

- [ ] **Step 9: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassificationSnapshotServiceTests|PimPcTrackerModelTests|PcActivityRecordKeyServiceTests"
```

Expected: PASS.

- [ ] **Step 10: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/Entities src/modules/Pim.Module.PcTracker/Services src/modules/Pim.Module.PcTracker/DTOs src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs
git commit -m "feat: persist pc classification source metadata"
```

---

### Task 3: Classification Rule Contract And Category Validation

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`

- [ ] **Step 1: Write failing scope compatibility test**

Append to `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`:

```csharp
[Fact]
public void Classify_AppScopeRuleClassifiesActivityForCompatibility()
{
    var context = CreateContext(
        RecordType: "window",
        AppName: "Code.exe",
        AppNameNormalized: "code",
        Title: "Program.cs",
        BucketType: "aw-watcher-window");
    var rule = new ActivityCategoryRuleEntity
    {
        Id = Guid.NewGuid(),
        RuleName = "Code app scope",
        Scope = "app",
        Status = "active",
        CategoryName = "Programming",
        Color = "#2563eb",
        Priority = 1000,
        Source = "user",
        ConditionsJson = """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""",
        Confidence = 0.95,
        Explanation = "App scope compatibility."
    };

    var result = ActivityClassifier.Classify(context, [rule]);

    Assert.Equal("Programming", result.CategoryName);
    Assert.Equal("rule", result.Source);
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter Classify_AppScopeRuleClassifiesActivityForCompatibility
```

Expected: FAIL because `scope = app` is ignored.

- [ ] **Step 2: Implement scope compatibility**

Modify `ActivityClassifier.CanClassifyActivity`:

```csharp
return string.Equals(rule.Scope, "activity", StringComparison.OrdinalIgnoreCase)
    || string.Equals(rule.Scope, "both", StringComparison.OrdinalIgnoreCase)
    || string.Equals(rule.Scope, "app", StringComparison.OrdinalIgnoreCase);
```

- [ ] **Step 3: Write failing rule service tests**

Create `tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationRuleServiceTests
{
    [Fact]
    public async Task SaveAsync_NormalizesAppScopeAndRequiresKnownCategory()
    {
        await using var db = CreateDb();
        db.Set<PcCategoryEntity>().Add(new PcCategoryEntity { Id = Guid.NewGuid(), Name = "Programming", Color = "#2563eb" });
        await db.SaveChangesAsync();
        var service = new ActivityClassificationRuleService(db);

        var rule = await service.SaveAsync(NewRule() with { Scope = "app", CategoryName = "Programming" }, CancellationToken.None);

        Assert.Equal("activity", rule.Scope);
        Assert.Equal("Programming", rule.CategoryName);
    }

    [Fact]
    public async Task SaveAsync_RejectsUnknownCategory()
    {
        await using var db = CreateDb();
        var service = new ActivityClassificationRuleService(db);

        var ex = await Assert.ThrowsAsync<ArgumentException>(() =>
            service.SaveAsync(NewRule() with { CategoryName = "Missing" }, CancellationToken.None));

        Assert.Contains("CategoryName", ex.Message);
    }

    [Fact]
    public async Task SaveAsync_RejectsDuplicateRuleName()
    {
        await using var db = CreateDb();
        db.Set<PcCategoryEntity>().Add(new PcCategoryEntity { Id = Guid.NewGuid(), Name = "Programming", Color = "#2563eb" });
        db.Set<ActivityCategoryRuleEntity>().Add(new ActivityCategoryRuleEntity
        {
            Id = Guid.NewGuid(),
            RuleName = "Code windows",
            Scope = "activity",
            CategoryName = "Programming",
            Status = "active",
            ConditionsJson = """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}"""
        });
        await db.SaveChangesAsync();
        var service = new ActivityClassificationRuleService(db);

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            service.SaveAsync(NewRule(), CancellationToken.None));
    }

    private static SaveActivityClassificationRuleRequest NewRule() =>
        new(
            "Code windows",
            "activity",
            "Programming",
            null,
            "#2563eb",
            900,
            """{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}""",
            0.95,
            "Matched Code.");

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityCategoryRuleEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRuleServiceTests
```

Expected: FAIL because the service does not exist.

- [ ] **Step 4: Add rule service**

Create `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs`:

```csharp
using System.Text.Json;
using System.Text.RegularExpressions;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public sealed class ActivityClassificationRuleService
{
    private static readonly TimeSpan RegexTimeout = TimeSpan.FromMilliseconds(100);
    private readonly PimDbContext _db;

    public ActivityClassificationRuleService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<List<ActivityCategoryRuleEntity>> LoadActiveAsync(CancellationToken ct)
    {
        return await _db.Set<ActivityCategoryRuleEntity>()
            .Where(rule => rule.Status == "active")
            .OrderByDescending(rule => rule.Priority)
            .ThenByDescending(rule => rule.CreatedAt)
            .ThenBy(rule => rule.RuleName)
            .ThenBy(rule => rule.Id)
            .ToListAsync(ct);
    }

    public async Task<List<ActivityClassificationRuleDto>> ListAsync(CancellationToken ct)
    {
        return await _db.Set<ActivityCategoryRuleEntity>()
            .OrderByDescending(rule => rule.Priority)
            .ThenBy(rule => rule.RuleName)
            .Select(rule => ToDto(rule))
            .ToListAsync(ct);
    }

    public async Task<ActivityClassificationRuleDto> SaveAsync(
        SaveActivityClassificationRuleRequest request,
        CancellationToken ct)
    {
        await ValidateAsync(request, ensureUniqueRuleName: true, ct);
        var rule = ToEntity(request);
        _db.Set<ActivityCategoryRuleEntity>().Add(rule);
        await _db.SaveChangesAsync(ct);
        return ToDto(rule);
    }

    public async Task ValidateAsync(
        SaveActivityClassificationRuleRequest request,
        bool ensureUniqueRuleName,
        CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(request.RuleName))
            throw new ArgumentException("RuleName is required.", nameof(request));

        ValidateConditionsJson(request.ConditionsJson);
        if (ensureUniqueRuleName
            && await _db.Set<ActivityCategoryRuleEntity>().AnyAsync(rule => rule.RuleName == request.RuleName, ct))
            throw new InvalidOperationException($"Activity classification rule '{request.RuleName}' already exists.");

        if (!string.IsNullOrWhiteSpace(request.CategoryName))
        {
            var exists = await _db.Set<PcCategoryEntity>()
                .AnyAsync(category => category.Name == request.CategoryName, ct);
            if (!exists)
                throw new ArgumentException($"CategoryName '{request.CategoryName}' does not exist.", nameof(request));
        }
    }

    public static string NormalizeScope(string? scope)
    {
        var normalized = (scope ?? string.Empty).Trim().ToLowerInvariant();
        return normalized switch
        {
            "" => "activity",
            "app" => "activity",
            "activity" => "activity",
            "both" => "both",
            "project" => "project",
            _ => throw new ArgumentException($"Unsupported classification rule scope '{scope}'.")
        };
    }

    public static ActivityCategoryRuleEntity ToEntity(SaveActivityClassificationRuleRequest request)
    {
        var now = DateTimeOffset.UtcNow;
        return new ActivityCategoryRuleEntity
        {
            Id = Guid.NewGuid(),
            RuleName = request.RuleName.Trim(),
            Scope = NormalizeScope(request.Scope),
            CategoryName = string.IsNullOrWhiteSpace(request.CategoryName) ? null : request.CategoryName.Trim(),
            ProjectTag = string.IsNullOrWhiteSpace(request.ProjectTag) ? null : request.ProjectTag.Trim(),
            Color = string.IsNullOrWhiteSpace(request.Color) ? "#64748b" : request.Color.Trim(),
            Priority = request.Priority,
            Source = "user",
            Status = "active",
            ConditionsJson = request.ConditionsJson,
            Confidence = request.Confidence,
            Explanation = request.Explanation,
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    public static ActivityClassificationRuleDto ToDto(ActivityCategoryRuleEntity rule) =>
        new(
            rule.Id,
            rule.RuleName,
            rule.Scope,
            rule.CategoryName,
            rule.ProjectTag,
            rule.Color,
            rule.Priority,
            rule.Source,
            rule.Status,
            rule.ConditionsJson,
            rule.Confidence,
            rule.Explanation);

    private static void ValidateConditionsJson(string? conditionsJson)
    {
        if (string.IsNullOrWhiteSpace(conditionsJson))
            throw new ArgumentException("ConditionsJson is required.");

        using var document = JsonDocument.Parse(conditionsJson);
        var root = document.RootElement;
        if (root.ValueKind != JsonValueKind.Object
            || !root.TryGetProperty("all", out var allConditions)
            || allConditions.ValueKind != JsonValueKind.Array
            || allConditions.GetArrayLength() == 0)
            throw new ArgumentException("ConditionsJson must contain a non-empty all array.");

        foreach (var condition in allConditions.EnumerateArray())
        {
            if (condition.ValueKind != JsonValueKind.Object
                || !condition.TryGetProperty("field", out var field)
                || !condition.TryGetProperty("op", out var op)
                || !condition.TryGetProperty("value", out var value)
                || field.ValueKind != JsonValueKind.String
                || op.ValueKind != JsonValueKind.String)
                throw new ArgumentException("Each condition must include field, op, and value.");

            if (op.GetString() == "regex"
                && value.ValueKind == JsonValueKind.String
                && !string.IsNullOrWhiteSpace(value.GetString()))
                _ = new Regex(value.GetString()!, RegexOptions.IgnoreCase | RegexOptions.CultureInvariant, RegexTimeout);
        }
    }
}
```

- [ ] **Step 5: Register and wire rule service**

In `PcTrackerModule.RegisterServices`:

```csharp
services.AddScoped<ActivityClassificationRuleService>();
```

In `/classification/rules`, use the service:

```csharp
readGroup.MapGet("/classification/rules", async (
    [FromServices] ActivityClassificationRuleService svc,
    CancellationToken ct) =>
{
    var rules = await svc.ListAsync(ct);
    return Results.Ok(ApiResponse<List<ActivityClassificationRuleDto>>.Ok(rules));
});
```

In direct `POST /classification/rules`, either remove the endpoint or make it validate and save through `ActivityClassificationRuleService.SaveAsync`. Keep this endpoint only for compatibility and do not use it from Route 3 UI.

- [ ] **Step 6: Use rule service from recompute**

Modify `ActivityClassificationRecomputeService` constructor to accept `ActivityClassificationRuleService ruleService`. Replace duplicate active-rule loading and unique-name validation:

```csharp
private readonly ActivityClassificationRuleService _rules;
```

Use:

```csharp
await _rules.ValidateAsync(ruleRequest, ensureUniqueRuleName: false, ct);
var existingRules = await _rules.LoadActiveAsync(ct);
await _rules.ValidateAsync(ruleRequest, ensureUniqueRuleName: true, ct);
var rule = ActivityClassificationRuleService.ToEntity(ruleRequest);
```

Keep preview behavior unchanged.

- [ ] **Step 7: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassificationRuleServiceTests|ActivityClassifierTests|ActivityClassificationRecomputeServiceTests"
```

Expected: PASS after test helpers instantiate the recompute service with `ActivityClassificationRuleService`.

- [ ] **Step 8: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassificationRuleServiceTests.cs tests/Pim.UnitTests/Services/ActivityClassifierTests.cs tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs
git commit -m "feat: normalize pc classification rule contracts"
```

---

### Task 4: Suggestion Draft, Preview, And Apply Flow

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`

- [ ] **Step 1: Fix stale suggestion service test helper**

Modify `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs` to instantiate `ActivitySuggestionService` with an `AppSignatureService`:

```csharp
private static ActivitySuggestionService CreateService(PimDbContext db) =>
    new(db, new AppSignatureService(db));
```

Replace `new ActivitySuggestionService(db)` in that file with `CreateService(db)`.

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivitySuggestionServiceTests
```

Expected: current tests compile before behavior changes.

- [ ] **Step 2: Add route 3 DTOs**

Append to `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`:

```csharp
public record SuggestionClassificationPreviewRequest(
    string? CategoryName,
    string? ProjectTag,
    ActivityClassificationApplyRangeRequest Range);

public record SuggestionClassificationApplyRequest(
    string? CategoryName,
    string? ProjectTag,
    ActivityClassificationApplyRangeRequest Range);

public record ActivityClassificationSuggestionPreviewDto(
    SaveActivityClassificationRuleRequest Rule,
    ActivityClassificationPreviewDto Preview);

public record ActivityClassificationSuggestionApplyDto(
    ActivityClassificationRuleDto Rule,
    ActivityClassificationPreviewDto Preview,
    Guid AuditId,
    string SuggestionStatus);
```

- [ ] **Step 3: Write failing draft service tests**

Create `tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ClassificationRuleDraftServiceTests
{
    [Fact]
    public async Task BuildSuggestionDraftAsync_CreatesDomainRuleForWebCluster()
    {
        await using var db = CreateDb();
        var suggestion = NewSuggestion("web:docs.example.com");
        db.Set<ActivityClassificationSuggestionEntity>().Add(suggestion);
        await db.SaveChangesAsync();
        var service = new ClassificationRuleDraftService(db);

        var rule = await service.BuildSuggestionDraftAsync(
            suggestion.Id,
            new SuggestionClassificationPreviewRequest(
                "Learning",
                "Docs",
                new ActivityClassificationApplyRangeRequest("today", "2026-07-05", "2026-07-05")),
            CancellationToken.None);

        Assert.Equal("activity", rule.Scope);
        Assert.Equal("Learning", rule.CategoryName);
        Assert.Equal("Docs", rule.ProjectTag);
        Assert.Contains("\"field\":\"domain\"", rule.ConditionsJson);
        Assert.Contains("\"op\":\"domainSuffix\"", rule.ConditionsJson);
        Assert.Contains("\"value\":\"docs.example.com\"", rule.ConditionsJson);
    }

    [Fact]
    public async Task BuildSuggestionDraftAsync_CreatesAppRuleForAppCluster()
    {
        await using var db = CreateDb();
        var suggestion = NewSuggestion("app:code");
        db.Set<ActivityClassificationSuggestionEntity>().Add(suggestion);
        await db.SaveChangesAsync();
        var service = new ClassificationRuleDraftService(db);

        var rule = await service.BuildSuggestionDraftAsync(
            suggestion.Id,
            new SuggestionClassificationPreviewRequest(
                "Programming",
                null,
                new ActivityClassificationApplyRangeRequest("today", "2026-07-05", "2026-07-05")),
            CancellationToken.None);

        Assert.Contains("\"field\":\"appNameNormalized\"", rule.ConditionsJson);
        Assert.Contains("\"value\":\"code\"", rule.ConditionsJson);
    }

    private static ActivityClassificationSuggestionEntity NewSuggestion(string clusterKey) =>
        new()
        {
            Id = Guid.NewGuid(),
            ClusterKey = clusterKey,
            Status = "pending",
            SampleCount = 1,
            TotalDurationSeconds = 600,
            CurrentCategory = "Other"
        };

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationSuggestionEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ClassificationRuleDraftServiceTests
```

Expected: FAIL because the service does not exist.

- [ ] **Step 4: Add rule draft service**

Create `src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs`:

```csharp
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public sealed class ClassificationRuleDraftService
{
    private readonly PimDbContext _db;

    public ClassificationRuleDraftService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<SaveActivityClassificationRuleRequest> BuildSuggestionDraftAsync(
        Guid suggestionId,
        SuggestionClassificationPreviewRequest request,
        CancellationToken ct)
    {
        var suggestion = await _db.Set<ActivityClassificationSuggestionEntity>()
            .FirstOrDefaultAsync(item => item.Id == suggestionId, ct)
            ?? throw new KeyNotFoundException($"Activity classification suggestion '{suggestionId}' was not found.");

        if (!string.Equals(suggestion.Status, "pending", StringComparison.Ordinal))
            throw new InvalidOperationException($"Suggestion '{suggestionId}' must be pending before preview or apply.");

        var condition = BuildCondition(suggestion.ClusterKey);
        var category = request.CategoryName ?? suggestion.SuggestedCategory ?? suggestion.CurrentCategory;
        var projectTag = request.ProjectTag ?? suggestion.SuggestedProjectTag;
        var ruleName = $"Suggestion: {suggestion.ClusterKey} {DateTimeOffset.UtcNow:yyyyMMddHHmmss}";

        return new SaveActivityClassificationRuleRequest(
            ruleName,
            "activity",
            string.IsNullOrWhiteSpace(category) ? null : category,
            string.IsNullOrWhiteSpace(projectTag) ? null : projectTag,
            "#64748b",
            900,
            JsonSerializer.Serialize(new { all = new[] { condition } }),
            0.95,
            $"Created from suggestion {suggestion.Id}.");
    }

    private static object BuildCondition(string clusterKey)
    {
        var separator = clusterKey.IndexOf(':');
        if (separator <= 0 || separator == clusterKey.Length - 1)
            throw new ArgumentException($"Unsupported suggestion cluster key '{clusterKey}'.");

        var kind = clusterKey[..separator].Trim().ToLowerInvariant();
        var value = clusterKey[(separator + 1)..].Trim();
        if (string.IsNullOrWhiteSpace(value))
            throw new ArgumentException($"Unsupported suggestion cluster key '{clusterKey}'.");

        return kind switch
        {
            "web" => new { field = "domain", op = "domainSuffix", value },
            "app" => new { field = "appNameNormalized", op = "equals", value },
            _ => throw new ArgumentException($"Unsupported suggestion cluster key '{clusterKey}'.")
        };
    }
}
```

Register in `PcTrackerModule.RegisterServices`:

```csharp
services.AddScoped<ClassificationRuleDraftService>();
```

- [ ] **Step 5: Add suggestion preview and apply methods**

Modify `ActivityClassificationRecomputeService` to add methods:

```csharp
public async Task<ActivityClassificationSuggestionPreviewDto> PreviewSuggestionAsync(
    Guid suggestionId,
    SuggestionClassificationPreviewRequest request,
    ClassificationRuleDraftService drafts,
    CancellationToken ct)
{
    var rule = await drafts.BuildSuggestionDraftAsync(suggestionId, request, ct);
    var preview = await PreviewRuleAsync(rule, request.Range, ct);
    return new ActivityClassificationSuggestionPreviewDto(rule, preview);
}

public async Task<ActivityClassificationSuggestionApplyDto> ApplySuggestionAsync(
    Guid suggestionId,
    SuggestionClassificationApplyRequest request,
    ClassificationRuleDraftService drafts,
    CancellationToken ct)
{
    var previewRequest = new SuggestionClassificationPreviewRequest(
        request.CategoryName,
        request.ProjectTag,
        request.Range);
    var rule = await drafts.BuildSuggestionDraftAsync(suggestionId, previewRequest, ct);
    var preview = await ApplyRuleAsync(rule, request.Range, ct);

    var suggestion = await _db.Set<ActivityClassificationSuggestionEntity>()
        .FirstAsync(item => item.Id == suggestionId, ct);
    suggestion.Status = "accepted";
    suggestion.UpdatedAt = DateTimeOffset.UtcNow;
    await _db.SaveChangesAsync(ct);

    var savedRule = await _db.Set<ActivityCategoryRuleEntity>()
        .OrderByDescending(item => item.CreatedAt)
        .FirstAsync(item => item.RuleName == rule.RuleName, ct);

    var audit = await _db.Set<ActivityClassificationAuditEntity>()
        .OrderByDescending(item => item.CreatedAt)
        .FirstOrDefaultAsync(item => item.RuleId == savedRule.Id, ct);

    return new ActivityClassificationSuggestionApplyDto(
        ActivityClassificationRuleService.ToDto(savedRule),
        preview,
        audit?.Id ?? Guid.Empty,
        suggestion.Status);
}
```

Extract a private `ApplyRuleCoreAsync` from the existing `ApplyRuleAsync` so both public rule apply and suggestion apply share one transaction. The core method should save the rule, write audit rows, recompute snapshots, optionally mark a suggestion accepted, commit once, and return the saved rule plus preview.

- [ ] **Step 6: Map suggestion preview/apply endpoints**

Modify `PcTrackerModule.MapEndpoints`:

```csharp
writeGroup.MapPost("/classification/suggestions/{id:guid}/preview", async (
    Guid id,
    [FromBody] SuggestionClassificationPreviewRequest req,
    [FromServices] ActivityClassificationRecomputeService recompute,
    [FromServices] ClassificationRuleDraftService drafts,
    CancellationToken ct) =>
{
    try
    {
        var preview = await recompute.PreviewSuggestionAsync(id, req, drafts, ct);
        return Results.Ok(ApiResponse<ActivityClassificationSuggestionPreviewDto>.Ok(preview));
    }
    catch (KeyNotFoundException)
    {
        return Results.NotFound(ApiResponse<string>.Error(404, "Suggestion not found."));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
    catch (InvalidOperationException ex)
    {
        return Results.Conflict(ApiResponse<string>.Error(409, ex.Message));
    }
});

writeGroup.MapPost("/classification/suggestions/{id:guid}/apply", async (
    Guid id,
    [FromBody] SuggestionClassificationApplyRequest req,
    [FromServices] ActivityClassificationRecomputeService recompute,
    [FromServices] ClassificationRuleDraftService drafts,
    CancellationToken ct) =>
{
    try
    {
        var result = await recompute.ApplySuggestionAsync(id, req, drafts, ct);
        return Results.Ok(ApiResponse<ActivityClassificationSuggestionApplyDto>.Ok(result));
    }
    catch (KeyNotFoundException)
    {
        return Results.NotFound(ApiResponse<string>.Error(404, "Suggestion not found."));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
    catch (InvalidOperationException ex)
    {
        return Results.Conflict(ApiResponse<string>.Error(409, ex.Message));
    }
});
```

Keep `/classification/suggestions/{id}/accept` only as a compatibility endpoint. It must not be used by the PC records page after Task 8.

- [ ] **Step 7: Add apply flow test**

Append to `ActivityClassificationRecomputeServiceTests`:

```csharp
[Fact]
public async Task ApplySuggestionAsync_SavesRuleRecomputesAndMarksSuggestionAccepted()
{
    await using var db = CreateDb();
    db.Set<PcCategoryEntity>().Add(new PcCategoryEntity { Id = Guid.NewGuid(), Name = "Programming", Color = "#2563eb" });
    db.Set<ActivityClassificationSuggestionEntity>().Add(new ActivityClassificationSuggestionEntity
    {
        Id = Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        ClusterKey = "app:code",
        Status = "pending",
        SampleCount = 1,
        TotalDurationSeconds = 600
    });
    db.Set<AwEventEntity>().Add(WindowEvent("2026-05-25T08:00:00Z", 600, "Code.exe", "Program.cs"));
    await db.SaveChangesAsync();
    var service = CreateService(db);
    var drafts = new ClassificationRuleDraftService(db);

    var result = await service.ApplySuggestionAsync(
        Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        new SuggestionClassificationApplyRequest(
            "Programming",
            null,
            new ActivityClassificationApplyRangeRequest("range", "2026-05-25", "2026-05-25")),
        drafts,
        CancellationToken.None);

    Assert.Equal("accepted", result.SuggestionStatus);
    Assert.Equal("Programming", result.Rule.CategoryName);
    Assert.Equal(1, await db.Set<ActivityClassificationEntity>().CountAsync());
}
```

Update `CreateService` in this test file to pass `new ActivityClassificationRuleService(db)` if Task 3 changed the constructor.

- [ ] **Step 8: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ClassificationRuleDraftServiceTests|ActivitySuggestionServiceTests|ActivityClassificationRecomputeServiceTests"
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs
git commit -m "feat: close pc classification suggestion loop"
```

---

### Task 5: Real Recompute Endpoint And PC Classification Audit Rows

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`

- [ ] **Step 1: Add recompute DTOs**

Append to `ActivityClassificationDtos.cs`:

```csharp
public record ActivityClassificationRecomputeRequest(
    ActivityClassificationApplyRangeRequest Range);

public record ActivityClassificationRecomputeDto(
    int RecomputedRecordCount,
    double RecomputedDurationSeconds,
    Guid AuditId,
    string Summary);
```

- [ ] **Step 2: Add failing recompute test**

Append to `ActivityClassificationRecomputeServiceTests`:

```csharp
[Fact]
public async Task RecomputeAsync_RecomputesExistingRangeWithoutCreatingRule()
{
    await using var db = CreateDb();
    db.Set<ActivityCategoryRuleEntity>().Add(CodeRule("Programming", 1000));
    db.Set<AwEventEntity>().Add(WindowEvent("2026-05-25T08:00:00Z", 600, "Code.exe", "Program.cs"));
    await db.SaveChangesAsync();
    var service = CreateService(db);

    var result = await service.RecomputeAsync(
        new ActivityClassificationApplyRangeRequest("range", "2026-05-25", "2026-05-25"),
        CancellationToken.None);

    Assert.Equal(1, result.RecomputedRecordCount);
    Assert.Equal(600, result.RecomputedDurationSeconds);
    Assert.NotEqual(Guid.Empty, result.AuditId);
    Assert.Equal(1, await db.Set<ActivityClassificationEntity>().CountAsync());
    Assert.Equal(1, await db.Set<ActivityClassificationAuditEntity>().CountAsync());
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter RecomputeAsync_RecomputesExistingRangeWithoutCreatingRule
```

Expected: FAIL because `RecomputeAsync` does not exist.

- [ ] **Step 3: Write PC audit row during apply**

In `ActivityClassificationRecomputeService.ApplyRuleCoreAsync`, after `records` are loaded for snapshot recompute and before commit, add:

```csharp
var pcAudit = new ActivityClassificationAuditEntity
{
    Id = Guid.NewGuid(),
    Operation = "rule.apply",
    RuleId = rule.Id,
    SuggestionId = null,
    RangeMode = range.Mode,
    DateFrom = range.DateFrom,
    DateTo = range.DateTo,
    AffectedRecordCount = preview.AffectedRecordCount,
    AffectedDurationSeconds = preview.AffectedDurationSeconds,
    AffectedRecordKeysJson = System.Text.Json.JsonSerializer.Serialize(records
        .Select(ActivityClassificationRecordKey.FromRecord)
        .Distinct(StringComparer.Ordinal)
        .ToList()),
    CreatedByUserId = _currentUser.UserId,
    CreatedAt = DateTimeOffset.UtcNow
};
_db.Set<ActivityClassificationAuditEntity>().Add(pcAudit);
```

- [ ] **Step 4: Implement recompute service method**

Add to `ActivityClassificationRecomputeService`:

```csharp
public async Task<ActivityClassificationRecomputeDto> RecomputeAsync(
    ActivityClassificationApplyRangeRequest range,
    CancellationToken ct)
{
    var rules = await _rules.LoadActiveAsync(ct);
    var records = await LoadActivityRecordsAsync(range, rules, ct);
    var duration = records.Sum(record => record.DurationSeconds ?? 0);
    var audit = new ActivityClassificationAuditEntity
    {
        Id = Guid.NewGuid(),
        Operation = "range.recompute",
        RuleId = null,
        SuggestionId = null,
        RangeMode = range.Mode,
        DateFrom = range.DateFrom,
        DateTo = range.DateTo,
        AffectedRecordCount = records.Count,
        AffectedDurationSeconds = duration,
        AffectedRecordKeysJson = System.Text.Json.JsonSerializer.Serialize(records
            .Select(ActivityClassificationRecordKey.FromRecord)
            .Distinct(StringComparer.Ordinal)
            .ToList()),
        CreatedByUserId = _currentUser.UserId,
        CreatedAt = DateTimeOffset.UtcNow
    };
    _db.Set<ActivityClassificationAuditEntity>().Add(audit);
    await _db.SaveChangesAsync(ct);

    await _snapshots.EnsureClassificationsAsync(records, rules, audit.Id, ct);

    return new ActivityClassificationRecomputeDto(
        records.Count,
        duration,
        audit.Id,
        $"Recomputed {records.Count} records for {range.Mode}.");
}
```

- [ ] **Step 5: Map real recompute endpoint**

Replace the current `/classification/recompute` endpoint in `PcTrackerModule.cs`.

```csharp
writeGroup.MapPost("/classification/recompute", async (
    [FromBody] ActivityClassificationRecomputeRequest req,
    [FromServices] ActivityClassificationRecomputeService svc,
    CancellationToken ct) =>
{
    try
    {
        var result = await svc.RecomputeAsync(req.Range, ct);
        return Results.Ok(ApiResponse<ActivityClassificationRecomputeDto>.Ok(result));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
});
```

- [ ] **Step 6: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassificationRecomputeServiceTests"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs
git commit -m "feat: recompute pc classifications with audit rows"
```

---

### Task 6: Activity Analysis API

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs`

- [ ] **Step 1: Add activity analysis DTOs**

Append to `ActivityClassificationDtos.cs`:

```csharp
public record PcActivityAnalysisResponse(
    string Date,
    int BlockMinutes,
    IReadOnlyList<PcActivityAnalysisBlockDto> Blocks);

public record PcActivityAnalysisBlockDto(
    string Start,
    string End,
    int IntensityScore,
    double ActiveDurationSeconds,
    int PendingClassificationCount,
    int ContextSwitchCount,
    int CategoryChangeCount,
    IReadOnlyList<PcActivityAnalysisCategoryDto> Categories,
    IReadOnlyList<PcActivityAnalysisAppDto> Apps);

public record PcActivityAnalysisCategoryDto(
    string CategoryName,
    string Color,
    double DurationSeconds);

public record PcActivityAnalysisAppDto(
    string AppName,
    double DurationSeconds);
```

- [ ] **Step 2: Write failing analysis test**

Create `tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class PcActivityAnalysisServiceTests
{
    [Fact]
    public async Task GetDailyAnalysisAsync_GroupsRecordsAndFlagsPendingClassification()
    {
        await using var db = CreateDb();
        db.Set<AwEventEntity>().AddRange(
            WindowEvent("2026-07-05T01:00:00Z", 600, "Code.exe", "Program.cs"),
            WindowEvent("2026-07-05T01:20:00Z", 300, "Mystery.exe", "Unknown"));
        await db.SaveChangesAsync();
        var tracker = new PcTrackerService(
            db,
            new ActivityClassificationSnapshotService(db, NullLogger<ActivityClassificationSnapshotService>.Instance),
            new ActivityClassificationSettingsService(db),
            new ActivityTimelineSmoothingService());
        var service = new PcActivityAnalysisService(tracker);

        var result = await service.GetDailyAnalysisAsync(new DateTime(2026, 7, 5), 60, CancellationToken.None);

        var block = Assert.Single(result.Blocks.Where(item => item.ActiveDurationSeconds > 0));
        Assert.Equal(900, block.ActiveDurationSeconds);
        Assert.True(block.IntensityScore > 0);
        Assert.True(block.Apps.Count >= 1);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }

    private static AwEventEntity WindowEvent(string timestamp, double duration, string appName, string title) =>
        new()
        {
            Id = Random.Shared.NextInt64(1, long.MaxValue),
            DeviceId = "device-1",
            Timestamp = DateTimeOffset.Parse(timestamp),
            Duration = duration,
            EventType = "window",
            AppName = appName,
            AppNameNormalized = AppNameNormalizer.Normalize(appName),
            WindowTitle = title,
            DataJson = "{}"
        };
}
```

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PcActivityAnalysisServiceTests
```

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement analysis service**

Create `src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs`:

```csharp
using System.Globalization;
using Pim.Module.PcTracker.DTOs;

namespace Pim.Module.PcTracker.Services;

public sealed class PcActivityAnalysisService
{
    private readonly PcTrackerService _tracker;

    public PcActivityAnalysisService(PcTrackerService tracker)
    {
        _tracker = tracker;
    }

    public async Task<PcActivityAnalysisResponse> GetDailyAnalysisAsync(
        DateTime date,
        int blockMinutes,
        CancellationToken ct)
    {
        if (blockMinutes is < 15 or > 240)
            throw new ArgumentException("blockMinutes must be between 15 and 240.");

        var dateText = date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        var detail = await _tracker.QueryCompleteDetailAsync(
            new DetailQueryParams(
                dateText,
                dateText,
                null,
                null,
                null,
                null,
                null,
                null,
                "date",
                "asc",
                1,
                2000,
                View: "interpreted"),
            ct);

        var dayStart = PcTrackerService.GetBusinessDayStartForQuery(date);
        var blockCount = (int)Math.Ceiling(TimeSpan.FromDays(1).TotalMinutes / blockMinutes);
        var blocks = new List<PcActivityAnalysisBlockDto>();

        for (var i = 0; i < blockCount; i++)
        {
            var start = dayStart.AddMinutes(i * blockMinutes);
            var end = start.AddMinutes(blockMinutes);
            var records = detail.Items
                .Where(record => record.DurationSeconds is > 0)
                .Where(record => DateTimeOffset.TryParse(record.Start, out var recordStart)
                    && recordStart >= start
                    && recordStart < end)
                .OrderBy(record => record.Start, StringComparer.Ordinal)
                .ToList();
            var activeSeconds = records.Sum(record => record.DurationSeconds ?? 0);
            var categories = records
                .GroupBy(record => record.CategoryName ?? "Other", StringComparer.OrdinalIgnoreCase)
                .Select(group => new PcActivityAnalysisCategoryDto(
                    group.Key,
                    group.Select(record => record.CategoryColor).FirstOrDefault(value => !string.IsNullOrWhiteSpace(value)) ?? "#64748b",
                    group.Sum(record => record.DurationSeconds ?? 0)))
                .OrderByDescending(item => item.DurationSeconds)
                .ToList();
            var apps = records
                .GroupBy(record => record.RecordType == "web-page"
                    ? record.Domain ?? record.BrowserAppName ?? "web"
                    : record.AppName ?? record.DisplayName ?? "unknown",
                    StringComparer.OrdinalIgnoreCase)
                .Select(group => new PcActivityAnalysisAppDto(
                    group.Key,
                    group.Sum(record => record.DurationSeconds ?? 0)))
                .OrderByDescending(item => item.DurationSeconds)
                .Take(5)
                .ToList();

            blocks.Add(new PcActivityAnalysisBlockDto(
                start.ToString("O"),
                end.ToString("O"),
                ToIntensity(activeSeconds, blockMinutes),
                activeSeconds,
                records.Count(record => string.Equals(record.ClassificationSource, "fallback", StringComparison.OrdinalIgnoreCase)
                    || record.ClassificationConfidence is < 0.5),
                CountSwitches(records.Select(record => record.AppName ?? record.Domain ?? record.DisplayName ?? string.Empty)),
                CountSwitches(records.Select(record => record.CategoryName ?? string.Empty)),
                categories,
                apps));
        }

        return new PcActivityAnalysisResponse(dateText, blockMinutes, blocks);
    }

    private static int ToIntensity(double activeSeconds, int blockMinutes)
    {
        var ratio = activeSeconds / (blockMinutes * 60.0);
        if (ratio <= 0) return 0;
        if (ratio <= 0.2) return 1;
        if (ratio <= 0.45) return 2;
        if (ratio <= 0.7) return 3;
        return 4;
    }

    private static int CountSwitches(IEnumerable<string> values)
    {
        string? previous = null;
        var count = 0;
        foreach (var value in values)
        {
            if (string.IsNullOrWhiteSpace(value))
                continue;
            if (previous is not null && !string.Equals(previous, value, StringComparison.OrdinalIgnoreCase))
                count++;
            previous = value;
        }

        return count;
    }
}
```

- [ ] **Step 4: Register and map endpoint**

In `PcTrackerModule.RegisterServices`:

```csharp
services.AddScoped<PcActivityAnalysisService>();
```

In `MapEndpoints`:

```csharp
readGroup.MapGet("/activity-analysis", async (
    [FromQuery] string? date,
    [FromQuery] int? blockMinutes,
    [FromServices] PcActivityAnalysisService svc,
    CancellationToken ct) =>
{
    var d = date is not null ? DateTime.Parse(date, CultureInfo.InvariantCulture) : DateTime.Today;
    var result = await svc.GetDailyAnalysisAsync(d, blockMinutes ?? 60, ct);
    return Results.Ok(ApiResponse<PcActivityAnalysisResponse>.Ok(result));
});
```

- [ ] **Step 5: Run focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PcActivityAnalysisServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs
git commit -m "feat: add pc activity analysis api"
```

---

### Task 7: Frontend Route 3 API And Types

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/pcTracker.ts`
- Modify: `tests/client-web/pcClassificationApiPath.test.ts`
- Modify: `tests/client-web/pcClassificationTypes.test.ts`
- Create: `tests/client-web/pcRoute3ApiPath.test.ts`
- Create: `tests/client-web/pcRoute3Types.test.ts`

- [ ] **Step 1: Write failing API path tests**

Create `tests/client-web/pcRoute3ApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { test } from 'vitest';
import { pcClassificationApiPaths, pcActivityAnalysisApiPath } from '../../src/client-web/src/api/pcTracker';

test('route 3 pc classification paths point at preview apply flow', () => {
  assert.equal(
    pcClassificationApiPaths.suggestionPreview('abc'),
    '/pc/classification/suggestions/abc/preview'
  );
  assert.equal(
    pcClassificationApiPaths.suggestionApply('abc'),
    '/pc/classification/suggestions/abc/apply'
  );
  assert.equal(pcClassificationApiPaths.recompute, '/pc/classification/recompute');
  assert.equal(pcActivityAnalysisApiPath('2026-07-05', 60), '/pc/activity-analysis?date=2026-07-05&blockMinutes=60');
});
```

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3ApiPath.test.ts
```

Expected: FAIL because paths do not exist.

- [ ] **Step 2: Update TypeScript types**

Modify `ActivityClassificationApplyRange` in `src/client-web/src/types/index.ts`:

```ts
export interface ActivityClassificationApplyRange {
  mode: 'today' | 'range';
  dateFrom?: string | null;
  dateTo?: string | null;
}
```

Append route 3 types:

```ts
export interface SuggestionClassificationPreviewRequest {
  categoryName: string | null;
  projectTag: string | null;
  range: ActivityClassificationApplyRange;
}

export interface ActivityClassificationSuggestionPreview {
  rule: SaveActivityClassificationRuleRequest;
  preview: ActivityClassificationPreview;
}

export interface SuggestionClassificationApplyRequest {
  categoryName: string | null;
  projectTag: string | null;
  range: ActivityClassificationApplyRange;
}

export interface ActivityClassificationSuggestionApply {
  rule: ActivityClassificationRule;
  preview: ActivityClassificationPreview;
  auditId: string;
  suggestionStatus: string;
}

export interface PcActivityAnalysisResponse {
  date: string;
  blockMinutes: number;
  blocks: PcActivityAnalysisBlock[];
}

export interface PcActivityAnalysisBlock {
  start: string;
  end: string;
  intensityScore: number;
  activeDurationSeconds: number;
  pendingClassificationCount: number;
  contextSwitchCount: number;
  categoryChangeCount: number;
  categories: PcActivityAnalysisCategory[];
  apps: PcActivityAnalysisApp[];
}

export interface PcActivityAnalysisCategory {
  categoryName: string;
  color: string;
  durationSeconds: number;
}

export interface PcActivityAnalysisApp {
  appName: string;
  durationSeconds: number;
}
```

Add optional key fields to `PcDetailRecord`:

```ts
recordKey?: string | null;
recordKeyVersion?: string | null;
recordKeyStability?: string | null;
sourceBucketIds?: string[] | null;
sourceType?: string | null;
interpretationVersion?: string | null;
```

- [ ] **Step 3: Add API functions**

Modify imports in `src/client-web/src/api/pcTracker.ts` to include new types:

```ts
ActivityClassificationSuggestionPreview,
ActivityClassificationSuggestionApply,
SuggestionClassificationPreviewRequest,
SuggestionClassificationApplyRequest,
PcActivityAnalysisResponse,
```

Extend `pcClassificationApiPaths`:

```ts
suggestionPreview: (id: string) => `/pc/classification/suggestions/${id}/preview`,
suggestionApply: (id: string) => `/pc/classification/suggestions/${id}/apply`,
recompute: '/pc/classification/recompute',
```

Add:

```ts
export function pcActivityAnalysisApiPath(date: string, blockMinutes = 60) {
  return `/pc/activity-analysis?date=${date}&blockMinutes=${blockMinutes}`;
}

export function previewActivityClassificationSuggestion(
  id: string,
  request: SuggestionClassificationPreviewRequest
) {
  return apiPost<ApiResponse<ActivityClassificationSuggestionPreview>>(
    pcClassificationApiPaths.suggestionPreview(id),
    request
  ).then(r => r.data);
}

export function applyActivityClassificationSuggestion(
  id: string,
  request: SuggestionClassificationApplyRequest
) {
  return apiPost<ApiResponse<ActivityClassificationSuggestionApply>>(
    pcClassificationApiPaths.suggestionApply(id),
    request
  ).then(r => r.data);
}

export function getPcActivityAnalysis(date: string, blockMinutes = 60) {
  return apiGet<ApiResponse<PcActivityAnalysisResponse>>(
    pcActivityAnalysisApiPath(date, blockMinutes)
  ).then(r => r.data);
}
```

Keep `acceptActivityClassificationSuggestion` exported only for compatibility. After Task 9, verify with `rg "acceptActivityClassificationSuggestion" src/client-web/src/pages src/client-web/src/components` that the PC records page no longer imports or calls it.

- [ ] **Step 4: Update client tests**

Modify `tests/client-web/pcClassificationApiPath.test.ts` expected paths to include new route 3 paths:

```ts
pcClassificationApiPaths.suggestionPreview('suggestion-1'),
pcClassificationApiPaths.suggestionApply('suggestion-1'),
pcClassificationApiPaths.recompute,
```

Expected values:

```ts
'/pc/classification/suggestions/suggestion-1/preview',
'/pc/classification/suggestions/suggestion-1/apply',
'/pc/classification/recompute',
```

Modify `tests/client-web/pcClassificationTypes.test.ts` to assert `mode: 'all'` is not used:

```ts
const range: ActivityClassificationApplyRange = {
  mode: 'today',
  dateFrom: '2026-07-05',
  dateTo: '2026-07-05',
};

assert.equal(range.mode, 'today');
```

Create `tests/client-web/pcRoute3Types.test.ts`:

```ts
import assert from 'node:assert/strict';
import { test } from 'vitest';
import type {
  ActivityClassificationSuggestionPreview,
  PcActivityAnalysisResponse,
} from '../../src/client-web/src/types';

test('route 3 preview and activity analysis types use camelCase fields', () => {
  const preview: ActivityClassificationSuggestionPreview = {
    rule: {
      ruleName: 'Docs',
      scope: 'activity',
      categoryName: 'Learning',
      projectTag: null,
      color: '#64748b',
      priority: 900,
      conditionsJson: '{"all":[]}',
      confidence: 0.95,
      explanation: null,
    },
    preview: {
      affectedRecordCount: 1,
      affectedDurationSeconds: 60,
      currentCategoryCounts: { Other: 1 },
      newCategoryCounts: { Learning: 1 },
      samples: [],
      requiresConfirmation: true,
      summary: 'Affected 1 record.',
    },
  };

  const analysis: PcActivityAnalysisResponse = {
    date: '2026-07-05',
    blockMinutes: 60,
    blocks: [{
      start: '2026-07-05T00:00:00Z',
      end: '2026-07-05T01:00:00Z',
      intensityScore: 3,
      activeDurationSeconds: 1200,
      pendingClassificationCount: 1,
      contextSwitchCount: 2,
      categoryChangeCount: 1,
      categories: [{ categoryName: 'Learning', color: '#64748b', durationSeconds: 1200 }],
      apps: [{ appName: 'Edge', durationSeconds: 1200 }],
    }],
  };

  assert.equal(preview.rule.scope, 'activity');
  assert.equal(analysis.blocks[0].pendingClassificationCount, 1);
});
```

- [ ] **Step 5: Run client type/path tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationTypes.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3ApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Types.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/pcTracker.ts tests/client-web/pcClassificationApiPath.test.ts tests/client-web/pcClassificationTypes.test.ts tests/client-web/pcRoute3ApiPath.test.ts tests/client-web/pcRoute3Types.test.ts
git commit -m "feat: add pc route 3 client contracts"
```

---

### Task 8: Classification Action Queue And Preview Dialog

**Files:**
- Create: `src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx`
- Create: `src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx`
- Create: `src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Test: `tests/client-web/pcRoute3Components.test.tsx`

- [ ] **Step 1: Write failing component smoke test**

Create `tests/client-web/pcRoute3Components.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { test } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import ClassificationActionQueue from '../../src/client-web/src/components/pc-tracker/ClassificationActionQueue';
import RuleImpactPreviewPanel from '../../src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel';

test('classification action queue exposes preview-first actions without accept button', () => {
  const html = renderToStaticMarkup(
    <ClassificationActionQueue
      suggestions={[{
        id: 's1',
        clusterKey: 'web:docs.example.com',
        sampleCount: 2,
        totalDurationSeconds: 600,
        sampleRecordsJson: '[]',
        sanitizedContextJson: '{}',
        currentCategory: 'Other',
        suggestedCategory: 'Learning',
        suggestedProjectTag: null,
        suggestedRulesJson: null,
        userFeedback: null,
        llmResponseJson: null,
        status: 'pending',
      }]}
      isLoading={false}
      onPreview={() => undefined}
      onReject={() => undefined}
    />
  );

  assert.equal(html.includes('Process and preview'), true);
  assert.equal(html.includes('Accept'), false);
});

test('rule impact panel shows affected record count', () => {
  const html = renderToStaticMarkup(
    <RuleImpactPreviewPanel
      preview={{
        affectedRecordCount: 3,
        affectedDurationSeconds: 900,
        currentCategoryCounts: { Other: 3 },
        newCategoryCounts: { Learning: 3 },
        samples: [],
        requiresConfirmation: true,
        summary: 'Affected 3 records.',
      }}
    />
  );

  assert.equal(html.includes('3'), true);
  assert.equal(html.includes('15'), true);
});
```

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: FAIL because components do not exist.

- [ ] **Step 2: Create rule impact panel**

Create `RuleImpactPreviewPanel.tsx`:

```tsx
import type { ActivityClassificationPreview } from '../../types';

interface Props {
  preview: ActivityClassificationPreview;
}

function formatMinutes(seconds: number) {
  return Math.round(seconds / 60).toLocaleString('zh-CN');
}

function formatCounts(counts: Record<string, number>) {
  const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return 'None';
  return entries.map(([name, count]) => `${name || 'Unknown'} ${count}`).join(', ');
}

export default function RuleImpactPreviewPanel({ preview }: Props) {
  return (
    <section className="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-950">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold">Rule impact preview</h3>
        {preview.requiresConfirmation && (
          <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-800">
            Confirmation required
          </span>
        )}
      </div>
      <p className="mt-2">
        Affects {preview.affectedRecordCount.toLocaleString('zh-CN')} records, about {formatMinutes(preview.affectedDurationSeconds)} minutes.
      </p>
      {preview.summary && <p className="mt-1 text-xs text-blue-800">{preview.summary}</p>}
      <div className="mt-2 grid gap-1 text-xs text-blue-900">
        <p>Before: {formatCounts(preview.currentCategoryCounts)}</p>
        <p>After: {formatCounts(preview.newCategoryCounts)}</p>
      </div>
    </section>
  );
}
```

- [ ] **Step 3: Create action queue**

Create `ClassificationActionQueue.tsx`:

```tsx
import type { ActivityClassificationSuggestion } from '../../types';

interface Props {
  suggestions: ActivityClassificationSuggestion[];
  isLoading: boolean;
  onPreview: (suggestion: ActivityClassificationSuggestion) => void;
  onReject: (suggestion: ActivityClassificationSuggestion) => void;
}

function formatMinutes(seconds: number) {
  return Math.round(seconds / 60).toLocaleString('zh-CN');
}

export default function ClassificationActionQueue({
  suggestions,
  isLoading,
  onPreview,
  onReject,
}: Props) {
  if (isLoading) {
    return <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">Loading classification work</div>;
  }

  const visible = suggestions.slice(0, 8);
  if (visible.length === 0) {
    return <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">No pending classification work.</div>;
  }

  return (
    <div className="space-y-2">
      {visible.map(suggestion => (
        <article key={suggestion.id} className="rounded-lg border border-amber-200 bg-amber-50/60 p-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="truncate text-sm font-semibold text-slate-950">{suggestion.appDisplayName || suggestion.clusterKey}</h3>
              <p className="mt-1 text-xs text-slate-600">
                {suggestion.sampleCount.toLocaleString('zh-CN')} samples · {formatMinutes(suggestion.totalDurationSeconds)} minutes
                {suggestion.currentCategory ? ` · current ${suggestion.currentCategory}` : ''}
              </p>
              {suggestion.suggestedCategory && (
                <p className="mt-1 text-xs font-medium text-blue-700">Suggested: {suggestion.suggestedCategory}</p>
              )}
            </div>
            <div className="flex shrink-0 gap-2">
              <button type="button" onClick={() => onPreview(suggestion)} className="pim-button-primary h-8 px-3 text-xs font-medium">
                Process and preview
              </button>
              <button type="button" onClick={() => onReject(suggestion)} className="pim-button-secondary h-8 px-3 text-xs font-medium">
                Ignore
              </button>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
```

- [ ] **Step 4: Create preview dialog**

Create `ClassificationPreviewDialog.tsx`:

```tsx
import { useEffect, useId, useState } from 'react';
import type {
  ActivityClassificationSuggestion,
  ActivityClassificationSuggestionPreview,
  ActivityClassificationApplyRange,
} from '../../types';
import RuleImpactPreviewPanel from './RuleImpactPreviewPanel';

interface Props {
  suggestion: ActivityClassificationSuggestion | null;
  date: string;
  preview: ActivityClassificationSuggestionPreview | null;
  isPreviewing: boolean;
  isApplying: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onPreview: (request: { categoryName: string | null; projectTag: string | null; range: ActivityClassificationApplyRange }) => void;
  onApply: (request: { categoryName: string | null; projectTag: string | null; range: ActivityClassificationApplyRange }) => void;
}

export default function ClassificationPreviewDialog({
  suggestion,
  date,
  preview,
  isPreviewing,
  isApplying,
  errorMessage,
  onClose,
  onPreview,
  onApply,
}: Props) {
  const titleId = useId();
  const [categoryName, setCategoryName] = useState('');
  const [projectTag, setProjectTag] = useState('');
  const [mode, setMode] = useState<ActivityClassificationApplyRange['mode']>('today');
  const [dateFrom, setDateFrom] = useState(date);
  const [dateTo, setDateTo] = useState(date);

  useEffect(() => {
    if (!suggestion) return;
    setCategoryName(suggestion.suggestedCategory || suggestion.currentCategory || '');
    setProjectTag(suggestion.suggestedProjectTag || '');
    setMode('today');
    setDateFrom(date);
    setDateTo(date);
  }, [date, suggestion]);

  if (!suggestion) return null;

  const range = { mode, dateFrom, dateTo };
  const request = {
    categoryName: categoryName.trim() || null,
    projectTag: projectTag.trim() || null,
    range,
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-3 py-6">
      <div className="absolute inset-0 bg-slate-950/25" onClick={onClose} />
      <section role="dialog" aria-modal="true" aria-labelledby={titleId} className="relative flex max-h-full w-full max-w-[680px] flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-2xl">
        <header className="border-b border-slate-200 px-5 py-4">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h2 id={titleId} className="text-base font-semibold text-slate-950">Classification preview</h2>
              <p className="mt-1 truncate text-sm text-slate-500">{suggestion.clusterKey}</p>
            </div>
            <button type="button" onClick={onClose} className="pim-button-secondary h-9 shrink-0 px-3 text-sm">Close</button>
          </div>
        </header>
        <div className="min-h-0 flex-1 space-y-4 overflow-auto px-5 py-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="text-sm">
              <span className="mb-1 block text-xs font-medium text-slate-500">Category</span>
              <input value={categoryName} onChange={event => setCategoryName(event.target.value)} className="h-10 w-full rounded-lg border border-slate-200 px-3 text-sm" />
            </label>
            <label className="text-sm">
              <span className="mb-1 block text-xs font-medium text-slate-500">Project tag</span>
              <input value={projectTag} onChange={event => setProjectTag(event.target.value)} className="h-10 w-full rounded-lg border border-slate-200 px-3 text-sm" />
            </label>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {(['today', 'range'] as const).map(value => (
              <button key={value} type="button" onClick={() => setMode(value)} className={value === mode ? 'pim-button-primary h-9 text-sm' : 'pim-button-secondary h-9 text-sm'}>
                {value === 'today' ? 'Today' : 'Range'}
              </button>
            ))}
          </div>
          {mode === 'range' && (
            <div className="grid gap-3 sm:grid-cols-2">
              <input type="date" value={dateFrom} onChange={event => setDateFrom(event.target.value)} className="h-10 rounded-lg border border-slate-200 px-3 text-sm" />
              <input type="date" value={dateTo} onChange={event => setDateTo(event.target.value)} className="h-10 rounded-lg border border-slate-200 px-3 text-sm" />
            </div>
          )}
          {errorMessage && <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{errorMessage}</div>}
          {preview && <RuleImpactPreviewPanel preview={preview.preview} />}
        </div>
        <footer className="flex justify-end gap-2 border-t border-slate-200 px-5 py-4">
          <button type="button" onClick={() => onPreview(request)} disabled={isPreviewing || isApplying} className="pim-button-secondary h-10 px-4 text-sm">
            {isPreviewing ? 'Previewing' : 'Preview'}
          </button>
          <button type="button" onClick={() => onApply(request)} disabled={!preview || isPreviewing || isApplying} className="pim-button-primary h-10 px-4 text-sm">
            {isApplying ? 'Applying' : 'Apply'}
          </button>
        </footer>
      </section>
    </div>
  );
}
```

- [ ] **Step 5: Run component test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx tests/client-web/pcRoute3Components.test.tsx
git commit -m "feat: add pc classification action queue"
```

---

### Task 9: Activity Analysis Heatmap And Full PC Page Integration

**Files:**
- Create: `src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Test: `tests/client-web/pcRoute3Components.test.tsx`

- [ ] **Step 1: Extend component smoke test for activity analysis**

Append to `tests/client-web/pcRoute3Components.test.tsx`:

```tsx
import ActivityAnalysisHeatmap from '../../src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap';

test('activity analysis heatmap renders separately from keyboard mouse heatmap', () => {
  const html = renderToStaticMarkup(
    <ActivityAnalysisHeatmap
      analysis={{
        date: '2026-07-05',
        blockMinutes: 60,
        blocks: [{
          start: '2026-07-05T00:00:00Z',
          end: '2026-07-05T01:00:00Z',
          intensityScore: 3,
          activeDurationSeconds: 1800,
          pendingClassificationCount: 1,
          contextSwitchCount: 2,
          categoryChangeCount: 1,
          categories: [{ categoryName: 'Programming', color: '#2563eb', durationSeconds: 1800 }],
          apps: [{ appName: 'Code.exe', durationSeconds: 1800 }],
        }],
      }}
      selectedStart={null}
      onSelectBlock={() => undefined}
    />
  );

  assert.equal(html.includes('Activity analysis'), true);
  assert.equal(html.includes('Keyboard'), false);
});
```

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: FAIL because component does not exist.

- [ ] **Step 2: Create activity analysis heatmap component**

Create `ActivityAnalysisHeatmap.tsx`:

```tsx
import type { PcActivityAnalysisBlock, PcActivityAnalysisResponse } from '../../types';

interface Props {
  analysis: PcActivityAnalysisResponse | undefined;
  selectedStart: string | null;
  onSelectBlock: (block: PcActivityAnalysisBlock) => void;
}

function colorForIntensity(score: number) {
  if (score <= 0) return '#f1f5f9';
  if (score === 1) return '#d9f2ec';
  if (score === 2) return '#9fdacf';
  if (score === 3) return '#43afa3';
  return '#0f8f88';
}

function formatMinutes(seconds: number) {
  return Math.round(seconds / 60).toLocaleString('zh-CN');
}

export default function ActivityAnalysisHeatmap({ analysis, selectedStart, onSelectBlock }: Props) {
  const blocks = analysis?.blocks ?? [];
  const selected = blocks.find(block => block.start === selectedStart) ?? blocks.find(block => block.activeDurationSeconds > 0);

  if (!analysis || blocks.length === 0) {
    return <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">No activity analysis data.</div>;
  }

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-6 gap-1 md:grid-cols-12">
        {blocks.map(block => {
          const selectedCell = block.start === selected?.start;
          return (
            <button
              key={block.start}
              type="button"
              title={`${new Date(block.start).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} · ${formatMinutes(block.activeDurationSeconds)} minutes`}
              onClick={() => onSelectBlock(block)}
              className={`h-9 rounded-md border text-[10px] font-semibold transition-transform hover:-translate-y-0.5 ${
                selectedCell ? 'border-slate-900' : block.pendingClassificationCount > 0 ? 'border-amber-500' : 'border-white'
              }`}
              style={{ backgroundColor: colorForIntensity(block.intensityScore) }}
            >
              {block.pendingClassificationCount > 0 ? block.pendingClassificationCount : ''}
            </button>
          );
        })}
      </div>
      <div className="flex flex-wrap gap-3 text-xs text-slate-500">
        <span>Activity analysis</span>
        <span>Filled cells show activity intensity</span>
        <span>Amber borders show pending classification</span>
      </div>
      {selected && (
        <section className="rounded-lg border border-slate-200 bg-slate-50 p-3">
          <div className="text-sm font-semibold text-slate-950">
            {new Date(selected.start).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - {new Date(selected.end).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
          </div>
          <p className="mt-1 text-xs text-slate-600">
            {formatMinutes(selected.activeDurationSeconds)} active minutes · {selected.contextSwitchCount} context switches · {selected.pendingClassificationCount} pending
          </p>
          <div className="mt-2 grid gap-2 md:grid-cols-2">
            <div className="text-xs text-slate-600">
              {selected.categories.slice(0, 4).map(item => (
                <div key={item.categoryName} className="flex items-center justify-between gap-2">
                  <span>{item.categoryName}</span>
                  <span>{formatMinutes(item.durationSeconds)}m</span>
                </div>
              ))}
            </div>
            <div className="text-xs text-slate-600">
              {selected.apps.slice(0, 4).map(item => (
                <div key={item.appName} className="flex items-center justify-between gap-2">
                  <span className="truncate">{item.appName}</span>
                  <span>{formatMinutes(item.durationSeconds)}m</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
```

- [ ] **Step 3: Integrate into full PC page**

Modify `PcTrackerPage.tsx`:

Remove imports:

```ts
acceptActivityClassificationSuggestion,
ClassificationSuggestionPanel,
QuickClassificationDialog,
```

Add imports:

```ts
applyActivityClassificationSuggestion,
getPcActivityAnalysis,
previewActivityClassificationSuggestion,
ActivityAnalysisHeatmap,
ClassificationActionQueue,
ClassificationPreviewDialog,
```

Add state:

```ts
const [selectedAnalysisBlockStart, setSelectedAnalysisBlockStart] = useState<string | null>(null);
const [previewError, setPreviewError] = useState<string | null>(null);
```

Add query:

```ts
const { data: activityAnalysis } = useQuery({
  queryKey: ['pc-activity-analysis', dateStr],
  queryFn: () => getPcActivityAnalysis(dateStr, 60),
  refetchInterval: 30000,
});
```

Replace existing suggestion section with:

```tsx
<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.45fr)_minmax(360px,0.8fr)]">
  <AnalysisCard title="Activity analysis" subtitle="New module: time-block activity state and classification gaps">
    <ActivityAnalysisHeatmap
      analysis={activityAnalysis}
      selectedStart={selectedAnalysisBlockStart}
      onSelectBlock={block => setSelectedAnalysisBlockStart(block.start)}
    />
  </AnalysisCard>

  <AnalysisCard title="Classification action queue" subtitle="New module: preview before applying rules">
    <ClassificationActionQueue
      suggestions={suggestions}
      isLoading={suggestionsLoading}
      onPreview={handleCorrectSuggestion}
      onReject={suggestion => rejectMutation.mutate(suggestion.id)}
    />
  </AnalysisCard>
</div>
```

Keep existing modules below this new block:

```tsx
<CategoryTimeline timeline={data?.timeline || []} />
<KeyboardHeatmap keystats={data?.keystats || null} />
```

Keep the existing `EventTimelineDialog` JSX from `PcTrackerPage.tsx` with its current `open`, `timeline`, `dateStr`, and `onClose` props. Do not modify `CategoryTimeline.tsx`, `EventTimelineDialog.tsx`, or `KeyboardHeatmap.tsx` in this task.

- [ ] **Step 4: Use suggestion preview/apply APIs**

Replace `previewMutation`:

```ts
const previewMutation = useMutation({
  mutationFn: ({
    id,
    request,
    requestId,
  }: {
    id: string;
    request: SuggestionClassificationPreviewRequest;
    requestId: number;
  }) => previewActivityClassificationSuggestion(id, request).then(result => ({ result, requestId })),
  onSuccess: ({ result, requestId }) => {
    if (requestId === previewRequestIdRef.current) {
      setPreview(result);
      setPreviewError(null);
    }
  },
  onError: error => {
    setPreviewError(error instanceof Error ? error.message : 'Preview failed');
  },
});
```

Replace `applyMutation`:

```ts
const applyMutation = useMutation({
  mutationFn: ({
    id,
    request,
  }: {
    id: string;
    request: SuggestionClassificationApplyRequest;
  }) => applyActivityClassificationSuggestion(id, request),
  onSuccess: () => {
    setActiveSuggestion(null);
    setPreview(null);
    setPreviewError(null);
    queryClient.invalidateQueries({ queryKey: ['pc-summary'] });
    queryClient.invalidateQueries({ queryKey: ['pc-activity-analysis'] });
    queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions'] });
    queryClient.invalidateQueries({ queryKey: ['pc-recent-project-tags'] });
  },
});
```

Update dialog props:

```tsx
<ClassificationPreviewDialog
  suggestion={activeSuggestion}
  date={dateStr}
  preview={preview}
  isPreviewing={previewMutation.isPending}
  isApplying={applyMutation.isPending}
  errorMessage={previewError}
  onClose={handleCloseDialog}
  onPreview={request => {
    if (!activeSuggestion) return;
    const requestId = previewRequestIdRef.current + 1;
    previewRequestIdRef.current = requestId;
    setPreview(null);
    setPreviewError(null);
    previewMutation.mutate({ id: activeSuggestion.id, request, requestId });
  }}
  onApply={request => {
    if (!activeSuggestion) return;
    applyMutation.mutate({ id: activeSuggestion.id, request });
  }}
/>
```

Type imports should include `SuggestionClassificationPreviewRequest`, `SuggestionClassificationApplyRequest`, and `ActivityClassificationSuggestionPreview`.

- [ ] **Step 5: Run frontend checks**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
npm --prefix src/client-web run build
```

Expected: PASS. The build verifies the full page still compiles with preserved modules.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx src/client-web/src/pages/PcTrackerPage.tsx tests/client-web/pcRoute3Components.test.tsx
git commit -m "feat: integrate pc route 3 full page modules"
```

---

### Task 10: Final Verification, Commit Review, Push, And GitHub Actions

**Files:**
- Read: `docs/superpowers/specs/2026-07-05-pc-records-understanding-display-design.md`
- Read: `docs/superpowers/plans/2026-07-05-pc-records-route-3.md`
- Verify: all touched source, tests, migrations, and docs

- [ ] **Step 1: Run backend verification**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS. If it fails, record the exact project, test name, and failure message, then fix before claiming completion.

- [ ] **Step 2: Run frontend verification**

Run:

```powershell
npm --prefix src/client-web run build
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcClassificationTypes.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3ApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Types.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Inspect final diff and generated files**

Run:

```powershell
git status --short --branch
git diff --stat origin/master...HEAD
git diff --check origin/master...HEAD
```

Expected:

- Only intentional source, test, migration, and plan/spec files are changed.
- No `.superpowers/brainstorm/`, `bin/`, `obj/`, `build/`, `dist/`, `publish/`, or API `wwwroot` build artifacts are staged.
- `git diff --check` has no whitespace errors.

- [ ] **Step 4: Confirm preserved modules**

Run:

```powershell
git diff --name-only origin/master...HEAD
```

Expected: `CategoryTimeline.tsx`, `EventTimelineDialog.tsx`, and `KeyboardHeatmap.tsx` are either absent from the diff or contain only import-safe changes explicitly needed for compilation. Their core rendering and interaction behavior are not replaced.

- [ ] **Step 5: Create final integration commit if needed**

If all previous tasks already committed focused changes, skip this step. If there are intentional remaining changes:

```powershell
git add <intentional files>
git commit -m "feat: complete pc records route 3"
```

- [ ] **Step 6: Push and watch GitHub Actions**

Run:

```powershell
git push -u origin feat/pc-records-route-3
gh run watch
```

Expected: GitHub Actions completes successfully.

If using direct `master` integration instead of a branch:

```powershell
git checkout master
git merge --ff-only feat/pc-records-route-3
git push origin master
gh run watch
```

- [ ] **Step 7: Final status**

Run:

```powershell
git status --short --branch
git log --oneline -5
```

Expected: working tree is clean, branch state is understood, and the latest commits show the Route 3 implementation.

Report:

- Local `dotnet test Pim.sln` result.
- Local `npm --prefix src/client-web run build` result.
- GitHub Actions run conclusion.
- Any remaining risks, especially if a verification failure is unrelated and intentionally left documented.
