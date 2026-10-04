package io.github.basil_as.basillines.engine

data class GameRecord(
    val score: Int,
    /** Epoch milliseconds when the game ended. */
    val endedAt: Long,
    val moves: Int,
    val lines: Int,
    val balls: Int,
    /** false when the player abandoned the game by starting a new one. */
    val completed: Boolean,
    /** Length of the longest line cleared in the game. */
    val maxLine: Int = 0,
    /** Active play time in milliseconds. */
    val durationMs: Long = 0,
    val mode: ModeId = ModeId.CLASSIC,
    /** How it was played (decision times, hesitation, danger); null for games recorded by older versions. */
    val cog: Cognition? = null
)

data class StatsSummary(
    val gamesPlayed: Int,
    val completedGames: Int,
    val bestScore: Int,
    val averageScore: Int,
    val totalScore: Int,
    val totalMoves: Int,
    val totalLines: Int,
    val lastScore: Int?,
    val bestRecord: GameRecord?
)

object GameStats {
    fun recordFrom(
        engine: GameEngine,
        completed: Boolean,
        nowMillis: Long,
        offsetMinutes: Int = java.util.TimeZone.getDefault().getOffset(nowMillis) / 60_000
    ) = GameRecord(
        score = engine.score,
        endedAt = nowMillis,
        moves = engine.moves,
        lines = engine.linesCleared,
        balls = engine.ballsCleared,
        completed = completed,
        maxLine = engine.maxLine,
        durationMs = engine.playMs,
        mode = engine.mode,
        cog = engine.tel.snapshot(Telemetry.zoneCode(offsetMinutes))
    )

    /** Newest first. Nothing is ever dropped: every game stays for the analysis of the years. */
    fun addRecord(history: List<GameRecord>, record: GameRecord): List<GameRecord> = listOf(record) + history

    fun summarize(history: List<GameRecord>): StatsSummary {
        val best = history.maxByOrNull { it.score }
        val total = history.sumOf { it.score }
        return StatsSummary(
            gamesPlayed = history.size,
            completedGames = history.count { it.completed },
            bestScore = best?.score ?: 0,
            averageScore = if (history.isEmpty()) 0 else Math.round(total.toDouble() / history.size).toInt(),
            totalScore = total,
            totalMoves = history.sumOf { it.moves },
            totalLines = history.sumOf { it.lines },
            lastScore = history.firstOrNull()?.score,
            bestRecord = best
        )
    }

    fun isNewRecord(score: Int, previousBest: Int): Boolean = score > 0 && score > previousBest

    fun encodeHistory(history: List<GameRecord>): String = history.joinToString(";") {
        "${it.score},${it.endedAt},${it.moves},${it.lines},${it.balls},${if (it.completed) 1 else 0},${it.maxLine},${it.durationMs},${it.mode.id}" +
            (it.cog?.let { c -> "," + c.values.joinToString(",") } ?: "")
    }

    /** Malformed entries are dropped; never throws. */
    fun decodeHistory(text: String?): List<GameRecord> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(";").mapNotNull(::decodeRecord)
    }

    private fun decodeRecord(entry: String): GameRecord? {
        val f = entry.split(",")
        if (f.size != 6 && f.size != 8 && f.size != 9 && f.size != 9 + CogKey.entries.size) return null
        val score = f[0].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val endedAt = f[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val moves = f[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val lines = f[3].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val balls = f[4].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val completed = when (f[5]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        var maxLine = 0
        var durationMs = 0L
        if (f.size >= 8) {
            maxLine = f[6].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            durationMs = f[7].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        }
        val mode = if (f.size >= 9) ModeId.fromId(f[8]) ?: return null else ModeId.CLASSIC
        val cog = if (f.size > 9) Cognition.of(f.drop(9).map { it.toLongOrNull() ?: return null }) ?: return null else null
        return GameRecord(score, endedAt, moves, lines, balls, completed, maxLine, durationMs, mode, cog)
    }
}
