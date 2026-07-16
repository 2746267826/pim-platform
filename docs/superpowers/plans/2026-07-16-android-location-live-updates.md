# Android Location Live Updates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 PIM Android 已有前台定位过程增加 Android 16 Live Updates（流体云）展示：Presenter 状态机 + 单通知 7101 + API 36 promote，不改采样策略。

**Architecture:** `ForegroundLocationService` 只派发 `LocationLiveUpdateEvent`；`LocationLiveUpdatePresenter` 纯 Kotlin 产出 `LocationNotificationUiModel`（含 30s SuccessHold）；`LocationNotificationRenderer.build(context, model)` 组装通知；`LiveUpdateNotificationCompat` 在 API 36+ 尝试 promote/ProgressStyle，失败静默回退。同一条 FGS 通知，无第二条。

**Tech Stack:** Kotlin, AndroidX NotificationCompat, Robolectric 4.12.2 (`@Config(sdk=[34])`), JUnit4, compileSdk 36 / targetSdk 34（初版）, Handler main looper.

**Spec:** `docs/superpowers/specs/2026-07-16-android-location-live-updates-design.md`

**Branch:** `codex/android-location-live-updates`（已有 design commit）

**Subagent policy (user-required):** 写本计划期间已并行使用 ≥10 个子代理调研；**执行本计划时也必须全程使用 ≥10 个子代理**（实现 / 测试 / 审查 / 验证拆分，禁止单会话串行包办全部 Task）。

---

## Final Objective

用户开启连续定位后，通知 `7101` 持续反映 Presenter 状态；API 36 设备请求 Live Update 提升；定位 accepted 后主句约 30s 显示精度摘要再回落；暂停/停止/同步回归绿；`LocationPolicyEngine` 零 diff。

## Do Not Touch

- `LocationPolicyEngine` 及任何采样 interval / distanceFilter / 质量门阈值
- 上传协议、队列语义、服务端 API
- 第二条定位通知 / OEM 私有流体云
- 轨迹地图虚线渲染
- 除非 CI 无法安装 android-36，否则不改无关 workflow；**允许**仅为安装 `platforms;android-36` 最小改动 `build-android.yml`（见 Task 8）

## File Structure Map

| Path | Action | Responsibility |
|---|---|---|
| `src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdateModels.kt` | Create | Phase, Event, UiModel, degraded kind |
| `src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdatePresenter.kt` | Create | reduce / current / deadline / all copy |
| `src/client-android/app/src/main/java/com/pim/app/notifications/LiveUpdateNotificationCompat.kt` | Create | API 36 gate + promote/ProgressStyle try/catch |
| `src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt` | Modify | `build(UiModel)` only; drop State copy helpers |
| `src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt` | Modify | dispatch + hold tick; no copy |
| `src/client-android/app/build.gradle.kts` | Modify | compileSdk 36 |
| `src/client-android/core/build.gradle.kts` | Modify | compileSdk 36 |
| `src/client-android/features/calendar/build.gradle.kts` | Modify | compileSdk 36 |
| `.github/workflows/build-android.yml` | Modify (minimal) | install platforms;android-36 |
| `.../LocationLiveUpdatePresenterTest.kt` | Create | pure JVM |
| `.../LocationNotificationRendererTest.kt` | Modify | UiModel |
| `.../LiveUpdateNotificationCompatTest.kt` | Create | SDK gate |
| `.../ForegroundLocationServiceTest.kt` | Modify | State → UiModel helpers only |

## Shared Types (locked — all tasks must use these names)

