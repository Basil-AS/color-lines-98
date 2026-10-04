package io.github.basil_as.basillines.engine

import kotlin.math.sqrt

/**
 * How the player thinks and when they play well, worked out from the play data of every game (mirrors src/cognition.ts: the
 * two apps must show the same findings for the same games). Scores of different modes are not comparable, so each game
 * gets an index against the average of its own mode (100 = a typical game of that mode).
 */
data class Bucket(val games: Int, val index: Int)
data class HourBucket(val hour: Int, val games: Int, val index: Int, val decisionMs: Int)
enum class Chronotype { LARK, DAY, EVENING, OWL }
data class BestWindow(val from: Int, val index: Int, val games: Int)
data class Phases(val early: Int, val mid: Int, val late: Int)
data class Tempo(val fast: Bucket, val mid: Bucket, val slow: Bucket)
data class Growth(val first: Int, val latest: Int, val percent: Int)

data class MindReport(
    val games: Int,
    val decisions: Long,
    val avgDecisionMs: Int,
    val variation: Double,
    val fastShare: Double,
    val slowShare: Double,
    val planning: Double,
    val byHour: List<HourBucket>,
    val bestWindow: BestWindow?,
    val chronotype: Chronotype?,
    /** Monday first. */
    val byWeekday: List<Bucket>,
    val phases: Phases,
    val sittings: List<Bucket>,
    val tempo: Tempo?,
    val tightest: Double,
    val dangerPer100: Double,
    val undosPer100: Double,
    val hintsPer100: Double,
    val missesPer100: Double,
    val clearingShare: Double,
    val growth: Growth?
)

enum class FindingId { WINDOW, CHRONOTYPE, TIRED, FRESH, TEMPO_FAST, TEMPO_SLOW, TEMPO_MID, SLOWDOWN, STEADY, ERRATIC, IMPULSIVE, PLANNER, TIGHT, CALM, GROWTH, WEEKDAY }

/** [a] and [b] are the numbers of the finding's sentence (which ones is up to the [id]; see the strings of the Mind tab). */
data class Finding(val id: FindingId, val a: Double = 0.0, val b: Double = 0.0, val c: Double = 0.0, val chronotype: Chronotype? = null)

object Mind {
    const val MIN_MOVES = 10
    const val NEEDED = 8
    private const val SESSION_GAP_MS = 45 * 60_000L
    private const val MIN_GROUP = 3

    private class Played(val r: GameRecord, val rel: Double, val start: Long, val decisionMs: Double)

    private fun avg(xs: List<Double>) = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size
    private fun bucket(xs: List<Double>) = Bucket(xs.size, Math.round(avg(xs)).toInt())
    private fun round1(v: Double) = Math.round(v * 10) / 10.0

    fun chronotypeOf(hour: Int) = when {
        hour in 5..10 -> Chronotype.LARK
        hour in 11..16 -> Chronotype.DAY
        hour in 17..22 -> Chronotype.EVENING
        else -> Chronotype.OWL
    }

