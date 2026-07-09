# Android App v2 Complete Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete native Android v2 PIM client described in `docs/superpowers/specs/2026-07-08-android-app-v2-complete-redesign-design.md`: clear API connection, five native tabs, manually controlled continuous collection, Android-compliant foreground location service, schedule/motion-aware tracking policy, strict location quality gate, local queues, status center, PR delivery, and green GitHub Actions.

**Architecture:** Keep the existing Android Gradle project, Hilt, Room, Retrofit, WorkManager, and Mobile module contracts, but replace the companion WebView shell with a native collection app. The foreground service owns continuous location and notification updates; pure Kotlin policy/quality classes decide intervals and upload eligibility; Room stores accepted points, dropped diagnostics, policy logs, and queues; WorkManager remains sync/heartbeat/retry fallback. Backend work is limited to contract alignment that Android v2 requires, especially strict `< 50m` accuracy and existing mobile query endpoints.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Hilt, Room, Retrofit/kotlinx.serialization, WorkManager, Google Play services Location/Activity Recognition, .NET 8 Minimal API, EF Core, xUnit, JUnit4, GitHub Actions.

---

## Scope Check

This is one Android v2 plan with a small backend contract lane. It is not split into separate plans because Android cannot meet the accepted design unless the backend contract accepts null altitude metadata, rejects `>= 50m` consistently, and exposes location/usage query data already present under `Pim.Module.Mobile`.

No Web client redesign is included. Web verification is only required if a backend contract or shared mobile API shape breaks the existing web client.

## Source Of Truth

- Design spec: `docs/superpowers/specs/2026-07-08-android-app-v2-complete-redesign-design.md`
- Visual companions recorded by the spec:
  - `.superpowers/brainstorm/android-app-20260708-213008/content/android-ui-home-options.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/android-module-architecture.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/android-visual-style-options.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/today-home-content-options.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/ui-architecture-v1.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/location-state-machine-v1.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/android-architecture-dataflow-v1.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/permissions-errors-testing-v1.html`
  - `.superpowers/brainstorm/android-app-20260708-213008/content/waiting-after-home-selection.html`

The generated `.superpowers/brainstorm/` files stay out of commits.

## Delivery Discipline

- Create a new branch before implementation: `codex/android-app-v2-redesign`.
- Use concurrent subagents after shared contracts are locked.
- Make focused commits at stable checkpoints.
- Push the branch and create a PR.
- Wait for GitHub Actions checks after PR creation.
- Do not call the branch complete until local verification and relevant GitHub Actions are green, or exact unrelated failures are documented.

## Parallel Subagent Assignment

Use `superpowers:subagent-driven-development` for execution. After Task 0 and Task 1 define shared contracts, dispatch these subagents concurrently:

- Subagent A, UI native shell: `src/client-android/app/src/main/java/com/pim/app/ui/**`, `MainActivity.kt`, launcher manifest entries. Builds five tabs, design system, Today, Tracks, Schedule, Status, Settings screens.
- Subagent B, API/auth/settings: `src/client-android/core/src/main/java/com/pim/core/settings/**`, `src/client-android/core/src/main/java/com/pim/core/network/**`, `src/client-android/core/src/main/java/com/pim/core/models/**`, `src/client-android/app/src/main/java/com/pim/app/settings/**`.
- Subagent C, foreground service/notification: `src/client-android/app/src/main/java/com/pim/app/location/service/**`, `src/client-android/app/src/main/java/com/pim/app/notifications/**`, `AndroidManifest.xml`.
- Subagent D, policy/motion/schedule: `src/client-android/app/src/main/java/com/pim/app/location/policy/**`, `src/client-android/app/src/main/java/com/pim/app/location/motion/**`, `src/client-android/app/src/main/java/com/pim/app/schedule/**`.
- Subagent E, quality/Room/sync: `src/client-android/app/src/main/java/com/pim/app/location/quality/**`, `src/client-android/app/src/main/java/com/pim/app/data/**`, `src/client-android/app/src/main/java/com/pim/app/mobile/sync/**`.
- Subagent F, status/permissions/diagnostics: `src/client-android/app/src/main/java/com/pim/app/status/**`, `src/client-android/app/src/main/java/com/pim/app/ui/status/**`, `src/client-android/app/src/main/java/com/pim/app/permissions/**`.
- Subagent G, backend contract: `src/modules/Pim.Module.Mobile/**`, `tests/Pim.UnitTests/Mobile/**`.
- Subagent H, verification/CI readiness: Android test source cleanup, Gradle build health, workflow status, final PR checklist.

Each subagent must return changed files, commands run, results, and blockers. Main agent reviews after every subagent result before merging work.

## File Structure Map

### Android App Module

- Modify: `src/client-android/app/build.gradle.kts`
  - Adds Google Play services location dependency and Room testing dependency if needed.
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
  - Makes `MainActivity` the launcher.
  - Declares background location, activity recognition, foreground service location permission, and `ForegroundLocationService` with `android:foregroundServiceType="location"`.
- Modify: `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`
  - Removes automatic `DataCollector.start()` from activity lifecycle.
  - Hosts `PimRootScreen`.
- Keep but stop using as launcher: `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt`
  - It may remain temporarily for endpoint companion features, but Android v2 launcher must not be the WebView shell.
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt`
  - Defines five bottom tabs: Today, Tracks, Schedule, Status, Settings.
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
  - Compose scaffold, top context, bottom navigation, screen routing.
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt`
  - Light map-tool palette and typography.
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/components/PimStatusChip.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/components/PimMetricRow.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`

### Android Core Settings And API

- Modify: `src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt`
  - Blank default for Android UI, URL normalization, localhost warning support.
- Create: `src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt`
  - Pure URL validation and warning model.
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt`
  - Throws clear configuration error for blank/invalid base URL instead of silently using phone-local `127.0.0.1`.
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt`
  - Adds mobile query endpoints for summary, timeline, quality, location history, location overview, tracks, segment points.
- Modify: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`
  - Adds Android DTOs matching existing backend mobile query DTOs and v2 location metadata.

### Android Tracking Core

- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
  - Persists normal interval `3 min`, schedule low-frequency `15 min`, movement interval `1 min`, recovery threshold `100m`, altitude wait `15s`, hard accuracy threshold `50m`.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
  - Pure state machine.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
  - `TrackingPolicy`, `LocationPolicyMode`, `PolicyDecision`, `ScheduleWindow`, `MotionSignal`.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt`
  - Haversine distance helper for 100m recovery.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt`
  - Strict `< 50m` gate and altitude wait result model.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt`
  - Coroutine wrapper around the pure quality gate.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
  - Owns continuous location request lifecycle.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt`
  - Starts/stops/binds commands from UI and notification actions.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt`
  - Activity transition registration and fallback status.
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
  - Loads calendar events and exposes current/upcoming windows with location text.

### Android Persistence And Sync

- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
  - Adds policy, quality flags, submitted timestamp, dropped diagnostic entity, policy transition entity, collection status entity.
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
  - Adds accepted point queue reads, mark synced/failed, dropped diagnostics, transition logs, summary queries.
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
  - Version bump and explicit migration.
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt`
  - Uses explicit migrations and stops destructive migration for queued collection data.
- Create: `src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt`
  - Inserts accepted points and dropped diagnostics.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt`
  - Uploads queued accepted points and preserves partial failures.
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
  - Incorporates location queue upload into manual/open-app sync without making WorkManager the location scheduler.
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationSyncWorker.kt`
  - Network retry worker for queued accepted points.
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt`
  - Creates `LocationSyncWorker`.

### Android Status And Notifications

- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt`
  - Handles pause/resume, sync now, open status.

### Backend Mobile Module

- Modify: `src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs`
  - Rejects `HorizontalAccuracyMeters >= 50`.
  - Preserves null altitude.
  - Reads quality flags from raw JSON when present.
- Modify: `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs`
  - Adds optional submitted/policy/motion/quality fields only if backend tests prove Android needs first-class fields beyond `RawJson`.
- Modify: `tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs`
  - Changes `50m` test from accepted to rejected; adds `49.9m` accepted and null altitude accepted.

## Shared Contracts

Use these names consistently across tasks:

