package com.locationjoystick.core.model

data class Waypoint(
    val id: String,
    val position: LatLng,
    val orderIndex: Int,
    val waitSeconds: Int = 0,
)
