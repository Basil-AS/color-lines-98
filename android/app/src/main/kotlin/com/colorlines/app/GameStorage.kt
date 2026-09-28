package com.colorlines.app

import android.content.Context
import com.colorlines.engine.GameEngine
import com.colorlines.engine.GameRecord
import com.colorlines.engine.GameStateCodec
import com.colorlines.engine.GameStats

enum class AppTheme { MODERN, CLASSIC_98 }

/** Everything the app remembers between launches. Invalid stored data falls back to defaults. */
class GameStorage(context: Context) {
    private val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)

    var theme: AppTheme
        get() = AppTheme.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: AppTheme.MODERN
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST, 0).coerceAtLeast(0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    var history: List<GameRecord>
        get() = GameStats.decodeHistory(prefs.getString(KEY_HISTORY, null))
        set(value) = prefs.edit().putString(KEY_HISTORY, GameStats.encodeHistory(value)).apply()

    fun loadGame(): GameEngine? = GameStateCodec.decode(prefs.getString(KEY_GAME, null))

    fun saveGame(engine: GameEngine) {
        prefs.edit().putString(KEY_GAME, GameStateCodec.encode(engine)).apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_BEST = "best_score"
        const val KEY_HISTORY = "history"
        const val KEY_GAME = "game"
    }
}
