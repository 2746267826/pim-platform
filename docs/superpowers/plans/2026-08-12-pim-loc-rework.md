# PIM 安卓定位采集改造 实现计划（三阶段）

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 按 `5_6334485245819101204.md`（2026-08-12 PIM 定位采集改造设计说明）实现安卓定位采集系统性改造：统一手动/自动采集引擎、quality gate 统一 20m、priority 全 HIGH_ACCURACY、常驻流 + 系统 interval 采集、自研传感器运动检测替换失效的 GMS 活动识别、日程降频 bug 修复、频率映射表重排。分三个顺序 PR 合入。

**架构：** 阶段 1 先落地低风险独立项（质量门/priority/日程/频率），阶段 2 引入纯逻辑可测的自研运动检测，阶段 3 把 LocationAcquisitionCoordinator 从"按需会话状态机"重构为"手动一次性采集 + 自动常驻流"统一引擎，ForegroundLocationService 从"每间隔开一次会话"改为"常驻流驱动 + 策略变化时重注册"。

**技术栈：** Kotlin 1.9.25、Coroutines/StateFlow、Hilt、Google Play services location 21.3.0、Room、Compose Material 3、JUnit4 + kotlinx-coroutines-test + Robolectric。

**设计依据（决策记录摘要，全部由用户实测+拍板）：**

| 决策 | 出处 |
|---|---|
| 手动 = 立即执行一次同一引擎；删除 AwaitingManualSubmit 确认步骤，达标即自动入库 | §3.1 + 用户拍板 |
| priority 全部 HIGH_ACCURACY，省电靠采样间隔 | §3.2 |
| quality gate 统一 20m；删除"精度阈值"设置项 | §3.2 + 用户拍板 |
| 低质量回退（best fix + 标记）仅用于手动一次采集；自动流只收 <20m 点，超限走 drop 诊断 | §3.2 + 用户拍板 |
| 常驻流 + 系统 interval；策略变化时重注册 LocationRequest；不做按需会话 | §3.3 |
| 自研传感器检测（加速度计 σ + 步数 + 重大运动），双防抖：运动 ≥5s、静止 ≥20s | §3.4 |
| MotionSignal 新增 Moving("移动中")；明显运动+步数→Walking，明显运动无步数→Moving，静止→Still | §3.4 |
| 日程降频仅当 locationText.isNotBlank()；运动打破降频已由自研检测自然生效 | §3.5 |
| 频率表：静止 3min / 日程 15min / 走路 60s（设置）/ 跑步·骑车·开车·Moving 硬编码 30s | §3.6 |
| 三阶段三 PR（loc-quality → loc-motion → loc-engine），每阶段可独立合入验证 | 用户拍板 |

---

## 执行总览

- 工作树规范（Linux）：`/workspace/pim-wt/{topic}`，分支 `opencode-linux/{topic}`，基于最新 `origin/master`。
- 分支：`opencode-linux/loc-quality`（Stage 1）、`opencode-linux/loc-motion`（Stage 2）、`opencode-linux/loc-engine`（Stage 3，L2 handoff，基于 Stage 2 合入后的 master）。
- 每阶段：TDD（A1）、全量 `gradlew :app:testDebugUnitTest --no-daemon` 门禁（基线 1224 项）、提交后 push 开 PR、等 CI 绿。PR 描述含四段双语模板（技术修改/功能变化/如何体验/测试）。
- 阶段间：前一个 PR 合入后再开始下一个。
- 禁止改动 `.github/workflows/*`。UI 可见文本用简体中文，代码标识符/日志/协议字段保持英文。
- 所有测试遵守 B1（注入时钟，不用固定时间戳）、B2（先归类失败）、A2（仅以最新一次运行结果为准）。
- 命令统一在 `src/client-android` 目录下执行：`./gradlew :app:testDebugUnitTest --no-daemon`。

**现有代码结构备忘（供各任务定位）：**

- 采集链：`ForegroundLocationService.startAutomaticLoop`（按需会话循环）→ `LocationAcquisitionCoordinator.startAutomaticSession/startManualSession`（会话状态机 10 态）→ `LocationAcquisitionEngine.acquire`（30s 超时采集）→ `FusedLocationUpdateSource.updates`（callbackFlow + setDurationMillis）→ `LocationQualityGate.evaluate`（阈值 50f）+ `AltitudeWaitCoordinator`（缺海拔等待 15s）。
- 策略链：`TrackingSettings`（SharedPreferences `pim_tracking`）→ `toTrackingPolicy` → `LocationPolicyEngine.reduce`（档位决策）→ `resolveLocationPriority`（PowerSavingNormal→BALANCED 100）→ `AutomaticSessionContext` → 引擎。
- 运动链：`MotionSignalRepository`（GMS ActivityRecognition 注册/回调，真机零回调）→ `MotionSignalStatus` StateFlow → `recomputePolicyDecision`。
- 存储：`LocationQueueRepository.enqueueAccepted(rawJson{...motionSignal, qualityFlags...}, source)` → Room `mobile_location_points`（source 列 manual/auto）→ 上传 `isAutoSubmitted = source != "manual"`。
- 消费者：`LocationCaptureRepository`（手动页状态映射）、`LocationLiveUpdatePublisher`（7102，Acquiring/Evaluating 时发布）、`LocationViewModel/LocationScreen`、`PimAppScaffold` 状态卡。
- 测试基建：coordinator 测试用手动装配 + FakeLocationAcquisitionRunner/TestLocationAcquisitionOperations/InMemorySharedPreferences；service 测试用 Robolectric `@Config(sdk=[34], application=TestPimApp)` + CoordinatorHarness 字段注入。

---

# Stage 1（PR: opencode-linux/loc-quality）—— 质量门 20m + 全 HIGH_ACCURACY + 日程修复 + 频率映射

## 文件地图

**生产代码：**
- 修改 `app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt` —— 删除 `maxUploadAccuracyMetersExclusive`（data class / defaults / read / write / KEY / toTrackingPolicy）
- 修改 `app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt` —— 删除精度校验（10–50m）与 `ACCURACY_MAX`
- 修改 `app/src/main/java/com/pim/app/settings/TrackingPresetCatalog.kt` —— 预设删除精度值
- 修改 `app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt` —— `TrackingPolicy` 删精度；`MotionSignal` 加 `Moving`；`movementIntervalFor` 重排
- 修改 `app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt` —— 日程 locationText 过滤；`isMoving` 加 Moving
- 修改 `app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt` —— 常量 `MAX_ACCURACY_METERS_EXCLUSIVE = 20f`；`fromTrackingSettings` 不再传精度
- 修改 `app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModels.kt` —— `LocationAcquisitionState` 删精度字段
- 修改 `app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinator.kt` —— 删精度快照；手动默认 priority 100 → HIGH_ACCURACY
- 修改 `app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt` —— `LocationCaptureState` 删精度字段
- 修改 `app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt` —— 默认阈值用 `LocationQualityGate.MAX_ACCURACY_METERS_EXCLUSIVE`
- 修改 `app/src/main/java/com/pim/app/ui/PimAppScaffold.kt` —— 精度规则行改用常量
- 修改 `app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt` —— 删除"精度阈值（米）"输入行与校验
- 修改 `app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt` —— `resolveLocationPriority` 恒为 HIGH_ACCURACY

**测试：**
- `app/src/test/.../location/quality/LocationQualityGateTest.kt`
- `app/src/test/.../location/policy/LocationPolicyEngineTest.kt`
- `app/src/test/.../settings/TrackingSettingsStoreTest.kt`、`TrackingSettingsValidatorTest.kt`
- `app/src/test/.../location/LocationSubmissionPolicyTest.kt`
- `app/src/test/.../location/acquisition/LocationAcquisitionCoordinatorTest.kt`
- `app/src/test/.../location/service/ForegroundLocationServiceTest.kt`
- `app/src/test/.../ui/settings/`（如存在精度相关用例）

---

### 任务 S1-1：LocationQualityGate 常量 20f + 删除 TrackingSettings/TrackingPolicy 精度字段

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt`
- 修改：`app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`
- 修改：`app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- 修改：`app/src/main/java/com/pim/app/settings/TrackingSettingsValidator.kt`
- 修改：`app/src/main/java/com/pim/app/settings/TrackingPresetCatalog.kt`
- 测试：`LocationQualityGateTest.kt`、`TrackingSettingsStoreTest.kt`、`TrackingSettingsValidatorTest.kt`

- [ ] **步骤 1：编写失败测试**

在 `LocationQualityGateTest.kt` 加两个用例（沿用文件内已有的 `fix()` 构造辅助）：

