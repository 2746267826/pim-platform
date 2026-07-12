package com.pim.app.mobile.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pim.app.settings.TrackingSettings
import com.pim.app.settings.TrackingSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingSettingsStore: TrackingSettingsStore
) {
    fun ensurePeriodic() {
        val settings = trackingSettingsStore.read()
        val networkType = resolvePeriodicNetworkType(settings)
        val request = buildPeriodicRequest(networkType)
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun enqueueNow(allowMeteredOnce: Boolean = false) {
        val settings = trackingSettingsStore.read()
        val networkType = resolveImmediateNetworkType(settings, allowMeteredOnce)
        val request = buildImmediateRequest(networkType, allowMeteredOnce)
        val policy = resolveExistingWorkPolicy(allowMeteredOnce)
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW_NAME, policy, request)
    }

    fun cancelOldWork() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("pim_upload")
        workManager.cancelUniqueWork("pim_location_upload")
        workManager.cancelUniqueWork("pim_mobile_background_sync")
        workManager.cancelUniqueWork("pim_endpoint_upload")
    }

    companion object {
        const val PERIODIC_NAME = "pim_mobile_sync_periodic"
        const val NOW_NAME = "pim_mobile_sync_now"

        fun resolvePeriodicNetworkType(settings: TrackingSettings): NetworkType {
            return if (settings.syncOnUnmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        }

        fun resolveImmediateNetworkType(settings: TrackingSettings, allowMeteredOnce: Boolean): NetworkType {
            return if (settings.syncOnUnmeteredOnly && !allowMeteredOnce) NetworkType.UNMETERED else NetworkType.CONNECTED
        }

        fun buildImmediateInputData(allowMeteredOnce: Boolean): Data {
            return workDataOf("allow_metered_once" to allowMeteredOnce)
        }

        fun buildPeriodicRequest(networkType: NetworkType): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()
            return PeriodicWorkRequestBuilder<MobileSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }

        fun buildImmediateRequest(networkType: NetworkType, allowMeteredOnce: Boolean): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()
            val inputData = buildImmediateInputData(allowMeteredOnce)
            return OneTimeWorkRequestBuilder<MobileSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build()
        }

        fun resolveExistingWorkPolicy(allowMeteredOnce: Boolean): ExistingWorkPolicy {
            return if (allowMeteredOnce) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        }
    }
}
