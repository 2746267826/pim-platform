package com.pim.app.status

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusCenterRepositoryFlowTest {

    @Test
    fun activeImmediateWorkClearsAcceptedAfterAcceptedWasObserved() = runTest {
        val signal = StatusAcceptedSignal()
        signal.trigger()

        val collectedSignalDuringEmission = mutableListOf<Boolean>()

        flowOf(
            StatusEmission(
                state = StatusCenterState.empty().copy(syncPhase = SyncPhase.Accepted),
                clearAcceptedAfterEmission = true
            )
        ).emitStates(signal::clearIfSet)
            .onEach { collectedSignalDuringEmission.add(signal.accepted.value) }
            .first()

        assertEquals(1, collectedSignalDuringEmission.size)
        assertTrue("signal must be true during onEach before clear", collectedSignalDuringEmission[0])
        assertFalse("signal must be false after emitStates clears", signal.accepted.value)
    }

}
