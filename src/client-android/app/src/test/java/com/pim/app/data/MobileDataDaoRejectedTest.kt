package com.pim.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MobileDataDaoRejectedTest {
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

    @Test
    fun pendingSyncBatchCountExcludesRejected() = runTest {
        dao.insertSyncBatch(batch(syncStatus = MobileSyncStatus.PENDING))
        dao.insertSyncBatch(batch(syncStatus = MobileSyncStatus.SYNCED))
        dao.insertSyncBatch(batch(syncStatus = MobileSyncStatus.REJECTED))
        dao.insertSyncBatch(batch(syncStatus = MobileSyncStatus.REJECTED))

        val count = dao.pendingSyncBatchCount().first()
        assertEquals(1, count)
    }

    @Test
    fun pendingDeviceProfileCountExcludesRejected() = runTest {
        dao.upsertDeviceProfile(profile(profileId = "p1", syncStatus = MobileSyncStatus.PENDING))
        dao.upsertDeviceProfile(profile(profileId = "p2", syncStatus = MobileSyncStatus.SYNCED))
        dao.upsertDeviceProfile(profile(profileId = "p3", syncStatus = MobileSyncStatus.REJECTED))

        val count = dao.pendingDeviceProfileCount().first()
        assertEquals(1, count)
    }

    @Test
    fun aggregateRejectedCountReturnsZeroWhenEmpty() = runTest {
        val count = dao.aggregateRejectedCount().first()
        assertEquals(0, count)
    }

    @Test
    fun aggregateRejectedCountIncludesAllBusinessTables() = runTest {
        dao.insertUsageEvents(listOf(event(syncStatus = MobileSyncStatus.REJECTED)))
        dao.insertUsageSummaries(listOf(summary(syncStatus = MobileSyncStatus.REJECTED)))
        dao.upsertAppMetadata(listOf(meta(syncStatus = MobileSyncStatus.REJECTED)))
        dao.insertLocationPoint(point(syncStatus = MobileSyncStatus.REJECTED))
        dao.insertSyncBatch(batch(syncStatus = MobileSyncStatus.REJECTED))
        dao.upsertDeviceProfile(profile(profileId = "p1", syncStatus = MobileSyncStatus.REJECTED))

        val count = dao.aggregateRejectedCount().first()
        assertEquals(6, count)
    }

    @Test
    fun aggregateRejectedCountExcludesMobileLogs() = runTest {
        dao.insertLogs(listOf(
            log(syncStatus = MobileSyncStatus.REJECTED),
            log(syncStatus = MobileSyncStatus.REJECTED)
        ))
        val count = dao.aggregateRejectedCount().first()
        assertEquals(0, count)
    }

    @Test
    fun aggregateRejectedCountIgnoresNonRejectedStatuses() = runTest {
        dao.insertUsageEvents(listOf(
            event(syncStatus = MobileSyncStatus.PENDING),
            event(syncStatus = MobileSyncStatus.SYNCED),
            event(syncStatus = MobileSyncStatus.FAILED)
        ))
        dao.insertUsageSummaries(listOf(summary(syncStatus = MobileSyncStatus.SYNCED)))

        val count = dao.aggregateRejectedCount().first()
        assertEquals(0, count)
    }

    private fun event(syncStatus: String = MobileSyncStatus.PENDING) = MobileUsageEventEntity(
        packageName = "com.test",
        eventType = 1,
        eventName = "test",
        eventTimeUtc = 1000L,
        source = "test",
        sourceWindowStartUtc = 0L,
        sourceWindowEndUtc = 2000L,
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )

    private fun summary(syncStatus: String = MobileSyncStatus.PENDING) = MobileUsageSummaryEntity(
        packageName = "com.test",
        windowStartUtc = 1000L,
        windowEndUtc = 2000L,
        totalTimeForegroundMs = 500L,
        lastTimeUsedUtc = 1500L,
        firstTimeStampUtc = 1000L,
        lastTimeStampUtc = 1500L,
        source = "test",
        sourceWindowStartUtc = 0L,
        sourceWindowEndUtc = 0L,
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )

    private fun meta(syncStatus: String = MobileSyncStatus.PENDING) = MobileAppMetadataEntity(
        packageName = "com.test.${java.util.UUID.randomUUID()}",
        label = "Test",
        versionCode = 1,
        firstInstallTimeUtc = 0L,
        lastUpdateTimeUtc = 0L,
        isSystemApp = false,
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )

    private fun point(syncStatus: String = MobileSyncStatus.PENDING) = MobileLocationPointEntity(
        latitude = 0.0,
        longitude = 0.0,
        recordedAtUtc = 1000L,
        source = "test",
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )

    private fun batch(syncStatus: String = MobileSyncStatus.PENDING) = MobileSyncBatchEntity(
        batchId = java.util.UUID.randomUUID().toString(),
        entityType = "test",
        rowCount = 1,
        syncStatus = syncStatus
    )

    private fun profile(
        profileId: String = "default",
        syncStatus: String = MobileSyncStatus.PENDING
    ) = MobileDeviceProfileEntity(
        profileId = profileId,
        deviceId = "test-device",
        manufacturer = "Test",
        brand = "Test",
        model = "Test",
        hardware = "Test",
        androidVersion = "14",
        sdkInt = 34,
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )

    private fun log(syncStatus: String = MobileSyncStatus.PENDING) = MobileLogEntity(
        level = "INFO",
        message = "test",
        occurredAtUtc = 1000L,
        source = "test",
        collectedAtUtc = 1000L,
        rawJson = "{}",
        syncStatus = syncStatus
    )
}
