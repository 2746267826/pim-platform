package com.pim.app.status

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueStatusRepositoryTest {
    @Test
    fun queueStatusRepositoryExposesPendingCounts() = runTest {
        val locations = MutableStateFlow(3)
        val usageEvents = MutableStateFlow(4)
        val usageSummaries = MutableStateFlow(5)
        val appMetadata = MutableStateFlow(6)
        val deviceProfiles = MutableStateFlow(7)
        val syncBatches = MutableStateFlow(8)

        val repository = QueueStatusRepository(locations, usageEvents, usageSummaries, appMetadata, deviceProfiles, syncBatches)
        val snapshot = repository.observe().first()

        assertEquals(3, snapshot.pendingLocationPoints)
        assertEquals(33, snapshot.pendingUploadTotal)
    }
}
