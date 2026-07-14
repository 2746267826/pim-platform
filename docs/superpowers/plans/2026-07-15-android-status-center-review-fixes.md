# Android Status Center Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 PR #27 已确认的状态中心事实性、可操作性和真机稳定性问题，同时保持个人项目所需的轻量实现。

**Architecture:** 保留现有 Room、WorkManager、Repository、ViewModel 和 Compose 边界。WorkInfo 只解释活动任务，终态读取 `MobileSyncState`；网络改为三态并与 PIM 服务器探测分层；瞬时操作反馈只放在 ViewModel，普通 UI 只显示受控中文摘要。探测并发只增加一个 `Mutex`，不引入数据库、任务代次、事件总线或新协调器。

**Tech Stack:** Kotlin, Coroutines Flow, WorkManager, Room, Hilt, Jetpack Compose, Robolectric, JUnit 4, Gradle, .NET test suite

---

## File Map

- `StatusResultMapper.kt`: 同步相位、网络/服务器问题和 Accepted 清理判定。
- `StatusActions.kt`: 同步请求成功后才发布 Accepted。
- `StatusCenterRepository.kt`: 纯 `combine`，先发射状态再清理 Accepted。
- `NetworkStatusProvider.kt`: `Unavailable/Restricted/Validated` 三态和 `SecurityException` 兜底。
- `ConnectionProbeService.kt`: 单例内一个 `Mutex` 串行化探测。
- `ConnectionProbeStore.kt`: 同服务器旧证据拒绝和时钟回拨恢复。
- `ConnectionProbePolicy.kt`, `SettingsViewModel.kt`, `StatusCenterViewModel.kt`: 复用 5 分钟缓存/30 秒异常重试策略，自动探测静默。
- `StatusDisplayText.kt`: 状态页实际使用的少量中文纯映射。
- `StatusIssue.kt`, `StatusCenterScreen.kt`: 固定摘要、Info 分区、窄屏布局和死字段清理。
- 现有 JVM/Robolectric/Compose 测试文件：逐项增加回归测试，不新建测试框架。

## Guardrails

- 不实现诊断 ZIP/日志导出；它仍是 Android 总计划的后续保留功能。
- 不改 `.github/workflows/*`，不新增模拟器 CI。
- 不新增数据库表、WorkManager generation/CAS、ProbeRunner、全局事件总线或完整 i18n。
- 不大拆 `StatusIssue.kt`、`StatusCenterScreen.kt`，不扩到 Today、Tracks、Schedule、Web 或 Windows UI。
- 每个生产行为先写失败测试并确认 RED；写 worker 串行运行，每步由根代理检查 diff 和测试。

### Task 0: Merge Upstream And Establish Baseline

**Files:**
- Verify: repository-wide

- [ ] **Step 1: Confirm the branch is clean and merge current upstream**

```powershell
git status --short --branch
git rev-list --left-right --count origin/master...HEAD
git merge origin/master -m "chore: merge origin/master ahead of Android status review fixes"
```

Expected: branch is `codex/android-status-actions`; merge completes without Android conflicts. If a conflict appears, stop and inspect it before editing.

- [ ] **Step 2: Configure the local Android toolchain for this machine**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
java -version
adb version
```

Expected: bundled Android Studio JBR and Android SDK tools respond.

- [ ] **Step 3: Run the post-merge baseline**

```powershell
dotnet test Pim.sln
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
Pop-Location
```

Expected: all commands pass before review fixes are introduced. Record an environmental failure exactly; do not mislabel it as a code regression.

### Task 1: Make Sync Phase Use Active Work And Persisted Outcomes

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusOverallAndSyncPhaseTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/ui/status/StatusPresentationTest.kt`

- [ ] **Step 1: Add failing phase tests**

Add tests with these exact contracts:

```kotlin
@Test fun oldFailedAndSucceededWorkInfosDoNotChooseTerminalPhase()
@Test fun periodicRunningIsRunning()
@Test fun workInfoBlockedIsWaiting()
@Test fun persistedPrerequisitePhaseIsBlocked()
@Test fun persistedRetryOutcomeIsFailed()
@Test fun completedWithErrorsIsFailed()
@Test fun persistedCompletedPhaseIsCompleted()
@Test fun acceptedIsEmittedBeforeActiveWorkPhase()
```

