package io.github.basil_as.basillines

import android.graphics.Color as AndroidColor
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.DosButton
import io.github.basil_as.basillines.engine.DosEffect
import io.github.basil_as.basillines.engine.DosState
import io.github.basil_as.basillines.engine.EffectKind
import io.github.basil_as.basillines.engine.Hall
import io.github.basil_as.basillines.engine.HallEntry
import io.github.basil_as.basillines.engine.GameEngine
import io.github.basil_as.basillines.engine.GameStats
import io.github.basil_as.basillines.engine.Hint
import io.github.basil_as.basillines.engine.Hinter
import io.github.basil_as.basillines.engine.Levels
import io.github.basil_as.basillines.engine.ModeId
import io.github.basil_as.basillines.engine.Modes
import io.github.basil_as.basillines.engine.Point
import io.github.basil_as.basillines.engine.SoundKind
import io.github.basil_as.basillines.engine.Progress
import io.github.basil_as.basillines.engine.ProgressTracker

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(GameStorage.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; the UI keeps clear of them with WindowInsets.safeDrawing.
        applySystemBars(dark = true)
        soundManager = SoundManager(this)
        val storage = GameStorage(this)
        setContent {
            ColorLinesApp(storage, soundManager, onSystemBars = ::applySystemBars)
        }
    }

    /** Light icons on dark backgrounds, dark icons on light ones. */
    private fun applySystemBars(dark: Boolean) {
        val transparent = AndroidColor.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

private fun colorScheme(p: Palette): ColorScheme {
    val outline = if (p.dark) Color(0xFF6B6B78) else Color(0xFF404040)
    return if (p.dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, surface = p.panel, onSurface = p.text,
            surfaceVariant = p.chip, onSurfaceVariant = p.textMuted, background = p.background,
            onBackground = p.text, outline = outline, error = p.danger
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, surface = p.panel, onSurface = p.text,
            surfaceVariant = if (p.cellStyle == CellStyle.ROUNDED) p.chip else Color(0xFFDFDFDF),
            onSurfaceVariant = p.textMuted, background = p.background, onBackground = p.text,
            outline = outline, error = p.danger
        )
    }
}

private data class LastResult(val xp: Int = 0, val levelUp: Int? = null, val unlocked: List<String> = emptyList())

private enum class Dialog { NONE, HELP, STATS, SETTINGS, NEW_GAME }

private const val HINTS_PER_GAME = 3

private fun gameOverNow(engine: GameEngine) = engine.isGameOver

/** What the HUD needs beyond the score: the mode (and its clock) and the hint button. */
private data class GameExtras(
    val modeLabel: String = "",
    val clock: String? = null,
    val hintsLeft: Int = 0,
    val onHint: () -> Unit = {},
    val onChangeMode: () -> Unit = {}
)

