# Android Location Live Updates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 在每次手动或后台自动定位的实际采集窗口内发布 Android 36.1 Live Update，并新增可手动定位、确认提交和查看权威上传计数的原生定位页面；API 36.1 以下保持无第二通知。

**Architecture:** 单一 LocationAcquisitionEngine 持有 Fused Location callback，LocationAcquisitionCoordinator 作为进程级权威状态机统一手动与自动 session。ForegroundLocationService 只负责策略调度和 location FGS 生命周期，LocationLiveUpdatePublisher 只观察 Acquiring 状态，Compose UI 只消费 Coordinator 与 Room 队列 Flow。

**Tech Stack:** Kotlin 1.9.25、Coroutines/StateFlow、Hilt、Google Play services location 21.3.0、Room 2.6.1、Compose Material 3、Android Notification.Builder API 36.1、Robolectric/JUnit4、Compose instrumentation tests。

---

## Execution Topology

- 第一阶段串行完成 Task 1 和 Task 2，固定构建环境、公共数据类型、接口和队列来源。
- 第二阶段由 Core worker 串行完成 Task 3 和 Task 4，并由根代理合入，因为后续所有消费者都依赖 Coordinator 的最终签名。
- 第三阶段完成 Task 5，固定 ForegroundLocationController 的手动/取消入口和服务生命周期。
- 第四阶段从 Task 5 的同一提交创建隔离 worktree，并行执行：
  - Live Update worker：Task 6。
  - UI worker：Task 7。
  - Read-only review worker：交叉核对 API 36.1、通知隐私、六项导航和测试覆盖，不写文件。
- 主工作树在并行阶段不写入 Task 6/7 文件；每个写 worker 做独立提交，根代理审查后 cherry-pick。
- Task 8 只在所有实现提交合入后执行。
- 不允许两个写代理同时修改同一 worktree。创建隔离 worktree 时必须使用 superpowers:using-git-worktrees。

## File Map

### New Production Files

- src/client-android/app/src/main/java/com/pim/app/location/LocationSnapshot.kt
  - 统一页面、Engine、Coordinator 和旧 facade 使用的位置快照。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModels.kt
  - TriggerType、AcquisitionPhase、LocationAcquisitionState、session request/result。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationUpdateSource.kt
  - Fused callback 的可替换适配器。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionEngine.kt
  - 单轮最长 30 秒采集、最佳候选、超时和解绑。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationPrerequisiteChecker.kt
  - 手动/自动权限、定位开关和 Google Play services 检查。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinator.kt
  - 统一状态机、互斥、质量门、手动确认和自动入队。
- src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModule.kt
  - Hilt interface bindings。
- src/client-android/app/src/main/java/com/pim/app/status/QueueStatusRepository.kt
  - 六类上传队列的唯一 Room Flow 来源。
- src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateCapability.kt
  - API 36.0/36.1 安全能力检测。
- src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRenderer.kt
  - 不含坐标的平台 Notification.Builder 实现。
- src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisher.kt
  - 状态订阅、节流、session 抑制与撤销。
- src/client-android/app/src/main/java/com/pim/app/ui/location/LocationUiState.kt
  - 中文展示模型和按钮可用性。
- src/client-android/app/src/main/java/com/pim/app/ui/location/LocationViewModel.kt
  - Coordinator、Controller 与 QueueStatusRepository 接线。
- src/client-android/app/src/main/java/com/pim/app/ui/location/LocationScreen.kt
  - 四区定位页面。

### New Test Files

- src/client-android/app/src/test/java/com/pim/app/v2/AndroidLiveUpdateBuildContractTest.kt
- src/client-android/app/src/test/java/com/pim/app/status/QueueStatusRepositoryTest.kt
- src/client-android/app/src/test/java/com/pim/app/location/acquisition/LocationAcquisitionEngineTest.kt
- src/client-android/app/src/test/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinatorTest.kt
- src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdateCapabilityTest.kt
- src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisherTest.kt
- src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRendererTest.kt
- src/client-android/app/src/test/java/com/pim/app/ui/location/LocationViewModelTest.kt
- src/client-android/app/src/androidTest/java/com/pim/app/location/liveupdate/LocationLiveUpdatePlatformTest.kt
- src/client-android/app/src/androidTest/java/com/pim/app/ui/location/LocationScreenTest.kt
- src/client-android/app/src/androidTest/java/com/pim/app/ui/root/PimRootScreenNavTest.kt
- src/client-android/app/src/androidTest/java/com/pim/app/ui/today/TodayScreenTest.kt

### Main Modified Files

- src/client-android/build.gradle.kts
- src/client-android/app/build.gradle.kts
- src/client-android/app/src/main/AndroidManifest.xml
- .github/workflows/build-android.yml
- src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt
- src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt
- src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt
- src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt
- src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt
- src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt
- src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt
- src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt
- src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt
- src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt
- src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt
- src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt
- src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt
- src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt
- src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt
- src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt
- src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt
- src/client-android/app/src/main/java/com/pim/app/MainActivity.kt
- src/client-android/app/src/main/java/com/pim/app/PimApp.kt

## Task 1: Upgrade The Compile Surface And Declare Promotion Permission

**Files:**
- Create: src/client-android/app/src/test/java/com/pim/app/v2/AndroidLiveUpdateBuildContractTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/manifest/PermissionManifestTest.kt
- Modify: src/client-android/build.gradle.kts
- Modify: src/client-android/app/build.gradle.kts
- Modify: src/client-android/app/src/main/AndroidManifest.xml
- Modify: .github/workflows/build-android.yml

The workflow edit is unavoidable: it currently installs only android-34, while app compilation will require the android-36.1 platform. Do not modify any other workflow.

- [ ] **Step 1: Write failing build and manifest contract tests**

~~~kotlin
@Test
fun androidBuildUsesFullSdk36Point1WithoutChangingTargetSdk() {
    val root = repoFile("..", "build.gradle.kts").readText()
    val app = repoFile("build.gradle.kts").readText()
    assertTrue(root.contains("version \"8.13.2\""))
    assertTrue(app.contains("compileSdk = 36"))
    assertTrue(app.contains("compileSdkMinor = 1"))
    assertTrue(app.contains("targetSdk = 34"))
    assertTrue(app.contains("minSdk = 26"))
}

