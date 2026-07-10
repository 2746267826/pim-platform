# Android Client Reliability Phase 1 Operational Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付真实可操作的 Android 状态中心、持久同步运行、唯一调度器、可配置采集、权限动作、连接探测和本地诊断导出，并通过无损 Room 3→4 升级消除虚假的上传队列。

**Architecture:** Room v4 保存业务队列、同步运行、执行租约、dead letter、诊断和日程缓存；`SyncRequestBroker` 把 manual/foreground/periodic/retry 合并到一个 immediate chain 和一个 periodic fallback，`SyncOrchestrator` 只协调独立步骤。Status 和 Settings 从 typed repositories 读取事实，所有外部动作由明确 executor 执行；日志只留本地，由白名单 ZIP exporter 分享。

**Tech Stack:** Kotlin, Jetpack Compose Material3, Hilt, Room 2.6.1, WorkManager 2.9.0, Retrofit/OkHttp 4.12, MockWebServer, AndroidX lifecycle-process, Android instrumentation, Compose UI test, .NET 8 Minimal API, EF Core, xUnit, JUnit4.

---

## Final Objective

Phase 1 结束时，即使 Today、Tracks、Schedule 仍等待后续阶段，用户也能从 Status 明确看到采集健康、传输阶段、业务待传数量、上次成功、失败原因和下一次尝试；“立即同步”、权限、服务、设置、诊断和导出按钮全部执行对应行为；重启或更新不会制造重复 worker，也不会默默清除采集意图。

## Preconditions

- 总计划：`docs/superpowers/plans/2026-07-10-android-client-complete-reliability.md`
- 设计规范：`docs/superpowers/specs/2026-07-10-android-client-complete-reliability-design.md`
- 从包含计划文档的最新 `origin/master` 创建独立 worktree。
- 分支固定为 `codex/android-operational-foundation`。
- `.opencode/`、`src/Pim.Api/wwwroot/`、Gradle build、APK、诊断 ZIP 不进入提交。

## File Structure Map

### Shared Android Contracts

- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncModels.kt`
  - Typed trigger, phase, outcome, counts, queue snapshot, failure, request.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncRunStore.kt`
  - Domain-to-Room mapping and persistent run transitions.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncExecutionGate.kt`
  - Lease acquire/renew/release and stale-run interruption.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncScheduler.kt`
  - Canonical WorkManager names, constraints, migration, WorkInfo projection.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncRequestBroker.kt`
  - Persist `Queued`, coalesce triggers, foreground cooldown, manual override.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncWorker.kt`
  - Single worker entry point for periodic and immediate execution.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncOrchestrator.kt`
  - Typed phase progression and terminal classification.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/DeviceRegistrationStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/UsageSyncStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/LocationSyncStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/HeartbeatStep.kt`
- Delete after callers migrate: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationSyncWorker.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/daemon/UploadWorker.kt`

### Room And Diagnostics

- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncEntities.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncRunDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncDeadLetterDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheEntity.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/DiagnosticDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- Create and commit: `src/client-android/app/schemas/com.pim.app.data.AppDatabase/3.json`
- Create and commit: `src/client-android/app/schemas/com.pim.app.data.AppDatabase/4.json`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/data/PimDatabaseMigrationTest.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticRetentionManager.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticExportValidator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticExporter.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticShareController.kt`
- Create: `src/client-android/app/src/main/res/xml/diagnostic_file_paths.xml`

### Status, Settings, Permissions, Startup

- Create: `src/client-android/app/src/main/java/com/pim/app/status/OperationalStatusModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/OperationalStatusRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssuePlanner.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusActionExecutor.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt`
- Replace responsibilities in: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/diagnostics/DiagnosticsViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/diagnostics/DiagnosticsScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingPresetCatalog.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/SettingsApplyCoordinator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/SystemPrerequisiteRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/SystemAction.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/StartupRecoveryRecordStore.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/StartupRecoveryCoordinator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/BootUpdateReceiver.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/AppForegroundObserver.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/PimApp.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`

### API Contracts

- Create: `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs`
- Modify: `src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs`
- Modify: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt`
- Test: `tests/Pim.UnitTests/Api/VersionEndpointTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs`
- Test: `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`

### Build, Wiring, Tests, And Reports

- Modify: `src/client-android/app/build.gradle.kts`
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncFailureClassifier.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt`
- Delete after callers migrate: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Replace responsibilities in: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/mobile/sync/SyncExecutionGateTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/diagnostics/DiagnosticsContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/settings/SettingsContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterContentTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticExporterTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticExportValidatorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticRetentionManagerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncFailureClassifierTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncOrchestratorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncRequestBrokerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncRunStoreTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncSchedulerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncTestFixtures.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/permissions/SystemPrerequisiteRepositoryTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/settings/SettingsApplyCoordinatorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingPresetCatalogTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsValidatorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/startup/AppForegroundObserverTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/startup/StartupRecoveryCoordinatorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/OperationalHealthEvaluatorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/StatusActionExecutorTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssuePlannerTest.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt`
- Create: `src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt`
- Create: `src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt`
- Create: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- Create: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-1.md`
- Modify: `scripts/build-android.bat`

## Shared Types

Use these exact names across every task:

```kotlin
enum class SyncTrigger { Manual, Foreground, Periodic, Retry }

enum class SyncPhase {
    Queued,
    CheckingPrerequisites,
    WaitingForNetwork,
    WaitingForAllowedNetwork,
    RegisteringDevice,
    QueryingGaps,
    CollectingUsage,
    UploadingUsage,
    UploadingLocations,
    ReportingHeartbeat,
    Verifying,
    Succeeded,
    SucceededWithRejects,
    RetryScheduled,
    Blocked,
    Failed,
    Interrupted
}

enum class SyncTerminalOutcome { Succeeded, SucceededWithRejects, Blocked, Failed, Interrupted }

enum class UploadNetworkPolicy { AnyConnected, Unmetered }

data class NetworkFacts(val connected: Boolean, val metered: Boolean)

@Serializable
data class SyncCategoryCounts(
    val attempted: Int = 0,
    val accepted: Int = 0,
    val skipped: Int = 0,
    val rejected: Int = 0,
    val failed: Int = 0,
    val serverConfirmed: Int = 0
)

@Serializable
data class BusinessQueueSnapshot(
    val pendingLocations: Int,
    val pendingUsageEvents: Int,
    val pendingUsageSummaries: Int,
    val pendingAppMetadata: Int,
    val oldestPendingAtUtcMillis: Long?,
    val approximateBytes: Long?
) {
    val total: Int
        get() = pendingLocations + pendingUsageEvents + pendingUsageSummaries + pendingAppMetadata
}

object MobileSyncStatus {
    const val PENDING = "pending"
    const val SYNCING = "syncing"
    const val SYNCED = "synced"
    const val FAILED = "failed"
    const val DEAD_LETTER = "dead-letter"
    const val LOCAL_ONLY = "local-only"
}
```

## Task 0: Create The Phase Worktree And Coverage Baseline

**Files:**
- Create: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`

- [ ] **Step 1: Create the isolated worktree**

Invoke `superpowers:using-git-worktrees`, then create `codex/android-operational-foundation` from the latest `origin/master`. Confirm:

```powershell
git status --short --branch
git rev-list --left-right --count master...origin/master
```

Expected: the feature worktree is clean and the ref count is `0 0` before edits.

- [ ] **Step 2: Run the Phase 1 baseline**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pim.UnitTests.Mobile"
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

Expected: record exact pass/fail counts. A pre-existing unrelated failure may be documented, but no Phase 1 completion claim is allowed while a touched surface fails.

- [ ] **Step 3: Create the coverage report with fixed requirement IDs**

Write this table header and rows; execution updates only Status/Evidence columns:

```markdown
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
| REL-12 | Room 3→4 preserves business data and settings | 1 | Planned | Phase 1 Task 4 |
| REL-13 | Required automated commands pass | 3 | Planned | Phase 3 Task 7 |
| REL-14 | Relevant GitHub Actions pass or non-trigger is documented | 3 | Planned | Phase 3 Tasks 7 and 9 |
| REL-15 | Signed APK passes the physical-device matrix | 3 | Planned | Phase 3 Task 8 |
| REL-16 | Final coverage report has no unverified row | 3 | Planned | Phase 3 Task 9 |
```

- [ ] **Step 4: Commit the baseline report**

```powershell
git add docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md
git commit -m "docs: add android reliability coverage matrix"
```

## Task 1: Add Android Behavior-Test Infrastructure And Capture Room Schema 3

**Files:**
- Modify: `src/client-android/app/build.gradle.kts`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- Create: `src/client-android/app/schemas/com.pim.app.data.AppDatabase/3.json`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt`

- [ ] **Step 1: Add a failing instrumentation smoke test**

```kotlin
package com.pim.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidInstrumentationSmokeTest {
    @Test
    fun applicationIdMatchesProductionPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.pim.app", context.packageName)
    }
}
```

- [ ] **Step 2: Run the test task and verify the missing infrastructure failure**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Expected: FAIL because Android test dependencies and the app runner are not configured.

- [ ] **Step 3: Configure instrumentation, WorkManager, MockWebServer, Compose, and Room schema export**

Add the serialization plugin beside `kotlin("kapt")` because Room run/probe snapshots use `kotlinx.serialization`:

```kotlin
kotlin("plugin.serialization")
```

Add to `android.defaultConfig` and `android` in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    javaCompileOptions {
        annotationProcessorOptions {
            arguments["room.schemaLocation"] = "$projectDir/schemas"
        }
    }
}

sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")

testOptions {
    unitTests.isIncludeAndroidResources = true
}
```

Add these dependencies:

```kotlin
implementation("androidx.lifecycle:lifecycle-process:2.6.2")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
testImplementation("androidx.work:work-testing:2.9.0")
testImplementation("androidx.room:room-testing:2.6.1")
testImplementation("androidx.test:core-ktx:1.5.0")
testImplementation("org.robolectric:robolectric:4.12.2")
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
androidTestImplementation("androidx.room:room-testing:2.6.1")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
```

Annotate JVM test classes that call `ApplicationProvider`, Room, or Android `SharedPreferences` with:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
```

Change `AppDatabase` to `exportSchema = true` while it is still version 3.

- [ ] **Step 4: Generate and inspect schema 3**

```powershell
.\gradlew.bat :app:kaptDebugKotlin --no-daemon
Get-Content -Raw app\schemas\com.pim.app.data.AppDatabase\3.json | ConvertFrom-Json | Out-Null
```

Expected: both commands pass and schema 3 lists all current business, log, policy, batch, and device tables.

- [ ] **Step 5: Compile the instrumentation test**

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Expected: PASS.

- [ ] **Step 6: Commit the test foundation before changing the schema version**

```powershell
git add src/client-android/app/build.gradle.kts src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt src/client-android/app/schemas src/client-android/app/src/androidTest
git commit -m "test: add android behavior test foundation"
```

## Task 2: Add Item-Level Mobile Ingest Acknowledgement And Capability Contract

**Files:**
- Create: `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- Modify: `src/Pim.Api/Program.cs`
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs`
- Modify: `src/modules/Pim.Module.Mobile/Entities/MobileSyncBatchEntity.cs`
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileUsageIngestServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs`
- Create: `tests/Pim.UnitTests/Api/VersionEndpointTests.cs`
- Modify: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt`

- [ ] **Step 1: Write failing backend tests for item results and the Phase 1 capability**

Add a service test that sends one new event and one duplicate, each with a client key:

```csharp
[Fact]
public async Task IngestAsync_ReturnsStableResultForEverySentItem()
{
    await using var db = MobileTestHelpers.CreateDb();
    var service = CreateService(db);
    var request = UploadRequest("batch-items", "Messages") with
    {
        Events =
        [
            Event("event-1", "2026-07-06T08:05:00Z"),
            Event("event-2", "2026-07-06T08:05:00Z")
        ]
    };

    var result = await service.IngestAsync(request, CancellationToken.None);

    Assert.Equal(2, result.ItemResults.Count);
    Assert.Equal("accepted", result.ItemResults.Single(x => x.ClientItemKey == "event-1").Outcome);
    Assert.Equal("skipped", result.ItemResults.Single(x => x.ClientItemKey == "event-2").Outcome);
    Assert.Equal(result.ItemResults.Count(x => x.Outcome == "accepted"), result.AcceptedCount);
    Assert.Equal(result.ItemResults.Count(x => x.Outcome == "skipped"), result.SkippedCount);
}

