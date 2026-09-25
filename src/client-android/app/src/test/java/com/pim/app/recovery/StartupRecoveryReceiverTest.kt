package com.pim.app.recovery

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartupRecoveryReceiverTest {

    @Test
    fun `dispatchStartupRecovery calls recover once for boot completed`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_BOOT_COMPLETED) {
            callCount++
        }
        assertTrue(result)
        assertEquals(1, callCount)
    }

    @Test
    fun `dispatchStartupRecovery calls recover once for package replaced`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_MY_PACKAGE_REPLACED) {
            callCount++
        }
        assertTrue(result)
        assertEquals(1, callCount)
    }

    @Test
    fun `dispatchStartupRecovery does not call recover for unknown action`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            callCount++
        }
        assertFalse(result)
        assertEquals(0, callCount)
    }

    @Test
    fun `dispatchStartupRecovery does not call recover for null`() = runTest {
        var callCount = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(null) {
            callCount++
        }
        assertFalse(result)
        assertEquals(0, callCount)
    }

    @Test
    fun `manifest declares both boot completed and package replaced for receiver`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pm = context.packageManager
        val bootReceivers = pm.queryBroadcastReceivers(
            Intent(Intent.ACTION_BOOT_COMPLETED),
            PackageManager.GET_INTENT_FILTERS
        )
        val replacedReceivers = pm.queryBroadcastReceivers(
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED),
            PackageManager.GET_INTENT_FILTERS
        )
        val targetName = StartupRecoveryReceiver::class.java.name
        assertTrue("BOOT_COMPLETED must resolve to StartupRecoveryReceiver",
            bootReceivers.any { it.activityInfo.name == targetName })
        assertTrue("MY_PACKAGE_REPLACED must resolve to StartupRecoveryReceiver",
            replacedReceivers.any { it.activityInfo.name == targetName })
    }

    /** AC-15.4：设备重启后必须重建保活闹钟（否则重启即永久失效）。 */
    @Test
    fun `AC-15_4 开机后重建保活闹钟`() = runTest {
        var recovered = 0
        var alarmRebuilt = 0
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(
            action = Intent.ACTION_BOOT_COMPLETED,
            recover = { recovered++ },
            rebuildKeepAliveAlarm = { alarmRebuilt++ },
            onKeepAliveFailure = { }
        )

        assertTrue(result)
        assertEquals(1, recovered)
        assertEquals("开机后必须重建闹钟", 1, alarmRebuilt)
    }

    /** AC-15.4：应用更新后被系统清空闹钟，同样要重建。 */
    @Test
    fun `AC-15_4 应用更新后重建保活闹钟`() = runTest {
        var alarmRebuilt = 0
        StartupRecoveryReceiver.dispatchStartupRecovery(
            action = Intent.ACTION_MY_PACKAGE_REPLACED,
            recover = { },
            rebuildKeepAliveAlarm = { alarmRebuilt++ },
            onKeepAliveFailure = { }
        )

        assertEquals(1, alarmRebuilt)
    }

    /** 闹钟重建失败不得影响采集恢复（两者必须互相独立）。 */
    @Test
    fun `闹钟重建失败不影响采集恢复且失败可见`() = runTest {
        var recovered = 0
        var reported: Exception? = null
        val result = StartupRecoveryReceiver.dispatchStartupRecovery(
            action = Intent.ACTION_BOOT_COMPLETED,
            recover = { recovered++ },
            rebuildKeepAliveAlarm = { throw IllegalStateException("boom") },
            onKeepAliveFailure = { reported = it }
        )

        assertTrue(result)
        assertEquals("采集恢复必须照常完成", 1, recovered)
        assertEquals("失败必须有可见出口（REQ-28）", "boom", reported?.message)
    }

    /**
     * 反之：采集恢复抛错时也必须尝试重建闹钟（否则一次采集恢复失败会让保活永久失效），
     * 且采集恢复的异常仍按既有语义向上传播。
     */
    @Test
    fun `采集恢复失败时仍尝试重建闹钟且异常继续传播`() = runTest {
        var alarmRebuilt = 0
        var thrown: Exception? = null
        try {
            StartupRecoveryReceiver.dispatchStartupRecovery(
                action = Intent.ACTION_BOOT_COMPLETED,
                recover = { throw IllegalStateException("recover-boom") },
                rebuildKeepAliveAlarm = { alarmRebuilt++ },
                onKeepAliveFailure = { }
            )
        } catch (ex: IllegalStateException) {
            thrown = ex
        }

        assertEquals("采集恢复失败不应阻止闹钟重建", 1, alarmRebuilt)
        assertEquals("采集恢复的异常应按既有语义向上传播", "recover-boom", thrown?.message)
    }
}