@Test
fun androidWorkflowInstallsFullSdk36Point1() {
    val workflow = repositoryRoot()
        .resolve(".github/workflows/build-android.yml")
        .readText()
    assertTrue(workflow.contains("platforms;android-36.1"))
    assertTrue(workflow.contains("build-tools;36.1.0"))
}
~~~

Add this assertion to PermissionManifestTest:

~~~kotlin
assertTrue(
    requestedPermissions.contains("android.permission.POST_PROMOTED_NOTIFICATIONS")
)
~~~

- [ ] **Step 2: Run the tests and confirm they fail**

Run from src/client-android:

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*AndroidLiveUpdateBuildContractTest" --tests "*PermissionManifestTest" --no-daemon
~~~

Expected: FAIL because AGP is 8.4.0, app compile SDK is 34, the manifest permission is absent, and CI installs android-34.

- [ ] **Step 3: Apply the exact build changes**

Use this Android block:

~~~kotlin
android {
    namespace = "com.pim.app"
    compileSdk = 36
    compileSdkMinor = 1
    defaultConfig {
        applicationId = "com.pim.app"
        minSdk = 26
        targetSdk = 34
    }
}
~~~

Set the root plugin version:

~~~kotlin
id("com.android.application") version "8.13.2" apply false
~~~

Add to the manifest beside POST_NOTIFICATIONS:

~~~xml
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
~~~

Change only the SDK install check in build-android.yml:

~~~yaml
- name: Install Android SDK packages (if missing)
  run: |
    if [ ! -d "$ANDROID_HOME/platforms/android-36.1" ]; then
      sdkmanager "platforms;android-36.1" "build-tools;36.1.0"
    fi
~~~

Keep Gradle 8.14, Kotlin 1.9.25, targetSdk 34, minSdk 26, AndroidX Core, Compose and Material versions unchanged.

- [ ] **Step 4: Run the targeted tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 5: Commit**

~~~powershell
git add .github/workflows/build-android.yml src/client-android/build.gradle.kts src/client-android/app/build.gradle.kts src/client-android/app/src/main/AndroidManifest.xml src/client-android/app/src/test/java/com/pim/app/manifest/PermissionManifestTest.kt src/client-android/app/src/test/java/com/pim/app/v2/AndroidLiveUpdateBuildContractTest.kt
git commit -m "build: enable Android 36.1 live update APIs"
~~~

## Task 2: Define Shared Session And Queue Contracts

**Files:**
- Create: src/client-android/app/src/main/java/com/pim/app/location/LocationSnapshot.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModels.kt
- Create: src/client-android/app/src/main/java/com/pim/app/status/QueueStatusRepository.kt
- Create: src/client-android/app/src/test/java/com/pim/app/status/QueueStatusRepositoryTest.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt

- [ ] **Step 1: Write failing source and queue tests**

Add manual/automatic source assertions:

~~~kotlin
@Test
fun acceptedLocationPreservesManualSource() {
    val entity = MobileLocationPointEntity.fromAccepted(
        accepted = acceptedLocation(),
        rawJson = "{}",
        source = "manual"
    )
    assertEquals("manual", entity.source)
}

@Test
fun acceptedLocationPreservesAutomaticSource() {
    val entity = MobileLocationPointEntity.fromAccepted(
        accepted = acceptedLocation(),
        rawJson = "{}",
        source = "auto"
    )
    assertEquals("auto", entity.source)
}
~~~

QueueStatusRepositoryTest must construct six MutableStateFlow values and assert both fields:

~~~kotlin
@Test
fun queueStatusCombinesAllSixQueuesAndKeepsLocationSeparate() = runTest {
    val repository = QueueStatusRepository(
        locations = MutableStateFlow(3),
        usageEvents = MutableStateFlow(4),
        usageSummaries = MutableStateFlow(5),
        appMetadata = MutableStateFlow(6),
        deviceProfiles = MutableStateFlow(7),
        syncBatches = MutableStateFlow(8)
    )
    val snapshot = repository.observe().first()
    assertEquals(3, snapshot.pendingLocationPoints)
    assertEquals(33, snapshot.pendingUploadTotal)
}
~~~

- [ ] **Step 2: Run the tests and confirm they fail**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationQueueMappingTest" --tests "*QueueStatusRepositoryTest" --no-daemon
~~~

Expected: FAIL because source is hardcoded to auto and QueueStatusRepository does not exist.

- [ ] **Step 3: Add the shared acquisition models**

LocationSnapshot.kt:

~~~kotlin
data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val provider: String,
    val source: String,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val timeMillis: Long
)
~~~

LocationAcquisitionModels.kt:

~~~kotlin
enum class TriggerType(val storageSource: String) {
    MANUAL("manual"),
    AUTOMATIC("auto")
}

enum class AcquisitionPhase {
    Idle,
    Preparing,
    Acquiring,
    Evaluating,
    AwaitingManualSubmit,
    Enqueuing,
    Completed,
    TimedOut,
    Failed,
    Cancelled
}

data class AutomaticSessionContext(
    val priority: Int,
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionSignal: String
)

data class LocationAcquisitionState(
    val sessionId: String? = null,
    val triggerType: TriggerType? = null,
    val phase: AcquisitionPhase = AcquisitionPhase.Idle,
    val bestLocation: LocationSnapshot? = null,
    val startedAtElapsedRealtimeMs: Long? = null,
    val deadlineAtElapsedRealtimeMs: Long? = null,
    val elapsedMs: Long = 0L,
    val maxUploadAccuracyMetersExclusive: Float = 50f,
    val errorReason: String? = null
) {
    val isBusy: Boolean
        get() = phase in setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating,
            AcquisitionPhase.AwaitingManualSubmit,
            AcquisitionPhase.Enqueuing
        )
}

sealed interface SessionStartResult {
    data class Started(val sessionId: String) : SessionStartResult
    data object Busy : SessionStartResult
    data class Rejected(val reason: String) : SessionStartResult
}
~~~

- [ ] **Step 4: Implement the shared queue Flow**

QueueStatusRepository exposes the same QueueStatusSnapshot used by StatusCenter:

