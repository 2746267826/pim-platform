package com.pim.app.ui.location

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pim.app.location.LocationSnapshot
import com.pim.app.ui.theme.PimTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun locationScreenShowsAllFourSections() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "尚未开始",
                phaseLabel = "空闲",
                deadlineText = "最长 30 秒",
                showStart = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(
                    state = uiState.value,
                    onStart = {},
                    onCancel = {},
                    onRestart = {},
                    onOpenSettings = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("location-status-section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-best-section").let {
            it.performScrollTo()
            it.assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("location-actions-section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-queue-section").let {
            it.performScrollTo()
            it.assertIsDisplayed()
        }
    }

    @Test
    fun idleStateShowsStartButton() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "尚未开始",
                phaseLabel = "空闲",
                showStart = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-start").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-start").performScrollTo()
        composeTestRule.onNodeWithText("开始定位").assertIsDisplayed()
    }

    @Test
    fun preparingShowsCancelButton() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "手动定位",
                phaseLabel = "准备中",
                showCancel = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-cancel").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun terminalStateShowsRestart() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "手动定位",
                phaseLabel = "已完成",
                showRestart = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-restart").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun openSettingsButtonShownForPrerequisiteErrors() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "尚未开始",
                phaseLabel = "空闲",
                showOpenSettings = true,
                errorMessage = "缺少精确定位权限",
                showStart = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-open-settings").assertIsDisplayed()
    }

    @Test
    fun openSettingsButtonTriggersOnOpenSettingsCallback() {
        var settingsOpened = false
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "尚未开始",
                phaseLabel = "空闲",
                showOpenSettings = true,
                errorMessage = "缺少精确定位权限",
                showStart = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(
                    state = uiState.value,
                    onStart = {},
                    onCancel = {},
                    onRestart = {},
                    onOpenSettings = { settingsOpened = true }
                )
            }
        }
        composeTestRule.onNodeWithTag("location-open-settings")
            .performScrollTo()
            .performClick()
        assertTrue("open settings callback should be invoked", settingsOpened)
    }

    @Test
    fun bestLocationDisplaysAllFields() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "手动定位",
                phaseLabel = "已完成",
                showRestart = true,
                bestLocation = LocationSnapshot(
                    latitude = 39.9042,
                    longitude = 116.4074,
                    horizontalAccuracyMeters = 15f,
                    provider = "gps",
                    source = "manual",
                    altitudeMeters = 52.0,
                    speedMetersPerSecond = 2.5f,
                    bearingDegrees = 90f,
                    timeMillis = 1700000000000L
                )
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-accuracy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-provider").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-latitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-longitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-altitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-speed").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-bearing").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-recorded-time").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bestLocationAllEightTagsAlwaysPresent() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "手动定位",
                phaseLabel = "已完成",
                showRestart = true,
                bestLocation = LocationSnapshot(
                    latitude = 39.9042,
                    longitude = 116.4074,
                    horizontalAccuracyMeters = null,
                    provider = "gps",
                    source = "manual",
                    altitudeMeters = null,
                    speedMetersPerSecond = null,
                    bearingDegrees = null,
                    timeMillis = 1700000000000L
                )
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-accuracy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-provider").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-latitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-longitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-altitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-speed").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-bearing").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location-recorded-time").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun manualVsAutoLabelDisplayed() {
        val manualState = mutableStateOf(
            LocationUiState(triggerLabel = "手动定位", phaseLabel = "采集位置中", showCancel = true)
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = manualState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithText("手动定位").assertIsDisplayed()
    }

    @Test
    fun startButtonTriggersOnStartCallback() {
        var started = false
        val uiState = mutableStateOf(
            LocationUiState(triggerLabel = "尚未开始", phaseLabel = "空闲", showStart = true)
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = { started = true }, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-start").performClick()
        assertTrue(started)
    }

    @Test
    fun cancelButtonTriggersOnCancelCallback() {
        var cancelled = false
        val uiState = mutableStateOf(
            LocationUiState(triggerLabel = "手动定位", phaseLabel = "采集位置中", showCancel = true)
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = { cancelled = true }, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-cancel").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun automaticBusyShowsDisabledStartWithProgressLabel() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "自动定位",
                phaseLabel = "采集位置中",
                showStart = true,
                showCancel = true,
                manualStartEnabled = false
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-start")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("定位进行中").assertIsDisplayed()
        composeTestRule.onNodeWithText("开始定位").assertDoesNotExist()
    }

    @Test
    fun manualBusyDoesNotShowStartButton() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "手动定位",
                phaseLabel = "采集位置中",
                showStart = false,
                showCancel = true,
                manualStartEnabled = true
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-start").assertDoesNotExist()
        composeTestRule.onNodeWithText("开始定位").assertDoesNotExist()
    }

    @Test
    fun pendingCountsDisplayed() {
        val uiState = mutableStateOf(
            LocationUiState(
                triggerLabel = "尚未开始",
                phaseLabel = "空闲",
                showStart = true,
                pendingUploadTotal = 15,
                pendingLocationPoints = 9
            )
        )
        composeTestRule.setContent {
            PimTheme {
                LocationScreen(state = uiState.value, onStart = {}, onCancel = {}, onRestart = {}, onOpenSettings = {})
            }
        }
        composeTestRule.onNodeWithTag("location-pending-total").performScrollTo().assertIsDisplayed()
            .assertTextEquals("15")
        composeTestRule.onNodeWithTag("location-pending-points").performScrollTo().assertIsDisplayed()
            .assertTextEquals("9")
    }
}
