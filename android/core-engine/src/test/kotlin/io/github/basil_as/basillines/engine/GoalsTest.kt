package io.github.basil_as.basillines.engine

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsTest {
    private val utc = ZoneId.of("UTC")
    private fun at(day: Int, hour: Int = 12) = java.time.LocalDateTime.of(2026, 9, day, hour, 0).atZone(utc).toInstant().toEpochMilli()
    private fun g(score: Int, time: Long, moves: Int, lines: Int, maxLine: Int, mode: ModeId, completed: Boolean = true) =
        GameRecord(score, time, moves, lines, lines * 5, completed, maxLine, 1, mode)

    private val history = listOf(
        g(340, at(20, 18), 61, 6, 7, ModeId.CLASSIC),
        g(120, at(20, 10), 33, 2, 5, ModeId.BLITZ),
        g(200, at(18), 44, 4, 6, ModeId.DAILY),
        g(90, at(17), 25, 1, 5, ModeId.EASY, false),
        g(410, at(15), 80, 9, 9, ModeId.CLASSIC)
    )

    private fun show(day: String, h: List<GameRecord> = history) =
        Goals.daily(h, day, utc).joinToString(" ") { "${it.type.name.lowercase()}=${it.target}" }

    @Test
    fun goalsMatchTheWebApp() {
        // Values computed by dailyGoals() in src/goals.ts for the same games.
        assertEquals("score=340.0 line=8.0 efficiency=5.4", show("2026-09-21"))
        assertEquals("score=340.0 efficiency=5.4 line=8.0", show("2026-09-22"))
        assertEquals("score=340.0 lines=7.0 line=8.0", show("2026-09-30"))
        assertEquals("score=340.0 lines=7.0 beat_yesterday=341.0", show("2026-10-05"))
        assertEquals("score=60.0 lines=3.0 line=5.0", show("2026-09-30", emptyList()))
    }

    @Test
    fun alwaysThreeDifferentGoalsWithAScoreGoal() {
        for (day in 1..28) {
            val goals = Goals.daily(history, "2026-09-%02d".format(day), utc)
            assertEquals(3, goals.size)
            assertEquals(3, goals.map { it.type }.toSet().size)
            assertTrue(goals.any { it.type == GoalType.SCORE })
        }
    }

    @Test
    fun progressUsesTodaysBestAndIgnoresEasy() {
        val goals = Goals.daily(history, "2026-09-30", utc)
        val today = listOf(g(400, at(30), 70, 8, 8, ModeId.CLASSIC), g(9000, at(30), 70, 8, 8, ModeId.EASY))
        val progress = Goals.evaluate(goals, today)
        assertTrue(progress.first { it.goal.type == GoalType.SCORE }.done)
        assertEquals(400.0, progress.first { it.goal.type == GoalType.SCORE }.value, 0.0)
        assertFalse(Goals.evaluate(goals, emptyList()).any { it.done })
    }

    @Test
    fun bonusIsPaidOncePerGoal() {
        val b = Goals.bonus(listOf("a"), listOf("a", "b", "c"), 3)
        assertEquals(listOf("b", "c"), b.newlyDone)
        assertEquals(50, b.xp)
        assertTrue(b.allDone)
        assertFalse(Goals.bonus(listOf("a", "b", "c"), listOf("a", "b", "c"), 3).allDone)
        assertEquals(0, Goals.bonus(listOf("a"), listOf("a"), 3).xp)
    }

    @Test
    fun goalExperienceCountsAndSurvivesSavingAndOldSaves() {
        val base = ProgressTracker.rebuild(listOf(g(300, at(20), 50, 4, 6, ModeId.CLASSIC)))
        val withBonus = ProgressTracker.applyGoalBonus(base, "2026-09-21", Goals.bonus(emptyList(), listOf("a", "b", "c"), 3))
        assertEquals(ProgressTracker.xpOf(base) + 75, ProgressTracker.xpOf(withBonus))
        assertEquals(listOf("2026-09-21"), withBonus.goalDays)
        val back = ProgressTracker.decode(ProgressTracker.encode(withBonus))
        assertEquals(75, back.bonusXp)
        assertEquals(withBonus.goalDays, back.goalDays)
        // A save made before goals existed still loads, with no bonus.
        val v1 = "v1|" + ProgressTracker.encode(base).split("|").let { "${it[1]}|${it[2]}|${it[3]}" }
        assertEquals(0, ProgressTracker.decode(v1).bonusXp)
        assertEquals(base.totalGames, ProgressTracker.decode(v1).totalGames)
        // Playing on keeps the bonus.
        assertEquals(75, ProgressTracker.applyGame(withBonus, g(10, at(21), 5, 0, 0, ModeId.CLASSIC)).progress.bonusXp)
    }
}
