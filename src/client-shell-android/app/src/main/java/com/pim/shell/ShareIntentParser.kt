package com.pim.shell

import android.content.Intent

data class SharePayload(val text: String, val url: String?)

object ShareIntentParser {
    private val urlRegex = Regex("""https?://\S+""")

    fun parse(intent: Intent?): SharePayload? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val url = urlRegex.find(text)?.value
        return SharePayload(text = text, url = url)
    }

    fun toJson(payload: SharePayload): String {
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val urlPart = payload.url?.let { """, "url":"${esc(it)}"""" } ?: ""
        return """{"text":"${esc(payload.text)}"$urlPart}"""
    }
}
