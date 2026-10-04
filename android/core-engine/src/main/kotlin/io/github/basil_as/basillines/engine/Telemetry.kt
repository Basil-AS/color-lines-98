package io.github.basil_as.basillines.engine

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** The numbers kept about how a game was played; the same names and meaning as src/engine/telemetry.ts. */
enum class CogKey(val json: String) {
    TZ("tz"), TM("tm"), THINK("think"), THINK_MAX("thinkMax"), THINK_SQ("thinkSq"), FAST("fast"), SLOW("slow"), LAT("lat"),
    FIRST("first"), P1("p1"), N1("n1"), P2("p2"), N2("n2"), P3("p3"), N3("n3"), UNDO("undo"), HINT("hint"), MISS("miss"),
    CLEARS("clears"), MULTI("multi"), DANGER("danger"), MIN_EMPTY("minEmpty")
}

/** The play data of a finished game: plain non-negative integers in [CogKey] order. */
data class Cognition(val values: List<Long>) {
    init { require(values.size == CogKey.entries.size) }
    operator fun get(k: CogKey): Long = values[k.ordinal]

    companion object {
        /** Validates untrusted data; null when anything is out of range. */
        fun of(values: List<Long>): Cognition? {
            if (values.size != CogKey.entries.size || values.any { it < 0 || it > 1_000_000_000_000L }) return null
            if (values[CogKey.TZ.ordinal] > 400) return null
            val v = values.toMutableList()
            v[CogKey.MIN_EMPTY.ordinal] = min(v[CogKey.MIN_EMPTY.ordinal], 81)
            return Cognition(v)
        }
    }
}

object Telemetry {
    const val FAST_MS = 2000
    const val SLOW_MS = 10_000
    const val DANGER_CELLS = 12
    const val MAX_THINK_MS = 60_000L

    /** Local time zone in quarter hours from UTC plus 100 (always positive). */
    fun zoneCode(offsetMinutes: Int): Long = (offsetMinutes / 15 + 100).toLong()
    fun offsetMinutes(tz: Long): Int = ((tz - 100) * 15).toInt()

    /** Local hour (0-23) and weekday (0 = Monday) of an instant seen from a stored zone. */
    fun localTime(at: Long, tz: Long?, fallbackOffsetMinutes: Int): Pair<Int, Int> {
        val offset = if (tz == null) fallbackOffsetMinutes else offsetMinutes(tz)
        val shifted = at + offset * 60_000L
        val days = Math.floorDiv(shifted, 86_400_000L)
        val ms = Math.floorMod(shifted, 86_400_000L)
        // 1970-01-01 was a Thursday (3 with Monday = 0).
        return (ms / 3_600_000L).toInt() to Math.floorMod(days + 3, 7L).toInt()
    }
}

/** The live counters of the game in play (mirrors the Telemetry class of the web engine). */
class TelemetryCounters {
    private val c = LongArray(CogKey.entries.size).also { it[CogKey.MIN_EMPTY.ordinal] = 81 }
    private var pend = 0L
    private var lat = -1L
    private var unknown = false

    private operator fun get(k: CogKey) = c[k.ordinal]
    private fun inc(k: CogKey, by: Long = 1) { c[k.ordinal] += by }

    /** A touch on the board; [deltaMs] is the time since the previous touch, null when that is not known. */
    fun action(deltaMs: Long?) {
        if (deltaMs == null || deltaMs < 0) {
            unknown = true
            if (lat < 0) lat = 0
            return
        }
        val ms = min(deltaMs, Telemetry.MAX_THINK_MS)
        pend += ms
        if (lat < 0) lat = ms
    }

    fun moved(moves: Int, lines: Int, empty: Int) {
        if (!unknown && pend > 0) {
            val ms = pend
            inc(CogKey.TM)
            inc(CogKey.THINK, ms)
            c[CogKey.THINK_MAX.ordinal] = maxOf(this[CogKey.THINK_MAX], ms)
            inc(CogKey.THINK_SQ, (ms / 100.0).pow(2).roundToLong())
            if (ms < Telemetry.FAST_MS) inc(CogKey.FAST)
            if (ms > Telemetry.SLOW_MS) inc(CogKey.SLOW)
            inc(CogKey.LAT, maxOf(0, lat))
            if (this[CogKey.TM] == 1L) c[CogKey.FIRST.ordinal] = ms
            when {
                moves <= 20 -> { inc(CogKey.P1, ms); inc(CogKey.N1) }
                moves <= 60 -> { inc(CogKey.P2, ms); inc(CogKey.N2) }
                else -> { inc(CogKey.P3, ms); inc(CogKey.N3) }
            }
        }
        if (lines > 0) inc(CogKey.CLEARS)
        if (lines > 1) inc(CogKey.MULTI)
        if (empty <= Telemetry.DANGER_CELLS) inc(CogKey.DANGER)
        c[CogKey.MIN_EMPTY.ordinal] = min(this[CogKey.MIN_EMPTY], empty.toLong())
        pend = 0; lat = -1; unknown = false
    }

    fun undo() = inc(CogKey.UNDO)
    fun hint() = inc(CogKey.HINT)
    fun miss() = inc(CogKey.MISS)

    /** The numbers without the time zone, for a saved game. */
    fun toList(): List<Long> = CogKey.entries.filter { it != CogKey.TZ }.map { this[it] }

    fun load(raw: List<Long>?) {
        val keys = CogKey.entries.filter { it != CogKey.TZ }
        if (raw == null || raw.size != keys.size || raw.any { it < 0 }) return
        keys.forEachIndexed { i, k -> c[k.ordinal] = raw[i] }
    }

    fun snapshot(tz: Long): Cognition = Cognition(c.toMutableList().also { it[CogKey.TZ.ordinal] = tz })
}
