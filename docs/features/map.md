# Map (MapLibre)

Main screen. Raster base map (OpenStreetMap by default) centered on `AppConstants.MapConstants.DEFAULT_LAT` / `AppConstants.MapConstants.DEFAULT_LON` first load. Scroll on by default.

Key files: `:feature:map:impl/MapScreen.kt`, `:feature:map:impl/MapViewModel.kt`

## Library

MapLibre Android SDK 12.x. Not osmdroid, not Google Maps.

- Raster tile source via `RasterSource`, built from the selected `MapTileSource` (see "Map source" below) by `Style.addRasterTiles` in `:core:map/MapLibreStyleExt.kt`.
- Location marker: `SymbolLayer` backed by GeoJSON. Update coords — no remove/re-add.
- Route polylines: `LineLayer` backed by GeoJSON `FeatureCollection`.
- Jitter radius overlay: `FillLayer` + `LineLayer` backed by a real-world-meters GeoJSON polygon (not `CircleLayer` — its radius is screen pixels, not meters).
- Offline tiles via `OfflineManager.downloadRegion()`.

## Map source

Settings → Menus → Map → **Map source** (`AppSettings.mapTileSource`, `MapTileSource` enum in
`:core:model`) picks the raster provider for **every** map surface: main map, floating widget
map (`MapFloatingView`), favorites picker (`MapPickerScreen`) and route creator
(`RouteCreatorScreen`). Each surface collects the setting from `SettingsRepository.getMapTileSource()`
and re-applies its MapLibre style when it changes, re-anchoring the camera on the same real-world
point.

| Source | Tiles | Datum | Zoom | Default center | Notes |
|---|---|---|---|---|---|
| `OSM` (default) | `tile.openstreetmap.org` | WGS-84 | 0–19 | Paris | Unchanged behaviour. |
| `AMAP` | `webrd0{1-4}.is.autonavi.com` (style 8, zh labels) | GCJ-02 | 3–18 | Beijing | Community raster endpoint, no API key. Fast from mainland China where OSM tiles are throttled. Coverage is mainland China only. |

Each source carries `minZoom`/`maxZoom`/`defaultCenter`. Every surface calls
`MapLibreMap.applyZoomBounds(tileSource)` when it applies a style, which clamps the camera to the
served range — otherwise Amap shows blank tiles when zoomed out past z3 or in past z18 and looks
broken. `defaultCenter` is used as the first-open camera when there is no position yet, and by
`MapController.startSpoofing` as the very first mock fix, so a fresh install on Amap starts inside
its coverage instead of over Paris.

No custom-URL option by design — keeps the setting a simple two-way toggle and avoids shipping a
free-form network field.

### GCJ-02 boundary

GCJ-02 is an obfuscated datum; drawing WGS-84 data on Amap tiles is off by ~100–700 m. Conversion
lives in `:core:map/projection/` (`Gcj02.kt`, pure Kotlin, unit-tested to 10 m round-trip) and is
applied **only at the MapLibre presentation edge**, in both directions, via
`MapTileSource.projection` (`MapProjection.Identity` for WGS-84 sources, `Gcj02Projection` for GCJ-02):

- `proj.toMap(...)` — every WGS-84 value handed to MapLibre: camera targets, position marker, route
  traces, waypoints, jitter circle, search/pending-tap markers, roaming preview.
- `proj.fromMap(...)` — every value read back from MapLibre: tap / long-press coordinates, picker
  selections, camera center when swapping styles.

Everything else — `AppSettings`, favorites, routes, `LocationRepository`, `LocationManager` mock
fixes, Nominatim, OSRM, exports — stays WGS-84. Never persist or spoof a GCJ-02 coordinate.

### Attribution

MapLibre's built-in attribution button is disabled on all surfaces, so each one renders
`MapAttribution` (`:core:map/ui/MapAttribution.kt`) in the bottom-left corner with a
per-provider credit line (`map_attribution_osm` / `map_attribution_amap`), which also surfaces the
GCJ-02 datum to the user.

## Navigation

