package com.pim.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ThrowableMessagesTest {
    @Test
    fun toCauseChainMessageIncludesNestedCauses() {
        val error = IllegalArgumentException(
            "Unable to create @Body converter",
            IllegalStateException("Serializer for class LoginRequest is not found")
        )

        assertEquals(
            "IllegalArgumentException: Unable to create @Body converter -> " +
                "IllegalStateException: Serializer for class LoginRequest is not found",
            error.toCauseChainMessage()
        )
    }
}
