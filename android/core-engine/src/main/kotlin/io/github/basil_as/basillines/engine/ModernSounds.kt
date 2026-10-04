package io.github.basil_as.basillines.engine

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Wave { SINE, TRIANGLE, SQUARE, SAWTOOTH }

/** Every look has its own voice: the same melodies played by a different instrument (mirrors src/modernsound.ts). */
enum class VoiceId(val wave: Wave?, val pitch: Double, val tempo: Double, val gain: Double) {
    SOFT(null, 1.0, 1.0, 1.0),
    BELL(Wave.SINE, 2.0, 0.8, 1.0),
    MARIMBA(Wave.TRIANGLE, 1.0, 0.7, 1.5),
    ARCADE(Wave.SQUARE, 1.0, 0.75, 0.45),
    SAW(Wave.SAWTOOTH, 1.0, 1.25, 0.5),
    GLASS(Wave.SINE, 1.5, 1.5, 1.0),
    WOOD(Wave.TRIANGLE, 1.5, 0.5, 1.6),
    CHIP(Wave.SQUARE, 2.0, 0.5, 0.4),
    TELETYPE(Wave.SQUARE, 1.5, 0.4, 0.4),
    BEEP(Wave.SQUARE, 1.25, 1.6, 0.45)
}

data class ModernNote(val freq: Int, val ms: Int, val wave: Wave, val gain: Double)

/** The soft synthesised sounds of the modern themes (the older looks keep their own sounds). */
object ModernSounds {
    private const val C5 = 523
    private const val D5 = 587
    private const val E5 = 659
    private const val G5 = 784
    private const val A5 = 880
    private const val C6 = 1047
    private const val D6 = 1175
    private const val E6 = 1319

    private fun n(freq: Int, ms: Int, wave: Wave = Wave.SINE, gain: Double = 0.22) = ModernNote(freq, ms, wave, gain)

    fun notes(kind: SoundKind, points: Int = 0): List<ModernNote> = when (kind) {
        SoundKind.SELECT -> listOf(n(C6, 70, Wave.SINE, 0.2))
        SoundKind.JUMP -> listOf(n(C5, 45), n(E5, 45), n(G5, 60))
        SoundKind.CLICK -> listOf(n(700, 35, Wave.TRIANGLE, 0.16))
        SoundKind.EAT -> {
            val ladder = listOf(C5, D5, E5, G5, A5, C6, D6, E6)
            val count = minOf(ladder.size, 3 + maxOf(0, points) / 12)
            ladder.take(count).mapIndexed { i, f -> n(f, 75, Wave.SINE, 0.2 + i * 0.01) }
        }
        SoundKind.LOSE -> listOf(440, 349, 294, 220).map { n(it, 190, Wave.TRIANGLE, 0.2) }
        SoundKind.WIN -> listOf(C5, E5, G5, C6, E6).map { n(it, 130, Wave.SINE, 0.22) }
        SoundKind.BLOCKED -> listOf(n(220, 80, Wave.TRIANGLE, 0.14), n(196, 110, Wave.TRIANGLE, 0.12))
        SoundKind.START -> listOf(n(G5, 70), n(C6, 70), n(E6, 110))
        SoundKind.LEVEL_UP -> listOf(C5, E5, G5, C6, E6).mapIndexed { i, f -> n(f, 90, Wave.SINE, 0.2 + i * 0.01) }
        SoundKind.ACHIEVEMENT -> listOf(n(E6, 70, Wave.SINE, 0.2), n(G5 * 2, 130, Wave.SINE, 0.22))
        SoundKind.COMBO -> listOf(G5, C6, E6, 1568, 2093).take(minOf(5, 1 + maxOf(1, points))).mapIndexed { i, f -> n(f, 60, Wave.SINE, 0.2 + i * 0.01) }
        SoundKind.DANGER -> listOf(n(262, 130, Wave.TRIANGLE, 0.18), n(247, 190, Wave.TRIANGLE, 0.18))
        SoundKind.TICK -> listOf(n(1200, 25, Wave.TRIANGLE, 0.14))
        SoundKind.HINT -> listOf(n(E6, 60, Wave.SINE, 0.18), n(1568, 90, Wave.SINE, 0.18))
        SoundKind.POP -> listOf(n(880, 25, Wave.SINE, 0.12), n(1109, 25, Wave.SINE, 0.12), n(1319, 30, Wave.SINE, 0.12))
        SoundKind.RECORD, SoundKind.CROWN ->
            listOf(C5, E5, G5, C6, G5, C6, E6).mapIndexed { i, f -> n(f, if (i == 6) 320 else 110, Wave.SINE, 0.22) }
    }

    fun voiced(kind: SoundKind, points: Int, voice: VoiceId): List<ModernNote> = notes(kind, points).map {
        ModernNote(
            freq = Math.round(it.freq * voice.pitch).toInt(),
            ms = maxOf(20, Math.round(it.ms * voice.tempo).toInt()),
            wave = voice.wave ?: it.wave,
            gain = it.gain * voice.gain
        )
    }

    /** Signed 16-bit mono PCM with a short attack and a smooth decay on every note. */
    fun render(notes: List<ModernNote>, sampleRate: Int = 22_050): ShortArray {
        val total = notes.sumOf { it.ms * sampleRate / 1000 }
        val out = ShortArray(total)
        var pos = 0
        for (note in notes) {
            val count = note.ms * sampleRate / 1000
            val attack = (0.008 * sampleRate).toInt().coerceAtLeast(1)
            for (i in 0 until count) {
                val phase = (i.toDouble() * note.freq / sampleRate) % 1.0
                val wave = when (note.wave) {
                    Wave.SINE -> sin(2 * PI * phase)
                    Wave.TRIANGLE -> 4 * kotlin.math.abs(phase - 0.5) - 1
                    Wave.SQUARE -> if (phase < 0.5) 1.0 else -1.0
                    Wave.SAWTOOTH -> 2 * phase - 1
                }
                val attackEnv = if (i < attack) i.toDouble() / attack else 1.0
                val decay = exp(-5.0 * i / count)
                out[pos + i] = (wave * attackEnv * decay * note.gain * Short.MAX_VALUE).toInt().toShort()
            }
            pos += count
        }
        return out
    }
}