```kotlin
enum class LocationPolicyMode {
    Off,
    PowerSavingNormal,
    ScheduleLowFrequency,
    MotionObservation,
    MovementRecovery,
    SyncFallback
}

data class TrackingPolicy(
    val normalIntervalMillis: Long = 3 * 60 * 1000L,
    val scheduleLowFrequencyIntervalMillis: Long = 15 * 60 * 1000L,
    val movementIntervalMillis: Long = 60 * 1000L,
    val scheduleRecoveryThresholdMeters: Double = 100.0,
    val altitudeWaitTimeoutMillis: Long = 15 * 1000L,
    val maxUploadAccuracyMetersExclusive: Float = 50f
)

data class PolicyDecision(
    val mode: LocationPolicyMode,
    val requestIntervalMillis: Long,
    val nextExpectedLocationAtMillis: Long,
    val reason: String,
    val scheduleLowFrequency: Boolean
)

data class ScheduleWindow(
    val id: String,
    val title: String,
    val locationText: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long
)

enum class MotionSignal {
    Unknown,
    Still,
    Walking,
    Running,
    OnBicycle,
    InVehicle
}

data class AcceptedLocation(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float,
    val altitudeMeters: Double?,
    val provider: String,
    val recordedAtUtcMillis: Long,
    val submittedAtUtcMillis: Long,
    val policyMode: LocationPolicyMode,
    val scheduleLowFrequency: Boolean,
    val motionSignal: MotionSignal,
    val qualityFlags: Set<String>
)

data class DroppedLocationDiagnostic(
    val recordedAtUtcMillis: Long,
    val provider: String?,
    val horizontalAccuracyMeters: Float?,
    val policyMode: LocationPolicyMode,
    val reason: String
)
```

## Task 0: Branch, Baseline, And Plan Commit

**Files:**
- Create during this task: no code files
- Commit already saved plan file: `docs/superpowers/plans/2026-07-08-android-app-v2-complete-redesign.md`

- [ ] **Step 1: Confirm repository state**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git status --short --branch
```

Expected: current branch is `master`; only intentional docs changes are present. If `master` is behind `origin/master`, run `git pull --ff-only` before creating the feature branch.

- [ ] **Step 2: Create feature branch**

Run:

```powershell
git switch master
git pull --ff-only
git switch -c codex/android-app-v2-redesign
```

Expected: `git status --short --branch` prints `## codex/android-app-v2-redesign`.

- [ ] **Step 3: Run baseline Android test command**

Run:

```powershell
Set-Location src\client-android
.\gradlew.bat testDebugUnitTest --no-daemon
Set-Location ..\..
```

Expected: command either passes or exposes current compile/test failures. If the existing mojibake sources fail to compile, record the exact compiler files and continue with Task 2 because Task 2 replaces those files.

- [ ] **Step 4: Commit this plan if not already committed**

Run:

```powershell
git add docs/superpowers/plans/2026-07-08-android-app-v2-complete-redesign.md
git commit -m "docs: plan android app v2 implementation"
```

Expected: a docs commit containing only the plan file.

## Task 1: Dependencies, Manifest Tests, And Launcher Ownership

**Files:**
- Modify: `src/client-android/app/build.gradle.kts`
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Modify: `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt`

- [ ] **Step 1: Write manifest contract test**

Create `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt`:

```kotlin
package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2ManifestTest {
    @Test
    fun manifestDeclaresNativeLauncherAndLocationForegroundService() {
        val manifest = repoFile("src", "main", "AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
        assertTrue(manifest.contains("android.permission.ACTIVITY_RECOGNITION"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_LOCATION"))
        assertTrue(manifest.contains(".location.service.ForegroundLocationService"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"location\""))
        assertTrue(manifest.contains("android:name=\".MainActivity\""))
        assertFalse("Web shell must not be the launcher", launcherBlock(manifest).contains(".ui.shell.PimShellActivity"))
    }

    private fun launcherBlock(manifest: String): String {
        val launcherIndex = manifest.indexOf("android.intent.category.LAUNCHER")
        if (launcherIndex < 0) return ""
        val start = manifest.lastIndexOf("<activity", launcherIndex).coerceAtLeast(0)
        val end = manifest.indexOf("</activity>", launcherIndex).let { if (it < 0) manifest.length else it }
        return manifest.substring(start, end)
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
```

- [ ] **Step 2: Run test and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2ManifestTest --no-daemon
cd ..\..
```

Expected: FAIL because `ACCESS_BACKGROUND_LOCATION`, `ACTIVITY_RECOGNITION`, `FOREGROUND_SERVICE_LOCATION`, and `ForegroundLocationService` are missing, and the Web shell is still the launcher.

- [ ] **Step 3: Add dependencies**

Modify `src/client-android/app/build.gradle.kts` dependencies:

```kotlin
implementation("com.google.android.gms:play-services-location:21.3.0")
testImplementation("androidx.room:room-testing:2.6.1")
```

- [ ] **Step 4: Change manifest ownership**

Modify `AndroidManifest.xml` so permissions include:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Make `MainActivity` the launcher:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".ui.shell.PimShellActivity"
    android:exported="false" />
```

Add service declaration:

```xml
<service
    android:name=".location.service.ForegroundLocationService"
    android:exported="false"
    android:foregroundServiceType="location" />
```

- [ ] **Step 5: Make `MainActivity` host v2 root**

Temporarily make `MainActivity.kt` compile against a root screen that Task 2 creates:

```kotlin
package com.pim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.pim.app.ui.root.PimRootScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PimRootScreen() }
    }
}
```

- [ ] **Step 6: Run manifest test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2ManifestTest --no-daemon
cd ..\..
```

Expected: PASS after Task 2 root file exists; if it fails before Task 2 because `PimRootScreen` is missing, keep the failing result and resolve in Task 2.

- [ ] **Step 7: Commit**

Run after Task 2 if compilation required both tasks:

```powershell
git add src/client-android/app/build.gradle.kts src/client-android/app/src/main/AndroidManifest.xml src/client-android/app/src/main/java/com/pim/app/MainActivity.kt src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt
git commit -m "feat: prepare android v2 launcher and permissions"
```

## Task 2: Native UI Shell, Theme, And Five Tabs

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt`

- [ ] **Step 1: Write shell source test**

Create `AndroidV2NativeShellTest.kt`:

```kotlin
package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2NativeShellTest {
    @Test
    fun rootDefinesApprovedFiveTabsAndNoWebViewPrimaryExperience() {
        val destination = repoFile("src", "main", "java", "com", "pim", "app", "ui", "root", "PimDestination.kt").readText()
        val root = repoFile("src", "main", "java", "com", "pim", "app", "ui", "root", "PimRootScreen.kt").readText()

        for (label in listOf("今日", "轨迹", "日程", "状态", "设置")) {
            assertTrue("$label tab must be present", destination.contains(label))
        }
        assertTrue(root.contains("NavigationBar"))
        assertTrue(root.contains("PimTheme"))
        assertFalse(root.contains("PimWebViewScreen"))
        assertFalse(root.contains("WebView"))
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
```

- [ ] **Step 2: Run test and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2NativeShellTest --no-daemon
cd ..\..
```

Expected: FAIL because the files do not exist.

- [ ] **Step 3: Create destination enum**

Create `PimDestination.kt`:

```kotlin
package com.pim.app.ui.root

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

enum class PimDestination(
    val label: String,
    val icon: ImageVector
) {
    Today("今日", Icons.Filled.LocationOn),
    Tracks("轨迹", Icons.Filled.Map),
    Schedule("日程", Icons.Filled.CalendarToday),
    Status("状态", Icons.Filled.VerifiedUser),
    Settings("设置", Icons.Filled.Settings)
}
```

- [ ] **Step 4: Create theme**

Create `PimTheme.kt`:

```kotlin
package com.pim.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PimLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1D63D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF09306F),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFB300),
    error = Color(0xFFC62828),
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF6),
    outlineVariant = Color(0xFFD4DCE8)
)

