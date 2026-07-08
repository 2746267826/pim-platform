# Schedule Task Workbench Full Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete every requirement in `2026-07-08-schedule-task-workbench-design.md` and finish all foundation gaps from `2026-07-08-schedule-task-workbench-foundation.md` as one fully verified product increment.

**Architecture:** Keep the Stage 5 calendar/task foundation, preserve the current module layout, and complete the product by adding missing planning objects, confirmation/audit enforcement, Outlook Graph execution, reminders, reports, data-center governance, Chinese Web UI, Windows companion shell, and Android companion shell. All important fact changes flow through the shared confirmation/audit pipeline; endpoint native clients cache only collection uploads and use embedded Web for complex modules.

**Tech Stack:** .NET 8, ASP.NET Minimal APIs, EF Core/Npgsql, Microsoft Graph REST, DataProtection secrets, xUnit, React 19, TypeScript, TanStack Query, React Router, Vite, Playwright/headless Chrome, WPF/WebView2/Toast notifications, Android Kotlin/Jetpack Compose/WebView/WorkManager/Hilt, GitHub Actions.

---

## Non-Negotiable Scope

This is not a new foundation-only milestone. It completes the entire approved design.

Every item previously listed as remaining is in scope:

- Microsoft Graph token encryption, device-code polling, refresh, delta reads, writeback, and conflict resolution.
- Audit version restore/export and batch governance execution.
- Windows WebView2 shell, tray audit center, Toast actions, notification routing, and online/offline boundary.
- Android WebView shell, permission center, collection quality, notification actions, and online/offline boundary.
- Reminder, report, and habit services with real persisted data and Web UI.
- Full Chinese UI for new schedule/task surfaces and removal of newly introduced English-only workbench copy.
- GitHub Actions validation for API, Web, Windows, and Android.

## Starting Point

The plan branch was created from latest `origin/master` in a short-path worktree:

- Worktree: `C:\pim-plan`
- Branch: `codex/schedule-task-completion-plan`
- Base: latest `origin/master` at plan creation

Implementation should use a separate execution branch after this plan is approved:

```powershell
git fetch --all --prune
git worktree add C:\pim-full codex/schedule-task-complete-system origin/master
cd C:\pim-full
git status --short --branch
```

Expected status:

```text
## codex/schedule-task-complete-system...origin/master
```

## File Map

### Shared Core And Infrastructure

- Modify `src/Pim.Core/Operations/OperationEnums.cs`: keep L0-L4 risk levels and add reusable operation categories.
- Modify `src/Pim.Core/Operations/ConfirmationDtos.cs`: complete field diff, before/after, source, object, action, second-level, strict confirmation, and audit metadata.
- Create `src/Pim.Core/Planning/PlanningDtos.cs`: shared planning DTOs for tasks, books, projects, segments, habits, availability, placeholders, reminders, reports.
- Create `src/Pim.Core/Planning/PlanningEnums.cs`: task states, segment states, habit cadence, reminder channels, report kinds, confirmation action names.
- Create `src/Pim.Core/Audit/AuditVersionDtos.cs`: object timeline, restore preview, restore apply, audit export DTOs.
- Modify `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`: enforce L2/L3/L4 gating and second-level/strict confirmation.
- Modify `src/Pim.Infrastructure/Operations/AuditLogService.cs`: keep general operation logs and link them to audit versions.
- Create `src/Pim.Infrastructure/Audit/AuditVersionService.cs`: immutable before/after persistence, timeline, export, restore preview/apply.
- Create `src/Pim.Infrastructure/Audit/AuditVersionEntity.cs`: persisted audit versions.
- Modify `src/Pim.Infrastructure/Data/PimDbContext.cs`: register all new entities.
- Create EF migrations under `src/Pim.Infrastructure/Data/Migrations/`: schema for projects, task books, checklist items, habits, reminders, reports, audit versions, sync items, sync conflict records, endpoint notification actions.

### Calendar, Planning, Outlook, Reminders, Reports

- Modify `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`: domain/project, task book, parent task, review reason, state reason, source metadata.
- Modify `src/modules/Pim.Module.Calendar/Entities/TaskExecutionSegmentEntity.cs`: status, source, confirmation id, audit version id, availability/placeholder links.
- Create `src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/SyncConnectionEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/SyncItemEntity.cs`.
- Create `src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs`.
- Modify `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`: align with `SyncConnectionEntity` and encrypted token references.
- Modify `src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs`: add detailed metrics, steps, next/delta link snapshots, writeback counts.
- Modify `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`: configure relationships, indexes, uniqueness, and soft delete filters.
- Modify `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`: keep current DTOs and add complete planning/sync/reminder/report/data-center contracts.
- Modify `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`: enforce task/event/habit invariants, segments, placeholders, availability, task hierarchy, and confirmation creation.
- Modify `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`: route all important fact changes through confirmation requests.
- Modify `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`: ensure L4 strict confirmation and audit versions for destructive operations.
- Modify `src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs`: audit-backed restore preview/apply.
- Modify `src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs`: global search, sync batches, audit versions, reports, reminders, habits, source ids, Graph ids, batch previews.
- Modify `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`: real Microsoft Graph flow.
- Create `src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs`: device-code/token/delta/event REST adapter.
- Create `src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs`: encrypted token storage, refresh, health, reconnect.
- Create `src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs`: conflict detection, manual resolution, writeback decisions.
- Create `src/modules/Pim.Module.Calendar/Services/ReminderService.cs`: strategies, channels, DND, delivery, actions.
- Create `src/modules/Pim.Module.Calendar/Services/ReportService.cs`: daily/weekly/monthly/project report generation, suggestions, confirmation creation.
- Modify `src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs` and `IcsService.cs`: keep ICS import/export and connect audit/import reports.
- Modify `src/modules/Pim.Module.Calendar/CalendarModule.cs`: expose all endpoints.
- Modify `src/Pim.Api/Today/TodaySectionProviders.cs`: complete Today command center sections.
- Modify `src/Pim.Api/Endpoints/OperationsEndpoints.cs`: detail, allowed action, second-level/strict confirm, reject, audit links.

### Web

- Modify `src/client-web/src/types/index.ts`: complete planning, reminders, reports, audit, Outlook, Windows, Android, data-center types.
- Modify `src/client-web/src/api/calendar.ts`: all planning/calendar/habit/reminder/report/data-center/sync endpoints.
- Modify `src/client-web/src/api/operations.ts`: confirmation detail, strict/second-level confirm, reject, audit timeline/export links.
- Create `src/client-web/src/api/endpoints.ts`: Windows/Android status, collection quality, notification action APIs.
- Create `src/client-web/src/i18n/scheduleWorkbench.zh-CN.ts`: Chinese copy for all schedule/task workbench pages.
- Modify `src/client-web/src/layout/AppLayout.tsx` and `Sidebar.tsx`: complete navigation with Chinese labels.
- Modify `src/client-web/src/pages/TodayPage.tsx`: full command center.
- Modify `src/client-web/src/pages/CalendarPage.tsx`: all layers, Outlook-only filter, segment/placeholder/habit/availability rendering.
- Modify `src/client-web/src/pages/TaskListPage.tsx`: project/book hierarchy, subtasks, checklists, state/reason, segments, audit, reminders/reports links.
- Modify `src/client-web/src/pages/WorkbenchPage.tsx`: Chinese operations dashboard.
- Modify `src/client-web/src/pages/SyncPage.tsx`: Graph settings, device code, token health, sync logs, conflicts, source batch management.
- Modify `src/client-web/src/pages/DataCenterPage.tsx`: global governance surface.
- Modify `src/client-web/src/pages/ConfirmationsPage.tsx`: before/after diff, second-level, strict confirmation, action choices.
- Modify `src/client-web/src/pages/RemindersPage.tsx`: real reminder center.
- Modify `src/client-web/src/pages/ReportsPage.tsx`: real report center.
- Modify `src/client-web/src/pages/HabitsPage.tsx`: real habit/routine center.
- Create `src/client-web/src/pages/AuditTimelinePage.tsx`: object timeline, restore/export.
- Create `src/client-web/src/pages/EndpointShellPage.tsx`: Windows/Android embedded Web landing/status.
- Modify `src/client-web/src/index.css`: visual polish, responsive constraints, no overlapping, no English-only new UI.

### Windows

- Modify `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`: WebView2 and Toast dependencies.
- Modify `src/client-windows/Pim.Client.App/App.xaml` and `App.xaml.cs`: route startup into companion shell.
- Modify `src/client-windows/Pim.Client.App/TrayIcon.cs`: tray audit center, open Web workbench, notification status.
- Create `src/client-windows/Pim.Client.App/MainShellWindow.xaml`.
- Create `src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs`.
- Create `src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs`: WebView2 navigation, auth token injection, server URL handling.
- Create `src/client-windows/Pim.Client.App/NotificationActionRouter.cs`: low-risk direct actions, high-risk open Web audit detail.
- Modify `src/client-windows/Pim.Client.App/StatusWindow.xaml` and `.cs`: polished status/account/server controls.
- Modify `src/client-windows/Pim.Client.Core/Services/ApiClient.cs`: endpoint status, notification actions, confirmation detail URLs.
- Create `src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs`: cache-only collection upload queue and online-only fact change guard.
- Create tests under `tests/Pim.UnitTests/ClientWindows`.

### Android

- Modify `src/client-android/settings.gradle.kts`: include any missing modules.
- Modify `src/client-android/app/build.gradle.kts`: WebView, WorkManager, notification, test dependencies.
- Modify `src/client-android/app/src/main/AndroidManifest.xml`: permissions, notification, foreground/upload workers.
- Create `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt`: native shell.
- Create `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`: embedded Web workbench.
- Create `src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt`.
- Create `src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt`.
- Create `src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt`.
- Create `src/client-android/app/src/main/java/com/pim/app/sync/EndpointUploadWorker.kt`.
- Create `src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt`.
- Modify existing Android calendar/task/search screens to route complex modules into WebView and keep native collection/status surfaces.
- Create Android tests under `src/client-android/app/src/test/java/com/pim/app/schedule/`.

### Tests, Verification, CI

