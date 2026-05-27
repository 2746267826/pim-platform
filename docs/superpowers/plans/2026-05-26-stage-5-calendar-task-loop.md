# Stage 5 Calendar Task Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Stage 5 calendar and task planning loop: reliable task/event management, Outlook-compatible ICS import, soft-delete recycle bin, strict delete confirmation, audit logging, and improved planning surfaces.

**Architecture:** Extend the existing `Pim.Module.Calendar` module instead of replacing it. Calendar service remains the server-side business owner; Web calls structured APIs for planning, delete preview, recycle-bin restore, import/export, and display. Soft-deleted records stay hidden from normal flows while a Settings recycle bin exposes recovery with conflict checks.

**Tech Stack:** .NET 8, ASP.NET Core minimal APIs, EF Core/Npgsql migrations, Ical.Net, xUnit, EF Core InMemory tests, React 19, TypeScript, TanStack Query, FullCalendar, Vite.

---

## Scope Check

This plan implements one capability package: Stage 5 calendar and task loop hardening. The work spans backend, frontend, tests, and docs because the ability is not complete unless task/event data, recycle-bin behavior, Outlook import, and Web confirmation flows all work together.

This plan does not implement Outlook two-way sync, meeting response workflow, automatic scheduling, plan-vs-PC-activity matching, permanent deletion, file binding, or MCP server exposure.

## File Structure

Backend files to modify:

- `src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs`: add delete operation tracking.
- `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`: add all-day, time-zone, source, raw ICS, external metadata, recurrence exception, and delete operation fields.
- `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`: add planned end and delete operation fields.
- `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`: add indexes for deleted records, operation ids, source ids, and planned ranges.
- `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`: add Stage 5 request/response DTOs.
- `src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs`: new focused audit writer for calendar module operations.
- `src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs`: new recycle-bin query, restore preview, restore, and restore-as-copy service.
- `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`: new delete preview and grouped soft-delete service.
- `src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs`: new Outlook-compatible import/export adapter around Ical.Net.
- `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`: keep CRUD/query/planning operations; delegate delete/recycle-bin/ICS work to focused services.
- `src/modules/Pim.Module.Calendar/CalendarModule.cs`: map new endpoints and return structured results.
- `src/Pim.Infrastructure/Data/Migrations/*`: add EF migration for Stage 5 fields.

Backend test files to create or modify:

- `tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs`
- `tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs`
- `tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs`
- `tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs`
- `tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs`
- `tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs`
- Existing `tests/Pim.UnitTests/Services/IcsServiceTests.cs` remains as legacy coverage for simple import/export.

Frontend files to modify:

- `src/client-web/src/types/index.ts`: add Stage 5 DTO types.
- `src/client-web/src/api/calendar.ts`: add endpoint path helpers and API methods.
- `src/client-web/src/layout/AppLayout.tsx`: route recycle bin page.
- `src/client-web/src/layout/Sidebar.tsx`: replace browser `confirm()` for book deletes.
- `src/client-web/src/pages/SettingsPage.tsx`: add recycle bin entry.
- `src/client-web/src/pages/RecycleBinPage.tsx`: new recycle-bin page.
- `src/client-web/src/pages/TaskListPage.tsx`: add durable task filters, batch selection, and delete confirmation.
- `src/client-web/src/pages/CalendarPage.tsx`: use task planning endpoint for drag-to-calendar and improve visual distinction.
- `src/client-web/src/pages/CalendarDataManager.tsx`: use strict delete confirmation and show import reports.
- `src/client-web/src/dialogs/TaskEditorDialog.tsx`: add planned end and remove browser `confirm()`.
- `src/client-web/src/dialogs/EventEditorDialog.tsx`: add all-day, source, and meeting-context display.
- `src/client-web/src/components/today/TodayScheduleList.tsx`: make events and tasks selectable/distinct.
- `src/client-web/src/components/today/TodayTaskColumn.tsx`: refine badges and empty-state actions.
- `src/client-web/src/ui/ConfirmActionDialog.tsx`: new reusable strict confirmation dialog.
- `src/client-web/src/ui/OperationResultBanner.tsx`: new structured operation result display.

Frontend test files to create or modify:

- `tests/client-web/calendarApiPath.test.ts`
- `tests/client-web/recycleBinApiPath.test.ts`
- `tests/client-web/calendarStage5Types.test.ts`
- `tests/client-web/confirmActionDialogModel.test.ts`
- `tests/client-web/tsconfig.calendar-stage5.json`

Docs to create:

- `docs/operations/calendar-task-stage5-acceptance.md`

---

