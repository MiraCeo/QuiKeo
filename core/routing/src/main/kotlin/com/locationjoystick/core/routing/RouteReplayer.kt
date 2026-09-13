package com.locationjoystick.core.routing

import com.locationjoystick.core.model.LatLng

/**
 * Common shape of [RouteReplayEngine] and [TeleportRouteEngine], so callers driving an active
 * replay (pause/resume/stop/jump) don't need to know which engine is running it.
 */
interface RouteReplayer {
    fun pause()

    fun resume(
        onPositionUpdate: (LatLng) -> Unit,
        onComplete: () -> Unit,
    )

    suspend fun stop()

    fun jumpToNextWaypoint(
        onPositionUpdate: (LatLng) -> Unit,
        onComplete: () -> Unit,
    ): LatLng?

    fun jumpToPreviousWaypoint(
        onPositionUpdate: (LatLng) -> Unit,
        onComplete: () -> Unit,
    ): LatLng?
}
