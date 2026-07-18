# Android 客户端 Stage 3 日程与采集策略实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不增加 Room 日程表、独立刷新 Worker 或 WebView 日程页的前提下，交付由真实数据驱动的 Android 日程页、日程缓存、采集策略可见性和诊断事实。

**Architecture:** `ScheduleWindowRepository` 负责 `[now-6h, now+7d)` 查询、按服务器隔离的单份 JSON 缓存和单 Mutex 刷新；原生日程 `ViewModel` 与 `ForegroundLocationService` 读取同一快照。现有 `LocationPolicyEngine` 负责策略判定，状态中心和诊断导出只消费运行时/缓存事实，不创建第二套状态源。

**Tech Stack:** Kotlin、Jetpack Compose、Hilt、Kotlin Coroutines/StateFlow、kotlinx.serialization、Android app-private files、Room 现有 DAO、JUnit/Robolectric、Gradle。

**Design reference:** `docs/superpowers/specs/2026-07-18-android-client-stage-3-schedule-policy-design.md`（已由用户确认）。

---

## 文件边界总览

新增：

- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheStore.kt`：缓存 JSON DTO、服务器隔离、原子写入和清理。
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyUiState.kt`：五种页面状态及展示模型。
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`：刷新、状态映射和重试。
- `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheStoreTest.kt`。
- `src/client-android/app/src/test/java/com/pim/app/ui/schedule/SchedulePolicyViewModelTest.kt`。

修改：

- `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- `src/client-android/app/src/main/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepository.kt`
- 对应现有 schedule、policy、status、diagnostic 测试。

不修改：Calendar API、Room schema/migrations、WebUI 路由、Stage 1 同步协议和认证协议。

---

### Task 1: 统一策略间隔与日程边界

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsValidatorTest.kt`

- [x] **Step 1: 写失败测试，锁定已确认行为。** 在 `LocationPolicyEngineTest` 增加：无地点活动日程进入低频；车载/骑行使用运动间隔的一半且最低 30 秒；开始边界包含、结束边界排除；距离恢复时静止用常规间隔、运动用派生间隔。

```kotlin
@Test
fun `active schedule without location still enters low frequency`() {
    val window = ScheduleWindow("event", "会议", "", 1_000L, 2_000L)
    val decision = engine.reduce(input(now = 1_500L, schedule = window))
    assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
    assertTrue(decision.scheduleLowFrequency)
}

@Test
fun `vehicle uses half movement interval but never below thirty seconds`() {
    val decision = LocationPolicyEngine(TrackingPolicy(movementIntervalMillis = 60_000L))
        .reduce(input(motion = MotionSignal.InVehicle))
    assertEquals(30_000L, decision.requestIntervalMillis)
}
```

- [x] **Step 2: 运行失败测试，确认失败来自旧逻辑。**

```powershell
cd src/client-android
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationPolicyEngineTest" --no-daemon
```

预期：无地点日程仍返回普通模式，车载仍使用完整运动间隔。

- [x] **Step 3: 实现最小策略改动。** 在 `LocationPolicyTypes.kt` 增加统一范围和派生函数：

```kotlin
object TrackingIntervalBounds {
    const val NORMAL_MIN_MILLIS = 60_000L
    const val NORMAL_MAX_MILLIS = 900_000L
    const val SCHEDULE_MIN_MILLIS = 300_000L
    const val SCHEDULE_MAX_MILLIS = 3_600_000L
    const val MOVEMENT_MIN_MILLIS = 30_000L
    const val MOVEMENT_MAX_MILLIS = 300_000L
}

fun TrackingPolicy.movementIntervalFor(signal: MotionSignal): Long = when (signal) {
    MotionSignal.OnBicycle, MotionSignal.InVehicle ->
        (movementIntervalMillis / 2L).coerceAtLeast(TrackingIntervalBounds.MOVEMENT_MIN_MILLIS)
    else -> movementIntervalMillis
}.coerceIn(
    TrackingIntervalBounds.MOVEMENT_MIN_MILLIS,
    TrackingIntervalBounds.MOVEMENT_MAX_MILLIS
)
```

移除 `ScheduleWindow.isActiveAt()`、`ScheduleWindowSelector.current()` 和 `upcoming()` 中的地点过滤；保留时间边界。`LocationPolicyEngine` 的运动和恢复分支使用 `movementIntervalFor()`，低频原因改为“当前日程时段，降低定位频率”。`TrackingSettingsValidator` 改用同一组常量。

- [x] **Step 4: 运行绿色测试并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LocationPolicyEngineTest" --tests "*TrackingSettingsValidator*" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/location/policy src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt src/client-android/app/src/test/java/com/pim/app/location/policy src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsValidatorTest.kt
git commit -m "feat: make schedule policy intervals factual"
```

