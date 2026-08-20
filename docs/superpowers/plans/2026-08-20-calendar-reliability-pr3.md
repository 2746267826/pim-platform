# Calendar Reliability PR3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 PR3：重复日程主模型——系列主事件 + 规范重复规则作为权威数据，occurrence 按规则即时生成，例外/取消独立持久化，提供从当前展开模型迁移的策略，并与 Outlook occurrence 双路径兼容。

**Architecture:** 保持唯一 `EventEntity` 表，新增 `IsSeriesMaster/IsException/SeriesMasterId` 三列（`RecurrenceId` 已存在）。系列主事件持有 `RRule+ExDatesJson`，普通 occurrence 不持久化，按 `RecurrenceService` 生成；例外/取消为 `IsException=true` 且 `SeriesMasterId` 指向主事件的独立行，通过 `(SeriesMasterId, RecurrenceId)` 唯一约束区分。查询层在 `CalendarService` 调用生成器后，用例外覆盖生成值，过滤 `legacy occurrence`（旧 Outlook occurrence 行）。前端在 `EventEditorDialog` 的“重复”折叠区接入图形化规则编辑器，API 通过 `scope=this|series` 区分单实例与系列操作。

**Tech Stack:** .NET 8, EF Core/Npgsql, Ical.Net, PostgreSQL, React 19, TypeScript, Tiptap, Luxon.

---

## Scope Decisions

- 新增列均 `nullable` 或带默认值，已有行不受影响；`Organizer` 仍保留 legacy 列。
- `SeriesMasterId` 为 `Guid?` 外键指向 `events.id`，不建强外键约束（`SetNull`），避免循环依赖，查询时校验归属。
- `IsSeriesMaster=false` 且无 `RRule` 的单次事件为普通事件，不走生成器。
- `RDATE` 不实现，`BYDAY/BYMONTH` 等高级规则仅支持基础子集（DAILY/WEEKLY/MONTHLY/YEARLY + INTERVAL + COUNT/UNTIL），与 spec 8.7 一致。
- 例外存储完整覆盖快照，不实现部分字段 patch。
- Outlook 普通 `occurrence` 行在迁移后标记为只读，不参与编辑/写回，查询时排除。
- PR3 阶段 A 并列运行，不删除旧数据；迁移脚本幂等可重跑。

---

