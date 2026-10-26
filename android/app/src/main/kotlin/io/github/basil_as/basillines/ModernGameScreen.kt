package io.github.basil_as.basillines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.Point
import kotlin.math.roundToInt

/** The board stays fixed while supporting information can scroll in short windows. */
@Composable
internal fun ModernGameScreen(
    palette: Palette,
    score: Int,
    best: Int,
    nextColors: List<BallColor>,
    canUndo: Boolean,
    soundEnabled: Boolean,
    snapshot: BoardSnapshot,
    onCellTap: (Point) -> Unit,
    onUndo: () -> Unit,
    onNewGame: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onToggleSound: () -> Unit,
    extras: GameExtras,
    trainingPanel: (@Composable () -> Unit)? = null,
    layoutScores: Pair<Int, Int>? = null
) {
    val header: @Composable () -> Unit = {
        ModernHeader(palette, score, best, nextColors, extras)
    }
    val measuredHeaderContent: @Composable () -> Unit = {
        if (layoutScores != null) ModernHeader(palette, layoutScores.first, layoutScores.second, nextColors, extras)
        else header()
    }
    val dock: @Composable () -> Unit = {
        ModernDock(palette, canUndo, soundEnabled, onUndo, onNewGame, onStats,
            onSettings, onHelp, onToggleSound, extras)
    }
    SubcomposeLayout(
        Modifier.fillMaxSize().background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        // Measure the stacked candidate before selecting either arrangement. An estimated
        // header can otherwise make a smaller side layout win near the window-size boundary.
        val stackPadding = (minOf(4f, width / density / 2, height / density / 2) * density).roundToInt()
        val stackWidth = (width - 2 * stackPadding).coerceAtLeast(0)
        val stackHeight = (height - 2 * stackPadding).coerceAtLeast(0)
        val measuredHeader = subcompose("header-probe") {
            Column(Modifier.clearAndSetSemantics {}.fillMaxWidth().verticalScroll(rememberScrollState())) { measuredHeaderContent() }
        }.single().measure(Constraints(minWidth = stackWidth, maxWidth = stackWidth,
            maxHeight = stackHeight / 3))
        val measuredDock = subcompose("dock-probe") {
            Column(Modifier.clearAndSetSemantics {}.fillMaxWidth().verticalScroll(rememberScrollState())) { dock() }
        }.single().measure(Constraints(minWidth = stackWidth, maxWidth = stackWidth,
            maxHeight = stackHeight / 3))
        val initial = adaptiveGameLayout(width / density, height / density, fontScale,
            measuredHeader.height / density, measuredDock.height / density)
        val padding = (initial.padding * density).roundToInt()
        val gap = (initial.gap * density).roundToInt()
        val innerWidth = (width - 2 * padding).coerceAtLeast(0)
        val innerHeight = (height - 2 * padding).coerceAtLeast(0)

        if (initial.sideBySide) {
            val boardSize = (initial.boardSize * density).roundToInt().coerceAtMost(innerHeight)
            val panelWidth = (initial.panelWidth * density).roundToInt()
                .coerceAtMost((innerWidth - boardSize - gap).coerceAtLeast(0))
            val board = subcompose("board") {
                ModernBoard(snapshot, palette, onCellTap, extras)
            }.single().measure(Constraints.fixed(boardSize, boardSize))
            val panel = subcompose("panel") {
                if (trainingPanel != null) {
                    Box(Modifier.fillMaxSize().testTag("adaptive-game-panel")) { trainingPanel() }
                } else {
                    Column(Modifier.fillMaxSize().testTag("adaptive-game-panel")
                        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ModernHeader(palette, score, best, nextColors, extras, sidePanel = true)
                        ModernDock(palette, canUndo, soundEnabled, onUndo, onNewGame, onStats,
                            onSettings, onHelp, onToggleSound, extras, sidePanel = true)
                    }
                }
            }.single().measure(Constraints.fixed(panelWidth, innerHeight))
            layout(width, height) {
                val start = ((width - boardSize - gap - panelWidth) / 2).coerceAtLeast(padding)
                board.placeRelative(start, (height - boardSize) / 2)
                panel.placeRelative(start + boardSize + gap, padding)
            }
        } else {
            val header = subcompose("header") {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { header() }
            }.single().measure(if (layoutScores != null) Constraints.fixed(stackWidth, measuredHeader.height)
                else Constraints(minWidth = stackWidth, maxWidth = stackWidth, maxHeight = stackHeight / 3))
            val dock = subcompose("dock") {
                if (trainingPanel != null) Box(Modifier.fillMaxSize()) { trainingPanel() }
                else Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { dock() }
            }.single().measure(if (trainingPanel != null) Constraints.fixed(stackWidth, measuredDock.height)
                else Constraints(minWidth = stackWidth, maxWidth = stackWidth, maxHeight = stackHeight / 3))
            val available = (innerHeight - header.height - dock.height - 2 * gap).coerceAtLeast(0)
            val boardSize = (initial.boardSize * density).roundToInt()
                .coerceIn(0, minOf(innerWidth, available))
            val board = subcompose("board") {
                ModernBoard(snapshot, palette, onCellTap, extras)
            }.single().measure(Constraints.fixed(boardSize, boardSize))
            layout(width, height) {
                header.placeRelative(padding, padding)
                board.placeRelative((width - boardSize) / 2,
                    padding + header.height + gap + (available - boardSize) / 2)
                dock.placeRelative(padding, height - padding - dock.height)
            }
        }
    }
}

