package com.pim.app.ui.status

import com.pim.app.status.StatusCenterState
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
        val enabled = setOf(SyncPhase.Idle, SyncPhase.Completed, SyncPhase.Failed, SyncPhase.Cancelled)

        SyncPhase.entries.forEach { phase ->
            val state = StatusCenterState.empty().copy(isLoading = false, syncPhase = phase)
            assertEquals(phase in enabled, syncButtonEnabled(state))
        }

        assertFalse(syncButtonEnabled(StatusCenterState.empty().copy(isLoading = true, syncPhase = SyncPhase.Idle)))
    }

    @Test
    fun phaseAndButtonLabelsExplainQueuedWork() {
        assertEquals("当前空闲", syncPhaseLabel(SyncPhase.Idle))
        assertEquals("等待网络或系统调度", syncPhaseLabel(SyncPhase.Waiting))
        assertEquals("请求已接受", syncButtonLabel(SyncPhase.Accepted))
        assertEquals("再次同步", syncButtonLabel(SyncPhase.Completed))
        assertTrue(syncButtonLabel(SyncPhase.Failed).contains("重新"))
    }
}
