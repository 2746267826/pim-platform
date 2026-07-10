# Android Client Reliability Phase 3 Schedule And Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用真实日程查询、缓存新鲜度、策略锚点和转换证据完成原生 Schedule 页面，并以两台 AVD、签名 CI APK、用户真机和完整覆盖报告证明 Android 可靠性改造全部完成。

**Architecture:** `ScheduleWindowRepository` 将 API 结果解析为带诊断的 load result，`ScheduleCacheRepository` 使用 Phase 1 已建立的 Room v4 表提供 6 小时新鲜度，`SchedulePolicyCoordinator` 统一 service start/foreground/30-minute/boundary refresh。定位策略把 anchor、进入、运动、距离恢复、结束和恢复变成持久领域事件；最终 draft PR 先产出签名 APK，再在同一 PR 上补充真机证据后关闭覆盖矩阵。

**Tech Stack:** Kotlin, Jetpack Compose Material3, Hilt, Room 2.6.1, coroutines, Android instrumentation, PowerShell/ADB/AVD, .NET 8 Calendar module, Ical.Net, xUnit, Playwright, GitHub CLI and Actions.

---

## Final Objective

Phase 3 结束时，Schedule 的当前/下一日程、策略模式、锚点、进入/退出原因、缓存新鲜度和历史转换全部来自真实证据；所有 Android/Web/API 自动门槛通过；签名 APK 在用户实际手机上完成 fresh/upgrade、权限、采集、同步、Web、日程、诊断和重启矩阵；最终覆盖报告不存在未验证需求。

## Preconditions

- Phase 2 PR 已合并到最新 `master`；`mobileItemResultsV1` 和 `androidEmbedV1` 均已部署。
- 创建 `codex/android-schedule-completion` 独立 worktree。
- Room 已是 schema 4；本阶段不得用 destructive migration 或临时 bump 规避 Phase 1 schema contract。
- 当前机器有 `Pixel_9` 和 `Pixel_Tablet` AVD，但开始时没有连接的物理设备；物理设备缺失是验收 checkpoint，不是范围删除理由。
- 真机升级只能使用同签名、递增 `versionCode` 的当前 master CI APK 和本 PR CI APK。

## File Structure Map

### Schedule Data And Policy

- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/SchedulePolicyModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleEventSource.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleRefreshPlanner.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/SchedulePolicyCoordinator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/PolicyRuntimeStore.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/DiagnosticDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheDao.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/startup/AppForegroundObserver.kt`

### Schedule UI And Tests

- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`
- Replace: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/MainActivity.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheRepositoryTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleRefreshPlannerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/SchedulePolicyCoordinatorTest.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/location/policy/PolicyRuntimeStoreTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/schedule/SchedulePolicyContentTest.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/lifecycle/BootAndUpdateRecoveryTest.kt`

### Backend Overlap Contract

- Modify: `src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs`
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceServiceTests.cs`

### Verification

- Create: `scripts/verify-android-device.ps1`
- Create: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`

## Task 0: Create The Final Worktree And Reconfirm Cross-Phase Baseline

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`

- [ ] **Step 1: Create the isolated worktree**

Invoke `superpowers:using-git-worktrees`, create `codex/android-schedule-completion` from updated `origin/master`, and confirm both Phase 1/2 merge commits exist.

- [ ] **Step 2: Run the full baseline**

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run build
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS. Remove only generated Web/build output from this run and keep it unstaged.

- [ ] **Step 3: Mark REL-11 and final verification rows Implementing**

Preserve all Verified Phase 1/2 evidence; add branch and baseline command IDs.

- [ ] **Step 4: Commit the phase marker**

```powershell
git add docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md
git commit -m "docs: start android schedule completion phase"
```

## Task 1: Fix Calendar Recurrence Overlap At The Android Query Boundary

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs`
- Create: `tests/Pim.UnitTests/Calendar/RecurrenceServiceTests.cs`

- [ ] **Step 1: Write a failing recurring-overlap test**

```csharp
[Fact]
public void ExpandEvents_IncludesOccurrenceThatStartsBeforeRangeAndEndsInsideIt()
{
    var service = new RecurrenceService(NullLogger<RecurrenceService>.Instance);
    var source = new EventEntity
    {
        Id = Guid.Parse("11111111-1111-1111-1111-111111111111"),
        DtStart = DateTimeOffset.Parse("2026-07-01T00:00:00Z"),
        DtEnd = DateTimeOffset.Parse("2026-07-01T08:00:00Z"),
        RRule = "FREQ=DAILY"
    };

    var occurrences = service.ExpandEvents(
        [source],
        DateTimeOffset.Parse("2026-07-10T06:00:00Z"),
        DateTimeOffset.Parse("2026-07-10T12:00:00Z"));

    var occurrence = Assert.Single(occurrences);
    Assert.Equal(DateTimeOffset.Parse("2026-07-10T00:00:00Z"), occurrence.OccurrenceStart);
    Assert.Equal(DateTimeOffset.Parse("2026-07-10T08:00:00Z"), occurrence.OccurrenceEnd);
}
```

- [ ] **Step 2: Run the test and verify the missed occurrence**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~RecurrenceServiceTests"
```

Expected: FAIL because occurrence enumeration starts at rangeStart.

- [ ] **Step 3: Search from one source duration before rangeStart and filter overlap**

