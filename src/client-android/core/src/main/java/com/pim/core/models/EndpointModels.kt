package com.pim.core.models

import kotlinx.serialization.Serializable

@Serializable
data class EndpointNotificationActionRequestDto(
    val action: String,
    val riskLevel: String,
    val confirmationId: String? = null,
    val relatedObjectType: String? = null,
    val relatedObjectId: String? = null
)

@Serializable
data class EndpointNotificationActionResponseDto(
    val result: String,
    val detailUrl: String? = null,
    val message: String? = null
)