- Modify `tests/Pim.UnitTests/Pim.UnitTests.csproj`: include new test files.
- Create/modify tests under `tests/Pim.UnitTests/Calendar`, `Operations`, `ClientWindows`, `Mobile`.
- Modify `tests/client-web/tsconfig.schedule-workbench.json`.
- Create `tests/client-web/scheduleWorkbenchCompletionTypes.test.ts`.
- Create `tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts`.
- Create `tests/client-web/scheduleWorkbenchLocalization.test.ts`.
- Create `tests/client-web/scheduleWorkbenchInteractions.test.tsx`.
- Create `tests/client-web/scheduleWorkbenchScreenshots.test.ts`.
- Modify `src/client-web/package.json`: add `test:schedule-workbench-complete`.
- Modify `.github/workflows/build-api.yml`, `build-web.yml`, `build-windows.yml`, `build-android.yml`: ensure workflow_dispatch and branch validation cover this branch.
- Modify `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`: replace foundation-only status with full completion status after implementation.
- Create `docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md`: final requirement evidence.

---

## Task 1: Baseline, Foundation Parity, And Contract Lock

**Files:**
- Modify: `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`
- Modify: `docs/superpowers/plans/2026-07-08-schedule-task-workbench-foundation.md`
- Create: `docs/superpowers/reports/2026-07-08-schedule-task-baseline-audit.md`
- Test: `tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs`
- Test: `tests/client-web/scheduleWorkbenchFoundationParity.test.ts`

- [ ] **Step 1: Write backend parity test**

Create `tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Core.Operations;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.UnitTests.Calendar;

public class ScheduleWorkbenchFoundationParityTests
{
    [Fact]
    public void FoundationRiskLevelsAndEntitiesRemainPresent()
    {
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L0AutomaticArtifact"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L1LowRiskAction"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L2PimFactChange"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L3ExternalSourceOrWriteback"));
        Assert.True(Enum.IsDefined(typeof(OperationRiskLevel), "L4BatchOrDestructiveGovernance"));

        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString("N"))
            .Options;
        using var db = new PimDbContext(options);

        Assert.NotNull(db.Model.FindEntityType(typeof(TaskExecutionSegmentEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(OutlookSyncBatchEntity)));
    }
}
```

- [ ] **Step 2: Write Web parity test**

Create `tests/client-web/scheduleWorkbenchFoundationParity.test.ts`:

```ts
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const appLayout = readFileSync('src/client-web/src/layout/AppLayout.tsx', 'utf8');
const sidebar = readFileSync('src/client-web/src/layout/Sidebar.tsx', 'utf8');
const types = readFileSync('src/client-web/src/types/index.ts', 'utf8');

for (const route of ['/workbench', '/sync', '/data-center', '/confirmations', '/reminders', '/reports', '/habits']) {
  assert.match(appLayout, new RegExp(route.replace('/', '\\/')));
  assert.match(sidebar, new RegExp(route.replace('/', '\\/')));
}

for (const symbol of ['OperationConfirmation', 'CalendarLayerResponse', 'OutlookSyncBatchResponse', 'DataCenterQueryResponse']) {
  assert.match(types, new RegExp(`interface ${symbol}|type ${symbol}`));
}
```

- [ ] **Step 3: Run parity tests and verify current state**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ScheduleWorkbenchFoundationParityTests
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchFoundationParity.test.ts
```

Expected: both PASS on a foundation-compatible master, or FAIL showing exactly which foundation pieces must be restored before Task 2.

- [ ] **Step 4: Restore missing foundation pieces when a parity test fails**

If a test fails, restore the missing file(s) from the existing implementation branch:

```powershell
git checkout origin/codex/schedule-task-workbench -- src/Pim.Core/Operations/OperationEnums.cs
git checkout origin/codex/schedule-task-workbench -- src/Pim.Core/Operations/ConfirmationDtos.cs
git checkout origin/codex/schedule-task-workbench -- src/modules/Pim.Module.Calendar
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/api/calendar.ts
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/api/operations.ts
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/WorkbenchPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/SyncPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/DataCenterPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/ConfirmationsPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/RemindersPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/ReportsPage.tsx
git checkout origin/codex/schedule-task-workbench -- src/client-web/src/pages/HabitsPage.tsx
git checkout origin/codex/schedule-task-workbench -- tests/Pim.UnitTests/Calendar
git checkout origin/codex/schedule-task-workbench -- tests/Pim.UnitTests/Operations/ScheduleWorkbenchConfirmationContractTests.cs
git checkout origin/codex/schedule-task-workbench -- tests/client-web/scheduleWorkbenchApiPath.test.ts
git checkout origin/codex/schedule-task-workbench -- tests/client-web/scheduleWorkbenchTypes.test.ts
```

Resolve conflicts by preserving latest `master` behavior and keeping the schedule workbench contract tests green.

- [ ] **Step 5: Add baseline audit report**

Create `docs/superpowers/reports/2026-07-08-schedule-task-baseline-audit.md` with this exact structure:

```markdown
# Schedule Task Baseline Audit

## Branch

- Implementation branch: codex/schedule-task-complete-system
- Base: latest origin/master at task start

## Foundation Parity

- Risk levels: verified by ScheduleWorkbenchFoundationParityTests
- Task execution segments: verified by ScheduleWorkbenchFoundationParityTests
- Outlook sync batch entity: verified by ScheduleWorkbenchFoundationParityTests
- Web routes and typed contracts: verified by scheduleWorkbenchFoundationParity.test.ts

## No Deferred Scope

The old foundation plan deferred Graph token/refresh, delta writeback, audit restore/export, Windows WebView2, Android WebView, and native notification actions. This completion plan implements those items in Tasks 5, 6, 9, 14, 15, and 16.
```

- [ ] **Step 6: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ScheduleWorkbenchFoundationParityTests|FullyQualifiedName~ScheduleWorkbenchConfirmationContractTests|FullyQualifiedName~TaskExecutionSegmentServiceTests"
npm --prefix src/client-web run test:schedule-workbench
git diff --check
```

Expected: PASS, PASS, no diff-check output.

Commit:

```powershell
git add docs/superpowers/reports/2026-07-08-schedule-task-baseline-audit.md tests/Pim.UnitTests/Calendar/ScheduleWorkbenchFoundationParityTests.cs tests/client-web/scheduleWorkbenchFoundationParity.test.ts src docs tests
git commit -m "test: lock schedule workbench foundation parity"
```

---

## Task 2: Complete Shared Planning Object Model

**Files:**
- Create: `src/Pim.Core/Planning/PlanningEnums.cs`
- Create: `src/Pim.Core/Planning/PlanningDtos.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/DomainProjectEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/TaskBookEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/TaskChecklistItemEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/HabitRoutineEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/HabitOccurrenceEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/AvailabilityWindowEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/AiPlanningPlaceholderEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`
- Modify: `src/Pim.Infrastructure/Data/PimDbContext.cs`
- Test: `tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs`

- [ ] **Step 1: Write failing model test**

Create `tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.UnitTests.Calendar;

public class PlanningObjectModelTests
{
    [Fact]
    public void ModelContainsAllApprovedPlanningObjects()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString("N"))
            .Options;
        using var db = new PimDbContext(options);

        Assert.NotNull(db.Model.FindEntityType(typeof(DomainProjectEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(TaskBookEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(TaskChecklistItemEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(HabitRoutineEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(HabitOccurrenceEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(AvailabilityWindowEntity)));
        Assert.NotNull(db.Model.FindEntityType(typeof(AiPlanningPlaceholderEntity)));
    }

    [Fact]
    public void TaskHasProjectBookHierarchyStateAndReviewMetadata()
    {
        var type = typeof(TaskEntity);
        Assert.NotNull(type.GetProperty("DomainProjectId"));
        Assert.NotNull(type.GetProperty("TaskBookId"));
        Assert.NotNull(type.GetProperty("ParentTaskId"));
        Assert.NotNull(type.GetProperty("StateReason"));
        Assert.NotNull(type.GetProperty("ReviewOutcome"));
        Assert.NotNull(type.GetProperty("Source"));
    }
}
```

- [ ] **Step 2: Run test and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PlanningObjectModelTests
```

Expected: FAIL because the complete object model is not present.

- [ ] **Step 3: Add shared enums**

Create `src/Pim.Core/Planning/PlanningEnums.cs`:

```csharp
namespace Pim.Core.Planning;

public enum TaskPlanningState
{
    Inbox,
    ToPlan,
    Planned,
    InProgress,
    Waiting,
    Blocked,
    Deferred,
    Paused,
    Completed,
    Cancelled
}

public enum TaskSegmentStatus { Planned, Active, Paused, Completed, Cancelled }
public enum HabitCadence { Daily, Weekly, Monthly, Custom }
public enum ReminderChannel { Web, WindowsToast, AndroidNotification, Email }
public enum ReminderStatus { Open, Snoozed, Sent, Acknowledged, Dismissed, Failed }
public enum ReportKind { Daily, Weekly, Monthly, Project }
public enum PlanningSource { Manual, Pim, Outlook, Ai, Template, Import }
```

- [ ] **Step 4: Add shared DTO contracts**

Create `src/Pim.Core/Planning/PlanningDtos.cs`:

```csharp
namespace Pim.Core.Planning;

