package com.pim.app.ui.shell

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    // --- webViewDocumentKey ---

    @Test
    fun `webViewDocumentKey same origin path with different query returns same key`() {
        assertEquals(
            webViewDocumentKey("https://pim.example/embed/android/today?foo=bar"),
            webViewDocumentKey("https://pim.example/embed/android/today?days=7")
        )
    }

    @Test
    fun `webViewDocumentKey same origin path with different fragment returns same key`() {
        assertEquals(
            webViewDocumentKey("https://pim.example/embed/android/today#section"),
            webViewDocumentKey("https://pim.example/embed/android/today#map")
        )
    }

    @Test
    fun `webViewDocumentKey same origin path with query and fragment equals plain path`() {
        assertEquals(
            webViewDocumentKey("https://pim.example/embed/android/today"),
            webViewDocumentKey("https://pim.example/embed/android/today?foo=bar#section")
        )
    }

    @Test
    fun `webViewDocumentKey different paths return different keys`() {
        assertNotEquals(
            webViewDocumentKey("https://pim.example/embed/android/today"),
            webViewDocumentKey("https://pim.example/embed/android/tracks")
        )
    }

    @Test
    fun `webViewDocumentKey different origins return different keys`() {
        assertNotEquals(
            webViewDocumentKey("http://127.0.0.1:5858/embed/android/today"),
            webViewDocumentKey("https://pim.example/embed/android/today")
        )
    }

    @Test
    fun `webViewDocumentKey about blank returns itself`() {
        assertEquals("about:blank", webViewDocumentKey("about:blank"))
    }

    // --- shouldReloadWebView ---

    @Test
    fun `shouldReloadWebView returns false when keys are equal`() {
        assertFalse(shouldReloadWebView(0L, 0L))
        assertFalse(shouldReloadWebView(5L, 5L))
    }

    @Test
    fun `shouldReloadWebView returns true when keys differ`() {
        assertTrue(shouldReloadWebView(0L, 1L))
        assertTrue(shouldReloadWebView(1L, 5L))
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

    // --- source contract: PimWebViewScreen loads targetUrl exactly once ---

    @Test
    fun `AndroidView update block must stay empty - factory owns initial load`() {
        val screenFile = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "shell", "PimWebViewScreen.kt"
        )
        val source = screenFile.readText()
        val hasNonEmptyUpdate = !Regex(
            """update\s*=\s*\{\s*\}""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        ).containsMatchIn(source)
        assertFalse(
            "AndroidView update block must stay empty; factory owns the initial load",
            hasNonEmptyUpdate
        )
    }

    // --- resolveTracksEmbedUrl ---

    @Test
    fun `resolveTracksEmbedUrl preserves trusted same origin tracks url with query`() {
        val result = resolveTracksEmbedUrl(
            candidate = "https://pim.example/embed/android/tracks?days=7&device=phone#map",
            serverUrl = "https://pim.example/api/v1/"
        )
        assertEquals(
            "https://pim.example/embed/android/tracks?days=7&device=phone#map",
            result
        )
    }

    @Test
    fun `resolveTracksEmbedUrl rejects candidate from different origin`() {
        val result = resolveTracksEmbedUrl(
            candidate = "https://evil.example/embed/android/tracks?days=7",
            serverUrl = "https://pim.example/api/v1/"
        )
        assertEquals(
            "https://pim.example/embed/android/tracks",
            result
        )
    }

    @Test
    fun `resolveTracksEmbedUrl rejects same origin non tracks path`() {
        val result = resolveTracksEmbedUrl(
            candidate = "https://pim.example/tasks",
            serverUrl = "https://pim.example/api/v1/"
        )
        assertEquals(
            "https://pim.example/embed/android/tracks",
            result
        )
    }

    @Test
    fun `resolveTracksEmbedUrl falls back when candidate is null`() {
        val result = resolveTracksEmbedUrl(
            candidate = null,
            serverUrl = "https://pim.example/api/v1/"
        )
        assertEquals(
            "https://pim.example/embed/android/tracks",
            result
        )
    }

    @Test
    fun `resolveTracksEmbedUrl falls back when server url is invalid`() {
        val result = resolveTracksEmbedUrl(
            candidate = "https://pim.example/embed/android/tracks?days=7",
            serverUrl = "not-a-valid-url"
        )
        assertEquals(
            "not-a-valid-url/embed/android/tracks",
            result
        )
    }

    // --- shouldSurfaceSslError ---

    @Test
    fun `shouldSurfaceSslError returns true when main frame url equals failed url`() {
        assertTrue(shouldSurfaceSslError("https://pim.example/embed/android/today", "https://pim.example/embed/android/today"))
    }

    @Test
    fun `shouldSurfaceSslError returns false when failed url is external tile subresource`() {
        assertFalse(shouldSurfaceSslError("https://pim.example/embed/android/today", "https://tiles.example.com/map/tile.png"))
    }

    @Test
    fun `shouldSurfaceSslError returns true when either url is null or blank`() {
        assertTrue(shouldSurfaceSslError(null, "https://pim.example/embed/android/today"))
        assertTrue(shouldSurfaceSslError("https://pim.example/embed/android/today", null))
        assertTrue(shouldSurfaceSslError("", "https://pim.example/embed/android/today"))
        assertTrue(shouldSurfaceSslError("https://pim.example/embed/android/today", ""))
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
