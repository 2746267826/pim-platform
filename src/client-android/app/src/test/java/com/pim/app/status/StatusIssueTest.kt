package com.pim.app.status

import com.pim.app.location.service.ForegroundLocationRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class StatusIssueTest {
    @Test
    fun actionLabelsAreReadable() {
        assertEquals("去设置", StatusIssue.apiAddressMissing().actionLabel)
        assertEquals("去设置", StatusIssue.backgroundLocationMissing().actionLabel)
        assertEquals("立即同步", StatusIssue.uploadQueueBacklog(18).actionLabel)
        assertEquals("重新检查连接", StatusIssue.probeBlocked().actionLabel)
        assertEquals("重新检查连接", StatusIssue.probePartial().actionLabel)
    }

    @Test
    fun snapshotPlannerAddsActionableBlockingIssues() {
        val snapshot = StatusCenterSnapshot(
            permissions = PermissionStatusSnapshot(
                notificationGranted = false,
                preciseLocationGranted = true,
                backgroundLocationGranted = false,
                usageAccessGranted = true,
                activityRecognitionGranted = false,
                batteryOptimizationGranted = false
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
        assertFalse("current-policy-state must not appear when policy is valid", issues.containsKey("current-policy-state"))
        assertFalse("location-dropped-recent must not appear for known dropped reason", issues.containsKey("location-dropped-recent"))
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
        assertEquals(StatusActionRoute.OpenPermissions, StatusActionRouter.route(StatusActionTarget.Permissions))
        assertEquals(StatusActionRoute.TriggerSync, StatusActionRouter.route(StatusActionTarget.Sync))
        assertEquals(StatusActionRoute.TriggerSync, StatusActionRouter.route(StatusActionTarget.Queue))
        assertEquals(StatusActionRoute.OpenNetworkSettings, StatusActionRouter.route(StatusActionTarget.NetworkSettings))
        assertEquals(StatusActionRoute.ConnectionCheck, StatusActionRouter.route(StatusActionTarget.ConnectionCheck))
        assertEquals(StatusActionRoute.None, StatusActionRouter.route(StatusActionTarget.None))
    }

    @Test
    fun syncActionRunnerRunsMobileSyncAndRefreshesStatus() = runBlocking {
        var synced = false
        var refreshed = false
        val runner = StatusSyncActionRunner(
            syncNow = { _ -> synced = true },
            refresh = { refreshed = true },
            acceptedSignal = StatusAcceptedSignal()
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertTrue(synced)
        assertTrue(refreshed)
    }

    @Test
    fun syncActionRunnerIgnoresNonSyncRoutes() = runBlocking {
        var synced = false
        val runner = StatusSyncActionRunner(
            syncNow = { _ -> synced = true },
            refresh = {},
            acceptedSignal = StatusAcceptedSignal()
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

    @Test
    fun acceptedSignalStartsFalse() {
        val signal = StatusAcceptedSignal()
        assertEquals(false, signal.state.value.isAccepted)
    }

    @Test
    fun acceptedSignalTriggerSetsTrue() {
        val signal = StatusAcceptedSignal()
        signal.trigger()
        assertEquals(true, signal.state.value.isAccepted)
    }

    @Test
    fun acceptedSignalClearIfGenerationResetsMatchingTrigger() {
        val signal = StatusAcceptedSignal()
        val generation = signal.trigger()
        signal.clearIfGeneration(generation)
        assertEquals(false, signal.state.value.isAccepted)
    }

    @Test
    fun acceptedSignalClearIfGenerationDoesNothingWhenNotTriggered() {
        val signal = StatusAcceptedSignal()
        signal.clearIfGeneration(0L)
        assertEquals(false, signal.state.value.isAccepted)
    }

    @Test
    fun syncActionPublishesAcceptedOnlyAfterEnqueueSucceeds() = runBlocking {
        val signal = StatusAcceptedSignal()
        var acceptedAtSyncTime = false
        var refreshed = false
        val runner = StatusSyncActionRunner(
            syncNow = { _ ->
                acceptedAtSyncTime = signal.state.value.isAccepted
            },
            refresh = { refreshed = true },
            acceptedSignal = signal
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertFalse("accepted must NOT be true during syncNow", acceptedAtSyncTime)
        assertTrue(refreshed)
        assertTrue("accepted must be true after syncNow succeeds", signal.state.value.isAccepted)
    }

    @Test
    fun syncActionRunnerPublishesAcceptedAndRefresh() = runBlocking {
        var synced = false
        var refreshed = false
        val signal = StatusAcceptedSignal()
        val runner = StatusSyncActionRunner(
            syncNow = { _ -> synced = true },
            refresh = { refreshed = true },
            acceptedSignal = signal
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertTrue(synced)
        assertTrue(refreshed)
        assertEquals(true, signal.state.value.isAccepted)
    }

    @Test
    fun syncActionRunnerDoesNotPublishAcceptedForNonSyncRoutes() = runBlocking {
        var synced = false
        val signal = StatusAcceptedSignal()
        val runner = StatusSyncActionRunner(
            syncNow = { _ -> synced = true },
            refresh = {},
            acceptedSignal = signal
        )

        runner.run(StatusActionRoute.OpenSettings)

        assertFalse(synced)
        assertEquals(false, signal.state.value.isAccepted)
    }

    @Test
    fun syncActionFailureDoesNotPublishAccepted() = runBlocking {
        val signal = StatusAcceptedSignal()
        val expectedException = RuntimeException("sync failed")
        var refreshCount = 0
        val runner = StatusSyncActionRunner(
            syncNow = { _ -> throw expectedException },
            refresh = { refreshCount++ },
            acceptedSignal = signal
        )

        val actualException = try {
            runner.run(StatusActionRoute.TriggerSync)
            null
        } catch (e: Throwable) {
            e
        }

        assertSame(expectedException, actualException)
        assertFalse("accepted must not be published on exception", signal.state.value.isAccepted)
        assertEquals(1, refreshCount)
    }

    @Test
    fun syncActionForwardsAllowMeteredOnce() = runBlocking {
        var captured: Boolean? = null
        val runner = StatusSyncActionRunner(
            syncNow = { captured = it },
            refresh = {},
            acceptedSignal = StatusAcceptedSignal()
        )

        runner.run(StatusActionRoute.TriggerSync, allowMeteredOnce = true)

        assertEquals(true, captured)
    }

    @Test
    fun syncActionDefaultsAllowMeteredOnceToFalse() = runBlocking {
        var captured: Boolean? = null
        val runner = StatusSyncActionRunner(
            syncNow = { captured = it },
            refresh = {},
            acceptedSignal = StatusAcceptedSignal()
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertEquals(false, captured)
    }

    @Test
    fun pendingUploadTotalAggregatesAllQueues() {
        val queues = QueueStatusSnapshot(
            pendingLocationPoints = 10,
            pendingUsageEvents = 5,
            pendingUsageSummaries = 3,
            pendingAppMetadata = 2,
            pendingDeviceProfile = 1,
            pendingSyncBatches = 0
        )
        assertEquals(10 + 5 + 3 + 2 + 1 + 0, queues.pendingUploadTotal)
    }

    @Test
    fun snapshotPlannerAddsBatteryOptimizationIssue() {
        val snapshot = StatusCenterSnapshot(
            permissions = PermissionStatusSnapshot(
                notificationGranted = true,
                preciseLocationGranted = true,
                backgroundLocationGranted = true,
                usageAccessGranted = true,
                activityRecognitionGranted = true,
                batteryOptimizationGranted = false
            ),
            api = ApiConnectionSnapshot(
                address = "https://valid.example",
                isValid = true,
                reasonCode = null,
                warnings = emptySet()
            ),
            auth = AuthStatusSnapshot(hasAccessToken = true, isExpired = false),
            service = ForegroundServiceSnapshot(
                continuousCollectionEnabled = true,
                serviceRunning = true
            ),
            tracking = TrackingPolicySnapshot(
                profile = "power-saving",
                currentPolicyMode = "PowerSavingNormal",
                nextExpectedLocationAtMillis = null
            ),
            queues = QueueStatusSnapshot(0, 0, 0, 0, 0, 0),
            diagnostics = DiagnosticSnapshot(null, null, null, null)
        )

        val issues = StatusIssuePlanner.plan(snapshot).associateBy { it.code }

        assertTrue(issues.containsKey("battery-optimization-missing"))
        val issue = issues.getValue("battery-optimization-missing")
        assertEquals(StatusSeverity.Warning, issue.severity)
        assertTrue(issue.title.contains("电池") || issue.title.contains("优化"))
        assertEquals(StatusActionTarget.Permissions, issue.target)
    }

    @Test
    fun unknownDroppedReasonProducesLocationDroppedRecent() {
        val snapshot = StatusCenterSnapshot(
            permissions = PermissionStatusSnapshot(true, true, true, true, true, true),
            api = ApiConnectionSnapshot("https://valid.example", isValid = true, reasonCode = null, warnings = emptySet()),
            auth = AuthStatusSnapshot(hasAccessToken = true, isExpired = false),
            service = ForegroundServiceSnapshot(continuousCollectionEnabled = true, serviceRunning = true),
            tracking = TrackingPolicySnapshot("power-saving", "", null),
            queues = QueueStatusSnapshot(0, 0, 0, 0, 0, 0),
            diagnostics = DiagnosticSnapshot(
                lastDroppedReason = "some-unknown-reason",
                lastDroppedAtMillis = 42_000L,
                lastLogMessage = null,
                lastHeartbeatStatus = null,
                recentLogMessages = emptyList()
            )
        )

        val issues = StatusIssuePlanner.plan(snapshot)
        assertTrue("unknown dropped reason must produce location-dropped-recent", issues.any { it.code == "location-dropped-recent" })
    }

    @Test
    fun networkDisconnectedTargetsNetworkSettings() {
        val issue = StatusIssue.networkDisconnected()
        assertEquals(StatusActionTarget.NetworkSettings, issue.target)
    }

    @Test
    fun probeBlockedTargetsConnectionCheck() {
        val issue = StatusIssue.probeBlocked()
        assertEquals(StatusActionTarget.ConnectionCheck, issue.target)
        assertEquals("重新检查连接", issue.actionLabel)
    }

    @Test
    fun probePartialTargetsConnectionCheck() {
        val issue = StatusIssue.probePartial()
        assertEquals(StatusActionTarget.ConnectionCheck, issue.target)
        assertEquals("重新检查连接", issue.actionLabel)
    }

    @Test
    fun foregroundServiceNotRunningOpensCollectionSettings() {
        val issue = StatusIssue.foregroundServiceNotRunning()
        assertEquals(StatusActionTarget.Settings, issue.target)
        assertEquals("查看采集设置", issue.actionLabel)
    }

    @Test
    fun locationAccuracyRejectedOpensCollectionSettings() {
        assertEquals(StatusActionTarget.Settings, StatusIssue.locationAccuracyRejected().target)
    }

    @Test
    fun altitudeMissingTimeoutOpensCollectionSettings() {
        assertEquals(StatusActionTarget.Settings, StatusIssue.altitudeMissingTimeout().target)
    }

    @Test
    fun uploadBacklogActionMatchesItsSyncRoute() {
        val issue = StatusIssue.uploadQueueBacklog(18)
        assertEquals("立即同步", issue.actionLabel)
        assertEquals(StatusActionTarget.Queue, issue.target)
        assertEquals(StatusActionRoute.TriggerSync, StatusActionRouter.route(issue.target))
    }

    @Test
    fun recentDroppedLocationHasNoAction() {
        assertEquals(StatusActionTarget.None, StatusIssue.recentDroppedLocation("x", null).target)
    }

    @Test
    fun routeForConnectionCheckReturnsConnectionCheck() {
        assertEquals(StatusActionRoute.ConnectionCheck, StatusActionRouter.route(StatusActionTarget.ConnectionCheck))
    }

    @Test
    fun routeForNetworkSettingsReturnsOpenNetworkSettings() {
        assertEquals(StatusActionRoute.OpenNetworkSettings, StatusActionRouter.route(StatusActionTarget.NetworkSettings))
    }

    @Test
    fun routeForNoneReturnsNone() {
        assertEquals(StatusActionRoute.None, StatusActionRouter.route(StatusActionTarget.None))
    }

    @Test
    fun syncFailureAlwaysUsesFixedSummary() {
        val issue = StatusIssue.syncFailure()
        assertEquals("最近同步出现异常，请导出日志查看详情。", issue.message)
        assertEquals(StatusSeverity.Critical, issue.severity)
        assertEquals(StatusActionTarget.Sync, issue.target)
    }

    @Test
    fun heartbeatFailureAlwaysUsesFixedSummary() {
        val issue = StatusIssue.heartbeatFailure()
        assertEquals("最近一次心跳上报异常。", issue.message)
        assertEquals(StatusSeverity.Warning, issue.severity)
        assertEquals(StatusActionTarget.Sync, issue.target)
    }

    @Test
    fun needAttentionContainsOnlyCriticalAndWarningInSeverityOrder() {
        val issues = listOf(
            StatusIssue.altitudeMissingTimeout(), // Info
            StatusIssue.probeBlocked(), // Critical
            StatusIssue.usageAccessMissing(), // Warning
            StatusIssue.apiAddressMissing(), // Critical
            StatusIssue.heartbeatFailure(), // Warning
            StatusIssue.recentDroppedLocation("test", null), // Info
        )
        val actionable = actionableStatusIssues(issues)
        val info = informationalStatusIssues(issues)

        assertEquals(4, actionable.size)
        assertTrue(actionable.all { it.severity != StatusSeverity.Info })
        assertEquals(
            listOf(
                StatusSeverity.Critical,
                StatusSeverity.Critical,
                StatusSeverity.Warning,
                StatusSeverity.Warning
            ),
            actionable.map { it.severity }
        )
        assertEquals(2, info.size)
        assertTrue(info.all { it.severity == StatusSeverity.Info })
    }

    @Test
    fun statusInformationAppearsOnlyWhenInfoExists() {
        val noInfo = listOf(
            StatusIssue.probeBlocked(), StatusIssue.usageAccessMissing()
        )
        val infoIssues = listOf(
            StatusIssue.altitudeMissingTimeout(),
            StatusIssue.recentDroppedLocation("x", null)
        )

        val info = informationalStatusIssues(noInfo)
        assertEquals(0, info.size)

        val info2 = informationalStatusIssues(infoIssues)
        assertEquals(2, info2.size)
    }

    @Test
    fun pendingUploadSnapshotHasNoPendingLogsField() {
        val hasField = runCatching {
            QueueStatusSnapshot::class.java.getDeclaredField("pendingLogs")
        }.isSuccess
        assertFalse("pendingLogs field must be removed from QueueStatusSnapshot", hasField)

        val q = QueueStatusSnapshot(
            pendingLocationPoints = 10,
            pendingUsageEvents = 5,
            pendingUsageSummaries = 3,
            pendingAppMetadata = 2,
            pendingDeviceProfile = 1,
            pendingSyncBatches = 0
        )
        assertEquals(10 + 5 + 3 + 2 + 1 + 0, q.pendingUploadTotal)
    }

    @Test
    fun batteryOptimizationGrantedDoesNotProduceIssue() {
        val snapshot = StatusCenterSnapshot(
            permissions = PermissionStatusSnapshot(
                notificationGranted = true,
                preciseLocationGranted = true,
                backgroundLocationGranted = true,
                usageAccessGranted = true,
                activityRecognitionGranted = true,
                batteryOptimizationGranted = true
            ),
            api = ApiConnectionSnapshot(
                address = "https://valid.example",
                isValid = true,
                reasonCode = null,
                warnings = emptySet()
            ),
            auth = AuthStatusSnapshot(hasAccessToken = true, isExpired = false),
            service = ForegroundServiceSnapshot(
                continuousCollectionEnabled = true,
                serviceRunning = true
            ),
            tracking = TrackingPolicySnapshot(
                profile = "power-saving",
                currentPolicyMode = "PowerSavingNormal",
                nextExpectedLocationAtMillis = null
            ),
            queues = QueueStatusSnapshot(0, 0, 0, 0, 0, 0),
            diagnostics = DiagnosticSnapshot(null, null, null, null)
        )

        val issues = StatusIssuePlanner.plan(snapshot).associateBy { it.code }

        assertFalse("battery-optimization-missing must not appear when granted", issues.containsKey("battery-optimization-missing"))
    }
}
