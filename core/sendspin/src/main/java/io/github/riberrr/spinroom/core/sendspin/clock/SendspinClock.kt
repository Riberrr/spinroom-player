package io.github.riberrr.spinroom.core.sendspin.clock

import io.github.riberrr.spinroom.core.sendspin.api.ClockSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [ClockSource] on top of [TimeFilter]. Thread-safe: the session feeds it, the audio thread reads it.
 * Before the first sample the mapping is the identity.
 */
class SendspinClock(
    private val localNow: () -> Long = { System.nanoTime() / 1000 },
    /** Minimum number of accepted samples before [converged] turns true. */
    private val minSamplesForConvergence: Int = 3,
    /** Offset standard deviation (µs) below which the filter counts as converged. */
    private val maxErrorForConvergenceUs: Double = 2_000.0,
) : ClockSource {
    private val filter = TimeFilter()
    private val lock = Any()
    private val _converged = MutableStateFlow(false)
    override val converged: StateFlow<Boolean> = _converged.asStateFlow()

    override fun localNowUs(): Long = localNow()

    override fun serverToLocalUs(serverUs: Long): Long = synchronized(lock) {
        if (filter.count == 0) serverUs else filter.clientTime(serverUs)
    }

    override fun localToServerUs(localUs: Long): Long = synchronized(lock) {
        if (filter.count == 0) localUs else filter.serverTime(localUs)
    }

    /** Current offset estimate standard deviation in µs (∞ before the first sample). */
    val errorUs: Double get() = synchronized(lock) { filter.errorUs }

    /** Feeds one exchange (T1..T4, µs). */
    fun addExchange(t1: Long, t2: Long, t3: Long, t4: Long): Boolean {
        val accepted = synchronized(lock) { filter.addExchange(t1, t2, t3, t4) }
        updateConverged()
        return accepted
    }

    /** Drops all state (new server / new clock domain). */
    fun reset() {
        synchronized(lock) { filter.reset() }
        updateConverged()
    }

    private fun updateConverged() {
        _converged.value = synchronized(lock) {
            filter.count >= minSamplesForConvergence && filter.errorUs <= maxErrorForConvergenceUs
        }
    }
}

/** Collects one burst of `client/time` exchanges and keeps the one with the smallest max_error. */
internal class TimeBurst(val size: Int) {
    var sent = 0; private set
    private var best: LongArray? = null
    private var bestError = Long.MAX_VALUE

    fun onSent() { sent++ }

    fun onReply(t1: Long, t2: Long, t3: Long, t4: Long) {
        val maxError = ((t4 - t1) - (t3 - t2)) / 2
        if (maxError in 1 until bestError) {
            bestError = maxError
            best = longArrayOf(t1, t2, t3, t4)
        }
    }

    val complete: Boolean get() = sent >= size

    fun best(): LongArray? = best
}