The mixed-history test must use both `FAILED` and `SUCCEEDED` and assert the result is derived from an otherwise idle `MobileSyncState`, independent of list order and UUID.

- [ ] **Step 2: Run the focused tests and verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusOverallAndSyncPhaseTest" --tests "com.pim.app.ui.status.StatusPresentationTest"
Pop-Location
```

Expected: failures show terminal WorkInfo pollution, missing `Blocked`, missing periodic `RUNNING`, and missing WorkInfo `BLOCKED` behavior.

- [ ] **Step 3: Implement the minimal phase resolver**

Add `Blocked` to `SyncPhase`, then replace terminal WorkInfo interpretation with this shape:

```kotlin
fun resolveSyncPhase(
    periodic: List<WorkInfo>,
    immediate: List<WorkInfo>,
    syncState: MobileSyncState,
    justAccepted: Boolean
): SyncPhase {
    if (justAccepted) return SyncPhase.Accepted
    val active = periodic + immediate
    if (active.any { it.state == WorkInfo.State.RUNNING }) return SyncPhase.Running
    if (immediate.any { it.state == WorkInfo.State.ENQUEUED } ||
        active.any { it.state == WorkInfo.State.BLOCKED }
    ) return SyncPhase.Waiting

    val phase = syncState.phase.lowercase()
    if (phase in BLOCKED_SYNC_PHASES) return SyncPhase.Blocked
    if (syncState.outcome != MobileSyncOutcome.SUCCESS ||
        phase == "failed" || phase.endsWith("-failed") || phase == "completed-with-errors"
    ) return SyncPhase.Failed
    if (phase in setOf("completed", "uploaded", "location-uploaded")) return SyncPhase.Completed
    return SyncPhase.Idle
}
```

Update `buildState()` to pass `syncState`. Keep `Cancelled` only as a compatible display value; do not infer it from historical WorkInfo. Add `Blocked` labels and disable the sync button while prerequisites are missing.

Also remove the old `failedCount > 0` fallback from issue creation. A `sync-failure` issue is added only when the resolved phase is `Failed`; otherwise an earlier run's count must not override the current persisted phase/outcome.

- [ ] **Step 4: Verify GREEN and commit**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusOverallAndSyncPhaseTest" --tests "com.pim.app.ui.status.StatusPresentationTest"
Pop-Location
git add src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/test/java/com/pim/app/status/StatusOverallAndSyncPhaseTest.kt src/client-android/app/src/test/java/com/pim/app/ui/status/StatusPresentationTest.kt
git commit -m "fix: derive Android sync phase from current facts"
```

Expected: focused tests pass.

### Task 2: Make Accepted Deterministic And Side-Effect Free

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/StatusCenterRepositoryFlowTest.kt`

- [ ] **Step 1: Add failing action and flow-order tests**

```kotlin
@Test fun syncActionPublishesAcceptedOnlyAfterEnqueueSucceeds()
@Test fun syncActionFailureDoesNotPublishAccepted()
@Test fun oldTerminalWorkDoesNotClearAccepted()
@Test fun activeImmediateWorkClearsAcceptedAfterAcceptedWasObserved()
```

For the final test, collect a one-item mapping flow, assert `acceptedSignal.accepted.value` is still `true` inside `onEach`, then assert it is `false` after collection.

- [ ] **Step 2: Run tests and verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusIssueTest" --tests "com.pim.app.status.StatusCenterRepositoryFlowTest"
Pop-Location
```

Expected: current runner publishes before enqueue, old terminal work clears Accepted, and repository combine has an input-mutating side effect.

- [ ] **Step 3: Move publication and cleanup to deterministic points**

Use this runner order:

```kotlin
suspend fun run(route: StatusActionRoute) {
    if (route != StatusActionRoute.TriggerSync) return
    try {
        syncNow()
        acceptedSignal.trigger()
    } finally {
        refresh()
    }
}
```

Limit cleanup to active immediate work:

