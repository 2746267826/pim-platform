package com.pim.app.ui.status

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionProbePollingTest {
    @Test
    fun pollingRunsOnlyWhileLifecycleIsStarted() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val owner = TestLifecycleOwner()
            var refreshCount = 0
            val polling = backgroundScope.launch {
                owner.lifecycle.repeatConnectionProbePolling {
                    refreshCount++
                    1_000L
                }
            }

            runCurrent()
            assertEquals(0, refreshCount)

            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertEquals(1, refreshCount)

            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(2, refreshCount)

            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            runCurrent()
            advanceTimeBy(5_000L)
            runCurrent()
            assertEquals(2, refreshCount)

            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertEquals(3, refreshCount)

            polling.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle = registry
    }
}
