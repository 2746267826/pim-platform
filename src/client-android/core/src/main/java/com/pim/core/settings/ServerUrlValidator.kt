package com.pim.core.settings

import java.net.URI

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

        val uri = runCatching { URI(input) }.getOrNull()
            ?: return invalid(input, "invalid-url")
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return invalid(input, "invalid-scheme")
        }
        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) {
            return invalid(input, "missing-host")
        }

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
            normalizedUrl = input.trimEnd('/') + "/",
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
