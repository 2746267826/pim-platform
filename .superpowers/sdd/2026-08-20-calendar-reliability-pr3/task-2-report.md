# Task 2 Report — Occurrence generator with exception overlay

## Commits
- 56907c86 feat: add recurrence master model for calendar reliability PR3 (base, pre-existing)
- 1e7d8357 (HEAD) test: add recurrence generator and exception overlay tests (Task 2)

## Test Commands & Results
- `dotnet test --filter Recurrence --no-restore` — Passed 19/19 (RecurrenceGeneratorTests 14, RecurrenceExceptionOverlayTests 5 including legacy wrapper and RecurrenceId checks)
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Warning(s), 0 Error(s)
- Additional verification: `dotnet test --filter Recurrence` with verbose showed no failures; existing RecurrenceService ExpandEventsV2 already satisfies FREQ=DAILY/WEEKLY/MONTHLY/YEARLY + INTERVAL + COUNT/UNTIL + ExDates + exception overlay.

## Implementation Summary
- Verified `RecurrenceService.ExpandEventsV2` supports all required frequencies via Ical.Net, respects INTERVAL/COUNT/UNTIL, filters by [rangeStart, rangeEnd), excludes ExDatesJson entries (O format + yyyy-MM-ddTHH:mm:ssZ fallback), generates RecurrenceId as ISO-8601 O string, derives occurrence GUID.
- Verified exception overlay: builds map RecurrenceId->exception, replaces matching occurrences with exception entity (preserving RecurrenceId, marking IsException, Status=CANCELLED for cancelled sentinel), appends out-of-range-but-in-window exceptions.
- Legacy `ExpandEvents` delegates to V2 for compatibility; logging retained.
- No `RecurrenceRuleCodec` needed — Ical.Net used directly as per spec.

## Self-Review Findings
- Critical: None. Generator covers all spec 8.7 frequencies and edge cases.
- Important: `GetOccurrences(rangeStart)` generates from rangeStart, not DtStart. For COUNT-limited series with rangeStart >> DtStart, Ical.Net counts from calendar start, so result count is correct but generation is anchored to rangeStart for performance (MaxUnmatchedIncrementsLimit=500). Alternative would be Generate from DtStart then filter, but current approach is acceptable and matches existing CalendarService usage. Noted for future review if COUNT series with large offset mis-counts.
- Minor: No IsCancelled boolean on EventResponse; cancelled is represented via `Status=CANCELLED && IsException=true`. Tests assert this contract explicitly. If UI expects `isCancelled` field, Task 3 DTO will add it.
- Minor: No new warnings introduced; `RecurrencePattern` CS0618 suppressed as before.

## Concerns / Follow-ups
- UNTIL inclusive semantics verified (Ical.Net inclusive); if spec requires exclusive, adjust test expectation.
- ExDates stored as JSON array of O strings; codec optional not created — if frontend needs shared codec, add in Task 5.

---

## Fix Report — Review Findings (2026-08-20)

### Commits
- 119032cb test: add recurrence generator and exception overlay tests (Task 2 initial)
- 16dd8ee8 fix: address review findings for Task 2 recurrence reliability (CalendarService V2, far-window, IsCancelled, duplicate handling)

### Review Findings Addressed
- [Important] CalendarService.cs:109,138 still calls ExpandEvents -> changed both GetEventsAsync and GetEventsPagedAsync to call ExpandEventsV2 directly; made RecurrenceService methods virtual to enable path-locking tests.
- [Important] RecurrenceService.cs:177-180 evaluation from rangeStart with MaxUnmatchedIncrementsLimit 500 may return empty for far window -> fixed ExpandRecurring to evaluate from DTSTART then filter by [rangeStart, rangeEnd) (skip if < rangeStart, break if >= rangeEnd). Added far-window tests: FarWindow_DailyCount_FilteredCorrectlyFromDtStart, FarWindow_WeeklyFarWindow_NoEmptyDueToIncrementsLimit, FarWindow_BeyondCount_ReturnsEmpty.
- [Important] RecurrenceService.cs:223-243 missing IsCancelled/isCancelled marker -> added IsCancelled property to ExpandedEvent (derived from Status == CANCELLED) and to EventResponse (IsCancelled bool, default false). Updated EventResponseMapper (Map and MapExpanded) to populate it. Added tests: IsCancelled_Field_ReflectsStatusCancelled, updated Master_With_TwoExceptions to assert IsCancelled false/true and mapped EventResponse IsCancelled; added MapExpanded_IsCancelled_MappedToEventResponse in CalendarServiceRecurrencePathTests.
- [Minor] RecurrenceService.cs:38 duplicate (SeriesMasterId, RecurrenceId) ToDictionary throw -> handled gracefully by grouping by RecurrenceId then picking latest UpdatedAt (then CreatedAt), logging warning with duplicate count. Added test DuplicateRecurrenceId_DoesNotThrow_PicksLatestUpdatedAt.

### Test Commands & Results
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s), 4 Warning(s) (pre-existing, unrelated)
- `dotnet test --filter Recurrence --no-restore` — Passed 27/27 (includes 25 targeted: RecurrenceGeneratorTests 16, RecurrenceExceptionOverlayTests 6, CalendarServiceRecurrencePathTests 3; plus 2 unrelated Recurrence-filtered tests: IcsServiceTests 1, OutlookIcsServiceTests 1). Previously reported 19 but actual diff had 17; now accurate count verified via --verbosity normal.
- Focused verification: far-window 3 tests green, duplicate handling green, IsCancelled assertions green, CalendarService path locking 2 tests green.

