package com.pim.app.status

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatusAcceptedSignalTest {
    @Test
    fun acceptedSignalClearsAfterFallbackTimeout() = runTest {
        val signal = StatusAcceptedSignal(this, timeoutMillis = 10_000L)

        signal.trigger()
        advanceTimeBy(9_999L)
        runCurrent()
        assertTrue(signal.state.value.isAccepted)

        advanceTimeBy(1L)
        runCurrent()
        assertFalse(signal.state.value.isAccepted)
    }

    @Test
    fun olderFallbackCannotClearNewerAcceptedGeneration() = runTest {
        val signal = StatusAcceptedSignal(this, timeoutMillis = 10_000L)

        val firstGeneration = signal.trigger()
        advanceTimeBy(5_000L)
        val secondGeneration = signal.trigger()

        advanceTimeBy(5_000L)
        runCurrent()
        assertTrue(signal.state.value.isAccepted)
        assertEquals(secondGeneration, signal.state.value.generation)

        signal.clearIfGeneration(firstGeneration)
        assertTrue(signal.state.value.isAccepted)

        advanceTimeBy(5_000L)
        runCurrent()
        assertFalse(signal.state.value.isAccepted)
    }
}