### Task 1: Series/exception entity fields and compatible migration

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Entities/EventEntity.cs`
- Modify: `src/modules/Pim.Module.Calendar/Entities/CalendarEntityConfigurations.cs`
- Create (EF): `src/Pim.Infrastructure/Data/Migrations/*_AddRecurrenceMasterModel.cs`
- Modify: `src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs`
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceMasterModelTests.cs`

- [ ] **Step 1.1: Write RED tests for new columns**

Add `RecurrenceMasterModelTests` asserting `EventEntity` has `IsSeriesMaster:bool` (default false), `IsException:bool` (default false), `SeriesMasterId:Guid?` (nullable), `RecurrenceId:string?` persisted, and unique index on `(SeriesMasterId, RecurrenceId)` where `IsException=true`.

- [ ] **Step 1.2: Run RED** `dotnet test --filter RecurrenceMasterModelTests` → FAIL.

- [ ] **Step 1.3: Add entity fields**

```csharp
[Column("is_series_master")] public bool IsSeriesMaster { get; set; }
[Column("is_exception")] public bool IsException { get; set; }
[Column("series_master_id")] public Guid? SeriesMasterId { get; set; }
[ForeignKey(nameof(SeriesMasterId))] public EventEntity? SeriesMaster { get; set; }
```

Configure `HasDefaultValue(false)` for bools, nullable for `SeriesMasterId`, and unique index:

```csharp
builder.HasIndex(e => new { e.SeriesMasterId, e.RecurrenceId })
  .IsUnique()
  .HasFilter("\"is_exception\" = true AND \"series_master_id\" IS NOT NULL AND \"recurrence_id\" IS NOT NULL AND \"deleted_at\" IS NULL");
```

- [ ] **Step 1.4: Generate migration** `dotnet ef migrations add AddRecurrenceMasterModel --project src/Pim.Infrastructure --startup-project src/Pim.Api --output-dir Data/Migrations`

Migration must only add nullable/defaulted columns + index, and backfill:

```sql
UPDATE events SET is_series_master = true WHERE rrule IS NOT NULL AND btrim(rrule) <> '' AND is_series_master = false;
```

Down removes columns/index only.

- [ ] **Step 1.5: Run GREEN** `dotnet test --filter RecurrenceMasterModelTests` + `dotnet build Pim.sln --no-restore` → PASS.

- [ ] **Step 1.6: Commit** `feat: add recurrence master model fields`.

---

### Task 2: Occurrence generator with exception overlay

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs`
- Create: `src/modules/Pim.Module.Calendar/Services/RecurrenceRuleCodec.cs` (optional)
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceGeneratorTests.cs`
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceExceptionOverlayTests.cs`

- [ ] **Step 2.1: RED generator tests** covering DAILY/WEEKLY/MONTHLY/YEARLY + INTERVAL + COUNT/UNTIL, no-RRule single, ExDates exclusion.

- [ ] **Step 2.2: RED exception overlay tests** — master + two exceptions (one modified, one CANCELLED) → generated list replaces matching RecurrenceId, cancelled marked `isCancelled`.

- [ ] **Step 2.3: Implement `OccurrenceGenerator`** using `Ical.Net` already referenced: parse `RRule`, respect `ExDatesJson`, filter by `[rangeStart, rangeEnd)`, generate occurrences with `DeriveOccurrenceId`-like logic but keep `RecurrenceId` as ISO string of original start. Support `FREQ=DAILY|WEEKLY|MONTHLY|YEARLY`, `INTERVAL`, `COUNT`, `UNTIL`.

- [ ] **Step 2.4: Implement `ExpandEventsWithExceptions`** — load exceptions via `SeriesMasterId`, build map `RecurrenceId -> exceptionEntity`, generate from master then overlay.

- [ ] **Step 2.5: Keep legacy `ExpandEvents` for compatibility**, add new `ExpandEventsV2` used by `CalendarService`, keep logging.

- [ ] **Step 2.6: Run GREEN** tests + `dotnet test --filter Recurrence`.

---

### Task 3: CalendarService series/exception write paths

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Create: `tests/Pim.UnitTests/Calendar/CalendarRecurrenceServiceTests.cs`

- [ ] **Step 3.1: DTO extensions** — `CreateEventRequest/UpdateEventRequest` add `IsSeriesMaster?`, `IsException?`, `SeriesMasterId?`, `RecurrenceId?` (optional, default null) for API roundtrip; `EventResponse` add `IsSeriesMaster/IsException/SeriesMasterId/RecurrenceId/IsCancelled` (derived from `Status=CANCELLED && IsException`).

- [ ] **Step 3.2: RED service tests** — create series (RRule+IsSeriesMaster), edit single occurrence → creates exception, cancel single → creates CANCELLED exception, edit series → updates master, delete single → cancelled exception, delete series → soft-delete master + exceptions.

- [ ] **Step 3.3: Implement `CreateEventAsync` branches** — if `RRule` present and `IsSeriesMaster` null → set `true`; if `IsException` true → require `SeriesMasterId + RecurrenceId` and verify master exists & belongs to same calendar user.

- [ ] **Step 3.4: Implement `UpdateEventAsync` with `scope` param** (enum `this|series` via additional DTO `UpdateEventScope`). `scope=series` updates master; `scope=this` creates/updates exception.

- [ ] **Step 3.5: Implement `DeleteEventAsync` with scope** — `scope=this` for occurrence → exception CANCELLED, `scope=series` or no RRule → soft-delete.

- [ ] **Step 3.6: Adjust `GetEventsAsync` to use `ExpandEventsV2` and filter legacy occurrences** (`OutlookEventType=occurrence` and `SeriesMasterId IS NULL`).

---

### Task 4: Migration scripts for legacy data (stage A/B)

**Files:**
- Modify: `src/Pim.Infrastructure/Data/Migrations/*_AddRecurrenceMasterModel.cs` (extend Up)
- Create: `src/Pim.Infrastructure/Data/Migrations/*_BackfillRecurrenceMaster.cs` (or extend same migration)
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceMigrationTests.cs`

- [ ] **Step 4.1: Extend migration SQL** — idempotent:
```sql
-- mark existing RRule rows as series master
UPDATE events SET is_series_master = true WHERE rrule IS NOT NULL AND btrim(rrule) <> '' AND is_series_master = false;
-- detect Outlook exceptions
UPDATE events SET is_exception = true, series_master_id = (SELECT id FROM events m WHERE m.outlook_event_id = events.outlook_series_master_id LIMIT 1)
WHERE outlook_event_type = 'exception' AND is_exception = false;
```

- [ ] **Step 4.2: Mark legacy普通 occurrence as read-only** — set `recurrence_metadata_json = jsonb_set(..., '{legacyOccurrence}', 'true')` where `outlook_event_type='occurrence'`.

- [ ] **Step 4.3: Tests for idempotency and rollback**.

---

### Task 5: Recurrence rule UI and calendar rendering

**Files:**
- Create: `src/client-web/src/components/calendar/RecurrenceRuleEditor.tsx`
- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Modify: `src/client-web/src/utils/calendarEvents.ts`
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Create: `tests/client-web/recurrenceRuleEditor.test.tsx`
- Create: `tests/client-web/calendarRecurrence.test.ts`

- [ ] **Step 5.1: RED frontend tests** — rule editor generates RRule for DAILY/WEEKLY/MONTHLY/YEARLY, interval, count/until; calendarEvents maps `IsException/IsCancelled` to greyed style and recurrence badge.

- [ ] **Step 5.2: Implement `RecurrenceRuleEditor`** — selects: Frequency (none/daily/weekly/monthly/yearly), Interval (1..30), Weekly ByDay checkboxes, Monthly by day, End condition (never/count/until). Converts to RFC5545 string via helper `rruleCodec`.

- [ ] **Step 5.3: Integrate into `EventEditorDialog` “重复” section** — PR2 already has read-only summary; PR3 replaces with editor bound to `rrule` + `IsSeriesMaster` toggle. For occurrence editing, show “编辑此实例 / 编辑整个系列” scope selector.

- [ ] **Step 5.4: Update `calendarEvents` to render series badge, grey cancelled, and handle `SeriesMasterId` grouping**.

- [ ] **Step 5.5:Wire tests into `test:schedule-workbench-complete`**.

---

### Task 6: Graph recurrence mapping (Outlook)

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/OutlookEventMapper.cs`
- Modify: `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs` (if needed)
- Create: `tests/Pim.UnitTests/Calendar/OutlookRecurrenceMappingTests.cs`

- [ ] **Step 6.1: Map Graph `recurrence` JSON → RRule + IsSeriesMaster + OutlookSeriesMasterId + OutlookEventType**, and reverse mapping on writeback via `OutlookEventWriteService`.

- [ ] **Step 6.2: Ensure `ExternalMetadataJson` retains original recurrence for diagnostics**.

---

### Task 7: Final verification and PR

**Files:**
- Modify: `docs/superpowers/plans/2026-08-20-calendar-reliability-pr3.md` (checkboxes)
- Modify: `docs/operations/microsoft-calendar-sync-acceptance.md` (if needed)

- [ ] **Step 7.1: Run focused backend** `dotnet test --filter Recurrence`
- [ ] **Step 7.2: Run full** `dotnet test Pim.sln --no-restore` + `npm --prefix src/client-web run test:schedule-workbench-complete` + `build`
- [ ] **Step 7.3: Global lint** `npm --prefix src/client-web run lint` (baseline) + targeted lint 0 errors
- [ ] **Step 7.4: Commit docs** `docs: record calendar PR3 acceptance`
- [ ] **Step 7.5: Push and open PR** `gh pr create --base master --head opencode-linux/cal-pr3 --title "feat: recurrence master model (calendar reliability PR3)"`
- [ ] **Step 7.6: Monitor CI** `gh pr checks --watch`
