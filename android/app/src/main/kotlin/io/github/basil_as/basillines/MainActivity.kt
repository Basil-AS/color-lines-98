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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import io.github.basil_as.basillines.engine.Goals
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

    private var stoppedAt = 0L

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        // The sound pool can go deaf while the app sleeps (the audio service restarts): rebuild it after a long absence.
        if (stoppedAt > 0 && ::soundManager.isInitialized) soundManager.onForeground(System.currentTimeMillis() - stoppedAt)
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

private data class LastResult(val xp: Int = 0, val levelUp: Int? = null, val unlocked: List<String> = emptyList(), val goalXp: Int = 0)

private enum class Dialog { NONE, HELP, STATS, SETTINGS, NEW_GAME, GOALS }

private const val HINTS_PER_GAME = 3

private fun gameOverNow(engine: GameEngine) = engine.isGameOver

/** What the HUD needs beyond the score: the mode (and its clock) and the hint button. */
private data class GameExtras(
    val mode: ModeId = ModeId.CLASSIC,
    val modeLabel: String = "",
    val clock: String? = null,
    val hintsLeft: Int = 0,
    val onHint: () -> Unit = {},
    val onChangeMode: () -> Unit = {},
    /** A finished game: the score panel offers "New game" and "Result" instead of a dialog over the board. */
    val gameOver: Boolean = false,
    val onShowResult: () -> Unit = {},
    val resultBadge: String = "",
    val goalsLabel: String = "",
    val onGoals: () -> Unit = {},
    /** The move to play out over the board, how lively to be, and whether the board is nearly full. */
    val fx: MoveFx? = null,
    val level: EffectsLevel = EffectsLevel.OFF,
    val danger: Boolean = false
)

