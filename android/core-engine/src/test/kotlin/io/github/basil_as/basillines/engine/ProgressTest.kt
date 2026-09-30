package io.github.basil_as.basillines.engine

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ProgressTest {

    private fun at(y: Int, m: Int, d: Int, h: Int = 12): Long =
        LocalDate.of(y, m, d).atTime(h, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun game(
        score: Int = 40, endedAt: Long = at(2026, 9, 29), moves: Int = 30, lines: Int = 3,
        balls: Int = 15, completed: Boolean = true, maxLine: Int = 5, durationMs: Long = 120_000
    ) = GameRecord(score, endedAt, moves, lines, balls, completed, maxLine, durationMs)

    @Test
    fun levelThresholdsFollowTheFormula() {
        assertEquals(listOf(0, 100, 300, 600, 1000), (1..5).map { Levels.threshold(it) })
    }

    @Test
    fun levelInfoFindsLevelAndFraction() {
        assertEquals(1, Levels.info(0).level)
        assertEquals(100, Levels.info(0).needed)
        assertEquals(1, Levels.info(99).level)
        assertEquals(2, Levels.info(100).level)
        assertEquals(2, Levels.info(299).level)
        assertEquals(3, Levels.info(300).level)
        assertEquals(0.5, Levels.info(450).fraction, 1e-9)
    }

    @Test
    fun titlesNeverRunOut() {
        assertEquals(0, Levels.titleIndex(1))
        assertEquals(Levels.TITLE_COUNT - 1, Levels.titleIndex(1000))
        for (l in 2..60) assertTrue(Levels.titleIndex(l) >= Levels.titleIndex(l - 1))
    }

    @Test
    fun experienceCombinesScoreLinesAndGames() {
        val p = ProgressTracker.empty().copy(totalScore = 100, totalLines = 4, totalGames = 3)
        assertEquals(100 + 20 + 30, ProgressTracker.xpOf(p))
    }

    @Test
    fun applyGameAccumulatesTotalsAndBests() {
        var p = ProgressTracker.empty()
        p = ProgressTracker.applyGame(p, game(score = 40, lines = 3, moves = 30, maxLine = 5)).progress
        p = ProgressTracker.applyGame(p, game(score = 90, lines = 2, moves = 80, maxLine = 7, completed = false)).progress
        assertEquals(2, p.totalGames)
        assertEquals(1, p.completedGames)
        assertEquals(130, p.totalScore)
        assertEquals(5, p.totalLines)
        assertEquals(110, p.totalMoves)
        assertEquals(30, p.totalBalls)
        assertEquals(240_000L, p.totalPlayMs)
        assertEquals(90, p.bestScore)
        assertEquals(7, p.bestLine)
        assertEquals(3, p.mostLinesInGame)
        assertEquals(80, p.longestGameMoves)
    }

    @Test
    fun achievementsUnlockOnceAndAreReported() {
        val first = ProgressTracker.applyGame(ProgressTracker.empty(), game(lines = 1, score = 10))
        assertTrue(first.unlocked.containsAll(listOf("first_game", "first_line")))
        assertEquals(game().endedAt, first.progress.achievements["first_game"])
        val second = ProgressTracker.applyGame(first.progress, game(lines = 1, score = 10, endedAt = at(2026, 9, 30)))
        assertFalse("first_game" in second.unlocked)
        assertEquals(game().endedAt, second.progress.achievements["first_game"])
    }

    @Test
    fun scoreLineAndMarathonAchievements() {
        val r = ProgressTracker.applyGame(ProgressTracker.empty(), game(score = 260, maxLine = 9, moves = 200))
        assertTrue(r.unlocked.containsAll(listOf("score_100", "score_250", "long_line_7", "long_line_9", "marathon")))
        assertFalse("score_500" in r.unlocked)
    }

    @Test
    fun levelAchievementUnlocksWhenExperienceCrossesTheThreshold() {
        val p = ProgressTracker.empty().copy(totalScore = 950)
        val r = ProgressTracker.applyGame(p, game(score = 60, lines = 0))
        assertTrue(ProgressTracker.xpOf(r.progress) >= Levels.threshold(5))
        assertTrue("level_5" in r.unlocked)
    }

    @Test
    fun achievementIdsAreUnique() {
        val ids = ProgressTracker.ACHIEVEMENTS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun eachPlayDayIsRecordedOnce() {
        var p = ProgressTracker.empty()
        p = ProgressTracker.applyGame(p, game()).progress
        p = ProgressTracker.applyGame(p, game(endedAt = at(2026, 9, 29, 20))).progress
        p = ProgressTracker.applyGame(p, game(endedAt = at(2026, 9, 30))).progress
        assertEquals(listOf("2026-09-29", "2026-09-30"), p.days)
    }

    @Test
    fun streaks() {
        val days = listOf("2026-09-20", "2026-09-27", "2026-09-28", "2026-09-29")
        assertEquals(3, ProgressTracker.currentStreak(days, "2026-09-29"))
        assertEquals(3, ProgressTracker.currentStreak(days, "2026-09-30"))
        assertEquals(0, ProgressTracker.currentStreak(days, "2026-10-02"))
        assertEquals(0, ProgressTracker.currentStreak(emptyList(), "2026-09-29"))
        assertEquals(3, ProgressTracker.bestStreak(days))
        assertEquals(4, ProgressTracker.bestStreak(listOf("2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04", "2026-02-01")))
        assertEquals(0, ProgressTracker.bestStreak(emptyList()))
        assertEquals(2, ProgressTracker.bestStreak(listOf("2025-12-31", "2026-01-01")))
        assertEquals(2, ProgressTracker.currentStreak(listOf("2026-02-28", "2026-03-01"), "2026-03-01"))
    }

    @Test
    fun streakAchievementUnlocks() {
        var p = ProgressTracker.empty()
        val unlocked = mutableListOf<String>()
        for (d in 27..29) {
            val r = ProgressTracker.applyGame(p, game(endedAt = at(2026, 9, d)))
            p = r.progress
            unlocked += r.unlocked
        }
        assertTrue("streak_3" in unlocked)
    }

    @Test
    fun rebuildEqualsApplyingOldestFirst() {
        val newestFirst = listOf(game(score = 120, endedAt = at(2026, 9, 29), maxLine = 6), game(score = 30, endedAt = at(2026, 9, 28), lines = 1))
        var expected = ProgressTracker.empty()
        for (g in newestFirst.reversed()) expected = ProgressTracker.applyGame(expected, g).progress
        assertEquals(expected, ProgressTracker.rebuild(newestFirst))
        assertEquals(ProgressTracker.empty(), ProgressTracker.rebuild(emptyList()))
    }

    @Test
    fun codecRoundTripsAndRejectsJunk() {
        val played = ProgressTracker.applyGame(ProgressTracker.empty(), game()).progress
        assertEquals(played, ProgressTracker.decode(ProgressTracker.encode(played)))
        for (bad in listOf(null, "", "garbage", "v9|1|2", "v1|a,b|x|y")) {
            assertEquals(ProgressTracker.empty(), ProgressTracker.decode(bad))
        }
    }

    @Test
    fun codecDropsInvalidPartsButKeepsValidOnes() {
        val played = ProgressTracker.applyGame(ProgressTracker.empty(), game()).progress
        val parts = ProgressTracker.encode(played).split("|").toMutableList()
        parts[2] = "2026-09-29,yesterday,7"
        parts[3] = "first_game:5,nonsense:1,first_line:x"
        val clean = ProgressTracker.decode(parts.joinToString("|"))
        assertEquals(listOf("2026-09-29"), clean.days)
        assertEquals(mapOf("first_game" to 5L), clean.achievements)
        assertEquals(played.totalGames, clean.totalGames)
    }

    @Test
    fun scoreTrendReturnsLatestOldestFirst() {
        val history = (5 downTo 1).map { game(score = it) }
        assertEquals(listOf(3, 4, 5), ProgressTracker.scoreTrend(history, 3))
        assertEquals(listOf(1, 2, 3, 4, 5), ProgressTracker.scoreTrend(history, 10))
        assertEquals(emptyList<Int>(), ProgressTracker.scoreTrend(emptyList(), 5))
    }
}