    fun report(history: List<GameRecord>, fallbackOffsetMinutes: Int = java.util.TimeZone.getDefault().rawOffset / 60_000): MindReport {
        val means = ModeId.entries.associateWith { m -> avg(history.filter { it.mode == m && it.moves >= MIN_MOVES }.map { it.score.toDouble() }) }
        val played = history.mapNotNull { r ->
            val c = r.cog ?: return@mapNotNull null
            if (c[CogKey.TM] == 0L || r.moves < MIN_MOVES) return@mapNotNull null
            val mean = means[r.mode] ?: 0.0
            Played(r, if (mean > 0) 100.0 * r.score / mean else 100.0, r.endedAt - r.durationMs, c[CogKey.THINK].toDouble() / c[CogKey.TM])
        }
        fun sum(k: CogKey) = played.sumOf { it.r.cog!![k] }
        fun local(p: Played) = Telemetry.localTime(p.start, p.r.cog!![CogKey.TZ], fallbackOffsetMinutes)

        val decisions = sum(CogKey.TM)
        val think = sum(CogKey.THINK)
        val moves = played.sumOf { it.r.moves.toLong() }
        fun per100(n: Long) = if (moves == 0L) 0.0 else 100.0 * n / moves

        val variations = played.mapNotNull { p ->
            val c = p.r.cog!!
            if (c[CogKey.TM] < 5) return@mapNotNull null
            val mean = c[CogKey.THINK].toDouble() / c[CogKey.TM]
            val variance = maxOf(0.0, c[CogKey.THINK_SQ] * 10_000.0 / c[CogKey.TM] - mean * mean)
            if (mean > 0) sqrt(variance) / mean else 0.0
        }

        val hours = (0 until 24).map { hour ->
            val inHour = played.filter { local(it).first == hour }
            HourBucket(hour, inHour.size, Math.round(avg(inHour.map { it.rel })).toInt(), Math.round(avg(inHour.map { it.decisionMs })).toInt())
        }

        var bestWindow: BestWindow? = null
        for (from in 0 until 24) {
            val parts = (0..2).map { hours[(from + it) % 24] }
            val games = parts.sumOf { it.games }
            if (games < MIN_GROUP * 2) continue
            val index = parts.sumOf { it.index.toDouble() * it.games } / games
            if (bestWindow == null || index > bestWindow.index) bestWindow = BestWindow(from, Math.round(index).toInt(), games)
        }
        val overall = avg(played.map { it.rel })
        if (bestWindow != null && (played.size < 12 || bestWindow.index < overall + 3)) bestWindow = null

        val byWeekday = (0 until 7).map { d -> bucket(played.filter { local(it).second == d }.map { it.rel }) }

        fun phase(p: CogKey, n: CogKey): Int { val count = sum(n); return if (count == 0L) 0 else Math.round(sum(p).toDouble() / count).toInt() }

        val chrono = history.sortedBy { it.endedAt }
        val relOf = HashMap<GameRecord, Double>().also { m -> played.forEach { m[it.r] = it.rel } }
        val groups = List(4) { mutableListOf<Double>() }
        var position = 0
        var lastEnd = Long.MIN_VALUE / 2
        for (g in chrono) {
            val start = g.endedAt - g.durationMs
            position = if (start - lastEnd > SESSION_GAP_MS) 1 else position + 1
            lastEnd = g.endedAt
            relOf[g]?.let { groups[minOf(position, 4) - 1].add(it) }
        }

        var tempo: Tempo? = null
        if (played.size >= 9) {
            val sorted = played.sortedBy { it.decisionMs }
            val third = sorted.size / 3
            tempo = Tempo(
                bucket(sorted.take(third).map { it.rel }),
                bucket(sorted.subList(third, sorted.size - third).map { it.rel }),
                bucket(sorted.takeLast(third).map { it.rel })
            )
        }

        var growth: Growth? = null
        val byTime = played.sortedBy { it.r.endedAt }
        if (byTime.size >= 100) {
            val first = Math.round(avg(byTime.take(50).map { it.rel })).toInt()
            val latest = Math.round(avg(byTime.takeLast(50).map { it.rel })).toInt()
            growth = Growth(first, latest, if (first > 0) Math.round(100.0 * (latest - first) / first).toInt() else 0)
        }

        return MindReport(
            games = played.size,
            decisions = decisions,
            avgDecisionMs = if (decisions == 0L) 0 else Math.round(think.toDouble() / decisions).toInt(),
            variation = Math.round(avg(variations) * 100) / 100.0,
            fastShare = if (decisions == 0L) 0.0 else sum(CogKey.FAST).toDouble() / decisions,
            slowShare = if (decisions == 0L) 0.0 else sum(CogKey.SLOW).toDouble() / decisions,
            planning = if (think == 0L) 0.0 else sum(CogKey.LAT).toDouble() / think,
            byHour = hours,
            bestWindow = bestWindow,
            chronotype = bestWindow?.let { chronotypeOf((it.from + 1) % 24) },
            byWeekday = byWeekday,
            phases = Phases(phase(CogKey.P1, CogKey.N1), phase(CogKey.P2, CogKey.N2), phase(CogKey.P3, CogKey.N3)),
            sittings = groups.map { bucket(it) },
            tempo = tempo,
            tightest = round1(avg(played.map { it.r.cog!![CogKey.MIN_EMPTY].toDouble() })),
            dangerPer100 = round1(per100(sum(CogKey.DANGER))),
            undosPer100 = round1(per100(sum(CogKey.UNDO))),
            hintsPer100 = round1(per100(sum(CogKey.HINT))),
            missesPer100 = round1(per100(sum(CogKey.MISS))),
            clearingShare = if (moves == 0L) 0.0 else sum(CogKey.CLEARS).toDouble() / moves,
            growth = growth
        )
    }

