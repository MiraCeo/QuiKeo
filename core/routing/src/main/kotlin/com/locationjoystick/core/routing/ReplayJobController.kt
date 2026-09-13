package com.locationjoystick.core.routing

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coroutine-lifecycle scaffolding shared by [RouteReplayEngine] and [TeleportRouteEngine]:
 * the replay scope, the current job, and its cancellation. Both engines own one instance and
 * keep their own tick logic (`launchReplay`, interpolation vs. wait-and-jump) untouched — only
 * the scaffolding that was identical byte-for-byte between them lives here.
 */
internal class ReplayJobController(
    tag: String,
) : AutoCloseable {
    private val exceptionHandler =
        CoroutineExceptionHandler { _, throwable -> Log.e(tag, "Replay coroutine crashed", throwable) }

    /** Scope for replay coroutines. Uses SupervisorJob so failures don't propagate. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)

    /** Serializes cancel+launch to prevent stale-job races on concurrent start/pause/resume calls. */
    private val jobMutex = Mutex()

    /** Current replay job. Read/write freely for cancel-and-replace; mutate to null only under [jobMutex]. */
    @Volatile var activeJob: Job? = null

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
