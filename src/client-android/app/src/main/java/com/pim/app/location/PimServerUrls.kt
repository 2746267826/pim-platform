package com.pim.app.location

// Default server URL: keep http for local dev (127.0.0.1:5858) so fresh installs work without TLS;
// production https is selected via user settings / normalized input. Non-loopback http is blocked
// by PimWebViewScreen + network_security_config (cleartext false except loopback).
const val DEFAULT_PIM_SERVER_URL = "http://127.0.0.1:5858"

fun normalizePimServerUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isEmpty()) return DEFAULT_PIM_SERVER_URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    // Bare host: default https except loopback/localhost which may use http for local dev
    val lower = trimmed.lowercase()
    return if (lower.startsWith("127.0.0.1") || lower.startsWith("localhost") || lower.startsWith("10.0.2.2") || lower == "::1" || lower == "[::1]") {
        "http://$trimmed"
    } else {
        "https://$trimmed"
    }
}

fun buildPimApiUrl(serverUrl: String, route: String): String {
    val base = normalizePimServerUrl(serverUrl)
    val apiBase = if (base.endsWith("/api/v1")) base else "$base/api/v1"
    return "$apiBase/${route.trimStart('/')}"
}