private static MobileUsageIngestService CreateService(PimDbContext db) => new(
    db,
    MobileTestHelpers.CurrentUser(),
    new MobileSessionInterpreter(db),
    MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-06T12:00:00Z")));

private static MobileUsageEventDto Event(string clientItemKey, string timestamp)
{
    var occurredAt = DateTimeOffset.Parse(timestamp);
    return new MobileUsageEventDto(
        "com.example.messages",
        "USER_INTERACTION",
        occurredAt,
        null,
        occurredAt,
        "{}",
        clientItemKey);
}
```

In `VersionEndpointTests`, assert `mobileItemResultsV1` exists and `androidEmbedV1` does not yet exist.

- [ ] **Step 2: Run focused backend tests and verify failure**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileUsageIngestServiceTests|FullyQualifiedName~VersionEndpointTests|FullyQualifiedName~MobileWebContractTests"
```

Expected: FAIL because `ClientItemKey`, `ItemResults`, and typed version capabilities do not exist.

- [ ] **Step 3: Extend DTOs without breaking older clients**

Use optional trailing keys and these exact response types:

```csharp
public sealed record MobileIngestItemResult(
    string ClientItemKey,
    string EntityType,
    string Outcome,
    string Code,
    string Message);

public sealed record MobileUsageIngestResult(
    string BatchId,
    int AcceptedCount,
    int SkippedCount,
    int RejectedCount,
    int FailedCount,
    IReadOnlyList<MobileIngestItemResult> ItemResults);
```

Add `string? ClientItemKey = null` as the last parameter of `MobileAppMetadataDto`, `MobileUsageEventDto`, and `MobileUsageSummaryDto`. When a legacy client omits it, derive a deterministic natural key; never return an empty key.

- [ ] **Step 4: Return one explicit result per app, event, and summary**

Implement these outcome rules in `MobileUsageIngestService`:

```csharp
private static MobileIngestItemResult Item(
    string clientItemKey,
    string entityType,
    string outcome,
    string code,
    string message)
    => new(clientItemKey, entityType, outcome, code, message);
```

- new valid row or successful upsert: `accepted` / `accepted`;
- existing identical event or unchanged natural-key row: `skipped` / `duplicate`;
- item validation failure: `rejected` / stable validation code;
- unexpected per-item persistence failure: `failed` / `persistence-failed` and preserve the transaction failure semantics;
- aggregate counts equal the grouped `ItemResults` counts for all three entity types;
- a repeated `ClientBatchId` returns the originally stored item results rather than reconstructing ambiguous aggregate counts. Reuse the existing PostgreSQL JSONB `MobileSyncBatchEntity.ErrorJson` column as a versioned response envelope; do not add or rename a database column.

Use this exact envelope for both successful and failed batches:

```csharp
public sealed record MobileSyncBatchEnvelope(
    int SchemaVersion,
    IReadOnlyList<MobileIngestItemResult> ItemResults,
    IReadOnlyList<string> BatchErrors);
```

Serialize `new MobileSyncBatchEnvelope(1, result.ItemResults, batchErrors)` into `ErrorJson`. On duplicate batch lookup, deserialize schema version 1 and return its `ItemResults`; a missing/unknown envelope is an aggregate-only legacy response and must not fabricate per-item results. Add a duplicate-batch test that verifies the second call returns byte-for-byte equivalent item results.

```csharp
[Fact]
public async Task IngestAsync_RepeatedBatchReturnsPersistedItemResults()
{
    await using var db = MobileTestHelpers.CreateDb();
    var service = CreateService(db);
    var request = UploadRequest("batch-repeat", "Messages");

    var first = await service.IngestAsync(request, CancellationToken.None);
    var second = await service.IngestAsync(request, CancellationToken.None);
    var batch = await db.Set<MobileSyncBatchEntity>().SingleAsync();
    var envelope = JsonSerializer.Deserialize<MobileSyncBatchEnvelope>(batch.ErrorJson)!;

    Assert.Equal(1, envelope.SchemaVersion);
    Assert.Equal(
        JsonSerializer.Serialize(first.ItemResults),
        JsonSerializer.Serialize(second.ItemResults));
    Assert.Equal(
        JsonSerializer.Serialize(first.ItemResults),
        JsonSerializer.Serialize(envelope.ItemResults));
}
```

- [ ] **Step 5: Add a typed version endpoint with only the shipped Phase 1 capability**

```csharp
namespace Pim.Api.Endpoints;

public sealed record ApiVersionResponse(string Version, IReadOnlyList<string> Capabilities);

public static class VersionEndpoints
{
    public const string MobileItemResultsV1 = "mobileItemResultsV1";
    public static IReadOnlyList<string> Capabilities { get; } = [MobileItemResultsV1];

    public static IEndpointRouteBuilder MapVersionEndpoints(this IEndpointRouteBuilder endpoints)
    {
        endpoints.MapGet("/api/version", () =>
        {
            var version = typeof(Program).Assembly
                .GetCustomAttributes(false)
                .OfType<System.Reflection.AssemblyInformationalVersionAttribute>()
                .FirstOrDefault()?.InformationalVersion ?? "0.0.0(unknown)";
            return Results.Ok(new ApiVersionResponse(version, Capabilities));
        }).AllowAnonymous();
        return endpoints;
    }
}
```

Replace the inline `Program.cs` version mapping with `app.MapVersionEndpoints()`.

- [ ] **Step 6: Add Android request keys and response models**

```kotlin
@Serializable
data class MobileIngestItemResult(
    val clientItemKey: String,
    val entityType: String,
    val outcome: String,
    val code: String,
    val message: String
)

@Serializable
data class MobileIngestResponse(
    val batchId: String,
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0,
    val itemResults: List<MobileIngestItemResult> = emptyList()
)
```

Add nullable `clientItemKey` fields to the three Android upload DTOs. Events and summaries use their Room row ID string; app metadata uses `"${packageName}@${versionCode}"`.

- [ ] **Step 7: Write and implement the Android acknowledgement planner**

The failing test must prove partial results do not mark the whole batch synced:

```kotlin
@Test
fun partialResponseSeparatesConfirmedRetryAndDeadLetterKeys() {
    val plan = MobileAcknowledgementPlanner.plan(
        sentKeys = setOf("11", "12", "13"),
        response = MobileIngestResponse(
            batchId = "batch-1",
            itemResults = listOf(
                MobileIngestItemResult("11", "usage-event", "accepted", "accepted", "OK"),
                MobileIngestItemResult("12", "usage-event", "rejected", "invalid-time", "bad time"),
                MobileIngestItemResult("13", "usage-event", "failed", "temporary", "retry")
            )
        )
    )

    assertEquals(setOf("11"), plan.confirmedKeys)
    assertEquals(setOf("12"), plan.deadLetterKeys)
    assertEquals(setOf("13"), plan.retryKeys)
}
```

Planner behavior for aggregate-only servers: confirm the complete sent set only when `accepted + skipped == sentKeys.size` and `rejected == 0 && failed == 0`; otherwise return `server-ack-ambiguous` with every key retained.

- [ ] **Step 8: Run focused backend and Android tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~MobileUsageIngestServiceTests|FullyQualifiedName~VersionEndpointTests|FullyQualifiedName~MobileWebContractTests"
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.MobileAcknowledgementPlannerTest" --no-daemon
```

Expected: PASS; JSON contract includes `itemResults`, every sent item has a stable key, and Phase 1 advertises only `mobileItemResultsV1`.

- [ ] **Step 9: Commit the shared acknowledgement contract**

```powershell
git add src/Pim.Api src/modules/Pim.Module.Mobile tests/Pim.UnitTests src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileAcknowledgementPlannerTest.kt
git commit -m "feat: add durable mobile item acknowledgements"
```

## Task 3: Add Server Endpoint Resolution, Native Token Refresh, And Real Connection Probe

**Files:**
- Create: `src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt`
- Test: `src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/auth/AuthSessionStore.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt`
- Create: `src/client-android/core/src/test/java/com/pim/core/network/AuthInterceptorTest.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt`

- [ ] **Step 1: Write failing endpoint derivation tests**

```kotlin
@Test
fun derivesApiAndWebEndpointsFromConfiguredApiBase() {
    val endpoints = PimServerEndpoints.from("http://127.0.0.1:5858/api/v1/")

    assertEquals("http://127.0.0.1:5858/", endpoints.webOrigin.toString())
    assertEquals("http://127.0.0.1:5858", endpoints.trustedOrigin)
    assertEquals("http://127.0.0.1:5858/health", endpoints.healthUrl.toString())
    assertEquals("http://127.0.0.1:5858/api/version", endpoints.versionUrl.toString())
    assertEquals("http://127.0.0.1:5858/api/v1/status/summary", endpoints.statusSummaryUrl.toString())
    assertEquals("http://127.0.0.1:5858/embed/android/today", endpoints.todayEmbedUrl.toString())
}

@Test
fun preservesHttpsAndPortAndNormalizesOneTrailingSlash() {
    val endpoints = PimServerEndpoints.from("https://pim.example:8443/api/v1")
    assertEquals("https://pim.example:8443/api/v1/", endpoints.apiBaseUrl.toString())
    assertEquals("https://pim.example:8443/", endpoints.webOrigin.toString())
    assertEquals("https://pim.example:8443", endpoints.trustedOrigin)
    assertEquals(
        "https://pim.example",
        PimServerEndpoints.from("https://pim.example/api/v1").trustedOrigin
    )
}

@Test
fun rejectsWrongPathQueryAndFragment() {
    assertFailsWith<IllegalArgumentException> { PimServerEndpoints.from("https://pim.example/v1") }
    assertFailsWith<IllegalArgumentException> { PimServerEndpoints.from("https://pim.example/api/v1?tenant=x") }
    assertFailsWith<IllegalArgumentException> { PimServerEndpoints.from("https://pim.example/api/v1#fragment") }
}
```

These tests cover slash normalization, HTTPS/port preservation, wrong path, query, and fragment rejection.

- [ ] **Step 2: Implement the single endpoint resolver with `HttpUrl`**

```kotlin
data class PimServerEndpoints(
    val apiBaseUrl: HttpUrl,
    val webOrigin: HttpUrl,
    val trustedOrigin: String,
    val healthUrl: HttpUrl,
    val versionUrl: HttpUrl,
    val statusSummaryUrl: HttpUrl,
    val todayEmbedUrl: HttpUrl,
    val tracksEmbedUrl: HttpUrl
) {
    companion object {
        fun from(configuredApiUrl: String): PimServerEndpoints {
            val api = configuredApiUrl.toHttpUrl()
            require(api.encodedPath.trimEnd('/') == "/api/v1") { "API path must end with /api/v1" }
            require(api.query == null && api.fragment == null) { "API URL must not contain query or fragment" }
            val apiBase = api.newBuilder()
                .encodedPath("/api/v1/")
                .query(null)
                .fragment(null)
                .build()
            val origin = api.newBuilder().encodedPath("/").query(null).fragment(null).build()
            val defaultPort = if (origin.scheme == "https") 443 else 80
            val originHost = if (':' in origin.host) "[${origin.host}]" else origin.host
            val trustedOrigin = buildString {
                append(origin.scheme).append("://").append(originHost)
                if (origin.port != defaultPort) append(':').append(origin.port)
            }
            return PimServerEndpoints(
                apiBaseUrl = apiBase,
                webOrigin = origin,
                trustedOrigin = trustedOrigin,
                healthUrl = origin.resolve("/health")!!,
                versionUrl = origin.resolve("/api/version")!!,
                statusSummaryUrl = origin.resolve("/api/v1/status/summary")!!,
                todayEmbedUrl = origin.resolve("/embed/android/today")!!,
                tracksEmbedUrl = origin.resolve("/embed/android/tracks")!!
            )
        }
    }
}
```

Use this resolver in all new code; do not extend the inconsistent `location/PimServerUrls.kt` helper.

- [ ] **Step 3: Write failing 401 refresh tests**

Use `MockWebServer` to return 401, a successful refresh payload, then 200. Assert exactly one refresh and one retried request. Add a second test where refresh is rejected and assert tokens are cleared once without a loop.

```kotlin
@Test
fun server401RefreshesOnceAndRetriesWithRotatedAccessToken() {
    server.enqueue(MockResponse().setResponseCode(401))
    server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
    val store = FakeAuthSessionStore("token-a", "refresh-a", expiresAtUtcMillis = Long.MAX_VALUE)
    val refresh = RecordingRefresh(store, succeeds = true)
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store, AuthRefreshCoordinator(store, refresh)))
        .build()

    client.newCall(Request.Builder().url(server.url("/api/v1/status/summary")).build()).execute().use {
        assertEquals(200, it.code)
    }

    assertEquals(1, refresh.calls)
    assertEquals("Bearer token-a", server.takeRequest().getHeader("Authorization"))
    assertEquals("Bearer token-b", server.takeRequest().getHeader("Authorization"))
}

@Test
fun rejectedRefreshClearsOnceAndDoesNotLoop() {
    server.enqueue(MockResponse().setResponseCode(401))
    val store = FakeAuthSessionStore("token-a", "refresh-a", expiresAtUtcMillis = Long.MAX_VALUE)
    val refresh = RecordingRefresh(store, succeeds = false)
    val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store, AuthRefreshCoordinator(store, refresh)))
        .build()

    client.newCall(Request.Builder().url(server.url("/api/v1/status/summary")).build()).execute().close()

    assertEquals(1, refresh.calls)
    assertEquals(1, store.clearCalls)
    assertEquals(1, server.requestCount)
}

private class RecordingRefresh(
    private val store: FakeAuthSessionStore,
    private val succeeds: Boolean
) : AuthRefreshOperation {
    var calls = 0
    override suspend fun refresh(refreshToken: String): Boolean {
        calls++
        if (succeeds) store.save("token-b", "refresh-b", Long.MAX_VALUE)
        return succeeds
    }
}

private class FakeAuthSessionStore(
    private var access: String?,
    private var refresh: String?,
    private var expiresAtUtcMillis: Long?
) : AuthSessionStore {
    var clearCalls = 0
    override fun accessToken() = access
    override fun refreshToken() = refresh
    override fun expiresAtUtcMillis() = expiresAtUtcMillis
    override fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long) {
        access = accessToken
        refresh = refreshToken
        this.expiresAtUtcMillis = expiresAtUtcMillis
    }
    override fun clear() {
        clearCalls++
        access = null
        refresh = null
        expiresAtUtcMillis = null
    }
}
```

Start/shutdown `MockWebServer` in `@Before`/`@After` as in the probe fixture.

- [ ] **Step 4: Make auth attachment and refresh explicit**

Introduce an `AuthMode` request tag:

```kotlin
enum class AuthMode { Required, Anonymous }

interface AuthSessionStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun expiresAtUtcMillis(): Long?
    fun save(accessToken: String, refreshToken: String, expiresAtUtcMillis: Long)
    fun clear()
}

fun interface AuthRefreshOperation {
    suspend fun refresh(refreshToken: String): Boolean
}
```

`TokenManager` implements `AuthSessionStore`. `AuthRefreshCoordinator` owns one `Mutex`, double-checks the token/expiry after acquiring it, and is the only caller of `AuthRefreshOperation`. `AuthInterceptor` must:

1. omit Authorization for `AuthMode.Anonymous`;
2. refresh pre-emptively when expiry is reached;
3. on a server 401, close the response, refresh once, and retry once;
4. clear tokens and return the second 401 when refresh is missing/rejected;
5. serialize concurrent refresh attempts so one rotated refresh token is used.

- [ ] **Step 5: Write failing probe-stage tests**

```kotlin
@Test
fun probeReportsAllSuccessfulStagesAndCapabilities() = runTest {
    enqueueJson(200, """{"status":"healthy"}""")
    enqueueJson(200, """{"version":"1.2.3","capabilities":["mobileItemResultsV1"]}""")
    enqueueJson(200, """{"code":0,"message":"OK","data":{"status":"Healthy"}}""")
    enqueueHtml(200, "<html><div id=\"root\"></div></html>")
    enqueueHtml(200, "<html><div id=\"root\"></div></html>")

    val result = service.probe(serverUrl)

    assertEquals(ConnectionProbeOutcome.Reachable, result.outcome)
    assertTrue(result.capabilities.mobileItemResultsV1)
    assertFalse(result.capabilities.androidEmbedV1)
    assertEquals(ConnectionProbeStage.EmbedBootstrap, result.lastCompletedStage)
}

@Test
fun transportFailuresHaveStableKinds() = runTest {
    val cases = listOf(
        UnknownHostException("dns") to ConnectionFailureKind.Dns,
        ConnectException("connect") to ConnectionFailureKind.Connect,
        SocketTimeoutException("timeout") to ConnectionFailureKind.Timeout,
        SSLHandshakeException("tls") to ConnectionFailureKind.Tls
    )
    for ((failure, expected) in cases) {
        val throwingClient = OkHttpClient.Builder()
            .addInterceptor { throw failure }
            .build()
        val result = serviceFor(throwingClient).probe("https://pim.invalid/api/v1/")
        assertEquals(expected, result.failureKind)
    }
}

@Test
fun wrongPathAndMissingCapabilitiesHaveDifferentOutcomes() = runTest {
    server.enqueue(MockResponse().setResponseCode(404))
    assertEquals(ConnectionFailureKind.WrongPath, service.probe(serverUrl).failureKind)

    enqueueJson(200, """{"status":"healthy"}""")
    enqueueJson(200, """{"version":"1.2.3","capabilities":[]}""")
    val incompatible = service.probe(serverUrl)
    assertEquals(ConnectionProbeOutcome.Blocked, incompatible.outcome)
    assertEquals(ConnectionFailureKind.IncompatibleVersion, incompatible.failureKind)

    enqueueJson(200, """{"status":"healthy"}""")
    enqueueJson(200, """{"version":"1.2.3","capabilities":["mobileItemResultsV1"]}""")
    enqueueJson(200, """{"code":0,"data":{"status":"Healthy"}}""")
    enqueueHtml(200, "<html><div id=\"root\"></div></html>")
    enqueueHtml(404, "missing embed")
    assertEquals(ConnectionProbeOutcome.Partial, service.probe(serverUrl).outcome)
}