```kotlin
// LocationLiveUpdateModels.kt
package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode

enum class LocationLiveUpdatePhase {
    Collecting,
    SuccessHold,
    Degraded,
    Paused
}

enum class LocationDegradedKind {
    Drop,
    Provider,
    Permission
}

sealed class LocationLiveUpdateEvent {
    data class Snapshot(
        val mode: LocationPolicyMode,
        val nextExpectedLocationText: String,
        val lastAcceptedLocationText: String,
        val lastAccuracyText: String,
        val pendingUploadCount: Int,
        val apiState: String,
        val lastDroppedReason: String?,
        val nextExpectedAtMillis: Long?,
        val lastAcceptedAtMillis: Long?,
        val requestIntervalMillis: Long? = null,
        val permissionOk: Boolean = true,
        val providerEnabled: Boolean = true
    ) : LocationLiveUpdateEvent()

    data class Accepted(
        val lastAcceptedLocationText: String,
        val lastAccuracyText: String,
        val lastAcceptedAtMillis: Long,
        val pendingUploadCount: Int? = null,
        val apiState: String? = null
    ) : LocationLiveUpdateEvent()

    data class Dropped(val reason: String) : LocationLiveUpdateEvent()

    data class PolicyChanged(
        val mode: LocationPolicyMode,
        val nextExpectedLocationText: String,
        val nextExpectedAtMillis: Long?,
        val requestIntervalMillis: Long? = null
    ) : LocationLiveUpdateEvent()

    data class ApiChanged(val apiState: String) : LocationLiveUpdateEvent()
    data class QueueChanged(val pendingUploadCount: Int) : LocationLiveUpdateEvent()
    data class ProviderDisabled(val provider: String? = null) : LocationLiveUpdateEvent()
    data object Paused : LocationLiveUpdateEvent()
    data object Tick : LocationLiveUpdateEvent()
}

data class LocationNotificationUiModel(
    val phase: LocationLiveUpdatePhase,
    val mode: LocationPolicyMode,
    val isOngoing: Boolean,
    val requestLiveUpdate: Boolean,
    val title: String,
    val collapsedText: String,
    val expandedText: String,
    val shortStatus: String,
    val progressPercent: Int?,
    val contentAction: CollectionControlAction
)
```

### Locked copy rules

| Case | Exact string |
|---|---|
| Collecting, no fix | `定位中 · 等待首次定位` |
| Collecting, has fix | `定位中 · {刚刚\|N秒前\|N分钟前\|HH:mm}` |
| SuccessHold | `已定位 · 精度 {lastAccuracyText}` |
| Degraded permission | `无法定位 · 权限不足` |
| Degraded provider | `定位中断 · GPS/网络已关` |
| Degraded drop (primary only if not SuccessHold) | `定位异常 · {reason}` |
| Paused | `定位已暂停` |
| shortStatus | 省电 / 日程低频 / 运动 / 移动恢复 / 同步兜底 / 已暂停 |
| full mode (expanded 策略) | 省电档 / 日程低频 / 运动观察 / 移动恢复 / 同步兜底 / 已暂停（= 现 `modeLabel`） |
| title | `PIM 定位` |

Relative time: `[0,10s)→刚刚`; `[10s,60s)→N秒前`; `[60s,60min)→N分钟前`; else `HH:mm`.

Progress: `null` if Paused/Off/no nextExpected; else linear from lastAccepted (or next-interval) to nextExpected, clamp 0..100; overdue → 100.

Phase priority: `Paused > Degraded(permission/provider) > SuccessHold > Degraded(drop soft) > Collecting`. Soft drop does **not** break SuccessHold primary; still in expanded.

### Gradle cwd

Always from `src/client-android` on Windows:

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationLiveUpdatePresenterTest" --no-daemon
```

---

### Task 1: Presenter RED tests (pure JVM)

**Files:**
- Create: `src/client-android/app/src/test/java/com/pim/app/notifications/LocationLiveUpdatePresenterTest.kt`
- (types not yet — expect compile fail)

- [ ] **Step 1: Write failing tests covering design cases 1–10**

Create the test file with fake clock and at least:

1. Snapshot active → Collecting + `定位中 · 等待首次定位`
2. Accepted → SuccessHold + deadline `now+30_000` + `已定位 · 精度 18m`
3. Second Accepted resets deadline
4. Expired Tick → Collecting
5. Early Tick keeps SuccessHold
6. Paused during hold → Paused, deadline null, progress null, not ongoing
7. Dropped during SuccessHold → still SuccessHold primary; expanded has `最近丢弃：…`
8. ProviderDisabled / permission Snapshot → degraded primary strings
9. All mode shortStatus labels
10. progressPercent in 0..100; null when paused; overdue clamps 100

Use types from **Shared Types** section exactly. Seed helper:

```kotlin
private var now = 1_720_000_000_000L
private val presenter get() = LocationLiveUpdatePresenter(
    successHoldMillis = 30_000L,
    clock = { now }
)