---

### Task 2: 建立按服务器隔离的单份 JSON 缓存

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheStore.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheStoreTest.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt`

- [x] **Step 1: 写失败测试。** 使用测试 Context 的临时 filesDir，覆盖成功读写、成功空列表、损坏 JSON 返回 null、server identity 隔离、失败元数据不覆盖 windows、清理删除文件。

```kotlin
@Test
fun `server identities never share cache`() {
    store.write("http://one:5858", document(windows = listOf(window("one"))))
    store.write("http://two:5858", document(windows = listOf(window("two"))))
    assertEquals("one", store.read("http://one:5858")!!.windows.single().title)
    assertEquals("two", store.read("http://two:5858")!!.windows.single().title)
}

@Test
fun `corrupt json is treated as missing`() {
    store.cacheFile("http://one:5858").writeText("not-json")
    assertNull(store.read("http://one:5858"))
}
```

- [x] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ScheduleCacheStoreTest" --no-daemon
```

预期：类和 DTO 尚不存在，测试编译失败。

- [x] **Step 3: 实现缓存 DTO 和存储。** 使用 primitive `@Serializable` DTO：

```kotlin
@Serializable
data class ScheduleCacheDocument(
    val windows: List<ScheduleCacheWindow> = emptyList(),
    val rangeStartMillis: Long = 0L,
    val rangeEndMillis: Long = 0L,
    val lastAttemptAtMillis: Long? = null,
    val lastSuccessAtMillis: Long? = null,
    val lastError: String? = null,
    val lastErrorKind: String? = null
)

@Serializable
data class ScheduleCacheWindow(
    val id: String,
    val title: String,
    val locationText: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long
)
```

`ScheduleCacheStore(@ApplicationContext Context, Json)` 将文件放在 `filesDir/schedule-cache/`，文件名使用 server URL 的 SHA-256 十六进制摘要。`write()` 先写 `.tmp` 再替换正式文件；`read()` 损坏时返回 null；`clear(serverIdentity)` 和 `clearAll()` 仅操作该目录。测试可通过 internal 构造函数传入临时目录，生产 API 不暴露文件路径。

- [x] **Step 4: 在 Hilt 注册单例并运行绿色测试。**

```kotlin
@Provides
@Singleton
fun provideScheduleCacheStore(
    @ApplicationContext context: Context,
    json: Json
): ScheduleCacheStore = ScheduleCacheStore(context, json)
```

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ScheduleCacheStoreTest" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleCacheStore.kt src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleCacheStoreTest.kt src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt
git commit -m "feat: add server-scoped schedule cache"
```

---

### Task 3: 让 Repository 成为唯一刷新与快照入口

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt`

- [x] **Step 1: 写失败测试。** 覆盖：`mapEvents()` 保留空地点事件；范围固定为 `now-6h` 到 `now+7d`；成功空列表为正常空；失败有缓存返回 stale；失败无缓存返回 missing/error；并发 `refreshIfStale()` 只调用一次 API。

```kotlin
@Test
fun `mapEvents keeps parseable event without location`() {
    val windows = ScheduleWindowRepository.mapEvents(listOf(event(location = null)))
    assertEquals(1, windows.size)
    assertEquals("", windows.single().locationText)
}

@Test
fun `stale refresh keeps last successful windows`() = runTest {
    repository.refreshIfStale(nowMillis = 1_000L)
    api.failNext = true
    val snapshot = repository.refreshIfStale(force = true, nowMillis = 2_000L)
    assertEquals(1, snapshot.windows.size)
    assertEquals(ScheduleCacheFreshness.Stale, snapshot.freshness)
}
```

- [x] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ScheduleWindowRepositoryTest" --no-daemon
```

- [x] **Step 3: 实现快照类型和刷新算法。** 在 repository 同文件定义：

```kotlin
enum class ScheduleCacheFreshness { Fresh, Stale, Missing }
enum class ScheduleRefreshErrorKind { Authentication, Network, Server, Cache }

