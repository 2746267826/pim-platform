package com.pim.app.status

import com.pim.core.auth.AuthMode
import com.pim.core.settings.PimServerEndpoints
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ConnectionProbeService(
    private val anonymousClient: OkHttpClient,
    private val authenticatedClient: OkHttpClient,
    private val tokenSource: ProbeTokenSource,
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
    private val monotonicNanos: () -> Long = System::nanoTime
) : ConnectionProbe {
    override suspend fun probe(serverUrl: String): ConnectionProbeResult {
        val progress = ProbeProgress(checkedAtUtcMillis = wallClockMillis())
        val urlStartedAt = monotonicNanos()
        val endpoints = try {
            PimServerEndpoints.from(serverUrl)
        } catch (_: IllegalArgumentException) {
            progress.recordLatency(ConnectionProbeStage.Url, urlStartedAt)
            return progress.result(
                outcome = ConnectionProbeOutcome.Blocked,
                failureKind = ConnectionFailureKind.InvalidUrl,
                safeMessage = "API 地址无效"
            )
        }
        progress.serverIdentity = endpoints.apiBaseUrl.toString()
        progress.recordLatency(ConnectionProbeStage.Url, urlStartedAt)
        progress.complete(ConnectionProbeStage.Url)

        execute(anonymousClient, anonymousRequest(endpoints.healthUrl), ConnectionProbeStage.Health, progress)
            .consume(
                onFailure = { failure -> return progress.blocked(failure) },
                onResponse = { response ->
                    httpFailure(response, optional = false)?.let { failure ->
                        return progress.blocked(failure)
                    }
                    progress.complete(ConnectionProbeStage.Health)
                }
            )

        var versionDocument: VersionDocument? = null
        execute(anonymousClient, anonymousRequest(endpoints.versionUrl), ConnectionProbeStage.Version, progress)
            .consume(
                onFailure = { failure -> return progress.blocked(failure) },
                onResponse = { response ->
                    httpFailure(response, optional = false)?.let { failure ->
                        return progress.blocked(failure)
                    }
                    versionDocument = runCatching {
                        JSON.decodeFromString<VersionDocument>(response.peekBody(MAX_RESPONSE_BYTES).string())
                    }.getOrNull()
                    if (versionDocument == null) {
                        return progress.result(
                            outcome = ConnectionProbeOutcome.Blocked,
                            failureKind = ConnectionFailureKind.IncompatibleVersion,
                            safeMessage = "服务器版本响应不兼容"
                        )
                    }
                    progress.complete(ConnectionProbeStage.Version)
                }
            )

        val declaredCapabilities = versionDocument!!.capabilities.toSet()
        val capabilities = ServerCapabilities(
            mobileItemResultsV1 = MOBILE_ITEM_RESULTS_V1 in declaredCapabilities,
            androidEmbedV1 = ANDROID_EMBED_V1 in declaredCapabilities
        )
        progress.capabilities = capabilities
        if (!capabilities.mobileItemResultsV1) {
            return progress.result(
                outcome = ConnectionProbeOutcome.Blocked,
                failureKind = ConnectionFailureKind.IncompatibleVersion,
                safeMessage = "服务器缺少移动端逐项确认能力"
            )
        }

        if (!tokenSource.currentAccessToken(serverUrl).isNullOrBlank()) {
            execute(
                authenticatedClient,
                requiredRequest(endpoints.statusSummaryUrl),
                ConnectionProbeStage.AuthenticatedStatus,
                progress
            ).consume(
                onFailure = { failure -> return progress.blocked(failure) },
                onResponse = { response ->
                    httpFailure(response, optional = false)?.let { failure ->
                        return progress.blocked(failure)
                    }
                    progress.complete(ConnectionProbeStage.AuthenticatedStatus)
                }
            )
        }

        execute(anonymousClient, anonymousRequest(endpoints.webOrigin), ConnectionProbeStage.WebRoot, progress)
            .consume(
                onFailure = { failure -> return progress.partial(failure) },
                onResponse = { response ->
                    httpFailure(response, optional = true)?.let { failure ->
                        return progress.partial(failure)
                    }
                    if (!response.isUsableHtmlBootstrap()) {
                        return progress.result(
                            outcome = ConnectionProbeOutcome.Partial,
                            failureKind = ConnectionFailureKind.Http,
                            httpStatus = response.code,
                            safeMessage = "Web 根页面无法使用"
                        )
                    }
                    progress.complete(ConnectionProbeStage.WebRoot)
                }
            )

        execute(
            anonymousClient,
            anonymousRequest(endpoints.todayEmbedUrl),
            ConnectionProbeStage.EmbedBootstrap,
            progress
        ).consume(
            onFailure = { failure -> return progress.partial(failure) },
            onResponse = { response ->
                httpFailure(response, optional = true)?.let { failure ->
                    return progress.partial(failure)
                }
                if (!response.isUsableHtmlBootstrap()) {
                    return progress.result(
                        outcome = ConnectionProbeOutcome.Partial,
                        failureKind = ConnectionFailureKind.Http,
                        httpStatus = response.code,
                        safeMessage = "Android 嵌入页面无法使用"
                    )
                }
                progress.complete(ConnectionProbeStage.EmbedBootstrap)
            }
        )

        return if (capabilities.androidEmbedV1) {
            progress.result(outcome = ConnectionProbeOutcome.Reachable)
        } else {
            progress.result(
                outcome = ConnectionProbeOutcome.Partial,
                failureKind = ConnectionFailureKind.IncompatibleVersion,
                safeMessage = "服务器尚未声明 Android 嵌入页面能力"
            )
        }
    }

    private suspend fun execute(
        client: OkHttpClient,
        request: Request,
        stage: ConnectionProbeStage,
        progress: ProbeProgress
    ): StageAttempt {
        val startedAt = monotonicNanos()
        return try {
            val response = client.newCall(request).awaitCancellableResponse()
            progress.recordLatency(stage, startedAt)
            StageAttempt.Completed(response)
        } catch (failure: IOException) {
            progress.recordLatency(stage, startedAt)
            StageAttempt.Failed(transportFailure(failure))
        }
    }

    private fun anonymousRequest(url: HttpUrl): Request {
        return Request.Builder()
            .url(url)
            .tag(AuthMode::class.java, AuthMode.Anonymous)
            .get()
            .build()
    }

    private fun requiredRequest(url: HttpUrl): Request {
        return Request.Builder()
            .url(url)
            .tag(AuthMode::class.java, AuthMode.Required)
            .get()
            .build()
    }

    private fun httpFailure(response: Response, optional: Boolean): ProbeFailure? {
        if (response.isSuccessful) return null
        val kind = when (response.code) {
            401 -> ConnectionFailureKind.Unauthorized
            404 -> ConnectionFailureKind.WrongPath
            else -> ConnectionFailureKind.Http
        }
        val message = when (kind) {
            ConnectionFailureKind.Unauthorized -> "服务器拒绝了登录状态"
            ConnectionFailureKind.WrongPath -> if (optional) "Web 页面路径不存在" else "API 路径不存在"
            else -> "服务器返回了 HTTP 错误"
        }
        return ProbeFailure(kind, response.code, message)
    }

    private fun transportFailure(failure: IOException): ProbeFailure {
        val kind = when (failure) {
            is UnknownHostException -> ConnectionFailureKind.Dns
            is SocketTimeoutException -> ConnectionFailureKind.Timeout
            is SSLException -> ConnectionFailureKind.Tls
            is ConnectException -> ConnectionFailureKind.Connect
            else -> ConnectionFailureKind.Connect
        }
        val message = when (kind) {
            ConnectionFailureKind.Dns -> "无法解析服务器地址"
            ConnectionFailureKind.Timeout -> "连接服务器超时"
            ConnectionFailureKind.Tls -> "TLS 安全连接失败"
            else -> "无法连接服务器"
        }
        return ProbeFailure(kind, httpStatus = null, safeMessage = message)
    }

    private fun Response.isUsableHtmlBootstrap(): Boolean {
        val contentType = header("Content-Type").orEmpty()
        if (!contentType.startsWith("text/html", ignoreCase = true)) return false
        val html = peekBody(MAX_RESPONSE_BYTES).string()
        return html.contains("<html", ignoreCase = true) && ROOT_MARKER.containsMatchIn(html)
    }

    private inline fun StageAttempt.consume(
        onFailure: (ProbeFailure) -> Unit,
        onResponse: (Response) -> Unit
    ) {
        when (this) {
            is StageAttempt.Failed -> onFailure(failure)
            is StageAttempt.Completed -> response.use(onResponse)
        }
    }

    private sealed interface StageAttempt {
        data class Completed(val response: Response) : StageAttempt
        data class Failed(val failure: ProbeFailure) : StageAttempt
    }

    private data class ProbeFailure(
        val kind: ConnectionFailureKind,
        val httpStatus: Int?,
        val safeMessage: String
    )

    private inner class ProbeProgress(
        private val checkedAtUtcMillis: Long
    ) {
        private val latencyMillisByStage = linkedMapOf<ConnectionProbeStage, Long>()
        private var lastCompletedStage: ConnectionProbeStage? = null
        var serverIdentity: String? = null
        var capabilities: ServerCapabilities = ServerCapabilities(false, false)

        fun recordLatency(stage: ConnectionProbeStage, startedAt: Long) {
            val elapsedNanos = (monotonicNanos() - startedAt).coerceAtLeast(0L)
            latencyMillisByStage[stage] = elapsedNanos / NANOS_PER_MILLISECOND
        }

        fun complete(stage: ConnectionProbeStage) {
            lastCompletedStage = stage
        }

        fun blocked(failure: ProbeFailure): ConnectionProbeResult {
            return result(
                outcome = ConnectionProbeOutcome.Blocked,
                failureKind = failure.kind,
                httpStatus = failure.httpStatus,
                safeMessage = failure.safeMessage
            )
        }

        fun partial(failure: ProbeFailure): ConnectionProbeResult {
            return result(
                outcome = ConnectionProbeOutcome.Partial,
                failureKind = failure.kind,
                httpStatus = failure.httpStatus,
                safeMessage = failure.safeMessage
            )
        }

        fun result(
            outcome: ConnectionProbeOutcome,
            failureKind: ConnectionFailureKind? = null,
            httpStatus: Int? = null,
            safeMessage: String? = null
        ): ConnectionProbeResult {
            return ConnectionProbeResult(
                outcome = outcome,
                checkedAtUtcMillis = checkedAtUtcMillis,
                serverIdentity = serverIdentity,
                lastCompletedStage = lastCompletedStage,
                latencyMillisByStage = latencyMillisByStage.toMap(),
                capabilities = capabilities,
                failureKind = failureKind,
                httpStatus = httpStatus,
                safeMessage = safeMessage
            )
        }
    }

    @Serializable
    private data class VersionDocument(
        val version: String? = null,
        val capabilities: List<String> = emptyList()
    )

    private companion object {
        const val MOBILE_ITEM_RESULTS_V1 = "mobileItemResultsV1"
        const val ANDROID_EMBED_V1 = "androidEmbedV1"
        const val MAX_RESPONSE_BYTES = 64L * 1024L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val JSON = Json { ignoreUnknownKeys = true }
        val ROOT_MARKER = Regex("""id\s*=\s*["']root["']""", RegexOption.IGNORE_CASE)
    }
}

internal suspend fun Call.awaitCancellableResponse(): Response {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        try {
            enqueue(object : Callback {
                override fun onFailure(call: Call, failure: IOException) {
                    continuation.resumeWithException(failure)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {
                        response.close()
                    }
                }
            })
        } catch (failure: Throwable) {
            continuation.resumeWithException(failure)
        }
    }
}
