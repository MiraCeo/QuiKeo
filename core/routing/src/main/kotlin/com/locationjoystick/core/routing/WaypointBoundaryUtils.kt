package com.locationjoystick.core.routing

/**
 * Nearest boundary index at or after [idx], clamped to the last boundary. Falls back to [idx]
 * itself when [boundaryIndices] is empty.
 */
internal fun nextBoundaryAtOrAfter(
    boundaryIndices: List<Int>,
    idx: Int,
): Int = boundaryIndices.firstOrNull { it >= idx } ?: boundaryIndices.lastOrNull() ?: idx

/** Nearest boundary index strictly before [idx], clamped to the first boundary (or [idx], see above). */
internal fun previousBoundaryBefore(
    boundaryIndices: List<Int>,
    idx: Int,
): Int = boundaryIndices.lastOrNull { it < idx } ?: boundaryIndices.firstOrNull() ?: idx
