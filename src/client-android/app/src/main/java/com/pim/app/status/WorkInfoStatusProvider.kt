package com.pim.app.status

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.pim.app.mobile.sync.MobileSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StatusWorkInfos(
    val periodic: List<WorkInfo>,
    val immediate: List<WorkInfo>
)

@Singleton
class WorkInfoStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val syncWorkInfos: Flow<StatusWorkInfos> = combine(
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(MobileSyncScheduler.PERIODIC_NAME),
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(MobileSyncScheduler.NOW_NAME)
    ) { periodic, now ->
        StatusWorkInfos(periodic = periodic, immediate = now)
    }
}