@Test
fun probeEvidenceExpiresAtExactlyFiveMinutes() {
    val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("probe-test", Context.MODE_PRIVATE)
    preferences.edit().clear().commit()
    val store = ConnectionProbeStore(preferences, Json { ignoreUnknownKeys = true })
    store.save(
        ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            lastCompletedStage = ConnectionProbeStage.EmbedBootstrap,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(true, true)
        )
    )
    assertTrue(store.isFresh(300_999L))
    assertFalse(store.isFresh(301_000L))
}

private fun serviceFor(client: OkHttpClient) = ConnectionProbeService(
    anonymousClient = client,
    authenticatedClient = client,
    tokenSource = FakeProbeTokenSource(null),
    nowMillis = { 1_000L }
)
```

Define the fixture in the same test class so every helper above is concrete:

```kotlin
private val server = MockWebServer()
private val tokenSource = FakeProbeTokenSource(accessToken = "probe-access")
private val service = ConnectionProbeService(
    anonymousClient = OkHttpClient(),
    authenticatedClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer ${tokenSource.accessToken}")
                    .build()
            )
        }
        .build(),
    tokenSource = tokenSource,
    nowMillis = { 1_000L }
)
private val serverUrl: String
    get() = server.url("/api/v1/").toString()

@Before fun setUp() = server.start()
@After fun tearDown() = server.shutdown()

private fun enqueueJson(code: Int, body: String) {
    server.enqueue(
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    )
}

private fun enqueueHtml(code: Int, body: String) {
    server.enqueue(
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "text/html; charset=utf-8")
            .setBody(body)
    )
}

private data class FakeProbeTokenSource(var accessToken: String?) : ProbeTokenSource {
    override fun currentAccessToken(): String? = accessToken
}
```

Define `ProbeTokenSource` in production as the narrow adapter over `TokenManager`; `ConnectionProbeService` uses the constructor shown by the fixture. Start/shutdown `MockWebServer` in `@Before`/`@After`.

The tests above cover DNS/connect/timeout/TLS, wrong path, missing `mobileItemResultsV1`, missing `androidEmbedV1`, and exact evidence expiry. `AuthInterceptorTest` covers 401 refresh.

- [ ] **Step 6: Implement typed probe models and the staged service**

```kotlin
@Serializable enum class ConnectionProbeStage { Url, Health, Version, AuthenticatedStatus, WebRoot, EmbedBootstrap }
@Serializable enum class ConnectionFailureKind { InvalidUrl, Dns, Connect, Timeout, Tls, Http, Unauthorized, WrongPath, IncompatibleVersion }
@Serializable enum class ConnectionProbeOutcome { Reachable, Partial, Blocked }

@Serializable
data class ServerCapabilities(
    val mobileItemResultsV1: Boolean,
    val androidEmbedV1: Boolean
)

@Serializable
data class ConnectionProbeResult(
    val outcome: ConnectionProbeOutcome,
    val checkedAtUtcMillis: Long,
    val lastCompletedStage: ConnectionProbeStage?,
    val latencyMillisByStage: Map<ConnectionProbeStage, Long>,
    val capabilities: ServerCapabilities,
    val failureKind: ConnectionFailureKind? = null,
    val httpStatus: Int? = null,
    val safeMessage: String? = null
)

fun interface ProbeTokenSource {
    fun currentAccessToken(): String?
}

class ConnectionProbeService(
    private val anonymousClient: OkHttpClient,
    private val authenticatedClient: OkHttpClient,
    private val tokenSource: ProbeTokenSource,
    private val nowMillis: () -> Long
)
```

`ProbeTokenSource` only decides whether the authenticated stage applies. Refresh remains solely inside the authenticated client's `AuthInterceptor`, preventing two refresh owners. Probe `/health`, `/api/version`, authenticated status when a token exists, Web root, then Today embed bootstrap. Missing `mobileItemResultsV1` returns `Blocked`; missing `androidEmbedV1` returns `Partial` until Phase 2.

- [ ] **Step 7: Persist probe evidence with a five-minute freshness rule**

`ConnectionProbeStore(preferences: SharedPreferences, json: Json)` persists one JSON result, exposes `StateFlow<ConnectionProbeResult?>`, `save(result)`, and `isFresh(nowMillis)` where freshness is `now - checkedAt < 5 minutes`. Status/Settings entry triggers a probe; a visible screen re-probes after expiry.

- [ ] **Step 8: Run probe and auth tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :core:testDebugUnitTest --tests "com.pim.core.settings.PimServerEndpointsTest" --no-daemon
.\gradlew.bat :core:testDebugUnitTest --tests "com.pim.core.network.AuthInterceptorTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.status.ConnectionProbeServiceTest" --no-daemon
```

Expected: PASS with exactly one refresh attempt and explicit TLS/wrong-path/capability classifications.

- [ ] **Step 9: Commit the endpoint, auth, and probe foundation**

```powershell
git add src/client-android/core src/client-android/app/src/main/java/com/pim/app/status src/client-android/app/src/test/java/com/pim/app/status
git commit -m "feat: add staged android connection probe"
```

## Task 4: Migrate Room 3 To 4 And Separate Business Data From Diagnostics

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncEntities.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncRunDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/SyncDeadLetterDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheEntity.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheDao.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/data/DiagnosticDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/data/PimDatabaseMigrationTest.kt`
- Create: `src/client-android/app/schemas/com.pim.app.data.AppDatabase/4.json`

- [ ] **Step 1: Write the failing 3→4 migration test with the reported 530-log fixture**

```kotlin
@RunWith(AndroidJUnit4::class)
class PimDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate3To4PreservesBusinessRowsAndMakesLogsLocalOnly() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO mobile_usage_events(package_name,event_type,event_name,event_time_utc,source,source_window_start_utc,source_window_end_utc,collected_at_utc,raw_json,sync_status,created_at_utc,updated_at_utc) VALUES('pkg',1,'move',1,'usage',1,2,2,'{}','pending',2,2)")
            execSQL("INSERT INTO mobile_location_points(latitude,longitude,recorded_at_utc,source,collected_at_utc,raw_json,policy_mode,schedule_low_frequency,quality_flags,sync_status,created_at_utc,updated_at_utc) VALUES(31.2,121.4,1,'auto',2,'{}','PowerSavingNormal',0,'[]','pending',2,2)")
            execSQL("INSERT INTO mobile_location_points(latitude,longitude,recorded_at_utc,source,collected_at_utc,raw_json,policy_mode,schedule_low_frequency,quality_flags,sync_status,last_error,created_at_utc,updated_at_utc) VALUES(31.3,121.5,3,'auto',3,'{}','PowerSavingNormal',0,'[]','failed','server-validation',3,3)")
            repeat(530) { index ->
                execSQL("INSERT INTO mobile_logs(level,message,occurred_at_utc,source,collected_at_utc,raw_json,sync_status,created_at_utc,updated_at_utc) VALUES('info','log-$index',$index,'android',$index,'{}','pending',$index,$index)")
            }
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, PimDatabaseMigrations.MIGRATION_3_4).use { db ->
            assertEquals(1L, db.longQuery("SELECT COUNT(*) FROM mobile_usage_events WHERE sync_status='pending'"))
            assertEquals(1L, db.longQuery("SELECT COUNT(*) FROM mobile_location_points WHERE sync_status='pending'"))
            assertEquals(1L, db.longQuery("SELECT COUNT(*) FROM mobile_location_points WHERE sync_status='failed' AND last_error='server-validation'"))
            assertEquals(530L, db.longQuery("SELECT COUNT(*) FROM mobile_logs WHERE sync_status='local-only'"))
            assertEquals(0L, db.longQuery("SELECT COUNT(*) FROM mobile_logs WHERE sync_status!='local-only'"))
            assertEquals(0L, db.longQuery("SELECT COUNT(*) FROM sync_runs"))
            assertEquals(0L, db.longQuery("SELECT COUNT(*) FROM schedule_window_cache"))
        }
    }

    private fun SupportSQLiteDatabase.longQuery(sql: String): Long = query(sql).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }

    private companion object { const val TEST_DB = "pim-migration-test" }
}
```

The new operational tables start empty. Migration success is proven by Room schema validation plus the preserved/mutated row assertions; do not invent a `SyncTrigger` solely to write migration bookkeeping.

In the same instrumentation class, write and verify auth-independent preferences around the migration:

```kotlin
val preferences = InstrumentationRegistry.getInstrumentation().targetContext
    .getSharedPreferences("pim_tracking", Context.MODE_PRIVATE)
preferences.edit()
    .putString("tracking.profile", "balanced")
    .putLong("tracking.normal_interval_millis", 120_000L)
    .putBoolean("tracking.continuous_collection_enabled", true)
    .commit()

// Run the database migration, then:
assertEquals("balanced", preferences.getString("tracking.profile", null))
assertEquals(120_000L, preferences.getLong("tracking.normal_interval_millis", -1L))
assertTrue(preferences.getBoolean("tracking.continuous_collection_enabled", false))
```

Keep the production provider filename `pim_tracking` unchanged. Database migration must not rewrite these settings.

- [ ] **Step 2: Compile the migration test and verify failure**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon
```

Expected: FAIL because schema 4, `MIGRATION_3_4`, and the new entities do not exist.

- [ ] **Step 3: Add the schema 4 entities**

Use these essential columns; all timestamps are UTC epoch milliseconds:

```kotlin
@Entity(
    tableName = "sync_runs",
    indices = [
        Index(value = ["requested_at_utc"]),
        Index(value = ["phase"]),
        Index(value = ["outcome"]),
        Index(value = ["lease_expires_at_utc"])
    ]
)
data class SyncRunEntity(
    @PrimaryKey @ColumnInfo(name = "run_id") val runId: String,
    @ColumnInfo(name = "work_manager_id") val workManagerId: String?,
    @ColumnInfo(name = "trigger_source") val triggerSource: String,
    @ColumnInfo(name = "allow_metered_once") val allowMeteredOnce: Boolean,
    @ColumnInfo(name = "requested_at_utc") val requestedAtUtc: Long,
    @ColumnInfo(name = "started_at_utc") val startedAtUtc: Long?,
    @ColumnInfo(name = "finished_at_utc") val finishedAtUtc: Long?,
    @ColumnInfo(name = "phase") val phase: String,
    @ColumnInfo(name = "progress_key") val progressKey: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "window_index") val windowIndex: Int,
    @ColumnInfo(name = "window_total") val windowTotal: Int?,
    @ColumnInfo(name = "queue_start_json") val queueStartJson: String,
    @ColumnInfo(name = "queue_finish_json") val queueFinishJson: String?,
    @ColumnInfo(name = "counts_json") val countsJson: String,
    @ColumnInfo(name = "last_http_status") val lastHttpStatus: Int?,
    @ColumnInfo(name = "error_code") val errorCode: String?,
    @ColumnInfo(name = "safe_message") val safeMessage: String?,
    @ColumnInfo(name = "cause_chain") val causeChain: String?,
    @ColumnInfo(name = "next_attempt_at_utc") val nextAttemptAtUtc: Long?,
    @ColumnInfo(name = "retry_count") val retryCount: Int,
    @ColumnInfo(name = "outcome") val outcome: String?,
    @ColumnInfo(name = "lease_owner") val leaseOwner: String?,
    @ColumnInfo(name = "lease_acquired_at_utc") val leaseAcquiredAtUtc: Long?,
    @ColumnInfo(name = "lease_expires_at_utc") val leaseExpiresAtUtc: Long?
)

@Entity(
    tableName = "sync_dead_letters",
    indices = [Index(value = ["rejected_at_utc"]), Index(value = ["entity_type"])]
)
data class SyncDeadLetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_key") val sourceKey: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "request_json") val requestJson: String,
    @ColumnInfo(name = "response_json") val responseJson: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "rejected_at_utc") val rejectedAtUtc: Long
)

@Entity(
    tableName = "schedule_window_cache",
    indices = [
        Index(value = ["record_kind"]),
        Index(value = ["event_id"]),
        Index(value = ["fetched_at_utc"]),
        Index(value = ["starts_at_utc"]),
        Index(value = ["ends_at_utc"])
    ]
)
data class ScheduleWindowCacheEntity(
    @PrimaryKey @ColumnInfo(name = "cache_key") val cacheKey: String,
    @ColumnInfo(name = "record_kind") val recordKind: String,
    @ColumnInfo(name = "event_id") val eventId: String?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "location_text") val locationText: String?,
    @ColumnInfo(name = "starts_at_utc") val startsAtUtc: Long?,
    @ColumnInfo(name = "ends_at_utc") val endsAtUtc: Long?,
    @ColumnInfo(name = "fetched_at_utc") val fetchedAtUtc: Long
) {
    companion object {
        const val METADATA_KEY = "__metadata__"
        const val KIND_METADATA = "metadata"
        const val KIND_EVENT = "event"
    }
}
```

Every successful schedule fetch writes exactly one metadata row with `cacheKey=METADATA_KEY`, even when the server returns zero events. Event rows use `cacheKey="event:$eventId"`; Phase 3 filters `recordKind=KIND_EVENT`. This keeps a true empty result distinguishable from “never fetched” without adding a schema 5 table.

Change `MobileLogEntity.syncStatus` default to `MobileSyncStatus.LOCAL_ONLY`. Add both `LOCAL_ONLY` and `DEAD_LETTER` to the existing `MobileSyncStatus` object exactly as defined in Shared Types.

Extend existing `MobileLocationPolicyTransitionEntity` with nullable `detailsJson`, `anchorLatitude`, `anchorLongitude`, and `distanceMeters` fields so Phase 3 can record factual schedule transitions without a schema 5 bump.

- [ ] **Step 4: Split DAOs by ownership**

`MobileDataDao` retains only business queue inserts/reads/acknowledgement and device registration. `DiagnosticDao` owns logs, dropped fixes, policy transitions, range export, retention, and clear. `SyncRunDao`, `SyncDeadLetterDao`, and `ScheduleCacheDao` own their tables.

The business queue summary query must use exactly four sources:

```kotlin
data class BusinessQueueCountRow(
    val pendingLocations: Int,
    val pendingUsageEvents: Int,
    val pendingUsageSummaries: Int,
    val pendingAppMetadata: Int,
    val oldestPendingAtUtcMillis: Long?
)

@Query(
    """
    SELECT
      (SELECT COUNT(*) FROM mobile_location_points WHERE sync_status IN ('pending','failed')) AS pendingLocations,
      (SELECT COUNT(*) FROM mobile_usage_events WHERE sync_status IN ('pending','failed')) AS pendingUsageEvents,
      (SELECT COUNT(*) FROM mobile_usage_summaries WHERE sync_status IN ('pending','failed')) AS pendingUsageSummaries,
      (SELECT COUNT(*) FROM mobile_app_metadata WHERE sync_status IN ('pending','failed')) AS pendingAppMetadata,
      MIN(oldest) AS oldestPendingAtUtcMillis
    FROM (
      SELECT MIN(created_at_utc) oldest FROM mobile_location_points WHERE sync_status IN ('pending','failed')
      UNION ALL SELECT MIN(created_at_utc) FROM mobile_usage_events WHERE sync_status IN ('pending','failed')
      UNION ALL SELECT MIN(created_at_utc) FROM mobile_usage_summaries WHERE sync_status IN ('pending','failed')
      UNION ALL SELECT MIN(created_at_utc) FROM mobile_app_metadata WHERE sync_status IN ('pending','failed')
    )
    """
)
fun observeBusinessQueueCounts(): Flow<BusinessQueueCountRow>
```

