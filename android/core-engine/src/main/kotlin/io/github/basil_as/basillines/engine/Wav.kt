package io.github.basil_as.basillines.engine

/** A 16-bit mono WAV file, so synthesised sounds can be loaded once into a SoundPool instead of building an AudioTrack per play. */
object Wav {
    const val SAMPLE_RATE = 22_050

    fun encode(pcm: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataBytes = pcm.size * 2
        val out = java.nio.ByteBuffer.allocate(44 + dataBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + dataBytes).put("WAVE".toByteArray(Charsets.US_ASCII))
        out.put("fmt ".toByteArray(Charsets.US_ASCII)).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        out.put("data".toByteArray(Charsets.US_ASCII)).putInt(dataBytes)
        for (s in pcm) out.putShort(s)
        return out.array()
    }

    /** Scales the samples so the loudest one reaches [peak] of full scale; synthesised sounds start quiet (a click peaks at 5%). */
    fun normalize(pcm: ShortArray, peak: Double): ShortArray {
        val loudest = pcm.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        if (loudest == 0) return pcm
        val k = peak * Short.MAX_VALUE / loudest
        return ShortArray(pcm.size) { (pcm[it] * k).toInt().coerceIn(-32767, 32767).toShort() }
    }
}
