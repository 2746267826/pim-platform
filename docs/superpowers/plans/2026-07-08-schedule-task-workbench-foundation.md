# Schedule Task Workbench Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable foundation for the approved Schedule And Task Workbench design: multi-segment task planning, confirmation visibility for important fact changes, Outlook Graph connection/sync visibility, Data Center search, and complete Web workbench shells.

**Architecture:** Keep the current Stage 5 calendar/task model intact and add source-aware planning primitives around it. Backend work adds small Calendar module services/entities and reuses the shared operations confirmation service. Web work adds route-level workbench pages that call real API wrappers and degrade gracefully when no data exists.

**Tech Stack:** .NET 8, ASP.NET Minimal APIs, EF Core/Npgsql, xUnit, React 19, TanStack Query, React Router, TypeScript, Vite.

---

## Scope

This plan is the first implementation milestone for the full design. It produces a runnable branch with verified foundations that later tracks can extend without changing the first contracts.

Included:

- Task execution segments as first-class persisted rows linked to tasks.
- Calendar layer query returning events, task segments, Outlook-origin flags, and pending suggestion placeholders.
- L0-L4 operation risk levels while preserving old Low/Medium/High data compatibility.
- Confirmation summary/detail API wrappers and Today/Web workbench panels.
- Outlook Graph settings, device-code request contract, sync batch log contract, and source tags.
- Data Center query over events, tasks, execution segments, confirmations, recycle bin, and sync batches.
- Web pages: Today command center additions, Workbench, Data Center, Sync, Confirmations, Reminders, Reports, and Habits placeholder pages with real navigation and typed API surfaces.
- Verification scripts for the new Web contracts.

Deferred to follow-up milestones:

- Full Microsoft token encryption/refresh implementation.
- Graph delta writeback execution.
- Audit version restore.
- Windows WebView2 and Android WebView embedding.
- Native endpoint notification action execution.

## File Map

- Modify `src/Pim.Core/Operations/OperationEnums.cs`: add L0-L4 risk values while retaining Low/Medium/High enum members.
- Modify `src/Pim.Core/Operations/ConfirmationDtos.cs`: add optional changed fields, allowed actions, object metadata, and second-level confirmation flags.
- Modify `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`: map legacy and L-level risk values safely.
- Create `src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs`: persisted execution segment.
- Create `src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs`: persisted sync run summary/log.
- Modify `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`: add provider settings, token health, tenant/client/scopes, and status.
- Modify `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`: indexes and relationships.
- Modify `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`: add segment, layer, Outlook, sync, and Data Center DTOs.
- Create `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`: segment invariants and calendar layer query.
- Create `src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs`: global query.
- Modify `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`: settings, device-code contract, batch recording, confirmation creation for Outlook-origin diffs.
- Modify `src/modules/Pim.Module.Calendar/CalendarModule.cs`: service registration and endpoints.
- Create `src/modules/Pim.Module.Calendar/Migrations/202607080001_ScheduleTaskWorkbenchFoundation.cs`: database migration.
- Create/modify tests under `tests/Pim.UnitTests/Calendar` and `tests/Pim.UnitTests/Operations`.
- Modify `src/client-web/src/types/index.ts`: add Web workbench contract types.
- Modify `src/client-web/src/api/calendar.ts`: segment, layer, Outlook, and Data Center API wrappers.
- Create `src/client-web/src/api/operations.ts`: confirmation API wrappers.
- Create `src/client-web/src/pages/WorkbenchPage.tsx`.
- Create `src/client-web/src/pages/SyncPage.tsx`.
- Create `src/client-web/src/pages/DataCenterPage.tsx`.
- Create `src/client-web/src/pages/ConfirmationsPage.tsx`.
- Create `src/client-web/src/pages/RemindersPage.tsx`.
- Create `src/client-web/src/pages/ReportsPage.tsx`.
- Create `src/client-web/src/pages/HabitsPage.tsx`.
- Modify `src/client-web/src/pages/TodayPage.tsx`: add density/focus controls and workbench summary panels.
- Modify `src/client-web/src/pages/CalendarPage.tsx`: add layer toggles and render execution segments.
- Modify `src/client-web/src/layout/AppLayout.tsx` and `src/client-web/src/layout/Sidebar.tsx`: route/navigation entries.
- Create tests under `tests/client-web`: `scheduleWorkbenchApiPath.test.ts`, `scheduleWorkbenchTypes.test.ts`, and `tsconfig.schedule-workbench.json`.
- Modify `src/client-web/package.json`: add `test:schedule-workbench`.
- Modify `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`: add milestone implementation status.

