package com.pim.app.status

import com.pim.app.location.service.ForegroundLocationRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class StatusIssueTest {
    @Test
    fun requiredIssuesHaveReadableActionLabels() {
        val issues = StatusIssue.requiredIssueCodes()

        assertTrue(issues.contains("api-address-missing"))
        assertTrue(issues.contains("background-location-missing"))
        assertTrue(issues.contains("foreground-service-not-running"))
        assertTrue(issues.contains("location-accuracy-rejected"))
        assertTrue(issues.contains("altitude-missing-timeout"))
        assertTrue(issues.contains("upload-queue-backlog"))

        assertEquals("去设置", StatusIssue.apiAddressMissing().actionLabel)
        assertEquals("去设置", StatusIssue.backgroundLocationMissing().actionLabel)
        assertEquals("去设置", StatusIssue.foregroundServiceNotRunning().actionLabel)
        assertEquals("去设置", StatusIssue.locationAccuracyRejected().actionLabel)
        assertEquals("去设置", StatusIssue.altitudeMissingTimeout().actionLabel)
        assertEquals("去设置", StatusIssue.uploadQueueBacklog(18).actionLabel)
    }

    @Test
    fun snapshotPlannerAddsActionableBlockingIssues() {
        val snapshot = StatusCenterSnapshot(
            permissions = PermissionStatusSnapshot(
                notificationGranted = false,
                preciseLocationGranted = true,
                backgroundLocationGranted = false,
                usageAccessGranted = true,
                activityRecognitionGranted = false
            ),
            api = ApiConnectionSnapshot(
                address = "",
                isValid = false,
                reasonCode = "missing",
                warnings = emptySet()
            ),
            auth = AuthStatusSnapshot(hasAccessToken = false, isExpired = false),
            service = ForegroundServiceSnapshot(
                continuousCollectionEnabled = true,
                serviceRunning = false
            ),
            tracking = TrackingPolicySnapshot(
                profile = "power-saving",
                currentPolicyMode = "PowerSavingNormal",
                nextExpectedLocationAtMillis = null
            ),
            queues = QueueStatusSnapshot(
                pendingLocationPoints = 12,
                pendingUsageEvents = 0,
                pendingUsageSummaries = 0,
                pendingAppMetadata = 0,
                pendingLogs = 0,
                pendingDeviceProfile = 0
            ),
            diagnostics = DiagnosticSnapshot(
                lastDroppedReason = "horizontal-accuracy-too-low",
                lastDroppedAtMillis = 1_000L,
                lastLogMessage = "timeout",
                lastHeartbeatStatus = "failed",
                recentLogMessages = listOf("timeout", "retry scheduled")
            )
        )

        val issues = StatusIssuePlanner.plan(snapshot).associateBy { it.code }

        assertEquals("配置 API 地址", issues.getValue("api-address-missing").title)
        assertEquals("后台定位未授权", issues.getValue("background-location-missing").title)
        assertEquals("前台定位服务未运行", issues.getValue("foreground-service-not-running").title)
        assertEquals("定位精度不达标", issues.getValue("location-accuracy-rejected").title)
        assertEquals("上传队列积压", issues.getValue("upload-queue-backlog").title)
        assertEquals("最近错误", issues.getValue("recent-error").title)
        assertEquals("当前策略", issues.getValue("current-policy-state").title)
        assertEquals("最近丢弃定位", issues.getValue("location-dropped-recent").title)
    }

    @Test
    fun trackingSnapshotUsesForegroundServiceRuntimeState() {
        val runtime = ForegroundLocationRuntimeState(
            isRunning = true,
            currentPolicyMode = "ScheduleLowFrequency",
            nextExpectedLocationAtMillis = 2_000L
        )

        val tracking = StatusTrackingMapper.fromRuntime("power-saving", runtime)

        assertEquals("power-saving", tracking.profile)
        assertEquals("ScheduleLowFrequency", tracking.currentPolicyMode)
        assertEquals(2_000L, tracking.nextExpectedLocationAtMillis)
    }

    @Test
    fun diagnosticSnapshotKeepsRecentLogs() {
        val diagnostics = DiagnosticSnapshot(
            lastDroppedReason = null,
            lastDroppedAtMillis = null,
            lastLogMessage = "first",
            lastHeartbeatStatus = null,
            recentLogMessages = listOf("first", "second")
        )

        assertEquals(listOf("first", "second"), diagnostics.recentLogMessages)
    }

    @Test
    fun actionRouterMapsTargetsToVisibleActions() {
        assertEquals(StatusActionRoute.OpenSettings, StatusActionRouter.route(StatusActionTarget.Settings))
        assertEquals(StatusActionRoute.OpenSettings, StatusActionRouter.route(StatusActionTarget.Login))
        assertEquals(StatusActionRoute.OpenSettings, StatusActionRouter.route(StatusActionTarget.Permissions))
        assertEquals(StatusActionRoute.TriggerSync, StatusActionRouter.route(StatusActionTarget.Sync))
        assertEquals(StatusActionRoute.StayOnStatus, StatusActionRouter.route(StatusActionTarget.Queue))
        assertEquals(StatusActionRoute.StayOnStatus, StatusActionRouter.route(StatusActionTarget.Status))
    }

    @Test
    fun syncActionRunnerRunsMobileSyncAndRefreshesStatus() = runBlocking {
        var synced = false
        var refreshed = false
        val runner = StatusSyncActionRunner(
            syncNow = { synced = true },
            refresh = { refreshed = true }
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertTrue(synced)
        assertTrue(refreshed)
    }

    @Test
    fun syncActionRunnerIgnoresNonSyncRoutes() = runBlocking {
        var synced = false
        val runner = StatusSyncActionRunner(
            syncNow = { synced = true },
            refresh = {}
        )

        runner.run(StatusActionRoute.OpenSettings)

        assertFalse(synced)
    }

    @Test
    fun refreshSignalStartsAtZeroAndAdvances() {
        val signal = StatusRefreshSignal()

        assertEquals(0L, signal.version.value)
        signal.requestRefresh()

        assertEquals(1L, signal.version.value)
    }
}