private fun dayKey(): String {
    val c = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(java.util.Locale.ROOT, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
}

@Composable
fun ColorLinesApp(storage: GameStorage, soundManager: SoundManager, onSystemBars: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    var theme by remember { mutableStateOf(storage.theme) }
    var engine by remember { mutableStateOf(storage.loadGame() ?: Modes.createEngine(storage.mode, dayKey())) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var hintsLeft by remember { mutableIntStateOf(HINTS_PER_GAME) }
    var pickedMode by remember { mutableStateOf(storage.mode) }
    // The engine is a plain object Compose cannot observe: bump `version` after every change.
    var version by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(storage.history) }
    var progress by remember { mutableStateOf<Progress>(storage.progress) }
    var bestScore by remember { mutableIntStateOf(maxOf(storage.bestScore, GameStats.summarize(storage.history).bestScore)) }
    var bestAtGameStart by remember { mutableIntStateOf(bestScore) }
    var soundEnabled by remember { mutableStateOf(soundManager.isEnabled) }
    var spawnStored by remember { mutableStateOf(storage.spawnPreview) }
    var hall by remember { mutableStateOf(storage.hall) }
    var playerName by remember { mutableStateOf(storage.playerName) }
    var showNext by remember { mutableStateOf(storage.showNext) }
    var dosWindow by remember { mutableStateOf(DosWindow.NONE) }
    var effects by remember { mutableStateOf<List<DosEffect>>(emptyList()) }
    var coronationStart by remember { mutableStateOf<Long?>(if (engine.score > Hall.kingOf(storage.hall).score) Long.MIN_VALUE / 2 else null) }
    var dialog by remember { mutableStateOf(Dialog.NONE) }
    var statsNow by remember { mutableLongStateOf(0L) }
    var lastResult by remember { mutableStateOf(LastResult()) }
    var lastActionAt by remember { mutableLongStateOf(0L) }

    val palette = paletteFor(theme)
    val spawnPreview = spawnStored ?: (theme != AppTheme.COLORLINES_92)
    val defaultName = stringResource(R.string.dos_defaultName)
    soundManager.profile = when (theme) {
        AppTheme.LINES_98 -> SoundManager.Profile.SAMPLED
        AppTheme.COLORLINES_92 -> SoundManager.Profile.PC_SPEAKER
        else -> SoundManager.Profile.MODERN
    }
    DisposableEffect(theme) {
        onSystemBars(palette.dark || palette.background == Color(0xFF008080) || palette.background == Color.Black)
        onDispose { }
    }

    fun commit() {
        version++
        storage.saveGame(engine)
    }

    // Counts active time between actions; long pauses (a forgotten app) are capped.
    fun trackTime() {
        val now = SystemClock.elapsedRealtime()
        // A timed mode has its own ticker; counting the gaps between taps would count the time twice.
        if (lastActionAt > 0 && engine.mode.timeLimitMs == null) engine.addPlayTime(minOf(now - lastActionAt, 60_000L))
        lastActionAt = now
    }

    fun record(completed: Boolean) {
        val rec = GameStats.recordFrom(engine, completed, System.currentTimeMillis())
        history = GameStats.addRecord(history, rec)
        storage.history = history
        val before = progress
        val applied = ProgressTracker.applyGame(before, rec)
        progress = applied.progress
        storage.progress = applied.progress
        val levelBefore = Levels.info(ProgressTracker.xpOf(before)).level
        val levelAfter = Levels.info(ProgressTracker.xpOf(applied.progress)).level
        if (completed) {
            hall = Hall.insert(hall, HallEntry(playerName.ifBlank { defaultName }, rec.score, rec.endedAt))
            storage.hall = hall
        }
        lastResult = LastResult(
            xp = ProgressTracker.xpOf(applied.progress) - ProgressTracker.xpOf(before),
            levelUp = if (levelAfter > levelBefore) levelAfter else null,
            unlocked = applied.unlocked
        )
    }

    /** The game just ended (board full or time up): sound, history, rewards. */
    fun finishGame() {
        soundManager.play(if (GameStats.isNewRecord(engine.score, bestAtGameStart)) SoundKind.RECORD else SoundKind.LOSE)
        record(completed = true)
        // The progress rewards follow the result after a short pause so the sounds do not blur together.
        val followUp = when {
            lastResult.levelUp != null -> SoundKind.LEVEL_UP
            lastResult.unlocked.isNotEmpty() -> SoundKind.ACHIEVEMENT
            else -> null
        }
        if (followUp != null) handler.postDelayed({ soundManager.play(followUp) }, 1100)
    }

    fun onCellTap(point: Point) {
        if (engine.isGameOver) return
        hint = null
        trackTime()
        if (engine.board[point] != null) {
            if (engine.selectedPoint == point) {
                engine.unselect()
            } else {
                engine.selectCell(point)
                soundManager.play(SoundKind.SELECT)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            commit()
            return
        }
        val from = engine.selectedPoint ?: return
        val before = engine.board.copy()
        val result = engine.moveBall(from, point)
        if (!result.success) {
            soundManager.play(SoundKind.BLOCKED)
            return
        }
        engine.unselect()
        soundManager.play(SoundKind.JUMP)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Remember what appeared and what burst, so the 1992 screen can animate it.
        val start = clockMs()
        val movedColor = before[from]
        val fx = mutableListOf<DosEffect>()
        for (s in result.spawnedBalls) fx += DosEffect(EffectKind.SPAWN, s.point.x, s.point.y, s.color, start)
        for (p in result.clearedPoints) {
            val color = if (p == point) movedColor else before[p] ?: result.spawnedBalls.firstOrNull { it.point == p }?.color
            if (color != null) fx += DosEffect(EffectKind.BURST, p.x, p.y, color, start)
        }
        effects = fx
        if (engine.score > Hall.kingOf(hall).score && coronationStart == null) {
            coronationStart = start
            if (!result.isGameOver) handler.postDelayed({ soundManager.play(SoundKind.CROWN) }, 450)
        }
        if (result.clearedPoints.isNotEmpty()) soundManager.play(SoundKind.EAT, result.pointsEarned)
        if (engine.score > bestScore) {
            bestScore = engine.score
            storage.bestScore = bestScore
        }
        if (result.isGameOver) finishGame()
        commit()
    }

    /** Every "new game" button opens the dialog: it names the mode and warns before a game is thrown away. */
    fun onNewGame() {
        pickedMode = engine.mode
        dialog = Dialog.NEW_GAME
    }

    fun startGame(mode: ModeId) {
        // Abandoning a game in progress still counts towards the history.
        if (!engine.isGameOver && engine.moves > 0) record(completed = false)
        engine = Modes.createEngine(mode, dayKey())
        storage.mode = mode
        hint = null
        hintsLeft = HINTS_PER_GAME
        effects = emptyList()
        coronationStart = null
        lastActionAt = 0L
        bestAtGameStart = bestScore
        dialog = Dialog.NONE
        soundManager.play(SoundKind.START)
        commit()
    }

    fun onHint() {
        if (engine.isGameOver || hintsLeft <= 0) return
        val found = Hinter.find(engine.board) ?: return
        engine.selectCell(found.from)
        hint = found
        hintsLeft -= 1
        soundManager.play(SoundKind.SELECT)
        commit()
    }

    // The clock of a timed mode runs while the game is on screen and no dialog is open.
    val lifecycleOwner = LocalContext.current as? androidx.lifecycle.LifecycleOwner
    LaunchedEffect(engine, dialog) {
        while (engine.mode.timeLimitMs != null && dialog == Dialog.NONE) {
            kotlinx.coroutines.delay(250)
            val visible = lifecycleOwner?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ?: true
            if (!visible || engine.isGameOver) continue
            engine.addPlayTime(250)
            if ((Modes.remainingMs(engine) ?: 1L) <= 0L) {
                engine.endGame()
                finishGame()
            }
            commit()
        }
    }

    fun onUndo() {
        hint = null
        trackTime()
        if (engine.undo()) {
            soundManager.play(SoundKind.CLICK)
            commit()
        }
    }

    // Everything below reads plain values captured for this version, never the live engine.
    val snapshot = remember(version, spawnPreview, hint) {
        BoardSnapshot(
            cells = List(BOARD_SIZE * BOARD_SIZE) { engine.board[it % BOARD_SIZE, it / BOARD_SIZE] },
            selected = engine.selectedPoint,
            reachable = engine.getReachableCellsForSelected(),
            incoming = if (spawnPreview && !engine.isGameOver) {
                engine.nextSpawnPoints.zip(engine.nextColors).toMap()
            } else {
                emptyMap()
            },
            hintTarget = hint?.takeIf { engine.selectedPoint == it.from }?.to
        )
    }
    val extras = GameExtras(
        modeLabel = stringResource(
            when (engine.mode) {
                ModeId.CLASSIC -> R.string.mode_classic
                ModeId.EASY -> R.string.mode_easy
                ModeId.BLITZ -> R.string.mode_blitz
                ModeId.DAILY -> R.string.mode_daily
            }
        ),
        clock = remember(version) {
            Modes.remainingMs(engine)?.let { ms -> "%d:%02d".format(java.util.Locale.ROOT, ms / 60000, ms / 1000 % 60) }
        },
        hintsLeft = if (gameOverNow(engine)) 0 else hintsLeft,
        onHint = ::onHint,
        onChangeMode = ::onNewGame
    )
    val score = remember(version) { engine.score }
    val nextColors = remember(version) { engine.nextColors }
    val canUndo = remember(version) { engine.canUndo }
    val gameOver = remember(version) { engine.isGameOver }

    MaterialTheme(
        colorScheme = colorScheme(palette),
        // The retro looks have no rounded corners anywhere: buttons, cards and dialogs are square.
        shapes = if (palette.square) androidx.compose.material3.Shapes(
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        ) else MaterialTheme.shapes
    ) {
        Box(Modifier.fillMaxSize().background(palette.background)) {
            if (theme == AppTheme.COLORLINES_92) {
                DosGameScreen(
                    snapshot = snapshot,
                    score = score,
                    kingScore = Hall.kingOf(hall).score,
                    nextColors = nextColors,
                    showNext = showNext,
                    soundEnabled = soundEnabled,
                    effects = effects,
                    coronationStart = coronationStart,
                    // The dethroned king keeps his name and record; the crowned pretender is the player.
                    kingName = Hall.kingOf(hall).name,
                    pretenderName = if (coronationStart != null) playerName.ifBlank { defaultName } else stringResource(R.string.dos_pretender),
                    window = dosWindow,
                    hall = hall,
                    canUndo = canUndo,
                    onCellTap = ::onCellTap,
                    onButton = { button ->
                        when (button) {
                            DosButton.HELP -> dosWindow = if (dosWindow == DosWindow.HELP) DosWindow.NONE else DosWindow.HELP
                            DosButton.SOUND -> {
                                soundEnabled = !soundEnabled
                                soundManager.isEnabled = soundEnabled
                            }
                            DosButton.NEXT -> {
                                showNext = !showNext
                                storage.showNext = showNext
                            }
                            DosButton.RESTART -> onNewGame()
                        }
                    },
                    onCloseWindow = { dosWindow = DosWindow.NONE },
                    onUndo = ::onUndo,
                    onTopTen = { dosWindow = if (dosWindow == DosWindow.TOP_TEN) DosWindow.NONE else DosWindow.TOP_TEN },
                    onStats = {
                        statsNow = System.currentTimeMillis()
                        dialog = Dialog.STATS
                    },
                    onSettings = { dialog = Dialog.SETTINGS },
                    extras = extras
                )
            } else GameScreen(
                palette = palette,
                score = score,
                best = maxOf(score, bestScore),
                nextColors = nextColors,
                canUndo = canUndo,
                soundEnabled = soundEnabled,
                snapshot = snapshot,
                onCellTap = ::onCellTap,
                onUndo = ::onUndo,
                onNewGame = ::onNewGame,
                onStats = {
                    statsNow = System.currentTimeMillis()
                    dialog = Dialog.STATS
                },
                onSettings = { dialog = Dialog.SETTINGS },
                onHelp = { dialog = Dialog.HELP },
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    soundManager.isEnabled = soundEnabled
                },
                extras = extras
            )
        }

        if (gameOver && dialog != Dialog.NEW_GAME) {
            GameOverDialog(
                score = score,
                best = maxOf(score, bestScore),
                newRecord = GameStats.isNewRecord(score, bestAtGameStart),
                xpGained = lastResult.xp,
                levelUp = lastResult.levelUp,
                unlocked = lastResult.unlocked,
                onPlayAgain = ::onNewGame
            )
        } else {
            when (dialog) {
                Dialog.NEW_GAME -> NewGameDialog(
                    current = pickedMode,
                    onPick = { pickedMode = it },
                    inProgress = !engine.isGameOver && engine.moves > 0,
                    score = score,
                    moves = engine.moves,
                    onStart = { startGame(pickedMode) },
                    onKeep = { dialog = Dialog.NONE }
                )
                Dialog.HELP -> HelpDialog(onClose = { dialog = Dialog.NONE })
                Dialog.STATS -> StatsDialog(
                    history = history,
                    progress = progress,
                    now = statsNow,
                    onClear = {
                        history = emptyList()
                        storage.history = history
                        storage.progress = Progress()
                        progress = storage.progress
                    },
                    onClose = { dialog = Dialog.NONE }
                )
                Dialog.SETTINGS -> SettingsDialog(
                    theme = theme,
                    onTheme = {
                        theme = it
                        storage.theme = it
                    },
                    soundEnabled = soundEnabled,
                    onToggleSound = {
                        soundEnabled = !soundEnabled
                        soundManager.isEnabled = soundEnabled
                    },
                    spawnPreview = spawnPreview,
                    onTogglePreview = {
                        spawnStored = !spawnPreview
                        storage.spawnPreview = spawnStored
                    },
                    language = storage.language,
                    onLanguage = { picked ->
                        if (picked != storage.language) {
                            storage.language = picked
                            (context as? android.app.Activity)?.recreate()
                        }
                    },
                    playerName = playerName,
                    onPlayerName = {
                        playerName = it.take(Hall.NAME_LIMIT)
                        storage.playerName = playerName
                    },
                    onClose = { dialog = Dialog.NONE }
                )
                Dialog.NONE -> Unit
            }
        }
    }
}

@Composable
private fun GameScreen(
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
    extras: GameExtras = GameExtras()
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val margin = if (maxWidth > maxHeight) 12.dp else 6.dp
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
                    verticalArrangement = Arrangement.Center
                ) {
                    HeaderCard(palette, score, best, nextColors, canUndo, soundEnabled, onUndo, onNewGame, onStats, onSettings, onHelp, onToggleSound, extras = extras)
                }
            }
        } else {
            // Portrait: the board takes the full width; the HUD is one compact card above it.
            val boardSize = minOf(maxWidth - margin * 2, 720.dp, (maxHeight - 160.dp - margin * 3).coerceAtLeast(240.dp))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(margin),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(margin, Alignment.CenterVertically)
            ) {
                Box(Modifier.widthIn(max = boardSize).fillMaxWidth()) {
                    HeaderCard(palette, score, best, nextColors, canUndo, soundEnabled, onUndo, onNewGame, onStats, onSettings, onHelp, onToggleSound, compact = true, extras = extras)
                }
                BoardView(snapshot, palette, onCellTap, Modifier.size(boardSize))
            }
        }
    }
}