---

### Task 1: Risk And Confirmation Contracts

**Files:**
- Modify: `src/Pim.Core/Operations/OperationEnums.cs`
- Modify: `src/Pim.Core/Operations/ConfirmationDtos.cs`
- Modify: `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`
- Test: `tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs`

- [ ] **Step 1: Write the failing tests**

Create `tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs`:

```csharp
using Pim.Core.Operations;

namespace Pim.UnitTests.Operations;

public class ScheduleWorkbenchConfirmationContractTests
{
    [Fact]
    public void RiskLevelsExposeWorkbenchScaleAndLegacyValues()
    {
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L0AutomaticArtifact"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L1LowRiskAction"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L2PimFactChange"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L3ExternalSourceOrWriteback"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L4BatchOrDestructiveGovernance"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "Low"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "Medium"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "High"));
    }

    [Fact]
    public void ConfirmationDtoCarriesDiffAndSecondLevelMetadata()
    {
        var dto = new OperationConfirmationDto(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "calendar.event.update",
            "Change Outlook event location",
            OperationRiskLevel.L3ExternalSourceOrWriteback,
            "outlook",
            "{}",
            "{}",
            OperationConfirmationStatus.Pending,
            DateTimeOffset.UtcNow.AddHours(2),
            DateTimeOffset.UtcNow,
            null,
            null,
            null,
            "corr-1",
            ["location"],
            ["keep_pim", "keep_outlook"],
            "event",
            Guid.NewGuid(),
            true);

        Assert.Contains("location", dto.ChangedFields);
        Assert.Contains("keep_outlook", dto.AllowedActions);
        Assert.Equal("event", dto.ObjectType);
        Assert.True(dto.RequiresSecondLevelConfirmation);
    }
}
```

- [ ] **Step 2: Run the tests and verify red**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ScheduleWorkbenchConfirmationContractTests
```

Expected: FAIL because L-level risk names and new DTO constructor parameters do not exist.

- [ ] **Step 3: Implement minimal contracts**

Edit `src/Pim.Core/Operations/OperationEnums.cs`:

```csharp
public enum OperationRiskLevel
{
    Low = 0,
    Medium = 1,
    High = 2,
    L0AutomaticArtifact = 10,
    L1LowRiskAction = 11,
    L2PimFactChange = 12,
    L3ExternalSourceOrWriteback = 13,
    L4BatchOrDestructiveGovernance = 14
}
```

Edit `src/Pim.Core/Operations/ConfirmationDtos.cs` by extending `OperationConfirmationDto`:

```csharp
public sealed record OperationConfirmationDto(
    Guid Id,
    Guid? RequestedByUserId,
    string OperationType,
    string Summary,
    OperationRiskLevel RiskLevel,
    string Source,
    string PayloadJson,
    string PreviewJson,
    OperationConfirmationStatus Status,
    DateTimeOffset ExpiresAt,
    DateTimeOffset CreatedAt,
    DateTimeOffset? ConfirmedAt,
    DateTimeOffset? ExecutedAt,
    string? ResultJson,
    string? CorrelationId,
    IReadOnlyList<string>? ChangedFields = null,
    IReadOnlyList<string>? AllowedActions = null,
    string? ObjectType = null,
    Guid? ObjectId = null,
    bool RequiresSecondLevelConfirmation = false);
