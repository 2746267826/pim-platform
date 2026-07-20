package com.pim.app.location.acquisition

import com.pim.app.location.LocationQueueRepository
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import com.pim.app.mobile.sync.MobileSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationAcquisitionModule {

    @Binds
    abstract fun bindLocationUpdateSource(
        implementation: FusedLocationUpdateSource
    ): LocationUpdateSource

    @Binds
    abstract fun bindLocationAcquisitionRunner(
        implementation: LocationAcquisitionEngine
    ): LocationAcquisitionRunner

    @Binds
    abstract fun bindLocationPrerequisiteChecker(
        implementation: AndroidLocationPrerequisiteChecker
    ): LocationPrerequisiteChecker
}

@Module
@InstallIn(SingletonComponent::class)
object LocationAcquisitionOperationsProvider {

    @Provides
    @Singleton
    @JvmStatic
    fun provideLocationAcquisitionOperations(
        repo: LocationQueueRepository,
        scheduler: MobileSyncScheduler
    ): LocationAcquisitionOperations = object : LocationAcquisitionOperations {
        override suspend fun enqueueAccepted(
            accepted: QualityAcceptedLocation,
            rawJson: String,
            source: String
        ) {
            repo.enqueueAccepted(accepted, rawJson, source)
        }

        override suspend fun recordDropped(fix: RawLocationFix, reason: String) {
            repo.recordDropped(fix, reason)
        }

        override fun scheduleSync() {
            scheduler.enqueueNow()
        }
    }
}
