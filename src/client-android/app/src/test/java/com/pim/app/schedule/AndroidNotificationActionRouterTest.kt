package com.pim.app.schedule

import com.pim.app.notifications.NotificationRoute
import com.pim.app.notifications.PimNotificationRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidNotificationActionRouterTest {
    @Test
    fun lowRiskActionCanExecuteDirectly() {
        val result = PimNotificationRouter().route("dismiss", "L1LowRiskAction")

        assertEquals(NotificationRoute.ExecuteOnline, result)
    }

    @Test
    fun highRiskActionOpensDetail() {
        val result = PimNotificationRouter().route(
            action = "confirm",
            riskLevel = "L3ExternalSourceOrWriteback",
            confirmationId = "confirmation-1"
        )

        assertTrue(result is NotificationRoute.OpenDetail)
        assertEquals("/confirmations/confirmation-1", (result as NotificationRoute.OpenDetail).detailUrl)
    }
}
