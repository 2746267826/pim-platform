package com.pim.app.location.liveupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationLiveUpdateCapabilityTest {

    @Test
    fun `isAvailable returns false when SDK_INT below 36`() {
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(35) {
            throw NoSuchFieldError("should not reach")
        }
        assertFalse(result)
    }

    @Test
    fun `major below 36 returns false without calling fullSdkCheck`() {
        var invoked = false
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(35) {
            invoked = true
            true
        }
        assertFalse(result)
        assertFalse(invoked)
    }

    @Test
    fun `major above 36 returns true without calling fullSdkCheck`() {
        var invoked = false
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(37) {
            invoked = true
            true
        }
        assertTrue(result)
        assertFalse(invoked)
    }

    @Test
    fun `major equals 36 delegates to fullSdkCheck when it returns true`() {
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(36) {
            true
        }
        assertTrue(result)
    }

    @Test
    fun `major equals 36 delegates to fullSdkCheck when it returns false`() {
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(36) {
            false
        }
        assertFalse(result)
    }

    @Test
    fun `linkage error from fullSdkCheck returns false`() {
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(36) {
            throw NoSuchFieldError("stub")
        }
        assertFalse(result)
    }

    @Test
    fun `classNotFound error from fullSdkCheck returns false`() {
        val result = LocationLiveUpdateCapability.supportsLiveUpdates(36) {
            throw NoClassDefFoundError("stub")
        }
        assertFalse(result)
    }

    @Test(expected = RuntimeException::class)
    fun `non linkage runtime exception from fullSdkCheck propagates`() {
        LocationLiveUpdateCapability.supportsLiveUpdates(36) {
            throw RuntimeException("not a linkage error")
        }
    }

    @Test
    fun `check delegates to isAvailable`() {
        val expected = LocationLiveUpdateCapability.isAvailable()
        assertEquals(expected, LocationLiveUpdateCapability.check())
    }

    @Test
    fun `supportsLiveUpdates is accessible within module`() {
        assertFalse(LocationLiveUpdateCapability.supportsLiveUpdates(35) { true })
    }
}
