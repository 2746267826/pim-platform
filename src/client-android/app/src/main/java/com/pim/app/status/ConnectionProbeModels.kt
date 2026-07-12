package com.pim.app.status

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionProbeStage { Url, Health, Version, AuthenticatedStatus, WebRoot, EmbedBootstrap }

@Serializable
enum class ConnectionFailureKind { InvalidUrl, Dns, Connect, Timeout, Tls, Http, Unauthorized, WrongPath, IncompatibleVersion }

@Serializable
enum class ConnectionProbeOutcome { Reachable, Partial, Blocked }

@Serializable
data class ServerCapabilities(
    val mobileItemResultsV1: Boolean,
    val androidEmbedV1: Boolean
)

@Serializable
data class ConnectionProbeResult(
    val outcome: ConnectionProbeOutcome,
    val checkedAtUtcMillis: Long,
    val serverIdentity: String? = null,
    val lastCompletedStage: ConnectionProbeStage?,
    val latencyMillisByStage: Map<ConnectionProbeStage, Long>,
    val capabilities: ServerCapabilities,
    val failureKind: ConnectionFailureKind? = null,
    val httpStatus: Int? = null,
    val safeMessage: String? = null
)

fun interface ProbeTokenSource {
    fun currentAccessToken(serverUrl: String): String?
}
