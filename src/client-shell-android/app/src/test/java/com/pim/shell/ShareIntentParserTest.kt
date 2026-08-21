package com.pim.shell

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentParserTest {
    private fun intentWith(action: String, text: String?, type: String = "text/plain") =
        Intent(action).apply {
            this.type = type
            if (text != null) putExtra(Intent.EXTRA_TEXT, text)
        }

    @Test fun `SEND with text returns payload`() {
        val payload = ShareIntentParser.parse(intentWith(Intent.ACTION_SEND, "https://example.com  hello"))
        assertEquals("https://example.com  hello", payload?.text)
    }
    @Test fun `non-SEND returns null`() {
        assertNull(ShareIntentParser.parse(Intent(Intent.ACTION_VIEW)))
    }
    @Test fun `SEND without text returns null`() {
        assertNull(ShareIntentParser.parse(intentWith(Intent.ACTION_SEND, null)))
    }
    @Test fun `extracts url from mixed text`() {
        val payload = ShareIntentParser.parse(intentWith(Intent.ACTION_SEND, "See https://example.com/path?q=1 nice"))
        assertEquals("https://example.com/path?q=1", payload?.url)
    }
}
