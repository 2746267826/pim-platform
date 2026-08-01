package com.pim.app.location

import com.pim.app.data.MobileLocationDroppedDiagnosticEntity
import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.data.MobileLocationPolicyTransitionEntity
import com.pim.app.data.PimDatabaseMigrations
import com.pim.app.location.policy.LocationPolicyMode
import com.pim.app.location.policy.PolicyDecision
import com.pim.app.location.quality.QualityAcceptedLocation
import com.pim.app.location.quality.RawLocationFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQueueMappingTest {
    private fun acceptedLocation(): QualityAcceptedLocation = QualityAcceptedLocation(
        fix = RawLocationFix(
            latitude = 31.230416,
            longitude = 121.473701,
            horizontalAccuracyMeters = 18f,
            altitudeMeters = null,
            provider = "gps",
            recordedAtMillis = 1_000L,
            policyMode = LocationPolicyMode.ScheduleLowFrequency.name,
            scheduleLowFrequency = true,
            motionSignal = "Still"
        ),
        altitudeMeters = null,
        acceptedAtMillis = 16_000L,
        qualityFlags = setOf("altitude-missing-timeout")
    )

    @Test
    fun acceptedLocationPreservesManualSource() {
        val accepted = acceptedLocation()
        val entity = MobileLocationPointEntity.fromAccepted(accepted, rawJson = "{}", source = "manual")
        assertEquals("manual", entity.source)
    }

    @Test
    fun acceptedLocationPreservesAutomaticSource() {
        val accepted = acceptedLocation()
        val entity = MobileLocationPointEntity.fromAccepted(accepted, rawJson = "{}", source = "auto")
        assertEquals("auto", entity.source)
    }

    @Test
    fun acceptedLocationStoresPolicyAndNullAltitudeFlag() {
        val accepted = QualityAcceptedLocation(
            fix = RawLocationFix(
                latitude = 31.230416,
                longitude = 121.473701,
                horizontalAccuracyMeters = 18f,
                altitudeMeters = null,
                provider = "gps",
                recordedAtMillis = 1_000L,
                policyMode = LocationPolicyMode.ScheduleLowFrequency.name,
                scheduleLowFrequency = true,
                motionSignal = "Still"
            ),
            altitudeMeters = null,
            acceptedAtMillis = 16_000L,
            qualityFlags = setOf("altitude-missing-timeout")
        )

        val entity = MobileLocationPointEntity.fromAccepted(accepted, rawJson = "{}")

        assertEquals(LocationPolicyMode.ScheduleLowFrequency.name, entity.policyMode)
        assertTrue(entity.scheduleLowFrequency)
        assertNull(entity.altitudeMeters)
        assertTrue(entity.qualityFlags.contains("altitude-missing-timeout"))
        assertEquals(18f, entity.accuracyMeters)
        assertEquals(16_000L, entity.submittedAtUtc)
        assertEquals("Still", entity.motionState)
    }

    @Test
    fun droppedDiagnosticStoresReasonAndPolicyMetadata() {
        val fix = RawLocationFix(
            latitude = 31.230416,
            longitude = 121.473701,
            horizontalAccuracyMeters = 50f,
            altitudeMeters = null,
            provider = "fused",
            recordedAtMillis = 3_000L,
            policyMode = LocationPolicyMode.PowerSavingNormal.name,
            scheduleLowFrequency = false,
            motionSignal = "Walking"
        )

        val entity = MobileLocationDroppedDiagnosticEntity.fromDropped(
            fix = fix,
            reason = "horizontal-accuracy-too-low",
            createdAtUtc = 4_000L
        )

        assertEquals(3_000L, entity.recordedAtUtc)
        assertEquals("fused", entity.provider)
        assertEquals(50f, entity.accuracyMeters)
        assertEquals(LocationPolicyMode.PowerSavingNormal.name, entity.policyMode)
        assertEquals("horizontal-accuracy-too-low", entity.reason)
        assertEquals(4_000L, entity.createdAtUtc)
    }

    @Test
    fun policyTransitionStoresModeNamesAndReason() {
        val decision = PolicyDecision(
            mode = LocationPolicyMode.MovementRecovery,
            requestIntervalMillis = 60_000L,
            nextExpectedLocationAtMillis = 70_000L,
            reason = "日程期间位置变化超过 100 米",
            scheduleLowFrequency = false
        )

        val entity = MobileLocationPolicyTransitionEntity.fromDecision(
            fromMode = LocationPolicyMode.ScheduleLowFrequency,
            decision = decision,
            occurredAtUtc = 10_000L
        )

        assertEquals(LocationPolicyMode.ScheduleLowFrequency.name, entity.fromMode)
        assertEquals(LocationPolicyMode.MovementRecovery.name, entity.toMode)
        assertEquals("日程期间位置变化超过 100 米", entity.reason)
        assertEquals(10_000L, entity.occurredAtUtc)
    }

    @Test
    fun databaseMigrationTwoToThreeIsRegistered() {
        assertTrue(PimDatabaseMigrations.ALL.any { it.startVersion == 2 && it.endVersion == 3 })
    }
}
