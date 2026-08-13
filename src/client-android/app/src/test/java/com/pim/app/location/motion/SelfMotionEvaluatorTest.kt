package com.pim.app.location.motion

import com.pim.app.location.policy.MotionSignal
import org.junit.Assert.assertEquals
import org.junit.Test

class SelfMotionEvaluatorTest {
    private var nowMillis = 0L
    private val evaluator = SelfMotionEvaluator(
        nowElapsedRealtimeMillis = { nowMillis }
    )

    private fun sampleRaw(delta: Double, count: Int = 60) {
        // 生成 count 个模长样本：一半 9.8-delta、一半 9.8+delta，
        // 标准差 = delta（均值 9.8）。delta=0.1→STILL、0.5→SHAKING、1.5→MOVING。
        repeat(count / 2) { evaluator.accelMagnitude(9.8 - delta) }
        repeat(count / 2) { evaluator.accelMagnitude(9.8 + delta) }
    }

    private fun moveToMoving() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        nowMillis = 3_000L
        sampleRaw(1.5) // MOVING，streak 3s
        nowMillis = 6_000L
        sampleRaw(1.5) // MOVING，streak 6s ≥ 5s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `initial signal is Unknown`() {
        assertEquals(MotionSignal.Unknown, evaluator.currentSignal())
    }

    @Test
    fun `first still window evaluates to Still`() {
        nowMillis = 0L
        sampleRaw(0.1)

        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `shaking alone does not leave Still`() {
        nowMillis = 0L
        sampleRaw(0.5) // SHAKING

        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `moving transition needs five seconds of sustained motion`() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        assertEquals(MotionSignal.Still, evaluator.currentSignal())

        nowMillis = 3_000L
        sampleRaw(1.5) // MOVING 窗口1 → 3s < 5s
        assertEquals(MotionSignal.Still, evaluator.currentSignal())

        nowMillis = 6_000L
        sampleRaw(1.5) // MOVING 窗口2 → 6s ≥ 5s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `brief moving burst under five seconds does not leave Still`() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        nowMillis = 3_000L
        sampleRaw(1.5) // 仅 3s 的运动波动
        assertEquals(MotionSignal.Still, evaluator.currentSignal())

        nowMillis = 6_000L
        sampleRaw(0.1) // 恢复静止，movingStreak 清零
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `still transition needs twenty seconds of stillness`() {
        moveToMoving()

        repeat(6) { index ->
            nowMillis = 9_000L + index * 3_000L
            sampleRaw(0.1) // 第 1..6 个 STILL 窗口 → 18s < 20s
            assertEquals(MotionSignal.Moving, evaluator.currentSignal())
        }

        nowMillis = 27_000L
        sampleRaw(0.1) // 第 7 个 STILL 窗口 → 21s ≥ 20s
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `shaking counts toward the moving streak but not alone`() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        nowMillis = 3_000L
        sampleRaw(0.5) // SHAKING → streak 3s
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
        nowMillis = 6_000L
        sampleRaw(0.5) // SHAKING → streak 6s ≥ 5s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `significant motion trigger accelerates the moving transition`() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        evaluator.significantMotionTriggered() // 计为一个 3s MOVING 窗口
        assertEquals(MotionSignal.Still, evaluator.currentSignal())

        nowMillis = 3_000L
        sampleRaw(1.5) // 再一个窗口 → 累计 6s ≥ 5s
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `moving without steps stays Moving`() {
        moveToMoving()

        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `walking signal requires step increments while moving`() {
        moveToMoving()

        evaluator.stepCount(1_000L) // 仅基线，不算增量
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())

        evaluator.stepCount(1_004L) // 4 步
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())

        evaluator.stepCount(1_012L) // 再 8 步
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())
    }

    @Test
    fun `step increments before entering moving are not attributed to the moving episode`() {
        nowMillis = 0L
        sampleRaw(0.1) // STILL
        evaluator.stepCount(500L) // 静止时基线
        evaluator.stepCount(512L) // 静止时走路（尚未进入 MOVING）
        assertEquals(MotionSignal.Still, evaluator.currentSignal())

        nowMillis = 3_000L
        sampleRaw(1.5)
        nowMillis = 6_000L
        sampleRaw(1.5) // 进入 MOVING，episode 清零
        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }

    @Test
    fun `signal stays Still when sensor readings are constant`() {
        nowMillis = 0L
        repeat(3) { index ->
            nowMillis = index * 3_000L
            sampleRaw(0.1)
            assertEquals(MotionSignal.Still, evaluator.currentSignal())
        }
    }

    @Test
    fun `reset clears all accumulated state back to Unknown`() {
        moveToMoving()
        evaluator.stepCount(1_000L)
        evaluator.stepCount(1_003L)
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())

        evaluator.reset()

        assertEquals(MotionSignal.Unknown, evaluator.currentSignal())
        // 重启后首个窗口不再计入停机前的 streak（直接按新 3s 窗口计算）
        nowMillis = 0L
        sampleRaw(1.5)
        assertEquals(MotionSignal.Still, evaluator.currentSignal())
    }

    @Test
    fun `walking stays sticky for the moving episode after steps`() {
        // 设计取舍固化：episode 内累计过步数后，只要防抖态仍是 MOVING 就保持
        // Walking（§3.4 映射规则），静止判定需要 20s 连续的 STILL 窗口。
        moveToMoving()
        evaluator.stepCount(1_000L)
        evaluator.stepCount(1_002L)
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())

        // 轻微的桌面振动（SHAKING）不清零 episode，也不会结束 MOVING 段
        nowMillis = 9_000L
        sampleRaw(0.5)
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())
    }

    @Test
    fun `step counter reset rebases the baseline without counting lost steps`() {
        moveToMoving()
        evaluator.stepCount(1_000L)
        evaluator.stepCount(1_004L)
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())

        // 系统重置计数器到 100（模拟设备重启后计数归零）
        evaluator.stepCount(100L)
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())
        evaluator.stepCount(103L) // 新基线上增量 3 步
        assertEquals(MotionSignal.Walking, evaluator.currentSignal())
    }

    @Test
    fun `significant motion trigger is a no-op while already moving`() {
        moveToMoving()

        evaluator.significantMotionTriggered()

        assertEquals(MotionSignal.Moving, evaluator.currentSignal())
    }
}
