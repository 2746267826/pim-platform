package com.pim.core.settings

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

data class PimServerEndpoints(
    val apiBaseUrl: HttpUrl,
    val webOrigin: HttpUrl,
    val trustedOrigin: String,
    val healthUrl: HttpUrl,
    val versionUrl: HttpUrl,
    val statusSummaryUrl: HttpUrl,
    val todayEmbedUrl: HttpUrl,
    val tracksEmbedUrl: HttpUrl
) {
    companion object {
        fun from(configuredApiUrl: String): PimServerEndpoints {
            val api = configuredApiUrl.toHttpUrl()
            require(api.scheme == "http" || api.scheme == "https") {
                "API URL scheme must be http or https"
            }
            require(api.host.isNotBlank()) { "API URL must contain a host" }
            require(api.encodedUsername.isEmpty() && api.encodedPassword.isEmpty()) {
                "API URL must not contain credentials"
            }
            require(api.encodedPath.trimEnd('/') == API_PATH) {
                "API URL path must be $API_PATH"
            }
            require(api.query == null && api.fragment == null) {
                "API URL must not contain a query or fragment"
            }

            val apiBaseUrl = api.newBuilder()
                .encodedPath("$API_PATH/")
                .query(null)
                .fragment(null)
                .build()
            val webOrigin = api.newBuilder()
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
            val originHost = if (':' in webOrigin.host) "[${webOrigin.host}]" else webOrigin.host
            val defaultPort = if (webOrigin.scheme == "https") 443 else 80
            val trustedOrigin = buildString {
                append(webOrigin.scheme)
                append("://")
                append(originHost)
                if (webOrigin.port != defaultPort) {
                    append(':')
                    append(webOrigin.port)
                }
            }

            return PimServerEndpoints(
                apiBaseUrl = apiBaseUrl,
                webOrigin = webOrigin,
                trustedOrigin = trustedOrigin,
                healthUrl = webOrigin.resolve("/health")!!,
                versionUrl = webOrigin.resolve("/api/version")!!,
                statusSummaryUrl = webOrigin.resolve("/api/v1/status/summary")!!,
                todayEmbedUrl = webOrigin.resolve("/embed/android/today")!!,
                tracksEmbedUrl = webOrigin.resolve("/embed/android/tracks")!!
            )
        }

        private const val API_PATH = "/api/v1"
    }
}
