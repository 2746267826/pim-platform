package com.pim.app.mobile.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSyncBatchIdTest {

    private val deviceId = "android-a5b98c2e27c8c280"
    private val windowStart = "2026-08-30T18:10:05.901Z"
    private val windowEnd = "2026-08-30T22:10:05.901Z"

    @Test
    fun sameWindowAndItemsProduceIdenticalBatchIdForRetryIdempotency() {
        val items = listOf(
            MobileAcknowledgementItem("usage-event", "101"),
            MobileAcknowledgementItem("usage-summary", "201"),
            MobileAcknowledgementItem("app-metadata", "com.example.app@1")
        )

        val batchId1 = stableBatchId(deviceId, windowStart, windowEnd, items)
        val batchId2 = stableBatchId(deviceId, windowStart, windowEnd, items)

        assertEquals(batchId1, batchId2)
        assertTrue(batchId1.startsWith("android-"))
        assertEquals(8 + 24, batchId1.length)
    }

    @Test
    fun itemsOrderDoesNotAffectBatchId() {
        val item1 = MobileAcknowledgementItem("usage-event", "101")
        val item2 = MobileAcknowledgementItem("usage-event", "102")
        val item3 = MobileAcknowledgementItem("usage-summary", "201")

        val batchId1 = stableBatchId(deviceId, windowStart, windowEnd, listOf(item1, item2, item3))
        val batchId2 = stableBatchId(deviceId, windowStart, windowEnd, listOf(item3, item1, item2))

        assertEquals(batchId1, batchId2)
    }

    @Test
    fun differentItemsInSameWindowProduceDistinctBatchIdsAvoidingReplayLoop() {
        // Issue #224 reproduction scenario:
        // Batch 1 has 500 items (e.g. IDs 1..500)
        // Batch 2 has different items (e.g. remaining IDs 501..600 or different subset after rejections)
        val batch1Items = (1..500).map { id ->
            MobileAcknowledgementItem("usage-summary", id.toString())
        }
        val batch2Items = (501..600).map { id ->
            MobileAcknowledgementItem("usage-summary", id.toString())
        }

        val batchId1 = stableBatchId(deviceId, windowStart, windowEnd, batch1Items)
        val batchId2 = stableBatchId(deviceId, windowStart, windowEnd, batch2Items)

        assertNotEquals(
            "Distinct batches in the same window must have distinct batchIds so the server does not replay old results",
            batchId1,
            batchId2
        )
    }

    @Test
    fun emptyItemsAndNonEmptyItemsProduceDistinctBatchIds() {
        val emptyBatchId = stableBatchId(deviceId, windowStart, windowEnd, emptyList())
        val nonEmptyBatchId = stableBatchId(
            deviceId,
            windowStart,
            windowEnd,
            listOf(MobileAcknowledgementItem("usage-event", "1"))
        )

        assertNotEquals(emptyBatchId, nonEmptyBatchId)
    }

    @Test
    fun differentWindowsProduceDistinctBatchIds() {
        val items = listOf(MobileAcknowledgementItem("usage-event", "1"))
        val id1 = stableBatchId(deviceId, windowStart, windowEnd, items)
        val id2 = stableBatchId(deviceId, windowStart, "2026-08-30T23:10:05.901Z", items)

        assertNotEquals(id1, id2)
    }

    @Test
    fun differentDevicesProduceDistinctBatchIds() {
        val items = listOf(MobileAcknowledgementItem("usage-event", "1"))
        val id1 = stableBatchId(deviceId, windowStart, windowEnd, items)
        val id2 = stableBatchId("android-different-device", windowStart, windowEnd, items)

        assertNotEquals(id1, id2)
    }
}
