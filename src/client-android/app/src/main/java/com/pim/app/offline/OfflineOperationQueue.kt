package com.pim.app.offline

/**
 * Persistent offline operation queue contract.
 *
 * Implementation note: the authoritative offline storage lives in
 * [com.pim.app.data.AppDatabase] (mobile_usage_events, mobile_location_points, etc.)
 * and is drained by [com.pim.app.mobile.sync.MobileSyncCoordinator].
 * This interface exists so the `offline` package is not a 5-string stub and
 * callers can queue lightweight control operations (e.g. upload-retry) without
 * importing `mobile.sync` directly.
 *
 * A Room-backed implementation can be added later (OfflineOperationEntity + DAO)
 * without changing callers; the current [OnlineOperationGuard] provides an
 * in-memory bounded queue plus WorkManager scheduling as the minimal viable
 * persistent contract.
 */
interface OfflineOperationQueueContract {
    fun canQueueOffline(operationKind: String): Boolean
    fun enqueueOffline(operation: OfflineOperation): Boolean
    fun pendingOfflineCount(): Int
}
