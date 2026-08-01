package com.pim.app.location

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pim.app.location.acquisition.LocationAcquisitionCoordinator
import com.pim.app.location.acquisition.LocationAcquisitionState
import com.pim.app.location.acquisition.SessionStartResult
import com.pim.app.location.service.ForegroundLocationController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

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

    @Test
    fun `request failure state ends capturing`() {
        val state = applyLocationRequestFailure(
            current = LocationCaptureState(
                isCapturing = true,
                statusMessage = "正在等待位置更新...",
                inlineReason = null
            ),
            errorMessage = "gms down"
        )
        assertFalse(state.isCapturing)
        assertEquals("定位请求失败", state.statusMessage)
        assertEquals("gms down", state.inlineReason)
    }

    @Test
    fun `request failure state uses fallback message when error null`() {
        val state = applyLocationRequestFailure(
            current = LocationCaptureState(isCapturing = true),
            errorMessage = null
        )
        assertFalse(state.isCapturing)
        assertEquals("定位请求失败", state.statusMessage)
        assertEquals("未知错误", state.inlineReason)
    }

    @Test
    fun `fresh seed location is usable`() {
        val now = 1_700_000_000_000L
        assertTrue(isUsableSeedLocation(locationTimeMillis = now - 30_000L, nowMillis = now))
    }

    @Test
    fun `stale seed location is rejected`() {
        val now = 1_700_000_000_000L
        assertFalse(
            isUsableSeedLocation(
                locationTimeMillis = now - SEED_LOCATION_MAX_AGE_MILLIS - 1L,
                nowMillis = now
            )
        )
    }

    @Test
    fun `seed location with non positive time is rejected`() {
        assertFalse(isUsableSeedLocation(locationTimeMillis = 0L, nowMillis = 1_700_000_000_000L))
        assertFalse(isUsableSeedLocation(locationTimeMillis = -1L, nowMillis = 1_700_000_000_000L))
    }

    @Test
    fun `seed location in the future is rejected`() {
        val now = 1_700_000_000_000L
        assertFalse(isUsableSeedLocation(locationTimeMillis = now + 60_000L, nowMillis = now))
    }

    @Test
    fun startCaptureSendsStartManualSessionIntent() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val coordinator = realCoordinator()
        val controller = ForegroundLocationController(context)
        val repo = LocationCaptureRepository(coordinator, controller)
        // Drain any startup intents
        while (shadowOf(context).nextStartedService != null) { }

        repo.startCapture()

        val intent = shadowOf(context).nextStartedService
        assertEquals(ForegroundLocationController.ACTION_START_MANUAL_SESSION, intent?.action)
    }

    @Test
    fun repositorySourceDoesNotCallCoordinatorStartOrCancel() {
        val relativePath = "app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt"
        val source = sequenceOf(
            java.io.File(relativePath),
            java.io.File(relativePath.removePrefix("app/")),
            java.io.File("..", relativePath)
        ).firstOrNull { it.isFile }?.readText()
            ?: error("source not found for $relativePath")
        assertFalse(
            "startCapture must not call coordinator.startManualSession directly",
            source.contains("coordinator.startManualSession")
        )
        assertFalse(
            "stopCapture must not call coordinator.cancelCurrentSession directly",
            source.contains("coordinator.cancelCurrentSession")
        )
        assertTrue(
            "startCapture must delegate to controller.startManualSession",
            source.contains("controller.startManualSession")
        )
        assertTrue(
            "stopCapture must delegate to controller.cancelLocationSession",
            source.contains("controller.cancelLocationSession")
        )
    }

    @Test
    fun submitManualDelegatesToCoordinator() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val coordinator = realCoordinator()
        val controller = ForegroundLocationController(context)
        val repo = LocationCaptureRepository(coordinator, controller)

        repo.submitCurrentLocationManually()

        // No assertions on results - just verifies no crash
        // (full verification needs coordinator test infrastructure)
    }

    private fun realCoordinator(): LocationAcquisitionCoordinator {
        // Use a simple real coordinator for basic compile/structural tests.
        // The full coordinator behavior is tested in LocationAcquisitionCoordinatorTest.
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return LocationAcquisitionCoordinator(
            runner = object : com.pim.app.location.acquisition.LocationAcquisitionRunner {
                override suspend fun acquire(
                    request: com.pim.app.location.acquisition.LocationEngineRequest,
                    onCandidate: suspend (com.pim.app.location.LocationSnapshot) -> Unit,
                    onAvailabilityChanged: suspend (Boolean) -> Unit
                ): com.pim.app.location.acquisition.LocationEngineResult {
                    return com.pim.app.location.acquisition.LocationEngineResult(
                        sessionId = request.sessionId,
                        bestLocation = null,
                        completion = com.pim.app.location.acquisition.LocationEngineCompletion.TimedOut
                    )
                }
            },
            prerequisiteChecker = object : com.pim.app.location.acquisition.LocationPrerequisiteChecker {
                override fun check(triggerType: com.pim.app.location.acquisition.TriggerType) =
                    com.pim.app.location.acquisition.LocationPrerequisiteResult.Ready
            },
            operations = object : com.pim.app.location.acquisition.LocationAcquisitionOperations {
                override suspend fun enqueueAccepted(
                    accepted: com.pim.app.location.quality.QualityAcceptedLocation,
                    rawJson: String,
                    source: String
                ) {}
                override suspend fun recordDropped(fix: com.pim.app.location.quality.RawLocationFix, reason: String) {}
                override fun scheduleSync() {}
            },
            json = json,
            trackingSettingsStore = com.pim.app.settings.TrackingSettingsStore(
                com.pim.app.testing.InMemorySharedPreferences()
            )
        )
    }
}
