package com.pim.app.location

import com.pim.app.data.MobileLocationPointEntity
import com.pim.app.location.quality.QualityAcceptedLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Trajectory compression for high-frequency location sampling.
 * - Time/distance clustering: points closer than 5m within 30s are considered stationary noise.
 * - Douglas-Peucker: reduces polyline while preserving shape, epsilon 8m by default.
 */
@Singleton
class TrajectoryCompressor @Inject constructor() {

    /**
     * Returns true if [current] should be dropped because it is too close in
     * time and space to [last] (stationary cluster).
     * Thresholds: distance < 5m && timeDelta < 30s.
     */
    fun shouldClusterDrop(
        last: QualityAcceptedLocation,
        current: QualityAcceptedLocation
    ): Boolean {
        val dt = abs(current.fix.recordedAtMillis - last.fix.recordedAtMillis)
        if (dt >= CLUSTER_INTERVAL_MILLIS) return false
        val dist = haversineMeters(
            last.fix.latitude, last.fix.longitude,
            current.fix.latitude, current.fix.longitude
        )
        return dist < CLUSTER_DISTANCE_METERS
    }

    fun shouldClusterDropEntities(
        last: MobileLocationPointEntity,
        current: MobileLocationPointEntity
    ): Boolean {
        val dt = abs(current.recordedAtUtc - last.recordedAtUtc)
        if (dt >= CLUSTER_INTERVAL_MILLIS) return false
        val dist = haversineMeters(last.latitude, last.longitude, current.latitude, current.longitude)
        return dist < CLUSTER_DISTANCE_METERS
    }

    /**
     * Douglas-Peucker compression on a batch ordered by recordedAtUtc.
     * Keeps first and last point, recursively keeps points with perpendicular
     * distance > epsilon. Uses equirectangular approximation for small areas.
     */
    fun compress(
        points: List<MobileLocationPointEntity>,
        epsilonMeters: Double = DOUGLAS_EPSILON_METERS
    ): List<MobileLocationPointEntity> {
        if (points.size <= 2) return points
        val keep = BooleanArray(points.size) { false }
        keep[0] = true
        keep[points.size - 1] = true
        douglasPeucker(points, 0, points.size - 1, epsilonMeters, keep)
        return points.filterIndexed { idx, _ -> keep[idx] }
    }

    /**
     * Streaming helper: compress a list of accepted locations in-memory (for enqueue path).
     */
    fun compressAccepted(
        points: List<QualityAcceptedLocation>,
        epsilonMeters: Double = DOUGLAS_EPSILON_METERS
    ): List<QualityAcceptedLocation> {
        if (points.size <= 2) return points
        val keep = BooleanArray(points.size) { false }
        keep[0] = true
        keep[points.size - 1] = true
        douglasPeuckerAccepted(points, 0, points.size - 1, epsilonMeters, keep)
        return points.filterIndexed { idx, _ -> keep[idx] }
    }

    private fun douglasPeucker(
        pts: List<MobileLocationPointEntity>,
        start: Int,
        end: Int,
        epsilon: Double,
        keep: BooleanArray
    ) {
        var maxDist = 0.0
        var maxIdx = -1
        val a = pts[start]
        val b = pts[end]
        for (i in start + 1 until end) {
            val d = perpendicularDistanceMeters(pts[i], a, b)
            if (d > maxDist) {
                maxDist = d
                maxIdx = i
            }
        }
        if (maxDist > epsilon && maxIdx != -1) {
            keep[maxIdx] = true
            douglasPeucker(pts, start, maxIdx, epsilon, keep)
            douglasPeucker(pts, maxIdx, end, epsilon, keep)
        }
    }

    private fun douglasPeuckerAccepted(
        pts: List<QualityAcceptedLocation>,
        start: Int,
        end: Int,
        epsilon: Double,
        keep: BooleanArray
    ) {
        var maxDist = 0.0
        var maxIdx = -1
        val a = pts[start]
        val b = pts[end]
        for (i in start + 1 until end) {
            val d = perpendicularDistanceAccepted(pts[i], a, b)
            if (d > maxDist) {
                maxDist = d
                maxIdx = i
            }
        }
        if (maxDist > epsilon && maxIdx != -1) {
            keep[maxIdx] = true
            douglasPeuckerAccepted(pts, start, maxIdx, epsilon, keep)
            douglasPeuckerAccepted(pts, maxIdx, end, epsilon, keep)
        }
    }

    private fun perpendicularDistanceMeters(
        p: MobileLocationPointEntity,
        a: MobileLocationPointEntity,
        b: MobileLocationPointEntity
    ): Double {
        if (a.latitude == b.latitude && a.longitude == b.longitude) {
            return haversineMeters(p.latitude, p.longitude, a.latitude, a.longitude)
        }
        // Equirectangular projection around mid-latitude
        val latRef = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val x0 = lonToMeters(p.longitude, latRef)
        val y0 = latToMeters(p.latitude)
        val x1 = lonToMeters(a.longitude, latRef)
        val y1 = latToMeters(a.latitude)
        val x2 = lonToMeters(b.longitude, latRef)
        val y2 = latToMeters(b.latitude)
        return pointToSegmentDistance(x0, y0, x1, y1, x2, y2)
    }

    private fun perpendicularDistanceAccepted(
        p: QualityAcceptedLocation,
        a: QualityAcceptedLocation,
        b: QualityAcceptedLocation
    ): Double {
        if (a.fix.latitude == b.fix.latitude && a.fix.longitude == b.fix.longitude) {
            return haversineMeters(p.fix.latitude, p.fix.longitude, a.fix.latitude, a.fix.longitude)
        }
        val latRef = Math.toRadians((a.fix.latitude + b.fix.latitude) / 2.0)
        val x0 = lonToMeters(p.fix.longitude, latRef)
        val y0 = latToMeters(p.fix.latitude)
        val x1 = lonToMeters(a.fix.longitude, latRef)
        val y1 = latToMeters(a.fix.latitude)
        val x2 = lonToMeters(b.fix.longitude, latRef)
        val y2 = latToMeters(b.fix.latitude)
        return pointToSegmentDistance(x0, y0, x1, y1, x2, y2)
    }

    private fun pointToSegmentDistance(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x2: Double, y2: Double
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) return sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1))
        val t = ((x0 - x1) * dx + (y0 - y1) * dy) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)
        val projX = x1 + clamped * dx
        val projY = y1 + clamped * dy
        return sqrt((x0 - projX) * (x0 - projX) + (y0 - projY) * (y0 - projY))
    }

    private fun lonToMeters(lon: Double, latRefRad: Double): Double =
        Math.toRadians(lon) * EARTH_RADIUS_METERS * cos(latRefRad)

    private fun latToMeters(lat: Double): Double =
        Math.toRadians(lat) * EARTH_RADIUS_METERS

    internal fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_METERS * c
    }

    companion object {
        const val CLUSTER_DISTANCE_METERS = 5.0
        const val CLUSTER_INTERVAL_MILLIS = 30_000L
        const val DOUGLAS_EPSILON_METERS = 8.0
        private const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
