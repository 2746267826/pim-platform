# Schedule And Task Workbench Design

## Goal

Build the next generation of PIM schedule and task features as a shared planning workbench across Web, Windows, Android, Outlook sync, reminders, reports, and audit governance.

This is an expansion of the existing Stage 5 calendar/task loop, not a blind rewrite. The current system already has calendar/task APIs, `/today`, `/calendar`, `/tasks`, `/settings/calendar-data`, recycle bin surfaces, ICS import/export, task planning, and Today section providers. This design keeps those foundations and extends them into a complete schedule/task operating system.

The design deliberately ignores short-term development cost. Implementation planning must still split the work into independent, testable tracks.

## Current Foundation

Relevant existing surfaces include:

- `docs/superpowers/specs/2026-05-26-stage-5-calendar-task-loop-design.md`
- `src/client-web/src/pages/TodayPage.tsx`
- `src/client-web/src/pages/CalendarPage.tsx`
- `src/client-web/src/pages/TaskListPage.tsx`
- `src/client-web/src/pages/CalendarDataManager.tsx`
- `src/client-web/src/pages/RecycleBinPage.tsx`
- `src/client-web/src/api/calendar.ts`
- `src/modules/Pim.Module.Calendar/CalendarModule.cs`
- `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- `src/Pim.Api/Today/TodaySectionProviders.cs`
- `src/client-windows`
- `src/client-android`

Stage 5 already established important invariants: a task planned onto the calendar remains a task, delete and restore need preview/audit behavior, ICS import/export exists, and Today should surface calendar/task sections. This design generalizes those ideas into multi-segment tasks, source-aware sync, confirmation requests, endpoint notifications, and a unified data center.

## Accepted Decisions

- Use the recommended route: shared core plus parallel product tracks.
- The default Web entry is a Today command center that mixes schedule, tasks, reminders, reports, sync risk, endpoint status, and confirmations.
- AI should be proactive, but changes to important facts require manual confirmation.
- Important facts include task/event name, time, location, status, owner/book/project, recurrence rules, deletion, restore, stop-sync, and external writeback.
- New artifacts such as reports, summaries, conflict explanations, and recommendations may be generated automatically.
- Task hierarchy includes domain/project, task book, task, subtasks, and checklist items.
- Basic tasks can be split into multiple execution segments.
- Tasks and events remain separate object types. Task execution segments project tasks onto calendar layers; they do not convert tasks into events.
- Calendar layers include events, task execution segments, habits/routines, time budget/availability windows, and AI suggested placeholders. Layers must be filterable.
- Habits/routines are independent objects that can project calendar blocks and generate tasks/checklists.
- Task state includes inbox, to-plan, planned, in-progress, waiting, blocked, deferred, paused, completed, cancelled, and reason/review metadata.
- UI density is switchable: standard, high-density, and focus. Standard is the default.
- Reminders use a complete reminder center with strategies, channels, DND, history, AI reasons, and action buttons.
- Reports include daily, weekly, monthly, and project reports covering planned vs actual, PC/mobile collection, habits, reminders, blockers, and AI suggestions.
- Outlook uses Microsoft Graph official APIs for two-way sync.
- Outlook connection is configured in Settings with Client ID, Tenant, Scopes, and device code flow through a Microsoft link/code.
- Outlook sync must expose detailed steps, logs, counts, diffs, and errors.
- Outlook-origin events receive a special source tag for filtering and batch management.
- Operations on Outlook-origin events require extra second-level confirmation when they modify core facts or write back externally.
- ICS import/export remains available as a lower-priority exchange path.
- Conflicts where PIM and Outlook both changed the same core field are manually resolved. AI may recommend, but cannot auto-resolve.
- Audit history must be complete enough for before/after review, object timelines, version restore, batch inspection, and audit export.
- Windows and Android do not rebuild every complex module natively. They become polished native shells with collection, upload, notifications, status, account/server controls, and embedded Web.
- Endpoint collection data may cache offline. Other operations stay online: confirmation, task/event changes, report input, audit detail, and Outlook writeback.
- Windows context collection is pluggable, starting with PC activity, window/browser context, input activity, and device state.
- Android context collection is pluggable, starting conservatively with app usage, location, and device state.
- Endpoint visual style should feel like PIM while respecting platform conventions.
- Implementation must happen on an independent new `codex/...` branch.
- Implementation should use goal mode for the long-running objective.
- Implementation should use multiple subagents/parallel tracks where interfaces are clear.
- Work should be committed at appropriate milestones and verified locally and through GA/GitHub Actions when appropriate.

## Product Principles

PIM should feel like a quiet command center rather than a collection of disconnected pages. The user should understand three things immediately:

- What is committed on the calendar.
- What work should be executed and why.
- What needs human judgment before the system changes important facts.

Automation is useful only when the boundary is clear. PIM can observe, summarize, recommend, cluster, explain, and draft. It cannot silently rewrite important personal planning facts.

The system should keep local object identity and external identity distinct. Outlook events are not "just PIM events"; they are PIM-managed projections of Graph resources with source metadata, writeback rules, and stricter confirmation.

## Product Model

### Core Objects

`DomainProject`

- Long-lived area or project.
- Owns goals, reports, task books, and planning context.
- Used for project reports and review filters.

`TaskBook`

- Organizes tasks within a domain/project.
- Supports batch governance, soft delete, restore, and reporting.

`Task`

- Represents work to complete.
- Can contain subtasks and checklist items.
- Can have multiple execution segments.
- Has state, reason, priority, estimate, review outcome, source, and audit history.

`TaskExecutionSegment`

- A planned block of time for working on a task.
- Has start, end/duration, status, source, planning reason, and confirmation state.
- Appears on calendar layers but remains linked to a task.

`CalendarEvent`

- A committed time event.
- May be PIM-native, ICS-imported, or Outlook Graph-synced.
- Has source tags, recurrence metadata, location, participants/meeting metadata when available, and audit history.

`HabitRoutine`

- Independent routine model.
- Can project routine blocks to calendar layers.
- Can generate tasks or checklist items.
- Has completion history and review metrics.

`Reminder`

- A notification intent with trigger, channel, DND policy, risk level, history, and available actions.
- May point to an object, report, confirmation request, or sync batch.

`ReportArtifact`

- Auto-generated or user-generated daily/weekly/monthly/project report.
- Stores inputs, generated content, metrics, recommendations, and follow-up confirmation requests.
- Does not directly mutate task/event facts.

`ConfirmationRequest`

- A pending or completed human decision.
- Stores risk level, affected object, proposed operation, before/after fields, AI recommendation, allowed actions, origin, and audit batch.

`AuditVersion`

- Immutable record of before/after, actor, source, timestamp, request id, batch id, AI request id when applicable, and external ids.
- Supports object timeline, export, and version restore.

`SyncConnection`

- External account/connection configuration, starting with Outlook Graph.
- Stores provider, tenant/client settings, encrypted token state, scopes, status, last sync, and disconnect metadata.

`SyncItem`

- Binding between a PIM object and an external object.
- For Outlook, stores Graph event id, iCalUId when available, changeKey/eTag, source calendar, delta state, source tag, and sync policy.

`SyncBatch`

- One sync run.
- Stores counts, steps, logs, errors, generated diffs, created confirmations, and writeback results.

`RecycleBinItem`

- Soft-deleted object with original source, delete operation id, restore policy, and audit chain.

### Invariants

- A task remains a task even when it appears on the calendar.
- A calendar event remains a commitment even when it is connected to a task or project.
- A task can have zero, one, or many execution segments.
- A habit is not a task by default. It can generate tasks/checklists or project routine blocks.
- Important fact changes must go through confirmation before persistence.
- Auto artifacts do not change facts until their suggestions are accepted through confirmation.
- External source identity is preserved. PIM ids and Graph ids are both visible in governance surfaces.
- Delete is soft by default and must preserve recovery metadata.

## Risk And Confirmation Model

All user-facing surfaces use the same risk levels.

`L0 Automatic Artifact`

- Reports, summaries, statistics, trend analysis, conflict explanations, and recommendation drafts.
- Can be generated and saved automatically.
- Cannot modify task/event/habit facts.

`L1 Low-Risk Action`

- Mark notification read, snooze, open details, archive report, dismiss suggestion.
- Can be performed from Web, Windows, or Android notification actions.
- Must still record action history.

`L2 PIM Fact Change`

- Changes to PIM task/event/habit core facts: name, time, location, status, project/book, recurrence, execution segment, deletion, restore.
- Requires human confirmation with field-level diff.

`L3 External Source Or Writeback`

- Outlook-origin item core changes, writeback to Microsoft Graph, external conflict resolution, source binding changes.
- Requires second-level confirmation with external impact, Graph/PIM ids, timestamps, and source metadata.

`L4 Batch Or Destructive Governance`

- Batch delete, stop sync, restore many objects, delete books with children, bulk writeback, recurrence-wide operations.
- Requires strict confirmation with impact preview, recoverability, and audit export path.

Confirmation detail pages must show:

- Affected object(s).
- Before/after values.
- Changed fields.
- Actor/source.
- AI recommendation and reason.
- External ids and external writeback effect when applicable.
- Allowed actions.
- Recovery path.
- Audit batch id.

## Web Workbench

### Navigation

Web is the complete workbench. Primary navigation includes:

- Today
- Tasks
- Calendar
- Habits
- Reminders
- Reports
- Data Center
- Sync
- Settings

### Today Command Center

Today is the default entry. It shows:

- Calendar commitments.
- Task execution segments.
- Habits/routines due today.
- AI suggested placeholders.
- Pending confirmations.
- High-risk sync conflicts.
- Reminder queue.
- Endpoint collection status.
- Report availability.

Today supports standard, high-density, and focus views:

- Standard: default balanced dashboard.
- High-density: more rows, compact cards, batch review.
- Focus: current/next work, blockers, minimal noise.

### Calendar

Calendar supports layer toggles:

- Events.
- Task execution segments.
- Habits/routines.
- Time budgets/availability windows.
- AI suggested placeholders.
- Outlook-only filter.

Task execution segments should be visually distinct from events. AI suggested placeholders should use a non-final visual style and require confirmation before becoming planned segments or events.

### Tasks

Tasks support:

- Domain/project and task book organization.
- Inbox and to-plan queue.
- Subtasks and checklists.
- Multiple execution segments.
- State and reason tracking.
- Blocked/waiting/deferred review fields.
- Batch governance.
- Links to reports, reminders, audit, and source objects.

### Reminders

The reminder center stores:

- Trigger reason.
- Risk level.
- Channels.
- DND and escalation policy.
- Delivery history.
- User response history.
- Related confirmation or report.

Channels include Web in-app, Windows Toast/tray center, Android notifications, and future extensibility.

### Reports

Reports include daily, weekly, monthly, and project reports. They can include:

- Planned vs actual.
- Task completion and state changes.
- Calendar occupancy.
- Outlook impact.
- PC/mobile collection quality.
- Habit/routine completion.
- Reminder response.
- Blockers and delays.
- AI observations and suggestions.

Report generation may be automatic. Suggestions that modify facts create confirmation requests.

### Sync And Settings

Sync pages show:

- Connection status.
- Provider settings.
- Sync batches.
- Step-by-step logs.
- Conflict queues.
- Source tags.
- Writeback confirmations.
- Disconnect/reconnect actions.

Settings include Microsoft Graph Client ID, Tenant, Scopes, sync window, writeback defaults, conflict policies, and token status.

## Outlook And ICS Sync

### Outlook Graph Connection

Outlook sync uses Microsoft Graph official APIs. The settings page supports:

- Client ID.
- Tenant: `common`, `organizations`, or a specific tenant id.
- Scopes: delegated `Calendars.ReadWrite`, `offline_access`, `User.Read`, `openid`, and `profile` as the default user-connected setup.
- Connection status.
- Token health.
- Disconnect/reconnect.

The connection flow uses Microsoft identity platform device code flow:

1. PIM requests a device code.
2. PIM shows the Microsoft verification URL and user code.
3. The user opens the Microsoft URL and enters the code.
4. PIM polls the token endpoint.
5. PIM stores encrypted token state after success.
6. PIM records the connection in audit history.

Official references:

- [Microsoft identity platform device code flow](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code)
- [Microsoft Graph event resource](https://learn.microsoft.com/en-us/graph/api/resources/event?view=graph-rest-1.0)
- [Microsoft Graph calendar resource](https://learn.microsoft.com/en-us/graph/api/resources/calendar?view=graph-rest-1.0)
- [Microsoft Graph event delta](https://learn.microsoft.com/en-us/graph/api/event-delta?view=graph-rest-1.0)

### Sync Flow

Every Outlook sync batch shows detailed progress:

1. Load provider configuration.
2. Validate token status and scopes.
3. Load previous delta state.
4. Read calendar view/events from Graph.
5. Follow nextLink/deltaLink as needed.
6. Map Graph events to PIM sync items.
7. Compare field-level changes.
8. Apply safe reads/imports.
9. Create confirmation requests for fact changes and conflicts.
10. Wait for user confirmation before writeback.
11. Write approved changes to Graph.
12. Record batch result, failures, and audit.

Batch metrics include:

- Read count.
- Created count.
- Updated count.
- Deleted/tombstone count.
- Skipped count.
- Conflict count.
- Confirmation count.
- Writeback count.
- Failure count.

### Source Tags

Outlook-origin events have visible source tags and filter controls. The user can:

- View only Outlook-origin events.
- Batch tag Outlook events.
- Pause sync for selected events.
- Stop syncing selected events.
- Inspect Graph/PIM ids.
- Open sync history.
- Export audit.

Stopping sync is `L4` and requires strict confirmation.

### Conflict Handling

Manual resolution is required when both sides changed the same core field. The conflict page shows:

- PIM version.
- Outlook version.
- Changed fields.
- PIM timestamp and actor.
- Outlook timestamp and Graph metadata.
- Graph event id.
- PIM event id.
- AI recommendation.

Allowed actions:

- Keep PIM.
- Keep Outlook.
- Merge by field.
- Create merge copy.
- Skip this batch.
- Stop syncing this item.

AI cannot auto-resolve core conflicts.

### ICS

ICS import/export remains available for:

- Manual exchange.
- Outlook-exported file compatibility.
- Import preview and duplicate detection.
- Export by selected objects or date range.

ICS is not the primary two-way sync mechanism.

## Windows Endpoint

Windows becomes a polished PIM companion shell.

Native responsibilities:

- PC activity collection.
- Window/browser context collection.
- Input activity and device state collection.
- Upload queue.
- Collection cache.
- Server/account settings.
- Toast notifications.
- Tray audit center.
- Open embedded Web workbench.
- Show sync/confirmation/report status.

Complex modules use embedded Web:

- Today.
- Tasks.
- Calendar.
- Reports.
- Outlook sync.
- Data center.
- Audit detail.

High-risk operations from Toast must open Web audit detail before confirmation. Low-risk notification actions can execute directly and record history.

## Android Endpoint

Android becomes a polished PIM mobile shell.

Native responsibilities:

- Permission center.
- Conservative default collection: app usage, location, device state.
- Collection quality and upload status.
- Notification actions.
- Open embedded Web workbench.
- Account/server status.
- Error recovery.

Future collection plugins may include health, Bluetooth, Wi-Fi, commute/traffic, and sensor context. Each source must have its own permission, toggle, quality, last upload, and error state.

High-risk operations from Android notifications must open App detail or Web audit detail. Low-risk actions may run from notification buttons.

## Offline Boundary

Only endpoint collection data may be cached offline.

The following require online execution:

- Task fact changes.
- Event fact changes.
- Habit rule changes.
- Confirmation decisions.
- Audit detail loading.
- Report input/edit actions.
- Outlook writeback.
- Batch governance.
- Restore/delete operations.

If an online operation is attempted offline, the endpoint should show a clear state and offer retry/open when online. It should not queue fact changes silently.

## Data Center

Data Center is the global governance surface. It complements object-specific pages; it does not replace them.

Data Center supports:

- Global search across tasks, events, execution segments, habits, reminders, reports, source ids, Graph ids, sync batches, locations, and audit records.
- Object filters.
- Source filters.
- Outlook-only views.
- Pending confirmation views.
- Recycle bin.
- Sync batches.
- Audit timelines.
- Version restore.
- Batch impact previews.
- Audit export.

Batch operations must preview:

- Affected object count.
- Affected object types.
- Changed fields.
- Source tags.
- External writeback effect.
- Recoverability.
- Risk level.
- Required confirmation path.

## Services And Boundaries

The implementation plan should separate these services clearly.

`PlanningModelService`

- Owns task/event/habit invariants.
- Validates execution segments and calendar layers.
- Exposes APIs used by Web and endpoint shells.

`ConfirmationService`

- Creates, lists, resolves, expires, and audits confirmation requests.
- Enforces risk rules for core facts and Outlook writeback.

`AuditVersionService`

- Records before/after and actor/source metadata.
- Provides object timelines, export, and restore support.

`OutlookSyncService`

- Owns Microsoft Graph device-code connection, token state, delta sync, Graph mapping, writeback, and sync batch logs.
- Calls ConfirmationService before core writes.

`IcsExchangeService`

- Keeps import/export behavior separate from Graph sync.
- Produces import preview/report and duplicate handling.

`ReminderService`

- Owns strategies, channels, DND, escalation, history, and endpoint delivery payloads.

`ReportService`

- Builds report artifacts from planning, collection, habit, reminder, and sync data.
- Creates confirmation requests for actionable recommendations.

`EndpointCollectionService`

- Accepts Windows/Android collection uploads.
- Tracks collection quality, permissions, device status, and upload queues.

`DataCenterQueryService`

- Provides cross-object search, filter, source, sync, recycle, and audit views.

## Key Data Flows

### AI Suggests Moving A Task Segment

1. AI reads calendar/task context.
2. AI generates recommendation as an automatic artifact.
3. Recommendation touches a task execution segment time.
4. ConfirmationService creates an `L2` confirmation.
5. Web/endpoint notification shows diff and reason.
6. User accepts or rejects.
7. Accepted change updates task segment and writes audit version.

### Outlook Changes A Meeting Location

1. OutlookSyncService reads Graph delta.
2. Sync item maps Graph event to PIM event.
3. Field diff detects location change.
4. Because it is Outlook-origin and core field, create `L3` request.
5. User sees Graph/PIM ids, before/after, source, timestamps.
6. User chooses keep PIM, keep Outlook, merge, skip, or stop sync.
7. Approved writeback, if any, runs through OutlookSyncService.
8. Batch result and audit version are recorded.

### Endpoint Receives High-Risk Notification

1. ReminderService delivers notification payload.
2. Windows Toast or Android notification shows summary and safe buttons.
3. High-risk confirmation opens Web audit detail.
4. User reviews full diff.
5. Confirmation decision is sent online.
6. Endpoint receives final status.

### Report Creates Follow-Up Suggestions

1. ReportService generates report artifact automatically.
2. Report includes observations and proposed changes.
3. Pure observations are saved in the report.
4. Proposed fact changes become confirmation requests.
5. The report links to the confirmations and later records outcomes.

## Error Handling

Token and auth errors:

- Expired device code: show a new code request action.
- Refresh token failure: mark connection as attention-needed and require reconnect.
- Scope missing: show required scopes and block writeback.

Graph sync errors:

- Network failure: keep batch retryable.
- Rate limit: back off and record delay.
- Writeback failure: keep confirmation unresolved or mark writeback failed without losing decision context.
- Deleted external event: create diff/tombstone record and require confirmation when local object has user changes.

Confirmation errors:

- Duplicate clicks must be idempotent.
- Stale confirmation must re-check current object version.
- Batch confirmation must lock or re-preview affected objects before commit.

Audit errors:

- A fact change cannot commit without audit metadata.
- If audit write fails, the fact write should fail or be compensated according to the transaction boundary chosen in implementation.

Endpoint errors:

- Offline collection uploads may queue.
- Offline fact changes are blocked.
- Notification action failure must show current status and retry/open detail.

## Testing And Verification

Backend tests:

- Task execution segment invariants.
- Event/task separation.
- Risk level classification.
- Confirmation creation and idempotent resolution.
- Audit before/after persistence.
- Data center search/filter.
- Recycle restore behavior.
- Outlook Graph mapping with mocked Graph responses.
- Delta/nextLink handling.
- Conflict detection.
- Writeback confirmation gates.

Frontend tests:

- Today command center density modes.
- Calendar layer toggles.
- Task multi-segment display.
- Confirmation detail diff.
- Outlook sync batch UI.
- Data Center batch preview.
- Reminder/report flows.

Endpoint tests:

- Collection cache/upload boundary.
- Low-risk notification action.
- High-risk notification opens detail.
- Offline blocking of fact changes.
- Embedded Web shell navigation.

Verification commands:

- `dotnet test Pim.sln`
- `npm --prefix src/client-web run build`
- Targeted endpoint builds/tests when those tracks change.
- Playwright/screenshot checks for major Web UI surfaces.
- GA/GitHub Actions validation at integration milestones and before pushing a final branch.

## Implementation Governance

Implementation must not start from this design doc alone until the follow-up implementation plan is written and approved.

Required implementation rules:

- Fetch and align with latest `master` before starting.
- Create an independent `codex/...` branch.
- Use goal mode for the full objective.
- Split into parallel tracks where interfaces are stable:
  - API/data model.
  - Web workbench.
  - Outlook Graph sync.
  - Windows shell.
  - Android shell.
  - Tests/docs.
- Use subagents for independent tracks when useful.
- Define contracts before parallel edits that touch the same model/API.
- Commit at meaningful, reversible milestones.
- Run relevant verification before each major commit.
- Use GA/GitHub Actions validation when an integration branch is ready.
- Do not commit `.superpowers/brainstorm/`; commit only the design doc and intentional documentation attachments.

## Attachment Index

All browser UI prototypes from the brainstorming session are preserved as formal design attachments under `docs/superpowers/specs/attachments/2026-07-08-schedule-task-ui/`.

- [today-entry-layout.html](attachments/2026-07-08-schedule-task-ui/today-entry-layout.html)
- [waiting-after-today-layout.html](attachments/2026-07-08-schedule-task-ui/waiting-after-today-layout.html)
- [today-command-center-complete-v1.html](attachments/2026-07-08-schedule-task-ui/today-command-center-complete-v1.html)
- [today-command-center-density-v2.html](attachments/2026-07-08-schedule-task-ui/today-command-center-density-v2.html)
- [today-command-center-reminders-v3.html](attachments/2026-07-08-schedule-task-ui/today-command-center-reminders-v3.html)
- [today-command-center-reports-v4.html](attachments/2026-07-08-schedule-task-ui/today-command-center-reports-v4.html)
- [today-command-center-outlook-v5.html](attachments/2026-07-08-schedule-task-ui/today-command-center-outlook-v5.html)
- [endpoint-companion-v6.html](attachments/2026-07-08-schedule-task-ui/endpoint-companion-v6.html)
- [parallel-roadmap-v7.html](attachments/2026-07-08-schedule-task-ui/parallel-roadmap-v7.html)
- [shared-core-model-v8.html](attachments/2026-07-08-schedule-task-ui/shared-core-model-v8.html)
- [web-workbench-v9.html](attachments/2026-07-08-schedule-task-ui/web-workbench-v9.html)
- [ai-confirmation-audit-v10.html](attachments/2026-07-08-schedule-task-ui/ai-confirmation-audit-v10.html)
- [outlook-sync-v11.html](attachments/2026-07-08-schedule-task-ui/outlook-sync-v11.html)
- [reminders-reports-v12.html](attachments/2026-07-08-schedule-task-ui/reminders-reports-v12.html)
- [endpoint-shells-v13.html](attachments/2026-07-08-schedule-task-ui/endpoint-shells-v13.html)
- [data-center-governance-v14.html](attachments/2026-07-08-schedule-task-ui/data-center-governance-v14.html)
- [architecture-delivery-v15.html](attachments/2026-07-08-schedule-task-ui/architecture-delivery-v15.html)

## Approval State

All major design sections were reviewed and approved during brainstorming:

- Shared core model.
- Web workbench.
- AI, confirmation, and audit.
- Outlook/ICS sync.
- Reminders and reports.
- Windows/Android endpoint shells.
- Data center and batch governance.
- Technical architecture and delivery governance.
