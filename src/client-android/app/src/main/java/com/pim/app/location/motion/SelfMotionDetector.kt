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
    private val onSignal: (MotionSignal) -> Unit = {},
    sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
) : SensorEventListener {

    private val sensorManager = sensorManager

    private var lastSignal = MotionSignal.Unknown
    private var started = false

    private val triggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            if (event != null) {
                evaluator.significantMotionTriggered()
                notifyIfChanged()
            }
            rearmSignificantMotion()
        }
    }

    /**
     * 幂等启动：重复调用不会重置检测状态（服务循环每分钟级调用一次）。
     * 只有 stop() 之后的重新 start() 才会 [SelfMotionEvaluator.reset]。
     *
     * 传感器注册不做 started 短路：SensorManager 对同一 (listener, sensor)
     * 重复注册是幂等的（内部去重，仅更新 delay），而步数/重大运动传感器在
     * 权限缺失时注册会失败（SecurityException 被吞掉）；若注册被短路挡住，
     * 权限恢复后下一轮 register 将永远不再尝试注册，步数检测直到服务重启
     * 才能恢复。因此每次 start() 都重新尝试注册全部传感器。
     */
    fun start() {
        val freshStart = !started
        started = true
        if (freshStart) {
            evaluator.reset()
            lastSignal = MotionSignal.Unknown
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // Android 10+ 上步数传感器需要 ACTIVITY_RECOGNITION 权限，无权限时
        // registerListener 直接抛 SecurityException：与重大运动传感器一致，
        // 权限被拒时优雅降级（仅加速度计仍工作），权限恢复后由下一轮
        // start() 重新尝试注册。
        runCatching {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        rearmSignificantMotion()
    }

    fun stop() {
        if (!started) return
        started = false
        sensorManager.unregisterListener(this)
        runCatching {
            sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let { sensor ->
                sensorManager.cancelTriggerSensor(triggerListener, sensor)
            }
        }
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
