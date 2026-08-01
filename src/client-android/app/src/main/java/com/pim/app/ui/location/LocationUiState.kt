package com.pim.app.ui.location

import com.pim.app.location.LocationSnapshot

data class LocationUiState(
    val triggerLabel: String = "尚未开始",
    val phaseLabel: String = "空闲",
    val elapsedText: String = "0 秒",
    val deadlineText: String = "最长 30 秒",
    val bestLocation: LocationSnapshot? = null,
    val pendingUploadTotal: Int = 0,
    val pendingLocationPoints: Int = 0,
    val errorMessage: String? = null,
    val showStart: Boolean = true,
    val showCancel: Boolean = false,
    val showSubmit: Boolean = false,
    val showRestart: Boolean = false,
    val showOpenSettings: Boolean = false,
    val isSubmitting: Boolean = false,
    val manualStartEnabled: Boolean = true
)

internal fun mapToLocationUiState(
    acqState: com.pim.app.location.acquisition.LocationAcquisitionState,
    queueSnapshot: com.pim.app.status.QueueStatusSnapshot
): LocationUiState {
    val phase = acqState.phase
    val triggerType = acqState.triggerType
    val isIdle = phase == com.pim.app.location.acquisition.AcquisitionPhase.Idle
    val isBusy = phase in setOf(
        com.pim.app.location.acquisition.AcquisitionPhase.Preparing,
        com.pim.app.location.acquisition.AcquisitionPhase.Acquiring,
        com.pim.app.location.acquisition.AcquisitionPhase.Evaluating
    )
    val isAwaitingManual = phase == com.pim.app.location.acquisition.AcquisitionPhase.AwaitingManualSubmit
    val isEnqueuing = phase == com.pim.app.location.acquisition.AcquisitionPhase.Enqueuing
    val isTerminal = phase in setOf(
        com.pim.app.location.acquisition.AcquisitionPhase.Completed,
        com.pim.app.location.acquisition.AcquisitionPhase.TimedOut,
        com.pim.app.location.acquisition.AcquisitionPhase.Failed,
        com.pim.app.location.acquisition.AcquisitionPhase.Cancelled
    )

    val triggerLabel = when (triggerType) {
        com.pim.app.location.acquisition.TriggerType.MANUAL -> "手动定位"
        com.pim.app.location.acquisition.TriggerType.AUTOMATIC -> "自动定位"
        null -> "尚未开始"
    }

    val phaseLabel = when (phase) {
        com.pim.app.location.acquisition.AcquisitionPhase.Idle -> "空闲"
        com.pim.app.location.acquisition.AcquisitionPhase.Preparing -> "准备中"
        com.pim.app.location.acquisition.AcquisitionPhase.Acquiring -> "采集位置中"
        com.pim.app.location.acquisition.AcquisitionPhase.Evaluating -> "评估中"
        com.pim.app.location.acquisition.AcquisitionPhase.AwaitingManualSubmit -> "等待提交"
        com.pim.app.location.acquisition.AcquisitionPhase.Enqueuing -> "提交中"
        com.pim.app.location.acquisition.AcquisitionPhase.Completed -> "已完成"
        com.pim.app.location.acquisition.AcquisitionPhase.TimedOut -> "超时"
        com.pim.app.location.acquisition.AcquisitionPhase.Failed -> "失败"
        com.pim.app.location.acquisition.AcquisitionPhase.Cancelled -> "已取消"
    }

    val elapsedText = if (phase != com.pim.app.location.acquisition.AcquisitionPhase.Idle && acqState.elapsedMs > 0) {
        "%.1f".format(acqState.elapsedMs / 1000.0) + " 秒"
    } else {
        "0 秒"
    }

    val errorMessage = acqState.errorReason
    val isPrecheckError = errorMessage != null && isIdle

    val manualStartEnabled = !(triggerType == com.pim.app.location.acquisition.TriggerType.AUTOMATIC && isBusy)

    return LocationUiState(
        triggerLabel = triggerLabel,
        phaseLabel = phaseLabel,
        elapsedText = elapsedText,
        deadlineText = "最长 30 秒",
        bestLocation = acqState.bestLocation,
        pendingUploadTotal = queueSnapshot.pendingUploadTotal,
        pendingLocationPoints = queueSnapshot.pendingLocationPoints,
        errorMessage = errorMessage,
        showStart = (isIdle && triggerType == null) ||
            (triggerType == com.pim.app.location.acquisition.TriggerType.AUTOMATIC && isBusy),
        showCancel = isBusy,
        showSubmit = isAwaitingManual,
        showRestart = isAwaitingManual || isTerminal,
        showOpenSettings = isPrecheckError,
        isSubmitting = isEnqueuing,
        manualStartEnabled = manualStartEnabled
    )
}
