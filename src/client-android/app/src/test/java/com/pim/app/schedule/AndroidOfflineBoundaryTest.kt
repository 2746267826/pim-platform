package com.pim.app.schedule

import com.pim.app.offline.OnlineOperationGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidOfflineBoundaryTest {
    @Test
    fun onlyCollectionUploadsCanQueueOffline() {
        val guard = OnlineOperationGuard()

        assertTrue(guard.canQueueOffline("collection-upload"))
        assertTrue(guard.canQueueOffline("android-location"))
        assertTrue(guard.canQueueOffline("device-state"))
        assertFalse(guard.canQueueOffline("task-fact-change"))
        assertFalse(guard.canQueueOffline("confirmation-decision"))
        assertFalse(guard.canQueueOffline("outlook-writeback"))
        assertFalse(guard.canQueueOffline("restore-delete-operation"))
    }
}