~~~kotlin
@Singleton
class QueueStatusRepository internal constructor(
    private val locations: Flow<Int>,
    private val usageEvents: Flow<Int>,
    private val usageSummaries: Flow<Int>,
    private val appMetadata: Flow<Int>,
    private val deviceProfiles: Flow<Int>,
    private val syncBatches: Flow<Int>
) {
    @Inject
    constructor(dao: MobileDataDao) : this(
        dao.pendingLocationPointCount(),
        dao.pendingUsageEventCount(),
        dao.pendingUsageSummaryCount(),
        dao.pendingAppMetadataCount(),
        dao.pendingDeviceProfileCount(),
        dao.pendingSyncBatchCount()
    )

    fun observe(): Flow<QueueStatusSnapshot> = combine(
        combine(locations, usageEvents, usageSummaries, ::Triple),
        combine(appMetadata, deviceProfiles, syncBatches, ::Triple)
    ) { first, second ->
        QueueStatusSnapshot(
            pendingLocationPoints = first.first,
            pendingUsageEvents = first.second,
            pendingUsageSummaries = first.third,
            pendingAppMetadata = second.first,
            pendingDeviceProfile = second.second,
            pendingSyncBatches = second.third
        )
    }
}
~~~

Inject QueueStatusRepository into StatusCenterRepository and replace its private queueSnapshotFlow implementation with queueStatusRepository.observe().

Change entity and repository signatures:

~~~kotlin
fun fromAccepted(
    accepted: QualityAcceptedLocation,
    rawJson: String,
    source: String = "auto"
): MobileLocationPointEntity

suspend fun enqueueAccepted(
    accepted: QualityAcceptedLocation,
    rawJson: String,
    source: String = "auto"
): Long
~~~

- [ ] **Step 5: Run the targeted tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 6: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/location/LocationSnapshot.kt src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModels.kt src/client-android/app/src/main/java/com/pim/app/status/QueueStatusRepository.kt src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt src/client-android/app/src/test/java/com/pim/app/status/QueueStatusRepositoryTest.kt src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt
git commit -m "refactor: define location session and queue contracts"
~~~

## Task 3: Build The Bounded Acquisition Engine

**Files:**
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationUpdateSource.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionEngine.kt
- Create: src/client-android/app/src/test/java/com/pim/app/location/acquisition/LocationAcquisitionEngineTest.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt

- [ ] **Step 1: Write failing Engine and altitude-cap tests**

Cover these exact cases:

- timeout with no candidate returns TimedOut and closes the source Flow;
- cancellation closes the source Flow immediately;
- candidates older than startedAtWallClockMillis are ignored;
- finite lower horizontal accuracy wins; equal accuracy chooses newer timeMillis;
- a missing-accuracy candidate is kept only when no valid candidate exists;
- a late candidate emitted after collection closes cannot change the result;
- altitude wait uses min(original altitude deadline, session wall-clock deadline).

Representative cap test:

~~~kotlin
@Test
fun altitudeWaitNeverRunsPastSessionDeadline() = runTest {
    var accepted: QualityAcceptedLocation? = null
    val coordinator = AltitudeWaitCoordinator(
        gate = LocationQualityGate(50f, 15_000L),
        nowMillis = { testScheduler.currentTime },
        delayMillis = { delay(it) }
    )
    coordinator.handleFix(
        fix = fix(recordedAtMillis = 0L, altitudeMeters = null),
        deadlineCapMillis = 5_000L,
        onAccepted = { accepted = it },
        onDropped = { _, _ -> error("unexpected drop") }
    )
    assertEquals(5_000L, testScheduler.currentTime)
    assertEquals(setOf("altitude-missing-timeout"), accepted?.qualityFlags)
}
~~~

- [ ] **Step 2: Run tests and confirm failure**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationAcquisitionEngineTest" --tests "*AltitudeWaitCoordinatorTest" --no-daemon
~~~

Expected: FAIL because the new Engine/source types and deadlineCapMillis do not exist.

- [ ] **Step 3: Implement a Flow-based Fused adapter**

Use these contracts:

~~~kotlin
data class LocationUpdateRequest(
    val priority: Int,
    val durationMillis: Long,
    val intervalMillis: Long = 1_000L,
    val minUpdateIntervalMillis: Long = 800L
)

sealed interface LocationUpdateEvent {
    data class Candidate(val location: LocationSnapshot) : LocationUpdateEvent
    data class Availability(val available: Boolean) : LocationUpdateEvent
}

interface LocationUpdateSource {
    fun updates(request: LocationUpdateRequest): Flow<LocationUpdateEvent>
}
~~~

FusedLocationUpdateSource must use callbackFlow, emit every item in LocationResult.locations, close with the request failure cause, and removeLocationUpdates inside awaitClose. Do not call lastLocation and do not keep a callback outside the Flow lifetime.

- [ ] **Step 4: Implement the bounded Engine**

~~~kotlin
sealed interface LocationEngineCompletion {
    data object TimedOut : LocationEngineCompletion
    data class Failed(val reason: String) : LocationEngineCompletion
}

data class LocationEngineRequest(
    val sessionId: String,
    val priority: Int,
    val timeoutMillis: Long,
    val startedAtWallClockMillis: Long
)

data class LocationEngineResult(
    val sessionId: String,
    val bestLocation: LocationSnapshot?,
    val completion: LocationEngineCompletion
)

interface LocationAcquisitionRunner {
    suspend fun acquire(
        request: LocationEngineRequest,
        onCandidate: suspend (LocationSnapshot) -> Unit,
        onAvailabilityChanged: suspend (Boolean) -> Unit = {}
    ): LocationEngineResult
}
~~~

LocationAcquisitionEngine must:

1. collect source.updates inside withTimeoutOrNull(request.timeoutMillis);
2. ignore Candidate when timeMillis is less than startedAtWallClockMillis;
3. update best before invoking onCandidate;
4. invoke onAvailabilityChanged for Availability events so Coordinator can show a recoverable status without ending the round;
5. return TimedOut when the timeout expires;
6. return Failed for ordinary source exceptions;
7. rethrow CancellationException so awaitClose removes the callback.

- [ ] **Step 5: Cap AltitudeWaitCoordinator**

