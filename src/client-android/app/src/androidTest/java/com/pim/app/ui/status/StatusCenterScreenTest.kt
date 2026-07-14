package com.pim.app.ui.status

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeStage
import com.pim.app.status.PermissionStatusSnapshot
import com.pim.app.status.ServerCapabilities
import com.pim.app.status.StatusActionTarget
import com.pim.app.status.StatusCenterState
import com.pim.app.status.StatusIssue
import com.pim.app.status.StatusOverall
import com.pim.app.status.StatusSeverity
import com.pim.app.status.SyncPhase
import com.pim.app.ui.theme.PimTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatusCenterScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingShowsProgressAndDisablesSync() {
        val state = mutableStateOf(StatusCenterState.empty().copy(isLoading = true, syncPhase = SyncPhase.Idle))
        composeTestRule.setContent { PimTheme { StatusCenterContent(state = state.value) } }

        composeTestRule.onNodeWithTag("status-loading").assertIsDisplayed()
        composeTestRule.onNodeWithText("正在读取状态").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-sync-button").assertIsNotEnabled()
    }

    @Test
    fun overallNormalShowsNormalText() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState(syncPhase = SyncPhase.Idle)) }
        }
        composeTestRule.onNodeWithTag("status-overall").assertIsDisplayed()
        composeTestRule.onNodeWithText("正常").assertIsDisplayed()
    }

    @Test
    fun overallAttentionShowsAttentionText() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(overall = StatusOverall.Attention)) }
        }
        composeTestRule.onNodeWithText("需注意").assertIsDisplayed()
    }

    @Test
    fun overallAbnormalShowsAbnormalText() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(overall = StatusOverall.Abnormal)) }
        }
        composeTestRule.onNodeWithText("异常").assertIsDisplayed()
    }

    @Test
    fun syncPhaseRunningShowsRunningText() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(syncPhase = SyncPhase.Running)) }
        }
        composeTestRule.onNodeWithTag("status-sync-phase").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-sync-button").assertIsNotEnabled()
    }

    @Test
    fun transportFactsShowCountsAndTimes() {
        composeTestRule.setContent {
            PimTheme {
                StatusCenterContent(
                    normalState().copy(
                        pendingTotal = 42,
                        acceptedCount = 5,
                        rejectedCount = 3,
                        permanentRejectedCount = 1,
                        lastSuccessfulUploadAt = "2026-07-14 10:00",
                        lastAttemptedUploadAt = "2026-07-14 10:05",
                        nextAttemptAtMillis = 60_000L
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag("status-pending").assertIsDisplayed()
        composeTestRule.onNodeWithText("42").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-confirmed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-rejected").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permanent-rejected").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-last-success").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-last-attempt").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-next-attempt").assertIsDisplayed()
    }

    @Test
    fun transportShowsPlaceholdersWithoutHistoryOrSchedule() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState()) }
        }

        composeTestRule.onNode(
            hasTestTag("status-last-success") and hasAnyDescendant(hasText("暂无"))
        ).assertExists()
        composeTestRule.onNode(
            hasTestTag("status-last-attempt") and hasAnyDescendant(hasText("暂无"))
        ).assertExists()
        composeTestRule.onNode(
            hasTestTag("status-next-attempt") and hasAnyDescendant(hasText("未安排"))
        ).assertExists()
    }

    @Test
    fun collectionPermissionsNetworkProbeAndDiagnosticsAreVisible() {
        val probeResult = ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            lastCompletedStage = ConnectionProbeStage.WebRoot,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(true, true),
            serverIdentity = "https://example/api/v1/"
        )
        val state = normalState().copy(
            snapshot = normalState().snapshot.copy(
                permissions = PermissionStatusSnapshot(
                    notificationGranted = true,
                    preciseLocationGranted = false,
                    backgroundLocationGranted = true,
                    usageAccessGranted = false,
                    activityRecognitionGranted = true,
                    batteryOptimizationGranted = false
                ),
                tracking = normalState().snapshot.tracking.copy(
                    nextExpectedLocationAtMillis = 1_752_488_200_000L
                ),
                diagnostics = normalState().snapshot.diagnostics.copy(
                    lastLogMessage = "手机同步已完成。",
                    recentLogMessages = emptyList()
                )
            ),
            lastProbeResult = probeResult,
            lastProbeCheckedAtMillis = 1_000L,
            networkConnected = true
        )
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(state = state) }
        }

        composeTestRule.onNodeWithTag("status-permission-notification").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permission-precise-location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permission-background-location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permission-usage-access").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permission-activity-recognition").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-permission-battery-optimization").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-network").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-probe").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-diagnostics").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-next-location").assertExists()
        composeTestRule.onNodeWithText("最近记录").assertExists()
        composeTestRule.onNodeWithText("手机同步已完成。").assertExists()
    }

    @Test
    fun connectionProbeShowsNotCheckedWhenNoEvidenceExists() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(lastProbeResult = null)) }
        }

        composeTestRule.onNode(
            hasTestTag("status-probe") and hasAnyDescendant(hasText("未检查"))
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun syncButtonIdleIsEnabled() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(syncPhase = SyncPhase.Idle, isLoading = false)) }
        }
        composeTestRule.onNodeWithTag("status-sync-button").assertIsEnabled()
    }

    @Test
    fun syncButtonRunningIsDisabled() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(syncPhase = SyncPhase.Running, isLoading = false)) }
        }
        composeTestRule.onNodeWithTag("status-sync-button").assertIsNotEnabled()
    }

    @Test
    fun syncButtonFailedIsEnabled() {
        composeTestRule.setContent {
            PimTheme { StatusCenterContent(normalState().copy(syncPhase = SyncPhase.Failed, isLoading = false)) }
        }
        composeTestRule.onNodeWithTag("status-sync-button").assertIsEnabled()
    }

    @Test
    fun syncButtonClicksOnceForIdle() {
        var clickCount = 0
        composeTestRule.setContent {
            PimTheme {
                StatusCenterContent(
                    state = normalState().copy(syncPhase = SyncPhase.Idle, isLoading = false),
                    onSyncNow = { clickCount++ }
                )
            }
        }
        composeTestRule.onNodeWithTag("status-sync-button").performClick()
        assertTrue("Expected 1 click", clickCount == 1)
    }

    @Test
    fun syncButtonIdleEnabledEvenWithZeroPendingAndOffline() {
        composeTestRule.setContent {
            PimTheme {
                StatusCenterContent(
                    state = normalState().copy(
                        syncPhase = SyncPhase.Idle,
                        pendingTotal = 0,
                        networkConnected = false,
                        isLoading = false
                    )
                )
            }
        }
        composeTestRule.onNodeWithTag("status-sync-button").assertIsEnabled()
    }

    @Test
    fun issueActionsAreExplicitAndNonActionableItemsHaveNoButton() {
        var actionClickCount = 0
        val actionableIssue = StatusIssue(
            code = "login-missing",
            severity = StatusSeverity.Critical,
            title = "未登录",
            message = "需要登录后才能同步。",
            actionLabel = "去登录",
            target = StatusActionTarget.Settings
        )
        val nonActionableIssue = StatusIssue.recentDroppedLocation("unknown", null)
        val state = normalState().copy(
            issues = listOf(actionableIssue, nonActionableIssue)
        )
        composeTestRule.setContent {
            PimTheme {
                StatusCenterContent(
                    state = state,
                    onIssueAction = { actionClickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("status-issue-login-missing").assertExists()
        composeTestRule.onNodeWithTag("status-issue-action-login-missing")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue("Expected 1 action click", actionClickCount == 1)
        composeTestRule.onNodeWithTag("status-issue-location-dropped-recent").assertExists()
        composeTestRule.onNodeWithTag("status-issue-action-location-dropped-recent").assertDoesNotExist()
    }

    private fun normalState(
        syncPhase: SyncPhase = SyncPhase.Idle
    ): StatusCenterState {
        return StatusCenterState.empty().copy(
            isLoading = false,
            overall = StatusOverall.Normal,
            syncPhase = syncPhase,
            snapshot = StatusCenterState.empty().snapshot
        )
    }
}
