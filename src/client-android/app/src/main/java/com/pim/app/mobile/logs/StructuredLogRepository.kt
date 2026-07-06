package com.pim.app.mobile.logs

import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileLogEntity
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StructuredLogRepository @Inject constructor(
    private val database: AppDatabase
) {
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

    private suspend fun write(
        level: String,
        operation: String,
        message: String,
        details: Map<String, Any?>,
        throwable: Throwable? = null
    ) {
        val nowUtc = System.currentTimeMillis()
        val rawJson = JSONObject()
            .put("operation", operation)
            .put("message", message)
            .put("details", details.toJsonObject())
            .put("occurredAtUtc", nowUtc)
            .apply {
                if (throwable != null) {
                    put("throwable", throwable.stackTraceToString())
                }
            }
            .toString()

        val log = MobileLogEntity(
            level = level,
            tag = operation,
            message = message,
            throwable = throwable?.stackTraceToString(),
            occurredAtUtc = nowUtc,
            source = "android",
            collectedAtUtc = nowUtc,
            rawJson = rawJson
        )

        database.mobileDataDao().insertLogs(listOf(log))

        when (level) {
            "error" -> Timber.e(throwable, "[$operation] $message")
            "warn" -> Timber.w("[$operation] $message")
            "debug" -> Timber.d("[$operation] $message")
            else -> Timber.i("[$operation] $message")
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