data class ScheduleCacheSnapshot(
    val serverIdentity: String,
    val windows: List<ScheduleWindow>,
    val freshness: ScheduleCacheFreshness,
    val lastAttemptAtMillis: Long?,
    val lastSuccessAtMillis: Long?,
    val lastError: String?,
    val errorKind: ScheduleRefreshErrorKind?
)
```

Repository 注入 `ApiService`、`ScheduleCacheStore`、`ServerSettingsStore`；持有 `MutableStateFlow<ScheduleCacheSnapshot>` 和一个 `Mutex`。`refreshIfStale(force, nowMillis)` 读取当前 URL 作为 server identity；15 分钟内且没有 force 时直接返回。否则调用现有 `apiService.getEvents(start, end)`，查询 `[now-6h, now+7d)`，映射全部可解析事件并写缓存。失败只更新尝试时间/固定中文错误，保留成功 windows；原始 exception、URL 和机器码不进入 UI snapshot。

401/认证失败映射为 `Authentication`，无连接/超时映射为 `Network`，非零业务响应和 5xx 映射为 `Server`，损坏缓存映射为 `Cache`；UI 只读取枚举和固定中文。

保留 `loadWindows(startMillis,endMillis)` 作为纯 API 测试入口，但 service 不再传 24 小时范围。`currentWindow()`、`upcomingWindows()` 改为普通函数。

- [x] **Step 4: 将清理接入设置动作。** `SettingsViewModel` 注入 `ScheduleCacheStore`。保存新服务器地址前记录旧 identity，保存成功后清理旧 identity；`logout()` 成功清 token 后清理当前 identity。扩展 `SettingsServerMutationTest`，证明 token 清理失败时不误清缓存、成功切换只清旧服务器文件。

- [x] **Step 5: 运行绿色测试并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ScheduleWindowRepositoryTest" --tests "*SettingsServerMutationTest" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt src/client-android/app/src/test/java/com/pim/app/ui/settings/SettingsServerMutationTest.kt
git commit -m "feat: make schedule repository cache-aware"
```

---

### Task 4: 将快照和策略历史接入 ForegroundLocationService

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/location/service/PolicyTransitionDeduper.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/location/service/ForegroundLocationServiceTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt`

- [x] **Step 1: 写失败测试。** 覆盖：service 使用 repository 快照；刷新失败保留旧 windows；策略 mode/interval/reason 任一变化时写一次 transition；相同 decision 不重复写；服务器变化先清空旧内存窗口；策略刷新不改 `continuousCollectionEnabled`；停止采集取消日程协程；已接受位置刷新通知。

- [x] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ForegroundLocationServiceTest" --no-daemon
```

- [x] **Step 3: 实现最小接入。**

1. 不维护第二份 `scheduleWindows` 列表；启动和位置评估统一调用 `scheduleWindowRepository.refreshIfStale()`，读取 `snapshotForCurrentServer()`，新鲜时不会发网络请求。
2. `handleLocation()` 和 `queueAccepted()` 使用受服务器 identity 保护的快照；切换服务器时 Repository 立即发布空 Missing 快照，再由现有异步刷新获取新数据。
3. 增加 `PolicyTransitionDeduper` 和集中 `applyDecision()`：

```kotlin
private fun applyDecision(decision: PolicyDecision) {
    val previous = lastRecordedDecision
    currentDecision = decision
    val changed = previous == null ||
        previous.mode != decision.mode ||
        previous.requestIntervalMillis != decision.requestIntervalMillis ||
        previous.reason != decision.reason
    if (changed) {
        lastRecordedDecision = decision
        scope.launch {
            locationQueueRepository.recordPolicyTransition(previous?.mode, decision)
        }
    }
}
```

并发写入通过 `Mutex` 串行，显式传播 `CancellationException`，普通写入异常隔离；测试证明相同 decision 不重复且快速连续转换不丢历史。

4. 扩展 `ForegroundLocationRuntimeState`：

```kotlin
val currentPolicyReason: String? = null,
val requestIntervalMillis: Long? = null,
val scheduleFreshness: ScheduleCacheFreshness = ScheduleCacheFreshness.Missing,
val scheduleLastSuccessAtMillis: Long? = null,
val scheduleLastAttemptAtMillis: Long? = null,
val scheduleLastError: String? = null
```

5. `publishRuntimeState()` 同时发布策略和缓存字段；缓存失败不得关闭采集或清空上传队列。停止采集时取消日程刷新和快照 collector；接受位置后刷新通知，保证队列计数与最近位置可见。