@Composable
private fun HeaderCard(
    palette: Palette,
    score: Int,
    best: Int,
    nextColors: List<BallColor>,
    canUndo: Boolean,
    soundEnabled: Boolean,
    onUndo: () -> Unit,
    onNewGame: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onToggleSound: () -> Unit,
    compact: Boolean = false,
    extras: GameExtras = GameExtras()
) {
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    val font = if (palette.cellStyle == CellStyle.ROUNDED) FontFamily.Default else FontFamily.Monospace
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (palette.windowTitleBar) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF000080), Color(0xFF1084D0))))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.app_name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (palette.windowTitleBar) {
            Lines98Panel(score, best, nextColors, canUndo, soundEnabled, onUndo, onNewGame, onStats, onSettings, onHelp, onToggleSound, extras)
            return@Card
        }
        Column(Modifier.padding(if (compact) 8.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
            ModeBar(palette, extras)
            if (compact) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NextPreview(palette, nextColors)
                    Spacer(Modifier.weight(1f))
                    StatBox(stringResource(R.string.hud_score), score, palette, live = true)
                    StatBox(stringResource(R.string.hud_best), best, palette)
                }
            } else Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = stringResource(R.string.app_name)
                Text(
                    if (palette.cellStyle == CellStyle.DOS) title.uppercase() else title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (palette.cellStyle == CellStyle.WIN98) palette.accent else palette.text,
                    fontFamily = font,
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
                if (!compact) NextPreview(palette, nextColors)
                Row(
                    Modifier.then(if (compact) Modifier.fillMaxWidth() else Modifier),
                    horizontalArrangement = if (compact) Arrangement.SpaceBetween else Arrangement.spacedBy(4.dp)
                ) {
                    ActionButton(AppIcons.Undo, stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
                    ActionButton(AppIcons.Lightbulb, stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0)
                    ActionButton(AppIcons.Refresh, stringResource(R.string.btn_newGame), onNewGame)
                    ActionButton(AppIcons.BarChart, stringResource(R.string.btn_stats), onStats)
                    ActionButton(
                        if (soundEnabled) AppIcons.VolumeUp else AppIcons.VolumeOff,
                        stringResource(if (soundEnabled) R.string.btn_mute else R.string.btn_unmute),
                        onToggleSound
                    )
                    ActionButton(AppIcons.Settings, stringResource(R.string.btn_settings), onSettings)
                    ActionButton(AppIcons.Help, stringResource(R.string.btn_help), onHelp)
                }
            }
        }
    }
}

