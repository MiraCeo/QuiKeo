package com.locationjoystick.core.routing

import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.distanceTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteInterpolatorPrecisionTest {
    private lateinit var interpolator: RouteInterpolator

    @Before
    fun setUp() {
        interpolator = RouteInterpolator()
    }

    // -------------------------------------------------------------------------
    // 1. Multi-tick cumulative distance precision
    // -------------------------------------------------------------------------

    @Test
    fun `cumulative distance after N ticks matches N times speed times deltaT`() {
        // Long north-running segment — no snapping occurs during the test
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(10.0, 0.0) // ~1111 km
        val waypoints = listOf(wp0, wp1)

        val speedMs = 5.0
        val deltaTimeMs = 1000L
        val ticks = 100
        val expectedTotal = speedMs * (deltaTimeMs / 1000.0) * ticks // 500m

        var pos = wp0
        var idx = 1
        var cumulativeDistance = 0.0
        repeat(ticks) {
            val prev = pos
            val result = interpolator.interpolateAlongRoute(waypoints, pos, idx, speedMs, deltaTimeMs)
            assertFalse("Should not reach end during test", result.reachedEnd)
            cumulativeDistance += prev.distanceTo(result.position)
            pos = result.position
            idx = result.nextWaypointIndex
        }

        // Allow 1% tolerance for floating-point / Haversine approximation
        assertEquals(expectedTotal, cumulativeDistance, expectedTotal * 0.01)
    }

    // -------------------------------------------------------------------------
    // 2. Snap threshold boundary — WAYPOINT_SNAP_THRESHOLD_METERS = 1.0
    // -------------------------------------------------------------------------

    @Test
    fun `position just outside snap threshold does not snap`() {
        // 0.000010° lat ≈ 1.11m at equator — above the 1m threshold
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.000010, 0.0) // ~1.11m
        val waypoints = listOf(start, target, LatLng(0.1, 0.0))

        // speed 0.0 → distanceToAdvance = 0, only threshold check fires
        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 0.0, 1000)

        assertFalse("Should not snap when distance > ${AppConstants.RouteConstants.WAYPOINT_SNAP_THRESHOLD_METERS}m", result.reachedEnd)
        assertEquals(1, result.nextWaypointIndex)
        assertEquals(start.latitude, result.position.latitude, 1e-9)
    }

    @Test
    fun `position just inside snap threshold does snap`() {
        // 0.000008° lat ≈ 0.89m at equator — below the 1m threshold
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.000008, 0.0) // ~0.89m
        val next = LatLng(0.1, 0.0)
        val waypoints = listOf(start, target, next)

        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 0.0, 1000)

        assertFalse("Should not report reachedEnd — next waypoint exists", result.reachedEnd)
        assertEquals("Index should advance on snap", 2, result.nextWaypointIndex)
        assertEquals(target.latitude, result.position.latitude, 1e-9)
    }

    @Test
    fun `snap threshold is consistent with WAYPOINT_SNAP_THRESHOLD_METERS constant`() {
        val start = LatLng(0.0, 0.0)
        val next = LatLng(0.1, 0.0)

        // 0.000004° ≈ 0.44m — well inside the 1m threshold → snaps
        val closeTarget = LatLng(0.000004, 0.0)
        val snapped =
            interpolator.interpolateAlongRoute(
                listOf(start, closeTarget, next),
                start,
                1,
                0.0,
                1000,
            )
        assertEquals("Should snap when well inside threshold", 2, snapped.nextWaypointIndex)

        // 0.000020° ≈ 2.22m — well outside the 1m threshold → does not snap
        val farTarget = LatLng(0.000020, 0.0)
        val notSnapped =
            interpolator.interpolateAlongRoute(
                listOf(start, farTarget, next),
                start,
                1,
                0.0,
                1000,
            )
        assertEquals("Should not snap when well outside threshold", 1, notSnapped.nextWaypointIndex)
    }

    // -------------------------------------------------------------------------
    // 3. Segment boundary carry-forward
    // -------------------------------------------------------------------------

    @Test
    fun `carry-forward moves position into next segment after overshooting waypoint`() {
        // 4 waypoints so nextIndex+1 < size is true, enabling carry-forward
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0) // ~111m north
        val wp2 = LatLng(0.002, 0.0) // another ~111m north
        val wp3 = LatLng(0.003, 0.0)
        val waypoints = listOf(wp0, wp1, wp2, wp3)

        // 200 m/s × 1s = 200m; wp1 is ~111m away → ~89m leftover carries into wp1→wp2
        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 200.0,
                deltaTimeMs = 1000L,
            )

        assertFalse(result.reachedEnd)
        assertEquals("Index should advance to wp2", 2, result.nextWaypointIndex)
        assertTrue(
            "Carry-forward: position should be north of wp1, not stuck at it",
            result.position.latitude > wp1.latitude,
        )
        assertTrue(
            "Carry-forward: position should not overshoot wp2",
            result.position.latitude <= wp2.latitude,
        )
    }

    @Test
    fun `carry-forward keeps consuming budget across a short intermediate segment`() {
        // Extreme overshoot: speed high enough that leftover after wp1 exceeds the
        // short wp1->wp2 segment too — the fix keeps consuming budget into wp2->wp3
        // instead of dropping the remainder at wp2 (the old, capped behavior).
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0) // ~111m
        val wp2 = LatLng(0.0011, 0.0) // only ~11m from wp1
        val wp3 = LatLng(0.01, 0.0)
        val waypoints = listOf(wp0, wp1, wp2, wp3)

        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 1000.0,
                deltaTimeMs = 1000L,
            )

        assertFalse(result.reachedEnd)
        // Full 1000m budget crosses wp1 (~111m) and wp2 (~11m more), landing in the wp2->wp3 leg.
        assertEquals("Index should advance past wp2 to wp3", 3, result.nextWaypointIndex)
        assertTrue(
            "Leftover budget should carry position past wp2, not cap there",
            result.position.latitude > wp2.latitude,
        )
        assertTrue(
            "Should not overshoot wp3 since the full budget is less than the whole route",
            result.position.latitude < wp3.latitude,
        )
    }

    @Test
    fun `carry-forward happens even when the next waypoint is the last one`() {
        // 3 waypoints: reaching wp1 still has budget left over, and wp2 is the last
        // waypoint — the fix carries the leftover into wp1->wp2 instead of dropping it
        // (the old bug required a waypoint *after* the next one to carry at all).
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0) // ~111m
        val wp2 = LatLng(0.002, 0.0)
        val waypoints = listOf(wp0, wp1, wp2)

        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 200.0,
                deltaTimeMs = 1000L,
            )

        assertFalse(result.reachedEnd)
        assertEquals(2, result.nextWaypointIndex)
        // Position carries past wp1 toward wp2 instead of snapping exactly to wp1.
        assertTrue(result.position.latitude > wp1.latitude)
        assertTrue(result.position.latitude < wp2.latitude)
    }
}
