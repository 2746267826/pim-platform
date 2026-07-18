package com.pim.app.schedule

import com.pim.app.location.policy.ScheduleWindow
import com.pim.core.models.EventResponse
import com.pim.core.network.ApiService
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

enum class ScheduleCacheFreshness { Fresh, Stale, Missing }

enum class ScheduleRefreshErrorKind { Authentication, Network, Server, Cache }

data class ScheduleCacheSnapshot(
    val serverIdentity: String,
    val windows: List<ScheduleWindow>,
    val freshness: ScheduleCacheFreshness,
    val lastAttemptAtMillis: Long?,
    val lastSuccessAtMillis: Long?,
    val lastError: String?,
    val errorKind: ScheduleRefreshErrorKind?
)

object ScheduleWindowSelector {
    fun current(windows: List<ScheduleWindow>, nowMillis: Long): ScheduleWindow? {
        return windows.firstOrNull { window ->
            nowMillis >= window.startsAtMillis &&
                nowMillis < window.endsAtMillis
        }
    }

    fun upcoming(
        windows: List<ScheduleWindow>,
        nowMillis: Long,
        limit: Int = 10
    ): List<ScheduleWindow> {
        return windows
            .filter { it.startsAtMillis > nowMillis }
            .sortedBy { it.startsAtMillis }
            .take(limit)
    }
}

