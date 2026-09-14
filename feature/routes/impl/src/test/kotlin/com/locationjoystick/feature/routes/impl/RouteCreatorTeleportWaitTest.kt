package com.locationjoystick.feature.routes.impl

import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Regression tests for [RouteCreatorViewModel]'s combined `placedWaypoints: List<Pair<LatLng,
 * Int>>` bookkeeping (position + wait duration in one list) — replaces the old parallel
 * `waypoints`/`waitSecondsList` arrays this task removed. Mirrors the extraction-logic-test
 * pattern in [RouteCreatorSaveRouteTest].
 */
class RouteCreatorTeleportWaitTest {
    // Mirrors RouteCreatorViewModel.addWaypoint's placedWaypoints append.
    private fun addWaypoint(
        placedWaypoints: List<Pair<LatLng, Int>>,
        newPoint: LatLng,
        waitSeconds: Int,
    ): List<Pair<LatLng, Int>> = placedWaypoints + (newPoint to waitSeconds)

    // Mirrors RouteCreatorViewModel.undoLastWaypoint's placedWaypoints dropLast.
    private fun undoLastWaypoint(placedWaypoints: List<Pair<LatLng, Int>>): List<Pair<LatLng, Int>> = placedWaypoints.dropLast(1)

    // Mirrors RouteCreatorViewModel.saveRoute's per-index Waypoint construction.
    private fun buildWaypoints(
        positions: List<LatLng>,
        placedWaypoints: List<Pair<LatLng, Int>>,
    ): List<Waypoint> =
        positions.mapIndexed { idx, latLng ->
            Waypoint(
                id = UUID.randomUUID().toString(),
                position = latLng,
                orderIndex = idx,
                waitSeconds = placedWaypoints.getOrNull(idx)?.second ?: 0,
            )
        }

    @Test
    fun `addWaypoint appends position and waitSeconds together`() {
        var placed = listOf<Pair<LatLng, Int>>()
        placed = addWaypoint(placed, LatLng(0.0, 0.0), 3)
        placed = addWaypoint(placed, LatLng(1.0, 1.0), 7)

        assertEquals(listOf(3, 7), placed.map { it.second })
        assertEquals(2, placed.size)
    }

    @Test
    fun `undoLastWaypoint drops the last position and its waitSeconds together`() {
        val placed = listOf(LatLng(0.0, 0.0) to 3, LatLng(1.0, 1.0) to 7)

        val newPlaced = undoLastWaypoint(placed)

        assertEquals(listOf(LatLng(0.0, 0.0) to 3), newPlaced)
    }

    @Test
    fun `saveRoute persists each Waypoint waitSeconds from the paired list`() {
        val positions = listOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0))
        val placed = listOf(LatLng(0.0, 0.0) to 3, LatLng(1.0, 1.0) to 7)

        val result = buildWaypoints(positions, placed)

        assertEquals(3, result[0].waitSeconds)
        assertEquals(7, result[1].waitSeconds)
    }

    @Test
    fun `non-teleport addWaypoint with waitSeconds 0 behaves like before the signature change`() {
        val placed = addWaypoint(emptyList(), LatLng(5.0, 5.0), 0)

        assertEquals(listOf(LatLng(5.0, 5.0) to 0), placed)
    }
}
