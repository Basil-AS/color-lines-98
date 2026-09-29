package io.github.basil_as.basillines.engine

enum class SoundKind { SELECT, JUMP, EAT, LOSE, WIN, CLICK }

data class Beep(val freq: Int, val ms: Int)

/** The beeps a PC speaker would play for a game event (used by the DOS theme). */
object PcSpeaker {
    private const val C5 = 523
    private const val E5 = 659
    private const val G5 = 784
    private const val A5 = 880
    private const val C6 = 1047
    private const val E6 = 1319
    private const val G6 = 1568

    fun notes(kind: SoundKind, points: Int = 0): List<Beep> = when (kind) {
        SoundKind.SELECT -> listOf(Beep(A5, 25))
        SoundKind.JUMP -> listOf(Beep(440, 20), Beep(660, 20))
        SoundKind.CLICK -> listOf(Beep(330, 20))
        SoundKind.EAT -> {
            val ladder = listOf(C5, E5, G5, C6, E6, G6, C6 * 2)
            val count = minOf(ladder.size, 3 + maxOf(0, points) / 12)
            ladder.take(count).map { Beep(it, 45) }
        }
        SoundKind.LOSE -> listOf(A5, G5, E5, C5, 262).map { Beep(it, 140) }
        SoundKind.WIN -> listOf(C5, E5, G5, C6).map { Beep(it, 110) }
    }

    /** Signed 16-bit mono PCM of a square wave for the given beeps. */
    fun render(beeps: List<Beep>, sampleRate: Int = 22_050, amplitude: Int = 6_000): ShortArray {
        val total = beeps.sumOf { it.ms * sampleRate / 1000 }
        val out = ShortArray(total)
        var pos = 0
        for (b in beeps) {
            val n = b.ms * sampleRate / 1000
            val period = sampleRate.toDouble() / b.freq
            for (i in 0 until n) {
                out[pos + i] = if ((i % period) < period / 2) amplitude.toShort() else (-amplitude).toShort()
            }
            pos += n
        }
        return out
    }
}