@Singleton
class ScheduleWindowRepository @Inject constructor(
    private val apiService: ApiService,
    private val cacheStore: ScheduleCacheStore,
    private val serverSettingsStore: ServerSettingsStore
) {
    private val _snapshot = MutableStateFlow(
        ScheduleCacheSnapshot(
            serverIdentity = "",
            windows = emptyList(),
            freshness = ScheduleCacheFreshness.Missing,
            lastAttemptAtMillis = null,
            lastSuccessAtMillis = null,
            lastError = null,
            errorKind = null
        )
    )
    val snapshot: StateFlow<ScheduleCacheSnapshot> = _snapshot.asStateFlow()

    private val mutex = Mutex()
    private val inFlightMap = mutableMapOf<String, CompletableDeferred<ScheduleCacheSnapshot>>()

    private fun isWithinWindow(nowMillis: Long, lastMillis: Long): Boolean =
        nowMillis >= lastMillis && nowMillis - lastMillis < FRESHNESS_WINDOW_MILLIS

    suspend fun refreshIfStale(
        force: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleCacheSnapshot {
        val identity = resolveIdentity()
        if (identity == null) {
            val invalidSnapshot = ScheduleCacheSnapshot(
                serverIdentity = "",
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Missing,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = null,
                lastError = "API 地址未配置或无效",
                errorKind = ScheduleRefreshErrorKind.Server
            )
            mutex.withLock { _snapshot.value = invalidSnapshot }
            return invalidSnapshot
        }

        if (!force) {
            val memory = mutex.withLock {
                ensureIdentityLocked(identity)
                _snapshot.value
            }
            if (memory.serverIdentity == identity &&
                memory.errorKind != null &&
                memory.lastAttemptAtMillis != null &&
                isWithinWindow(nowMillis, memory.lastAttemptAtMillis)
            ) {
                return memory
            }

            val outcome = cacheStore.readOutcome(identity)
            if (outcome is ScheduleCacheStore.CacheReadResult.Found) {
                val doc = outcome.document

                if (doc.lastSuccessAtMillis != null &&
                    doc.lastError == null &&
                    isWithinWindow(nowMillis, doc.lastSuccessAtMillis)
                ) {
                    val freshSnapshot = buildFromCache(doc, identity, ScheduleCacheFreshness.Fresh)
                    publishIfCurrent(identity, freshSnapshot)
                    return freshSnapshot
                }

                if (doc.lastAttemptAtMillis != null &&
                    isWithinWindow(nowMillis, doc.lastAttemptAtMillis)
                ) {
                    val freshness = if (doc.lastSuccessAtMillis != null)
                        ScheduleCacheFreshness.Stale else ScheduleCacheFreshness.Missing
                    val throttleSnapshot = buildFromCache(doc, identity, freshness)
                    publishIfCurrent(identity, throttleSnapshot)
                    return throttleSnapshot
                }
            }
            // Corrupt falls through to network single-flight refresh.
        } else {
            mutex.withLock { ensureIdentityLocked(identity) }
        }

        return singleFlight(identity, nowMillis)
    }

    private suspend fun singleFlight(
        identity: String,
        nowMillis: Long
    ): ScheduleCacheSnapshot {
        val deferred = CompletableDeferred<ScheduleCacheSnapshot>()

        mutex.withLock {
            ensureIdentityLocked(identity)
            val existing = inFlightMap[identity]
            if (existing != null) {
                return@withLock existing
            }
            inFlightMap[identity] = deferred
            null
        }?.let { return it.await() }

        try {
            val result = doRefresh(identity, nowMillis)
            deferred.complete(result)
            publishIfCurrent(identity, result)
            return result
        } catch (e: CancellationException) {
            deferred.completeExceptionally(e)
            throw e
        } catch (e: Exception) {
            val errorResult = handleRefreshError(e, identity, nowMillis)
            deferred.complete(errorResult)
            publishIfCurrent(identity, errorResult)
            return errorResult
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            mutex.withLock {
                if (inFlightMap[identity] === deferred) inFlightMap.remove(identity)
            }
        }
    }

    fun snapshotForCurrentServer(): ScheduleCacheSnapshot {
        val identity = resolveIdentity()
        val current = _snapshot.value
        if (identity == null) {
            if (current.serverIdentity == "" && current.freshness == ScheduleCacheFreshness.Missing) {
                return current
            }
            val invalid = ScheduleCacheSnapshot(
                serverIdentity = "",
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Missing,
                lastAttemptAtMillis = current.lastAttemptAtMillis,
                lastSuccessAtMillis = null,
                lastError = "API 地址未配置或无效",
                errorKind = ScheduleRefreshErrorKind.Server
            )
            _snapshot.value = invalid
            return invalid
        }
        if (current.serverIdentity != identity) {
            val cleared = ScheduleCacheSnapshot(
                serverIdentity = identity,
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Missing,
                lastAttemptAtMillis = null,
                lastSuccessAtMillis = null,
                lastError = null,
                errorKind = null
            )
            _snapshot.value = cleared
            return cleared
        }
        return current
    }

    private fun ensureIdentityLocked(identity: String) {
        val current = _snapshot.value
        if (current.serverIdentity != identity) {
            _snapshot.value = ScheduleCacheSnapshot(
                serverIdentity = identity,
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Missing,
                lastAttemptAtMillis = null,
                lastSuccessAtMillis = null,
                lastError = null,
                errorKind = null
            )
        }
    }

    private suspend fun publishIfCurrent(identity: String, snapshot: ScheduleCacheSnapshot) {
        mutex.withLock {
            // Do not reset identity here: a slower older flight must not clobber a newer identity.
            if (_snapshot.value.serverIdentity == identity) {
                _snapshot.value = snapshot
            }
        }
    }

    private suspend fun doRefresh(
        identity: String,
        nowMillis: Long
    ): ScheduleCacheSnapshot {
        val startMillis = nowMillis - QUERY_RANGE_MILLIS
        val endMillis = nowMillis + QUERY_FUTURE_MILLIS
        val startIso = Instant.ofEpochMilli(startMillis).toString()
        val endIso = Instant.ofEpochMilli(endMillis).toString()

        val response = apiService.getEvents(start = startIso, end = endIso)

        if (response.code != 0) {
            return buildFailureSnapshot(
                identity = identity,
                nowMillis = nowMillis,
                errorKind = ScheduleRefreshErrorKind.Server,
                errorMessage = "服务器暂时不可用",
                startMillis = startMillis,
                endMillis = endMillis
            )
        }

        val events = response.data.orEmpty()
        val windows = mapEvents(events)
        val cacheWindows = windows.map { it.toCacheWindow() }

        val wrote = writeCacheBestEffort(
            identity,
            ScheduleCacheDocument(
                windows = cacheWindows,
                rangeStartMillis = startMillis,
                rangeEndMillis = endMillis,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = nowMillis,
                lastError = null,
                lastErrorKind = null
            )
        )

        return if (wrote) {
            ScheduleCacheSnapshot(
                serverIdentity = identity,
                windows = windows,
                freshness = ScheduleCacheFreshness.Fresh,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = nowMillis,
                lastError = null,
                errorKind = null
            )
        } else {
            ScheduleCacheSnapshot(
                serverIdentity = identity,
                windows = windows,
                freshness = ScheduleCacheFreshness.Fresh,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = nowMillis,
                lastError = "本地日程缓存不可用",
                errorKind = ScheduleRefreshErrorKind.Cache
            )
        }
    }

    private fun handleRefreshError(
        error: Exception,
        identity: String,
        nowMillis: Long
    ): ScheduleCacheSnapshot {
        val (errorKind, errorMessage) = classifyError(error)
        return buildFailureSnapshot(
            identity = identity,
            nowMillis = nowMillis,
            errorKind = errorKind,
            errorMessage = errorMessage,
            startMillis = nowMillis - QUERY_RANGE_MILLIS,
            endMillis = nowMillis + QUERY_FUTURE_MILLIS
        )
    }

    private fun buildFailureSnapshot(
        identity: String,
        nowMillis: Long,
        errorKind: ScheduleRefreshErrorKind,
        errorMessage: String,
        startMillis: Long,
        endMillis: Long
    ): ScheduleCacheSnapshot {
        val outcome = cacheStore.readOutcome(identity)
        if (outcome is ScheduleCacheStore.CacheReadResult.Corrupt) {
            // Prefer local cache damage over network/server classification when file is unreadable.
            return ScheduleCacheSnapshot(
                serverIdentity = identity,
                windows = emptyList(),
                freshness = ScheduleCacheFreshness.Missing,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = null,
                lastError = "本地日程缓存不可用",
                errorKind = ScheduleRefreshErrorKind.Cache
            )
        }

        val doc = if (outcome is ScheduleCacheStore.CacheReadResult.Found) outcome.document else null
        val windows = doc?.windows?.map { it.toScheduleWindow() }.orEmpty()
        val freshness = if (doc?.lastSuccessAtMillis != null)
            ScheduleCacheFreshness.Stale else ScheduleCacheFreshness.Missing

        writeCacheBestEffort(
            identity,
            ScheduleCacheDocument(
                windows = doc?.windows.orEmpty(),
                rangeStartMillis = doc?.rangeStartMillis ?: startMillis,
                rangeEndMillis = doc?.rangeEndMillis ?: endMillis,
                lastAttemptAtMillis = nowMillis,
                lastSuccessAtMillis = doc?.lastSuccessAtMillis,
                lastError = errorMessage,
                lastErrorKind = errorKind.name
            )
        )

        return ScheduleCacheSnapshot(
            serverIdentity = identity,
            windows = windows,
            freshness = freshness,
            lastAttemptAtMillis = nowMillis,
            lastSuccessAtMillis = doc?.lastSuccessAtMillis,
            lastError = errorMessage,
            errorKind = errorKind
        )
    }

    private fun writeCacheBestEffort(identity: String, document: ScheduleCacheDocument): Boolean {
        return try {
            cacheStore.write(identity, document)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun classifyError(error: Exception): Pair<ScheduleRefreshErrorKind, String> {
        return when {
            error is HttpException -> when (error.code()) {
                401, 403 -> ScheduleRefreshErrorKind.Authentication to "登录状态已失效"
                in 500..599 -> ScheduleRefreshErrorKind.Server to "服务器暂时不可用"
                else -> ScheduleRefreshErrorKind.Server to "服务器暂时不可用"
            }
            error is IOException -> ScheduleRefreshErrorKind.Network to "网络不可用"
            error is kotlinx.serialization.SerializationException ->
                ScheduleRefreshErrorKind.Server to "服务器返回数据格式错误"
            else -> ScheduleRefreshErrorKind.Server to "服务器暂时不可用"
        }
    }

    private fun resolveIdentity(): String? {
        val url = serverSettingsStore.getBaseUrl()
        if (url.isBlank()) return null
        return runCatching {
            PimServerEndpoints.from(url).apiBaseUrl.toString()
        }.getOrNull()
    }

    private fun buildFromCache(
        doc: ScheduleCacheDocument,
        identity: String,
        freshness: ScheduleCacheFreshness
    ): ScheduleCacheSnapshot {
        return ScheduleCacheSnapshot(
            serverIdentity = identity,
            windows = doc.windows.map { it.toScheduleWindow() },
            freshness = freshness,
            lastAttemptAtMillis = doc.lastAttemptAtMillis,
            lastSuccessAtMillis = doc.lastSuccessAtMillis,
            lastError = doc.lastError,
            errorKind = doc.lastErrorKind?.let { enumValueOfOrNull<ScheduleRefreshErrorKind>(it) }
        )
    }

    suspend fun loadWindows(startMillis: Long, endMillis: Long): List<ScheduleWindow> {
        val response = apiService.getEvents(
            start = Instant.ofEpochMilli(startMillis).toString(),
            end = Instant.ofEpochMilli(endMillis).toString()
        )
        if (response.code != 0) {
            error(response.message.ifBlank { "加载日程失败" })
        }
        return mapEvents(response.data.orEmpty())
    }

    suspend fun currentWindow(windows: List<ScheduleWindow>, nowMillis: Long): ScheduleWindow? {
        return ScheduleWindowSelector.current(windows, nowMillis)
    }

    suspend fun upcomingWindows(windows: List<ScheduleWindow>, nowMillis: Long): List<ScheduleWindow> {
        return ScheduleWindowSelector.upcoming(windows, nowMillis)
    }

    companion object {
        private const val FRESHNESS_WINDOW_MILLIS = 15 * 60 * 1000L
        private const val QUERY_RANGE_MILLIS = 6 * 60 * 60 * 1000L
        private const val QUERY_FUTURE_MILLIS = 7 * 24 * 60 * 60 * 1000L

        fun mapEvents(events: List<EventResponse>): List<ScheduleWindow> {
            return events.mapNotNull { event ->
                val location = event.location?.trim().orEmpty()
                val startsAt = event.dtStart.toEpochMillisOrNull() ?: return@mapNotNull null
                val endsAt = event.dtEnd.toEpochMillisOrNull() ?: return@mapNotNull null
                ScheduleWindow(
                    id = event.id,
                    title = event.title,
                    locationText = location,
                    startsAtMillis = startsAt,
                    endsAtMillis = endsAt
                )
            }.sortedBy { it.startsAtMillis }
        }

        private fun String.toEpochMillisOrNull(): Long? {
            return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
        }

        internal fun ScheduleCacheWindow.toScheduleWindow(): ScheduleWindow = ScheduleWindow(
            id = id, title = title, locationText = locationText,
            startsAtMillis = startsAtMillis, endsAtMillis = endsAtMillis
        )

        internal fun ScheduleWindow.toCacheWindow(): ScheduleCacheWindow = ScheduleCacheWindow(
            id = id, title = title, locationText = locationText,
            startsAtMillis = startsAtMillis, endsAtMillis = endsAtMillis
        )

        internal inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? {
            return runCatching { enumValueOf<T>(name) }.getOrNull()
        }
    }
}
