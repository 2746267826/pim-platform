package com.pim.core.settings

data class ServerUrlValidationResult(
    val input: String,
    val normalizedUrl: String,
    val isValid: Boolean,
    val reasonCode: String? = null,
    val warnings: Set<String> = emptySet()
)

object ServerUrlValidator {
    fun validate(value: String?): ServerUrlValidationResult {
        val input = value?.trim().orEmpty()
        if (input.isBlank()) {
            return ServerUrlValidationResult(
                input = input,
                normalizedUrl = "",
                isValid = false,
                reasonCode = "missing"
            )
        }

        val endpoints = runCatching { PimServerEndpoints.from(input) }.getOrNull()
            ?: return invalid(input, "invalid-api-url")
        val scheme = endpoints.apiBaseUrl.scheme
        val host = endpoints.apiBaseUrl.host.lowercase()

        val warnings = buildSet {
            if (host == "127.0.0.1" || host == "localhost" || host == "::1" || host == "[::1]") {
                add("real-device-localhost")
            }
            if (scheme == "http") {
                add("cleartext-http")
            }
        }

        return ServerUrlValidationResult(
            input = input,
            normalizedUrl = endpoints.apiBaseUrl.toString(),
            isValid = true,
            warnings = warnings
        )
    }

    private fun invalid(input: String, reasonCode: String): ServerUrlValidationResult {
        return ServerUrlValidationResult(
            input = input,
            normalizedUrl = input,
            isValid = false,
            reasonCode = reasonCode
        )
    }
}
