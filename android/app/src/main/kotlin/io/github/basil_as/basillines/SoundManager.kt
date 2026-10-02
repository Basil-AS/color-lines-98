package io.github.basil_as.basillines

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import io.github.basil_as.basillines.engine.ModernSounds
import io.github.basil_as.basillines.engine.PcSpeaker
import io.github.basil_as.basillines.engine.SoundKind

class SoundManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE)
    var isEnabled: Boolean = prefs.getBoolean("sound_enabled", true)
        set(value) {
            field = value
            prefs.edit().putBoolean("sound_enabled", value).apply()
        }

    /** Recorded samples (Lines 98), PC-speaker beeps (DOS look) or soft synthesis (modern looks). */
    enum class Profile { SAMPLED, PC_SPEAKER, MODERN }

    var profile: Profile = Profile.MODERN
    var voice: io.github.basil_as.basillines.engine.VoiceId = io.github.basil_as.basillines.engine.VoiceId.SOFT

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
    private val startSoundId = soundPool.load(context, R.raw.start_game, 1)
    private val levelUpSoundId = soundPool.load(context, R.raw.level_up, 1)
    private val fireworksSoundId = soundPool.load(context, R.raw.fireworks, 1)
    private val bonusSoundId = soundPool.load(context, R.raw.jump_bonus, 1)
    private val clickSoundId = soundPool.load(context, R.raw.button_click, 1)

    private fun play(soundId: Int, volume: Float = 0.8f) {
        if (!isEnabled) return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    private fun beep(kind: SoundKind, points: Int = 0) {
        // Some devices refuse to build an AudioTrack; the game must go on silently rather than crash.
        runCatching { beepUnsafe(kind, points) }
    }

    private fun beepUnsafe(kind: SoundKind, points: Int) {
        if (!isEnabled) return
        val pcm = if (profile == Profile.PC_SPEAKER) {
            PcSpeaker.render(PcSpeaker.notes(kind, points))
        } else {
            ModernSounds.render(ModernSounds.voiced(kind, points, voice))
        }
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(22_050)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            // No usable audio output right now: stay silent instead of failing the game.
            track.release()
            return
        }
        track.write(pcm, 0, pcm.size)
        track.setNotificationMarkerPosition(pcm.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) = t.release()
            override fun onPeriodicNotification(t: AudioTrack) = Unit
        })
        track.play()
    }

    /** Plays a game event in the voice of the current look: samples, PC-speaker beeps or soft tones. */
    fun play(kind: SoundKind, points: Int = 0) {
        if (profile != Profile.SAMPLED) {
            beep(kind, points)
            return
        }
        when (kind) {
            SoundKind.SELECT -> play(selectSoundId, 0.7f)
            SoundKind.JUMP -> play(jumpSoundId, 0.7f)
            SoundKind.EAT -> play(
                when {
                    points >= 30 -> eat5SoundId
                    points >= 20 -> eat4SoundId
                    points >= 15 -> eat3SoundId
                    points >= 12 -> eat2SoundId
                    else -> eat1SoundId
                },
                0.9f
            )
            SoundKind.LOSE -> play(loseSoundId, 0.9f)
            SoundKind.WIN, SoundKind.CROWN -> play(winSoundId, 0.9f)
            SoundKind.CLICK -> play(clickSoundId, 0.5f)
            SoundKind.BLOCKED -> play(clickSoundId, 0.3f)
            SoundKind.START -> play(startSoundId, 0.7f)
            SoundKind.LEVEL_UP -> play(levelUpSoundId, 0.8f)
            SoundKind.RECORD -> play(fireworksSoundId, 0.8f)
            SoundKind.ACHIEVEMENT -> play(bonusSoundId, 0.8f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
