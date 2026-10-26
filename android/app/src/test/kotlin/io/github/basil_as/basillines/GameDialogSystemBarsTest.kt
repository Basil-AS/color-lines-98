package io.github.basil_as.basillines

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.basil_as.basillines.engine.CustomTheme
import io.github.basil_as.basillines.engine.LookOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/** Android 10 uses decor flags, so these assert the actual focused dialog's request. */
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29], qualifiers = "en-rUS-w899dp-h423dp-land")
class GameDialogSystemBarsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun supportingDialogRequestsImmersiveBarsInLandscape() {
        rule.setContent { MaterialTheme { HelpDialog(onClose = {}, onTutorial = {}) } }
        assertLandscapeDialogBars()
    }

    @Test fun appearanceStudioRequestsImmersiveBarsInLandscape() {
        rule.setContent {
            MaterialTheme {
                LookStudioDialog(AppTheme.MODERN, LookOverrides(), CustomTheme.DEFAULT, true,
                    {}, {}, {}, {}, {})
            }
        }
        assertLandscapeDialogBars()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-port")
    fun portraitSupportingDialogKeepsSystemBarsAvailable() {
        rule.setContent { MaterialTheme { HelpDialog(onClose = {}, onTutorial = {}) } }
        rule.runOnIdle {
            val flags = ShadowDialog.getLatestDialog().window!!.decorView.systemUiVisibility
            assertEquals(0, flags and View.SYSTEM_UI_FLAG_FULLSCREEN)
            assertEquals(0, flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }

    private fun assertLandscapeDialogBars() {
        rule.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog()
            assertTrue("The regression must inspect a showing dialog, not the activity", dialog.isShowing)
            val flags = dialog.window!!.decorView.systemUiVisibility
            assertTrue("Dialog must hide the status bar", flags and View.SYSTEM_UI_FLAG_FULLSCREEN != 0)
            assertTrue("Dialog must hide the navigation bar", flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0)
            assertTrue("A swipe must reveal transient bars", flags and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY != 0)
        }
    }
}
