package com.locationjoystick.core.model

/** Geodetic datum a raster tile provider renders its map in. */
enum class MapCoordinateSystem {
    /** Standard GPS datum — what the app stores and what `LocationManager` receives. */
    WGS84,

    /** Chinese obfuscated datum used by Amap/Gaode and every mainland-China provider. */
    GCJ02,
}

/**
 * Raster base-map provider. Shared by every map view (main map, floating widget, favorites
 * picker, route creator) so a single Settings choice switches all of them together.
 *
 * [tileUrlTemplates] are XYZ templates in MapLibre `{z}/{x}/{y}` syntax; several entries mean
 * the provider shards across subdomains and MapLibre should round-robin between them.
 */
enum class MapTileSource(
    val tileUrlTemplates: List<String>,
    val maxZoom: Float,
    val coordinateSystem: MapCoordinateSystem,
) {
    OSM(
        tileUrlTemplates = listOf("https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
        maxZoom = 19f,
        coordinateSystem = MapCoordinateSystem.WGS84,
    ),

    /**
     * Amap/Gaode road map (style 8 — Chinese labels). Community-known unofficial raster endpoint,
     * no API key; fast and reliable from mainland China where OSM tiles are throttled.
     */
    AMAP(
        tileUrlTemplates =
            (1..4).map { shard ->
                "https://webrd0$shard.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}"
            },
        maxZoom = 18f,
        coordinateSystem = MapCoordinateSystem.GCJ02,
    ),
    ;

    companion object {
        val DEFAULT = OSM

        /** Lenient parse for persisted/imported values — unknown names fall back to [DEFAULT]. */
        fun fromName(name: String?): MapTileSource = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
