package com.pim.app.forensics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-3 / REQ-4 的负载契约（AC-4.1 / AC-4.2 / AC-27.1 / AC-27.3）。
 *
 * 负载是唯一会离开设备的取证数据，因此这里逐条检查：读不到的字段留空并标注不可用、
 * 不含经纬度、不含令牌/口令/账号字段。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForensicPayloadsTest {

    private val fullContext = ForensicContext(
        screenOn = true,
        unlocked = false,
        foregroundPackage = "com.tencent.mm",
        foregroundAppLabel = "微信",
        charging = true,
        batteryPercent = 88
    )

    private val emptyContext = ForensicContext(
        unavailableFields = listOf("屏幕状态", "解锁状态", "前台应用", "充电状态", "电量百分比")
    )

    @Test
    fun `heartbeat payload carries every field required by REQ-3`() {
        val snapshot = HeartbeatSnapshot(
            bootElapsedMillis = 1_234_567L,
            sinceLastHeartbeatMillis = 900_000L,
            standbyBucket = AndroidHeartbeatSnapshotReader.STANDBY_BUCKET_RARE,
            standbyBucketLabel = "罕见",
            ignoringBatteryOptimizations = false,
            dozeMode = true,
            powerSaveMode = false,
            foregroundServiceRunning = true,
            context = fullContext
        )

        val json = JSONObject(ForensicPayloads.heartbeat(snapshot))

        assertEquals(1_234_567L, json.getLong("bootElapsedMs"))
        assertEquals(900_000L, json.getLong("sinceLastHeartbeatMs"))
        assertEquals(40, json.getInt("standbyBucket"))
        assertEquals("罕见", json.getString("standbyBucketLabel"))
        assertFalse(json.getBoolean("ignoringBatteryOptimizations"))
        assertTrue(json.getBoolean("dozeMode"))
        assertFalse(json.getBoolean("powerSaveMode"))
        assertTrue(json.getBoolean("foregroundServiceRunning"))
    }

    @Test
    fun `AC-4_1 context fields follow the real device state`() {
        val snapshot = HeartbeatSnapshot(
            bootElapsedMillis = 1L,
            sinceLastHeartbeatMillis = null,
            standbyBucket = null,
            standbyBucketLabel = "未知",
            ignoringBatteryOptimizations = null,
            dozeMode = null,
            powerSaveMode = null,
            foregroundServiceRunning = false,
            context = fullContext
        )

        val json = JSONObject(ForensicPayloads.heartbeat(snapshot))

        assertTrue(json.getBoolean("screenOn"))
        assertFalse(json.getBoolean("unlocked"))
        assertEquals("com.tencent.mm", json.getString("foregroundPackage"))
        assertEquals("微信", json.getString("foregroundAppLabel"))
        assertTrue(json.getBoolean("charging"))
        assertEquals(88, json.getInt("batteryPercent"))
        assertFalse(json.has("unavailableFields"))

        val dark = JSONObject(
            ForensicPayloads.heartbeat(snapshot.copy(context = fullContext.copy(screenOn = false, charging = false)))
        )
        assertFalse(dark.getBoolean("screenOn"))
        assertFalse(dark.getBoolean("charging"))
    }

    @Test
    fun `AC-4_2 unreadable fields stay empty and are marked unavailable`() {
        val snapshot = HeartbeatSnapshot(
            bootElapsedMillis = 1L,
            sinceLastHeartbeatMillis = null,
            standbyBucket = null,
            standbyBucketLabel = "未知",
            ignoringBatteryOptimizations = null,
            dozeMode = null,
            powerSaveMode = null,
            foregroundServiceRunning = false,
            context = emptyContext
        )

        val json = JSONObject(ForensicPayloads.heartbeat(snapshot))

        assertTrue(json.isNull("screenOn"))
        assertTrue(json.isNull("unlocked"))
        assertTrue(json.isNull("foregroundPackage"))
        assertTrue(json.isNull("batteryPercent"))
        assertTrue(json.getJSONArray("unavailableFields").length() == 5)
        // 不允许填猜测值。
        assertFalse(json.has("lastKnownScreenOn"))
    }

    @Test
    fun `AC-4_2 foreground app is recorded as package and label only`() {
        val json = JSONObject(
            ForensicPayloads.heartbeat(
                HeartbeatSnapshot(
                    bootElapsedMillis = 1L,
                    sinceLastHeartbeatMillis = null,
                    standbyBucket = null,
                    standbyBucketLabel = "未知",
                    ignoringBatteryOptimizations = null,
                    dozeMode = null,
                    powerSaveMode = null,
                    foregroundServiceRunning = true,
                    context = fullContext
                )
            )
        )

        // 只有包名与应用名，没有任何应用使用内容字段。
        assertTrue(json.has("foregroundPackage"))
        assertTrue(json.has("foregroundAppLabel"))
        for (forbidden in listOf("activityName", "windowTitle", "screenTitle", "text", "content", "url")) {
            assertFalse("foreground context must not contain $forbidden", json.has(forbidden))
        }
    }

    @Test
    fun `AC-27_1 no payload contains coordinates tokens or credentials`() {
        val payloads = listOf(
            ForensicPayloads.heartbeat(
                HeartbeatSnapshot(
                    bootElapsedMillis = 1L,
                    sinceLastHeartbeatMillis = null,
                    standbyBucket = null,
                    standbyBucketLabel = "未知",
                    ignoringBatteryOptimizations = null,
                    dozeMode = null,
                    powerSaveMode = null,
                    foregroundServiceRunning = true,
                    context = fullContext
                )
            ),
            ForensicPayloads.processExit(
                reason = ProcessExitReasons.LOW_MEMORY,
                occurredAtMillis = 1L,
                importance = 100,
                pssKb = 1L,
                rssKb = 2L,
                description = "system description",
                inference = null,
                context = fullContext
            ),
            ForensicPayloads.forceStop(
                kind = ForceStopKinds.FORCE_STOP,
                evidence = "sentinel-missing",
                inference = "哨兵（周期同步作业）已消失。",
                context = fullContext
            )
        )

        for (payload in payloads) {
            val json = JSONObject(payload)
            for (forbidden in listOf(
                "latitude", "longitude", "lat", "lon", "location",
                "token", "accessToken", "refreshToken", "password", "secret",
                "account", "email", "username"
            )) {
                assertFalse("payload must not contain $forbidden: $payload", json.has(forbidden))
            }
        }
    }

    @Test
    fun `process exit payload omits an empty system description instead of guessing one`() {
        val json = JSONObject(
            ForensicPayloads.processExit(
                reason = ProcessExitReasons.UNKNOWN,
                occurredAtMillis = 42L,
                importance = null,
                pssKb = null,
                rssKb = null,
                description = "   ",
                inference = "系统未提供该次退出的原因。",
                context = emptyContext
            )
        )

        assertFalse(json.has("description"))
        assertEquals("系统未提供该次退出的原因。", json.getString("inference"))
        assertTrue(json.isNull("importance"))
        assertTrue(json.isNull("pssKb"))
    }

    @Test
    fun `force stop payload records kind and evidence for later review`() {
        val json = JSONObject(
            ForensicPayloads.forceStop(
                kind = ForceStopKinds.REBOOT,
                evidence = "boot-elapsed-decreased",
                inference = null,
                context = emptyContext
            )
        )

        assertEquals("reboot", json.getString("kind"))
        assertEquals("boot-elapsed-decreased", json.getString("evidence"))
        assertFalse(json.has("inference"))
    }

    @Test
    fun `api reason mapping covers the documented ApplicationExitInfo constants`() {
        assertEquals(ProcessExitReasons.UNKNOWN, ProcessExitReasons.fromApiReason(0))
        assertEquals(ProcessExitReasons.EXIT_SELF, ProcessExitReasons.fromApiReason(1))
        assertEquals(ProcessExitReasons.SIGNALED, ProcessExitReasons.fromApiReason(2))
        assertEquals(ProcessExitReasons.LOW_MEMORY, ProcessExitReasons.fromApiReason(3))
        assertEquals(ProcessExitReasons.CRASH, ProcessExitReasons.fromApiReason(4))
        assertEquals(ProcessExitReasons.CRASH_NATIVE, ProcessExitReasons.fromApiReason(5))
        assertEquals(ProcessExitReasons.ANR, ProcessExitReasons.fromApiReason(6))
        assertEquals(ProcessExitReasons.INITIALIZATION_FAILURE, ProcessExitReasons.fromApiReason(7))
        assertEquals(ProcessExitReasons.PERMISSION_CHANGE, ProcessExitReasons.fromApiReason(8))
        assertEquals(ProcessExitReasons.EXCESSIVE_RESOURCE_USAGE, ProcessExitReasons.fromApiReason(9))
        assertEquals(ProcessExitReasons.USER_REQUESTED, ProcessExitReasons.fromApiReason(10))
        assertEquals(ProcessExitReasons.USER_STOPPED, ProcessExitReasons.fromApiReason(11))
        assertEquals(ProcessExitReasons.DEPENDENCY_DIED, ProcessExitReasons.fromApiReason(12))
        assertEquals(ProcessExitReasons.OTHER, ProcessExitReasons.fromApiReason(13))
        assertEquals(ProcessExitReasons.FREEZER, ProcessExitReasons.fromApiReason(14))
        assertEquals(ProcessExitReasons.PACKAGE_STATE_CHANGE, ProcessExitReasons.fromApiReason(15))
        assertEquals(ProcessExitReasons.PACKAGE_UPDATED, ProcessExitReasons.fromApiReason(16))
        // 未来新增的取值一律落到"未知"，不得猜成某个具体原因。
        assertEquals(ProcessExitReasons.UNKNOWN, ProcessExitReasons.fromApiReason(99))
    }

    @Test
    fun `standby bucket labels are Chinese and unknown buckets are not guessed`() {
        val reader = AndroidHeartbeatSnapshotReader(
            androidx.test.core.app.ApplicationProvider.getApplicationContext()
        )

        assertEquals("活跃", reader.standbyBucketLabel(10))
        assertEquals("工作集", reader.standbyBucketLabel(20))
        assertEquals("常用", reader.standbyBucketLabel(30))
        assertEquals("罕见", reader.standbyBucketLabel(40))
        assertEquals("受限", reader.standbyBucketLabel(45))
        assertEquals("未知", reader.standbyBucketLabel(null))
        assertEquals("未知", reader.standbyBucketLabel(999))
    }

    @Test
    fun `exit reason labels are Simplified Chinese`() {
        assertEquals("内存不足被系统回收", ExitReasonLabels.labelOf(ProcessExitReasons.LOW_MEMORY))
        assertEquals("用户强停", ExitReasonLabels.labelOf(ProcessExitReasons.USER_REQUESTED))
        assertEquals("未知（系统未提供退出记录）", ExitReasonLabels.labelOf(ProcessExitReasons.NO_RECORD))
        assertEquals("系统未给出原因", ExitReasonLabels.labelOf("REASON_SOMETHING_NEW"))
    }
}