```kotlin
@Test
fun `default gate threshold is 20 meters exclusive`() {
    val gate = LocationQualityGate()
    val accepted = gate.evaluate(
        fix(horizontalAccuracyMeters = 19.9f, altitudeMeters = 5.0),
        nowMillis = 1_000L
    )
    assertTrue(accepted is QualityDecision.AcceptNow)
    val dropped = gate.evaluate(
        fix(horizontalAccuracyMeters = 20f, altitudeMeters = 5.0),
        nowMillis = 1_000L
    )
    assertEquals("horizontal-accuracy-too-low", (dropped as QualityDecision.Drop).reason)
}

@Test
fun `fromTrackingSettings applies the fixed 20m threshold and settings altitude timeout`() {
    val gate = LocationQualityGate.fromTrackingSettings(
        TrackingSettings.defaults().copy(altitudeWaitTimeoutMillis = 30_000L)
    )
    val dropped = gate.evaluate(
        fix(horizontalAccuracyMeters = 25f, altitudeMeters = 5.0),
        nowMillis = 1_000L
    )
    assertEquals("horizontal-accuracy-too-low", (dropped as QualityDecision.Drop).reason)
}
```

在 `TrackingSettingsStoreTest.kt` 与 `TrackingSettingsValidatorTest.kt` 中，把所有 `maxUploadAccuracyMetersExclusive = Xf` 相关构造/断言删除（编译失败即 RED）。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.quality.LocationQualityGateTest" --tests "com.pim.app.settings.TrackingSettingsStoreTest" --tests "com.pim.app.settings.TrackingSettingsValidatorTest"`
预期：新用例 FAIL（默认阈值仍是 50f），settings 测试编译失败。

- [ ] **步骤 3：实现**

`LocationQualityGate.kt`：

```kotlin
class LocationQualityGate(
    private val maxAccuracyMetersExclusive: Float = MAX_ACCURACY_METERS_EXCLUSIVE,
    private val altitudeWaitTimeoutMillis: Long = 15_000L
) {
    // evaluate / timeoutDecision 逻辑不变
    companion object {
        const val MAX_ACCURACY_METERS_EXCLUSIVE = 20f

        fun fromTrackingSettings(settings: TrackingSettings): LocationQualityGate =
            LocationQualityGate(altitudeWaitTimeoutMillis = settings.altitudeWaitTimeoutMillis)
    }
}
```

`TrackingSettingsStore.kt`：从 `TrackingSettings` data class、`defaults()`、`read()`、`write()`、`KEY_MAX_UPLOAD_ACCURACY_EXCLUSIVE`、`toTrackingPolicy()` 中删除该字段。
`LocationPolicyTypes.kt`：`TrackingPolicy` 删除该字段。
`TrackingSettingsValidator.kt`：删除精度校验块与 `ACCURACY_MAX` 常量。
`TrackingPresetCatalog.kt`：`TrackingPreset` 删除该字段，`applyTo` 同步，三个预设删除精度行。

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 定位质量门阈值统一为 20m 并移除精度设置项 / unify location quality gate at 20m and drop accuracy setting"
```

---

### 任务 S1-2：coordinator / capture repo / submission policy / 状态 UI 接入 20f 常量

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModels.kt`
- 修改：`app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionCoordinator.kt`
- 修改：`app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt`
- 修改：`app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt`
- 修改：`app/src/main/java/com/pim/app/ui/PimAppScaffold.kt`
- 测试：`LocationSubmissionPolicyTest.kt`、`LocationAcquisitionCoordinatorTest.kt`

- [ ] **步骤 1：编写失败测试**

`LocationSubmissionPolicyTest.kt`：确认 `decide(horizontalAccuracyMeters = 25f, autoAlreadySubmitted = false)`（不传阈值参数）断言 `canSubmitManually == false`。现有用例若用默认 50f 断言 25f 可提交，改后自然 RED。

`LocationAcquisitionCoordinatorTest.kt`：删除 `session reads maxUploadAccuracyMetersExclusive from settings store and surfaces it in state` 系列用例（:1228-1321 附近，编译失败即 RED）；把手动/自动会话请求构造处的 `priority = 100` 改为 `priority = 102`（:80 附近的 `AutomaticSessionContext(priority = 100, ...)`，以及 Fake runner 断言 priority 的位置）。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.LocationSubmissionPolicyTest" --tests "com.pim.app.location.acquisition.LocationAcquisitionCoordinatorTest"`
预期：FAIL/编译失败。

- [ ] **步骤 3：实现**

- `LocationAcquisitionModels.kt`：`LocationAcquisitionState` 删除 `maxUploadAccuracyMetersExclusive`。
- `LocationAcquisitionCoordinator.kt`：`startSession` 删除 `maxUploadAccuracyMetersExclusive = settings.maxUploadAccuracyMetersExclusive`；`:339` 改为 `priority = context?.priority ?: Priority.PRIORITY_HIGH_ACCURACY`（`import com.google.android.gms.location.Priority`）。
- `LocationCaptureRepository.kt`：`LocationCaptureState` 与 `toCaptureState` 删除该字段。
- `LocationSubmissionPolicy.kt`：默认参数改为 `maxUploadAccuracyMetersExclusive: Float = LocationQualityGate.MAX_ACCURACY_METERS_EXCLUSIVE`。
- `PimAppScaffold.kt`：`decide(...)` 调用删除 `maxUploadAccuracyMetersExclusive = state.maxUploadAccuracyMetersExclusive`（用默认常量）。

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 采集状态与提交策略统一使用 20m 质量门常量 / surface fixed 20m gate in capture state and submission policy"
```

---

### 任务 S1-3：设置 UI 删除"精度阈值（米）"输入

**文件：**
- 修改：`app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt`
- 修改：`app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt`
- 测试：`app/src/test/.../ui/settings/` 下相关用例（引用 `accuracyMetersText` 的删除）

- [ ] **步骤 1：编写失败测试**

在 settings 相关测试中删除引用 `accuracyMetersText` / `accuracy` 校验的用例；若无引用则跳过。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：编译失败（`accuracyMetersText` 相关引用）。

- [ ] **步骤 3：实现**

- `SettingsViewModel.kt`：删除 `accuracyMetersText` 状态字段（:56）、`onAccuracyChange`（:382）、校验分支（:411-412）、写入（:431）、回填（:674）。
- `SettingsScreen.kt`：删除精度输入行（:322-334 附近整块）。

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：PASS（全量单测）。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 设置页移除精度阈值输入，质量门固定 20m / remove accuracy threshold input from settings screen"
```

---

### 任务 S1-4：priority 全 HIGH_ACCURACY

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- 测试：`ForegroundLocationServiceTest.kt`

- [ ] **步骤 1：编写失败测试**

`ForegroundLocationServiceTest.kt` 的 `resolveLocationPriorityMapsPolicyModes`（:232-257）整体替换为：

```kotlin
@Test
fun resolveLocationPriorityIsHighAccuracyForEveryMode() {
    LocationPolicyMode.entries.forEach { mode ->
        assertEquals(
            Priority.PRIORITY_HIGH_ACCURACY,
            ForegroundLocationService.resolveLocationPriority(mode),
            "mode $mode must use HIGH_ACCURACY"
        )
    }
}
```

`resolveLocationPriorityCoversEveryPolicyMode`（:260-263）保留。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.service.ForegroundLocationServiceTest"`
预期：FAIL（PowerSavingNormal 等仍映射 BALANCED）。

- [ ] **步骤 3：实现**

`ForegroundLocationService.kt` companion：

```kotlin
fun resolveLocationPriority(mode: LocationPolicyMode): Int =
    Priority.PRIORITY_HIGH_ACCURACY
```

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 所有策略档位定位优先级统一为 HIGH_ACCURACY / all policy modes use HIGH_ACCURACY priority"
```

---

### 任务 S1-5：日程降频仅限有位置信息的日程（Bug 1）

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- 测试：`LocationPolicyEngineTest.kt`

- [ ] **步骤 1：编写失败测试**

在 `LocationPolicyEngineTest.kt` 加（沿用文件内现有 helper 的真实签名）：

```kotlin
@Test
fun `schedule without location text does not lower frequency`() {
    val engine = LocationPolicyEngine(TrackingPolicy())
    val blankLocation = scheduleWindow(
        locationText = "",
        startsAtMillis = 0L,
        endsAtMillis = 60_000L
    )
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 30_000L,
            collectionEnabled = true,
            currentScheduleWindow = blankLocation,
            motionSignal = MotionSignal.Still
        )
    )
    assertEquals(LocationPolicyMode.PowerSavingNormal, decision.mode)
    assertFalse(decision.scheduleLowFrequency)
}