public sealed record DomainProjectDto(Guid Id, string Name, string? Description, string Status);
public sealed record TaskBookDto(Guid Id, Guid? DomainProjectId, string Name, string Kind, string Status);
public sealed record TaskChecklistItemDto(Guid Id, Guid TaskId, string Title, bool IsDone, int SortOrder);
public sealed record HabitRoutineDto(Guid Id, string Title, HabitCadence Cadence, string Source, string Status);
public sealed record HabitOccurrenceDto(Guid Id, Guid HabitRoutineId, DateTimeOffset StartsAt, DateTimeOffset EndsAt, string Status);
public sealed record AvailabilityWindowDto(Guid Id, DateTimeOffset StartsAt, DateTimeOffset EndsAt, string Kind, string Source);
public sealed record AiPlanningPlaceholderDto(Guid Id, string Title, DateTimeOffset StartsAt, DateTimeOffset EndsAt, string Reason, Guid? ConfirmationId);
```

- [ ] **Step 5: Add entities and configure them**

Implement each entity with `Id`, `UserId`, `CreatedAt`, `UpdatedAt`, `DeletedAt`, source/status fields, and relationships matching the DTOs.

Add these required indexes in `CalendarEntityConfigurations.cs`:

```csharp
builder.Entity<DomainProjectEntity>().HasIndex(x => new { x.UserId, x.Name }).IsUnique();
builder.Entity<TaskBookEntity>().HasIndex(x => new { x.UserId, x.Name, x.DomainProjectId });
builder.Entity<TaskChecklistItemEntity>().HasIndex(x => new { x.TaskId, x.SortOrder });
builder.Entity<HabitRoutineEntity>().HasIndex(x => new { x.UserId, x.Status });
builder.Entity<HabitOccurrenceEntity>().HasIndex(x => new { x.UserId, x.StartsAt, x.EndsAt });
builder.Entity<AvailabilityWindowEntity>().HasIndex(x => new { x.UserId, x.StartsAt, x.EndsAt });
builder.Entity<AiPlanningPlaceholderEntity>().HasIndex(x => new { x.UserId, x.StartsAt, x.EndsAt });
```

- [ ] **Step 6: Extend TaskEntity**

Add these properties to `src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs`:

```csharp
public Guid? DomainProjectId { get; set; }
public Guid? TaskBookId { get; set; }
public Guid? ParentTaskId { get; set; }
public string Source { get; set; } = "manual";
public string StateReason { get; set; } = string.Empty;
public string ReviewOutcome { get; set; } = string.Empty;
public DomainProjectEntity? DomainProject { get; set; }
public TaskBookEntity? TaskBook { get; set; }
public TaskEntity? ParentTask { get; set; }
public ICollection<TaskEntity> Subtasks { get; set; } = new List<TaskEntity>();
public ICollection<TaskChecklistItemEntity> ChecklistItems { get; set; } = new List<TaskChecklistItemEntity>();
```

- [ ] **Step 7: Create migration**

Run:

```powershell
dotnet ef migrations add CompletePlanningObjectModel --project src/Pim.Infrastructure --startup-project src/Pim.Api --output-dir Data/Migrations
```

If `dotnet ef` is unavailable, create a hand-written migration that creates the tables and indexes listed above.

- [ ] **Step 8: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~PlanningObjectModelTests
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~CalendarStage5ModelTests
```

Expected: PASS.

Commit:

```powershell
git add src/Pim.Core/Planning src/modules/Pim.Module.Calendar/Entities src/modules/Pim.Module.Calendar/DTOs src/Pim.Infrastructure/Data tests/Pim.UnitTests/Calendar/PlanningObjectModelTests.cs
git commit -m "feat: complete planning object model"
```

---

## Task 3: Confirmation And Audit Enforcement For All Important Fact Changes

**Files:**
- Create: `src/Pim.Core/Audit/AuditVersionDtos.cs`
- Create: `src/Pim.Infrastructure/Audit/AuditVersionEntity.cs`
- Create: `src/Pim.Infrastructure/Audit/AuditVersionService.cs`
- Modify: `src/Pim.Infrastructure/Operations/OperationConfirmationService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarDeleteService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs`
- Modify: `src/Pim.Api/Endpoints/OperationsEndpoints.cs`
- Test: `tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs`
- Test: `tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs`

- [ ] **Step 1: Write failing confirmation gate tests**

Create `tests/Pim.UnitTests/Operations/ScheduleFactConfirmationGateTests.cs`:

```csharp
namespace Pim.UnitTests.Operations;

public class ScheduleFactConfirmationGateTests
{
    [Theory]
    [InlineData("title")]
    [InlineData("dtStart")]
    [InlineData("dtEnd")]
    [InlineData("location")]
    [InlineData("status")]
    [InlineData("recurrence")]
    [InlineData("delete")]
    [InlineData("restore")]
    public async Task ImportantScheduleFactChangesCreatePendingConfirmation(string field)
    {
        using var scope = CalendarTestHost.CreateScope();
        var scenario = await CalendarTestHost.SeedEventAsync(scope, source: "manual");

        var result = await CalendarTestHost.RequestEventFactChangeAsync(scope, scenario.EventId, field);

        Assert.Equal("PendingConfirmation", result.Kind);
        Assert.Equal("L2PimFactChange", result.RiskLevel);
        Assert.Contains(field, result.ChangedFields);
    }

    [Fact]
    public async Task OutlookOriginCoreFactChangeRequiresSecondLevelConfirmation()
    {
        using var scope = CalendarTestHost.CreateScope();
        var scenario = await CalendarTestHost.SeedEventAsync(scope, source: "outlook");

        var result = await CalendarTestHost.RequestEventFactChangeAsync(scope, scenario.EventId, "location");

        Assert.Equal("L3ExternalSourceOrWriteback", result.RiskLevel);
        Assert.True(result.RequiresSecondLevelConfirmation);
    }
}
```

- [ ] **Step 2: Write failing audit tests**

Create `tests/Pim.UnitTests/Operations/AuditVersionServiceTests.cs`:

```csharp
namespace Pim.UnitTests.Operations;

public class AuditVersionServiceTests
{
    [Fact]
    public async Task AcceptedFactChangeWritesBeforeAfterAuditVersion()
    {
        using var scope = CalendarTestHost.CreateScope();
        var scenario = await CalendarTestHost.SeedEventAsync(scope, source: "manual", title: "Before");

        var confirmation = await CalendarTestHost.RequestEventTitleChangeAsync(scope, scenario.EventId, "After");
        await CalendarTestHost.ConfirmAndExecuteAsync(scope, confirmation.Id);

        var timeline = await CalendarTestHost.GetAuditTimelineAsync(scope, "event", scenario.EventId);
        var item = Assert.Single(timeline.Items);
        Assert.Contains("\"title\":\"Before\"", item.BeforeJson);
        Assert.Contains("\"title\":\"After\"", item.AfterJson);
        Assert.Equal(confirmation.Id, item.ConfirmationId);
    }
}
```

- [ ] **Step 3: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ScheduleFactConfirmationGateTests|FullyQualifiedName~AuditVersionServiceTests"
```

Expected: FAIL because direct fact changes are not all gated and audit versions are incomplete.

- [ ] **Step 4: Add audit DTOs and entity**

Create `src/Pim.Core/Audit/AuditVersionDtos.cs`:

```csharp
namespace Pim.Core.Audit;

public sealed record AuditVersionDto(
    Guid Id,
    string ObjectType,
    Guid ObjectId,
    Guid? ConfirmationId,
    string Source,
    string Actor,
    string BeforeJson,
    string AfterJson,
    string ChangedFieldsJson,
    DateTimeOffset CreatedAt);

public sealed record AuditTimelineResponse(IReadOnlyList<AuditVersionDto> Items);
public sealed record RestorePreviewResponse(string ObjectType, Guid ObjectId, string Summary, bool RequiresConfirmation, IReadOnlyList<string> ChangedFields);
public sealed record AuditExportResponse(string FileName, string ContentType, string Content);
```

Create `AuditVersionEntity` with matching columns and indexes:

```csharp
builder.Entity<AuditVersionEntity>().HasIndex(x => new { x.ObjectType, x.ObjectId, x.CreatedAt });
builder.Entity<AuditVersionEntity>().HasIndex(x => x.ConfirmationId);
```

- [ ] **Step 5: Implement audit service**

Create `AuditVersionService` with these methods:

```csharp
Task<AuditVersionDto> RecordAsync(string objectType, Guid objectId, object before, object after, IReadOnlyList<string> changedFields, Guid? confirmationId, string source, CancellationToken ct);
Task<AuditTimelineResponse> GetTimelineAsync(string objectType, Guid objectId, CancellationToken ct);
Task<RestorePreviewResponse> PreviewRestoreAsync(Guid auditVersionId, CancellationToken ct);
Task<AuditVersionDto> ApplyRestoreAsync(Guid auditVersionId, Guid confirmationId, CancellationToken ct);
Task<AuditExportResponse> ExportAsync(DateTimeOffset start, DateTimeOffset end, CancellationToken ct);
```

- [ ] **Step 6: Gate all important fact changes**

Route these changes through `OperationConfirmationService.CreateAsync` before persistence:

```text
event.title
event.dtStart
event.dtEnd
event.location
event.status
event.rrule
event.delete
event.restore
task.title
task.dtStart
task.due
task.plannedEnd
task.status
task.project/book
task.segment.start/end/status/delete
habit.title/cadence/status/rule/delete
sync.stop
outlook.writeback
batch.delete/restore
```

Use:

```csharp
var risk = source == "outlook"
    ? OperationRiskLevel.L3ExternalSourceOrWriteback
    : OperationRiskLevel.L2PimFactChange;
```

Set `RequiresSecondLevelConfirmation = risk == OperationRiskLevel.L3ExternalSourceOrWriteback`.

Set `OperationRiskLevel.L4BatchOrDestructiveGovernance` for batch delete, stop sync, book delete with children, recurrence-wide delete, and bulk writeback.

- [ ] **Step 7: Extend operations endpoints**

Add endpoints:

```csharp
group.MapGet("/confirmations/{id:guid}", GetConfirmationDetail);
group.MapPost("/confirmations/{id:guid}/confirm-second-level", ConfirmSecondLevel);
group.MapPost("/confirmations/{id:guid}/confirm-strict", ConfirmStrict);
group.MapGet("/audit/{objectType}/{objectId:guid}", GetAuditTimeline);
group.MapPost("/audit/{auditVersionId:guid}/restore-preview", PreviewAuditRestore);
group.MapPost("/audit/{auditVersionId:guid}/restore", ApplyAuditRestore);
group.MapGet("/audit/export", ExportAudit);
```

- [ ] **Step 8: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ScheduleFactConfirmationGateTests|FullyQualifiedName~AuditVersionServiceTests|FullyQualifiedName~CalendarDeleteServiceTests|FullyQualifiedName~CalendarRecycleBinServiceTests"
dotnet test Pim.sln
```

Expected: PASS.

Commit:

```powershell
git add src/Pim.Core/Audit src/Pim.Infrastructure/Audit src/Pim.Infrastructure/Data src/Pim.Infrastructure/Operations src/modules/Pim.Module.Calendar src/Pim.Api/Endpoints tests/Pim.UnitTests/Operations
git commit -m "feat: enforce schedule fact confirmations and audit versions"
```

---

## Task 4: Complete Planning Services, Layers, Tasks, Habits, Availability, And AI Placeholders

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `src/Pim.Api/Today/TodaySectionProviders.cs`
- Test: `tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs`
- Test: `tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs`

- [ ] **Step 1: Write failing service tests**

