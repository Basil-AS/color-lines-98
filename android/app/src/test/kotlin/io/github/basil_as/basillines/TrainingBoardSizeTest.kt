package io.github.basil_as.basillines

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29], qualifiers = "en-rUS-w411dp-h891dp-port")
class TrainingBoardSizeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun verify(entry: String) {
        val before = rule.onNodeWithTag("adaptive-game-board").fetchSemanticsNode().boundsInRoot
        rule.clickGameAction("Rules and info")
        rule.onNodeWithText(entry).performClick()
        if (entry == "Strategy coach") {
            rule.onNodeWithText("Show a hint").assertIsDisplayed()
            rule.onNodeWithText("Leave training").assertIsDisplayed()
        }
        rule.onNodeWithText("Details").assertIsDisplayed().performClick()
        rule.onNodeWithText("Close").performClick()
        val after = rule.onNodeWithTag("adaptive-game-board").fetchSemanticsNode().boundsInRoot
        assertEquals("Training must preserve board width", before.width, after.width, .5f)
        assertEquals("Training must preserve board height", before.height, after.height, .5f)
        assertEquals("Training must preserve board position", before.top, after.top, .5f)
        assertEquals("Training must preserve board position", before.left, after.left, .5f)
    }
    @Test fun recordInProgressKeepsBoardUnchangedDuringTraining() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("colorlines_prefs", android.content.Context.MODE_PRIVATE)
        val parts = prefs.getString("game", null)!!.split("|").toMutableList()
        parts[1] = "12345678"
        prefs.edit().putString("game", parts.joinToString("|")).putInt("best_score", 0).commit()
        rule.activityRule.scenario.recreate()
        verify("Strategy coach")
    }
    @Test fun portraitLargeFontKeepsTrainingActionsVisible() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        rule.activityRule.scenario.recreate()
        try { verify("Strategy coach") } finally { org.robolectric.RuntimeEnvironment.setFontScale(1f) }
    }
    @Test fun portraitCoachKeepsBoardUnchanged() = verify("Strategy coach")
    @Test @Config(qualifiers = "en-rUS-w891dp-h411dp-land")
    fun landscapeCoachKeepsBoardUnchanged() = verify("Strategy coach")
    @Test fun portraitBasicTutorialKeepsBoardUnchanged() = verify("Take the tutorial")
    @Test @Config(qualifiers = "en-rUS-w891dp-h411dp-land")
    fun landscapeBasicTutorialKeepsBoardUnchanged() = verify("Take the tutorial")
}