@Composable
fun PimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PimLightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
```

- [ ] **Step 5: Create shared section component**

Create `PimSection.kt`:

```kotlin
package com.pim.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PimSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
```

- [ ] **Step 6: Create native placeholder screens with approved hierarchy**

Each screen must use Chinese labels and no WebView. Create the five screen files with these function names:

```kotlin
@Composable fun TodayScreen()
@Composable fun TracksScreen()
@Composable fun SchedulePolicyScreen()
@Composable fun StatusCenterScreen()
@Composable fun SettingsScreen()
```

The visible first version must contain these section titles:

```text
今日概览
轨迹历史
日程低频策略
状态中心
设置
```

- [ ] **Step 7: Create root scaffold**

Create `PimRootScreen.kt`:

```kotlin
package com.pim.app.ui.root

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pim.app.ui.schedule.SchedulePolicyScreen
import com.pim.app.ui.settings.SettingsScreen
import com.pim.app.ui.status.StatusCenterScreen
import com.pim.app.ui.theme.PimTheme
import com.pim.app.ui.today.TodayScreen
import com.pim.app.ui.tracks.TracksScreen

@Composable
fun PimRootScreen() {
    var selected by rememberSaveable { mutableStateOf(PimDestination.Today) }
    PimTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    PimDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (selected) {
                PimDestination.Today -> TodayScreen()
                PimDestination.Tracks -> TracksScreen()
                PimDestination.Schedule -> SchedulePolicyScreen()
                PimDestination.Status -> StatusCenterScreen()
                PimDestination.Settings -> SettingsScreen()
            }
        }
    }
}
```

If `modifier` is unused after lint, pass it to each screen signature before committing.

- [ ] **Step 8: Run shell tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2NativeShellTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2ManifestTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 9: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui src/client-android/app/src/test/java/com/pim/app/v2 src/client-android/app/src/main/java/com/pim/app/MainActivity.kt src/client-android/app/src/main/AndroidManifest.xml
git commit -m "feat: add native android v2 shell"
```

## Task 3: API Address, URL Validation, Login, And Retrofit Rebuild

**Files:**
- Modify: `src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt`
- Create: `src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt`
- Create: `src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt`
- Create: `src/client-android/core/src/test/java/com/pim/core/network/ApiClientProviderConfigurationTest.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Write URL validator tests**

Create `ServerUrlValidatorTest.kt`:

```kotlin
package com.pim.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlValidatorTest {
    @Test
    fun blankUrlIsNotConfigured() {
        val result = ServerUrlValidator.validate("")
        assertFalse(result.isValid)
        assertEquals("missing", result.reasonCode)
    }

    @Test
    fun publicIpIsAcceptedAndGetsTrailingSlash() {
        val result = ServerUrlValidator.validate("http://203.0.113.8:5858/api/v1")
        assertTrue(result.isValid)
        assertEquals("http://203.0.113.8:5858/api/v1/", result.normalizedUrl)
    }

    @Test
    fun publicDomainIsAccepted() {
        val result = ServerUrlValidator.validate("https://pim.example.com/api/v1/")
        assertTrue(result.isValid)
        assertEquals("https://pim.example.com/api/v1/", result.normalizedUrl)
    }

    @Test
    fun realDeviceLocalhostReceivesWarning() {
        val result = ServerUrlValidator.validate("http://127.0.0.1:5858/api/v1/")
        assertTrue(result.isValid)
        assertTrue(result.warnings.contains("real-device-localhost"))
    }
}
```

- [ ] **Step 2: Run tests and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :core:testDebugUnitTest --tests com.pim.core.settings.ServerUrlValidatorTest --no-daemon
cd ..\..
```

Expected: FAIL because `ServerUrlValidator` does not exist.

- [ ] **Step 3: Implement validator**

Create `ServerUrlValidator.kt`:

```kotlin
package com.pim.core.settings

import java.net.URI

data class ServerUrlValidationResult(
    val input: String,
    val normalizedUrl: String,
    val isValid: Boolean,
    val reasonCode: String? = null,
    val warnings: Set<String> = emptySet()
)

object ServerUrlValidator {
    fun validate(value: String?): ServerUrlValidationResult {
        val input = value?.trim().orEmpty()
        if (input.isBlank()) {
            return ServerUrlValidationResult(input, "", false, "missing")
        }

        val uri = runCatching { URI(input) }.getOrNull()
            ?: return ServerUrlValidationResult(input, input, false, "invalid-url")

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return ServerUrlValidationResult(input, input, false, "invalid-scheme")
        }
        if (uri.host.isNullOrBlank()) {
            return ServerUrlValidationResult(input, input, false, "missing-host")
        }

        val normalized = input.trimEnd('/') + "/"
        val host = uri.host.lowercase()
        val warnings = buildSet {
            if (host == "127.0.0.1" || host == "localhost" || host == "::1") add("real-device-localhost")
            if (scheme == "http") add("cleartext-http")
        }
        return ServerUrlValidationResult(input, normalized, true, warnings = warnings)
    }
}
```

- [ ] **Step 4: Change server settings store**

Update `ServerSettingsStore` so blank remains blank and stored invalid URLs are returned as raw text for UI correction:

```kotlin
companion object {
    const val DEFAULT_BASE_URL = ""
    const val KEY_SERVER_BASE_URL = "server_base_url"
    private const val PREFS_NAME = "pim_server_settings"

    fun normalizeBaseUrl(value: String?): String {
        return ServerUrlValidator.validate(value).normalizedUrl
    }
}
```

`getBaseUrl()` must return `""` when nothing is configured.

- [ ] **Step 5: Make API provider fail clearly when unconfigured**

In `ApiClientProvider.createApiService`, validate before building Retrofit:

```kotlin
private fun createApiService(baseUrl: String, client: OkHttpClient): ApiService {
    val validation = ServerUrlValidator.validate(baseUrl)
    check(validation.isValid) {
        "API address is not configured or invalid: ${validation.reasonCode ?: "unknown"}"
    }
    return Retrofit.Builder()
        .baseUrl(validation.normalizedUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
        .build()
        .create(ApiService::class.java)
}
```

- [ ] **Step 6: Build Settings UI around API first**

`SettingsScreen` must put API address before collection controls. It must show:

- Text field label: `API 地址`
- Example: `https://pim.example.com/api/v1/`
- Warning for `real-device-localhost`: `在真机上 127.0.0.1 指向手机本机，通常无法连接你的服务器。`
- Buttons: `保存`, `测试连接`, `登录`, `退出登录`

- [ ] **Step 7: Run URL and Android shell tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :core:testDebugUnitTest --tests com.pim.core.settings.ServerUrlValidatorTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2NativeShellTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add src/client-android/core/src/main/java/com/pim/core/settings src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt src/client-android/core/src/test/java/com/pim/core src/client-android/app/src/main/java/com/pim/app/ui/settings
git commit -m "feat: make android api address explicit"
```

## Task 4: Tracking Settings Defaults And Continuous Collection Toggle

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Write defaults test**

Create `TrackingSettingsStoreTest.kt` with a pure defaults test:

```kotlin
package com.pim.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingSettingsStoreTest {
    @Test
    fun defaultProfileIsPowerSavingAndConfigurableValuesMatchSpec() {
        val defaults = TrackingSettings.defaults()
        assertEquals("power-saving", defaults.profile)
        assertEquals(3 * 60 * 1000L, defaults.normalIntervalMillis)
        assertEquals(15 * 60 * 1000L, defaults.scheduleLowFrequencyIntervalMillis)
        assertEquals(60 * 1000L, defaults.movementIntervalMillis)
        assertEquals(100.0, defaults.scheduleRecoveryThresholdMeters, 0.001)
        assertEquals(15 * 1000L, defaults.altitudeWaitTimeoutMillis)
        assertEquals(50f, defaults.maxUploadAccuracyMetersExclusive)
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.settings.TrackingSettingsStoreTest --no-daemon
cd ..\..
```

Expected: FAIL because `TrackingSettings` does not exist.

- [ ] **Step 3: Implement settings data and store**

Create:

```kotlin
package com.pim.app.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class TrackingSettings(
    val profile: String,
    val continuousCollectionEnabled: Boolean,
    val normalIntervalMillis: Long,
    val scheduleLowFrequencyIntervalMillis: Long,
    val movementIntervalMillis: Long,
    val scheduleRecoveryThresholdMeters: Double,
    val altitudeWaitTimeoutMillis: Long,
    val maxUploadAccuracyMetersExclusive: Float
) {
    companion object {
        fun defaults() = TrackingSettings(
            profile = "power-saving",
            continuousCollectionEnabled = false,
            normalIntervalMillis = 3 * 60 * 1000L,
            scheduleLowFrequencyIntervalMillis = 15 * 60 * 1000L,
            movementIntervalMillis = 60 * 1000L,
            scheduleRecoveryThresholdMeters = 100.0,
            altitudeWaitTimeoutMillis = 15 * 1000L,
            maxUploadAccuracyMetersExclusive = 50f
        )
    }
}

@Singleton
class TrackingSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("pim_tracking_settings", Context.MODE_PRIVATE)

    fun read(): TrackingSettings {
        val defaults = TrackingSettings.defaults()
        return defaults.copy(
            continuousCollectionEnabled = prefs.getBoolean("continuous_collection_enabled", defaults.continuousCollectionEnabled),
            normalIntervalMillis = prefs.getLong("normal_interval_ms", defaults.normalIntervalMillis),
            scheduleLowFrequencyIntervalMillis = prefs.getLong("schedule_low_frequency_interval_ms", defaults.scheduleLowFrequencyIntervalMillis),
            movementIntervalMillis = prefs.getLong("movement_interval_ms", defaults.movementIntervalMillis),
            scheduleRecoveryThresholdMeters = prefs.getFloat("schedule_recovery_threshold_m", defaults.scheduleRecoveryThresholdMeters.toFloat()).toDouble()
        )
    }

    fun setContinuousCollectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("continuous_collection_enabled", enabled).apply()
    }
}
```

- [ ] **Step 4: Add Settings UI controls**

`SettingsScreen` must show:

- `持续采集` switch, default off.
- `省电档` as current profile.
- `常规间隔 3 分钟`
- `日程低频 15 分钟`
- `移动间隔 1 分钟`
- `日程恢复阈值 100m`
- `高度等待 15 秒`
- `上传精度 < 50m`

- [ ] **Step 5: Wire toggle to service controller**

`SettingsViewModel` must call `ForegroundLocationController.start()` only after API address, login, notification permission, foreground precise location, and background location are ready. If not ready, it writes a `StatusIssue` and keeps persisted `continuousCollectionEnabled=false`.

- [ ] **Step 6: Run test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.settings.TrackingSettingsStoreTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/settings src/client-android/app/src/main/java/com/pim/app/ui/settings src/client-android/app/src/test/java/com/pim/app/settings
git commit -m "feat: add android tracking settings"
```