### Task 1: Add Stage 5 Calendar Model Fields

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/*`

- [ ] **Step 1: Write failing model tests**

Create `tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarStage5ModelTests
{
    [Fact]
    public async Task EventEntity_PreservesOutlookImportMetadata()
    {
        PimDbContext.RegisterModuleAssembly(typeof(EventEntity).Assembly);
        await using var db = CreateDb();
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var calendar = new CalendarEntity
        {
            UserId = userId,
            Name = "Outlook",
            Kind = "calendar",
            IsDefault = true
        };
        var evt = new EventEntity
        {
            CalendarId = calendar.Id,
            Calendar = calendar,
            Uid = "local-uid@pim",
            SourceUid = "outlook-source-uid",
            Title = "Outlook all day",
            DtStart = new DateTimeOffset(2026, 5, 26, 0, 0, 0, TimeSpan.FromHours(8)),
            DtEnd = new DateTimeOffset(2026, 5, 27, 0, 0, 0, TimeSpan.FromHours(8)),
            IsAllDay = true,
            TimeZoneId = "Asia/Shanghai",
            SourceTimeZoneId = "China Standard Time",
            Source = "outlook-ics",
            SourceIcsComponent = "BEGIN:VEVENT\r\nUID:outlook-source-uid\r\nEND:VEVENT",
            ExternalMetadataJson = "{\"organizer\":\"mailto:owner@example.com\"}",
            RecurrenceId = "20260526T090000",
            ExDatesJson = "[\"2026-05-27\"]",
            RecurrenceMetadataJson = "{\"exceptionCount\":1}"
        };

        db.Set<CalendarEntity>().Add(calendar);
        db.Set<EventEntity>().Add(evt);
        await db.SaveChangesAsync();

        var saved = await db.Set<EventEntity>().SingleAsync();
        Assert.True(saved.IsAllDay);
        Assert.Equal("Asia/Shanghai", saved.TimeZoneId);
        Assert.Equal("China Standard Time", saved.SourceTimeZoneId);
        Assert.Equal("outlook-ics", saved.Source);
        Assert.Equal("outlook-source-uid", saved.SourceUid);
        Assert.Contains("BEGIN:VEVENT", saved.SourceIcsComponent);
        Assert.Contains("organizer", saved.ExternalMetadataJson);
        Assert.Equal("20260526T090000", saved.RecurrenceId);
        Assert.Contains("2026-05-27", saved.ExDatesJson);
        Assert.Contains("exceptionCount", saved.RecurrenceMetadataJson);
    }

    [Fact]
    public async Task CalendarTaskAndEvent_SupportDeleteOperationTracking()
    {
        PimDbContext.RegisterModuleAssembly(typeof(EventEntity).Assembly);
        await using var db = CreateDb();
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var operationId = Guid.Parse("11111111-1111-1111-1111-111111111111");
        var calendar = new CalendarEntity
        {
            UserId = userId,
            Name = "Work",
            Kind = "calendar",
            DeletedAt = DateTimeOffset.UtcNow,
            DeletedByOperationId = operationId,
            DeletedByOperationKind = "calendar-book"
        };
        var task = new TaskEntity
        {
            UserId = userId,
            Uid = "task@pim",
            Title = "Planned work",
            PlannedEnd = new DateTimeOffset(2026, 5, 26, 11, 0, 0, TimeSpan.Zero),
            DeletedAt = DateTimeOffset.UtcNow,
            DeletedByOperationId = operationId,
            DeletedByOperationKind = "task-book"
        };
        var evt = new EventEntity
        {
            CalendarId = calendar.Id,
            Calendar = calendar,
            Uid = "event@pim",
            Title = "Deleted event",
            DtStart = new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero),
            DtEnd = new DateTimeOffset(2026, 5, 26, 10, 0, 0, TimeSpan.Zero),
            DeletedAt = DateTimeOffset.UtcNow,
            DeletedByOperationId = operationId,
            DeletedByOperationKind = "calendar-book"
        };

        db.Set<CalendarEntity>().Add(calendar);
        db.Set<TaskEntity>().Add(task);
        db.Set<EventEntity>().Add(evt);
        await db.SaveChangesAsync();

        Assert.Empty(await db.Set<CalendarEntity>().ToListAsync());
        Assert.Empty(await db.Set<EventEntity>().ToListAsync());
        Assert.Empty(await db.Set<TaskEntity>().ToListAsync());

        var deletedCalendar = await db.Set<CalendarEntity>().IgnoreQueryFilters().SingleAsync();
        var deletedTask = await db.Set<TaskEntity>().IgnoreQueryFilters().SingleAsync();
        var deletedEvent = await db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync();
        Assert.Equal(operationId, deletedCalendar.DeletedByOperationId);
        Assert.Equal(operationId, deletedTask.DeletedByOperationId);
        Assert.Equal(operationId, deletedEvent.DeletedByOperationId);
        Assert.Equal("calendar-book", deletedCalendar.DeletedByOperationKind);
        Assert.Equal("task-book", deletedTask.DeletedByOperationKind);
        Assert.Equal("calendar-book", deletedEvent.DeletedByOperationKind);
        Assert.Equal(new DateTimeOffset(2026, 5, 26, 11, 0, 0, TimeSpan.Zero), deletedTask.PlannedEnd);
    }

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"calendar-stage5-model-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: Run the model tests and verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarStage5ModelTests
```

Expected: FAIL with compiler errors for missing `SourceUid`, `IsAllDay`, `TimeZoneId`, `SourceTimeZoneId`, `SourceIcsComponent`, `ExternalMetadataJson`, `RecurrenceId`, `ExDatesJson`, `RecurrenceMetadataJson`, `DeletedByOperationId`, `DeletedByOperationKind`, and `PlannedEnd`.

- [ ] **Step 3: Add entity properties**

Modify `src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs` by adding these properties before navigation collections:

```csharp
[Column("deleted_by_operation_id")] public Guid? DeletedByOperationId { get; set; }
[Column("deleted_by_operation_kind")][MaxLength(64)] public string? DeletedByOperationKind { get; set; }
```

Modify `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs` by adding these properties before `CreatedAt`:

```csharp
[Column("is_all_day")] public bool IsAllDay { get; set; }
[Column("time_zone_id")][MaxLength(100)] public string? TimeZoneId { get; set; }
[Column("source_time_zone_id")][MaxLength(100)] public string? SourceTimeZoneId { get; set; }
[Column("source_uid")][MaxLength(255)] public string? SourceUid { get; set; }
[Column("source_ics_component")] public string? SourceIcsComponent { get; set; }
[Column("external_metadata_json", TypeName = "jsonb")] public string ExternalMetadataJson { get; set; } = "{}";
[Column("recurrence_id")][MaxLength(255)] public string? RecurrenceId { get; set; }
[Column("exdates_json", TypeName = "jsonb")] public string ExDatesJson { get; set; } = "[]";
[Column("recurrence_metadata_json", TypeName = "jsonb")] public string RecurrenceMetadataJson { get; set; } = "{}";
[Column("deleted_by_operation_id")] public Guid? DeletedByOperationId { get; set; }
[Column("deleted_by_operation_kind")][MaxLength(64)] public string? DeletedByOperationKind { get; set; }
```

Modify `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs` by adding these properties after `DtStart` and before `CreatedAt`:

```csharp
[Column("planned_end")] public DateTimeOffset? PlannedEnd { get; set; }
[Column("deleted_by_operation_id")] public Guid? DeletedByOperationId { get; set; }
[Column("deleted_by_operation_kind")][MaxLength(64)] public string? DeletedByOperationKind { get; set; }
```

- [ ] **Step 4: Add EF configuration**

Modify `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`:

```csharp
public class CalendarEntityConfiguration : IEntityTypeConfiguration<CalendarEntity>
{
    public void Configure(EntityTypeBuilder<CalendarEntity> builder)
    {
        builder.HasQueryFilter(c => c.DeletedAt == null);
        builder.HasIndex(c => c.UserId);
        builder.HasIndex(c => new { c.UserId, c.DeletedAt });
        builder.HasIndex(c => c.DeletedByOperationId);
    }
}

public class EventEntityConfiguration : IEntityTypeConfiguration<EventEntity>
{
    public void Configure(EntityTypeBuilder<EventEntity> builder)
    {
        builder.HasQueryFilter(e => e.DeletedAt == null);
        builder.Property(e => e.ExternalMetadataJson).HasDefaultValue("{}");
        builder.Property(e => e.ExDatesJson).HasDefaultValue("[]");
        builder.Property(e => e.RecurrenceMetadataJson).HasDefaultValue("{}");
        builder.HasIndex(e => e.CalendarId);
        builder.HasIndex(e => e.Uid);
        builder.HasIndex(e => e.SourceUid);
        builder.HasIndex(e => new { e.DeletedAt, e.DtStart });
        builder.HasIndex(e => e.DeletedByOperationId);
        builder.HasOne(e => e.Calendar)
            .WithMany(c => c.Events)
            .HasForeignKey(e => e.CalendarId);
    }
}

public class TaskEntityConfiguration : IEntityTypeConfiguration<TaskEntity>
{
    public void Configure(EntityTypeBuilder<TaskEntity> builder)
    {
        builder.HasQueryFilter(t => t.DeletedAt == null);
        builder.HasIndex(t => t.UserId);
        builder.HasIndex(t => new { t.UserId, t.CalendarId });
        builder.HasIndex(t => t.Status);
        builder.HasIndex(t => new { t.UserId, t.DeletedAt });
        builder.HasIndex(t => new { t.UserId, t.DtStart, t.PlannedEnd });
        builder.HasIndex(t => t.DeletedByOperationId);
        builder.HasOne(t => t.Calendar)
            .WithMany(c => c.Tasks)
            .HasForeignKey(t => t.CalendarId);
        builder.HasOne(t => t.ParentTask)
            .WithMany(t => t.SubTasks)
            .HasForeignKey(t => t.ParentTaskId);
    }
}
```

Keep the existing `PendingConfirmationEntityConfiguration`, `SchedulingFeedbackEntityConfiguration`, and `OutlookConnectionEntityConfiguration` in the same file unchanged.

- [ ] **Step 5: Extend DTO records**

Modify `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs` so these records include Stage 5 fields:

```csharp
public record CreateEventRequest(
    [Required] Guid CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    [MaxLength(500)] string? Location,
    [Required] DateTimeOffset DtStart,
    [Required] DateTimeOffset DtEnd,
    string? RRule,
    string? Uid = null,
    bool IsAllDay = false,
    string? TimeZoneId = null
);

public record EventResponse(
    Guid Id, Guid CalendarId, string Uid, string Title,
    string? Description, string? Location,
    DateTimeOffset DtStart, DateTimeOffset DtEnd,
    string? RRule, string Status, string Source,
    Guid? OriginalEventId = null,
    bool IsAllDay = false,
    string? TimeZoneId = null,
    string? SourceTimeZoneId = null,
    string? SourceUid = null,
    string ExternalMetadataJson = "{}",
    string? RecurrenceId = null,
    string ExDatesJson = "[]",
    string RecurrenceMetadataJson = "{}"
);

public record CreateTaskRequest(
    Guid? CalendarId,
    [Required][MaxLength(255)] string Title,
    string? Description,
    int Priority,
    string? EstimatedDuration,
    string? MinimumSegment,
    DateTimeOffset? Due,
    DateTimeOffset? DtStart,
    string? Status = null,
    DateTimeOffset? PlannedEnd = null
);

public record TaskResponse(
    Guid Id, Guid? CalendarId, string Uid, string Title,
    string? Description, int Priority,
    string? EstimatedDuration, string? MinimumSegment,
    DateTimeOffset? DtStart, DateTimeOffset? Due,
    string Status, bool IsInbox, int SortOrder,
    List<TaskResponse> SubTasks,
    DateTimeOffset? PlannedEnd = null
);

public record MoveTaskRequest(
    DateTimeOffset? ScheduledStart,
    TimeSpan? Duration,
    int? NewSortOrder,
    DateTimeOffset? PlannedEnd = null
);
```

- [ ] **Step 6: Update service mapping for new fields**

Modify `CalendarService.CreateEventAsync`, `UpdateEventAsync`, `MapEvent`, `MapExpandedEvent`, `CreateTaskAsync`, `UpdateTaskAsync`, `MoveTaskAsync`, and `MapTask` to set and return:

```csharp
entity.IsAllDay = request.IsAllDay;
entity.TimeZoneId = request.TimeZoneId;
```

```csharp
task.PlannedEnd = request.PlannedEnd;
```

```csharp
if (request.PlannedEnd.HasValue)
    task.PlannedEnd = request.PlannedEnd;
else if (request.Duration.HasValue && request.ScheduledStart.HasValue)
    task.PlannedEnd = request.ScheduledStart.Value.Add(request.Duration.Value);
```

Return the new fields in `EventResponse` and `TaskResponse`.

- [ ] **Step 7: Add EF migration**

Run:

```powershell
dotnet ef migrations add Stage5CalendarTaskLoop --project src/Pim.Infrastructure --startup-project src/Pim.Api --context PimDbContext
```

Expected: migration files are created under `src/Pim.Infrastructure/Data/Migrations`.

- [ ] **Step 8: Run model tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarStage5ModelTests
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Entities src/modules/Pim.Module.Calendar/DTOs src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Calendar/CalendarStage5ModelTests.cs
git commit -m "feat(calendar): add stage 5 planning model fields"
```

---

### Task 2: Add Calendar Operation DTOs And Audit Writer

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs`

- [ ] **Step 1: Write failing audit writer tests**

Create `tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarAuditWriterTests
{
    [Fact]
    public async Task RecordSuccessAsync_WritesCalendarAuditWithMetadata()
    {
        await using var db = CreateDb();
        var writer = new CalendarAuditWriter(new AuditLogService(db));
        var userId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        var resourceId = Guid.Parse("11111111-1111-1111-1111-111111111111");

        await writer.RecordSuccessAsync(
            userId,
            "calendar.events.delete",
            "calendar_event",
            resourceId,
            new Dictionary<string, string>
            {
                ["title"] = "Focus block",
                ["operationId"] = "22222222-2222-2222-2222-222222222222",
                ["affectedCount"] = "1"
            });

        var audit = await db.AuditLogs.SingleAsync();
        Assert.Equal(userId, audit.UserId);
        Assert.Equal("calendar.events.delete", audit.Action);
        Assert.Equal("calendar_event", audit.ResourceType);
        Assert.Equal(resourceId.ToString(), audit.ResourceId);
        Assert.Equal("calendar", audit.Source);
        Assert.Contains("Focus block", audit.MetadataJson);
        Assert.Contains("affectedCount", audit.MetadataJson);
    }

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"calendar-audit-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: Run audit tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarAuditWriterTests
```

Expected: FAIL with missing `CalendarAuditWriter`.

- [ ] **Step 3: Add operation DTOs**

Append these records to `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`:

```csharp
public record CalendarOperationSample(
    Guid Id,
    string Type,
    string Title,
    DateTimeOffset? Start,
    DateTimeOffset? End,
    string? BookName
);

public record CalendarDeletePreviewResponse(
    string TargetType,
    Guid TargetId,
    string Title,
    string OperationKind,
    int AffectedCount,
    IReadOnlyList<CalendarOperationSample> Samples,
    string Summary,
    bool RequiresStrictConfirmation
);

public record CalendarOperationResult(
    string Operation,
    Guid OperationId,
    int AffectedCount,
    IReadOnlyList<Guid> AffectedIds,
    IReadOnlyList<CalendarOperationSample> Samples,
    string Message
);

public record CalendarRestoreConflict(
    Guid DeletedId,
    string DeletedType,
    Guid ActiveId,
    string ActiveType,
    string Reason,
    string Title
);

public record CalendarRestorePreviewResponse(
    string TargetType,
    Guid TargetId,
    string Title,
    int RestoreCount,
    IReadOnlyList<CalendarOperationSample> Samples,
    IReadOnlyList<CalendarRestoreConflict> Conflicts,
    bool CanRestoreWithoutConflict
);

public record CalendarRestoreRequest(bool RestoreAsCopy = false);

public record CalendarRecycleBinItem(
    Guid Id,
    string Type,
    string Title,
    DateTimeOffset DeletedAt,
    string? BookName,
    DateTimeOffset? Start,
    DateTimeOffset? End,
    string Source,
    Guid? DeletedByOperationId,
    string? DeletedByOperationKind
);

public record CalendarRecycleBinDetail(
    CalendarRecycleBinItem Item,
    string? Description,
    string MetadataJson,
    IReadOnlyList<CalendarOperationSample> ChildSamples
);

public record BatchIdsRequest(IReadOnlyList<Guid> Ids);

public record BatchTaskUpdateRequest(
    IReadOnlyList<Guid> Ids,
    string? Status,
    int? Priority,
    Guid? CalendarId
);

public record PlanTaskRequest(
    DateTimeOffset PlannedStart,
    DateTimeOffset? PlannedEnd,
    string? EstimatedDuration
);

public record ImportSkippedItem(
    string Reason,
    string Title,
    DateTimeOffset? Start,
    string? Uid
);

public record ImportReport(
    int Imported,
    int Skipped,
    IReadOnlyDictionary<string, int> SkippedReasons,
    IReadOnlyList<ImportSkippedItem> Samples
);
```

- [ ] **Step 4: Create audit writer**

Create `src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs`:

```csharp
using Pim.Core.Operations;

namespace Pim.Module.Calendar.Services;

public sealed class CalendarAuditWriter
{
    private const string Source = "calendar";
    private readonly IAuditLogService _auditLog;

    public CalendarAuditWriter(IAuditLogService auditLog)
    {
        _auditLog = auditLog;
    }

    public Task RecordSuccessAsync(
        Guid userId,
        string action,
        string resourceType,
        Guid resourceId,
        IReadOnlyDictionary<string, string>? metadata = null,
        CancellationToken ct = default)
        => _auditLog.RecordAsync(new CreateAuditLogRequest(
            userId,
            AuditActorType.User,
            action,
            resourceType,
            resourceId.ToString(),
            Source,
            AuditResult.Success,
            null,
            null,
            null,
            metadata,
            null,
            null), ct);
}
```

- [ ] **Step 5: Register audit writer**

Modify `src/modules/Pim.Module.Calendar/CalendarModule.cs` in `RegisterServices`:

```csharp
services.AddScoped<CalendarAuditWriter>();
```

- [ ] **Step 6: Run audit tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarAuditWriterTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/CalendarAuditWriterTests.cs
git commit -m "feat(calendar): add operation results and audit writer"
```

---

### Task 3: Implement Delete Preview And Grouped Soft Delete

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs`

- [ ] **Step 1: Write failing delete service tests**

Create `tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarDeleteServiceTests
{
    private static readonly Guid UserId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    [Fact]
    public async Task DeleteEventAsync_SoftDeletesEventAndWritesAudit()
    {
        await using var db = CreateDb();
        var calendar = SeedCalendar(db, "Work", "calendar");
        var evt = SeedEvent(db, calendar, "Focus block");
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var result = await service.DeleteEventAsync(evt.Id);

        Assert.Equal("calendar.events.delete", result.Operation);
        Assert.Equal(1, result.AffectedCount);
        var deleted = await db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync();
        Assert.NotNull(deleted.DeletedAt);
        Assert.Equal(result.OperationId, deleted.DeletedByOperationId);
        Assert.Equal("single-event", deleted.DeletedByOperationKind);
        var audit = await db.AuditLogs.SingleAsync();
        Assert.Equal("calendar.events.delete", audit.Action);
    }

    [Fact]
    public async Task DeleteCalendarBookAsync_DeletesOnlyActiveChildrenWithSameOperationId()
    {
        await using var db = CreateDb();
        var calendar = SeedCalendar(db, "Work", "calendar");
        var active = SeedEvent(db, calendar, "Active child");
        var alreadyDeleted = SeedEvent(db, calendar, "Earlier child");
        alreadyDeleted.DeletedAt = DateTimeOffset.UtcNow.AddDays(-1);
        alreadyDeleted.DeletedByOperationId = Guid.Parse("99999999-9999-9999-9999-999999999999");
        alreadyDeleted.DeletedByOperationKind = "single-event";
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var preview = await service.PreviewCalendarDeleteAsync(calendar.Id);
        Assert.Equal(1, preview.AffectedCount);
        Assert.Contains(preview.Samples, sample => sample.Title == "Active child");

        var result = await service.DeleteCalendarAsync(calendar.Id);

        var deletedCalendar = await db.Set<CalendarEntity>().IgnoreQueryFilters().SingleAsync(c => c.Id == calendar.Id);
        var deletedActive = await db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync(e => e.Id == active.Id);
        var untouchedEarlier = await db.Set<EventEntity>().IgnoreQueryFilters().SingleAsync(e => e.Id == alreadyDeleted.Id);
        Assert.Equal(result.OperationId, deletedCalendar.DeletedByOperationId);
        Assert.Equal(result.OperationId, deletedActive.DeletedByOperationId);
        Assert.NotEqual(result.OperationId, untouchedEarlier.DeletedByOperationId);
        Assert.Equal("calendar-book", deletedCalendar.DeletedByOperationKind);
        Assert.Equal("calendar-book", deletedActive.DeletedByOperationKind);
    }

    [Fact]
    public async Task BatchDeleteTasksAsync_UsesOneOperationIdForAllTasks()
    {
        await using var db = CreateDb();
        var taskA = SeedTask(db, "A");
        var taskB = SeedTask(db, "B");
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var result = await service.BatchDeleteTasksAsync(new[] { taskA.Id, taskB.Id });

        Assert.Equal(2, result.AffectedCount);
        var deleted = await db.Set<TaskEntity>().IgnoreQueryFilters().OrderBy(t => t.Title).ToListAsync();
        Assert.All(deleted, task => Assert.Equal(result.OperationId, task.DeletedByOperationId));
        Assert.All(deleted, task => Assert.Equal("batch-task", task.DeletedByOperationKind));
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(EventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"calendar-delete-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }

    private static CalendarDeleteService CreateService(PimDbContext db)
        => new(
            db,
            new FixedCurrentUserService(UserId),
            new CalendarAuditWriter(new AuditLogService(db)));

    private static CalendarEntity SeedCalendar(PimDbContext db, string name, string kind)
    {
        var calendar = new CalendarEntity
        {
            UserId = UserId,
            Name = name,
            Kind = kind,
            Color = "#2563EB"
        };
        db.Set<CalendarEntity>().Add(calendar);
        return calendar;
    }

    private static EventEntity SeedEvent(PimDbContext db, CalendarEntity calendar, string title)
    {
        var evt = new EventEntity
        {
            Calendar = calendar,
            CalendarId = calendar.Id,
            Uid = $"{Guid.NewGuid()}@pim",
            Title = title,
            DtStart = new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero),
            DtEnd = new DateTimeOffset(2026, 5, 26, 10, 0, 0, TimeSpan.Zero)
        };
        db.Set<EventEntity>().Add(evt);
        return evt;
    }

    private static TaskEntity SeedTask(PimDbContext db, string title)
    {
        var task = new TaskEntity
        {
            UserId = UserId,
            Uid = $"{Guid.NewGuid()}@pim",
            Title = title
        };
        db.Set<TaskEntity>().Add(task);
        return task;
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }
}
```

- [ ] **Step 2: Run delete tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarDeleteServiceTests
```

Expected: FAIL with missing `CalendarDeleteService`.

- [ ] **Step 3: Create delete service**

Create `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs` with these public methods:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class CalendarDeleteService
{
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;
    private readonly CalendarAuditWriter _audit;

    public CalendarDeleteService(PimDbContext db, ICurrentUserService currentUser, CalendarAuditWriter audit)
    {
        _db = db;
        _currentUser = currentUser;
        _audit = audit;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(1002, "Not authenticated");

    public async Task<CalendarDeletePreviewResponse> PreviewCalendarDeleteAsync(Guid calendarId, CancellationToken ct = default)
    {
        var calendar = await LoadActiveCalendarAsync(calendarId, ct);
        var samples = calendar.Kind == "task"
            ? await _db.Set<TaskEntity>().Where(t => t.CalendarId == calendarId && t.UserId == UserId)
                .OrderBy(t => t.Due ?? DateTimeOffset.MaxValue)
                .Take(5)
                .Select(t => new CalendarOperationSample(t.Id, "task", t.Title, t.DtStart, t.PlannedEnd, calendar.Name))
                .ToListAsync(ct)
            : await _db.Set<EventEntity>().Where(e => e.CalendarId == calendarId && e.Calendar.UserId == UserId)
                .OrderBy(e => e.DtStart)
                .Take(5)
                .Select(e => new CalendarOperationSample(e.Id, "event", e.Title, e.DtStart, e.DtEnd, calendar.Name))
                .ToListAsync(ct);

        var count = calendar.Kind == "task"
            ? await _db.Set<TaskEntity>().CountAsync(t => t.CalendarId == calendarId && t.UserId == UserId, ct)
            : await _db.Set<EventEntity>().CountAsync(e => e.CalendarId == calendarId && e.Calendar.UserId == UserId, ct);

        return new CalendarDeletePreviewResponse(
            calendar.Kind == "task" ? "task-book" : "calendar",
            calendar.Id,
            calendar.Name,
            calendar.Kind == "task" ? "task-book" : "calendar-book",
            count,
            samples,
            $"Deleting {calendar.Name} moves {count} active child item(s) to the recycle bin.",
            true);
    }

    public async Task<CalendarOperationResult> DeleteCalendarAsync(Guid calendarId, CancellationToken ct = default)
    {
        var calendar = await LoadActiveCalendarAsync(calendarId, ct);
        var operationId = Guid.NewGuid();
        var operationKind = calendar.Kind == "task" ? "task-book" : "calendar-book";
        var now = DateTimeOffset.UtcNow;
        var affectedIds = new List<Guid> { calendar.Id };
        var samples = new List<CalendarOperationSample>();

        calendar.DeletedAt = now;
        calendar.DeletedByOperationId = operationId;
        calendar.DeletedByOperationKind = operationKind;

        if (calendar.Kind == "task")
        {
            var tasks = await _db.Set<TaskEntity>()
                .Where(t => t.CalendarId == calendar.Id && t.UserId == UserId)
                .OrderBy(t => t.Due ?? DateTimeOffset.MaxValue)
                .ToListAsync(ct);
            foreach (var task in tasks)
            {
                task.DeletedAt = now;
                task.DeletedByOperationId = operationId;
                task.DeletedByOperationKind = operationKind;
                affectedIds.Add(task.Id);
                if (samples.Count < 5)
                    samples.Add(new CalendarOperationSample(task.Id, "task", task.Title, task.DtStart, task.PlannedEnd, calendar.Name));
            }
        }
        else
        {
            var events = await _db.Set<EventEntity>()
                .Where(e => e.CalendarId == calendar.Id && e.Calendar.UserId == UserId)
                .OrderBy(e => e.DtStart)
                .ToListAsync(ct);
            foreach (var evt in events)
            {
                evt.DeletedAt = now;
                evt.DeletedByOperationId = operationId;
                evt.DeletedByOperationKind = operationKind;
                affectedIds.Add(evt.Id);
                if (samples.Count < 5)
                    samples.Add(new CalendarOperationSample(evt.Id, "event", evt.Title, evt.DtStart, evt.DtEnd, calendar.Name));
            }
        }

        await _db.SaveChangesAsync(ct);
        await _audit.RecordSuccessAsync(UserId, "calendar.books.delete", "calendar_book", calendar.Id,
            new Dictionary<string, string>
            {
                ["operationId"] = operationId.ToString(),
                ["operationKind"] = operationKind,
                ["title"] = calendar.Name,
                ["affectedCount"] = affectedIds.Count.ToString()
            }, ct);

        return new CalendarOperationResult("calendar.books.delete", operationId, affectedIds.Count, affectedIds, samples, "Moved to recycle bin");
    }

    public async Task<CalendarOperationResult> DeleteEventAsync(Guid eventId, CancellationToken ct = default)
    {
        var evt = await _db.Set<EventEntity>()
            .FirstOrDefaultAsync(e => e.Id == eventId && e.Calendar.UserId == UserId, ct)
            ?? throw new DomainException(02001, "Event not found");
        var operationId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        evt.DeletedAt = now;
        evt.DeletedByOperationId = operationId;
        evt.DeletedByOperationKind = "single-event";
        await _db.SaveChangesAsync(ct);
        await _audit.RecordSuccessAsync(UserId, "calendar.events.delete", "calendar_event", evt.Id,
            new Dictionary<string, string> { ["operationId"] = operationId.ToString(), ["title"] = evt.Title, ["affectedCount"] = "1" }, ct);
        var sample = new CalendarOperationSample(evt.Id, "event", evt.Title, evt.DtStart, evt.DtEnd, evt.Calendar?.Name);
        return new CalendarOperationResult("calendar.events.delete", operationId, 1, new[] { evt.Id }, new[] { sample }, "Moved to recycle bin");
    }

    public async Task<CalendarOperationResult> BatchDeleteEventsAsync(IEnumerable<Guid> ids, CancellationToken ct = default)
    {
        var idSet = ids.Distinct().ToHashSet();
        var events = await _db.Set<EventEntity>()
            .Where(e => idSet.Contains(e.Id) && e.Calendar.UserId == UserId)
            .OrderBy(e => e.DtStart)
            .ToListAsync(ct);
        var operationId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        foreach (var evt in events)
        {
            evt.DeletedAt = now;
            evt.DeletedByOperationId = operationId;
            evt.DeletedByOperationKind = "batch-event";
        }
        await _db.SaveChangesAsync(ct);
        var samples = events.Take(5).Select(e => new CalendarOperationSample(e.Id, "event", e.Title, e.DtStart, e.DtEnd, e.Calendar?.Name)).ToList();
        await _audit.RecordSuccessAsync(UserId, "calendar.events.batch_delete", "calendar_event", operationId,
            new Dictionary<string, string> { ["operationId"] = operationId.ToString(), ["affectedCount"] = events.Count.ToString() }, ct);
        return new CalendarOperationResult("calendar.events.batch_delete", operationId, events.Count, events.Select(e => e.Id).ToList(), samples, "Moved to recycle bin");
    }

    public async Task<CalendarOperationResult> DeleteTaskAsync(Guid taskId, CancellationToken ct = default)
    {
        var task = await _db.Set<TaskEntity>()
            .FirstOrDefaultAsync(t => t.Id == taskId && t.UserId == UserId, ct)
            ?? throw new DomainException(02004, "Task not found");
        return await SoftDeleteTasksAsync(new[] { task }, "single-task", "calendar.tasks.delete", ct);
    }

    public async Task<CalendarOperationResult> BatchDeleteTasksAsync(IEnumerable<Guid> ids, CancellationToken ct = default)
    {
        var idSet = ids.Distinct().ToHashSet();
        var tasks = await _db.Set<TaskEntity>()
            .Where(t => idSet.Contains(t.Id) && t.UserId == UserId)
            .OrderBy(t => t.Due ?? DateTimeOffset.MaxValue)
            .ToListAsync(ct);
        return await SoftDeleteTasksAsync(tasks, "batch-task", "calendar.tasks.batch_delete", ct);
    }

    private async Task<CalendarOperationResult> SoftDeleteTasksAsync(IReadOnlyList<TaskEntity> tasks, string operationKind, string action, CancellationToken ct)
    {
        var operationId = Guid.NewGuid();
        var now = DateTimeOffset.UtcNow;
        foreach (var task in tasks)
        {
            task.DeletedAt = now;
            task.DeletedByOperationId = operationId;
            task.DeletedByOperationKind = operationKind;
        }
        await _db.SaveChangesAsync(ct);
        var samples = tasks.Take(5).Select(t => new CalendarOperationSample(t.Id, "task", t.Title, t.DtStart, t.PlannedEnd, t.Calendar?.Name)).ToList();
        await _audit.RecordSuccessAsync(UserId, action, "calendar_task", operationId,
            new Dictionary<string, string> { ["operationId"] = operationId.ToString(), ["affectedCount"] = tasks.Count.ToString() }, ct);
        return new CalendarOperationResult(action, operationId, tasks.Count, tasks.Select(t => t.Id).ToList(), samples, "Moved to recycle bin");
    }

    private async Task<CalendarEntity> LoadActiveCalendarAsync(Guid calendarId, CancellationToken ct)
        => await _db.Set<CalendarEntity>().FirstOrDefaultAsync(c => c.Id == calendarId && c.UserId == UserId, ct)
            ?? throw new DomainException(02002, "Calendar not found");
}
```

- [ ] **Step 4: Register delete service**

Modify `CalendarModule.RegisterServices`:

```csharp
services.AddScoped<CalendarDeleteService>();
```

- [ ] **Step 5: Wire delete endpoints to the delete service**

Modify `CalendarModule.MapEndpoints`:

```csharp
group.MapPost("/calendars/{id:guid}/delete-preview", async (
    Guid id,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarDeletePreviewResponse>.Ok(await svc.PreviewCalendarDeleteAsync(id, ct))));

group.MapDelete("/calendars/{id:guid}", async (
    Guid id,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.DeleteCalendarAsync(id, ct))));

group.MapDelete("/events/{id:guid}", async (
    Guid id,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.DeleteEventAsync(id, ct))));

group.MapPost("/events/batch-delete", async (
    [FromBody] BatchIdsRequest req,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.BatchDeleteEventsAsync(req.Ids, ct))));

group.MapDelete("/tasks/{id:guid}", async (
    Guid id,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.DeleteTaskAsync(id, ct))));

group.MapPost("/tasks/batch-delete", async (
    [FromBody] BatchIdsRequest req,
    [FromServices] CalendarDeleteService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.BatchDeleteTasksAsync(req.Ids, ct))));
```

Remove or replace the older delete endpoint bodies that returned `ApiResponse<string>.Ok("deleted")`.

- [ ] **Step 6: Run delete tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarDeleteServiceTests
```

Expected: PASS.

- [ ] **Step 7: Run quick calendar API smoke tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarService
```

Expected: PASS or "No test matches" if no service tests use that namespace filter.

- [ ] **Step 8: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/CalendarDeleteServiceTests.cs
git commit -m "feat(calendar): add grouped soft delete"
```

---

### Task 4: Implement Recycle Bin Query, Restore, Conflict Handling, And Restore-As-Copy

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs`

- [ ] **Step 1: Write failing recycle-bin tests**

Create `tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Infrastructure.Operations;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarRecycleBinServiceTests
{
    private static readonly Guid UserId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    [Fact]
    public async Task ListAsync_ReturnsDeletedEventsAndTasksOnly()
    {
        await using var db = CreateDb();
        var calendar = SeedCalendar(db, "Work", "calendar");
        SeedDeletedEvent(db, calendar, "Deleted event");
        SeedActiveEvent(db, calendar, "Active event");
        SeedDeletedTask(db, "Deleted task");
        SeedActiveTask(db, "Active task");
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var result = await service.ListAsync("all", null, null, null, 1, 20);

        Assert.Equal(2, result.TotalCount);
        Assert.Contains(result.Items, item => item.Type == "event" && item.Title == "Deleted event");
        Assert.Contains(result.Items, item => item.Type == "task" && item.Title == "Deleted task");
        Assert.DoesNotContain(result.Items, item => item.Title == "Active event");
        Assert.DoesNotContain(result.Items, item => item.Title == "Active task");
    }

    [Fact]
    public async Task RestoreEventAsync_ReturnsConflictWhenEquivalentActiveEventExists()
    {
        await using var db = CreateDb();
        var calendar = SeedCalendar(db, "Work", "calendar");
        var deleted = SeedDeletedEvent(db, calendar, "Standup");
        SeedActiveEvent(db, calendar, "Standup");
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var preview = await service.PreviewRestoreAsync("event", deleted.Id);

        Assert.False(preview.CanRestoreWithoutConflict);
        var conflict = Assert.Single(preview.Conflicts);
        Assert.Equal("same-title-time", conflict.Reason);
    }

    [Fact]
    public async Task RestoreEventAsCopy_ClearsDeletedAtAndCreatesNewUid()
    {
        await using var db = CreateDb();
        var calendar = SeedCalendar(db, "Work", "calendar");
        var deleted = SeedDeletedEvent(db, calendar, "Standup");
        var originalUid = deleted.Uid;
        SeedActiveEvent(db, calendar, "Standup");
        await db.SaveChangesAsync();
        var service = CreateService(db);

        var result = await service.RestoreAsync("event", deleted.Id, new CalendarRestoreRequest(RestoreAsCopy: true));

        var restored = await db.Set<EventEntity>().SingleAsync(e => e.Id == deleted.Id);
        Assert.Null(restored.DeletedAt);
        Assert.NotEqual(originalUid, restored.Uid);
        Assert.Null(restored.SourceUid);
        Assert.Equal("calendar.recycle_bin.restore_copy", result.Operation);
    }

    [Fact]
    public async Task RestoreCalendar_RestoresOnlyChildrenFromSameOperation()
    {
        await using var db = CreateDb();
        var operationId = Guid.Parse("11111111-1111-1111-1111-111111111111");
        var calendar = SeedCalendar(db, "Deleted book", "calendar");
        calendar.DeletedAt = DateTimeOffset.UtcNow;
        calendar.DeletedByOperationId = operationId;
        calendar.DeletedByOperationKind = "calendar-book";
        var sameOperation = SeedDeletedEvent(db, calendar, "Same operation");
        sameOperation.DeletedByOperationId = operationId;
        sameOperation.DeletedByOperationKind = "calendar-book";
        var earlier = SeedDeletedEvent(db, calendar, "Earlier deleted");
        earlier.DeletedByOperationId = Guid.Parse("22222222-2222-2222-2222-222222222222");
        earlier.DeletedByOperationKind = "single-event";
        await db.SaveChangesAsync();
        var service = CreateService(db);

        await service.RestoreAsync("calendar", calendar.Id, new CalendarRestoreRequest());

        Assert.NotNull(await db.Set<CalendarEntity>().SingleOrDefaultAsync(c => c.Id == calendar.Id));
        Assert.NotNull(await db.Set<EventEntity>().SingleOrDefaultAsync(e => e.Id == sameOperation.Id));
        Assert.Null(await db.Set<EventEntity>().SingleOrDefaultAsync(e => e.Id == earlier.Id));
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(EventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"calendar-recycle-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }

    private static CalendarRecycleBinService CreateService(PimDbContext db)
        => new(db, new FixedCurrentUserService(UserId), new CalendarAuditWriter(new AuditLogService(db)));

    private static CalendarEntity SeedCalendar(PimDbContext db, string name, string kind)
    {
        var calendar = new CalendarEntity { UserId = UserId, Name = name, Kind = kind, Color = "#2563EB" };
        db.Set<CalendarEntity>().Add(calendar);
        return calendar;
    }

    private static EventEntity SeedActiveEvent(PimDbContext db, CalendarEntity calendar, string title)
    {
        var evt = new EventEntity
        {
            Calendar = calendar,
            CalendarId = calendar.Id,
            Uid = $"{Guid.NewGuid()}@pim",
            Title = title,
            DtStart = new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero),
            DtEnd = new DateTimeOffset(2026, 5, 26, 10, 0, 0, TimeSpan.Zero)
        };
        db.Set<EventEntity>().Add(evt);
        return evt;
    }

    private static EventEntity SeedDeletedEvent(PimDbContext db, CalendarEntity calendar, string title)
    {
        var evt = SeedActiveEvent(db, calendar, title);
        evt.DeletedAt = DateTimeOffset.UtcNow;
        evt.DeletedByOperationId = Guid.NewGuid();
        evt.DeletedByOperationKind = "single-event";
        return evt;
    }

    private static TaskEntity SeedActiveTask(PimDbContext db, string title)
    {
        var task = new TaskEntity
        {
            UserId = UserId,
            Uid = $"{Guid.NewGuid()}@pim",
            Title = title,
            DtStart = new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero),
            Due = new DateTimeOffset(2026, 5, 27, 9, 0, 0, TimeSpan.Zero)
        };
        db.Set<TaskEntity>().Add(task);
        return task;
    }

    private static TaskEntity SeedDeletedTask(PimDbContext db, string title)
    {
        var task = SeedActiveTask(db, title);
        task.DeletedAt = DateTimeOffset.UtcNow;
        task.DeletedByOperationId = Guid.NewGuid();
        task.DeletedByOperationKind = "single-task";
        return task;
    }

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }
}
```

- [ ] **Step 2: Run recycle-bin tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarRecycleBinServiceTests
```

Expected: FAIL with missing `CalendarRecycleBinService`.

- [ ] **Step 3: Implement recycle-bin service**

Create `src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs` with these responsibilities:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Common;
using Pim.Core.Exceptions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class CalendarRecycleBinService
{
    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;
    private readonly CalendarAuditWriter _audit;

    public CalendarRecycleBinService(PimDbContext db, ICurrentUserService currentUser, CalendarAuditWriter audit)
    {
        _db = db;
        _currentUser = currentUser;
        _audit = audit;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(1002, "Not authenticated");

    public async Task<PagedResult<CalendarRecycleBinItem>> ListAsync(
        string? type,
        string? search,
        DateTimeOffset? deletedFrom,
        DateTimeOffset? deletedTo,
        int page,
        int pageSize,
        CancellationToken ct = default)
    {
        var normalizedType = string.IsNullOrWhiteSpace(type) ? "all" : type.Trim();
        var items = new List<CalendarRecycleBinItem>();

        if (normalizedType is "all" or "calendar" or "task-book")
        {
            var calendars = await _db.Set<CalendarEntity>().IgnoreQueryFilters()
                .Where(c => c.UserId == UserId && c.DeletedAt != null)
                .ToListAsync(ct);
            items.AddRange(calendars.Select(c => new CalendarRecycleBinItem(
                c.Id,
                c.Kind == "task" ? "task-book" : "calendar",
                c.Name,
                c.DeletedAt!.Value,
                null,
                null,
                null,
                "manual",
                c.DeletedByOperationId,
                c.DeletedByOperationKind)));
        }

        if (normalizedType is "all" or "event")
        {
            var events = await _db.Set<EventEntity>().IgnoreQueryFilters()
                .Include(e => e.Calendar)
                .Where(e => e.DeletedAt != null && e.Calendar.UserId == UserId)
                .ToListAsync(ct);
            items.AddRange(events.Select(e => new CalendarRecycleBinItem(
                e.Id,
                "event",
                e.Title,
                e.DeletedAt!.Value,
                e.Calendar.Name,
                e.DtStart,
                e.DtEnd,
                e.Source,
                e.DeletedByOperationId,
                e.DeletedByOperationKind)));
        }

        if (normalizedType is "all" or "task")
        {
            var tasks = await _db.Set<TaskEntity>().IgnoreQueryFilters()
                .Include(t => t.Calendar)
                .Where(t => t.DeletedAt != null && t.UserId == UserId)
                .ToListAsync(ct);
            items.AddRange(tasks.Select(t => new CalendarRecycleBinItem(
                t.Id,
                "task",
                t.Title,
                t.DeletedAt!.Value,
                t.Calendar?.Name,
                t.DtStart,
                t.PlannedEnd,
                "manual",
                t.DeletedByOperationId,
                t.DeletedByOperationKind)));
        }

        if (!string.IsNullOrWhiteSpace(search))
            items = items.Where(i => i.Title.Contains(search.Trim(), StringComparison.OrdinalIgnoreCase)).ToList();
        if (deletedFrom.HasValue)
            items = items.Where(i => i.DeletedAt >= deletedFrom.Value).ToList();
        if (deletedTo.HasValue)
            items = items.Where(i => i.DeletedAt <= deletedTo.Value).ToList();

        var safePage = Math.Max(1, page);
        var safePageSize = Math.Clamp(pageSize, 1, 100);
        var ordered = items.OrderByDescending(i => i.DeletedAt).ToList();
        var total = ordered.Count;
        var pageItems = ordered.Skip((safePage - 1) * safePageSize).Take(safePageSize).ToList();
        return new PagedResult<CalendarRecycleBinItem>(pageItems, safePage, safePageSize, total, (int)Math.Ceiling(total / (double)safePageSize));
    }

    public async Task<CalendarRestorePreviewResponse> PreviewRestoreAsync(string type, Guid id, CancellationToken ct = default)
    {
        var normalizedType = NormalizeType(type);
        var samples = new List<CalendarOperationSample>();
        var conflicts = new List<CalendarRestoreConflict>();

        if (normalizedType == "event")
        {
            var evt = await LoadDeletedEventAsync(id, ct);
            samples.Add(new CalendarOperationSample(evt.Id, "event", evt.Title, evt.DtStart, evt.DtEnd, evt.Calendar.Name));
            conflicts.AddRange(await FindEventConflictsAsync(evt, ct));
            return new CalendarRestorePreviewResponse("event", evt.Id, evt.Title, 1, samples, conflicts, conflicts.Count == 0);
        }

        if (normalizedType == "task")
        {
            var task = await LoadDeletedTaskAsync(id, ct);
            samples.Add(new CalendarOperationSample(task.Id, "task", task.Title, task.DtStart, task.PlannedEnd, task.Calendar?.Name));
            conflicts.AddRange(await FindTaskConflictsAsync(task, ct));
            return new CalendarRestorePreviewResponse("task", task.Id, task.Title, 1, samples, conflicts, conflicts.Count == 0);
        }

        var calendar = await LoadDeletedCalendarAsync(id, ct);
        var childSamples = await BuildBookRestoreSamplesAsync(calendar, ct);
        return new CalendarRestorePreviewResponse(normalizedType, calendar.Id, calendar.Name, 1 + childSamples.Count, childSamples, conflicts, true);
    }

    public async Task<CalendarOperationResult> RestoreAsync(string type, Guid id, CalendarRestoreRequest request, CancellationToken ct = default)
    {
        var preview = await PreviewRestoreAsync(type, id, ct);
        if (preview.Conflicts.Count > 0 && !request.RestoreAsCopy)
            throw new DomainException(02020, "Restore has conflicts");

        var normalizedType = NormalizeType(type);
        var operationId = Guid.NewGuid();
        var affected = new List<Guid>();
        var samples = new List<CalendarOperationSample>();
        var action = request.RestoreAsCopy ? "calendar.recycle_bin.restore_copy" : "calendar.recycle_bin.restore";

        if (normalizedType == "event")
        {
            var evt = await LoadDeletedEventAsync(id, ct);
            if (request.RestoreAsCopy)
            {
                evt.Uid = $"{Guid.NewGuid()}@pim";
                evt.SourceUid = null;
            }
            ClearDelete(evt);
            affected.Add(evt.Id);
            samples.Add(new CalendarOperationSample(evt.Id, "event", evt.Title, evt.DtStart, evt.DtEnd, evt.Calendar.Name));
        }
        else if (normalizedType == "task")
        {
            var task = await LoadDeletedTaskAsync(id, ct);
            if (request.RestoreAsCopy)
                task.Uid = $"{Guid.NewGuid()}@pim";
            ClearDelete(task);
            affected.Add(task.Id);
            samples.Add(new CalendarOperationSample(task.Id, "task", task.Title, task.DtStart, task.PlannedEnd, task.Calendar?.Name));
        }
        else
        {
            var calendar = await LoadDeletedCalendarAsync(id, ct);
            var restoreOperationId = calendar.DeletedByOperationId;
            ClearDelete(calendar);
            affected.Add(calendar.Id);
            if (restoreOperationId.HasValue)
            {
                if (calendar.Kind == "task")
                {
                    var tasks = await _db.Set<TaskEntity>().IgnoreQueryFilters()
                        .Where(t => t.UserId == UserId && t.DeletedAt != null && t.DeletedByOperationId == restoreOperationId)
                        .ToListAsync(ct);
                    foreach (var task in tasks)
                    {
                        ClearDelete(task);
                        affected.Add(task.Id);
                        if (samples.Count < 5)
                            samples.Add(new CalendarOperationSample(task.Id, "task", task.Title, task.DtStart, task.PlannedEnd, calendar.Name));
                    }
                }
                else
                {
                    var events = await _db.Set<EventEntity>().IgnoreQueryFilters()
                        .Where(e => e.CalendarId == calendar.Id && e.DeletedAt != null && e.DeletedByOperationId == restoreOperationId)
                        .ToListAsync(ct);
                    foreach (var evt in events)
                    {
                        ClearDelete(evt);
                        affected.Add(evt.Id);
                        if (samples.Count < 5)
                            samples.Add(new CalendarOperationSample(evt.Id, "event", evt.Title, evt.DtStart, evt.DtEnd, calendar.Name));
                    }
                }
            }
        }

        await _db.SaveChangesAsync(ct);
        await _audit.RecordSuccessAsync(UserId, action, "calendar_recycle_bin", id,
            new Dictionary<string, string>
            {
                ["operationId"] = operationId.ToString(),
                ["targetType"] = normalizedType,
                ["affectedCount"] = affected.Count.ToString()
            }, ct);

        return new CalendarOperationResult(action, operationId, affected.Count, affected, samples, "Restored from recycle bin");
    }

    private static string NormalizeType(string type)
        => type.Trim() switch
        {
            "calendar" => "calendar",
            "task-book" => "task-book",
            "event" => "event",
            "task" => "task",
            _ => throw new DomainException(02021, "Unsupported recycle bin type")
        };

    private static void ClearDelete(CalendarEntity entity)
    {
        entity.DeletedAt = null;
        entity.DeletedByOperationId = null;
        entity.DeletedByOperationKind = null;
        entity.UpdatedAt = DateTimeOffset.UtcNow;
    }

    private static void ClearDelete(EventEntity entity)
    {
        entity.DeletedAt = null;
        entity.DeletedByOperationId = null;
        entity.DeletedByOperationKind = null;
        entity.UpdatedAt = DateTimeOffset.UtcNow;
    }

    private static void ClearDelete(TaskEntity entity)
    {
        entity.DeletedAt = null;
        entity.DeletedByOperationId = null;
        entity.DeletedByOperationKind = null;
        entity.UpdatedAt = DateTimeOffset.UtcNow;
    }
}
```

Add private loader/conflict helper methods in the same file:

```csharp
private async Task<EventEntity> LoadDeletedEventAsync(Guid id, CancellationToken ct)
{
    var entity = await _db.Set<EventEntity>()
        .IgnoreQueryFilters()
        .Include(e => e.Calendar)
        .FirstOrDefaultAsync(e => e.Id == id && e.DeletedAt != null && e.Calendar.UserId == UserId, ct);

    return entity ?? throw new DomainException(02001, "Event not found");
}

private async Task<TaskEntity> LoadDeletedTaskAsync(Guid id, CancellationToken ct)
{
    var entity = await _db.Set<TaskEntity>()
        .IgnoreQueryFilters()
        .Include(t => t.Calendar)
        .FirstOrDefaultAsync(t => t.Id == id && t.DeletedAt != null && t.UserId == UserId, ct);

    return entity ?? throw new DomainException(02004, "Task not found");
}

private async Task<CalendarEntity> LoadDeletedCalendarAsync(Guid id, CancellationToken ct)
{
    var entity = await _db.Set<CalendarEntity>()
        .IgnoreQueryFilters()
        .FirstOrDefaultAsync(c => c.Id == id && c.DeletedAt != null && c.UserId == UserId, ct);

    return entity ?? throw new DomainException(02002, "Calendar not found");
}

private async Task<IReadOnlyList<CalendarRestoreConflict>> FindEventConflictsAsync(EventEntity evt, CancellationToken ct)
{
    var conflicts = new List<CalendarRestoreConflict>();
    if (!string.IsNullOrWhiteSpace(evt.Uid))
    {
        var sameUid = await _db.Set<EventEntity>()
            .FirstOrDefaultAsync(e => e.Id != evt.Id && e.Calendar.UserId == UserId && e.Uid == evt.Uid, ct);
        if (sameUid is not null)
            conflicts.Add(new CalendarRestoreConflict(evt.Id, "event", sameUid.Id, "event", "same-uid", sameUid.Title));
    }
    if (!string.IsNullOrWhiteSpace(evt.SourceUid))
    {
        var sameSourceUid = await _db.Set<EventEntity>()
            .FirstOrDefaultAsync(e => e.Id != evt.Id && e.Calendar.UserId == UserId && e.SourceUid == evt.SourceUid, ct);
        if (sameSourceUid is not null)
            conflicts.Add(new CalendarRestoreConflict(evt.Id, "event", sameSourceUid.Id, "event", "same-source-uid", sameSourceUid.Title));
    }
    var sameTime = await _db.Set<EventEntity>()
        .FirstOrDefaultAsync(e => e.Id != evt.Id && e.Calendar.UserId == UserId && e.Title == evt.Title && e.DtStart == evt.DtStart && e.DtEnd == evt.DtEnd, ct);
    if (sameTime is not null)
        conflicts.Add(new CalendarRestoreConflict(evt.Id, "event", sameTime.Id, "event", "same-title-time", sameTime.Title));
    return conflicts;
}

private async Task<IReadOnlyList<CalendarRestoreConflict>> FindTaskConflictsAsync(TaskEntity task, CancellationToken ct)
{
    var sameTask = await _db.Set<TaskEntity>()
        .FirstOrDefaultAsync(t => t.Id != task.Id && t.UserId == UserId && t.Title == task.Title && t.Due == task.Due && t.DtStart == task.DtStart, ct);
    return sameTask is null
        ? Array.Empty<CalendarRestoreConflict>()
        : new[] { new CalendarRestoreConflict(task.Id, "task", sameTask.Id, "task", "same-title-due-planned-start", sameTask.Title) };
}

private async Task<IReadOnlyList<CalendarOperationSample>> BuildBookRestoreSamplesAsync(CalendarEntity calendar, CancellationToken ct)
{
    if (calendar.DeletedByOperationId is null) return Array.Empty<CalendarOperationSample>();
    if (calendar.Kind == "task")
    {
        return await _db.Set<TaskEntity>().IgnoreQueryFilters()
            .Where(t => t.UserId == UserId && t.DeletedAt != null && t.DeletedByOperationId == calendar.DeletedByOperationId)
            .OrderBy(t => t.Due ?? DateTimeOffset.MaxValue)
            .Take(5)
            .Select(t => new CalendarOperationSample(t.Id, "task", t.Title, t.DtStart, t.PlannedEnd, calendar.Name))
            .ToListAsync(ct);
    }

    return await _db.Set<EventEntity>().IgnoreQueryFilters()
        .Where(e => e.CalendarId == calendar.Id && e.DeletedAt != null && e.DeletedByOperationId == calendar.DeletedByOperationId)
        .OrderBy(e => e.DtStart)
        .Take(5)
        .Select(e => new CalendarOperationSample(e.Id, "event", e.Title, e.DtStart, e.DtEnd, calendar.Name))
        .ToListAsync(ct);
}
```

- [ ] **Step 4: Register and map recycle-bin endpoints**

Modify `CalendarModule.RegisterServices`:

```csharp
services.AddScoped<CalendarRecycleBinService>();
```

Add endpoints to `CalendarModule.MapEndpoints`:

```csharp
group.MapGet("/recycle-bin", async (
    [FromQuery] string? type,
    [FromQuery] string? search,
    [FromQuery] DateTimeOffset? deletedFrom,
    [FromQuery] DateTimeOffset? deletedTo,
    [FromQuery] int? page,
    [FromQuery] int? pageSize,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<PagedResult<CalendarRecycleBinItem>>.Ok(
        await svc.ListAsync(type, search, deletedFrom, deletedTo, page ?? 1, pageSize ?? 50, ct))));

group.MapPost("/recycle-bin/{type}/{id:guid}/restore-preview", async (
    string type,
    Guid id,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarRestorePreviewResponse>.Ok(await svc.PreviewRestoreAsync(type, id, ct))));

group.MapPost("/recycle-bin/{type}/{id:guid}/restore", async (
    string type,
    Guid id,
    [FromBody] CalendarRestoreRequest request,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.RestoreAsync(type, id, request, ct))));
```

Also add convenience restore endpoints:

```csharp
group.MapPost("/calendars/{id:guid}/restore", async (
    Guid id,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.RestoreAsync("calendar", id, new CalendarRestoreRequest(), ct))));

group.MapPost("/events/{id:guid}/restore", async (
    Guid id,
    [FromBody] CalendarRestoreRequest request,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.RestoreAsync("event", id, request, ct))));

group.MapPost("/tasks/{id:guid}/restore", async (
    Guid id,
    [FromBody] CalendarRestoreRequest request,
    [FromServices] CalendarRecycleBinService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.RestoreAsync("task", id, request, ct))));
```

- [ ] **Step 5: Run recycle-bin tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarRecycleBinServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs
git commit -m "feat(calendar): add recycle bin restore flow"
```

---

### Task 5: Add Task Search, Pagination, Batch Update, And Planning API

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs`

- [ ] **Step 1: Write failing task planning tests**

Create `tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarTaskPlanningTests
{
    private static readonly Guid UserId = Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    [Fact]
    public async Task PlanTaskAsync_SetsPlannedRangeWithoutCreatingEvent()
    {
        await using var db = CreateDb();
        var task = new TaskEntity { UserId = UserId, Uid = "task@pim", Title = "Write plan", IsInbox = true };
        db.Set<TaskEntity>().Add(task);
        await db.SaveChangesAsync();
        var service = CreateCalendarService(db);

        var planned = await service.PlanTaskAsync(task.Id, new PlanTaskRequest(
            new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero),
            new DateTimeOffset(2026, 5, 26, 10, 30, 0, TimeSpan.Zero),
            "PT1H30M"));

        Assert.Equal(task.Id, planned.Id);
        Assert.False(planned.IsInbox);
        Assert.Equal(new DateTimeOffset(2026, 5, 26, 9, 0, 0, TimeSpan.Zero), planned.DtStart);
        Assert.Equal(new DateTimeOffset(2026, 5, 26, 10, 30, 0, TimeSpan.Zero), planned.PlannedEnd);
        Assert.Empty(await db.Set<EventEntity>().ToListAsync());
    }

    [Fact]
    public async Task GetTasksPagedAsync_FiltersSearchStatusAndPriority()
    {
        await using var db = CreateDb();
        db.Set<TaskEntity>().AddRange(
            new TaskEntity { UserId = UserId, Uid = "a@pim", Title = "Alpha deep work", Priority = 1, Status = "NEEDS-ACTION" },
            new TaskEntity { UserId = UserId, Uid = "b@pim", Title = "Beta admin", Priority = 3, Status = "COMPLETED" });
        await db.SaveChangesAsync();
        var service = CreateCalendarService(db);

        var result = await service.GetTasksPagedAsync(
            inbox: null,
            search: "Alpha",
            calendarId: null,
            status: "NEEDS-ACTION",
            priority: 1,
            plannedFrom: null,
            plannedTo: null,
            dueFrom: null,
            dueTo: null,
            page: 1,
            pageSize: 20);

        var item = Assert.Single(result.Items);
        Assert.Equal("Alpha deep work", item.Title);
        Assert.Equal(1, result.TotalCount);
    }

    [Fact]
    public async Task BatchUpdateTasksAsync_UpdatesStatusForRequestedTasksOnly()
    {
        await using var db = CreateDb();
        var a = new TaskEntity { UserId = UserId, Uid = "a@pim", Title = "A", Status = "NEEDS-ACTION" };
        var b = new TaskEntity { UserId = UserId, Uid = "b@pim", Title = "B", Status = "NEEDS-ACTION" };
        db.Set<TaskEntity>().AddRange(a, b);
        await db.SaveChangesAsync();
        var service = CreateCalendarService(db);

        var result = await service.BatchUpdateTasksAsync(new BatchTaskUpdateRequest(new[] { a.Id }, "COMPLETED", null, null));

        Assert.Equal(1, result.AffectedCount);
        Assert.Equal("COMPLETED", (await db.Set<TaskEntity>().SingleAsync(t => t.Id == a.Id)).Status);
        Assert.Equal("NEEDS-ACTION", (await db.Set<TaskEntity>().SingleAsync(t => t.Id == b.Id)).Status);
    }

    private static PimDbContext CreateDb()
    {
        PimDbContext.RegisterModuleAssembly(typeof(EventEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase($"calendar-task-planning-{Guid.NewGuid()}")
            .Options;
        return new PimDbContext(options);
    }

    private static CalendarService CreateCalendarService(PimDbContext db)
        => new(db, new FixedCurrentUserService(UserId), new RecurrenceService(NullLogger<RecurrenceService>.Instance));

    private sealed class FixedCurrentUserService(Guid userId) : ICurrentUserService
    {
        public Guid? UserId { get; } = userId;
        public string? Role => "user";
    }
}
```

- [ ] **Step 2: Run task planning tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarTaskPlanningTests
```

Expected: FAIL with missing `PlanTaskAsync`, `GetTasksPagedAsync`, and `BatchUpdateTasksAsync`.

- [ ] **Step 3: Add service methods**

Modify `CalendarService.cs` to add:

```csharp
public async Task<PagedResult<TaskResponse>> GetTasksPagedAsync(
    bool? inbox,
    string? search,
    Guid? calendarId,
    string? status,
    int? priority,
    DateTimeOffset? plannedFrom,
    DateTimeOffset? plannedTo,
    DateTimeOffset? dueFrom,
    DateTimeOffset? dueTo,
    int page = 1,
    int pageSize = 50,
    CancellationToken ct = default)
{
    var query = _db.Set<TaskEntity>().Where(t => t.UserId == UserId);

    if (inbox.HasValue) query = query.Where(t => t.IsInbox == inbox.Value);
    if (!string.IsNullOrWhiteSpace(search)) query = query.Where(t => t.Title.Contains(search.Trim()));
    if (calendarId.HasValue) query = query.Where(t => t.CalendarId == calendarId.Value);
    if (!string.IsNullOrWhiteSpace(status)) query = query.Where(t => t.Status == status.Trim());
    if (priority.HasValue) query = query.Where(t => t.Priority == priority.Value);
    if (plannedFrom.HasValue) query = query.Where(t => t.DtStart >= plannedFrom.Value);
    if (plannedTo.HasValue) query = query.Where(t => t.DtStart < plannedTo.Value);
    if (dueFrom.HasValue) query = query.Where(t => t.Due >= dueFrom.Value);
    if (dueTo.HasValue) query = query.Where(t => t.Due < dueTo.Value);

    var safePage = Math.Max(1, page);
    var safePageSize = Math.Clamp(pageSize, 1, 100);
    var totalCount = await query.CountAsync(ct);
    var totalPages = (int)Math.Ceiling(totalCount / (double)safePageSize);
    var tasks = await query
        .OrderBy(t => t.Status == "COMPLETED")
        .ThenBy(t => t.Due ?? DateTimeOffset.MaxValue)
        .ThenBy(t => t.SortOrder)
        .Skip((safePage - 1) * safePageSize)
        .Take(safePageSize)
        .ToListAsync(ct);

    return new PagedResult<TaskResponse>(tasks.Select(MapTask).ToList(), safePage, safePageSize, totalCount, totalPages);
}

public async Task<TaskResponse> PlanTaskAsync(Guid id, PlanTaskRequest request, CancellationToken ct = default)
{
    var task = await _db.Set<TaskEntity>()
        .FirstOrDefaultAsync(t => t.Id == id && t.UserId == UserId, ct)
        ?? throw new DomainException(02004, "Task not found");

    task.DtStart = request.PlannedStart;
    task.PlannedEnd = request.PlannedEnd;
    task.EstimatedDuration = ParseDuration(request.EstimatedDuration);
    task.IsInbox = false;
    task.UpdatedAt = DateTimeOffset.UtcNow;

    await _db.SaveChangesAsync(ct);
    return MapTask(task);
}

public async Task<CalendarOperationResult> BatchUpdateTasksAsync(BatchTaskUpdateRequest request, CancellationToken ct = default)
{
    var idSet = request.Ids.Distinct().ToHashSet();
    var tasks = await _db.Set<TaskEntity>()
        .Where(t => idSet.Contains(t.Id) && t.UserId == UserId)
        .ToListAsync(ct);

    foreach (var task in tasks)
    {
        if (request.Status is not null)
        {
            task.Status = request.Status;
            task.CompletedAt = request.Status == "COMPLETED" ? DateTimeOffset.UtcNow : null;
        }
        if (request.Priority.HasValue)
            task.Priority = request.Priority.Value;
        if (request.CalendarId.HasValue)
        {
            task.CalendarId = request.CalendarId;
            task.IsInbox = false;
        }
        task.UpdatedAt = DateTimeOffset.UtcNow;
    }

    await _db.SaveChangesAsync(ct);
    var operationId = Guid.NewGuid();
    var samples = tasks.Take(5).Select(t => new CalendarOperationSample(t.Id, "task", t.Title, t.DtStart, t.PlannedEnd, t.Calendar?.Name)).ToList();
    return new CalendarOperationResult("calendar.tasks.batch_update", operationId, tasks.Count, tasks.Select(t => t.Id).ToList(), samples, "Updated tasks");
}
```

- [ ] **Step 4: Map task query and planning endpoints**

Modify task endpoints in `CalendarModule.cs`:

```csharp
group.MapGet("/tasks", async (
    [FromQuery] bool? inbox,
    [FromQuery] string? search,
    [FromQuery] Guid? calendarId,
    [FromQuery] string? status,
    [FromQuery] int? priority,
    [FromQuery] DateTimeOffset? plannedFrom,
    [FromQuery] DateTimeOffset? plannedTo,
    [FromQuery] DateTimeOffset? dueFrom,
    [FromQuery] DateTimeOffset? dueTo,
    [FromQuery] int? page,
    [FromQuery] int? pageSize,
    [FromServices] CalendarService svc,
    CancellationToken ct) =>
{
    if (search is null && calendarId is null && status is null && priority is null &&
        plannedFrom is null && plannedTo is null && dueFrom is null && dueTo is null &&
        page is null && pageSize is null)
    {
        return Results.Ok(ApiResponse<List<TaskResponse>>.Ok(await svc.GetTasksAsync(inbox, ct)));
    }

    var result = await svc.GetTasksPagedAsync(inbox, search, calendarId, status, priority, plannedFrom, plannedTo, dueFrom, dueTo, page ?? 1, pageSize ?? 50, ct);
    return Results.Ok(ApiResponse<PagedResult<TaskResponse>>.Ok(result));
});

group.MapPost("/tasks/{id:guid}/plan", async (
    Guid id,
    [FromBody] PlanTaskRequest req,
    [FromServices] CalendarService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<TaskResponse>.Ok(await svc.PlanTaskAsync(id, req, ct))));

group.MapPost("/tasks/batch-update", async (
    [FromBody] BatchTaskUpdateRequest req,
    [FromServices] CalendarService svc,
    CancellationToken ct) =>
    Results.Ok(ApiResponse<CalendarOperationResult>.Ok(await svc.BatchUpdateTasksAsync(req, ct))));
```

- [ ] **Step 5: Run task planning tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarTaskPlanningTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/CalendarTaskPlanningTests.cs
git commit -m "feat(calendar): add task planning api"
```

---

### Task 6: Implement Outlook-Compatible ICS Import Report And Metadata Preservation

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/IcsService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs`

- [ ] **Step 1: Write failing Outlook ICS tests**

Create `tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs`:

```csharp
using Pim.Module.Calendar.Services;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class OutlookIcsServiceTests
{
    [Fact]
    public void ImportOutlookIcs_ParsesAllDayEvent()
    {
        var service = new OutlookIcsService();
        var ics = """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN
        BEGIN:VEVENT
        UID:all-day@example.com
        SUMMARY:All Day
        DTSTART;VALUE=DATE:20260526
        DTEND;VALUE=DATE:20260527
        END:VEVENT
        END:VCALENDAR
        """;

        var parsed = service.Parse(ics);

        var item = Assert.Single(parsed.Events);
        Assert.True(item.IsAllDay);
        Assert.Equal("all-day@example.com", item.Uid);
        Assert.Equal("All Day", item.Title);
    }

    [Fact]
    public void ImportOutlookIcs_PreservesMeetingMetadataAndRawComponent()
    {
        var service = new OutlookIcsService();
        var ics = """
        BEGIN:VCALENDAR
        VERSION:2.0
        METHOD:PUBLISH
        PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN
        BEGIN:VEVENT
        UID:meeting@example.com
        SUMMARY:Project Meeting
        DTSTART;TZID=China Standard Time:20260526T090000
        DTEND;TZID=China Standard Time:20260526T100000
        ORGANIZER;CN=Owner:mailto:owner@example.com
        ATTENDEE;CN=Guest;RSVP=TRUE:mailto:guest@example.com
        SEQUENCE:3
        X-MICROSOFT-CDO-BUSYSTATUS:BUSY
        X-ALT-DESC;FMTTYPE=text/html:<html><body>Meeting</body></html>
        END:VEVENT
        END:VCALENDAR
        """;

        var parsed = service.Parse(ics);

        var item = Assert.Single(parsed.Events);
        Assert.Equal("China Standard Time", item.SourceTimeZoneId);
        Assert.Contains("BEGIN:VEVENT", item.SourceIcsComponent);
        Assert.Contains("owner@example.com", item.ExternalMetadataJson);
        Assert.Contains("guest@example.com", item.ExternalMetadataJson);
        Assert.Contains("X-MICROSOFT-CDO-BUSYSTATUS", item.ExternalMetadataJson);
        Assert.Contains("htmlDescription", item.ExternalMetadataJson);
    }

    [Fact]
    public void ImportOutlookIcs_PreservesRecurrenceFields()
    {
        var service = new OutlookIcsService();
        var ics = """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:series@example.com
        SUMMARY:Weekly Review
        DTSTART:20260526T090000Z
        DTEND:20260526T100000Z
        RRULE:FREQ=WEEKLY;COUNT=4
        EXDATE:20260602T090000Z
        RECURRENCE-ID:20260609T090000Z
        END:VEVENT
        END:VCALENDAR
        """;

        var parsed = service.Parse(ics);

        var item = Assert.Single(parsed.Events);
        Assert.Equal("FREQ=WEEKLY;COUNT=4", item.RRule);
        Assert.Contains("20260602T090000Z", item.ExDatesJson);
        Assert.Equal("20260609T090000Z", item.RecurrenceId);
        Assert.Contains("RECURRENCE-ID", item.RecurrenceMetadataJson);
    }
}
```

- [ ] **Step 2: Run Outlook ICS tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookIcsServiceTests
```

Expected: FAIL with missing `OutlookIcsService`.

- [ ] **Step 3: Create parsed Outlook records**

Create `src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs`:

```csharp
using System.Text.Json;
using Ical.Net;

namespace Pim.Module.Calendar.Services;

public sealed class OutlookIcsService
{
    public OutlookIcsParseResult Parse(string icsContent)
    {
        if (string.IsNullOrWhiteSpace(icsContent))
            return new OutlookIcsParseResult(Array.Empty<OutlookIcsParsedEvent>());

        var calendar = Calendar.Load(icsContent);
        var rawComponents = ExtractRawEventComponents(icsContent);
        var events = calendar.Events.Select((e, index) =>
        {
            var raw = index < rawComponents.Count ? rawComponents[index] : string.Empty;
            var start = e.Start?.AsUtc;
            var end = e.End?.AsUtc;
            var isAllDay = e.Start?.HasDate == true && !e.Start.HasTime;
            var sourceTimeZone = e.Start?.TzId ?? e.Properties.FirstOrDefault(p => p.Name == "DTSTART")?.Parameters.FirstOrDefault(p => p.Name == "TZID")?.Value;
            var metadata = BuildMetadata(calendar.Method, e.Properties);
            var exdates = e.ExceptionDates.SelectMany(periods => periods.Select(p => p.StartTime.ToString())).ToList();
            var recurrenceId = e.RecurrenceId?.ToString();
            var recurrenceMetadata = new Dictionary<string, object?>
            {
                ["recurrenceId"] = recurrenceId,
                ["exDates"] = exdates
            };

            return new OutlookIcsParsedEvent(
                e.Uid ?? Guid.NewGuid().ToString(),
                e.Summary ?? "Untitled",
                e.Description,
                e.Location,
                start is not null ? new DateTimeOffset(start.Value, TimeSpan.Zero) : DateTimeOffset.MinValue,
                end is not null ? new DateTimeOffset(end.Value, TimeSpan.Zero) : DateTimeOffset.MinValue,
                e.RecurrenceRules.FirstOrDefault()?.ToString(),
                isAllDay,
                sourceTimeZone,
                raw,
                JsonSerializer.Serialize(metadata),
                recurrenceId,
                JsonSerializer.Serialize(exdates),
                JsonSerializer.Serialize(recurrenceMetadata));
        }).ToList();

        return new OutlookIcsParseResult(events);
    }

    private static Dictionary<string, object?> BuildMetadata(string? method, IEnumerable<Ical.Net.CalendarProperty> properties)
    {
        var metadata = new Dictionary<string, object?>();
        if (!string.IsNullOrWhiteSpace(method))
            metadata["method"] = method;

        foreach (var property in properties)
        {
            if (property.Name is "ORGANIZER" or "ATTENDEE" or "SEQUENCE" or "CLASS" or "TRANSP" or "PRIORITY" or "CATEGORIES")
                metadata[property.Name.ToLowerInvariant()] = property.Value?.ToString();
            if (property.Name.StartsWith("X-MICROSOFT", StringComparison.OrdinalIgnoreCase) || property.Name.StartsWith("X-MS-OLK", StringComparison.OrdinalIgnoreCase))
                metadata[property.Name] = property.Value?.ToString();
            if (property.Name == "X-ALT-DESC")
                metadata["htmlDescription"] = property.Value?.ToString();
        }

        return metadata;
    }

    private static List<string> ExtractRawEventComponents(string icsContent)
    {
        var normalized = icsContent.Replace("\r\n", "\n").Replace("\r", "\n");
        var components = new List<string>();
        var start = 0;
        while (true)
        {
            var begin = normalized.IndexOf("BEGIN:VEVENT", start, StringComparison.OrdinalIgnoreCase);
            if (begin < 0) break;
            var end = normalized.IndexOf("END:VEVENT", begin, StringComparison.OrdinalIgnoreCase);
            if (end < 0) break;
            end += "END:VEVENT".Length;
            components.Add(normalized[begin..end].Replace("\n", "\r\n"));
            start = end;
        }
        return components;
    }
}

public sealed record OutlookIcsParseResult(IReadOnlyList<OutlookIcsParsedEvent> Events);

public sealed record OutlookIcsParsedEvent(
    string Uid,
    string Title,
    string? Description,
    string? Location,
    DateTimeOffset Start,
    DateTimeOffset End,
    string? RRule,
    bool IsAllDay,
    string? SourceTimeZoneId,
    string SourceIcsComponent,
    string ExternalMetadataJson,
    string? RecurrenceId,
    string ExDatesJson,
    string RecurrenceMetadataJson
);
```

- [ ] **Step 4: Register Outlook ICS service**

Modify `CalendarModule.RegisterServices`:

```csharp
services.AddScoped<OutlookIcsService>();
```

- [ ] **Step 5: Add import method to CalendarService**

Add to `CalendarService.cs`:

```csharp
public async Task<ImportReport> ImportOutlookIcsAsync(
    string icsContent,
    Guid? targetCalendarId,
    OutlookIcsService outlookIcs,
    CancellationToken ct = default)
{
    var parsed = outlookIcs.Parse(icsContent);
    var calendar = targetCalendarId.HasValue
        ? await _db.Set<CalendarEntity>().FirstOrDefaultAsync(c => c.Id == targetCalendarId.Value && c.UserId == UserId, ct)
            ?? throw new DomainException(02003, "Calendar not found")
        : await GetOrCreateDefaultCalendarAsync("calendar", ct);

    var imported = 0;
    var skipped = new List<ImportSkippedItem>();
    var reasonCounts = new Dictionary<string, int>();

    foreach (var item in parsed.Events)
    {
        var reason = await FindActiveDuplicateReasonAsync(item, ct);
        if (reason is not null)
        {
            reasonCounts[reason] = reasonCounts.GetValueOrDefault(reason) + 1;
            if (skipped.Count < 10)
                skipped.Add(new ImportSkippedItem(reason, item.Title, item.Start, item.Uid));
            continue;
        }

        _db.Set<EventEntity>().Add(new EventEntity
        {
            CalendarId = calendar.Id,
            Uid = item.Uid,
            SourceUid = item.Uid,
            Title = item.Title,
            Description = item.Description,
            Location = item.Location,
            DtStart = item.Start,
            DtEnd = item.End,
            RRule = item.RRule,
            IsAllDay = item.IsAllDay,
            Source = "outlook-ics",
            SourceTimeZoneId = item.SourceTimeZoneId,
            SourceIcsComponent = item.SourceIcsComponent,
            ExternalMetadataJson = item.ExternalMetadataJson,
            RecurrenceId = item.RecurrenceId,
            ExDatesJson = item.ExDatesJson,
            RecurrenceMetadataJson = item.RecurrenceMetadataJson
        });
        imported++;
    }

    await _db.SaveChangesAsync(ct);
    return new ImportReport(imported, skipped.Count, reasonCounts, skipped);
}

private async Task<string?> FindActiveDuplicateReasonAsync(OutlookIcsParsedEvent item, CancellationToken ct)
{
    if (await _db.Set<EventEntity>().AnyAsync(e => e.Calendar.UserId == UserId && e.Uid == item.Uid, ct))
        return "same-uid";
    if (await _db.Set<EventEntity>().AnyAsync(e => e.Calendar.UserId == UserId && e.SourceUid == item.Uid, ct))
        return "same-source-uid";
    if (await _db.Set<EventEntity>().AnyAsync(e => e.Calendar.UserId == UserId && e.Title == item.Title && e.DtStart == item.Start && e.DtEnd == item.End, ct))
        return "same-title-time";
    return null;
}
```

- [ ] **Step 6: Replace import endpoint implementation**

Modify `/import-ics` endpoint in `CalendarModule.cs` to call `ImportOutlookIcsAsync` and return `ImportReport`:

```csharp
var report = await calendarService.ImportOutlookIcsAsync(icsContent, targetCalendarId, outlookIcsService, ct);
return Results.Ok(ApiResponse<ImportReport>.Ok(report));
```

Inject `[FromServices] OutlookIcsService outlookIcsService` into the endpoint parameters.

- [ ] **Step 7: Run Outlook ICS tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookIcsServiceTests
```

Expected: PASS.

- [ ] **Step 8: Run legacy ICS tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~IcsServiceTests
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs
git commit -m "feat(calendar): import outlook ics reports"
```

---

### Task 7: Add Calendar Endpoint Path Tests

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs`

- [ ] **Step 1: Write endpoint path tests**

Create `tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs`:

```csharp
using Pim.Module.Calendar;
using Xunit;

namespace Pim.UnitTests.Calendar;

public class CalendarEndpointPathTests
{
    [Fact]
    public void CalendarEndpointPaths_AreStable()
    {
        Assert.Equal("/api/v1/calendar", CalendarEndpointPaths.Root);
        Assert.Equal("/api/v1/calendar/recycle-bin", CalendarEndpointPaths.RecycleBin);
        Assert.Equal("/api/v1/calendar/events/batch-delete", CalendarEndpointPaths.EventBatchDelete);
        Assert.Equal("/api/v1/calendar/tasks/batch-update", CalendarEndpointPaths.TaskBatchUpdate);
        Assert.Equal("/api/v1/calendar/tasks/batch-delete", CalendarEndpointPaths.TaskBatchDelete);
        Assert.Equal("/api/v1/calendar/import-ics", CalendarEndpointPaths.ImportIcs);
        Assert.Equal("/api/v1/calendar/export-ics", CalendarEndpointPaths.ExportIcs);
        Assert.Equal("/api/v1/calendar/tasks/abc/plan", CalendarEndpointPaths.TaskPlan("abc"));
        Assert.Equal("/api/v1/calendar/recycle-bin/event/abc/restore-preview", CalendarEndpointPaths.RecycleRestorePreview("event", "abc"));
        Assert.Equal("/api/v1/calendar/recycle-bin/event/abc/restore", CalendarEndpointPaths.RecycleRestore("event", "abc"));
    }
}
```

- [ ] **Step 2: Run endpoint path tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarEndpointPathTests
```

Expected: FAIL with missing `CalendarEndpointPaths`.

- [ ] **Step 3: Add endpoint path constants**

Append to `src/modules/Pim.Module.Calendar/CalendarModule.cs`:

```csharp
public static class CalendarEndpointPaths
{
    public const string Root = "/api/v1/calendar";
    public const string RecycleBin = "/api/v1/calendar/recycle-bin";
    public const string EventBatchDelete = "/api/v1/calendar/events/batch-delete";
    public const string TaskBatchUpdate = "/api/v1/calendar/tasks/batch-update";
    public const string TaskBatchDelete = "/api/v1/calendar/tasks/batch-delete";
    public const string ImportIcs = "/api/v1/calendar/import-ics";
    public const string ExportIcs = "/api/v1/calendar/export-ics";

    public static string TaskPlan(string id) => $"{Root}/tasks/{id}/plan";
    public static string RecycleRestorePreview(string type, string id) => $"{RecycleBin}/{type}/{id}/restore-preview";
    public static string RecycleRestore(string type, string id) => $"{RecycleBin}/{type}/{id}/restore";
}
```

Change `MapEndpoints` group creation to use `CalendarEndpointPaths.Root`:

```csharp
var group = endpoints.MapGroup(CalendarEndpointPaths.Root)
    .RequireAuthorization();
```

- [ ] **Step 4: Run endpoint path tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarEndpointPathTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/CalendarModule.cs tests/Pim.UnitTests/Calendar/CalendarEndpointPathTests.cs
git commit -m "test(calendar): lock stage 5 endpoint paths"
```

---

### Task 8: Add Frontend Stage 5 Types And Calendar API Client

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`
- Create: `tests/client-web/recycleBinApiPath.test.ts`
- Modify: `tests/client-web/calendarApiPath.test.ts`
- Create: `tests/client-web/calendarStage5Types.test.ts`
- Create: `tests/client-web/tsconfig.calendar-stage5.json`

- [ ] **Step 1: Write frontend API path tests**

Create `tests/client-web/recycleBinApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { calendarApiPaths } from '../../src/client-web/src/api/calendar';

assert.equal(calendarApiPaths.recycleBin(), '/calendar/recycle-bin');
assert.equal(
  calendarApiPaths.recycleBin({ type: 'event', search: 'plan', page: 2, pageSize: 20 }),
  '/calendar/recycle-bin?type=event&search=plan&page=2&pageSize=20'
);
assert.equal(
  calendarApiPaths.recycleRestorePreview('event', 'abc'),
  '/calendar/recycle-bin/event/abc/restore-preview'
);
assert.equal(
  calendarApiPaths.recycleRestore('event', 'abc'),
  '/calendar/recycle-bin/event/abc/restore'
);
assert.equal(calendarApiPaths.taskPlan('abc'), '/calendar/tasks/abc/plan');
assert.equal(calendarApiPaths.taskBatchUpdate(), '/calendar/tasks/batch-update');
assert.equal(calendarApiPaths.taskBatchDelete(), '/calendar/tasks/batch-delete');
```

Modify `tests/client-web/calendarApiPath.test.ts` to add:

```ts
import { calendarApiPaths } from '../../src/client-web/src/api/calendar';

assert.equal(calendarApiPaths.calendarDeletePreview('abc'), '/calendar/calendars/abc/delete-preview');
assert.equal(calendarApiPaths.eventBatchDelete(), '/calendar/events/batch-delete');
```

Create `tests/client-web/calendarStage5Types.test.ts`:

```ts
import type {
  CalendarOperationResult,
  CalendarRecycleBinItem,
  CalendarRestorePreviewResponse,
  EventResponse,
  ImportReport,
  TaskResponse,
} from '../../src/client-web/src/types';

const event: EventResponse = {
  id: 'event-id',
  calendarId: 'calendar-id',
  uid: 'uid',
  title: 'Event',
  dtStart: '2026-05-26T09:00:00Z',
  dtEnd: '2026-05-26T10:00:00Z',
  status: 'CONFIRMED',
  source: 'outlook-ics',
  isAllDay: false,
  timeZoneId: 'Asia/Shanghai',
  sourceTimeZoneId: 'China Standard Time',
  sourceUid: 'uid',
  externalMetadataJson: '{}',
  exDatesJson: '[]',
  recurrenceMetadataJson: '{}',
};

const task: TaskResponse = {
  id: 'task-id',
  title: 'Task',
  priority: 1,
  status: 'NEEDS-ACTION',
  isInbox: false,
  plannedEnd: '2026-05-26T10:00:00Z',
};

const recycleItem: CalendarRecycleBinItem = {
  id: 'deleted-id',
  type: 'event',
  title: 'Deleted',
  deletedAt: '2026-05-26T10:00:00Z',
  source: 'manual',
};

const restorePreview: CalendarRestorePreviewResponse = {
  targetType: 'event',
  targetId: 'deleted-id',
  title: 'Deleted',
  restoreCount: 1,
  samples: [],
  conflicts: [],
  canRestoreWithoutConflict: true,
};

const operation: CalendarOperationResult = {
  operation: 'calendar.events.delete',
  operationId: 'operation-id',
  affectedCount: 1,
  affectedIds: ['deleted-id'],
  samples: [],
  message: 'Moved to recycle bin',
};

const report: ImportReport = {
  imported: 1,
  skipped: 1,
  skippedReasons: { 'same-uid': 1 },
  samples: [{ reason: 'same-uid', title: 'Existing', start: '2026-05-26T09:00:00Z', uid: 'uid' }],
};

void event;
void task;
void recycleItem;
void restorePreview;
void operation;
void report;
```

Create `tests/client-web/tsconfig.calendar-stage5.json`:

```json
{
  "extends": "../../src/client-web/tsconfig.json",
  "compilerOptions": {
    "noEmit": true,
    "types": ["node"]
  },
  "include": [
    "./calendarStage5Types.test.ts"
  ]
}
```

- [ ] **Step 2: Run frontend tests and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\recycleBinApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.calendar-stage5.json
```

Expected: FAIL with missing `calendarApiPaths` and missing Stage 5 types.

- [ ] **Step 3: Add frontend types**

Modify `src/client-web/src/types/index.ts`:

```ts
export interface EventResponse {
  id: string;
  calendarId: string;
  uid: string;
  title: string;
  description?: string;
  location?: string;
  dtStart: string;
  dtEnd: string;
  rrule?: string;
  status: string;
  source: string;
  originalEventId?: string;
  isAllDay?: boolean;
  timeZoneId?: string;
  sourceTimeZoneId?: string;
  sourceUid?: string;
  externalMetadataJson?: string;
  recurrenceId?: string;
  exDatesJson?: string;
  recurrenceMetadataJson?: string;
}

export interface TaskResponse {
  id: string;
  calendarId?: string;
  title: string;
  description?: string;
  priority: number;
  estimatedDuration?: string;
  minimumSegment?: string;
  dtStart?: string;
  due?: string;
  status: string;
  isInbox: boolean;
  plannedEnd?: string;
}

export interface CalendarOperationSample {
  id: string;
  type: string;
  title: string;
  start?: string | null;
  end?: string | null;
  bookName?: string | null;
}

export interface CalendarDeletePreviewResponse {
  targetType: string;
  targetId: string;
  title: string;
  operationKind: string;
  affectedCount: number;
  samples: CalendarOperationSample[];
  summary: string;
  requiresStrictConfirmation: boolean;
}

export interface CalendarOperationResult {
  operation: string;
  operationId: string;
  affectedCount: number;
  affectedIds: string[];
  samples: CalendarOperationSample[];
  message: string;
}

export interface CalendarRestoreConflict {
  deletedId: string;
  deletedType: string;
  activeId: string;
  activeType: string;
  reason: string;
  title: string;
}

export interface CalendarRestorePreviewResponse {
  targetType: string;
  targetId: string;
  title: string;
  restoreCount: number;
  samples: CalendarOperationSample[];
  conflicts: CalendarRestoreConflict[];
  canRestoreWithoutConflict: boolean;
}

export interface CalendarRecycleBinItem {
  id: string;
  type: 'calendar' | 'task-book' | 'event' | 'task' | string;
  title: string;
  deletedAt: string;
  bookName?: string | null;
  start?: string | null;
  end?: string | null;
  source: string;
  deletedByOperationId?: string | null;
  deletedByOperationKind?: string | null;
}

export interface ImportSkippedItem {
  reason: string;
  title: string;
  start?: string | null;
  uid?: string | null;
}

export interface ImportReport {
  imported: number;
  skipped: number;
  skippedReasons: Record<string, number>;
  samples: ImportSkippedItem[];
}
```

- [ ] **Step 4: Add API path helpers and methods**

Modify `src/client-web/src/api/calendar.ts`:

```ts
export const calendarApiPaths = {
  recycleBin(params: { type?: string; search?: string; page?: number; pageSize?: number } = {}) {
    const searchParams = new URLSearchParams();
    if (params.type) searchParams.set('type', params.type);
    if (params.search) searchParams.set('search', params.search);
    if (params.page) searchParams.set('page', String(params.page));
    if (params.pageSize) searchParams.set('pageSize', String(params.pageSize));
    const qs = searchParams.toString();
    return `/calendar/recycle-bin${qs ? `?${qs}` : ''}`;
  },
  recycleRestorePreview(type: string, id: string) {
    return `/calendar/recycle-bin/${type}/${id}/restore-preview`;
  },
  recycleRestore(type: string, id: string) {
    return `/calendar/recycle-bin/${type}/${id}/restore`;
  },
  calendarDeletePreview(id: string) {
    return `/calendar/calendars/${id}/delete-preview`;
  },
  eventBatchDelete() {
    return '/calendar/events/batch-delete';
  },
  taskPlan(id: string) {
    return `/calendar/tasks/${id}/plan`;
  },
  taskBatchUpdate() {
    return '/calendar/tasks/batch-update';
  },
  taskBatchDelete() {
    return '/calendar/tasks/batch-delete';
  },
};
```

Add imports for new types and methods:

```ts
import type {
  CalendarDeletePreviewResponse,
  CalendarOperationResult,
  CalendarRecycleBinItem,
  CalendarRestorePreviewResponse,
  ImportReport,
} from '../types';
```

Add API methods:

```ts
export async function getRecycleBin(params: { type?: string; search?: string; page?: number; pageSize?: number } = {}) {
  const r = await apiGet<ApiResponse<PagedResult<CalendarRecycleBinItem>>>(calendarApiPaths.recycleBin(params));
  return r.data;
}

export async function previewRecycleRestore(type: string, id: string) {
  const r = await apiPost<ApiResponse<CalendarRestorePreviewResponse>>(calendarApiPaths.recycleRestorePreview(type, id));
  return r.data;
}

export async function restoreRecycleItem(type: string, id: string, restoreAsCopy = false) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(calendarApiPaths.recycleRestore(type, id), { restoreAsCopy });
  return r.data;
}

export async function previewCalendarDelete(id: string) {
  const r = await apiPost<ApiResponse<CalendarDeletePreviewResponse>>(calendarApiPaths.calendarDeletePreview(id));
  return r.data;
}

export async function planTask(id: string, data: { plannedStart: string; plannedEnd?: string; estimatedDuration?: string }) {
  const r = await apiPost<ApiResponse<TaskResponse>>(calendarApiPaths.taskPlan(id), data);
  return r.data;
}

export async function batchDeleteTasks(ids: string[]) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(calendarApiPaths.taskBatchDelete(), { ids });
  return r.data;
}

export async function batchUpdateTasks(data: { ids: string[]; status?: string; priority?: number; calendarId?: string }) {
  const r = await apiPost<ApiResponse<CalendarOperationResult>>(calendarApiPaths.taskBatchUpdate(), data);
  return r.data;
}
```

Update `batchDeleteEvents` return type to `CalendarOperationResult`.

Update `importIcs` to parse `ApiResponse<ImportReport>`.

- [ ] **Step 5: Run frontend path and type tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\calendarApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests\client-web\recycleBinApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.calendar-stage5.json
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/calendar.ts tests/client-web/calendarApiPath.test.ts tests/client-web/recycleBinApiPath.test.ts tests/client-web/calendarStage5Types.test.ts tests/client-web/tsconfig.calendar-stage5.json
git commit -m "feat(web): add calendar stage 5 api types"
```

---

### Task 9: Add Strict Confirmation Dialog And Operation Result Banner

**Files:**
- Create: `src/client-web/src/ui/ConfirmActionDialog.tsx`
- Create: `src/client-web/src/ui/OperationResultBanner.tsx`
- Create: `tests/client-web/confirmActionDialogModel.test.ts`

- [ ] **Step 1: Write confirmation model tests**

Create `tests/client-web/confirmActionDialogModel.test.ts`:

```ts
import assert from 'node:assert/strict';
import { buildDeleteConfirmationCopy } from '../../src/client-web/src/ui/ConfirmActionDialog';

assert.deepEqual(
  buildDeleteConfirmationCopy({
    targetType: 'event',
    title: 'Focus block',
    affectedCount: 1,
    samples: [],
  }),
  {
    title: '删除日程',
    description: 'Focus block 将移动到回收站，可以在设置中恢复。',
    confirmLabel: '移动到回收站',
  },
);

assert.deepEqual(
  buildDeleteConfirmationCopy({
    targetType: 'calendar',
    title: '工作日历',
    affectedCount: 12,
    samples: [{ id: 'a', type: 'event', title: 'Standup' }],
  }),
  {
    title: '删除日历本',
    description: '工作日历 和 12 个关联项目将一起移动到回收站。',
    confirmLabel: '确认移动 12 项',
  },
);
```

- [ ] **Step 2: Run confirmation tests and verify failure**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\confirmActionDialogModel.test.ts
```

Expected: FAIL with missing `ConfirmActionDialog`.

- [ ] **Step 3: Create strict confirmation dialog**

Create `src/client-web/src/ui/ConfirmActionDialog.tsx`:

```tsx
import type { CalendarOperationSample } from '../types';

export interface DeleteConfirmationInput {
  targetType: string;
  title: string;
  affectedCount: number;
  samples: CalendarOperationSample[];
}

export function buildDeleteConfirmationCopy(input: DeleteConfirmationInput) {
  const typeLabel = input.targetType === 'calendar'
    ? '日历本'
    : input.targetType === 'task-book'
      ? '任务本'
      : input.targetType === 'task'
        ? '任务'
        : '日程';

  if (input.affectedCount <= 1) {
    return {
      title: `删除${typeLabel}`,
      description: `${input.title} 将移动到回收站，可以在设置中恢复。`,
      confirmLabel: '移动到回收站',
    };
  }

  return {
    title: `删除${typeLabel}`,
    description: `${input.title} 和 ${input.affectedCount} 个关联项目将一起移动到回收站。`,
    confirmLabel: `确认移动 ${input.affectedCount} 项`,
  };
}

export default function ConfirmActionDialog({
  open,
  input,
  isPending,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  input: DeleteConfirmationInput | null;
  isPending?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  if (!open || !input) return null;
  const copy = buildDeleteConfirmationCopy(input);

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/30 px-4">
      <div className="w-full max-w-lg rounded-lg border border-slate-200 bg-white shadow-2xl">
        <header className="border-b border-slate-200 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-950">{copy.title}</h2>
          <p className="mt-1 text-sm text-slate-500">{copy.description}</p>
        </header>
        {input.samples.length > 0 && (
          <div className="max-h-56 overflow-auto px-5 py-3">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">影响样例</p>
            <ul className="space-y-2">
              {input.samples.map(sample => (
                <li key={`${sample.type}-${sample.id}`} className="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700">
                  {sample.title}
                </li>
              ))}
            </ul>
          </div>
        )}
        <footer className="flex items-center justify-end gap-2 border-t border-slate-200 px-5 py-4">
          <button type="button" onClick={onCancel} className="pim-button-secondary px-4 py-2 text-sm">
            取消
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
          >
            {isPending ? '处理中...' : copy.confirmLabel}
          </button>
        </footer>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Create operation result banner**

Create `src/client-web/src/ui/OperationResultBanner.tsx`:

```tsx
import type { CalendarOperationResult, ImportReport } from '../types';

export function OperationResultBanner({
  result,
  onDismiss,
}: {
  result: CalendarOperationResult | ImportReport | null;
  onDismiss: () => void;
}) {
  if (!result) return null;

  const isImport = 'imported' in result;
  const title = isImport
    ? `导入 ${result.imported} 条，跳过 ${result.skipped} 条`
    : `${result.message}，影响 ${result.affectedCount} 项`;

  return (
    <section className="rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-800" role="status">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-medium">{title}</p>
          {isImport && Object.keys(result.skippedReasons).length > 0 && (
            <p className="mt-1 text-xs text-blue-700">
              跳过原因：{Object.entries(result.skippedReasons).map(([key, count]) => `${key} ${count}`).join('，')}
            </p>
          )}
        </div>
        <button type="button" onClick={onDismiss} className="text-xs font-medium text-blue-700 hover:text-blue-900">
          关闭
        </button>
      </div>
    </section>
  );
}

export default OperationResultBanner;
```

- [ ] **Step 5: Run confirmation model tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\confirmActionDialogModel.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/ui/ConfirmActionDialog.tsx src/client-web/src/ui/OperationResultBanner.tsx tests/client-web/confirmActionDialogModel.test.ts
git commit -m "feat(web): add strict operation confirmation"
```

---

### Task 10: Add Recycle Bin Page And Settings Route

**Files:**
- Create: `src/client-web/src/pages/RecycleBinPage.tsx`
- Modify: `src/client-web/src/pages/SettingsPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: Create recycle bin page**

Create `src/client-web/src/pages/RecycleBinPage.tsx`:

```tsx
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getRecycleBin, previewRecycleRestore, restoreRecycleItem } from '../api/calendar';
import PageHeader from '../ui/PageHeader';
import EmptyState from '../ui/EmptyState';
import OperationResultBanner from '../ui/OperationResultBanner';
import type { CalendarOperationResult, CalendarRecycleBinItem, CalendarRestorePreviewResponse } from '../types';

const typeOptions = [
  { value: 'all', label: '全部' },
  { value: 'event', label: '日程' },
  { value: 'task', label: '任务' },
  { value: 'calendar', label: '日历本' },
  { value: 'task-book', label: '任务本' },
] as const;

export default function RecycleBinPage() {
  const queryClient = useQueryClient();
  const [type, setType] = useState('all');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<CalendarRecycleBinItem | null>(null);
  const [preview, setPreview] = useState<CalendarRestorePreviewResponse | null>(null);
  const [result, setResult] = useState<CalendarOperationResult | null>(null);

  const listQuery = useQuery({
    queryKey: ['calendar-recycle-bin', type, search],
    queryFn: () => getRecycleBin({ type, search, page: 1, pageSize: 50 }),
  });

  const previewMutation = useMutation({
    mutationFn: (item: CalendarRecycleBinItem) => previewRecycleRestore(item.type, item.id),
    onSuccess: data => setPreview(data),
  });

  const restoreMutation = useMutation({
    mutationFn: ({ item, restoreAsCopy }: { item: CalendarRecycleBinItem; restoreAsCopy: boolean }) =>
      restoreRecycleItem(item.type, item.id, restoreAsCopy),
    onSuccess: data => {
      setResult(data);
      setPreview(null);
      setSelected(null);
      queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      queryClient.invalidateQueries({ queryKey: ['today-sections'] });
      queryClient.invalidateQueries({ queryKey: ['today-section'] });
    },
  });

  const items = listQuery.data?.items ?? [];
  const selectedPreview = useMemo(() => preview && selected ? { preview, selected } : null, [preview, selected]);

  function openRestore(item: CalendarRecycleBinItem) {
    setSelected(item);
    setPreview(null);
    previewMutation.mutate(item);
  }

  return (
    <div className="mx-auto max-w-6xl space-y-4 pb-8">
      <PageHeader title="回收站" subtitle="查看并恢复已删除的任务、日程、日历本和任务本。" />
      <OperationResultBanner result={result} onDismiss={() => setResult(null)} />

      <section className="pim-panel flex flex-wrap items-center gap-3 p-4">
        <select value={type} onChange={event => setType(event.target.value)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm">
          {typeOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
        <input
          value={search}
          onChange={event => setSearch(event.target.value)}
          placeholder="搜索标题"
          className="min-w-64 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400"
        />
        <span className="ml-auto text-sm text-slate-500">共 {listQuery.data?.totalCount ?? 0} 项</span>
      </section>

      <section className="pim-panel overflow-hidden">
        {items.length === 0 ? (
          <EmptyState title="回收站为空" description="删除的任务、日程和本会显示在这里。" />
        ) : (
          <table className="w-full text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs text-slate-500">
              <tr>
                <th className="px-4 py-3">类型</th>
                <th className="px-4 py-3">标题</th>
                <th className="px-4 py-3">原本</th>
                <th className="px-4 py-3">删除时间</th>
                <th className="px-4 py-3">操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={`${item.type}-${item.id}`} className="border-b border-slate-100">
                  <td className="px-4 py-3 text-slate-500">{typeLabel(item.type)}</td>
                  <td className="px-4 py-3 font-medium text-slate-900">{item.title}</td>
                  <td className="px-4 py-3 text-slate-500">{item.bookName ?? '-'}</td>
                  <td className="px-4 py-3 text-slate-500">{new Date(item.deletedAt).toLocaleString('zh-CN')}</td>
                  <td className="px-4 py-3">
                    <button type="button" className="pim-button-secondary px-3 py-1.5 text-sm" onClick={() => openRestore(item)}>
                      恢复
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {selectedPreview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4">
          <div className="w-full max-w-lg rounded-lg border border-slate-200 bg-white p-5 shadow-2xl">
            <h2 className="text-base font-semibold text-slate-950">恢复 {selectedPreview.selected.title}</h2>
            {selectedPreview.preview.conflicts.length === 0 ? (
              <p className="mt-2 text-sm text-slate-500">将恢复 {selectedPreview.preview.restoreCount} 项。</p>
            ) : (
              <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                发现 {selectedPreview.preview.conflicts.length} 个冲突。可以取消，或恢复为独立副本。
              </div>
            )}
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" className="pim-button-secondary px-4 py-2 text-sm" onClick={() => { setSelected(null); setPreview(null); }}>
                取消
              </button>
              {selectedPreview.preview.conflicts.length > 0 && (
                <button type="button" className="pim-button-secondary px-4 py-2 text-sm" onClick={() => restoreMutation.mutate({ item: selectedPreview.selected, restoreAsCopy: true })}>
                  恢复为副本
                </button>
              )}
              <button
                type="button"
                disabled={!selectedPreview.preview.canRestoreWithoutConflict || restoreMutation.isPending}
                className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50"
                onClick={() => restoreMutation.mutate({ item: selectedPreview.selected, restoreAsCopy: false })}
              >
                恢复
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function typeLabel(type: string) {
  if (type === 'event') return '日程';
  if (type === 'task') return '任务';
  if (type === 'calendar') return '日历本';
  if (type === 'task-book') return '任务本';
  return type;
}
```

- [ ] **Step 2: Add Settings entry**

Modify `src/client-web/src/pages/SettingsPage.tsx` `settingsLinks`:

```ts
{
  title: '回收站',
  description: '查看并恢复已删除的任务、日程、日历本和任务本',
  label: '收',
  to: '/settings/recycle-bin',
},
```

- [ ] **Step 3: Add route**

Modify `src/client-web/src/layout/AppLayout.tsx` imports:

```tsx
import RecycleBinPage from '../pages/RecycleBinPage';
```

Add route:

```tsx
<Route path="/settings/recycle-bin" element={<RecycleBinPage />} />
```

- [ ] **Step 4: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/RecycleBinPage.tsx src/client-web/src/pages/SettingsPage.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat(web): add calendar recycle bin"
```

---

### Task 11: Replace Book Delete Browser Confirm In Sidebar

**Files:**
- Modify: `src/client-web/src/layout/Sidebar.tsx`

- [ ] **Step 1: Update Sidebar imports**

Modify imports in `Sidebar.tsx`:

```tsx
import { getCalendars, createCalendar, updateCalendar, deleteCalendar, previewCalendarDelete } from '../api/calendar';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
```

- [ ] **Step 2: Add delete preview state**

Inside `CalendarBookSection`, add:

```tsx
const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
const [deleteId, setDeleteId] = useState<string | null>(null);
```

Add preview mutation:

```tsx
const previewDeleteMut = useMutation({
  mutationFn: previewCalendarDelete,
  onSuccess: preview => {
    setDeleteInput({
      targetType: preview.targetType,
      title: preview.title,
      affectedCount: Math.max(1, preview.affectedCount),
      samples: preview.samples,
    });
  },
});
```

Update delete mutation `onSuccess`:

```tsx
onSuccess: () => {
  queryClient.invalidateQueries({ queryKey });
  queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
  setDeleteInput(null);
  setDeleteId(null);
}
```

- [ ] **Step 3: Replace delete button behavior**

Replace the delete button `onClick`:

```tsx
onClick={() => {
  setDeleteId(book.id);
  previewDeleteMut.mutate(book.id);
}}
```

Add this JSX at the bottom of `CalendarBookSection`:

```tsx
<ConfirmActionDialog
  open={Boolean(deleteInput)}
  input={deleteInput}
  isPending={deleteMut.isPending}
  onCancel={() => {
    setDeleteInput(null);
    setDeleteId(null);
  }}
  onConfirm={() => {
    if (deleteId) deleteMut.mutate(deleteId);
  }}
/>
```

- [ ] **Step 4: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/layout/Sidebar.tsx
git commit -m "feat(web): confirm calendar book deletes"
```

---

### Task 12: Upgrade Task List Page For Durable Filters And Batch Operations

**Files:**
- Modify: `src/client-web/src/pages/TaskListPage.tsx`
- Modify: `src/client-web/src/api/calendar.ts`

- [ ] **Step 1: Add paged task API helper**

Modify `src/client-web/src/api/calendar.ts`:

```ts
export interface GetTasksParams {
  inbox?: boolean;
  search?: string;
  calendarId?: string;
  status?: string;
  priority?: number;
  plannedFrom?: string;
  plannedTo?: string;
  dueFrom?: string;
  dueTo?: string;
  page?: number;
  pageSize?: number;
}

export async function getTasksPaged(params: GetTasksParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.inbox !== undefined) searchParams.set('inbox', String(params.inbox));
  if (params.search) searchParams.set('search', params.search);
  if (params.calendarId) searchParams.set('calendarId', params.calendarId);
  if (params.status) searchParams.set('status', params.status);
  if (params.priority !== undefined) searchParams.set('priority', String(params.priority));
  if (params.plannedFrom) searchParams.set('plannedFrom', params.plannedFrom);
  if (params.plannedTo) searchParams.set('plannedTo', params.plannedTo);
  if (params.dueFrom) searchParams.set('dueFrom', params.dueFrom);
  if (params.dueTo) searchParams.set('dueTo', params.dueTo);
  if (params.page) searchParams.set('page', String(params.page));
  if (params.pageSize) searchParams.set('pageSize', String(params.pageSize));
  const r = await apiGet<ApiResponse<PagedResult<TaskResponse>>>(`/calendar/tasks?${searchParams.toString()}`);
  return r.data;
}
```

- [ ] **Step 2: Refactor TaskListPage queries**

Modify `TaskListPage.tsx` to use server-side filters:

```tsx
const { data: taskBooks = [] } = useQuery({
  queryKey: ['calendars', 'task'],
  queryFn: () => getCalendars('task'),
});

const taskQuery = useQuery({
  queryKey: ['tasks-paged', filter, search, selectedTaskBook],
  queryFn: () => getTasksPaged(buildTaskQuery(filter, search, selectedTaskBook, todayStr)),
});
```

Add helper:

```tsx
function buildTaskQuery(filter: string, search: string, calendarId: string, todayStr: string) {
  const query: GetTasksParams = { page: 1, pageSize: 100 };
  if (search) query.search = search;
  if (calendarId) query.calendarId = calendarId;
  if (filter === 'inbox') query.inbox = true;
  if (filter === 'high') query.priority = 1;
  if (filter === 'completed') query.status = 'COMPLETED';
  if (filter === 'planned') {
    query.plannedFrom = `${todayStr}T00:00:00`;
    query.plannedTo = `${todayStr}T23:59:59`;
  }
  if (filter === 'today') {
    query.dueFrom = `${todayStr}T00:00:00`;
    query.dueTo = `${todayStr}T23:59:59`;
  }
  return query;
}
```

Add filters:

```ts
const filters = [
  { key: 'all', label: '全部' },
  { key: 'inbox', label: '收集箱' },
  { key: 'today', label: '今日截止' },
  { key: 'planned', label: '今日已排程' },
  { key: 'high', label: '高优先' },
  { key: 'completed', label: '已完成' },
] as const;
```

- [ ] **Step 3: Add batch selection and strict delete**

Add selection state:

```tsx
const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
```

Add batch delete mutation:

```tsx
const batchDeleteMutation = useMutation({
  mutationFn: () => batchDeleteTasks(Array.from(selectedIds)),
  onSuccess: () => {
    setSelectedIds(new Set());
    setDeleteInput(null);
    queryClient.invalidateQueries({ queryKey: ['tasks'] });
    queryClient.invalidateQueries({ queryKey: ['tasks-paged'] });
    queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
  },
});
```

Add batch delete button:

```tsx
<button
  type="button"
  disabled={selectedIds.size === 0}
  onClick={() => setDeleteInput({
    targetType: 'task',
    title: '选中的任务',
    affectedCount: selectedIds.size,
    samples: filtered.filter(task => selectedIds.has(task.id)).slice(0, 5).map(task => ({
      id: task.id,
      type: 'task',
      title: task.title,
      start: task.dtStart,
      end: task.plannedEnd,
      bookName: undefined,
    })),
  })}
  className="rounded-lg border border-red-200 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 disabled:opacity-40"
>
  删除选中
</button>
```

Render `ConfirmActionDialog` and call `batchDeleteMutation.mutate()` in `onConfirm`.

- [ ] **Step 4: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/TaskListPage.tsx src/client-web/src/api/calendar.ts
git commit -m "feat(web): upgrade task list filters"
```

---

### Task 13: Update Calendar Drag-To-Plan And Editor Drawers

**Files:**
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/src/dialogs/TaskEditorDialog.tsx`
- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`

- [ ] **Step 1: Use task planning API in CalendarPage**

Modify imports:

```tsx
import { getEvents, getTasks, planTask } from '../api/calendar';
import { useMutation, useQueryClient } from '@tanstack/react-query';
```

Add mutation:

```tsx
const queryClient = useQueryClient();
const planTaskMutation = useMutation({
  mutationFn: ({ task, plannedStart }: { task: TaskResponse; plannedStart: string }) =>
    planTask(task.id, {
      plannedStart,
      plannedEnd: task.estimatedDuration ? undefined : task.due,
      estimatedDuration: task.estimatedDuration,
    }),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['tasks'] });
    queryClient.invalidateQueries({ queryKey: ['events'] });
    queryClient.invalidateQueries({ queryKey: ['today-sections'] });
    queryClient.invalidateQueries({ queryKey: ['today-section'] });
  },
});
```

Update `handleExternalDrop`:

```tsx
const scheduledStart = toLocalDateTimeInputValue(dropInfo.date);
planTaskMutation.mutate({ task, plannedStart: scheduledStart });
```

Keep the edit drawer path for clicking already planned tasks.

- [ ] **Step 2: Add planned end to TaskEditorDialog**

Add state:

```tsx
const [plannedEnd, setPlannedEnd] = useState(task?.plannedEnd || '');
```

Include in submit data:

```tsx
plannedEnd: plannedEnd || undefined,
```

Add field:

```tsx
<Field label="计划结束">
  <input
    type="datetime-local"
    value={plannedEnd}
    onChange={e => setPlannedEnd(e.target.value)}
    className="w-full border rounded px-3 py-2 text-sm"
  />
</Field>
```

Replace task delete browser confirm with `ConfirmActionDialog` using one-item input.

- [ ] **Step 3: Add all-day and source display to EventEditorDialog**

Add state:

```tsx
const [isAllDay, setIsAllDay] = useState(Boolean(event?.isAllDay));
```

Include in submit data:

```tsx
isAllDay,
```

Add fields:

```tsx
<Field label="全天">
  <label className="inline-flex items-center gap-2 text-sm text-slate-600">
    <input type="checkbox" checked={isAllDay} onChange={e => setIsAllDay(e.target.checked)} />
    全天事件
  </label>
</Field>

{event?.source === 'outlook-ics' && (
  <div className="rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-sm text-blue-800">
    这是从 Outlook ICS 导入的事件。会议上下文会保留，但 PIM 不处理会议接受、拒绝或参会人状态。
  </div>
)}
```

Replace event delete browser confirm with `ConfirmActionDialog`.

- [ ] **Step 4: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/CalendarPage.tsx src/client-web/src/dialogs/TaskEditorDialog.tsx src/client-web/src/dialogs/EventEditorDialog.tsx
git commit -m "feat(web): plan tasks from calendar"
```

---

### Task 14: Upgrade Calendar Data Manager Import Report And Delete Confirmation

**Files:**
- Modify: `src/client-web/src/pages/CalendarDataManager.tsx`

- [ ] **Step 1: Replace import message with operation banner**

Import:

```tsx
import OperationResultBanner from '../ui/OperationResultBanner';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import type { CalendarOperationResult, ImportReport } from '../types';
```

Replace `importMsg` with:

```tsx
const [operationResult, setOperationResult] = useState<CalendarOperationResult | ImportReport | null>(null);
const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
```

Update import mutation `onSuccess`:

```tsx
onSuccess: (result) => {
  setOperationResult(result);
  queryClient.invalidateQueries({ queryKey: ['events-paged'] });
  queryClient.invalidateQueries({ queryKey: ['events'] });
}
```

Render:

```tsx
<OperationResultBanner result={operationResult} onDismiss={() => setOperationResult(null)} />
```

- [ ] **Step 2: Replace batch delete browser confirm**

Update `handleBatchDelete`:

```tsx
function handleBatchDelete() {
  if (!data || selectedIds.size === 0) return;
  const selectedItems = data.items.filter(event => selectedIds.has(event.id));
  setDeleteInput({
    targetType: 'event',
    title: '选中的日程',
    affectedCount: selectedItems.length,
    samples: selectedItems.slice(0, 5).map(event => ({
      id: event.originalEventId ?? event.id,
      type: 'event',
      title: event.title,
      start: event.dtStart,
      end: event.dtEnd,
      bookName: calendars?.find(cal => cal.id === event.calendarId)?.name,
    })),
  });
}
```

Add confirm dialog:

```tsx
<ConfirmActionDialog
  open={Boolean(deleteInput)}
  input={deleteInput}
  isPending={deleteMut.isPending}
  onCancel={() => setDeleteInput(null)}
  onConfirm={() => {
    if (!data) return;
    const originalIds = Array.from(selectedIds).map(id => {
      const evt = data.items.find(e => e.id === id);
      return evt?.originalEventId ?? id;
    });
    deleteMut.mutate(Array.from(new Set(originalIds)));
  }}
/>
```

Update delete mutation `onSuccess`:

```tsx
onSuccess: (result) => {
  setOperationResult(result);
  setSelectedIds(new Set());
  setDeleteInput(null);
  queryClient.invalidateQueries({ queryKey: ['events-paged'] });
  queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
}
```

- [ ] **Step 3: Add Outlook metadata to detail dialog**

Inside detail dialog description list, add:

```tsx
{detailEvent.source === 'outlook-ics' && (
  <div>
    <dt className="text-gray-400">Outlook 导入</dt>
    <dd>会议字段已保留，PIM 暂不处理会议响应。</dd>
  </div>
)}
{detailEvent.externalMetadataJson && detailEvent.externalMetadataJson !== '{}' && (
  <div>
    <dt className="text-gray-400">保留元数据</dt>
    <dd className="max-h-32 overflow-auto rounded bg-gray-50 p-2 font-mono text-xs">{detailEvent.externalMetadataJson}</dd>
  </div>
)}
```

- [ ] **Step 4: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/CalendarDataManager.tsx
git commit -m "feat(web): show calendar import reports"
```

---

### Task 15: Refine Today Calendar And Task Sections

**Files:**
- Modify: `src/client-web/src/pages/TodayPage.tsx`
- Modify: `src/client-web/src/components/today/TodaySectionHost.tsx`
- Modify: `src/client-web/src/components/today/TodayScheduleList.tsx`
- Modify: `src/client-web/src/components/today/TodayTaskColumn.tsx`

- [ ] **Step 1: Add event editor support on Today**

Modify `TodayPage.tsx` imports:

```tsx
import EventEditorDialog from '../dialogs/EventEditorDialog';
import type { EventResponse, TaskResponse, TodaySectionKind, TodaySectionRegistryItem } from '../types';
```

Use the exported `ScheduledItem` type from `TodayScheduleList` instead of importing from `types`:

```tsx
import TodayScheduleList, { type ScheduledItem } from '../components/today/TodayScheduleList';
```

Add state:

```tsx
const [eventEditorOpen, setEventEditorOpen] = useState(false);
const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();
```

Add handler:

```tsx
function openScheduledItem(item: ScheduledItem) {
  if (item.type === 'task') {
    openTask(item.task);
    return;
  }
  setEditingEvent(item.event);
  setEventEditorOpen(true);
}
```

Pass `onSelectScheduled={openScheduledItem}` through `TodaySectionHost`.

- [ ] **Step 2: Widen TodaySectionHost scheduled selection type**

Modify `src/client-web/src/components/today/TodaySectionHost.tsx` imports:

```tsx
import TodayScheduleList, { type ScheduledItem } from './TodayScheduleList';
```

Change the prop type:

```tsx
onSelectScheduled?: (item: ScheduledItem) => void;
```

Keep the existing `calendar.schedule` handler shape, but now it passes the full item:

```tsx
onSelect={item => {
  if (item.type === 'task') {
    onSelectTask?.(item.task);
  }
  onSelectScheduled?.(item);
}}
```

Render:

```tsx
<EventEditorDialog
  open={eventEditorOpen}
  onClose={() => setEventEditorOpen(false)}
  event={editingEvent}
/>
```

- [ ] **Step 3: Make TodayScheduleList events selectable**

Modify `ScheduledItem` event shape in `TodayScheduleList.tsx`:

```ts
| {
    type: 'event';
    id: string;
    event: EventResponse;
    title: string;
    start: string;
    end?: string;
    meta?: string;
    color?: string;
  }
```

When mapping events:

```ts
event,
```

Set `canSelect = Boolean(onSelect)` for both event and task:

```tsx
const canSelect = Boolean(onSelect);
```

Call `onSelect?.(item)` for both types.

- [ ] **Step 4: Add better empty actions in TodayTaskColumn**

Change empty state description:

```tsx
<EmptyState title="没有未完成任务" description="可以新建任务，或打开日历安排今天的工作。" />
```

Add planned end badge when available:

```tsx
{task.plannedEnd && <StatusBadge tone="activity">计划至 {formatDue(task.plannedEnd)}</StatusBadge>}
```

- [ ] **Step 5: Build web**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/pages/TodayPage.tsx src/client-web/src/components/today/TodaySectionHost.tsx src/client-web/src/components/today/TodayScheduleList.tsx src/client-web/src/components/today/TodayTaskColumn.tsx
git commit -m "feat(web): refine today planning sections"
```

---

### Task 16: Add Stage 5 Acceptance Documentation And Final Verification

**Files:**
- Create: `docs/operations/calendar-task-stage5-acceptance.md`

- [ ] **Step 1: Create acceptance doc**

Create `docs/operations/calendar-task-stage5-acceptance.md`:

```markdown
# Calendar And Task Stage 5 Acceptance

## Scope

Stage 5 verifies the calendar and task planning loop.

This stage includes:

- Task and event management hardening.
- Planned task time ranges.
- Outlook-compatible ICS import reports.
- Meeting metadata preservation without meeting workflow.
- Soft-delete recycle bin.
- Strict delete confirmation.
- Restore conflict handling.
- Today calendar/task section refinement.

This stage does not include:

- Outlook two-way sync.
- Meeting accept, reject, tentative, RSVP, attendee state sync, or meeting updates.
- Automatic scheduling algorithm workflow.
- Plan-vs-PC-activity matching.
- Permanent deletion.
- MCP server exposure.

## API Checks

- `GET /api/v1/calendar/tasks?search=...&page=1&pageSize=50` returns paged tasks.
- `POST /api/v1/calendar/tasks/{id}/plan` plans a task without creating an event.
- `POST /api/v1/calendar/tasks/batch-update` updates selected tasks.
- `DELETE /api/v1/calendar/tasks/{id}` moves a task to the recycle bin.
- `POST /api/v1/calendar/tasks/batch-delete` moves selected tasks to the recycle bin.
- `POST /api/v1/calendar/calendars/{id}/delete-preview` returns child impact for calendar and task books.
- `DELETE /api/v1/calendar/calendars/{id}` moves a book and active children to the recycle bin.
- `GET /api/v1/calendar/recycle-bin` lists deleted calendar/task objects.
- `POST /api/v1/calendar/recycle-bin/{type}/{id}/restore-preview` reports conflicts.
- `POST /api/v1/calendar/recycle-bin/{type}/{id}/restore` restores or restores as copy.
- `POST /api/v1/calendar/import-ics` returns imported/skipped counts and skipped reasons.
- `GET /api/v1/calendar/export-ics` excludes deleted events.

## Web Checks

- Open `/calendar`.
- Drag an unscheduled task onto the calendar.
- Confirm the task appears as a planned task and no event is created.
- Open `/tasks`.
- Filter by inbox, today, planned, high priority, and completed.
- Batch delete tasks and confirm the strict dialog mentions the recycle bin.
- Delete a single event from the editor and confirm the strict dialog mentions the recycle bin.
- Open Settings and then Recycle Bin.
- Restore a deleted task.
- Delete a non-empty calendar or task book from the sidebar and confirm child impact appears.
- Restore the deleted book and confirm same-operation children return.
- Delete an event, create the same event again, and confirm creation succeeds.
- Restore the old event and confirm conflict handling appears.
- Import an Outlook ICS with normal events, all-day events, recurring events, and meeting fields.
- Confirm the import report shows imported and skipped counts.
- Open imported meeting-like event details and confirm meeting context is read-only.
- Open Today and confirm events and planned tasks are distinct and clickable.

## Verification Commands

Run backend tests:

```powershell
dotnet test Pim.sln
```

Build Web:

```powershell
npm --prefix src/client-web run build
```

Run focused Web checks:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\calendarApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests\client-web\recycleBinApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.calendar-stage5.json
npm --prefix src/client-web exec tsx -- tests\client-web\confirmActionDialogModel.test.ts
```
```

- [ ] **Step 2: Run backend tests**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 4: Run focused frontend checks**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests\client-web\calendarApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests\client-web\recycleBinApiPath.test.ts
npm --prefix src/client-web exec tsc -- -p tests\client-web\tsconfig.calendar-stage5.json
npm --prefix src/client-web exec tsx -- tests\client-web\confirmActionDialogModel.test.ts
```

Expected: PASS.

- [ ] **Step 5: Check git status**

Run:

```powershell
git status --short --branch
```

Expected: only pre-existing unrelated files remain, such as untracked `docs/plan.md`.

- [ ] **Step 6: Commit**

```powershell
git add docs/operations/calendar-task-stage5-acceptance.md
git commit -m "docs: add calendar task stage 5 acceptance"
```

---

## Plan Self-Review

Spec coverage:

- Data model fields are covered by Task 1.
- Delete audit is covered by Task 2 and Task 3.
- Grouped soft delete and delete preview are covered by Task 3.
- Recycle bin, restore, conflict checks, and restore-as-copy are covered by Task 4.
- Task planning without event creation is covered by Task 5 and Task 13.
- Outlook-compatible ICS import and meeting metadata preservation are covered by Task 6 and Task 14.
- Stable API path coverage is covered by Task 7 and Task 8.
- Strict confirmation is covered by Task 9, Task 11, Task 12, Task 13, and Task 14.
- Recycle bin Web UI is covered by Task 10.
- Task list, calendar page, editor drawers, and Today refinements are covered by Task 12 through Task 15.
- Manual acceptance and final verification are covered by Task 16.

Placeholder scan:

- The plan contains no `TBD`, `TODO`, `FIXME`, "implement later", or "similar to Task N" instructions.
- Steps that change code include concrete code blocks or exact replacement snippets.
- Each task includes a verification command and a commit command.

Type consistency:

- Backend operation DTO names match frontend type names where JSON casing maps from PascalCase to camelCase.
- `PlanTaskRequest`, `CalendarOperationResult`, `CalendarDeletePreviewResponse`, `CalendarRestorePreviewResponse`, `CalendarRecycleBinItem`, and `ImportReport` are used consistently across backend and frontend tasks.
- The task planned range uses existing `dtStart` plus new `plannedEnd` consistently.
