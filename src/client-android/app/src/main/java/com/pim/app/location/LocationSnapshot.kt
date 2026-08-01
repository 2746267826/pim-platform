package com.pim.app.location

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Float?,
    val provider: String,
    val source: String,
    val altitudeMeters: Double?,
    val speedMetersPerSecond: Float?,
    val bearingDegrees: Float?,
    val timeMillis: Long
)
