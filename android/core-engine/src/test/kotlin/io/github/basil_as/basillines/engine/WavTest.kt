package io.github.basil_as.basillines.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class WavTest {
    @Test
    fun writesAValidMonoSixteenBitFile() {
        val pcm = shortArrayOf(0, 1000, -1000, Short.MAX_VALUE)
        val bytes = Wav.encode(pcm)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals(bytes.size - 8, b.getInt(4))
        assertEquals("WAVEfmt ", String(bytes, 8, 8))
        assertEquals(1, b.getShort(20).toInt()) // PCM
        assertEquals(1, b.getShort(22).toInt()) // mono
        assertEquals(22_050, b.getInt(24))
        assertEquals(16, b.getShort(34).toInt())
        assertEquals("data", String(bytes, 36, 4))
        assertEquals(8, b.getInt(40))
        assertEquals(1000, b.getShort(46).toInt())
        assertEquals(44 + 8, bytes.size)
    }

    @Test
    fun everyRenderedSoundBecomesAWavOfTheRightLength() {
        for (voice in VoiceId.entries) for (kind in SoundKind.entries) {
            val pcm = ModernSounds.render(ModernSounds.voiced(kind, 30, voice))
            assertEquals(44 + pcm.size * 2, Wav.encode(pcm).size)
        }
        val beeps = PcSpeaker.render(PcSpeaker.notes(SoundKind.EAT, 40))
        assertEquals(44 + beeps.size * 2, Wav.encode(beeps).size)
    }
}
