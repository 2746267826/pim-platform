package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                // Always record soft-drop; SuccessHold primary wins via resolvePhase priority.
                degradedKind = LocationDegradedKind.Drop
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

    private fun buildCollapsedText(phase: LocationLiveUpdatePhase): String {
        return when (phase) {
            // Keep pause primary, but surface sync/API feedback in collapsed (EXTRA_TEXT).
            LocationLiveUpdatePhase.Paused -> pausedCollapsedText()
            LocationLiveUpdatePhase.SuccessHold -> "已定位 · 精度 $lastAccuracyText"
            LocationLiveUpdatePhase.Degraded -> when (degradedKind) {
                LocationDegradedKind.Permission -> "无法定位 · 权限不足"
                LocationDegradedKind.Provider -> "定位中断 · GPS/网络已关"
                LocationDegradedKind.Drop -> "定位异常 · ${lastDroppedReason.orEmpty()}"
                null -> "定位异常 · ${lastDroppedReason.orEmpty()}"
            }
            LocationLiveUpdatePhase.Collecting -> {
                if (lastAcceptedAtMillis == null) {
                    "定位中 · 等待首次定位"
                } else {
                    "定位中 · ${formatRelativeTime(clock(), lastAcceptedAtMillis, "等待首次定位")}"
                }
            }
        }
    }

    private fun pausedCollapsedText(): String {
        val trimmed = apiState.trim()
        if (trimmed.isEmpty() || trimmed == "正常") return "定位已暂停"
        return "定位已暂停 · $trimmed"
    }

    private fun buildExpandedText(phase: LocationLiveUpdatePhase): String {
        return buildList {
            add("状态：${phaseStatusLabel(phase)}")
            add("策略：${modeFullLabel(mode)}")
            add("最近更新：${recentUpdateLine()}")
            add("精度：$lastAccuracyText")
            add("下次定位：$nextExpectedLocationText")
            add("最近位置：$lastAcceptedLocationText")
            add("待上传 $pendingUploadCount，${apiStateLabel(apiState)}")
            lastDroppedReason?.let { add("最近丢弃：$it") }
        }.joinToString("\n")
    }

    private fun phaseStatusLabel(phase: LocationLiveUpdatePhase): String {
        return when (phase) {
            LocationLiveUpdatePhase.Collecting -> "定位中"
            LocationLiveUpdatePhase.SuccessHold -> "已定位"
            LocationLiveUpdatePhase.Paused -> "已暂停"
            LocationLiveUpdatePhase.Degraded -> when (degradedKind) {
                LocationDegradedKind.Permission -> "无法定位"
                LocationDegradedKind.Provider -> "定位中断"
                LocationDegradedKind.Drop -> "定位异常"
                null -> "定位异常"
            }
        }
    }

    private fun recentUpdateLine(): String {
        val last = lastAcceptedAtMillis
        if (last == null) return "无"
        val relative = formatRelativeTime(clock(), last, "无")
        val clockText = absoluteClockText(last)
        return "$relative（$clockText）"
    }

    private fun progressPercent(phase: LocationLiveUpdatePhase): Int? {
        if (phase == LocationLiveUpdatePhase.Paused || mode == LocationPolicyMode.Off) return null
        val next = nextExpectedAtMillis ?: return null
        val start = lastAcceptedAtMillis
            ?: requestIntervalMillis?.let { next - it }
            ?: return null
        val span = (next - start).coerceAtLeast(1L)
        val elapsed = clock() - start
        return ((elapsed * 100L) / span).toInt().coerceIn(0, 100)
    }

    private fun modeShortLabel(mode: LocationPolicyMode): String = when (mode) {
        LocationPolicyMode.Off -> "已暂停"
        LocationPolicyMode.PowerSavingNormal -> "省电"
        LocationPolicyMode.ScheduleLowFrequency -> "日程低频"
        LocationPolicyMode.MotionObservation -> "运动"
        LocationPolicyMode.MovementRecovery -> "移动恢复"
        LocationPolicyMode.SyncFallback -> "同步兜底"
    }

    private fun modeFullLabel(mode: LocationPolicyMode): String = when (mode) {
        LocationPolicyMode.Off -> "已暂停"
        LocationPolicyMode.PowerSavingNormal -> "省电档"
        LocationPolicyMode.ScheduleLowFrequency -> "日程低频"
        LocationPolicyMode.MotionObservation -> "运动观察"
        LocationPolicyMode.MovementRecovery -> "移动恢复"
        LocationPolicyMode.SyncFallback -> "同步兜底"
    }

    private fun apiStateLabel(apiState: String): String {
        return "API ${apiState.removePrefix("API ").trim()}"
    }

    companion object {
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        internal fun formatRelativeTime(
            nowMillis: Long,
            lastUpdateMillis: Long?,
            neverText: String
        ): String {
            if (lastUpdateMillis == null) return neverText
            val delta = (nowMillis - lastUpdateMillis).coerceAtLeast(0L)
            return when {
                delta < 10_000L -> "刚刚"
                delta < 60_000L -> "${delta / 1_000L}秒前"
                delta < 3_600_000L -> "${delta / 60_000L}分钟前"
                else -> absoluteClockText(lastUpdateMillis)
            }
        }

        private fun absoluteClockText(millis: Long): String {
            return timeFormatter.format(
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            )
        }
    }
}