```kotlin
fun shouldClearAcceptedSignal(justAccepted: Boolean, immediate: List<WorkInfo>): Boolean =
    justAccepted && immediate.any {
        it.state == WorkInfo.State.ENQUEUED ||
            it.state == WorkInfo.State.RUNNING ||
            it.state == WorkInfo.State.BLOCKED
    }
```

Keep `combine` pure by returning a small internal emission value, then clear only after downstream receives the state:

```kotlin
internal data class StatusEmission(
    val state: StatusCenterState,
    val clearAcceptedAfterEmission: Boolean
)

internal fun Flow<StatusEmission>.emitStates(
    clearAccepted: () -> Unit
): Flow<StatusCenterState> = transform { emission ->
    emit(emission.state)
    if (emission.clearAcceptedAfterEmission) clearAccepted()
}
```

- [ ] **Step 4: Verify GREEN and commit**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusIssueTest" --tests "com.pim.app.status.StatusCenterRepositoryFlowTest" --tests "com.pim.app.status.StatusOverallAndSyncPhaseTest"
Pop-Location
git add src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt src/client-android/app/src/test/java/com/pim/app/status/StatusCenterRepositoryFlowTest.kt
git commit -m "fix: preserve accepted sync feedback until active work"
```

### Task 3: Add Three-State Network Facts And Manifest Permission

**Files:**
- Modify: `src/client-android/app/src/main/AndroidManifest.xml`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/NetworkStatusProvider.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/manifest/PermissionManifestTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/NetworkStatusProviderTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusOverallAndSyncPhaseTest.kt`

- [ ] **Step 1: Add failing permission, capability, exception, and issue tests**

```kotlin
@Test fun manifestDeclaresAccessNetworkState()
@Test fun internetWithoutValidatedIsRestricted()
@Test fun internetAndValidatedIsValidated()
@Test fun securityExceptionFailsClosed()
@Test fun restrictedNetworkWithReachableServerIsInfo()
@Test fun restrictedNetworkDoesNotHideBlockedProbe()
@Test fun unavailableNetworkDoesNotHidePartialProbe()
```

Update existing callback tests to expect `NetworkAvailability` values and ensure cancellation unregisters only a successfully registered callback.

Run capability/callback coverage with `@Config(sdk = [26, 34])`, including `onAvailableWithoutValidatedEmitsRestricted`, so both the minimum supported API behavior and current target behavior are checked.

- [ ] **Step 2: Run tests and verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.manifest.PermissionManifestTest" --tests "com.pim.app.status.NetworkStatusProviderTest" --tests "com.pim.app.status.StatusOverallAndSyncPhaseTest"
Pop-Location
```

Expected: permission is absent, `VALIDATED` is ignored, and network/probe issues are short-circuited.

- [ ] **Step 3: Implement the three-state provider and independent issue mapping**

```kotlin
enum class NetworkAvailability { Unavailable, Restricted, Validated }

internal fun availabilityFor(capabilities: NetworkCapabilities?): NetworkAvailability {
    if (capabilities == null) return NetworkAvailability.Unavailable
    val internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    return if (internet && validated) NetworkAvailability.Validated else NetworkAvailability.Restricted
}

internal inline fun safeNetworkRead(block: () -> NetworkAvailability): NetworkAvailability =
    try { block() } catch (_: SecurityException) { NetworkAvailability.Unavailable }
```

Use `safeNetworkRead` for initial read, `onAvailable`, `onLost`, and capability reads. Catch registration `SecurityException`, emit `Unavailable`, and call `unregisterNetworkCallback` only when registration succeeded. Add:

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Replace `networkConnected: Boolean` with `networkAvailability`. Generate the system-network issue and probe issue independently. Restricted + reachable is Info; Restricted + missing/unreachable probe is Warning; Unavailable is Critical.

- [ ] **Step 4: Verify GREEN and commit**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.manifest.PermissionManifestTest" --tests "com.pim.app.status.NetworkStatusProviderTest" --tests "com.pim.app.status.StatusOverallAndSyncPhaseTest"
Pop-Location
git add src/client-android/app/src/main/AndroidManifest.xml src/client-android/app/src/main/java/com/pim/app/status/NetworkStatusProvider.kt src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt src/client-android/app/src/main/java/com/pim/app/status/StatusResultMapper.kt src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/test/java/com/pim/app/manifest/PermissionManifestTest.kt src/client-android/app/src/test/java/com/pim/app/status/NetworkStatusProviderTest.kt src/client-android/app/src/test/java/com/pim/app/status/StatusOverallAndSyncPhaseTest.kt
git commit -m "fix: report validated Android network availability"
```

