package io.github.basil_as.basillines.engine

import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily goals built from the player's own results, so they stay a fair stretch whatever the skill level
 * (mirrors src/goals.ts): a weak player is asked for a bit more than the average, a strong one for a lot more.
 */
enum class GoalType { SCORE, LINE, MOVES, LINES, EFFICIENCY, BEAT_YESTERDAY }

data class Goal(val id: String, val type: GoalType, val target: Double)

data class GoalProgress(val goal: Goal, val value: Double, val done: Boolean)

data class GoalBonus(val newlyDone: List<String>, val xp: Int, val allDone: Boolean)

object Goals {
    const val BONUS_XP = 25
    private val POOL = listOf(GoalType.LINE, GoalType.MOVES, GoalType.LINES, GoalType.EFFICIENCY, GoalType.BEAT_YESTERDAY)
    private const val MIN_EFFICIENCY_MOVES = 15

    /** Easy mode has fewer colours, so its results would distort the targets. */
    private fun counts(g: GameRecord) = g.mode != ModeId.EASY

    private fun dayStart(dayKey: String, zone: ZoneId): Long = LocalDate.parse(dayKey).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun mean(v: List<Double>) = if (v.isEmpty()) 0.0 else v.sum() / v.size

    /** `Math.round` in JavaScript rounds halves up, like [Math.floor] of x + 0.5. */
    private fun round(x: Double) = Math.floor(x + 0.5)

    fun daily(newestFirst: List<GameRecord>, dayKey: String, zone: ZoneId = ZoneId.systemDefault()): List<Goal> {
        val start = dayStart(dayKey, zone)
        val past = newestFirst.filter { counts(it) && it.endedAt < start }.take(20)
        val beginner = past.size < 3

        val scores = past.map { it.score.toDouble() }
        val avg = mean(scores)
        val best = scores.maxOrNull() ?: 0.0
        val scoreTarget = if (beginner) 60.0 else Math.ceil(maxOf(avg + 0.5 * (best - avg), avg + 10) / 10) * 10

        val lineTarget = minOf(9.0, maxOf(5.0, round(mean(past.map { it.maxLine.toDouble() })) + 1))
        val movesTarget = if (beginner) 30.0 else maxOf(20.0, round(mean(past.map { it.moves.toDouble() }) * 1.2))
        val linesTarget = if (beginner) 3.0 else maxOf(2.0, round(mean(past.map { it.lines.toDouble() }) * 1.25))
        val eff = past.filter { it.moves > 0 }.map { it.score.toDouble() / it.moves }
        val efficiencyTarget = if (beginner || eff.isEmpty()) 1.5 else maxOf(1.0, round(mean(eff) * 1.15 * 10) / 10)

        var yesterdayBest = 50.0
        if (past.isNotEmpty()) {
            val lastDay = java.time.Instant.ofEpochMilli(past[0].endedAt).atZone(zone).toLocalDate()
            val from = lastDay.atStartOfDay(zone).toInstant().toEpochMilli()
            yesterdayBest = past.filter { it.endedAt >= from }.maxOf { it.score } + 1.0
        }

        val target = mapOf(
            GoalType.SCORE to scoreTarget, GoalType.LINE to lineTarget, GoalType.MOVES to movesTarget,
            GoalType.LINES to linesTarget, GoalType.EFFICIENCY to efficiencyTarget, GoalType.BEAT_YESTERDAY to yesterdayBest
        )
        val h = hashString(dayKey)
        val a = (h % POOL.size).toInt()
        val b = ((a + 1 + ((h ushr 8) % (POOL.size - 1)).toInt()) % POOL.size)
        return listOf(GoalType.SCORE, POOL[a], POOL[b]).map { Goal("$dayKey:${it.name}", it, target.getValue(it)) }
    }

    private fun valueOf(type: GoalType, games: List<GameRecord>): Double {
        val list = games.filter { counts(it) }
        return when (type) {
            GoalType.SCORE, GoalType.BEAT_YESTERDAY -> list.maxOfOrNull { it.score.toDouble() } ?: 0.0
            GoalType.LINE -> list.maxOfOrNull { it.maxLine.toDouble() } ?: 0.0
            GoalType.MOVES -> list.maxOfOrNull { it.moves.toDouble() } ?: 0.0
            GoalType.LINES -> list.maxOfOrNull { it.lines.toDouble() } ?: 0.0
            GoalType.EFFICIENCY -> list.filter { it.moves >= MIN_EFFICIENCY_MOVES }.maxOfOrNull { it.score.toDouble() / it.moves } ?: 0.0
        }
    }

    fun evaluate(goals: List<Goal>, todaysGames: List<GameRecord>): List<GoalProgress> =
        goals.map { g -> valueOf(g.type, todaysGames).let { GoalProgress(g, it, it >= g.target) } }

    fun bonus(alreadyDone: List<String>, nowDone: List<String>, total: Int): GoalBonus {
        val newly = nowDone.filter { it !in alreadyDone }
        return GoalBonus(newly, newly.size * BONUS_XP, nowDone.size >= total && newly.isNotEmpty())
    }
}