@Composable
private fun ModernBoard(snapshot: BoardSnapshot, palette: Palette, onCellTap: (Point) -> Unit, extras: GameExtras) {
    Box(Modifier.fillMaxSize().testTag("adaptive-game-board")) {
        BoardView(snapshot, palette, onCellTap, Modifier.fillMaxSize(),
            fx = extras.fx, level = extras.level, danger = extras.danger)
    }
}

@Composable
private fun ModernHeader(palette: Palette, score: Int, best: Int, nextColors: List<BallColor>, extras: GameExtras, sidePanel: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrowHeader = maxWidth < 320.dp
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val separateScore = (sidePanel && narrowHeader) || score >= 10000 || best >= 10000 || LocalDensity.current.fontScale > 1.4f
            if (separateScore) {
                ModernScore(stringResource(R.string.hud_score), score, palette, Modifier.fillMaxWidth(), true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    ModernScore(stringResource(R.string.hud_best), best, palette, Modifier.weight(1f))
                    ModernNext(palette, nextColors, Modifier.weight(.8f))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    ModernScore(stringResource(R.string.hud_score), score, palette, Modifier.weight(1f), true)
                    ModernScore(stringResource(R.string.hud_best), best, palette, Modifier.weight(1f))
                    ModernNext(palette, nextColors, Modifier.weight(1.2f))
                }
            }
            // Independent entries retain their natural width: long translations never break into a
            // stack of letters. The row scrolls when the font or window leaves insufficient room.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ModernAction(palette, extras.modeLabel, extras.onChangeMode, icon = null)
                if (extras.clock != null) {
                    val clockDescription = stringResource(R.string.mode_timeLeft, extras.clock)
                    Text(extras.clock, color = palette.accent, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.semantics { contentDescription = clockDescription })
                }
                if (extras.goalsLabel.isNotEmpty()) {
                    ModernAction(palette, extras.goalsLabel, extras.onGoals, AppIcons.Flag)
                }
            }
        }
    }
}

