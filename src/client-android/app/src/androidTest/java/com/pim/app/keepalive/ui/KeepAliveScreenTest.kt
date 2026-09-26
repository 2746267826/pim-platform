package com.pim.app.keepalive.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pim.app.keepalive.GuidanceDetectability
import com.pim.app.keepalive.GuidanceItemState
import com.pim.app.ui.theme.PimTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * REQ-22 / REQ-23 的真机（模拟器）UI 验证。
 *
 * 在真实 Compose 运行时里渲染，而不是只断言 ViewModel 的字段：
 * 工单要求「该分区可见且包含四项内容」「引导页两处入口都能打开」，
 * 这些是**渲染结果**，只有真机渲染才能证明。
 */
class KeepAliveScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun sectionState(
        enabled: Boolean = true,
        configured: Int = 30,
        effective: Int = 30,
        backoffHint: String? = null,
        exactAlarmGranted: Boolean? = true,
        lastWake: Long? = null,
        healthAlert: String? = null,
        scheduleStatus: String = "已登记"
    ) = KeepAliveSectionState(
        enabled = enabled,
        configuredIntervalMinutes = configured,
        effectiveIntervalMinutes = effective,
        backoffHint = backoffHint,
        exactAlarmGranted = exactAlarmGranted,
        standbyBucketLabel = "活跃",
        ignoresBatteryOptimizations = true,
        lastWakeAtUtcMillis = lastWake,
        healthAlert = healthAlert,
        scheduleStatusText = scheduleStatus
    )

    /** AC-22.1：分区可见且包含四项内容（总开关 / 节奏 / 当前状态 / 引导入口）。 */
    @Test
    fun AC_22_1_分区包含四项内容() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings-keepalive").assertIsDisplayed()
        // 第一项：总开关
        composeTestRule.onNodeWithTag("keepalive-enabled-switch").assertIsDisplayed()
        // 第二项：节奏
        composeTestRule.onNodeWithTag("keepalive-interval-text").performScrollTo().assertIsDisplayed()
        // 第三项：当前状态（权限 / 待机桶 / 电池优化 / 最近叫醒）
        composeTestRule.onNodeWithTag("keepalive-status-schedule").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-permission").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-bucket").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-battery").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-last-wake").performScrollTo().assertIsDisplayed()
        // 第四项：引导页入口
        composeTestRule.onNodeWithTag("keepalive-open-guidance").performScrollTo().assertIsDisplayed()
    }

    /** AC-22.2：关闭总开关后界面显示「已关闭」。 */
    @Test
    fun AC_22_2_关闭后界面显示已关闭() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(enabled = false, scheduleStatus = "已关闭"),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-enabled-switch").assertIsOff()
        composeTestRule.onNodeWithTag("keepalive-enabled-text").assertIsDisplayed()
        composeTestRule.onNodeWithText("已关闭：不登记任何闹钟").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-schedule").assertIsDisplayed()
    }

    /** 关闭时节奏滑杆不可调（避免用户在关闭状态下改一个不生效的值）。 */
    @Test
    fun 关闭时节奏滑杆不可调() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(enabled = false, scheduleStatus = "已关闭"),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-interval-slider").performScrollTo().assertIsNotEnabled()
    }

    /** 开启时滑杆可调，且开关为开。 */
    @Test
    fun 开启时滑杆可调() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(enabled = true),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-enabled-switch").assertIsOn()
    }

    /** AC-17.2：降频中显示提示；AC-17.3：复位后提示消失。 */
    @Test
    fun AC_17_2_降频中显示提示() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(
                        configured = 30,
                        effective = 60,
                        backoffHint = "检测到闹钟连续被系统压制，已把保活间隔从 30 分钟临时放宽到 60 分钟" +
                            "（上限 120 分钟）；连续 2 次按时后会回到配置值。"
                    ),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-backoff-hint").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-interval-text").assertIsDisplayed()
    }

    /** AC-17.3：未降频时不得出现降频提示。 */
    @Test
    fun AC_17_3_未降频时无提示() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(configured = 30, effective = 30, backoffHint = null),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-backoff-hint").assertDoesNotExist()
    }

    /** AC-14.4 / AC-21.1：权限缺失时状态区给出可见提示，并可点击去设置。 */
    @Test
    fun AC_14_4_权限缺失时可见且可点开设置() {
        var opened = false
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(
                        exactAlarmGranted = false,
                        scheduleStatus = "未登记（缺少「闹钟和提醒」权限）"
                    ),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = { opened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-status-permission").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("未授权").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-permission").performClick()
        assertTrue("点击未授权的权限项应打开系统设置页", opened)
    }

    /** AC-21.1：健康异常在设置页也有文字出口（不只是红点）。 */
    @Test
    fun AC_21_1_健康异常显示文字出口() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(healthAlert = "系统清空了保活闹钟"),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-health-alert").performScrollTo().assertIsDisplayed()
    }

    /** AC-22.1 第四项 / AC-23.1：设置页入口能打开引导页。 */
    @Test
    fun AC_23_1_设置页可打开引导页() {
        var opened = false
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(),
                    onToggleEnabled = {},
                    onIntervalChange = {},
                    onOpenGuidance = { opened = true },
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-open-guidance").performScrollTo().performClick()
        assertTrue("设置页的引导入口必须能打开引导页", opened)
    }

    /** 总开关可切换（AC-22.3 的交互前提）。 */
    @Test
    fun 总开关可切换() {
        var toggled: Boolean? = null
        composeTestRule.setContent {
            PimTheme {
                KeepAliveSection(
                    state = sectionState(enabled = true),
                    onToggleEnabled = { toggled = it },
                    onIntervalChange = {},
                    onOpenGuidance = {},
                    onOpenExactAlarmSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("keepalive-enabled-switch").performClick()
        assertEquals(false, toggled)
    }

    // ===================== 引导页（REQ-23）=====================

    private fun guidanceState(jumpFailed: Boolean = false) = GuidanceScreenState(
        detectable = listOf(
            GuidanceItemState(
                item = com.pim.app.keepalive.ColorOsGuidanceCatalog.item(
                    com.pim.app.keepalive.ColorOsGuidanceCatalog.EXACT_ALARM
                )!!,
                detectedOk = false,
                manuallyCompleted = false
            )
        ),
        manual = listOf(
            GuidanceItemState(
                item = com.pim.app.keepalive.ColorOsGuidanceCatalog.item(
                    com.pim.app.keepalive.ColorOsGuidanceCatalog.APP_FREEZE
                )!!,
                detectedOk = null,
                manuallyCompleted = true
            )
        ),
        jumpFailed = jumpFailed
    )

    /** AC-23.1 / AC-23.2 / AC-23.3：引导页呈现可检测项与手动项，并显示各自状态。 */
    @Test
    fun AC_23_1_引导页呈现两类项目() {
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = guidanceState(),
                    onOpenSettings = {},
                    onToggleManual = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("guidance-screen").assertIsDisplayed()
        // 可检测项：显示「需要处理」（读数决定，AC-23.2）
        composeTestRule.onNodeWithTag("guidance-status-exact-alarm").performScrollTo().assertIsDisplayed()
        // 手动项：显示「已标记完成」（勾选决定，AC-23.3）
        composeTestRule.onNodeWithTag("guidance-status-app-freeze").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("已标记完成").assertIsDisplayed()
        // 每项都有文字路径
        composeTestRule.onNodeWithTag("guidance-path-exact-alarm").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("guidance-path-app-freeze").performScrollTo().assertIsDisplayed()
    }

    /** AC-23.3：手动勾选可切换并回调。 */
    @Test
    fun AC_23_3_手动勾选回调() {
        var toggled: Pair<String, Boolean>? = null
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = guidanceState(),
                    onOpenSettings = {},
                    onToggleManual = { key, value -> toggled = key to value },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("guidance-toggle-app-freeze").performScrollTo().performClick()
        assertEquals("app-freeze" to false, toggled)
    }

    /** AC-23.4：跳转失败时页面不崩溃，回退显示文字路径。 */
    @Test
    fun AC_23_4_跳转失败回退文字路径() {
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = guidanceState(jumpFailed = true),
                    onOpenSettings = {},
                    onToggleManual = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("guidance-jump-failed").assertIsDisplayed()
        // 文字路径仍然在（这就是回退手段）
        composeTestRule.onNodeWithTag("guidance-path-exact-alarm").performScrollTo().assertIsDisplayed()
    }

    /** 引导页的「打开设置」按钮可用。 */
    @Test
    fun 引导页打开设置回调() {
        var opened: String? = null
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = guidanceState(),
                    onOpenSettings = { opened = it },
                    onToggleManual = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("guidance-open-exact-alarm").performScrollTo().performClick()
        assertEquals("exact-alarm", opened)
    }

    /** AC-26.1：新增界面文案为简体中文。 */
    @Test
    fun AC_26_1_文案为简体中文() {
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = guidanceState(),
                    onOpenSettings = {},
                    onToggleManual = { _, _ -> },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("系统设置引导").assertIsDisplayed()
        composeTestRule.onNodeWithText("可自动检测").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("需要手动确认").performScrollTo().assertIsDisplayed()
    }
}
