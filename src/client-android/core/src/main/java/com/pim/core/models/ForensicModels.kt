package com.pim.core.models

import kotlinx.serialization.Serializable

/**
 * 取证事件批量上报（REQ-5 / 工单 §7.2）。
 *
 * 走独立通道，不改动也复用既有 `mobile_sync_batches` 语义（工单 §7.3）。
 */
@Serializable
data class MobileForensicsUploadRequest(
    val deviceId: String,
    val batchId: String? = null,
    val events: List<MobileForensicEventDto> = emptyList(),
    val droppedReasonSummaries: List<MobileDroppedReasonSummaryDto> = emptyList()
)

@Serializable
data class MobileForensicEventDto(
    val clientItemKey: String,
    val eventType: String,
    val occurredAtUtc: String,
    val payloadJson: String? = null
)

/** 按「设备本地日 + 原因」聚合的定位丢弃统计快照（REQ-9）。 */
@Serializable
data class MobileDroppedReasonSummaryDto(
    val localDate: String,
    val reason: String,
    val count: Int
)

@Serializable
data class MobileForensicsIngestResponse(
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0,
    val acceptedKeys: List<String> = emptyList(),
    val skippedKeys: List<String> = emptyList(),
    val rejectedKeys: List<String> = emptyList(),
    val acceptedDroppedReasonCount: Int = 0
)
