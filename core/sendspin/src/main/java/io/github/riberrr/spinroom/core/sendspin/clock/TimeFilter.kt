package io.github.riberrr.spinroom.core.sendspin.clock

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Sendspin time filter: 2D Kalman filter on (offset = server − client, drift), docs/SENDSPIN.md §6.
 * Port of github.com/Sendspin/time-filter. Not thread-safe; [SendspinClock] guards it.
 */
class TimeFilter(
    private val processStdDev: Double = 0.0,
    private val driftProcessStdDev: Double = 1e-11,
    private val forgetFactor: Double = 2.0,
    private val adaptiveCutoff: Double = 3.0,
    private val minSamples: Int = 100,
    private val driftSignificance: Double = 2.0,
    private val maxErrorScale: Double = 0.5,
) {
    var offset = 0.0; private set
    var drift = 0.0; private set
    private var pOO = Double.POSITIVE_INFINITY
    private var pOD = 0.0
    private var pDD = 0.0
    var lastUpdate = 0L; private set
    var count = 0; private set
    var useDrift = false; private set

    /** Standard deviation of the offset estimate, µs (∞ before the first sample). */
    val errorUs: Double get() = sqrt(pOO)

    fun reset() {
        offset = 0.0; drift = 0.0; pOO = Double.POSITIVE_INFINITY; pOD = 0.0; pDD = 0.0
        lastUpdate = 0; count = 0; useDrift = false
    }

    /** Feeds one `server/time` exchange. Returns false when the sample was discarded. */
    fun addExchange(t1: Long, t2: Long, t3: Long, t4: Long): Boolean {
        val measurement = ((t2 - t1) + (t3 - t4)) / 2
        val maxError = ((t4 - t1) - (t3 - t2)) / 2
        return update(measurement, maxError, t4)
    }

    fun update(measurement: Long, maxError: Long, timeAdded: Long): Boolean {
        if (timeAdded <= lastUpdate || maxError <= 0) return false
        val dt = (timeAdded - lastUpdate).toDouble()
        lastUpdate = timeAdded
        val m = measurement.toDouble()
        val r = (maxErrorScale * maxError).let { it * it }

        when (count) {
            0 -> {
                offset = m; pOO = r; drift = 0.0; count = 1
                return true
            }
            1 -> {
                drift = (m - offset) / dt
                pDD = (pOO + r) / (dt * dt)
                offset = m; pOO = r; count = 2
                return true
            }
        }

        val offsetP = offset + drift * dt
        var pOOp = pOO + 2 * pOD * dt + pDD * dt * dt + processStdDev * processStdDev * dt
        var pODp = pOD + pDD * dt
        var pDDp = pDD + driftProcessStdDev * driftProcessStdDev * dt

        val y = m - offsetP
        if (count < minSamples) {
            count++
        } else if (abs(y) > adaptiveCutoff * maxError) {
            val f = forgetFactor * forgetFactor
            pOOp *= f; pODp *= f; pDDp *= f
        }

        val s = pOOp + r
        val kO = pOOp / s
        val kD = pODp / s
        offset = offsetP + kO * y
        drift += kD * y
        pOO = pOOp - kO * pOOp
        pOD = pODp - kD * pOOp
        pDD = pDDp - kD * pODp
        useDrift = drift * drift > driftSignificance * driftSignificance * pDD
        return true
    }

    fun serverTime(clientUs: Long): Long {
        val d = if (useDrift) drift else 0.0
        return clientUs + roundHalfAway(offset + d * (clientUs - lastUpdate))
    }

    fun clientTime(serverUs: Long): Long {
        val d = if (useDrift) drift else 0.0
        return roundHalfAway((serverUs - offset + d * lastUpdate) / (1 + d))
    }

    private fun roundHalfAway(x: Double): Long = (sign(x) * floor(abs(x) + 0.5)).toLong()
}