Create `tests/Pim.UnitTests/Calendar/PlanningModelServiceCompletionTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class PlanningModelServiceCompletionTests
{
    [Fact]
    public async Task CalendarLayersReturnEventsSegmentsHabitsAvailabilityAndAiPlaceholders()
    {
        using var scope = CalendarTestHost.CreateScope();
        await CalendarTestHost.SeedLayerFixtureAsync(scope);
        var service = scope.GetRequiredService<PlanningModelService>();

        var result = await service.GetCalendarLayersAsync(CalendarTestHost.TodayLayerQuery(
            "events", "task-segments", "habits", "availability", "ai-placeholders"), CancellationToken.None);

        Assert.Contains(result.Items, x => x.Layer == "events");
        Assert.Contains(result.Items, x => x.Layer == "task-segments");
        Assert.Contains(result.Items, x => x.Layer == "habits");
        Assert.Contains(result.Items, x => x.Layer == "availability");
        Assert.Contains(result.Items, x => x.Layer == "ai-placeholders" && x.RequiresConfirmation);
    }

    [Fact]
    public async Task BasicTaskCanHaveMultipleNonOverlappingSegments()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<PlanningModelService>();
        var task = await CalendarTestHost.CreateTaskAsync(scope, "Write plan");

        var first = await CalendarTestHost.CreateSegmentAsync(scope, task.Id, "2026-07-08T09:00:00Z", "2026-07-08T10:00:00Z");
        var second = await CalendarTestHost.CreateSegmentAsync(scope, task.Id, "2026-07-08T14:00:00Z", "2026-07-08T15:00:00Z");

        Assert.NotEqual(first.Id, second.Id);
    }
}
```

- [ ] **Step 2: Write failing Today section test**

Create `tests/Pim.UnitTests/Today/TodayScheduleWorkbenchSectionTests.cs`:

```csharp
namespace Pim.UnitTests.Today;

public class TodayScheduleWorkbenchSectionTests
{
    [Fact]
    public async Task TodayRegistryIncludesScheduleWorkbenchSections()
    {
        using var scope = TodayTestHost.CreateScope();
        var registry = await TodayTestHost.GetRegistryAsync(scope, new DateOnly(2026, 7, 8));

        Assert.Contains(registry.Sections, x => x.Kind == "calendar.schedule");
        Assert.Contains(registry.Sections, x => x.Kind == "calendar.tasks");
        Assert.Contains(registry.Sections, x => x.Kind == "calendar.habits");
        Assert.Contains(registry.Sections, x => x.Kind == "operations.confirmations");
        Assert.Contains(registry.Sections, x => x.Kind == "reports.available");
        Assert.Contains(registry.Sections, x => x.Kind == "endpoints.status");
    }
}
```

- [ ] **Step 3: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~PlanningModelServiceCompletionTests|FullyQualifiedName~TodayScheduleWorkbenchSectionTests"
```

Expected: FAIL until all layer sources and Today sections are implemented.

- [ ] **Step 4: Complete DTOs and endpoints**

Add endpoint contracts:

```csharp
group.MapGet("/projects", ListProjects);
group.MapPost("/projects", CreateProject);
group.MapGet("/task-books", ListTaskBooks);
group.MapPost("/task-books", CreateTaskBook);
group.MapPost("/tasks/{id:guid}/checklist", AddChecklistItem);
group.MapGet("/habits", ListHabits);
group.MapPost("/habits", CreateHabit);
group.MapPost("/habits/{id:guid}/occurrences", CreateHabitOccurrence);
group.MapGet("/availability", ListAvailability);
group.MapPost("/availability", CreateAvailabilityWindow);
group.MapPost("/ai-placeholders", CreateAiPlaceholder);
group.MapPost("/ai-placeholders/{id:guid}/confirm", ConfirmAiPlaceholder);
```

- [ ] **Step 5: Implement layer projections**

`PlanningModelService.GetCalendarLayersAsync` must include:

```text
events -> CalendarEvent
task-segments -> TaskExecutionSegment
habits -> HabitOccurrence
availability -> AvailabilityWindow
ai-placeholders -> AiPlanningPlaceholder with RequiresConfirmation = true
```

AI placeholders never become planned facts directly. `ConfirmAiPlaceholder` creates an L2 confirmation, and execution creates either a `TaskExecutionSegment` or `CalendarEvent` after confirmation.

- [ ] **Step 6: Complete Today sections**

Update `TodaySectionProviders.cs` to return these section kinds:

```text
calendar.schedule
calendar.tasks
calendar.habits
calendar.availability
calendar.ai_placeholders
operations.confirmations
sync.outlook
reminders.queue
reports.available
endpoints.status
pc.activity
pc.quality
pc.classification_suggestions
```

- [ ] **Step 7: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~PlanningModelServiceCompletionTests|FullyQualifiedName~TodayScheduleWorkbenchSectionTests|FullyQualifiedName~CalendarTaskPlanningTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar src/Pim.Api/Today tests/Pim.UnitTests/Calendar tests/Pim.UnitTests/Today
git commit -m "feat: complete planning layers and today sections"
```

---

## Task 5: Microsoft Graph Device Code, Token, Delta, And Writeback Execution

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/MicrosoftGraphDeviceCodeClient.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookGraphModels.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/OutlookSyncBatchEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/SyncConnectionEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/SyncItemEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs`

- [ ] **Step 1: Write failing device-code tests**

Create `tests/Pim.UnitTests/Calendar/OutlookGraphDeviceCodeFlowTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class OutlookGraphDeviceCodeFlowTests
{
    [Fact]
    public async Task DeviceCodeFlowStoresEncryptedTokensAndUpdatesConnectionHealth()
    {
        using var scope = CalendarTestHost.CreateScopeWithGraphFixture(
            deviceCodeJson: GraphFixtures.DeviceCodeSuccess,
            tokenJson: GraphFixtures.TokenSuccess);
        var service = scope.GetRequiredService<OutlookSyncService>();

        var code = await service.CreateDeviceCodeRequestAsync(CalendarTestHost.UserId, CancellationToken.None);
        var result = await service.PollDeviceCodeAsync(CalendarTestHost.UserId, code.DeviceCode, CancellationToken.None);

        Assert.Equal("Connected", result.Status);
        Assert.Equal("Healthy", result.TokenHealth);
        Assert.DoesNotContain("access-token", result.StoredTokenSecret);
        Assert.Contains("Calendars.ReadWrite", result.Scopes);
    }
}
```

- [ ] **Step 2: Write failing delta and writeback tests**

Create `tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class OutlookGraphDeltaSyncTests
{
    [Fact]
    public async Task DeltaSyncFollowsNextLinkAndStoresDeltaLink()
    {
        using var scope = CalendarTestHost.CreateScopeWithGraphFixture(
            deltaPages: [GraphFixtures.EventDeltaPage1WithNextLink, GraphFixtures.EventDeltaPage2WithDeltaLink]);
        var service = scope.GetRequiredService<OutlookSyncService>();

        var batch = await service.SyncAsync(CalendarTestHost.UserId, CancellationToken.None);

        Assert.Equal(2, batch.ReadCount);
        Assert.Contains(batch.Steps, x => x.Name == "Follow nextLink");
        Assert.Contains(batch.Steps, x => x.Name == "Store deltaLink");
    }

    [Fact]
    public async Task OutlookCoreDiffCreatesL3ConfirmationBeforeLocalMutation()
    {
        using var scope = CalendarTestHost.CreateScopeWithGraphFixture(
            deltaPages: [GraphFixtures.LocationChangedDelta]);
        var service = scope.GetRequiredService<OutlookSyncService>();

        var batch = await service.SyncAsync(CalendarTestHost.UserId, CancellationToken.None);

        Assert.Equal(1, batch.ConfirmationCount);
        Assert.Equal(0, batch.UpdatedCount);
    }
}
```

Create `tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class OutlookGraphWritebackTests
{
    [Fact]
    public async Task ConfirmedWritebackPatchesGraphWithChangeKeyAndRecordsAudit()
    {
        using var scope = CalendarTestHost.CreateScopeWithGraphFixture(
            patchResponseJson: GraphFixtures.EventPatchSuccess);
        var scenario = await CalendarTestHost.SeedOutlookEventWithPendingWritebackAsync(scope);

        await CalendarTestHost.ConfirmSecondLevelAndExecuteAsync(scope, scenario.ConfirmationId);

        var graph = scope.GetRequiredService<FakeMicrosoftGraphClient>();
        Assert.Contains(graph.PatchRequests, x => x.Url.Contains("/me/events/") && x.Body.Contains("location"));
        var audit = await CalendarTestHost.GetAuditTimelineAsync(scope, "event", scenario.EventId);
        Assert.NotEmpty(audit.Items);
    }
}
```

- [ ] **Step 3: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookGraphDeviceCodeFlowTests|FullyQualifiedName~OutlookGraphDeltaSyncTests|FullyQualifiedName~OutlookGraphWritebackTests"
```

Expected: FAIL until real Graph flow is implemented.

- [ ] **Step 4: Implement Microsoft Graph client**

`MicrosoftGraphDeviceCodeClient` must call official endpoints:

```text
POST https://login.microsoftonline.com/{tenant}/oauth2/v2.0/devicecode
POST https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token
GET  https://graph.microsoft.com/v1.0/me/calendarView
GET  nextLink/deltaLink absolute URLs returned by Graph
PATCH https://graph.microsoft.com/v1.0/me/events/{id}
```

Use `HttpClient`, accept an injectable clock, and parse Graph errors into typed results:

```csharp
public interface IMicrosoftGraphClient
{
    Task<DeviceCodeResult> RequestDeviceCodeAsync(string tenant, string clientId, string scopes, CancellationToken ct);
    Task<TokenResult> PollDeviceCodeAsync(string tenant, string clientId, string deviceCode, CancellationToken ct);
    Task<TokenResult> RefreshAsync(string tenant, string clientId, string refreshToken, string scopes, CancellationToken ct);
    Task<GraphDeltaPage> GetDeltaPageAsync(string accessToken, string url, CancellationToken ct);
    Task<GraphEvent> PatchEventAsync(string accessToken, string eventId, string changeKey, object patch, CancellationToken ct);
}
```

- [ ] **Step 5: Implement token service**

`OutlookTokenService` must:

```text
encrypt access token, refresh token, expiry, tenant, client id, scopes
refresh before expiry
mark token health Healthy, Expiring, Expired, Missing, RefreshFailed
clear tokens on disconnect
record audit for connect, reconnect, disconnect
```

Use existing `ISecretProtector`.

- [ ] **Step 6: Implement delta sync**

`OutlookSyncService.SyncAsync` must:

