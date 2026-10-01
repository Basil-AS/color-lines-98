package io.github.basil_as.basillines.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything the player has achieved over all games, independent of the capped history list. */
data class Progress(
    val totalGames: Int = 0,
    val completedGames: Int = 0,
    val totalScore: Int = 0,
    val totalLines: Int = 0,
    val totalBalls: Int = 0,
    val totalMoves: Int = 0,
    val totalPlayMs: Long = 0,
    val bestScore: Int = 0,
    val bestLine: Int = 0,
    val mostLinesInGame: Int = 0,
    val longestGameMoves: Int = 0,
    /** Local calendar days (yyyy-MM-dd) with at least one finished game, ascending. */
    val days: List<String> = emptyList(),
    /** Achievement id to the time it was unlocked (epoch millis). */
    val achievements: Map<String, Long> = emptyMap(),
    /** Experience earned from daily goals (beyond what the games themselves give). */
    val bonusXp: Int = 0,
    /** Days on which all daily goals were completed. */
    val goalDays: List<String> = emptyList(),
    /** Games played and best score in every mode. */
    val gamesByMode: Map<ModeId, Int> = ModeId.entries.associateWith { 0 },
    val bestByMode: Map<ModeId, Int> = ModeId.entries.associateWith { 0 }
)

data class LevelInfo(val level: Int, val xp: Int, val into: Int, val needed: Int) {
    val fraction: Double get() = into.toDouble() / needed
}

data class AppliedGame(val progress: Progress, val unlocked: List<String>)

class Achievement(val id: String, val test: (Progress, GameRecord, Int, Int) -> Boolean)

object Levels {
    const val TITLE_COUNT = 7
    private val TITLE_FROM_LEVEL = intArrayOf(1, 3, 5, 7, 10, 15, 20)

    /** Experience needed to reach level [n] (level 1 needs none). */
    fun threshold(n: Int): Int = 50 * n * (n - 1)

    fun info(xp: Int): LevelInfo {
        var level = 1
        while (threshold(level + 1) <= xp) level++
        return LevelInfo(level, xp, xp - threshold(level), threshold(level + 1) - threshold(level))
    }

    fun titleIndex(level: Int): Int = TITLE_FROM_LEVEL.indexOfLast { level >= it }.coerceAtLeast(0)
}

object ProgressTracker {
    const val DAYS_LIMIT = 400
    private const val VERSION = "v3"
    private const val V2 = "v2"
    private const val V1 = "v1"
    private val DAY_RE = Regex("""\d{4}-\d{2}-\d{2}""")

    /** A real calendar day (2026-13-45 has the right shape but is not one, and would crash date maths). */
    fun isDay(text: String): Boolean = DAY_RE.matches(text) && runCatching { LocalDate.parse(text) }.isSuccess