## Task 5: Policy Engine, Schedule Low Frequency, Motion, And 100m Recovery

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt`

- [ ] **Step 1: Write policy tests**

Create tests covering every accepted transition:

```kotlin
package com.pim.app.location.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPolicyEngineTest {
    private val policy = TrackingPolicy()
    private val now = 1_000_000L
    private val schedule = ScheduleWindow("s1", "办公室", "上海市黄浦区", now - 1_000L, now + 60_000L)

    @Test
    fun offBecomesNormalWhenCollectionStarts() {
        val engine = LocationPolicyEngine(policy)
        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true))
        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
        assertEquals(policy.normalIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun currentScheduleWithLocationEntersLowFrequency() {
        val engine = LocationPolicyEngine(policy)
        val decision = engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule))
        assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
        assertEquals(policy.scheduleLowFrequencyIntervalMillis, decision.requestIntervalMillis)
    }

    @Test
    fun scheduleEndsReturnsToNormal() {
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule))
        val decision = engine.reduce(LocationPolicyInput(nowMillis = now + 120_000L, collectionEnabled = true, currentScheduleWindow = null))
        assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
    }

    @Test
    fun movementOverOneHundredMetersRecoversFromScheduleLowFrequency() {
        val engine = LocationPolicyEngine(policy)
        engine.reduce(LocationPolicyInput(nowMillis = now, collectionEnabled = true, currentScheduleWindow = schedule))
        engine.onAcceptedLocation(PolicyLocation(31.230416, 121.473701, now))

        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now + 60_000L, collectionEnabled = true, currentScheduleWindow = schedule)
        )
        engine.onAcceptedLocation(PolicyLocation(31.232000, 121.473701, now + 60_000L))

        val recovered = engine.reduce(
            LocationPolicyInput(nowMillis = now + 61_000L, collectionEnabled = true, currentScheduleWindow = schedule)
        )

        assertEquals(LocationPolicyMode.MovementRecovery, recovered.mode)
        assertEquals(policy.movementIntervalMillis, recovered.requestIntervalMillis)
    }

    @Test
    fun motionSignalShortensInterval() {
        val engine = LocationPolicyEngine(policy)
        val decision = engine.reduce(
            LocationPolicyInput(nowMillis = now, collectionEnabled = true, motionSignal = MotionSignal.Walking)
        )
        assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
        assertEquals(policy.movementIntervalMillis, decision.requestIntervalMillis)
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.policy.LocationPolicyEngineTest --no-daemon
cd ..\..
```

Expected: FAIL because policy classes do not exist.

- [ ] **Step 3: Implement pure policy types**

Create the shared contracts from the Shared Contracts section in `LocationPolicyTypes.kt`, plus:

```kotlin
data class PolicyLocation(
    val latitude: Double,
    val longitude: Double,
    val recordedAtMillis: Long
)

data class LocationPolicyInput(
    val nowMillis: Long,
    val collectionEnabled: Boolean,
    val currentScheduleWindow: ScheduleWindow? = null,
    val motionSignal: MotionSignal = MotionSignal.Unknown
)
```

- [ ] **Step 4: Implement distance helper**

Create `GeoDistance.kt`:

```kotlin
package com.pim.app.location.policy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2).pow(2.0) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2.0)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
```

- [ ] **Step 5: Implement policy engine**

`LocationPolicyEngine` must:

- Keep last mode.
- Keep schedule anchor location when entering `ScheduleLowFrequency`.
- Use `MotionObservation` when signal is walking/running/bicycle/vehicle.
- Use `MovementRecovery` if distance from schedule anchor is `> 100m`.
- Return `Off` when collection disabled.

- [ ] **Step 6: Run tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.policy.LocationPolicyEngineTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/location/policy src/client-android/app/src/test/java/com/pim/app/location/policy
git commit -m "feat: add schedule aware location policy"
```

## Task 6: Strict Location Quality Gate And Altitude Wait

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt`

- [ ] **Step 1: Write quality tests**

Create `LocationQualityGateTest.kt`:

```kotlin
package com.pim.app.location.quality

