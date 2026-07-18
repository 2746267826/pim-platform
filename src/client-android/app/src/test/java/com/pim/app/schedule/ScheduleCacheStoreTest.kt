package com.pim.app.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScheduleCacheStoreTest {
    private lateinit var cacheDir: File
    private lateinit var store: ScheduleCacheStore
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cacheDir = File(context.filesDir, "schedule-cache-test")
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        store = ScheduleCacheStore(cacheDir, json)
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `round trip with locations and empty location`() {
        val windows = listOf(
            ScheduleCacheWindow("a", "With Location", "Office", 1000L, 2000L),
            ScheduleCacheWindow("b", "No Location", "", 3000L, 4000L)
        )
        val doc = ScheduleCacheDocument(
            windows = windows,
            rangeStartMillis = 1000L,
            rangeEndMillis = 4000L,
            lastAttemptAtMillis = 5000L,
            lastSuccessAtMillis = 5000L
        )
        store.write("http://test:5858", doc)
        val result = store.read("http://test:5858")
        assertNotNull(result)
        assertEquals(2, result!!.windows.size)
        assertEquals("With Location", result.windows[0].title)
        assertEquals("Office", result.windows[0].locationText)
        assertEquals("No Location", result.windows[1].title)
        assertEquals("", result.windows[1].locationText)
        assertEquals(1000L, result.rangeStartMillis)
        assertEquals(4000L, result.rangeEndMillis)
        assertEquals(5000L, result.lastAttemptAtMillis)
        assertEquals(5000L, result.lastSuccessAtMillis)
    }

    @Test
    fun `empty windows round trip preserves metadata`() {
        val doc = ScheduleCacheDocument(
            windows = emptyList(),
            rangeStartMillis = 1000L,
            rangeEndMillis = 2000L,
            lastAttemptAtMillis = 3000L,
            lastSuccessAtMillis = 4000L
        )
        store.write("http://test:5858", doc)
        val result = store.read("http://test:5858")
        assertNotNull(result)
        assertTrue(result!!.windows.isEmpty())
        assertEquals(1000L, result.rangeStartMillis)
        assertEquals(2000L, result.rangeEndMillis)
        assertEquals(3000L, result.lastAttemptAtMillis)
        assertEquals(4000L, result.lastSuccessAtMillis)
    }

    @Test
    fun `corrupt json returns null`() {
        store.cacheFile("http://test:5858").writeText("not-json")
        assertNull(store.read("http://test:5858"))
    }

    @Test
    fun `missing required windows key returns null`() {
        store.cacheFile("http://test:5858").writeText("""{"rangeStartMillis":1,"rangeEndMillis":2}""")
        assertNull(store.read("http://test:5858"))
    }

    @Test
    fun `server identities never share cache`() {
        store.write("http://one:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "one", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        store.write("http://two:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("2", "two", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        assertEquals("one", store.read("http://one:5858")!!.windows.single().title)
        assertEquals("two", store.read("http://two:5858")!!.windows.single().title)
    }

    @Test
    fun `identity does not appear in file content`() {
        store.write("https://secret-server:5858", ScheduleCacheDocument(windows = emptyList(), rangeStartMillis = 1L, rangeEndMillis = 2L))
        val files = cacheDir.listFiles() ?: emptyArray()
        for (file in files) {
            if (file.name.endsWith(".json")) {
                val content = file.readText()
                assertTrue("secret-server" !in content)
            }
        }
    }

    @Test
    fun `failure metadata update preserves windows and lastSuccess`() {
        val original = ScheduleCacheDocument(
            windows = listOf(ScheduleCacheWindow("1", "Meeting", "", 100L, 200L)),
            rangeStartMillis = 100L,
            rangeEndMillis = 200L,
            lastAttemptAtMillis = 1000L,
            lastSuccessAtMillis = 1000L
        )
        store.write("http://test:5858", original)

        val updated = store.read("http://test:5858")!!.copy(
            lastAttemptAtMillis = 2000L,
            lastError = "timeout",
            lastErrorKind = "Network"
        )
        store.write("http://test:5858", updated)

        val result = store.read("http://test:5858")
        assertNotNull(result)
        assertEquals(1, result!!.windows.size)
        assertEquals("Meeting", result.windows[0].title)
        assertEquals(100L, result.windows[0].startsAtMillis)
        assertEquals(2000L, result.lastAttemptAtMillis)
        assertEquals(1000L, result.lastSuccessAtMillis)
        assertEquals("timeout", result.lastError)
        assertEquals("Network", result.lastErrorKind)
    }

    @Test
    fun `write does not leave tmp files`() {
        store.write("http://test:5858", ScheduleCacheDocument(windows = emptyList(), rangeStartMillis = 1L, rangeEndMillis = 2L))
        val tmpFiles = cacheDir.listFiles { f -> f.name.endsWith(".tmp") }.orEmpty()
        assertTrue(tmpFiles.isEmpty())
    }

    @Test
    fun `orphaned tmp file does not affect read`() {
        val hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest("http://test:5858".toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        File(cacheDir, "${hash}.json.tmp").writeText("""{"windows":[],"rangeStartMillis":1,"rangeEndMillis":2}""")
        assertNull(store.read("http://test:5858"))
    }

    @Test
    fun `clear single server does not affect others`() {
        store.write("http://one:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "one", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        store.write("http://two:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("2", "two", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        store.clear("http://one:5858")
        assertNull(store.read("http://one:5858"))
        assertNotNull(store.read("http://two:5858"))
    }

    @Test
    fun `clearAll removes everything`() {
        store.write("http://one:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("1", "one", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        store.write("http://two:5858", ScheduleCacheDocument(windows = listOf(ScheduleCacheWindow("2", "two", "", 100L, 200L)), rangeStartMillis = 100L, rangeEndMillis = 200L))
        store.clearAll()
        assertNull(store.read("http://one:5858"))
        assertNull(store.read("http://two:5858"))
    }

    @Test
    fun `clearAll on empty directory is idempotent`() {
        store.clearAll()
        assertTrue(cacheDir.isDirectory || !cacheDir.exists())
    }

    @Test
    fun `tmp file cleaned up when writeBytes throws IOException`() {
        val doc = ScheduleCacheDocument(windows = emptyList(), rangeStartMillis = 1L, rangeEndMillis = 2L)
        val hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest("http://test:5858".toByteArray(java.nio.charset.StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val tmpFile = File(cacheDir, "${hash}.json.tmp")
        tmpFile.mkdirs()
        try {
            store.write("http://test:5858", doc)
            fail("Expected IOException")
        } catch (_: IOException) {
        }
        assertFalse("tmp directory should be cleaned up", tmpFile.exists())
    }

    @Test
    fun `readOutcome returns Found for valid cache`() {
        store.write("http://test:5858", ScheduleCacheDocument(windows = emptyList(), rangeStartMillis = 1L, rangeEndMillis = 2L))
        val result = store.readOutcome("http://test:5858")
        assertTrue(result is ScheduleCacheStore.CacheReadResult.Found)
    }

    @Test
    fun `readOutcome returns Missing when file does not exist`() {
        val result = store.readOutcome("http://not-written:5858")
        assertTrue(result is ScheduleCacheStore.CacheReadResult.Missing)
    }

    @Test
    fun `readOutcome returns Corrupt for damaged file`() {
        store.cacheFile("http://test:5858").writeText("not-json-at-all")
        val result = store.readOutcome("http://test:5858")
        assertTrue(result is ScheduleCacheStore.CacheReadResult.Corrupt)
    }

    @Test
    fun `read returns null for both Missing and Corrupt`() {
        assertNull(store.read("http://not-written:5858"))
        store.cacheFile("http://test:5858").writeText("broken")
        assertNull(store.read("http://test:5858"))
    }
}