private fun snapshot(
    mode: LocationPolicyMode = LocationPolicyMode.PowerSavingNormal,
    permissionOk: Boolean = true,
    providerEnabled: Boolean = true,
    lastAcceptedAtMillis: Long? = null,
    nextExpectedAtMillis: Long? = now + 180_000L
) = LocationLiveUpdateEvent.Snapshot(
    mode = mode,
    nextExpectedLocationText = "3 分钟后",
    lastAcceptedLocationText = lastAcceptedAtMillis?.let { "21:24" } ?: "无",
    lastAccuracyText = if (lastAcceptedAtMillis == null) "无" else "18m",
    pendingUploadCount = 0,
    apiState = "正常",
    lastDroppedReason = null,
    nextExpectedAtMillis = nextExpectedAtMillis,
    lastAcceptedAtMillis = lastAcceptedAtMillis,
    requestIntervalMillis = 180_000L,
    permissionOk = permissionOk,
    providerEnabled = providerEnabled
)
```

Example assertion for accepted:

```kotlin
@Test
fun accepted_entersSuccessHold_withDeadline() {
    val p = LocationLiveUpdatePresenter(30_000L) { now }
    p.reduce(snapshot())
    val ui = p.reduce(
        LocationLiveUpdateEvent.Accepted(
            lastAcceptedLocationText = "21:24",
            lastAccuracyText = "18m",
            lastAcceptedAtMillis = now
        )
    )
    assertEquals(LocationLiveUpdatePhase.SuccessHold, ui.phase)
    assertEquals("已定位 · 精度 18m", ui.collapsedText)
    assertEquals(now + 30_000L, p.successHoldDeadlineMillis())
    assertTrue(ui.isOngoing)
    assertTrue(ui.requestLiveUpdate)
}
```

- [ ] **Step 2: Run RED**

```bat
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationLiveUpdatePresenterTest" --no-daemon
```

Expected: compile error — unresolved `LocationLiveUpdatePresenter` / events.

- [ ] **Step 3: Commit test file**

```bash
git add src/client-android/app/src/test/java/com/pim/app/notifications/LocationLiveUpdatePresenterTest.kt
git commit -m "test: add LocationLiveUpdatePresenter RED tests"
```

---

### Task 2: Models + Presenter GREEN (core phase machine)

**Files:**
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdateModels.kt`
- Create: `src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdatePresenter.kt`
- Test: Task 1 file

- [ ] **Step 1: Add models file** with Shared Types exactly.

- [ ] **Step 2: Implement Presenter skeleton**