/** The mode in play, and the clock in a timed one; tapping the mode opens the new-game dialog. */
@Composable
private fun ModeBar(palette: Palette, extras: GameExtras) {
    val clockText = extras.clock?.let { stringResource(R.string.mode_timeLeft, it) }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = listOfNotNull(extras.modeLabel, clockText).joinToString(", ") },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(extras.modeLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textMuted)
        if (extras.clock != null) {
            Text(
                extras.clock,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = palette.accent,
                            )
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NextPreview(palette: Palette, colors: List<BallColor>) {
    val names = colors.map { colorLabel(it) }.joinToString(", ")
    val description = stringResource(R.string.next_label, names)
    Row(
        Modifier
            .clip(RoundedCornerShape(if (palette.square) 0.dp else 8.dp))
            .background(palette.statBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.hud_next), fontSize = 12.sp, color = palette.statLabel)
        colors.forEach { color ->
            Box(Modifier.size(16.dp).clip(CircleShape).background(ballColor(color)))
        }
    }
}

@Composable
private fun StatBox(label: String, value: Int, palette: Palette, live: Boolean = false) {
    val liveModifier = if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier
    Column(
        Modifier
            .clip(RoundedCornerShape(if (palette.square) 0.dp else 8.dp))
            .background(palette.statBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(liveModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, color = palette.statLabel)
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
private fun DosGameScreen(
    snapshot: BoardSnapshot,
    score: Int,
    kingScore: Int,
    nextColors: List<BallColor>,
    showNext: Boolean,
    soundEnabled: Boolean,
    effects: List<DosEffect>,
    coronationStart: Long?,
    kingName: String,
    pretenderName: String,
    window: DosWindow,
    hall: List<HallEntry>,
    canUndo: Boolean,
    onCellTap: (Point) -> Unit,
    onButton: (DosButton) -> Unit,
    onCloseWindow: () -> Unit,
    onUndo: () -> Unit,
    onTopTen: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    extras: GameExtras = GameExtras()
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val margin = 12.dp
        val toolsHeight = 56.dp
        val width = minOf(maxWidth - margin * 2, (maxHeight - toolsHeight - margin) * 4f / 3f, 960.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(margin),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            DosScreen(
                state = DosState(
                    cells = snapshot.cells,
                    selected = snapshot.selected,
                    next = nextColors,
                    showNext = showNext,
                    score = score,
                    kingScore = kingScore,
                    soundOn = soundEnabled,
                    nowMs = 0,
                    effects = effects,
                    coronationStart = coronationStart
                ),
                kingName = kingName,
                pretenderName = pretenderName,
                window = window,
                hall = hall,
                boardCells = { modifier -> DosBoardCells(snapshot, onCellTap, modifier) },
                onButton = onButton,
                onCloseWindow = onCloseWindow,
                modifier = Modifier.width(width).aspectRatio(4f / 3f)
            )
            ModeBar(paletteFor(AppTheme.COLORLINES_92), extras)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(AppIcons.Undo, stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
                ActionButton(AppIcons.Lightbulb, stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0)
                ActionButton(AppIcons.Trophy, stringResource(R.string.dos_topTen), onTopTen)
                ActionButton(AppIcons.BarChart, stringResource(R.string.btn_stats), onStats)
                ActionButton(AppIcons.Settings, stringResource(R.string.btn_settings), onSettings)
            }
        }
    }
}

private val LED_DIGITS = listOf(
    R.drawable.led_0, R.drawable.led_1, R.drawable.led_2, R.drawable.led_3, R.drawable.led_4,
    R.drawable.led_5, R.drawable.led_6, R.drawable.led_7, R.drawable.led_8, R.drawable.led_9
)

/** A number drawn with the LED digit images of Lines 98 (zero padded to [digits]). */
@Composable
private fun LedNumber(value: Int, digits: Int = 5) {
    val text = maxOf(0, value).toString().padStart(digits, '0')
    Row(Modifier.height(30.dp).semantics { contentDescription = value.toString() }) {
        text.forEach { ch ->
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(LED_DIGITS[ch - '0']),
                contentDescription = null,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

/** Lines 98 for Windows: best | next | score on one black LED panel, and a text menu instead of icons. */
@Composable
private fun Lines98Panel(
    score: Int,
    best: Int,
    nextColors: List<BallColor>,
    canUndo: Boolean,
    soundEnabled: Boolean,
    onUndo: () -> Unit,
    onNewGame: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onToggleSound: () -> Unit,
    extras: GameExtras
) {
    val names = nextColors.map { colorLabel(it) }.joinToString(", ")
    val nextDescription = stringResource(R.string.next_label, names)
    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeBar(paletteFor(AppTheme.LINES_98), extras)
        Row(
            Modifier.fillMaxWidth().background(Color.Black).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.semantics { contentDescription = "" }) { LedNumber(best) }
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = nextDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                nextColors.forEach { color ->
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            when (spriteIndex(color)) {
                                0 -> R.drawable.ball_sprite_0
                                1 -> R.drawable.ball_sprite_1
                                2 -> R.drawable.ball_sprite_2
                                3 -> R.drawable.ball_sprite_3
                                4 -> R.drawable.ball_sprite_4
                                5 -> R.drawable.ball_sprite_5
                                else -> R.drawable.ball_sprite_6
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { LedNumber(score) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            MenuItem(stringResource(R.string.btn_newGame), onNewGame)
            MenuItem(stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
            MenuItem(stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0)
            MenuItem(stringResource(if (soundEnabled) R.string.btn_mute else R.string.btn_unmute), onToggleSound)
            MenuItem(stringResource(R.string.btn_stats), onStats)
            MenuItem(stringResource(R.string.btn_settings), onSettings)
            MenuItem(stringResource(R.string.btn_help), onHelp)
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 13.sp, color = if (enabled) Color.Black else Color(0xFF808080)) }
}
