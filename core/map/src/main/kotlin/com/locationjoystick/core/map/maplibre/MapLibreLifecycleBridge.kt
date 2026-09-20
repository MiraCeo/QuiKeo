package com.locationjoystick.core.map.maplibre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapView

/**
 * Forwards Android lifecycle events to [mapView].
 *
 * Extracted from the verbatim `DisposableEffect` block duplicated in all 4 map screens.
 *
 * `ON_START`/`ON_STOP` are forwarded for every foreground/background transition, not just the
 * first and last one. MapLibre needs `onStop()` to pause the render thread and deactivate its
 * `FileSource`/`ConnectivityReceiver`; skipping it leaves the renderer running while the app sits
 * in the background. Start/stop are also ref-counted inside MapLibre, so the forwarder tracks
 * whether the view is currently started and never emits an unbalanced pair.
 *
 * @param mapView The MapLibre [MapView] to forward events to.
 * @param lifecycleOwner The lifecycle owner to observe. Defaults to [LocalLifecycleOwner].
 * @param callCreateOnAttach If `true` (default), calls `mapView.onCreate(null)` and
 *   `mapView.onStart()` immediately when the effect is first attached - required when
 *   the MapView is created inside a `remember {}` block (MapScreen, MapFloatingView).
 *   Pass `false` when the MapView is only driven by the observer and must not be created
 *   eagerly (MapPickerScreen, RouteCreatorScreen pattern). This controls attachment only;
 *   later `ON_START`/`ON_STOP` events are forwarded either way.
 */
@Composable
fun MapLibreLifecycleBridge(
    mapView: MapView,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    callCreateOnAttach: Boolean = true,
) {
    DisposableEffect(lifecycleOwner, mapView) {
        val forwarder =
            MapLifecycleForwarder(
                target = mapView.asLifecycleTarget(),
                createOnAttach = callCreateOnAttach,
            )
        val observer = LifecycleEventObserver { _, event -> forwarder.onEvent(event) }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            forwarder.onDispose()
        }
    }
}

/** The `MapView` lifecycle surface the forwarder drives, so it can be tested without Android. */
internal interface MapLifecycleTarget {
    fun onCreate()

    fun onStart()

    fun onResume()

    fun onPause()

    fun onStop()

    fun onDestroy()
}

/**
 * Pure lifecycle-event state machine behind [MapLibreLifecycleBridge].
 *
 * Kept free of Compose and Android so the start/stop pairing can be unit tested directly.
 */
internal class MapLifecycleForwarder(
    private val target: MapLifecycleTarget,
    createOnAttach: Boolean,
) {
    /** Whether [target] is currently started, used to keep `onStart`/`onStop` balanced. */
    var isStarted: Boolean = false
        private set

    init {
        if (createOnAttach) {
            target.onCreate()
            target.onStart()
            isStarted = true
        }
    }

    fun onEvent(event: Lifecycle.Event) {
        when (event) {
            // Attaching to an already-started lifecycle replays ON_START; isStarted absorbs it.
            Lifecycle.Event.ON_START -> if (!isStarted) start()
            Lifecycle.Event.ON_RESUME -> target.onResume()
            Lifecycle.Event.ON_PAUSE -> target.onPause()
            Lifecycle.Event.ON_STOP -> if (isStarted) stop()
            else -> Unit
        }
    }

    fun onDispose() {
        target.onPause()
        if (isStarted) stop()
        target.onDestroy()
    }

    private fun start() {
        target.onStart()
        isStarted = true
    }

    private fun stop() {
        target.onStop()
        isStarted = false
    }
}

private fun MapView.asLifecycleTarget(): MapLifecycleTarget {
    val mapView = this
    return object : MapLifecycleTarget {
        override fun onCreate() = mapView.onCreate(null)

        override fun onStart() = mapView.onStart()

        override fun onResume() = mapView.onResume()

        override fun onPause() = mapView.onPause()

        override fun onStop() = mapView.onStop()

        override fun onDestroy() = mapView.onDestroy()
    }
}
