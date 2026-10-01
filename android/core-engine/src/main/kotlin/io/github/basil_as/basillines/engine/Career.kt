package io.github.basil_as.basillines.engine

import java.time.LocalDate
import java.time.ZoneId

/** One day of play in the permanent ledger (the game history only keeps the latest 1000 games). */
data class DayEntry(
    val games: Int = 0,
    val completed: Int = 0,
    val score: Long = 0,
    val best: Int = 0,
    val moves: Long = 0,
    val lines: Long = 0,
    val playMs: Long = 0
)

typealias Ledger = Map<String, DayEntry>

/** Long-term analysis; mirrors src/ledger.ts and src/career.ts so both platforms tell the same story. */
object Careers {
    private val DAY_RE = Regex("""\d{4}-\d{2}-\d{2}""")

    fun addGame(ledger: Ledger, r: GameRecord, zone: ZoneId = ZoneId.systemDefault()): Ledger {
        val key = ProgressTracker.dayKey(r.endedAt, zone)
        val d = ledger[key] ?: DayEntry()
        return ledger + (key to DayEntry(
            d.games + 1, d.completed + if (r.completed) 1 else 0, d.score + r.score, maxOf(d.best, r.score),
            d.moves + r.moves, d.lines + r.lines, d.playMs + r.durationMs
        ))
    }

    fun fromHistory(history: List<GameRecord>, zone: ZoneId = ZoneId.systemDefault()): Ledger =
        history.fold(emptyMap()) { l, r -> addGame(l, r, zone) }

    /** Day by day, keeps whichever side saw more games. */
    fun merge(a: Ledger, b: Ledger): Ledger {
        val out = a.toMutableMap()
        for ((k, v) in b) if (out[k] == null || v.games > out[k]!!.games) out[k] = v
        return out
    }

    /** Two ledgers, then every day re-counted from the merged games, taking the fuller of each (same-day games of two devices all stay). */
    fun mergeWithHistory(a: Ledger, b: Ledger, mergedHistory: List<GameRecord>, zone: ZoneId = ZoneId.systemDefault()): Ledger =
        merge(merge(a, b), fromHistory(mergedHistory, zone))

