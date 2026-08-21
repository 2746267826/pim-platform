package com.pim.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerSettingsNormalizeTest {
    @Test fun `bare host gets https scheme`() {
        assertEquals("https://pim.example.com", ServerSettingsStore.normalize("pim.example.com"))
    }
    @Test fun `trailing slash is trimmed`() {
        assertEquals("https://pim.example.com", ServerSettingsStore.normalize("https://pim.example.com/"))
    }
    @Test fun `explicit http is preserved`() {
        assertEquals("http://192.168.1.10:5858", ServerSettingsStore.normalize("http://192.168.1.10:5858"))
    }
    @Test fun `blank is rejected`() {
        assertNull(ServerSettingsStore.normalize(""))
        assertNull(ServerSettingsStore.normalize("   "))
        assertNull(ServerSettingsStore.normalize(null))
    }
    @Test fun `non http scheme is rejected`() {
        assertNull(ServerSettingsStore.normalize("ftp://example.com"))
    }
    @Test fun `insecure detection`() {
        assertTrue(ServerSettingsStore.isInsecure("http://192.168.1.10:5858"))
    }
}
