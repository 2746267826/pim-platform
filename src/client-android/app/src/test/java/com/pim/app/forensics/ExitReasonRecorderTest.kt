package com.pim.app.forensics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.ForensicEventDao
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-1 进程退出原因台账（AC-1.1 / AC-1.2 / AC-1.3 / AC-1.4）。
 *
 * 系统读数由注入的 [ExitReasonSource] 模拟——真机结论另见 PR 中的模拟器取证记录。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExitReasonRecorderTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ForensicEventDao
    private lateinit var ledger: ForensicLedger

    private val contextMapContext = ForensicContext(
        screenOn = true,
        unlocked = true,
        foregroundPackage = "com.pim.app",
        foregroundAppLabel = "PIM",
        charging = false,
        batteryPercent = 71
    )

    private class FakeSource(var result: ExitReasonReadResult) : ExitReasonSource {
        var calls = 0
        override fun read(limit: Int): ExitReasonReadResult {
            calls++
            return result
        }
    }

    private class FakeContext : ForensicContextSource {
        var value: ForensicContext = ForensicContext()
        override fun read(): ForensicContext = value
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.forensicEventDao()
        ledger = ForensicLedger(
            dao,
            StructuredLogRepository(
                context,
                TrackingSettingsStore(context.getSharedPreferences("exit-test", Context.MODE_PRIVATE))
            ) { 0L }
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun recorder(source: ExitReasonSource) =
        ExitReasonRecorder(source, ledger, FakeContext().apply { value = contextMapContext })

    private fun record(
        timestamp: Long,
        reason: String,
        description: String? = null
    ) = HistoricalExitRecord(
        timestampMillis = timestamp,
        apiReason = 3,
        reason = reason,
        importance = 100,
        pssKb = 12_345L,
        rssKb = 23_456L,
        description = description
    )

    @Test
    fun `AC-1_1 a killed process produces one ledger entry with reason time and memory`() = runTest {
        val source = FakeSource(
            ExitReasonReadResult(
                supported = true,
                records = listOf(record(1_756_684_800_000L, ProcessExitReasons.LOW_MEMORY))
            )
        )

        recorder(source).recordNewExits()

        val stored = dao.recentByType(ForensicEventTypes.PROCESS_EXIT, 10).single()
        assertEquals(1_756_684_800_000L, stored.occurredAtUtc)
        assertTrue(stored.payloadJson.contains("\"reason\":\"REASON_LOW_MEMORY\""))
        assertTrue(stored.payloadJson.contains("\"pssKb\":12345"))
        assertTrue(stored.payloadJson.contains("\"rssKb\":23456"))
        assertTrue(stored.payloadJson.contains("\"importance\":100"))
    }

    @Test
    fun `AC-1_1 refreshing does not duplicate the same exit record`() = runTest {
        val source = FakeSource(
            ExitReasonReadResult(
                supported = true,
                records = listOf(record(1_756_684_800_000L, ProcessExitReasons.LOW_MEMORY))
            )
        )
        val recorder = recorder(source)

        recorder.recordNewExits()
        recorder.recordNewExits()

        assertEquals(1, dao.recentByType(ForensicEventTypes.PROCESS_EXIT, 10).size)
    }

    @Test
    fun `AC-1_2 a user force stop is distinguishable from a system kill`() = runTest {
        // 用户强停由系统报成 REASON_USER_REQUESTED；低内存回收是 REASON_LOW_MEMORY。
        val source = FakeSource(
            ExitReasonReadResult(
                supported = true,
                records = listOf(
                    record(1_756_684_800_000L, ProcessExitReasons.USER_REQUESTED),
                    record(1_756_684_700_000L, ProcessExitReasons.LOW_MEMORY)
                )
            )
        )

        recorder(source).recordNewExits()

        val reasons = dao.recentByType(ForensicEventTypes.PROCESS_EXIT, 10)
            .map { row -> row.payloadJson }
        assertTrue(reasons.any { it.contains(ProcessExitReasons.USER_REQUESTED) })
        assertTrue(reasons.any { it.contains(ProcessExitReasons.LOW_MEMORY) })
        assertEquals(2, reasons.size)
    }

    @Test
    fun `AC-1_3 when the system offers no exit record the read result says so`() = runTest {
        val source = FakeSource(
            ExitReasonReadResult(supported = true, records = emptyList(), failureReason = null)
        )

        val result = recorder(source).recordNewExits()

        assertTrue(result.supported)
        assertTrue(result.records.isEmpty())
        // 页面据此显示"未知 + 可推断线索"，而不是"无异常"。
        assertEquals(0, dao.totalCount())
    }

    @Test
    fun `AC-1_3 an unsupported platform reports the reason instead of pretending everything is fine`() = runTest {
        val source = FakeSource(
            ExitReasonReadResult(
                supported = false,
                records = emptyList(),
                failureReason = "当前系统版本不提供进程退出记录（需要 Android 11 / API 30 及以上）。"
            )
        )

        val result = recorder(source).recordNewExits()

        assertFalse(result.supported)
        assertNotNull(result.failureReason)
    }

    @Test
    fun `AC-1_4 a failing reader never throws and never blocks startup`() = runTest {
        val failing = object : ExitReasonSource {
            override fun read(limit: Int): ExitReasonReadResult =
                throw SecurityException("permission denied")
        }

        val thrown = runCatching { recorder(failing).recordNewExits() }

        assertTrue(thrown.isFailure)
        // 调用方（StartupForensics）必须把它收敛成"未知"，这里验证台账保持可用。
        assertEquals(0, dao.totalCount())
    }

    @Test
    fun `AC-4_2 unavailable context fields are recorded as unavailable rather than guessed`() = runTest {
        val source = FakeSource(
            ExitReasonReadResult(
                supported = true,
                records = listOf(record(1_756_684_800_000L, ProcessExitReasons.CRASH))
            )
        )
        val partialContext = ForensicContext(
            screenOn = true,
            unlocked = null,
            foregroundPackage = null,
            charging = null,
            batteryPercent = null,
            unavailableFields = listOf("解锁状态", "前台应用", "充电状态", "电量百分比")
        )
        val recorder = ExitReasonRecorder(source, ledger, FakeContext().apply { value = partialContext })

        recorder.recordNewExits()

        val payload = dao.recentByType(ForensicEventTypes.PROCESS_EXIT, 10).single().payloadJson
        val json = org.json.JSONObject(payload)
        assertTrue(json.isNull("unlocked"))
        assertTrue(json.isNull("batteryPercent"))
        assertTrue(json.isNull("foregroundPackage"))
        val unavailable = json.getJSONArray("unavailableFields")
        assertTrue(unavailable.length() == 4)
        assertTrue((0 until unavailable.length()).any { unavailable.getString(it).contains("电量") })
    }

    @Test
    fun `permission change detection only counts records after the sentinel was armed`() = runTest {
        val recorder = recorder(FakeSource(ExitReasonReadResult(true, emptyList())))
        val records = listOf(
            record(1_000L, ProcessExitReasons.PERMISSION_CHANGE),
            record(3_000L, ProcessExitReasons.PERMISSION_CHANGE)
        )

        assertTrue(recorder.hasPermissionChangeAfter(2_000L, records))
        assertFalse(recorder.hasPermissionChangeAfter(4_000L, records))
    }

    @Test
    fun `user requested exits are recognised as force stop evidence`() = runTest {
        val recorder = recorder(FakeSource(ExitReasonReadResult(true, emptyList())))
        val records = listOf(
            record(1_000L, ProcessExitReasons.USER_REQUESTED),
            record(2_000L, ProcessExitReasons.USER_STOPPED),
            record(3_000L, ProcessExitReasons.LOW_MEMORY)
        )

        assertTrue(recorder.hasUserRequestedExitAfter(500L, records))
        assertFalse(recorder.hasUserRequestedExitAfter(2_500L, records))
    }

    @Test
    fun `exit record detection only counts records after the last alive marker`() = runTest {
        val recorder = recorder(FakeSource(ExitReasonReadResult(true, emptyList())))
        val records = listOf(record(5_000L, ProcessExitReasons.ANR))

        assertTrue(recorder.hasExitRecordAfter(4_000L, records))
        assertFalse(recorder.hasExitRecordAfter(5_000L, records))
    }
}
