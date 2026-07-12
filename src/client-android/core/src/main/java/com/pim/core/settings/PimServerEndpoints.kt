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
            val trustedOrigin = trustedOriginOf(webOrigin)

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

        fun trustedOriginOf(url: HttpUrl): String {
            require(url.scheme == "http" || url.scheme == "https") {
                "Server URL scheme must be http or https"
            }
            val originHost = if (':' in url.host) "[${url.host}]" else url.host
            val defaultPort = if (url.scheme == "https") 443 else 80
            return buildString {
                append(url.scheme)
                append("://")
                append(originHost)
                if (url.port != defaultPort) {
                    append(':')
                    append(url.port)
                }
            }
        }

        fun normalizeTrustedOrigin(value: String): String {
            val url = value.toHttpUrl()
            require(url.encodedUsername.isEmpty() && url.encodedPassword.isEmpty()) {
                "Server origin must not contain credentials"
            }
            require(url.encodedPath == "/" && url.query == null && url.fragment == null) {
                "Server origin must not contain a path, query, or fragment"
            }
            return trustedOriginOf(url)
        }

        fun apiBaseUrlForTrustedOrigin(value: String): HttpUrl {
            val trustedOrigin = normalizeTrustedOrigin(value).toHttpUrl()
            return trustedOrigin.resolve("$API_PATH/")!!
        }

        private const val API_PATH = "/api/v1"
    }
}