import com.pim.app.location.policy.LocationPolicyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQualityGateTest {
    private val gate = LocationQualityGate(maxAccuracyMetersExclusive = 50f, altitudeWaitTimeoutMillis = 15_000L)

    @Test
    fun noAccuracyIsDropped() {
        val result = gate.evaluate(Fix(horizontalAccuracyMeters = null))
        assertTrue(result is QualityDecision.Drop)
        assertEquals("missing-horizontal-accuracy", (result as QualityDecision.Drop).reason)
    }

    @Test
    fun fortyNinePointNineMetersIsAccepted() {
        val result = gate.evaluate(Fix(horizontalAccuracyMeters = 49.9f, altitudeMeters = 12.0))
        assertTrue(result is QualityDecision.AcceptNow)
    }

    @Test
    fun fiftyMetersIsDropped() {
        val result = gate.evaluate(Fix(horizontalAccuracyMeters = 50.0f))
        assertTrue(result is QualityDecision.Drop)
        assertEquals("horizontal-accuracy-too-low", (result as QualityDecision.Drop).reason)
    }

    @Test
    fun missingAltitudeWaitsThenAcceptsNullAltitudeWithFlag() {
        val wait = gate.evaluate(Fix(horizontalAccuracyMeters = 18f, altitudeMeters = null, recordedAtMillis = 1_000L))
        assertTrue(wait is QualityDecision.WaitForAltitude)

        val timeout = gate.timeoutDecision(
            pending = (wait as QualityDecision.WaitForAltitude).pending,
            nowMillis = 16_001L
        )

        assertTrue(timeout is QualityDecision.AcceptNow)
        val accepted = (timeout as QualityDecision.AcceptNow).accepted
        assertNull(accepted.altitudeMeters)
        assertTrue(accepted.qualityFlags.contains("altitude-missing-timeout"))
    }

    private fun Fix(
        horizontalAccuracyMeters: Float?,
        altitudeMeters: Double? = null,
        recordedAtMillis: Long = 1_000L
    ) = RawLocationFix(
        latitude = 31.230416,
        longitude = 121.473701,
        horizontalAccuracyMeters = horizontalAccuracyMeters,
        altitudeMeters = altitudeMeters,
        provider = "gps",
        recordedAtMillis = recordedAtMillis,
        policyMode = LocationPolicyMode.PowerSavingNormal,
        scheduleLowFrequency = false,
        motionSignalName = "Unknown"
    )
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.quality.LocationQualityGateTest --no-daemon
cd ..\..
```

Expected: FAIL because quality classes do not exist.

- [ ] **Step 3: Implement quality gate types**

Create:

```kotlin
package com.pim.app.location.quality

import com.pim.app.location.policy.LocationPolicyMode

data class RawLocationFix(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val altitudeMeters: Double?,
    val provider: String,
    val recordedAtMillis: Long,
    val policyMode: LocationPolicyMode,
    val scheduleLowFrequency: Boolean,
    val motionSignalName: String
)

data class QualityAcceptedLocation(
    val fix: RawLocationFix,
    val altitudeMeters: Double?,
    val acceptedAtMillis: Long,
    val qualityFlags: Set<String>
)

data class PendingAltitudeFix(
    val fix: RawLocationFix,
    val deadlineMillis: Long
)

sealed class QualityDecision {
    data class AcceptNow(val accepted: QualityAcceptedLocation) : QualityDecision()
    data class WaitForAltitude(val pending: PendingAltitudeFix) : QualityDecision()
    data class Drop(val fix: RawLocationFix, val reason: String) : QualityDecision()
}
```

- [ ] **Step 4: Implement rules**

`LocationQualityGate.evaluate()` must implement:

- `horizontalAccuracyMeters == null` -> drop `missing-horizontal-accuracy`.
- `horizontalAccuracyMeters >= 50f` -> drop `horizontal-accuracy-too-low`.
- `< 50f` with altitude -> accept immediately.
- `< 50f` without altitude -> wait for altitude deadline.

`timeoutDecision()` must accept null altitude with `altitude-missing-timeout`.

- [ ] **Step 5: Implement coroutine coordinator**

`AltitudeWaitCoordinator` wraps the gate and exposes:

```kotlin
suspend fun handleFix(
    fix: RawLocationFix,
    onAccepted: suspend (QualityAcceptedLocation) -> Unit,
    onDropped: suspend (RawLocationFix, String) -> Unit
)
```

It must delay only for the pending fix's remaining `15s`; it must not fake altitude as `0`.

- [ ] **Step 6: Run tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.quality.LocationQualityGateTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/location/quality src/client-android/app/src/test/java/com/pim/app/location/quality
git commit -m "feat: add strict location quality gate"
```

## Task 7: Room Queue, Dropped Diagnostics, And Policy Logs

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt`

- [ ] **Step 1: Write queue mapping test**

Create `LocationQueueMappingTest.kt`:

```kotlin
package com.pim.app.location

import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQueueMappingTest {
    @Test
    fun acceptedLocationStoresPolicyAndNullAltitudeFlag() {
        val accepted = QualityAcceptedLocation(
            fix = RawLocationFix(
                latitude = 31.230416,
                longitude = 121.473701,
                horizontalAccuracyMeters = 18f,
                altitudeMeters = null,
                provider = "gps",
                recordedAtMillis = 1_000L,
                policyMode = LocationPolicyMode.ScheduleLowFrequency,
                scheduleLowFrequency = true,
                motionSignalName = "Still"
            ),
            altitudeMeters = null,
            acceptedAtMillis = 16_000L,
            qualityFlags = setOf("altitude-missing-timeout")
        )

        val entity = MobileLocationPointEntity.fromAccepted(accepted, rawJson = "{}")

        assertEquals("ScheduleLowFrequency", entity.policyMode)
        assertTrue(entity.scheduleLowFrequency)
        assertNull(entity.altitudeMeters)
        assertTrue(entity.qualityFlags.contains("altitude-missing-timeout"))
        assertEquals(18f, entity.accuracyMeters)
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.LocationQueueMappingTest --no-daemon
cd ..\..
```

Expected: FAIL because entity fields and `fromAccepted` are missing.

- [ ] **Step 3: Extend location entity**

Add fields to `MobileLocationPointEntity`:

```kotlin
@ColumnInfo(name = "submitted_at_utc") val submittedAtUtc: Long? = null,
@ColumnInfo(name = "policy_mode") val policyMode: String = "PowerSavingNormal",
@ColumnInfo(name = "schedule_low_frequency") val scheduleLowFrequency: Boolean = false,
@ColumnInfo(name = "motion_state") val motionState: String? = null,
@ColumnInfo(name = "quality_flags") val qualityFlags: String = "[]"
```

Add companion mapper:

```kotlin
companion object {
    fun fromAccepted(accepted: QualityAcceptedLocation, rawJson: String): MobileLocationPointEntity {
        return MobileLocationPointEntity(
            latitude = accepted.fix.latitude,
            longitude = accepted.fix.longitude,
            altitudeMeters = accepted.altitudeMeters,
            accuracyMeters = accepted.fix.horizontalAccuracyMeters,
            provider = accepted.fix.provider,
            recordedAtUtc = accepted.fix.recordedAtMillis,
            submittedAtUtc = accepted.acceptedAtMillis,
            source = "auto",
            collectedAtUtc = accepted.acceptedAtMillis,
            rawJson = rawJson,
            policyMode = accepted.fix.policyMode.name,
            scheduleLowFrequency = accepted.fix.scheduleLowFrequency,
            motionState = accepted.fix.motionSignalName,
            qualityFlags = accepted.qualityFlags.sorted().joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        )
    }
}
```

- [ ] **Step 4: Add diagnostics entities**

Add:

```kotlin
@Entity(tableName = "mobile_location_dropped_diagnostics")
data class MobileLocationDroppedDiagnosticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recorded_at_utc") val recordedAtUtc: Long,
    @ColumnInfo(name = "provider") val provider: String?,
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Float?,
    @ColumnInfo(name = "policy_mode") val policyMode: String,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "created_at_utc") val createdAtUtc: Long = System.currentTimeMillis()
)

@Entity(tableName = "mobile_location_policy_transitions")
data class MobileLocationPolicyTransitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_mode") val fromMode: String?,
    @ColumnInfo(name = "to_mode") val toMode: String,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "occurred_at_utc") val occurredAtUtc: Long
)
```

- [ ] **Step 5: Update DAO**

Add DAO methods:

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertDroppedLocationDiagnostic(diagnostic: MobileLocationDroppedDiagnosticEntity): Long

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertPolicyTransition(transition: MobileLocationPolicyTransitionEntity): Long

@Query("SELECT * FROM mobile_location_dropped_diagnostics ORDER BY recorded_at_utc DESC LIMIT :limit")
fun recentDroppedLocationDiagnostics(limit: Int = 20): Flow<List<MobileLocationDroppedDiagnosticEntity>>

@Query("SELECT * FROM mobile_location_policy_transitions ORDER BY occurred_at_utc DESC LIMIT :limit")
fun recentPolicyTransitions(limit: Int = 20): Flow<List<MobileLocationPolicyTransitionEntity>>
```

- [ ] **Step 6: Version database and preserve queues**

Update `AppDatabase` to version `3`, add both new entities, and remove `fallbackToDestructiveMigration()` from `AppModule`. Add a migration path that creates the two tables and adds nullable/new default columns to `mobile_location_points`.

- [ ] **Step 7: Run test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.LocationQueueMappingTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/data src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt src/client-android/app/src/test/java/com/pim/app/location
git commit -m "feat: persist android location queue diagnostics"
```

## Task 8: Foreground Location Service And Informative Notification

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt`

- [ ] **Step 1: Write notification renderer tests**

Create tests:

```kotlin
package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationNotificationRendererTest {
    @Test
    fun collapsedTextShowsStrategyNextAccuracyQueueAndApi() {
        val text = LocationNotificationRenderer.collapsedText(
            state = LocationNotificationState(
                mode = LocationPolicyMode.ScheduleLowFrequency,
                nextExpectedLocationText = "12 分钟后",
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                pendingUploadCount = 3,
                apiState = "正常",
                lastDroppedReason = null
            )
        )

        assertTrue(text.contains("日程低频"))
        assertTrue(text.contains("12 分钟后"))
        assertTrue(text.contains("18m"))
        assertTrue(text.contains("待上传 3"))
        assertTrue(text.contains("正常"))
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.notifications.LocationNotificationRendererTest --no-daemon
cd ..\..
```

