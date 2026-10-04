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
    private val keyOfId = java.util.concurrent.ConcurrentHashMap<Int, String>()
    private val cacheDir = java.io.File(context.cacheDir, "sounds").apply { mkdirs() }

    /** A sound asked for before it finished loading plays as soon as it is ready (if that is within a moment). */
    private val waiting = java.util.concurrent.ConcurrentHashMap<Int, Pair<Long, Float>>()
    private var failures = 0
    private var lastRebuild = 0L

    private var soundPool: SoundPool = buildPool()

    private fun buildPool(): SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .also { pool ->
            pool.setOnLoadCompleteListener { p, id, status ->
                if (status == 0) {
                    ready.add(id)
                    // Something asked for it while it loaded: play it now unless that was long ago.
                    waiting.remove(id)?.let { (at, vol) -> if (System.currentTimeMillis() - at < 600) p.play(id, vol, vol, 1, 0, 1f) }
                } else {
                    // A failed load must not stay a silent hole: forget it so the next request loads it again.
                    keyOfId.remove(id)?.let { synthIds.remove(it) }
                }
            }
        }

    private var raws = loadRaws()

    private class Raws(val select: Int, val jump: Int, val eat: List<Int>, val lose: Int, val win: Int, val start: Int, val levelUp: Int, val fireworks: Int, val bonus: Int, val click: Int)

    private fun loadRaws() = Raws(
        select = soundPool.load(context, R.raw.select_ball, 1),
        jump = soundPool.load(context, R.raw.jump, 1),
        eat = listOf(R.raw.eat_score_1, R.raw.eat_score_2, R.raw.eat_score_3, R.raw.eat_score_4, R.raw.eat_score_5).map { soundPool.load(context, it, 1) },
        lose = soundPool.load(context, R.raw.lose, 1),
        win = soundPool.load(context, R.raw.win, 1),
        start = soundPool.load(context, R.raw.start_game, 1),
        levelUp = soundPool.load(context, R.raw.level_up, 1),
        fireworks = soundPool.load(context, R.raw.fireworks, 1),
        bonus = soundPool.load(context, R.raw.jump_bonus, 1),
        click = soundPool.load(context, R.raw.button_click, 1)
    )

    /**
     * Plays a sound; a stream id of 0 means the pool could not play it (the audio service restarted, or the pool went
     * deaf after the app was in the background). After two such failures in a row the pool is built again.
     */
    private fun play(soundId: Int, volume: Float = 0.8f) {
        if (!isEnabled || soundId <= 0) return
        if (soundId !in ready) {
            waiting[soundId] = System.currentTimeMillis() to volume
            return
        }
        val stream = soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        if (stream == 0) {
            if (++failures >= 2) recover()
        } else failures = 0
    }

    /** Builds the pool and everything in it again; safe to call at any time (it is rate-limited). */
    @Synchronized
    fun recover() {
        val now = System.currentTimeMillis()
        if (now - lastRebuild < 3000) return
        lastRebuild = now
        failures = 0
        runCatching { soundPool.release() }
        ready.clear(); synthIds.clear(); keyOfId.clear(); waiting.clear()
        soundPool = buildPool()
        raws = loadRaws()
        prepare()
    }

    /** The app came back to the front: after a long time in the background the pool is rebuilt, which is cheap. */
    fun onForeground(awayMs: Long) {
        if (awayMs > 60_000) recover()
    }

    /** How many sizes of "eat" there are (more balls cleared, longer rising run of notes); a chain has one per link. */
    private fun bucketOf(kind: SoundKind, points: Int) = when (kind) {
        SoundKind.EAT -> (points / 12).coerceIn(0, 5)
        SoundKind.COMBO -> points.coerceIn(1, 4)
        else -> 0
    }

    private fun pointsOf(kind: SoundKind, bucket: Int) = if (kind == SoundKind.COMBO) bucket else bucket * 12

    /**
     * The SoundPool id of a synthesised sound, created once: the notes are rendered into a small WAV file in the cache
     * and loaded into the pool. The file is written again when the system cleared the cache meanwhile.
     */
    private fun synthId(kind: SoundKind, bucket: Int): Int {
        val key = "${profile.name}-${voice.name}-${kind.name}-$bucket"
        synthIds[key]?.let { return it }
        val points = pointsOf(kind, bucket)
        // Normalised: the synthesised notes are quiet (a click peaks at 5% of full scale), the beeps flat and harsh.
        val pcm = if (profile == Profile.PC_SPEAKER) io.github.basil_as.basillines.engine.Wav.normalize(PcSpeaker.render(PcSpeaker.notes(kind, points)), 0.5)
        else io.github.basil_as.basillines.engine.Wav.normalize(ModernSounds.render(ModernSounds.voiced(kind, points, voice)), 0.8)
        if (pcm.isEmpty()) return -1
        cacheDir.mkdirs()
        val file = java.io.File(cacheDir, "$key.wav")
        file.writeBytes(io.github.basil_as.basillines.engine.Wav.encode(pcm))
        val id = soundPool.load(file.absolutePath, 1)
        val existing = synthIds.putIfAbsent(key, id)
        if (existing == null) keyOfId[id] = key
        return existing ?: id
    }

    /** Renders and loads every sound of the current look in the background, so the first play is instant. */
    private fun prepare() {
        if (profile == Profile.SAMPLED) return
        Thread {
            runCatching {
                for (kind in SoundKind.entries) {
                    val buckets = when (kind) { SoundKind.EAT -> 0..5; SoundKind.COMBO -> 1..4; else -> 0..0 }
                    for (b in buckets) synthId(kind, b)
                }
            }
        }.start()
    }

    private fun beep(kind: SoundKind, points: Int = 0) {
        if (!isEnabled) return
        // Some devices refuse to load a file; the game must go on silently rather than crash.
        runCatching { play(synthId(kind, bucketOf(kind, points)), 1f) }
    }

    /** Plays a game event in the voice of the current look: samples, PC-speaker beeps or soft tones. */
    fun play(kind: SoundKind, points: Int = 0) {
        if (profile != Profile.SAMPLED) {
            beep(kind, points)
            return
        }
        val r = raws
        when (kind) {
            SoundKind.SELECT -> play(r.select, 0.7f)
            SoundKind.JUMP -> play(r.jump, 0.7f)
            SoundKind.EAT -> play(
                r.eat[when {
                    points >= 30 -> 4
                    points >= 20 -> 3
                    points >= 15 -> 2
                    points >= 12 -> 1
                    else -> 0
                }],
                0.9f
            )
            SoundKind.LOSE -> play(r.lose, 0.9f)
            SoundKind.WIN, SoundKind.CROWN -> play(r.win, 0.9f)
            SoundKind.CLICK -> play(r.click, 0.5f)
            SoundKind.BLOCKED -> play(r.click, 0.3f)
            SoundKind.START -> play(r.start, 0.7f)
            SoundKind.LEVEL_UP -> play(r.levelUp, 0.8f)
            SoundKind.RECORD -> play(r.fireworks, 0.8f)
            SoundKind.ACHIEVEMENT -> play(r.bonus, 0.8f)
            SoundKind.COMBO -> play(r.eat[(1 + points).coerceIn(1, 4)], 0.8f)
            SoundKind.DANGER -> play(r.click, 0.3f)
            SoundKind.TICK -> play(r.click, 0.2f)
            SoundKind.HINT -> play(r.select, 0.5f)
            SoundKind.POP -> play(r.select, 0.25f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
