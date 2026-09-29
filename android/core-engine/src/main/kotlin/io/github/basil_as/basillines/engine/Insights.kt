package io.github.basil_as.basillines.engine

import java.time.Instant
import java.time.ZoneId

/** Analysis of the recorded games. All inputs are newest first, as kept in the history. */
object Insights {
    enum class Direction { UP, DOWN, FLAT }

    data class Trend(val recentAverage: Int, val previousAverage: Int, val changePercent: Int, val direction: Direction)
    data class DayActivity(val day: String, val games: Int, val best: Int)
    data class HistogramBar(val from: Int, val count: Int)
    data class WeekdayStat(val weekday: Int, val games: Int, val average: Int)
    data class Efficiency(val average: Double, val best: Double)
    data class RecordStep(val score: Int, val at: Long)

    private fun average(values: List<Int>) = if (values.isEmpty()) 0.0 else values.sum().toDouble() / values.size

    /** Average of the latest [window] games against the [window] before them; null until both exist. */
    fun trendOf(newestFirst: List<GameRecord>, window: Int = 10): Trend? {
        if (newestFirst.size < window * 2) return null
        val recent = average(newestFirst.take(window).map { it.score })
        val previous = average(newestFirst.drop(window).take(window).map { it.score })
        val change = if (previous == 0.0) (if (recent == 0.0) 0 else 100) else Math.round((recent - previous) / previous * 100).toInt()
        val direction = when {
            Math.abs(change) < 3 -> Direction.FLAT
            change > 0 -> Direction.UP
            else -> Direction.DOWN
        }
        return Trend(Math.round(recent).toInt(), Math.round(previous).toInt(), change, direction)
    }

    /** The last [days] calendar days ending at [nowMs], oldest first. */
    fun dailyActivity(newestFirst: List<GameRecord>, days: Int, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): List<DayActivity> {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val byDay = linkedMapOf<String, IntArray>() // games, best
        for (i in days - 1 downTo 0) byDay[today.minusDays(i.toLong()).toString()] = intArrayOf(0, 0)
        for (g in newestFirst) {
            val entry = byDay[Instant.ofEpochMilli(g.endedAt).atZone(zone).toLocalDate().toString()] ?: continue
            entry[0]++
            entry[1] = maxOf(entry[1], g.score)
        }
        return byDay.map { (day, v) -> DayActivity(day, v[0], v[1]) }
    }

    /** Scores in buckets of [size], including empty buckets between the lowest and highest. */
    fun scoreHistogram(games: List<GameRecord>, size: Int): List<HistogramBar> {
        if (games.isEmpty()) return emptyList()
        val last = games.maxOf { it.score } / size
        val counts = IntArray(last + 1)
        for (g in games) counts[g.score / size]++
        return counts.mapIndexed { i, c -> HistogramBar(i * size, c) }
    }

    /** Monday first: 0 = Monday ... 6 = Sunday. */
    fun weekdayActivity(games: List<GameRecord>, zone: ZoneId = ZoneId.systemDefault()): List<WeekdayStat> {
        val games7 = IntArray(7)
        val total = IntArray(7)
        for (g in games) {
            val weekday = Instant.ofEpochMilli(g.endedAt).atZone(zone).dayOfWeek.value - 1
            games7[weekday]++
            total[weekday] += g.score
        }
        return (0 until 7).map { WeekdayStat(it, games7[it], if (games7[it] == 0) 0 else Math.round(total[it].toDouble() / games7[it]).toInt()) }
    }

    /** Points per move: the overall rate and the best single game. */
    fun efficiency(games: List<GameRecord>): Efficiency {
        val score = games.sumOf { it.score }
        val moves = games.sumOf { it.moves }
        val best = games.filter { it.moves > 0 }.maxOfOrNull { it.score.toDouble() / it.moves } ?: 0.0
        return Efficiency(if (moves == 0) 0.0 else score.toDouble() / moves, best)
    }

    /** Every time the personal best improved, oldest first. */
    fun recordProgression(newestFirst: List<GameRecord>): List<RecordStep> {
        val steps = mutableListOf<RecordStep>()
        var best = 0
        for (g in newestFirst.asReversed()) {
            if (g.score > best) {
                best = g.score
                steps += RecordStep(g.score, g.endedAt)
            }
        }
        return steps
    }

    /** Games needed to earn [remainingXp] at the pace of the latest games; null without any games. */
    fun levelEta(newestFirst: List<GameRecord>, remainingXp: Int, window: Int = 10): Int? {
        val recent = newestFirst.take(window)
        if (recent.isEmpty()) return null
        val perGame = average(recent.map { it.score + it.lines * 5 + 10 })
        return if (remainingXp <= 0) 0 else Math.ceil(remainingXp / perGame).toInt()
    }
}