- TopAppBar hamburger opens nav drawer via `onOpenDrawer: () -> Unit`. Drawer owned by `LjApp`, not `LjNavHost`.
- Start/stop spoofing is controlled solely from the top bar's `LjScaffold`/`LjTopBar` toggle (see @docs/features/mock-location.md, "Global Start/Stop Control") — there is no separate start/stop FAB on the map screen.

## Interactions

- Long-press → bottom sheet with "Walk here" / "Teleport here".
- Tap route point → select.
- Tap empty map in edit mode → add waypoint.
- Camera follow: disabled on `REASON_API_GESTURE`. Re-enabled via re-center FAB.

## Configurable FABs

`MapFabColumn` renders Favorites/Routes/Roaming/Search in the shared `AppFeature` order (see @docs/features/widget.md, "Configurability"), filtered to features enabled for the `MAP` surface — configured in Settings → Menus → "App Features". Routes and Roaming also force-show while actively in progress, even if toggled off, so the user can still control a running session.

## Jitter Radius Overlay

When Settings → Menus → Debug → "Debug stats" (`AppSettings.debugStatsEnabled`)
is on, a translucent circle is drawn on the map centered on the current
spoofed position, radius = the position-jitter radius currently in effect
(idle or moving, whichever applies this tick — see @docs/features/mock-location.md,
"Position jitter"). Lets the user visually calibrate Settings → Location
Randomness jitter values against the map instead of guessing from a raw
meters number. Rendered as a real-world-meters GeoJSON polygon
(`buildCirclePolygonGeoJson`, `:core:map`) via `FillLayer` + `LineLayer` —
not `CircleLayer`, whose radius is always screen pixels, not meters. Main
map screen only, not the floating map.

## Lifecycle

- Forward all lifecycle events to `MapView`.
- Never call MapLibre APIs before `onMapReady`.
- Use `MapLibreLifecycleBridge` (`:core:map`) rather than a hand-written
  `DisposableEffect`. It observes the host lifecycle and keeps `onStart`/`onStop`
  balanced via an internal started flag, so an `ON_START` replayed at attach time
  is absorbed instead of double-starting the view.
- `ON_START`/`ON_STOP` must be forwarded on *every* foreground/background
  transition, not only the first and last. In MapLibre 13.x `onPause()`/`onResume()`
  are no-op stubs for the renderer: `onStop()` is what pauses the render thread and
  deactivates `FileSource`/`ConnectivityReceiver`. Skipping it leaves the map
  rendering and holding GPU memory while the app sits in the background.
- `callCreateOnAttach = true` when the `MapView` is built inside `remember {}` and
  needs `onCreate(null)` + `onStart()` at attach time (main map screen, floating
  map). Pass `false` when the view must only be driven by later lifecycle events
  (favourites picker, route creator). The flag controls attach-time behaviour
  only - `ON_START`/`ON_STOP` forwarding is unconditional either way.

## Rendering mode

The main map screen builds its `MapView` in TextureView mode:

```kotlin
MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true))
```

In the default SurfaceView mode the framework destroys the `Surface` when the
activity stops, which makes MapLibre tear down and rebuild its whole renderer
(`MapRenderer::onSurfaceDestroyed` -> `resetRenderer()`). The rebuilt renderer
starts empty - tile pyramid, glyph/icon atlases and GPU buffers are all gone -
so returning to the foreground shows a visible redraw while tiles are refetched
and reuploaded.

A `TextureView` keeps its `SurfaceTexture` alive across activity stop
(`destroyHardwareResources()` only detaches the hardware layer; the texture is
released in `onDetachedFromWindowInternal()`, which stopping does not trigger),
so the renderer survives and the previous frame is still on screen when the user
comes back. Measured on a PJD110: no `surfaceDestroyed`, no Vulkan
re-initialisation, and the frame after the window animation is pixel-identical
to the one from before backgrounding.

Cost is one extra composition copy per frame. This did not increase measured
memory - foreground PSS was lower than with SurfaceView, because the per-return
rebuild spike disappears. Under extreme memory pressure the texture can still be
reclaimed, which degrades to the old redraw rather than failing.

The favourites picker, route creator and floating map still use the default
SurfaceView mode.