```kotlin
class LocationLiveUpdatePresenter(
    private val successHoldMillis: Long = 30_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var phase = LocationLiveUpdatePhase.Paused
    private var mode = LocationPolicyMode.Off
    private var nextExpectedLocationText = "暂停"
    private var lastAcceptedLocationText = "无"
    private var lastAccuracyText = "无"
    private var pendingUploadCount = 0
    private var apiState = "正常"
    private var lastDroppedReason: String? = null
    private var nextExpectedAtMillis: Long? = null
    private var lastAcceptedAtMillis: Long? = null
    private var requestIntervalMillis: Long? = null
    private var permissionOk = true
    private var providerEnabled = true
    private var degradedKind: LocationDegradedKind? = null
    private var successHoldUntil: Long? = null
    private var lastUi: LocationNotificationUiModel = buildUi()

    fun reduce(event: LocationLiveUpdateEvent): LocationNotificationUiModel {
        when (event) {
            is LocationLiveUpdateEvent.Snapshot -> applySnapshot(event)
            is LocationLiveUpdateEvent.Accepted -> applyAccepted(event)
            is LocationLiveUpdateEvent.Dropped -> {
                lastDroppedReason = event.reason
                if (phase != LocationLiveUpdatePhase.SuccessHold) {
                    degradedKind = LocationDegradedKind.Drop
                }
            }
            is LocationLiveUpdateEvent.PolicyChanged -> {
                mode = event.mode
                nextExpectedLocationText = event.nextExpectedLocationText
                nextExpectedAtMillis = event.nextExpectedAtMillis
                event.requestIntervalMillis?.let { requestIntervalMillis = it }
            }
            is LocationLiveUpdateEvent.ApiChanged -> apiState = event.apiState
            is LocationLiveUpdateEvent.QueueChanged -> pendingUploadCount = event.pendingUploadCount
            is LocationLiveUpdateEvent.ProviderDisabled -> {
                providerEnabled = false
                degradedKind = LocationDegradedKind.Provider
            }
            LocationLiveUpdateEvent.Paused -> {
                mode = LocationPolicyMode.Off
                successHoldUntil = null
                nextExpectedLocationText = "暂停"
                nextExpectedAtMillis = null
            }
            LocationLiveUpdateEvent.Tick -> {
                val until = successHoldUntil
                if (until != null && clock() >= until) {
                    successHoldUntil = null
                }
            }
        }
        phase = resolvePhase()
        lastUi = buildUi()
        return lastUi
    }

    fun current(): LocationNotificationUiModel = lastUi

    fun successHoldDeadlineMillis(): Long? = successHoldUntil

    private fun applyAccepted(event: LocationLiveUpdateEvent.Accepted) {
        lastAcceptedLocationText = event.lastAcceptedLocationText
        lastAccuracyText = event.lastAccuracyText
        lastAcceptedAtMillis = event.lastAcceptedAtMillis
        event.pendingUploadCount?.let { pendingUploadCount = it }
        event.apiState?.let { apiState = it }
        lastDroppedReason = null
        successHoldUntil = event.lastAcceptedAtMillis + successHoldMillis
        if (mode == LocationPolicyMode.Off) {
            mode = LocationPolicyMode.PowerSavingNormal
        }
        permissionOk = true
        providerEnabled = true
        degradedKind = null
    }

    private fun applySnapshot(event: LocationLiveUpdateEvent.Snapshot) {
        mode = event.mode
        nextExpectedLocationText = event.nextExpectedLocationText
        lastAcceptedLocationText = event.lastAcceptedLocationText
        lastAccuracyText = event.lastAccuracyText
        pendingUploadCount = event.pendingUploadCount
        apiState = event.apiState
        lastDroppedReason = event.lastDroppedReason
        nextExpectedAtMillis = event.nextExpectedAtMillis
        lastAcceptedAtMillis = event.lastAcceptedAtMillis
        requestIntervalMillis = event.requestIntervalMillis
        permissionOk = event.permissionOk
        providerEnabled = event.providerEnabled
        successHoldUntil = null
        degradedKind = when {
            !event.permissionOk -> LocationDegradedKind.Permission
            !event.providerEnabled -> LocationDegradedKind.Provider
            else -> null
        }
    }

    private fun resolvePhase(): LocationLiveUpdatePhase {
        if (mode == LocationPolicyMode.Off) return LocationLiveUpdatePhase.Paused
        if (!permissionOk) {
            degradedKind = LocationDegradedKind.Permission
            return LocationLiveUpdatePhase.Degraded
        }
        if (!providerEnabled) {
            degradedKind = LocationDegradedKind.Provider
            return LocationLiveUpdatePhase.Degraded
        }
        val until = successHoldUntil
        if (until != null && clock() < until) return LocationLiveUpdatePhase.SuccessHold
        successHoldUntil = null
        if (degradedKind == LocationDegradedKind.Drop && lastDroppedReason != null) {
            return LocationLiveUpdatePhase.Degraded
        }
        return LocationLiveUpdatePhase.Collecting
    }

    private fun buildUi(): LocationNotificationUiModel {
        val p = resolvePhase()
        val ongoing = p != LocationLiveUpdatePhase.Paused && mode != LocationPolicyMode.Off
        return LocationNotificationUiModel(
            phase = p,
            mode = mode,
            isOngoing = ongoing,
            requestLiveUpdate = ongoing,
            title = "PIM 定位",
            collapsedText = buildCollapsedText(p),
            expandedText = buildExpandedText(p),
            shortStatus = modeShortLabel(mode),
            progressPercent = progressPercent(p),
            contentAction = collectionControlAction(mode)
        )
    }

    // implement buildCollapsedText / buildExpandedText / modeShortLabel /
    // modeFullLabel / formatRelativeTime / progressPercent per Locked copy rules
}
```

- [ ] **Step 3: Run GREEN for Task 1 tests**

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationLiveUpdatePresenterTest" --no-daemon
```

Expected: PASS (add any missing assertions from Task 1 that still fail).

- [ ] **Step 4: Commit**

```bash
git add src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdateModels.kt \
  src/client-android/app/src/main/java/com/pim/app/notifications/LocationLiveUpdatePresenter.kt \
  src/client-android/app/src/test/java/com/pim/app/notifications/LocationLiveUpdatePresenterTest.kt
