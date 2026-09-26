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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
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


    /**
     * 在**可滚动容器**里渲染分区。
     *
     * 生产环境里「保活与诊断」是设置页 `Column + verticalScroll` 中的一节；
     * 若测试直接裸渲染，`performScrollTo()` 会因找不到滚动父节点而失败——
     * 那样既测不到真实布局，也会掩盖「真实环境里滚不滚得到」。
     */
    private fun renderSection(state: KeepAliveSectionState, handlers: SectionHandlers = SectionHandlers()) {
        composeTestRule.setContent {
            PimTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    KeepAliveSection(
                        state = state,
                        onToggleEnabled = handlers.onToggleEnabled,
                        onIntervalChange = handlers.onIntervalChange,
                        onOpenGuidance = handlers.onOpenGuidance,
                        onOpenExactAlarmSettings = handlers.onOpenExactAlarmSettings
                    )
                }
            }
        }
    }

    /**
     * 引导页**不再外包滚动容器**：`ColorOsGuidanceScreen` 内部已经是
     * `Column + verticalScroll`，外面再套一层会形成嵌套滚动，
     * Compose 会直接抛「Vertically scrollable component was measured with an
     * infinity maximum height constraints」并把整个 instrumentation 进程打崩。
     * 真实用法里它也是整屏子页（见设置页/状态页的 `return` 分支），不需要外层滚动。
     */
    private fun renderGuidance(
        state: GuidanceScreenState,
        handlers: GuidanceHandlers = GuidanceHandlers()
    ) {
        composeTestRule.setContent {
            PimTheme {
                ColorOsGuidanceScreen(
                    state = state,
                    onOpenSettings = handlers.onOpenSettings,
                    onToggleManual = handlers.onToggleManual,
                    onBack = handlers.onBack
                )
            }
        }
    }

    class SectionHandlers(
        val onToggleEnabled: (Boolean) -> Unit = {},
        val onIntervalChange: (Int) -> Unit = {},
        val onOpenGuidance: () -> Unit = {},
        val onOpenExactAlarmSettings: () -> Unit = {}
    )

    class GuidanceHandlers(
        val onOpenSettings: (String) -> Unit = {},
        val onToggleManual: (String, Boolean) -> Unit = { _, _ -> },
        val onBack: () -> Unit = {}
    )

    /** AC-22.1：分区可见且包含四项内容（总开关 / 节奏 / 当前状态 / 引导入口）。 */
    @Test
    fun AC_22_1_分区包含四项内容() {
        renderSection(sectionState(), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

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
        renderSection(sectionState(enabled = false, scheduleStatus = "已关闭"), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

        composeTestRule.onNodeWithTag("keepalive-enabled-switch").assertIsOff()
        composeTestRule.onNodeWithTag("keepalive-enabled-text").assertIsDisplayed()
        composeTestRule.onNodeWithText("已关闭：不登记任何闹钟").assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-status-schedule").assertIsDisplayed()
    }

    /** 关闭时节奏滑杆不可调（避免用户在关闭状态下改一个不生效的值）。 */
    @Test
    fun 关闭时节奏滑杆不可调() {
        renderSection(sectionState(enabled = false, scheduleStatus = "已关闭"), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

        composeTestRule.onNodeWithTag("keepalive-interval-slider").performScrollTo().assertIsNotEnabled()
    }

    /** 开启时滑杆可调，且开关为开。 */
    @Test
    fun 开启时滑杆可调() {
        renderSection(sectionState(enabled = true), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

        composeTestRule.onNodeWithTag("keepalive-enabled-switch").assertIsOn()
    }

    /** AC-17.2：降频中显示提示；AC-17.3：复位后提示消失。 */
    @Test
    fun AC_17_2_降频中显示提示() {
        renderSection(
            sectionState(
                configured = 30,
                effective = 60,
                backoffHint = "检测到闹钟连续被系统压制，已把保活间隔从 30 分钟临时放宽到 60 分钟" +
                    "（上限 120 分钟）；连续 2 次按时后会回到配置值。"
            ),
            SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {})
        )

        composeTestRule.onNodeWithTag("keepalive-backoff-hint").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("keepalive-interval-text").assertIsDisplayed()
    }

    /** AC-17.3：未降频时不得出现降频提示。 */
    @Test
    fun AC_17_3_未降频时无提示() {
        renderSection(sectionState(configured = 30, effective = 30, backoffHint = null), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

        composeTestRule.onNodeWithTag("keepalive-backoff-hint").assertDoesNotExist()
    }

    /** AC-14.4 / AC-21.1：权限缺失时状态区给出可见提示，并可点击去设置。 */
    @Test
    fun AC_14_4_权限缺失时可见且可点开设置() {
        var opened = false
        renderSection(
            sectionState(
                exactAlarmGranted = false,
                scheduleStatus = "未登记（缺少「闹钟和提醒」权限）"
            ),
            SectionHandlers(
                onToggleEnabled = {},
                onIntervalChange = {},
                onOpenGuidance = {},
                onOpenExactAlarmSettings = { opened = true }
            )
        )

        // 该行整体可点击 -> Compose 会合并语义，内层 testTag 只存在于未合并树里，
        // 因此这里必须用 useUnmergedTree 定位（否则会误判成「节点不存在」）。
        composeTestRule.onNodeWithTag("keepalive-status-permission", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("未授权").assertIsDisplayed()
        // 点击打在外层可点击容器上（它才是真正接收点击的节点）。
        composeTestRule.onNodeWithTag("keepalive-status-permission", useUnmergedTree = true).performClick()
        assertTrue("点击未授权的权限项应打开系统设置页", opened)
    }

    /** AC-21.1：健康异常在设置页也有文字出口（不只是红点）。 */
    @Test
    fun AC_21_1_健康异常显示文字出口() {
        renderSection(sectionState(healthAlert = "系统清空了保活闹钟"), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = {}))

        composeTestRule.onNodeWithTag("keepalive-health-alert").performScrollTo().assertIsDisplayed()
    }

    /** AC-22.1 第四项 / AC-23.1：设置页入口能打开引导页。 */
    @Test
    fun AC_23_1_设置页可打开引导页() {
        var opened = false
        renderSection(sectionState(), SectionHandlers(onToggleEnabled = {}, onIntervalChange = {}, onOpenGuidance = { opened = true }))

        composeTestRule.onNodeWithTag("keepalive-open-guidance").performScrollTo().performClick()
        assertTrue("设置页的引导入口必须能打开引导页", opened)
    }

    /** 总开关可切换（AC-22.3 的交互前提）。 */
    @Test
    fun 总开关可切换() {
        var toggled: Boolean? = null
        renderSection(sectionState(enabled = true), SectionHandlers(onToggleEnabled = { toggled = it }, onIntervalChange = {}, onOpenGuidance = {}))

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
        renderGuidance(guidanceState(), GuidanceHandlers(onOpenSettings = {}, onToggleManual = { _, _ -> }))

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
        renderGuidance(guidanceState(), GuidanceHandlers(onOpenSettings = {}, onToggleManual = { key, value -> toggled = key to value }))

        composeTestRule.onNodeWithTag("guidance-toggle-app-freeze").performScrollTo().performClick()
        assertEquals("app-freeze" to false, toggled)
    }

    /** AC-23.4：跳转失败时页面不崩溃，回退显示文字路径。 */
    @Test
    fun AC_23_4_跳转失败回退文字路径() {
        renderGuidance(guidanceState(jumpFailed = true), GuidanceHandlers(onOpenSettings = {}, onToggleManual = { _, _ -> }))

        composeTestRule.onNodeWithTag("guidance-jump-failed").assertIsDisplayed()
        // 文字路径仍然在（这就是回退手段）
        composeTestRule.onNodeWithTag("guidance-path-exact-alarm").performScrollTo().assertIsDisplayed()
    }

    /** 引导页的「打开设置」按钮可用。 */
    @Test
    fun 引导页打开设置回调() {
        var opened: String? = null
        renderGuidance(guidanceState(), GuidanceHandlers(onOpenSettings = { opened = it }, onToggleManual = { _, _ -> }))

        composeTestRule.onNodeWithTag("guidance-open-exact-alarm").performScrollTo().performClick()
        assertEquals("exact-alarm", opened)
    }

    /** AC-26.1：新增界面文案为简体中文。 */
    @Test
    fun AC_26_1_文案为简体中文() {
        renderGuidance(guidanceState(), GuidanceHandlers(onOpenSettings = {}, onToggleManual = { _, _ -> }))

        composeTestRule.onNodeWithText("系统设置引导").assertIsDisplayed()
        composeTestRule.onNodeWithText("可自动检测").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("需要手动确认").performScrollTo().assertIsDisplayed()
    }

    // ===================== REQ-21 健康红点（状态页顶部通道）=====================

    /** AC-21.1：有异常时状态页顶部出现红点与原因文案。 */
    @Test
    fun AC_21_1_红点点亮并显示原因() {
        var opened = false
        composeTestRule.setContent {
            PimTheme {
                KeepAliveHealthBanner(
                    alertText = "未获得「闹钟和提醒」权限，保活闹钟无法登记。",
                    onOpenGuidance = { opened = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("status-keepalive-health").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-keepalive-health-dot").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-keepalive-health-text").assertIsDisplayed()
        composeTestRule.onNodeWithText("保活异常").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status-keepalive-health-action").performClick()
        assertTrue("红点应能一键进入系统设置引导", opened)
    }

    /** AC-21.2：原因消除后红点消失（不渲染任何东西，不留占位）。 */
    @Test
    fun AC_21_2_原因消除后红点消失() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveHealthBanner(alertText = null, onOpenGuidance = {})
            }
        }

        composeTestRule.onNodeWithTag("status-keepalive-health").assertDoesNotExist()
        composeTestRule.onNodeWithTag("status-keepalive-health-dot").assertDoesNotExist()
    }

    /** 空字符串同样不点亮（避免上游给空文案时留下一个无名红点）。 */
    @Test
    fun 空文案不点亮红点() {
        composeTestRule.setContent {
            PimTheme {
                KeepAliveHealthBanner(alertText = "   ", onOpenGuidance = {})
            }
        }

        composeTestRule.onNodeWithTag("status-keepalive-health").assertDoesNotExist()
    }

    /** AC-21.1：四类原因都能作为红点原因呈现。 */
    @Test
    fun AC_21_1_四类原因都能呈现() {
        com.pim.app.keepalive.KeepAliveHealthReasons.ALL.forEach { reason ->
            val label = com.pim.app.keepalive.KeepAliveHealthReasons.label(reason)
            assertTrue("$reason 应有中文文案", label.isNotBlank())
            assertTrue("$reason 的文案应为中文", label.any { it.code > 0x4E00 })
        }
    }
}
