package com.pim.app.mobile.logs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pim.app.settings.TrackingSettingsStore
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
class StructuredLogRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: StructuredLogRepository
    private lateinit var settingsStore: TrackingSettingsStore
    private var nowMillis = 10_000_000L
    private val testLogDir: File get() = File(context.filesDir, "logs")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testLogDir.deleteRecursively()
        nowMillis = 10_000_000L
        val prefs = context.getSharedPreferences("test_tracking_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        settingsStore = TrackingSettingsStore(prefs)
        repository = StructuredLogRepository(context, settingsStore) { nowMillis }
    }

    @After
    fun tearDown() {
        testLogDir.deleteRecursively()
    }

    @Test
    fun writesJsonlFileOnInfo() = runTest {
        repository.info("test-op", "test message", mapOf("key" to "value"))

        val files = testLogDir.listFiles() ?: emptyArray()
        val jsonlFile = files.firstOrNull { it.name.endsWith(".jsonl") }
        assertNotNull("JSONL file should exist", jsonlFile)

        val lines = jsonlFile!!.readLines()
        assertEquals(1, lines.size)

        val json = JSONObject(lines[0])
        assertEquals("info", json.getString("level"))
        assertEquals("test-op", json.getString("tag"))
        assertEquals("test message", json.getString("message"))
        assertTrue(json.has("details"))
        assertTrue(json.has("occurredAtUtc"))
        assertEquals("android", json.getString("source"))
    }

    @Test
    fun recentReturnsLatestFirst() = runTest {
        repository.info("op1", "msg1")
        repository.info("op2", "msg2")
        repository.info("op3", "msg3")

        val recent = repository.recent(2)
        assertEquals(2, recent.size)
        assertEquals("op3", recent[0].tag)
        assertEquals("msg3", recent[0].message)
        assertEquals("op2", recent[1].tag)
        assertEquals("msg2", recent[1].message)
    }

    @Test
    fun logWithNanDetailsDoesNotThrowAndWritesSubsequentLogs() = runTest {
        repository.info("bad-op", "bad details", details = mapOf("bad" to Double.NaN))

        repository.info("good-op", "good message")
        val recent = repository.recent(10)
        assertTrue("Should still be able to read at least one log after NaN", recent.isNotEmpty())
        assertEquals("good-op", recent[0].tag)
    }

    @Test
    fun recentSkipsCorruptLines() = runTest {
        repository.info("valid-op", "valid message")

        val jsonlFile = testLogDir.listFiles()!!.first { it.name.endsWith(".jsonl") }
        jsonlFile.appendText("not valid json\n")

        val recent = repository.recent(10)
        assertEquals(1, recent.size)
        assertEquals("valid-op", recent[0].tag)
    }

    @Test
    fun debugNotWrittenWhenVerboseDisabled() = runTest {
        settingsStore.setVerboseLoggingEnabled(false, nowMillis)

        repository.debug("op", "msg")

        assertFalse(
            "No log file should exist when verbose disabled",
            testLogDir.exists() && testLogDir.listFiles()!!.any { it.name.endsWith(".jsonl") }
        )
    }

    @Test
    fun debugWrittenWhenVerboseEnabled() = runTest {
        settingsStore.setVerboseLoggingEnabled(true, nowMillis)

        repository.debug("op", "verbose debug message")

        val files = testLogDir.listFiles() ?: emptyArray()
        val jsonlFile = files.firstOrNull { it.name.endsWith(".jsonl") }
        assertNotNull("JSONL file should exist for debug when verbose enabled", jsonlFile)
        val json = JSONObject(jsonlFile!!.readLines().first())
        assertEquals("debug", json.getString("level"))
    }

    @Test
    fun debugNotWrittenWhenVerboseExpired() = runTest {
        settingsStore.setVerboseLoggingEnabled(true, nowMillis)
        nowMillis += 25L * 60 * 60 * 1000L

        repository.debug("op", "msg after expiry")

        assertFalse(
            "No log file should exist after verbose expiry",
            testLogDir.exists() && testLogDir.listFiles()!!.any { it.name.endsWith(".jsonl") }
        )
    }

    @Test
    fun infoWrittenWhenVerboseDisabled() = runTest {
        settingsStore.setVerboseLoggingEnabled(false, nowMillis)

        repository.info("op", "info message")

        val files = testLogDir.listFiles() ?: emptyArray()
        assertTrue(
            "Info log should exist even when verbose disabled",
            files.any { it.name.endsWith(".jsonl") }
        )
    }

    @Test
    fun warnWrittenWhenVerboseDisabled() = runTest {
        settingsStore.setVerboseLoggingEnabled(false, nowMillis)

        repository.warn("op", "warn message")

        val files = testLogDir.listFiles() ?: emptyArray()
        assertTrue(
            "Warn log should exist even when verbose disabled",
            files.any { it.name.endsWith(".jsonl") }
        )
    }

    @Test
    fun errorWrittenWhenVerboseDisabled() = runTest {
        settingsStore.setVerboseLoggingEnabled(false, nowMillis)

        repository.error("op", "error message")

        val files = testLogDir.listFiles() ?: emptyArray()
        assertTrue(
            "Error log should exist even when verbose disabled",
            files.any { it.name.endsWith(".jsonl") }
        )
    }

    @Test
    fun retentionKeepsLast7Days() = runTest {
        settingsStore.write(settingsStore.read().copy(logRetentionDays = 7))
        nowMillis = 1705276800000L
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-01-08.jsonl").writeText("old\n")
        File(testLogDir, "mobile-2024-01-09.jsonl").writeText("keep boundary\n")
        File(testLogDir, "mobile-2024-01-15.jsonl").writeText("today\n")

        repository.info("op", "trigger cleanup")

        assertFalse("File before cutoff should be deleted", File(testLogDir, "mobile-2024-01-08.jsonl").exists())
        assertTrue("File at cutoff boundary should be kept", File(testLogDir, "mobile-2024-01-09.jsonl").exists())
        assertTrue("Today's file should be kept", File(testLogDir, "mobile-2024-01-15.jsonl").exists())
    }

    @Test
    fun retentionDeletesOlderFiles() = runTest {
        settingsStore.write(settingsStore.read().copy(logRetentionDays = 7))
        nowMillis = 1705363200000L
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-01-09.jsonl").writeText("delete\n")
        File(testLogDir, "mobile-2024-01-10.jsonl").writeText("keep boundary\n")
        File(testLogDir, "mobile-2024-01-15.jsonl").writeText("keep\n")
        File(testLogDir, "mobile-2024-01-16.jsonl").writeText("keep\n")

        repository.info("op", "trigger cleanup")

        assertFalse("2024-01-09 should be deleted", File(testLogDir, "mobile-2024-01-09.jsonl").exists())
        assertTrue("2024-01-10 should be kept at boundary", File(testLogDir, "mobile-2024-01-10.jsonl").exists())
        assertTrue("2024-01-15 should be kept", File(testLogDir, "mobile-2024-01-15.jsonl").exists())
        assertTrue("2024-01-16 should be kept", File(testLogDir, "mobile-2024-01-16.jsonl").exists())
    }

    @Test
    fun retentionPreservesNonLogFiles() = runTest {
        nowMillis = 1705363200000L
        settingsStore.write(settingsStore.read().copy(logRetentionDays = 1))
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-01-10.jsonl").writeText("old log\n")
        File(testLogDir, "important.txt").writeText("not a log\n")
        File(testLogDir, "other.dat").writeText("data\n")

        repository.info("op", "trigger cleanup")

        assertFalse("Old log file should be deleted", File(testLogDir, "mobile-2024-01-10.jsonl").exists())
        assertTrue("important.txt should be preserved", File(testLogDir, "important.txt").exists())
        assertTrue("other.dat should be preserved", File(testLogDir, "other.dat").exists())
    }

    @Test
    fun retentionPreservesDirectoryMatchingLogName() = runTest {
        nowMillis = 1705363200000L
        settingsStore.write(settingsStore.read().copy(logRetentionDays = 1))
        testLogDir.mkdirs()
        val dir = File(testLogDir, "mobile-2024-01-10.jsonl")
        dir.mkdirs()

        repository.info("op", "trigger cleanup")

        assertTrue("Directory matching mobile-*.jsonl should be preserved", dir.exists())
    }

    @Test
    fun logFilesReturnsStableSortedList() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("a\n")
        File(testLogDir, "mobile-2024-07-14.jsonl").writeText("b\n")
        File(testLogDir, "mobile-2024-07-15.jsonl").writeText("c\n")
        File(testLogDir, "not-a-log.txt").writeText("d\n")

        val files = repository.logFiles()

        assertEquals(3, files.size)
        assertEquals("mobile-2024-07-14.jsonl", files[0].name)
        assertEquals("mobile-2024-07-15.jsonl", files[1].name)
        assertEquals("mobile-2024-07-16.jsonl", files[2].name)
    }

    @Test
    fun logFilesExcludesNonLogs() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("a\n")
        File(testLogDir, ".hidden_file").writeText("secret\n")

        val files = repository.logFiles()

        assertEquals(1, files.size)
        assertEquals("mobile-2024-07-16.jsonl", files[0].name)
    }

    @Test
    fun logFilesReturnsEmptyWhenNoLogs() = runTest {
        val files = repository.logFiles()
        assertTrue(files.isEmpty())
    }

    @Test
    fun clearDeletesOnlyMobileLogs() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("log\n")
        File(testLogDir, "other.txt").writeText("data\n")
        File(testLogDir, "mobile-2024-07-15.jsonl").writeText("log\n")

        repository.clear()

        assertFalse("mobile-2024-07-16.jsonl should be deleted", File(testLogDir, "mobile-2024-07-16.jsonl").exists())
        assertFalse("mobile-2024-07-15.jsonl should be deleted", File(testLogDir, "mobile-2024-07-15.jsonl").exists())
        assertTrue("other.txt should be preserved", File(testLogDir, "other.txt").exists())
    }

    @Test
    fun clearNoOpWhenNoLogDir() = runTest {
        repository.clear()
    }

    @Test
    fun clearPreservesNonMatchingFiles() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "other.txt").writeText("data\n")

        repository.clear()

        assertTrue("other.txt should be preserved", File(testLogDir, "other.txt").exists())
    }

    @Test
    fun clearPreservesDirectoryMatchingLogName() = runTest {
        testLogDir.mkdirs()
        val dir = File(testLogDir, "mobile-2024-07-16.jsonl")
        dir.mkdirs()

        repository.clear()

        assertTrue("Directory matching mobile-*.jsonl should be preserved", dir.exists())
    }

    @Test
    fun snapshotFilesReturnsOnlyLogFilesSorted() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("a\n")
        File(testLogDir, "mobile-2024-07-14.jsonl").writeText("b\n")
        File(testLogDir, "mobile-2024-07-15.jsonl").writeText("c\n")
        File(testLogDir, "not-a-log.txt").writeText("d\n")

        val snapshots = repository.snapshotFiles()
        assertEquals(3, snapshots.size)
        assertEquals("mobile-2024-07-14.jsonl", snapshots[0].fileName)
        assertEquals("mobile-2024-07-15.jsonl", snapshots[1].fileName)
        assertEquals("mobile-2024-07-16.jsonl", snapshots[2].fileName)
    }

    @Test
    fun snapshotFilesContentIsSnapshotAtCallTime() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("original\n")

        val snapshots = repository.snapshotFiles()
        assertEquals(1, snapshots.size)
        assertEquals("original\n", snapshots[0].content)

        File(testLogDir, "mobile-2024-07-16.jsonl").appendText("appended\n")

        assertEquals("original\n", snapshots[0].content)
    }

    @Test
    fun snapshotFilesExcludesNonLogFiles() = runTest {
        testLogDir.mkdirs()
        File(testLogDir, "mobile-2024-07-16.jsonl").writeText("a\n")
        File(testLogDir, ".hidden").writeText("secret\n")

        val snapshots = repository.snapshotFiles()
        assertEquals(1, snapshots.size)
        assertEquals("mobile-2024-07-16.jsonl", snapshots[0].fileName)
    }

    @Test
    fun snapshotFilesEmptyWhenNoLogs() = runTest {
        val snapshots = repository.snapshotFiles()
        assertTrue(snapshots.isEmpty())
    }
}
