package com.pim.app.location.liveupdate

import com.pim.app.location.acquisition.TriggerType

data class LocationLiveUpdateContent(
    val sessionId: String,
    val triggerType: TriggerType,
    val elapsedSeconds: Long,
    val accuracyMeters: Float?,
    val providerLabel: String
)
