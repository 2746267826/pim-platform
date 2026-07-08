package com.pim.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlValidatorTest {
    @Test
    fun blankUrlIsNotConfigured() {
        val result = ServerUrlValidator.validate("")

        assertFalse(result.isValid)
        assertEquals("missing", result.reasonCode)
        assertEquals("", result.normalizedUrl)
    }

    @Test
    fun publicIpIsAcceptedAndGetsTrailingSlash() {
        val result = ServerUrlValidator.validate("http://203.0.113.8:5858/api/v1")

        assertTrue(result.isValid)
        assertEquals("http://203.0.113.8:5858/api/v1/", result.normalizedUrl)
    }

    @Test
    fun publicDomainIsAccepted() {
        val result = ServerUrlValidator.validate("https://pim.example.com/api/v1/")

        assertTrue(result.isValid)
        assertEquals("https://pim.example.com/api/v1/", result.normalizedUrl)
    }

    @Test
    fun realDeviceLocalhostReceivesWarning() {
        val result = ServerUrlValidator.validate("http://127.0.0.1:5858/api/v1/")

        assertTrue(result.isValid)
        assertTrue(result.warnings.contains("real-device-localhost"))
    }

    @Test
    fun serverSettingsDefaultIsBlankForRealPhones() {
        assertEquals("", ServerSettingsStore.DEFAULT_BASE_URL)
        assertEquals("", ServerSettingsStore.normalizeBaseUrl(""))
    }
}
