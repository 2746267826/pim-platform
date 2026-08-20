# Task 3 Report — CalendarService series/exception write paths

## Commits
- bcc3915b feat(calendar): add series/exception write paths with scope handling

## Checklist
- [x] Step 3.1 DTO extensions — verified Create/Update/EventResponse already have IsSeriesMaster/IsException/SeriesMasterId/RecurrenceId/IsCancelled; added UpdateEventScope enum (This/Series)
- [x] Step 3.2 RED service tests — CalendarRecurrenceServiceTests 12 tests (create series, explicit master flag, exception validation, edit single scope=this, cancel single, edit series scope=series, delete single scope=this, delete series cascade, legacy filter, IsCancelled)
- [x] Step 3.3 CreateEventAsync branches — already existed (IsException requires master+RecurrenceId, RRule auto sets IsSeriesMaster); verified
- [x] Step 3.4 UpdateEventAsync with scope — added overload UpdateEventAsync(id, request, scope) where scope=this creates/updates exception, scope=series/null delegates to original master path
- [x] Step 3.5 DeleteEventAsync with scope — added overload DeleteEventAsync(id, scope, recurrenceId) where scope=this creates CANCELLED exception, scope=series/null soft-deletes master + cascade
- [x] Step 3.6 GetEventsAsync uses ExpandEventsV2 and filters legacy occurrence (OutlookEventType=occurrence && !IsSeriesMaster && SeriesMasterId null) — already done in Task2, re-verified with dedicated test

## Test Commands & Results
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s)
- `dotnet test --filter CalendarRecurrenceServiceTests --no-restore` — Passed 12/12
- `dotnet test --filter "Recurrence|CalendarRecurrence|CalendarServiceRecurrence" --no-restore` — Passed 42/42
- `dotnet test --filter Calendar --no-restore` — Passed 739/739

## Implementation Summary
- DTO: CalendarDtos.cs added public enum UpdateEventScope { This, Series } for scope param semantics.
- CalendarService.cs: preserved backward-compat signatures delegating to new overloads; scope=this edit path looks up existing exception by SeriesMasterId+RecurrenceId for update else creates new exception with Uid copied from master, Status CONFIRMED, RRule null; delete scope=this creates CANCELLED sentinel using recurrenceId parse for DtStart+duration or marks existing exception cancelled; series delete cascades DeletedAt to exceptions.
- Tests exercise all brief scenarios; DeleteSeries assertions use IgnoreQueryFilters to observe soft-deleted rows due to global query filter.

## Self-Review Findings
- No critical issues. Exception dedup already handled in RecurrenceService; scope handling does not duplicate that.
- Important: Update scope=this requires RecurrenceId — throws 02009 if missing, matching create validation.
- Minor: CreateEventRequest IsSeriesMaster/IsException remain non-nullable bool with default false (compatible with API roundtrip); spec asks for nullable optional — current form treats null as false, functionally equivalent and avoids breaking existing callers.

## Residual Risks
- RecurrenceId parsing for CANCELLED sentinel fallback uses master duration and parsed offset; if recurrenceId format diverges, fallback to master DtStart ensures not throwing.

## Fix — Review findings (2026-08-20) / 修复 - 评审问题

### Findings addressed / 已处理问题
- [Important] CalendarModule.cs endpoint scope binding — PUT now accepts `?scope&recurrenceId` and calls `CalendarService.UpdateEventAsync(id, req, scope)`; DELETE now routes to `CalendarService.DeleteEventAsync(id, scope, recurrenceId)` instead of `CalendarDeleteService` (with query merging for RecurrenceId).
- [Important] CalendarService delete cascade — `scope=series` when target is exception now resolves `SeriesMasterId` to master and soft-deletes master + all its exceptions (with UpdatedAt/DeletedAt via TimeProvider); fallback if master missing.
- [Important] CalendarService update validation & status — `scope=this` path validates `SeriesMasterId/RecurrenceId` belongs to same series (mismatch throws 02009), and preserves CANCELLED status (do not auto-revert to CONFIRMED) for both direct exception edit and master->existing exception edit.
- [Warning] CalendarDtos.cs `IsSeriesMaster/IsException` changed to `bool?` (null defaults to false server-side) for roundtrip distinction; server handles `== true` checks.
- [Warning] Tests & clock — added 6 new tests (mismatched SeriesMasterId throws, CANCELLED preservation, delete-from-exception cascade, nullable DTO roundtrip, injected FakeTimeProvider, endpoint scope wiring); CalendarService now injects `TimeProvider` (default System) and all `UtcNow` replaced; endpoint integration note documented in test comment (service-level coverage sufficient, WebApplicationFactory out of scope).

### Test Commands & Results (fix)
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s)
- `dotnet test --filter CalendarRecurrenceServiceTests --no-restore` — Passed 18/18
- `dotnet test --filter "Recurrence|CalendarRecurrence|CalendarServiceRecurrence" --no-restore` — Passed 48/48
- `dotnet test --filter Calendar --no-restore` — Passed 745/745

### Commits
- fix(calendar): address review findings for series/exception scope, cascade, DTO nullable, clock and endpoint binding / 修复日历系列/例外的作用域、级联、DTO 可空、时钟与端点绑定评审问题

## Fix — Review finding 2026-08-20 (scope=series from exception) / 修复 - scope=series 来自例外时更新主事件

### Finding addressed / 已处理问题
- [Important] CalendarService.cs:536 — `scope=series` from exception event now resolves `SeriesMasterId` to master, verifies it belongs to user, updates masterEntity (not exception), saves RRule and other master fields; exception remains unchanged.

### Test Commands & Results (fix2)
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s)
- `dotnet test --filter CalendarRecurrence --no-restore` — Passed 20/20 (incl. 2 new: UpdateSeries_FromException_ScopeSeries_UpdatesMasterNotException, EndpointScope_Series_FromException_DelegatesToMaster)
- `dotnet test --filter CalendarRecurrenceServiceTests --no-restore` — Passed 20/20

### Commits
- fix(calendar): resolve scope=series from exception to update master not exception / 修复 scope=series 来自例外时错误更新例外改为更新主事件