@Test
fun `schedule with location text still lowers frequency`() {
    val engine = LocationPolicyEngine(TrackingPolicy())
    val located = scheduleWindow(
        locationText = "会议室",
        startsAtMillis = 0L,
        endsAtMillis = 60_000L
    )
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 30_000L,
            collectionEnabled = true,
            currentScheduleWindow = located,
            motionSignal = MotionSignal.Still
        )
    )
    assertEquals(LocationPolicyMode.ScheduleLowFrequency, decision.mode)
    assertTrue(decision.scheduleLowFrequency)
}
```

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.policy.LocationPolicyEngineTest"`
预期：第一个用例 FAIL（blank location 仍降频）。

- [ ] **步骤 3：实现**

`LocationPolicyEngine.kt:23`：

```kotlin
val activeSchedule = input.currentScheduleWindow?.takeIf {
    it.isActiveAt(input.nowMillis) && it.locationText.isNotBlank()
}
```

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS（含现有全部用例，确认没有位置的日程也不进锚点/恢复逻辑）。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "fix: 无位置信息的日程不再降频，仅在 locationText 非空时进入日程低频档 / only schedules with location text lower the tracking frequency"
```

---

### 任务 S1-6：MotionSignal 新增 Moving + 频率映射表重排

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt`
- 修改：`app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt`
- 测试：`LocationPolicyEngineTest.kt`

- [ ] **步骤 1：编写失败测试**

在 `LocationPolicyEngineTest.kt` 加：

```kotlin
@Test
fun `running uses the hardcoded 30s interval regardless of movement setting`() {
    val policy = TrackingPolicy(movementIntervalMillis = 300_000L)
    val engine = LocationPolicyEngine(policy)
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 1_000L,
            collectionEnabled = true,
            motionSignal = MotionSignal.Running
        )
    )
    assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
    assertEquals(30_000L, decision.requestIntervalMillis)
}

@Test
fun `moving uses the hardcoded 30s interval`() {
    val policy = TrackingPolicy(movementIntervalMillis = 300_000L)
    val engine = LocationPolicyEngine(policy)
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 1_000L,
            collectionEnabled = true,
            motionSignal = MotionSignal.Moving
        )
    )
    assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
    assertEquals(30_000L, decision.requestIntervalMillis)
}

@Test
fun `walking uses the configured movement interval`() {
    val policy = TrackingPolicy(movementIntervalMillis = 60_000L)
    val engine = LocationPolicyEngine(policy)
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 1_000L,
            collectionEnabled = true,
            motionSignal = MotionSignal.Walking
        )
    )
    assertEquals(60_000L, decision.requestIntervalMillis)
}

@Test
fun `moving signal breaks schedule low frequency and enters motion observation`() {
    val engine = LocationPolicyEngine(TrackingPolicy())
    val located = scheduleWindow(locationText = "会议室", startsAtMillis = 0L, endsAtMillis = 60_000L)
    val decision = engine.reduce(
        LocationPolicyInput(
            nowMillis = 30_000L,
            collectionEnabled = true,
            currentScheduleWindow = located,
            motionSignal = MotionSignal.Moving
        )
    )
    assertEquals(LocationPolicyMode.MotionObservation, decision.mode)
    assertFalse(decision.scheduleLowFrequency)
}
```

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.policy.LocationPolicyEngineTest"`
预期：编译失败（`MotionSignal.Moving` 不存在）。

- [ ] **步骤 3：实现**

`LocationPolicyTypes.kt`：

```kotlin
enum class MotionSignal(val displayName: String) {
    Unknown("未知"),
    Still("静止"),
    Walking("步行"),
    Running("跑步"),
    OnBicycle("骑行"),
    InVehicle("车载"),
    Moving("移动中")
}

fun TrackingPolicy.movementIntervalFor(signal: MotionSignal): Long = when (signal) {
    MotionSignal.Running,
    MotionSignal.OnBicycle,
    MotionSignal.InVehicle,
    MotionSignal.Moving -> TrackingIntervalBounds.MOVEMENT_MIN_MILLIS
    else -> movementIntervalMillis
}.coerceIn(
    TrackingIntervalBounds.MOVEMENT_MIN_MILLIS,
    TrackingIntervalBounds.MOVEMENT_MAX_MILLIS
)
```

`LocationPolicyEngine.kt` `isMoving()`：

```kotlin
private fun MotionSignal.isMoving(): Boolean = when (this) {
    MotionSignal.Walking,
    MotionSignal.Running,
    MotionSignal.OnBicycle,
    MotionSignal.InVehicle,
    MotionSignal.Moving -> true
    MotionSignal.Unknown,
    MotionSignal.Still -> false
}
```

注意：`movementIntervalFor` 的 `else` 分支覆盖 Walking/Still/Unknown（仍返回 `movementIntervalMillis`），保证函数全模式有定义。

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: MotionSignal 新增 Moving，跑步/骑车/开车/移动中统一 30s 硬编码间隔 / add Moving signal and hardcode 30s fast-motion intervals"
```

---

### 任务 S1-7：Stage 1 全量验证 + PR

- [ ] **步骤 1：全量单测**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：全部 PASS（若环境无网络/依赖缓存问题导致失败，先归类 B2 再决定）。

- [ ] **步骤 2：确认改动范围**

运行：`git status --short --branch` 与 `git diff --stat`
预期：仅 src/client-android/app/src 下的预期文件。

- [ ] **步骤 3：Push + 开 PR**

```bash
git push -u origin opencode-linux/loc-quality
gh pr create --base master --head opencode-linux/loc-quality --title "..." --body "..."
```

PR 标题：`feat: 定位质量门 20m + 全 HIGH_ACCURACY + 日程降频修复 + 频率映射（Stage 1）/ location quality gate 20m, HIGH_ACCURACY priority, schedule fix (Stage 1)`

PR 描述四段双语模板：
- `## 技术修改 / Technical changes`：文件清单 + 三个关键决策（质量门常量、priority 统一、locationText 过滤 + Moving 枚举与 30s 映射）。
- `## 功能变化 / Feature changes`：设置页删除"精度阈值"；所有定位一律高精度；无位置日程不再降频。
- `## 如何体验 / How to try it`：打开定位页触发一次定位看精度门槛收紧；观察日程时段频率。
- `## 测试 / Tests`：`./gradlew :app:testDebugUnitTest --no-daemon` 结果与数量。

- [ ] **步骤 4：等 CI 绿**

等待 GitHub Actions 对应 job 通过；若因 path filter 未触发，明确说明。

- [ ] **步骤 5：合入后清理**

`git checkout master && git pull`，删除本地分支与 worktree 引用（合入后统一清理，保留 worktree 以便 Stage 2 复用目录时重建）。

---

# Stage 2（PR: opencode-linux/loc-motion）—— 自研传感器运动检测

## 设计要点（移植自 actrec-test，真机验证）

- **参考实现**：`/workspace/actrec-test/app/src/main/java/com/test/actrec/MainActivity.kt` 内 `SelfMotionDetector`（60 样本模长标准差分块判定，阈值 σ<0.25 静止 / <1.0 轻微晃动 / ≥1.0 明显运动；步数传感器增量；重大运动一次性触发后重新注册）。
- **PIM 差异**：
  1. 采样率按设计文档"60 样本 ≈3s 窗口 @20Hz"实现——加速度计用 `SENSOR_DELAY_GAME`（≈20Hz，参考实现实际用了 NORMAL≈5Hz 是注释与实现不一致）。
  2. 新增双防抖（设计文档 §3.4 拍板）：运动判定持续 ≥5s、静止判定持续 ≥20s（时间累计式，不依赖窗口计数）。
  3. 映射规则：明显运动+步数增量→Walking；明显运动无步数→Moving；静止→Still；轻微晃动不单独输出（计入不静止侧，见下方状态机）。Running/OnBicycle/InVehicle 保留枚举但不产生。
  4. 纯逻辑与 Android 传感器解耦：新增 `SelfMotionEvaluator`（纯 Kotlin，注入时钟，可单测），`SelfMotionDetector` 只是 SensorEventListener 薄包装。
  5. 步数/重大运动需要 ACTIVITY_RECOGNITION 权限（Android 10+）；缺失时降级为仅加速度计并给出 issue 提示，不崩溃。
