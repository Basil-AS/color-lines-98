package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class MindTest {
    private val MIN = 60_000L

    private fun cog(decisionMs: Long = 3000, over: Map<CogKey, Long> = emptyMap()): Cognition {
        val tm = 40L
        val base = mutableMapOf(
            CogKey.TZ to 100L, CogKey.TM to tm, CogKey.THINK to tm * decisionMs, CogKey.THINK_SQ to Math.round(tm * Math.pow(decisionMs / 100.0, 2.0)),
            CogKey.MIN_EMPTY to 30L, CogKey.P1 to 20 * decisionMs, CogKey.N1 to 20L, CogKey.P2 to 20 * decisionMs, CogKey.N2 to 20L
        )
        base.putAll(over)
        return Cognition(CogKey.entries.map { base[it] ?: 0L })
    }

    private fun game(day: Int, hour: Int, score: Int, decisionMs: Long = 3000, mode: ModeId = ModeId.CLASSIC, over: Map<CogKey, Long> = emptyMap(), endedAt: Long? = null): GameRecord {
        val durationMs = 10 * MIN
        val start = LocalDateTime.of(2026, 3, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return GameRecord(score, endedAt ?: (start + durationMs), 40, 3, 15, true, 5, durationMs, mode, cog(decisionMs, over))
    }

    @Test
    fun noPlayDataNoFindings() {
        val r = Mind.report(listOf(game(1, 9, 100).copy(cog = null)))
        assertEquals(0, r.games)
        assertTrue(Mind.findings(r).isEmpty())
    }

    @Test
    fun findsTheBestTimeOfDay() {
        val h = mutableListOf<GameRecord>()
        for (d in 1..10) { h += game(d, 21, 400); h += game(d, 8, 200); h += game(d, 14, 250) }
        val r = Mind.report(h, 0)
        assertEquals(30, r.games)
        assertEquals(10, r.byHour[21].games)
        assertTrue(r.byHour[21].index > r.byHour[8].index)
        assertTrue(r.bestWindow!!.from in 19..21)
        assertEquals(Chronotype.EVENING, r.chronotype)
        assertTrue(Mind.findings(r).any { it.id == FindingId.WINDOW })
    }

    @Test
    fun indexesEveryGameAgainstItsOwnMode() {
        val h = (1..6).map { game(it, 10, 1000, mode = ModeId.BLITZ) } + (1..6).map { game(it, 10, 100) }
        assertEquals(100, Mind.report(h, 0).byHour[10].index)
    }

    @Test
    fun measuresTempoGroups() {
        val h = (0 until 12).map { game(it + 1, 10, 100 + it * 20, 1000L + it * 500) }
        val r = Mind.report(h, 0)
        assertEquals(3750.0, r.avgDecisionMs.toDouble(), 100.0)
        assertTrue(r.tempo!!.slow.index > r.tempo!!.fast.index)
        assertTrue(Mind.findings(r).any { it.id == FindingId.TEMPO_SLOW })
    }

    @Test
    fun noticesTirednessOverASitting() {
        val h = (0 until 30).map { i ->
            val score = listOf(500, 300, 200)[i % 3]
            game(1, 10, score, endedAt = LocalDateTime.of(2026, 3, i / 3 + 1, 10, 10 + (i % 3) * 15).toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        val r = Mind.report(h, 0)
        assertEquals(10, r.sittings[0].games)
        assertTrue(r.sittings[0].index > r.sittings[2].index)
        assertTrue(Mind.findings(r).any { it.id == FindingId.TIRED })
    }

    @Test
    fun countsDangerHelpAndMissesPer100Moves() {
        val over = mapOf(CogKey.DANGER to 20L, CogKey.UNDO to 2L, CogKey.HINT to 1L, CogKey.MISS to 4L, CogKey.CLEARS to 10L, CogKey.MIN_EMPTY to 5L)
        val r = Mind.report((1..10).map { game(it, 10, 200, over = over) }, 0)
        assertEquals(50.0, r.dangerPer100, 0.0); assertEquals(5.0, r.undosPer100, 0.0); assertEquals(2.5, r.hintsPer100, 0.0)
        assertEquals(10.0, r.missesPer100, 0.0); assertEquals(0.25, r.clearingShare, 0.0); assertEquals(5.0, r.tightest, 0.0)
        assertTrue(Mind.findings(r).any { it.id == FindingId.TIGHT })
    }

    @Test
    fun comparesFirstAndLatestOnceThereAreEnough() {
        val h = (0 until 120).map { game((it % 28) + 1, 10, 100 + it, endedAt = 1_767_225_600_000L + it * 86_400_000L) }
        val g = Mind.report(h, 0).growth!!
        assertTrue(g.latest > g.first)
    }

    @Test
    fun namesThePartOfTheDay() {
        assertEquals(listOf(Chronotype.OWL, Chronotype.LARK, Chronotype.DAY, Chronotype.EVENING, Chronotype.OWL), listOf(3, 8, 13, 19, 23).map { Mind.chronotypeOf(it) })
    }

    @Test
    fun yearsAndMemories() {
        fun at(y: Int, m: Int, d: Int) = LocalDateTime.of(y, m, d, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        fun rec(score: Int, at: Long) = GameRecord(score, at, 30, 2, 10, true, 5, 60_000)
        val ledger = Careers.fromHistory(listOf(rec(300, at(2024, 3, 5)), rec(500, at(2024, 3, 6)), rec(100, at(2024, 7, 1)), rec(900, at(2025, 3, 5))), ZoneOffset.UTC)
        val years = Careers.yearly(ledger)
        assertEquals(listOf(2024, 2025), years.map { it.year })
        assertEquals(3, years[0].games); assertEquals(900L, years[0].score); assertEquals(500, years[0].best); assertEquals("2024-03", years[0].bestMonth)
        assertEquals(listOf(Careers.Memory("2025-03-05", 1, 1, 900), Careers.Memory("2024-03-05", 2, 1, 300)), Careers.memories(ledger, "2026-03-05"))
        assertEquals(listOf(Careers.Memory("2024-03-05", 0, 1, 300)), Careers.memories(ledger, "2024-04-05"))
        assertTrue(Careers.memories(ledger, "2026-01-01").isEmpty())
    }
}
