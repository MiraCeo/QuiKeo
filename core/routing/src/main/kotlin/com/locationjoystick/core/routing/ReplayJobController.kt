package com.locationjoystick.core.routing

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coroutine-lifecycle scaffolding shared by [RouteReplayEngine] and [TeleportRouteEngine]:
 * the replay scope, the current job, and its cancel/replace/pause operations. Both engines own
 * one instance and keep only their own tick logic (interpolation vs. wait-and-jump) —
 * cancel-previous/join/launch/pause is identical byte-for-byte between them and lives here.
 */
internal class ReplayJobController(
    tag: String,
) : AutoCloseable {
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> Log.e(tag, "Replay coroutine crashed", throwable) }

    /** Scope for replay coroutines. Uses SupervisorJob so failures don't propagate. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    /** Serializes cancel-and-join against a concurrent launch in [cancelAndJoinActive]. */
    private val jobMutex = Mutex()

    /** Current replay job. Only ever mutated by the methods below — never by callers directly. */
    @Volatile private var activeJob: Job? = null

    /** True while a launched job is still active (running or paused-but-not-yet-cancelled logic lives in the caller). */
    val isRunning: Boolean get() = activeJob?.isActive == true

    /**
     * Cancels any current job and launches [block] as the new one, joining the cancelled job
     * first so the old and new ticks never overlap. Mirrors the cancel-previous / join /
     * launch / store pattern both engines' `launchReplay` used to hand-roll.
     */
    fun launch(block: suspend CoroutineScope.() -> Unit) {
        val previousJob = activeJob
        previousJob?.cancel()
        activeJob =
            scope.launch {
                previousJob?.join()
                block()
            }
    }

    /** Cancels the active job without nulling it or joining — used by `pause()`. */
    fun cancel() {
        activeJob?.cancel()
    }

    /**
     * Cancels any active job without joining or clearing engine-owned resume state. Call from
     * service onDestroy to stop movement without destroying the scope — engines are @Singleton
     * and must remain usable after the service is recreated.
     */
    fun cancelActiveReplay() {
        activeJob?.cancel()
        activeJob = null
    }

    /** Cancels and joins the active job under [jobMutex], then nulls it. Used by `stop()`. */
    suspend fun cancelAndJoinActive() {
        jobMutex.withLock {
            activeJob?.cancelAndJoin()
            activeJob = null
        }
    }

    /**
     * Releases the scope permanently. Only call when the engine will truly never be reused
     * (i.e. process teardown). Do NOT call from service onDestroy since the engine is a
     * @Singleton that outlives any single service instance.
     */
    override fun close() {
        activeJob?.cancel()
        scope.cancel()
    }
}
