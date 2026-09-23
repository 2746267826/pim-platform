package com.pim.app.forensics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.ForensicEventDao
import com.pim.app.data.MobileSyncStatus
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.mobile.logs.StructuredLogRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-3 / REQ-6 的设备端台账（AC-3.3 / AC-6.2）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForensicLedgerTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ForensicEventDao
    private lateinit var ledger: ForensicLedger

    private val day = 24L * 60L * 60L * 1000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.forensicEventDao()
        val settings = TrackingSettingsStore(
            context.getSharedPreferences("ledger-test", Context.MODE_PRIVATE)
        )
        ledger = ForensicLedger(dao, StructuredLogRepository(context, settings) { 0L })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `AC-3_3 several wakes within the same second produce exactly one heartbeat`() = runTest {
        val base = 1_756_684_800_123L

        assertTrue(ledger.recordHeartbeat(base, "{\"n\":1}"))
        assertFalse(ledger.recordHeartbeat(base + 10, "{\"n\":2}"))
        assertFalse(ledger.recordHeartbeat(base + 800, "{\"n\":3}"))

        assertEquals(1, ledger.totalCount())
        val stored = dao.recentByType(ForensicEventTypes.HEARTBEAT, 10).single()
        assertEquals("{\"n\":1}", stored.payloadJson)
    }

    @Test
    fun `next second produces a new heartbeat`() = runTest {
        val base = 1_756_684_800_000L

        assertTrue(ledger.recordHeartbeat(base, "{}"))
        assertTrue(ledger.recordHeartbeat(base + 1_000, "{}"))

        assertEquals(2, ledger.totalCount())
    }

    @Test
    fun `exit records are deduplicated by second and reason`() = runTest {
        // ApplicationExitInfo 没有唯一 ID：同一（秒 + 原因）的重复读取只应产生一条。
        val at = 1_756_684_800_000L

        assertTrue(ledger.recordProcessExit(at, ProcessExitReasons.LOW_MEMORY, "{}"))
        assertFalse(ledger.recordProcessExit(at + 500, ProcessExitReasons.LOW_MEMORY, "{}"))
        assertTrue(ledger.recordProcessExit(at + 500, ProcessExitReasons.ANR, "{}"))

        assertEquals(2, ledger.totalCount())
    }

    @Test
    fun `AC-6_2 entries older than thirty days are purged regardless of sync status`() = runTest {
        val now = 1_756_684_800_000L
        val old = now - 31 * day
        val recent = now - 29 * day

        dao.insertIgnore(
            com.pim.app.data.ForensicEventEntity(
                eventType = ForensicEventTypes.HEARTBEAT,
                occurredAtUtc = old,
                clientItemKey = "old-synced",
                payloadJson = "{}",
                syncStatus = MobileSyncStatus.SYNCED
            )
        )
        dao.insertIgnore(
            com.pim.app.data.ForensicEventEntity(
                eventType = ForensicEventTypes.HEARTBEAT,
                occurredAtUtc = old,
                clientItemKey = "old-pending",
                payloadJson = "{}",
                syncStatus = MobileSyncStatus.PENDING
            )
        )
        ledger.recordHeartbeat(recent, "{}")

        val removed = ledger.purgeExpired(now)

        assertEquals(2, removed)
        assertEquals(1, ledger.totalCount())
        assertEquals(1, ledger.pendingCount())
        assertEquals(recent, ledger.lastHeartbeatAtUtc())
    }

    @Test
    fun `pending count drops to zero once every event is marked synced`() = runTest {
        val at = 1_756_684_800_000L
        ledger.recordHeartbeat(at, "{}")
        ledger.recordProcessExit(at, ProcessExitReasons.ANR, "{}")

        assertEquals(2, ledger.pendingCount())

        val keys = listOf(
            ledger.heartbeatKey(at),
            ledger.exitKey(at, ProcessExitReasons.ANR)
        )
        ledger.markSynced(keys)

        assertEquals(0, ledger.pendingCount())
        assertEquals(2, ledger.totalCount())
    }

    @Test
    fun `failed uploads stay pending with a visible error`() = runTest {
        val at = 1_756_684_800_000L
        ledger.recordHeartbeat(at, "{}")

        ledger.markFailed(listOf(ledger.heartbeatKey(at)), "network down")

        assertEquals(1, ledger.pendingCount())
        val row = dao.recentByType(ForensicEventTypes.HEARTBEAT, 1).single()
        assertEquals(MobileSyncStatus.PENDING, row.syncStatus)
        assertEquals("network down", row.lastError)
    }

    @Test
    fun `latest cause event prefers the newest exit or force stop record`() = runTest {
        val at = 1_756_684_800_000L
        ledger.recordProcessExit(at - 60_000, ProcessExitReasons.LOW_MEMORY, "{}")
        ledger.recordForceStop(at, ForceStopKinds.FORCE_STOP, "{}")

        val latest = ledger.latestCauseEvent()
        assertEquals(ForensicEventTypes.FORCE_STOP, latest?.eventType)
        assertEquals(at, latest?.occurredAtUtc)
    }
}
