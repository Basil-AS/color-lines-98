package io.github.basil_as.basillines

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeRight
import io.github.basil_as.basillines.engine.Progress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "en-rUS-w400dp-h800dp")
class SwipeDialogTabsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun statisticsSwipesToNeighborAndBackAndButtonsStillWork() {
        rule.setContent {
            MaterialTheme {
                StatsDialog(emptyList(), Progress(), emptyMap(), 1_700_000_000_000, null, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Career").assertIsSelected()
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeRight() }
        rule.onNodeWithText("Overview").assertIsSelected()
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeUp() }
        rule.onNodeWithText("Overview").assertIsSelected()
        rule.onNodeWithText("Mind").performClick().assertIsSelected()
    }

    @Test
    @Config(qualifiers = "en-rUS-w800dp-h400dp-land")
    fun landscapeVerticalScrollKeepsTabsAndCloseVisible() {
        rule.setContent {
            MaterialTheme {
                StatsDialog(emptyList(), Progress(), emptyMap(), 1_700_000_000_000, null, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeUp() }
        rule.onNodeWithText("Overview").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test fun settingsSwipesBetweenGroups() {
        rule.setContent {
            MaterialTheme {
                SettingsDialog(AppTheme.MODERN, {}, DayNight.AUTO, {}, false, true, {}, true, {},
                    AppLanguage.AUTO, {}, EffectsLevel.FULL, {}, true, true, {}, {}, "4.0.0", true, {}, null,
                    {}, io.github.basil_as.basillines.engine.LookOverrides(), {},
                    io.github.basil_as.basillines.engine.CustomTheme.DEFAULT, {}, {}, {}, {})
            }
        }
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeLeft() }
        rule.onNodeWithText("Game").assertIsSelected()
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeLeft() }
        rule.onNodeWithText("System").assertIsSelected()
        rule.onNodeWithTag("dialog-tab-pages").performTouchInput { swipeRight() }
        rule.onNodeWithText("Game").assertIsSelected()
    }
}