Change handleFix to accept deadlineCapMillis:

~~~kotlin
suspend fun handleFix(
    fix: RawLocationFix,
    deadlineCapMillis: Long? = null,
    onAccepted: suspend (QualityAcceptedLocation) -> Unit,
    onDropped: suspend (RawLocationFix, String) -> Unit
)
~~~

When the gate returns WaitForAltitude, replace the pending deadline with min(pending.deadlineMillis, deadlineCapMillis) before waiting. Existing callers may omit the argument.

- [ ] **Step 6: Run tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 7: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/location/acquisition src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt src/client-android/app/src/test/java/com/pim/app/location/acquisition/LocationAcquisitionEngineTest.kt src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt
git commit -m "feat: add bounded location acquisition engine"
~~~

## Task 4: Implement The Coordinator And Retire The Second State Machine

**Files:**
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationPrerequisiteChecker.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinator.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModule.kt
- Create: src/client-android/app/src/test/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinatorTest.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/LocationCaptureRepositoryTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt

- [ ] **Step 1: Write failing Coordinator tests**

The test fixture must inject a TestScope, deterministic UUIDs/clocks, a fake LocationAcquisitionRunner, prerequisite result, enqueue lambda, dropped-diagnostic lambda and sync lambda.

Required tests:

- manual success reaches AwaitingManualSubmit and enqueue count remains zero;
- submitManualResult enters Enqueuing and then Completed;
- manual enqueue failure returns AwaitingManualSubmit with the same bestLocation;
- automatic success enqueues exactly once with source auto and schedules sync once;
- automatic enqueue failure reaches Failed and is not retried;
- low-quality manual result reaches Failed and remains visible but unsubmitable;
- low-quality automatic result records a dropped diagnostic;
- no candidate reaches TimedOut;
- precheck failure never invokes Engine;
- manual busy makes automatic return Busy;
- automatic busy makes manual return Busy;
- cancel with matching session ID reaches Cancelled;
- cancel with stale session ID is ignored;
- late Engine callbacks with an old session ID are ignored;
- manual restart may replace only a manual AwaitingManualSubmit state.

Representative manual test:

~~~kotlin
@Test
fun manualCandidateWaitsForExplicitSubmit() = runTest {
    val fixture = coordinatorFixture(triggerCandidate = accurateLocation())
    val started = fixture.coordinator.startManualSession()
    assertTrue(started is SessionStartResult.Started)
    advanceUntilIdle()
    assertEquals(AcquisitionPhase.AwaitingManualSubmit, fixture.coordinator.state.value.phase)
    assertEquals(0, fixture.enqueueCalls)

    fixture.coordinator.submitManualResult()
    advanceUntilIdle()
    assertEquals(AcquisitionPhase.Completed, fixture.coordinator.state.value.phase)
    assertEquals(1, fixture.enqueueCalls)
    assertEquals("manual", fixture.lastSource)
}
~~~

- [ ] **Step 2: Run tests and confirm failure**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationAcquisitionCoordinatorTest" --no-daemon
~~~

Expected: FAIL because Coordinator and prerequisite checker do not exist.

- [ ] **Step 3: Implement prerequisite checking**

Use a typed failure:

~~~kotlin
sealed interface LocationPrerequisiteResult {
    data object Ready : LocationPrerequisiteResult
    data class Blocked(val reason: String) : LocationPrerequisiteResult
}

interface LocationPrerequisiteChecker {
    fun check(triggerType: TriggerType): LocationPrerequisiteResult
}
~~~

AndroidLocationPrerequisiteChecker rules:

- both triggers require ACCESS_FINE_LOCATION;
- automatic additionally requires ACCESS_BACKGROUND_LOCATION on API 29+;
- both require enabled system location and available Google Play services;
- manual does not read or change continuousCollectionEnabled.

- [ ] **Step 4: Implement Coordinator state transitions**

Public API:

~~~kotlin
@Singleton
class LocationAcquisitionCoordinator {
    val state: StateFlow<LocationAcquisitionState>

    fun startManualSession(replaceAwaitingManual: Boolean = false): SessionStartResult

    fun startAutomaticSession(
        context: AutomaticSessionContext
    ): SessionStartResult

    fun cancelCurrentSession(expectedSessionId: String? = null)

    fun submitManualResult()
}
~~~

Implementation requirements:

1. Generate a UUID before Preparing and store wall/elapsed start plus a 30_000 ms deadline.
2. Keep one session Job and reject conflicting starts.
3. Start a one-second elapsed ticker only while the session is active.
4. Run Engine and AltitudeWaitCoordinator concurrently; a quality acceptance cancels the Engine Job and therefore removes the Fused callback.
5. Best candidate is always exposed in state, even when the final phase is Failed.
6. Convert snapshot to RawLocationFix using AutomaticSessionContext for automatic sessions and PowerSavingNormal/Unknown defaults for manual sessions.
7. Automatic acceptance calls enqueueAccepted with source auto, then MobileSyncScheduler.enqueueNow.
8. Manual acceptance stores the accepted result privately and enters AwaitingManualSubmit; only submitManualResult enqueues with source manual.
9. Automatic dropped results call LocationQueueRepository.recordDropped.
10. Any state write checks the current session ID.
11. Manual enqueue failure retains the accepted result; automatic enqueue failure does not retry.

Bind interfaces:

~~~kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationAcquisitionModule {
    @Binds
    abstract fun bindLocationUpdateSource(
        implementation: FusedLocationUpdateSource
    ): LocationUpdateSource

    @Binds
    abstract fun bindLocationAcquisitionRunner(
        implementation: LocationAcquisitionEngine
    ): LocationAcquisitionRunner

    @Binds
    abstract fun bindLocationPrerequisiteChecker(
        implementation: AndroidLocationPrerequisiteChecker
    ): LocationPrerequisiteChecker
}
~~~

- [ ] **Step 5: Convert LocationCaptureRepository into a compatibility facade**

It must no longer import FusedLocationProviderClient, LocationCallback, LocationServices or Looper.

