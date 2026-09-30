package io.github.basil_as.basillines

import android.content.Context
import io.github.basil_as.basillines.engine.GameEngine
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.GameStateCodec
import io.github.basil_as.basillines.engine.GameStats
import io.github.basil_as.basillines.engine.Hall
import io.github.basil_as.basillines.engine.ModeId
import io.github.basil_as.basillines.engine.HallEntry
import io.github.basil_as.basillines.engine.Progress
import io.github.basil_as.basillines.engine.ProgressTracker

/** The language the player picked inside the game; AUTO follows the system. */
enum class AppLanguage(val tag: String?) { AUTO(null), EN("en"), RU("ru") }

enum class AppTheme { MODERN, LIGHT, MATERIAL, NEON, CONTRAST, LINES_98, COLORLINES_92 }

/** Everything the app remembers between launches. Invalid stored data falls back to defaults. */
class GameStorage(context: Context) {
    private val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() = parseTheme(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var language: AppLanguage
        get() = AppLanguage.entries.firstOrNull { it.name == prefs.getString(KEY_LANG, null) } ?: AppLanguage.AUTO
        set(value) = prefs.edit().putString(KEY_LANG, value.name).apply()

    /** The mode chosen last in the new-game dialog. */
    var mode: ModeId
        get() = ModeId.fromId(prefs.getString(KEY_MODE, null)) ?: ModeId.CLASSIC
        set(value) = prefs.edit().putString(KEY_MODE, value.id).apply()

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

    /** The player's explicit choice for marking the spawn cells, or null to use the theme default. */
    var spawnPreview: Boolean?
        get() = if (prefs.contains(KEY_PREVIEW)) prefs.getBoolean(KEY_PREVIEW, true) else null
        set(value) {
            if (value == null) prefs.edit().remove(KEY_PREVIEW).apply() else prefs.edit().putBoolean(KEY_PREVIEW, value).apply()
        }

    /** The Top Ten of the 1992 screen. */
    var hall: List<HallEntry>
        get() = Hall.decode(prefs.getString(KEY_HALL, null))
        set(value) = prefs.edit().putString(KEY_HALL, Hall.encode(value)).apply()

    var playerName: String
        get() = (prefs.getString(KEY_NAME, "") ?: "").take(Hall.NAME_LIMIT)
        set(value) = prefs.edit().putString(KEY_NAME, value.take(Hall.NAME_LIMIT)).apply()

    /** F3 "NEXT" of the 1992 screen: show the upcoming colours. On by default. */
    var showNext: Boolean
        get() = prefs.getBoolean(KEY_NEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_NEXT, value).apply()

    fun loadGame(): GameEngine? = GameStateCodec.decode(prefs.getString(KEY_GAME, null))

    fun saveGame(engine: GameEngine) {
        prefs.edit().putString(KEY_GAME, GameStateCodec.encode(engine)).apply()
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_LANG = "language"
        private const val KEY_MODE = "mode"
        private const val KEY_BEST = "best_score"
        private const val KEY_HISTORY = "history"
        private const val KEY_GAME = "game"
        private const val KEY_PROGRESS = "progress"
        private const val KEY_PREVIEW = "spawn_preview"
        private const val KEY_HALL = "hall"
        private const val KEY_NAME = "player_name"
        private const val KEY_NEXT = "show_next"

        /** Names used before the four current themes existed. */
        private val LEGACY = mapOf("CLASSIC_98" to AppTheme.LINES_98)

        /** A context whose resources use the language chosen in the game (the system one for AUTO). */
        fun localized(base: Context): Context {
            val prefs = base.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
            val tag = AppLanguage.entries.firstOrNull { it.name == prefs.getString(KEY_LANG, null) }?.tag ?: return base
            val locale = java.util.Locale.forLanguageTag(tag)
            java.util.Locale.setDefault(locale)
            val config = android.content.res.Configuration(base.resources.configuration)
            config.setLocale(locale)
            return base.createConfigurationContext(config)
        }

        fun parseTheme(raw: String?): AppTheme =
            AppTheme.entries.firstOrNull { it.name == raw } ?: LEGACY[raw] ?: AppTheme.MODERN
    }
}