```text
1 Load provider configuration
2 Validate token status and scopes
3 Refresh token when needed
4 Load previous delta link
5 Read calendarView when no delta link exists
6 Follow nextLink until exhausted
7 Store deltaLink
8 Map Graph event id, iCalUId, changeKey, ETag, subject, body, location, start/end timezone, recurrence
9 Create local event for new safe imports
10 Create L3 confirmation for external core diffs
11 Record batch counts and steps
12 Preserve local object when confirmation is pending
```

- [ ] **Step 7: Implement writeback execution**

When an approved confirmation has `operationType` in:

```text
outlook.writeback
outlook.conflict.keep_pim
outlook.conflict.merge
```

call Graph PATCH with current changeKey. If Graph returns precondition/conflict, create a new `SyncConflictEntity` and keep the original confirmation result linked.

- [ ] **Step 8: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookGraphDeviceCodeFlowTests|FullyQualifiedName~OutlookGraphDeltaSyncTests|FullyQualifiedName~OutlookGraphWritebackTests|FullyQualifiedName~OutlookGraphSyncFoundationTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar/Services src/modules/Pim.Module.Calendar/Entities src/modules/Pim.Module.Calendar/CalendarModule.cs src/Pim.Infrastructure/Data tests/Pim.UnitTests/Calendar/OutlookGraph*Tests.cs
git commit -m "feat: execute outlook graph sync and writeback"
```

---

## Task 6: Outlook Conflict Resolution, Source Governance, And ICS Completion

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Services/OutlookConflictService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/IcsService.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs`

- [ ] **Step 1: Write failing conflict tests**

Create `tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class OutlookConflictResolutionTests
{
    [Theory]
    [InlineData("keep_pim")]
    [InlineData("keep_outlook")]
    [InlineData("merge_by_field")]
    [InlineData("create_merge_copy")]
    [InlineData("skip_batch")]
    [InlineData("stop_sync")]
    public async Task ManualConflictActionsCreateExpectedConfirmationRisk(string action)
    {
        using var scope = CalendarTestHost.CreateScopeWithGraphFixture(deltaPages: [GraphFixtures.BothSidesChangedLocation]);
        var conflict = await CalendarTestHost.CreateOutlookConflictAsync(scope);

        var confirmation = await CalendarTestHost.RequestConflictActionAsync(scope, conflict.Id, action);

        Assert.Contains(action, confirmation.AllowedActions);
        Assert.Equal(action == "stop_sync" ? "L4BatchOrDestructiveGovernance" : "L3ExternalSourceOrWriteback", confirmation.RiskLevel);
        Assert.True(confirmation.RequiresSecondLevelConfirmation || action == "stop_sync");
    }
}
```

- [ ] **Step 2: Write failing source governance and ICS tests**

Create tests asserting:

```csharp
Assert.Contains(outlookOnly.Items, x => x.Source == "outlook");
Assert.DoesNotContain(outlookOnly.Items, x => x.Source == "manual");
Assert.Equal("L4BatchOrDestructiveGovernance", stopSyncPreview.RiskLevel);
Assert.Contains("GraphEventId", auditExport.Content);
Assert.Contains("UID:", icsExport.Content);
Assert.Contains(importPreview.Samples, x => x.Reason == "duplicate");
```

- [ ] **Step 3: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookConflictResolutionTests|FullyQualifiedName~OutlookSourceGovernanceTests|FullyQualifiedName~OutlookIcsCompletionTests"
```

Expected: FAIL.

- [ ] **Step 4: Implement conflict service**

`OutlookConflictService` must expose:

```csharp
Task<SyncConflictDetailDto> GetAsync(Guid conflictId, CancellationToken ct);
Task<OperationConfirmationDto> RequestActionAsync(Guid conflictId, ConflictResolutionRequest request, CancellationToken ct);
Task ExecuteConfirmedResolutionAsync(Guid confirmationId, CancellationToken ct);
```

`ConflictResolutionRequest` includes `Action`, `MergedFields`, and `Reason`.

- [ ] **Step 5: Implement source governance**

Add endpoints:

```csharp
group.MapGet("/outlook/events", ListOutlookEvents);
group.MapPost("/outlook/events/batch-tag", BatchTagOutlookEvents);
group.MapPost("/outlook/events/{id:guid}/pause-sync", PauseSync);
group.MapPost("/outlook/events/{id:guid}/stop-sync-preview", StopSyncPreview);
group.MapPost("/outlook/events/{id:guid}/stop-sync", StopSyncConfirmed);
group.MapGet("/outlook/events/{id:guid}/history", GetOutlookHistory);
```

- [ ] **Step 6: Complete ICS behavior**

Keep ICS as secondary exchange path with:

```text
import preview
duplicate detection
skipped reason counts
export selected objects
export date range
audit import report
```

- [ ] **Step 7: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~OutlookConflictResolutionTests|FullyQualifiedName~OutlookSourceGovernanceTests|FullyQualifiedName~OutlookIcsCompletionTests|FullyQualifiedName~OutlookIcsServiceTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar tests/Pim.UnitTests/Calendar/OutlookConflictResolutionTests.cs tests/Pim.UnitTests/Calendar/OutlookSourceGovernanceTests.cs tests/Pim.UnitTests/Calendar/OutlookIcsCompletionTests.cs
git commit -m "feat: complete outlook conflict and source governance"
```

---

## Task 7: Reminder Service And Notification Payloads

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/ReminderService.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `src/Pim.Api/Today/TodaySectionProviders.cs`
- Test: `tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs`

- [ ] **Step 1: Write failing reminder tests**

Create `tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class ReminderServiceTests
{
    [Fact]
    public async Task ReminderStoresTriggerRiskChannelsDndHistoryAndRelatedObject()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<ReminderService>();

        var reminder = await service.CreateAsync(new CreateReminderRequest(
            RelatedObjectType: "confirmation",
            RelatedObjectId: Guid.NewGuid(),
            Title: "Review Outlook change",
            TriggerReason: "L3 confirmation waiting",
            RiskLevel: "L3ExternalSourceOrWriteback",
            Channels: ["Web", "WindowsToast", "AndroidNotification"],
            DoNotDisturbStart: "22:00",
            DoNotDisturbEnd: "08:00",
            ScheduledAt: new DateTimeOffset(2026, 7, 8, 9, 0, 0, TimeSpan.Zero)), CancellationToken.None);

        Assert.Equal("Review Outlook change", reminder.Title);
        Assert.Contains("WindowsToast", reminder.Channels);
        Assert.Equal("Open", reminder.Status);
    }

    [Fact]
    public async Task LowRiskActionExecutesAndHighRiskActionReturnsOpenDetail()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<ReminderService>();
        var reminder = await CalendarTestHost.SeedReminderAsync(scope, risk: "L3ExternalSourceOrWriteback");

        var action = await service.HandleActionAsync(reminder.Id, "confirm", CancellationToken.None);

        Assert.Equal("OpenDetailRequired", action.Kind);
        Assert.Contains("/confirmations/", action.DetailUrl);
    }
}
```

- [ ] **Step 2: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~ReminderServiceTests
```

Expected: FAIL.

- [ ] **Step 3: Implement reminder storage and endpoints**

Add endpoints:

```csharp
group.MapGet("/reminders", ListReminders);
group.MapPost("/reminders", CreateReminder);
group.MapPost("/reminders/{id:guid}/snooze", SnoozeReminder);
group.MapPost("/reminders/{id:guid}/dismiss", DismissReminder);
group.MapPost("/reminders/{id:guid}/actions/{action}", HandleReminderAction);
group.MapGet("/reminders/delivery-log", GetDeliveryLog);
```

- [ ] **Step 4: Implement notification payload contract**

Every delivery payload must include:

```json
{
  "reminderId": "guid",
  "title": "string",
  "body": "string",
  "riskLevel": "L0AutomaticArtifact|L1LowRiskAction|L2PimFactChange|L3ExternalSourceOrWriteback|L4BatchOrDestructiveGovernance",
  "relatedObjectType": "string",
  "relatedObjectId": "guid",
  "detailUrl": "/confirmations/{id}",
  "actions": ["open", "snooze", "dismiss"]
}
```

Low-risk actions may execute directly. L2/L3/L4 actions return `OpenDetailRequired`.

- [ ] **Step 5: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ReminderServiceTests|FullyQualifiedName~ReminderNotificationPayloadTests|FullyQualifiedName~TodayScheduleWorkbenchSectionTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar src/Pim.Api/Today tests/Pim.UnitTests/Calendar/Reminder*Tests.cs
git commit -m "feat: add reminder service and notification payloads"
```

---

## Task 8: Report Artifacts, AI Suggestions, And Follow-Up Confirmations

**Files:**
- Create: `src/modules/Pim.Module.Calendar/Entities/ReportArtifactEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Entities/ReportSuggestionEntity.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/ReportService.cs`
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `src/Pim.Api/Today/TodaySectionProviders.cs`
- Test: `tests/Pim.UnitTests/Calendar/ReportServiceTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs`

- [ ] **Step 1: Write failing report tests**

Create `tests/Pim.UnitTests/Calendar/ReportServiceTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class ReportServiceTests
{
    [Theory]
    [InlineData("Daily")]
    [InlineData("Weekly")]
    [InlineData("Monthly")]
    [InlineData("Project")]
    public async Task GeneratesReportArtifactWithoutMutatingFacts(string kind)
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<ReportService>();

        var report = await service.GenerateAsync(new GenerateReportRequest(kind, DateOnly.Parse("2026-07-08"), Guid.Empty), CancellationToken.None);

        Assert.Equal(kind, report.Kind);
        Assert.Equal("L0AutomaticArtifact", report.RiskLevel);
        Assert.NotEmpty(report.ContentMarkdown);
        Assert.Equal(0, await CalendarTestHost.CountFactChangesAsync(scope));
    }
}
```

Create `tests/Pim.UnitTests/Calendar/ReportSuggestionConfirmationTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class ReportSuggestionConfirmationTests
{
    [Fact]
    public async Task ActionableReportSuggestionCreatesConfirmationInsteadOfChangingFacts()
    {
        using var scope = CalendarTestHost.CreateScope();
        var service = scope.GetRequiredService<ReportService>();
        var report = await CalendarTestHost.SeedReportWithSuggestionAsync(scope, "move-task-segment");

        var confirmation = await service.RequestSuggestionActionAsync(report.SuggestionId, CancellationToken.None);

        Assert.Equal("L2PimFactChange", confirmation.RiskLevel);
        Assert.Contains("startsAt", confirmation.ChangedFields);
    }
}
```

