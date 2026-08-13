# 采集端高速轨迹模式 实现计划

> **面向 AI 代理的工作者：** 步骤使用复选框（`- [ ]`）语法跟踪进度。
> 设计文档：`PR63_采集端高速轨迹模式`（决策人：用户，全部参数已拍板）。

**目标：** 采集端（Android）在相对高速运动时自动从间隔采样升级为 2.5s 轨迹级密集采样，并在通知栏、应用内、流体云（Live Update）三处提示状态。

**架构：**
- 纯逻辑状态机 `HighSpeedTracker`（不依赖 Android API，可单测）：GPS speed ≥ 8 km/h 持续 10s → Active（触发期 Accumulating 已切 2.5s 密集采样以便快速确认）；Active 期间 speed < 1 km/h 持续 60s → Inactive（回落恢复策略引擎常规档）。
- 状态机挂在 `LocationPolicyEngine` 内（最高优先级：高速档 > 常规策略档 > 日程降频），`reduce()` 每次调用观察最新 GPS speed。
- 服务自动循环等待条件从「运动信号变化或 30s」扩展为「运动信号变化或新 fix 入库或 30s」：`recordAccepted` 记录最新 speed 并 bump `fixRecordedSignal`，循环醒来重算策略并（按需）重注册常驻流 → 间隔变化即时生效。
- 三处提示共用服务运行时状态 `ForegroundLocationRuntimeState`（新增 3 字段）：7101 通知栏（高速档专用文案）、LocationScreen 应用内行、`LocationLiveUpdatePublisher` 观察同一状态流发布/取消 7102 Live Update（高速档优先覆盖会话 LU，单通知 ID 切换）。

**技术栈：** Kotlin, kotlinx.coroutines StateFlow, Compose UI, Robolectric/JUnit 单测。CI 门禁 `:app:testDebugUnitTest` + `:app:assembleDebug`。

---

## 参数（用户已拍板，写死为常量）

| 参数 | 值 |
|---|---|
| 触发速度 | ≥ 8 km/h（≈2.2222 m/s） |
| 触发防抖 | 10s |
| 高速档采样间隔 | 2.5s |
| 回落速度 | < 1 km/h（≈0.2778 m/s） |
| 回落防抖 | 60s |

## 文件清单

**创建：**
- `app/src/main/java/com/pim/app/location/highspeed/HighSpeedTracker.kt` — 纯状态机
- `app/src/test/java/com/pim/app/location/highspeed/HighSpeedTrackerTest.kt`

**修改：**
- `app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt` — 新增枚举 `HighSpeed`、间隔常量、`LocationPolicyInput.speedMetersPerSecond`
- `app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt` — 集成 tracker，最高优先级分支
- `app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt` — 高速档优先级用例
- `app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt` — 新增 3 字段
- `app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt` — lastSpeed、fixRecordedSignal、循环唤醒、notification()/publishRuntimeState() 扩展
- `app/src/test/java/com/pim/app/location/service/ForegroundLocationServiceTest.kt` — 集成用例
- `app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt` — 高速档文案 + modeLabel
- `app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt`
- `app/src/main/java/com/pim/app/status/StatusDisplayText.kt` — "HighSpeed" → "高速轨迹"
- `app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisher.kt` — 高速档观察 + 优先级
- `app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRenderer.kt` — `tryBuildAndNotifyHighSpeed`
- `app/src/main/java/com/pim/app/location/acquisition/LocationAcquisitionModule.kt` — DI 注入 highSpeedFlow
- `app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdatePublisherTest.kt`
- `app/src/test/java/com/pim/app/location/liveupdate/LocationLiveUpdateNotificationRendererTest.kt`
- `app/src/main/java/com/pim/app/ui/location/LocationUiState.kt` / `LocationViewModel.kt` / `LocationScreen.kt` — 应用内提示
- `app/src/test/java/com/pim/app/ui/location/LocationViewModelTest.kt`

---

### 任务 1：HighSpeedTracker 纯状态机

**文件：** 创建 `app/src/main/java/com/pim/app/location/highspeed/HighSpeedTracker.kt`、`app/src/test/java/com/pim/app/location/highspeed/HighSpeedTrackerTest.kt`

- [ ] 步骤 1：写失败测试（RED）：触发（≥8km/h 持续 10s）、临界（7.9 不触发）、回落（<1km/h 持续 60s）、红灯 30s 不掉档、触发后立即回落、速度波动重置、null 语义（未激活不触发；激活计 slow）、恰好 10s 边界、reset()
- [ ] 步骤 2：运行确认失败（`./gradlew :app:testDebugUnitTest --tests "*HighSpeedTrackerTest"`）
- [ ] 步骤 3：实现 `HighSpeedTracker`（模式 Inactive/Accumulating/Active；`observe(speedMetersPerSecond: Float?)`；`activeSinceElapsedRealtimeMillis`；`reset()`）
- [ ] 步骤 4：运行确认通过
- [ ] 步骤 5：Commit `feat: 高速轨迹状态机 / high-speed track state machine`

### 任务 2：策略引擎集成

**文件：** 修改 `LocationPolicyTypes.kt`、`LocationPolicyEngine.kt`、`LocationPolicyEngineTest.kt`

