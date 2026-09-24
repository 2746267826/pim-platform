package com.pim.app.forensics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.DroppedDiagnosticExportRow
import com.pim.app.data.ForensicEventDao
import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileLocationDroppedDiagnosticEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.core.models.ApiResponse
import com.pim.core.models.MobileForensicsIngestResponse
import com.pim.core.models.MobileForensicsUploadRequest
import com.pim.core.network.ApiService
import java.lang.reflect.Proxy
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
 * REQ-5 / REQ-9 的设备端上报（AC-5.1 / AC-5.2 / AC-5.3 / AC-9.2）。
 *
 * 用记录型假 ApiService（动态代理，与生产 `ApiClientProvider.dynamicApiService()` 同一手法）
 * 捕获真实请求对象，因此断言的是**实际会发出去的内容**，而不是测试自己构造的副本。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForensicUploadCoordinatorTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var dao: ForensicEventDao
    private lateinit var mobileDao: MobileDataDao
    private lateinit var ledger: ForensicLedger
    private lateinit var logs: StructuredLogRepository

    private val captured = mutableListOf<MobileForensicsUploadRequest>()

    private val hour = 3_600_000L
    private val now = 1_756_684_800_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.forensicEventDao()
        mobileDao = db.mobileDataDao()
        logs = StructuredLogRepository(
            context,
            TrackingSettingsStore(context.getSharedPreferences("upload-test", Context.MODE_PRIVATE))
        ) { now }
        ledger = ForensicLedger(dao, logs)
        captured.clear()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 记录型假 ApiService：只实现取证上报，其余方法一律抛出（用不到就是不该被调用）。
     * 动态代理可以直接实现 `suspend` 方法——这正是生产代码里 `dynamicApiService()` 的做法。
     */
    private fun fakeApi(
        respond: (MobileForensicsUploadRequest) -> ApiResponse<MobileForensicsIngestResponse>
    ): ApiService = Proxy.newProxyInstance(
        ApiService::class.java.classLoader,
        arrayOf(ApiService::class.java)
    ) { _, method, args ->
        when (method.name) {
            "uploadMobileForensics" -> {
                val request = args!![0] as MobileForensicsUploadRequest
                captured += request
                respond(request)
            }
            "toString" -> "RecordingApiService"
            "hashCode" -> System.identityHashCode(this)
            "equals" -> false
            else -> error("Unexpected API call in test: ${method.name}")
        }
    } as ApiService

    private fun ack(
        acceptedKeys: List<String> = emptyList(),
        skippedKeys: List<String> = emptyList(),
        rejectedKeys: List<String> = emptyList()
    ) = ApiResponse(
        code = 0,
        message = "ok",
        data = MobileForensicsIngestResponse(
            acceptedCount = acceptedKeys.size,
            skippedCount = skippedKeys.size,
            rejectedCount = rejectedKeys.size,
            failedCount = 0,
            acceptedKeys = acceptedKeys,
            skippedKeys = skippedKeys,
            rejectedKeys = rejectedKeys,
            acceptedDroppedReasonCount = 0
        )
    )

    private fun coordinator(respond: (MobileForensicsUploadRequest) -> ApiResponse<MobileForensicsIngestResponse>) =
        ForensicUploadCoordinator(
            context = context,
            api = fakeApi(respond),
            dao = dao,
            logs = logs,
            sentinelStore = ForensicSentinelStore(context),
            nowUtcMillis = { now }
        )

    @Test
    fun `AC-5_1 all locally queued events are uploaded through the forensic channel`() = runTest {
        ledger.recordHeartbeat(now - 2 * hour, "{\"bootElapsedMs\":1}")
        ledger.recordHeartbeat(now - hour, "{\"bootElapsedMs\":2}")
        ledger.recordProcessExit(
            now - 30 * 60_000L,
            ProcessExitReasons.LOW_MEMORY,
            "{\"reason\":\"REASON_LOW_MEMORY\"}"
        )

        var call = 0
        val uploaded = coordinator { request ->
            call++
            if (call == 1) ack(acceptedKeys = request.events.map { it.clientItemKey }) else ack()
        }.uploadPending()

        assertEquals(3, uploaded)
        val first = captured.first()
        assertEquals(3, first.events.size)
        assertTrue(first.events.any { it.eventType == ForensicEventTypes.HEARTBEAT })
        assertTrue(first.events.any { it.eventType == ForensicEventTypes.PROCESS_EXIT })
        assertTrue(first.events.any { it.payloadJson!!.contains("REASON_LOW_MEMORY") })
        assertTrue(first.batchId!!.startsWith("android-forensics-"))
        assertEquals(0, ledger.pendingCount())
    }

    @Test
    fun `AC-5_2 a server-side skip still settles the local queue and does not duplicate rows`() = runTest {
        val key = ledger.heartbeatKey(now)
        ledger.recordHeartbeat(now, "{}")

        var call = 0
        val uploaded = coordinator {
            call++
            if (call == 1) ack(skippedKeys = listOf(key)) else ack()
        }.uploadPending()

        assertEquals(1, uploaded)
        assertEquals(0, ledger.pendingCount())
        assertEquals(1, ledger.totalCount())
    }

    @Test
    fun `AC-5_3 a failed upload keeps events locally and never throws`() = runTest {
        ledger.recordHeartbeat(now, "{}")

        val uploaded = coordinator { throw java.io.IOException("network down") }.uploadPending()

        assertEquals(0, uploaded)
        assertEquals(1, ledger.pendingCount())
        assertEquals(1, ledger.totalCount())
        val row = dao.pendingEvents(MobileSyncStatus.PENDING, 10).single()
        assertEquals(MobileSyncStatus.PENDING, row.syncStatus)
        assertTrue(row.lastError != null)
    }

    @Test
    fun `AC-5_3 a rejected item stays pending with a visible reason`() = runTest {
        val key = ledger.heartbeatKey(now)
        ledger.recordHeartbeat(now, "{}")

        var call = 0
        coordinator {
            call++
            if (call == 1) ack(rejectedKeys = listOf(key)) else ack()
        }.uploadPending()

        // 先按本批全部 rejected 计：服务端明确拒绝的条目留在 pending 并带原因，不静默消失。
        assertEquals(1, ledger.pendingCount())
        val row = dao.pendingEvents(MobileSyncStatus.PENDING, 10).single()
        assertEquals("服务端拒绝了该取证事件条目。", row.lastError)

        // 第二次上报被接受后队列归零，证明"失败不影响采集与同步"的另一半：重试能收敛。
        coordinator { ack(acceptedKeys = listOf(key)) }.uploadPending()
        assertEquals(0, ledger.pendingCount())
    }

    @Test
    fun `AC-9_2 dropped reason counts are uploaded per local day and reason`() = runTest {
        insertDropped(reason = "horizontal-accuracy-too-low", atUtc = now - 2 * hour)
        insertDropped(reason = "horizontal-accuracy-too-low", atUtc = now - hour)
        insertDropped(reason = "missing-horizontal-accuracy", atUtc = now - 30 * 60_000L)

        var call = 0
        coordinator {
            call++
            if (call == 1) {
                ack(acceptedKeys = emptyList())
            } else {
                ack()
            }
        }.uploadPending()

        val summaries = captured.first().droppedReasonSummaries
        val today = summaries.filter { it.count > 0 }
        assertEquals(2, today.size)
        assertEquals(
            2,
            today.single { it.reason == "horizontal-accuracy-too-low" }.count
        )
        assertEquals(
            1,
            today.single { it.reason == "missing-horizontal-accuracy" }.count
        )
        // 窗口内的其余日期显式上报 0，服务端才能把旧计数归零而不是越用越偏。
        assertTrue(summaries.any { it.count == 0 })
    }

    @Test
    fun `nothing is uploaded when the queue is empty`() = runTest {
        val uploaded = coordinator { ack() }.uploadPending()

        assertEquals(0, uploaded)
        assertTrue(captured.isEmpty())
    }

    @Test
    fun `recent dropped details expose the fields required by the in-app page`() = runTest {
        mobileDao.insertDroppedLocationDiagnostic(
            MobileLocationDroppedDiagnosticEntity(
                recordedAtUtc = now - hour,
                provider = "gps",
                accuracyMeters = 55f,
                policyMode = "Movement",
                reason = "horizontal-accuracy-too-low"
            )
        )

        val details: List<DroppedDiagnosticExportRow> = coordinator { ack() }.recentDroppedDetails(limit = 20)

        val row = details.single()
        assertEquals("horizontal-accuracy-too-low", row.reason)
        assertEquals("gps", row.provider)
        assertEquals(55f, row.accuracyMeters!!, 0.001f)
        assertEquals("Movement", row.policyMode)
    }

    private suspend fun insertDropped(reason: String, atUtc: Long) {
        mobileDao.insertDroppedLocationDiagnostic(
            MobileLocationDroppedDiagnosticEntity(
                recordedAtUtc = atUtc,
                provider = "gps",
                accuracyMeters = 55f,
                policyMode = "PowerSavingNormal",
                reason = reason
            )
        )
    }
}
