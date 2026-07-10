# Android Client Complete Reliability Coverage

Baseline date: 2026-07-10

## Phase 1 Baseline

| Command | Outcome | Evidence |
| --- | --- | --- |
| `git status --short --branch` | Pass | Clean `codex/android-operational-foundation` worktree at `32a7ab5b4bb5843d8a8f2a46fd8a73a833de1a46`, tracking `origin/master`. |
| `git rev-list --left-right --count master...origin/master` | Pass | `0 0`. |
| `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pim.UnitTests.Mobile"` | Pass | 80 passed, 0 failed, 0 skipped, 80 total. Warnings: `CS8604` at `RecurrenceService.cs:65` and `CS8602` at `AuthEndpoints.cs:113`. |
| `Set-Location src/client-android; .\gradlew.bat testDebugUnitTest --no-daemon` | Pass | Exit code 0. The Gradle console exposed no test summary; generated JUnit XML recorded 30 suites and 85 tests: 85 passed, 0 failed, 0 errors, 0 skipped. |
| `Set-Location src/client-android; .\gradlew.bat :app:assembleDebug --no-daemon` | Pass | `BUILD SUCCESSFUL` in 15s; 84 actionable tasks (31 executed, 53 up-to-date). Warning: deprecated Gradle features are incompatible with Gradle 9.0. |

## Coverage Matrix

| ID | Requirement | Phase | Status | Evidence |
| --- | --- | --- | --- | --- |
| REL-01 | Overall operational health uses live evidence | 1 | Planned | Phase 1 Task 12 |
| REL-02 | Every Status action performs its label | 1 | Planned | Phase 1 Tasks 10 and 13 |
| REL-03 | Manual sync gives immediate persistent feedback | 1 | Planned | Phase 1 Tasks 4-7 and 13 |
| REL-04 | Exactly one periodic sync job exists | 1 | Planned | Phase 1 Task 7 |
| REL-05 | Boot/update restores work and collection intent | 1 | Planned | Phase 1 Task 8 |
| REL-06 | Logs are local-only and excluded from upload total | 1 | Planned | Phase 1 Tasks 4 and 11 |
| REL-07 | Settings validate, persist, apply, roll back, restore | 1 | Planned | Phase 1 Task 9 |
| REL-08 | Diagnostic ZIP includes approved facts and excludes secrets | 1 | Planned | Phase 1 Task 11 |
| REL-09 | Today and Tracks show server-only data plus native transfer state | 2 | Planned | Phase 2 Tasks 1-6 and 9-10 |
| REL-10 | Web authentication/navigation use trusted origin | 2 | Planned | Phase 2 Tasks 1 and 8-10 |
| REL-11 | Schedule policy and stale/error states use real evidence | 3 | Planned | Phase 3 Tasks 1-5 |
| REL-12 | Room 3->4 preserves business data and settings | 1 | Planned | Phase 1 Task 4 |
| REL-13 | Required automated commands pass | 3 | Planned | Phase 3 Task 7 |
| REL-14 | Relevant GitHub Actions pass or non-trigger is documented | 3 | Planned | Phase 3 Tasks 7 and 9 |
| REL-15 | Signed APK passes the physical-device matrix | 3 | Planned | Phase 3 Task 8 |
| REL-16 | Final coverage report has no unverified row | 3 | Planned | Phase 3 Task 9 |