```csharp
var duration = entity.DtEnd - entity.DtStart;
var occurrenceSearchStart = rangeStart - duration;
var occurrences = calEvent.GetOccurrences(
    new CalDateTime(occurrenceSearchStart.UtcDateTime),
    options);

foreach (var occurrence in occurrences)
{
    var start = new DateTimeOffset(occurrence.Period.StartTime.Value, TimeSpan.Zero);
    if (start >= rangeEnd)
        break;
    var end = new DateTimeOffset(
        occurrence.Period.EndTime?.Value ?? start.Add(duration).UtcDateTime,
        TimeSpan.Zero);
    if (end > rangeStart && start < rangeEnd)
        results.Add(new ExpandedEvent(entity, DeriveOccurrenceId(entity.Id, start), start, end));
}
```

- [ ] **Step 4: Run recurrence and Calendar tests**

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~RecurrenceServiceTests|FullyQualifiedName~Calendar"
```

Expected: PASS for recurring and non-recurring overlap.

- [ ] **Step 5: Commit the overlap contract**

```powershell
git add src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs tests/Pim.UnitTests/Calendar/RecurrenceServiceTests.cs
git commit -m "fix: include overlapping recurring schedules"
```

## Task 2: Build Fresh/Stale Schedule Cache And Parse Diagnostics

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/SchedulePolicyModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleEventSource.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheDao.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheRepositoryTest.kt`

- [ ] **Step 1: Write failing load-state tests**

```kotlin
@Test
fun apiFailureUsesFreshCacheForPolicyButStaleCacheForDisplayOnly() = runTest {
    source.failure = IOException("offline")
    cache.save(windows(), fetchedAtUtcMillis = now - 5.hours.inWholeMilliseconds)
    assertTrue(repository.load(now).canDrivePolicy)

    cache.save(windows(), fetchedAtUtcMillis = now - 7.hours.inWholeMilliseconds)
    val stale = repository.load(now)
    assertFalse(stale.canDrivePolicy)
    assertTrue(stale.isStale)
    assertEquals(ScheduleDataSource.Cache, stale.source)
}

@Test
fun successfulEmptyResponsePersistsFreshEmptyMetadata() = runTest {
    source.events = emptyList()
    val result = repository.load(now)
    val persisted = cache.read()
    assertEquals(ScheduleDataSource.Server, result.source)
    assertTrue(result.windows.isEmpty())
    assertEquals(now, persisted?.fetchedAtUtcMillis)
    assertTrue(persisted?.windows.orEmpty().isEmpty())
}

@Test
fun errorWithoutCacheIsNotReportedAsEmpty() = runTest {
    source.failure = IOException("offline")
    val result = repository.load(now)
    assertEquals(ScheduleDataSource.None, result.source)
    assertEquals("schedule-source-unavailable", result.errorCode)
    assertFalse(result.canDrivePolicy)
}

@Test
fun malformedEventKeepsDiagnosticWhileValidLongEventSurvives() = runTest {
    source.events = listOf(
        EventResponse("bad", "坏数据", location = "上海", dtStart = "not-time", dtEnd = "2026-07-10T09:00:00Z"),
        EventResponse("long", "长会议", location = "上海", dtStart = "2026-07-10T01:00:00Z", dtEnd = "2026-07-10T09:00:00Z"),
        EventResponse("next", "下一场", location = "北京", dtStart = "2026-07-10T10:00:00Z", dtEnd = "2026-07-10T11:00:00Z")
    )
    val result = repository.load(now)
    assertEquals(listOf("long", "next"), result.windows.map { it.id })
    assertEquals("bad", result.parseDiagnostics.single().eventId)
    assertEquals("invalid-start", result.parseDiagnostics.single().code)
    assertEquals(
        (now - 6.hours.inWholeMilliseconds) until (now + 7.days.inWholeMilliseconds),
        source.requestedRange
    )
}

private val now = Instant.parse("2026-07-10T08:00:00Z").toEpochMilli()
private lateinit var database: AppDatabase
private lateinit var cache: ScheduleCacheRepository
private lateinit var source: FakeScheduleEventSource
private lateinit var repository: ScheduleWindowRepository

@Before
fun setUp() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()
    cache = ScheduleCacheRepository(database.scheduleCacheDao())
    source = FakeScheduleEventSource()
    repository = ScheduleWindowRepository(source, cache)
}

@After
fun tearDown() = database.close()

private fun windows() = listOf(
    ScheduleWindow(
        id = "event-1",
        title = "办公室",
        locationText = "上海市黄浦区",
        startsAtMillis = now - 30.minutes.inWholeMilliseconds,
        endsAtMillis = now + 30.minutes.inWholeMilliseconds
    )
)

private class FakeScheduleEventSource : ScheduleEventSource {
    var events: List<EventResponse> = emptyList()
    var failure: Throwable? = null
    var requestedRange: LongRange? = null

    override suspend fun load(startUtcMillis: Long, endUtcMillis: Long): List<EventResponse> {
        requestedRange = startUtcMillis until endUtcMillis
        failure?.let { throw it }
        return events
    }
}
```

The four tests above cover API success-empty, error/no cache, malformed-plus-valid parsing, an event that began seven hours earlier but is still active, and deterministic upcoming order (`startsAtMillis`, then ID).

- [ ] **Step 2: Define complete load models**

```kotlin
enum class ScheduleDataSource { Server, Cache, None }

data class ScheduleParseDiagnostic(
    val eventId: String,
    val code: String,
    val safeMessage: String
)

data class ScheduleLoadResult(
    val windows: List<ScheduleWindow>,
    val source: ScheduleDataSource,
    val fetchedAtUtcMillis: Long?,
    val isStale: Boolean,
    val canDrivePolicy: Boolean,
    val parseDiagnostics: List<ScheduleParseDiagnostic>,
    val errorCode: String?
)

data class CachedScheduleWindowSet(
    val windows: List<ScheduleWindow>,
    val fetchedAtUtcMillis: Long
)

fun interface ScheduleEventSource {
    suspend fun load(startUtcMillis: Long, endUtcMillis: Long): List<EventResponse>
}
```

