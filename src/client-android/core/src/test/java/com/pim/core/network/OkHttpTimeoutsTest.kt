package com.pim.core.network

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpTimeoutsTest {
    @Test
    fun applyPimApiTimeoutsAllowsLargeMobileUploads() {
        val client = OkHttpClient.Builder()
            .applyPimApiTimeouts()
            .build()

        assertEquals(15_000, client.connectTimeoutMillis)
        assertEquals(60_000, client.readTimeoutMillis)
        assertEquals(60_000, client.writeTimeoutMillis)
        assertEquals(90_000, client.callTimeoutMillis)
    }
}