git commit -m "feat: add LocationLiveUpdatePresenter phase machine"
```

---

### Task 3: Presenter copy completeness (expanded order, relative time, progress)

**Files:**
- Modify: `LocationLiveUpdatePresenter.kt`
- Modify: `LocationLiveUpdatePresenterTest.kt`

- [ ] **Step 1: Add failing tests** for expanded fixed order, API prefix de-dupe (`API 无法连接` → line `待上传 n，API 无法连接` not `API API`), relative time buckets, progress table:

| now-last | next-last | expected |
|---|---|---|
| 0 | 100s | ~0 |
| 50s | 100s | 50 |
| 100s | 100s | 100 |
| 150s | 100s | 100 |

- [ ] **Step 2: RED then implement helpers exactly**

```kotlin
internal fun formatRelativeTime(nowMillis: Long, lastUpdateMillis: Long?, neverText: String): String {
    if (lastUpdateMillis == null) return neverText
    val delta = (nowMillis - lastUpdateMillis).coerceAtLeast(0L)
    return when {
        delta < 10_000L -> "刚刚"
        delta < 60_000L -> "${delta / 1_000L}秒前"
        delta < 3_600_000L -> "${delta / 60_000L}分钟前"
        else -> ForegroundLocationService.timeFormatter.format(
            // Prefer java.time in presenter without Service dependency:
            // DateTimeFormatter.ofPattern("HH:mm") with system zone
            java.time.Instant.ofEpochMilli(lastUpdateMillis)
                .atZone(java.time.ZoneId.systemDefault())
        )
    }
}
```

Do **not** import Service from Presenter for formatter — use local `DateTimeFormatter.ofPattern("HH:mm")`.

- [ ] **Step 3: GREEN + commit**

```bash
git commit -am "feat: complete live-update presenter copy and progress"
```

---

### Task 4: Renderer UiModel contract (RED→GREEN)

**Files:**
- Modify: `LocationNotificationRenderer.kt`
- Modify: `LocationNotificationRendererTest.kt`

- [ ] **Step 1: Rewrite tests to UiModel fixture**

Keep:
- `collectionControlActionShowsPauseWhenActive`
- `collectionControlActionShowsResumeWhenPaused`

Rewrite:
- `ongoingEventFlagWhenActive` / `noOngoingEventWhenPaused` / `pausedStateShowsResumeAction` to use `uiModel(...)`

Move copy tests that called `collapsedText(state)` / `expandedText(state)` into PresenterTest (delete from RendererTest).

Add:
- content from model
- channel id `pim_location_collection`
- `NOTIFICATION_ID == 7101`
- actions order active/paused

```kotlin
private fun uiModel(
    mode: LocationPolicyMode = LocationPolicyMode.PowerSavingNormal,
    isOngoing: Boolean = mode != LocationPolicyMode.Off,
    requestLiveUpdate: Boolean = isOngoing,
    collapsedText: String = "定位中 · 刚刚",
    expandedText: String = "状态：定位中\n策略：省电档"
) = LocationNotificationUiModel(
    phase = if (isOngoing) LocationLiveUpdatePhase.Collecting else LocationLiveUpdatePhase.Paused,
    mode = mode,
    isOngoing = isOngoing,
    requestLiveUpdate = requestLiveUpdate,
    title = "PIM 定位",
    collapsedText = collapsedText,
    expandedText = expandedText,
    shortStatus = "省电",
    progressPercent = 40,
    contentAction = collectionControlAction(mode)
)
```

- [ ] **Step 2: RED**

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationNotificationRendererTest" --no-daemon
```

- [ ] **Step 3: Change `build`**

```kotlin
fun build(context: Context, model: LocationNotificationUiModel): Notification {
    ensureChannel(context)
    val control = model.contentAction
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setContentTitle(model.title)
        .setContentText(model.collapsedText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(model.expandedText))
        .setOngoing(model.isOngoing)
        .setOnlyAlertOnce(true)
        .setContentIntent(openStatusPendingIntent(context))
        .addAction(0, control.label, receiverPendingIntent(context, control.action, 10))
        .addAction(0, "同步", receiverPendingIntent(context, ForegroundLocationController.ACTION_SYNC_NOW, 11))
        .addAction(0, "状态", receiverPendingIntent(context, ForegroundLocationController.ACTION_OPEN_STATUS, 12))
    return LiveUpdateNotificationCompat.applyIfSupported(builder, model).build()
}
```