`ApiScheduleEventSource` is the production adapter over the full Retrofit interface:

```kotlin
class ScheduleSourceException(val code: String, message: String) : IOException(message)

class ApiScheduleEventSource @Inject constructor(
    private val apiService: ApiService
) : ScheduleEventSource {
    override suspend fun load(startUtcMillis: Long, endUtcMillis: Long): List<EventResponse> {
        val response = apiService.getEvents(
            start = Instant.ofEpochMilli(startUtcMillis).toString(),
            end = Instant.ofEpochMilli(endUtcMillis).toString()
        )
        if (response.code != 0) {
            throw ScheduleSourceException(
                code = "calendar-api-${response.code}",
                message = response.message.ifBlank { "日程服务返回错误" }
            )
        }
        return response.data.orEmpty()
    }
}
```

Hilt binds it to `ScheduleEventSource`, keeping tests independent from the full Retrofit interface.

- [ ] **Step 3: Parse without silently dropping evidence**

`ScheduleWindowRepository` returns valid windows plus one diagnostic for blank/invalid location time, invalid ISO, end<=start, and missing ID. A blank location is ignored for policy but is not a parse error. Query interval is `[now-6h, now+7d)` and relies on server overlap semantics.

- [ ] **Step 4: Implement transactional cache replacement and freshness**

On successful API response, replace cached windows and fetchedAt in one Room transaction, including a true empty result. Always insert the Phase 1 metadata row (`cacheKey=__metadata__`, `recordKind=metadata`) and zero or more `recordKind=event` rows; an empty event list therefore remains distinguishable from a missing cache. On error:

- cache age <= 6h: `source=Cache`, `canDrivePolicy=true`;
- cache age > 6h: `source=Cache`, `isStale=true`, `canDrivePolicy=false`;
- no cache: `source=None`, error state;
- never show API error/no cache as `当前没有日程`.

- [ ] **Step 5: Run repository/cache tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.schedule.ScheduleWindowRepositoryTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.schedule.ScheduleCacheRepositoryTest" --no-daemon
```

Expected: PASS for server/cache/empty/error/stale/parse states.

- [ ] **Step 6: Commit schedule data state**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/schedule src/client-android/app/src/main/java/com/pim/app/data/ScheduleCacheDao.kt src/client-android/app/src/test/java/com/pim/app/schedule
git commit -m "feat: cache android schedule policy evidence"
```

## Task 3: Persist Policy Anchor, Recovery, And Transition Evidence

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/location/policy/PolicyRuntimeStore.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/location/policy/PolicyRuntimeStoreTest.kt`

- [ ] **Step 1: Write failing policy-domain event tests**

```kotlin
@Test
fun scheduleEntryAnchorDistanceRecoveryAndExitProduceEvidence() {
    val engine = LocationPolicyEngine(policy)
    val entered = engine.evaluate(input(schedule = schedule, now = 1_000L))
    val anchored = engine.onAcceptedLocation(PolicyLocation(31.2304, 121.4737, 2_000L))
    val recovered = engine.onAcceptedLocation(PolicyLocation(31.2320, 121.4737, 3_000L))
    val exited = engine.evaluate(input(schedule = null, now = 4_000L))

    assertEquals(PolicyTransitionKind.ScheduleEntered, entered.events.single().kind)
    assertEquals(PolicyTransitionKind.AnchorSet, anchored.single().kind)
    assertEquals(PolicyTransitionKind.DistanceRecovery, recovered.single().kind)
    assertTrue(recovered.single().distanceMeters!! > policy.scheduleRecoveryThresholdMeters)
    assertEquals(PolicyTransitionKind.ScheduleEnded, exited.events.single().kind)
}

private val policy = TrackingSettings.defaults().toTrackingPolicy()
private val schedule = ScheduleWindow(
    id = "schedule-1",
    title = "办公室",
    locationText = "上海市黄浦区",
    startsAtMillis = 0L,
    endsAtMillis = 3_500L
)

private fun input(schedule: ScheduleWindow?, now: Long) = LocationPolicyInput(
    nowMillis = now,
    collectionEnabled = true,
    currentScheduleWindow = schedule,
    motionSignal = MotionSignal.Unknown
)

@Test
fun repeatedWindowDoesNotDuplicateEntryAndMotionIsRecordedOnce() {
    val engine = LocationPolicyEngine(policy)
    assertEquals(PolicyTransitionKind.ScheduleEntered, engine.evaluate(input(schedule, 1_000L)).events.single().kind)
    assertTrue(engine.evaluate(input(schedule, 1_100L)).events.isEmpty())
    val motion = engine.evaluate(
        input(schedule, 1_200L).copy(motionSignal = MotionSignal.Walking)
    )
    assertEquals(PolicyTransitionKind.MotionObserved, motion.events.single().kind)
    assertTrue(engine.evaluate(input(schedule, 1_300L).copy(motionSignal = MotionSignal.Walking)).events.isEmpty())
}

@Test
fun restoredSnapshotEmitsOnceAndDisabledCollectionTurnsOff() {
    val original = LocationPolicyEngine(policy).evaluate(input(schedule, 1_000L)).snapshot
    val restored = LocationPolicyEngine(policy)
    assertEquals(PolicyTransitionKind.Restored, restored.restore(original, nowMillis = 1_500L).single().kind)
    assertTrue(restored.restore(original, nowMillis = 1_600L).isEmpty())
    val disabled = restored.evaluate(
        input(schedule, 1_700L).copy(collectionEnabled = false)
    )
    assertEquals(LocationPolicyMode.Off, disabled.decision.mode)
}

