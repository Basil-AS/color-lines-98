package io.github.basil_as.basillines.engine

import io.github.basil_as.basillines.engine.Insights.Direction
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class InsightsTest {
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        LocalDate.of(y, m, d).atTime(h, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun g(score: Int, endedAt: Long = at(2026, 9, 29), moves: Int = 20, lines: Int = 2) =
        GameRecord(score, endedAt, moves, lines, 10, true, 5, 60_000)

    @Test
    fun trendNeedsEnoughGamesAndComparesTheLatestWithThePrevious() {
        assertNull(Insights.trendOf(listOf(g(10), g(20)), 5))
        val up = Insights.trendOf(List(5) { g(120) } + List(5) { g(100) }, 5)!!
        assertEquals(120, up.recentAverage)
        assertEquals(20, up.changePercent)
        assertEquals(Direction.UP, up.direction)
        assertEquals(Direction.DOWN, Insights.trendOf(List(5) { g(80) } + List(5) { g(100) }, 5)!!.direction)
        assertEquals(Direction.FLAT, Insights.trendOf(List(10) { g(100) }, 5)!!.direction)
        assertEquals(Direction.FLAT, Insights.trendOf(List(5) { g(101) } + List(5) { g(100) }, 5)!!.direction)
        assertEquals(100, Insights.trendOf(List(5) { g(50) } + List(5) { g(0) }, 5)!!.changePercent)
    }

    @Test
    fun dailyActivityHasOneEntryPerDayOldestFirst() {
        val history = listOf(g(50, at(2026, 9, 29, 20)), g(80, at(2026, 9, 29, 9)), g(30, at(2026, 9, 27)))
        val days = Insights.dailyActivity(history, 5, at(2026, 9, 29))
        assertEquals(listOf("2026-09-25", "2026-09-26", "2026-09-27", "2026-09-28", "2026-09-29"), days.map { it.day })
        assertEquals(listOf(0, 0, 1, 0, 2), days.map { it.games })
        assertEquals(80, days[4].best)
        assertTrue(Insights.dailyActivity(emptyList(), 3, at(2026, 9, 29)).all { it.games == 0 })
    }

    @Test
    fun histogramKeepsEmptyBucketsBetween() {
        assertEquals(listOf(3, 1), Insights.scoreHistogram(listOf(g(5), g(15), g(16), g(95)), 50).map { it.count })
        assertEquals(listOf(1, 0, 0, 1), Insights.scoreHistogram(listOf(g(10), g(160)), 50).map { it.count })
        assertTrue(Insights.scoreHistogram(emptyList(), 50).isEmpty())
    }

    @Test
    fun weekdaysStartOnMonday() {
        // 2026-09-28 is a Monday, 2026-09-29 a Tuesday.
        val w = Insights.weekdayActivity(listOf(g(10, at(2026, 9, 28)), g(30, at(2026, 9, 28)), g(50, at(2026, 9, 29))))
        assertEquals(Insights.WeekdayStat(0, 2, 20), w[0])
        assertEquals(Insights.WeekdayStat(1, 1, 50), w[1])
        assertEquals(Insights.WeekdayStat(6, 0, 0), w[6])
    }

    @Test
    fun efficiencyIsPointsPerMove() {
        val e = Insights.efficiency(listOf(g(100, moves = 50), g(30, moves = 10), g(0, moves = 0)))
        assertEquals(130.0 / 60, e.average, 1e-9)
        assertEquals(3.0, e.best, 1e-9)
        assertEquals(Insights.Efficiency(0.0, 0.0), Insights.efficiency(emptyList()))
    }

    @Test
    fun recordProgressionListsEveryImprovementOldestFirst() {
        val newestFirst = listOf(g(90, 4), g(40, 3), g(60, 2), g(30, 1))
        assertEquals(listOf(30 to 1L, 60 to 2L, 90 to 4L), Insights.recordProgression(newestFirst).map { it.score to it.at })
        assertTrue(Insights.recordProgression(emptyList()).isEmpty())
    }

    @Test
    fun levelEtaUsesTheRecentPace() {
        val recent = List(5) { g(40, lines = 2) } // 40 + 10 + 10 = 60 xp per game
        assertEquals(2, Insights.levelEta(recent, 120))
        assertEquals(0, Insights.levelEta(recent, 0))
        assertNull(Insights.levelEta(emptyList(), 100))
        assertEquals(10, Insights.levelEta(listOf(g(0, lines = 0)), 100))
    }
}
