package com.locationjoystick.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.locationjoystick.core.model.LatLng
import com.locationjoystick.core.model.Waypoint

/**
 * Room entity for route waypoints.
 *
 * Each waypoint belongs to a single [RouteEntity] (foreign key with CASCADE delete).
 * Waypoints are ordered by [orderIndex] within their route.
 *
 * @property routeId Foreign key to the parent route
 * @property latitude Waypoint latitude in degrees
 * @property longitude Waypoint longitude in degrees
 * @property orderIndex Position in the route (0 = first waypoint)
 * @property waitSeconds Seconds to wait at this waypoint before jumping onward (teleport routes only)
 */
@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class WaypointEntity(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int,
    val waitSeconds: Int = 0,
)

/** Converts this entity to the domain [Waypoint] model. */
fun WaypointEntity.toDomain(): Waypoint =
    Waypoint(
        id = id,
        position = LatLng(latitude = latitude, longitude = longitude),
        orderIndex = orderIndex,
        waitSeconds = waitSeconds,
    )

/** Converts a domain [Waypoint] to this entity for a given route. */
fun Waypoint.toEntity(routeId: String): WaypointEntity =
    WaypointEntity(
        id = id,
        routeId = routeId,
        latitude = position.latitude,
        longitude = position.longitude,
        orderIndex = orderIndex,
        waitSeconds = waitSeconds,
    )