- **状态机（纯逻辑，时间累计式）**：每 60 个加速度样本（≈3s）结算一次窗口：raw=STILL(σ<0.25) / SHAKING(0.25≤σ<1.0) / MOVING(σ≥1.0)。维护 movingStreakMillis 与 stillStreakMillis：raw≠STILL 则 movingStreak+=窗口时长、stillStreak 清零；raw==STILL 则反向。当前防抖状态 STILL 下 movingStreak≥5000 切 MOVING；MOVING 下 stillStreak≥20000 切 STILL。重大运动触发计为一个 3s 的 MOVING 窗口（加速唤醒但尊重 5s 规则）。防抖态 MOVING 期间累计步数：>0→Walking，否则→Moving；防抖态 STILL→Still。仅在信号变化时通知下游。

## 文件地图

**生产代码：**
- 新建 `app/src/main/java/com/pim/app/location/motion/SelfMotionEvaluator.kt`（纯逻辑）
- 新建 `app/src/main/java/com/pim/app/location/motion/SelfMotionDetector.kt`（传感器薄包装）
- 重写 `app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt`（去掉 GMS 路径，接自研检测；保留 `MotionSignalStatus` 与 `status` StateFlow 表面）
- 删除 `app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt` 内 `MotionSignalMapper`/`MotionTransitionPlanner`/`MotionTransitionReceiver`
- 修改 `app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`（注册/注销调用点）
- 修改 `app/src/main/AndroidManifest.xml`（删除 MotionTransitionReceiver 声明）

**测试：**
- 新建 `app/src/test/.../location/motion/SelfMotionEvaluatorTest.kt`
- 删除 `app/src/test/.../location/motion/MotionSignalMapperTest.kt`
- 修改 `ForegroundLocationServiceTest.kt`（如引用了 registerActivityTransitions）

---

### 任务 S2-1：SelfMotionEvaluator 纯逻辑（σ 窗口 + 双防抖 + 步数映射）

**文件：**
- 创建：`app/src/main/java/com/pim/app/location/motion/SelfMotionEvaluator.kt`
- 测试：`app/src/test/.../location/motion/SelfMotionEvaluatorTest.kt`

- [ ] **步骤 1：编写失败测试**

`SelfMotionEvaluatorTest.kt`（JUnit4，注入时钟）：

```kotlin
class SelfMotionEvaluatorTest {
    private var nowMillis = 0L
    private val evaluator = SelfMotionEvaluator(nowElapsedRealtimeMillis = { nowMillis })

    private fun sampleStd(std: Double, count: Int = 60) {
        // 生成 count 个模长样本，使窗口结算出指定 σ 近似值：
        // 全部等值 → σ≈0（STILL）；一半 9.8 一半 10.6 → σ≈0.4（SHAKING）；
        // 一半 9.0 一半 11.6 → σ≈1.3（MOVING）。具体生成方式见实现细节，
        // 测试只关心 eval 结果对应的信号。
    }

    @Test
    fun `initial signal is Unknown`() {
        assertEquals(MotionSignal.Unknown, evaluator.currentSignal())
    }

    @Test
    fun `still window after 5s of shaking-to-moving transitions to Moving`() {
        nowMillis = 0L
        sampleStd(0.1) // STILL
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
        nowMillis = 3_000L
        sampleStd(1.3) // MOVING 窗口1
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
        nowMillis = 6_000L
        sampleStd(1.3) // MOVING 窗口2 → 累计 6s ≥ 5s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `walking signal requires step increments while moving`() {
        // 先切到 MOVING（同上）
        evaluator.stepCount(1_000L)   // 基线
        // 进入 MOVING 后：
        evaluator.stepCount(1_004L)   // 4 步
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())
    }

    @Test
    fun `moving without steps stays Moving`() {
        // 切到 MOVING 且无步数
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `still transition needs 20 seconds of stillness`() {
        // 已 MOVING；7 个 STILL 窗口（每 3s）→ 21s ≥ 20s
        repeat(6) { nowMillis += 3_000L; sampleStd(0.1) }
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
        nowMillis += 3_000L
        sampleStd(0.1) // 第 7 个
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `brief moving burst under 5 seconds does not leave Still`() {
        nowMillis = 0L; sampleStd(0.1)
        nowMillis = 3_000L; sampleStd(1.3) // 仅 3s
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
        nowMillis = 6_000L; sampleStd(0.1) // 恢复静止，movingStreak 清零
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `significant motion trigger accelerates the moving transition`() {
        nowMillis = 0L; sampleStd(0.1)
        evaluator.significantMotionTriggered() // 计为一个 3s MOVING 窗口
        nowMillis = 3_000L; sampleStd(1.3)     // 再一个 → 累计 6s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }
}
```

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.motion.SelfMotionEvaluatorTest"`
预期：编译失败（类不存在）。

- [ ] **步骤 3：实现**

`SelfMotionEvaluator.kt` 骨架（纯 Kotlin，无 Android 依赖）：

```kotlin
package com.pim.app.location.motion

import com.pim.app.location.policy.MotionSignal
import kotlin.math.sqrt

class SelfMotionEvaluator(
    private val windowSizeSamples: Int = 60,
    private val windowDurationMillis: Long = 3_000L,
    private val movingDebounceMillis: Long = 5_000L,
    private val stillDebounceMillis: Long = 20_000L,
    private val nowElapsedRealtimeMillis: () -> Long
) {
    enum class RawState { STILL, SHAKING, MOVING }

    private val magnitudes = ArrayDeque<Double>()
    private var lastWindowAtMillis: Long? = null
    private var movingStreakMillis = 0L
    private var stillStreakMillis = 0L
    private var debouncedMoving = false
    private var episodeStepTotal = 0L
    private var lastStepTotal = -1L
    private var signal = MotionSignal.Unknown

    fun accelMagnitude(magnitude: Double) {
        magnitudes.addLast(magnitude)
        if (magnitudes.size >= windowSizeSamples) {
            val now = nowElapsedRealtimeMillis()
            val windowDuration = lastWindowAtMillis?.let { now - it } ?: windowDurationMillis
            val raw = evaluateRaw(magnitudes)
            magnitudes.clear()
            accumulate(raw, windowDuration.coerceAtLeast(0L))
            lastWindowAtMillis = now
            recomputeSignal()
        }
    }

    fun stepCount(total: Long) {
        if (lastStepTotal == -1L) {
            lastStepTotal = total // 基线
            return
        }
        if (total > lastStepTotal) {
            episodeStepTotal += total - lastStepTotal
            lastStepTotal = total
            recomputeSignal()
        }
    }

    fun significantMotionTriggered() {
        if (!debouncedMoving) {
            movingStreakMillis += windowDurationMillis
            if (movingStreakMillis >= movingDebounceMillis) {
                debouncedMoving = true
                stillStreakMillis = 0L
                recomputeSignal()
            }
        }
    }

    fun currentSignal(): MotionSignal = signal

    private fun evaluateRaw(samples: ArrayDeque<Double>): RawState {
        val mean = samples.average()
        val std = sqrt(samples.map { (it - mean) * (it - mean) }.average())
        return when {
            std < 0.25 -> RawState.STILL
            std < 1.0 -> RawState.SHAKING
            else -> RawState.MOVING
        }
    }

    private fun accumulate(raw: RawState, windowDuration: Long) {
        if (raw == RawState.STILL) {
            movingStreakMillis = 0L
            stillStreakMillis += windowDuration
            if (debouncedMoving && stillStreakMillis >= stillDebounceMillis) {
                debouncedMoving = false
                episodeStepTotal = 0L
                movingStreakMillis = 0L
            }
        } else {
            stillStreakMillis = 0L
            movingStreakMillis += windowDuration
            if (!debouncedMoving && movingStreakMillis >= movingDebounceMillis) {
                debouncedMoving = true
                episodeStepTotal = 0L
                stillStreakMillis = 0L
            }
        }
    }

    private fun recomputeSignal() {
        signal = if (!debouncedMoving) {
            MotionSignal.Still
        } else if (episodeStepTotal > 0L) {
            MotionSignal.Walking
        } else {
            MotionSignal.Moving
        }
    }
}
```

说明：
- 进入 MOVING 时清空 episodeStepTotal（新 episode 重新累计）；切回 STILL 也清空。
- 防抖窗口时长用上次窗口到本次的实际间隔（无上次时按 windowDurationMillis=3000 估）。
- 步数事件先于首个窗口时只记基线；窗口结算与步数事件都会重算信号。
- 测试的 `sampleStd` 用等值/双值样本即可控制 σ 落在目标区间（数值精确性不做断言，只断言信号）。

- [ ] **步骤 4：运行验证通过**