@Test
fun staleScheduleEvidenceFallsBackToNormalInterval() {
    val evaluation = LocationPolicyEngine(policy).evaluate(input(schedule = null, now = 2_000L))
    assertEquals(LocationPolicyMode.PowerSavingNormal, evaluation.decision.mode)
    assertEquals(policy.normalIntervalMillis, evaluation.decision.requestIntervalMillis)
}
```

In the existing `sameScheduleIdWithChangedWindowResetsRecoveryState` test, replace the final decision assertion with:

```kotlin
val evaluation = engine.evaluate(
    LocationPolicyInput(
        nowMillis = now + 71_000L,
        collectionEnabled = true,
        currentScheduleWindow = updatedSameIdSchedule
    )
)
assertEquals(LocationPolicyMode.ScheduleLowFrequency, evaluation.decision.mode)
assertEquals(PolicyTransitionKind.ScheduleEntered, evaluation.events.single().kind)
assertEquals("上海市徐汇区", evaluation.snapshot.scheduleLocation)
```

The tests above cover motion de-duplication, process restore, disabled collection, and stale-cache normal fallback.

- [ ] **Step 2: Define persistent runtime and transition contracts**

```kotlin
enum class PolicyTransitionKind {
    ScheduleEntered, AnchorSet, MotionObserved, DistanceRecovery, ScheduleEnded, Restored
}

data class PolicyRuntimeSnapshot(
    val mode: LocationPolicyMode,
    val scheduleId: String?,
    val scheduleTitle: String?,
    val scheduleLocation: String?,
    val scheduleStartsAtUtcMillis: Long?,
    val scheduleEndsAtUtcMillis: Long?,
    val anchorLatitude: Double?,
    val anchorLongitude: Double?,
    val anchorAtUtcMillis: Long?,
    val recoveryActive: Boolean,
    val reason: String,
    val savedAtUtcMillis: Long
)

data class PolicyTransitionEvent(
    val kind: PolicyTransitionKind,
    val fromMode: LocationPolicyMode?,
    val toMode: LocationPolicyMode,
    val occurredAtUtcMillis: Long,
    val reason: String,
    val anchorLatitude: Double?,
    val anchorLongitude: Double?,
    val distanceMeters: Double?,
    val details: Map<String, String>
)
```

- [ ] **Step 3: Make policy evaluation return decision plus events**

```kotlin
data class PolicyEvaluation(
    val decision: PolicyDecision,
    val snapshot: PolicyRuntimeSnapshot,
    val events: List<PolicyTransitionEvent>
)
```

`evaluate()` replaces callers of `reduce()` after compatibility tests pass. `onAcceptedLocation()` returns anchor/recovery events. `restore(snapshot)` validates schedule identity/time and emits `Restored` once.

- [ ] **Step 4: Persist runtime snapshot and detailed Room transitions**

`PolicyRuntimeStore` uses one SharedPreferences object for the latest snapshot. `LocationQueueRepository.recordPolicyTransition(event)` writes existing v4 `mobile_location_policy_transitions` fields plus `details_json`, anchor latitude/longitude, and distance columns created by the Phase 1 migration.

- [ ] **Step 5: Run policy tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.policy.*" --no-daemon
```

Expected: PASS and every requested transition has persistent facts.

- [ ] **Step 6: Commit policy evidence**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/location src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt src/client-android/app/src/test/java/com/pim/app/location/policy
git commit -m "feat: persist android schedule policy transitions"
```

## Task 4: Refresh Schedule At Service, Foreground, Interval, And Boundary Triggers

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleRefreshPlanner.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/SchedulePolicyCoordinator.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/startup/AppForegroundObserver.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleRefreshPlannerTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/SchedulePolicyCoordinatorTest.kt`

- [ ] **Step 1: Write failing refresh-planner tests**