@Composable
private fun ModernScore(label: String, value: Int, palette: Palette, modifier: Modifier, primary: Boolean = false) {
    Column(modifier.semantics(mergeDescendants = true) {
        if (primary) liveRegion = LiveRegionMode.Polite
    }) {
        Text(label, color = palette.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val measurer = rememberTextMeasurer()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val nominal = if (primary) 30.sp else 23.sp
            val style = LocalTextStyle.current.copy(fontSize = nominal, fontWeight = FontWeight.Bold,
                fontFamily = if (palette.square) FontFamily.Monospace else FontFamily.Default)
            val text = value.toString()
            val natural = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width
            val available = with(LocalDensity.current) { maxWidth.toPx() }
            val fit = ((available - 2f).coerceAtLeast(0f) / natural.coerceAtLeast(1)).coerceIn(.01f, 1f)
            // Scores are information, never abbreviated by an ellipsis. The surrounding
            // composition gives long values a full row; fit only handles the final pixels.
            Text(text, modifier = Modifier.fillMaxWidth(), color = if (primary) palette.statValue else palette.text,
                style = style.copy(fontSize = nominal * fit), maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun ModernNext(palette: Palette, colors: List<BallColor>, modifier: Modifier) {
    val description = stringResource(R.string.next_label, colors.map { colorLabel(it) }.joinToString(", "))
    val sprites = rememberSprites(palette)
    val measurer = rememberTextMeasurer()
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.hud_next), color = palette.textMuted, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Canvas(Modifier.fillMaxWidth().size(28.dp)) {
            val step = minOf(28.dp.toPx(), size.width / colors.size.coerceAtLeast(1))
            colors.forEachIndexed { index, ball ->
                drawBall(palette, sprites, ball, Offset(step * (index + 0.5f), size.height / 2),
                    minOf(step, size.height) * 0.36f, measurer = measurer)
            }
        }
    }
}

/** All everyday commands are visible; only a genuinely short rail needs to scroll. */
@Composable
private fun ModernDock(palette: Palette, canUndo: Boolean, soundEnabled: Boolean,
    onUndo: () -> Unit, onNewGame: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit,
    onHelp: () -> Unit, onToggleSound: () -> Unit, extras: GameExtras, sidePanel: Boolean = false) {
    val primary = listOf(
        ModernCommand(stringResource(R.string.btn_newGame), AppIcons.Refresh, onNewGame, emphasis = true),
        ModernCommand(stringResource(R.string.btn_undo), AppIcons.Undo, onUndo, enabled = canUndo),
        if (extras.gameOver) ModernCommand(
            stringResource(R.string.gameover_result) + extras.resultBadge.let { if (it.isEmpty()) "" else " $it" },
            AppIcons.Flag, extras.onShowResult)
        else ModernCommand(stringResource(R.string.btn_hint, extras.hintsLeft.toString()),
            AppIcons.Lightbulb, extras.onHint, enabled = extras.hintsLeft > 0,
            displayLabel = stringResource(R.string.btn_hint_short, extras.hintsLeft.toString()))
    )
    val secondary = listOf(
        ModernCommand(stringResource(R.string.btn_settings), AppIcons.Settings, onSettings),
        ModernCommand(stringResource(R.string.btn_stats), AppIcons.BarChart, onStats),
        ModernCommand(stringResource(if (soundEnabled) R.string.btn_mute else R.string.btn_unmute),
            if (soundEnabled) AppIcons.VolumeUp else AppIcons.VolumeOff, onToggleSound,
            displayLabel = stringResource(if (soundEnabled) R.string.btn_sound_on else R.string.btn_sound_off)),
        ModernCommand(stringResource(R.string.btn_help), AppIcons.Help, onHelp)
    )
    val rows = if (sidePanel) (primary + secondary).chunked(2)
        else listOf(primary) + secondary.chunked(2)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { commands ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                commands.forEach { command ->
                    ModernAction(palette, command.label, command.onClick, command.icon,
                        Modifier.weight(1f), command.enabled, command.emphasis, stacked = true, displayLabel = command.displayLabel)
                }
                if (commands.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class ModernCommand(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val emphasis: Boolean = false,
    val displayLabel: String = label
)

@Composable
private fun ModernAction(palette: Palette, label: String, onClick: () -> Unit, icon: ImageVector?,
    modifier: Modifier = Modifier, enabled: Boolean = true, emphasis: Boolean = false,
    stacked: Boolean = false, displayLabel: String = label) {
    val shape = RoundedCornerShape(12.dp)
    TextButton(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
        shape = shape,
        border = BorderStroke(1.dp, if (emphasis && enabled) palette.accent else palette.accent.copy(alpha = .25f)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (emphasis) palette.accent else palette.panel,
            contentColor = if (emphasis) palette.onAccent else palette.text,
            disabledContentColor = palette.textMuted,
            disabledContainerColor = palette.chip)) {
        if (stacked) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = if (emphasis && enabled) palette.onAccent else if (enabled) palette.accent else palette.textMuted)
                Text(displayLabel, modifier = Modifier.weight(1f), fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Start,
                    minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