private fun dayKey(): String {
    val c = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(java.util.Locale.ROOT, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
}

@Composable
fun ColorLinesApp(storage: GameStorage, soundManager: SoundManager, onSystemBars: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val haptics = remember { Haptics(context) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    var theme by remember { mutableStateOf(storage.theme) }
    var engine by remember { mutableStateOf(storage.loadGame() ?: Modes.createEngine(storage.mode, dayKey())) }
    var hint by remember { mutableStateOf<Hint?>(null) }
    var hintsLeft by remember { mutableIntStateOf(HINTS_PER_GAME) }
    var pickedMode by remember { mutableStateOf(storage.mode) }
    // The engine is a plain object Compose cannot observe: bump `version` after every change.
    var version by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(storage.history) }
    var ledger by remember { mutableStateOf(storage.ledger) }
    var goalsDone by remember { mutableStateOf(storage.loadGoalsDone(dayKey())) }
    var updateOffer by remember { mutableStateOf<io.github.basil_as.basillines.engine.UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val appVersion = remember { UpdateClient.versionName(context) }
    var dataMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<io.github.basil_as.basillines.engine.Backup?>(null) }
    var progress by remember { mutableStateOf<Progress>(storage.progress) }
    var bestScore by remember { mutableIntStateOf(maxOf(storage.bestScore, GameStats.summarize(storage.history).bestScore)) }
    var bestAtGameStart by remember { mutableIntStateOf(bestScore) }
    var soundEnabled by remember { mutableStateOf(soundManager.isEnabled) }
    var spawnStored by remember { mutableStateOf(storage.spawnPreview) }
    var hall by remember { mutableStateOf(storage.hall) }
    var playerName by remember { mutableStateOf(storage.playerName) }
    var showNext by remember { mutableStateOf(storage.showNext) }
    var effectsLevel by remember { mutableStateOf(storage.effects) }
    var vibration by remember { mutableStateOf(storage.vibration) }
    haptics.enabled = vibration
    var moveFx by remember { mutableStateOf<MoveFx?>(null) }
    var fxCounter by remember { mutableLongStateOf(0L) }
    var combo by remember { mutableIntStateOf(0) }
    var dangerWarned by remember { mutableStateOf(false) }
    var dosWindow by remember { mutableStateOf(DosWindow.NONE) }
    var effects by remember { mutableStateOf<List<DosEffect>>(emptyList()) }
    var coronationStart by remember { mutableStateOf<Long?>(if (engine.score > Hall.kingOf(storage.hall).score) Long.MIN_VALUE / 2 else null) }
    var dialog by remember { mutableStateOf(Dialog.NONE) }
    var showResult by remember { mutableStateOf(false) }
    val isOver = engine.isGameOver
    LaunchedEffect(isOver) { if (!isOver) showResult = false }
    var statsNow by remember { mutableLongStateOf(0L) }
    var lastResult by remember { mutableStateOf(LastResult()) }
    var lastActionAt by remember { mutableLongStateOf(0L) }

    val palette = paletteFor(theme)
    val spawnPreview = spawnStored ?: (theme != AppTheme.COLORLINES_92)
    // The captions of the 1992 screen stay in English, like the original, whatever the language of the app.
    val defaultName = "Player"
    soundManager.voice = theme.voice
    soundManager.profile = when (theme) {
        AppTheme.LINES_98, AppTheme.LINES_98_PLUS -> SoundManager.Profile.SAMPLED
        AppTheme.COLORLINES_92 -> SoundManager.Profile.PC_SPEAKER
        else -> SoundManager.Profile.MODERN
    }
    soundManager.prime()
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
        engine.noteAction(if (lastActionAt > 0) minOf(now - lastActionAt, 60_000L) else null, countTime = engine.mode.timeLimitMs == null)
        lastActionAt = now
    }

    fun record(completed: Boolean) {
        val rec = GameStats.recordFrom(engine, completed, System.currentTimeMillis())
        history = GameStats.addRecord(history, rec)
        storage.history = history
        ledger = io.github.basil_as.basillines.engine.Careers.addGame(ledger, rec)
        storage.ledger = ledger
        val before = progress
        val applied = ProgressTracker.applyGame(before, rec)
        // Daily goals: the experience of every newly completed goal, once.
        val goalDay = ProgressTracker.dayKey(rec.endedAt)
        val dayGoals = Goals.daily(history, goalDay)
        val doneNow = Goals.evaluate(dayGoals, history.filter { ProgressTracker.dayKey(it.endedAt) == goalDay }).filter { it.done }.map { it.goal.id }
        val alreadyDone = storage.loadGoalsDone(goalDay)
        val goalBonus = Goals.bonus(alreadyDone, doneNow, dayGoals.size)
        var nextProgress = applied.progress
        val newlyUnlocked = applied.unlocked.toMutableList()
        if (goalBonus.newlyDone.isNotEmpty()) {
            nextProgress = ProgressTracker.applyGoalBonus(nextProgress, goalDay, goalBonus)
            val extra = ProgressTracker.awardAchievements(nextProgress, rec)
            nextProgress = extra.progress
            newlyUnlocked += extra.unlocked
            storage.saveGoalsDone(goalDay, doneNow)
            if (goalDay == dayKey()) goalsDone = doneNow
        }
        progress = nextProgress
        storage.progress = nextProgress
        val levelBefore = Levels.info(ProgressTracker.xpOf(before)).level
        val levelAfter = Levels.info(ProgressTracker.xpOf(nextProgress)).level
        if (completed) {
            hall = Hall.insert(hall, HallEntry(playerName.ifBlank { defaultName }, rec.score, rec.endedAt))
            storage.hall = hall
        }
        lastResult = LastResult(
            xp = ProgressTracker.xpOf(nextProgress) - ProgressTracker.xpOf(before),
            goalXp = goalBonus.xp,
            levelUp = if (levelAfter > levelBefore) levelAfter else null,
            unlocked = newlyUnlocked
        )
    }

    /** The game just ended (board full or time up): sound, history, rewards. */
    fun finishGame() {
        val newBest = GameStats.isNewRecord(engine.score, bestAtGameStart)
        soundManager.play(if (newBest) SoundKind.RECORD else SoundKind.LOSE)
        haptics.play(if (newBest) HapticKind.RECORD else HapticKind.GAME_OVER)
        if (newBest) handler.postDelayed({ moveFx = MoveFx(++fxCounter, emptyList(), BallColor.RED, emptyList(), emptyList(), 0, 0, celebrate = true) }, 300)
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
                haptics.play(HapticKind.SELECT)
            }
            commit()
            return
        }
        val from = engine.selectedPoint ?: return
        val before = engine.board.copy()
        val result = engine.moveBall(from, point)
        if (!result.success) {
            soundManager.play(SoundKind.BLOCKED)
            haptics.play(HapticKind.BLOCKED)
            return
        }
        engine.unselect()
        soundManager.play(SoundKind.JUMP)

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
        // The look with a living board (not the 1992 picture) plays the move out; the sounds follow its timing.
        val animated = effectsLevel != EffectsLevel.OFF && theme != AppTheme.COLORLINES_92
        val free = engine.board.getEmptyCells().size
        val path = result.path ?: listOf(from, point)
        val lag = if (animated) travelMs(path.size - 1).toLong() else 0L
        fun after(ms: Long, block: () -> Unit) { if (ms > 0) handler.postDelayed(block, ms) else block() }
        val cleared = result.clearedPoints.isNotEmpty()
        combo = if (cleared) combo + 1 else 0
        val comboNow = combo
        if (animated) {
            moveFx = MoveFx(
                ++fxCounter, path, movedColor ?: BallColor.RED,
                result.spawnedBalls.map { it.point to it.color },
                fx.filter { it.kind == EffectKind.BURST }.map { Point(it.x, it.y) to it.color },
                result.pointsEarned, comboNow
            )
        }
        haptics.play(if (cleared) (if (result.pointsEarned >= 28) HapticKind.BIG_CLEAR else HapticKind.CLEAR) else HapticKind.MOVE)
        if (engine.score > Hall.kingOf(hall).score && coronationStart == null) {
            coronationStart = start
            if (!result.isGameOver) after(450 + lag) { soundManager.play(SoundKind.CROWN) }
        }
        if (cleared) {
            // Clearing a line is a free turn, so chains are natural: each link sounds higher.
            after(lag) { soundManager.play(SoundKind.EAT, result.pointsEarned) }
            if (comboNow >= 2) after(lag + 180) { soundManager.play(SoundKind.COMBO, comboNow); haptics.play(HapticKind.COMBO) }
        }
        if (result.spawnedBalls.isNotEmpty()) after(lag + 60) { soundManager.play(SoundKind.POP) }
        // A nearly full board: one warning each time it gets that tight.
        if (!result.isGameOver && free <= 8 && !dangerWarned) {
            dangerWarned = true
            after(lag + 350) { soundManager.play(SoundKind.DANGER); haptics.play(HapticKind.DANGER) }
        } else if (free > 12) dangerWarned = false
        if (engine.score > bestScore) {
            bestScore = engine.score
            storage.bestScore = bestScore
        }
        if (result.isGameOver) finishGame()
        commit()
    }

    /** The dialog names the mode (and lets it change) and warns before a game in progress is thrown away. */
    fun openNewGameDialog() {
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
        moveFx = null
        combo = 0
        dangerWarned = false
        coronationStart = null
        // The first decision of a game is timed from the moment the board appears.
        lastActionAt = SystemClock.elapsedRealtime()
        bestAtGameStart = bestScore
        dialog = Dialog.NONE
        showResult = false
        soundManager.play(SoundKind.START)
        commit()
    }

    /** After a finished game there is nothing to warn about: play the same mode again straight away. */
    fun onNewGame() {
        if (engine.isGameOver) startGame(engine.mode) else openNewGameDialog()
    }

    fun onHint() {
        if (engine.isGameOver || hintsLeft <= 0) return
        val found = Hinter.find(engine.board) ?: return
        engine.selectCell(found.from)
        engine.noteHint()
        hint = found
        hintsLeft -= 1
        soundManager.play(SoundKind.HINT)
        haptics.play(HapticKind.HINT)
        commit()
    }

    // The clock of a timed mode runs while the game is on screen and no dialog is open.
    val lifecycleOwner = LocalContext.current as? androidx.lifecycle.LifecycleOwner
    LaunchedEffect(engine, dialog, pendingImport) {
        while (engine.mode.timeLimitMs != null && dialog == Dialog.NONE && pendingImport == null) {
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
            haptics.play(HapticKind.UNDO)
            commit()
        }
    }

    // ---- updates: one HTTPS question to GitHub, once a day, never blocking ------------------------------
    val updateLatest = stringResource(R.string.update_latest)
    val updateFailed = stringResource(R.string.update_failed)
    val updateChecking = stringResource(R.string.update_checking)
    val signatureText = remember {
        val sha = UpdateClient.signatureSha256(context)
        sha
    }
    val signedOk = if (signatureText == null) null else stringResource(R.string.update_signedOk, io.github.basil_as.basillines.engine.Updates.shortFingerprint(signatureText))
    val signedOther = if (signatureText == null) null else stringResource(R.string.update_signedOther, io.github.basil_as.basillines.engine.Updates.shortFingerprint(signatureText))
    val signatureLine = when {
        signatureText == null -> stringResource(R.string.update_signedUnknown)
        io.github.basil_as.basillines.engine.Updates.isProjectKey(signatureText) -> signedOk!!
        else -> signedOther!!
    }
    var autoUpdate by remember { mutableStateOf(storage.autoUpdateCheck) }
    val checkScope = androidx.compose.runtime.rememberCoroutineScope()

    fun checkNow(manual: Boolean) {
        if (manual) updateStatus = updateChecking
        checkScope.launch {
            storage.lastUpdateCheck = System.currentTimeMillis()
            when (val r = UpdateClient.check(context)) {
                is UpdateClient.Result.Newer -> {
                    updateStatus = null
                    if (manual || storage.dismissedUpdate != r.info.version) updateOffer = r.info
                }
                UpdateClient.Result.UpToDate -> updateStatus = if (manual) updateLatest else null
                UpdateClient.Result.Failed -> updateStatus = if (manual) updateFailed else null
            }
        }
    }
    LaunchedEffect(Unit) {
        if (storage.autoUpdateCheck && System.currentTimeMillis() - storage.lastUpdateCheck > 24 * 60 * 60 * 1000L) checkNow(manual = false)
    }

    // ---- saving results to a file and loading them back -------------------------------------
    val bad = stringResource(R.string.data_error_notBackup)
    val errors = mapOf(
        io.github.basil_as.basillines.engine.ParsedBackup.Error.EMPTY to stringResource(R.string.data_error_empty),
        io.github.basil_as.basillines.engine.ParsedBackup.Error.TOO_BIG to stringResource(R.string.data_error_tooBig),
        io.github.basil_as.basillines.engine.ParsedBackup.Error.NOT_JSON to stringResource(R.string.data_error_notJson),
        io.github.basil_as.basillines.engine.ParsedBackup.Error.NOT_BACKUP to bad,
        io.github.basil_as.basillines.engine.ParsedBackup.Error.NEWER to stringResource(R.string.data_error_newer)
    )
    val mergedMsg = stringResource(R.string.data_done_merge)
    val replacedMsg = stringResource(R.string.data_done_replace)

    val exportFailed = stringResource(R.string.data_error_export)
    val exportDone = stringResource(R.string.data_done_export)

    fun writeTo(uri: android.net.Uri?, text: String) {
        if (uri == null) return
        // "wt" truncates: an old, longer file picked in the save dialog must not leave its tail behind the new JSON.
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        }.getOrDefault(false)
        dataMessage = if (ok) exportDone else exportFailed
    }

    val exportJson = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
        val theme = theme.name.lowercase().replace("_", "")
        writeTo(
            uri,
            io.github.basil_as.basillines.engine.Backups.encode(
                io.github.basil_as.basillines.engine.Backup(
                    System.currentTimeMillis(), "android", version, history, ledger, progress, hall,
                    io.github.basil_as.basillines.engine.BackupSettings(
                        theme, storage.language.name.lowercase(), playerName, soundEnabled, spawnStored, showNext, engine.mode,
                        effectsLevel.id, vibration
                    )
                )
            )
        )
    }
    val exportCsv = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        writeTo(uri, io.github.basil_as.basillines.engine.Backups.toCsv(history))
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val importPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // Reading and parsing a file from a cloud provider can be slow: do it off the main thread.
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val limit = io.github.basil_as.basillines.engine.Backups.MAX_CHARS + 1
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(64 * 1024)
                        while (out.size() < limit) { val n = input.read(buf); if (n < 0) break; out.write(buf, 0, n) }
                        out.toString(Charsets.UTF_8.name())
                    }
                }.getOrNull()?.removePrefix("\uFEFF") ?: ""
                val parsed = io.github.basil_as.basillines.engine.Backups.parse(text)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    when (parsed) {
                        is io.github.basil_as.basillines.engine.ParsedBackup.Ok -> { pendingImport = parsed.backup; dataMessage = null }
                        is io.github.basil_as.basillines.engine.ParsedBackup.Error -> dataMessage = errors.getValue(parsed)
                    }
                }
            }
        }
    }

    fun applyImport(b: io.github.basil_as.basillines.engine.Backup, replace: Boolean) {
        val backups = io.github.basil_as.basillines.engine.Backups
        val nextHistory = if (replace) b.history else backups.mergeHistories(history, b.history)
        val nextProgress = if (replace) b.progress else backups.mergeProgress(progress, b.progress, nextHistory)
        history = nextHistory; storage.history = nextHistory
        progress = nextProgress; storage.progress = nextProgress
        hall = if (replace) b.hall else backups.mergeHalls(hall, b.hall); storage.hall = hall
        ledger = if (replace) b.ledger else io.github.basil_as.basillines.engine.Careers.mergeWithHistory(ledger, b.ledger, nextHistory); storage.ledger = ledger
        bestScore = maxOf(bestScore, nextProgress.bestScore); storage.bestScore = bestScore
        if (replace) {
            val st = b.settings
            AppTheme.entries.firstOrNull { it.name.lowercase().replace("_", "") == st.theme }?.let { theme = it; storage.theme = it }
            st.playerName?.let { playerName = it; storage.playerName = it }
            st.soundEnabled?.let { soundEnabled = it; soundManager.isEnabled = it }
            st.spawnPreview?.let { spawnStored = it; storage.spawnPreview = it }
            st.showNext?.let { showNext = it; storage.showNext = it }
            EffectsLevel.parse(st.effects)?.let { effectsLevel = it; storage.effects = it }
            st.vibration?.let { vibration = it; storage.vibration = it }
            val lang = AppLanguage.entries.firstOrNull { it.name.lowercase() == st.language }
            if (lang != null && lang != storage.language) { storage.language = lang; (context as? android.app.Activity)?.recreate() }
        }
        dataMessage = if (replace) replacedMsg else mergedMsg
        pendingImport = null
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
    // Goals are fixed for the day from earlier results; progress includes the game in play.
    val today = remember(version) { dayKey() }
    val goalProgress = remember(version, history) {
        val goals = Goals.daily(history, today)
        val live = if (!engine.isGameOver && engine.moves > 0) listOf(GameStats.recordFrom(engine, false, System.currentTimeMillis())) else emptyList()
        Goals.evaluate(goals, (live + history).filter { ProgressTracker.dayKey(it.endedAt) == today })
    }
    val extras = GameExtras(
        mode = engine.mode,
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
        onChangeMode = ::openNewGameDialog,
        gameOver = gameOverNow(engine),
        onShowResult = { showResult = true },
        resultBadge = if (gameOverNow(engine) && (lastResult.xp > 0 || lastResult.levelUp != null || lastResult.unlocked.isNotEmpty())) "+${lastResult.xp}" else "",
        goalsLabel = stringResource(R.string.goals_button, goalProgress.count { it.done }.toString(), goalProgress.size.toString()),
        fx = moveFx,
        level = if (theme == AppTheme.COLORLINES_92) EffectsLevel.OFF else effectsLevel,
        danger = !gameOverNow(engine) && remember(version) { engine.board.getEmptyCells().size <= 10 },
        onGoals = { dialog = Dialog.GOALS }
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
                    pretenderName = if (coronationStart != null) playerName.ifBlank { defaultName } else "Pretender",
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

        updateOffer?.let { info ->
            UpdateDialog(
                version = info.version,
                current = appVersion,
                onDownload = { UpdateClient.download(context, info); updateOffer = null },
                onLater = { storage.dismissedUpdate = info.version; updateOffer = null }
            )
        }

        pendingImport?.let { b ->
            val times = b.history.map { it.endedAt }
            ImportDialog(
                games = maxOf(b.progress.totalGames, b.history.size),
                currentGames = maxOf(progress.totalGames, history.size),
                exportedAt = b.exportedAt,
                first = times.minOrNull(),
                last = times.maxOrNull(),
                onMerge = { applyImport(b, replace = false) },
                onReplace = { applyImport(b, replace = true) },
                onCancel = { pendingImport = null }
            )
        }

        // Shown only when asked for (the "Result" button); the game never covers the board by itself.
        if (gameOver && showResult && dialog == Dialog.NONE) {
            GameOverDialog(
                score = score,
                best = maxOf(score, bestScore),
                newRecord = GameStats.isNewRecord(score, bestAtGameStart),
                xpGained = lastResult.xp,
                levelUp = lastResult.levelUp,
                unlocked = lastResult.unlocked,
                onPlayAgain = { showResult = false; onNewGame() },
                onClose = { showResult = false },
                goalXp = lastResult.goalXp
            )
        }
        run {
            when (dialog) {
                Dialog.NEW_GAME -> NewGameDialog(
                    current = pickedMode,
                    playing = engine.mode,
                    modeStats = remember(history) {
                        ModeId.entries.associateWith { m -> history.count { it.mode == m } to (history.filter { it.mode == m }.maxOfOrNull { it.score } ?: 0) }
                    },
                    onPick = { pickedMode = it },
                    inProgress = !engine.isGameOver && engine.moves > 0,
                    score = score,
                    moves = engine.moves,
                    onStart = { startGame(pickedMode) },
                    onKeep = { dialog = Dialog.NONE }
                )
                Dialog.GOALS -> GoalsDialog(goalProgress, ProgressTracker.currentStreak(progress.goalDays, today), onClose = { dialog = Dialog.NONE })
                Dialog.HELP -> HelpDialog(onClose = { dialog = Dialog.NONE })
                Dialog.STATS -> StatsDialog(
                    history = history,
                    progress = progress,
                    ledger = ledger,
                    now = statsNow,
                    dataMessage = dataMessage,
                    onExportJson = { exportJson.launch("color-lines-backup-${dayKey()}.json") },
                    onExportCsv = { exportCsv.launch("color-lines-games-${dayKey()}.csv") },
                    onImport = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onClear = {
                        history = emptyList()
                        storage.history = history
                        ledger = emptyMap()
                        storage.ledger = ledger
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
                    effects = effectsLevel,
                    onEffects = { effectsLevel = it; storage.effects = it },
                    vibrationSupported = haptics.supported,
                    vibration = vibration,
                    onVibration = { vibration = it; storage.vibration = it; haptics.enabled = it; if (it) haptics.play(HapticKind.SELECT) },
                    versionName = appVersion,
                    signature = signatureLine,
                    autoUpdate = autoUpdate,
                    onAutoUpdate = { autoUpdate = it; storage.autoUpdateCheck = it },
                    updateStatus = updateStatus,
                    onCheckUpdate = { checkNow(manual = true) },
                    onClose = { dialog = Dialog.NONE; updateStatus = null }
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
            // Landscape: the board takes the whole height, the score and the next balls stand on its left and the actions
            // on its right, so neither side is a tall stack of big controls.
            val boardSize = minOf(maxHeight - margin * 2, maxWidth * 0.6f)
            Row(
                Modifier.fillMaxSize().padding(margin),
                horizontalArrangement = Arrangement.spacedBy(margin),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    InfoRail(palette, score, best, nextColors, extras)
                }
                BoardView(snapshot, palette, onCellTap, Modifier.size(boardSize), fx = extras.fx, level = extras.level, danger = extras.danger)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    ActionRail(palette, canUndo, soundEnabled, onUndo, onNewGame, onStats, onSettings, onHelp, onToggleSound, extras)
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
                BoardView(snapshot, palette, onCellTap, Modifier.size(boardSize), fx = extras.fx, level = extras.level, danger = extras.danger)
            }
        }
    }
}

/** Landscape, left of the board: the mode, the score and the best score, the next balls. */
@Composable
private fun InfoRail(palette: Palette, score: Int, best: Int, nextColors: List<BallColor>, extras: GameExtras) {
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (palette.windowTitleBar) {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF000080), Color(0xFF1084D0)))).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.app_name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                val font = if (palette.cellStyle == CellStyle.ROUNDED) FontFamily.Default else FontFamily.Monospace
                val title = stringResource(R.string.app_name)
                Text(if (palette.cellStyle == CellStyle.DOS) title.uppercase() else title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (palette.cellStyle == CellStyle.WIN98) palette.accent else palette.text, fontFamily = font)
            }
            ModeBar(if (palette.windowTitleBar) paletteFor(AppTheme.LINES_98) else palette, extras)
            if (palette.windowTitleBar) {
                Column(Modifier.fillMaxWidth().background(Color.Black).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LedNumber(best)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nextColors.forEach { color ->
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(spriteDrawable(color)),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { LedNumber(score) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(stringResource(R.string.hud_score), score, palette, live = true)
                    StatBox(stringResource(R.string.hud_best), best, palette)
                }
                NextPreview(palette, nextColors)
            }
        }
    }
}