运行：同步骤 2 命令。预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 自研运动检测纯逻辑（σ 窗口 + 5s/20s 双防抖 + 步数映射）/ self motion evaluator with sigma windows and dual debounce"
```

---

### 任务 S2-2：SelfMotionDetector 传感器薄包装 + MotionSignalRepository 重写

**文件：**
- 创建：`app/src/main/java/com/pim/app/location/motion/SelfMotionDetector.kt`
- 重写：`app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt`
- 修改：`app/src/main/AndroidManifest.xml`

- [ ] **步骤 1：编写失败测试**

`MotionSignalRepository` 的公开表面不变（`status: StateFlow<MotionSignalStatus>`），service 测试继续编译即视为通过；在 `ForegroundLocationServiceTest` 中把引用 `registerActivityTransitions()` 的调用改为 `register()`（若测试直接调用该方法）。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.service.ForegroundLocationServiceTest"`
预期：编译失败（`registerActivityTransitions` 不存在或行为断言失败）。

- [ ] **步骤 3：实现**

`SelfMotionDetector.kt`（SensorEventListener 薄包装，参考 actrec-test 移植）：

```kotlin
package com.pim.app.location.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import com.pim.app.location.policy.MotionSignal
import kotlin.math.sqrt

class SelfMotionDetector(
    context: Context,
    private val evaluator: SelfMotionEvaluator = SelfMotionEvaluator(
        nowElapsedRealtimeMillis = { SystemClock.elapsedRealtime() }
    ),
    private val onSignal: (MotionSignal) -> Unit = {}
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val triggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            if (event != null) evaluator.significantMotionTriggered()
            rearmSignificantMotion()
        }
    }

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        rearmSignificantMotion()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                evaluator.accelMagnitude(sqrt(x * x + y * y + z * z))
            }
            Sensor.TYPE_STEP_COUNTER -> evaluator.stepCount(event.values[0].toLong())
        }
        notifyIfChanged()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun rearmSignificantMotion() {
        sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let {
            sensorManager.requestTriggerSensor(triggerListener, it)
        }
    }

    private fun notifyIfChanged() {
        // 信号变化时才通知（由 evaluator 保证幂等：currentSignal 只在变化时不同）
        onSignal(evaluator.currentSignal())
    }
}
```

注意：
- 传感器不存在/未注册时静默降级（加速度计缺失时信号停留在 Unknown）。
- 需要处理 `TYPE_SIGNIFICANT_MOTION` 在 Android 10+ 无 ACTIVITY_RECOGNITION 权限时抛 `SecurityException` 的兜底：`rearmSignificantMotion` 用 `runCatching` 包裹。

`MotionSignalRepository.kt` 重写（去掉全部 GMS 依赖）：

```kotlin
@Singleton
class MotionSignalRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(MotionSignalStatus(MotionSignal.Unknown, null, null))
    val status: StateFlow<MotionSignalStatus> = _status.asStateFlow()

    private val detector = SelfMotionDetector(
        context = context,
        evaluator = SelfMotionEvaluator(nowElapsedRealtimeMillis = { SystemClock.elapsedRealtime() }),
        onSignal = { signal ->
            if (signal != _status.value.signal) {
                _status.value = MotionSignalStatus(signal, issueCode = null, message = null)
            }
        }
    )

    fun register() {
        detector.start()
    }

    fun unregister() {
        detector.stop()
    }
}
```

`MotionSignalStatus` 保留原定义（signal / issueCode / message），删除 `ACTIVITY_RECOGNITION_PERMISSION_MESSAGE` 与 `unavailable()` 的旧使用处（后续如需传感器缺失提示再补 issue，本期先保持 Unknown）。

`AndroidManifest.xml`：删除 `MotionTransitionReceiver` 的 `<receiver>` 声明（:57-59 附近）。`ACTIVITY_RECOGNITION` 权限声明保留（步数/重大运动传感器仍需要）。

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：全量 PASS（MotionSignalMapperTest 删除）。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src src/client-android/app/src/main/AndroidManifest.xml
git commit -m "feat: 运动检测改为自研传感器实现，移除失效的 GMS 活动识别路径 / replace GMS activity recognition with self sensor motion detection"
```

---

### 任务 S2-3：service 注册/注销调用点迁移 + 删除旧 mapper 测试

**文件：**
- 修改：`app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt`
- 删除：`app/src/test/.../location/motion/MotionSignalMapperTest.kt`
- 测试：`ForegroundLocationServiceTest.kt`

- [ ] **步骤 1：编写失败测试**

`ForegroundLocationServiceTest.kt`：搜索 `registerActivityTransitions` / `unregisterActivityTransitions` 的引用并替换为 `register` / `unregister`（编译失败即 RED）。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.service.ForegroundLocationServiceTest"`
预期：编译失败。

- [ ] **步骤 3：实现**

`ForegroundLocationService.kt`：
- `initializeAutomaticRuntime`（:499）：`motionSignalRepository.registerActivityTransitions()` → `motionSignalRepository.register()`
- `startAutomaticLoop`（:508）：同上
- `stopCollection`（:324）：`runCatching { motionSignalRepository.unregisterActivityTransitions() }` → `runCatching { motionSignalRepository.unregister() }`

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：全量 PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "refactor: 运动检测注册入口迁移到自研传感器实现 / switch motion registration entrypoints to self detector"
```

---

### 任务 S2-4：Stage 2 全量验证 + PR

- [ ] **步骤 1：全量单测**：`./gradlew :app:testDebugUnitTest --no-daemon` 全绿。
- [ ] **步骤 2：改动范围确认**：`git status --short --branch` + `git diff --stat`，仅预期文件。
- [ ] **步骤 3：Push + 开 PR**

```bash
git push -u origin opencode-linux/loc-motion
gh pr create --base master --head opencode-linux/loc-motion --title "..." --body "..."
```

PR 标题：`feat: 自研传感器运动检测替换 GMS 活动识别（Stage 2）/ self sensor motion detection replaces GMS activity recognition (Stage 2)`
PR 描述四段双语模板（技术修改：SelfMotionEvaluator/Detector/Repository 重写 + 移除 GMS；功能变化：运动状态从永远 Unknown 变为实时检测；如何体验：走路/静止时观察策略档位变化；测试：单测数量与结果）。

- [ ] **步骤 4：等 CI 绿**；步骤 5：合入后同步 master 并重建下一阶段 worktree。

---

# Stage 3（PR: opencode-linux/loc-engine）—— 统一采集引擎 + 常驻流

## 设计要点

1. **统一引擎**：手动 = 立即执行一次同一引擎（一次性采集：HIGH_ACCURACY + 1s 间隔 + 30s 截止 + 20m 门 + 15s 海拔等待）；自动 = 同一引擎的常驻流（注册时先做一次"预热采集"等 GPS 收敛，之后系统按 interval 回调 fix，逐点过门入库）。删除 AwaitingManualSubmit/Enqueuing 相与 pendingAccepted/submitManualResult 整条手动确认状态机，手动达标即自动入库（source 仍标 "manual"）。
2. **低质量回退**（仅手动）：30s 截止未收到 <20m fix 时，用当时最好 fix 直接入库并打 `low-quality-accuracy` 标记（UI 明示）；一个 fix 都没有 → TimedOut。自动流不做回退：≥20m 的 fix 全部走 drop 诊断（含预热期）。
3. **常驻流**：`FusedLocationUpdateSource.updates` 支持 `durationMillis <= 0`（不调用 setDurationMillis，持续注册）；流内每个 fix 直接过门：接受→入库（source=auto）+ 更新 streamState + 通知 service；缺海拔→直接带 `altitude-missing` 标记接受（流内不做 15s 等待，GPS 热态下海拔基本都有；手动一次性仍保留 15s 等待）。
4. **service 驱动**：`startAutomaticLoop` 改为策略驱动循环——每轮重算决策，决策变化才 applyDecision + （重）注册流；等待条件用 30s 兜底超时 + 运动信号变化即时唤醒。每次入库点（onRecorded 回调）更新锚点（onAcceptedLocation）、通知文本与下次定位倒计时。
5. **状态机**：`state`（手动一次性 + 自动预热共用同一 AcquisitionPhase 集，但自动预热写入 streamState 侧）与 `streamState`（自动流活动状态）两个 StateFlow。手动取消仅作用于手动一次性会话；自动流由 service 启停。
6. **Live Update（7102）**：保持仅手动一次性（Acquiring/Evaluating）发布；流模式不发布（避免每 interval 弹通知；API 36 模拟器镜像问题见设计文档 §5，后续再评估）。
7. **UI**：删除"提交位置"按钮与提交状态文案；Completed 时显示精度与（如存在）低质量标记。

## 文件地图

**生产代码：**
- 修改 `acquisition/LocationAcquisitionModels.kt` —— 阶段、上下文、状态、stream 状态、低质量标记常量
- 重写 `acquisition/LocationAcquisitionCoordinator.kt` —— 统一一次性采集 + 常驻流 + 回退
- 修改 `acquisition/LocationAcquisitionEngine.kt` —— 基本不变（一次性采集逻辑复用）
- 修改 `acquisition/LocationUpdateSource.kt` —— durationMillis<=0 时持续注册
- 修改 `acquisition/LocationAcquisitionModule.kt` —— 如绑定签名变化则同步
- 重写 `service/ForegroundLocationService.kt` 的 startAutomaticLoop / startManualSession / 相关 waiter
- 修改 `location/LocationCaptureRepository.kt` —— 删提交、加 lastQualityFlags
- 修改 `ui/location/LocationUiState.kt`、`LocationScreen.kt`、`LocationViewModel.kt` —— 删提交、低质量提示
- 修改 `ui/PimAppScaffold.kt` —— LocationTab 删提交行
- 修改 `liveupdate/LocationLiveUpdatePublisher.kt` —— 仅手动一次性发布（StreamActive 不发布）

**测试：**
- 重写 `acquisition/LocationAcquisitionCoordinatorTest.kt`（保留 Fake runner/ops 基建）
- 修改 `service/ForegroundLocationServiceTest.kt`
- 修改 `LocationCaptureRepositoryTest.kt`、`ui/location/LocationViewModelTest.kt`、`liveupdate/LocationLiveUpdatePublisherTest.kt`
- 修改 `ui/location/LocationScreenTest.kt`（androidTest）

---

### 任务 S3-1：Models 与 UpdateSource 先决改动

**文件：**
- 修改：`acquisition/LocationAcquisitionModels.kt`
- 修改：`acquisition/LocationUpdateSource.kt`
- 测试：`LocationAcquisitionCoordinatorTest.kt`（编译适配）、`LocationAcquisitionEngineTest.kt`

- [ ] **步骤 1：编写失败测试**

`LocationAcquisitionEngineTest.kt` 加：

```kotlin
@Test
fun `updates request with non-positive duration maps to no duration limit`() {
    // FakeLocationUpdateSource 断言 LocationUpdateRequest.durationMillis <= 0 时
    // FusedLocationUpdateSource 不调用 setDurationMillis —— 通过 Fake 记录请求断言：
    // 此处验证 LocationUpdateRequest 数据类可携带 durationMillis = 0（引擎透传）。
    val request = LocationUpdateRequest(priority = 102, durationMillis = 0L, intervalMillis = 60_000L)
    assertEquals(0L, request.durationMillis)
}
```

若 `FusedLocationUpdateSource` 可被 Fake 替代（现有 `LocationAcquisitionEngineTest` 用 FakeLocationUpdateSource），则本任务以模型编译通过为主；真实 Fused 的 setDurationMillis 行为由单元测试不可达，用 `LocationUpdateSource.kt` 的条件分支 + 代码审查覆盖。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.acquisition.LocationAcquisitionEngineTest"`
预期：编译失败（`AutomaticSessionContext` 尚在）。

