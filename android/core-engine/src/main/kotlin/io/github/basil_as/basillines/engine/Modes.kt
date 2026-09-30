package io.github.basil_as.basillines.engine

/** The ways to play; mirrors src/engine/modes.ts and src/modes.ts. */
enum class ModeId(val id: String, val colors: Int, val timeLimitMs: Long?, val seeded: Boolean) {
    CLASSIC("classic", 7, null, false),
    EASY("easy", 5, null, false),
    BLITZ("blitz", 7, 180_000L, false),
    DAILY("daily", 7, null, true);

    companion object {
        fun fromId(id: String?): ModeId? = entries.firstOrNull { it.id == id }
    }
}

object Modes {
    /** Easy mode drops two colours: red, green, blue, yellow, magenta. */
    val EASY_COLORS = listOf(BallColor.RED, BallColor.GREEN, BallColor.BLUE, BallColor.YELLOW, BallColor.MAGENTA)

    fun dailySeed(dayKey: String): Long = hashString("color-lines-daily:$dayKey")

    fun colorsFor(mode: ModeId): List<BallColor> = if (mode == ModeId.EASY) EASY_COLORS else BallColor.entries.toList()

    fun createEngine(mode: ModeId, dayKey: String, random: kotlin.random.Random = kotlin.random.Random.Default): GameEngine =
        GameEngine(
            random = random,
            colors = colorsFor(mode),
            mode = mode,
            seed = if (mode.seeded) dailySeed(dayKey) else null
        )

    /** Time left in a timed mode, or null when the mode has no clock. */
    fun remainingMs(engine: GameEngine): Long? = engine.mode.timeLimitMs?.let { (it - engine.playMs).coerceAtLeast(0) }
}