/** Landscape, right of the board: the actions as a compact grid of icon buttons (a text menu in the Windows look). */
@Composable
private fun ActionRail(
    palette: Palette,
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
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (palette.windowTitleBar) {
            Column(Modifier.padding(6.dp)) {
                MenuItem(stringResource(R.string.btn_newGame), onNewGame)
                MenuItem(stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
                MenuItem(stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0)
                MenuItem(stringResource(if (soundEnabled) R.string.btn_mute else R.string.btn_unmute), onToggleSound)
                MenuItem(stringResource(R.string.btn_stats), onStats)
                MenuItem(stringResource(R.string.btn_settings), onSettings)
                MenuItem(stringResource(R.string.btn_help), onHelp)
            }
        } else {
            val buttons: List<@Composable () -> Unit> = listOf(
                { ActionButton(AppIcons.Undo, stringResource(R.string.btn_undo), onUndo, enabled = canUndo) },
                { ActionButton(AppIcons.Lightbulb, stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0) },
                { ActionButton(AppIcons.Refresh, stringResource(R.string.btn_newGame), onNewGame) },
                { ActionButton(AppIcons.BarChart, stringResource(R.string.btn_stats), onStats) },
                { ActionButton(if (soundEnabled) AppIcons.VolumeUp else AppIcons.VolumeOff, stringResource(if (soundEnabled) R.string.btn_mute else R.string.btn_unmute), onToggleSound) },
                { ActionButton(AppIcons.Settings, stringResource(R.string.btn_settings), onSettings) },
                { ActionButton(AppIcons.Help, stringResource(R.string.btn_help), onHelp) }
            )
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in buttons.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { it() } }
                }
            }
        }
    }
}