- [ ] **Step 2: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ReportServiceTests|FullyQualifiedName~ReportSuggestionConfirmationTests"
```

Expected: FAIL.

- [ ] **Step 3: Implement report service**

Add report generation inputs:

```text
planned vs actual
task completion and state changes
calendar occupancy
Outlook impact
PC collection quality
Android/mobile collection quality
habit completion
reminder response
blockers and delays
AI observations and suggestions
```

Persist report artifacts with `InputsJson`, `MetricsJson`, `ContentMarkdown`, `GeneratedAt`, `Kind`, `ProjectId`, `RiskLevel = L0AutomaticArtifact`.

- [ ] **Step 4: Implement report endpoints**

```csharp
group.MapGet("/reports", ListReports);
group.MapPost("/reports/generate", GenerateReport);
group.MapGet("/reports/{id:guid}", GetReport);
group.MapPost("/reports/{id:guid}/archive", ArchiveReport);
group.MapPost("/reports/suggestions/{id:guid}/request-action", RequestReportSuggestionAction);
```

- [ ] **Step 5: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~ReportServiceTests|FullyQualifiedName~ReportSuggestionConfirmationTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar src/Pim.Api/Today tests/Pim.UnitTests/Calendar/Report*Tests.cs
git commit -m "feat: add report artifacts and suggestion confirmations"
```

---

## Task 9: Full Data Center Governance, Batch Preview, Restore, And Export

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/DataCenterGovernanceService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- Modify: `src/Pim.Infrastructure/Audit/AuditVersionService.cs`
- Test: `tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs`
- Test: `tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs`

- [ ] **Step 1: Write failing coverage tests**

Create `tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs`:

```csharp
namespace Pim.UnitTests.Calendar;

public class DataCenterCoverageTests
{
    [Fact]
    public async Task GlobalSearchCoversAllApprovedObjectTypes()
    {
        using var scope = CalendarTestHost.CreateScope();
        await CalendarTestHost.SeedFullDataCenterFixtureAsync(scope);
        var service = scope.GetRequiredService<DataCenterQueryService>();

        var result = await service.QueryAsync(new DataCenterQueryRequest(null, null, null, false, 1, 200), CancellationToken.None);

        foreach (var type in new[] { "task", "event", "task-segment", "habit", "reminder", "report", "confirmation", "sync-batch", "sync-conflict", "audit-version", "recycle-bin" })
        {
            Assert.Contains(result.Items, x => x.ObjectType == type);
        }
    }
}
```

Create `tests/Pim.UnitTests/Calendar/DataCenterGovernanceTests.cs` with assertions for:

```csharp
Assert.Equal("L4BatchOrDestructiveGovernance", preview.RiskLevel);
Assert.True(preview.RequiresStrictConfirmation);
Assert.Contains("Recoverability", preview.Summary);
Assert.NotEmpty(preview.AffectedObjectTypes);
Assert.Contains("audit-export.json", export.FileName);
```

- [ ] **Step 2: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~DataCenterCoverageTests|FullyQualifiedName~DataCenterGovernanceTests"
```

Expected: FAIL.

- [ ] **Step 3: Implement query coverage**

`DataCenterQueryService` must query:

```text
tasks
events
task execution segments
habits
habit occurrences
availability
AI placeholders
reminders
reminder deliveries
reports
report suggestions
confirmations
sync connections
sync items
sync batches
sync conflicts
audit versions
recycle bin rows
Graph ids and source ids
```

- [ ] **Step 4: Implement governance endpoints**

```csharp
group.MapPost("/data-center/batch/preview", PreviewBatchOperation);
group.MapPost("/data-center/batch/request-confirmation", RequestBatchConfirmation);
group.MapPost("/data-center/batch/execute", ExecuteConfirmedBatch);
group.MapGet("/data-center/audit/export", ExportAudit);
group.MapPost("/data-center/restore/preview", PreviewRestore);
group.MapPost("/data-center/restore/request-confirmation", RequestRestoreConfirmation);
```

- [ ] **Step 5: Verify green and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~DataCenterCoverageTests|FullyQualifiedName~DataCenterGovernanceTests|FullyQualifiedName~AuditVersionServiceTests"
```

Expected: PASS.

Commit:

```powershell
git add src/modules/Pim.Module.Calendar src/Pim.Infrastructure/Audit tests/Pim.UnitTests/Calendar/DataCenter*Tests.cs
git commit -m "feat: complete data center governance"
```

---

## Task 10: Complete Chinese Web Contracts And Localization

**Files:**
- Create: `src/client-web/src/i18n/scheduleWorkbench.zh-CN.ts`
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/api/operations.ts`
- Create: `src/client-web/src/api/endpoints.ts`
- Modify: `src/client-web/package.json`
- Test: `tests/client-web/scheduleWorkbenchCompletionTypes.test.ts`
- Test: `tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts`
- Test: `tests/client-web/scheduleWorkbenchLocalization.test.ts`
- Test: `tests/client-web/tsconfig.schedule-workbench.json`

- [ ] **Step 1: Write failing type/API/localization tests**

Create `tests/client-web/scheduleWorkbenchCompletionTypes.test.ts`:

```ts
import assert from 'node:assert/strict';
import type {
  DomainProject,
  TaskBook,
  TaskChecklistItem,
  HabitRoutine,
  ReminderSummary,
  ReportArtifact,
  AuditVersion,
  SyncConflict,
  EndpointStatus,
} from '../../src/client-web/src/types';

const project: DomainProject = { id: 'p1', name: '项目', description: null, status: 'Active' };
const book: TaskBook = { id: 'b1', domainProjectId: 'p1', name: '任务本', kind: 'task', status: 'Active' };
const checklist: TaskChecklistItem = { id: 'c1', taskId: 't1', title: '检查项', isDone: false, sortOrder: 1 };
const habit: HabitRoutine = { id: 'h1', title: '运动', cadence: 'Daily', source: 'manual', status: 'Active' };
const reminder: ReminderSummary = { id: 'r1', title: '提醒', riskLevel: 'L1LowRiskAction', channels: ['Web'], status: 'Open' };
const report: ReportArtifact = { id: 'rp1', kind: 'Daily', title: '日报', riskLevel: 'L0AutomaticArtifact', generatedAt: '2026-07-08T00:00:00Z' };
const audit: AuditVersion = { id: 'a1', objectType: 'task', objectId: 't1', beforeJson: '{}', afterJson: '{}', changedFields: [], createdAt: '2026-07-08T00:00:00Z' };
const conflict: SyncConflict = { id: 's1', provider: 'outlook', objectType: 'event', objectId: 'e1', changedFields: ['location'], status: 'Pending' };
const endpoint: EndpointStatus = { deviceId: 'win-1', platform: 'windows', uploadStatus: 'Healthy', collectionCacheCount: 0, onlineOnlyBlockedCount: 0 };

assert.equal(project.name, '项目');
void book; void checklist; void habit; void reminder; void report; void audit; void conflict; void endpoint;
```

Create `tests/client-web/scheduleWorkbenchLocalization.test.ts`:

```ts
import assert from 'node:assert/strict';
import { scheduleWorkbenchZhCN } from '../../src/client-web/src/i18n/scheduleWorkbench.zh-CN';

for (const key of [
  'workbench.title',
  'today.title',
  'calendar.layers.events',
  'sync.deviceCode',
  'confirmations.secondLevelRequired',
  'dataCenter.batchPreview',
  'reminders.title',
  'reports.title',
  'habits.title',
  'endpoints.windows',
  'endpoints.android',
]) {
  assert.equal(typeof scheduleWorkbenchZhCN[key], 'string', key);
  assert.equal(/[A-Za-z]{4,}/.test(scheduleWorkbenchZhCN[key]), false, key);
}
```

- [ ] **Step 2: Run tests and verify red**

```powershell
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.schedule-workbench.json
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchLocalization.test.ts
```

Expected: FAIL.

- [ ] **Step 3: Add complete TypeScript contracts**

Add all types used by the tests plus:

```ts
export type TaskPlanningState = 'Inbox' | 'ToPlan' | 'Planned' | 'InProgress' | 'Waiting' | 'Blocked' | 'Deferred' | 'Paused' | 'Completed' | 'Cancelled';
export type CalendarLayerId = 'events' | 'task-segments' | 'habits' | 'availability' | 'ai-placeholders';
export type EndpointPlatform = 'windows' | 'android';
export type NotificationActionResult = 'Executed' | 'OpenDetailRequired' | 'Rejected' | 'Failed';
```

- [ ] **Step 4: Add Chinese localization dictionary**

Create `scheduleWorkbench.zh-CN.ts` with every visible new workbench string. The first required entries are:

```ts
export const scheduleWorkbenchZhCN: Record<string, string> = {
  'workbench.title': '日程任务工作台',
  'today.title': '今日指挥台',
  'calendar.layers.events': '日程',
  'calendar.layers.taskSegments': '任务时间段',
  'calendar.layers.habits': '习惯',
  'calendar.layers.availability': '可用时间',
  'calendar.layers.aiPlaceholders': 'AI 建议占位',
  'sync.deviceCode': '设备代码连接',
  'confirmations.secondLevelRequired': '此操作需要二级确认',
  'dataCenter.batchPreview': '批量影响预览',
  'reminders.title': '提醒中心',
  'reports.title': '报告中心',
  'habits.title': '习惯中心',
  'endpoints.windows': 'Windows 端',
  'endpoints.android': '安卓端'
};
```

- [ ] **Step 5: Add API wrappers**

Complete wrappers for projects, task books, checklist, habits, reminders, reports, audit, sync conflicts, endpoints.

- [ ] **Step 6: Verify green and commit**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench
npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.schedule-workbench.json
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchLocalization.test.ts
```

Expected: PASS.

Commit:

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api src/client-web/src/i18n tests/client-web src/client-web/package.json
git commit -m "feat: complete schedule workbench web contracts"
```

---

## Task 11: Complete Web Today, Calendar, Tasks, And Habit Workflows

**Files:**
- Modify: `src/client-web/src/pages/TodayPage.tsx`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/src/pages/TaskListPage.tsx`
- Modify: `src/client-web/src/pages/HabitsPage.tsx`
- Create: `src/client-web/src/components/schedule/TaskSegmentEditor.tsx`
- Create: `src/client-web/src/components/schedule/TaskHierarchyPanel.tsx`
- Create: `src/client-web/src/components/schedule/HabitRoutineEditor.tsx`
- Create: `src/client-web/src/components/schedule/CalendarLayerToolbar.tsx`
- Test: `tests/client-web/scheduleWorkbenchInteractions.test.tsx`
- Test: `tests/client-web/scheduleWorkbenchScreenshots.test.ts`