```

Edit `OperationConfirmationService.Map` to parse risk levels with fallback:

```csharp
private static OperationRiskLevel ParseRiskLevel(string value)
{
    return Enum.TryParse<OperationRiskLevel>(value, out var parsed)
        ? parsed
        : OperationRiskLevel.Medium;
}
```

Use `ParseRiskLevel(entity.RiskLevel)` in `Map`.

- [ ] **Step 4: Verify green**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ScheduleWorkbenchConfirmationContractTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/Pim.Core/Operations/OperationEnums.cs src/Pim.Core/Operations/ConfirmationDtos.cs src/Pim.Infrastructure/Operations/OperationConfirmationService.cs tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs
git commit -m "feat: extend confirmation risk contracts"
```

---

### Task 2: Task Execution Segments

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Test: `tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs`

- [ ] **Step 1: Write the failing tests**

Create `tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs` with tests for:

```csharp
namespace Pim.UnitTests.Calendar;

public class TaskExecutionSegmentServiceTests
{
    [Fact]
    public async Task CreateSegmentRejectsEndBeforeStart()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<PlanningModelService>();
        var task = await CalendarTestHost.CreateTaskAsync(scope, "Deep work");

        await Assert.ThrowsAsync<DomainException>(() => service.CreateSegmentAsync(
            task.Id,
            new CreateTaskExecutionSegmentRequest(
                new DateTimeOffset(2026, 7, 8, 11, 0, 0, TimeSpan.Zero),
                new DateTimeOffset(2026, 7, 8, 10, 0, 0, TimeSpan.Zero),
                "planned",
                "manual",
                "invalid range"),
            CancellationToken.None));
    }

    [Fact]
    public async Task CreateSegmentKeepsTaskIdentityAndReturnsSegment()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<PlanningModelService>();
        var task = await CalendarTestHost.CreateTaskAsync(scope, "Write report");

        var segment = await service.CreateSegmentAsync(
            task.Id,
            new CreateTaskExecutionSegmentRequest(
                new DateTimeOffset(2026, 7, 8, 9, 0, 0, TimeSpan.Zero),
                new DateTimeOffset(2026, 7, 8, 10, 30, 0, TimeSpan.Zero),
                "planned",
                "manual",
                "user plan"),
            CancellationToken.None);

        Assert.Equal(task.Id, segment.TaskId);
        Assert.Equal("Write report", segment.TaskTitle);
        Assert.Equal("manual", segment.Source);
        Assert.Equal("planned", segment.Status);
    }
}
```

If `CalendarTestHost` is missing, create a private helper in the test file that builds an in-memory `PimDbContext`, registers `PlanningModelService`, and seeds one `TaskEntity`.

- [ ] **Step 2: Run the tests and verify red**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TaskExecutionSegmentServiceTests
```

Expected: FAIL because the entity, DTO, and service do not exist.

- [ ] **Step 3: Add entity and DTOs**

Create `TaskExecutionSegmentEntity` with columns: `id`, `task_id`, `user_id`, `starts_at`, `ends_at`, `status`, `source`, `planning_reason`, `confirmation_id`, `created_at`, `updated_at`, `deleted_at`.

Add DTO records to `CalendarDtos.cs`:

```csharp
public record CreateTaskExecutionSegmentRequest(
    DateTimeOffset StartsAt,
    DateTimeOffset EndsAt,
    string Status,
    string Source,
    string? PlanningReason);

public record TaskExecutionSegmentResponse(
    Guid Id,
    Guid TaskId,
    string TaskTitle,
    DateTimeOffset StartsAt,
    DateTimeOffset EndsAt,
    string Status,
    string Source,
    string? PlanningReason,
    Guid? ConfirmationId);
