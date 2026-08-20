# Task 6 Report — Graph recurrence mapping (Outlook)

## Commits
- feat(calendar): map Graph recurrence to RRule and build Graph recurrence on writeback / 日历 Graph 重复规则与 RRule 双向映射

## Checklist
- [x] Step 6.1 Map Graph recurrence JSON → RRule + IsSeriesMaster + OutlookSeriesMasterId + OutlookEventType, and reverse mapping on writeback via OutlookEventWriteService
- [x] Step 6.2 Ensure ExternalMetadataJson retains original recurrence for diagnostics

## Test Commands & Results
- `dotnet build Pim.sln --no-restore` — Build succeeded, 0 Warning(s), 0 Error(s)
- `dotnet test --filter OutlookRecurrenceMapping --no-restore` — Passed 11/11 (RED→GREEN)
- `dotnet test --filter OutlookEventWriteServiceTests --no-restore` — Passed 99/99 (NonemptyRRule now verifies recurrence payload)
- `dotnet test --filter Recurrence --no-restore` — Passed 62/62

## Implementation Summary
- OutlookEventMapper.cs: Added `MapRecurrenceToEntity` to translate Graph `recurrence.pattern` (daily/weekly/absoluteMonthly/absoluteYearly + interval) and `range` (noEnd/numbered/endDate) ↔ RFC5545 RRule (`FREQ=DAILY;INTERVAL=2;COUNT=5` / `UNTIL=YYYYMMDDTHHMMSSZ`). Sets `IsSeriesMaster/IsException/RRule/OutlookSeriesMasterId/OutlookEventType` accordingly; retains raw JSON in `GraphRecurrenceJson` and full event in `ExternalMetadataJson.sourceSnapshot.event`.
- OutlookEventMapper.cs: Extended `BuildWritePayload` to emit `recurrence` (pattern+range) when `RRule` present; maps FREQ→graph pattern type and INTERVAL/COUNT/UNTIL→range.
- OutlookEventWriteService.cs: Removed blanket `RRule` ban ("不支持创建或修改重复日程规则"), kept whitespace guard; writeback now forwards RRule via `BuildWritePayload`.
- OutlookEventWriteServiceTests.cs: Updated `NonemptyRRule_Rejected` → `NonemptyRRule_Allowed_SendsGraphRecurrence` to assert recurrence payload.
- OutlookRecurrenceMappingTests.cs: Created 11 RED tests covering seriesMaster daily/weekly/monthly/yearly, external metadata retention, exception/singleInstance mapping, and writeback payload generation.

## Self-Review Findings
- No critical issues. Unsupported pattern types return null RRule safely.
- UNTIL conversion uses master DtStart UTC time; whitespace RRule still rejected with 02009.

## Residual Risks
- Graph `relativeMonthly/relativeYearly` mapped to MONTHLY/YEARLY without BYDAY details (spec subset); future extension may need BYDAY handling.