Remove production `LocationNotificationState` data class and `collapsedText(state)` / `expandedText(state)` after no main references.

Keep `CHANNEL_ID`, `NOTIFICATION_ID`, channel importance LOW, action request codes 10/11/12/20.

Temporary: if Compat helper not yet created, stub:

```kotlin
// only if Task 5 not done — better create empty applyIfSupported that returns builder
```

- [ ] **Step 4: GREEN + commit**

```bash
git commit -am "feat: build location notification from UiModel"
```

---

### Task 5: LiveUpdateNotificationCompat

**Files:**
- Create: `LiveUpdateNotificationCompat.kt`
- Create: `LiveUpdateNotificationCompatTest.kt` (Robolectric sdk 34)
- Modify: Renderer (already calls helper)

- [ ] **Step 1: Failing tests** — `@Config(sdk=[34])` build with `requestLiveUpdate=true` does not throw; channel id present.

- [ ] **Step 2: Implement**

```kotlin
object LiveUpdateNotificationCompat {
    private const val MIN_SDK = 36

    fun applyIfSupported(
        builder: NotificationCompat.Builder,
        model: LocationNotificationUiModel
    ): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT < MIN_SDK) return builder
        if (!model.requestLiveUpdate || !model.isOngoing) return builder
        return try {
            applyApi36(builder, model)
        } catch (_: Throwable) {
            builder
        }
    }

    @androidx.annotation.RequiresApi(36)
    private fun applyApi36(
        builder: NotificationCompat.Builder,
        model: LocationNotificationUiModel
    ): NotificationCompat.Builder {
        // Prefer androidx APIs if on classpath after compileSdk 36:
        // builder.setRequestPromotedOngoing(true)
        // builder.setShortCriticalText(model.shortStatus.take(7))
        // ProgressStyle with model.progressPercent
        //
        // If symbols missing until Task 8, use reflection on Notification.Builder
        // after builder.build() is not ideal — keep try/catch no-op until compileSdk 36.
        // After Task 8, implement real calls here.
        builder.setColorized(true)
        return builder
    }
}
```

After Task 8 compileSdk 36, flesh out real ProgressStyle + requestPromotedOngoing (Compat if available, else platform extras key `android.requestPromotedOngoing` + `Notification.ProgressStyle`).

**Promotion eligibility note:** system may require ongoing + title + promotable style + often colorized. First version: set colorized + ProgressStyle when API present; never crash.

- [ ] **Step 3: GREEN + commit**

```bash
git commit -am "feat: add LiveUpdateNotificationCompat API gate"
```

---

### Task 6: Service integration (dispatch + hold tick)

**Files:**
- Modify: `ForegroundLocationService.kt` only (not PolicyEngine)

- [ ] **Step 1: Add fields**

```kotlin
private val liveUpdatePresenter = LocationLiveUpdatePresenter()
private val mainHandler = Handler(Looper.getMainLooper())
private var successHoldTick: Runnable? = null
@Volatile private var destroyed = false
```

- [ ] **Step 2: Replace notification pipeline**

```kotlin
private fun dispatch(event: LocationLiveUpdateEvent) {
    liveUpdatePresenter.reduce(event)
    publishRuntimeState()
    if (destroyed) return
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.notify(
        LocationNotificationRenderer.NOTIFICATION_ID,
        LocationNotificationRenderer.build(this, liveUpdatePresenter.current())
    )
    scheduleSuccessHoldTick(liveUpdatePresenter.successHoldDeadlineMillis())
}

private fun notification(): Notification {
    return LocationNotificationRenderer.build(this, liveUpdatePresenter.current())
}

private fun scheduleSuccessHoldTick(deadline: Long?) {
    clearSuccessHoldTick()
    if (deadline == null || destroyed) return
    val delay = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
    val tick = Runnable {
        successHoldTick = null
        if (destroyed) return@Runnable
        if (currentDecision.mode == LocationPolicyMode.Off) return@Runnable
        dispatch(LocationLiveUpdateEvent.Tick)
    }
    successHoldTick = tick
    mainHandler.postDelayed(tick, delay)
}

private fun clearSuccessHoldTick() {
    successHoldTick?.let { mainHandler.removeCallbacks(it) }
    successHoldTick = null
}

private fun snapshotEvent(): LocationLiveUpdateEvent.Snapshot =
    LocationLiveUpdateEvent.Snapshot(
        mode = currentDecision.mode,
        nextExpectedLocationText = nextExpectedLocationText(currentDecision),
        lastAcceptedLocationText = lastAcceptedLocationText,
        lastAccuracyText = lastAccuracyText,
        pendingUploadCount = pendingUploadCount,
        apiState = apiState,
        lastDroppedReason = lastDroppedReason,
        nextExpectedAtMillis = currentDecision.nextExpectedLocationAtMillis
            .takeUnless { it == Long.MAX_VALUE },
        lastAcceptedAtMillis = null, // set if you track millis; optional first pass
        requestIntervalMillis = currentDecision.requestIntervalMillis.takeIf { it > 0L },
        permissionOk = hasRequiredLocationPermissions(),
        providerEnabled = enabledProviders().isNotEmpty()
    )
```

