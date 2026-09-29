package io.github.basil_as.basillines

import android.content.Context
import io.github.basil_as.basillines.engine.GameEngine
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.GameStateCodec
import io.github.basil_as.basillines.engine.GameStats
import io.github.basil_as.basillines.engine.Progress
import io.github.basil_as.basillines.engine.ProgressTracker

enum class AppTheme { MODERN, LIGHT, LINES_98, COLORLINES_92 }

/** Everything the app remembers between launches. Invalid stored data falls back to defaults. */
class GameStorage(context: Context) {
    private val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() = parseTheme(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST, 0).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    var history: List<GameRecord>
        get() = GameStats.decodeHistory(prefs.getString(KEY_HISTORY, null))
        set(value) = prefs.edit().putString(KEY_HISTORY, GameStats.encodeHistory(value)).apply()

    /** The profile; rebuilt from the history for installs that predate profiles. */
    var progress: Progress
        get() {
            val saved = ProgressTracker.decode(prefs.getString(KEY_PROGRESS, null))
            val past = history
            return if (saved.totalGames == 0 && past.isNotEmpty()) ProgressTracker.rebuild(past) else saved
        }
        set(value) = prefs.edit().putString(KEY_PROGRESS, ProgressTracker.encode(value)).apply()

    /** Show small balls on the board where the next balls will appear. On by default. */
    var spawnPreview: Boolean
        get() = prefs.getBoolean(KEY_PREVIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_PREVIEW, value).apply()

    fun loadGame(): GameEngine? = GameStateCodec.decode(prefs.getString(KEY_GAME, null))

    fun saveGame(engine: GameEngine) {
        prefs.edit().putString(KEY_GAME, GameStateCodec.encode(engine)).apply()
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_BEST = "best_score"
        private const val KEY_HISTORY = "history"
        private const val KEY_GAME = "game"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_PREVIEW = "spawn_preview"

        /** Names used before the four current themes existed. */
        private val LEGACY = mapOf("CLASSIC_98" to AppTheme.LINES_98)

        fun parseTheme(raw: String?): AppTheme =
            AppTheme.entries.firstOrNull { it.name == raw } ?: LEGACY[raw] ?: AppTheme.MODERN
    }
}