    fun sanitize(raw: Any?): Ledger {
        val map = raw as? Map<*, *> ?: return emptyMap()
        val out = linkedMapOf<String, DayEntry>()
        for ((k, v) in map) {
            val key = k as? String ?: continue
            val o = v as? Map<*, *> ?: continue
            if (!ProgressTracker.isDay(key)) continue
            // Bounded, so a hostile file cannot overflow the sums.
            fun n(name: String): Long = (o[name] as? Double)?.takeIf { it >= 0 && it.isFinite() }?.coerceAtMost(1e12)?.toLong() ?: 0L
            val games = n("games").coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (games <= 0) continue
            out[key] = DayEntry(games, n("completed").coerceAtMost(games.toLong()).toInt(), n("score"), n("best").coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), n("moves"), n("lines"), n("playMs"))
        }
        return out
    }

    fun encode(ledger: Ledger): Map<String, Any?> = ledger.toSortedMap().mapValues { (_, d) ->
        mapOf("games" to d.games, "completed" to d.completed, "score" to d.score, "best" to d.best, "moves" to d.moves, "lines" to d.lines, "playMs" to d.playMs)
    }

    // ---- totals, streaks, months ---------------------------------------------------------

    data class Totals(val games: Int, val completed: Int, val score: Long, val lines: Long, val moves: Long, val playMs: Long, val activeDays: Int)

    fun totals(l: Ledger) = Totals(
        l.values.sumOf { it.games }, l.values.sumOf { it.completed }, l.values.sumOf { it.score },
        l.values.sumOf { it.lines }, l.values.sumOf { it.moves }, l.values.sumOf { it.playMs }, l.size
    )

    fun streaks(l: Ledger, today: String): Pair<Int, Int> {
        val days = l.keys.map { LocalDate.parse(it).toEpochDay() }.sorted()
        var longest = 0
        var run = 0
        days.forEachIndexed { i, d -> run = if (i > 0 && d - days[i - 1] == 1L) run + 1 else 1; longest = maxOf(longest, run) }
        val set = days.toSet()
        var cursor = LocalDate.parse(today).toEpochDay()
        if (cursor !in set) cursor--
        var current = 0
        while (cursor in set) { current++; cursor-- }
        return current to longest
    }

    data class MonthStat(val month: String, val games: Int, val score: Long, val best: Int, val average: Int, val activeDays: Int, val playMs: Long)

    fun monthly(l: Ledger): List<MonthStat> = l.entries.groupBy { it.key.take(7) }.map { (m, days) ->
        val games = days.sumOf { it.value.games }
        val score = days.sumOf { it.value.score }
        MonthStat(m, games, score, days.maxOf { it.value.best }, Math.round(score.toDouble() / games).toInt(), days.size, days.sumOf { it.value.playMs })
    }.sortedBy { it.month }

    // ---- seasons ---------------------------------------------------------------------------

    enum class Tier { BRONZE, SILVER, GOLD, PLATINUM, LEGEND }

    private val TIER_FROM = doubleArrayOf(0.0, 0.5, 1.0, 1.5, 2.5)
    private const val DEFAULT_BASELINE = 1500L

    fun baseline(past: List<MonthStat>): Long {
        val recent = past.takeLast(6).map { it.score }.filter { it > 0 }.sorted()
        if (recent.isEmpty()) return DEFAULT_BASELINE
        val mid = recent.size / 2
        val median = if (recent.size % 2 == 1) recent[mid] else Math.round((recent[mid - 1] + recent[mid]) / 2.0)
        return maxOf(200L, median)
    }

    fun tierOf(points: Long, baseline: Long): Tier {
        var tier = 0
        TIER_FROM.forEachIndexed { i, f -> if (points >= f * baseline) tier = i }
        return Tier.entries[tier]
    }

    data class Season(
        val month: String, val points: Long, val games: Int, val baseline: Long, val tier: Tier,
        val nextTier: Tier?, val toNext: Long?, val daysLeft: Int,
        val past: List<Triple<String, Long, Tier>>, val bestMonth: MonthStat?
    )

    fun season(l: Ledger, today: LocalDate): Season {
        val current = today.toString().take(7)
        val all = monthly(l)
        val before = all.filter { it.month < current }
        val mine = all.firstOrNull { it.month == current }
        val base = baseline(before)
        val points = mine?.score ?: 0L
        val tier = tierOf(points, base)
        val next = Tier.entries.getOrNull(tier.ordinal + 1)
        return Season(
            current, points, mine?.games ?: 0, base, tier, next,
            next?.let { maxOf(0L, Math.ceil(TIER_FROM[tier.ordinal + 1] * base).toLong() - points) },
            today.lengthOfMonth() - today.dayOfMonth,
            before.mapIndexed { i, m -> Triple(m.month, m.score, tierOf(m.score, baseline(before.take(i)))) }.takeLast(12).reversed(),
            all.maxByOrNull { it.score }
        )
    }

    // ---- endless milestones ----------------------------------------------------------------

    data class Ladder(val reached: Int, val previous: Double, val next: Double, val fraction: Double)

    fun ladder(value: Double, unit: Double): Ladder {
        val steps = doubleArrayOf(1.0, 2.5, 5.0)
        var reached = 0
        var previous = 0.0
        var scale = 1.0
        while (true) {
            for (s in steps) {
                val target = Math.round(s * scale * unit * 1000) / 1000.0
                if (value < target) return Ladder(reached, previous, target, (value - previous) / (target - previous))
                previous = target
                reached++
            }
            scale *= 10
        }
    }

    enum class Track(val unit: Double) { GAMES(10.0), SCORE(1000.0), LINES(10.0), HOURS(1.0), DAYS(1.0) }

    fun milestones(t: Totals): List<Triple<Track, Double, Ladder>> {
        val hours = Math.floor(t.playMs / 360_000.0) / 10
        val values = mapOf(Track.GAMES to t.games.toDouble(), Track.SCORE to t.score.toDouble(), Track.LINES to t.lines.toDouble(), Track.HOURS to hours, Track.DAYS to t.activeDays.toDouble())
        return Track.entries.map { Triple(it, values.getValue(it), ladder(values.getValue(it), it.unit)) }
    }

    // ---- heatmap ---------------------------------------------------------------------------

    /** Weeks as columns, Monday first; null after today. Level 0..4 relative to the busiest day. */
    fun heatmap(l: Ledger, today: LocalDate, weeks: Int): List<List<Pair<String, Int>?>> {
        val max = maxOf(1, l.values.maxOfOrNull { it.games } ?: 1)
        val start = today.minusDays((today.dayOfWeek.value - 1).toLong() + (weeks - 1) * 7L)
        return List(weeks) { w ->
            List(7) { r ->
                val d = start.plusDays(w * 7L + r)
                if (d.isAfter(today)) null else {
                    val games = l[d.toString()]?.games ?: 0
                    d.toString() to if (games == 0) 0 else Math.ceil(games.toDouble() / max * 4).toInt().coerceIn(1, 4)
                }
            }
        }
    }
}
