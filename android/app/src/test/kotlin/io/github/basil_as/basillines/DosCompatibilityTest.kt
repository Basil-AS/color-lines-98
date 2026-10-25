package io.github.basil_as.basillines

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-rUS-w914dp-h411dp-land")
class DosCompatibilityTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun startDos(fontScale: Float = 1f) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("colorlines_prefs", Context.MODE_PRIVATE).edit()
            .clear().putString("theme", AppTheme.COLORLINES_92.name).commit()
        RuntimeEnvironment.setFontScale(fontScale)
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    @After fun resetFontScale() { RuntimeEnvironment.setFontScale(1f) }

    @Test fun largeLandscapeTextKeepsGoalsVisibleAndUsable() {
        startDos(2f)
        val goals = rule.onNodeWithText("Goals 0/3").assertIsDisplayed()
        val bounds = goals.fetchSemanticsNode().boundsInRoot
        assertTrue("Goals need a visible touch target: $bounds", bounds.width > 0 && bounds.height > 0)
        goals.performTouchInput { click() }
        rule.onNodeWithText("Today's goals").assertIsDisplayed()
    }

    @Test fun largeLandscapeTextKeepsBothBottomKeysFullyVisibleAndAtLeast48DpTall() {
        startDos(2f)
        val viewport = rule.onRoot().getUnclippedBoundsInRoot()
        for (label in listOf("F3: Show next balls", "F4: Restart")) {
            val key = rule.onNodeWithContentDescription(label).assertIsDisplayed()
            val bounds = key.getUnclippedBoundsInRoot()
            assertTrue("$label must be at least 48 dp: $bounds", bounds.bottom - bounds.top >= 48.dp)
            assertTrue("$label must fit inside $viewport: $bounds", bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
            val clipped = key.fetchSemanticsNode().boundsInRoot
            val density = rule.density.density
            assertTrue("$label visible target must be at least 48 dp: $clipped", clipped.height / density >= 47.9f)
        }
    }

    @Test fun androidBackClosesHelpWithoutFinishingTheGame() {
        startDos()
        rule.onNodeWithContentDescription("F1: Help").performTouchInput { click() }
        verifyBackClosesOverlay()
    }

    @Test fun androidBackClosesTopTenWithoutFinishingTheGame() {
        startDos()
        rule.onNodeWithContentDescription("Top Ten").performClick()
        verifyBackClosesOverlay()
    }

    private fun verifyBackClosesOverlay() {
        rule.onNodeWithContentDescription("Close").assertIsDisplayed()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertFalse("Back must leave the game running", rule.activity.isFinishing) }
        rule.onNodeWithContentDescription("Close").assertDoesNotExist()
        rule.onNodeWithContentDescription("Row 1, column 1", substring = true).assertIsDisplayed()
    }
}
