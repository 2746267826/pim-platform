package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LocationLiveUpdatePresenterTest {
    private var now = 1_720_000_000_000L

    private fun presenter() = LocationLiveUpdatePresenter(
        successHoldMillis = 30_000L,
        clock = { now }
    )

    private fun snapshot(
        mode: LocationPolicyMode = LocationPolicyMode.PowerSavingNormal,
        permissionOk: Boolean = true,
        providerEnabled: Boolean = true,
        lastAcceptedAtMillis: Long? = null,
        nextExpectedAtMillis: Long? = now + 180_000L,
        lastDroppedReason: String? = null,
        apiState: String = "正常",
        pendingUploadCount: Int = 0,
        requestIntervalMillis: Long? = 180_000L,
        nextExpectedLocationText: String = "3 分钟后",
        lastAcceptedLocationText: String = lastAcceptedAtMillis?.let { "21:24" } ?: "无",
        lastAccuracyText: String = if (lastAcceptedAtMillis == null) "无" else "18m"
    ) = LocationLiveUpdateEvent.Snapshot(
        mode = mode,
        nextExpectedLocationText = nextExpectedLocationText,
        lastAcceptedLocationText = lastAcceptedLocationText,
        lastAccuracyText = lastAccuracyText,
        pendingUploadCount = pendingUploadCount,
        apiState = apiState,
        lastDroppedReason = lastDroppedReason,
        nextExpectedAtMillis = nextExpectedAtMillis,
        lastAcceptedAtMillis = lastAcceptedAtMillis,
        requestIntervalMillis = requestIntervalMillis,
        permissionOk = permissionOk,
        providerEnabled = providerEnabled
    )

    @Test
    fun case1_snapshotActive_collectingWaitingFirstFix() {
        val p = presenter()
        val ui = p.reduce(snapshot())
        assertEquals(LocationLiveUpdatePhase.Collecting, ui.phase)
        assertEquals("定位中 · 等待首次定位", ui.collapsedText)
        assertEquals("PIM 定位", ui.title)
        assertTrue(ui.isOngoing)
        assertTrue(ui.requestLiveUpdate)
        assertEquals("省电", ui.shortStatus)
        assertEquals("暂停", ui.contentAction.label)
        assertEquals(ForegroundLocationController.ACTION_PAUSE_COLLECTION, ui.contentAction.action)
    }

    @Test
    fun case2_accepted_entersSuccessHold_withDeadline() {
        val p = presenter()
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

    @Test
    fun case3_secondAccepted_resetsDeadline() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        now += 10_000L
        val ui = p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "12m",
                lastAcceptedAtMillis = now
            )
        )
        assertEquals(LocationLiveUpdatePhase.SuccessHold, ui.phase)
        assertEquals("已定位 · 精度 12m", ui.collapsedText)
        assertEquals(now + 30_000L, p.successHoldDeadlineMillis())
    }

    @Test
    fun case4_expiredTick_returnsCollecting() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        now += 30_000L
        val ui = p.reduce(LocationLiveUpdateEvent.Tick)
        assertEquals(LocationLiveUpdatePhase.Collecting, ui.phase)
        assertNull(p.successHoldDeadlineMillis())
        assertEquals("定位中 · 30秒前", ui.collapsedText)
    }

    @Test
    fun case5_earlyTick_keepsSuccessHold() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        now += 15_000L
        val ui = p.reduce(LocationLiveUpdateEvent.Tick)
        assertEquals(LocationLiveUpdatePhase.SuccessHold, ui.phase)
        assertEquals("已定位 · 精度 18m", ui.collapsedText)
        assertNotNull(p.successHoldDeadlineMillis())
    }

    @Test
    fun case6_pausedDuringHold_clearsDeadlineAndProgress() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        val ui = p.reduce(LocationLiveUpdateEvent.Paused)
        assertEquals(LocationLiveUpdatePhase.Paused, ui.phase)
        assertEquals("定位已暂停", ui.collapsedText)
        assertNull(p.successHoldDeadlineMillis())
        assertNull(ui.progressPercent)
        assertFalse(ui.isOngoing)
        assertFalse(ui.requestLiveUpdate)
        assertEquals("已暂停", ui.shortStatus)
        assertEquals("恢复", ui.contentAction.label)
        assertEquals(ForegroundLocationController.ACTION_RESUME_COLLECTION, ui.contentAction.action)
    }

    @Test
    fun case7_droppedDuringSuccessHold_keepsPrimary_showsExpandedDrop() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        val ui = p.reduce(LocationLiveUpdateEvent.Dropped("精度不足"))
        assertEquals(LocationLiveUpdatePhase.SuccessHold, ui.phase)
        assertEquals("已定位 · 精度 18m", ui.collapsedText)
        assertTrue(ui.expandedText.contains("最近丢弃：精度不足"))
    }

    @Test
    fun case7b_softDropAfterHoldExpires_becomesDegradedPrimary() {
        val p = presenter()
        p.reduce(snapshot())
        p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        p.reduce(LocationLiveUpdateEvent.Dropped("精度不足"))
        now += 30_001L
        val ui = p.reduce(LocationLiveUpdateEvent.Tick)
        assertEquals(LocationLiveUpdatePhase.Degraded, ui.phase)
        assertEquals("定位异常 · 精度不足", ui.collapsedText)
        assertTrue(ui.expandedText.contains("最近丢弃：精度不足"))
    }

    @Test
    fun case8_providerDisabled_and_permissionSnapshot_degradedPrimary() {
        val p = presenter()
        p.reduce(snapshot())
        val providerUi = p.reduce(LocationLiveUpdateEvent.ProviderDisabled("gps"))
        assertEquals(LocationLiveUpdatePhase.Degraded, providerUi.phase)
        assertEquals("定位中断 · GPS/网络已关", providerUi.collapsedText)

        val permissionUi = p.reduce(snapshot(permissionOk = false))
        assertEquals(LocationLiveUpdatePhase.Degraded, permissionUi.phase)
        assertEquals("无法定位 · 权限不足", permissionUi.collapsedText)
    }

    @Test
    fun case8b_droppedOutsideHold_becomesDegradedPrimary() {
        val p = presenter()
        p.reduce(snapshot())
        val ui = p.reduce(LocationLiveUpdateEvent.Dropped("速度异常"))
        assertEquals(LocationLiveUpdatePhase.Degraded, ui.phase)
        assertEquals("定位异常 · 速度异常", ui.collapsedText)
    }

    @Test
    fun case9_allModeShortStatusLabels() {
        val p = presenter()
        val expected = mapOf(
            LocationPolicyMode.PowerSavingNormal to "省电",
            LocationPolicyMode.ScheduleLowFrequency to "日程低频",
            LocationPolicyMode.MotionObservation to "运动",
            LocationPolicyMode.MovementRecovery to "移动恢复",
            LocationPolicyMode.SyncFallback to "同步兜底",
            LocationPolicyMode.Off to "已暂停"
        )
        expected.forEach { (mode, label) ->
            val ui = p.reduce(snapshot(mode = mode, nextExpectedAtMillis = if (mode == LocationPolicyMode.Off) null else now + 180_000L))
            assertEquals(mode.name, label, ui.shortStatus)
        }
    }

    @Test
    fun case10_progressPercent_bounds_and_nullWhenPaused() {
        val p = presenter()
        val last = now
        val next = now + 100_000L
        p.reduce(
            snapshot(
                lastAcceptedAtMillis = last,
                nextExpectedAtMillis = next,
                requestIntervalMillis = 100_000L
            )
        )

        assertEquals(0, p.current().progressPercent)

        now = last + 50_000L
        assertEquals(50, p.reduce(LocationLiveUpdateEvent.Tick).progressPercent)

        now = last + 100_000L
        assertEquals(100, p.reduce(LocationLiveUpdateEvent.Tick).progressPercent)

        now = last + 150_000L
        assertEquals(100, p.reduce(LocationLiveUpdateEvent.Tick).progressPercent)

        val paused = p.reduce(LocationLiveUpdateEvent.Paused)
        assertNull(paused.progressPercent)
    }

    @Test
    fun expandedText_fixedOrder_and_optionalDrop() {
        val p = presenter()
        p.reduce(
            snapshot(
                lastAcceptedAtMillis = now - 5_000L,
                lastAccuracyText = "18m",
                lastAcceptedLocationText = "21:24",
                pendingUploadCount = 2,
                apiState = "正常"
            )
        )
        val withDrop = p.reduce(LocationLiveUpdateEvent.Dropped("精度不足"))
        val lines = withDrop.expandedText.lines()
        assertEquals("状态：定位异常", lines[0])
        assertEquals("策略：省电档", lines[1])
        assertTrue(lines[2].startsWith("最近更新："))
        assertEquals("精度：18m", lines[3])
        assertEquals("下次定位：3 分钟后", lines[4])
        assertEquals("最近位置：21:24", lines[5])
        assertEquals("待上传 2，API 正常", lines[6])
        assertEquals("最近丢弃：精度不足", lines[7])
    }

    @Test
    fun apiState_prefixIsDeduped() {
        val p = presenter()
        val ui = p.reduce(snapshot(apiState = "API 无法连接"))
        assertTrue(ui.expandedText.contains("待上传 0，API 无法连接"))
        assertFalse(ui.expandedText.contains("API API"))
    }

    @Test
    fun relativeTime_buckets() {
        assertEquals("刚刚", LocationLiveUpdatePresenter.formatRelativeTime(now, now, "无"))
        assertEquals("刚刚", LocationLiveUpdatePresenter.formatRelativeTime(now, now - 9_999L, "无"))
        assertEquals("10秒前", LocationLiveUpdatePresenter.formatRelativeTime(now, now - 10_000L, "无"))
        assertEquals("59秒前", LocationLiveUpdatePresenter.formatRelativeTime(now, now - 59_000L, "无"))
        assertEquals("1分钟前", LocationLiveUpdatePresenter.formatRelativeTime(now, now - 60_000L, "无"))
        assertEquals("59分钟前", LocationLiveUpdatePresenter.formatRelativeTime(now, now - 59 * 60_000L, "无"))

        val old = now - 3_600_000L
        val expected = DateTimeFormatter.ofPattern("HH:mm")
            .format(Instant.ofEpochMilli(old).atZone(ZoneId.systemDefault()))
        assertEquals(expected, LocationLiveUpdatePresenter.formatRelativeTime(now, old, "无"))
        assertEquals("无", LocationLiveUpdatePresenter.formatRelativeTime(now, null, "无"))
    }

    @Test
    fun collecting_withPriorFix_usesRelativeTime() {
        val p = presenter()
        val ui = p.reduce(snapshot(lastAcceptedAtMillis = now - 25_000L))
        assertEquals(LocationLiveUpdatePhase.Collecting, ui.phase)
        assertEquals("定位中 · 25秒前", ui.collapsedText)
    }

    @Test
    fun current_returnsLastReducedModel() {
        val p = presenter()
        val ui = p.reduce(snapshot())
        assertEquals(ui, p.current())
    }

    @Test
    fun successHold_expandedStatusIsLocated() {
        val p = presenter()
        p.reduce(snapshot())
        val ui = p.reduce(
            LocationLiveUpdateEvent.Accepted(
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                lastAcceptedAtMillis = now
            )
        )
        assertTrue(ui.expandedText.startsWith("状态：已定位\n"))
        assertTrue(ui.expandedText.contains("策略：省电档"))
    }
}