- [ ] **步骤 3：实现**

`LocationAcquisitionModels.kt`：

```kotlin
enum class TriggerType(val storageSource: String) {
    MANUAL("manual"),
    AUTOMATIC("auto")
}

enum class AcquisitionPhase {
    Idle,
    Preparing,
    Acquiring,
    Evaluating,
    Completed,
    TimedOut,
    Failed,
    Cancelled,
    StreamActive
}

data class AcquisitionContext(
    val policyMode: String,
    val scheduleLowFrequency: Boolean,
    val motionSignal: String,
    val requestIntervalMillis: Long
)

data class LocationAcquisitionState(
    val sessionId: String? = null,
    val triggerType: TriggerType? = null,
    val phase: AcquisitionPhase = AcquisitionPhase.Idle,
    val bestLocation: LocationSnapshot? = null,
    val startedAtElapsedRealtimeMs: Long? = null,
    val deadlineAtElapsedRealtimeMs: Long? = null,
    val elapsedMs: Long = 0L,
    val lastQualityFlags: Set<String> = emptySet(),
    val errorReason: String? = null
) {
    val isBusy: Boolean
        get() = phase in setOf(
            AcquisitionPhase.Preparing,
            AcquisitionPhase.Acquiring,
            AcquisitionPhase.Evaluating
        )
}

data class AutomaticStreamState(
    val active: Boolean = false,
    val requestIntervalMillis: Long = 0L,
    val latestFix: LocationSnapshot? = null,
    val latestQualityFlags: Set<String> = emptySet(),
    val lastError: String? = null
)

sealed interface SessionStartResult {
    data class Started(val sessionId: String) : SessionStartResult
    data object Busy : SessionStartResult
    data class Rejected(val reason: String) : SessionStartResult
}
```

低质量标记常量：在 `LocationQualityGate.kt` companion 增加：

```kotlin
const val LOW_QUALITY_ACCURACY_FLAG = "low-quality-accuracy"
```

`LocationUpdateSource.kt`：

```kotlin
override fun updates(request: LocationUpdateRequest): Flow<LocationUpdateEvent> = callbackFlow {
    val builder = LocationRequest.Builder(request.priority, request.intervalMillis)
        .setMinUpdateIntervalMillis(request.minUpdateIntervalMillis)
    if (request.durationMillis > 0L) {
        builder.setDurationMillis(request.durationMillis)
    }
    val locationRequest = builder.build()
    // ...其余不变
}
```

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.acquisition.LocationAcquisitionEngineTest"`
预期：PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 采集模型引入 AcquisitionContext/流状态与低质量标记，更新源支持常驻注册 / introduce acquisition context, stream state and persistent update registration"
```

---

### 任务 S3-2：Coordinator 统一一次性采集（手动直达入库 + 低质量回退）

**文件：**
- 重写：`acquisition/LocationAcquisitionCoordinator.kt`
- 测试：`LocationAcquisitionCoordinatorTest.kt`

- [ ] **步骤 1：编写失败测试**

在 `LocationAcquisitionCoordinatorTest.kt` 加：

```kotlin
@Test
fun `manual session enqueues accepted fix directly without awaiting manual submit`() = runTest {
    createCoordinator(this)
    prerequisiteChecker.ready()

    val started = coordinator.startManualSession() as SessionStartResult.Started
    runner.waitForAcquire()
    runner.emitCandidate(goodFix(latitude = 31.23, longitude = 121.47, accuracyMeters = 8f))
    runner.complete(
        LocationEngineResult(
            sessionId = started.sessionId,
            bestLocation = goodFix(...),
            completion = LocationEngineCompletion.TimedOut
        )
    )
    runCurrent()

    assertTrue(operations.enqueued.single().source == "manual")
    assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
    assertFalse(coordinator.state.value.phase == AcquisitionPhase.AwaitingManualSubmit)
}

@Test
fun `manual session falls back to best fix with low-quality flag on timeout`() = runTest {
    createCoordinator(this)
    prerequisiteChecker.ready()

    val poorFix = snapshot(accuracyMeters = 45f)  // ≥20m，门会 Drop
    val started = coordinator.startManualSession() as SessionStartResult.Started
    runner.waitForAcquire()
    runner.emitCandidate(poorFix)
    runner.complete(
        LocationEngineResult(
            sessionId = started.sessionId,
            bestLocation = poorFix,
            completion = LocationEngineCompletion.TimedOut
        )
    )
    runCurrent()

    assertEquals(AcquisitionPhase.Completed, coordinator.state.value.phase)
    assertTrue(operations.enqueued.single().rawJson.contains("low-quality-accuracy"))
    assertEquals(setOf("low-quality-accuracy"), coordinator.state.value.lastQualityFlags)
}

@Test
fun `manual session with no fix at all ends timed out without enqueue`() = runTest {
    createCoordinator(this)
    prerequisiteChecker.ready()

    val started = coordinator.startManualSession() as SessionStartResult.Started
    runner.waitForAcquire()
    runner.complete(
        LocationEngineResult(
            sessionId = started.sessionId,
            bestLocation = null,
            completion = LocationEngineCompletion.TimedOut
        )
    )
    runCurrent()

    assertEquals(AcquisitionPhase.TimedOut, coordinator.state.value.phase)
    assertTrue(operations.enqueued.isEmpty())
}

