package com.pim.app.recovery

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundRecoveryObserverTest {

    @Test
    fun `observer recovers once for each started entry`() = runTest {
        val owner = TestLifecycleOwner()
        var recoveryCount = 0
        owner.registry.addObserver(
            ForegroundRecoveryObserver(backgroundScope) {
                recoveryCount++
            }
        )

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        runCurrent()
        assertEquals(1, recoveryCount)

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        runCurrent()
        assertEquals(2, recoveryCount)
    }

    @Test
    fun `sync enqueue failure is reported without blocking recovery`() = runTest {
        val owner = TestLifecycleOwner()
        val expected = IllegalStateException("work manager unavailable")
        var reported: Exception? = null
        var recoveryCount = 0
        owner.registry.addObserver(
            ForegroundRecoveryObserver(
                scope = backgroundScope,
                enqueueImmediateSync = { throw expected },
                reportSyncFailure = { reported = it },
                recover = { recoveryCount++ }
            )
        )

        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        runCurrent()

        assertSame(expected, reported)
        assertEquals(1, recoveryCount)
    }

    @Test
    fun `sync enqueue cancellation propagates without starting recovery`() = runTest {
        val owner = TestLifecycleOwner()
        var recoveryCount = 0
        var cancelled = false
        owner.registry.addObserver(
            ForegroundRecoveryObserver(
                scope = backgroundScope,
                enqueueImmediateSync = { throw CancellationException("cancelled") },
                recover = { recoveryCount++ }
            )
        )

        try {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(0, recoveryCount)
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle = registry
    }
}
