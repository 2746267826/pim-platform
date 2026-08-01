package com.pim.app.status

import com.pim.app.data.MobileDataDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class QueueStatusRepository internal constructor(
    private val locations: Flow<Int>,
    private val usageEvents: Flow<Int>,
    private val usageSummaries: Flow<Int>,
    private val appMetadata: Flow<Int>,
    private val deviceProfiles: Flow<Int>,
    private val syncBatches: Flow<Int>
) {
    @Inject
    constructor(dao: MobileDataDao) : this(
        dao.pendingLocationPointCount(),
        dao.pendingUsageEventCount(),
        dao.pendingUsageSummaryCount(),
        dao.pendingAppMetadataCount(),
        dao.pendingDeviceProfileCount(),
        dao.pendingSyncBatchCount()
    )

    fun observe(): Flow<QueueStatusSnapshot> = combine(
        combine(locations, usageEvents, usageSummaries, ::Triple),
        combine(appMetadata, deviceProfiles, syncBatches, ::Triple)
    ) { first, second ->
        QueueStatusSnapshot(
            pendingLocationPoints = first.first,
            pendingUsageEvents = first.second,
            pendingUsageSummaries = first.third,
            pendingAppMetadata = second.first,
            pendingDeviceProfile = second.second,
            pendingSyncBatches = second.third
        )
    }
}