@Test
fun `manual restart replaces an in-flight one-shot session`() = runTest {
    createCoordinator(this)
    prerequisiteChecker.ready()

    val first = coordinator.startManualSession() as SessionStartResult.Started
    runner.waitForAcquire()
    val second = coordinator.startManualSession() as SessionStartResult.Started
    assertNotEquals(first.sessionId, second.sessionId)
}
```

（沿用文件内现有 `goodFix`/`snapshot` 等 helper 的真实签名；`operations.enqueued` 记录 (accepted, rawJson, source) 三元组。）

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.acquisition.LocationAcquisitionCoordinatorTest"`
预期：新用例 FAIL（手动仍走 AwaitingManualSubmit；无回退）。

- [ ] **步骤 3：实现**

`LocationAcquisitionCoordinator.kt` 重写要点（保留现有并发安全模式：CAS 状态机、ownerJob 检查、测试缝）：

```kotlin
@Singleton
class LocationAcquisitionCoordinator @Inject constructor(
    private val runner: LocationAcquisitionRunner,
    private val prerequisiteChecker: LocationPrerequisiteChecker,
    private val operations: LocationAcquisitionOperations,
    private val json: Json,
    private val trackingSettingsStore: TrackingSettingsStore
) {
    internal var testScope: CoroutineScope? = null
    internal var uuidGenerator: () -> String = { UUID.randomUUID().toString() }
    internal var wallClockMillis: () -> Long = { System.currentTimeMillis() }
    internal var elapsedRealtimeMillis: () -> Long = { SystemClock.elapsedRealtime() }
    internal var onRecorded: (suspend (LocationSnapshot) -> Unit)? = null

    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scope: CoroutineScope get() = testScope ?: internalScope

    private val _state = MutableStateFlow(LocationAcquisitionState())
    val state: StateFlow<LocationAcquisitionState> = _state.asStateFlow()

    private val _streamState = MutableStateFlow(AutomaticStreamState())
    val streamState: StateFlow<AutomaticStreamState> = _streamState.asStateFlow()

    private var sessionJob: Job? = null
    private var streamJob: Job? = null

    // ── 手动一次性采集 ────────────────────────────────
    fun startManualSession(): SessionStartResult {
        // 1) 若已有手动一次性会话在跑：先取消（restart 语义）
        cancelCurrentSession(_state.value.sessionId)
        // 2) precheck（Blocked → Idle + errorReason → Rejected）
        // 3) startOneShotSession(uuid, MANUAL, context = null)
    }

    fun cancelCurrentSession(expectedSessionId: String? = null): Boolean {
        // 与现状一致，仅允许 Preparing/Acquiring/Evaluating 取消；
        // Cancelled 终态后置；ownerJob CAS 模式保留。
    }

    private suspend fun runOneShotSession(sessionId, triggerType, context, ownerJob) = coroutineScope {
        // ticker（1s 更新 elapsedMs）+ acquireAndEvaluate（复用现有结构）：
        //   gate = LocationQualityGate.fromTrackingSettings(settings)  // 20f 常量
        //   request = LocationEngineRequest(priority = HIGH_ACCURACY, timeoutMillis = 30_000, ...)
        //   候选 → toRawFix → altitudeWaitCoordinator.handleFix（15s 等待保留）
        //   接受 → onQualityAccepted（取消 engine job）
        // 终局：
        //   accepted != null → enqueueOneShot(accepted, triggerType, sessionId, ownerJob) → Completed
        //   accepted == null && best != null && allowLowQualityFallback →
        //       QualityAcceptedLocation(fix = best.toRawFix(...), altitudeMeters = best.altitudeMeters,
        //           acceptedAtMillis = wallClockMillis(),
        //           qualityFlags = setOf(LOW_QUALITY_ACCURACY_FLAG))
        //       → enqueueOneShot → Completed（state.lastQualityFlags 携带）
        //   best == null → TimedOut（errorReason = "获取位置超时，未获得任何定位结果"）
        //   engine Failed → Failed
    }

    private suspend fun enqueueOneShot(accepted, triggerType, sessionId, ownerJob) {
        // 统一入库：rawJson(accepted, triggerType.storageSource) + operations.enqueueAccepted + scheduleSync
        // 成功后 phase = Completed（state 带 lastQualityFlags）；失败 → Failed(errorReason)
    }

    // ── 自动常驻流 ────────────────────────────────────
    fun startAutomaticStream(context: AcquisitionContext): Boolean {
        if (_streamState.value.active) return updateAutomaticStream(context)
        // 预热一次性采集（allowLowQualityFallback = false）：
        //   跑一次 runOneShotSession(AUTOMATIC, context)，其记录写入 streamState 侧
        //   （warm-up 会话期间 phase = StreamActive，bestLocation = 预热候选，
        //    完成/放弃后进入常驻阶段）
        // 常驻流 job：
        //   source.updates(LocationUpdateRequest(priority = HIGH_ACCURACY,
        //       intervalMillis = context.requestIntervalMillis, durationMillis = 0L))
        //     .collect { event ->
        //       Candidate → gate.evaluate：
        //         AcceptNow → enqueueStream(accepted) + streamState 更新 + onRecorded
        //         Drop      → recordDrop + （可选的 streamState.lastError 提示）
        //         WaitForAltitude → 立即以 altitude-missing 标记接受（不等待）
        //       Availability → 忽略
        //     }
        // 异常 → streamState.lastError；job 保持可重试（service 下轮重注册）
    }

    fun updateAutomaticStream(context: AcquisitionContext): Boolean {
        // 若未激活 → startAutomaticStream；若激活且 interval/上下文变化 → 取消旧流 job，
        // 重新 startAutomaticStream（预热 + 新 interval 常驻）
    }

    fun stopAutomaticStream() {
        // 取消流 job + streamState 复位
    }

    fun isAutomaticStreamActive(): Boolean = _streamState.value.active

    // rawJson 不变（含 motionSignal / qualityFlags / policyMode / scheduleLowFrequency）
}
```

要点：
- 一次性采集的手动/自动差异只剩 `storageSource` 与 `allowLowQualityFallback`（自动预热 false）——"一套逻辑"。
- 预热会话与手动会话共用 `runOneShotSession`，但预热期间把 phase 写进 `_streamState`（phase=StreamActive 固定，elapsedMs/bestLocation 更新到 streamState），不打扰手动 UI 的 `state`。
- `onRecorded` 在每次成功入库后回调（service 用来更新锚点/通知/倒计时）。
- 流内每次入库 `operations.scheduleSync()` 照旧。

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.acquisition.LocationAcquisitionCoordinatorTest"`
预期：PASS（先删除旧的手动确认/自动分支相关用例；保留并发与取消类用例并按新 phase 集适配）。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 采集协调器统一为手动一次性采集（含低质量回退）+ 自动常驻流 / unify coordinator as one-shot manual capture and persistent automatic stream"
```

---

### 任务 S3-3：ForegroundLocationService 改为常驻流驱动

**文件：**
- 重写：`service/ForegroundLocationService.kt` 的 startAutomaticLoop / startManualSession / waiter / cancel 相关
- 测试：`ForegroundLocationServiceTest.kt`

- [ ] **步骤 1：编写失败测试**

在 `ForegroundLocationServiceTest.kt` 加：

```kotlin
@Test
fun `automatic loop registers a persistent stream with the policy interval`() = runTest {
    // 启动连续采集；断言 coordinator.startAutomaticStream 被调用且
    // AcquisitionContext.requestIntervalMillis == 决策间隔（3min 默认）
    // （通过 CoordinatorHarness 包装的 fake coordinator 记录调用）
}

@Test
fun `motion signal change restarts the stream with the new interval`() = runTest {
    // 注入 motionSignalRepository 状态 Still → 断言流以 3min 注册；
    // 推送 Walking → 断言流以 60s 重注册
}

@Test
fun `recorded point feeds policy anchor and notification text`() = runTest {
    // 触发一次流入库（fake runner/ops）→ 断言 policyEngine.onAcceptedLocation 被喂
    // （锚点生效：再次决策时 MovementRecovery 分支可用）
}
```

（沿用文件内现有 harness：CoordinatorHarness / ControllableRunner / no-op operations / 字段注入。）

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.service.ForegroundLocationServiceTest"`
预期：新用例 FAIL（旧循环无流概念）。

- [ ] **步骤 3：实现**

`ForegroundLocationService.kt` 核心替换：

