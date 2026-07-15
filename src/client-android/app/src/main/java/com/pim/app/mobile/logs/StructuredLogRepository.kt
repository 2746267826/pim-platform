package com.pim.app.mobile.logs

import android.content.Context
import com.pim.app.settings.TrackingSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StructuredLogEntry(
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String? = null,
    val occurredAtUtc: Long
)

@Singleton
class StructuredLogRepository internal constructor(
    @ApplicationContext private val context: Context,
    private val trackingSettingsStore: TrackingSettingsStore,
    private val nowMillis: () -> Long
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        trackingSettingsStore: TrackingSettingsStore
    ) : this(context, trackingSettingsStore, System::currentTimeMillis)

    private val mutex = Mutex()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun debug(
        operation: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) = write("debug", operation, message, details)

    suspend fun info(
        operation: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) = write("info", operation, message, details)

    suspend fun warn(
        operation: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) = write("warn", operation, message, details)

    suspend fun error(
        operation: String,
        message: String,
        throwable: Throwable? = null,
        details: Map<String, Any?> = emptyMap()
    ) = write("error", operation, message, details, throwable)

    suspend fun recent(limit: Int = 6): List<StructuredLogEntry> = withContext(Dispatchers.IO) {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.isDirectory) return@withContext emptyList()

        val files = logDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("mobile-") && it.name.endsWith(".jsonl") }
            ?.sortedByDescending { it.name }
            ?: return@withContext emptyList()

        val entries = mutableListOf<StructuredLogEntry>()
        for (file in files) {
            if (entries.size >= limit) break
            try {
                val lines = file.useLines { it.toList() }
                for (i in lines.indices.reversed()) {
                    if (entries.size >= limit) break
                    try {
                        val json = JSONObject(lines[i])
                        entries.add(
                            StructuredLogEntry(
                                level = json.optString("level", ""),
                                tag = json.optString("tag", json.optString("operation", "")),
                                message = json.optString("message", ""),
                                throwable = json.optString("throwable").takeIf { it.isNotEmpty() },
                                occurredAtUtc = json.optLong("occurredAtUtc", 0L)
                            )
                        )
                    } catch (_: Exception) {
                        // skip corrupt lines
                    }
                }
            } catch (_: Exception) {
                Timber.w("Failed to read log file: ${file.name}")
            }
        }
        entries.take(limit)
    }

    suspend fun logFiles(): List<File> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.isDirectory) return@withContext emptyList()
            logDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("mobile-") && it.name.endsWith(".jsonl") }
                ?.sortedBy { it.name }
                ?: emptyList()
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.isDirectory) return@withContext
            logDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("mobile-") && it.name.endsWith(".jsonl") }
                ?.forEach { it.delete() }
        }
    }

    private suspend fun write(
        level: String,
        operation: String,
        message: String,
        details: Map<String, Any?>,
        throwable: Throwable? = null
    ) = withContext(Dispatchers.IO) {
        val nowUtc = nowMillis()

        if (level == "debug" && !trackingSettingsStore.isVerboseLoggingEnabled(nowUtc)) {
            return@withContext
        }

        mutex.withLock {
            try {
                val json = JSONObject()
                    .put("level", level)
                    .put("tag", operation)
                    .put("message", message)
                    .put("details", details.toJsonObject())
                    .put("occurredAtUtc", nowUtc)
                    .put("source", "android")
                    .apply {
                        if (throwable != null) {
                            put("throwable", throwable.stackTraceToString())
                        }
                    }
                    .toString()

                val line = "$json\n"
                val datePart = dateFormat.format(Date(nowUtc))
                val logDir = File(context.filesDir, "logs")
                logDir.mkdirs()
                val file = File(logDir, "mobile-$datePart.jsonl")
                file.appendText(line, Charsets.UTF_8)

                cleanupOldFiles(trackingSettingsStore.read().logRetentionDays, nowUtc)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to write log to JSONL")
            }
        }

        when (level) {
            "error" -> Timber.e(throwable, "[$operation] $message")
            "warn" -> Timber.w("[$operation] $message")
            "debug" -> Timber.d("[$operation] $message")
            else -> Timber.i("[$operation] $message")
        }
    }

    private fun cleanupOldFiles(retentionDays: Int, nowUtc: Long) {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.isDirectory) return
        if (retentionDays <= 0) return
        val cutoffDate = dateFormat.format(
            Date(nowUtc - (retentionDays - 1).toLong() * 86_400_000L)
        )
        logDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("mobile-") && file.name.endsWith(".jsonl")) {
                val datePart = file.name.removePrefix("mobile-").removeSuffix(".jsonl")
                if (datePart < cutoffDate) {
                    file.delete()
                }
            }
        }
    }
}

private fun Map<String, Any?>.toJsonObject(): JSONObject {
    val json = JSONObject()
    forEach { (key, value) ->
        json.put(key, value ?: JSONObject.NULL)
    }
    return json
}