~~~kotlin
@Singleton
class LocationCaptureRepository @Inject constructor(
    private val coordinator: LocationAcquisitionCoordinator,
    private val controller: ForegroundLocationController
) {
    val state: StateFlow<LocationCaptureState> = coordinator.state
        .map(::toLegacyLocationCaptureState)
        .stateIn(
            CoroutineScope(SupervisorJob() + Dispatchers.Default),
            SharingStarted.Eagerly,
            LocationCaptureState()
        )

    fun startCapture() = controller.startManualSession()
    fun stopCapture() = controller.cancelLocationSession()
    fun submitCurrentLocationManually() = coordinator.submitManualResult()
}
~~~

Change LocationSubmissionPolicy so shouldAutoSubmit is always false. Keep manual accuracy validation for the dead compatibility UI.

In PimAppScaffold, keep MobileStatusViewModel and login/server code because static contract tests read them. Remove only independent location callback ownership and remove LocationCaptureViewModel.onCleared calling stopCapture; page lifecycle must not cancel a process session.

- [ ] **Step 6: Run targeted tests**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationAcquisitionCoordinatorTest" --tests "*LocationCaptureRepositoryTest" --tests "*LocationSubmissionPolicyTest" --no-daemon
~~~

Expected: PASS, including an assertion that an accurate manual location never auto-submits.

- [ ] **Step 7: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/location/acquisition src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt src/client-android/app/src/test/java/com/pim/app/location
git commit -m "feat: coordinate manual and automatic location sessions"
~~~

## Task 5: Convert ForegroundLocationService To Discrete Automatic Rounds

**Files:**
- Modify: src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/service/ForegroundLocationControllerTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/location/service/ForegroundLocationServiceTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt

- [ ] **Step 1: Write failing controller and service tests**

Controller tests:

~~~kotlin
@Test
fun startManualSessionUsesForegroundServiceWithoutChangingCollectionSetting() {
    controller.startManualSession()
    val intent = shadowOf(context).nextStartedService
    assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent.action)
}

@Test
fun cancelLocationSessionIncludesExpectedSessionId() {
    controller.cancelLocationSession("session-1")
    val intent = shadowOf(context).nextStartedService
    assertEquals(ForegroundLocationController.ACTION_CANCEL_LOCATION_SESSION, intent.action)
    assertEquals("session-1", intent.getStringExtra(ForegroundLocationController.EXTRA_SESSION_ID))
}
~~~

Service tests must prove:

- start/pause/resume/sync behavior remains covered;
- no FusedLocationProviderClient or LocationCallback is owned by the service;
- automatic start calls Coordinator with priority from resolveLocationPriority;
- the next automatic round is scheduled after the previous terminal phase;
- Busy waits until the manual session becomes non-busy;
- disabling continuous collection cancels only an automatic session;
- a manual session continues when continuous collection is disabled;
- manual-only service stops foreground only after the session reaches AwaitingManualSubmit or a final phase;
- pending upload text comes from QueueStatusRepository and may decrease.

- [ ] **Step 2: Run targeted tests and confirm failure**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ForegroundLocationControllerTest" --tests "*ForegroundLocationServiceTest" --tests "*LocationNotificationRendererTest" --no-daemon
~~~

Expected: FAIL because the actions and Coordinator integration are absent.

- [ ] **Step 3: Add controller actions**

~~~kotlin
fun startManualSession() {
    ContextCompat.startForegroundService(
        context,
        serviceIntent(ACTION_START_MANUAL_SESSION)
    )
}

fun cancelLocationSession(expectedSessionId: String? = null) {
    context.startService(
        serviceIntent(ACTION_CANCEL_LOCATION_SESSION)
            .putExtra(EXTRA_SESSION_ID, expectedSessionId)
    )
}
~~~

Add constants ACTION_START_MANUAL_SESSION, ACTION_CANCEL_LOCATION_SESSION and EXTRA_SESSION_ID. Add openLocationIntent with destination extra location.

- [ ] **Step 4: Rewrite service ownership**

Remove service fields fusedClient, locationCallback, registeredIntervalMillis, registeredPriority, qualityCoordinator and pendingUploadCount.

Inject:

~~~kotlin
@Inject lateinit var acquisitionCoordinator: LocationAcquisitionCoordinator
@Inject lateinit var queueStatusRepository: QueueStatusRepository
~~~

Keep LocationPolicyEngine, motion, schedule cache, transition writer, sync action and notification 7101.

Automatic loop algorithm:

1. startCollection performs current permission/GMS/location checks and starts 7101.
2. Initialize policy and schedule the first automatic session immediately.
3. Before each round refresh schedule/motion facts and compute PolicyDecision.
4. Call startAutomaticSession with priority, mode, scheduleLowFrequency and motion signal.
5. If Busy, wait until state.isBusy becomes false without overwriting the manual session.
6. If Started, wait for the matching session to reach Completed, TimedOut, Failed or Cancelled.
7. On Completed with bestLocation, call policyEngine.onAcceptedLocation and refresh last accepted/accuracy text.
8. Recompute policy and delay requestIntervalMillis before the next round.
9. Cancel the loop when continuous collection becomes false.

Manual action algorithm:

1. Call startForeground with existing 7101 before starting work.
2. Do not call setContinuousCollectionEnabled.
3. Call startManualSession with replaceAwaitingManual true.
4. If continuous collection is false, observe this manual session and stop foreground/service only after Evaluating has completed and state is AwaitingManualSubmit, Completed, TimedOut, Failed or Cancelled.
5. If continuous collection is true, keep the normal service and automatic loop; the loop waits while manual state is busy.

Pause/stop collection must cancel an automatic session but not a manual one. onDestroy cancels only a session still in Preparing or Acquiring; it must preserve AwaitingManualSubmit so a manual result survives intentional FGS release.

- [ ] **Step 5: Make the 7101 queue count authoritative**

Collect queueStatusRepository.observe() into a service field pendingUploadTotal. Rename LocationNotificationState.pendingUploadCount and ForegroundLocationRuntimeState.pendingUploadCount to pendingUploadTotal. Never increment it in memory.

The existing channel ID pim_location_collection and notification ID 7101 must remain unchanged.

- [ ] **Step 6: Run targeted tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 7: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt src/client-android/app/src/test/java/com/pim/app/location/service src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt
git commit -m "refactor: schedule bounded foreground location rounds"
~~~