Expected: FAIL because renderer does not exist.

- [ ] **Step 3: Implement renderer**

Renderer must produce Chinese content for:

- `省电档`
- `日程低频`
- `运动中`
- `API 无法连接`
- `待上传 N`
- last dropped reason

It must expose pure functions for tests and Android notification builder input.

- [ ] **Step 4: Implement service**

`ForegroundLocationService` must:

- Call `startForeground()` promptly with a notification.
- Request location using the interval from `LocationPolicyEngine`.
- Feed raw fixes into `LocationQualityGate`.
- Queue accepted points before upload.
- Insert dropped diagnostics for rejected fixes.
- Update notification when mode, queue, API, or last fix changes.
- Stop when user disables continuous collection.

- [ ] **Step 5: Implement controller and actions**

`ForegroundLocationController` must expose:

```kotlin
fun start()
fun stop()
fun syncNow()
fun openStatusIntent(): Intent
```

`NotificationActionReceiver` must handle:

- `ACTION_PAUSE_COLLECTION`
- `ACTION_RESUME_COLLECTION`
- `ACTION_SYNC_NOW`
- `ACTION_OPEN_STATUS`

- [ ] **Step 6: Run renderer tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.notifications.LocationNotificationRendererTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/main/java/com/pim/app/notifications src/client-android/app/src/test/java/com/pim/app/notifications
git commit -m "feat: add foreground location notification"
```

## Task 9: Motion Signals And Schedule Windows

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt`

- [ ] **Step 1: Write schedule selector test**

The selector must treat any current event with nonblank `location` as a schedule-low-frequency signal:

```kotlin
package com.pim.app.schedule

import com.pim.app.location.policy.ScheduleWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleWindowRepositoryTest {
    @Test
    fun currentWindowRequiresTimeRangeAndLocationText() {
        val now = 10_000L
        val windows = listOf(
            ScheduleWindow("1", "无地点会议", "", 9_000L, 11_000L),
            ScheduleWindow("2", "办公室", "上海市黄浦区", 9_000L, 11_000L)
        )

        assertEquals("2", ScheduleWindowSelector.current(windows, now)?.id)
        assertNull(ScheduleWindowSelector.current(windows, 12_000L))
    }
}
```

- [ ] **Step 2: Implement schedule selector**

Create pure selector:

```kotlin
object ScheduleWindowSelector {
    fun current(windows: List<ScheduleWindow>, nowMillis: Long): ScheduleWindow? {
        return windows.firstOrNull { window ->
            window.locationText.isNotBlank() &&
                nowMillis >= window.startsAtMillis &&
                nowMillis < window.endsAtMillis
        }
    }
}
```

- [ ] **Step 3: Implement repository**

`ScheduleWindowRepository` calls `ApiService.getEvents(start, end)`, filters nonblank `location`, maps to `ScheduleWindow`, and exposes current/upcoming windows to UI and policy.

- [ ] **Step 4: Implement motion mapper**

`MotionSignalRepository` uses Activity Recognition transition APIs when available. If Google Play services or permission is missing, it reports a status issue `activity-recognition-unavailable` and policy continues without motion shortening.

