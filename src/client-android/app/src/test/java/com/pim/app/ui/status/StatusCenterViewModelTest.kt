package com.pim.app.ui.status

import com.pim.app.mobile.diagnostics.DiagnosticExportException
import com.pim.app.mobile.diagnostics.DiagnosticExportResult
import com.pim.app.status.StatusAcceptedSignal
import com.pim.app.status.StatusActionRoute
import com.pim.app.status.StatusSyncActionRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        val signal = StatusAcceptedSignal()
        var syncCallCount = 0
        var refreshCount = 0
        val runner = StatusSyncActionRunner(
            syncNow = { syncCallCount++ },
            refresh = { refreshCount++ },
            acceptedSignal = signal
        )

        runner.run(StatusActionRoute.TriggerSync)

        assertEquals("syncNow runs exactly once", 1, syncCallCount)
        assertEquals("refresh runs exactly once", 1, refreshCount)
        assertTrue("accepted is published after syncNow succeeds", signal.state.value.isAccepted)
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

    @Test
    fun exportHelperSuccessWithoutCoordinates() = runTest {
        val file = File("/tmp/test.zip")
        val states = mutableListOf<ExportFeedbackState>()

        runExportWithFeedback(
            includeRecentLocations = false,
            export = { DiagnosticExportResult(file, 0) },
            onState = { states.add(it) }
        )

        assertEquals(2, states.size)
        assertEquals(ExportFeedbackState(isExporting = true), states[0])
        assertEquals(
            ExportFeedbackState(
                isExporting = false,
                feedback = DiagnosticExportFeedback.PackageReady,
                exportedFile = file,
                coordinateCount = 0
            ),
            states[1]
        )
    }

    @Test
    fun exportHelperSuccessWithCoordinates() = runTest {
        val file = File("/tmp/test.zip")
        val states = mutableListOf<ExportFeedbackState>()

        runExportWithFeedback(
            includeRecentLocations = true,
            export = { DiagnosticExportResult(file, 5) },
            onState = { states.add(it) }
        )

        assertEquals(2, states.size)
        assertEquals(ExportFeedbackState(isExporting = true), states[0])
        assertEquals(
            ExportFeedbackState(
                isExporting = false,
                feedback = DiagnosticExportFeedback.PackageReadyWithLocations,
                exportedFile = file,
                coordinateCount = 5
            ),
            states[1]
        )
    }

    @Test
    fun exportHelperInsufficientStorage() = runTest {
        val states = mutableListOf<ExportFeedbackState>()

        runExportWithFeedback(
            includeRecentLocations = false,
            export = { throw DiagnosticExportException("INSUFFICIENT_STORAGE", "no space") },
            onState = { states.add(it) }
        )

        assertEquals(2, states.size)
        assertEquals(ExportFeedbackState(isExporting = true), states[0])
        assertEquals(
            ExportFeedbackState(
                isExporting = false,
                feedback = DiagnosticExportFeedback.InsufficientStorage
            ),
            states[1]
        )
    }

    @Test
    fun exportHelperGeneralException() = runTest {
        val states = mutableListOf<ExportFeedbackState>()

        runExportWithFeedback(
            includeRecentLocations = false,
            export = { throw RuntimeException("generic failure") },
            onState = { states.add(it) }
        )

        assertEquals(2, states.size)
        assertEquals(ExportFeedbackState(isExporting = true), states[0])
        assertEquals(
            ExportFeedbackState(
                isExporting = false,
                feedback = DiagnosticExportFeedback.ExportFailed
            ),
            states[1]
        )
    }

    @Test
    fun exportHelperCancellationExceptionRethrows() = runTest {
        val states = mutableListOf<ExportFeedbackState>()

        val result = kotlin.runCatching {
            runExportWithFeedback(
                includeRecentLocations = false,
                export = { throw CancellationException() },
                onState = { states.add(it) }
            )
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(1, states.size)
        assertEquals(ExportFeedbackState(isExporting = true), states[0])
    }

    @Test
    fun beginExportPreventsDuplicateAndClearsPreviousResult() {
        val previousFile = File("/tmp/previous.zip")
        val started = beginDiagnosticExport(
            DiagnosticExportUiState(
                includeRecentLocations = true,
                showLocationConfirmation = true,
                exportedFile = previousFile,
                coordinateCount = 10,
                feedback = DiagnosticExportFeedback.PackageReadyWithLocations
            )
        )

        assertEquals(
            DiagnosticExportUiState(
                includeRecentLocations = true,
                isExporting = true
            ),
            started
        )
        assertEquals(
            null,
            beginDiagnosticExport(DiagnosticExportUiState(isExporting = true))
        )
    }
}
