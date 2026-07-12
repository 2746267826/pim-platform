package com.pim.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PimServerEndpointsTest {
    @Test
    fun derivesApiAndWebEndpointsFromConfiguredApiBase() {
        val endpoints = PimServerEndpoints.from("http://127.0.0.1:5858/api/v1/")

        assertEquals("http://127.0.0.1:5858/api/v1/", endpoints.apiBaseUrl.toString())
        assertEquals("http://127.0.0.1:5858/", endpoints.webOrigin.toString())
        assertEquals("http://127.0.0.1:5858", endpoints.trustedOrigin)
        assertEquals("http://127.0.0.1:5858/health", endpoints.healthUrl.toString())
        assertEquals("http://127.0.0.1:5858/api/version", endpoints.versionUrl.toString())
        assertEquals("http://127.0.0.1:5858/api/v1/status/summary", endpoints.statusSummaryUrl.toString())
        assertEquals("http://127.0.0.1:5858/embed/android/today", endpoints.todayEmbedUrl.toString())
        assertEquals("http://127.0.0.1:5858/embed/android/tracks", endpoints.tracksEmbedUrl.toString())
    }

    @Test
    fun preservesHttpsAndPortAndNormalizesExactlyOneTrailingSlash() {
        assertEquals(
            "https://pim.example:8443/api/v1/",
            PimServerEndpoints.from("https://pim.example:8443/api/v1").apiBaseUrl.toString()
        )
        assertEquals(
            "https://pim.example:8443/api/v1/",
            PimServerEndpoints.from("https://pim.example:8443/api/v1////").apiBaseUrl.toString()
        )

        val endpoints = PimServerEndpoints.from("https://pim.example:8443/api/v1/")
        assertEquals("https://pim.example:8443/", endpoints.webOrigin.toString())
        assertEquals("https://pim.example:8443", endpoints.trustedOrigin)
    }

    @Test
    fun trustedOriginOmitsDefaultPorts() {
        assertEquals(
            "http://pim.example",
            PimServerEndpoints.from("http://pim.example:80/api/v1").trustedOrigin
        )
        assertEquals(
            "https://pim.example",
            PimServerEndpoints.from("https://pim.example:443/api/v1").trustedOrigin
        )
    }

    @Test
    fun preservesIpv6HostWithBrackets() {
        val endpoints = PimServerEndpoints.from("http://[2001:db8::1]:5858/api/v1/")

        assertEquals("http://[2001:db8::1]:5858/", endpoints.webOrigin.toString())
        assertEquals("http://[2001:db8::1]:5858", endpoints.trustedOrigin)
        assertEquals("http://[2001:db8::1]:5858/health", endpoints.healthUrl.toString())
    }

    @Test
    fun rejectsWrongPathQueryFragmentSchemeHostAndCredentials() {
        val invalidUrls = listOf(
            "https://pim.example/v1",
            "https://pim.example/api/v1/extra",
            "https://pim.example/api/v1?tenant=x",
            "https://pim.example/api/v1#fragment",
            "ftp://pim.example/api/v1",
            "http:///api/v1",
            "https://user:secret@pim.example/api/v1"
        )

        invalidUrls.forEach { configuredUrl ->
            assertThrows(configuredUrl, IllegalArgumentException::class.java) {
                PimServerEndpoints.from(configuredUrl)
            }
        }
    }
}
