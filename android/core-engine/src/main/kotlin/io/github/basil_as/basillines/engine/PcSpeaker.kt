package io.github.basil_as.basillines.engine

/**
 * Everything the game can say: SELECT/JUMP/EAT/LOSE/CLICK are the moves, BLOCKED a refused move, START a new
 * game, RECORD a new best score, CROWN the pretender taking the throne, LEVEL_UP and ACHIEVEMENT the rewards,
 * COMBO a chain of clearing moves (its length is `points`), DANGER a nearly full board, TICK the last seconds of a timed
 * game, HINT a hint, POP new balls appearing.
 */
enum class SoundKind { SELECT, JUMP, EAT, LOSE, WIN, CLICK, BLOCKED, START, RECORD, CROWN, LEVEL_UP, ACHIEVEMENT, COMBO, DANGER, TICK, HINT, POP }

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
        SoundKind.BLOCKED -> listOf(Beep(196, 70), Beep(147, 90))
        SoundKind.START -> listOf(C5, G5, C6).map { Beep(it, 70) }
        SoundKind.LEVEL_UP -> listOf(C5, E5, G5, C6, E6).map { Beep(it, 75) }
        SoundKind.ACHIEVEMENT -> listOf(Beep(E6, 60), Beep(G6, 110))
        SoundKind.COMBO -> listOf(C6, E6, G6, C6 * 2).take(minOf(4, 1 + maxOf(1, points))).map { Beep(it, 40) }
        SoundKind.DANGER -> listOf(Beep(131, 100), Beep(123, 150))
        SoundKind.TICK -> listOf(Beep(1200, 15))
        SoundKind.HINT -> listOf(Beep(E6, 40), Beep(G6, 60))
        SoundKind.POP -> listOf(Beep(900, 12), Beep(1200, 12), Beep(1500, 12))
        // The trumpet fanfare of the original characters: a short triple, then the rising chord.
        SoundKind.RECORD, SoundKind.CROWN -> listOf(
            Beep(C5, 90), Beep(C5, 90), Beep(C5, 90), Beep(E5, 160), Beep(G5, 120), Beep(C6, 320)
        )
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