### Task 4: Serialize Probes And Preserve Newest Evidence

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeStoreTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbePolicyTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt`

- [ ] **Step 1: Add failing concurrency, freshness, rollback, and policy tests**

```kotlin
@Test fun concurrentProbesNeverOverlapNetworkExecution()
@Test fun olderEvidenceForSameServerIsRejected()
@Test fun olderEvidenceForDifferentServerIsAccepted()
@Test fun clockRollbackAllowsFutureEvidenceToBeReplaced()
@Test fun fakeStoreRejectsExpiredEvidence()
@Test fun automaticSettingsProbeIsSilentOnFailure()
```

The concurrency test must assert maximum simultaneous execution is `1`; two callers may perform two sequential probes. Do not incorrectly assert request coalescing.

- [ ] **Step 2: Run tests and verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.ConnectionProbeServiceTest" --tests "com.pim.app.status.ConnectionProbeStoreTest" --tests "com.pim.app.status.ConnectionProbePolicyTest" --tests "com.pim.app.ui.settings.SettingsServerMutationTest"
Pop-Location
```

Expected: probes overlap, old evidence overwrites new evidence, FakeStore ignores age, or automatic Settings polling changes action feedback.

- [ ] **Step 3: Add only the lightweight synchronization and timestamp guard**

```kotlin
private val probeMutex = Mutex()

override suspend fun probe(serverUrl: String): ConnectionProbeResult =
    probeMutex.withLock { probeLocked(serverUrl) }
```

Move the existing probe body unchanged into `probeLocked`. In `ConnectionProbeStore`, inject a default clock and compare/write under the existing lock:

```kotlin
class ConnectionProbeStore(
    private val preferences: SharedPreferences,
    private val json: Json,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ConnectionProbeEvidenceStore {
    override fun save(result: ConnectionProbeResult): Boolean = synchronized(lock) {
        val current = mutableResult.value
        val staleSameServer = current != null &&
            current.serverIdentity == result.serverIdentity &&
            result.checkedAtUtcMillis < current.checkedAtUtcMillis &&
            current.checkedAtUtcMillis <= nowMillis()
        if (staleSameServer) return@synchronized false
        val committed = preferences.edit()
            .putString(KEY_RESULT, json.encodeToString(result))
            .commit()
        if (committed) mutableResult.value = result
        committed
    }
}
```

Use existing `resolveProbeResult()` from Settings and Status. Automatic exceptions retry in 30 seconds without overwriting user feedback; normal `Blocked`, `Partial`, and `Reachable` results use the 5-minute cache. Remove Status ViewModel init probing and the Screen `ON_RESUME` probe; keep the visible-screen loop and manual force action.

- [ ] **Step 4: Fix FakeStore and verify GREEN**

```kotlin
override fun freshResult(serverIdentity: String, nowMillis: Long): ConnectionProbeResult? =
    storedResult?.takeIf { it.serverIdentity == serverIdentity }?.takeIf {
        val age = nowMillis - it.checkedAtUtcMillis
        age >= 0L && age < ConnectionProbeStore.FRESHNESS_MILLIS
    }
```

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.ConnectionProbeServiceTest" --tests "com.pim.app.status.ConnectionProbeStoreTest" --tests "com.pim.app.status.ConnectionProbePolicyTest" --tests "com.pim.app.ui.settings.SettingsServerMutationTest"
Pop-Location
git add src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeStoreTest.kt src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbePolicyTest.kt src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt
git commit -m "fix: serialize Android connection probes"
```

### Task 5: Add Inline Action Feedback And Controlled Chinese Text

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/status/StatusDisplayText.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/status/StatusDisplayTextTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterScreenTest.kt`

