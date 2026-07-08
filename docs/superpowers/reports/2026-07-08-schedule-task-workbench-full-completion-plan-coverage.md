# Schedule Task Workbench Full Completion Plan Coverage Report

## Purpose

This report proves that `docs/superpowers/plans/2026-07-08-schedule-task-workbench-full-completion.md` covers the complete scope of:

- `docs/superpowers/specs/2026-07-08-schedule-task-workbench-design.md`
- `docs/superpowers/plans/2026-07-08-schedule-task-workbench-foundation.md`

The report proves plan coverage. It does not claim the implementation is already complete.

## Branch And Base

- Plan branch: `codex/schedule-task-completion-plan`
- Worktree: `C:\pim-plan`
- Base: latest `origin/master` at plan creation
- Reason for short path: project-local `.worktrees` checkout failed on Windows because tracked Android `build/intermediates` paths exceeded path length limits; `C:\pim-plan` avoids that failure.

## Foundation Plan Coverage

| Foundation requirement | Covered by new plan |
| --- | --- |
| Task execution segments | Task 1 parity, Task 2 model, Task 4 planning services, Task 11 Web tasks/calendar |
| Calendar layer query | Task 1 parity, Task 4 all layers, Task 11 Web calendar |
| Events and task segments | Task 4 and Task 11 |
| Outlook-origin flags and source tags | Task 5 Graph sync, Task 6 source governance, Task 12 Sync/Data Center UI |
| Pending suggestion placeholders | Task 4 AI placeholders, Task 11 Calendar placeholder UI |
| L0-L4 risk levels | Task 1 parity, Task 3 confirmation enforcement |
| Confirmation API wrappers and panels | Task 3 operations endpoints, Task 10 Web contracts, Task 12 Confirmations page |
| Outlook settings | Task 5 token/connection, Task 12 Sync page |
| Device-code request contract | Task 5 real device-code flow, Task 12 Sync page |
| Sync batch log contract | Task 5 batch execution, Task 6 source governance, Task 12 Sync page |
| Data Center query | Task 9 full query/governance, Task 12 UI |
| Web workbench pages | Task 10 contracts/localization, Task 11 planning pages, Task 12 governance pages |
| Web verification scripts | Task 10, Task 11, Task 12, Task 16 |

## Former Deferred Items Are No Longer Deferred

| Former deferred item | New plan task |
| --- | --- |
| Full Microsoft token encryption/refresh implementation | Task 5 |
| Graph delta writeback execution | Task 5 |
| Conflict resolution execution | Task 6 |
| Audit version restore/export | Task 3 and Task 9 |
| Windows WebView2 embedding | Task 13 |
| Android WebView embedding | Task 14 |
| Native endpoint notification action execution | Task 7, Task 13, Task 14, Task 15 |

## Complete Design Coverage

| Design section | Required content | New plan coverage |
| --- | --- | --- |
| Goal | Shared schedule/task workbench across Web, Windows, Android, Outlook, reminders, reports, audit governance | Tasks 2-18 |
| Current Foundation | Extend Stage 5 without blind rewrite | Task 1 parity and every subsequent task preserves Stage 5 tests |
| Accepted Decisions | Shared core, Today command center, manual confirmation, hierarchy, multi-segment tasks, layers, Outlook Graph, endpoint shells, GA | Tasks 1-18 |
| Product Principles | Quiet command center, clear automation boundary, external identity preserved | Tasks 3, 5, 6, 9, 11, 12 |
| Core Objects | DomainProject, TaskBook, Task, Segment, Event, Habit, Reminder, Report, Confirmation, AuditVersion, SyncConnection, SyncItem, SyncBatch, RecycleBin | Tasks 2, 3, 5, 7, 8, 9 |
| Invariants | Task/event separation, multi-segment tasks, habit independence, confirmation before facts, source identity, soft delete | Tasks 2, 3, 4, 6, 9 |
| Risk Model | L0-L4, second-level, strict L4, detail fields | Task 3, Task 12 |
| Web Navigation | Today, Tasks, Calendar, Habits, Reminders, Reports, Data Center, Sync, Settings | Tasks 10-12 |
| Today Command Center | Calendar, segments, habits, placeholders, confirmations, sync conflicts, reminders, endpoints, reports, density modes | Task 4 backend, Task 11 Web |
| Calendar | Events, segments, habits, availability, placeholders, Outlook-only filter, non-final AI style | Task 4 backend, Task 11 Web |
| Tasks | Project/book hierarchy, subtasks, checklist, states, segments, audit/report/reminder/source links | Task 2 backend, Task 11 Web |
| Reminders | Trigger, risk, channels, DND, escalation, delivery/user history, related confirmation/report | Task 7 backend, Task 12 Web, Tasks 13-14 endpoints |
| Reports | Daily/weekly/monthly/project, planned vs actual, PC/mobile quality, habits, reminders, blockers, AI suggestions | Task 8 backend, Task 12 Web |
| Sync Settings | Connection, provider settings, batches, logs, conflicts, source tags, writeback, disconnect/reconnect | Tasks 5-6 backend, Task 12 Web |
| Outlook Graph | Official device code, token storage, delta, Graph event mapping, writeback | Task 5 |
| Source Tags | Outlook-only, batch tags, pause/stop sync, Graph/PIM ids, sync history, audit export | Task 6, Task 9, Task 12 |
| Conflict Handling | Keep PIM, keep Outlook, merge by field, copy, skip, stop sync | Task 6, Task 12 |
| ICS | Import preview, duplicate detection, export selected/date range, audit import report | Task 6 |
| Windows Endpoint | Collection, upload queue, settings, Toast, tray audit, embedded Web, status | Task 13, Task 15 |
| Android Endpoint | Permission center, app usage/location/device state, quality/upload, notifications, embedded Web, status | Task 14, Task 15 |
| Offline Boundary | Only collection cache offline; fact changes online only | Task 13, Task 14, Task 15 |
| Data Center | Search, filters, Outlook-only, pending, recycle, sync batches, audit timeline, restore, batch preview, export | Task 9, Task 12 |
| Services | Planning, Confirmation, Audit, Outlook, ICS, Reminder, Report, Endpoint, Data Center | Tasks 2-9, Task 15 |
| Key Flows | AI segment move, Outlook location change, endpoint high-risk notification, report suggestions | Task 4, Task 5, Task 7, Task 8, Task 13, Task 14 |
| Error Handling | Token, Graph, confirmation, audit, endpoint offline failures | Task 3, Task 5, Task 13, Task 14, Task 15 |
| Testing | Backend, frontend, endpoint tests, screenshot checks, GA | Tasks 16-18 |
| Implementation Governance | latest master, codex branch, goal mode, subagents, commits, local and GA validation | Plan Starting Point, Task 17, Task 18 |

## Verification Coverage

The plan requires these local verification commands:

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

The plan requires these GitHub Actions:

```powershell
gh workflow run build-api.yml --ref codex/schedule-task-complete-system
gh workflow run build-web.yml --ref codex/schedule-task-complete-system
gh workflow run build-windows.yml --ref codex/schedule-task-complete-system
gh workflow run build-android.yml --ref codex/schedule-task-complete-system
```

## Completeness Claim

The new plan contains no intentionally postponed product areas from the complete design. Every known foundation gap and every full-design section has a mapped implementation task, tests, and verification gate.

Implementation is still a separate step after plan approval.