```kotlin
private fun startAutomaticLoop() {
    automaticLoopJob?.cancel()
    automaticLoopJob = scope.launch {
        while (trackingSettingsStore.read().continuousCollectionEnabled) {
            refreshScheduleWindows()
            applyScheduleSnapshot(scheduleWindowRepository.snapshotForCurrentServer())
            val decision = recomputePolicyDecision()
            // 只按关键字段比较：nextExpectedLocationAtMillis 每轮必然不同
            // （now + interval），不能进 data class equals。
            if (decision.mode != currentDecision.mode ||
                decision.requestIntervalMillis != currentDecision.requestIntervalMillis ||
                decision.scheduleLowFrequency != currentDecision.scheduleLowFrequency ||
                decision.reason != currentDecision.reason
            ) {
                applyDecision(decision)
                updateNotification()
            }
            if (decision.mode != LocationPolicyMode.Off) {
                val ctx = AcquisitionContext(
                    policyMode = decision.mode.name,
                    scheduleLowFrequency = decision.scheduleLowFrequency,
                    motionSignal = motionSignalRepository.status.value.signal.name,
                    requestIntervalMillis = decision.requestIntervalMillis.coerceAtLeast(1_000L)
                )
                if (!locationAcquisitionCoordinator.isAutomaticStreamActive()) {
                    locationAcquisitionCoordinator.startAutomaticStream(ctx)
                } else {
                    locationAcquisitionCoordinator.updateAutomaticStream(ctx)
                }
            } else {
                locationAcquisitionCoordinator.stopAutomaticStream()
            }
            // 运动信号变化即时唤醒；最迟 30s 重算一次（覆盖日程/设置变化）
            withTimeoutOrNull(30_000L) {
                val currentSignal = motionSignalRepository.status.value.signal
                motionSignalRepository.status.first { it.signal != currentSignal }
            }
        }
        explicitTeardown = true
        isPausing = false
        locationAcquisitionCoordinator.stopAutomaticStream()
        stopCollection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
```

配套：
- `initializeAutomaticRuntime` 里把 `locationAcquisitionCoordinator.onRecorded` 接到：

```kotlin
locationAcquisitionCoordinator.onRecorded = { snapshot ->
    policyEngine?.onAcceptedLocation(
        PolicyLocation(snapshot.latitude, snapshot.longitude, snapshot.timeMillis)
    )
    lastAcceptedLocationText = timeFormatter.format(
        Instant.ofEpochMilli(snapshot.timeMillis).atZone(ZoneId.systemDefault())
    )
    lastAccuracyText = "${snapshot.horizontalAccuracyMeters?.toInt() ?: 0}m"
    lastDroppedReason = null
    currentDecision = currentDecision.copy(
        nextExpectedLocationAtMillis = System.currentTimeMillis() +
            currentDecision.requestIntervalMillis
    )
    updateNotification()
}
```

- `startManualSession(startId)`：去掉 replaceAwaitingManual/Busy 采纳逻辑 → `coordinator.startManualSession()`；Started → ownedManualSessionId + startForeground + 终态 waiter（waiter 看到 Completed/TimedOut/Failed/Cancelled 或 sessionId 变化即退役：清 owner、若连续采集未启用则移除前台并 stopSelf）。
- `cancelActiveAutomaticSession()` → `stopAutomaticStream()`（stopCollection 内）。
- `onDestroy` 的手动取消分支按新 phase 集（Preparing/Acquiring/Evaluating）保留。
- `ACTION_CANCEL_LOCATION_SESSION` 的 fail-closed 逻辑保留（仍只取消手动一次性会话）。
- `PolicyDecision` 比较需要 data class equals（已是）。

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon --tests "com.pim.app.location.service.ForegroundLocationServiceTest"`
预期：PASS（删除旧"按需会话循环"相关用例，保留前台/通知/取消类用例并适配）。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 前台服务改为常驻流驱动，策略变化时重注册 LocationRequest / drive collection from persistent stream and re-register on policy change"
```

---

### 任务 S3-4：UI 与消费者适配（删提交按钮、低质量提示）

**文件：**
- 修改：`location/LocationCaptureRepository.kt`
- 修改：`ui/location/LocationUiState.kt`、`LocationScreen.kt`、`LocationViewModel.kt`
- 修改：`ui/PimAppScaffold.kt`
- 修改：`liveupdate/LocationLiveUpdatePublisher.kt`
- 测试：`LocationCaptureRepositoryTest.kt`、`LocationViewModelTest.kt`、`LocationLiveUpdatePublisherTest.kt`、`LocationScreenTest.kt`（androidTest）

- [ ] **步骤 1：编写失败测试**

`LocationCaptureRepositoryTest.kt`：
- 删 `submitCurrentLocationManually` 相关用例。
- 加：`toCaptureState` 映射 Completed + lastQualityFlags 非空 → `statusMessage` 含"低质量"且 `showLowQualityWarning == true`。

`LocationLiveUpdatePublisherTest.kt`：
- 加：phase = StreamActive（经 `LocationAcquisitionState(phase = StreamActive)` 或 streamState）→ 不发布、取消已有通知。

`LocationScreenTest.kt`（androidTest）：
- 删"提交位置"按钮用例；加：Completed 且低质量标记时界面显示警告文案。

- [ ] **步骤 2：运行验证失败**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：编译失败（submit 相关 API 已删）/ 新用例 FAIL。

- [ ] **步骤 3：实现**

- `LocationCaptureRepository.kt`：
  - `LocationCaptureState`：删 `isSubmitting`/`autoSubmitted`，加 `lastQualityFlags: Set<String>`、`showLowQualityWarning: Boolean`。
  - `toCaptureState`：AwaitingManualSubmit/Enqueuing 分支删除；Completed 文案"定位完成"；`showLowQualityWarning = lastQualityFlags.contains(LOW_QUALITY_ACCURACY_FLAG)`。
  - 删 `submitCurrentLocationManually()`。
- `LocationUiState.kt`：删 `showSubmit`/`isSubmitting`；加 `showLowQualityWarning`；phaseLabel 删 AwaitingManualSubmit/Enqueuing。
- `LocationViewModel.kt`：删 `submit()`。
- `LocationScreen.kt`：删提交按钮块（:183-195）；Completed + 低质量时在状态区显示警告文本（红色，文案"精度不足，已标记低质量"）。
- `PimAppScaffold.kt`：LocationTab 删 onSubmit 参数与"提交状态"行；精度规则行文案保留（LocationSubmissionPolicy 默认 20f）。
- `LocationLiveUpdatePublisher.kt`：handleState 对 `phase == StreamActive` 或 state 侧 Idle 期间不做发布（现有 `phase != Acquiring && != Evaluating → cancelAndReset` 已覆盖 StreamActive；确认 streamState 不进入 state）。

- [ ] **步骤 4：运行验证通过**

运行：`./gradlew :app:testDebugUnitTest --no-daemon`
预期：全量 PASS。

- [ ] **步骤 5：Commit**

```bash
git add -A src/client-android/app/src
git commit -m "feat: 定位页移除手动提交步骤，低质量回退点明确提示 / drop manual submit step and surface low-quality fallback in UI"
```

---

### 任务 S3-5：Stage 3 全量验证 + PR

- [ ] **步骤 1：全量单测**：`./gradlew :app:testDebugUnitTest --no-daemon` 全绿。
- [ ] **步骤 2：改动范围确认**：`git status --short --branch` + `git diff --stat`，仅预期文件。
- [ ] **步骤 3：Push + 开 PR**（标题：`feat: 统一定位采集引擎 + 常驻流（Stage 3）/ unified location engine and persistent stream (Stage 3)`；四段双语模板同上；功能变化：手动定位自动入库、低质量明示、自动采集常驻高精度流）。
- [ ] **步骤 4：等 CI 绿**。
- [ ] **步骤 5：合入后清理 worktree 与分支**（`git worktree remove` + 本地分支删除）。

---

## 验收清单（三阶段合入后）

1. 入库点全部为 <20m 或带 `low-quality-accuracy` 标记（手动回退）——数据库抽查：`mobile_location_points` 中 `raw_json->>'qualityFlags'` 不含标记的点精度均 <20m。
2. 手动触发即自动入库，UI 无"提交位置"按钮；低质量回退点在 UI 与数据中双明示。
3. 自动采集为常驻流：策略间隔变化（静止↔运动）时重新注册 LocationRequest；无"上一个没完成"问题。
4. 运动状态实时更新（走路→Walking / 骑车→Moving / 静止→Still），不再永远 Unknown；日程内运动可打破降频（锚点 >100m 恢复）。
5. 无位置信息的日程不再降频。
6. 运动档频率：走路=设置值，跑步/骑车/开车/Moving=30s；静止 3min、日程 15min。
7. 所有改动经 `./gradlew :app:testDebugUnitTest --no-daemon` 全量绿 + PR CI 绿。

## 二期（不在本期范围）

- 自行车实测与振动特征细分类型（Running/OnBicycle/InVehicle 实产）
- 服务端轨迹平滑
- Live Update 在常驻流场景的发布策略（等 android-36.1 模拟器镜像修复）
