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
                pendingUploadCount = 0,
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
                pendingUploadCount = 1,
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
                pendingUploadCount = 2,
                apiState = "正常"
            )
        )

        assertTrue(
            "无前缀状态也应显示 API 标签: $text",
            text.contains("待上传 2，API 正常")
        )
    }

    private fun state(
        mode: LocationPolicyMode,
        nextExpectedLocationText: String = "12 分钟后",
        lastAcceptedLocationText: String = "21:24",
        lastAccuracyText: String = "18m",
        pendingUploadCount: Int = 3,
        apiState: String = "正常",
        lastDroppedReason: String? = null
    ): LocationNotificationState {
        return LocationNotificationState(
            mode = mode,
            nextExpectedLocationText = nextExpectedLocationText,
            lastAcceptedLocationText = lastAcceptedLocationText,
            lastAccuracyText = lastAccuracyText,
            pendingUploadCount = pendingUploadCount,
            apiState = apiState,
            lastDroppedReason = lastDroppedReason
        )
    }
}