No log, sync batch, or device profile field may appear in this projection.

- [ ] **Step 5: Implement the explicit migration**

`MIGRATION_3_4` must:

1. create the three tables and indexes using the entity column names above;
   `sync_runs.allow_metered_once` is `INTEGER NOT NULL DEFAULT 0`; schedule metadata/event nullable fields and `record_kind` match the entity exactly;
2. `UPDATE mobile_logs SET sync_status='local-only', last_error=NULL`;
3. add `details_json`, `anchor_latitude`, `anchor_longitude`, and `distance_meters` nullable columns to `mobile_location_policy_transitions`;
4. leave `sync_runs`, `sync_dead_letters`, and `schedule_window_cache` empty;
5. leave business rows, failed/rejected facts, settings, and auth untouched;
6. register `MIGRATION_3_4` in `PimDatabaseMigrations.ALL`.

- [ ] **Step 6: Bump the database and generate schema 4**

Add all entities and abstract DAO accessors to `AppDatabase`, set `version = 4`, then run:

```powershell
.\gradlew.bat :app:kaptDebugKotlin --no-daemon
Get-Content -Raw app\schemas\com.pim.app.data.AppDatabase\4.json | ConvertFrom-Json | Out-Null
```

Expected: PASS and schema 4 contains all three new tables.

- [ ] **Step 7: Run the real migration on Pixel_9**

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.data.PimDatabaseMigrationTest --no-daemon
```

Expected: PASS; 530 logs are local-only and the two pending business rows remain pending.

- [ ] **Step 8: Run all Android JVM tests**

```powershell
.\gradlew.bat testDebugUnitTest --no-daemon
```

Expected: PASS after removing or rewriting source-contract tests that assert old worker names or pending log semantics. Keep encoding and launcher guard tests.

- [ ] **Step 9: Commit the migration**

```powershell
git add src/client-android/app/build.gradle.kts src/client-android/app/schemas src/client-android/app/src/main/java/com/pim/app/data src/client-android/app/src/androidTest/java/com/pim/app/data src/client-android/app/src/test
git commit -m "feat: migrate android operational data to room v4"
```

## Task 5: Persist Typed Sync Runs And Cross-Process Execution Leases

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncRunStore.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncExecutionGate.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/SyncRunDao.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncRunStoreTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/mobile/sync/SyncExecutionGateTest.kt`

- [ ] **Step 1: Write failing domain-transition tests**

```kotlin
@Test
fun runningRunCanOnlyFinishOnce() = runTest {
    val run = store.create(SyncTrigger.Manual, queue(total = 4), requestedAt = 1_000L)

    store.start(run.id, owner = "worker-1", nowMillis = 1_100L)
    store.advance(run.id, SyncPhase.UploadingLocations, "sync.uploading-locations", nowMillis = 1_200L)
    store.finish(run.id, SyncTerminalOutcome.Succeeded, queue(total = 0), nowMillis = 1_300L)

    assertFailsWith<IllegalStateException> {
        store.finish(run.id, SyncTerminalOutcome.Failed, queue(total = 0), nowMillis = 1_400L)
    }
}

private lateinit var database: AppDatabase
private lateinit var store: SyncRunStore

@Before
fun setUp() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()
    store = SyncRunStore(database.syncRunDao())
}

@After
fun tearDown() = database.close()

private fun queue(total: Int) = BusinessQueueSnapshot(
    pendingLocations = total,
    pendingUsageEvents = 0,
    pendingUsageSummaries = 0,
    pendingAppMetadata = 0,
    oldestPendingAtUtcMillis = if (total == 0) null else 900L,
    approximateBytes = null
)

@Test
fun waitingRunCanScheduleRetryWithoutTerminalOutcome() = runTest {
    val run = store.create(SyncTrigger.Retry, queue(2), requestedAt = 1_000L)
    store.start(run.id, "worker-1", 1_100L)
    store.advance(run.id, SyncPhase.WaitingForNetwork, "sync.waiting-network", 1_200L)
    store.scheduleRetry(run.id, SyncFailure("timeout", "连接超时", null, null, true), 61_200L, 1_300L)
    val persisted = store.get(run.id)
    assertEquals(SyncPhase.RetryScheduled, persisted.phase)
    assertEquals(61_200L, persisted.nextAttemptAtUtcMillis)
    assertNull(persisted.outcome)
}

@Test
fun partialAcknowledgementFinishesWithRejectsAndCounts() = runTest {
    val run = store.create(SyncTrigger.Manual, queue(3), 1_000L)
    store.start(run.id, "worker-1", 1_100L)
    store.updateCounts(run.id, "usage", SyncCategoryCounts(3, 1, 0, 1, 1, 1), 1_200L)
    store.finish(run.id, SyncTerminalOutcome.SucceededWithRejects, queue(1), 1_300L)
    assertEquals(SyncTerminalOutcome.SucceededWithRejects, store.get(run.id).outcome)
    assertEquals(1, store.get(run.id).countsByCategory.getValue("usage").serverConfirmed)
}

@Test
fun blockedRunPersistsExactFailure() = runTest {
    val run = store.create(SyncTrigger.Manual, queue(1), 1_000L)
    store.start(run.id, "worker-1", 1_100L)
    store.finish(
        run.id,
        SyncTerminalOutcome.Blocked,
        queue(1),
        nowMillis = 1_200L,
        failure = SyncFailure("login-required", "需要重新登录", null, 401, false)
    )
    assertEquals(SyncTerminalOutcome.Blocked, store.get(run.id).outcome)
    assertEquals("login-required", store.get(run.id).failure?.code)
}
```

These tests cover `Queued -> WaitingForNetwork`, retry estimate, partial success, and blocked result; the lease test below covers interrupted stale runs.

- [ ] **Step 2: Implement typed models and legal transitions**

In addition to the shared enums, define:

```kotlin
data class SyncRunId(val value: String)

@Serializable
data class SyncFailure(
    val code: String,
    val safeMessage: String,
    val causeChain: String?,
    val httpStatus: Int?,
    val retryable: Boolean
)

data class SyncRun(
    val id: SyncRunId,
    val workManagerId: String?,
    val trigger: SyncTrigger,
    val allowMeteredOnce: Boolean,
    val requestedAtUtcMillis: Long,
    val startedAtUtcMillis: Long?,
    val finishedAtUtcMillis: Long?,
    val phase: SyncPhase,
    val progressKey: String,
    val category: String?,
    val windowIndex: Int,
    val windowTotal: Int?,
    val queueAtStart: BusinessQueueSnapshot,
    val queueAtFinish: BusinessQueueSnapshot?,
    val countsByCategory: Map<String, SyncCategoryCounts>,
    val failure: SyncFailure?,
    val nextAttemptAtUtcMillis: Long?,
    val retryCount: Int,
    val outcome: SyncTerminalOutcome?
)
```

Implement `SyncRunStore(private val dao: SyncRunDao)` with `create`, `joinActive`, `attachWorkManagerId`, `start`, `advance`, `updateCounts`, `scheduleRetry`, `finish(..., failure: SyncFailure? = null)`, `get`, `latestActive`, and `recentHistory`. Use a private `Json { ignoreUnknownKeys = true; encodeDefaults = true }` instance for all Room JSON fields so the fixture above exercises real serialization in an in-memory Room database.

- [ ] **Step 3: Write failing persistent lease tests**

```kotlin
@Test
fun expiredLeaseInterruptsOldRunAndAllowsNewOwner() = runTest {
    val first = store.create(SyncTrigger.Periodic, queue(total = 2), requestedAt = 1_000L)
    assertTrue(gate.acquire(first.id, "worker-a", nowMillis = 1_000L, leaseMillis = 60_000L))

    val second = store.create(SyncTrigger.Manual, queue(total = 2), requestedAt = 70_000L)
    assertTrue(gate.acquire(second.id, "worker-b", nowMillis = 70_000L, leaseMillis = 60_000L))

    assertEquals(SyncTerminalOutcome.Interrupted, store.get(first.id).outcome)
    assertEquals("worker-b", dao.activeLease(nowMillis = 70_000L)?.leaseOwner)
}
```

In `SyncExecutionGateTest`, initialize the referenced fields against a real in-memory instrumentation database:

```kotlin
private lateinit var database: AppDatabase
private lateinit var dao: SyncRunDao
private lateinit var store: SyncRunStore
private lateinit var gate: SyncExecutionGate

@Before
fun setUp() {
    database = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        AppDatabase::class.java
    ).allowMainThreadQueries().build()
    dao = database.syncRunDao()
    store = SyncRunStore(dao)
    gate = SyncExecutionGate(dao)
}

@After
fun tearDown() = database.close()

private fun queue(total: Int) = BusinessQueueSnapshot(total, 0, 0, 0, 900L, null)
```

- [ ] **Step 4: Implement lease operations as Room transactions**

`SyncRunDao.acquireLease()` must atomically:

1. find an unexpired lease and return false if owned by another run;
2. mark every expired leased run `Interrupted`, clear lease columns, and set finish time;
3. set owner/acquired/expiry on the requested run;
4. never overwrite a terminal run.

Renew only when run ID and owner match. Release clears only the matching owner. Default lease duration is 10 minutes; the worker renews at every phase transition.

- [ ] **Step 5: Run unit and instrumentation tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.SyncRunStoreTest" --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.mobile.sync.SyncExecutionGateTest --no-daemon
```

Expected: PASS; only one unexpired owner exists and stale work becomes `Interrupted`.

- [ ] **Step 6: Commit the run store and gate**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/mobile/sync src/client-android/app/src/main/java/com/pim/app/data/SyncRunDao.kt src/client-android/app/src/test/java/com/pim/app/mobile/sync src/client-android/app/src/androidTest/java/com/pim/app/mobile/sync
git commit -m "feat: persist typed android sync runs"
```

## Task 6: Split The Sync Orchestrator Into Acknowledged Steps

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncOrchestrator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncFailureClassifier.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/DeviceRegistrationStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/UsageSyncStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/LocationSyncStep.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/steps/HeartbeatStep.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/SyncDeadLetterDao.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncOrchestratorTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncFailureClassifierTest.kt`

- [ ] **Step 1: Write failing orchestrator sequence tests with fakes**

```kotlin
@Test
fun usagePermissionFailureStillUploadsLocationsAndReportsHeartbeat() = runTest {
    val calls = mutableListOf<String>()
    val orchestrator = orchestrator(
        usage = fakeUsage(calls, StepResult.Skipped("usage-permission-missing")),
        location = fakeLocation(calls, StepResult.Succeeded(SyncCategoryCounts(2, 2, serverConfirmed = 2))),
        heartbeat = fakeHeartbeat(calls, StepResult.Succeeded(SyncCategoryCounts())),
        calls = calls
    )

    val result = orchestrator.execute(runId, workerId = "worker-1")

    assertEquals(listOf("device", "usage", "location", "heartbeat", "verify"), calls)
    assertEquals(SyncExecutionResult.Completed(SyncTerminalOutcome.Succeeded), result)
}
```

Use these concrete fakes in the same test file; `store`, `gate`, and `runId` use the in-memory Room setup from Task 5 and acquire `worker-1` before execution:

```kotlin
private fun fakeDevice(calls: MutableList<String>) =
    DeviceRegistrationOperation { calls += "device"; StepResult.Succeeded(SyncCategoryCounts()) }

private fun fakeUsage(calls: MutableList<String>, result: StepResult) =
    UsageSyncOperation { calls += "usage"; result }

private fun fakeLocation(calls: MutableList<String>, result: StepResult) =
    LocationSyncOperation { calls += "location"; result }

private fun fakeHeartbeat(calls: MutableList<String>, result: StepResult) =
    HeartbeatOperation { calls += "heartbeat"; result }

private fun orchestrator(
    usage: UsageSyncOperation,
    location: LocationSyncOperation,
    heartbeat: HeartbeatOperation,
    calls: MutableList<String> = mutableListOf()
) = SyncOrchestrator(
    runStore = store,
    executionGate = gate,
    deviceRegistration = fakeDevice(calls),
    usage = usage,
    location = location,
    heartbeat = heartbeat,
    verifyQueue = VerifyQueueOperation {
        calls += "verify"
        BusinessQueueSnapshot(0, 0, 0, 0, null, 0L)
    }
)
```

Always pass the same `calls` list into the helper. This prevents a fixture-local list from hiding sequence errors.

Implement the remaining sequence cases with this exact expectation table (create/acquire a fresh run for each row):

```kotlin
data class OrchestratorCase(
    val name: String,
    val usage: StepResult,
    val location: StepResult,
    val heartbeat: StepResult,
    val expected: SyncExecutionResult
)

val retryAt = 61_000L
val cases = listOf(
    OrchestratorCase(
        "partial reject",
        StepResult.Rejected(
            SyncCategoryCounts(attempted = 2, accepted = 1, rejected = 1, serverConfirmed = 1),
            listOf(DeadLetterEvidence("12", "usage-event", "{}", "{}", "invalid-time", "bad time"))
        ),
        StepResult.Succeeded(SyncCategoryCounts()),
        StepResult.Succeeded(SyncCategoryCounts()),
        SyncExecutionResult.Completed(SyncTerminalOutcome.SucceededWithRejects)
    ),
    OrchestratorCase(
        "heartbeat fails after confirmed location",
        StepResult.Succeeded(SyncCategoryCounts()),
        StepResult.Succeeded(SyncCategoryCounts(attempted = 2, accepted = 2, serverConfirmed = 2)),
        StepResult.Failed(SyncFailure("http-503", "服务暂不可用", null, 503, true)),
        SyncExecutionResult.Retry(SyncFailure("http-503", "服务暂不可用", null, 503, true), retryAt)
    ),
    OrchestratorCase(
        "tls is permanent",
        StepResult.Failed(SyncFailure("tls", "证书校验失败", null, null, false)),
        StepResult.Skipped("not-reached"),
        StepResult.Skipped("not-reached"),
        SyncExecutionResult.Completed(SyncTerminalOutcome.Blocked)
    )
)

@Test
fun cancellationInterruptsAndRethrows() = runTest {
    val calls = mutableListOf<String>()
    val sut = orchestrator(
        usage = UsageSyncOperation { calls += "usage"; throw CancellationException("test cancel") },
        location = fakeLocation(calls, StepResult.Succeeded(SyncCategoryCounts())),
        heartbeat = fakeHeartbeat(calls, StepResult.Succeeded(SyncCategoryCounts())),
        calls = calls
    )
    assertFailsWith<CancellationException> { sut.execute(runId, "worker-1") }
    assertEquals(SyncTerminalOutcome.Interrupted, store.get(runId).outcome)
}