- [x] **Step 4: 运行 location/service 回归并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*ForegroundLocationServiceTest" --tests "*LocationQueueMappingTest" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/location/service src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt src/client-android/app/src/test/java/com/pim/app/location/service src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt
git commit -m "feat: expose schedule cache and policy transitions"
```

---

### Task 5: 创建 ViewModel 并替换占位日程页

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyUiState.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyViewModel.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- Modify: `src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt`
- Create: `src/client-android/app/src/test/java/com/pim/app/ui/schedule/SchedulePolicyViewModelTest.kt`

- [x] **Step 1: 写失败的 ViewModel 测试。** 锁定五种状态、重试和时间选择：

```kotlin
@Test
fun `successful empty response becomes Empty not Error`() = runTest {
    val state = mapper.stateFor(snapshot(freshness = Fresh, windows = emptyList()))
    assertIs<SchedulePolicyUiState.Empty>(state)
}

@Test
fun `failed refresh with cache becomes StaleContent`() = runTest {
    val state = mapper.stateFor(snapshot(freshness = Stale, windows = listOf(window())))
    assertIs<SchedulePolicyUiState.StaleContent>(state)
}
```

- [x] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SchedulePolicyViewModelTest" --no-daemon
```

- [x] **Step 3: 实现状态模型和 ViewModel。** `SchedulePolicyUiState` 必须有 `Loading`、`Content`、`Empty`、`StaleContent`、`Error` 五种互斥状态；共享内容模型携带当前项、下一项、按日期分组列表、缓存时间和策略摘要。ViewModel 注入 repository 和 `TrackingSettingsStore`，初始化调用 `refreshIfStale()`，`retry()` 传 `force=true`。策略摘要从 `ForegroundLocationService.runtimeState` 读取 mode/reason/interval，恢复距离来自设置。

- [x] **Step 4: 重写 Compose 页面。** 签名改为 `SchedulePolicyScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {})`，内部使用 `hiltViewModel()` 和 `collectAsStateWithLifecycle()`。渲染来源/时间/刷新按钮、当前项、下一项、日期分组列表、策略摘要、stale 警告和错误重试；`Authentication` 错误显示“前往设置”并调用回调，其余错误显示重试。`PimRootScreen` 传入 `{ selected = PimDestination.Settings }`。按钮和内容使用稳定 testTag：`schedule-refresh`、`schedule-retry`、`schedule-settings`、`schedule-current`、`schedule-upcoming`、`schedule-policy`。不增加 CRUD、WebView 或完整日历控件。

- [x] **Step 5: 更新静态契约测试，运行绿色测试并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*SchedulePolicyViewModelTest" --tests "*AndroidV2ScreenContentTest" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/ui/schedule src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt src/client-android/app/src/test/java/com/pim/app/ui/schedule src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt
git commit -m "feat: replace placeholder Android schedule screen"
```

---

### Task 6: 将缓存事实接入状态中心

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt`
- Modify: `src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusIssueTest.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/status/StatusCenterRepositoryFlowTest.kt`

- [ ] **Step 1: 写失败测试。** 新鲜/成功空不产生 issue；stale + cache 产生 Warning；missing + error 产生 Critical；tracking 快照包含 reason/interval；Info issue 仍不进入“需要处理”。

- [ ] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*StatusIssue*" --tests "*StatusCenterRepository*" --no-daemon
```

- [ ] **Step 3: 实现最小状态模型扩展。**

```kotlin
data class ScheduleCacheStatusSnapshot(
    val freshness: ScheduleCacheFreshness,
    val hasCachedWindows: Boolean,
    val lastSuccessAtMillis: Long?,
    val lastAttemptAtMillis: Long?,
    val lastError: String?
)

data class PolicyTransitionSnapshot(
    val fromMode: String?,
    val toMode: String,
    val reason: String,
    val occurredAtMillis: Long
)
```

`StatusCenterSnapshot` 増加 `schedule` 和最多 5 条 `recentPolicyTransitions`，`TrackingPolicySnapshot` 增加 `currentPolicyReason` 和 `requestIntervalMillis`，`StatusTrackingMapper.fromRuntime()` 直接映射 runtime。`StatusCenterRepository` 复用 `dao.recentPolicyTransitions(limit = 5)`。`StatusIssuePlanner` 新增：

- `schedule-cache-stale`：Warning，标题“日程数据可能过期”，复用 `ConnectionCheck` 动作。
- `schedule-cache-error`：Critical，标题“日程数据暂时不可用”，复用 `ConnectionCheck` 动作。

不要把原始 exception、服务器 URL 或机器码上屏。

- [ ] **Step 4: 接入现有 Flow 并显示事实。** 在 `StatusCenterRepository` 的 `combine` 中加入 `ScheduleWindowRepository.snapshot`、`ForegroundLocationService.runtimeState` 和最近策略转换；不创建轮询。`StatusCenterScreen` 在现有跟踪/诊断区域增加缓存新鲜度、上次成功、策略原因和最多 5 条切换记录，不重排同步与问题列表。

- [ ] **Step 5: 运行绿色测试并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*StatusIssue*" --tests "*StatusCenterRepository*" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/status src/client-android/app/src/main/java/com/pim/app/ui/status src/client-android/app/src/test/java/com/pim/app/status
git commit -m "feat: show schedule freshness in status center"
```

