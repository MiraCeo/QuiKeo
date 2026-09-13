package com.locationjoystick.core.routing

import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.common.util.calculateBearing
import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.distanceTo
import javax.inject.Inject
import javax.inject.Singleton
import com.locationjoystick.core.common.util.advancePosition as geoAdvancePosition

/**
 * Handles position interpolation along routes for replay and roaming.
 *
 * Used by [RouteReplayEngine] and [RoamingEngine] to advance positions
 * along a series of waypoints at a given speed.
 *
 * Key functionality:
 * - Advances a position along a great-circle bearing by a given distance
 * - Handles waypoint arrival detection and automatic advancement to next waypoint
 * - Snaps to waypoints within a threshold distance ([AppConstants.RouteConstants.WAYPOINT_SNAP_THRESHOLD_METERS])
 */
@Singleton
class RouteInterpolator
    @Inject
    constructor() {
        /**
         * Advances a position along a bearing by a given distance.
         *
         * @param from Starting position
         * @param bearingDeg Bearing in degrees (0 = north, 90 = east)
         * @param distanceMeters Distance to travel in meters
         * @return New position after advancing
         */
        fun advancePosition(
            from: LatLng,
            bearingDeg: Double,
            distanceMeters: Double,
        ): LatLng {
            val (lat, lon) = geoAdvancePosition(from.latitude, from.longitude, bearingDeg, distanceMeters)
            return LatLng(lat, lon)
        }

        /**
         * Interpolates movement along a route for one time step.
         *
         * @param waypoints Ordered list of waypoints to follow
         * @param currentPosition Current position
         * @param currentWaypointIndex Index of the target waypoint in the list
         * @param speedMs Movement speed in meters per second
         * @param deltaTimeMs Time step in milliseconds
         * @return [InterpolationResult] with new position, next waypoint index, and end-of-route flag
         */
        fun interpolateAlongRoute(
            waypoints: List<LatLng>,
            currentPosition: LatLng,
            currentWaypointIndex: Int,
            speedMs: Double,
            deltaTimeMs: Long,
        ): InterpolationResult {
            if (waypoints.size < 2 || currentWaypointIndex >= waypoints.size) {
                return InterpolationResult(currentPosition, currentWaypointIndex, reachedEnd = true)
            }

            var position = currentPosition
            var waypointIndex = currentWaypointIndex
            var remaining = speedMs * (deltaTimeMs / 1000.0)

            // Consume the full tick budget across as many consecutive waypoints as
            // needed (dense geometry, e.g. OSRM road-following, can put several
            // waypoints within one tick's travel distance) rather than carrying
            // leftover distance forward only one segment and dropping the rest.
            while (true) {
                val target = waypoints[waypointIndex]
                val distanceToTarget = position.distanceTo(target)

                if (distanceToTarget > AppConstants.RouteConstants.WAYPOINT_SNAP_THRESHOLD_METERS &&
                    remaining < distanceToTarget
                ) {
                    val bearing = calculateBearing(position.latitude, position.longitude, target.latitude, target.longitude)
                    val newPosition = advancePosition(position, bearing, remaining)
                    return InterpolationResult(newPosition, waypointIndex, reachedEnd = false)
                }

                remaining = (remaining - distanceToTarget).coerceAtLeast(0.0)
                position = target
                val nextIndex = waypointIndex + 1
                if (nextIndex >= waypoints.size) {
                    return InterpolationResult(position, waypointIndex, reachedEnd = true)
                }
                waypointIndex = nextIndex
                if (remaining <= 0.0) {
                    return InterpolationResult(position, waypointIndex, reachedEnd = false)
                }
            }
        }
    }

/**
 * Result of one interpolation step along a route.
 *
 * @property position New position after interpolation
 * @property nextWaypointIndex Index of the next waypoint to target
 * @property reachedEnd True if the last waypoint has been reached
 */
data class InterpolationResult(
    val position: LatLng,
    val nextWaypointIndex: Int,
    val reachedEnd: Boolean,
)