- [ ] **Step 1: Write failing interaction tests**

Create tests asserting:

```ts
assertPageSourceContains('src/client-web/src/pages/TodayPage.tsx', ['日程任务工作台', '待确认', 'Outlook 同步', '提醒队列', '报告']);
assertPageSourceContains('src/client-web/src/pages/CalendarPage.tsx', ['CalendarLayerToolbar', 'outlookOnly', 'ai-placeholders']);
assertPageSourceContains('src/client-web/src/pages/TaskListPage.tsx', ['TaskHierarchyPanel', 'TaskSegmentEditor', 'Checklist']);
assertPageSourceContains('src/client-web/src/pages/HabitsPage.tsx', ['HabitRoutineEditor', '完成历史', '投射到日历']);
```

- [ ] **Step 2: Run tests and verify red**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchInteractions.test.tsx
```

Expected: FAIL.

- [ ] **Step 3: Implement Today command center**

Today must show:

```text
calendar commitments
task execution segments
habits due today
AI suggested placeholders
pending confirmations
high-risk sync conflicts
reminder queue
endpoint collection status
report availability
standard / high-density / focus views
```

- [ ] **Step 4: Implement Calendar layers**

Calendar must support layer toggles:

```text
events
task-segments
habits
availability
ai-placeholders
outlook-only
```

AI placeholders use non-final styling and click into confirmation detail.

- [ ] **Step 5: Implement task hierarchy and segment workflow**

Tasks page must support:

```text
domain/project filter
task book filter
subtasks
checklists
multiple execution segments
state and reason
blocked/waiting/deferred review fields
audit links
report links
reminder links
source object links
```

- [ ] **Step 6: Implement habit center**

Habit page must support:

```text
create/edit habit routines
cadence filters
routine block projection to calendar
task/checklist generation request
completion history
review metrics
confirmation for habit rule changes
```

- [ ] **Step 7: Verify screenshots and commit**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Start Vite and capture:

```powershell
npm --prefix src/client-web run dev -- --host 127.0.0.1 --port 63767
```

Inspect `/today`, `/calendar`, `/tasks`, `/habits` at desktop and mobile widths.

Commit:

```powershell
git add src/client-web/src/pages src/client-web/src/components/schedule tests/client-web
git commit -m "feat: complete web planning workflows"
```

---

## Task 12: Complete Web Sync, Confirmations, Data Center, Reminders, Reports, Audit

**Files:**
- Modify: `src/client-web/src/pages/SyncPage.tsx`
- Modify: `src/client-web/src/pages/ConfirmationsPage.tsx`
- Modify: `src/client-web/src/pages/DataCenterPage.tsx`
- Modify: `src/client-web/src/pages/RemindersPage.tsx`
- Modify: `src/client-web/src/pages/ReportsPage.tsx`
- Create: `src/client-web/src/pages/AuditTimelinePage.tsx`
- Create: `src/client-web/src/components/schedule/BeforeAfterDiff.tsx`
- Create: `src/client-web/src/components/schedule/StrictConfirmationPanel.tsx`
- Create: `src/client-web/src/components/schedule/OutlookConflictResolver.tsx`
- Create: `src/client-web/src/components/schedule/DataCenterBatchPreview.tsx`
- Test: `tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx`

- [ ] **Step 1: Write failing governance UI tests**

Create tests asserting source includes:

```ts
assertPageSourceContains('src/client-web/src/pages/SyncPage.tsx', ['设备代码', 'tokenHealth', 'OutlookConflictResolver', 'deltaLink', 'writeback']);
assertPageSourceContains('src/client-web/src/pages/ConfirmationsPage.tsx', ['BeforeAfterDiff', 'StrictConfirmationPanel', '二级确认', 'allowedActions']);
assertPageSourceContains('src/client-web/src/pages/DataCenterPage.tsx', ['DataCenterBatchPreview', '审计导出', '版本恢复', 'Outlook-only']);
assertPageSourceContains('src/client-web/src/pages/RemindersPage.tsx', ['提醒中心', 'DND', '发送历史', '操作按钮']);
assertPageSourceContains('src/client-web/src/pages/ReportsPage.tsx', ['日报', '周报', '月报', '项目报告', '后续确认']);
assertPageSourceContains('src/client-web/src/pages/AuditTimelinePage.tsx', ['恢复预览', '导出审计']);
```

- [ ] **Step 2: Run tests and verify red**

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx
```

Expected: FAIL.

- [ ] **Step 3: Implement Sync page**

Sync page must include:

```text
Client ID, Tenant, Scopes
Microsoft verification link and user code
token health
sync window
writeback defaults
conflict policy
delta link status
batch steps and counts
conflict queue
source tags
disconnect/reconnect
```

- [ ] **Step 4: Implement Confirmations page**

Confirmations must show:

```text
affected objects
before/after diff
changed fields
actor/source
AI recommendation and reason
external ids
external writeback effect
allowed actions
recovery path
audit batch id
second-level confirmation
strict L4 confirmation
```

- [ ] **Step 5: Implement Data Center page**

Data Center must support global search, object filters, source filters, Outlook-only view, pending view, recycle bin, sync batches, audit timelines, version restore, batch impact preview, audit export.

- [ ] **Step 6: Implement Reminders and Reports pages**

Reminders page uses real API data with trigger reason, risk, channels, DND, escalation, delivery history, user response history, and related object links.

Reports page uses real API data with daily/weekly/monthly/project tabs, generated content, metrics, suggestions, and confirmation outcomes.

- [ ] **Step 7: Verify and commit**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Expected: PASS.

Commit:

```powershell
git add src/client-web/src/pages src/client-web/src/components/schedule tests/client-web
git commit -m "feat: complete web governance workflows"
```

---

## Task 13: Windows Companion Shell, WebView2, Toasts, Tray Audit Center

**Files:**
- Modify: `src/client-windows/Pim.Client.App/Pim.Client.App.csproj`
- Modify: `src/client-windows/Pim.Client.App/App.xaml`
- Modify: `src/client-windows/Pim.Client.App/App.xaml.cs`
- Create: `src/client-windows/Pim.Client.App/MainShellWindow.xaml`
- Create: `src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs`
- Create: `src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs`
- Create: `src/client-windows/Pim.Client.App/NotificationActionRouter.cs`
- Modify: `src/client-windows/Pim.Client.App/TrayIcon.cs`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`
- Modify: `src/client-windows/Pim.Client.Core/Services/ApiClient.cs`
- Create: `src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs`
- Test: `tests/Pim.UnitTests/ClientWindows/WindowsNotificationActionRouterTests.cs`

- [ ] **Step 1: Write failing Windows tests**

Create tests asserting:

```csharp
Assert.Contains("WebView2", projectFile);
Assert.Contains("EmbeddedWebViewHost", mainShellCode);
Assert.Equal("OpenDetailRequired", router.Route("confirm", "L3ExternalSourceOrWriteback").Kind);
Assert.Equal("Executed", router.Route("dismiss", "L1LowRiskAction").Kind);
Assert.True(boundary.CanQueueOffline("collection-upload"));
Assert.False(boundary.CanQueueOffline("task-fact-change"));
```

- [ ] **Step 2: Run tests and verify red**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsCompanionShellTests|FullyQualifiedName~WindowsNotificationActionRouterTests"
```

Expected: FAIL.

- [ ] **Step 3: Add WebView2 shell**

Main shell must provide:

```text
embedded Web workbench
server URL setting
account state
collection/upload state
notification center
open Today / Tasks / Calendar / Reports / Outlook Sync / Data Center / Audit detail
```

- [ ] **Step 4: Add Toast/tray routing**

Low-risk Toast actions call API directly and record action history.

High-risk Toast actions open Web audit detail in WebView:

```text
L2 -> /confirmations/{id}
L3 -> /confirmations/{id}
L4 -> /confirmations/{id}
```

- [ ] **Step 5: Enforce offline boundary**

Windows may queue:

```text
PC activity collection
window/browser context
input activity
device state
upload retry
```

Windows must block:

```text
task fact changes
event fact changes
habit rule changes
confirmation decisions
report edits
Outlook writeback
restore/delete operations
```

- [ ] **Step 6: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~WindowsCompanionShellTests|FullyQualifiedName~WindowsNotificationActionRouterTests|FullyQualifiedName~ApiClientDefaultsTests"
dotnet publish src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Release -o publish/PimDaemon -r win-x64 --self-contained true
```

Expected: PASS and publish succeeds.

Commit:

```powershell
git add src/client-windows tests/Pim.UnitTests/ClientWindows
git commit -m "feat: add windows companion shell"
```

---

## Task 14: Android Companion Shell, Permission Center, WebView, Notifications

**Files:**
- Modify: `src/client-android/app/build.gradle.kts`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/permissions/PermissionCenterScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/PimNotificationRouter.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/sync/EndpointUploadWorker.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/AndroidNotificationActionRouterTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt`

- [ ] **Step 1: Write failing Android tests**

Create tests:

```kotlin
class AndroidNotificationActionRouterTest {
    @Test fun lowRiskActionCanExecuteDirectly() {
        val result = PimNotificationRouter().route("dismiss", "L1LowRiskAction")
        assertEquals(NotificationRoute.ExecuteOnline, result)
    }

    @Test fun highRiskActionOpensDetail() {
        val result = PimNotificationRouter().route("confirm", "L3ExternalSourceOrWriteback")
        assertTrue(result is NotificationRoute.OpenDetail)
    }
}

class AndroidOfflineBoundaryTest {
    @Test fun onlyCollectionUploadsCanQueueOffline() {
        val guard = OnlineOperationGuard()
        assertTrue(guard.canQueueOffline("collection-upload"))
        assertFalse(guard.canQueueOffline("task-fact-change"))
        assertFalse(guard.canQueueOffline("confirmation-decision"))
        assertFalse(guard.canQueueOffline("outlook-writeback"))
    }
}
```

- [ ] **Step 2: Run tests and verify red**