```kotlin
@Test
fun nextRefreshUsesEarlierOfThirtyMinutesAndKnownBoundary() {
    assertEquals(
        10.minutes.inWholeMilliseconds,
        planner.nextDelay(
            nowUtcMillis = 1_000L,
            activeCollection = true,
            nextBoundaryUtcMillis = 1_000L + 10.minutes.inWholeMilliseconds
        )
    )
    assertEquals(
        30.minutes.inWholeMilliseconds,
        planner.nextDelay(1_000L, activeCollection = true, nextBoundaryUtcMillis = null)
    )
    assertNull(planner.nextDelay(1_000L, activeCollection = false, nextBoundaryUtcMillis = 2_000L))
}

private val planner = ScheduleRefreshPlanner()

@Test
fun serviceForegroundAndManualTriggersLoadImmediately() = runTest {
    val calls = mutableListOf<Long>()
    val coordinator = SchedulePolicyCoordinator(
        load = ScheduleLoadOperation { now -> calls += now; freshScheduleResult(now) },
        nowMillis = { 5_000L }
    )
    coordinator.refresh(ScheduleRefreshTrigger.ServiceStart)
    coordinator.refresh(ScheduleRefreshTrigger.AppForeground)
    coordinator.refresh(ScheduleRefreshTrigger.Manual)
    assertEquals(listOf(5_000L, 5_000L, 5_000L), calls)
}

@Test
fun staleCacheIsDisplayOnlyAndServerRecoveryRestoresPolicyWindows() = runTest {
    val window = ScheduleWindow("event-1", "办公室", "上海", 4_000L, 6_000L)
    val results = ArrayDeque(listOf(
        ScheduleLoadResult(listOf(window), ScheduleDataSource.Cache, 0L, true, false, emptyList(), "offline"),
        ScheduleLoadResult(listOf(window), ScheduleDataSource.Server, 5_000L, false, true, emptyList(), null)
    ))
    val coordinator = SchedulePolicyCoordinator(
        load = ScheduleLoadOperation { results.removeFirst() },
        nowMillis = { 5_000L }
    )

    coordinator.refresh(ScheduleRefreshTrigger.ActiveInterval)
    assertEquals(listOf(window), coordinator.state.value.windows)
    assertTrue(coordinator.policyWindows.value.isEmpty())
    coordinator.refresh(ScheduleRefreshTrigger.ActiveInterval)
    assertEquals(listOf(window), coordinator.policyWindows.value)
}

@Test
fun concurrentRefreshesShareOneLoad() = runTest {
    val entered = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var calls = 0
    val coordinator = SchedulePolicyCoordinator(
        load = ScheduleLoadOperation {
            calls++
            entered.complete(Unit)
            release.await()
            freshScheduleResult(5_000L)
        },
        nowMillis = { 5_000L }
    )

    val first = async { coordinator.refresh(ScheduleRefreshTrigger.ActiveInterval) }
    entered.await()
    val second = async { coordinator.refresh(ScheduleRefreshTrigger.ActiveInterval) }
    yield()
    assertEquals(1, calls)
    release.complete(Unit)
    assertEquals(first.await(), second.await())
}

private fun freshScheduleResult(now: Long) = ScheduleLoadResult(
    windows = emptyList(),
    source = ScheduleDataSource.Server,
    fetchedAtUtcMillis = now,
    isStale = false,
    canDrivePolicy = true,
    parseDiagnostics = emptyList(),
    errorCode = null
)
```

These tests cover immediate triggers, disabled periodic scheduling, stale display-only cache, server recovery, and concurrent coalescing.

- [ ] **Step 2: Implement one coordinator for all query triggers**

```kotlin
enum class ScheduleRefreshTrigger { ServiceStart, AppForeground, ActiveInterval, KnownBoundary, Manual }

fun interface ScheduleLoadOperation {
    suspend fun load(nowUtcMillis: Long): ScheduleLoadResult
}
```

`SchedulePolicyCoordinator(load: ScheduleLoadOperation, nowMillis: () -> Long)` implements `suspend fun refresh(trigger): ScheduleLoadResult`, coalesces concurrent calls, publishes `StateFlow<ScheduleLoadResult>` as `state`, and publishes `StateFlow<List<ScheduleWindow>>` as `policyWindows` containing windows only when `canDrivePolicy=true`. Production DI passes `ScheduleLoadOperation(repository::load)`. It logs parse/API/cache facts without credentials.

- [ ] **Step 3: Replace one-shot service schedule loading**

Remove `ForegroundLocationService.refreshScheduleWindows()` and its +24h range. At service start collect coordinator state and schedule the next refresh. While collection is active, refresh every 30 minutes or at the earlier known schedule start/end boundary. A stale/no cache causes normal interval, never low-frequency mode.

- [ ] **Step 4: Publish a complete runtime snapshot**

Extend `ForegroundLocationRuntimeState` with current schedule ID/title/location/start/end, anchor text/time, decision reason, exit conditions, latest transition, and schedule freshness. Persist transitions before publishing state.

- [ ] **Step 5: Trigger foreground refresh without duplicate sync requests**

`AppForegroundObserver` calls both schedule coordinator and the Phase 1 sync foreground request. Each keeps its own coalescing/cooldown; Activity recreation must not duplicate either.

- [ ] **Step 6: Run schedule coordinator and service tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.schedule.ScheduleRefreshPlannerTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.schedule.SchedulePolicyCoordinatorTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.policy.LocationPolicyEngineTest" --no-daemon
```

Expected: PASS for all triggers, stale fallback, coalescing, and boundary timing.

- [ ] **Step 7: Commit schedule/service integration**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/schedule src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/main/java/com/pim/app/startup src/client-android/app/src/test/java/com/pim/app/schedule
git commit -m "feat: refresh android schedule policy reliably"
```

## Task 5: Replace The Native Schedule Placeholder With Real Evidence

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`
- Replace: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/ui/schedule/SchedulePolicyContentTest.kt`

- [ ] **Step 1: Write failing Compose tests for all page states**

