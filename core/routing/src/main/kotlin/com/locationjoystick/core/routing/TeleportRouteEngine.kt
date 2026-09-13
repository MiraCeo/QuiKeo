package com.locationjoystick.core.routing

import android.util.Log
import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TeleportRouteEngine"

/**
 * Engine for replaying a "teleport route": instantly jumps between waypoints, waiting each
 * waypoint's configured duration before jumping to the next.
 *
 * Ticks at the same 1 Hz cadence as [RouteReplayEngine] so the mock fix never goes stale while
 * waiting (mirrors paused route replay — see docs/features/routes.md, "Replay"). Only one replay
 * active at a time.
 */
@Singleton
class TeleportRouteEngine
    @Inject
    constructor() :
    AutoCloseable,
        RouteReplayer {
        private val jobController = ReplayJobController(TAG)

        @Volatile private var resumeIndex: Int = 0

        @Volatile private var resumeRemainingWaitMs: Long = 0L
        private val savedWaypointsRef = AtomicReference<List<LatLng>>(emptyList())
        private val savedWaitSecondsRef = AtomicReference<List<Int>>(emptyList())

        @Volatile private var isLooping: Boolean = false

        @Volatile private var boundaryIndices: List<Int> = emptyList()

        /**
         * @param waypoints Positions in order.
         * @param waitSecondsPerWaypoint Same length as [waypoints]; seconds to wait at each
         *   before jumping onward.
         */
        fun start(
            waypoints: List<LatLng>,
            waitSecondsPerWaypoint: List<Int>,
            isLooping: Boolean = false,
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ) {
            savedWaypointsRef.set(waypoints)
            savedWaitSecondsRef.set(waitSecondsPerWaypoint)
            this.isLooping = isLooping
            this.boundaryIndices = waypoints.indices.toList()
            resumeIndex = 0
            resumeRemainingWaitMs = waitMsFor(0)
            launchReplay(onPositionUpdate, onComplete)
            Log.i(TAG, "Teleport replay started: ${waypoints.size} waypoints looping=$isLooping")
        }

        override fun resume(
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ) {
            launchReplay(onPositionUpdate, onComplete)
            Log.i(TAG, "Teleport replay resumed at index $resumeIndex")
        }

        override fun pause() {
            jobController.activeJob?.cancel()
            Log.i(TAG, "Teleport replay paused at index $resumeIndex")
        }

        override suspend fun stop() {
            jobController.cancelAndJoinActive()
            savedWaypointsRef.set(emptyList())
            savedWaitSecondsRef.set(emptyList())
            resumeIndex = 0
            resumeRemainingWaitMs = 0L
            Log.i(TAG, "Teleport replay stopped")
        }

        /** Jumps to [target] (clamped), resetting its wait timer to full — same call whichever direction. */
        private fun jumpToWaypoint(
            target: Int,
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ): LatLng? {
            val waypoints = savedWaypointsRef.get()
            if (waypoints.isEmpty()) return null
            val clamped = target.coerceIn(0, waypoints.size - 1)
            val wasRunning = jobController.activeJob?.isActive == true
            jobController.activeJob?.cancel()
            resumeIndex = clamped
            resumeRemainingWaitMs = waitMsFor(clamped)
            if (wasRunning) launchReplay(onPositionUpdate, onComplete)
            Log.i(TAG, "Jumped to waypoint $clamped, wait timer reset")
            return waypoints[clamped]
        }

        override fun jumpToNextWaypoint(
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ): LatLng? = jumpToWaypoint(nextBoundaryAtOrAfter(boundaryIndices, resumeIndex + 1), onPositionUpdate, onComplete)

        override fun jumpToPreviousWaypoint(
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ): LatLng? = jumpToWaypoint(previousBoundaryBefore(boundaryIndices, resumeIndex), onPositionUpdate, onComplete)

        /**
         * Cancels any active replay job. Call from service onDestroy to stop movement without
         * destroying the scope — the engine is a @Singleton and must remain usable after the
         * service is recreated.
         */
        fun cancelActiveReplay() {
            jobController.cancelActiveReplay()
        }

        override fun close() {
            jobController.close()
        }

        private fun waitMsFor(index: Int): Long = savedWaitSecondsRef.get().getOrElse(index) { 0 }.coerceAtLeast(0) * 1000L

        private fun launchReplay(
            onPositionUpdate: (LatLng) -> Unit,
            onComplete: () -> Unit,
        ) {
            val previousJob = jobController.activeJob
            previousJob?.cancel()
            val snapshot = savedWaypointsRef.get()
            if (snapshot.size < 2) {
                onComplete()
                return
            }
            var index = resumeIndex
            var remainingWaitMs = resumeRemainingWaitMs

            jobController.activeJob =
                jobController.scope.launch {
                    previousJob?.join()
                    onPositionUpdate(snapshot[index])
                    while (isActive) {
                        delay(AppConstants.LocationConstants.UPDATE_INTERVAL_MS)
                        remainingWaitMs -= AppConstants.LocationConstants.UPDATE_INTERVAL_MS
                        resumeRemainingWaitMs = remainingWaitMs
                        if (remainingWaitMs > 0) {
                            // Still waiting: re-push the frozen position so the fix never goes stale.
                            try {
                                onPositionUpdate(snapshot[index])
                            } catch (e: Exception) {
                                Log.e(TAG, "onPositionUpdate failed", e)
                            }
                            continue
                        }
                        val atEnd = index >= snapshot.size - 1
                        if (atEnd) {
                            if (isLooping) {
                                index = 0
                            } else {
                                if (isActive) {
                                    try {
                                        onComplete()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "onComplete failed", e)
                                    }
                                }
                                break
                            }
                        } else {
                            index += 1
                        }
                        remainingWaitMs = waitMsFor(index)
                        resumeIndex = index
                        resumeRemainingWaitMs = remainingWaitMs
                        try {
                            onPositionUpdate(snapshot[index])
                        } catch (e: Exception) {
                            Log.e(TAG, "onPositionUpdate failed", e)
                        }
                    }
                }
        }
    }
