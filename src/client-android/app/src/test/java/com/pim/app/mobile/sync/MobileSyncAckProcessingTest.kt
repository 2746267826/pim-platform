package com.pim.app.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileAppMetadataEntity
import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.data.MobileSyncStatus
import com.pim.app.data.MobileUsageEventEntity
import com.pim.app.data.MobileUsageSummaryEntity
import com.pim.core.models.MobileIngestItemResult
import com.pim.core.models.MobileIngestResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MobileSyncAckProcessingTest {
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

    // --- Confirmed items deleted for all three entity types ---

    @Test
    fun confirmedItemsDeleted() = runTest {
        val eventIds = dao.insertUsageEvents(listOf(event(), event()))
        val summaryIds = dao.insertUsageSummaries(listOf(summary(), summary()))
        dao.upsertAppMetadata(listOf(appMeta("com.a"), appMeta("com.b")))

        processUsageAcknowledgements(dao, linkedSetOf(
            MobileAcknowledgementItem("usage-event", eventIds[0].toString()),
            MobileAcknowledgementItem("usage-summary", summaryIds[0].toString()),
            MobileAcknowledgementItem("app-metadata", "com.a@1")
        ), MobileIngestResponse(
            batchId = "test",
            acceptedCount = 3,
            itemResults = listOf(
                MobileIngestItemResult(eventIds[0].toString(), "usage-event", "accepted", "0", "ok"),
                MobileIngestItemResult(summaryIds[0].toString(), "usage-summary", "accepted", "0", "ok"),
                MobileIngestItemResult("com.a@1", "app-metadata", "accepted", "0", "ok")
            )
        ))

        assertEquals(1, dao.getUsageEventsBySyncStatus().size)
        assertEquals(eventIds[1], dao.getUsageEventsBySyncStatus().single().id)
        assertEquals(1, dao.getUsageSummariesBySyncStatus().size)
        assertEquals(summaryIds[1], dao.getUsageSummariesBySyncStatus().single().id)
        assertEquals(1, dao.getAppMetadataBySyncStatus().size)
        assertEquals("com.b", dao.getAppMetadataBySyncStatus().single().packageName)
    }

    // --- processUsageAcknowledgements end-to-end ---

    @Test
    fun processUsageAcknowledgementsEndToEnd() = runTest {
        val ids = dao.insertUsageEvents(List(3) { event() })
        val summaryIds = dao.insertUsageSummaries(List(2) { summary() })
        dao.upsertAppMetadata(listOf(appMeta("com.foo"), appMeta("com.bar")))

        val sentItems = linkedSetOf(
            MobileAcknowledgementItem("usage-event", ids[0].toString()),
            MobileAcknowledgementItem("usage-event", ids[1].toString()),
            MobileAcknowledgementItem("usage-event", ids[2].toString()),
            MobileAcknowledgementItem("usage-summary", summaryIds[0].toString()),
            MobileAcknowledgementItem("usage-summary", summaryIds[1].toString()),
            MobileAcknowledgementItem("app-metadata", "com.foo@1"),
            MobileAcknowledgementItem("app-metadata", "com.bar@1")
        )

        val response = MobileIngestResponse(
            batchId = "test",
            acceptedCount = 3,
            skippedCount = 1,
            rejectedCount = 1,
            failedCount = 2,
            itemResults = listOf(
                MobileIngestItemResult(ids[0].toString(), "usage-event", "accepted", "0", "ok"),
                MobileIngestItemResult(ids[1].toString(), "usage-event", "rejected", "INVALID", "bad data"),
                MobileIngestItemResult(ids[2].toString(), "usage-event", "failed", "ERR", "temp failure"),
                MobileIngestItemResult(summaryIds[0].toString(), "usage-summary", "accepted", "0", "ok"),
                MobileIngestItemResult(summaryIds[1].toString(), "usage-summary", "failed", "ERR", "server err"),
                MobileIngestItemResult("com.foo@1", "app-metadata", "accepted", "0", "ok"),
                MobileIngestItemResult("com.bar@1", "app-metadata", "skipped", "0", "duplicate")
            )
        )

        processUsageAcknowledgements(dao, sentItems, response)

        val pendingEvents = dao.getUsageEventsBySyncStatus(MobileSyncStatus.PENDING)
        assertEquals(1, pendingEvents.size)
        assertEquals(ids[2], pendingEvents[0].id)
        assertEquals("ERR: temp failure", pendingEvents[0].lastError)

        val rejectedEvents = dao.getUsageEventsBySyncStatus(MobileSyncStatus.REJECTED)
        assertEquals(1, rejectedEvents.size)
        assertEquals(ids[1], rejectedEvents[0].id)
        assertEquals("INVALID: bad data", rejectedEvents[0].lastError)

        val pendingSummaries = dao.getUsageSummariesBySyncStatus(MobileSyncStatus.PENDING)
        assertEquals(1, pendingSummaries.size)
        assertEquals(summaryIds[1], pendingSummaries[0].id)
        assertEquals("ERR: server err", pendingSummaries[0].lastError)

        assertEquals(0, dao.getAppMetadataBySyncStatus().size)
    }

    // --- Fallback errors when code/message empty ---

    @Test
    fun processUsageAcknowledgementsFallbackErrors() = runTest {
        val ids = dao.insertUsageEvents(List(2) { event() })
        processUsageAcknowledgements(dao, linkedSetOf(
            MobileAcknowledgementItem("usage-event", ids[0].toString()),
            MobileAcknowledgementItem("usage-event", ids[1].toString())
        ), MobileIngestResponse(
            batchId = "test",
            rejectedCount = 1,
            failedCount = 1,
            itemResults = listOf(
                MobileIngestItemResult(ids[0].toString(), "usage-event", "rejected", "", ""),
                MobileIngestItemResult(ids[1].toString(), "usage-event", "failed", "", "")
            )
        ))

        assertEquals("server-rejected", dao.getUsageEventsBySyncStatus(MobileSyncStatus.REJECTED).single().lastError)
        assertEquals("server-retry", dao.getUsageEventsBySyncStatus(MobileSyncStatus.PENDING).single().lastError)
    }

    // --- Partial error info (only code or only message) ---

    @Test
    fun processUsageAcknowledgementsPartialErrors() = runTest {
        val ids = dao.insertUsageEvents(List(2) { event() })
        processUsageAcknowledgements(dao, linkedSetOf(
            MobileAcknowledgementItem("usage-event", ids[0].toString()),
            MobileAcknowledgementItem("usage-event", ids[1].toString())
        ), MobileIngestResponse(
            batchId = "test",
            rejectedCount = 1,
            failedCount = 1,
            itemResults = listOf(
                MobileIngestItemResult(ids[0].toString(), "usage-event", "rejected", "ERR_CODE", ""),
                MobileIngestItemResult(ids[1].toString(), "usage-event", "failed", "", "some msg")
            )
        ))

        assertEquals("ERR_CODE", dao.getUsageEventsBySyncStatus(MobileSyncStatus.REJECTED).single().lastError)
        assertEquals("some msg", dao.getUsageEventsBySyncStatus(MobileSyncStatus.PENDING).single().lastError)
    }

    // --- Cross-type safety: same clientItemKey for different entity types ---

    @Test
    fun processUsageAcknowledgementsCrossTypeSafety() = runTest {
        val eventId = dao.insertUsageEvents(listOf(event())).single()
        val summaryId = dao.insertUsageSummaries(listOf(summary())).single()
        val key = "1"

        processUsageAcknowledgements(dao, linkedSetOf(
            MobileAcknowledgementItem("usage-event", key),
            MobileAcknowledgementItem("usage-summary", key)
        ), MobileIngestResponse(
            batchId = "test",
            acceptedCount = 1,
            rejectedCount = 1,
            itemResults = listOf(
                MobileIngestItemResult(key, "usage-event", "accepted", "0", "ok"),
                MobileIngestItemResult(key, "usage-summary", "rejected", "INVALID", "bad")
            )
        ))

        if (eventId.toString() == key) {
            assertEquals(0, dao.getUsageEventsBySyncStatus().size)
        }

        val rejectedSummaries = dao.getUsageSummariesBySyncStatus(MobileSyncStatus.REJECTED)
        assertNotNull(rejectedSummaries.find { it.id == summaryId })
    }

    // --- Pending counts exclude SYNCED and REJECTED ---

    @Test
    fun pendingCountsExcludeSyncedAndRejected() = runTest {
        dao.insertUsageEvents(listOf(
            event().copy(syncStatus = MobileSyncStatus.SYNCED),
            event().copy(syncStatus = MobileSyncStatus.REJECTED),
            event().copy(syncStatus = MobileSyncStatus.FAILED),
            event()
        ))
        dao.insertUsageSummaries(listOf(
            summary().copy(syncStatus = MobileSyncStatus.SYNCED),
            summary().copy(syncStatus = MobileSyncStatus.REJECTED),
            summary().copy(syncStatus = MobileSyncStatus.FAILED),
            summary()
        ))
        dao.upsertAppMetadata(listOf(
            appMeta("a").copy(syncStatus = MobileSyncStatus.SYNCED),
            appMeta("b").copy(syncStatus = MobileSyncStatus.REJECTED),
            appMeta("c").copy(syncStatus = MobileSyncStatus.FAILED),
            appMeta("d")
        ))
        dao.insertLocationPoints(listOf(
            locPoint().copy(syncStatus = MobileSyncStatus.SYNCED),
            locPoint().copy(syncStatus = MobileSyncStatus.REJECTED),
            locPoint().copy(syncStatus = MobileSyncStatus.FAILED),
            locPoint().copy(syncStatus = MobileSyncStatus.PENDING),
            locPoint().copy(syncStatus = MobileSyncStatus.SYNCING)
        ))

        assertEquals(2, dao.pendingUsageEventCount().first())
        assertEquals(2, dao.pendingUsageSummaryCount().first())
        assertEquals(2, dao.pendingAppMetadataCount().first())
        assertEquals(3, dao.pendingLocationPointCount().first())
    }

    companion object {
        fun event() = MobileUsageEventEntity(
            packageName = "com.test",
            eventType = 1,
            eventName = "TEST",
            eventTimeUtc = 1000,
            source = "test",
            sourceWindowStartUtc = 900,
            sourceWindowEndUtc = 1100,
            collectedAtUtc = 1010,
            rawJson = "{}"
        )

        fun summary() = MobileUsageSummaryEntity(
            packageName = "com.test",
            windowStartUtc = 900,
            windowEndUtc = 1100,
            totalTimeForegroundMs = 100,
            lastTimeUsedUtc = 1000,
            firstTimeStampUtc = 900,
            lastTimeStampUtc = 1100,
            source = "usage_stats",
            sourceWindowStartUtc = 900,
            sourceWindowEndUtc = 1100,
            collectedAtUtc = 1010,
            rawJson = "{}"
        )

        fun appMeta(pkg: String) = MobileAppMetadataEntity(
            packageName = pkg,
            label = "Test",
            versionName = "1.0",
            versionCode = 1,
            firstInstallTimeUtc = 100,
            lastUpdateTimeUtc = 200,
            isSystemApp = false,
            collectedAtUtc = 300,
            rawJson = "{}"
        )

        fun locPoint() = MobileLocationPointEntity(
            latitude = 0.0,
            longitude = 0.0,
            recordedAtUtc = 1000,
            source = "test",
            collectedAtUtc = 1000,
            rawJson = "{}"
        )
    }
}