@Test
fun failureClassifierHasExactRetryBoundary() {
    val cases = listOf(
        SyncFailureSignal(SyncFailureSource.Dns) to true,
        SyncFailureSignal(SyncFailureSource.Connect) to true,
        SyncFailureSignal(SyncFailureSource.Timeout) to true,
        SyncFailureSignal(SyncFailureSource.Http, 429) to true,
        SyncFailureSignal(SyncFailureSource.Http, 500) to true,
        SyncFailureSignal(SyncFailureSource.Http, 503) to true,
        SyncFailureSignal(SyncFailureSource.Tls) to false,
        SyncFailureSignal(SyncFailureSource.Http, 404) to false,
        SyncFailureSignal(SyncFailureSource.MissingCapability) to false,
        SyncFailureSignal(SyncFailureSource.AuthRefreshRejected, 401) to false
    )
    cases.forEach { (signal, retryable) ->
        assertEquals(retryable, SyncFailureClassifier.classify(signal).retryable)
    }
}
```

For the heartbeat row, assert persisted location `serverConfirmed==2` after `Retry`. Ambiguous aggregate acknowledgement remains in `MobileAcknowledgementPlannerTest` from Task 2.

- [ ] **Step 2: Define one result contract for every step**

```kotlin
sealed interface StepResult {
    data class Succeeded(val counts: SyncCategoryCounts) : StepResult
    data class Rejected(val counts: SyncCategoryCounts, val evidence: List<DeadLetterEvidence>) : StepResult
    data class Skipped(val code: String) : StepResult
    data class Failed(val failure: SyncFailure) : StepResult
}

sealed interface SyncExecutionResult {
    data class Completed(val outcome: SyncTerminalOutcome) : SyncExecutionResult
    data class Retry(val failure: SyncFailure, val nextAttemptAtUtcMillis: Long) : SyncExecutionResult
}

data class DeadLetterEvidence(
    val sourceKey: String,
    val entityType: String,
    val requestJson: String,
    val responseJson: String,
    val code: String,
    val message: String
)

data class SyncStepContext(val runId: SyncRunId, val workerId: String)

fun interface DeviceRegistrationOperation { suspend fun run(context: SyncStepContext): StepResult }
fun interface UsageSyncOperation { suspend fun run(context: SyncStepContext): StepResult }
fun interface LocationSyncOperation { suspend fun run(context: SyncStepContext): StepResult }
fun interface HeartbeatOperation { suspend fun run(context: SyncStepContext): StepResult }
fun interface VerifyQueueOperation { suspend fun run(): BusinessQueueSnapshot }
```

`SyncOrchestrator` uses the constructor shown by the test fixture. The concrete `DeviceRegistrationStep`, `UsageSyncStep`, `LocationSyncStep`, and `HeartbeatStep` implement their matching operation interfaces. Each step depends on only its API/collector/DAO boundary and returns structured facts, never localized UI copy.

`execute()` returns `Completed` only after persisting one of the five terminal outcomes. A retryable failure persists phase `RetryScheduled`, leaves `outcome=null`, computes `nextAttemptAtUtcMillis`, and returns `SyncExecutionResult.Retry`; it must never encode retry as `Failed`.

- [ ] **Step 3: Implement failure classification before retry decisions**

```kotlin
enum class SyncFailureSource { Dns, Connect, Timeout, Tls, Http, MissingCapability, AuthRefreshRejected }
data class SyncFailureSignal(val source: SyncFailureSource, val httpStatus: Int? = null)
```

Use exact rules:

- no network: waiting constraint, no HTTP attempt;
- DNS/connect/timeout/transient I/O, 429, 5xx: retryable;
- TLS certificate/hostname/protocol, 404 wrong path, missing `mobileItemResultsV1`: blocked until config/server change;
- missing token or one rejected refresh: blocked login;
- item 4xx result: dead letter, run can finish `SucceededWithRejects`;
- heartbeat failure never rolls back server-confirmed rows.

- [ ] **Step 4: Implement acknowledged usage updates**

`UsageSyncStep` sends Room IDs/client keys, applies `MobileAcknowledgementPlanner`, then in one Room transaction:

1. mark only `confirmedKeys` synced;
2. insert one `SyncDeadLetterEntity` for every permanent rejection and mark its source row `dead-letter`;
3. keep `retryKeys` pending with safe error code;
4. retain every row for `server-ack-ambiguous`;
5. record request/response evidence without Authorization or refresh tokens.

- [ ] **Step 5: Implement location acknowledgement without retrying permanent rows**

Location requests remain one row per API call, so a successful response confirms that row. Map item 4xx validation to dead letter, and only network/429/5xx back to pending. Replace the existing behavior that rereads every `FAILED` row forever.

- [ ] **Step 6: Implement the orchestrator phase sequence**

```kotlin
val phases = listOf(
    SyncPhase.CheckingPrerequisites,
    SyncPhase.RegisteringDevice,
    SyncPhase.QueryingGaps,
    SyncPhase.CollectingUsage,
    SyncPhase.UploadingUsage,
    SyncPhase.UploadingLocations,
    SyncPhase.ReportingHeartbeat,
    SyncPhase.Verifying
)
```

Advance and renew the lease at every phase. Persist category/window/count progress. On process cancellation, rethrow `CancellationException` after recording `Interrupted`. Re-read the business queue before terminal outcome.

- [ ] **Step 7: Run focused sync tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.SyncOrchestratorTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.SyncFailureClassifierTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.MobileAcknowledgementPlannerTest" --no-daemon
```

Expected: PASS; partial responses change only named rows and heartbeat failure preserves confirmed uploads.

- [ ] **Step 8: Commit the decomposed orchestrator**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/mobile/sync src/client-android/app/src/main/java/com/pim/app/data src/client-android/app/src/test/java/com/pim/app/mobile/sync
git commit -m "feat: orchestrate acknowledged mobile sync steps"
```

## Task 7: Replace Three Scheduling Paths With One Scheduler And Broker

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncScheduler.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncRequestBroker.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/SyncWorker.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/daemon/UploadWorker.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncWorker.kt`
- Delete: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationSyncWorker.kt`
- Delete after all callers compile: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncSchedulerTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncRequestBrokerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/SyncTestFixtures.kt`

- [ ] **Step 1: Write failing scheduler tests around a fake WorkManager gateway**

```kotlin
@Test
fun ensurePeriodicKeepsExactlyOneCanonicalRequestAndCancelsOldNames() {
    scheduler.ensurePeriodic(UploadNetworkPolicy.Unmetered)

    assertEquals(setOf("pim_upload", "pim_mobile_background_sync", "pim_location_upload"), gateway.cancelledNames)
    assertEquals("pim_mobile_sync_periodic", gateway.periodic.single().name)
    assertEquals(NetworkType.UNMETERED, gateway.periodic.single().networkType)
    assertEquals(15L, gateway.periodic.single().repeatMinutes)
}

@Test
fun networkPolicyUpdatesCanonicalPeriodicName() {
    scheduler.ensurePeriodic(UploadNetworkPolicy.AnyConnected)
    scheduler.ensurePeriodic(UploadNetworkPolicy.Unmetered)
    assertEquals(listOf("pim_mobile_sync_periodic", "pim_mobile_sync_periodic"), gateway.periodic.map { it.name })
    assertTrue(gateway.periodic.all { it.policy == ExistingPeriodicWorkPolicy.UPDATE })
    assertEquals(NetworkType.UNMETERED, gateway.periodic.last().networkType)
}

@Test
fun manualUsesKeepButConfirmedWaitingOverrideUsesReplace() {
    scheduler.enqueueImmediate("run-1", manual = true, allowMeteredOnce = false, replaceWaiting = false)
    scheduler.enqueueImmediate("run-1", manual = true, allowMeteredOnce = true, replaceWaiting = true)
    assertEquals(ExistingWorkPolicy.KEEP, gateway.immediate[0].policy)
    assertEquals(NetworkType.UNMETERED, gateway.immediate[0].networkType)
    assertTrue(gateway.immediate[0].expedited)
    assertEquals(ExistingWorkPolicy.REPLACE, gateway.immediate[1].policy)
    assertEquals(NetworkType.CONNECTED, gateway.immediate[1].networkType)
}

private val gateway = RecordingWorkManagerGateway()
private val scheduler = SyncScheduler(gateway, uploadPolicy = { UploadNetworkPolicy.Unmetered })

internal class RecordingWorkManagerGateway : WorkManagerGateway {
    val cancelledNames = mutableSetOf<String>()
    val periodic = mutableListOf<PeriodicWorkSpec>()
    val immediate = mutableListOf<ImmediateWorkSpec>()
    override fun cancelUniqueWork(name: String) { cancelledNames += name }
    override fun enqueuePeriodic(spec: PeriodicWorkSpec) { periodic += spec }
    override fun enqueueImmediate(spec: ImmediateWorkSpec) { immediate += spec }
}
```

Place `RecordingWorkManagerGateway` in `SyncTestFixtures.kt` so scheduler and broker tests use the same recorder.

The tests above cover constraint updates, expedited fallback metadata, normal `KEEP`, and confirmed waiting `REPLACE`. `SyncRequestBrokerTest` below covers active-lease joining without enqueue.

- [ ] **Step 2: Implement canonical scheduler constants and requests**

```kotlin
object SyncWorkNames {
    const val PERIODIC = "pim_mobile_sync_periodic"
    const val IMMEDIATE = "pim_mobile_sync_once"
    val obsolete = setOf("pim_upload", "pim_mobile_background_sync", "pim_location_upload")
}

data class PeriodicWorkSpec(
    val name: String,
    val networkType: NetworkType,
    val repeatMinutes: Long,
    val policy: ExistingPeriodicWorkPolicy
)

data class ImmediateWorkSpec(
    val name: String,
    val runId: String,
    val networkType: NetworkType,
    val policy: ExistingWorkPolicy,
    val expedited: Boolean
)

interface WorkManagerGateway {
    fun cancelUniqueWork(name: String)
    fun enqueuePeriodic(spec: PeriodicWorkSpec)
    fun enqueueImmediate(spec: ImmediateWorkSpec)
}
```

- periodic request: 15 minutes, chosen CONNECTED/UNMETERED constraint, exponential backoff;
- immediate request: unique `pim_mobile_sync_once`, requested network constraint, `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` for manual;
- normal duplicate requests use `ExistingWorkPolicy.KEEP` and join the persisted active/waiting run;
- a confirmed one-run metered override may use `ExistingWorkPolicy.REPLACE` only when the same run is `WaitingForAllowedNetwork` and has no active lease; it reuses that run ID with `CONNECTED` and persists `allowMeteredOnce=true` before replacement;
- use `ExistingPeriodicWorkPolicy.UPDATE` so a settings change updates constraints without a second periodic row;
- persist a versioned migration marker after old names are canceled.

- [ ] **Step 3: Write failing broker feedback and cooldown tests**

```kotlin
@Test
fun manualRequestPersistsQueuedBeforeEnqueue() = runTest {
    val result = broker.request(SyncRequest(trigger = SyncTrigger.Manual))

    assertEquals(SyncPhase.Queued, store.get(result.runId).phase)
    assertEquals(result.runId.value, gateway.immediate.single().runId)
}

@Test
fun foregroundRequestsUseFiveMinuteCooldown() = runTest {
    assertTrue(broker.requestForeground(nowMillis = 1_000L).enqueued)
    assertFalse(broker.requestForeground(nowMillis = 120_000L).enqueued)
    assertTrue(broker.requestForeground(nowMillis = 301_001L).enqueued)
}

@Test
fun activeLeaseJoinsWithoutSecondEnqueue() = runTest {
    val first = broker.request(SyncRequest(SyncTrigger.Manual))
    assertTrue(gate.acquire(first.runId, "worker-1", 1_000L, 60_000L))
    val second = broker.request(SyncRequest(SyncTrigger.Manual))
    assertEquals(first.runId, second.runId)
    assertTrue(second.joinedActiveRun)
    assertEquals(1, gateway.immediate.size)
}

@Test
fun confirmedMeteredOverrideReusesWaitingRunAndReplacesWork() = runTest {
    val scheduler = SyncScheduler(gateway, uploadPolicy = { UploadNetworkPolicy.Unmetered })
    broker = SyncRequestBroker(
        store = store,
        executionGate = gate,
        scheduler = scheduler,
        queueSnapshot = { BusinessQueueSnapshot(1, 0, 0, 0, 900L, null) },
        networkFacts = { NetworkFacts(connected = true, metered = true) },
        nowMillis = { 1_000L }
    )
    val waiting = broker.request(SyncRequest(SyncTrigger.Manual))
    assertEquals(SyncPhase.WaitingForAllowedNetwork, store.get(waiting.runId).phase)

    val override = broker.request(SyncRequest(SyncTrigger.Manual, allowMeteredOnce = true))
    assertEquals(waiting.runId, override.runId)
    assertTrue(store.get(waiting.runId).allowMeteredOnce)
    assertEquals(ExistingWorkPolicy.REPLACE, gateway.immediate.last().policy)
    assertEquals(NetworkType.CONNECTED, gateway.immediate.last().networkType)
}

private lateinit var database: AppDatabase
private lateinit var store: SyncRunStore
private lateinit var gate: SyncExecutionGate
private lateinit var gateway: RecordingWorkManagerGateway
private lateinit var broker: SyncRequestBroker

@Before
fun setUp() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), AppDatabase::class.java
    ).allowMainThreadQueries().build()
    store = SyncRunStore(database.syncRunDao())
    gate = SyncExecutionGate(database.syncRunDao())
    gateway = RecordingWorkManagerGateway()
    val scheduler = SyncScheduler(gateway, uploadPolicy = { UploadNetworkPolicy.AnyConnected })
    broker = SyncRequestBroker(
        store = store,
        executionGate = gate,
        scheduler = scheduler,
        queueSnapshot = { BusinessQueueSnapshot(1, 0, 0, 0, 900L, null) },
        networkFacts = { NetworkFacts(connected = true, metered = false) },
        nowMillis = { 1_000L }
    )
}

@After fun tearDown() = database.close()
```

- [ ] **Step 4: Implement broker coalescing and metered override**

```kotlin
data class SyncRequest(
    val trigger: SyncTrigger,
    val allowMeteredOnce: Boolean = false
)

data class SyncRequestResult(
    val runId: SyncRunId,
    val enqueued: Boolean,
    val joinedActiveRun: Boolean
)
```

Create/join a run, persist `Queued`, then enqueue. If unmetered is required and only metered access exists, persist `WaitingForAllowedNetwork`. A confirmed one-run override atomically updates that run's `allowMeteredOnce`, verifies no active lease, and asks the scheduler to replace the waiting unique request with `CONNECTED`; it never changes the saved upload preference. The flag remains on that historical run as diagnostic evidence and every new run defaults to false.

- [ ] **Step 5: Implement the single worker entry**

`SyncWorker` reads run ID/trigger from input data, creates a periodic run if none was supplied, acquires the lease, calls `SyncOrchestrator`, and maps `SyncExecutionResult`:

- `Completed(Succeeded|SucceededWithRejects|Blocked)`: `Result.success()` because the typed run holds the user outcome;
- `Retry`: the orchestrator has already persisted `RetryScheduled`; return `Result.retry()`;
- `Completed(Failed|Interrupted)`: `Result.failure()` unless interruption was WorkManager cancellation, which is rethrown;
- programmer/database invariant failure: persist `Failed`, then `Result.failure()`.

- [ ] **Step 6: Route notification/service sync through the broker**

Replace `ForegroundLocationService.runManualSync()` direct coordinator execution with `SyncRequestBroker.request(Manual)`. The service notification reads persisted run state; it does not start a separate foreground upload loop.

- [ ] **Step 7: Remove old workers and coordinator registrations**

Update `PimWorkerFactory` to create only `SyncWorker` plus unrelated `EndpointUploadWorker`. Remove all imports/calls for the three deleted worker files and `MobileSyncCoordinator`.

