package com.pim.app.ui.status

import com.pim.app.status.StatusCenterState
import com.pim.app.status.PolicyTransitionSnapshot
import com.pim.app.status.StatusDisplayText
import com.pim.app.status.SyncPhase
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusPresentationTest {
    @Test
    fun epochMillisAreFormattedAsAnAbsoluteTime() {
        val timestamp = Instant.parse("2026-07-14T10:10:00Z").toEpochMilli()

        assertEquals("07-14 10:10", formatEpochMillis(timestamp, ZoneId.of("UTC")))
    }

    @Test
    fun syncButtonEnablementCoversEveryPhaseAndLoading() {
        val enabled = setOf(
            SyncPhase.Idle,
            SyncPhase.Waiting,
            SyncPhase.Completed,
            SyncPhase.Failed,
            SyncPhase.Cancelled
        )

        SyncPhase.entries.forEach { phase ->
            val state = StatusCenterState.empty().copy(isLoading = false, syncPhase = phase)
            assertEquals("phase=$phase enabled=${phase in enabled}", phase in enabled, syncButtonEnabled(state))
        }

        assertFalse(syncButtonEnabled(StatusCenterState.empty().copy(isLoading = true, syncPhase = SyncPhase.Idle)))
    }

    @Test
    fun phaseAndButtonLabelsExplainQueuedWork() {
        assertEquals("当前空闲", syncPhaseLabel(SyncPhase.Idle))
        assertEquals("等待网络或系统调度", syncPhaseLabel(SyncPhase.Waiting))
        assertEquals("同步条件未满足", syncPhaseLabel(SyncPhase.Blocked))
        assertEquals("请求已接受", syncButtonLabel(SyncPhase.Accepted))
        assertEquals("暂不可同步", syncButtonLabel(SyncPhase.Blocked))
        assertEquals("再次同步", syncButtonLabel(SyncPhase.Completed))
        assertTrue(syncButtonLabel(SyncPhase.Failed).contains("重新"))
        assertFalse(syncButtonEnabled(StatusCenterState.empty().copy(isLoading = false, syncPhase = SyncPhase.Blocked)))
    }

    @Test
    fun `scheduleFreshnessLabelMatchesDisplayText`() {
        assertEquals("新鲜", com.pim.app.status.StatusDisplayText.scheduleFreshness(com.pim.app.schedule.ScheduleCacheFreshness.Fresh))
        assertEquals("可能过期", com.pim.app.status.StatusDisplayText.scheduleFreshness(com.pim.app.schedule.ScheduleCacheFreshness.Stale))
        assertEquals("暂无", com.pim.app.status.StatusDisplayText.scheduleFreshness(com.pim.app.schedule.ScheduleCacheFreshness.Missing))
    }

    @Test
    fun `policyReasonLabelReturnsSafeText`() {
        assertEquals("当前日程时段，降低定位频率", StatusDisplayText.scheduleReason("当前日程时段，降低定位频率"))
        assertEquals("暂无", StatusDisplayText.scheduleReason(null))
        assertEquals("暂无", StatusDisplayText.scheduleReason(""))
        assertEquals("策略已更新", StatusDisplayText.scheduleReason("internal_code=123"))
        assertEquals("检测到运动状态：步行", StatusDisplayText.scheduleReason("检测到运动状态：步行"))
        assertEquals("策略已更新", StatusDisplayText.scheduleReason("检测到运动状态：flying"))
        assertEquals(
            "日程期间位置变化超过 100 米",
            StatusDisplayText.scheduleReason("日程期间位置变化超过 100 米")
        )
    }

    @Test
    fun `policyTransitionSummaryIncludesTimeModesAndSafeReason`() {
        val transition = PolicyTransitionSnapshot(
            fromMode = "PowerSavingNormal",
            toMode = "ScheduleLowFrequency",
            reason = "当前日程时段，降低定位频率",
            occurredAtMillis = Instant.parse("2026-07-14T10:10:00Z").toEpochMilli()
        )

        assertEquals(
            "07-14 10:10 · 常规省电 → 日程低频 · 当前日程时段，降低定位频率",
            formatPolicyTransition(transition, ZoneId.of("UTC"))
        )
    }

    @Test
    fun `policyIntervalUsesReadableMinutesAndSeconds`() {
        assertEquals("5 分钟", formatPolicyInterval(300_000L))
        assertEquals("1分30秒", formatPolicyInterval(90_000L))
        assertEquals("30 秒", formatPolicyInterval(30_000L))
        assertEquals("未安排", formatPolicyInterval(0L))
        assertEquals("未安排", formatPolicyInterval(-1L))
    }

    @Test
    fun waitingSyncCanRequestOneTimeNetworkOverride() {
        val state = StatusCenterState.empty().copy(
            isLoading = false,
            syncPhase = SyncPhase.Waiting
        )

        assertTrue(syncButtonEnabled(state))
        assertEquals("立即同步", syncButtonLabel(SyncPhase.Waiting))
    }
}
