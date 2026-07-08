package com.pim.app.location.policy

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun metersBetween(a: PolicyLocation, b: PolicyLocation): Double {
        val deltaLatitude = Math.toRadians(b.latitude - a.latitude)
        val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
        val startLatitude = Math.toRadians(a.latitude)
        val endLatitude = Math.toRadians(b.latitude)

        val haversine = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
            cos(startLatitude) * cos(endLatitude) *
            sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
        val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
        return EARTH_RADIUS_METERS * centralAngle
    }
}