```

- [ ] **Step 4: Add service and endpoints**

Create `PlanningModelService` with methods:

```csharp
Task<TaskExecutionSegmentResponse> CreateSegmentAsync(Guid taskId, CreateTaskExecutionSegmentRequest request, CancellationToken ct);
Task<IReadOnlyList<TaskExecutionSegmentResponse>> ListSegmentsAsync(Guid taskId, CancellationToken ct);
Task DeleteSegmentAsync(Guid taskId, Guid segmentId, CancellationToken ct);
```

Register it in `CalendarModule.RegisterServices`.

Add endpoints:

```csharp
group.MapGet("/tasks/{id:guid}/segments", ...);
group.MapPost("/tasks/{id:guid}/segments", ...);
group.MapDelete("/tasks/{taskId:guid}/segments/{segmentId:guid}", ...);
```

- [ ] **Step 5: Verify green**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~TaskExecutionSegmentServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar/TaskExecutionSegmentServiceTests.cs
git commit -m "feat: add task execution segments"
```

---

### Task 3: Calendar Layers And Data Center Query

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Test: `tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs`

- [ ] **Step 1: Write the failing tests**

Create tests that seed one event, one task segment, and one pending confirmation, then assert:

```csharp
Assert.Contains(result.Items, item => item.ObjectType == "event");
Assert.Contains(result.Items, item => item.ObjectType == "task-segment");
Assert.Contains(result.Items, item => item.ObjectType == "confirmation");
Assert.Contains(layer.Items, item => item.Layer == "task-segments");
```

- [ ] **Step 2: Run the tests and verify red**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarWorkbenchQueryTests
```

Expected: FAIL because layer and Data Center services do not exist.

- [ ] **Step 3: Add DTOs**

Add records:

```csharp
public record CalendarLayerQuery(DateTimeOffset Start, DateTimeOffset End, IReadOnlyList<string>? Layers, bool OutlookOnly = false);

public record CalendarLayerItem(
    string Id,
    string Layer,
    string ObjectType,
    Guid ObjectId,
    string Title,
    DateTimeOffset StartsAt,
    DateTimeOffset EndsAt,
    string Source,
    string Status,
    string Color,
    bool RequiresConfirmation);

public record CalendarLayerResponse(DateTimeOffset Start, DateTimeOffset End, IReadOnlyList<CalendarLayerItem> Items);

public record DataCenterQueryRequest(string? Search, string? ObjectType, string? Source, bool PendingOnly, int Page = 1, int PageSize = 50);

public record DataCenterItem(
    string ObjectType,
    Guid ObjectId,
    string Title,
    string Source,
    string Status,
    DateTimeOffset? StartsAt,
    DateTimeOffset? EndsAt,
    string Summary);

public record DataCenterQueryResponse(IReadOnlyList<DataCenterItem> Items, int Page, int PageSize, int TotalCount);
```

- [ ] **Step 4: Implement queries**

Add `PlanningModelService.GetCalendarLayersAsync(CalendarLayerQuery query, CancellationToken ct)` to merge:

- `events` as `Layer = "events"`.
- `task_execution_segments` as `Layer = "task-segments"`.

Add `DataCenterQueryService.QueryAsync` to merge queryable projections from events, tasks, task segments, operation confirmations, recycle bin rows using `IgnoreQueryFilters`, and sync batches from Task 4 after the entity exists.

- [ ] **Step 5: Add endpoints**

In `CalendarModule`:

```csharp
group.MapGet("/layers", ...);
group.MapPost("/data-center/query", ...);
```

Add constants:

```csharp
public const string CalendarLayers = "/api/v1/calendar/layers";
public const string DataCenterQuery = "/api/v1/calendar/data-center/query";
```

- [ ] **Step 6: Verify green**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarWorkbenchQueryTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar/CalendarWorkbenchQueryTests.cs
git commit -m "feat: query schedule workbench layers"
```

---

