package com.pim.app.offline

class OnlineOperationGuard {
    private val offlineQueueableOperations = setOf(
        "collection-upload",
        "android-location",
        "android-usage",
        "device-state",
        "upload-retry"
    )

    fun canQueueOffline(operationKind: String): Boolean {
        return operationKind.trim() in offlineQueueableOperations
    }

    fun requiresOnline(operationKind: String): Boolean {
        return !canQueueOffline(operationKind)
    }
}