- [ ] **Step 8: Run scheduler, broker, worker registration, and all JVM tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.SyncSchedulerTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.mobile.sync.SyncRequestBrokerTest" --no-daemon
.\gradlew.bat testDebugUnitTest --no-daemon
```

Expected: PASS and repository search finds canonical names only outside migration constants/tests:

```powershell
rg -n "pim_upload|pim_mobile_background_sync|pim_location_upload" app/src/main app/src/test
```

Expected matches: `SyncWorkNames.obsolete` and migration assertions only.

- [ ] **Step 9: Commit the scheduler replacement**

```powershell
git add src/client-android/app/src/main src/client-android/app/src/test
git commit -m "feat: unify android sync scheduling"
```

## Task 8: Recover Scheduler And Collection Intent After Boot, Update, And Process Death

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/SystemPrerequisiteRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/SystemAction.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/StartupRecoveryRecordStore.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/StartupRecoveryCoordinator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/BootUpdateReceiver.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/startup/AppForegroundObserver.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/PimApp.kt`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Test: `src/client-android/app/src/test/java/com/pim/app/startup/StartupRecoveryCoordinatorTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/startup/AppForegroundObserverTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt`

- [ ] **Step 1: Write failing recovery tests for every durable outcome**

```kotlin
@Test
fun enabledIntentSurvivesPermissionBlockAndBecomesActionRequired() = runTest {
    val harness = RecoveryHarness(
        enabled = true,
        prerequisites = CollectionPrerequisites.ready().copy(backgroundLocation = false)
    )

    val result = harness.coordinator.reconcile(StartupTrigger.BootCompleted, visibleApp = false)

    assertEquals(StartupRecoveryOutcome.UserActionRequired, result.outcome)
    assertEquals("background-location-missing", result.reasonCode)
    assertTrue(harness.intent.enabled)
    assertEquals(0, harness.service.startCalls)
}

@Test
fun durableRecoveryCasesHaveExactOutcomes() = runTest {
    val cases = listOf(
        RecoveryCase("disabled", false, StartupTrigger.BootCompleted, CollectionPrerequisites.ready(), CollectionStartResult.Started, StartupRecoveryOutcome.NotRequested, 0),
        RecoveryCase("boot", true, StartupTrigger.BootCompleted, CollectionPrerequisites.ready(), CollectionStartResult.Started, StartupRecoveryOutcome.Restored, 1),
        RecoveryCase("update", true, StartupTrigger.AppReplaced, CollectionPrerequisites.ready(), CollectionStartResult.Started, StartupRecoveryOutcome.Restored, 1),
        RecoveryCase(
            "background start denied", true, StartupTrigger.BootCompleted, CollectionPrerequisites.ready(),
            CollectionStartResult.Blocked("foreground-start-not-allowed", SystemAction.OpenAppDetails),
            StartupRecoveryOutcome.UserActionRequired, 1
        ),
        RecoveryCase(
            "security exception", true, StartupTrigger.BootCompleted, CollectionPrerequisites.ready(),
            CollectionStartResult.Blocked("security-exception", SystemAction.OpenAppLocationSettings),
            StartupRecoveryOutcome.UserActionRequired, 1
        ),
        RecoveryCase(
            "startup timeout", true, StartupTrigger.Foreground, CollectionPrerequisites.ready(),
            CollectionStartResult.Failed("service-start-timeout", "10 秒内未确认服务运行"),
            StartupRecoveryOutcome.Failed, 1
        )
    )
    cases.forEach { case ->
        val harness = RecoveryHarness(case.enabled, case.prerequisites, case.startResult)
        val result = harness.coordinator.reconcile(case.trigger, visibleApp = case.trigger == StartupTrigger.Foreground)
        assertEquals(case.name, case.expected, result.outcome)
        assertEquals(case.name, case.expectedStartCalls, harness.service.startCalls)
        assertEquals(case.name, case.enabled, harness.intent.enabled)
        assertEquals(1, harness.scheduler.calls)
        assertEquals(1, harness.leases.calls)
    }
}

private data class RecoveryCase(
    val name: String,
    val enabled: Boolean,
    val trigger: StartupTrigger,
    val prerequisites: CollectionPrerequisites,
    val startResult: CollectionStartResult,
    val expected: StartupRecoveryOutcome,
    val expectedStartCalls: Int
)

private class RecoveryHarness(
    enabled: Boolean,
    prerequisites: CollectionPrerequisites,
    startResult: CollectionStartResult = CollectionStartResult.Started
) {
    val intent = InMemoryCollectionIntentStore(enabled)
    val service = RecordingCollectionStarter(startResult)
    val scheduler = RecordingStartupScheduler()
    val leases = RecordingLeaseReconciler()
    val records = InMemoryRecoveryRecordSink()
    val coordinator = StartupRecoveryCoordinator(
        scheduler = scheduler,
        leases = leases,
        collectionIntent = intent,
        prerequisites = CollectionPrerequisiteSource { prerequisites },
        service = service,
        records = records,
        nowMillis = { 1_000L }
    )
}

@Test
fun visibleForegroundRetriesPreviousBackgroundStartDenial() = runTest {
    val harness = RecoveryHarness(
        enabled = true,
        prerequisites = CollectionPrerequisites.ready(),
        startResult = CollectionStartResult.Blocked("foreground-start-not-allowed", SystemAction.OpenAppDetails)
    )
    assertEquals(
        StartupRecoveryOutcome.UserActionRequired,
        harness.coordinator.reconcile(StartupTrigger.BootCompleted, visibleApp = false).outcome
    )
    harness.service.result = CollectionStartResult.Started
    assertEquals(
        StartupRecoveryOutcome.Restored,
        harness.coordinator.reconcile(StartupTrigger.Foreground, visibleApp = true).outcome
    )
    assertEquals(2, harness.service.startCalls)
}
```

The matrix covers disabled intent, successful boot/update, typed `ForegroundServiceStartNotAllowedException`/`SecurityException` mapping, startup timeout, and invocation of stale-lease reconciliation. The two-step test covers visible foreground retry; real stale-lease mutation remains covered by Task 5 instrumentation and Phase 3 boot instrumentation.

- [ ] **Step 2: Define and persist one recovery record**

```kotlin
enum class StartupTrigger { ProcessCreated, BootCompleted, AppReplaced, Foreground }
enum class StartupRecoveryOutcome { NotRequested, Restored, UserActionRequired, Failed }

data class StartupRecoveryRecord(
    val trigger: StartupTrigger,
    val attemptedAtUtcMillis: Long,
    val outcome: StartupRecoveryOutcome,
    val reasonCode: String?,
    val requiredAction: SystemAction?
)

data class CollectionPrerequisites(
    val notification: Boolean,
    val preciseLocation: Boolean,
    val backgroundLocation: Boolean,
    val usageAccess: Boolean,
    val activityRecognition: Boolean,
    val batteryExempt: Boolean,
    val locationProviderEnabled: Boolean,
    val foregroundServiceAllowed: Boolean
) {
    companion object {
        fun ready() = CollectionPrerequisites(true, true, true, true, true, true, true, true)
    }
}

sealed interface SystemAction {
    data object RequestNotificationPermission : SystemAction
    data object RequestPreciseLocation : SystemAction
    data object OpenAppLocationSettings : SystemAction
    data object OpenUsageAccessSettings : SystemAction
    data object RequestActivityRecognition : SystemAction
    data object OpenBatteryOptimizationSettings : SystemAction
    data object OpenSystemLocationSettings : SystemAction
    data object OpenAppDetails : SystemAction
}
```

`StartupRecoveryRecordStore` uses `pim_startup_recovery` SharedPreferences, stores only the latest record, and never writes credentials.

- [ ] **Step 3: Make service start return structured evidence without clearing intent**

```kotlin
sealed interface CollectionStartResult {
    data object Started : CollectionStartResult
    data class Blocked(val reasonCode: String, val action: SystemAction) : CollectionStartResult
    data class Failed(val reasonCode: String, val safeMessage: String) : CollectionStartResult
}

fun interface CollectionPrerequisiteSource { suspend fun snapshot(): CollectionPrerequisites }
interface CollectionIntentStore {
    val enabled: Boolean
    fun setEnabled(enabled: Boolean)
}
fun interface StartupSchedulerReconciler { suspend fun reconcile() }
fun interface ExpiredLeaseReconciler { suspend fun interruptExpired(): Int }
fun interface CollectionServiceStarter { suspend fun start(): CollectionStartResult }
fun interface RecoveryRecordSink { fun save(record: StartupRecoveryRecord) }
```

The production adapters delegate to `TrackingSettingsStore`, `SyncScheduler`, `SyncExecutionGate`, `SystemPrerequisiteRepository`, `ForegroundLocationController`, and `StartupRecoveryRecordStore`. Use these test implementations in `StartupRecoveryCoordinatorTest`:

```kotlin
private class InMemoryCollectionIntentStore(initial: Boolean) : CollectionIntentStore {
    override var enabled: Boolean = initial
        private set
    override fun setEnabled(enabled: Boolean) { this.enabled = enabled }
}
private class RecordingCollectionStarter(var result: CollectionStartResult) : CollectionServiceStarter {
    var startCalls = 0
    override suspend fun start(): CollectionStartResult { startCalls++; return result }
}
private class RecordingStartupScheduler : StartupSchedulerReconciler {
    var calls = 0
    override suspend fun reconcile() { calls++ }
}
private class RecordingLeaseReconciler : ExpiredLeaseReconciler {
    var calls = 0
    override suspend fun interruptExpired(): Int { calls++; return 0 }
}
private class InMemoryRecoveryRecordSink : RecoveryRecordSink {
    val records = mutableListOf<StartupRecoveryRecord>()
    override fun save(record: StartupRecoveryRecord) { records += record }
}
```

`StartupRecoveryCoordinator` uses the constructor shown by `RecoveryHarness`; it calls scheduler then lease reconciliation before reading intent/prerequisites.

`ForegroundLocationController.start()` performs the platform start and the coordinator verifies `ForegroundLocationService.runtimeState` reaches running or a typed blocked state within 10 seconds. Remove every path in Settings/service that sets `continuousCollectionEnabled=false` merely because permission, API, auth, provider, or background-start conditions are currently unavailable. Only explicit user pause writes false.

- [ ] **Step 4: Implement idempotent reconciliation order**

`StartupRecoveryCoordinator.reconcile()` must:

1. call `SyncScheduler.ensurePeriodic()` and old-name migration;
2. interrupt expired leases;
3. read durable collection intent;
4. record `NotRequested` when false;
5. evaluate precise/background location, provider state, notification policy, and platform FGS permission;
6. start the service only when permitted;
7. retain enabled intent and record exact `UserActionRequired` otherwise.

Background sync remains scheduled even when collection is blocked.

- [ ] **Step 5: Add the protected boot/update receiver**

```kotlin
@AndroidEntryPoint
class BootUpdateReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: StartupRecoveryCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        val trigger = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> StartupTrigger.BootCompleted
            Intent.ACTION_MY_PACKAGE_REPLACED -> StartupTrigger.AppReplaced
            else -> return
        }
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { coordinator.reconcile(trigger, visibleApp = false) }
            finally { result.finish() }
        }
    }
}
```

Manifest additions:

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver
    android:name=".startup.BootUpdateReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

Validate replacement intent data belongs to `com.pim.app`. Do not register `LOCKED_BOOT_COMPLETED`; Room/settings are credential-protected.

- [ ] **Step 6: Reconcile process creation and foreground sessions**

`PimApp.onCreate()` runs scheduler/stale-lease reconciliation only. `AppForegroundObserver`, registered with `ProcessLifecycleOwner`, performs full visible reconciliation and `SyncRequestBroker.requestForeground()` once per foreground session with the persisted five-minute cooldown.

- [ ] **Step 7: Run recovery and manifest tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.startup.*" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.v2.AndroidV2ManifestTest" --no-daemon
```

Expected: PASS; blocked recovery retains enabled intent and the receiver has only protected actions.

- [ ] **Step 8: Commit startup recovery**

```powershell
git add src/client-android/app/src/main/AndroidManifest.xml src/client-android/app/src/main/java/com/pim/app/PimApp.kt src/client-android/app/src/main/java/com/pim/app/startup src/client-android/app/src/main/java/com/pim/app/permissions src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/test/java/com/pim/app/startup src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt
git commit -m "feat: recover android collection after lifecycle changes"
```

## Task 9: Add Presets, Bounded Advanced Settings, And Atomic Apply

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingPresetCatalog.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/SettingsApplyCoordinator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingPresetCatalogTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsValidatorTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/settings/SettingsApplyCoordinatorTest.kt`

- [ ] **Step 1: Write failing preset and bounds tests**

```kotlin
@Test
fun approvedPresetsMatchTheDesign() {
    assertEquals(TrackingIntervals(180_000, 900_000, 60_000, 100.0), catalog.powerSaving.intervals)
    assertEquals(TrackingIntervals(120_000, 600_000, 45_000, 75.0), catalog.balanced.intervals)
    assertEquals(TrackingIntervals(60_000, 300_000, 30_000, 50.0), catalog.highFrequency.intervals)
    assertTrue(catalog.all.all { it.maxAcceptedAccuracyMetersExclusive == 50f })
}

@Test
fun validatorRejectsEveryOutOfRangeFieldTogether() {
    val result = validator.validate(
        TrackingSettings.defaults().copy(
            normalIntervalMillis = 59_999,
            scheduleLowFrequencyIntervalMillis = 3_000_000,
            movementIntervalMillis = 10_000,
            scheduleRecoveryThresholdMeters = 700.0,
            maxUploadAccuracyMetersExclusive = 60f,
            altitudeWaitTimeoutMillis = 40_000,
            logRetentionDays = 3
        )
    )
    assertEquals(
        setOf("normal-interval", "schedule-interval", "motion-interval", "recovery-distance", "accuracy", "altitude-wait", "log-retention"),
        result.errors.map { it.code }.toSet()
    )
}

private val catalog = TrackingPresetCatalog()
private val validator = TrackingSettingsValidator()
```

- [ ] **Step 2: Define preset and persisted setting types**

```kotlin
enum class TrackingProfileId { PowerSaving, Balanced, HighFrequency, Custom }

data class TrackingSettings(
    val profile: TrackingProfileId,
    val restorePreset: TrackingProfileId,
    val continuousCollectionEnabled: Boolean,
    val normalIntervalMillis: Long,
    val scheduleLowFrequencyIntervalMillis: Long,
    val movementIntervalMillis: Long,
    val scheduleRecoveryThresholdMeters: Double,
    val altitudeWaitTimeoutMillis: Long,
    val maxUploadAccuracyMetersExclusive: Float,
    val logRetentionDays: Int,
    val uploadNetworkPolicy: UploadNetworkPolicy,
    val verboseLoggingUntilUtcMillis: Long?
) {
    companion object {
        fun defaults() = TrackingSettings(
            profile = TrackingProfileId.PowerSaving,
            restorePreset = TrackingProfileId.PowerSaving,
            continuousCollectionEnabled = false,
            normalIntervalMillis = 180_000L,
            scheduleLowFrequencyIntervalMillis = 900_000L,
            movementIntervalMillis = 60_000L,
            scheduleRecoveryThresholdMeters = 100.0,
            altitudeWaitTimeoutMillis = 15_000L,
            maxUploadAccuracyMetersExclusive = 50f,
            logRetentionDays = 7,
            uploadNetworkPolicy = UploadNetworkPolicy.AnyConnected,
            verboseLoggingUntilUtcMillis = null
        )
    }
}
```