### Task 4: Outlook Graph Settings And Sync Batch Visibility

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs`

- [ ] **Step 1: Write the failing tests**

Create tests asserting:

```csharp
Assert.Equal("common", settings.TenantId);
Assert.Contains("Calendars.ReadWrite", settings.Scopes);
Assert.Contains("offline_access", settings.Scopes);
Assert.StartsWith("https://login.microsoftonline.com/common/oauth2/v2.0/devicecode", request.Endpoint);
Assert.Equal(OperationRiskLevel.L3ExternalSourceOrWriteback, confirmation.RiskLevel);
Assert.True(confirmation.RequiresSecondLevelConfirmation);
Assert.Equal("outlook", batch.Provider);
Assert.Contains(batch.Steps, step => step.Name == "Load provider configuration");
```

- [ ] **Step 2: Run the tests and verify red**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookGraphSyncFoundationTests
```

Expected: FAIL because settings and batch contracts do not exist.

- [ ] **Step 3: Add entities and DTOs**

Add Outlook connection fields:

```csharp
[Column("provider")][MaxLength(40)] public string Provider { get; set; } = "outlook";
[Column("client_id")][MaxLength(255)] public string? ClientId { get; set; }
[Column("tenant_id")][MaxLength(255)] public string TenantId { get; set; } = "common";
[Column("scopes")][MaxLength(1000)] public string Scopes { get; set; } = "Calendars.ReadWrite offline_access User.Read openid profile";
[Column("status")][MaxLength(40)] public string Status { get; set; } = "not-connected";
[Column("token_health")][MaxLength(40)] public string TokenHealth { get; set; } = "missing";
[Column("delta_link")] public string? DeltaLink { get; set; }
[Column("last_error")] public string? LastError { get; set; }
```

Create `OutlookSyncBatchEntity` with provider, status, counts, `steps_json`, `errors_json`, `started_at`, `finished_at`, and `created_confirmation_count`.

Add DTOs:

```csharp
public record OutlookSettingsResponse(string Provider, string TenantId, string? ClientId, string Scopes, string Status, string TokenHealth, DateTimeOffset? LastSyncedAt, string? LastError);
public record UpdateOutlookSettingsRequest(string TenantId, string? ClientId, string Scopes);
public record OutlookDeviceCodeRequestResponse(string Endpoint, string VerificationUri, string UserCode, DateTimeOffset ExpiresAt, string Message);
public record OutlookSyncStep(string Name, string Status, string Detail, DateTimeOffset At);
public record OutlookSyncBatchResponse(Guid Id, string Provider, string Status, int ReadCount, int CreatedCount, int UpdatedCount, int ConflictCount, int ConfirmationCount, int FailureCount, IReadOnlyList<OutlookSyncStep> Steps, string? ErrorSummary, DateTimeOffset StartedAt, DateTimeOffset? FinishedAt);
```

- [ ] **Step 4: Implement service methods**

Add:

```csharp
Task<OutlookSettingsResponse> GetSettingsAsync(Guid userId, CancellationToken ct);
Task<OutlookSettingsResponse> UpdateSettingsAsync(Guid userId, UpdateOutlookSettingsRequest request, CancellationToken ct);
Task<OutlookDeviceCodeRequestResponse> CreateDeviceCodeRequestAsync(Guid userId, CancellationToken ct);
Task<OutlookSyncBatchResponse> SyncAsync(Guid userId, CancellationToken ct);
Task<IReadOnlyList<OutlookSyncBatchResponse>> ListBatchesAsync(Guid userId, CancellationToken ct);
```

`CreateDeviceCodeRequestAsync` builds endpoint `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/devicecode` and returns a deterministic message when no `HttpClient` response is configured in tests.

`SyncAsync` creates a batch before external work, appends steps, catches exceptions by updating the same batch to `failed`, and returns the batch response. Do not reference a batch variable outside the scope where it is assigned.

For Outlook-origin core diffs, call `IOperationConfirmationService.CreateAsync` with `RiskLevel = L3ExternalSourceOrWriteback`, `Source = "outlook"`, changed fields, allowed actions, and `RequiresSecondLevelConfirmation = true`.

