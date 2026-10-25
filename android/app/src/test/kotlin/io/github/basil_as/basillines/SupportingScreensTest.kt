package io.github.basil_as.basillines

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import io.github.basil_as.basillines.engine.Progress
import org.junit.Assert.assertEquals
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import io.github.basil_as.basillines.engine.CustomTheme
import io.github.basil_as.basillines.engine.LookOverrides
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], manifest = Config.NONE, qualifiers = "en-rUS-w320dp-h280dp-land")
class SupportingScreensTest {
    @get:Rule val rule = createComposeRule()

    private fun studio(fontScale: Float = 1f) {
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    LookStudioDialog(AppTheme.MODERN, LookOverrides(), CustomTheme.DEFAULT, false,
                        onOverrides = {}, onCustom = {}, onUseCustom = {}, onApplyShare = {}, onClose = {})
                }
            }
        }
    }

    @Test
    fun studioChoicesKeepFullTouchTargetsOnAShortScreen() {
        studio()
        val flat = rule.onNodeWithText("Flat").performScrollTo().fetchSemanticsNode()
        val density = rule.density.density
        assertTrue("Flat option has at least a 48dp target", flat.boundsInRoot.height / density >= 48f)
    }

    @Test
    fun builderAndSharingRemainReachableAtDoubleFontSize() {
        studio(2f)
        rule.onNodeWithText("Close").assertIsDisplayed()
        rule.onNodeWithText("Builder").performScrollTo().performClick()
        rule.onNodeWithText("Use my look").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Copy code").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Apply code").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Close").assertIsDisplayed()
    }
    @Test
    fun settingsLabelsWrapAndEveryGamePreferenceRemainsReachable() {
        var sound = 0
        var preview = 0
        var vibration = 0
        var tutorial = 0
        var update = 0
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                MaterialTheme {
                    SettingsDialog(AppTheme.MODERN, {}, DayNight.AUTO, {}, false, true, { sound++ }, true, { preview++ },
                        AppLanguage.AUTO, {}, EffectsLevel.FULL, {}, true, true, { vibration++ }, { tutorial++ },
                        "4.0.0", true, {}, null, { update++ }, LookOverrides(), {}, CustomTheme.DEFAULT, {}, {}, {}, {})
                }
            }
        }
        rule.onNodeWithText("Sound effects").performScrollTo().performClick()
        val label = rule.onNodeWithText("Show where new balls will appear").performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Scaled preference label remains fully laid out", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        label.performClick()
        rule.onNodeWithText("Vibration").performScrollTo().performClick()
        rule.onNodeWithText("Take the tutorial").performScrollTo().performClick()
        rule.onNodeWithText("Check for updates").performScrollTo().performClick()
        assertEquals(listOf(1, 1, 1, 1, 1), listOf(sound, preview, vibration, tutorial, update))
    }

    @Test
    fun statisticsDataActionsRemainReachableOnAShortScreen() {
        var json = 0
        var csv = 0
        var imported = 0
        rule.setContent {
            MaterialTheme {
                StatsDialog(emptyList(), Progress(), emptyMap(), 1_700_000_000_000, null,
                    { json++ }, { csv++ }, { imported++ }, {}, {})
            }
        }
        rule.onNodeWithText("Data").performScrollTo().performClick()
        rule.onNodeWithText("Save backup (JSON)").performScrollTo().performClick()
        rule.onNodeWithText("Save games as a spreadsheet (CSV)").performScrollTo().performClick()
        rule.onNodeWithText("Load a backup from a file").performScrollTo().performClick()
        assertEquals(listOf(1, 1, 1), listOf(json, csv, imported))
    }

    @Test
    fun tappingOutsideThePanelDismissesTheDialog() {
        var closed = 0
        rule.setContent { MaterialTheme { GoalsDialog(emptyList(), 0) { closed++ } } }
        rule.onNode(isDialog()).performTouchInput { click(Offset(1f, 1f)) }
        rule.runOnIdle { assertEquals(1, closed) }
    }

}
