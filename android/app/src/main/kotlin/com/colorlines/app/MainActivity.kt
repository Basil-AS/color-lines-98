package com.colorlines.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colorlines.engine.BallColor
import com.colorlines.engine.GameEngine
import com.colorlines.engine.GameRecord
import com.colorlines.engine.GameStats
import com.colorlines.engine.Point

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; the UI keeps clear of them with WindowInsets.safeDrawing.
        val transparent = AndroidColor.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent)
        )
        soundManager = SoundManager(this)
        val storage = GameStorage(this)
        setContent { BasilLinesApp(storage, soundManager) }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

private fun colorScheme(theme: AppTheme, p: Palette): ColorScheme =
    if (theme == AppTheme.MODERN) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            surface = p.panel, onSurface = p.text,
            surfaceVariant = p.chip, onSurfaceVariant = p.textMuted,
            background = p.background, onBackground = p.text,
            outline = Color(0xFF6B6B78)
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            surface = p.panel, onSurface = p.text,
            surfaceVariant = Color(0xFFDFDFDF), onSurfaceVariant = p.textMuted,
            background = p.background, onBackground = p.text,
            outline = Color(0xFF404040)
        )
    }

@Composable
fun BasilLinesApp(storage: GameStorage, soundManager: SoundManager) {
    val haptic = LocalHapticFeedback.current

    var theme by remember { mutableStateOf(storage.theme) }
    val engine = remember { storage.loadGame() ?: GameEngine() }
    // The engine is a plain object Compose cannot observe: bump `version` after every change.
    var version by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(storage.history) }
    var bestScore by remember { mutableIntStateOf(maxOf(storage.bestScore, GameStats.summarize(storage.history).bestScore)) }
    var bestAtGameStart by remember { mutableIntStateOf(bestScore) }
    var soundEnabled by remember { mutableStateOf(soundManager.isEnabled) }
    var showHelp by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    fun commit() {
        version++
        storage.saveGame(engine)
    }

    fun record(completed: Boolean) {
        history = GameStats.addRecord(history, GameStats.recordFrom(engine, completed, System.currentTimeMillis()))
        storage.history = history
    }

    fun onCellTap(point: Point) {
        if (engine.isGameOver) return
        if (engine.board[point] != null) {
            if (engine.selectedPoint == point) {
                engine.unselect()
            } else {
                engine.selectCell(point)
                soundManager.playSelect()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            commit()
            return
        }
        val from = engine.selectedPoint ?: return
        val result = engine.moveBall(from, point)
        if (!result.success) {
            soundManager.playClick()
            return
        }
        engine.unselect()
        soundManager.playJump()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (result.clearedPoints.isNotEmpty()) soundManager.playEat(result.pointsEarned)
        if (engine.score > bestScore) {
            bestScore = engine.score
            storage.bestScore = bestScore
        }
        if (result.isGameOver) {
            soundManager.playLose()
            record(completed = true)
        }
        commit()
    }

    fun onNewGame() {
        // Abandoning a game in progress still counts towards the history.
        if (!engine.isGameOver && engine.moves > 0) record(completed = false)
        engine.startNewGame()
        bestAtGameStart = bestScore
        soundManager.playClick()
        commit()
    }

    fun onUndo() {
        if (engine.undo()) {
            soundManager.playClick()
            commit()
        }
    }

    // Everything below reads plain values captured for this version, never the live engine.
    val snapshot = remember(version) {
        BoardSnapshot(
            cells = List(BOARD_SIZE * BOARD_SIZE) { engine.board[it % BOARD_SIZE, it / BOARD_SIZE] },
            selected = engine.selectedPoint,
            reachable = engine.getReachableCellsForSelected()
        )
    }
    val score = remember(version) { engine.score }
    val nextColors = remember(version) { engine.nextColors }
    val canUndo = remember(version) { engine.canUndo }
    val gameOver = remember(version) { engine.isGameOver }

    val palette = paletteFor(theme)
    MaterialTheme(colorScheme = colorScheme(theme, palette)) {
        Box(Modifier.fillMaxSize().background(palette.background)) {
            GameScreen(
                palette = palette,
                theme = theme,
                score = score,
                best = maxOf(score, bestScore),
                nextColors = nextColors,
                canUndo = canUndo,
                soundEnabled = soundEnabled,
                snapshot = snapshot,
                onCellTap = ::onCellTap,
                onUndo = ::onUndo,
                onNewGame = ::onNewGame,
                onStats = { showStats = true },
                onHelp = { showHelp = true },
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    soundManager.isEnabled = soundEnabled
                },
                onTheme = {
                    theme = it
                    storage.theme = it
                }
            )
        }

        if (gameOver) {
            GameOverDialog(
                score = score,
                best = maxOf(score, bestScore),
                newRecord = GameStats.isNewRecord(score, bestAtGameStart),
                onPlayAgain = ::onNewGame
            )
        } else {
            if (showHelp) HelpDialog(onClose = { showHelp = false })
            if (showStats) {
                StatsDialog(
                    history = history,
                    onClear = {
                        history = emptyList()
                        storage.history = history
                    },
                    onClose = { showStats = false }
                )
            }
        }
    }
}

