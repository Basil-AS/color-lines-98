package com.colorlines.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
    var isEnabled: Boolean = prefs.getBoolean("sound_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val selectSoundId = soundPool.load(context, R.raw.select_ball, 1)
    private val jumpSoundId = soundPool.load(context, R.raw.jump, 1)
    private val eat1SoundId = soundPool.load(context, R.raw.eat_score_1, 1)
    private val eat2SoundId = soundPool.load(context, R.raw.eat_score_2, 1)
    private val eat3SoundId = soundPool.load(context, R.raw.eat_score_3, 1)
    private val eat4SoundId = soundPool.load(context, R.raw.eat_score_4, 1)
    private val eat5SoundId = soundPool.load(context, R.raw.eat_score_5, 1)
    private val loseSoundId = soundPool.load(context, R.raw.lose, 1)
    private val winSoundId = soundPool.load(context, R.raw.win, 1)
    private val clickSoundId = soundPool.load(context, R.raw.button_click, 1)

    private fun play(soundId: Int, volume: Float = 0.8f) {
        if (!isEnabled) return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    fun playSelect() = play(selectSoundId, 0.7f)
    fun playJump() = play(jumpSoundId, 0.7f)

    fun playEat(points: Int) {
        val soundId = when {
            points >= 30 -> eat5SoundId
            points >= 20 -> eat4SoundId
            points >= 15 -> eat3SoundId
            points >= 12 -> eat2SoundId
            else -> eat1SoundId
        }
        play(soundId, 0.9f)
    }

    fun playLose() = play(loseSoundId, 0.9f)
    fun playWin() = play(winSoundId, 0.9f)
    fun playClick() = play(clickSoundId, 0.5f)

    fun release() {
        soundPool.release()
    }
}
