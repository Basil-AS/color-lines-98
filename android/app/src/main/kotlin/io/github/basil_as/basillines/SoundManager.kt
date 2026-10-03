package io.github.basil_as.basillines

import android.content.Context
import android.media.AudioAttributes
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
        set(value) {
            if (field != value) { field = value; prepare() }
        }
    var voice: io.github.basil_as.basillines.engine.VoiceId = io.github.basil_as.basillines.engine.VoiceId.SOFT
        set(value) {
            if (field != value) { field = value; prepare() }
        }

    private val ready: MutableSet<Int> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val synthIds = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val cacheDir = java.io.File(context.cacheDir, "sounds").apply { mkdirs() }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .also { pool -> pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) } }

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
        // A sound still decoding is skipped (it is ready within a moment of start-up); never block or fail the game.
        if (soundId in ready) soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    /** How many sizes of "eat" there are (more balls cleared, longer rising run of notes). */
    private fun bucketOf(kind: SoundKind, points: Int) = if (kind == SoundKind.EAT) (points / 12).coerceIn(0, 5) else 0

    /**
     * The SoundPool id of a synthesised sound, created once: the notes are rendered into a small WAV file in the cache
     * and loaded into the pool. (An AudioTrack per play leaked when its end marker never fired, and eventually nothing
     * was audible.)
     */
    private fun synthId(kind: SoundKind, bucket: Int): Int {
        val key = "${profile.name}-${voice.name}-${kind.name}-$bucket"
        synthIds[key]?.let { return it }
        val points = bucket * 12
        // Normalised: the synthesised notes are quiet (a click peaks at 5% of full scale), the beeps flat and harsh.
        val pcm = if (profile == Profile.PC_SPEAKER) io.github.basil_as.basillines.engine.Wav.normalize(PcSpeaker.render(PcSpeaker.notes(kind, points)), 0.5)
        else io.github.basil_as.basillines.engine.Wav.normalize(ModernSounds.render(ModernSounds.voiced(kind, points, voice)), 0.8)
        if (pcm.isEmpty()) return -1
        val file = java.io.File(cacheDir, "$key.wav")
        file.writeBytes(io.github.basil_as.basillines.engine.Wav.encode(pcm))
        val id = soundPool.load(file.absolutePath, 1)
        return synthIds.putIfAbsent(key, id) ?: id
    }

    /** Renders and loads every sound of the current look in the background, so the first play is instant. */
    private fun prepare() {
        if (profile == Profile.SAMPLED) return
        Thread {
            runCatching {
                for (kind in SoundKind.entries) {
                    val buckets = if (kind == SoundKind.EAT) 0..5 else 0..0
                    for (b in buckets) synthId(kind, b)
                }
            }
        }.start()
    }

    private fun beep(kind: SoundKind, points: Int = 0) {
        if (!isEnabled) return
        // Some devices refuse to load a file; the game must go on silently rather than crash.
        runCatching {
            val id = synthId(kind, bucketOf(kind, points))
            if (id > 0 && id in ready) soundPool.play(id, 1f, 1f, 1, 0, 1.0f)
        }
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
