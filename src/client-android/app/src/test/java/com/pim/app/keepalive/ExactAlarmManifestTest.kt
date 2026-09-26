package com.pim.app.keepalive

import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-14 的清单契约：**双声明**（决策索引 D2：「权限两个都声明」）。
 *
 * 这条必须对着**打包后的清单**断言，而不是只读源码里的 XML——
 * 清单合并（library manifest merge）可能在构建时丢掉其中一条，
 * 源码里写了不等于装出来的包里有。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class ExactAlarmManifestTest {

    private fun requestedPermissions(): List<String> {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        return (info.requestedPermissions ?: emptyArray()).toList()
    }

    /** AC-14.1 的前提：USE_EXACT_ALARM 必须声明（API 33+ 安装即授予）。 */
    @Test
    fun `REQ-14 清单声明 USE_EXACT_ALARM`() {
        assertTrue(
            "清单必须声明 android.permission.USE_EXACT_ALARM（REQ-14 双声明之一）",
            requestedPermissions().contains("android.permission.USE_EXACT_ALARM")
        )
    }

    /** AC-14.2 的前提：SCHEDULE_EXACT_ALARM 必须声明（供未授权时引导用户打开）。 */
    @Test
    fun `REQ-14 清单声明 SCHEDULE_EXACT_ALARM`() {
        assertTrue(
            "清单必须声明 android.permission.SCHEDULE_EXACT_ALARM（REQ-14 双声明之一）",
            requestedPermissions().contains("android.permission.SCHEDULE_EXACT_ALARM")
        )
    }

    /** AC-15.3 的前提：不得使用需要 setAlarmClock 的清单项（本应用只用 setExactAndAllowWhileIdle）。 */
    @Test
    fun `AC-15_3 不包括会引入状态栏闹钟图标的清单项`() {
        val permissions = requestedPermissions()
        assertTrue(
            "不得声明 SCHEDULE_EXACT_ALARM 之外的闹钟类权限（避免状态栏闹钟图标）",
            permissions.none { it.contains("ALARM", ignoreCase = true) && !it.contains("EXACT_ALARM") }
        )
    }
}