- [ ] 步骤 1：RED — 测试：Accumulating/Active 时 reduce 返回 HighSpeed + 2500ms；优先级覆盖日程低频/运动档；Inactive 时原行为不变；reason 区分
- [ ] 步骤 2：确认失败
- [ ] 步骤 3：实现 — 枚举加 `HighSpeed`；`TrackingIntervalBounds.HIGH_SPEED_INTERVAL_MILLIS = 2_500L`（+MIN/MAX 2000/5000 约束）；`LocationPolicyInput.speedMetersPerSecond: Float? = null`；引擎构造注入 `HighSpeedTracker`（默认 `SystemClock.elapsedRealtime()`），`reduce()` 先 `observe` 再在 collectionEnabled 后、日程/运动判断前返回高速档决策
- [ ] 步骤 4：确认通过
- [ ] 步骤 5：Commit `feat: 策略引擎接入高速轨迹档 / integrate high-speed mode into policy engine`

### 任务 3：服务集成

**文件：** 修改 `ForegroundLocationService.kt`、`ForegroundLocationRuntimeState.kt`、`ForegroundLocationServiceTest.kt`

- [ ] 步骤 1：RED — 测试：emit 高速 fix（9 m/s）→ 循环重注册流为 2500ms；连续高速 fix + idleFor(10s) → `runtimeState.highSpeedActive == true`；连续低速 fix + idleFor(60s) → false；通知栏文案含「高速轨迹记录中」
- [ ] 步骤 2：确认失败
- [ ] 步骤 3：实现 — `lastSpeedMetersPerSecond`、`fixRecordedSignal: MutableStateFlow<Long>`；`recordAccepted` 记录 speed + bump；循环等待改 `merge(运动信号变化, fixRecordedSignal变化).first()`（保留 `withTimeoutOrNull(30_000L)`）；`recomputePolicyDecision` 传 speed；`publishRuntimeState`/`notification()` 扩展
- [ ] 步骤 4：确认通过（含既有 ForegroundLocationServiceTest 全绿）
- [ ] 步骤 5：Commit `feat: 服务按高速档驱动密集采样与运行时状态 / drive dense sampling from service loop`

### 任务 4：通知栏 7101 + 状态中心文案

**文件：** 修改 `LocationNotificationRenderer.kt`、`StatusDisplayText.kt` 及各自测试

- [ ] 步骤 1：RED — `LocationNotificationState.highSpeedActive` 时 collapsed 含「高速轨迹记录中」；expanded 含已记录时长；`modeLabel(HighSpeed) == "高速轨迹"`；`StatusDisplayText.policyMode("HighSpeed") == "高速轨迹"`
- [ ] 步骤 2：确认失败
- [ ] 步骤 3：实现（`highSpeedActive/highSpeedElapsedSeconds` 默认参数保持既有测试编译；`modeLabel` 加分支）
- [ ] 步骤 4：确认通过
- [ ] 步骤 5：Commit `feat: 高速档通知栏与状态中心文案 / high-speed notification and status text`

### 任务 5：Live Update 高速档

**文件：** 修改 `LocationLiveUpdatePublisher.kt`、`LocationLiveUpdateNotificationRenderer.kt`、`LocationAcquisitionModule.kt`、`PimApp.kt`（如需）及测试

- [ ] 步骤 1：RED — 测试：highSpeedActive → 发布高速内容；inactive → 取消；活跃期间节流（10s 内多次 emission 只发一次）；高速档发布时抑制会话发布；回落且会话仍 Acquiring 时恢复会话发布；`cancelStaleNotification` 不误取消高速 LU
- [ ] 步骤 2：确认失败
- [ ] 步骤 3：实现 — 渲染器加 `tryBuildAndNotifyHighSpeed`（标题「高速轨迹记录中」、内容含已记录时长、ongoing、无取消 action）；发布器加 `highSpeedFlow: StateFlow<ForegroundLocationRuntimeState>? = null` 与 `startHighSpeed` 观察逻辑（synchronized 同锁、`handleState` 顶部 `if (highSpeedPublished) return`、回落时重放 `handleState(stateFlow.value)`）；DI 注入 `ForegroundLocationService.runtimeState`
- [ ] 步骤 4：确认通过（含既有 LocationLiveUpdatePublisherTest 全绿）
- [ ] 步骤 5：Commit `feat: 高速档 Live Update 发布与优先级 / high-speed Live Update publishing`

### 任务 6：应用内定位页 UI

**文件：** 修改 `LocationUiState.kt`、`LocationViewModel.kt`、`LocationScreen.kt`、`LocationViewModelTest.kt`

- [ ] 步骤 1：RED — `mapToLocationUiState`（runtime 默认参数）映射 `highSpeedActive/highSpeedElapsedSeconds`；LocationScreen StatusSection 激活时渲染「高速轨迹」行（testTag `location-highspeed-status`）
- [ ] 步骤 2：确认失败
- [ ] 步骤 3：实现 — ViewModel combine 加入 `ForegroundLocationService.runtimeState`
- [ ] 步骤 4：确认通过
- [ ] 步骤 5：Commit `feat: 定位页显示高速轨迹状态 / show high-speed mode on location screen`

### 任务 7：全量验证

- [ ] `./gradlew :app:testDebugUnitTest --offline` 全绿
- [ ] `./gradlew :app:assembleDebug --offline` 通过
- [ ] `git status --short --branch` 仅预期变更

### 任务 8：提交推送开 PR

- [ ] 双语提交、push `opencode-linux/highspeed-mode`
- [ ] `gh pr create`（四段式双语 PR 描述），等 CI（build-android）绿