- [ ] **Step 1: Add failing feedback and text-safety tests**

```kotlin
@Test fun unknownCodesNeverEchoRawMachineText()
@Test fun knownProfilePolicyAndDropReasonsUseChineseLabels()
@Test fun syncFailureUsesFixedSummaryInsteadOfLastError()
@Test fun manualProbeShowsCheckingCompletedAndFailedFeedback()
@Test fun syncSubmitFailureShowsFixedInlineFeedback()
@Test fun diagnosticsDoNotRenderRawLogOrExceptionText()
```

- [ ] **Step 2: Run JVM tests and the available Compose test target to verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusDisplayTextTest" --tests "com.pim.app.status.StatusIssueTest"
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.status.StatusCenterScreenTest
Pop-Location
```

Expected: JVM tests fail until mappings/fixed summaries exist. Instrumented command either reports the expected UI failures or an explicit missing-device blocker.

- [ ] **Step 3: Implement a small mapping object, not an i18n framework**

```kotlin
object StatusDisplayText {
    fun apiReason(code: String?): String = when (code) {
        null, "" -> "暂无"
        "missing" -> "未配置"
        "invalid-api-url" -> "地址格式不正确"
        else -> "未知状态"
    }

    fun profile(value: String?): String = when (value) {
        "power-saving" -> "省电"
        "standard" -> "标准"
        "high-precision" -> "高精度"
        "custom" -> "自定义"
        null, "" -> "暂无"
        else -> "未知状态"
    }

    fun droppedReason(value: String?): String = when (value) {
        "missing-horizontal-accuracy" -> "缺少水平精度"
        "horizontal-accuracy-too-low" -> "定位精度不达标"
        "altitude-missing-timeout" -> "等待高度超时"
        null, "" -> "暂无"
        else -> "其他原因"
    }

    fun policyMode(value: String?): String = when (value) {
        "Off" -> "已停止"
        "PowerSavingNormal" -> "常规省电"
        "ScheduleLowFrequency" -> "日程低频"
        "MotionObservation" -> "运动观察"
        "MovementRecovery" -> "移动恢复"
        "SyncFallback" -> "同步兜底"
        null, "" -> "暂无"
        else -> "未知状态"
    }

    fun heartbeat(value: String?): String = when (value) {
        "心跳上报成功" -> "正常"
        "心跳上报失败" -> "最近上报异常"
        null, "" -> "暂无"
        else -> "未知状态"
    }
}
```

Use these mappings in both `StatusIssue` factories and Compose facts. `StatusIssue.syncFailure()` must always say `最近同步出现异常，请导出日志查看详情。`; heartbeat and diagnostic rows use fixed summaries such as `有近期诊断记录` and never interpolate `lastError`, `lastLogMessage`, `recentLogMessages`, enum names, `null`, or unknown codes.

- [ ] **Step 4: Add ViewModel-only feedback**

```kotlin
enum class StatusActionFeedback {
    ProbeChecking, ProbeCompleted, ProbeFailed, SyncSubmitFailed
}

private val _feedback = MutableStateFlow<StatusActionFeedback?>(null)
val feedback: StateFlow<StatusActionFeedback?> = _feedback.asStateFlow()
```

Manual probe sets Checking then Completed/Failed. `syncNow()` catches non-cancellation enqueue exceptions and sets SyncSubmitFailed. Automatic probing never writes `_feedback`. Map values to exactly:

```text
检查中
检查已完成
检查未完成，请稍后重试
同步请求未能提交，请稍后重试
```

- [ ] **Step 5: Verify GREEN and commit**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusDisplayTextTest" --tests "com.pim.app.status.StatusIssueTest"
Pop-Location
git add src/client-android/app/src/main/java/com/pim/app/status/StatusDisplayText.kt src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/test/java/com/pim/app/status/StatusDisplayTextTest.kt src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterScreenTest.kt
git commit -m "fix: explain Android status actions in Chinese"
```