    /** Plain findings, each only when the data supports it. */
    fun findings(r: MindReport): List<Finding> {
        val out = mutableListOf<Finding>()
        if (r.games < NEEDED) return out
        r.bestWindow?.let {
            out += Finding(FindingId.WINDOW, it.from.toDouble(), ((it.from + 3) % 24).toDouble(), (it.index - 100).toDouble())
            r.chronotype?.let { t -> out += Finding(FindingId.CHRONOTYPE, chronotype = t) }
        }
        val s1 = r.sittings[0]; val s3 = r.sittings[2]; val s4 = r.sittings[3]
        val laterGames = s3.games + s4.games
        if (laterGames >= MIN_GROUP * 2 && s1.games >= MIN_GROUP) {
            val later = (s3.index.toDouble() * s3.games + s4.index.toDouble() * s4.games) / laterGames
            val drop = Math.round(s1.index - later).toInt()
            if (drop >= 6) out += Finding(FindingId.TIRED, drop.toDouble())
            else if (drop <= -6) out += Finding(FindingId.FRESH, (-drop).toDouble())
        }
        r.tempo?.let { t ->
            val best = maxOf(t.fast.index, t.mid.index, t.slow.index)
            if (best - minOf(t.fast.index, t.mid.index, t.slow.index) >= 6) {
                val id = when (best) { t.fast.index -> FindingId.TEMPO_FAST; t.slow.index -> FindingId.TEMPO_SLOW; else -> FindingId.TEMPO_MID }
                out += Finding(id, (best - 100).toDouble())
            }
        }
        if (r.phases.early > 0 && r.phases.late > 0 && r.phases.late.toDouble() / r.phases.early >= 1.3) {
            out += Finding(FindingId.SLOWDOWN, Math.round(100.0 * (r.phases.late - r.phases.early) / r.phases.early).toDouble())
        }
        if (r.variation > 0) out += Finding(if (r.variation <= 0.8) FindingId.STEADY else FindingId.ERRATIC, r.variation)
        if (r.fastShare >= 0.35) out += Finding(FindingId.IMPULSIVE, Math.round(r.fastShare * 100).toDouble())
        else if (r.planning >= 0.6) out += Finding(FindingId.PLANNER, Math.round(r.planning * 100).toDouble())
        if (r.dangerPer100 >= 25) out += Finding(FindingId.TIGHT, Math.round(r.dangerPer100).toDouble())
        else if (r.tightest >= 20) out += Finding(FindingId.CALM, r.tightest)
        r.growth?.let { g -> if (Math.abs(g.percent) >= 5) out += Finding(FindingId.GROWTH, g.percent.toDouble(), g.first.toDouble(), g.latest.toDouble()) }
        val days = r.byWeekday.filter { it.games >= MIN_GROUP }
        if (days.size >= 4) {
            val top = days.reduce { a, b -> if (b.index > a.index) b else a }
            if (top.index >= 108) out += Finding(FindingId.WEEKDAY, r.byWeekday.indexOf(top).toDouble(), (top.index - 100).toDouble())
        }
        return out
    }
}
