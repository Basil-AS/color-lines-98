package io.github.basil_as.basillines

import android.content.Context
import android.media.SoundPool
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.robolectric.Shadows.shadowOf
import androidx.test.core.app.ApplicationProvider
import io.github.basil_as.basillines.engine.SoundKind
import io.github.basil_as.basillines.engine.VoiceId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoundLifecycleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun delayedGameSoundAfterReleaseIsIgnored() {
        val sounds = SoundManager(context)
        sounds.isEnabled = true
        sounds.release()
        // Result rewards are posted 1100 ms after the move. Destruction can happen first.
        sounds.play(SoundKind.ACHIEVEMENT)
    }

    @Test
    fun disposedCompositionCannotPrimeOrChangeTheReleasedProfile() {
        val sounds = SoundManager(context)
        sounds.release()
        sounds.prime()
        sounds.profile = SoundManager.Profile.PC_SPEAKER
        sounds.voice = VoiceId.entries.first { it != VoiceId.SOFT }
    }

    @Test
    fun foregroundRecoveryAfterReleaseCannotReopenThePool() {
        val sounds = SoundManager(context)
        sounds.release()
        sounds.recover()
        sounds.onForeground(70_000)
        sounds.release()
    }
    @Test
    fun queuedLoadCompletionCannotPlayAfterRelease() {
        val sounds = SoundManager(context)
        sounds.isEnabled = true
        sounds.profile = SoundManager.Profile.SAMPLED
        // Keep the captured old pool to deliver the callback after its owner is destroyed.
        val field = SoundManager::class.java.getDeclaredField("soundPool").apply { isAccessible = true }
        val pool = field.get(sounds) as SoundPool
        val shadow = shadowOf(pool)
        sounds.play(SoundKind.SELECT)
        sounds.release()
        shadow.notifyResourceLoaded(R.raw.select_ball, true)
        assertFalse("No late playback, including warm-up, after release", shadow.wasResourcePlayed(R.raw.select_ball))
    }

    @Test
    fun aReplacementManagerStillPlaysAfterTheOldOneWasReleased() {
        val old = SoundManager(context)
        old.release()
        val replacement = SoundManager(context)
        try {
            replacement.isEnabled = true
            replacement.profile = SoundManager.Profile.SAMPLED
            val field = SoundManager::class.java.getDeclaredField("soundPool").apply { isAccessible = true }
            val pool = field.get(replacement) as SoundPool
            replacement.play(SoundKind.SELECT)
            shadowOf(pool).notifyResourceLoaded(R.raw.select_ball, true)
            assertTrue(shadowOf(pool).wasResourcePlayed(R.raw.select_ball))
            // A disposed Activity's late event cannot affect the recreated Activity's pool.
            old.play(SoundKind.ACHIEVEMENT)
        } finally {
            replacement.release()
        }
    }

}