## Task 6: Publish API 36.1 Live Updates

**Files:**
- Create: src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateCapability.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRenderer.kt
- Create: src/client-android/app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisher.kt
- Create: src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdateCapabilityTest.kt
- Create: src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisherTest.kt
- Create: src/client-android/app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRendererTest.kt
- Create: src/client-android/app/src/androidTest/java/com/pim/app/location/liveupdate/LocationLiveUpdatePlatformTest.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/PimApp.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt

- [ ] **Step 1: Write failing capability and publisher tests**

Capability tests must verify:

- major below 36 is false and does not invoke fullSdkCheck;
- major above 36 is true and does not invoke fullSdkCheck;
- major 36 delegates to fullSdkCheck;
- NoSuchFieldError and NoClassDefFoundError return false;
- an ordinary RuntimeException is not swallowed.

~~~kotlin
@Test
fun api36LinkageFailureReturnsFalse() {
    assertFalse(supportsLiveUpdates(36) { throw NoClassDefFoundError("VERSION_CODES_FULL") })
}
~~~

Publisher fake-platform tests must verify:

- only the Acquiring/Evaluating window publishes;
- elapsed/accuracy updates are limited to once per 2_000 ms;
- an accuracy improvement smaller than 5 m does not force an early update;
- leaving the Acquiring/Evaluating window (AwaitingManualSubmit/Enqueuing/terminal/Idle/Preparing) cancels immediately;
- suppressSession blocks the same session but not a new session;
- notification content contains no latitude or longitude field.

- [ ] **Step 2: Run tests and confirm failure**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationLiveUpdateCapabilityTest" --tests "*LocationLiveUpdatePublisherTest" --tests "*LocationLiveUpdateNotificationRendererTest" --no-daemon
~~~

Expected: FAIL because the liveupdate package does not exist.

- [ ] **Step 3: Implement the minor-SDK capability helper**

~~~kotlin
internal fun supportsLiveUpdates(
    majorSdk: Int,
    fullSdkCheck: () -> Boolean
): Boolean = when {
    majorSdk < 36 -> false
    majorSdk > 36 -> true
    else -> try {
        fullSdkCheck()
    } catch (_: LinkageError) {
        false
    }
}

internal object LocationLiveUpdateCapability {
    fun isAvailable(): Boolean = supportsLiveUpdates(Build.VERSION.SDK_INT) {
        Api36.isBaklava1OrLater()
    }

    @RequiresApi(36)
    private object Api36 {
        @SuppressLint("NewApi")
        fun isBaklava1OrLater(): Boolean =
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
    }
}
~~~

Do not use reflection, strings or BAKLAVA without the _1 suffix.

- [ ] **Step 4: Implement coordinate-free notification rendering**

~~~kotlin
internal data class LocationLiveUpdateContent(
    val sessionId: String,
    val triggerType: TriggerType,
    val elapsedSeconds: Long,
    val accuracyMeters: Float?,
    val providerLabel: String?
)
~~~

Constants:

~~~kotlin
const val CHANNEL_ID = "pim_location_live_update"
const val LIVE_UPDATE_NOTIFICATION_ID = 7102
private const val REQUEST_CODE_CANCEL = 71020
private const val REQUEST_CODE_OPEN = 71021
private const val REQUEST_CODE_DELETE = 71022
~~~

Renderer preconditions in this exact order:

1. capability available;
2. POST_NOTIFICATIONS granted;
3. NotificationManager.canPostPromotedNotifications returns true;
4. create IMPORTANCE_LOW channel;
5. build and notify.

Use platform Notification.Builder and Notification.BigTextStyle:

~~~kotlin
Notification.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
    .setContentTitle(
        if (content.triggerType == TriggerType.MANUAL) "手动定位" else "自动定位"
    )
    .setContentText(summaryText(content))
    .setStyle(Notification.BigTextStyle().bigText(expandedText(content)))
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setVisibility(Notification.VISIBILITY_PUBLIC)
    .setRequestPromotedOngoing(true)
    .setShortCriticalText(shortCriticalText(content))
    .setContentIntent(openLocationIntent)
    .setDeleteIntent(deleteIntent)
    .addAction(cancelAction)
    .addAction(openAction)
    .build()
~~~

PendingIntent cancel/delete data URIs must include the session ID because extras do not participate in PendingIntent identity:

~~~kotlin
Uri.parse("pim://location-live/" + content.sessionId + "/cancel")
Uri.parse("pim://location-live/" + content.sessionId + "/delete")
~~~

Use FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE. API 36.1 failure at any precondition is a no-op and must not create a fallback notification.

- [ ] **Step 5: Implement Publisher lifecycle**

Public methods:

~~~kotlin
fun start(scope: CoroutineScope)
fun cancelStaleNotification()
fun suppressSession(sessionId: String)
~~~

The Publisher injects LocationAcquisitionCoordinator and collects state. It creates LocationLiveUpdateContent only from sessionId, triggerType, elapsedMs, accuracy and provider. It must not accept LocationSnapshot as renderer input.

NotificationActionReceiver actions:

- CANCEL_LOCATION_SESSION reads session ID and calls coordinator.cancelCurrentSession(expectedSessionId).
- DISMISS_LOCATION_LIVE_UPDATE reads session ID and calls publisher.suppressSession(sessionId).
- delete does not cancel the location session.

PimApp.onCreate must call cancelStaleNotification before start(scope). Do not place unconditional cancellation inside RunningStateRestorer.ensureRunningState because that method runs while a valid session may still be active.

- [ ] **Step 6: Add API 36.1 instrumentation coverage**

LocationLiveUpdatePlatformTest must skip when capability is false. On 36.1 it builds a notification and asserts:

~~~kotlin
assertTrue(notification.hasPromotableCharacteristics())
assertFalse(renderedText.contains("31.2304"))
assertFalse(renderedText.contains("121.4737"))
assertNotEquals(
    LocationNotificationRenderer.NOTIFICATION_ID,
    LIVE_UPDATE_NOTIFICATION_ID
)
~~~

Do not assert that FLAG_PROMOTED_ONGOING is immediately set or that the system chip is visible.