**Mount points (required):**

| Site | Call |
|---|---|
| startCollection before startForeground | `liveUpdatePresenter.reduce(snapshotEvent())` then startForeground(id, notification()) — **do not** double-notify if startForeground already posts |
| queueAccepted after fields update | `dispatch(Accepted(...))` instead of updateNotification |
| recordDropped | `dispatch(Dropped(...))` |
| policy change after reduce | `dispatch(PolicyChanged(...))` or Snapshot |
| apiState changes | `dispatch(ApiChanged(...))` |
| onProviderDisabled | `dispatch(ProviderDisabled(provider))` |
| pause path | `clearSuccessHoldTick(); ...; liveUpdatePresenter.reduce(Paused); stopCollection(); nm.notify(... notification())` |
| stop / onDestroy | `destroyed=true` (destroy); `clearSuccessHoldTick()`; cancel as today |
| updateNotification() | either delete and use dispatch only, or `dispatch` with last event type carefully — prefer explicit events |

**startForeground path:** reduce Snapshot first, then `startForeground(NOTIFICATION_ID, notification())` without extra notify.

**Sync path:** keep existing ongoing-flag detection for restorePausedNotification; Presenter Paused must keep `isOngoing=false` so FLAG_ONGOING_EVENT stays off. Prefer single notify after ApiChanged.

- [ ] **Step 3: Compile**

```bat
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

- [ ] **Step 4: Commit**

```bash
git commit -am "feat: wire live update presenter into ForegroundLocationService"
```

---

### Task 7: Service regression tests migration

**Files:**
- Modify: `ForegroundLocationServiceTest.kt`
- Check: `AndroidV2CollectionControlContractTest.kt` (string `startForeground(LocationNotificationRenderer.NOTIFICATION_ID, notification())` still valid)

- [ ] **Step 1: Replace `pausedState()` / `lateState` builders**

```kotlin
private fun pausedUiModel() = LocationNotificationUiModel(
    phase = LocationLiveUpdatePhase.Paused,
    mode = LocationPolicyMode.Off,
    isOngoing = false,
    requestLiveUpdate = false,
    title = "PIM 定位",
    collapsedText = "定位已暂停",
    expandedText = "状态：已暂停\n策略：已暂停",
    shortStatus = "已暂停",
    progressPercent = null,
    contentAction = collectionControlAction(LocationPolicyMode.Off)
)
```

Use `LocationNotificationRenderer.build(context, pausedUiModel())` where tests pre-seed notifications.

- [ ] **Step 2: Keep all existing lifecycle assertions** (pause keeps resume action, stop cancels, sync paths, permission intent, etc.). Update EXTRA_TEXT expectations if collapsed copy changed (`已暂停` still required; power-saving must not flash on sync-from-paused).

- [ ] **Step 3: Run**

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.service.ForegroundLocationServiceTest" --tests "com.pim.app.notifications.*" --no-daemon
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git commit -am "test: migrate location service notification helpers to UiModel"
```

---

### Task 8: compileSdk 36 + real API 36 promote + CI platform

**Files:**
- `app/build.gradle.kts`, `core/build.gradle.kts`, `features/calendar/build.gradle.kts`: `compileSdk = 36`, **keep** `targetSdk = 34` on app
- `LiveUpdateNotificationCompat.kt`: real API calls
- `.github/workflows/build-android.yml`: ensure `platforms;android-36` (and build-tools if required)

- [ ] **Step 1: Bump compileSdk in three modules**

```kotlin
compileSdk = 36
// app defaultConfig.targetSdk stays 34
```

