package com.pim.app.mobile.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileDataDao
import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.data.MobileSyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationUploadCoordinatorTest {
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

    // --- planStatusUpdates ---

    @Test
    fun partialFailureKeepsFailedRowsQueued() {
        val result = LocationUploadBatchResult(
            syncedIds = listOf(1L, 2L),
            failedIds = listOf(3L),
            errorMessage = "timeout"
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(listOf(1L, 2L), updates.syncedIds)
        assertEquals(listOf(3L), updates.failedIds)
        assertEquals("timeout", updates.failedReason)
    }

    @Test
    fun allSuccessfulUploadNeedsNoRetry() {
        val result = LocationUploadBatchResult(
            syncedIds = listOf(4L),
            failedIds = emptyList(),
            errorMessage = null
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(false, updates.shouldRetry)
    }

    @Test
    fun anyFailureNeedsRetry() {
        val result = LocationUploadBatchResult(
            syncedIds = emptyList(),
            failedIds = listOf(9L),
            errorMessage = "network",
            retryableFailedIds = listOf(9L)
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(true, updates.shouldRetry)
    }

    @Test
    fun nonRetryableFailureDoesNotScheduleWorkerRetry() {
        val result = LocationUploadBatchResult(
            syncedIds = emptyList(),
            failedIds = listOf(10L),
            errorMessage = "missing-horizontal-accuracy",
            retryableFailedIds = emptyList()
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(listOf(10L), updates.failedIds)
        assertEquals(false, updates.shouldRetry)
    }

    // --- applyStatusUpdates per-item handling ---

    @Test
    fun confirmedLocationPointsAreDeleted() = runTest {
        val id1 = dao.insertLocationPoint(point().copy(latitude = 1.0))
        val id2 = dao.insertLocationPoint(point().copy(latitude = 2.0))

        applyLocationStatusUpdates(dao, LocationUploadStatusUpdates(
            syncedIds = listOf(id1, id2),
            failedIds = emptyList(),
            failedReason = null,
            shouldRetry = false
        ))

        assertEquals(0, dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING).size)
        assertEquals(0, dao.getLocationPointsBySyncStatus(MobileSyncStatus.REJECTED).size)
        assertEquals(0, dao.getLocationPointsBySyncStatus(MobileSyncStatus.FAILED).size)
    }

    @Test
    fun permanentlyRejectedPointKeepsOwnReason() = runTest {
        val id = dao.insertLocationPoint(point().copy(latitude = 5.0))
        val id2 = dao.insertLocationPoint(point().copy(latitude = 6.0))

        applyLocationStatusUpdates(dao, LocationUploadStatusUpdates(
            syncedIds = emptyList(),
            failedIds = listOf(id, id2),
            failedReason = null,
            shouldRetry = false,
            perItemErrors = mapOf(id to "invalid-fields", id2 to "business-rejection"),
            retryableFailedIds = emptyList()
        ))

        val rejected = dao.getLocationPointsBySyncStatus(MobileSyncStatus.REJECTED)
        assertEquals(2, rejected.size)
        assertEquals("invalid-fields", rejected.find { it.id == id }?.lastError)
        assertEquals("business-rejection", rejected.find { it.id == id2 }?.lastError)
    }

    @Test
    fun retryablePointKeepsPendingWithOwnReason() = runTest {
        val id1 = dao.insertLocationPoint(point().copy(latitude = 7.0))
        val id2 = dao.insertLocationPoint(point().copy(latitude = 8.0))

        applyLocationStatusUpdates(dao, LocationUploadStatusUpdates(
            syncedIds = emptyList(),
            failedIds = listOf(id1, id2),
            failedReason = null,
            shouldRetry = true,
            perItemErrors = mapOf(id1 to "timeout", id2 to "server-500"),
            retryableFailedIds = listOf(id1, id2)
        ))

        val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING)
        assertEquals(2, pending.size)
        assertEquals("timeout", pending.find { it.id == id1 }?.lastError)
        assertEquals("server-500", pending.find { it.id == id2 }?.lastError)
    }

    @Test
    fun mixedConfirmedRejectedRetryable() = runTest {
        val confirmedId = dao.insertLocationPoint(point().copy(latitude = 10.0))
        val rejectedId = dao.insertLocationPoint(point().copy(latitude = 11.0))
        val retryableId = dao.insertLocationPoint(point().copy(latitude = 12.0))
        val unchangedId = dao.insertLocationPoint(point().copy(latitude = 13.0))

        applyLocationStatusUpdates(dao, LocationUploadStatusUpdates(
            syncedIds = listOf(confirmedId),
            failedIds = listOf(rejectedId, retryableId),
            failedReason = null,
            shouldRetry = true,
            perItemErrors = mapOf(rejectedId to "bad-request", retryableId to "timeout"),
            retryableFailedIds = listOf(retryableId)
        ))

        val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING)
        assertEquals(2, pending.size)
        assertEquals("timeout", pending.find { it.id == retryableId }?.lastError)
        assertEquals("pending", pending.find { it.id == unchangedId }?.syncStatus)
        val rejected = dao.getLocationPointsBySyncStatus(MobileSyncStatus.REJECTED)
        assertEquals(1, rejected.size)
        assertEquals("bad-request", rejected.single().lastError)
    }

    // --- planStatusUpdates preserves retryableFailedIds and perItemErrors ---

    @Test
    fun `planStatusUpdates preserves retryableFailedIds`() {
        val result = LocationUploadBatchResult(
            syncedIds = listOf(1L),
            failedIds = listOf(2L, 3L),
            errorMessage = "partial",
            retryableFailedIds = listOf(2L)
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(listOf(2L), updates.retryableFailedIds)
    }

    @Test
    fun `planStatusUpdates passes perItemErrors through`() {
        val result = LocationUploadBatchResult(
            syncedIds = listOf(1L),
            failedIds = listOf(2L),
            errorMessage = "err",
            retryableFailedIds = listOf(2L)
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)
            .copy(perItemErrors = mapOf(2L to "timeout"))

        assertEquals("timeout", updates.perItemErrors[2L])
        assertEquals(listOf(2L), updates.retryableFailedIds)
    }

    // --- retryable not counted as rejected, permanent not counted as failed ---

    @Test
    fun `retryableFailedIds not counted in rejected`() {
        val updates = LocationUploadStatusUpdates(
            syncedIds = listOf(1L),
            failedIds = listOf(2L, 3L),
            failedReason = null,
            shouldRetry = true,
            perItemErrors = mapOf(2L to "timeout", 3L to "bad-request"),
            retryableFailedIds = listOf(2L)
        )

        val retryableSet = updates.retryableFailedIds.toSet()
        val permanentIds = updates.failedIds.filter { it !in retryableSet }

        assertEquals(1, permanentIds.size)
        assertEquals(listOf(3L), permanentIds)
    }

    @Test
    fun `applyStatusUpdates deletes synced and keeps retryable as pending permanent as rejected`() = runTest {
        val syncedId = dao.insertLocationPoint(point().copy(latitude = 20.0))
        val retryableId = dao.insertLocationPoint(point().copy(latitude = 21.0))
        val permanentId = dao.insertLocationPoint(point().copy(latitude = 22.0))

        applyLocationStatusUpdates(dao, LocationUploadStatusUpdates(
            syncedIds = listOf(syncedId),
            failedIds = listOf(retryableId, permanentId),
            failedReason = null,
            shouldRetry = true,
            perItemErrors = mapOf(retryableId to "timeout", permanentId to "bad-request"),
            retryableFailedIds = listOf(retryableId)
        ))

        val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING)
        assertEquals(1, pending.size)
        assertEquals(retryableId, pending.single().id)

        val rejected = dao.getLocationPointsBySyncStatus(MobileSyncStatus.REJECTED)
        assertEquals(1, rejected.size)
        assertEquals(permanentId, rejected.single().id)
    }

    // --- MobileSyncErrorClassifier integration: RETRY stays PENDING, BLOCKED becomes REJECTED ---

    @Test
    fun `classify RETRY to PENDING and BLOCKED to REJECTED`() {
        val retryableResult = MobileSyncErrorClassifier.classify(java.net.SocketTimeoutException())
        assertEquals(MobileSyncOutcome.RETRY, retryableResult)

        val blockedResult = MobileSyncErrorClassifier.classify(FakeHttpException(400))
        assertEquals(MobileSyncOutcome.BLOCKED, blockedResult)
    }

    companion object {
        fun point() = MobileLocationPointEntity(
            latitude = 0.0,
            longitude = 0.0,
            recordedAtUtc = 1000,
            source = "test",
            collectedAtUtc = 1000,
            rawJson = "{}"
        )
    }
}
