package com.pim.app.ui.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pim.app.MainActivity
import com.pim.app.ui.theme.PimTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PimRootScreenNavTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val allTags = listOf(
        "pim-nav-today", "pim-nav-location", "pim-nav-tracks",
        "pim-nav-schedule", "pim-nav-status", "pim-nav-settings"
    )

    @Test
    fun navigationHasSixItemsWithCorrectTags() {
        for (tag in allTags) {
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun navigationLabelsAreChinese() {
        for (label in listOf("今日", "定位", "轨迹", "日程", "状态", "设置")) {
            composeTestRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun clickingLocationNavShowsLocationPage() {
        composeTestRule.onNodeWithTag("pim-nav-location").performClick()
        composeTestRule.onNodeWithTag("location-status-section").assertIsDisplayed()
    }
}

class PimBottomNavigationLayoutTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val allTags = listOf(
        "pim-nav-today", "pim-nav-location", "pim-nav-tracks",
        "pim-nav-schedule", "pim-nav-status", "pim-nav-settings"
    )

    @Test
    fun navItemsFitWithoutOverlapOnNarrowScreens() {
        var widthDp by mutableStateOf(320)
        var selected = PimDestination.Today
        composeTestRule.setContent {
            PimTheme {
                Box(modifier = Modifier.width(widthDp.dp).testTag("pim-nav-host")) {
                    PimBottomNavigation(
                        selected = selected,
                        onSelected = { selected = it }
                    )
                }
            }
        }

        for (widthDpValue in listOf(320, 360)) {
            widthDp = widthDpValue
            composeTestRule.waitForIdle()

            val hostBounds = composeTestRule.onNodeWithTag("pim-nav-host")
                .getUnclippedBoundsInRoot()

            for (tag in allTags) {
                composeTestRule.onAllNodesWithTag(tag).assertCountEquals(1)
                val nodeBounds = composeTestRule.onNodeWithTag(tag)
                    .getUnclippedBoundsInRoot()
                assertTrue(
                    "$widthDpValue $tag left ${nodeBounds.left.value} >= host left ${hostBounds.left.value}",
                    nodeBounds.left >= hostBounds.left
                )
                assertTrue(
                    "$widthDpValue $tag right ${nodeBounds.right.value} <= host right ${hostBounds.right.value}",
                    nodeBounds.right <= hostBounds.right
                )
            }

            val items = allTags.map { tag ->
                tag to composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            }.sortedBy { (_, bounds) -> bounds.left }

            for (i in 0 until items.size - 1) {
                val (prevTag, prevBounds) = items[i]
                val (nextTag, nextBounds) = items[i + 1]
                assertTrue(
                    "$widthDpValue $prevTag.right ${prevBounds.right.value} > $nextTag.left ${nextBounds.left.value}",
                    prevBounds.right <= nextBounds.left + 0.5.dp
                )
            }

            composeTestRule.onNodeWithTag("pim-nav-location").performClick()
            assertTrue(
                "clicking Location at ${widthDpValue}dp",
                selected == PimDestination.Location
            )
        }
    }
}
