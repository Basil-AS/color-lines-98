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
        assertEquals(listOf(220, 175, 147, 110), saw.map { it.freq })
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
}