---

### Task 7: 将缓存元数据加入诊断导出

**Files:**
- Modify: `src/client-android/app/src/main/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepository.kt`
- Test: `src/client-android/app/src/test/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepositoryTest.kt`

- [ ] **Step 1: 写失败测试。** 导出后读取 `status.json`，断言包含 `scheduleFreshness`、`scheduleLastSuccessAtUtc`、`scheduleLastAttemptAtUtc`、`scheduleLastError`、`currentPolicyMode`、`currentPolicyReason` 和最多 20 条 `recentPolicyTransitions`；同时断言 token、refresh token、password 和完整认证设置不存在。

- [ ] **Step 2: 运行失败测试。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*DiagnosticExportRepositoryTest" --no-daemon
```

- [ ] **Step 3: 扩展 `buildStatus()`。** 注入 `ScheduleWindowRepository`，读取 `snapshot.value`；当前策略读取 `ForegroundLocationService.runtimeState.value`；从已有 `dao.recentPolicyTransitions(limit = 20).first()` 构造 JSON 数组。只在现有 `status.json` 追加白名单字段，不新增 ZIP entry、不改变 `CORE_ENTRIES`，继续经过 `DiagnosticRedactor` 和 credential leak 扫描。

- [ ] **Step 4: 运行绿色测试并提交。**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*DiagnosticExportRepositoryTest" --no-daemon
git add src/client-android/app/src/main/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepository.kt src/client-android/app/src/test/java/com/pim/app/mobile/diagnostics/DiagnosticExportRepositoryTest.kt
git commit -m "feat: export schedule policy facts in diagnostics"
```

---

### Task 8: 全量回归与本地设备验收

**Files:**
- Modify only when a failing test exposes an in-scope defect; add a failing regression test first.
- Do not modify `.github/workflows/*`.

- [ ] **Step 1: 运行 Android 全量单元测试和 debug 构建。**

```powershell
cd src/client-android
.\gradlew.bat :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

预期：新增和现有测试全部通过；Room schema 版本、entities 和 migrations 没有变化。

- [ ] **Step 2: 运行后端回归和差异检查。**

```powershell
cd ../..
dotnet test Pim.sln
git diff --check
```

- [ ] **Step 3: 有设备时运行 instrumentation。**

```powershell
cd src/client-android
adb devices -l
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

若设备列表为空，记录为本地环境阻塞，不修改 CI、不伪称通过；Android unit 和 APK build 仍是必需门禁。

- [ ] **Step 4: 手动验收关键路径。** 使用真实服务器验证：有日程、成功空列表、断网旧缓存、首次失败无缓存、重试、无地点日程、日程进入/退出、静止/步行/车载间隔、距离恢复、进程重启和服务器切换。页面与状态中心必须显示同一时间戳和错误事实。

- [ ] **Step 5: 检查提交边界。**

```powershell
git status --short --branch
git log --oneline -8
```

只允许预期源码、测试和文档变更；不得提交 `build/`、`bin/`、`obj/`、`.opencode-session/` 或其他生成物。

---

## 执行顺序与审查门

任务必须按 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 串行执行。每个任务完成后：

1. 只读 spec-reviewer 对照设计和本计划检查功能完整性与禁用范围。
2. 只读 quality-reviewer 检查并发、错误处理、测试信号和 Kotlin 风格。
3. 有问题时由唯一写入代理修复并重新运行该任务测试；未通过审查不得进入下一任务。

OpenCode 实现代理不得提交、推送或创建 PR；根代理负责检查 diff、提交、推送和 CI。

## 最终完成标准

- 日程页不再显示写死占位文案，五种状态和刷新反馈均由真实缓存事实驱动。
- 断网失败保留旧缓存，成功空列表不误报，服务器切换不串缓存。
- 日程、运动和距离恢复策略与状态页/诊断 ZIP 使用同一决定事实，策略历史无重复噪声。
- `dotnet test Pim.sln`、Android unit/debug build 和 `git diff --check` 通过；设备测试按环境如实记录。
- PR diff 不包含 Room schema、独立 Worker、WebView 日程、CRUD 或生成产物。