```kotlin
@Test
fun staleCacheIsVisibleButNeverClaimsLowFrequencyPolicy() {
    compose.setContent {
        SchedulePolicyContent(
            state = scheduleState(source = ScheduleDataSource.Cache, stale = true, canDrivePolicy = false),
            onRefresh = {}
        )
    }

    compose.onNodeWithText("缓存已过期").assertExists()
    compose.onNodeWithText("当前使用常规采集间隔").assertExists()
    compose.onNodeWithText("当前没有日程").assertDoesNotExist()
}

@Test
fun loadingAndFreshEmptyHaveDifferentEvidence() {
    compose.setContent { SchedulePolicyContent(ScheduleScreenState.Loading, onRefresh = {}) }
    compose.onNodeWithText("正在加载日程").assertExists()
}

@Test
fun freshEmptyIsTheOnlyTrueEmptyState() {
    compose.setContent { SchedulePolicyContent(ScheduleScreenState.Empty(1_000L), onRefresh = {}) }
    compose.onNodeWithText("当前没有日程").assertExists()
    compose.onNodeWithText("数据时间：", substring = true).assertExists()
}

@Test
fun partialAndErrorExposeDiagnosticsAndRetry() {
    val partial = ScheduleScreenState.Partial(
        scheduleState(ScheduleDataSource.Server, stale = false, canDrivePolicy = true).model,
        listOf(ScheduleParseDiagnostic("bad-event", "invalid-start", "开始时间无效"))
    )
    compose.setContent { SchedulePolicyContent(partial, onRefresh = {}) }
    compose.onNodeWithText("部分日程无法解析").assertExists()
    compose.onNodeWithText("开始时间无效").assertExists()
}

@Test
fun apiErrorWithoutCacheNeverLooksEmpty() {
    compose.setContent {
        SchedulePolicyContent(
            ScheduleScreenState.Error("schedule-source-unavailable", "日程服务不可用", staleModel = null),
            onRefresh = {}
        )
    }
    compose.onNodeWithText("日程服务不可用").assertExists()
    compose.onNodeWithText("重试").assertExists()
    compose.onNodeWithText("当前没有日程").assertDoesNotExist()
}

@Test
fun contentShowsCurrentUpcomingAndTransitionEvidence() {
    val base = scheduleState(ScheduleDataSource.Server, stale = false, canDrivePolicy = true).model
    val content = ScheduleScreenState.Content(
        base.copy(
            currentEvent = ScheduleEventUi("event-1", "办公室", "黄浦区", 500L, 1_500L),
            transitions = listOf(
                ScheduleTransitionUi(PolicyTransitionKind.DistanceRecovery, 900L, "移动超过恢复距离", 128.4)
            )
        )
    )
    compose.setContent { SchedulePolicyContent(content, onRefresh = {}) }
    compose.onNodeWithText("办公室").assertExists()
    compose.onNodeWithText("黄浦区").assertExists()
    compose.onNodeWithText("客户现场").assertExists()
    compose.onNodeWithText("移动超过恢复距离").assertExists()
    compose.onNodeWithText("128.4 米").assertExists()
}

private fun scheduleState(
    source: ScheduleDataSource,
    stale: Boolean,
    canDrivePolicy: Boolean
): ScheduleScreenState.Content = ScheduleScreenState.Content(
    SchedulePolicyUiModel(
        source = source,
        fetchedAtUtcMillis = 1_000L,
        isStale = stale,
        canDrivePolicy = canDrivePolicy,
        currentEvent = null,
        nextEvent = ScheduleEventUi("event-2", "客户现场", "浦东新区", 2_000L, 3_000L),
        upcomingEvents = emptyList(),
        mode = if (canDrivePolicy) LocationPolicyMode.ScheduleLowFrequency else LocationPolicyMode.PowerSavingNormal,
        intervalMillis = if (canDrivePolicy) 600_000L else 120_000L,
        anchorText = null,
        entryReason = if (canDrivePolicy) "命中当前日程" else "日程证据已过期",
        exitConditions = listOf("日程结束", "移动超过恢复距离"),
        transitions = emptyList()
    )
)
```

These tests cover Loading, fresh Empty, current/upcoming content, transition history, Partial parse diagnostics, API error/no cache, and retry.

- [ ] **Step 2: Define the UI state explicitly**

```kotlin
sealed interface ScheduleScreenState {
    data object Loading : ScheduleScreenState
    data class Content(val model: SchedulePolicyUiModel) : ScheduleScreenState
    data class Empty(val fetchedAtUtcMillis: Long) : ScheduleScreenState
    data class Partial(val model: SchedulePolicyUiModel, val diagnostics: List<ScheduleParseDiagnostic>) : ScheduleScreenState
    data class Error(
        val code: String,
        val safeMessage: String,
        val staleModel: SchedulePolicyUiModel?
    ) : ScheduleScreenState
}

data class ScheduleEventUi(
    val id: String,
    val title: String,
    val location: String,
    val startsAtUtcMillis: Long,
    val endsAtUtcMillis: Long
)

data class ScheduleTransitionUi(
    val kind: PolicyTransitionKind,
    val occurredAtUtcMillis: Long,
    val reason: String,
    val distanceMeters: Double?
)

data class SchedulePolicyUiModel(
    val source: ScheduleDataSource,
    val fetchedAtUtcMillis: Long,
    val isStale: Boolean,
    val canDrivePolicy: Boolean,
    val currentEvent: ScheduleEventUi?,
    val nextEvent: ScheduleEventUi?,
    val upcomingEvents: List<ScheduleEventUi>,
    val mode: LocationPolicyMode,
    val intervalMillis: Long,
    val anchorText: String?,
    val entryReason: String,
    val exitConditions: List<String>,
    val transitions: List<ScheduleTransitionUi>
)
```

- [ ] **Step 3: Build ViewModel from coordinator, runtime, and transitions**

The model includes current event or accurate none, next event, upcoming location-bearing events, interval/mode/anchor/reason/exit conditions, fetchedAt/source/stale, and recent entry/motion/recovery/end/restore transitions. Map UTC facts to device-local display.

- [ ] **Step 4: Implement the native screen**

Use unframed vertical sections in this order:

1. current policy conclusion and freshness;
2. current/next location-bearing schedule;
3. interval, anchor, entry reason, exit conditions;
4. upcoming schedules;
5. recent transitions;
6. parse/API/cache diagnostics with retry.

Do not render fixed `当前没有日程`; only a successful fresh empty query may produce that state.

- [ ] **Step 5: Run Schedule Compose tests**

```powershell
Set-Location src/client-android
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.ui.schedule.SchedulePolicyContentTest --no-daemon
```

Expected: PASS for every explicit state and action.

- [ ] **Step 6: Commit the Schedule screen**

```powershell
git add src/client-android/app/src/main/java/com/pim/app/ui/schedule src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt src/client-android/app/src/androidTest/java/com/pim/app/ui/schedule
git commit -m "feat: show real android schedule policy"
```

