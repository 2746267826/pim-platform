package com.pim.app.location.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import com.pim.app.TestPimApp
import org.robolectric.Shadows.shadowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        val detector = SelfMotionDetector(context, evaluator)

        detector.start()
        detector.start()
        detector.start()

        assertEquals("idempotent start must reset exactly once", 1, evaluator.resetCount)
    }

    @Test
    fun `start after stop resets evaluator state`() {
        val evaluator = CountingEvaluator()
        val detector = SelfMotionDetector(context, evaluator)

        detector.start()
        detector.stop()
        detector.start()

        assertEquals("stop/start cycle must reset for a fresh detection session", 2, evaluator.resetCount)
    }

    @Test
    fun `stop without start is a no-op`() {
        val evaluator = CountingEvaluator()
        val detector = SelfMotionDetector(context, evaluator)

        detector.stop()
        detector.stop()

        assertEquals(0, evaluator.resetCount)
    }

    @Test
    fun `start does not throw when sensors are unavailable`() {
        val detector = SelfMotionDetector(context, CountingEvaluator())

        detector.start()
        detector.stop()
    }

    @Test
    fun `every start re-attempts sensor registration without resetting`() {
        val evaluator = CountingEvaluator()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadow = shadowOf(sensorManager)
        shadow.addSensor(newSensor(Sensor.TYPE_ACCELEROMETER))
        shadow.addSensor(newSensor(Sensor.TYPE_STEP_COUNTER))
        val detector = SelfMotionDetector(context, evaluator)

        detector.start()
        detector.start()
        detector.start()

        assertEquals("repeated start must not reset", 1, evaluator.resetCount)
        assertTrue(
            "listeners must be (re)registered on every start",
            shadow.hasListener(detector, shadow.getSensorList(Sensor.TYPE_STEP_COUNTER).single())
        )
    }

    @Test
    fun `start after failed registration re-attempts step counter once permission restored`() {
        val evaluator = CountingEvaluator()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val shadow = shadowOf(sensorManager)
        val stepSensor = newSensor(Sensor.TYPE_STEP_COUNTER)
        shadow.addSensor(newSensor(Sensor.TYPE_ACCELEROMETER))
        shadow.addSensor(stepSensor)
        val detector = SelfMotionDetector(context, evaluator)

        // 权限缺失：注册全部失败（SecurityException 等价路径），无监听注册
        shadow.forceListenersToFail = true
        detector.start()
        assertTrue(shadow.getListeners().isEmpty())
        assertEquals(1, evaluator.resetCount)

        // 权限恢复：下一轮 register 必须重新尝试注册（不能被 started 短路），
        // 且不重置检测状态
        shadow.forceListenersToFail = false
        detector.start()
        assertEquals(1, evaluator.resetCount)
        assertTrue(
            "step counter must be re-registered after permission restore",
            shadow.hasListener(detector, stepSensor)
        )
    }

    private fun newSensor(type: Int): Sensor {
        val sensor = Sensor::class.java.getDeclaredConstructor().apply { isAccessible = true }
            .newInstance()
        val typeField = Sensor::class.java.getDeclaredField("mType")
        typeField.isAccessible = true
        typeField.set(sensor, type)
        return sensor
    }
}