- [ ] **Step 2: Implement API 36 body** using platform/Compat symbols now on classpath; keep try/catch.

```kotlin
// Example shape after symbols resolve — adjust to actual SDK:
builder.setRequestPromotedOngoing(true) // if on NotificationCompat.Builder
// or extras putBoolean("android.requestPromotedOngoing", true)
// ProgressStyle single segment length 100, setProgress(progressPercent ?: 0)
// setShortCriticalText(model.shortStatus.take(7))
```

- [ ] **Step 3: CI minimal change** (unavoidable for green CI):

In `build-android.yml` where android-34 is installed, also install:

```bash
sdkmanager "platforms;android-36"
```

Explain in PR: required for compileSdk 36.

- [ ] **Step 4: Verify**

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.*" --tests "com.pim.app.location.service.ForegroundLocationServiceTest" --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

- [ ] **Step 5: Policy engine untouched**

```bash
git diff origin/master -- src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt
```

Expected: empty.

- [ ] **Step 6: Commit**

```bash
git commit -am "chore: compileSdk 36 and wire Live Update platform APIs"
```

---

### Task 9: Full unit verification + PR

**Files:** verify only

- [ ] **Step 1: Full app unit tests**

```bat
cd src\client-android
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

- [ ] **Step 2: Policy tests still green**

```bat
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.policy.*" --no-daemon
```

- [ ] **Step 3: Grep guards**

```bash
rg "LocationNotificationState" src/client-android/app/src/main
rg "LocationPolicyEngine" -n # only confirm no accidental edits via git diff
```

Expected: no `LocationNotificationState` in main.

- [ ] **Step 4: Push + PR**

```bash
git status --short --branch
git push -u origin codex/android-location-live-updates
gh pr create --title "feat: add Android location Live Updates" --body "$(cat <<'EOF'
## Summary
- Add LocationLiveUpdatePresenter for location notification phases and 30s success hold copy
- Render single FGS notification 7101 from UiModel; API 36 Live Update promote with safe fallback
- No LocationPolicyEngine / sampling changes

## Verification
- `src/client-android/gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`
- Policy engine: `git diff origin/master -- .../LocationPolicyEngine.kt` empty
- Manual Android 16 Live Update visibility still recommended on device

## Notes
- compileSdk 36 / targetSdk 34 initially
- CI installs platforms;android-36 for compile
- Skipped `dotnet test Pim.sln` (Android-only)
EOF
)"
```

- [ ] **Step 5: Wait for GitHub Actions `build-android`** (triggered by `src/client-android/**`). If only docs changed previously jobs skipped; after code, unit+assembleRelease must pass.

---

### Task 10: Manual acceptance checklist (human / device)

Not automated. Record results in PR comment.

- [ ] Android 16 device/emulator: collecting shows promoted/ongoing Live Update when system allows
- [ ] Accept a fix → collapsed shows `已定位 · 精度 …` ~30s → back to `定位中 · …`
- [ ] Pause / resume / sync buttons work; pause not ongoing
- [ ] Stop removes notification
- [ ] Android 14/15: new copy ongoing notification, no crash, no promote required
- [ ] Confirm sampling intervals unchanged in settings behavior

---

## Spec coverage self-check

| Spec requirement | Task |
|---|---|
| Presenter + UiModel + Events | 1–3 |
| Copy / 30s hold / priority | 2–3 |
| Renderer build(UiModel) | 4 |
| LiveUpdate compat + SDK gate | 5, 8 |
| Service dispatch + tick | 6 |
| Service regression | 7 |
| compileSdk 36 | 8 |
| PR + CI | 9 |
| Manual | 10 |
| No PolicyEngine change | 6, 8, 9 |
| Single notification 7101 | 4, 6 |
| Display failure ≠ stop GPS | 5, 6 |

## Placeholder / consistency scan

- No TBD steps; Shared Types locked across tasks.
- `contentAction` is `CollectionControlAction` everywhere.
- `progressPercent` overdue = 100 (not null).
- Soft drop does not break SuccessHold.

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-16-android-location-live-updates.md`.

**Two execution options:**

1. **Subagent-Driven (required by user: ≥10 subagents)** — fresh subagent per task + review subagents; total subagent count must be ≥10 across implementation.
2. **Inline Execution** — not preferred here because user mandated multi-subagent execution.

**Which approach?** Reply `1` to start subagent-driven implementation (will also commit this plan doc first).