## Task 6: Add Automated AVD, Boot, Update, And Evidence Collection Script

**Files:**
- Create: `scripts/verify-android-device.ps1`
- Create: `src/client-android/app/src/androidTest/java/com/pim/app/lifecycle/BootAndUpdateRecoveryTest.kt`
- Test: `scripts/verify-android-device.ps1`

- [ ] **Step 1: Write failing lifecycle instrumentation assertions**

`BootAndUpdateRecoveryTest` invokes the recovery coordinator with BootCompleted/AppReplaced/Foreground triggers against real WorkManager test state and Room. Assert one canonical periodic request, retained collection intent, stale lease interruption, and exact action-required record.

- [ ] **Step 2: Implement strict script parameters and tool discovery**

```powershell
param(
    [Parameter(Mandatory=$true)][string]$Apk,
    [string]$Serial,
    [ValidateSet('fresh','upgrade')][string]$Mode = 'fresh',
    [ValidateSet('Pixel_9','Pixel_Tablet','physical')][string]$Target = 'Pixel_9',
    [string]$ServerUrl = 'http://10.0.2.2:5858/api/v1/'
)
```

Resolve SDK at `$env:ANDROID_HOME` or `C:\Users\a2746\AppData\Local\Android\Sdk`; resolve JBR at `$env:JAVA_HOME` or `C:\Program Files\Android\Android Studio\jbr`. Fail with a precise message when APK/tool/target is missing.

- [ ] **Step 3: Automate AVD boot and installation safely**

For AVD targets: start hidden emulator process, wait for `adb wait-for-device` and `sys.boot_completed=1`, unlock, then install. Fresh mode uses `adb uninstall com.pim.app` followed by install; upgrade uses `adb install -r` and never clears app data. Physical mode never starts an emulator and requires an explicit serial.

- [ ] **Step 4: Run instrumentation and lifecycle probes**

Run:

```text
& $AdbPath -s $Serial shell am instrument -w com.pim.app.test/androidx.test.runner.AndroidJUnitRunner
& $AdbPath -s $Serial shell am kill com.pim.app
& $AdbPath -s $Serial shell monkey -p com.pim.app 1
& $AdbPath -s $Serial reboot
```

After reboot, wait for unlock/boot before inspecting. Record `am kill` separately from `am force-stop`; force-stop intentionally suppresses receivers until user launch and must not be described as ordinary process death.

- [ ] **Step 5: Collect only non-secret evidence**

Write a local timestamped directory outside Git with app version, package permission states, process/service status, instrumentation output, WorkManager names exposed through the app's diagnostic status, and screenshots requested by the operator. Do not collect passwords, tokens, Authorization headers, raw diagnostic ZIP, or raw coordinates.

- [ ] **Step 6: Dry-run script validation and run both AVDs**

```powershell
& scripts/verify-android-device.ps1 -Apk src/client-android/app/build/outputs/apk/debug/app-debug.apk -Target Pixel_9 -Mode fresh
& scripts/verify-android-device.ps1 -Apk src/client-android/app/build/outputs/apk/debug/app-debug.apk -Target Pixel_Tablet -Mode fresh
```

Expected: both AVDs boot, install, pass instrumentation; Pixel_9 covers interactions/cold boot, Pixel_Tablet covers layout sanity.

- [ ] **Step 7: Commit script and lifecycle tests**

```powershell
git add scripts/verify-android-device.ps1 src/client-android/app/src/androidTest/java/com/pim/app/lifecycle/BootAndUpdateRecoveryTest.kt
git commit -m "test: automate android lifecycle verification"
```

## Task 7: Run All Automated Gates And Open A Draft PR For Signed APK

**Files:**
- Create: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`

- [ ] **Step 1: Run the full backend gate**

```powershell
dotnet test Pim.sln
```

Expected: PASS; record total tests and duration.

- [ ] **Step 2: Run the full Web gate**

```powershell
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run test:schedule-workbench-complete
npm --prefix src/client-web run build
```

Expected: PASS. Keep generated wwwroot unstaged.

- [ ] **Step 3: Run the full Android gate**

```powershell
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS on Pixel_9; local release signing status is recorded explicitly.

- [ ] **Step 4: Scan production placeholder and stale-contract patterns**

```powershell
rg -n "地图预览将在这里|停留：0 次|轨迹片段将在这里|当前没有带位置信息的日程|pim_mobile_background_sync|pim_upload|pendingLogCount" src/client-android/app/src/main
```

Expected: no matches except obsolete-name migration constants when that search is run separately with context.

- [ ] **Step 5: Create the verification report with fixed sections**

Include Environment, Commits, Automated Commands, Pixel_9, Pixel_Tablet, Signed Artifacts, Physical Device Matrix, GitHub Actions, Coverage Closure, and Private Evidence Handling. Enter executed facts only; physical rows remain `Blocked: no connected physical device` until tested.

- [ ] **Step 6: Commit automated evidence**

```powershell
git add docs/superpowers/reports
git commit -m "docs: record android automated completion evidence"
```

- [ ] **Step 7: Push and open a draft PR**

```powershell
git push -u origin codex/android-schedule-completion
gh pr create --draft --base master --head codex/android-schedule-completion --title "feat: complete android schedule and reliability" --body-file docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md
gh pr checks --watch
```

Expected: Android/API checks trigger and pass; Web triggers only if Phase 3 touches Web files. Record any path-filter non-trigger.

- [ ] **Step 8: Download the signed PR APK**

