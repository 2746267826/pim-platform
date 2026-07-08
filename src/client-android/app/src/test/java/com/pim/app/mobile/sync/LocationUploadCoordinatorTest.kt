package com.pim.app.mobile.sync

import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUploadCoordinatorTest {
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
            errorMessage = "network"
        )

        val updates = LocationUploadPlanner.planStatusUpdates(result)

        assertEquals(true, updates.shouldRetry)
    }

    @Test
    fun syncWorkerRetriesPartialFailures() {
        val updates = LocationUploadStatusUpdates(
            syncedIds = emptyList(),
            failedIds = listOf(9L),
            failedReason = "network",
            shouldRetry = true
        )

        val result = LocationSyncWorkResultPlanner.fromUpdates(updates)

        assertEquals(ListenableWorker.Result.retry().javaClass, result.javaClass)
    }

    @Test
    fun syncWorkerRetriesTransientExceptions() {
        val result = LocationSyncWorkResultPlanner.fromTransientFailure()

        assertEquals(ListenableWorker.Result.retry().javaClass, result.javaClass)
    }
}