Read old string profile values compatibly. `TrackingPreset.applyTo(current)` replaces the six collection values and sets both `profile` and `restorePreset` to that preset. Editing an advanced field sets `profile=Custom` and preserves `restorePreset`. Restore reapplies `restorePreset`. Verbose logging enables for exactly 24 hours.

- [ ] **Step 3: Implement all bounds as one validation result**

- normal 1–15 minutes;
- schedule 5–60 minutes;
- motion 30 seconds–5 minutes;
- recovery 25–500 meters;
- strict accuracy threshold 10–50 meters;
- altitude wait 0–30 seconds;
- log retention 1, 7, 14, or 30 days;
- restore preset cannot be `Custom`.

- [ ] **Step 4: Write failing atomic apply/rollback tests**

```kotlin
@Test
fun downstreamFailureRestoresCompletePreviousObjectAndSchedulerConstraint() = runTest {
    val previous = store.read()
    service.failReload = true
    val requested = catalog.balanced.applyTo(previous)
        .copy(uploadNetworkPolicy = UploadNetworkPolicy.Unmetered)

    val result = coordinator.apply(requested)

    assertTrue(result is SettingsApplyResult.Failed)
    assertEquals(previous, store.read())
    assertEquals(previous.uploadNetworkPolicy, scheduler.lastPolicy)
}

private val store = InMemoryTrackingSettingsPersistence(TrackingSettings.defaults())
private val service = RecordingPolicyReloader()
private val scheduler = RecordingConstraintUpdater()
private val coordinator = SettingsApplyCoordinator(validator, store, scheduler, service)

private class InMemoryTrackingSettingsPersistence(
    private var value: TrackingSettings
) : TrackingSettingsPersistence {
    override fun read() = value
    override fun writeValidated(settings: TrackingSettings): Boolean {
        value = settings
        return true
    }
}

private class RecordingPolicyReloader : CollectionPolicyReloader {
    var failReload = false
    override suspend fun reload(): Boolean = !failReload
}

private class RecordingConstraintUpdater : SyncConstraintUpdater {
    var lastPolicy: UploadNetworkPolicy? = null
    override fun update(policy: UploadNetworkPolicy): Boolean {
        lastPolicy = policy
        return true
    }
}
```

- [ ] **Step 5: Implement atomic persistence and coordinated apply**

`TrackingSettingsStore.writeValidated()` writes the complete object with one `SharedPreferences.Editor.commit()` and returns failure when disk commit fails. `SettingsApplyCoordinator` validates, persists, updates `SyncScheduler` constraints, asks a running service to reload policy, and rolls back the full old object plus old scheduler policy if either downstream operation fails.

Use these narrow ports for the coordinator; production adapters delegate to `TrackingSettingsStore`, `SyncScheduler.ensurePeriodic`, and the foreground service controller:

```kotlin
interface TrackingSettingsPersistence {
    fun read(): TrackingSettings
    fun writeValidated(settings: TrackingSettings): Boolean
}
fun interface SyncConstraintUpdater { fun update(policy: UploadNetworkPolicy): Boolean }
fun interface CollectionPolicyReloader { suspend fun reload(): Boolean }
```

Logout clears tokens and embedded Web auth but does not turn off continuous collection. Collection and transfer eligibility are independent.

- [ ] **Step 6: Run settings tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.settings.*" --no-daemon
```

Expected: PASS for presets, every bound, custom/restore, commit failure, service reload, scheduler update, and rollback.

- [ ] **Step 7: Commit settings behavior**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/settings src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/test/java/com/pim/app/settings
git commit -m "feat: add configurable android collection profiles"
```

## Task 10: Add System Prerequisites And Exact Status Actions

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/permissions/SystemPrerequisiteRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusActionExecutor.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusPermissionNavigator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/permissions/SystemPrerequisiteRepositoryTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusActionExecutorTest.kt`

- [ ] **Step 1: Write failing action routing tests**

```kotlin
@Test
fun everyIssueActionExecutesItsNamedEffect() = runTest {
    executor.execute(StatusAction.OpenApiSettings)
    executor.execute(StatusAction.OpenLogin)
    executor.execute(StatusAction.RequestNotificationPermission)
    executor.execute(StatusAction.RequestPreciseLocation)
    executor.execute(StatusAction.OpenUsageAccess)
    executor.execute(StatusAction.OpenBackgroundLocation)
    executor.execute(StatusAction.RequestActivityRecognition)
    executor.execute(StatusAction.OpenBatterySettings)
    executor.execute(StatusAction.OpenSystemLocationSettings)
    executor.execute(StatusAction.StartCollection)
    executor.execute(StatusAction.SyncNow)
    executor.execute(StatusAction.OpenTransfer)
    executor.execute(StatusAction.OpenDiagnostics)
    executor.execute(StatusAction.OpenLocationQuality)

    assertEquals(
        listOf(
            "notification-permission", "precise-location-permission", "usage-settings",
            "app-location-settings", "activity-recognition-permission",
            "battery-settings", "system-location-settings"
        ),
        systemActions.opened
    )
    assertEquals(1, collection.startCalls)
    assertEquals(1, sync.manualCalls)
    assertEquals(
        listOf("api-settings", "login", "transfer", "diagnostics", "location-quality"),
        navigation.opened
    )
}

private val systemActions = RecordingSystemActions()
private val collection = RecordingCollectionActions()
private val sync = RecordingSyncActions()
private val navigation = RecordingStatusNavigation()
private val executor = StatusActionExecutor(systemActions, collection, sync, navigation)

private class RecordingSystemActions : StatusSystemActions {
    val opened = mutableListOf<String>()
    override suspend fun open(key: String) { opened += key }
}
private class RecordingCollectionActions : StatusCollectionActions {
    var startCalls = 0
    override suspend fun start() { startCalls++ }
}
private class RecordingSyncActions : StatusSyncActions {
    var manualCalls = 0
    override suspend fun requestManual() { manualCalls++ }
}
private class RecordingStatusNavigation : StatusNavigationActions {
    val opened = mutableListOf<String>()
    override fun open(route: String) { opened += route }
}
```

- [ ] **Step 2: Define the exact action set**

```kotlin
sealed interface StatusAction {
    data object OpenApiSettings : StatusAction
    data object OpenLogin : StatusAction
    data object RequestNotificationPermission : StatusAction
    data object RequestPreciseLocation : StatusAction
    data object OpenBackgroundLocation : StatusAction
    data object OpenUsageAccess : StatusAction
    data object RequestActivityRecognition : StatusAction
    data object OpenBatterySettings : StatusAction
    data object OpenSystemLocationSettings : StatusAction
    data object StartCollection : StatusAction
    data object SyncNow : StatusAction
    data object OpenTransfer : StatusAction
    data object OpenDiagnostics : StatusAction
    data object OpenLocationQuality : StatusAction
}

fun interface StatusSystemActions { suspend fun open(key: String) }
fun interface StatusCollectionActions { suspend fun start() }
fun interface StatusSyncActions { suspend fun requestManual() }
fun interface StatusNavigationActions { fun open(route: String) }
```

- [ ] **Step 3: Expand prerequisite evidence**

`SystemPrerequisiteRepository.snapshot()` includes notification, precise/background location, usage access, activity recognition, battery optimization exemption, system location provider, and foreground service runtime. It distinguishes core collection blockers from optional quality degradations.

- [ ] **Step 4: Implement platform-specific intents and runtime requests**

- usage: `Settings.ACTION_USAGE_ACCESS_SETTINGS`;
- background/approximate-only: app details/location permission settings;
- provider disabled: `Settings.ACTION_LOCATION_SOURCE_SETTINGS`;
- battery: `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` with app-details fallback;
- notification/precise/activity: Activity Result runtime permission launcher;
- API/login: native Settings server/account section, focused on the matching control;
- service: visible `ForegroundLocationController.start()` result;
- sync: `SyncRequestBroker.request(Manual)`;
- diagnostics/location quality: real native subpage navigation.

No executor branch may merely expand the clicked row.

- [ ] **Step 5: Run prerequisite and action tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.permissions.*" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.status.StatusActionExecutorTest" --no-daemon
```

Expected: PASS and action labels can be mechanically mapped to one effect.

- [ ] **Step 6: Commit exact actions**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/permissions src/client-android/app/src/main/java/com/pim/app/status src/client-android/app/src/test/java/com/pim/app/permissions src/client-android/app/src/test/java/com/pim/app/status
git commit -m "feat: route android status actions to real effects"
```

## Task 11: Implement Local Diagnostic Retention, Clearing, ZIP Export, And Sharing

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticRetentionManager.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticExportValidator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticExporter.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/diagnostics/DiagnosticShareController.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/DiagnosticDao.kt`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Create: `src/client-android/app/src/main/res/xml/diagnostic_file_paths.xml`
- Test: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticExporterTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticExportValidatorTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/diagnostics/DiagnosticRetentionManagerTest.kt`

- [ ] **Step 1: Write failing secret-exclusion and raw-coordinate tests**

```kotlin
@get:Rule
val temporaryFolder = TemporaryFolder()

@Test
fun exportIncludesRawLocationButRejectsEveryCredentialField() = runTest {
    val source = FixtureDiagnosticDataSource(
        markerData(accessToken = "pim-secret-access-token", refreshToken = "pim-secret-refresh-token")
    )
    val exporter = DiagnosticExporter(temporaryFolder.root, source, DiagnosticExportValidator())
    val result = exporter.export(
        DiagnosticExportRequest(DiagnosticRange.Last7Days, includeRawLocationConfirmed = true)
    )
    val entries = unzipText(result)

    assertTrue(entries.getValue("location-points.jsonl").contains("31.230416"))
    assertFalse(entries.values.any { it.contains("pim-secret-access-token") })
    assertFalse(entries.values.any { it.contains("pim-secret-refresh-token") })
    assertEquals(EXPECTED_ENTRY_NAMES, entries.keys)
}

private val EXPECTED_ENTRY_NAMES = setOf(
    "manifest.json", "status.json", "settings.json", "workmanager.json",
    "sync-history.jsonl", "logs.jsonl", "location-points.jsonl",
    "dropped-location.jsonl", "policy-transitions.jsonl", "schedule-cache.json",
    "database-counts.json"
)

private fun markerData(accessToken: String, refreshToken: String) = DiagnosticExportSnapshot(
    jsonByEntry = DiagnosticEntry.entries.associateWith { entry ->
        when (entry) {
            DiagnosticEntry.Manifest -> "{\"schemaVersion\":1}"
            DiagnosticEntry.Settings -> "{\"serverUrl\":\"http://127.0.0.1:5858\",\"accessToken\":\"$accessToken\"}"
            DiagnosticEntry.Logs -> "{\"message\":\"probe\",\"refreshToken\":\"$refreshToken\"}\n"
            DiagnosticEntry.LocationPoints -> "{\"latitude\":31.230416,\"longitude\":121.473701,\"accuracyMeters\":8.0}\n"
            else -> if (entry.fileName.endsWith(".jsonl")) "" else "{}"
        }
    }
)

