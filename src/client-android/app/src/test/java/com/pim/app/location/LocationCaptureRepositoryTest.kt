package com.pim.app.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationCaptureRepositoryTest {

    @Test
    fun `enqueueSuccessShowsQueuedMessage`() {
        val msg = formatSubmitStatus(enqueued = true)
        assertEquals("已加入上传队列", msg)
    }

    @Test
    fun `enqueueFailureShowsErrorMessage`() {
        val msg = formatSubmitStatus(enqueued = false, error = "queue full")
        assertEquals("加入上传队列失败：queue full", msg)
    }

    @Test
    fun `enqueueFailureWithNullErrorShowsGenericMessage`() {
        val msg = formatSubmitStatus(enqueued = false, error = null)
        assertEquals("加入上传队列失败：未知错误", msg)
    }

    // --- enqueueThenSchedule contract ---

    @Test
    fun `enqueueThenSchedule success calls both once`() = runTest {
        var enqueueCalls = 0
        var scheduleCalls = 0

        val result = enqueueThenSchedule(
            enqueue = { enqueueCalls++ },
            schedule = { scheduleCalls++ }
        )

        assertTrue(result.isSuccess)
        assertEquals(1, enqueueCalls)
        assertEquals(1, scheduleCalls)
    }

    @Test
    fun `enqueueThenSchedule enqueue failure does not schedule and returns failure`() = runTest {
        var enqueueCalls = 0
        var scheduleCalls = 0

        val result = enqueueThenSchedule(
            enqueue = { enqueueCalls++; throw RuntimeException("db error") },
            schedule = { scheduleCalls++ }
        )

        assertTrue(result.isFailure)
        assertEquals(1, enqueueCalls)
        assertEquals(0, scheduleCalls)
    }

    @Test
    fun `enqueueThenSchedule rethrows CancellationException`() = runTest {
        try {
            enqueueThenSchedule(
                enqueue = { throw CancellationException("cancelled") },
                schedule = { }
            )
            throw AssertionError("Expected CancellationException")
        } catch (ex: CancellationException) {
            // expected
        }
    }

    // --- resolveAutoSubmittedState ---

    @Test
    fun `autoSubmitted manual success stays false`() {
        assertFalse(resolveAutoSubmittedState(current = false, isAutoSubmit = false, success = true))
    }

    @Test
    fun `autoSubmitted auto success becomes true`() {
        assertTrue(resolveAutoSubmittedState(current = false, isAutoSubmit = true, success = true))
    }

    @Test
    fun `autoSubmitted auto failure stays false`() {
        assertFalse(resolveAutoSubmittedState(current = false, isAutoSubmit = true, success = false))
    }

    @Test
    fun `autoSubmitted manual failure stays false`() {
        assertFalse(resolveAutoSubmittedState(current = false, isAutoSubmit = false, success = false))
    }

    @Test
    fun `autoSubmitted already true stays true on auto success`() {
        assertTrue(resolveAutoSubmittedState(current = true, isAutoSubmit = true, success = true))
    }

    @Test
    fun `autoSubmitted already true stays true on manual success`() {
        assertTrue(resolveAutoSubmittedState(current = true, isAutoSubmit = false, success = true))
    }

    @Test
    fun `autoSubmitted already true stays true on failure`() {
        assertTrue(resolveAutoSubmittedState(current = true, isAutoSubmit = true, success = false))
    }
}