- [ ] **Step 5: Run tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.schedule.ScheduleWindowRepositoryTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.location.motion.MotionSignalMapperTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/location/motion src/client-android/app/src/main/java/com/pim/app/schedule src/client-android/app/src/test/java/com/pim/app/location/motion src/client-android/app/src/test/java/com/pim/app/schedule
git commit -m "feat: add schedule and motion tracking signals"
```

## Task 10: Mobile Query API Models For Today And Tracks

**Files:**
- Modify: `src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt`
- Modify: `src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt`
- Test: `src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt`

- [ ] **Step 1: Write API source contract test**

Create:

```kotlin
package com.pim.core.network

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileQueryApiContractTest {
    @Test
    fun apiServiceContainsMobileQueryEndpoints() {
        val api = repoFile("src", "main", "java", "com", "pim", "core", "network", "ApiService.kt").readText()

        assertTrue(api.contains("@GET(\"mobile/summary\")"))
        assertTrue(api.contains("@GET(\"mobile/timeline\")"))
        assertTrue(api.contains("@GET(\"mobile/quality\")"))
        assertTrue(api.contains("@GET(\"mobile/location/history\")"))
        assertTrue(api.contains("@GET(\"mobile/location/analytics/overview\")"))
        assertTrue(api.contains("@GET(\"mobile/location/analytics/tracks\")"))
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
```

- [ ] **Step 2: Run and confirm failure**

Run:

```powershell
cd src\client-android
.\gradlew.bat :core:testDebugUnitTest --tests com.pim.core.network.MobileQueryApiContractTest --no-daemon
cd ..\..
```

Expected: FAIL because query endpoints are missing from Android Retrofit interface.

- [ ] **Step 3: Add Retrofit endpoints**

Add:

```kotlin
@GET("mobile/summary")
suspend fun getMobileSummary(
    @Query("date") date: String? = null,
    @Query("deviceId") deviceId: String? = null
): ApiResponse<MobileUsageSummaryResponse>

@GET("mobile/location/analytics/overview")
suspend fun getMobileLocationOverview(
    @Query("rangeStartUtc") rangeStartUtc: String,
    @Query("rangeEndUtc") rangeEndUtc: String,
    @Query("deviceId") deviceId: String? = null,
    @Query("maxAccuracyMeters") maxAccuracyMeters: Double = 50.0
): ApiResponse<MobileLocationAnalyticsOverviewResponse>

@GET("mobile/location/analytics/tracks")
suspend fun getMobileLocationTracks(
    @Query("rangeStartUtc") rangeStartUtc: String,
    @Query("rangeEndUtc") rangeEndUtc: String,
    @Query("deviceId") deviceId: String? = null,
    @Query("maxAccuracyMeters") maxAccuracyMeters: Double = 50.0
): ApiResponse<List<MobileLocationTrackDto>>
```

- [ ] **Step 4: Add DTOs**

Mirror existing backend DTO names from `src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs` and `MobileLocationAnalyticsDtos.cs`. Include only fields used by Android UI first screen and tracks:

- `MobileUsageSummaryResponse`
- `MobileAppUsageSummaryDto`
- `MobileLocationAnalyticsOverviewResponse`
- `MobileLocationTrackDto`
- `MobileLocationSegmentDto`
- `MobileLocationPathPointDto`
- `MobileGeoBoundsDto`

- [ ] **Step 5: Implement overview repository**

`MobileOverviewRepository` combines:

- `getMobileSummary(date)`
- `getMobileLocationOverview(rangeStartUtc, rangeEndUtc)`
- `getMobileLocationTracks(rangeStartUtc, rangeEndUtc)`
- Room pending queue counts for offline display

- [ ] **Step 6: Run API contract test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :core:testDebugUnitTest --tests com.pim.core.network.MobileQueryApiContractTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt src/client-android/core/src/test/java/com/pim/core/network src/client-android/app/src/main/java/com/pim/app/mobile/summary
git commit -m "feat: add android mobile query contracts"
```

## Task 11: Native Today, Tracks, Schedule, Status, And Settings Screens

**Files:**
- Modify/Create under: `src/client-android/app/src/main/java/com/pim/app/ui/today/**`
- Modify/Create under: `src/client-android/app/src/main/java/com/pim/app/ui/tracks/**`
- Modify/Create under: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/**`
- Modify/Create under: `src/client-android/app/src/main/java/com/pim/app/ui/status/**`
- Modify/Create under: `src/client-android/app/src/main/java/com/pim/app/ui/settings/**`
- Test: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt`

- [ ] **Step 1: Write source content test**

Create `AndroidV2ScreenContentTest.kt` that asserts required visible labels exist in screen files:

```kotlin
package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2ScreenContentTest {
    @Test
    fun screensExposeApprovedInformationArchitecture() {
        assertContains("ui/today/TodayScreen.kt", listOf("今日概览", "今日轨迹", "停留", "移动距离", "手机使用"))
        assertContains("ui/tracks/TracksScreen.kt", listOf("轨迹历史", "时间范围", "质量过滤", "< 50m", "原始点"))
        assertContains("ui/schedule/SchedulePolicyScreen.kt", listOf("日程低频策略", "当前日程", "恢复阈值", "100m", "策略切换"))
        assertContains("ui/status/StatusCenterScreen.kt", listOf("状态中心", "API", "权限", "前台服务", "上传队列", "最近错误"))
        assertContains("ui/settings/SettingsScreen.kt", listOf("API 地址", "持续采集", "省电档", "3 分钟", "15 分钟", "1 分钟", "< 50m"))
    }

    private fun assertContains(path: String, labels: List<String>) {
        val file = repoFile("src", "main", "java", "com", "pim", "app", *path.split('/').toTypedArray()).readText()
        labels.forEach { label -> assertTrue("$path missing $label", file.contains(label)) }
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
```

- [ ] **Step 2: Run and confirm failure for incomplete screens**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2ScreenContentTest --no-daemon
cd ..\..
```

Expected: FAIL until all five screens contain required content.

- [ ] **Step 3: Implement Today screen**

Today screen must show:

- Collection status chip.
- Today's map-like preview panel using a clean placeholder if native map is not integrated in this task.
- Metrics: stays, movement distance, quality/completeness.
- Mobile usage summary with top apps.
- Current policy summary and next expected location time.
- Small warning linking to `状态` when unhealthy.

- [ ] **Step 4: Implement Tracks screen**

Tracks screen must show:

- Range selector: today, 7 days, 30 days.
- Quality filter default `< 50m`.
- Track/stay/segment list from `MobileOverviewRepository`.
- Selected segment details.
- Raw point section scoped to selection.

- [ ] **Step 5: Implement Schedule screen**

Schedule screen must show:

- Current schedule window with location, if any.
- Policy effect: `日程低频`, interval `15 分钟`, anchor, exit conditions.
- Upcoming schedule windows with locations.
- Recent policy transitions.
- Diagnostics for unavailable schedule data.

- [ ] **Step 6: Implement Status screen**

Status screen must show actionable rows for:

- API address missing/invalid/unreachable.
- Login missing/expired.
- Notification, foreground location, background location, usage access, activity recognition.
- Foreground service state.
- Current policy mode and next expected location.
- Last accepted location and last dropped reason.
- Upload queue, heartbeat, sync attempts, last API error, recent logs.

- [ ] **Step 7: Implement Settings screen**

Settings screen must show API first, login, connection test, continuous collection, permissions, and tracking policy values. The continuous collection switch must remain off when blocking requirements are missing.

- [ ] **Step 8: Run screen content test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2ScreenContentTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 9: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt
git commit -m "feat: build android v2 native screens"
```

## Task 12: Permission And Status Center Repositories

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt`

- [ ] **Step 1: Write status issue test**

Create:

```kotlin
package com.pim.app.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusIssueTest {
    @Test
    fun requiredIssuesHaveActionLabels() {
        val issues = StatusIssue.requiredIssueCodes()

        assertTrue(issues.contains("api-address-missing"))
        assertTrue(issues.contains("background-location-missing"))
        assertTrue(issues.contains("foreground-service-not-running"))
        assertTrue(issues.contains("location-accuracy-rejected"))
        assertTrue(issues.contains("altitude-missing-timeout"))
        assertTrue(issues.contains("upload-queue-backlog"))

        val issue = StatusIssue.apiAddressMissing()
        assertEquals("去设置", issue.actionLabel)
    }
}
```

- [ ] **Step 2: Implement issue model**

`StatusIssue` fields:

```kotlin
data class StatusIssue(
    val code: String,
    val severity: StatusSeverity,
    val title: String,
    val message: String,
    val lastOccurredAtMillis: Long?,
    val actionLabel: String,
    val target: StatusActionTarget
)
```

Add enum `StatusSeverity` and `StatusActionTarget`.

- [ ] **Step 3: Implement permissions repository**

Repository checks:

- `POST_NOTIFICATIONS`
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- usage access
- `ACTIVITY_RECOGNITION`

It must return data, not start permission requests directly.

- [ ] **Step 4: Implement status repository**

Combine:

- permission snapshot,
- API address validation,
- token state,
- foreground service status,
- tracking settings,
- queue counts,
- dropped diagnostics,
- heartbeat/sync state,
- recent logs.

- [ ] **Step 5: Run test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.status.StatusIssueTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/permissions src/client-android/app/src/main/java/com/pim/app/status src/client-android/app/src/main/java/com/pim/app/ui/status src/client-android/app/src/test/java/com/pim/app/status
git commit -m "feat: add android status center diagnostics"
```

## Task 13: Location Queue Upload And WorkManager Retry Boundary

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationSyncWorker.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/sync/LocationUploadCoordinatorTest.kt`

- [ ] **Step 1: Write upload result planner test**

Use a pure planner so partial failure behavior is testable:

```kotlin
package com.pim.app.mobile.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUploadCoordinatorTest {
    @Test
    fun partialFailureKeepsFailedRowsQueued() {
        val result = LocationUploadBatchResult(
            syncedIds = listOf(1L, 2L),
            failedIds = listOf(3L),
            errorMessage = "timeout"
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(listOf(1L, 2L), updates.syncedIds)
        assertEquals(listOf(3L), updates.failedIds)
        assertEquals("timeout", updates.failedReason)
    }
}
```

- [ ] **Step 2: Implement coordinator**

`LocationUploadCoordinator` must:

- Read pending accepted location rows from Room.
- Convert rows to `MobileLocationPointRequest`.
- Upload in small batches or sequentially.
- Mark successful rows `SYNCED`.
- Mark failed rows `FAILED` with error while keeping them eligible for retry.
- Never upload dropped diagnostics as accepted points.

- [ ] **Step 3: Implement worker**

`LocationSyncWorker`:

- Requires network.
- Calls `LocationUploadCoordinator.uploadPending()`.
- Returns `Result.retry()` for transient API/network failures.
- Does not schedule minute-level location collection.

- [ ] **Step 4: Wire manual sync**

`MobileSyncCoordinator.syncOnOpen()` and status `同步现在` action call location upload after usage sync preparation, preserving existing usage behavior.

- [ ] **Step 5: Run test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.mobile.sync.LocationUploadCoordinatorTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app/mobile/sync src/client-android/app/src/main/java/com/pim/app/di/PimWorkerFactory.kt src/client-android/app/src/test/java/com/pim/app/mobile/sync
git commit -m "feat: sync queued android locations"
```

## Task 14: Backend Strict Accuracy And Null Altitude Contract

**Files:**
- Modify: `src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs`
- Modify: `tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs`

- [ ] **Step 1: Change backend tests first**

Update tests so:

```csharp
[Fact]
public async Task SubmitAsync_AcceptsAccuracyBelowFiftyMeters()
{
    await using var db = MobileTestHelpers.CreateDb();
    var service = new MobileLocationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-06T12:00:00Z")));

    var point = await service.SubmitAsync(Request(49.9), CancellationToken.None);

    Assert.Equal("usable", point.Quality);
}

[Fact]
public async Task SubmitAsync_RejectsFiftyMeterAccuracy()
{
    await using var db = MobileTestHelpers.CreateDb();
    var service = new MobileLocationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-06T12:00:00Z")));

    var error = await Assert.ThrowsAsync<DomainException>(() => service.SubmitAsync(Request(50), CancellationToken.None));

    Assert.Equal(6202, error.ErrorCode);
}

[Fact]
public async Task SubmitAsync_AcceptsNullAltitudeWithQualityFlagInRawJson()
{
    await using var db = MobileTestHelpers.CreateDb();
    var service = new MobileLocationService(db, MobileTestHelpers.CurrentUser(), MobileTestHelpers.Time(DateTimeOffset.Parse("2026-07-06T12:00:00Z")));

    var point = await service.SubmitAsync(Request(18) with
    {
        AltitudeMeters = null,
        RawJson = "{\"qualityFlags\":[\"altitude-missing-timeout\"]}"
    }, CancellationToken.None);

    Assert.Null(point.AltitudeMeters);
    Assert.Contains("altitude-missing-timeout", point.RawJson);
}
```

- [ ] **Step 2: Run backend tests and confirm failure**

Run:

```powershell
dotnet test Pim.sln --filter "FullyQualifiedName~MobileLocationServiceTests"
```

Expected: FAIL because service still accepts exactly `50`.

- [ ] **Step 3: Update backend service**

Change:

```csharp
if (request.HorizontalAccuracyMeters > MaxUsableAccuracyMeters)
```

to:

```csharp
if (request.HorizontalAccuracyMeters >= MaxUsableAccuracyMeters)
```

Keep `AltitudeMeters` nullable and store `RawJson` unchanged.

- [ ] **Step 4: Run backend tests**

Run:

```powershell
dotnet test Pim.sln --filter "FullyQualifiedName~MobileLocationServiceTests"
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs
git commit -m "fix: enforce strict mobile location accuracy"
```

## Task 15: Remove Mojibake From Active Android UI

**Files:**
- Modify active UI files under: `src/client-android/app/src/main/java/com/pim/app/ui/**`
- Modify active tracking/status files under: `src/client-android/app/src/main/java/com/pim/app/**`
- Test: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt`

- [ ] **Step 1: Write encoding source test**

Create:

```kotlin
package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class AndroidV2TextEncodingTest {
    @Test
    fun activeAndroidV2SourcesDoNotContainMojibakeMarkers() {
        val roots = listOf(
            repoFile("src", "main", "java", "com", "pim", "app", "ui"),
            repoFile("src", "main", "java", "com", "pim", "app", "location"),
            repoFile("src", "main", "java", "com", "pim", "app", "status")
        )
        val markers = listOf("绔", "鐘舵", "浠婃", "璁剧", "鎸佺", "鏉冮")
        val offenders = roots
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .filter { file -> markers.any { marker -> file.readText().contains(marker) } }
            .map { it.path }

        assertFalse("Mojibake markers found in active v2 sources: $offenders", offenders.isNotEmpty())
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
```

- [ ] **Step 2: Run and confirm failure if active files still contain mojibake**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2TextEncodingTest --no-daemon
cd ..\..
```

Expected: PASS only after active v2 source files use readable Chinese.

- [ ] **Step 3: Replace active strings**

Replace active UI/status strings with readable Chinese. Do not spend time rewriting unused legacy shell strings unless tests or active launcher paths still include those files.

- [ ] **Step 4: Run test**

Run:

```powershell
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests com.pim.app.v2.AndroidV2TextEncodingTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/client-android/app/src/main/java/com/pim/app src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt
git commit -m "fix: restore readable android v2 text"
```

## Task 16: Integration Build And Manual Device Script

**Files:**
- Create: `docs/superpowers/reports/2026-07-08-android-app-v2-manual-verification.md`

- [ ] **Step 1: Run Android unit tests**

Run:

```powershell
cd src\client-android
.\gradlew.bat testDebugUnitTest --no-daemon
cd ..\..
```

Expected: PASS.

- [ ] **Step 2: Run Android build**

Run:

```powershell
cd src\client-android
.\gradlew.bat assembleDebug --no-daemon
cd ..\..
```

Expected: PASS and debug APK generated under `src/client-android/app/build/outputs/apk/debug/`. Do not commit `build/`.

- [ ] **Step 3: Run backend tests**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 4: Run web build if backend DTO changes affected web**

Run only if shared mobile DTOs or response shapes changed:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 5: Write manual verification report**

Create `docs/superpowers/reports/2026-07-08-android-app-v2-manual-verification.md` with these checked or documented items:

```markdown
# Android App v2 Manual Verification

- [ ] Fresh install opens native five-tab UI.
- [ ] Settings presents API address before collection controls.
- [ ] Public server IP/domain can be saved.
- [ ] Login succeeds against configured API.
- [ ] Continuous collection remains off until manually enabled.
- [ ] Notification permission flow is visible.
- [ ] Foreground precise location permission flow is visible.
- [ ] Background location permission flow is visible.
- [ ] Persistent notification shows strategy, next location, recent accuracy, queue, and API/sync state.
- [ ] Status tab shows API, permissions, foreground service, queue, heartbeat, and recent errors.
- [ ] Schedule window with location enters low-frequency mode.
- [ ] Movement over 100m exits schedule low-frequency mode.
- [ ] Accuracy `49.9m` queues accepted point.
- [ ] Accuracy `50.0m` is dropped locally and not uploaded.
- [ ] Missing altitude after 15s uploads null altitude with `altitude-missing-timeout`.
- [ ] API outage keeps local queue and shows status error.
- [ ] API recovery uploads queued accepted points.
```

For items not manually verified, write the exact reason and matching automated test.

- [ ] **Step 6: Commit report**

Run:

```powershell
git add docs/superpowers/reports/2026-07-08-android-app-v2-manual-verification.md
git commit -m "docs: record android v2 verification"
```

## Task 17: Final Local Verification, PR, And GitHub Actions

**Files:**
- No source changes unless verification finds a defect.

- [ ] **Step 1: Re-run full verification**

Run:

```powershell
git status --short --branch
cd src\client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat assembleDebug --no-daemon
cd ..\..
dotnet test Pim.sln
```

If web was touched:

```powershell
npm --prefix src/client-web run build
```

Expected: all relevant commands PASS.

- [ ] **Step 2: Confirm generated outputs are not staged**

Run:

```powershell
git status --short
```

Expected: no `build/`, `bin/`, `obj/`, `dist/`, `.superpowers/brainstorm/`, npm cache, or API `wwwroot` generated output in tracked changes.

- [ ] **Step 3: Push branch**

Run:

```powershell
git push -u origin codex/android-app-v2-redesign
```

Expected: push succeeds.

- [ ] **Step 4: Create PR**

Run:

```powershell
gh pr create --base master --head codex/android-app-v2-redesign --title "Android app v2 complete redesign" --body-file docs/superpowers/reports/2026-07-08-android-app-v2-manual-verification.md
```

Expected: GitHub returns a PR URL.

- [ ] **Step 5: Wait for GitHub Actions**

Run:

```powershell
gh pr checks --watch
```

Expected: relevant Android and backend checks pass. If a check fails, open the failing log with:

```powershell
gh run view --log-failed
```

Investigate, fix, commit, push, and wait again. The branch is not complete with red checks unless the failure is proven unrelated and documented with exact workflow/job/log details.

## Coverage Matrix

- Final goal 1, five native tabs: Tasks 1, 2, 11.
- Final goal 2, API address input and no real-device `127.0.0.1` default: Task 3.
- Final goal 3, manual `持续采集`: Task 4.
- Final goal 4, real foreground service notification: Task 8.
- Final goal 5, background location permission: Tasks 1, 12.
- Final goal 6, configurable power-saving policy: Task 4.
- Final goal 7, schedule-aware low frequency: Tasks 5, 9, 11.
- Final goal 8, motion-aware recovery: Tasks 5, 9.
- Final goal 9, strict `< 50m` upload gate: Tasks 6, 14.
- Final goal 10, 15s altitude wait and null altitude flag: Tasks 6, 14.
- Final goal 11, local persistence and upload queues: Tasks 7, 13.
- Final goal 12, status center: Tasks 11, 12.
- Final goal 13, tests/manual verification/build: Tasks 1 through 17.
- Required branch/commits/PR/GA/subagents: Tasks 0, Parallel Subagent Assignment, Task 17.

## Platform References

- Android foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android background location guidance: https://developer.android.com/develop/sensors-and-location/location/background
- Android activity recognition transitions: https://developer.android.com/develop/sensors-and-location/location/transitions
- Android notification runtime permission: https://developer.android.com/develop/ui/views/notifications/notification-permission
- Android WorkManager periodic work: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- `PeriodicWorkRequest` reference: https://developer.android.com/reference/androidx/work/PeriodicWorkRequest

## Self-Review

- Spec coverage: every final goal in the design spec maps to at least one task in the Coverage Matrix.
- Visual companion coverage: Today emphasis, five-tab navigation, light map-tool style, UI skeleton, location state machine, architecture/dataflow, permissions/errors/testing all map to Tasks 1 through 13 and Task 16.
- Type consistency: shared names are `TrackingPolicy`, `LocationPolicyMode`, `PolicyDecision`, `ScheduleWindow`, `MotionSignal`, `AcceptedLocation`, and `DroppedLocationDiagnostic`; later tasks reuse these names.
- Delivery discipline: branch creation, subagent concurrency, frequent commits, push, PR creation, and GitHub Actions wait are explicit.
- Placeholder scan: plan avoids undefined deferred work; every task has concrete files, commands, expected results, and success criteria.
