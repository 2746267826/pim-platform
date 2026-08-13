package com.pim.app.location.motion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class SelfMotionDetectorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class CountingEvaluator : SelfMotionEvaluator(nowElapsedRealtimeMillis = { 0L }) {
        var resetCount = 0
        override fun reset() {
            resetCount++
        }
    }

    @Test
    fun `repeated start does not reset evaluator state`() {
        val evaluator = CountingEvaluator()
        val detector = SelfMotionDetector(context, evaluator) {}

        detector.start()
        detector.start()
        detector.start()

        assertEquals("idempotent start must reset exactly once", 1, evaluator.resetCount)
    }

    @Test
    fun `start after stop resets evaluator state`() {
        val evaluator = CountingEvaluator()
        val detector = SelfMotionDetector(context, evaluator) {}

        detector.start()
        detector.stop()
        detector.start()

        assertEquals("stop/start cycle must reset for a fresh detection session", 2, evaluator.resetCount)
    }

    @Test
    fun `stop without start is a no-op`() {
        val evaluator = CountingEvaluator()
        val detector = SelfMotionDetector(context, evaluator) {}

        detector.stop()
        detector.stop()

        assertEquals(0, evaluator.resetCount)
    }

    @Test
    fun `start does not throw when sensors are unavailable`() {
        val detector = SelfMotionDetector(context, CountingEvaluator()) {}

        detector.start()
        detector.stop()
    }
}
