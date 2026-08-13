package com.pim.app.notifications

import android.app.Application
import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.service.ForegroundLocationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class LocationNotificationRendererTest {
    @Test
    fun notificationStateUsesPendingUploadTotal() {
        val ns = LocationNotificationState(
            mode = LocationPolicyMode.PowerSavingNormal,
            nextExpectedLocationText = "1 分钟后",
            lastAcceptedLocationText = "12:00",
            lastAccuracyText = "10m",
            pendingUploadTotal = 7,
            apiState = "正常",
            lastDroppedReason = null
        )
        assertEquals(7, ns.pendingUploadTotal)
    }

    @Test
    fun collapsedTextShowsStrategyNextAccuracyQueueAndApi() {
        val text = LocationNotificationRenderer.collapsedText(
            state = state(mode = LocationPolicyMode.ScheduleLowFrequency)
        )

        assertTrue(text.contains("日程低频"))
        assertTrue(text.contains("12 分钟后"))
        assertTrue(text.contains("18m"))
        assertTrue(text.contains("待上传 3"))
        assertTrue(text.contains("正常"))
    }

    @Test
    fun expandedTextShowsDroppedReason() {
        val text = LocationNotificationRenderer.expandedText(
            state = state(
                mode = LocationPolicyMode.MovementRecovery,
                nextExpectedLocationText = "1 分钟后",
                lastAcceptedLocationText = "无",
                lastAccuracyText = "无",
                pendingUploadTotal = 0,
                apiState = "API 无法连接",
                lastDroppedReason = "误差必须小于 50 米"
            )
        )

        assertTrue(text.contains("移动恢复"))
        assertTrue(text.contains("API 无法连接"))
        assertTrue(text.contains("最近丢弃：误差必须小于 50 米"))
    }

    @Test
    fun collectionControlActionShowsPauseWhenActive() {
        val action = collectionControlAction(LocationPolicyMode.PowerSavingNormal)

        assertEquals("暂停", action.label)
        assertEquals(ForegroundLocationController.ACTION_PAUSE_COLLECTION, action.action)
    }

    @Test
    fun collectionControlActionShowsResumeWhenPaused() {
        val action = collectionControlAction(LocationPolicyMode.Off)

        assertEquals("恢复", action.label)
        assertEquals(ForegroundLocationController.ACTION_RESUME_COLLECTION, action.action)
    }

    @Test
    fun ongoingEventFlagWhenActive() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context, state(mode = LocationPolicyMode.PowerSavingNormal)
        )

        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun noOngoingEventWhenPaused() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context, state(mode = LocationPolicyMode.Off)
        )

        assertFalse((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
    }

    @Test
    fun pausedStateShowsResumeAction() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = LocationNotificationRenderer.build(
            context, state(mode = LocationPolicyMode.Off)
        )

        assertEquals("恢复", notification.actions[0].title)
    }

    @Test
    fun expandedTextDoesNotDuplicateApiPrefix() {
        val text = LocationNotificationRenderer.expandedText(
            state = state(
                mode = LocationPolicyMode.ScheduleLowFrequency,
                nextExpectedLocationText = "3 分钟后",
                lastAcceptedLocationText = "12:00",
                lastAccuracyText = "10m",
                pendingUploadTotal = 1,
                apiState = "API 无法连接",
                lastDroppedReason = null
            )
        )
        assertFalse("展开文本不应包含 API API", text.contains("API API"))
        assertTrue(
            "应正确显示 API 状态一次: $text",
            text.contains("待上传 1，API 无法连接")
        )
    }

    @Test
    fun expandedTextAddsApiLabelWhenStateHasNoPrefix() {
        val text = LocationNotificationRenderer.expandedText(
            state = state(
                mode = LocationPolicyMode.PowerSavingNormal,
                pendingUploadTotal = 2,
                apiState = "正常"
            )
        )

        assertTrue(
            "无前缀状态也应显示 API 标签: $text",
            text.contains("待上传 2，API 正常")
        )
    }

    @Test
    fun collapsedTextShowsHighSpeedCopyWhenActive() {
        val text = LocationNotificationRenderer.collapsedText(
            state = state(mode = LocationPolicyMode.HighSpeed)
                .copy(highSpeedActive = true, highSpeedElapsedSeconds = 95)
        )

        assertTrue("collapsed text must show high-speed copy but was: $text", text.contains("高速轨迹记录中"))
        assertTrue("collapsed text must show elapsed but was: $text", text.contains("1 分 35 秒"))
    }

    @Test
    fun collapsedTextRestoresNormalCopyWhenHighSpeedInactive() {
        val text = LocationNotificationRenderer.collapsedText(
            state = state(mode = LocationPolicyMode.HighSpeed)
        )

        assertFalse("inactive high-speed must not show dedicated copy: $text", text.contains("高速轨迹记录中"))
        assertTrue(text.contains("高速轨迹"))
    }

    @Test
    fun expandedTextShowsDenseSamplingAndElapsedWhenActive() {
        val text = LocationNotificationRenderer.expandedText(
            state = state(mode = LocationPolicyMode.HighSpeed)
                .copy(highSpeedActive = true, highSpeedElapsedSeconds = 130)
        )

        assertTrue(text.contains("2.5s 密集采样"))
        assertTrue(text.contains("2 分 10 秒"))
        assertTrue(text.contains("待上传 3"))
    }

    @Test
    fun modeLabelHighSpeedIsChinese() {
        assertEquals("高速轨迹", LocationNotificationRenderer.modeLabel(LocationPolicyMode.HighSpeed))
    }

    @Test
    fun elapsedTextFormatsSecondsAndMinutes() {
        assertEquals("5 秒", LocationNotificationRenderer.elapsedText(5))
        assertEquals("59 秒", LocationNotificationRenderer.elapsedText(59))
        assertEquals("1 分 0 秒", LocationNotificationRenderer.elapsedText(60))
        assertEquals("1 分 35 秒", LocationNotificationRenderer.elapsedText(95))
    }

    private fun state(
        mode: LocationPolicyMode,
        nextExpectedLocationText: String = "12 分钟后",
        lastAcceptedLocationText: String = "21:24",
        lastAccuracyText: String = "18m",
        pendingUploadTotal: Int = 3,
        apiState: String = "正常",
        lastDroppedReason: String? = null
    ): LocationNotificationState {
        return LocationNotificationState(
            mode = mode,
            nextExpectedLocationText = nextExpectedLocationText,
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadTotal = pendingUploadTotal,
            apiState = apiState,
            lastDroppedReason = lastDroppedReason
        )
    }
}
