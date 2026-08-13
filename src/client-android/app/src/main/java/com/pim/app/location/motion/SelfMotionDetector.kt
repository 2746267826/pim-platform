package com.pim.app.location.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.SystemClock
import com.pim.app.location.policy.MotionSignal
import kotlin.math.sqrt

/**
 * 自研运动检测传感器薄包装：把加速度计/步数/重大运动传感器事件喂给
 * [SelfMotionEvaluator]，信号变化时通过 [onSignal] 通知。不依赖 GMS。
 *
 * - 加速度计 SENSOR_DELAY_GAME（≈20Hz），60 样本 ≈3s 一个窗口
 * - 步数传感器记录全局累计值，由 evaluator 记基线算增量
 * - 重大运动一次性触发，触发后重新 requestTriggerSensor
 * - 传感器缺失或权限不足时静默降级（加速度计缺失则信号停留在 Unknown）
 */
class SelfMotionDetector(
    context: Context,
    private val evaluator: SelfMotionEvaluator = SelfMotionEvaluator(
        nowElapsedRealtimeMillis = { SystemClock.elapsedRealtime() }
    ),
    private val onSignal: (MotionSignal) -> Unit = {}
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var lastSignal = MotionSignal.Unknown

    private val triggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            if (event != null) {
                evaluator.significantMotionTriggered()
                notifyIfChanged()
            }
            rearmSignificantMotion()
        }
    }

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        rearmSignificantMotion()
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                evaluator.accelMagnitude(sqrt(x * x + y * y + z * z))
                notifyIfChanged()
            }
            Sensor.TYPE_STEP_COUNTER -> {
                evaluator.stepCount(event.values[0].toLong())
                notifyIfChanged()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun rearmSignificantMotion() {
        runCatching {
            sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let {
                sensorManager.requestTriggerSensor(triggerListener, it)
            }
        }
    }

    private fun notifyIfChanged() {
        val signal = evaluator.currentSignal()
        if (signal != lastSignal) {
            lastSignal = signal
            onSignal(signal)
        }
    }
}
