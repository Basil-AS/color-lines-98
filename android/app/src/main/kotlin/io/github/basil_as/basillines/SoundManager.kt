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
    /** One folder per app version: a new version may change the synthesis, so old files are never reused. */
    private val cacheDir = java.io.File(context.cacheDir, "sounds-" + UpdateClient.versionName(context)).apply { mkdirs() }

    /** A sound asked for before it finished loading plays as soon as it is ready (if that is within a moment). */
    private val waiting = java.util.concurrent.ConcurrentHashMap<Int, Pair<Long, Float>>()

    /** Synthesised sounds asked for before they even have a pool id: key -> (when, volume). */
    private val wanted = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Float>>()
    private val inflight: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** Rendering and writing sounds never happens on the thread that plays them (that froze the game on the first move). */
    private val worker = java.util.concurrent.Executors.newFixedThreadPool(2) { r -> Thread(r, "sound-prep").apply { isDaemon = true } }

    /** Bumped whenever the pool is rebuilt, so work started for the old pool is dropped. */
    @Volatile private var generation = 0
    @Volatile private var warmed = false
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
                // Ids start at 1 in every pool: a late answer from a pool that was rebuilt must not mark a sound ready here.
                if (p !== soundPool) return@setOnLoadCompleteListener
                if (status == 0) {
                    ready.add(id)
                    // Something asked for it while it loaded: play it now unless that was long ago.
                    waiting.remove(id)?.let { (at, vol) -> if (System.currentTimeMillis() - at < WAIT_MS) p.play(id, vol, vol, 1, 0, 1f) }
                    warmUp(p, id)
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
        generation++
        warmed = false
        runCatching { soundPool.release() }
        ready.clear(); synthIds.clear(); keyOfId.clear(); waiting.clear(); wanted.clear(); inflight.clear()
        soundPool = buildPool()
        raws = loadRaws()
        prepare()
    }

    /** The app came back to the front: after a long time in the background the pool is rebuilt, which is cheap. */
    fun onForeground(awayMs: Long) {
        if (awayMs > 60_000) recover() else if (awayMs > 5_000) warmed = false
    }

    /** The first sound after a pause often starts late while the audio path wakes up; a silent one wakes it in advance. */
    private fun warmUp(pool: SoundPool, id: Int) {
        if (warmed) return
        warmed = true
        runCatching { pool.play(id, 0f, 0f, 0, 0, 1f) }
    }

    /** How many sizes of "eat" there are (more balls cleared, longer rising run of notes); a chain has one per link. */
    private fun bucketOf(kind: SoundKind, points: Int) = when (kind) {
        SoundKind.EAT -> (points / 12).coerceIn(0, 5)
        SoundKind.COMBO -> points.coerceIn(1, 4)
        else -> 0
    }

    private fun pointsOf(kind: SoundKind, bucket: Int) = if (kind == SoundKind.COMBO) bucket else bucket * 12

    private fun keyOf(kind: SoundKind, bucket: Int) = "${profile.name}-${voice.name}-${kind.name}-$bucket"

    /**
     * Renders one synthesised sound into a small WAV file in the cache (reusing the file when it is already there) and
     * loads it into the pool. Runs on a worker thread; the UI thread only ever looks the id up.
     */
    private fun requestSynth(kind: SoundKind, bucket: Int, volume: Float?) {
        val key = keyOf(kind, bucket)
        if (volume != null) wanted[key] = System.currentTimeMillis() to volume
        if (synthIds.containsKey(key) || !inflight.add(key)) return
        val gen = generation
        val pool = soundPool
        val prof = profile
        val voiceNow = voice
        worker.execute {
            try {
                val file = java.io.File(cacheDir, "$key.wav")
                if (!file.exists() || file.length() < 64) {
                    val points = pointsOf(kind, bucket)
                    // Normalised: the synthesised notes are quiet (a click peaks at 5% of full scale), the beeps flat and harsh.
                    val pcm = if (prof == Profile.PC_SPEAKER) io.github.basil_as.basillines.engine.Wav.normalize(PcSpeaker.render(PcSpeaker.notes(kind, points)), 0.5)
                    else io.github.basil_as.basillines.engine.Wav.normalize(ModernSounds.render(ModernSounds.voiced(kind, points, voiceNow)), 0.8)
                    if (pcm.isEmpty()) return@execute
                    cacheDir.mkdirs()
                    val tmp = java.io.File(cacheDir, "$key.tmp")
                    tmp.writeBytes(io.github.basil_as.basillines.engine.Wav.encode(pcm))
                    tmp.renameTo(file)
                }
                if (gen != generation || pool !== soundPool) return@execute
                val id = pool.load(file.absolutePath, 1)
                if (id <= 0) return@execute
                synthIds[key] = id
                keyOfId[id] = key
                wanted.remove(key)?.let { waiting[id] = it }
                // The pool may have answered before the request was noted.
                if (id in ready) waiting.remove(id)?.let { (at, vol) -> if (System.currentTimeMillis() - at < WAIT_MS) pool.play(id, vol, vol, 1, 0, 1f) }
            } catch (_: Throwable) {
                // A device that refuses a file stays silent for that sound; the game goes on.
            } finally {
                inflight.remove(key)
            }
        }
    }

    @Volatile private var primedFor = -1

    /** Starts loading the current look once (changing the look or rebuilding the pool starts it again by itself). */
    fun prime() {
        if (primedFor == generation) return
        primedFor = generation
        prepare()
    }

    /** Loads every sound of the current look in the background, the ones heard most often first, so the first play is instant. */
    private fun prepare() {
        if (profile == Profile.SAMPLED) return
        val order = listOf(SoundKind.SELECT, SoundKind.JUMP, SoundKind.EAT, SoundKind.BLOCKED, SoundKind.COMBO)
        primedFor = generation
        val kinds = order + SoundKind.entries.filter { it !in order }
        for (kind in kinds) {
            val buckets = when (kind) { SoundKind.EAT -> 0..5; SoundKind.COMBO -> 1..4; else -> 0..0 }
            for (b in buckets) requestSynth(kind, b, null)
        }
    }

    private fun beep(kind: SoundKind, points: Int = 0) {
        if (!isEnabled) return
        val bucket = bucketOf(kind, points)
        val id = synthIds[keyOf(kind, bucket)]
        if (id == null) {
            // Not loaded yet: ask for it (it plays the moment it is ready) instead of rendering it here on the UI thread.
            requestSynth(kind, bucket, 1f)
            return
        }
        play(id, 1f)
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
        generation++
        worker.shutdownNow()
        soundPool.release()
    }

    private companion object {
        /** A sound that was asked for while loading still plays if it becomes ready within this time. */
        const val WAIT_MS = 1500L
    }
}
