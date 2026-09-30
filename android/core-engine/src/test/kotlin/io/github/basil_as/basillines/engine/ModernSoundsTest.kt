package io.github.basil_as.basillines.engine

import org.junit.Assert.*
import org.junit.Test

class ModernSoundsTest {

    @Test
    fun everyEventIsAShortQuietAudibleSound() {
        for (kind in SoundKind.entries) {
            val notes = ModernSounds.notes(kind, 30)
            assertTrue(notes.isNotEmpty())
            for (n in notes) {
                assertTrue(n.freq in 100..3000)
                assertTrue(n.ms > 0)
                assertTrue(n.gain > 0 && n.gain <= 0.5)
            }
            assertTrue(notes.sumOf { it.ms } < 1600)
        }
    }

    @Test
    fun biggerScoresPlayALongerRunButItIsCapped() {
        val lengths = listOf(10, 30, 60, 500).map { ModernSounds.notes(SoundKind.EAT, it).size }
        assertTrue(lengths[1] > lengths[0])
        assertTrue(lengths[2] > lengths[1])
        assertTrue(lengths[3] <= 8)
    }

    @Test
    fun winRisesAndLoseFalls() {
        val win = ModernSounds.notes(SoundKind.WIN).map { it.freq }
        val lose = ModernSounds.notes(SoundKind.LOSE).map { it.freq }
        assertTrue(win.first() < win.last())
        assertTrue(lose.first() > lose.last())
    }

    @Test
    fun rendersPcmOfTheRightLengthThatFadesOutWithoutClipping() {
        val pcm = ModernSounds.render(listOf(ModernNote(440, 100, Wave.SINE, 0.2)), sampleRate = 8000)
        assertEquals(800, pcm.size)
        assertTrue(pcm.any { it > 0 } && pcm.any { it < 0 })
        assertTrue(pcm.all { kotlin.math.abs(it.toInt()) < Short.MAX_VALUE * 0.25 })
        val head = pcm.take(200).maxOf { kotlin.math.abs(it.toInt()) }
        val tail = pcm.takeLast(100).maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("decays", tail < head)
        assertEquals(0, ModernSounds.render(emptyList()).size)
    }
}
