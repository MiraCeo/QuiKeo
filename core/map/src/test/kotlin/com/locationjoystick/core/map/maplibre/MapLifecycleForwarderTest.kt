package com.locationjoystick.core.map.maplibre

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the start/stop forwarding that [MapLibreLifecycleBridge] performs.
 *
 * The bug these pin down: `ON_START`/`ON_STOP` used to be skipped whenever `callCreateOnAttach`
 * was true, so a map created inside `remember {}` never received `onStop()` when the app went to
 * the background and MapLibre kept its render thread alive.
 */
class MapLifecycleForwarderTest {
    private class RecordingTarget : MapLifecycleTarget {
        val calls = mutableListOf<String>()

        override fun onCreate() {
            calls += "onCreate"
        }

        override fun onStart() {
            calls += "onStart"
        }

        override fun onResume() {
            calls += "onResume"
        }

        override fun onPause() {
            calls += "onPause"
        }

        override fun onStop() {
            calls += "onStop"
        }

        override fun onDestroy() {
            calls += "onDestroy"
        }
    }

    private fun forwarder(
        target: RecordingTarget,
        createOnAttach: Boolean,
    ) = MapLifecycleForwarder(target = target, createOnAttach = createOnAttach)

    @Test
    fun `createOnAttach initialises the map view once`() {
        val target = RecordingTarget()
        forwarder(target, createOnAttach = true)

        assertEquals(listOf("onCreate", "onStart"), target.calls)
    }

    @Test
    fun `no initialisation when createOnAttach is false`() {
        val target = RecordingTarget()
        forwarder(target, createOnAttach = false)

        assertTrue(target.calls.isEmpty())
    }

    @Test
    fun `background transition forwards onStop even when created on attach`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        target.calls.clear()

        subject.onEvent(Lifecycle.Event.ON_PAUSE)
        subject.onEvent(Lifecycle.Event.ON_STOP)

        assertEquals(listOf("onPause", "onStop"), target.calls)
        assertFalse(subject.isStarted)
    }

    @Test
    fun `returning to the foreground forwards onStart again`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        subject.onEvent(Lifecycle.Event.ON_STOP)
        target.calls.clear()

        subject.onEvent(Lifecycle.Event.ON_START)
        subject.onEvent(Lifecycle.Event.ON_RESUME)

        assertEquals(listOf("onStart", "onResume"), target.calls)
        assertTrue(subject.isStarted)
    }

    @Test
    fun `repeated background cycles stay balanced`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        target.calls.clear()

        repeat(3) {
            subject.onEvent(Lifecycle.Event.ON_START)
            subject.onEvent(Lifecycle.Event.ON_RESUME)
            subject.onEvent(Lifecycle.Event.ON_PAUSE)
            subject.onEvent(Lifecycle.Event.ON_STOP)
        }

        // Attaching already started the view, so the first cycle's ON_START is absorbed as a
        // duplicate; the remaining two cycles each re-start it. Every ON_STOP is forwarded.
        assertEquals(2, target.calls.count { it == "onStart" })
        assertEquals(3, target.calls.count { it == "onStop" })
        assertFalse(subject.isStarted)
    }

    @Test
    fun `replayed ON_START after attach does not double start`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        target.calls.clear()

        subject.onEvent(Lifecycle.Event.ON_START)

        assertTrue(target.calls.isEmpty())
        assertTrue(subject.isStarted)
    }

    @Test
    fun `ON_STOP is ignored when the view was never started`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = false)

        subject.onEvent(Lifecycle.Event.ON_STOP)

        assertTrue(target.calls.isEmpty())
        assertFalse(subject.isStarted)
    }

    @Test
    fun `observer driven start then stop is forwarded when not created on attach`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = false)

        subject.onEvent(Lifecycle.Event.ON_START)
        subject.onEvent(Lifecycle.Event.ON_STOP)

        assertEquals(listOf("onStart", "onStop"), target.calls)
    }

    @Test
    fun `dispose stops a started view before destroying it`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        target.calls.clear()

        subject.onDispose()

        assertEquals(listOf("onPause", "onStop", "onDestroy"), target.calls)
    }

    @Test
    fun `dispose after background does not stop twice`() {
        val target = RecordingTarget()
        val subject = forwarder(target, createOnAttach = true)
        subject.onEvent(Lifecycle.Event.ON_STOP)
        target.calls.clear()

        subject.onDispose()

        assertEquals(listOf("onPause", "onDestroy"), target.calls)
    }
}
