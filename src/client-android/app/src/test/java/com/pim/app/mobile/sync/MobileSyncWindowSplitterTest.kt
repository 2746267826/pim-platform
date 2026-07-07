package com.pim.app.mobile.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class MobileSyncWindowSplitterTest {
    @Test
    fun splitGapWindowUsesTwoHourUploadWindows() {
        val start = 1_000L
        val windows = splitGapWindowForUpload(
            windowStartUtc = start,
            windowEndUtc = start + 5L * 60L * 60L * 1000L + 30L * 60L * 1000L
        )

        assertEquals(
            listOf(
                UploadWindow(start, start + 2L * 60L * 60L * 1000L),
                UploadWindow(start + 2L * 60L * 60L * 1000L, start + 4L * 60L * 60L * 1000L),
                UploadWindow(
                    start + 4L * 60L * 60L * 1000L,
                    start + 5L * 60L * 60L * 1000L + 30L * 60L * 1000L
                )
            ),
            windows
        )
    }

    @Test
    fun splitGapWindowReturnsSingleWindowWhenAlreadySmall() {
        val start = 1_000L
        val end = start + 45L * 60L * 1000L

        assertEquals(
            listOf(UploadWindow(start, end)),
            splitGapWindowForUpload(start, end)
        )
    }

    @Test
    fun splitGapWindowReturnsEmptyListForInvalidRange() {
        assertEquals(emptyList<UploadWindow>(), splitGapWindowForUpload(2_000L, 2_000L))
        assertEquals(emptyList<UploadWindow>(), splitGapWindowForUpload(3_000L, 2_000L))
    }
}
