package io.github.basil_as.basillines.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicesTest {
    @Test
    fun voicesMatchTheWebApp() {
        // Values computed by voicedNotes() in src/modernsound.ts.
        val chip = ModernSounds.voiced(SoundKind.EAT, 30, VoiceId.CHIP)
        assertEquals(listOf(1046, 1174, 1318, 1568, 1760), chip.map { it.freq })
        assertEquals(List(5) { 38 }, chip.map { it.ms })
        assertTrue(chip.all { it.wave == Wave.SQUARE })
        val saw = ModernSounds.voiced(SoundKind.LOSE, 0, VoiceId.SAW)
        assertEquals(listOf(440, 349, 294, 220), saw.map { it.freq })
        assertEquals(List(4) { 238 }, saw.map { it.ms })
    }

    @Test
    fun everyVoiceRendersEveryEventAndNoTwoSoundAlike() {
        val seen = HashSet<List<ModernNote>>()
        for (voice in VoiceId.entries) {
            for (kind in SoundKind.entries) {
                val notes = ModernSounds.voiced(kind, 30, voice)
                assertTrue("$voice/$kind", notes.isNotEmpty())
                assertTrue(ModernSounds.render(notes).isNotEmpty())
            }
            assertTrue("$voice duplicates another voice", seen.add(ModernSounds.voiced(SoundKind.EAT, 30, voice)))
        }
    }

    @Test
    fun everyEventInEveryVoiceReachesTheSameLoudnessAfterNormalising() {
        for (voice in VoiceId.entries) for (kind in SoundKind.entries) {
            val pcm = Wav.normalize(ModernSounds.render(ModernSounds.voiced(kind, 30, voice)), 0.8)
            val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$voice/$kind peaks at $peak of 32767", peak in 25_000..27_000)
            assertTrue("$voice never plays below the original pitch", voice.pitch >= 1.0)
        }
        val beeps = Wav.normalize(PcSpeaker.render(PcSpeaker.notes(SoundKind.SELECT, 0)), 0.5)
        assertTrue(beeps.maxOf { kotlin.math.abs(it.toInt()) } in 15_000..17_500)
        assertEquals(0, Wav.normalize(ShortArray(4), 0.8).maxOf { it.toInt() })
    }
}
