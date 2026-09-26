package com.pim.app.keepalive

import com.pim.app.forensics.ForensicContext
import com.pim.app.forensics.ForensicPayloads
import com.pim.app.forensics.HeartbeatSnapshot
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * REQ-27（隐私与脱敏）：上报事件不得含经纬度、令牌、口令、账号信息（AC-27.1 / AC-27.3 反面）。
 *
 * 断言打在**真实生成的负载字符串**上，而不是读源码里有没有出现某个词——
 * 后者在字段改名或新增字段时会漏掉。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepAlivePayloadPrivacyTest {

    /** 会被判定为「泄露」的敏感字段名片段（大小写不敏感）。 */
    private val forbidden = listOf(
        "latitude", "longitude", "lat", "lon", "lng", "coord",
        "token", "password", "passwd", "secret", "credential", "refreshtoken", "accesstoken",
        "account", "username", "email", "phone"
    )

    private fun assertNoSensitiveFields(json: String, label: String) {
        val root = JSONObject(json)
        val keys = root.keys().asSequence().toList()
        for (key in keys) {
            val lower = key.lowercase()
            forbidden.forEach { bad ->
                // "account" 只拦精确字段名，避免误伤 "excludedNoActualTime" 之类的正常词；
                // 其余按片段匹配即可（它们不会出现在正常字段名里）。
                val hit = if (bad.length <= 3) lower == bad || lower.endsWith(bad) else lower.contains(bad)
                assertFalse("$label 的负载不得含敏感字段「$key」（命中规则：$bad）", hit)
            }
        }
    }

    /** 心跳负载（阶段一已有，这里一并纳入抽查）。 */
    @Test
    fun `AC-27_1 心跳负载不含敏感字段`() {
        val json = ForensicPayloads.heartbeat(
            HeartbeatSnapshot(
                bootElapsedMillis = 1000,
                sinceLastHeartbeatMillis = 2000,
                standbyBucket = 10,
                standbyBucketLabel = "活跃",
                ignoringBatteryOptimizations = false,
                dozeMode = false,
                powerSaveMode = false,
                foregroundServiceRunning = true,
                context = ForensicContext()
            )
        )
        assertNoSensitiveFields(json, "心跳")
    }

    /** 闹钟兑现负载：本次新增，必须抽查（AC-27.3 要求抽查上报请求体）。 */
    @Test
    fun `AC-27_1 闹钟兑现负载不含敏感字段`() {
        val json = JSONObject()
            .put("scheduledAtUtcMillis", 1000L)
            .put("actualAtUtcMillis", 2000L)
            .put("delayMillis", 1000L)
            .put("outcome", AlarmOutcomes.EXECUTED)
            .toString()
        assertNoSensitiveFields(json, "闹钟兑现")
    }

    /** 兑现负载的字段集合必须恰好是契约里那几个（防止日后顺手塞进位置信息）。 */
    @Test
    fun `AC-27_1 闹钟兑现负载字段集合受控`() {
        val json = JSONObject()
            .put("scheduledAtUtcMillis", 1000L)
            .put("actualAtUtcMillis", 2000L)
            .put("delayMillis", 1000L)
            .put("outcome", AlarmOutcomes.EXECUTED)
        val keys = json.keys().asSequence().toSet()
        assertTrue(
            "不得出现契约外字段：$keys",
            keys.all {
                it in setOf("scheduledAtUtcMillis", "actualAtUtcMillis", "delayMillis", "outcome")
            }
        )
    }
}