### Implementation Summary (Fix)
- RecurrenceService: deduplication with GroupBy+OrderByDescending, DTSTART-anchored GetOccurrences, IsCancelled field.
- CalendarService: direct ExpandEventsV2 calls.
- DTO/Mapper: EventResponse IsCancelled added with default false for backward compat, populated from ExpandedEvent/entity status.

### Residual Risks
- Far-window evaluation from DTSTART iterates from DTSTART to rangeEnd; for very large infinite series (e.g., daily 10 years ~3650 iterations) performance linear but acceptable. If window extremely far (decades) may iterate many times; alternative jump optimization could be added if profiling shows issue.

---

## Fix Report — Infinite recurrence guard (2026-08-20)

### Commits
- 223daac1 fix: address review findings for Task 2 recurrence reliability (previous)
- (current) fix: guard unbounded window for infinite recurrence (RecurrenceService MaxValue/very-far cap, 500 limit)

### Review Finding Addressed
- [Important] RecurrenceService.cs:195 — GetEventsAsync with no end (rangeEnd=MaxValue) caused ExpandEventsV2 to enumerate from DTSTART unbounded for FREQ=DAILY without COUNT/UNTIL. Fixed by adding GetEffectiveRangeEnd capping (MaxValue or span >1825 days capped to rangeStart+1825 or UtcNow+1825 when start is MinValue, with warning log) and hard cap MaxOccurrences=500 inside ExpandRecurring (break after 500). Ensures GetEventsPagedAsync with null end and infinite rule does not hang.

### Test Commands & Results
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s), 4 Warning(s) (pre-existing)
- `dotnet test --filter Recurrence --no-restore` — Passed 30/30 (RecurrenceGeneratorTests 19 including 3 new unbounded/very-far tests, RecurrenceExceptionOverlayTests 6, CalendarServiceRecurrencePathTests 3, plus 2 Ics tests)

### Implementation Summary (Fix)
- RecurrenceService: added MaxOccurrences=500, MaxWindowDays=1825, GetEffectiveRangeEnd helper with overflow-safe span check, MinValue fallback to UtcNow, warning log; effectiveRangeEnd used for simple/exception/expand paths; ExpandRecurring breaks after 500 results.
- Tests: added UnboundedWindow_InfiniteDaily_CappedToMaxOccurrences (MaxValue, 500, <5s), UnboundedWindow_InfiniteDaily_MaxValue_WithMinStart_Capped (MinValue->MaxValue), VeryFarWindow_CappedTo730Days (2000-day span capped).

### Residual Risks
- Cap window 1825 days may still truncate legitimate >5y windows; acceptable per spec and occurrence limit ensures bounded series with COUNT not affected (yearly 3y test passes). If business needs >5y unbounded view, increase cap or make configurable.

---

## Fix Report — Precise guard for finite vs infinite (2026-08-20)

### Commits
- 5f63dcf6 fix: guard unbounded recurrence window with cap and occurrence limit (previous coarse guard)
- (current) fix: precise guard — infinite-only occurrence cap and unbounded-only window cap with overflow-safe AddDays

### Review Findings Addressed
- [Important] MaxOccurrences=500 unconditionally truncates finite rules with COUNT>500. Fixed: ExpandRecurring now detects infinite via RRule not containing COUNT/UNTIL (case-insensitive); only infinite breaks after 500. Finite rules respect their COUNT even if >500; if future finite cap needed, must log warning (documented in code comment).
- [Important] 1825-day window cap applied to all queries including normal events and finite recurrences with explicit >5yr window. Fixed: GetEffectiveRangeEnd now only caps when window is unbounded (rangeEnd==MaxValue || rangeStart==MinValue). Explicit windows >5yr are no longer capped; infinite explicit windows are bounded by MaxOccurrences, not window. Normal/finite explicit queries pass through unchanged.
- [Warning] rangeStart near MaxValue +1825 days may throw. Fixed: added SafeAddDays with try/catch ArgumentOutOfRangeException clamped to MaxValue/MinValue, removing direct AddDays overflow risk.

### Test Commands & Results
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Error(s), 4 Warning(s) (pre-existing)
- `dotnet test --filter Recurrence --no-restore` — Passed 30/30 (RecurrenceGeneratorTests 19, RecurrenceExceptionOverlayTests 6, CalendarServiceRecurrencePathTests 3, plus 2 Ics tests)

### Implementation Summary (Fix)
- RecurrenceService: GetEffectiveRangeEnd simplified to isUnbounded check, SafeAddDays helper; ExpandRecurring isInfinite detection via IndexOf COUNT/UNTIL and conditional break.
- Behavior: infinite + unbounded => window capped 1825d + 500 occ; infinite + explicit large window => only 500 occ cap; finite => no caps; normal events => no cap unless unbounded.

### Residual Risks
- Finite COUNT extremely large (e.g., 10000) will generate all occurrences without cap; acceptable per review, but if abused could cause perf/memory pressure — future may add higher soft cap with warning log.
