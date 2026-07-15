package com.pim.app.mobile.diagnostics

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticRedactor @Inject constructor() {

    companion object {
        private val NORMALIZED_SENSITIVE_KEYS = setOf(
            "token", "accesstoken", "refreshtoken",
            "password", "authorization", "cookie", "setcookie",
            "secret", "apikey"
        )

        private val JWT = Regex("""eyJ[a-zA-Z0-9_\-+/=]+(?:\.[a-zA-Z0-9_\-+/=]+){2}""")

        private val AUTH_HEADER = Regex(
            """(?i)(\bauthorization\s*:)(?!\s*\[REDACTED])(\s*.+?)(?=\r?\n|$)"""
        )

        private val STANDALONE_BEARER = Regex(
            """(?i)(\bbearer\s+)(?!\s*\[REDACTED])([^\r\n&,;]+)"""
        )

        private val SENSITIVE_HEADER = Regex(
            """(?i)(\b(?:cookie|set[-_]?cookie)\s*:)(?!\s*\[REDACTED])(\s*.+?)(?=\r?\n|$)"""
        )

        private val SENSITIVE_ASSIGNMENT = Regex(
            """(?i)((?:"|')?(?:[a-zA-Z][a-zA-Z0-9_-]*)?(?:token|password|secret|api[-_]?key|authorization|cookie)(?:"|')?\s*[=:]\s*)(?!(?:\s*)(?:"|')?\[REDACTED])(?:"[^"]*"|'[^']*'|[^\r\n&,;]+)"""
        )

        private val DRIVE_LETTER = Regex("""^[A-Za-z]:""")
    }

    fun redactJsonLine(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("{") -> try {
                redactObject(JSONObject(trimmed)).toString()
            } catch (e: JSONException) {
                redactText(input)
            }

            trimmed.startsWith("[") -> try {
                redactArray(JSONArray(trimmed)).toString()
            } catch (e: JSONException) {
                redactText(input)
            }

            else -> redactText(input)
        }
    }

    fun redactText(input: String): String {
        var result = input
        result = JWT.replace(result) { "[REDACTED]" }
        result = AUTH_HEADER.replace(result) { "${it.groupValues[1]}[REDACTED]" }
        result = STANDALONE_BEARER.replace(result) { "${it.groupValues[1]}[REDACTED]" }
        result = SENSITIVE_HEADER.replace(result) { "${it.groupValues[1]}[REDACTED]" }
        result = SENSITIVE_ASSIGNMENT.replace(result) { "${it.groupValues[1]}[REDACTED]" }
        return result
    }

    fun findCredentialLeaks(input: String): Set<String> {
        val codes = mutableSetOf<String>()
        if (JWT.containsMatchIn(input)) codes.add("jwt")
        if (STANDALONE_BEARER.containsMatchIn(input)) codes.add("bearer")
        if (SENSITIVE_ASSIGNMENT.containsMatchIn(input)) codes.add("sensitive-assignment")
        if (AUTH_HEADER.containsMatchIn(input) || SENSITIVE_HEADER.containsMatchIn(input)) codes.add("sensitive-header")
        return codes
    }

    fun isUnsafeEntryName(name: String): Boolean {
        if (name.contains("..")) return true
        if (name.startsWith("/")) return true
        if (name.startsWith("\\")) return true
        if (DRIVE_LETTER.containsMatchIn(name)) return true

        val normalized = name.lowercase().replace(Regex("[^a-z0-9]"), "")
        return listOf("token", "password", "authorization", "cookie", "secret", "apikey")
            .any { normalized.contains(it) }
    }

    private fun redactObject(obj: JSONObject): JSONObject {
        val result = JSONObject()
        for (key in obj.keys()) {
            val normalized = key.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
            if (isSensitiveKey(normalized)) {
                result.put(key, "[REDACTED]")
            } else {
                result.put(key, redactValue(obj.get(key)))
            }
        }
        return result
    }

    private fun isSensitiveKey(key: String): Boolean =
        key in NORMALIZED_SENSITIVE_KEYS || NORMALIZED_SENSITIVE_KEYS.any { key.endsWith(it) }

    private fun redactArray(arr: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until arr.length()) {
            result.put(redactValue(arr.get(i)))
        }
        return result
    }

    private fun redactValue(value: Any?): Any? {
        return when (value) {
            is JSONObject -> redactObject(value)
            is JSONArray -> redactArray(value)
            is String -> redactText(value)
            else -> value
        }
    }
}
