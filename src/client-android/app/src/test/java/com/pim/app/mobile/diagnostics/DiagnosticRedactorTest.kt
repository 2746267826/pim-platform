package com.pim.app.mobile.diagnostics

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiagnosticRedactorTest {

    private val redactor = DiagnosticRedactor()

    // --- redactJsonLine ---

    @Test
    fun redactJsonLine_redactsSensitiveKeysInNestedObject() {
        val input = """{"user":{"name":"Alice","token":"s3cret","data":{"apiKey":"xyz","idToken":"jwt1","clientSecret":"cs1","sessionCookie":"sc1"}}}"""
        val result = redactor.redactJsonLine(input)

        assertFalse(result, result.contains("s3cret"))
        assertFalse(result, result.contains("xyz"))
        assertFalse(result, result.contains("jwt1"))
        assertFalse(result, result.contains("cs1"))
        assertFalse(result, result.contains("sc1"))
        assertTrue(result, result.contains("[REDACTED]"))

        val parsed = JSONObject(result)
        assertEquals("[REDACTED]", parsed.getJSONObject("user").getString("token"))
        val data = parsed.getJSONObject("user").getJSONObject("data")
        assertEquals("[REDACTED]", data.getString("apiKey"))
        assertEquals("[REDACTED]", data.getString("idToken"))
        assertEquals("[REDACTED]", data.getString("clientSecret"))
        assertEquals("[REDACTED]", data.getString("sessionCookie"))
        assertEquals("Alice", parsed.getJSONObject("user").getString("name"))
    }

    @Test
    fun redactJsonLine_redactsSensitiveKeysInArray() {
        val input = """[{"token":"abc"},{"name":"test","apiKey":"key123","authToken":"at1","userPassword":"up1","serviceApiKey":"sak1"}]"""
        val result = redactor.redactJsonLine(input)

        assertFalse(result, result.contains("abc"))
        assertFalse(result, result.contains("key123"))
        assertFalse(result, result.contains("at1"))
        assertFalse(result, result.contains("up1"))
        assertFalse(result, result.contains("sak1"))

        val parsed = JSONArray(result)
        assertEquals("[REDACTED]", parsed.getJSONObject(0).getString("token"))
        assertEquals("[REDACTED]", parsed.getJSONObject(1).getString("apiKey"))
        assertEquals("[REDACTED]", parsed.getJSONObject(1).getString("authToken"))
        assertEquals("[REDACTED]", parsed.getJSONObject(1).getString("userPassword"))
        assertEquals("[REDACTED]", parsed.getJSONObject(1).getString("serviceApiKey"))
        assertEquals("test", parsed.getJSONObject(1).getString("name"))
    }

    @Test
    fun redactJsonLine_fallsBackToTextRedaction() {
        val input = "Authorization: Bearer abc123def"
        val result = redactor.redactJsonLine(input)
        assertFalse(result, result.contains("abc123def"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactJsonLine_redactsSecretsInJsonStringValues() {
        val input = """{"message":"Authorization: Bearer secret123","throwable":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdA","details":{"note":"password=mysecret"}}"""
        val result = redactor.redactJsonLine(input)
        assertFalse(result, result.contains("secret123"))
        assertFalse(result, result.contains("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdA"))
        assertFalse(result, result.contains("mysecret"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    // --- redactText ---

    @Test
    fun redactText_redactsCookieMultiValueAndAuthBasic() {
        val input = "Cookie: name=value; other=val\nAuthorization: Basic YTpY\nSet-Cookie: session=abc; Path=/"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("name=value; other=val"))
        assertFalse(result, result.contains("YTpY"))
        assertFalse(result, result.contains("session=abc; Path=/"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_removesBearerAndJwt() {
        val input = "Authorization: Bearer abc123def\nsome text\nBearer xyz789\nJWT: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdA"
        val result = redactor.redactText(input)

        assertFalse(result, result.contains("abc123def"))
        assertFalse(result, result.contains("xyz789"))
        assertFalse(result, result.contains("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdA"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_removesSensitiveAssignments() {
        val input = "access_token=abc123&password: secret123&api_key=xyz456"
        val result = redactor.redactText(input)

        assertFalse(result, result.contains("abc123"))
        assertFalse(result, result.contains("secret123"))
        assertFalse(result, result.contains("xyz456"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsQuotedAssignment() {
        val input = """password="my secret value"&access_token='token data'&secret=plain"""
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("my secret value"))
        assertFalse(result, result.contains("token data"))
        assertFalse(result, result.contains("plain"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_preservesBenignText() {
        val input = "token refresh failed\npassword field missing\nauthorization check passed\nsome cookie data"
        val result = redactor.redactText(input)
        assertEquals(input, result)
    }

    // --- findCredentialLeaks ---

    @Test
    fun findCredentialLeaks_detectsRemainingCredentials() {
        val result = redactor.findCredentialLeaks(
            "Bearer abc123\neyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dGVzdA\npassword=secret"
        )
        assertTrue("should detect bearer", result.contains("bearer"))
        assertTrue("should detect jwt", result.contains("jwt"))
        assertTrue("should detect assignment", result.contains("sensitive-assignment"))
    }

    @Test
    fun findCredentialLeaks_detectsSensitiveHeaders() {
        val raw = redactor.findCredentialLeaks(
            "Cookie: secret\nSet-Cookie: secret2\nAuthorization: Basic xyz"
        )
        assertTrue("should detect unredacted cookie", raw.contains("sensitive-header"))

        val clean = redactor.findCredentialLeaks(
            "Cookie: [REDACTED]\nSet-Cookie: [REDACTED]\nAuthorization: [REDACTED]"
        )
        assertTrue("should accept redacted headers, got=$clean", clean.isEmpty())
    }

    @Test
    fun findCredentialLeaks_acceptsRedactedAndBenign() {
        val result = redactor.findCredentialLeaks(
            "Bearer [REDACTED]\npassword: [REDACTED]\ntoken refresh failed\npassword field missing"
        )
        assertTrue("should be empty for redacted/benign", result.isEmpty())
    }

    @Test
    fun findCredentialLeaks_acceptsQuotedRedactedJsonAssignments() {
        val result = redactor.findCredentialLeaks(
            """{"clientSecret":"[REDACTED]","accessToken":"[REDACTED]"}"""
        )

        assertTrue("redacted JSON values must pass final archive scan: $result", result.isEmpty())
    }

    // --- redactText: authorization/cookie assignment ---

    @Test
    fun redactText_redactsAuthorizationAssignment() {
        val input = "Authorization=Basic abc"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("Basic abc"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsCookieAssignment() {
        val input = "Cookie=session-secret"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("session-secret"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsSetCookieAssignment() {
        val input = "set_cookie='secret'"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("'secret'"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    // --- findCredentialLeaks: detect new assignment types ---

    @Test
    fun findCredentialLeaks_detectsAuthorizationAssignment() {
        val raw = redactor.findCredentialLeaks("Authorization=Basic abc")
        assertTrue("should detect authorization assignment", raw.contains("sensitive-assignment"))

        val clean = redactor.findCredentialLeaks("Authorization=[REDACTED]")
        assertTrue("should accept redacted authorization", clean.isEmpty())
    }

    @Test
    fun findCredentialLeaks_detectsCookieAssignment() {
        val raw = redactor.findCredentialLeaks("Cookie=session-secret")
        assertTrue("should detect cookie assignment", raw.contains("sensitive-assignment"))

        val clean = redactor.findCredentialLeaks("Cookie=[REDACTED]")
        assertTrue("should accept redacted cookie", clean.isEmpty())
    }

    // --- header regex regression ---

    @Test
    fun redactText_redactsProcessCookieSuffixAssignment() {
        val input = "processCookie: benign"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("benign"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsSetCookieSuffixAssignment() {
        val input = "CookieManager.setCookie: benign"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("benign"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsStandardCookieHeader() {
        val input = "Cookie: secret"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("secret"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsStandardSetCookieHeader() {
        val input = "Set-Cookie: secret"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("secret"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactText_redactsCamelCaseSuffixAssignments() {
        val input = "authToken=abc123&clientSecret=secret456&userPassword=pass789&serviceApiKey=key000"
        val result = redactor.redactText(input)
        assertFalse(result, result.contains("abc123"))
        assertFalse(result, result.contains("secret456"))
        assertFalse(result, result.contains("pass789"))
        assertFalse(result, result.contains("key000"))
        assertTrue(result, result.contains("[REDACTED]"))
    }

    @Test
    fun redactJsonLine_fallsBackOnMalformedJsonWithSensitiveKey() {
        val input = """{"key":"val","clientSecret":"secret-value""""
        val result = redactor.redactJsonLine(input)
        assertFalse("secret-value should be redacted in fallback", result.contains("secret-value"))
        assertTrue("should contain [REDACTED]", result.contains("[REDACTED]"))

        val leaks = redactor.findCredentialLeaks(input)
        assertTrue("should detect sensitive assignment in raw text", leaks.contains("sensitive-assignment"))
    }

    // --- space-containing unquoted values ---

    @Test
    fun redactText_redactsUnquotedValueWithSpacesUntilAmpersand() {
        val input = "password=super secret&next=ok"
        val result = redactor.redactText(input)
        assertEquals("password=[REDACTED]&next=ok", result)
    }

    @Test
    fun redactText_redactsBearerValueWithSpacesUntilSemicolon() {
        val input = "Bearer abc def; next"
        val result = redactor.redactText(input)
        assertEquals("Bearer [REDACTED]; next", result)
    }

    @Test
    fun findCredentialLeaks_detectsAndClearsSpaceContainingValues() {
        val rawInput = "password=super secret\nBearer abc def; next"
        val raw = redactor.findCredentialLeaks(rawInput)
        assertTrue("should detect sensitive-assignment in raw", raw.contains("sensitive-assignment"))
        assertTrue("should detect bearer in raw", raw.contains("bearer"))

        val redacted = redactor.redactText(rawInput)
        assertFalse("assignment tail must not remain", redacted.contains("secret"))
        assertFalse("bearer tail must not remain", redacted.contains("def"))
        val clean = redactor.findCredentialLeaks(redacted)
        assertTrue("redacted result should have no leaks: $clean", clean.isEmpty())
    }

    // --- isUnsafeEntryName ---

    @Test
    fun isUnsafeEntryName_rejectsUnsafeNames() {
        assertTrue("token file name", redactor.isUnsafeEntryName("my_token.txt"))
        assertTrue("password file name", redactor.isUnsafeEntryName("password.txt"))
        assertTrue("authorization file name", redactor.isUnsafeEntryName("authorization.log"))
        assertTrue("cookie file name", redactor.isUnsafeEntryName("cookie_data.txt"))
        assertTrue("secret file name", redactor.isUnsafeEntryName("secret.key"))
        assertTrue("api key file name", redactor.isUnsafeEntryName("api_key_file.txt"))
        assertTrue("absolute path /", redactor.isUnsafeEntryName("/etc/passwd"))
        assertTrue("absolute path C:", redactor.isUnsafeEntryName("C:\\config\\file"))
        assertTrue("path traversal", redactor.isUnsafeEntryName("../outside"))
        assertTrue("deep path traversal", redactor.isUnsafeEntryName("dir/../../etc"))
        assertTrue("absolute backslash", redactor.isUnsafeEntryName("\\Windows\\system32"))
    }

    @Test
    fun isUnsafeEntryName_acceptsSafeNames() {
        assertFalse("simple txt", redactor.isUnsafeEntryName("notes.txt"))
        assertFalse("simple md", redactor.isUnsafeEntryName("readme.md"))
        assertFalse("numbers", redactor.isUnsafeEntryName("2024-01-01.log"))
        assertFalse("health", redactor.isUnsafeEntryName("health_check.json"))
    }
}