### Task 6: Separate Actionable Issues And Finish Narrow-Screen UI

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt`
- Test: `src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterScreenTest.kt`

- [ ] **Step 1: Add failing grouping, dead-field, and 320dp tests**

```kotlin
@Test fun needAttentionContainsOnlyCriticalAndWarningInSeverityOrder()
@Test fun statusInformationAppearsOnlyWhenInfoExists()
@Test fun pendingUploadSnapshotHasNoPendingLogsField()
@Test fun narrowStatusContentKeepsSyncCountsAndActionsVisible()
```

The narrow test uses a 320dp-wide host and long values, then asserts sync phase, button, all four counters, and issue action remain displayed without duplicate or clipped semantic nodes.

- [ ] **Step 2: Run tests and verify RED**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon --tests "com.pim.app.status.StatusIssueTest"
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.status.StatusCenterScreenTest
Pop-Location
```

- [ ] **Step 3: Apply the focused UI and model cleanup**

```kotlin
val actionable = issues
    .filter { it.severity != StatusSeverity.Info }
    .sortedBy { if (it.severity == StatusSeverity.Critical) 0 else 1 }
val information = issues.filter { it.severity == StatusSeverity.Info }
```

Render actionable items under `需要处理`; render `状态信息` only when `information` is non-empty. Wrap repeated issue rows in `key(issue.code)`. Give sync phase text `Modifier.weight(1f)`, `maxLines = 1`, and ellipsis; give count value/label one line with ellipsis. Keep button dimensions stable.

Delete `QueueStatusSnapshot.pendingLogs` and all constructor arguments. Do not replace it with `pendingLogCount()` because structured logs are not an upload queue.

- [ ] **Step 4: Verify GREEN and commit**

```powershell
Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
Pop-Location
git add src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt src/client-android/app/src/androidTest/java/com/pim/app/ui/status/StatusCenterScreenTest.kt
git commit -m "fix: separate Android status issues from information"
```

### Task 7: Document Local Gate, Review, Verify, And Update PR #27

**Files:**
- Modify: `AGENTS.md`
- Verify: all files changed by Tasks 1-6

- [ ] **Step 1: Document the local instrumentation gate**

Add one concise Android note to `AGENTS.md`:

```markdown
- Android status UI changes must also run `src/client-android/gradlew.bat :app:connectedDebugAndroidTest --no-daemon` on a started emulator or physical device; this is a local gate because CI does not provide an emulator.
```

- [ ] **Step 2: Run final local verification**

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Push-Location src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
Pop-Location

dotnet test Pim.sln
git diff --check
git status --short --branch
```

Expected: unit, assemble, instrumentation, .NET, and diff checks pass. If no emulator/device is connected, report the exact `connectedDebugAndroidTest` blocker and do not claim that gate passed.

- [ ] **Step 3: Run parallel read-only review**

Dispatch 10 read-only OpenCode workers at the configured cheap tier to independently review: spec coverage, sync/Accepted, network, probe/store, ViewModel feedback, UI/text safety, tests, concurrency/cancellation, Android compatibility, and regression risk. Reject any worker file edits, integrate only verified findings, and rerun affected tests after each fix.

- [ ] **Step 4: Commit documentation and any verified review fixes**

```powershell
git add AGENTS.md
git commit -m "docs: record Android status instrumentation gate"
git status --short --branch
```

- [ ] **Step 5: Push and wait for PR checks**

```powershell
git push origin codex/android-status-actions
gh pr view 27 --json url,headRefName,statusCheckRollup
gh pr checks 27 --watch
```

Expected: PR #27 points at `codex/android-status-actions` and all triggered GitHub Actions checks pass. If a workflow is skipped by path filters, record that explicitly.

## Completion Criteria

- All three Critical review bugs have reproducing tests and fixes.
- Confirmed Important/minor items in the approved design are implemented; rejected busy-loop/fallback suggestions are not added.
- Historical WorkInfo, unvalidated network, and raw machine/error text cannot mislead the status UI.
- Manual sync and connection checks have immediate, fixed Chinese feedback.
- Android unit/assemble, local Compose gate, .NET tests, and PR checks are accounted for with exact evidence.
- Diagnostic ZIP remains unimplemented and visibly listed as later work, not falsely claimed complete.
