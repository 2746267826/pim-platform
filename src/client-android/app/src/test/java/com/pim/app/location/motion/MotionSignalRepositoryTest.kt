package com.pim.app.location.motion

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.policy.MotionSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class MotionSignalRepositoryTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `permission denied surfaces an issue and register does not crash`() {
        shadowOf(context).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val repository = MotionSignalRepository(context)

        repository.register()

        val status = repository.status.value
        assertEquals(MotionSignal.Unknown, status.signal)
        assertEquals("activity-recognition-missing", status.issueCode)
        assertNotNull(status.message)
        repository.unregister()
    }

    @Test
    fun `permission granted registers without issue`() {
        // 显式授权（共享 Application 的权限状态可能被前序用例 deny 污染）
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val repository = MotionSignalRepository(context)

        repository.register()

        assertNull(repository.status.value.issueCode)
        repository.unregister()
    }

    @Test
    fun `register is idempotent and does not flip the status back to Unknown`() {
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val repository = MotionSignalRepository(context)

        repository.register()
        repository.register()
        repository.register()

        assertEquals(
            "repeated register must keep the initial Unknown status without issue churn",
            MotionSignal.Unknown,
            repository.status.value.signal
        )
        repository.unregister()
    }

    @Test
    fun `permission restore clears the issue immediately on the next register`() {
        shadowOf(context).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val repository = MotionSignalRepository(context)
        repository.register()
        assertEquals("activity-recognition-missing", repository.status.value.issueCode)

        // 权限恢复：下一轮 register 立即清除残留 issue，不依赖信号变化
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        repository.register()

        assertNull(repository.status.value.issueCode)
        repository.unregister()
    }

    @Test
    fun `permission denied writes the issue immediately regardless of the current signal`() {
        shadowOf(context).grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        val repository = MotionSignalRepository(context)
        repository.register()
        assertNull(repository.status.value.issueCode)

        // 信号稳定（如长期静止 Still）时权限被拒：issue 必须立即写入，
        // 否则 UI 看不到"健身运动权限未开启"提示
        shadowOf(context).denyPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
        repository.register()

        assertEquals("activity-recognition-missing", repository.status.value.issueCode)
        repository.unregister()
    }
}
