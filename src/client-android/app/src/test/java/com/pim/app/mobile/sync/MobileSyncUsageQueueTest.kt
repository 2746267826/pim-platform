package com.pim.app.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileAppMetadataEntity
import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileSyncStatus
import com.pim.app.data.MobileUsageEventEntity
import com.pim.app.data.MobileUsageSummaryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MobileSyncUsageQueueTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MobileDataDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.mobileDataDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- PendingUsageBatch loading ---

    @Test
    fun `loadPendingUsageBatch includes PENDING and FAILED excludes SYNCED and REJECTED`() = runTest {
        val e1 = dao.insertUsageEvents(listOf(event(1000, MobileSyncStatus.PENDING))).first()
        dao.insertUsageEvents(listOf(event(2000, MobileSyncStatus.SYNCED)))
        dao.insertUsageEvents(listOf(event(3000, MobileSyncStatus.REJECTED)))
        val e2 = dao.insertUsageEvents(listOf(event(4000, MobileSyncStatus.FAILED))).first()

        val batch = loadPendingUsageBatch(dao, 500)

        val allIds = (batch.events.map { it.id } + batch.summaries.map { it.id }).toSet()
        assertTrue(allIds.contains(e1))
        assertTrue(allIds.contains(e2))
        assertEquals(2, batch.events.size)
        assertEquals(0, batch.summaries.size)
    }

    @Test
    fun `loadPendingUsageBatch PENDING prioritized before FAILED`() = runTest {
        dao.insertUsageEvents(listOf(event(1000, MobileSyncStatus.FAILED)))
        val pendingId = dao.insertUsageEvents(listOf(event(2000, MobileSyncStatus.PENDING))).first()

        val batch = loadPendingUsageBatch(dao, 1)

        assertEquals(1, batch.events.size)
        assertEquals(pendingId, batch.events.single().id)
    }

    @Test
    fun `loadPendingUsageBatch limit 500 is not blocking`() = runTest {
        val entities = (0 until 501).map { i -> event(i * 1000L, MobileSyncStatus.PENDING) }
        dao.insertUsageEvents(entities)

        val batch = loadPendingUsageBatch(dao, 500)

        assertEquals(500, batch.totalCount)
        assertEquals(500, batch.events.size)
        assertEquals(501, pendingUsageRemaining(dao))
    }

    @Test
    fun `loadPendingUsageBatch appMetadataOnly yields valid window start LE end plus 1ms`() = runTest {
        dao.upsertAppMetadata(listOf(appMeta("com.test", collectedAtUtc = 5000, MobileSyncStatus.PENDING)))

        val batch = loadPendingUsageBatch(dao, 500)

        assertTrue(batch.events.isEmpty())
        assertTrue(batch.summaries.isEmpty())
        assertEquals(1, batch.apps.size)
        assertNotNull(batch.windowStartUtc)
        assertNotNull(batch.windowEndUtc)
        assertTrue(batch.windowEndUtc!! > batch.windowStartUtc!!)
        assertTrue(batch.windowStartUtc!! <= 5000)
        assertTrue(batch.windowEndUtc!! >= 5000)
    }

    @Test
    fun `loadPendingUsageBatch mixed types captures window from earliest to latest`() = runTest {
        dao.insertUsageEvents(listOf(event(1000, MobileSyncStatus.PENDING)))
        dao.insertUsageSummaries(listOf(summary(8000, 9000, MobileSyncStatus.PENDING)))
        dao.upsertAppMetadata(listOf(appMeta("com.test", collectedAtUtc = 5000, MobileSyncStatus.PENDING)))

        val batch = loadPendingUsageBatch(dao, 500)

        assertEquals(1000L, batch.windowStartUtc)
        assertEquals(9000L, batch.windowEndUtc)
    }

    @Test
    fun `loadPendingUsageBatch empty returns null window`() = runTest {
        val batch = loadPendingUsageBatch(dao, 500)

        assertEquals(0, batch.totalCount)
        assertEquals(null, batch.windowStartUtc)
        assertEquals(null, batch.windowEndUtc)
    }

    // --- pendingUsageRemaining excludes location ---

    @Test
    fun `pendingUsageRemaining counts usage only not location`() = runTest {
        dao.insertUsageEvents(listOf(event(1000, MobileSyncStatus.PENDING)))
        dao.insertUsageSummaries(listOf(summary(2000, 3000, MobileSyncStatus.PENDING)))
        dao.upsertAppMetadata(listOf(appMeta("com.a", collectedAtUtc = 5000, MobileSyncStatus.PENDING)))

        val locPoint = com.pim.app.data.MobileLocationPointEntity(
            latitude = 0.0, longitude = 0.0,
            recordedAtUtc = 1000, source = "test",
            collectedAtUtc = 1000, rawJson = "{}"
        )
        dao.insertLocationPoint(locPoint)

        val remaining = pendingUsageRemaining(dao)

        assertEquals(3, remaining)
    }

    @Test
    fun `pendingUsageRemaining returns true row count not category count`() = runTest {
        dao.insertUsageEvents(listOf(event(1000, MobileSyncStatus.PENDING), event(2000, MobileSyncStatus.PENDING)))
        dao.insertUsageSummaries(listOf(summary(3000, 4000, MobileSyncStatus.PENDING)))
        dao.upsertAppMetadata(listOf(appMeta("com.a", collectedAtUtc = 5000, MobileSyncStatus.PENDING)))

        val locPoint = com.pim.app.data.MobileLocationPointEntity(
            latitude = 0.0, longitude = 0.0,
            recordedAtUtc = 1000, source = "test",
            collectedAtUtc = 1000, rawJson = "{}"
        )
        dao.insertLocationPoint(locPoint)

        val remaining = pendingUsageRemaining(dao)

        assertEquals(4, remaining)
    }

    // --- sortedMergeOutcome ---

    @Test
    fun `sortedMergeOutcome RETRY beats BLOCKED`() {
        assertEquals(MobileSyncOutcome.RETRY, sortedMergeOutcome(MobileSyncOutcome.RETRY, MobileSyncOutcome.BLOCKED))
    }

    @Test
    fun `sortedMergeOutcome BLOCKED beats SUCCESS`() {
        assertEquals(MobileSyncOutcome.BLOCKED, sortedMergeOutcome(MobileSyncOutcome.BLOCKED, MobileSyncOutcome.SUCCESS))
    }

    @Test
    fun `sortedMergeOutcome RETRY beats SUCCESS`() {
        assertEquals(MobileSyncOutcome.RETRY, sortedMergeOutcome(MobileSyncOutcome.RETRY, MobileSyncOutcome.SUCCESS))
    }

    companion object {
        fun event(timeUtc: Long, status: String) = MobileUsageEventEntity(
            packageName = "com.test",
            className = null,
            eventType = 1,
            eventName = "test",
            eventTimeUtc = timeUtc,
            source = "test",
            sourceWindowStartUtc = timeUtc,
            sourceWindowEndUtc = timeUtc + 1000,
            collectedAtUtc = timeUtc,
            rawJson = "{}",
            syncStatus = status
        )

        fun summary(startUtc: Long, endUtc: Long, status: String) = MobileUsageSummaryEntity(
            packageName = "com.test",
            windowStartUtc = startUtc,
            windowEndUtc = endUtc,
            totalTimeForegroundMs = 1000,
            lastTimeUsedUtc = endUtc,
            firstTimeStampUtc = startUtc,
            lastTimeStampUtc = endUtc,
            source = "UsageStatsManager",
            sourceWindowStartUtc = startUtc,
            sourceWindowEndUtc = endUtc,
            collectedAtUtc = endUtc,
            rawJson = "{}",
            syncStatus = status
        )

        fun appMeta(pkg: String, collectedAtUtc: Long, status: String) = MobileAppMetadataEntity(
            packageName = pkg,
            label = "Test",
            versionCode = 1,
            firstInstallTimeUtc = 0,
            lastUpdateTimeUtc = 0,
            isSystemApp = false,
            collectedAtUtc = collectedAtUtc,
            rawJson = "{}",
            syncStatus = status
        )
    }
}
