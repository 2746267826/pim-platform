package com.pim.app.offline

import android.content.Context
import androidx.work.WorkManager
import com.pim.app.mobile.sync.MobileSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline queueable operation descriptor.
 * Real persistence is done by [com.pim.app.data.AppDatabase] (mobile_* tables)
 * and upload is performed by [com.pim.app.mobile.sync.MobileSyncCoordinator] via
 * [com.pim.app.mobile.sync.MobileSyncWorker]. This guard is the package-level
 * entry point so callers do not directly depend on mobile/sync internals.
 */
data class OfflineOperation(
    val operationKind: String,
    val payloadJson: String? = null,
    val enqueuedAtUtc: Long = System.currentTimeMillis()
)

interface OfflineOperationQueue {
    fun canQueue(operationKind: String): Boolean
    fun enqueue(operation: OfflineOperation): Boolean
    fun pendingCount(): Int
}

/**
 * Guard that decides whether an operation can be queued offline.
 * Previously a 5-string in-memory set; now it also exposes a persistent
 * queue contract and WorkManager scheduling, delegating actual sync to
 * [com.pim.app.mobile.sync.MobileSyncCoordinator].
 */
@Singleton
class OnlineOperationGuard @Inject constructor(
    @ApplicationContext private val context: Context? = null,
    private val scheduler: MobileSyncScheduler? = null
) {

    // Secondary constructor for unit tests without Hilt
    constructor() : this(context = null, scheduler = null)

    private val offlineQueueableOperations = setOf(
        "collection-upload",
        "android-location",
        "android-usage",
        "device-state",
        "upload-retry"
    )

    // In-memory fallback queue for callers that do not go through Room directly.
    // Bounded to avoid unbounded growth if sync is stuck.
    private val inMemoryQueue = ArrayDeque<OfflineOperation>()
    private val maxInMemoryQueueSize = 500
    private val lock = Any()

    fun canQueueOffline(operationKind: String): Boolean {
        return operationKind.trim() in offlineQueueableOperations
    }

    fun requiresOnline(operationKind: String): Boolean {
        return !canQueueOffline(operationKind)
    }

    /**
     * Enqueue an operation for offline delivery. Returns false if the operation
     * kind is not queueable or the in-memory buffer is full.
     * Real data (location/usage) is persisted via Room by callers; this method
     * ensures a [MobileSyncScheduler] work is enqueued so delivery resumes when
     * network returns.
     */
    fun enqueueOffline(operation: OfflineOperation): Boolean {
        val kind = operation.operationKind.trim()
        if (kind !in offlineQueueableOperations) return false
        synchronized(lock) {
            if (inMemoryQueue.size >= maxInMemoryQueueSize) return false
            inMemoryQueue.addLast(operation.copy(operationKind = kind))
        }
        scheduleSync()
        return true
    }

    fun pendingCount(): Int = synchronized(lock) { inMemoryQueue.size }

    fun peekAll(): List<OfflineOperation> = synchronized(lock) { inMemoryQueue.toList() }

    fun drainAll(): List<OfflineOperation> = synchronized(lock) {
        val copy = inMemoryQueue.toList()
        inMemoryQueue.clear()
        copy
    }

    private fun scheduleSync() {
        // Prefer MobileSyncScheduler (which configures constraints); fallback to direct WorkManager.
        val s = scheduler
        if (s != null) {
            try {
                s.enqueueNow()
                return
            } catch (_: Exception) {
                // fall through to WorkManager direct
            }
        }
        val ctx = context ?: return
        try {
            val wm = WorkManager.getInstance(ctx)
            // Fallback when scheduler is unavailable (e.g. in unit tests or early init): enqueue MobileSyncWorker directly.
            val request = androidx.work.OneTimeWorkRequestBuilder<com.pim.app.mobile.sync.MobileSyncWorker>()
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniqueWork(MobileSyncScheduler.NOW_NAME, androidx.work.ExistingWorkPolicy.KEEP, request)
        } catch (_: Exception) {
        }
    }
}