- [ ] **Step 5: Add endpoints**

Add:

```csharp
group.MapGet("/outlook/settings", ...);
group.MapPut("/outlook/settings", ...);
group.MapPost("/outlook/device-code", ...);
group.MapGet("/outlook/sync/batches", ...);
```

Keep existing `POST /outlook/sync` and change response from string to `OutlookSyncBatchResponse`.

- [ ] **Step 6: Verify green**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~OutlookGraphSyncFoundationTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs
git commit -m "feat: expose outlook sync foundation"
```

---

### Task 5: EF Migration

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Migrations/202607080001_ScheduleTaskWorkbenchFoundation.cs`
- Test: `tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs`

- [ ] **Step 1: Write the failing model test**

Create a test that asserts the EF model contains entity types for `TaskExecutionSegmentEntity` and `OutlookSyncBatchEntity`, and that indexes exist on task id, user id, start time, provider, and started time.

- [ ] **Step 2: Run the test and verify red**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ScheduleWorkbenchModelTests
```

Expected: FAIL before migration/configuration is present.

- [ ] **Step 3: Add migration**

Create a hand-written migration in the Calendar module migrations folder that:

- Creates `task_execution_segments`.
- Creates `outlook_sync_batches`.
- Adds Outlook connection columns from Task 4.
- Adds indexes declared in entity configurations.

- [ ] **Step 4: Verify green**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ScheduleWorkbenchModelTests
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/modules/Pim.Module.Calendar/Migrations tests/Pim.UnitTests/Calendar/ScheduleWorkbenchModelTests.cs
git commit -m "feat: add schedule workbench migration"
```

---

### Task 6: Web API Contracts

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`
- Create: `src/client-web/src/api/operations.ts`
- Create: `tests/client-web/scheduleWorkbenchApiPath.test.ts`
- Create: `tests/client-web/scheduleWorkbenchTypes.test.ts`
- Create: `tests/client-web/tsconfig.schedule-workbench.json`
- Modify: `src/client-web/package.json`

- [ ] **Step 1: Write failing API path tests**

Create tests asserting these paths and request bodies:

```ts
assert.equal(calendarApiPaths.taskSegments('task-1'), '/calendar/tasks/task-1/segments');
assert.equal(calendarApiPaths.calendarLayers({ start: '2026-07-08T00:00:00Z', end: '2026-07-09T00:00:00Z', layers: ['events', 'task-segments'], outlookOnly: true }), '/calendar/layers?start=2026-07-08T00%3A00%3A00Z&end=2026-07-09T00%3A00%3A00Z&layers=events%2Ctask-segments&outlookOnly=true');
assert.equal(calendarApiPaths.dataCenterQuery(), '/calendar/data-center/query');
assert.equal(calendarApiPaths.outlookSettings(), '/calendar/outlook/settings');
assert.equal(calendarApiPaths.outlookDeviceCode(), '/calendar/outlook/device-code');
assert.equal(calendarApiPaths.outlookSyncBatches(), '/calendar/outlook/sync/batches');
```

Create `operationsApiPaths.pendingConfirmations()` with `/operations/confirmations/pending`, `confirm(id)`, `reject(id)`, and `detail(id)`.

- [ ] **Step 2: Write failing type test**

Create `scheduleWorkbenchTypes.test.ts` that imports the new types and instantiates:

```ts
const risk: OperationRiskLevel = 'L3ExternalSourceOrWriteback';
const layer: CalendarLayerId = 'task-segments';
const density: WorkbenchDensityMode = 'focus';
```

- [ ] **Step 3: Add package script**

Add:

```json
"test:schedule-workbench": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchApiPath.test.ts && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.schedule-workbench.json && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchTypes.test.ts"
```

- [ ] **Step 4: Run tests and verify red**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench
```

Expected: FAIL because the wrappers and types do not exist.

- [ ] **Step 5: Implement wrappers and types**

Add type aliases and interfaces for:

