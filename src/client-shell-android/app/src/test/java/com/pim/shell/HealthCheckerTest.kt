package com.pim.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthCheckerTest {
    @Test fun `healthy returns normalized url`() {
        assertEquals("https://pim.example.com", HealthChecker { 200 }.check("pim.example.com"))
    }
    @Test fun `error status returns null`() {
        assertNull(HealthChecker { 500 }.check("https://pim.example.com"))
    }
    @Test fun `connection failure returns null`() {
        assertNull(HealthChecker { throw java.io.IOException("offline") }.check("https://pim.example.com"))
    }
    @Test fun `invalid address returns null without network call`() {
        var called = false
        assertNull(HealthChecker { called = true; 200 }.check("   "))
        assertEquals(false, called)
    }
}