- [ ] **Step 7: Run unit tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 8: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/location/liveupdate src/client-android/app/src/main/java/com/pim/app/notifications/NotificationActionReceiver.kt src/client-android/app/src/main/java/com/pim/app/PimApp.kt src/client-android/app/src/test/java/com/pim/app/location/liveupdate src/client-android/app/src/androidTest/java/com/pim/app/location/liveupdate src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt
git commit -m "feat: publish Android location live updates"
~~~

## Task 7: Add The Location Page, Six-Item Navigation And Separate Counts

**Files:**
- Create: src/client-android/app/src/main/java/com/pim/app/ui/location/LocationUiState.kt
- Create: src/client-android/app/src/main/java/com/pim/app/ui/location/LocationViewModel.kt
- Create: src/client-android/app/src/main/java/com/pim/app/ui/location/LocationScreen.kt
- Create: src/client-android/app/src/test/java/com/pim/app/ui/location/LocationViewModelTest.kt
- Create: src/client-android/app/src/androidTest/java/com/pim/app/ui/location/LocationScreenTest.kt
- Create: src/client-android/app/src/androidTest/java/com/pim/app/ui/root/PimRootScreenNavTest.kt
- Create: src/client-android/app/src/androidTest/java/com/pim/app/ui/today/TodayScreenTest.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/MainActivity.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt
- Modify: src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/ui/today/TodayViewModelTest.kt
- Modify: src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterScreenTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NativeShellTest.kt
- Modify: src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt

- [ ] **Step 1: Write failing ViewModel and count mapping tests**

Today mapper:

~~~kotlin
@Test
fun mapperExposesSeparateLocationQueueCount() {
    val state = baseState(
        pendingTotal = 9,
        pendingLocationPoints = 3,
        isLoading = false
    )
    val ui = TodayStatusMapper.fromStatus(state)
    assertEquals(9, ui.pendingCount)
    assertEquals(3, ui.pendingLocationPoints)
}
~~~

LocationViewModel mapping must assert manual/automatic label, phase label, coordinates, total/location queue counts and button flags.

- [ ] **Step 2: Run unit tests and confirm failure**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationViewModelTest" --tests "*TodayViewModelTest" --tests "*AndroidV2NativeShellTest" --tests "*AndroidV2ScreenContentTest" --no-daemon
~~~

Expected: FAIL because Location UI and the sixth destination do not exist.

- [ ] **Step 3: Implement LocationUiState and ViewModel**

LocationUiState must contain:

~~~kotlin
data class LocationUiState(
    val triggerLabel: String = "尚未开始",
    val phaseLabel: String = "空闲",
    val elapsedText: String = "0 秒",
    val deadlineText: String = "最长 30 秒",
    val bestLocation: LocationSnapshot? = null,
    val pendingUploadTotal: Int = 0,
    val pendingLocationPoints: Int = 0,
    val errorMessage: String? = null,
    val showStart: Boolean = true,
    val showCancel: Boolean = false,
    val showSubmit: Boolean = false,
    val showRestart: Boolean = false,
    val showOpenSettings: Boolean = false,
    val isSubmitting: Boolean = false,
    val manualStartEnabled: Boolean = true
)
~~~

LocationViewModel injects Coordinator, ForegroundLocationController and QueueStatusRepository. Its state is combine(coordinator.state, queueStatusRepository.observe()). Actions:

~~~kotlin
fun startOrRestart() = foregroundLocationController.startManualSession()
fun cancel() = foregroundLocationController.cancelLocationSession(
    coordinator.state.value.sessionId
)
fun submit() = coordinator.submitManualResult()
~~~

It must not inject Context, FusedLocationProviderClient or LocationCaptureRepository.

- [ ] **Step 4: Implement the four-section screen**

Use LazyColumn with full-width sections separated by HorizontalDivider, not nested cards.

Stable tags:

- location-status-section
- location-best-section
- location-actions-section
- location-queue-section
- location-start
- location-cancel
- location-submit
- location-restart
- location-open-settings
- location-pending-total
- location-pending-points
- location-accuracy
- location-provider
- location-latitude
- location-longitude
- location-altitude
- location-speed
- location-bearing
- location-recorded-time

Idle shows 开始定位. Automatic busy (TriggerType.AUTOMATIC with Preparing/Acquiring/Evaluating) keeps the location-start entry visible but disabled with 定位进行中 text; manual busy does not show it. Preparing/Acquiring/Evaluating shows 取消. AwaitingManualSubmit shows 提交位置 and 重新定位. Enqueuing shows disabled 提交中. Final phases show 重新定位.

For missing precise permission, disabled system location or unavailable Google Play services, show 打开设置 and route to the existing Settings destination. Display horizontal accuracy, Provider, latitude, longitude, altitude, speed, bearing and localized recorded time in the best-position section. Display latitude/longitude only in LocationScreen. Do not pass them to liveupdate package.

- [ ] **Step 5: Add Today and Status counts**

Add pendingLocationPoints to TodayUiState and map from state.snapshot.queues.pendingLocationPoints. Add tagged chips:

- today-pending-total
- today-pending-location

Status TransportSection should use two rows:

1. 待传总数 and 定位待传;
2. 本轮确认、本轮拒绝、永久拒绝.

Add status-pending-location. This avoids squeezing five equal-width chips into 320 dp.

- [ ] **Step 6: Add six-item navigation and robust destination intents**

PimDestination order:

~~~kotlin
Today("今日", Icons.Filled.LocationOn),
Location("定位", Icons.Filled.MyLocation),
Tracks("轨迹", Icons.Filled.LocationOn),
Schedule("日程", Icons.Filled.CheckCircle),
Status("状态", Icons.Filled.Security),
Settings("设置", Icons.Filled.Settings)
~~~

Extract PimBottomNavigation(selected, onSelected) and give every NavigationBarItem Modifier.weight(1f) plus tag pim-nav followed by lower-case enum name.

Route PimDestination.Location to LocationScreen and pass onOpenSettings = { selected = PimDestination.Settings }.

MainActivity must support destination changes while already running. Keep destination in Compose state, parse both onCreate and onNewIntent, and use FLAG_ACTIVITY_CLEAR_TOP plus FLAG_ACTIVITY_SINGLE_TOP in notification content intents.

The route mapping must recognize location and status.

