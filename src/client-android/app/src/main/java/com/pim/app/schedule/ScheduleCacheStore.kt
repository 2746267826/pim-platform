package com.pim.app.schedule

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Serializable
data class ScheduleCacheWindow(
    val id: String,
    val title: String,
    val locationText: String,
    val startsAtMillis: Long,
    val endsAtMillis: Long
)

@Serializable
data class ScheduleCacheDocument(
    val windows: List<ScheduleCacheWindow>,
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val lastAttemptAtMillis: Long? = null,
    val lastSuccessAtMillis: Long? = null,
    val lastError: String? = null,
    val lastErrorKind: String? = null
)

class ScheduleCacheStore(
    private val json: Json,
    private val cacheDir: File
) {
    private val lock = Any()

    constructor(context: Context, json: Json) : this(json, File(context.filesDir, "schedule-cache"))

    internal constructor(cacheDir: File, json: Json) : this(json, cacheDir)

    internal sealed class CacheReadResult {
        data class Found(val document: ScheduleCacheDocument) : CacheReadResult()
        data object Missing : CacheReadResult()
        data object Corrupt : CacheReadResult()
    }

    internal fun readOutcome(serverIdentity: String): CacheReadResult {
        synchronized(lock) {
            val file = cacheFile(serverIdentity)
            if (!file.exists()) return CacheReadResult.Missing
            return try {
                CacheReadResult.Found(json.decodeFromString<ScheduleCacheDocument>(file.readText()))
            } catch (_: Exception) {
                CacheReadResult.Corrupt
            }
        }
    }

    fun read(serverIdentity: String): ScheduleCacheDocument? {
        val outcome = readOutcome(serverIdentity)
        return if (outcome is CacheReadResult.Found) outcome.document else null
    }

    fun write(serverIdentity: String, document: ScheduleCacheDocument) {
        synchronized(lock) {
            cacheDir.mkdirs()
            val file = cacheFile(serverIdentity)
            val tmpFile = File(cacheDir, "${hashIdentity(serverIdentity)}.json.tmp")
            try {
                val bytes = json.encodeToString(ScheduleCacheDocument.serializer(), document).toByteArray(Charsets.UTF_8)
                tmpFile.writeBytes(bytes)
                try {
                    Files.move(
                        tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(
                        tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } finally {
                tmpFile.delete()
            }
        }
    }

    fun clear(serverIdentity: String) {
        synchronized(lock) {
            val hash = hashIdentity(serverIdentity)
            File(cacheDir, "$hash.json").delete()
            File(cacheDir, "$hash.json.tmp").delete()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        }
    }

    internal fun cacheFile(serverIdentity: String): File =
        File(cacheDir, "${hashIdentity(serverIdentity)}.json")

    private fun hashIdentity(identity: String): String {
        val bytes = identity.trim().toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