- `OperationRiskLevel`
- `OperationConfirmation`
- `WorkbenchDensityMode`
- `CalendarLayerId`
- `TaskExecutionSegmentResponse`
- `CalendarLayerResponse`
- `DataCenterQueryResponse`
- `OutlookSettingsResponse`
- `OutlookDeviceCodeRequestResponse`
- `OutlookSyncBatchResponse`
- `ReminderSummary`
- `ReportSummary`
- `HabitRoutineSummary`

Add calendar API functions for segments, layers, Data Center query, Outlook settings, device code, sync, and sync batches.

Add operations API functions for pending/detail/confirm/reject confirmations.

- [ ] **Step 6: Verify green**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/calendar.ts src/client-web/src/api/operations.ts tests/client-web/scheduleWorkbenchApiPath.test.ts tests/client-web/scheduleWorkbenchTypes.test.ts tests/client-web/tsconfig.schedule-workbench.json src/client-web/package.json
git commit -m "feat: add schedule workbench web contracts"
```

---

### Task 7: Web Workbench Pages And Navigation

**Files:**
- Create: `src/client-web/src/pages/WorkbenchPage.tsx`
- Create: `src/client-web/src/pages/SyncPage.tsx`
- Create: `src/client-web/src/pages/DataCenterPage.tsx`
- Create: `src/client-web/src/pages/ConfirmationsPage.tsx`
- Create: `src/client-web/src/pages/RemindersPage.tsx`
- Create: `src/client-web/src/pages/ReportsPage.tsx`
- Create: `src/client-web/src/pages/HabitsPage.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/pages/TodayPage.tsx`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Test: `tests/client-web/scheduleWorkbenchApiPath.test.ts`

- [ ] **Step 1: Extend the failing Web test**

Add source assertions:

```ts
const appLayout = readFileSync(new URL('../../src/client-web/src/layout/AppLayout.tsx', import.meta.url), 'utf8');
const sidebar = readFileSync(new URL('../../src/client-web/src/layout/Sidebar.tsx', import.meta.url), 'utf8');
const today = readFileSync(new URL('../../src/client-web/src/pages/TodayPage.tsx', import.meta.url), 'utf8');
const calendar = readFileSync(new URL('../../src/client-web/src/pages/CalendarPage.tsx', import.meta.url), 'utf8');

assert.match(appLayout, /path="\/workbench"/);
assert.match(appLayout, /path="\/sync"/);
assert.match(appLayout, /path="\/data-center"/);
assert.match(appLayout, /path="\/confirmations"/);
assert.match(sidebar, /\/workbench/);
assert.match(sidebar, /\/sync/);
assert.match(sidebar, /\/data-center/);
assert.match(today, /densityMode/);
assert.match(calendar, /task-segments/);
```

- [ ] **Step 2: Run tests and verify red**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench
```

Expected: FAIL because routes/pages/source markers do not exist.

- [ ] **Step 3: Implement pages**

Implement `WorkbenchPage.tsx` as a full first-screen dashboard with:

- Page header.
- Density segmented controls: `standard`, `dense`, `focus`.
- Cards for schedule layers, pending confirmations, Outlook sync, reminders, reports, endpoint status.
- Links to `/calendar`, `/tasks`, `/confirmations`, `/sync`, `/data-center`, `/reminders`, `/reports`.

Implement `SyncPage.tsx` with:

- Outlook settings form for tenant, client id, scopes.
- Device-code panel showing verification URL and user code.
- Sync run button.
- Sync batch timeline with step names, counts, errors, and source tag text.
- Warning copy for second-level confirmation on Outlook-origin fact changes.

Implement `DataCenterPage.tsx` with:

- Search input.
- Object/source/pending filters.
- Results table with object type, title, source, status, time, summary.
- Batch preview panel text driven by selected rows.

Implement `ConfirmationsPage.tsx` with:

