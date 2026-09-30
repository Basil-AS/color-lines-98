package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class PcSpeakerTest {

    @Test
    fun everyEventIsAShortAudibleMelody() {
        for (kind in SoundKind.entries) {
            val notes = PcSpeaker.notes(kind, 20)
            assertTrue("$kind has notes", notes.isNotEmpty())
            for (n in notes) {
                assertTrue(n.freq in 100..5000)
                assertTrue(n.ms > 0)
            }
            assertTrue(notes.sumOf { it.ms } < 1500)
        }
    }

    @Test
    fun biggerScoresPlayALongerFlourish() {
        assertTrue(PcSpeaker.notes(SoundKind.EAT, 60).size > PcSpeaker.notes(SoundKind.EAT, 10).size)
    }

    @Test
    fun loseDescendsAndWinAscends() {
        val lose = PcSpeaker.notes(SoundKind.LOSE).map { it.freq }
        val win = PcSpeaker.notes(SoundKind.WIN).map { it.freq }
        assertTrue(lose.first() > lose.last())
        assertTrue(win.first() < win.last())
    }

    @Test
    fun rendersASquareWaveOfTheRightLength() {
        val pcm = PcSpeaker.render(listOf(Beep(440, 100)), sampleRate = 8000, amplitude = 1000)
        assertEquals(800, pcm.size)
        assertTrue(pcm.all { it == 1000.toShort() || it == (-1000).toShort() })
        assertTrue(pcm.any { it > 0 } && pcm.any { it < 0 })
        assertEquals(0, PcSpeaker.render(emptyList()).size)
    }
}
