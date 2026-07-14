package com.pim.app.ui.status

import com.pim.app.status.StatusAcceptedSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusCenterViewModelTest {

    @Test
    fun manualProbeSuccessShowsCheckingThenCompleted() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        runProbeWithFeedback(
            probe = {},
            feedbackSetter = { feedback.add(it) }
        )

        assertEquals(
            listOf(StatusActionFeedback.ProbeChecking, StatusActionFeedback.ProbeCompleted),
            feedback
        )
    }

    @Test
    fun probeExceptionShowsCheckingThenFailed() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        runProbeWithFeedback(
            probe = { throw RuntimeException("probe failed") },
            feedbackSetter = { feedback.add(it) }
        )

        assertEquals(
            listOf(StatusActionFeedback.ProbeChecking, StatusActionFeedback.ProbeFailed),
            feedback
        )
    }

    @Test
    fun syncSubmitExceptionClearsOldAndSetsSyncSubmitFailed() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        runSyncWithFeedback(
            sync = { throw RuntimeException("enqueue failed") },
            feedbackSetter = { feedback.add(it) }
        )

        assertEquals(
            listOf(null, StatusActionFeedback.SyncSubmitFailed),
            feedback
        )
    }

    @Test
    fun syncSubmitSuccessDoesNotLeaveSyncSubmitFailed() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        runSyncWithFeedback(
            sync = { /* success */ },
            feedbackSetter = { feedback.add(it) }
        )

        assertEquals(listOf(null), feedback)
    }

    @Test
    fun syncCancellationExceptionIsNotConvertedToFeedback() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        val result = kotlin.runCatching {
            runSyncWithFeedback(
                sync = { throw CancellationException() },
                feedbackSetter = { feedback.add(it) }
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(listOf(null), feedback)
    }

    @Test
    fun syncDoesNotDuplicateAcceptedOrRefresh() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()
        val signal = StatusAcceptedSignal()
        var syncCallCount = 0
        var refreshCount = 0

        runSyncWithFeedback(
            sync = {
                syncCallCount++
                signal.trigger()
                refreshCount++
            },
            feedbackSetter = { feedback.add(it) }
        )

        assertEquals("sync body runs exactly once", 1, syncCallCount)
        assertEquals("refresh called exactly once by sync runner", 1, refreshCount)
        assertTrue("accepted triggered by sync runner", signal.accepted.value)
        assertEquals(listOf(null), feedback)
    }

    @Test
    fun probeCancellationExceptionIsNotConvertedToFailed() = runTest {
        val feedback = mutableListOf<StatusActionFeedback?>()

        val result = kotlin.runCatching {
            runProbeWithFeedback(
                probe = { throw CancellationException() },
                feedbackSetter = { feedback.add(it) }
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(
            listOf(StatusActionFeedback.ProbeChecking),
            feedback
        )
    }
}