@Composable
private fun GameScreen(
    palette: Palette,
    theme: AppTheme,
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
    onHelp: () -> Unit,
    onToggleSound: () -> Unit,
    onTheme: (AppTheme) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val margin = 12.dp
        if (maxWidth > maxHeight) {
            // Landscape: board on the left, panel on the right.
            val boardSize = minOf(maxHeight - margin * 2, maxWidth * 0.58f)
            Row(
                Modifier.fillMaxSize().padding(margin),
                horizontalArrangement = Arrangement.spacedBy(margin),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardView(snapshot, palette, onCellTap, Modifier.size(boardSize))
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(margin, Alignment.CenterVertically)
                ) {
                    HeaderCard(palette, theme, score, best, nextColors, canUndo, soundEnabled, onUndo, onNewGame, onStats, onHelp, onToggleSound)
                    ThemeSwitcher(theme, onTheme)
                }
            }
        } else {
            val boardSize = minOf(maxWidth - margin * 2, 560.dp, (maxHeight - 250.dp).coerceAtLeast(240.dp))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(margin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(margin, Alignment.CenterVertically)
            ) {
                Box(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    HeaderCard(palette, theme, score, best, nextColors, canUndo, soundEnabled, onUndo, onNewGame, onStats, onHelp, onToggleSound)
                }
                BoardView(snapshot, palette, onCellTap, Modifier.size(boardSize))
                ThemeSwitcher(theme, onTheme)
            }
        }
    }
}

@Composable
private fun HeaderCard(
    palette: Palette,
    theme: AppTheme,
    score: Int,
    best: Int,
    nextColors: List<BallColor>,
    canUndo: Boolean,
    soundEnabled: Boolean,
    onUndo: () -> Unit,
    onNewGame: () -> Unit,
    onStats: () -> Unit,
    onHelp: () -> Unit,
    onToggleSound: () -> Unit
) {
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    val mono = if (theme == AppTheme.CLASSIC_98) FontFamily.Monospace else FontFamily.Default
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_name),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.text,
                    fontFamily = mono,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(stringResource(R.string.hud_score), score, palette, live = true)
                    StatBox(stringResource(R.string.hud_best), best, palette)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NextPreview(palette, nextColors)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActionButton(AppIcons.Undo, stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
                    ActionButton(AppIcons.Refresh, stringResource(R.string.btn_new_game), onNewGame)
                    ActionButton(AppIcons.BarChart, stringResource(R.string.btn_stats), onStats)
                    ActionButton(
                        if (soundEnabled) AppIcons.VolumeUp else AppIcons.VolumeOff,
                        stringResource(if (soundEnabled) R.string.btn_sound_off else R.string.btn_sound_on),
                        onToggleSound
                    )
                    ActionButton(AppIcons.Help, stringResource(R.string.btn_help), onHelp)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NextPreview(palette: Palette, colors: List<BallColor>) {
    val names = colors.map { colorLabel(it) }.joinToString(", ")
    val description = stringResource(R.string.next_label, names)
    Row(
        Modifier
            .clip(RoundedCornerShape(if (palette.square) 0.dp else 8.dp))
            .background(palette.chip)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.hud_next),
            fontSize = 12.sp,
            color = if (palette.square) Color.White else palette.textMuted
        )
        colors.forEach { color ->
            Box(Modifier.size(16.dp).clip(CircleShape).background(ballColor(color)))
        }
    }
}

@Composable
private fun StatBox(label: String, value: Int, palette: Palette, live: Boolean = false) {
    val live = if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
    Column(
        Modifier
            .clip(RoundedCornerShape(if (palette.square) 0.dp else 8.dp))
            .background(if (palette.square) Color.Black else palette.chip)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(live),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = if (palette.square) Color(0xFFB0B0B0) else palette.textMuted)
        Text(
            value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = palette.statValue,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ThemeSwitcher(theme: AppTheme, onTheme: (AppTheme) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = theme == AppTheme.MODERN,
            onClick = { onTheme(AppTheme.MODERN) },
            label = { Text(stringResource(R.string.theme_modern)) }
        )
        FilterChip(
            selected = theme == AppTheme.CLASSIC_98,
            onClick = { onTheme(AppTheme.CLASSIC_98) },
            label = { Text(stringResource(R.string.theme_classic98)) }
        )
    }
}