    val ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement("first_game") { p, _, _, _ -> p.totalGames >= 1 },
        Achievement("first_line") { p, _, _, _ -> p.totalLines >= 1 },
        Achievement("long_line_7") { p, _, _, _ -> p.bestLine >= 7 },
        Achievement("long_line_9") { p, _, _, _ -> p.bestLine >= 9 },
        Achievement("score_100") { p, _, _, _ -> p.bestScore >= 100 },
        Achievement("score_250") { p, _, _, _ -> p.bestScore >= 250 },
        Achievement("score_500") { p, _, _, _ -> p.bestScore >= 500 },
        Achievement("score_1000") { p, _, _, _ -> p.bestScore >= 1000 },
        Achievement("games_10") { p, _, _, _ -> p.totalGames >= 10 },
        Achievement("games_50") { p, _, _, _ -> p.totalGames >= 50 },
        Achievement("games_100") { p, _, _, _ -> p.totalGames >= 100 },
        Achievement("lines_50") { p, _, _, _ -> p.totalLines >= 50 },
        Achievement("lines_250") { p, _, _, _ -> p.totalLines >= 250 },
        Achievement("streak_3") { _, _, _, streak -> streak >= 3 },
        Achievement("streak_7") { _, _, _, streak -> streak >= 7 },
        Achievement("marathon") { _, r, _, _ -> r.moves >= 150 },
        Achievement("level_5") { _, _, level, _ -> level >= 5 },
        Achievement("level_10") { _, _, level, _ -> level >= 10 },
        Achievement("daily_first") { _, r, _, _ -> r.mode == ModeId.DAILY },
        Achievement("all_modes") { p, _, _, _ -> ModeId.entries.all { (p.gamesByMode[it] ?: 0) > 0 } },
        Achievement("goal_first") { p, _, _, _ -> p.bonusXp > 0 },
        Achievement("goal_day") { p, _, _, _ -> p.goalDays.isNotEmpty() },
        Achievement("goals_7") { p, _, _, _ -> bestStreak(p.goalDays) >= 7 }
    )

    private val ID_RE = Regex("[a-z0-9_]{1,40}")

    fun empty() = Progress()

    fun xpOf(p: Progress): Int = p.totalScore + p.totalLines * 5 + p.totalGames * 10 + p.bonusXp

    fun dayKey(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toString()

    private fun dayNumber(key: String): Long = LocalDate.parse(key).toEpochDay()

    /** Consecutive days ending today (or yesterday, so the streak survives until tonight). */
    fun currentStreak(days: List<String>, today: String): Int {
        if (days.isEmpty()) return 0
        val set = days.map(::dayNumber).toSet()
        val todayN = dayNumber(today)
        var cursor = when {
            todayN in set -> todayN
            todayN - 1 in set -> todayN - 1
            else -> return 0
        }
        var streak = 0
        while (cursor in set) {
            streak++
            cursor--
        }
        return streak
    }

    fun bestStreak(days: List<String>): Int {
        val nums = days.map(::dayNumber).toSortedSet().toList()
        var best = 0
        var run = 0
        nums.forEachIndexed { i, n ->
            run = if (i > 0 && n == nums[i - 1] + 1) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    /** Folds one finished game into the profile. Pure: the input is not modified. */
    fun applyGame(before: Progress, record: GameRecord): AppliedGame {
        val key = dayKey(record.endedAt)
        val days = if (key in before.days) before.days else (before.days + key).sorted().takeLast(DAYS_LIMIT)
        var progress = Progress(
            totalGames = before.totalGames + 1,
            completedGames = before.completedGames + if (record.completed) 1 else 0,
            totalScore = before.totalScore + record.score,
            totalLines = before.totalLines + record.lines,
            totalBalls = before.totalBalls + record.balls,
            totalMoves = before.totalMoves + record.moves,
            totalPlayMs = before.totalPlayMs + record.durationMs,
            bestScore = maxOf(before.bestScore, record.score),
            bestLine = maxOf(before.bestLine, record.maxLine),
            mostLinesInGame = maxOf(before.mostLinesInGame, record.lines),
            longestGameMoves = maxOf(before.longestGameMoves, record.moves),
            days = days,
            achievements = before.achievements,
            bonusXp = before.bonusXp,
            goalDays = before.goalDays
        )
        return awardAchievements(progress.copy(
            gamesByMode = before.gamesByMode + (record.mode to (before.gamesByMode[record.mode] ?: 0) + 1),
            bestByMode = before.bestByMode + (record.mode to maxOf(before.bestByMode[record.mode] ?: 0, record.score))
        ), record)
    }

    /** Unlocks every achievement whose condition now holds; the input is not modified. */
    fun awardAchievements(before: Progress, record: GameRecord): AppliedGame {
        val level = Levels.info(xpOf(before)).level
        val streak = currentStreak(before.days, dayKey(record.endedAt))
        val unlocked = mutableListOf<String>()
        val achievements = before.achievements.toMutableMap()
        for (a in ACHIEVEMENTS) {
            if (a.id !in achievements && a.test(before, record, level, streak)) {
                achievements[a.id] = record.endedAt
                unlocked += a.id
            }
        }
        return AppliedGame(before.copy(achievements = achievements), unlocked)
    }

    /** Adds the experience of newly completed goals; a day with every goal done is remembered. */
    fun applyGoalBonus(before: Progress, day: String, bonus: GoalBonus): Progress = before.copy(
        bonusXp = before.bonusXp + bonus.xp,
        goalDays = if (bonus.allDone && day !in before.goalDays) (before.goalDays + day).sorted().takeLast(DAYS_LIMIT) else before.goalDays
    )

    /** Rebuilds the profile from a newest-first history (saves made before profiles existed). */
    fun rebuild(newestFirst: List<GameRecord>): Progress =
        newestFirst.asReversed().fold(empty()) { p, r -> applyGame(p, r).progress }

    /** The latest scores, oldest first, for a small trend chart. */
    fun scoreTrend(newestFirst: List<GameRecord>, count: Int): List<Int> =
        newestFirst.take(count).map { it.score }.reversed()

    fun encode(p: Progress): String = listOf(
        VERSION,
        listOf(
            p.totalGames, p.completedGames, p.totalScore, p.totalLines, p.totalBalls, p.totalMoves,
            p.totalPlayMs, p.bestScore, p.bestLine, p.mostLinesInGame, p.longestGameMoves
        ).joinToString(","),
        p.days.joinToString(","),
        p.achievements.entries.joinToString(",") { "${it.key}:${it.value}" },
        p.bonusXp.toString(),
        p.goalDays.joinToString(","),
        ModeId.entries.joinToString(",") { (p.gamesByMode[it] ?: 0).toString() },
        ModeId.entries.joinToString(",") { (p.bestByMode[it] ?: 0).toString() }
    ).joinToString("|")

    private fun perMode(text: String): Map<ModeId, Int> {
        val n = text.split(",").map { it.toIntOrNull()?.takeIf { v -> v >= 0 } ?: 0 }
        return ModeId.entries.mapIndexed { i, m -> m to n.getOrElse(i) { 0 } }.toMap()
    }

    /** Validates untrusted stored data; anything wrong falls back to an empty profile. */
    fun decode(text: String?): Progress {
        if (text.isNullOrEmpty()) return empty()
        val parts = text.split("|")
        val okShape = (parts[0] == VERSION && parts.size == 8) || (parts[0] == V2 && parts.size == 6) || (parts[0] == V1 && parts.size == 4)
        if (!okShape) return empty()
        val n = parts[1].split(",").map { it.toLongOrNull()?.takeIf { v -> v >= 0 } ?: return empty() }
        if (n.size != 11) return empty()
        val days = if (parts[2].isEmpty()) emptyList() else parts[2].split(",").filter { isDay(it) }
        val achievements = mutableMapOf<String, Long>()
        if (parts[3].isNotEmpty()) {
            for (entry in parts[3].split(",")) {
                val kv = entry.split(":")
                // Ids this version does not know (earned on another platform) are kept, so a backup round trip loses nothing.
                if (kv.size != 2 || !ID_RE.matches(kv[0])) continue
                val whenMs = kv[1].toLongOrNull()?.takeIf { it >= 0 } ?: continue
                achievements[kv[0]] = whenMs
            }
        }
        return Progress(
            totalGames = n[0].toInt(), completedGames = n[1].toInt(), totalScore = n[2].toInt(),
            totalLines = n[3].toInt(), totalBalls = n[4].toInt(), totalMoves = n[5].toInt(),
            totalPlayMs = n[6], bestScore = n[7].toInt(), bestLine = n[8].toInt(),
            mostLinesInGame = n[9].toInt(), longestGameMoves = n[10].toInt(),
            days = days.takeLast(DAYS_LIMIT), achievements = achievements,
            bonusXp = if (parts.size >= 6) parts[4].toIntOrNull()?.takeIf { it >= 0 } ?: 0 else 0,
            goalDays = if (parts.size >= 6 && parts[5].isNotEmpty()) parts[5].split(",").filter { isDay(it) }.takeLast(DAYS_LIMIT) else emptyList(),
            gamesByMode = if (parts.size == 8) perMode(parts[6]) else ModeId.entries.associateWith { 0 },
            bestByMode = if (parts.size == 8) perMode(parts[7]) else ModeId.entries.associateWith { 0 }
        )
    }
}
