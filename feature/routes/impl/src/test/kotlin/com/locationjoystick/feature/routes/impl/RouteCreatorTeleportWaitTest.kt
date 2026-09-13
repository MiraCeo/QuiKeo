package com.locationjoystick.feature.routes.impl

import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.RouteType
import com.locationjoystick.core.model.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Regression tests for the parallel-array bookkeeping between
 * [CreatorState.waypoints] and [CreatorState.waitSecondsList] that
 * [RouteCreatorViewModel.addWaypoint]/`undoLastWaypoint`/`saveRoute` maintain for
 * [RouteType.TELEPORT] routes — mirrors the extraction-logic-test pattern in
 * [RouteCreatorSaveRouteTest].
 */
class RouteCreatorTeleportWaitTest {
    // Mirrors RouteCreatorViewModel.addWaypoint's waitSecondsList append.
    private fun addWaypoint(
        waypoints: List<LatLng>,
        waitSecondsList: List<Int>,
        newPoint: LatLng,
        waitSeconds: Int,
    ): Pair<List<LatLng>, List<Int>> = (waypoints + newPoint) to (waitSecondsList + waitSeconds)

    // Mirrors RouteCreatorViewModel.undoLastWaypoint's waitSecondsList dropLast.
    private fun undoLastWaypoint(
        waypoints: List<LatLng>,
        waitSecondsList: List<Int>,
    ): Pair<List<LatLng>, List<Int>> = waypoints.dropLast(1) to waitSecondsList.dropLast(1)

    // Mirrors RouteCreatorViewModel.saveRoute's per-index Waypoint construction.
    private fun buildWaypoints(
        positions: List<LatLng>,
        waitSecondsList: List<Int>,
    ): List<Waypoint> =
        positions.mapIndexed { idx, latLng ->
            Waypoint(
                id = UUID.randomUUID().toString(),
                position = latLng,
                orderIndex = idx,
                waitSeconds = waitSecondsList.getOrElse(idx) { 0 },
            )
        }

    @Test
    fun `addWaypoint appends waitSeconds at the matching index`() {
        var (waypoints, waits) = listOf<LatLng>() to listOf<Int>()
        val addResult1 = addWaypoint(waypoints, waits, LatLng(0.0, 0.0), 3)
        waypoints = addResult1.first
        waits = addResult1.second
        val addResult2 = addWaypoint(waypoints, waits, LatLng(1.0, 1.0), 7)
        waypoints = addResult2.first
        waits = addResult2.second

        assertEquals(listOf(3, 7), waits)
        assertEquals(2, waypoints.size)
    }

    @Test
    fun `undoLastWaypoint drops the last waitSecondsList entry along with the waypoint`() {
        val waypoints = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0))
        val waits = listOf(3, 7)

        val (newWaypoints, newWaits) = undoLastWaypoint(waypoints, waits)

        assertEquals(listOf(LatLng(0.0, 0.0)), newWaypoints)
        assertEquals(listOf(3), newWaits)
    }

    @Test
    fun `saveRoute persists each Waypoint waitSeconds from waitSecondsList`() {
        val positions = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0))
        val waits = listOf(3, 7)

        val result = buildWaypoints(positions, waits)

        assertEquals(3, result[0].waitSeconds)
        assertEquals(7, result[1].waitSeconds)
    }

    @Test
    fun `non-teleport addWaypoint with waitSeconds 0 behaves like before the signature change`() {
        val (waypoints, waits) = addWaypoint(emptyList(), emptyList(), LatLng(5.0, 5.0), 0)

        assertEquals(listOf(LatLng(5.0, 5.0)), waypoints)
        assertEquals(listOf(0), waits)
    }
}
