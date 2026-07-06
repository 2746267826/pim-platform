package com.pim.app.location

const val DEFAULT_PIM_SERVER_URL = "http://127.0.0.1:5858"

fun normalizePimServerUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isEmpty()) return DEFAULT_PIM_SERVER_URL
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
}

fun buildPimApiUrl(serverUrl: String, route: String): String {
    val base = normalizePimServerUrl(serverUrl)
    val apiBase = if (base.endsWith("/api/v1")) base else "$base/api/v1"
    return "$apiBase/${route.trimStart('/')}"
}