- [ ] **Step 7: Write instrumentation tests**

LocationScreenTest:

- four sections visible;
- manual/automatic trigger labels;
- coordinates visible on page;
- automatic active state keeps location-start visible but disabled with 定位进行中 text;
- total/location counts both visible;
- submit/restart/cancel states.
- precheck failure exposes the 打开设置 action;
- all best-position fields render without overlapping.

PimRootScreenNavTest runs the pure bottom navigation in 320 dp and 360 dp hosts. For each width:

1. assert six tags exist;
2. assert bounds remain within host;
3. sort item bounds by left and assert each right is less than or equal to the next left;
4. click Location and assert callback receives PimDestination.Location.

TodayScreenTest and StatusCenterScreenTest assert both total and location count tags.

Make TodayStatusBar internal instead of private so TodayScreenTest can render the state-driven native bar without constructing a Hilt ViewModel or WebView.

- [ ] **Step 8: Run unit tests**

Run the Step 2 command again.

Expected: PASS.

- [ ] **Step 9: Run targeted instrumentation tests on a started emulator**

~~~powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.location.LocationScreenTest
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.root.PimRootScreenNavTest
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.today.TodayScreenTest,com.pim.app.ui.status.StatusCenterScreenTest
~~~

Expected: PASS.

- [ ] **Step 10: Commit**

~~~powershell
git add src/client-android/app/src/main/java/com/pim/app/ui/location src/client-android/app/src/main/java/com/pim/app/ui/root src/client-android/app/src/main/java/com/pim/app/ui/today src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/main/java/com/pim/app/MainActivity.kt src/client-android/app/src/test/java/com/pim/app/ui/location src/client-android/app/src/test/java/com/pim/app/ui/today src/client-android/app/src/test/java/com/pim/app/v2 src/client-android/app/src/androidTest/java/com/pim/app/ui
git commit -m "feat: add Android location status page"
~~~

## Task 8: Integrate, Verify And Open The Pull Request

**Files:**
- Modify only files required by integration failures.
- Do not modify .github/workflows beyond the android-36.1 package change in Task 1.

- [ ] **Step 1: Rebase or merge worker commits into the integration branch**

Review each commit with:

~~~powershell
git show --stat --oneline HEAD
git diff origin/master...HEAD -- src/client-android .github/workflows/build-android.yml docs/superpowers
~~~

Confirm no build/, .gradle/, .codex-tmp/, APK or generated schema artifacts are staged.

- [ ] **Step 2: Run the complete Android unit suite**

~~~powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
~~~

Expected: PASS.

- [ ] **Step 3: Run lint and assemble**

~~~powershell
.\gradlew.bat :app:lintDebug :app:assembleDebug --no-daemon
~~~

Expected: PASS.

- [ ] **Step 4: Run the mandatory connected test gate**

Start an emulator or connect a physical device, then run:

~~~powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
~~~

Expected: PASS. This gate is mandatory for Android status UI changes and cannot be replaced by CI.

- [ ] **Step 5: Validate the platform matrix**

API 34:

- no pim_location_live_update channel;
- no notification 7102;
- persistent notification 7101 still works;
- manual/automatic acquisition and Location page still work.

API 36.0:

- capability helper catches missing minor-SDK symbols;
- no crash, channel or 7102 notification.

API 36.1:

- promotion disabled: no 7102 notification;
- promotion enabled: notification is promotable, only exists in the Acquiring/Evaluating window, has cancel/open actions and no coordinates;
- delete suppresses only the current session;
- leaving the Acquiring/Evaluating window cancels 7102 immediately;
- persistent 7101 remains independent.

The final system chip shape is a manual observation, not a pass/fail assertion.

- [ ] **Step 6: Run repository status checks**

~~~powershell
git diff --check
git status --short --branch
git diff --cached --name-status
~~~

Expected: only intentional source, test, docs and the single Android workflow file are committed.

- [ ] **Step 7: Commit integration fixes**

~~~powershell
git add -u -- .github/workflows/build-android.yml src/client-android/build.gradle.kts src/client-android/app/build.gradle.kts src/client-android/app/src
git add -- docs/superpowers/plans/2026-07-20-android-location-live-updates.md docs/superpowers/specs/2026-07-20-android-location-live-updates-design.md
git commit -m "test: verify Android location live updates"
~~~

Skip this commit when no integration fix is needed.

- [ ] **Step 8: Push and open a PR**

~~~powershell
git push -u origin codex/android-live-updates-espresso-fix
gh pr create --base master --head codex/android-live-updates-espresso-fix --title "feat: add Android location live updates" --body "Adds bounded manual and automatic location sessions, API 36.1 Live Updates with an older-version no-op, independent notification IDs 7101/7102, a native Location page, authoritative total/location queue counts, and the Android 36.1 CI SDK package. Verification evidence is included in the PR checks and description updates."
~~~

The PR body must summarize:

- bounded manual/automatic sessions;
- API 36.1 Live Update with API below 36.1 no-op;
- independent 7101/7102 notifications;
- Location page and queue counts;
- compile SDK/CI package update;
- unit, lint, assemble and connected-test evidence;
- API 34/36.0/36.1 manual matrix status.

- [ ] **Step 9: Wait for GitHub Actions**

Use gh pr checks with watch mode until all triggered checks finish. If Android workflow does not trigger because of path filters, state that explicitly. Do not call the task complete while a triggered check is pending or failing.

## Plan Self-Review Checklist

- Every confirmed requirement maps to a task:
  - bounded 30-second sessions: Task 3 and Task 4;
  - manual and automatic sessions: Task 4 and Task 5;
  - manual confirmation and automatic enqueue: Task 4;
  - independent Live Update: Task 6;
  - API below 36.1 no-op: Task 1 and Task 6;
  - Location page and manual button: Task 7;
  - total and location queue counts: Task 2 and Task 7;
  - narrow six-item navigation: Task 7;
  - local connected test gate: Task 8.
- No task uses getCurrentLocation, OEM APIs, custom RemoteViews, targetSdk 36 or a fallback second notification.
- The only shared write hotspots are ForegroundLocationController, MainActivity and PimApp; ownership is fixed by task and integration order.
- Existing dirty build outputs are never staged or cleaned.
