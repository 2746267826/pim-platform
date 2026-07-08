package com.pim.app.notifications

import com.pim.core.models.ApiResponse
import com.pim.core.models.EndpointNotificationActionRequestDto
import com.pim.core.models.EndpointNotificationActionResponseDto
import com.pim.core.network.ApiService

class EndpointNotificationActionDispatcher(
    private val sender: suspend (
        String,
        EndpointNotificationActionRequestDto
    ) -> ApiResponse<EndpointNotificationActionResponseDto>
) {
    constructor(apiService: ApiService) : this(apiService::sendEndpointNotificationAction)

    suspend fun execute(
        deviceId: String,
        action: String,
        riskLevel: String,
        confirmationId: String?,
        relatedObjectType: String?,
        relatedObjectId: String?
    ): EndpointNotificationActionResponseDto? {
        val response = sender(
            deviceId,
            EndpointNotificationActionRequestDto(
                action = action,
                riskLevel = riskLevel,
                confirmationId = confirmationId,
                relatedObjectType = relatedObjectType,
                relatedObjectId = relatedObjectId
            )
        )
        return response.data
    }
}