```powershell
$runId = gh run list --workflow build-android.yml --branch codex/android-schedule-completion --limit 1 --json databaseId --jq '.[0].databaseId'
gh run download $runId --dir build/signed-pr-apk
Get-ChildItem build/signed-pr-apk -Recurse -Filter *.apk
```

Expected: exactly one signed CI APK artifact with a higher versionCode than current master.

## Task 8: Execute The Signed-APK Physical-Device Matrix

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`

- [ ] **Step 1: Stop here when no physical device is connected**

```powershell
& 'C:\Users\a2746\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
```

Expected before continuing: exactly one explicitly selected physical serial. An empty list is a real blocker; do not mark the Goal complete or replace these steps with emulator evidence.

- [ ] **Step 2: Download the current master signed APK for upgrade baseline**

```powershell
$masterRun = gh run list --workflow build-android.yml --branch master --status success --limit 1 --json databaseId --jq '.[0].databaseId'
gh run download $masterRun --dir build/signed-master-apk
Get-ChildItem build/signed-master-apk -Recurse -Filter *.apk
```

Verify both APK signatures match with `apksigner verify --print-certs` and PR versionCode is greater.

- [ ] **Step 3: Verify fresh install, API, auth, and permissions**

Use the signed PR APK. Record API save/real probe; login/logout/expiry/refresh; each permission denial/grant/settings return; provider/battery state; collection start/stop/service restart. Never write credentials into the report.

- [ ] **Step 4: Verify collection quality and schedule policy**

Record `<50m` accepted, `>=50m` rejected, missing-altitude timeout, schedule low-frequency entry, anchor, motion/distance recovery, schedule exit, no event, upcoming event, stale cache, and server failure.

- [ ] **Step 5: Verify every transfer path and visible reaction**

Record manual, foreground, approximate 15-minute periodic, offline waiting, allowed-network recovery, unmetered waiting/one-run override, timeout, 5xx, 401 refresh, partial acknowledgement, dead letter, heartbeat failure after confirmed data, and next retry estimate.

- [ ] **Step 6: Verify Today, Tracks, and diagnostics**

Record public server map/server timestamp, filter/segment/raw pagination, Web auth/error/retry, post-sync refresh, diagnostic ZIP creation/share/unzip/raw coordinate presence/credential absence. Keep the ZIP private and delete the temporary shared copy after inspection.

- [ ] **Step 7: Verify process death, reboot, and app update separately**

1. normal background then `am kill`: enabled collection recovers;
2. phone reboot then unlock: canonical worker exists and collection resumes or exact action appears;
3. install current master signed APK, seed pending business rows/settings, then `adb install -r` PR APK: data/settings remain and old logs no longer inflate queue;
4. document `force-stop` as Android's explicit stopped-package state and verify visible launch reconciliation, without claiming boot receiver ran while force-stopped.

- [ ] **Step 8: Verify accessibility and narrow layout**

Test larger font, narrow portrait, rotation, screen off/background, and repeated tab navigation; no text overlap, clipped buttons, blank maps, or nested-scroll traps.

- [ ] **Step 9: Complete the physical report rows with evidence references**

Every row records signed APK version/hash, device model/Android version, timestamp, result, and safe evidence filename. Do not commit raw coordinates, tokens, logs containing private data, or APK binaries.

## Task 9: Close Coverage, Update The Draft PR, And Observe Final Checks

**Files:**
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md`
- Modify: `docs/superpowers/reports/2026-07-10-android-client-complete-reliability-verification.md`

- [ ] **Step 1: Re-run all automated commands after physical-evidence edits**

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run test:android-embed
npm --prefix src/client-web run build
Set-Location src/client-android
.\gradlew.bat testDebugUnitTest --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Expected: PASS.

- [ ] **Step 2: Close all 16 coverage rows**

Change REL-11 through REL-16 to Verified only after evidence exists. Scan Status for forbidden remaining values:

```powershell
Select-String -Path docs/superpowers/reports/2026-07-10-android-client-complete-reliability-coverage.md -Pattern 'Planned|Implementing|Blocked|Unverified'
```

Expected: no matches.

- [ ] **Step 3: Validate report privacy and Git scope**

```powershell
git status --short --branch
git diff --check origin/master...HEAD
git diff --name-only origin/master...HEAD
```

Expected: source/tests/scripts/docs only; no build, wwwroot, APK, ZIP, diagnostic JSONL, raw coordinate screenshot, token, or `.opencode/` entry.

- [ ] **Step 4: Commit final evidence**

```powershell
git add docs/superpowers/reports
git commit -m "docs: verify complete android client reliability"
git push
```

- [ ] **Step 5: Wait for the new signed artifact and relevant checks**

```powershell
gh pr checks --watch
```

If the final docs-only push triggers no new workflow by path filters, record that and retain the earlier green source commit checks plus final commit hash.

- [ ] **Step 6: Mark the PR ready only after signed-device evidence remains valid**

```powershell
gh pr ready
gh pr view --json url,state,isDraft,statusCheckRollup
```

Expected: ready PR, all relevant checks green, no incomplete coverage row.

## Final Completion Gate

The program is complete only when:

- Schedule uses real current/next/upcoming events, cache freshness, runtime policy, anchor and transitions;
- recurring overlap test passes;
- production placeholder scan is clean;
- full .NET, Web, Android JVM, Android instrumentation and release commands pass;
- Pixel_9 cold-boot/update tests and Pixel_Tablet layout sanity pass;
- signed current→new APK upgrade preserves business data/settings;
- the user's physical phone passes every matrix row;
- diagnostic ZIP proves raw-coordinate presence and credential absence while staying private;
- all relevant GitHub Actions are green or exact path-filter non-trigger is recorded;
- coverage report has all 16 rows Verified.
