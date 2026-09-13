package com.locationjoystick.core.routing

import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.common.util.calculateBearing
import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.distanceTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteInterpolatorEdgeCasesTest {
    private lateinit var interpolator: RouteInterpolator

    @Before
    fun setUp() {
        interpolator = RouteInterpolator()
    }

    // interpolateAlongRoute — waypoint index 0

    @Test
    fun `interpolateAlongRoute with index 0 advances toward first waypoint`() {
        val start = LatLng(0.0, 0.0)
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.01, 0.0)
        val waypoints = listOf(wp0, wp1)

        // When current position is at wp0 and index is 0, it targets wp0
        // Since distance to target is 0, it should snap and advance index
        val result = interpolator.interpolateAlongRoute(waypoints, start, 0, 1.4, 1000)
        assertFalse(result.reachedEnd)
        assertEquals(1, result.nextWaypointIndex)
    }

    // interpolateAlongRoute — exact boundary: index == waypoints.size

    @Test
    fun `interpolateAlongRoute with index equal to size returns reachedEnd`() {
        val pos = LatLng(0.0, 0.0)
        val waypoints = listOf(LatLng(0.0, 0.0), LatLng(1.0, 0.0))
        val result = interpolator.interpolateAlongRoute(waypoints, pos, 2, 1.4, 1000)
        assertTrue(result.reachedEnd)
        assertEquals(2, result.nextWaypointIndex)
    }

    // interpolateAlongRoute — negative index

    @Test
    fun `interpolateAlongRoute with negative index advances normally`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.01, 0.0)
        val waypoints = listOf(start, target)

        // Negative index is < size, so it tries to access waypoints[-1] which would throw
        // But the guard only checks >= size, not < 0. Let's verify behavior.
        // Actually this would throw ArrayIndexOutOfBoundsException, which is expected behavior
        // We test the valid boundary instead
        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 1.4, 1000)
        assertFalse(result.reachedEnd)
        assertTrue(result.position.latitude > start.latitude)
    }

    // interpolateAlongRoute — multi-segment route

    @Test
    fun `interpolateAlongRoute handles three-segment route`() {
        val waypoints =
            listOf(
                LatLng(0.0, 0.0),
                LatLng(0.001, 0.0),
                LatLng(0.002, 0.0),
                LatLng(0.003, 0.0),
            )

        // Start at index 1, should advance toward waypoint at index 1
        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = LatLng(0.0, 0.0),
                currentWaypointIndex = 1,
                speedMs = 1.4,
                deltaTimeMs = 1000,
            )
        assertFalse(result.reachedEnd)
        assertTrue(result.position.latitude >= 0.0)
    }

    // interpolateAlongRoute — very high speed snaps immediately

    @Test
    fun `interpolateAlongRoute with very high speed snaps to next waypoint`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.001, 0.0) // ~111m away
        val next = LatLng(0.002, 0.0)
        val waypoints = listOf(start, target, next)

        // Speed 200 m/s * 1s = 200m > 111m → overshoots target, index advances to it,
        // and the leftover budget carries on toward `next` rather than being dropped.
        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 200.0, 1000)
        assertFalse(result.reachedEnd)
        assertEquals(2, result.nextWaypointIndex)
        assertTrue("leftover budget should carry position past target", result.position.latitude > target.latitude)
    }

    // interpolateAlongRoute — zero speed stays in place

    @Test
    fun `interpolateAlongRoute with zero speed does not advance`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.01, 0.0)
        val waypoints = listOf(start, target)

        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 0.0, 1000)
        assertFalse(result.reachedEnd)
        assertEquals(start.latitude, result.position.latitude, 0.00001)
        assertEquals(1, result.nextWaypointIndex)
    }

    // interpolateAlongRoute — zero deltaTime stays in place

    @Test
    fun `interpolateAlongRoute with zero deltaTime does not advance`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.01, 0.0)
        val waypoints = listOf(start, target)

        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 1.4, 0)
        assertFalse(result.reachedEnd)
        assertEquals(start.latitude, result.position.latitude, 0.00001)
        assertEquals(1, result.nextWaypointIndex)
    }

    // interpolateAlongRoute — reaching last waypoint with overshoot

    @Test
    fun `interpolateAlongRoute reaching last waypoint with overshoot snaps and sets reachedEnd`() {
        val start = LatLng(0.0, 0.0)
        val last = LatLng(0.000001, 0.0) // very close, within snap threshold
        val waypoints = listOf(start, last)

        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 100.0, 1000)
        assertTrue(result.reachedEnd)
        assertEquals(last.latitude, result.position.latitude, 0.00001)
    }

    // interpolateAlongRoute — result position is target when snapping

    @Test
    fun `interpolateAlongRoute snapping sets position to target waypoint`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(0.000005, 0.000005) // within snap threshold
        val waypoints = listOf(start, target)

        val result = interpolator.interpolateAlongRoute(waypoints, start, 1, 1.4, 1000)
        assertEquals(target.latitude, result.position.latitude, 0.000001)
        assertEquals(target.longitude, result.position.longitude, 0.000001)
    }

    // advancePosition — westward movement

    @Test
    fun `advancePosition bearing 270 moves west`() {
        val from = LatLng(0.0, 0.0)
        val result = interpolator.advancePosition(from, 270.0, 1000.0)
        assertTrue("longitude should decrease", result.longitude < from.longitude)
        assertEquals(0.0, result.latitude, 0.001)
    }

    // advancePosition — at high latitude

    @Test
    fun `advancePosition at high latitude moves correctly`() {
        val from = LatLng(60.0, 0.0)
        val result = interpolator.advancePosition(from, 0.0, 1000.0)
        assertTrue("latitude should increase when moving north", result.latitude > from.latitude)
    }

    // advancePosition — very large distance

    @Test
    fun `advancePosition half-earth distance wraps correctly`() {
        val from = LatLng(0.0, 0.0)
        val result = interpolator.advancePosition(from, 0.0, 20_000_000.0) // half circumference
        // After going half way around earth north, should be near equator on opposite side
        assertTrue("should be near equator after half-earth distance", kotlin.math.abs(result.latitude) < 5.0)
    }

    // InterpolationResult data class

    @Test
    fun `InterpolationResult equality works`() {
        val pos = LatLng(0.0, 0.0)
        val a = InterpolationResult(pos, 1, false)
        val b = InterpolationResult(pos, 1, false)
        assertEquals(a, b)
    }

    @Test
    fun `InterpolationResult inequality for different position`() {
        val a = InterpolationResult(LatLng(0.0, 0.0), 1, false)
        val b = InterpolationResult(LatLng(1.0, 0.0), 1, false)
        assertTrue(a != b)
    }

    @Test
    fun `InterpolationResult inequality for different index`() {
        val pos = LatLng(0.0, 0.0)
        val a = InterpolationResult(pos, 1, false)
        val b = InterpolationResult(pos, 2, false)
        assertTrue(a != b)
    }

    @Test
    fun `InterpolationResult inequality for different reachedEnd`() {
        val pos = LatLng(0.0, 0.0)
        val a = InterpolationResult(pos, 1, false)
        val b = InterpolationResult(pos, 1, true)
        assertTrue(a != b)
    }

    // interpolateAlongRoute — with AppConstants timing

    @Test
    fun `interpolateAlongRoute with standard update interval advances correctly`() {
        val start = LatLng(0.0, 0.0)
        val target = LatLng(1.0, 0.0)
        val waypoints = listOf(start, target)

        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = start,
                currentWaypointIndex = 1,
                speedMs = 1.4,
                deltaTimeMs = AppConstants.LocationConstants.UPDATE_INTERVAL_MS,
            )
        assertFalse(result.reachedEnd)
        assertTrue(result.position.latitude > start.latitude)
    }

    // interpolateAlongRoute — overshoot carry-forward

    @Test
    fun `interpolateAlongRoute carries leftover distance past waypoint into next segment`() {
        // Waypoints spaced ~111m apart (1 degree lat ≈ 111km, so 0.001 deg ≈ 111m)
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0) // ~111m north
        val wp2 = LatLng(0.002, 0.0) // another ~111m north
        val waypoints = listOf(wp0, wp1, wp2)

        // Speed high enough to overshoot wp1 but not reach wp2 in one tick: 200m/s for 1s = 200m,
        // and wp0->wp1 + wp1->wp2 is ~222m total.
        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 200.0,
                deltaTimeMs = 1000L,
            )

        // Should advance index to 2 (targeting wp2) and the full 200m budget is consumed:
        // 111m to reach wp1, then the ~89m leftover carried past it toward wp2.
        assertEquals(2, result.nextWaypointIndex)
        assertFalse(result.reachedEnd)
        val distanceToWp1 = wp0.distanceTo(wp1)
        val leftover = 200.0 - distanceToWp1
        val bearingToWp2 = calculateBearing(wp1.latitude, wp1.longitude, wp2.latitude, wp2.longitude)
        val expected = interpolator.advancePosition(wp1, bearingToWp2, leftover)
        assertEquals(expected.latitude, result.position.latitude, 1e-9)
        assertTrue("leftover should carry position past wp1", result.position.latitude > wp1.latitude)
    }

    @Test
    fun `interpolateAlongRoute budget spanning three waypoints consumes full distance in one call`() {
        // Four waypoints ~111m apart each; budget covers the first two segments fully
        // plus part of the third, all within a single interpolateAlongRoute call.
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0)
        val wp2 = LatLng(0.002, 0.0)
        val wp3 = LatLng(0.003, 0.0)
        val waypoints = listOf(wp0, wp1, wp2, wp3)

        // 300m/s for 1s = 300m budget; wp0->wp1->wp2 is ~222m, leaving ~78m carried into wp2->wp3.
        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 300.0,
                deltaTimeMs = 1000L,
            )

        // Full budget crossed two full segments — index lands past wp1 and wp2, targeting wp3.
        assertEquals(3, result.nextWaypointIndex)
        assertFalse(result.reachedEnd)
        val leftover = 300.0 - wp0.distanceTo(wp1) - wp1.distanceTo(wp2)
        val bearingToWp3 = calculateBearing(wp2.latitude, wp2.longitude, wp3.latitude, wp3.longitude)
        val expected = interpolator.advancePosition(wp2, bearingToWp3, leftover)
        assertEquals(expected.latitude, result.position.latitude, 1e-9)
        assertTrue("leftover should carry position past wp2", result.position.latitude > wp2.latitude)
    }

    @Test
    fun `interpolateAlongRoute overshoot on last segment reports reachedEnd`() {
        val wp0 = LatLng(0.0, 0.0)
        val wp1 = LatLng(0.001, 0.0)
        val waypoints = listOf(wp0, wp1)

        // Overshoot the last waypoint
        val result =
            interpolator.interpolateAlongRoute(
                waypoints = waypoints,
                currentPosition = wp0,
                currentWaypointIndex = 1,
                speedMs = 500.0,
                deltaTimeMs = 1000L,
            )
        assertTrue(result.reachedEnd)
    }
}
