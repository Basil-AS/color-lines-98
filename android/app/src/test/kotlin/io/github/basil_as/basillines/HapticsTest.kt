package io.github.basil_as.basillines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {

    @Test
    fun everyHapticKindHasAPattern() {
        for (kind in HapticKind.entries) {
            val steps = HAPTIC_PATTERNS[kind]
            assertTrue("Expected pattern for $kind in HAPTIC_PATTERNS", steps != null && steps.isNotEmpty())
            assertTrue("Expected kind.steps to be non-empty for $kind", kind.steps.isNotEmpty())
            assertTrue("Expected kind.pattern to be non-empty for $kind", kind.pattern.isNotEmpty())
        }
    }

    @Test
    fun durationsAndAmplitudesArePositiveAndAmplitudesWithin255() {
        for (kind in HapticKind.entries) {
            val steps = HAPTIC_PATTERNS.getValue(kind)
            for ((index, step) in steps.withIndex()) {
                assertTrue(
                    "Duration of step $index in $kind must be positive: ${step.durationMs}",
                    step.durationMs > 0L
                )
                assertTrue(
                    "Amplitude of step $index in $kind must be positive: ${step.amplitude}",
                    step.amplitude > 0
                )
                assertTrue(
                    "Amplitude of step $index in $kind must be <= 255: ${step.amplitude}",
                    step.amplitude <= 255
                )
                assertTrue(
                    "Gap of step $index in $kind must be >= 0: ${step.gapMs}",
                    step.gapMs >= 0L
                )
            }
        }
    }

    @Test
    fun totalDurationPerKindIsWithinLimit() {
        for (kind in HapticKind.entries) {
            val steps = HAPTIC_PATTERNS.getValue(kind)
            val totalWithGaps = steps.sumOf { it.durationMs + it.gapMs }
            val totalDurationsOnly = steps.sumOf { it.durationMs }

            assertTrue(
                "Total duration with gaps for $kind ($totalWithGaps ms) must be <= 900ms",
                totalWithGaps <= 900L
            )
            assertTrue(
                "Total pulse duration for $kind ($totalDurationsOnly ms) must be <= 900ms",
                totalDurationsOnly <= 900L
            )
            assertEquals(
                "kind.totalDurationMs should match calculated total for $kind",
                totalWithGaps,
                kind.totalDurationMs
            )
        }
    }

    @Test
    fun scalingClampsCorrectlyTo1To255() {
        assertEquals(100, scaleAmplitude(100, 1.0f))
        assertEquals(50, scaleAmplitude(100, 0.5f))
        assertEquals(200, scaleAmplitude(100, 2.0f))
        assertEquals(255, scaleAmplitude(200, 2.0f))
        assertEquals(1, scaleAmplitude(100, 0.0f))
        assertEquals(1, scaleAmplitude(100, -2.0f))
        assertEquals(1, scaleAmplitude(1, 0.01f))
        assertEquals(255, scaleAmplitude(255, 5.0f))

        val testScales = listOf(-1.0f, 0.0f, 0.01f, 0.5f, 1.0f, 1.8f, 3.0f, 10.0f)
        for (kind in HapticKind.entries) {
            val steps = HAPTIC_PATTERNS.getValue(kind)
            for (step in steps) {
                for (scale in testScales) {
                    val scaled = step.scaled(scale)
                    assertTrue(
                        "Scaled amplitude must be in 1..255 for scale $scale on amplitude ${step.amplitude}: ${scaled.amplitude}",
                        scaled.amplitude in 1..255
                    )
                }
            }
        }
    }

    @Test
    fun waveformAndFallbackTimingsGeneration() {
        for (kind in HapticKind.entries) {
            val steps = HAPTIC_PATTERNS.getValue(kind)
            val (timings, amplitudes) = stepsToWaveform(steps, 1.0f)

            assertEquals(
                "Timings and amplitudes array length must match for $kind",
                timings.size,
                amplitudes.size
            )
            assertTrue("Waveform timings must not be empty for $kind", timings.isNotEmpty())
            assertTrue(
                "All timings must be positive for $kind",
                timings.all { it > 0L }
            )
            assertTrue(
                "All amplitudes must be in 0..255 for $kind",
                amplitudes.all { it in 0..255 }
            )

            val fallbackTimings = stepsToTimings(steps)
            assertTrue("Fallback timings must not be empty for $kind", fallbackTimings.isNotEmpty())
            assertTrue("kind.pattern must match fallbackTimings for $kind", kind.pattern.contentEquals(fallbackTimings))
        }
    }
}
