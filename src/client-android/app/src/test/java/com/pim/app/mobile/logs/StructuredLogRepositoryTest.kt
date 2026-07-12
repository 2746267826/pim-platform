package com.pim.app.mobile.logs

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pim.app.data.AppDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
class StructuredLogRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: StructuredLogRepository
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "logs").deleteRecursively()
        repository = StructuredLogRepository(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        File(context.filesDir, "logs").deleteRecursively()
    }

    @Test
    fun writesJsonlFileOnInfo() = runTest {
        repository.info("test-op", "test message", mapOf("key" to "value"))

        val logDir = File(context.filesDir, "logs")
        val files = logDir.listFiles() ?: emptyArray()
        val jsonlFile = files.firstOrNull { it.name.endsWith(".jsonl") }
        assertTrue("JSONL file should exist", jsonlFile != null)

        val lines = jsonlFile!!.readLines()
        assertEquals(1, lines.size)

        val json = JSONObject(lines[0])
        assertEquals("info", json.getString("level"))
        assertEquals("test-op", json.getString("tag"))
        assertEquals("test message", json.getString("message"))
        assertTrue(json.has("details"))
        assertTrue(json.has("occurredAtUtc"))
        assertEquals("android", json.getString("source"))

        val roomCount = db.mobileDataDao().pendingLogCount().first()
        assertEquals("Room mobile_logs should remain 0", 0, roomCount)
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

        val logDir = File(context.filesDir, "logs")
        val jsonlFile = logDir.listFiles()!!.first { it.name.endsWith(".jsonl") }
        jsonlFile.appendText("not valid json\n")

        val recent = repository.recent(10)
        assertEquals(1, recent.size)
        assertEquals("valid-op", recent[0].tag)
    }
}
