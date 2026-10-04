package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class TelemetryTest {
    private fun c(t: TelemetryCounters) = t.snapshot(100)

    @Test
    fun measuresDecisionsAndSpread() {
        val t = TelemetryCounters()
        t.action(1500); t.action(500); t.moved(1, 0, 70)
        t.action(12000); t.moved(2, 1, 60)
        val s = c(t)
        assertEquals(2L, s[CogKey.TM]); assertEquals(14000L, s[CogKey.THINK]); assertEquals(12000L, s[CogKey.THINK_MAX])
        assertEquals(1L, s[CogKey.SLOW]); assertEquals(0L, s[CogKey.FAST]); assertEquals(13500L, s[CogKey.LAT])
        assertEquals(2000L, s[CogKey.FIRST]); assertEquals(14000L, s[CogKey.P1]); assertEquals(1L, s[CogKey.CLEARS])
    }

    @Test
    fun aMoveWithUnknownStartIsNotCounted() {
        val t = TelemetryCounters()
        t.action(null); t.action(900); t.moved(1, 0, 70)
        assertEquals(0L, c(t)[CogKey.TM])
        t.action(900); t.moved(2, 0, 70)
        assertEquals(1L, c(t)[CogKey.TM])
    }

    @Test
    fun phasesDangerAndMulti() {
        val t = TelemetryCounters()
        for (m in 1..70) { t.action(3000); t.moved(m, if (m == 5) 2 else 0, if (m >= 65) 10 else 50) }
        val s = c(t)
        assertEquals(listOf(20L, 40L, 10L), listOf(s[CogKey.N1], s[CogKey.N2], s[CogKey.N3]))
        assertEquals(1L, s[CogKey.MULTI]); assertEquals(6L, s[CogKey.DANGER]); assertEquals(10L, s[CogKey.MIN_EMPTY])
    }

    @Test
    fun aBreakIsClippedToAMinute() {
        val t = TelemetryCounters()
        t.action(9_999_999); t.moved(1, 0, 70)
        assertEquals(60_000L, c(t)[CogKey.THINK_MAX])
    }

    @Test
    fun savedGameArrayRoundTripsAndIgnoresGarbage() {
        val t = TelemetryCounters()
        t.action(2500); t.moved(1, 0, 70); t.undo(); t.hint(); t.miss()
        val copy = TelemetryCounters().also { it.load(t.toList()) }
        assertEquals(t.toList(), copy.toList())
        assertEquals(0L, c(TelemetryCounters().also { it.load(listOf(1, 2)) })[CogKey.TM])
        assertEquals(81L, c(TelemetryCounters())[CogKey.MIN_EMPTY])
    }

    @Test
    fun validatesStoredPlayData() {
        assertNull(Cognition.of(List(CogKey.entries.size - 1) { 0L }))
        assertNull(Cognition.of(List(CogKey.entries.size) { -1L }))
        assertNotNull(Cognition.of(List(CogKey.entries.size) { 0L }))
    }

    @Test
    fun readsLocalHourAndWeekdayInAStoredZone() {
        // 2026-03-02 (Monday) 22:30 UTC seen from UTC+3 is 01:30 on Tuesday.
        val at = java.time.LocalDateTime.of(2026, 3, 2, 22, 30).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        assertEquals(1 to 1, Telemetry.localTime(at, 112, 0))
        assertEquals(22 to 0, Telemetry.localTime(at, 100, 0))
    }

    @Test
    fun theEngineFeedsTheCounters() {
        val e = GameEngine(random = Random(7))
        var moved = 0
        var guard = 0
        while (moved < 5 && guard++ < 400) {
            e.noteAction(2000)
            loop@ for (y in 0 until 9) for (x in 0 until 9) {
                if (e.board[x, y] == null) continue
                for (ty in 0 until 9) for (tx in 0 until 9) {
                    if (e.board[tx, ty] != null) continue
                    if (e.moveBall(Point(x, y), Point(tx, ty)).success) { moved++; break@loop }
                }
            }
        }
        assertEquals(moved.toLong(), e.tel.snapshot(100)[CogKey.TM])
        assertEquals(moved * 2000L, e.tel.snapshot(100)[CogKey.THINK])
        e.noteHint(); assertTrue(e.undo())
        assertEquals(1L, e.tel.snapshot(100)[CogKey.HINT]); assertEquals(1L, e.tel.snapshot(100)[CogKey.UNDO])
        val back = GameStateCodec.decode(GameStateCodec.encode(e))!!
        assertEquals(e.tel.toList(), back.tel.toList())
    }
}
