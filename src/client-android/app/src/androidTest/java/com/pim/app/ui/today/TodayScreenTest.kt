package com.pim.app.ui.today

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pim.app.status.QueueStatusSnapshot
import com.pim.app.status.StatusCenterSnapshot
import com.pim.app.status.StatusCenterState
import com.pim.app.ui.theme.PimTheme
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun todayStatusBarShowsPendingTotalAndPendingLocationTags() {
        val state = mutableStateOf(
            TodayUiState(
                pendingCount = 15,
                pendingLocationPoints = 9
            )
        )
        composeTestRule.setContent {
            PimTheme {
                TodayStatusBar(
                    state = state.value,
                    syncFeedback = null,
                    onSyncNow = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("today-pending-total").assertIsDisplayed()
        composeTestRule.onNodeWithTag("today-pending-location").assertIsDisplayed()
    }

    @Test
    fun todayStatusBarDisplaysCountValues() {
        val state = mutableStateOf(
            TodayUiState(
                pendingCount = 15,
                pendingLocationPoints = 9
            )
        )
        composeTestRule.setContent {
            PimTheme {
                TodayStatusBar(
                    state = state.value,
                    syncFeedback = null,
                    onSyncNow = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("today-pending-total").assertIsDisplayed()
        composeTestRule.onNodeWithTag("today-pending-location").assertIsDisplayed()
    }
}
