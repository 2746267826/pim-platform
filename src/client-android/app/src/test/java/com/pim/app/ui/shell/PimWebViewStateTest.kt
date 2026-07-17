package com.pim.app.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PimWebViewStateTest {

    // --- extractPath ---

    @Test
    fun `extractPath returns path from full https url`() {
        assertEquals("/embed/android/today", extractPath("https://pim.example/embed/android/today"))
    }

    @Test
    fun `extractPath returns path from url with query`() {
        assertEquals("/embed/android/today", extractPath("https://pim.example/embed/android/today?foo=bar&baz=1"))
    }

    @Test
    fun `extractPath returns path from url with fragment`() {
        assertEquals("/embed/android/today", extractPath("https://pim.example/embed/android/today#section"))
    }

    @Test
    fun `extractPath returns path from url with query and fragment`() {
        assertEquals("/embed/android/today", extractPath("https://pim.example/embed/android/today?foo=bar#section"))
    }

    @Test
    fun `extractPath returns empty string for origin only url`() {
        assertEquals("", extractPath("https://pim.example"))
    }

    @Test
    fun `extractPath returns slash for origin with trailing slash`() {
        assertEquals("/", extractPath("https://pim.example/"))
    }

    @Test
    fun `extractPath returns raw input for about blank`() {
        assertEquals("about:blank", extractPath("about:blank"))
    }

    // --- extractOrigin ---

    @Test
    fun `extractOrigin returns origin from https url`() {
        assertEquals("https://pim.example", extractOrigin("https://pim.example/embed/android/today"))
    }

    @Test
    fun `extractOrigin returns origin from https url with non default port`() {
        assertEquals("https://pim.example:5858", extractOrigin("https://pim.example:5858/embed/android/today"))
    }

    @Test
    fun `extractOrigin returns origin from http url with non default port`() {
        assertEquals("http://127.0.0.1:5858", extractOrigin("http://127.0.0.1:5858/embed/android/today"))
    }

    @Test
    fun `extractOrigin returns origin from http url default port`() {
        assertEquals("http://example.com", extractOrigin("http://example.com/embed/android/today"))
    }

    @Test
    fun `extractOrigin returns null for about blank`() {
        assertEquals(null, extractOrigin("about:blank"))
    }

    @Test
    fun `extractOrigin returns null for invalid url`() {
        assertEquals(null, extractOrigin(""))
    }

    @Test
    fun `extractOrigin keeps brackets for ipv6`() {
        assertEquals(
            "http://[2001:db8::1]:5858",
            extractOrigin("http://[2001:db8::1]:5858/embed/android/today")
        )
    }

    // --- isTrustedEmbedPath ---

    @Test
    fun `isTrustedEmbedPath returns true for today embed`() {
        assertTrue(isTrustedEmbedPath("/embed/android/today"))
    }

    @Test
    fun `isTrustedEmbedPath returns true for tracks embed`() {
        assertTrue(isTrustedEmbedPath("/embed/android/tracks"))
    }

    @Test
    fun `isTrustedEmbedPath returns false for root path`() {
        assertFalse(isTrustedEmbedPath("/"))
    }

    @Test
    fun `isTrustedEmbedPath returns false for empty path`() {
        assertFalse(isTrustedEmbedPath(""))
    }

    @Test
    fun `isTrustedEmbedPath returns false for similar but different path`() {
        assertFalse(isTrustedEmbedPath("/embed/android/today-other"))
    }

    @Test
    fun `isTrustedEmbedPath returns false for subpath`() {
        assertFalse(isTrustedEmbedPath("/embed/android/today/extra"))
    }

    @Test
    fun `isTrustedEmbedPath returns false for unrelated path`() {
        assertFalse(isTrustedEmbedPath("/some/other/page"))
    }

    // --- shouldOpenInSystemBrowser ---

    @Test
    fun `external url should open in system browser`() {
        assertTrue(shouldOpenInSystemBrowser("https://google.com", "https://pim.example"))
    }

    @Test
    fun `same origin non embed path should open externally`() {
        assertTrue(shouldOpenInSystemBrowser("https://pim.example/some/other", "https://pim.example"))
    }

    @Test
    fun `same origin root path should open externally`() {
        assertTrue(shouldOpenInSystemBrowser("https://pim.example/", "https://pim.example"))
    }

    @Test
    fun `same origin today embed should not open externally`() {
        assertFalse(shouldOpenInSystemBrowser("https://pim.example/embed/android/today", "https://pim.example"))
    }

    @Test
    fun `same origin tracks embed should not open externally`() {
        assertFalse(shouldOpenInSystemBrowser("https://pim.example/embed/android/tracks", "https://pim.example"))
    }

    @Test
    fun `same origin today embed with query should not open externally`() {
        assertFalse(shouldOpenInSystemBrowser("https://pim.example/embed/android/today?foo=bar", "https://pim.example"))
    }

    @Test
    fun `same origin today embed with fragment should not open externally`() {
        assertFalse(shouldOpenInSystemBrowser("https://pim.example/embed/android/today#section", "https://pim.example"))
    }

    @Test
    fun `same origin different port is not same origin check`() {
        assertTrue(shouldOpenInSystemBrowser("https://pim.example:8080/embed/android/today", "https://pim.example"))
    }

    @Test
    fun `navigation decision blocks when trusted origin is missing`() {
        assertEquals(
            PimWebNavigationAction.Block,
            decidePimWebNavigation("https://pim.example/embed/android/today", null)
        )
    }

    @Test
    fun `navigation decision loads only trusted embed urls`() {
        assertEquals(
            PimWebNavigationAction.LoadInWebView,
            decidePimWebNavigation(
                "https://pim.example/embed/android/tracks?days=7#map",
                "https://pim.example"
            )
        )
    }

    @Test
    fun `navigation decision opens same origin non embed urls externally`() {
        assertEquals(
            PimWebNavigationAction.OpenInSystemBrowser,
            decidePimWebNavigation("https://pim.example/tasks", "https://pim.example")
        )
    }

    // --- isHttpScheme ---

    @Test
    fun `isHttpScheme returns true for http url`() {
        assertTrue(isHttpScheme("http://pim.example/embed/android/today"))
    }

    @Test
    fun `isHttpScheme returns false for https url`() {
        assertFalse(isHttpScheme("https://pim.example/embed/android/today"))
    }

    @Test
    fun `isHttpScheme returns false for about blank`() {
        assertFalse(isHttpScheme("about:blank"))
    }

    @Test
    fun `isHttpScheme returns false for data url`() {
        assertFalse(isHttpScheme("data:text/html,hello"))
    }

    // --- errorCodeToErrorMessage ---

    @Test
    fun `http 401 maps to login expired`() {
        val result = errorCodeToErrorMessage(401, "Unauthorized")
        assertEquals("登录已过期", result.reason)
        assertTrue(result.isLoginExpired)
    }

    @Test
    fun `http 403 maps to login expired`() {
        val result = errorCodeToErrorMessage(403, "Forbidden")
        assertEquals("登录已过期", result.reason)
        assertTrue(result.isLoginExpired)
    }

    @Test
    fun `http 404 maps to generic error with code`() {
        val result = errorCodeToErrorMessage(404, "Not Found")
        assertFalse(result.isLoginExpired)
        assertTrue("reason should contain code 404 but was: ${result.reason}", result.reason.contains("404"))
    }

    @Test
    fun `http 500 maps to generic error`() {
        val result = errorCodeToErrorMessage(500, "Internal Server Error")
        assertFalse(result.isLoginExpired)
        assertTrue("reason should contain code 500 but was: ${result.reason}", result.reason.contains("500"))
    }

    @Test
    fun `http 0 with description maps to generic error`() {
        val result = errorCodeToErrorMessage(0, "net::ERR_CONNECTION_REFUSED")
        assertFalse(result.isLoginExpired)
        assertTrue("reason should contain description", result.reason.contains("net::ERR_CONNECTION_REFUSED"))
    }

    // --- webViewInternalErrorToReason ---

    @Test
    fun `internal error description is included`() {
        val reason = webViewInternalErrorToReason("net::ERR_NAME_NOT_RESOLVED")
        assertTrue("reason should contain error description", reason.contains("net::ERR_NAME_NOT_RESOLVED"))
    }

    @Test
    fun `buildPimWebUrl maps today and tracks aliases to embed routes`() {
        val server = "https://pim.example/api/v1/"

        assertEquals(
            "https://pim.example/embed/android/today",
            buildPimWebUrl(server, "/today")
        )
        assertEquals(
            "https://pim.example/embed/android/tracks",
            buildPimWebUrl(server, "/tracks")
        )
    }

    @Test
    fun `buildPimWebUrl preserves query and fragment for tracks`() {
        assertEquals(
            "https://pim.example/embed/android/tracks?days=7&device=phone#map",
            buildPimWebUrl(
                "https://pim.example/api/v1/",
                "/tracks?days=7&device=phone#map"
            )
        )
    }

    @Test
    fun `buildPimWebUrl does not reinterpret tasks as tracks`() {
        assertEquals(
            "https://pim.example/tasks",
            buildPimWebUrl("https://pim.example/api/v1/", "/tasks")
        )
    }

    // --- PimWebViewState model ---

    @Test
    fun `loading has isLoading true`() {
        val s: PimWebViewState = PimWebViewState.Loading
        assertTrue(s.isLoading)
        assertFalse(s.isError)
        assertFalse(s.isContent)
    }

    @Test
    fun `content has isContent true and holds url`() {
        val s = PimWebViewState.Content("https://pim.example/embed/android/today")
        assertTrue(s.isContent)
        assertFalse(s.isLoading)
        assertFalse(s.isError)
        assertEquals("https://pim.example/embed/android/today", s.url)
    }

    @Test
    fun `error has isError true and holds reason`() {
        val s = PimWebViewState.Error("加载失败")
        assertTrue(s.isError)
        assertFalse(s.isLoading)
        assertFalse(s.isContent)
        assertEquals("加载失败", s.reason)
        assertFalse(s.isLoginExpired)
    }

    @Test
    fun `error can be login expired`() {
        val s = PimWebViewState.Error("登录已过期", isLoginExpired = true)
        assertTrue(s.isError)
        assertTrue(s.isLoginExpired)
        assertEquals("登录已过期", s.reason)
    }
}