private fun spriteDrawable(color: BallColor): Int = when (spriteIndex(color)) {
    0 -> R.drawable.ball_sprite_0
    1 -> R.drawable.ball_sprite_1
    2 -> R.drawable.ball_sprite_2
    3 -> R.drawable.ball_sprite_3
    4 -> R.drawable.ball_sprite_4
    5 -> R.drawable.ball_sprite_5
    else -> R.drawable.ball_sprite_6
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
                    if (extras.gameOver) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { GameOverActions(onNewGame, extras) }
                    } else {
                        NextPreview(palette, nextColors)
                        Spacer(Modifier.weight(1f))
                    }
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
                if (!compact) {
                    if (extras.gameOver) GameOverActions(onNewGame, extras) else NextPreview(palette, nextColors)
                }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(extras.modeLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textMuted)
            ModeDots(extras.mode, Modifier.padding(start = 8.dp))
        }
        if (extras.goalsLabel.isNotEmpty()) {
            androidx.compose.material3.TextButton(onClick = extras.onGoals, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(extras.goalsLabel, fontSize = 13.sp, color = palette.accent)
            }
        }
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
    DosLayout(
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
            coronationStart = coronationStart,
            russian = false // the 1992 screen is the original: English in every language
        ),
        kingName = kingName,
        pretenderName = pretenderName,
        window = window,
        hall = hall,
        boardCells = { modifier -> DosBoardCells(snapshot, onCellTap, modifier) },
        onButton = onButton,
        onCloseWindow = onCloseWindow,
        modeBar = { ModeBar(paletteFor(AppTheme.COLORLINES_92).copy(textMuted = Color(0xFF55FFFF), accent = Color(0xFFFFFF55)), extras) },
        tools = { horizontal ->
            val buttons: @Composable () -> Unit = {
                ActionButton(AppIcons.Undo, stringResource(R.string.btn_undo), onUndo, enabled = canUndo)
                ActionButton(AppIcons.Lightbulb, stringResource(R.string.btn_hint, extras.hintsLeft.toString()), extras.onHint, enabled = extras.hintsLeft > 0)
                ActionButton(AppIcons.Trophy, stringResource(R.string.dos_topTen), onTopTen)
                ActionButton(AppIcons.BarChart, stringResource(R.string.btn_stats), onStats)
                ActionButton(AppIcons.Settings, stringResource(R.string.btn_settings), onSettings)
            }
            if (horizontal) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { buttons() }
            else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { buttons() }
        },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    )
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
            if (extras.gameOver) GameOverActions(onNewGame, extras) else Row(
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

/** Where the next balls were: once the game is over, "New game" (the same mode) and the result, never a dialog. */
@Composable
private fun GameOverActions(onNewGame: () -> Unit, extras: GameExtras) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onNewGame, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            Text(stringResource(R.string.btn_newGame), maxLines = 1)
        }
        OutlinedButton(onClick = extras.onShowResult, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                stringResource(R.string.gameover_result) + if (extras.resultBadge.isNotEmpty()) " ${extras.resultBadge}" else "",
                maxLines = 1
            )
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
