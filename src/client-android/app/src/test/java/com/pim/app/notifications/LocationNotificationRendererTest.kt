package com.pim.app.notifications

import com.pim.app.location.policy.LocationPolicyMode
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationNotificationRendererTest {
    @Test
    fun collapsedTextShowsStrategyNextAccuracyQueueAndApi() {
        val text = LocationNotificationRenderer.collapsedText(
            state = LocationNotificationState(
                mode = LocationPolicyMode.ScheduleLowFrequency,
                nextExpectedLocationText = "12 分钟后",
                lastAcceptedLocationText = "21:24",
                lastAccuracyText = "18m",
                pendingUploadCount = 3,
                apiState = "正常",
                lastDroppedReason = null
            )
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
            state = LocationNotificationState(
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
}
