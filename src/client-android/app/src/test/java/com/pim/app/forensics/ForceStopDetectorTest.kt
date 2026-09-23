package com.pim.app.forensics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REQ-2 强停检测判定（AC-2.1 / AC-2.2 / AC-2.3）。
 *
 * 判定是纯函数，因此三种结论都能用注入数据在 JVM 单元测试里逐条复现；
 * 真机结论另见 PR 中的模拟器取证记录与"需要需求方在真机上验证"清单。
 */
class ForceStopDetectorTest {

    private val minute = 60_000L

    private fun state(
        armed: Boolean = true,
        armedAt: Long? = 1_000_000L,
        lastAliveBoot: Long? = 500_000L,
        lastAliveAt: Long? = 1_100_000L
    ) = SentinelState(
        armed = armed,
        armedAtUtcMillis = armedAt,
        lastAliveBootElapsedMillis = lastAliveBoot,
        lastAliveAtUtcMillis = lastAliveAt
    )

    private fun input(
        nowBootElapsed: Long = 900_000L,
        sentinelPresent: Boolean = true,
        exitRecordAfterLastAlive: Boolean = false,
        exitRecordSaysUserRequested: Boolean = false,
        permissionChangeAfterArmed: Boolean = false
    ) = ForceStopDetectionInput(
        nowUtcMillis = 1_200_000L,
        nowBootElapsedMillis = nowBootElapsed,
        sentinelPresent = sentinelPresent,
        exitRecordAfterLastAlive = exitRecordAfterLastAlive,
        exitRecordSaysUserRequested = exitRecordSaysUserRequested,
        permissionChangeAfterArmed = permissionChangeAfterArmed
    )

    @Test
    fun `AC-2_1 sentinel missing without reboot and without exit record is a suspected force stop`() {
        val verdict = ForceStopDetector.detect(state(), input(sentinelPresent = false))

        assertEquals(ForceStopVerdict.ForceStop, verdict)
        assertEquals(ForceStopKinds.FORCE_STOP, ForceStopDetector.kindOf(verdict))
        assertEquals("疑似强停", ForceStopDetector.labelOf(ForceStopKinds.FORCE_STOP))
    }

    @Test
    fun `AC-2_2 boot elapsed going backwards is a reboot and never a force stop`() {
        val verdict = ForceStopDetector.detect(
            state(),
            input(nowBootElapsed = 100_000L, sentinelPresent = false)
        )

        assertEquals(ForceStopVerdict.Reboot, verdict)
        assertEquals(ForceStopKinds.REBOOT, ForceStopDetector.kindOf(verdict))
        // AC-2.2 反面：重启不得被计为强停。
        assertFalse(ForceStopDetector.kindOf(verdict) == ForceStopKinds.FORCE_STOP)
    }

    @Test
    fun `AC-2_3 permission change clears the sentinel and is not counted as a force stop`() {
        val verdict = ForceStopDetector.detect(
            state(),
            input(sentinelPresent = false, permissionChangeAfterArmed = true)
        )

        assertEquals(ForceStopVerdict.SentinelClearedByPermissionChange, verdict)
        assertEquals(
            ForceStopKinds.SENTINEL_CLEARED_PERMISSION,
            ForceStopDetector.kindOf(verdict)
        )
        assertEquals(
            "哨兵被清空（权限变更）",
            ForceStopDetector.labelOf(ForceStopKinds.SENTINEL_CLEARED_PERMISSION)
        )
        // AC-2.3：不得计为强停。
        assertFalse(ForceStopDetector.kindOf(verdict) == ForceStopKinds.FORCE_STOP)
    }

    @Test
    fun `an exit record after the last alive marker explains the death and is not a force stop`() {
        // 系统如实记录了死因（例如低内存回收）：这既不是强停，也不该产生强停事件。
        val verdict = ForceStopDetector.detect(
            state(),
            input(sentinelPresent = false, exitRecordAfterLastAlive = true)
        )

        assertEquals(ForceStopVerdict.None, verdict)
        assertEquals(null, ForceStopDetector.kindOf(verdict))
    }

    @Test
    fun `REASON_USER_REQUESTED is itself force stop evidence even though the system recorded an exit`() {
        // 实测（API 36 模拟器，2026-09-23）：`adb shell am force-stop com.pim.app`
        // 之后重新打开应用，系统留下一条 REASON_USER_REQUESTED 退出记录。
        // 若把它当成"系统已解释死因"就会漏报强停，因此这条记录必须直接判为强停。
        val verdict = ForceStopDetector.detect(
            state(),
            input(
                sentinelPresent = false,
                exitRecordAfterLastAlive = true,
                exitRecordSaysUserRequested = true
            )
        )

        assertEquals(ForceStopVerdict.ForceStop, verdict)
        assertEquals(ForceStopKinds.FORCE_STOP, ForceStopDetector.kindOf(verdict))
    }

    @Test
    fun `permission change wins over a user requested looking record`() {
        // AC-2.3：权限变更会同时让哨兵消失并留下退出记录，必须记成"哨兵被清空（权限变更）"。
        val verdict = ForceStopDetector.detect(
            state(),
            input(
                sentinelPresent = false,
                exitRecordAfterLastAlive = true,
                exitRecordSaysUserRequested = true,
                permissionChangeAfterArmed = true
            )
        )

        assertEquals(ForceStopVerdict.SentinelClearedByPermissionChange, verdict)
    }

    @Test
    fun `a package update is not reported as a force stop`() {
        // 实测：`adb install -r` 之后系统留下 REASON_PACKAGE_UPDATED —— 既不是强停也不是重启。
        val verdict = ForceStopDetector.detect(
            state(),
            input(sentinelPresent = true, exitRecordAfterLastAlive = true)
        )

        assertEquals(ForceStopVerdict.None, verdict)
    }

    @Test
    fun `nothing is reported when the sentinel is still present`() {
        val verdict = ForceStopDetector.detect(state(), input(sentinelPresent = true))
        assertEquals(ForceStopVerdict.None, verdict)
    }

    @Test
    fun `nothing is reported on the very first run without a baseline`() {
        val verdict = ForceStopDetector.detect(
            state(armed = false, armedAt = null, lastAliveBoot = null, lastAliveAt = null),
            input(sentinelPresent = false)
        )

        assertEquals(ForceStopVerdict.None, verdict)
    }

    @Test
    fun `sentinel that was never armed cannot produce a force stop verdict`() {
        val verdict = ForceStopDetector.detect(
            state(armed = false, lastAliveBoot = 500_000L),
            input(sentinelPresent = false)
        )

        assertEquals(ForceStopVerdict.None, verdict)
    }

    @Test
    fun `evidence strings describe the actual observation`() {
        assertEquals(
            "boot-elapsed-decreased",
            ForceStopDetector.evidenceOf(ForceStopVerdict.Reboot)
        )
        assertEquals(
            "sentinel-missing",
            ForceStopDetector.evidenceOf(ForceStopVerdict.ForceStop)
        )
        assertEquals(
            "permission-change",
            ForceStopDetector.evidenceOf(ForceStopVerdict.SentinelClearedByPermissionChange)
        )
    }
}
