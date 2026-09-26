package com.pim.app.keepalive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-23 ColorOS 设置引导页。
 *
 * 关键区分（工单依据 android-平台依据 §6）：
 * **可检测项**自动读数，**不可检测项**只能手动勾选并记住。
 * 把不可检测项谎报成「已检测」是这类引导页最典型的错误，因此这里逐项断言分类，
 * 并断言每一项都有可执行的文字路径（AC-23.4 的回退依赖它）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorOsGuidanceCatalogTest {

    private fun logs(): StructuredLogRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TrackingSettingsStore(
            context.getSharedPreferences("guidance-test", Context.MODE_PRIVATE)
        )
        return StructuredLogRepository(context, store) { 0L }
    }

    /** AC-23.1 / AC-23.3：清单覆盖工单点名的全部关键项。 */
    @Test
    fun `REQ-23 清单覆盖工单点名的全部引导项`() {
        val keys = ColorOsGuidanceCatalog.ITEMS.map { it.key }.toSet()
        assertTrue(keys.containsAll(
            setOf(
                ColorOsGuidanceCatalog.EXACT_ALARM,
                ColorOsGuidanceCatalog.BATTERY_OPTIMIZATION,
                ColorOsGuidanceCatalog.STANDBY_BUCKET,
                ColorOsGuidanceCatalog.AUTO_START,
                ColorOsGuidanceCatalog.BACKGROUND_RUNNING,
                ColorOsGuidanceCatalog.APP_FREEZE,
                ColorOsGuidanceCatalog.SLEEP_STANDBY
            )
        ))
    }

    /**
     * AC-23.2 的前提：只有系统真的能查的项才允许标成「可检测」。
     * 厂商后台管理开关（自启动/后台运行/速冻/睡眠待机）官方无稳定公开 API，
     * 必须归入手动项——否则引导页会显示一个编造的「已开启」。
     */
    @Test
    fun `AC-23_2 厂商开关归入手动项且不得标为可检测`() {
        val manualKeys = ColorOsGuidanceCatalog.manual.map { it.key }.toSet()
        assertEquals(
            setOf(
                ColorOsGuidanceCatalog.AUTO_START,
                ColorOsGuidanceCatalog.BACKGROUND_RUNNING,
                ColorOsGuidanceCatalog.APP_FREEZE,
                ColorOsGuidanceCatalog.SLEEP_STANDBY
            ),
            manualKeys
        )
    }

    /** AC-23.2：闹钟权限 / 电池优化 / 待机桶三项可自动检测。 */
    @Test
    fun `AC-23_2 可检测项为闹钟权限与电池优化与待机桶`() {
        assertEquals(
            setOf(
                ColorOsGuidanceCatalog.EXACT_ALARM,
                ColorOsGuidanceCatalog.BATTERY_OPTIMIZATION,
                ColorOsGuidanceCatalog.STANDBY_BUCKET
            ),
            ColorOsGuidanceCatalog.detectable.map { it.key }.toSet()
        )
    }

    /** AC-23.4：每一项都必须有文字路径（跳转失败时回退显示）。 */
    @Test
    fun `AC-23_4 每一项都有可读的文字路径`() {
        ColorOsGuidanceCatalog.ITEMS.forEach { item ->
            assertTrue("${item.title} 缺少文字路径", item.manualPath.isNotBlank())
            assertTrue("${item.title} 缺少原因说明", item.why.isNotBlank())
        }
    }

    /** AC-26.1 / AC-26.2：标题与说明为简体中文，且不含英文占位串。 */
    @Test
    fun `AC-26_1 引导项文案为简体中文`() {
        ColorOsGuidanceCatalog.ITEMS.forEach { item ->
            assertTrue("标题应为中文：${item.title}", item.title.any { it.code > 0x4E00 })
            assertFalse("不得出现 TODO 占位：${item.title}", item.title.contains("TODO", ignoreCase = true))
            assertFalse("不得出现占位串：${item.why}", item.why.contains("lorem", ignoreCase = true))
        }
    }

    /** AC-23.3：手动勾选后状态被记住（此处验证状态模型的语义）。 */
    @Test
    fun `AC-23_3 手动勾选后视为已满足`() {
        val item = ColorOsGuidanceCatalog.item(ColorOsGuidanceCatalog.AUTO_START)!!
        val before = GuidanceItemState(item, detectedOk = null, manuallyCompleted = false)
        val after = GuidanceItemState(item, detectedOk = null, manuallyCompleted = true)

        assertFalse(before.satisfied)
        assertTrue(after.satisfied)
        assertEquals("待确认", before.statusText())
        assertEquals("已标记完成", after.statusText())
    }

    /** 可检测项的满足与否由读数决定，不受手动勾选影响。 */
    @Test
    fun `AC-23_2 可检测项的满足与否由读数决定`() {
        val item = ColorOsGuidanceCatalog.item(ColorOsGuidanceCatalog.EXACT_ALARM)!!
        val denied = GuidanceItemState(item, detectedOk = false, manuallyCompleted = true)

        assertFalse("读数说未授予时，不能因为手动勾选就当作已就绪", denied.satisfied)
        assertEquals("需要处理", denied.statusText())
    }

    /** AC-23.2：读不到时显示「无法读取」，不得猜值。 */
    @Test
    fun `AC-23_2 读不到时状态为待确认`() {
        val item = ColorOsGuidanceCatalog.item(ColorOsGuidanceCatalog.STANDBY_BUCKET)!!
        val unknown = GuidanceItemState(item, detectedOk = null, manuallyCompleted = false)
        assertEquals("待确认", unknown.statusText())
    }

    /** AC-22.1：设置页与引导页共用同一份清单（可检测项 + 手动项 = 全部）。 */
    @Test
    fun `可检测项与手动项之和等于全部项`() {
        assertEquals(
            ColorOsGuidanceCatalog.ITEMS.size,
            ColorOsGuidanceCatalog.detectable.size + ColorOsGuidanceCatalog.manual.size
        )
    }

    /** 待机桶文案必须是中文可读（AC-26.1）。 */
    @Test
    fun `AC-26_1 待机桶文案为中文`() {
        assertEquals("受限（后台活动被大幅限制）", GuidanceDetector.bucketOf(45))
        assertEquals("活跃", GuidanceDetector.bucketOf(10))
        assertTrue(GuidanceDetector.bucketOf(9999).isNotBlank())
    }

    /** AC-23.2：检测器在真机/模拟器上必须能给出读数（Robolectric 提供真实系统服务）。 */
    @Test
    fun `AC-23_2 检测器能读取电池优化与闹钟权限状态`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checker = ExactAlarmPermissionChecker(context, KeepAliveTestDoubles.scheduler(), logs())
        val detector = GuidanceDetector(context, checker, logs())

        // 读得到（非 null）即说明走到了真实系统 API；具体真假由设备状态决定。
        assertNotNull(detector.ignoresBatteryOptimizations())
        assertNotNull(detector.exactAlarmGranted())

        val states = detector.detectableStates(emptySet())
        assertEquals("可检测项应为三项", 3, states.size)
        assertTrue(states.all { it.item.detectability == GuidanceDetectability.DETECTABLE })
    }

    /** AC-23.3：手动项的状态来自持久化的勾选集合。 */
    @Test
    fun `AC-23_3 手动项状态来自已勾选集合`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checker = ExactAlarmPermissionChecker(context, KeepAliveTestDoubles.scheduler(), logs())
        val detector = GuidanceDetector(context, checker, logs())

        val none = detector.manualStates(emptySet())
        assertTrue(none.none { it.manuallyCompleted })

        val some = detector.manualStates(setOf(ColorOsGuidanceCatalog.APP_FREEZE))
        val freeze = some.first { it.item.key == ColorOsGuidanceCatalog.APP_FREEZE }
        assertTrue(freeze.manuallyCompleted)
        assertTrue(freeze.satisfied)
    }

    /** 未知 key 返回 null，不得编造一个引导项。 */
    @Test
    fun `未知 key 返回 null`() {
        assertNull(ColorOsGuidanceCatalog.item("no-such-key"))
    }
}
