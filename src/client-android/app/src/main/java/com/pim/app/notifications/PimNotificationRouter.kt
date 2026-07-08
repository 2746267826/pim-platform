package com.pim.app.notifications

sealed class NotificationRoute {
    data object ExecuteOnline : NotificationRoute()
    data class OpenDetail(val detailUrl: String) : NotificationRoute()
    data object RetryWhenOnline : NotificationRoute()
}

class PimNotificationRouter {
    private val highRiskLevels = setOf(
        "L2PimFactChange",
        "L3ExternalSourceOrWriteback",
        "L4BatchOrDestructiveGovernance",
        "Medium",
        "High"
    )

    fun route(
        action: String,
        riskLevel: String,
        confirmationId: String? = null,
        relatedObjectType: String? = null,
        relatedObjectId: String? = null,
        isOnline: Boolean = true
    ): NotificationRoute {
        if (riskLevel in highRiskLevels) {
            return NotificationRoute.OpenDetail(detailUrl(confirmationId, relatedObjectType, relatedObjectId))
        }

        if (!isOnline) {
            return NotificationRoute.RetryWhenOnline
        }

        val normalizedAction = action.trim().lowercase()
        return when (normalizedAction) {
            "dismiss", "snooze", "open", "complete" -> NotificationRoute.ExecuteOnline
            else -> NotificationRoute.OpenDetail("/confirmations")
        }
    }

    private fun detailUrl(
        confirmationId: String?,
        relatedObjectType: String?,
        relatedObjectId: String?
    ): String {
        if (!confirmationId.isNullOrBlank()) return "/confirmations/$confirmationId"
        if (!relatedObjectType.isNullOrBlank() && !relatedObjectId.isNullOrBlank()) {
            return "/audit/$relatedObjectType/$relatedObjectId"
        }
        return "/confirmations"
    }
}