```powershell
cd src/client-android
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL until shell/router/guard are implemented.

- [ ] **Step 3: Implement Android native shell**

Shell must include:

```text
permission center
app usage status
location status
device state status
collection quality
upload queue
account/server status
embedded Web workbench
error recovery
```

- [ ] **Step 4: Implement Android WebView**

WebView must:

```text
load server-configured Web URL
open Today / Tasks / Calendar / Reports / Outlook Sync / Data Center / Audit detail
show offline state instead of queuing fact changes
pass auth tokens through approved app storage
```

- [ ] **Step 5: Implement notification actions**

Android notification buttons:

```text
L0/L1 -> direct online API action, record history
L2/L3/L4 -> open app detail or Web audit detail
offline high-risk -> show retry/open when online
```

- [ ] **Step 6: Verify Android build and commit**

Run:

```powershell
cd src/client-android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Expected: PASS.

Commit:

```powershell
git add src/client-android
git commit -m "feat: add android companion shell"
```

---

## Task 15: Endpoint Status APIs And Collection Boundary

**Files:**
- Create: `src/Pim.Core/Endpoints/EndpointDtos.cs`
- Create: `src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs`
- Create: `src/Pim.Api/Endpoints/EndpointEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`
- Modify: `src/client-web/src/pages/EndpointShellPage.tsx`
- Test: `tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs`
- Test: `tests/client-web/endpointShellPage.test.tsx`

- [ ] **Step 1: Write failing endpoint boundary tests**

Create tests asserting:

```csharp
Assert.True(service.CanCacheOffline("pc-activity"));
Assert.True(service.CanCacheOffline("android-location"));
Assert.False(service.CanCacheOffline("task-fact-change"));
Assert.False(service.CanCacheOffline("confirmation-decision"));
Assert.False(service.CanCacheOffline("report-edit"));
Assert.False(service.CanCacheOffline("outlook-writeback"));
```

- [ ] **Step 2: Implement APIs**

Add endpoints:

```csharp
app.MapGet("/api/v1/endpoints", ListEndpointStatuses);
app.MapPost("/api/v1/endpoints/{deviceId}/heartbeat", UpsertEndpointHeartbeat);
app.MapGet("/api/v1/endpoints/{deviceId}/collection-quality", GetCollectionQuality);
app.MapPost("/api/v1/endpoints/{deviceId}/notification-actions", HandleEndpointNotificationAction);
```

- [ ] **Step 3: Verify and commit**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~EndpointBoundaryTests
npm --prefix src/client-web exec tsx -- tests/client-web/endpointShellPage.test.tsx
```

Expected: PASS.

Commit:

```powershell
git add src/Pim.Core/Endpoints src/Pim.Infrastructure/Endpoints src/Pim.Api/Endpoints src/client-web/src/pages/EndpointShellPage.tsx tests
git commit -m "feat: add endpoint status and offline boundary APIs"
```

---

## Task 16: End-To-End Tests, Browser Screenshots, And Visual Polish

**Files:**
- Create: `tests/client-web/scheduleWorkbenchE2e.test.ts`
- Create: `tests/client-web/scheduleWorkbenchVisualAudit.test.ts`
- Modify: `src/client-web/src/index.css`
- Modify: `src/client-web/package.json`

- [ ] **Step 1: Add E2E test script**

Add package script:

```json
"test:schedule-workbench-complete": "cd ../.. && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchCompletionApiPath.test.ts && npm --prefix src/client-web exec tsc -- -p tests/client-web/tsconfig.schedule-workbench.json && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchCompletionTypes.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchLocalization.test.ts && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchInteractions.test.tsx && npm --prefix src/client-web exec tsx -- tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx"
```

- [ ] **Step 2: Add screenshot checklist**

Create `tests/client-web/scheduleWorkbenchVisualAudit.test.ts` that loads:

```text
/today
/calendar
/tasks
/habits
/reminders
/reports
/sync
/data-center
/confirmations
/audit/task/{id}
/endpoint-shell
```

At widths:

```text
390x844
768x1024
1440x1000
```

Assert:

```text
body text is not login page after test token injection
main content is non-empty
no element has negative bounding box
no buttons have clipped text
no English heading remains in new schedule workbench surfaces
calendar canvas/list is nonblank
```

- [ ] **Step 3: Polish UI**

Fix every visual issue found by screenshots:

```text
text overflow
nested card clutter
English-only labels
button clipping
calendar layer overlap
mobile layout overflow
sidebar width clipping
```

- [ ] **Step 4: Verify and commit**

Run:

```powershell
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Expected: PASS.

Commit:

```powershell
git add src/client-web tests/client-web
git commit -m "test: verify schedule workbench web completion"
```

---

## Task 17: Full Local Verification And GitHub Actions

**Files:**
- Modify: `.github/workflows/build-api.yml`
- Modify: `.github/workflows/build-web.yml`
- Modify: `.github/workflows/build-windows.yml`
- Modify: `.github/workflows/build-android.yml`
- Create: `docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md`

- [ ] **Step 1: Verify all local surfaces**

Run:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
dotnet publish src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Release -o publish/PimDaemon -r win-x64 --self-contained true
cd src/client-android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
cd ..\..
git diff --check
git status --short --branch
```

Expected:

```text
dotnet test: 0 failed
web tests: 0 failed
web build: success
windows publish: success
android unit tests: success
android assembleDebug: success
git diff --check: no output
git status: only intentional report/status files before final commit
```

- [ ] **Step 2: Update workflows**

Ensure every workflow supports `workflow_dispatch` and can run on the implementation branch.

Android workflow must run:

```yaml
- name: Unit tests
  run: ./gradlew :app:testDebugUnitTest

- name: Assemble debug
  run: ./gradlew :app:assembleDebug
```

- [ ] **Step 3: Push branch and trigger GA**

```powershell
git push -u origin codex/schedule-task-complete-system
gh workflow run build-api.yml --ref codex/schedule-task-complete-system
gh workflow run build-web.yml --ref codex/schedule-task-complete-system
gh workflow run build-windows.yml --ref codex/schedule-task-complete-system
gh workflow run build-android.yml --ref codex/schedule-task-complete-system
```

- [ ] **Step 4: Wait for GA**

```powershell
gh run list --branch codex/schedule-task-complete-system --limit 10
gh run watch <api-run-id> --exit-status --interval 15
gh run watch <web-run-id> --exit-status --interval 15
gh run watch <windows-run-id> --exit-status --interval 15
gh run watch <android-run-id> --exit-status --interval 15
```

Expected: all success.

- [ ] **Step 5: Create completion evidence report**

Create `docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md` with:

```markdown
# Schedule Task Workbench Completion Evidence

## Local Verification

- dotnet test Pim.sln: PASS
- npm --prefix src/client-web run test:schedule-workbench-complete: PASS
- npm --prefix src/client-web run build: PASS
- Windows publish: PASS
- Android unit tests and assembleDebug: PASS

## GitHub Actions

- Build API: <url> success
- Build Web Client: <url> success
- Build Windows Client: <url> success
- Build Android: <url> success

## Browser Visual Evidence

- Today: screenshot path
- Calendar: screenshot path
- Tasks: screenshot path
- Habits: screenshot path
- Reminders: screenshot path
- Reports: screenshot path
- Sync: screenshot path
- Data Center: screenshot path
- Confirmations: screenshot path
- Audit Timeline: screenshot path
- Endpoint Shell: screenshot path

## Requirement Coverage

All requirements listed in 2026-07-08-schedule-task-workbench-design.md are implemented by Tasks 1-16 and verified by Task 17.
```

- [ ] **Step 6: Commit final evidence**

```powershell
git add .github/workflows docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md
git commit -m "docs: record schedule workbench completion evidence"
```

---

## Task 18: Final Review, PR, And Merge Readiness

**Files:**
- Modify: `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`
- Modify: `docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md`

- [ ] **Step 1: Replace status section with full completion**

Update the design doc status:

```markdown
## Implementation Status

### Full Completion

Implemented on branch `codex/schedule-task-complete-system`:

- Shared planning model, confirmation, audit, Outlook Graph, reminders, reports, Data Center, Web, Windows, and Android endpoint shells.
- No design requirements remain intentionally unimplemented.
- Local and GitHub Actions verification passed for API, Web, Windows, and Android.
```

- [ ] **Step 2: Request code review**

Use `superpowers:requesting-code-review` with:

```text
DESCRIPTION: Full completion of Schedule And Task Workbench design across backend, Web, Windows, Android, Outlook, reminders, reports, audit, and Data Center.
PLAN_OR_REQUIREMENTS: docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md and this full completion plan.
BASE_SHA: merge-base with origin/master
HEAD_SHA: current HEAD
```

- [ ] **Step 3: Fix review findings**

For every Critical or Important finding:

```text
read finding
verify against code
write failing test
fix
run targeted tests
commit
```

- [ ] **Step 4: Create PR**

```powershell
gh pr create --base master --head codex/schedule-task-complete-system --title "feat: complete schedule task workbench" --body-file docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md
```

- [ ] **Step 5: Final branch status**

Run:

```powershell
git status --short --branch
gh pr checks --watch
```

Expected:

```text
git status: clean
all PR checks: success
```

Commit any PR description/doc updates:

```powershell
git add docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md docs/superpowers/reports/2026-07-08-schedule-task-workbench-completion-evidence.md
git commit -m "docs: mark schedule workbench fully complete"
git push
```

---

## Self-Review Checklist

- Product model: covered by Tasks 2, 4, 5, 7, 8, 9.
- Risk and confirmation model: covered by Tasks 3, 5, 6, 7, 8, 9, 12, 13, 14, 15.
- Web workbench: covered by Tasks 10, 11, 12, 16.
- Outlook Graph and ICS: covered by Tasks 5 and 6.
- Windows endpoint: covered by Task 13 and Task 15.
- Android endpoint: covered by Task 14 and Task 15.
- Offline boundary: covered by Tasks 13, 14, 15.
- Data Center: covered by Task 9 and Task 12.
- Services and boundaries: covered by Tasks 2-9 and 15.
- Key data flows: AI task segment suggestions in Tasks 4 and 8; Outlook location change in Tasks 5 and 6; endpoint high-risk notification in Tasks 7, 13, and 14; report follow-up suggestions in Task 8.
- Error handling: Graph token/network/writeback in Task 5; confirmation stale/idempotent in Task 3; audit failure in Task 3; endpoint offline in Tasks 13-15.
- Testing and verification: covered by Tasks 16-18.
- Foundation plan: all foundation included by Tasks 1-4, 10-12; all old deferred items included by Tasks 5, 6, 9, 13, 14, 15.

Plan complete and saved to `docs/superpowers/plans/2026-07-08-schedule-task-workbench-full-completion.md`.

