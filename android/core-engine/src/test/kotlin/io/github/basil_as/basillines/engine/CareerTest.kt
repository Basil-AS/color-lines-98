package io.github.basil_as.basillines.engine

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerTest {
    private val utc: ZoneId = ZoneId.of("UTC")
    private fun at(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(utc).plusHours(12).toInstant().toEpochMilli()
    private fun g(score: Int, time: Long, completed: Boolean = true) = GameRecord(score, time, 30, 3, 15, completed, 5, 60_000, ModeId.CLASSIC)

    private val ledger = Careers.fromHistory(
        listOf(g(50, at(2026, 8, 30)), g(70, at(2026, 8, 31)), g(200, at(2026, 9, 1)), g(400, at(2026, 9, 5)), g(100, at(2026, 9, 5))), utc
    )

    @Test
    fun totalsStreaksAndMonthsMatchTheWebApp() {
        assertEquals(Careers.Totals(5, 5, 820, 15, 150, 300_000, 4), Careers.totals(ledger))
        assertEquals(3 to 3, Careers.streaks(ledger, "2026-09-01"))
        assertEquals(3 to 3, Careers.streaks(ledger, "2026-09-02"))
        assertEquals(0 to 3, Careers.streaks(ledger, "2026-09-04"))
        assertEquals(1, Careers.streaks(ledger, "2026-09-05").first)
        val m = Careers.monthly(ledger)
        assertEquals(listOf("2026-08", "2026-09"), m.map { it.month })
        assertEquals(Careers.MonthStat("2026-09", 3, 700, 400, 233, 2, 180_000), m[1])
    }

    @Test
    fun seasonsAreJudgedAgainstYourTypicalMonth() {
        assertEquals(1500L, Careers.baseline(emptyList()))
        assertEquals(Careers.Tier.BRONZE, Careers.tierOf(999, 2000))
        assertEquals(Careers.Tier.SILVER, Careers.tierOf(1000, 2000))
        assertEquals(Careers.Tier.GOLD, Careers.tierOf(2000, 2000))
        assertEquals(Careers.Tier.PLATINUM, Careers.tierOf(3000, 2000))
        assertEquals(Careers.Tier.LEGEND, Careers.tierOf(5000, 2000))
        val l = mapOf(
            "2026-07-10" to DayEntry(4, 4, 2000, 700, 1, 1, 1),
            "2026-08-10" to DayEntry(4, 4, 1000, 400, 1, 1, 1),
            "2026-09-10" to DayEntry(2, 2, 1200, 800, 1, 1, 1)
        )
        val s = Careers.season(l, LocalDate.of(2026, 9, 20))
        assertEquals("2026-09", s.month)
        assertEquals(1500L, s.baseline)
        assertEquals(Careers.Tier.SILVER, s.tier)
        assertEquals(Careers.Tier.GOLD, s.nextTier)
        assertEquals(300L, s.toNext)
        assertEquals(10, s.daysLeft)
        assertEquals(listOf("2026-08", "2026-07"), s.past.map { it.first })
        assertEquals("2026-07", s.bestMonth?.month)
        val top = Careers.season(mapOf("2026-01-02" to DayEntry(1, 1, 99999, 99999, 1, 1, 1)), LocalDate.of(2026, 1, 15))
        assertEquals(Careers.Tier.LEGEND, top.tier)
        assertNull(top.toNext)
    }

    @Test
    fun milestonesNeverRunOut() {
        assertEquals(Careers.Ladder(0, 0.0, 10.0, 0.0), Careers.ladder(0.0, 10.0))
        assertEquals(25.0, Careers.ladder(10.0, 10.0).next, 0.0)
        assertEquals(100.0, Careers.ladder(99.0, 10.0).next, 0.0)
        assertEquals(250.0, Careers.ladder(100.0, 10.0).next, 0.0)
        assertEquals(2.5, Careers.ladder(2.4, 1.0).next, 0.0)
        val far = Careers.ladder(3_000_000.0, 1000.0)
        assertTrue(far.next > 3_000_000 && far.fraction in 0.0..1.0)
        val tracks = Careers.milestones(Careers.Totals(120, 100, 40_000, 300, 1, 7_200_000, 12))
        assertEquals(2.0, tracks.first { it.first == Careers.Track.HOURS }.second, 0.0)
        assertEquals(250.0, tracks.first { it.first == Careers.Track.GAMES }.third.next, 0.0)
    }

    @Test
    fun heatmapIsWholeWeeksMondayFirstWithoutTheFuture() {
        val today = LocalDate.of(2026, 9, 30) // Wednesday
        val l = Careers.fromHistory(listOf(g(1, at(2026, 9, 30)), g(1, at(2026, 9, 30)), g(1, at(2026, 9, 28))), utc)
        val grid = Careers.heatmap(l, today, 4)
        assertEquals(4, grid.size)
        assertTrue(grid.all { it.size == 7 })
        assertEquals("2026-09-28" to 2, grid[3][0])
        assertEquals("2026-09-30" to 4, grid[3][2])
        assertNull(grid[3][3])
    }

    @Test
    fun mergeKeepsTheFullerDayAndSanitizeDropsGarbage() {
        val a = Careers.fromHistory(listOf(g(1, at(2026, 9, 1))), utc)
        val b = Careers.fromHistory(listOf(g(1, at(2026, 9, 1)), g(1, at(2026, 9, 1))), utc)
        assertEquals(2, Careers.merge(a, b)["2026-09-01"]!!.games)
        assertEquals(2, Careers.merge(b, a)["2026-09-01"]!!.games)
        val dirty = Json.parse("""{"2026-01-01":{"games":2,"completed":9,"score":-5,"best":"x"},"nope":{"games":1},"2026-02-02":{"games":0}}""")
        assertEquals(mapOf("2026-01-01" to DayEntry(2, 2, 0, 0, 0, 0, 0)), Careers.sanitize(dirty))
        assertTrue(Careers.sanitize(null).isEmpty())
    }

    @Test
    fun twoDevicesPlayingTheSameDayKeepAllGames() {
        val mine = listOf(g(100, at(2026, 9, 1)), g(200, at(2026, 9, 1)))
        val theirs = listOf(g(300, at(2026, 9, 1)))
        val merged = Careers.mergeWithHistory(Careers.fromHistory(mine, utc), Careers.fromHistory(theirs, utc), theirs + mine, utc)
        assertEquals(3, merged["2026-09-01"]!!.games)
        assertEquals(600L, merged["2026-09-01"]!!.score)
    }
}
