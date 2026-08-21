package com.pim.shell

import android.content.Intent
import org.json.JSONObject

data class SharePayload(val text: String, val url: String?)

object ShareIntentParser {
    private val urlRegex = Regex("""https?://\S+""")

    fun parse(intent: Intent?): SharePayload? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val rawUrl = urlRegex.find(text)?.value
        val url = rawUrl?.trimEnd('.', ',', ';', '!', ')', ']', '}', '\'', '"', '。', '，')?.takeIf { it.isNotEmpty() }
        return SharePayload(text = text, url = url)
    }

    fun toJson(payload: SharePayload): String {
        return JSONObject().put("text", payload.text).apply { payload.url?.let { put("url", it) } }.toString()
    }
}