private fun unzipText(result: DiagnosticExportResult): Map<String, String> =
    ZipFile(result.file).use { zip ->
        zip.entries().asSequence().associate { entry ->
            entry.name to zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
```

Expected names are exactly the 11 files in the design spec.

- [ ] **Step 2: Define export range and result contracts**

```kotlin
enum class DiagnosticRange { Last24Hours, Last7Days, AllRetained }

data class DiagnosticExportRequest(
    val range: DiagnosticRange = DiagnosticRange.Last7Days,
    val includeRawLocationConfirmed: Boolean
)

data class DiagnosticExportResult(
    val file: File,
    val generatedAtUtcMillis: Long,
    val entryNames: Set<String>,
    val byteCount: Long
)

enum class DiagnosticEntry(val fileName: String) {
    Manifest("manifest.json"),
    Status("status.json"),
    Settings("settings.json"),
    WorkManager("workmanager.json"),
    SyncHistory("sync-history.jsonl"),
    Logs("logs.jsonl"),
    LocationPoints("location-points.jsonl"),
    DroppedLocation("dropped-location.jsonl"),
    PolicyTransitions("policy-transitions.jsonl"),
    ScheduleCache("schedule-cache.json"),
    DatabaseCounts("database-counts.json")
}

data class DiagnosticExportSnapshot(val jsonByEntry: Map<DiagnosticEntry, String>)

fun interface DiagnosticDataSource {
    suspend fun load(range: DiagnosticRange): DiagnosticExportSnapshot
}

private class FixtureDiagnosticDataSource(
    private val snapshot: DiagnosticExportSnapshot
) : DiagnosticDataSource {
    override suspend fun load(range: DiagnosticRange) = snapshot
}
```

The production `DiagnosticDataSource` builds each enum entry from explicit DTO/DAO projections; it never accepts arbitrary entry names. The fixture deliberately injects token-shaped fields so the sanitizer/validator test proves they are removed. Reject generation before writing when raw-location confirmation is false.

- [ ] **Step 3: Sanitize at log write and whitelist at export**

`StructuredLogRepository` must remove keys matching password, token, authorization, cookie, secret, and login body names recursively before persistence. Export builds DTOs from approved columns instead of dumping SharedPreferences/database files, then applies the same recursive JSON/JSONL redactor to every entry as defense in depth. Redacted object keys are omitted; redacted scalar values become `"[redacted]"`. `DiagnosticExportValidator` scans entry names, JSON keys, Authorization patterns, and token-shaped test markers; validation failure deletes the ZIP and returns an error.

- [ ] **Step 4: Write the deterministic ZIP**

Use `ZipOutputStream`, UTF-8, sorted rows, and these entries:

```text
manifest.json
status.json
settings.json
workmanager.json
sync-history.jsonl
logs.jsonl
location-points.jsonl
dropped-location.jsonl
policy-transitions.jsonl
schedule-cache.json
database-counts.json
```

Write to `cacheDir/diagnostics`; export works without auth/network.

- [ ] **Step 5: Implement retention and clear boundaries**

- logs: selected 1/7/14/30-day age plus 20 MB cap, oldest first;
- verbose logging: automatically standard after `verboseLoggingUntilUtcMillis`;
- expired ZIP packages: delete after 24 hours;
- clear diagnostics: delete logs, dropped fixes, policy transitions, stale schedule cache, and terminal sync runs except newest terminal;
- never delete active runs, business queues, dead letters, settings, auth, or fresh schedule cache.

- [ ] **Step 6: Add read-only FileProvider sharing**

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.diagnostics.files"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/diagnostic_file_paths" />
</provider>
```

`diagnostic_file_paths.xml` exposes only `<cache-path name="diagnostics" path="diagnostics/" />`. Share with `Intent.ACTION_SEND`, MIME `application/zip`, read permission only.

- [ ] **Step 7: Run all diagnostic tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.diagnostics.*" --no-daemon
```

Expected: PASS; raw coordinate is present, credentials are absent, invalid ZIP is deleted, and clear retains protected rows.

- [ ] **Step 8: Commit local diagnostics**

```powershell
git add src/client-android/app/src/main/AndroidManifest.xml src/client-android/app/src/main/res/xml src/client-android/app/src/main/java/com/pim/app/diagnostics src/client-android/app/src/main/java/com/pim/app/mobile/logs src/client-android/app/src/main/java/com/pim/app/data/DiagnosticDao.kt src/client-android/app/src/test/java/com/pim/app/diagnostics
git commit -m "feat: export local android diagnostics"
```

## Task 12: Build The Operational Status Model And Actionable Issue Planner

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/status/OperationalStatusModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/OperationalStatusRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssuePlanner.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Delete after callers migrate: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/OperationalHealthEvaluatorTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssuePlannerTest.kt`

- [ ] **Step 1: Write failing health-separation tests**

```kotlin
@Test
fun uploadingDoesNotTurnHealthyDeviceIntoWarning() {
    val state = healthyFacts().copy(sync = activeSync(SyncPhase.UploadingLocations))
    assertEquals(OperationalHealth.Healthy, evaluator.evaluate(state))
}

@Test
fun stoppedServiceIsNotAnIssueWhenCollectionWasDisabledByUser() {
    val issues = planner.plan(healthyFacts().copy(collectionDesired = false, serviceRunning = false))
    assertFalse(issues.any { it.code == "foreground-service-not-running" })
}

@Test
fun successfulInfoLogIsNeverLabeledRecentError() {
    val issues = planner.plan(healthyFacts().copy(latestErrorLog = null, latestInfoLog = "手机同步已完成。"))
    assertFalse(issues.any { it.code == "recent-error" })
}

private val evaluator = OperationalHealthEvaluator()
private val planner = StatusIssuePlanner(evaluator)

private fun healthyFacts() = OperationalFacts(
    collectionDesired = true,
    serviceRunning = true,
    corePrerequisitesReady = true,
    optionalDegradationCodes = emptySet(),
    connectionProbeFresh = true,
    businessQueue = BusinessQueueSnapshot(0, 0, 0, 0, null, 0L),
    sync = null,
    consecutiveTransientSyncFailures = 0,
    latestErrorLog = null,
    latestInfoLog = null,
    latestDroppedFixCode = null,
    deadLetterCount = 0,
    diagnosticBytes = 0L,
    lastAcceptedPointAtUtcMillis = 900L
)

private fun activeSync(phase: SyncPhase) = SyncRun(
    id = SyncRunId("run-active"),
    workManagerId = "work-active",
    trigger = SyncTrigger.Manual,
    allowMeteredOnce = false,
    requestedAtUtcMillis = 900L,
    startedAtUtcMillis = 950L,
    finishedAtUtcMillis = null,
    phase = phase,
    progressKey = "sync.${phase.name}",
    category = null,
    windowIndex = 0,
    windowTotal = null,
    queueAtStart = BusinessQueueSnapshot(1, 0, 0, 0, 800L, null),
    queueAtFinish = null,
    countsByCategory = emptyMap(),
    failure = null,
    nextAttemptAtUtcMillis = null,
    retryCount = 0,
    outcome = null
)
```

- [ ] **Step 2: Define operational facts separately from UI copy**

```kotlin
enum class OperationalHealth { Healthy, NeedsAttention, Blocked, Unknown }

data class StatusIssue(
    val code: String,
    val severity: OperationalHealth,
    val titleKey: String,
    val happenedKey: String,
    val impactKey: String,
    val evidence: String,
    val lastOccurredAtUtcMillis: Long?,
    val automaticRecoveryKey: String,
    val action: StatusAction,
    val actionLabelKey: String,
    val technicalDetails: String?
)

data class OperationalFacts(
    val collectionDesired: Boolean,
    val serviceRunning: Boolean,
    val corePrerequisitesReady: Boolean,
    val optionalDegradationCodes: Set<String>,
    val connectionProbeFresh: Boolean,
    val businessQueue: BusinessQueueSnapshot,
    val sync: SyncRun?,
    val consecutiveTransientSyncFailures: Int,
    val latestErrorLog: String?,
    val latestInfoLog: String?,
    val latestDroppedFixCode: String?,
    val deadLetterCount: Int,
    val diagnosticBytes: Long,
    val lastAcceptedPointAtUtcMillis: Long?
)
```

Primary Chinese copy maps from keys in the UI layer; technical codes stay in details/export.

- [ ] **Step 3: Implement repository composition**

Combine:

- four-category business queue count, oldest age, approximate bytes;
- latest active/recent `SyncRun` and WorkInfo state/next schedule estimate;
- startup recovery record;
- live prerequisites and foreground service runtime;
- current tracking settings/policy and last accepted point;
- connection probe and its five-minute freshness;
- error-level log only, latest drop, dead-letter count, diagnostic bytes.

Do not include logs, sync batches, or device profile in business pending total.

- [ ] **Step 4: Implement health and issue rules**

- `Healthy`: every enabled core function has fresh evidence;
- `NeedsAttention`: quality/optional degradation or three consecutive transient sync failures;
- `Blocked`: enabled core collection/transfer cannot operate;
- `Unknown`: required evidence absent/stale;
- no data expected or collection intentionally off does not become Unknown;
- automatic retries appear in transfer detail, not Needs Action, until user intervention is useful.

Issue actions and labels must match the design table exactly.

- [ ] **Step 5: Run status model tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.status.OperationalHealthEvaluatorTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.status.StatusIssuePlannerTest" --no-daemon
```

Expected: PASS for healthy uploading, blocked collection, waiting network, retry estimate, stale probe, dead letters, and intentional-off states.

- [ ] **Step 6: Commit operational status facts**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/status src/client-android/app/src/test/java/com/pim/app/status
git commit -m "feat: derive android status from operational evidence"
```

## Task 13: Replace Status And Settings Placeholders With Behavior-Tested Screens

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/diagnostics/DiagnosticsViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/diagnostics/DiagnosticsScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/settings/SettingsContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/diagnostics/DiagnosticsContentTest.kt`

- [ ] **Step 1: Write failing Compose tests for manual sync and named actions**

```kotlin
@Test
fun manualSyncImmediatelyShowsQueuedAndInvokesSyncAction() {
    var action: StatusAction? = null
    var state by mutableStateOf(statusState())
    compose.setContent {
        StatusCenterContent(
            state = state,
            onAction = {
                action = it
                if (it == StatusAction.SyncNow) {
                    state = statusState(SyncPhase.Queued, SyncTrigger.Manual)
                }
            }
        )
    }

    compose.onNodeWithText("立即同步").performClick()
    assertEquals(StatusAction.SyncNow, action)
    compose.onNodeWithText("已排队").assertExists()
    compose.onNodeWithText("手动触发").assertExists()
}

@Test
fun everyVisibleIssueButtonEmitsItsNamedAction() {
    val expected = listOf(
        "配置服务器" to StatusAction.OpenApiSettings,
        "登录" to StatusAction.OpenLogin,
        "允许通知" to StatusAction.RequestNotificationPermission,
        "允许精确定位" to StatusAction.RequestPreciseLocation,
        "允许后台定位" to StatusAction.OpenBackgroundLocation,
        "允许使用情况访问" to StatusAction.OpenUsageAccess,
        "允许运动识别" to StatusAction.RequestActivityRecognition,
        "调整电池设置" to StatusAction.OpenBatterySettings,
        "开启系统定位" to StatusAction.OpenSystemLocationSettings,
        "开始采集" to StatusAction.StartCollection,
        "立即同步" to StatusAction.SyncNow,
        "查看传输" to StatusAction.OpenTransfer,
        "查看诊断" to StatusAction.OpenDiagnostics,
        "查看定位质量" to StatusAction.OpenLocationQuality
    )
    val observed = mutableListOf<StatusAction>()
    val issues = expected.mapIndexed { index, (label, action) ->
        StatusIssueUi(
            code = "issue-$index",
            severity = OperationalHealth.NeedsAttention,
            title = "测试问题 $index",
            happened = "测试事件",
            impact = "测试影响",
            evidence = "test-evidence",
            lastOccurredAtUtcMillis = 1_000L,
            automaticRecovery = "等待用户操作",
            action = action,
            actionLabel = label,
            technicalDetails = null
        )
    }
    compose.setContent {
        StatusCenterContent(statusState().copy(issues = issues), onAction = { observed += it })
    }
    expected.forEach { (label, _) -> compose.onNodeWithText(label).performClick() }
    assertEquals(expected.map { it.second }, observed)
}

private fun statusState(
    phase: SyncPhase? = null,
    trigger: SyncTrigger? = null
) = StatusCenterUiState(
    health = OperationalHealth.Healthy,
    conclusionText = "采集与传输正常",
    evidenceAtUtcMillis = 1_000L,
    issues = emptyList(),
    transfer = TransferUiState(
        trigger = trigger,
        phase = phase,
        pendingBusinessCount = if (phase == null) 1 else 0,
        attempted = 0,
        serverConfirmed = 0,
        total = null,
        lastSuccessAtUtcMillis = null,
        nextAttemptAtUtcMillis = null,
        failureText = null
    ),
    collectionSummary = "持续采集已开启",
    connectionSummary = "服务器可达",
    diagnosticSummary = "没有活动错误"
)
```

The exhaustive callback test above covers API, login, every permission/system setting, service start, transfer, diagnostics, and location-quality actions without source-text assertions.

- [ ] **Step 2: Implement the Status layout in fixed section order**

1. overall conclusion: health, impact, evidence time, action count;
2. Needs Action: active actionable issues only;
3. Data Transfer: trigger, typed phase, category/window, counts, confirmation, business queue, last success, next estimate;
4. Collection and Connection: probe, auth, permissions, service, policy, next/last point;
5. Diagnostic Evidence: active errors, last rejection, history, export.

Known totals use a progress bar. Unknown totals show phase/window/count without a fake percent. The manual button calls `viewModel.syncNow()` and is disabled/joined while a run is active.

Use this UI contract so tests do not construct repository internals:

```kotlin
data class StatusCenterUiState(
    val health: OperationalHealth,
    val conclusionText: String,
    val evidenceAtUtcMillis: Long?,
    val issues: List<StatusIssueUi>,
    val transfer: TransferUiState,
    val collectionSummary: String,
    val connectionSummary: String,
    val diagnosticSummary: String
)

data class StatusIssueUi(
    val code: String,
    val severity: OperationalHealth,
    val title: String,
    val happened: String,
    val impact: String,
    val evidence: String,
    val lastOccurredAtUtcMillis: Long?,
    val automaticRecovery: String,
    val action: StatusAction,
    val actionLabel: String,
    val technicalDetails: String?
)

data class TransferUiState(
    val trigger: SyncTrigger?,
    val phase: SyncPhase?,
    val pendingBusinessCount: Int,
    val attempted: Int,
    val serverConfirmed: Int,
    val total: Int?,
    val lastSuccessAtUtcMillis: Long?,
    val nextAttemptAtUtcMillis: Long?,
    val failureText: String?
)
```

- [ ] **Step 3: Implement settings behavior and permission return refresh**

Expose:

- API edit/save, derived Web root, real staged probe with time/stage;
- login/logout/token validity without token text;
- three preset segmented control, bounded advanced controls, restore defaults;
- any/unmetered network choice and one-run override explanation;
- continuous collection desired state and exact blocked reason;
- notification, precise/background location, usage, activity, battery, provider, service state;
- diagnostics retention and 24-hour verbose toggle.

Use Activity Result launchers for runtime permission requests. Refresh prerequisites/probe when the app returns to foreground.

- [ ] **Step 4: Implement diagnostics view, raw-location confirmation, clear, and share**

The view lists active errors and recent typed history, supports 24h/7d/all range, requires an explicit coordinates warning before export, displays generation/validation failures, launches the read-only share intent, and confirms clear with the protected-row statement.

- [ ] **Step 5: Add root subpage navigation without adding a sixth tab**

Keep the five bottom destinations. Under Status, maintain `StatusSubpage.Center`, `StatusSubpage.Diagnostics`, and `StatusSubpage.LocationQuality`; back returns to Center. Actions can focus the transfer section using a stable section key.

- [ ] **Step 6: Run Compose instrumentation tests on Pixel_9**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.pim.app.ui --no-daemon
```

Expected: PASS; every button callback matches its text, manual sync shows queued immediately, settings persist across recreation, and diagnostic export requires confirmation.

- [ ] **Step 7: Run all Android tests and build release**

```powershell
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS. Local release may be unsigned when signing environment variables are absent; record that fact rather than treating it as the final signed APK.

- [ ] **Step 8: Commit the Phase 1 screens**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui src/client-android/app/src/androidTest/java/com/pim/app/ui
git commit -m "feat: complete android status and settings workflows"
```

## Task 14: Verify Phase 1, Update Coverage, And Open The PR

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- Create: `docs/superpowers/reports/2026-07-10-android-client-reliability-phase-1.md`
- Modify: `scripts/build-android.bat`

- [ ] **Step 1: Correct the documented emulator API default**

Change only the displayed emulator address in `scripts/build-android.bat` from port `5000` to the repository default:

```text
http://10.0.2.2:5858/api/v1/
```

- [ ] **Step 2: Run focused backend and full solution tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pim.UnitTests.Mobile|FullyQualifiedName~Pim.UnitTests.Api"
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 3: Run the complete Phase 1 Android gate**

```powershell
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS on Pixel_9. Inspect WorkManager and assert one `pim_mobile_sync_periodic`, no obsolete scheduled work, and no active duplicate lease.

- [ ] **Step 4: Exercise emulator operational scenarios**

Record screenshots/evidence for Healthy, Queued, Uploading, WaitingForNetwork, WaitingForAllowedNetwork, RetryScheduled, Blocked login, blocked permission, SucceededWithRejects, and diagnostic export. Verify phone-sized text at larger font scale.

- [ ] **Step 5: Update coverage rows REL-01 through REL-08 and REL-12**

Mark a row `Verified` only with command/test/screenshot evidence. Phase 2/3 rows stay `Planned`.

- [ ] **Step 6: Write the Phase 1 report**

Include commit hashes, commands, pass counts, AVD identity, Room fixture facts, WorkInfo names, known unsigned/signed status, and remaining Phase 2/3 scope. Do not attach raw diagnostic ZIPs.

- [ ] **Step 7: Commit verification evidence**

```powershell
git add scripts/build-android.bat docs/superpowers/reports
git commit -m "docs: record android operational foundation evidence"
```

- [ ] **Step 8: Recheck the final diff before push**

```powershell
git status --short --branch
git diff --check origin/master...HEAD
git log --oneline origin/master..HEAD
```

Expected: only intentional source/tests/scripts/docs; no build, wwwroot, APK, ZIP, private location, or `.opencode/` files.

- [ ] **Step 9: Push, open the Phase 1 PR, and observe checks**

```powershell
git push -u origin codex/android-operational-foundation
gh pr create --base master --head codex/android-operational-foundation --title "feat: establish android operational reliability" --body-file docs/superpowers/reports/2026-07-10-android-client-reliability-phase-1.md
gh pr checks --watch
```

Expected: Android and API workflows trigger and pass. Web may not trigger because Phase 1 does not touch `src/client-web/**`; record that exact path-filter result.

## Phase 1 Completion Gate

Do not begin Phase 2 until the Phase 1 PR is merged and all of these are true:

- Room 3→4 migration fixture with 530 logs passes on an emulator;
- only one periodic sync request exists;
- manual sync persists Queued before work execution;
- item-level acknowledgement and dead letters are behavior-tested;
- connection probe distinguishes transient, TLS, auth, path, and capability failures;
- collection intent survives transient blockers and boot/update recovery;
- Status, Settings, permissions, diagnostics, export, clear, and share interactions pass Compose tests;
- full Android and .NET gates pass;
- relevant GitHub Actions are green;
- coverage report preserves Phase 2 and Phase 3 as remaining scope.
