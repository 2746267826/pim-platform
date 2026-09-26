package com.pim.app.keepalive

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 保活台账的读取窗口（REQ-17）。
 *
 * 这里守的是一个**由 `started` 开始标记引入的回归**：每次叫醒现在写两条记录
 * （started + 结果），如果读取窗口只按「需要几条**结果**」来定，窗口里的记录会被
 * started 标记占掉一半，导致「连续 3 次被压制」看不到足够证据、降频**晚一拍**才触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepAliveLedgerTest {

    private lateinit var db: AppDatabase
    private lateinit var ledger: KeepAliveLedger

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settings = TrackingSettingsStore(
            context.getSharedPreferences("keepalive-ledger-test", Context.MODE_PRIVATE)
        )
        ledger = KeepAliveLedger(db.forensicEventDao(), StructuredLogRepository(context, settings) { 0L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 模拟一次真实叫醒：先写 started 开始标记，再写带结果的记录（与执行链一致）。
     *
     * 时间戳贴近真实时序：started 落在预定时刻，结果落在预定时刻 + 延迟，
     * 因此按时间倒序读出来的顺序必然是 result_n, started_n, result_{n-1}, started_{n-1}, …
     * ——即**每条结果前面都夹着一个 started 标记**。这正是会吃掉读取窗口的情形。
     */
    private suspend fun wake(scheduled: Long, outcome: String, delayMillis: Long?) {
        ledger.recordFulfillment(
            AlarmFulfillmentRecord(
                scheduledAtUtcMillis = scheduled,
                actualAtUtcMillis = null,
                outcome = AlarmOutcomes.STARTED
            )
        )
        ledger.recordFulfillment(
            AlarmFulfillmentRecord(
                scheduledAtUtcMillis = scheduled,
                actualAtUtcMillis = delayMillis?.let { scheduled + it },
                outcome = outcome
            )
        )
    }

    /**
     * 连续 3 次被压制后，读取窗口必须能看到这 3 条结果——否则降频会晚一拍。
     *
     * 这条直接对应 AC-17.2：连续 3 次被压制后，**下一个周期**间隔就要 ×2。
     */
    @Test
    fun `AC-17_2 三次被压制后读取窗口能看到三次结果`() = runTest {
        // 每次叫醒间隔 30 分钟（与默认节奏一致），结果延迟 20 分钟（>15，构成被压制）。
        // 间距远大于延迟，保证时间戳互不并列，排序结果确定。
        repeat(3) { index ->
            wake(
                scheduled = 1_000_000L + index * 30 * 60_000L,
                outcome = AlarmOutcomes.SUPPRESSED,
                delayMillis = 20 * 60_000L
            )
        }

        val records = ledger.recentFulfillments()
        val evidence = records.filter { it.isSuppressedEvidence }

        assertTrue(
            "读取窗口必须覆盖 3 条被压制结果（实际只看到 ${evidence.size} 条，窗口=${records.size}）",
            evidence.size >= AlarmSuppressionPolicy.SUPPRESSIONS_BEFORE_BACKOFF
        )

        val effective = AlarmSuppressionPolicy.resolveEffectiveMinutes(
            configuredMinutes = 30,
            effectiveMinutes = 30,
            recentRecords = records
        )
        assertEquals("连续 3 次被压制后应立即降频到 60 分钟", 60, effective)
    }

    /** `started` 标记不得被当成证据（否则会把「连续」序列打断）。 */
    @Test
    fun `started 标记不构成任何证据`() = runTest {
        wake(scheduled = 1_000_000L, outcome = AlarmOutcomes.SUPPRESSED, delayMillis = 20 * 60_000L)

        val records = ledger.recentFulfillments()
        val started = records.filter { it.outcome == AlarmOutcomes.STARTED }

        assertTrue("应能读到 started 标记", started.isNotEmpty())
        assertTrue("started 不得构成被压制证据", started.none { it.isSuppressedEvidence })
        assertTrue("started 不得构成按时证据", started.none { it.isOnTimeEvidence })
    }
}