- Pending list from `getPendingConfirmations`.
- Detail panel rendering changed fields, allowed actions, risk, source, object ids, and second-level marker.
- Confirm and reject buttons.

Implement `RemindersPage.tsx`, `ReportsPage.tsx`, and `HabitsPage.tsx` as complete empty-state pages with filter controls and sample-free panels that explain live data absence through state labels, not marketing text.

- [ ] **Step 4: Wire navigation**

Add lazy imports/routes:

```tsx
const WorkbenchPage = lazy(() => import('../pages/WorkbenchPage'));
const SyncPage = lazy(() => import('../pages/SyncPage'));
const DataCenterPage = lazy(() => import('../pages/DataCenterPage'));
const ConfirmationsPage = lazy(() => import('../pages/ConfirmationsPage'));
const RemindersPage = lazy(() => import('../pages/RemindersPage'));
const ReportsPage = lazy(() => import('../pages/ReportsPage'));
const HabitsPage = lazy(() => import('../pages/HabitsPage'));
```

Add sidebar nav entries with paths `/workbench`, `/confirmations`, `/sync`, `/data-center`, `/reminders`, `/reports`, `/habits`.

- [ ] **Step 5: Extend Today and Calendar**

Today:

- Add `densityMode` state.
- Add a compact confirmation queue using `getPendingConfirmations`.
- Add sync summary link using `getOutlookSyncBatches`.

Calendar:

- Add layer toggle state for `events`, `task-segments`, `habits`, `availability`, and `ai-placeholders`.
- Fetch `getCalendarLayers`.
- Render task segments as distinct FullCalendar events with class name `pim-calendar-layer-task-segment`.

- [ ] **Step 6: Verify green and build**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench
npm --prefix src/client-web run build
```

Expected: both PASS. Vite may warn about chunk size; that warning is accepted for this milestone.

- [ ] **Step 7: Commit**

```powershell
git add src/client-web/src src/client-web/package.json tests/client-web
git commit -m "feat: render schedule workbench web shell"
```

---

### Task 8: Final Verification, Browser Check, And Milestone Status

**Files:**
- Modify: `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`

- [ ] **Step 1: Update spec status**

Append a milestone status section:

```markdown
## Implementation Status

### 2026-07-08 Foundation Milestone

Implemented on branch `codex/schedule-task-workbench`:

- Multi-segment task execution model.
- Calendar layer query.
- Shared confirmation risk contracts for L0-L4.
- Outlook Graph settings and sync batch visibility.
- Data Center query foundation.
- Web workbench shell pages and navigation.

Remaining milestones:

- Full Graph token refresh, delta writeback, and conflict resolution execution.
- Audit version restore/export.
- Native Windows WebView2 shell and notification actions.
- Native Android WebView shell and notification actions.
```

- [ ] **Step 2: Run full verification**

Run:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run test:schedule-workbench
npm --prefix src/client-web run build
git diff --check
git status --short --branch
```

Expected:

- `dotnet test Pim.sln`: PASS.
- `test:schedule-workbench`: PASS.
- `build`: PASS with no TypeScript errors.
- `git diff --check`: no output.
- `git status`: only intentional doc change before commit.

- [ ] **Step 3: Start browser preview**

Run a dev server on a free port:

```powershell
npm --prefix src/client-web run dev -- --host 127.0.0.1 --port 63767
```

Open the in-app browser to `http://127.0.0.1:63767/#/workbench` or the actual Vite URL, then inspect:

- `/workbench`
- `/calendar`
- `/sync`
- `/data-center`
- `/confirmations`

Capture screenshots if visual layout needs iteration.

- [ ] **Step 4: Commit status**

```powershell
git add docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md
git commit -m "docs: record schedule workbench foundation status"
```

- [ ] **Step 5: Finish branch**

Use `superpowers:verification-before-completion`, then `superpowers:requesting-code-review`, then `superpowers:finishing-a-development-branch`.

If all verification passes, push the branch and request GA/GitHub Actions validation for the integration branch.
