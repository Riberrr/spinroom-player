package io.github.riberrr.spinroom.core.sendspin.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Maps between the server clock and the local monotonic clock (microseconds).
 * Implemented in S02 on top of the Kalman time filter (docs/SENDSPIN.md §6); consumed by S04 for scheduling.
 */
interface ClockSource {
    /** Local monotonic time in microseconds (e.g. System.nanoTime() / 1000). */
    fun localNowUs(): Long

    /** Converts a server timestamp (e.g. AudioChunk.timestampUs) to local monotonic time. */
    fun serverToLocalUs(serverUs: Long): Long

    /** Converts local monotonic time to the server clock. */
    fun localToServerUs(localUs: Long): Long

    /** True once the filter converged enough to schedule playback (gate for `available: true`). */
    val converged: StateFlow<Boolean>
}
