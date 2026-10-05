package io.github.basil_as.basillines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import io.github.basil_as.basillines.engine.BallColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import io.github.basil_as.basillines.engine.StepId
import io.github.basil_as.basillines.engine.GameEngine
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.GameStats
import io.github.basil_as.basillines.engine.Insights
import io.github.basil_as.basillines.engine.LineDetector
import io.github.basil_as.basillines.engine.ModeId
import io.github.basil_as.basillines.engine.Levels
import io.github.basil_as.basillines.engine.Progress
import io.github.basil_as.basillines.engine.ProgressTracker
import io.github.basil_as.basillines.engine.ScoringSystem
import java.text.DateFormat
import java.util.Date

private val LEVEL_TITLES = listOf(
    R.string.level_title_novice, R.string.level_title_apprentice, R.string.level_title_skilled,
    R.string.level_title_expert, R.string.level_title_master, R.string.level_title_grandmaster,
    R.string.level_title_legend
)

/** Name and description of every achievement, keyed by the id used in the engine. */
val ACHIEVEMENT_TEXT: Map<String, Pair<Int, Int>> = mapOf(
    "first_game" to (R.string.ach_first_game_name to R.string.ach_first_game_desc),
    "first_line" to (R.string.ach_first_line_name to R.string.ach_first_line_desc),
    "long_line_7" to (R.string.ach_long_line_7_name to R.string.ach_long_line_7_desc),
    "long_line_9" to (R.string.ach_long_line_9_name to R.string.ach_long_line_9_desc),
    "score_100" to (R.string.ach_score_100_name to R.string.ach_score_100_desc),
    "score_250" to (R.string.ach_score_250_name to R.string.ach_score_250_desc),
    "score_500" to (R.string.ach_score_500_name to R.string.ach_score_500_desc),
    "score_1000" to (R.string.ach_score_1000_name to R.string.ach_score_1000_desc),
    "games_10" to (R.string.ach_games_10_name to R.string.ach_games_10_desc),
    "games_50" to (R.string.ach_games_50_name to R.string.ach_games_50_desc),
    "games_100" to (R.string.ach_games_100_name to R.string.ach_games_100_desc),
    "lines_50" to (R.string.ach_lines_50_name to R.string.ach_lines_50_desc),
    "lines_250" to (R.string.ach_lines_250_name to R.string.ach_lines_250_desc),
    "streak_3" to (R.string.ach_streak_3_name to R.string.ach_streak_3_desc),
    "streak_7" to (R.string.ach_streak_7_name to R.string.ach_streak_7_desc),
    "marathon" to (R.string.ach_marathon_name to R.string.ach_marathon_desc),
    "level_5" to (R.string.ach_level_5_name to R.string.ach_level_5_desc),
    "level_10" to (R.string.ach_level_10_name to R.string.ach_level_10_desc),
    "daily_first" to (R.string.ach_daily_first_name to R.string.ach_daily_first_desc),
    "all_modes" to (R.string.ach_all_modes_name to R.string.ach_all_modes_desc),
    "goal_first" to (R.string.ach_goal_first_name to R.string.ach_goal_first_desc),
    "goal_day" to (R.string.ach_goal_day_name to R.string.ach_goal_day_desc),
    "goals_7" to (R.string.ach_goals_7_name to R.string.ach_goals_7_desc)
)

private val THEME_NAMES = listOf(
    AppTheme.MODERN to R.string.theme_modern,
    AppTheme.LIGHT to R.string.theme_light,
    AppTheme.MATERIAL to R.string.theme_material,
    AppTheme.NEON to R.string.theme_neon,
    AppTheme.SYNTHWAVE to R.string.theme_synthwave,
    AppTheme.OCEAN to R.string.theme_ocean,
    AppTheme.PAPER to R.string.theme_paper,
    AppTheme.GAMEBOY to R.string.theme_gameboy,
    AppTheme.TERMINAL to R.string.theme_terminal,
    AppTheme.CONTRAST to R.string.theme_contrast,
    AppTheme.LINES_98 to R.string.theme_lines98,
    AppTheme.LINES_98_PLUS to R.string.theme_lines98plus,
    AppTheme.COLORLINES_92 to R.string.theme_colorlines92
)

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    if (totalSeconds < 60) return stringResource(R.string.time_s, totalSeconds)
    val totalMinutes = totalSeconds / 60
    if (totalMinutes < 60) return stringResource(R.string.time_m, totalMinutes)
    return stringResource(R.string.time_hm, totalMinutes / 60, "%02d".format(totalMinutes % 60))
}

@Composable
fun GameOverDialog(
    score: Int,
    best: Int,
    newRecord: Boolean,
    xpGained: Int,
    levelUp: Int?,
    unlocked: List<String>,
    onPlayAgain: () -> Unit,
    onClose: () -> Unit,
    goalXp: Int = 0
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.gameover_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (goalXp > 0) Text(stringResource(R.string.goals_reached) + ": +" + goalXp + " XP", fontWeight = FontWeight.Bold)
                if (newRecord) {
                    Text(
                        "★ " + stringResource(R.string.gameover_newRecord),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.gameover_final), score.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.gameover_best), best.toString(), Modifier.weight(1f))
                }
                if (xpGained > 0) Text(stringResource(R.string.gameover_xp, xpGained), fontWeight = FontWeight.Bold)
                if (levelUp != null) {
                    Text(
                        stringResource(R.string.gameover_levelUp, levelUp),
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold
                    )
                }
                if (unlocked.isNotEmpty()) {
                    Text(stringResource(R.string.gameover_unlocked), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    unlocked.forEach { id -> AchievementRow(id, unlockedAt = 0L, showDate = false) }
                }
            }
        },
        confirmButton = { Button(onClick = onPlayAgain) { Text(stringResource(R.string.gameover_playAgain)) } },
        dismissButton = { OutlinedButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun AchievementRow(id: String, unlockedAt: Long?, showDate: Boolean = true) {
    val text = ACHIEVEMENT_TEXT[id] ?: return
    val done = unlockedAt != null
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (done) 0.7f else 0.3f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (done) "★" else "🔒", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(text.first), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(stringResource(text.second), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showDate) {
                Text(
                    if (done && unlockedAt != null) dateFormat.format(Date(unlockedAt)) else stringResource(R.string.stats_locked),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HelpDialog(onClose: () -> Unit, onTutorial: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.help_objective))
                Text(stringResource(R.string.help_scoring))
                Column {
                    for (n in 5..9) {
                        Text(
                            "• " + stringResource(
                                R.string.help_scoringLine,
                                pluralStringResource(R.plurals.balls, n, n),
                                LineDetector.calculateScore(n, ScoringSystem.GAMOS_1992)
                            )
                        )
                    }
                }
                Text(stringResource(R.string.help_movement))
                Text(stringResource(R.string.help_freeTurn))
                Text(stringResource(R.string.help_modes), fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• " + stringResource(R.string.help_modes_classic))
                    Text("• " + stringResource(R.string.help_modes_easy))
                    Text("• " + stringResource(R.string.help_modes_blitz))
                    Text("• " + stringResource(R.string.help_modes_daily))
                }
                Text(stringResource(R.string.help_hint))
                Text(stringResource(R.string.help_switching))
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } },
        dismissButton = { TextButton(onClick = onTutorial) { Text(stringResource(R.string.tutorial_start)) } }
    )
}

@Composable
fun SettingsDialog(
    theme: AppTheme,
    onTheme: (AppTheme) -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    spawnPreview: Boolean,
    onTogglePreview: () -> Unit,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    effects: EffectsLevel,
    onEffects: (EffectsLevel) -> Unit,
    vibrationSupported: Boolean,
    vibration: Boolean,
    onVibration: (Boolean) -> Unit,
    onTutorial: () -> Unit,
    versionName: String,
    autoUpdate: Boolean,
    onAutoUpdate: (Boolean) -> Unit,
    updateStatus: String?,
    onCheckUpdate: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_theme), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var picking by remember { mutableStateOf(false) }
                val currentName = stringResource(THEME_NAMES.first { it.first == theme }.second)
                // The themes stay folded: one row shows the current look, a tap opens the picker with previews.
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { picking = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeSwatch(theme, Modifier.width(88.dp).height(56.dp))
                    Text(currentName, Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { picking = true }) { Text(stringResource(R.string.settings_themeChange)) }
                }
                if (picking) {
                    AlertDialog(
                        onDismissRequest = { picking = false },
                        title = { Text(stringResource(R.string.settings_themePick)) },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                THEME_NAMES.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pair.forEach { (value, name) ->
                                            val label = stringResource(name)
                                            Column(
                                                Modifier
                                                    .weight(1f)
                                                    .border(if (theme == value) 3.dp else 1.dp, if (theme == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                    .selectable(selected = theme == value, role = Role.RadioButton, onClick = { onTheme(value); picking = false })
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                ThemeSwatch(value, Modifier.fillMaxWidth().height(64.dp))
                                                Text(label, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.btn_close)) } }
                    )
                }
                SwitchRow(stringResource(R.string.settings_sound), soundEnabled, onToggleSound)
                SwitchRow(stringResource(R.string.settings_preview), spawnPreview, onTogglePreview)
                Text(stringResource(R.string.settings_effects), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(EffectsLevel.OFF to R.string.effects_off, EffectsLevel.CALM to R.string.effects_calm, EffectsLevel.FULL to R.string.effects_full).forEach { (level, name) ->
                        androidx.compose.material3.FilterChip(
                            selected = effects == level,
                            onClick = { onEffects(level) },
                            label = { Text(stringResource(name)) },
                            modifier = Modifier.semantics { role = Role.RadioButton }
                        )
                    }
                }
                Text(stringResource(R.string.settings_effectsHint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                // Vibration is its own switch, separate from the sound.
                SwitchRow(stringResource(R.string.settings_vibration), vibration) { onVibration(!vibration) }
                if (!vibrationSupported) Text(stringResource(R.string.settings_vibrationOff), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tutorial_start), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
                Text(stringResource(R.string.lang_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    AppLanguage.AUTO to R.string.lang_auto,
                    AppLanguage.EN to R.string.lang_en,
                    AppLanguage.RU to R.string.lang_ru
                ).forEach { (value, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == value, role = Role.RadioButton, onClick = { onLanguage(value) })
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == value, onClick = null)
                        Text(stringResource(name), Modifier.padding(start = 12.dp))
                    }
                }
                Text(stringResource(R.string.update_version, versionName), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                SwitchRow(stringResource(R.string.update_auto), autoUpdate) { onAutoUpdate(!autoUpdate) }
                Text(stringResource(R.string.update_autoHint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onCheckUpdate, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(stringResource(R.string.update_check))
                }
                if (updateStatus != null) Text(updateStatus, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TrendChart(values: List<Int>) {
    if (values.size < 2) return
    val color = MaterialTheme.colorScheme.primary
    val max = maxOf(values.max(), 1)
    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height - 4f - (v.toFloat() / max) * (size.height - 8f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, radius = 4f, center = Offset(x, y))
        }
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

@Composable
fun StatsDialog(
    history: List<GameRecord>,
    progress: Progress,
    ledger: io.github.basil_as.basillines.engine.Ledger,
    now: Long,
    dataMessage: String?,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val today = remember(now) { java.time.Instant.ofEpochMilli(if (now == 0L) System.currentTimeMillis() else now).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    val summary = GameStats.summarize(history)
    var confirming by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val level = Levels.info(ProgressTracker.xpOf(progress))
    val title = LEVEL_TITLES[Levels.titleIndex(level.level)]
    val streak = ProgressTracker.currentStreak(progress.days, ProgressTracker.dayKey(now))
    val unlockedCount = ProgressTracker.ACHIEVEMENTS.count { it.id in progress.achievements }
    val shown = if (showAll) history.take(200) else history.take(10)

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.stats_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(R.string.stats_tab_overview, R.string.stats_tab_career, R.string.stats_tab_mind, R.string.stats_tab_seasons, R.string.stats_tab_records, R.string.stats_tab_data)
                androidx.compose.material3.ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                    tabs.forEachIndexed { i, name ->
                        androidx.compose.material3.Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(name), maxLines = 1) })
                    }
                }
                when (tab) {
                    1 -> { CareerTab(ledger, today); return@Column }
                    2 -> { MindTab(history); return@Column }
                    3 -> { SeasonsTab(ledger, today); return@Column }
                    4 -> { RecordsTab(history, ledger); return@Column }
                    5 -> { DataTab(dataMessage, history.size, onExportJson, onExportCsv, onImport); return@Column }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.level_label, level.level), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(stringResource(title), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { level.fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(
                            stringResource(R.string.level_xp, level.into, level.needed),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val cards = listOf(
                    stringResource(R.string.stats_gamesPlayed) to progress.totalGames.toString(),
                    stringResource(R.string.stats_best) to progress.bestScore.toString(),
                    stringResource(R.string.stats_average) to summary.averageScore.toString(),
                    stringResource(R.string.stats_totalScore) to progress.totalScore.toString(),
                    stringResource(R.string.stats_lines) to progress.totalLines.toString(),
                    stringResource(R.string.stats_bestLine) to progress.bestLine.toString(),
                    stringResource(R.string.stats_streak) to pluralStringResource(R.plurals.days, streak, streak),
                    stringResource(R.string.stats_bestStreak) to ProgressTracker.bestStreak(progress.days).let { pluralStringResource(R.plurals.days, it, it) },
                    stringResource(R.string.stats_playTime) to formatDuration(progress.totalPlayMs),
                    stringResource(R.string.stats_longestGame) to pluralStringResource(R.plurals.moves, progress.longestGameMoves, progress.longestGameMoves)
                )
                cards.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
                    }
                }

                if (history.isNotEmpty()) InsightsSection(history, progress, level.level, level.xp, now)

                Text(stringResource(R.string.stats_trend), Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (history.size < 2) Text(stringResource(R.string.stats_empty)) else TrendChart(ProgressTracker.scoreTrend(history, 20))

                Text(
                    stringResource(R.string.stats_achievements) + " · " +
                        stringResource(R.string.stats_achievementsCount, unlockedCount, ProgressTracker.ACHIEVEMENTS.size),
                    Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ProgressTracker.ACHIEVEMENTS.forEach { AchievementRow(it.id, progress.achievements[it.id]) }

                Text(stringResource(R.string.stats_recent), Modifier.padding(top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (history.isEmpty()) Text(stringResource(R.string.stats_empty))
                shown.forEach { game ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(game.score.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                if (game == summary.bestRecord) Badge(stringResource(R.string.stats_bestMark), filled = true)
                                if (!game.completed) Badge(stringResource(R.string.stats_unfinished), filled = false)
                            }
                            Text(
                                dateFormat.format(Date(game.endedAt)) + " · " +
                                    pluralStringResource(R.plurals.moves, game.moves, game.moves) + " · " +
                                    formatDuration(game.durationMs),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (history.size > 10) {
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(if (showAll) stringResource(R.string.stats_showLess) else stringResource(R.string.stats_showAll, history.size))
                    }
                }
                if (history.isNotEmpty() || progress.totalGames > 0) {
                    if (confirming) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.stats_confirmClear))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onClear(); confirming = false }) { Text(stringResource(R.string.stats_confirmYes)) }
                                OutlinedButton(onClick = { confirming = false }) { Text(stringResource(R.string.stats_confirmNo)) }
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { confirming = true }) { Text(stringResource(R.string.stats_clear)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

@Composable
private fun Badge(text: String, filled: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
            Text(text.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun BarChart(values: List<Int>, labels: List<String>, description: String) {
    val max = maxOf(values.maxOrNull() ?: 1, 1)
    val color = MaterialTheme.colorScheme.primary
    val a11y = description + ": " + labels.zip(values).joinToString(", ") { "${it.first} ${it.second}" }
    Row(
        Modifier.fillMaxWidth().height(84.dp).semantics { contentDescription = a11y },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(if (v == 0) 0.03f else maxOf(0.1f, v.toFloat() / max) * 0.8f)
                        .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
                Text(labels[i], fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsightsSection(history: List<GameRecord>, progress: Progress, level: Int, xp: Int, now: Long) {
    val window = 10
    val trend = Insights.trendOf(history, window)
    val eff = Insights.efficiency(history)
    val eta = Insights.levelEta(history, Levels.threshold(level + 1) - xp)
    val days = Insights.dailyActivity(history, 14, now)
    val hist = Insights.scoreHistogram(history, 50)
    val weekdays = Insights.weekdayActivity(history)
    val steps = Insights.recordProgression(history).takeLast(5)
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val weekdayNames = remember {
        // 2024-01-01 is a Monday.
        (0 until 7).map { java.time.DayOfWeek.of(it + 1).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.insights_title), Modifier.padding(top = 8.dp), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (trend == null) {
                stringResource(R.string.insights_trend_needMore, maxOf(1, window * 2 - history.size))
            } else {
                val id = when (trend.direction) {
                    Insights.Direction.UP -> R.string.insights_trend_up
                    Insights.Direction.DOWN -> R.string.insights_trend_down
                    Insights.Direction.FLAT -> R.string.insights_trend_flat
                }
                stringResource(id, window, trend.recentAverage, kotlin.math.abs(trend.changePercent))
            },
            fontSize = 14.sp
        )
        Text(stringResource(R.string.insights_efficiency, "%.1f".format(eff.average), "%.1f".format(eff.best)), fontSize = 14.sp)
        if (eta != null) {
            Text(
                stringResource(R.string.insights_eta, pluralStringResource(R.plurals.games, eta, eta), level + 1),
                fontSize = 14.sp
            )
        }
        Text(stringResource(R.string.insights_daily), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(days.map { it.games }, days.map { it.day.takeLast(2) }, stringResource(R.string.insights_daily))
        Text(stringResource(R.string.insights_histogram), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(hist.map { it.count }, hist.map { stringResource(R.string.insights_bucketLabel, it.from) }, stringResource(R.string.insights_histogram))
        Text(stringResource(R.string.insights_weekdays), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BarChart(weekdays.map { it.average }, weekdayNames, stringResource(R.string.insights_weekdays))
        if (steps.isNotEmpty()) {
            Text(stringResource(R.string.insights_records), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            steps.forEach { Text("${it.score} · " + dateFormat.format(Date(it.at)), fontSize = 13.sp) }
        }
    }
}

/** Starting a game: the mode, and a warning that is impossible to miss when a game is in progress. */
@Composable
fun NewGameDialog(
    current: ModeId,
    playing: ModeId,
    modeStats: Map<ModeId, Pair<Int, Int>>,
    onPick: (ModeId) -> Unit,
    inProgress: Boolean,
    score: Int,
    moves: Int,
    onStart: () -> Unit,
    onKeep: () -> Unit,
    onTutorial: () -> Unit
) {
    val modes = listOf(
        Triple(ModeId.CLASSIC, R.string.mode_classic, R.string.mode_classic_desc),
        Triple(ModeId.EASY, R.string.mode_easy, R.string.mode_easy_desc),
        Triple(ModeId.BLITZ, R.string.mode_blitz, R.string.mode_blitz_desc),
        Triple(ModeId.DAILY, R.string.mode_daily, R.string.mode_daily_desc)
    )
    // The mode is rarely changed: show the one that will be played and fold the list behind a button.
    var picking by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.newgame_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (inProgress) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).semantics {
                            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Assertive
                        }
                    ) {
                        Text(
                            stringResource(R.string.newgame_warning, score.toString(), pluralStringResource(R.plurals.moves, moves, moves)),
                            Modifier.padding(12.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (inProgress && current != playing) {
                    Text(
                        stringResource(R.string.newgame_switching, stringResource(modeName(playing)), stringResource(modeName(current))),
                        Modifier.padding(bottom = 8.dp)
                    )
                }
                if (!picking) {
                    val shown = modes.first { it.first == current }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(shown.second), fontWeight = FontWeight.SemiBold)
                                ModeDots(current, Modifier.padding(start = 8.dp))
                            }
                            Text(stringResource(shown.third), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { picking = true }) { Text(stringResource(R.string.newgame_changeMode)) }
                    }
                } else Text(stringResource(R.string.mode_label), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (picking) modes.forEach { (mode, name, desc) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = current == mode, role = Role.RadioButton, onClick = { onPick(mode) })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == mode, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(name), fontWeight = FontWeight.SemiBold)
                                ModeDots(mode, Modifier.padding(start = 8.dp))
                            }
                            Text(stringResource(desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val limit = mode.timeLimitMs
                            val facts = stringResource(
                                R.string.mode_facts,
                                stringResource(R.string.mode_colors, mode.colors.toString()),
                                if (limit == null) stringResource(R.string.mode_noLimit) else stringResource(R.string.mode_minutes, (limit / 60000).toString())
                            )
                            val played = modeStats[mode]
                            Text(
                                if (played != null && played.first > 0) facts + " · " + stringResource(R.string.mode_bestIn, played.second.toString(), played.first.toString()) else facts,
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(onClick = onTutorial, modifier = Modifier.padding(top = 4.dp)) { Text(stringResource(R.string.tutorial_start)) }
            }
        },
        // With a game in progress the safe choice is the focused, prominent one.
        confirmButton = {
            if (inProgress) {
                Button(onClick = onKeep) { Text(stringResource(R.string.newgame_keep)) }
            } else {
                Button(onClick = onStart) { Text(stringResource(R.string.newgame_startMode, stringResource(modeName(current)))) }
            }
        },
        dismissButton = {
            if (inProgress) {
                OutlinedButton(onClick = onStart) { Text(stringResource(R.string.newgame_startMode, stringResource(modeName(current)))) }
            } else {
                // Always a way back: opening the dialog must never force a new game.
                OutlinedButton(onClick = onKeep) { Text(stringResource(R.string.data_cancel)) }
            }
        }
    )
}

internal fun modeName(mode: ModeId): Int = when (mode) {
    ModeId.CLASSIC -> R.string.mode_classic
    ModeId.EASY -> R.string.mode_easy
    ModeId.BLITZ -> R.string.mode_blitz
    ModeId.DAILY -> R.string.mode_daily
}

/** The colours a mode plays with, as small dots. */
@Composable
internal fun ModeDots(mode: ModeId, modifier: Modifier = Modifier) {
    val colors = io.github.basil_as.basillines.engine.Modes.colorsFor(mode)
    Row(modifier.semantics { contentDescription = "" }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.forEach { c -> Box(Modifier.size(9.dp).background(ballColor(c), androidx.compose.foundation.shape.CircleShape)) }
    }
}

/** A backup file was chosen: add it (nothing is lost) or replace everything here, which asks twice. */
@Composable
fun ImportDialog(
    games: Int,
    currentGames: Int,
    exportedAt: Long,
    first: Long?,
    last: Long?,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onCancel: () -> Unit
) {
    var replacing by remember { mutableStateOf(false) }
    var understood by remember { mutableStateOf(false) }
    val fmt = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    fun day(ms: Long) = fmt.format(java.util.Date(ms))
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.data_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.data_preview, if (exportedAt > 0) day(exportedAt) else "?", games.toString()), fontWeight = FontWeight.SemiBold)
                if (first != null && last != null) Text(stringResource(R.string.data_range, day(first), day(last)), fontSize = 13.sp)
                if (replacing) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(R.string.data_replaceWarning, currentGames.toString()), Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold) }
                    Row(
                        Modifier.fillMaxWidth().toggleable(value = understood, role = Role.Checkbox, onValueChange = { understood = it }).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(checked = understood, onCheckedChange = null)
                        Text(stringResource(R.string.data_replaceCheck), Modifier.padding(start = 8.dp))
                    }
                } else {
                    Text(stringResource(R.string.data_mergeHint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (replacing) {
                Button(onClick = onReplace, enabled = understood, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.data_replaceConfirm))
                }
            } else {
                Button(onClick = onMerge) { Text(stringResource(R.string.data_merge)) }
            }
        },
        dismissButton = {
            Row {
                if (!replacing) OutlinedButton(onClick = { replacing = true }) { Text(stringResource(R.string.data_replace)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.data_cancel)) }
            }
        }
    )
}

internal fun goalTitle(type: io.github.basil_as.basillines.engine.GoalType): Int = when (type) {
    io.github.basil_as.basillines.engine.GoalType.SCORE -> R.string.goals_score
    io.github.basil_as.basillines.engine.GoalType.LINE -> R.string.goals_line
    io.github.basil_as.basillines.engine.GoalType.MOVES -> R.string.goals_moves
    io.github.basil_as.basillines.engine.GoalType.LINES -> R.string.goals_lines
    io.github.basil_as.basillines.engine.GoalType.EFFICIENCY -> R.string.goals_efficiency
    io.github.basil_as.basillines.engine.GoalType.BEAT_YESTERDAY -> R.string.goals_beatYesterday
}

/** A target or value without a pointless ".0" (efficiency keeps one decimal). */
internal fun goalNumber(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, v)

/** Today's goals with progress; they are built from the player's own recent games so they stay fair. */
@Composable
fun GoalsDialog(progress: List<io.github.basil_as.basillines.engine.GoalProgress>, goalStreak: Int, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.goals_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.goals_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (p in progress) {
                    val title = stringResource(goalTitle(p.goal.type), goalNumber(p.goal.target))
                    val fraction = (p.value / p.goal.target).toFloat().coerceIn(0f, 1f)
                    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = title }) {
                        Text((if (p.done) "✓ " else "") + title, fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        Text(
                            if (p.done) stringResource(R.string.goals_done) else stringResource(R.string.goals_progress, goalNumber(p.value), goalNumber(p.goal.target)),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(stringResource(R.string.goals_bonus, io.github.basil_as.basillines.engine.Goals.BONUS_XP.toString()), fontSize = 13.sp)
                if (progress.isNotEmpty() && progress.all { it.done }) Text(stringResource(R.string.goals_allDone), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.goals_streak) + ": " + goalStreak, fontSize = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}

/** Where an update is: offered, being downloaded, ready to install, waiting for the install permission, or failed. */
sealed interface UpdateStage {
    data object Offer : UpdateStage
    /** [progress] is 0..1, or negative while the size is not known. */
    data class Downloading(val progress: Float) : UpdateStage
    data class Ready(val file: java.io.File) : UpdateStage
    data class NeedPermission(val file: java.io.File) : UpdateStage
    data class Failed(val why: UpdateInstaller.Failure) : UpdateStage
}

@Composable
fun UpdateDialog(
    version: String,
    current: String,
    stage: UpdateStage,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onAllow: () -> Unit,
    onBrowser: () -> Unit,
    onCancel: () -> Unit,
    onLater: () -> Unit
) {
    val busy = stage is UpdateStage.Downloading
    AlertDialog(
        // Back or a tap outside while downloading stops the download; otherwise it is "later".
        onDismissRequest = if (busy) onCancel else onLater,
        title = { Text(stringResource(if (stage is UpdateStage.Ready || stage is UpdateStage.NeedPermission) R.string.update_readyTitle else R.string.update_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.update_available, version, current), fontWeight = FontWeight.SemiBold)
                when (stage) {
                    UpdateStage.Offer -> Text(stringResource(R.string.update_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is UpdateStage.Downloading -> {
                        if (stage.progress >= 0f) {
                            LinearProgressIndicator(progress = { stage.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.update_downloading, (stage.progress * 100).toInt()), fontSize = 13.sp)
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(stringResource(R.string.update_downloadingUnknown), fontSize = 13.sp)
                        }
                    }
                    is UpdateStage.Ready -> Text(stringResource(R.string.update_ready), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    is UpdateStage.NeedPermission -> Text(stringResource(R.string.update_needPermission), fontSize = 13.sp)
                    is UpdateStage.Failed -> Text(
                        stringResource(
                            when (stage.why) {
                                UpdateInstaller.Failure.NETWORK -> R.string.update_errNetwork
                                UpdateInstaller.Failure.CHECKSUM -> R.string.update_errChecksum
                                UpdateInstaller.Failure.WRONG_APP -> R.string.update_errWrongApp
                            }
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Assertive }
                    )
                }
            }
        },
        confirmButton = {
            when (stage) {
                UpdateStage.Offer -> Button(onClick = onDownload) { Text(stringResource(R.string.update_downloadInstall)) }
                is UpdateStage.Downloading -> {}
                is UpdateStage.Ready -> Button(onClick = onInstall) { Text(stringResource(R.string.update_install)) }
                is UpdateStage.NeedPermission -> Button(onClick = onAllow) { Text(stringResource(R.string.update_allow)) }
                is UpdateStage.Failed -> Button(onClick = onDownload) { Text(stringResource(R.string.update_retry)) }
            }
        },
        dismissButton = {
            when (stage) {
                is UpdateStage.Downloading -> TextButton(onClick = onCancel) { Text(stringResource(R.string.data_cancel)) }
                is UpdateStage.Failed -> TextButton(onClick = onBrowser) { Text(stringResource(R.string.update_browser)) }
                else -> TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
            }
        }
    )
}


/** A small picture of a look: its board with three balls, drawn from the real palette and ball style. */
@Composable
internal fun ThemeSwatch(theme: AppTheme, modifier: Modifier = Modifier) {
    val palette = paletteFor(theme)
    val sprites = rememberSprites(palette)
    val balls = listOf(BallColor.RED, BallColor.BLUE, BallColor.GREEN)
    Canvas(modifier.clip(RoundedCornerShape(if (palette.square) 0.dp else 8.dp)).background(palette.background).border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(if (palette.square) 0.dp else 8.dp))) {
        val pad = size.height * 0.1f
        val board = size.height - pad * 2
        val left = (size.width - board) / 2f
        drawRect(palette.boardBackground, androidx.compose.ui.geometry.Offset(left, pad), androidx.compose.ui.geometry.Size(board, board))
        val cell = board / 3f
        for (r in 0 until 3) for (c in 0 until 3) drawCell(palette, left + c * cell, pad + r * cell, cell)
        listOf(0 to 0, 1 to 1, 2 to 2).forEachIndexed { i, (c, r) ->
            drawBall(palette, sprites, balls[i], androidx.compose.ui.geometry.Offset(left + (c + 0.5f) * cell, pad + (r + 0.5f) * cell), cell * 0.38f, shadow = true)
        }
        // A strip of the panel colour beside the board, as the real screen has.
        drawRect(palette.panel, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(left * 0.55f, size.height))
        drawRect(palette.accent, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(left * 0.55f, size.height * 0.12f))
    }
}

/** A tutorial in progress: the step, the game it replaced, and whether the last try missed. */
class TutorialRun(val step: Int, val saved: GameEngine, val retry: Boolean)

internal fun tutorialText(id: StepId, retry: Boolean): Int = when {
    retry && id == StepId.LINE -> R.string.tutorial_retry_line
    retry && id == StepId.BLOCKED -> R.string.tutorial_retry_blocked
    else -> when (id) {
        StepId.SELECT -> R.string.tutorial_select
        StepId.MOVE -> R.string.tutorial_move
        StepId.NEXT -> R.string.tutorial_next
        StepId.LINE -> R.string.tutorial_line
        StepId.FREE -> R.string.tutorial_free
        StepId.BLOCKED -> R.string.tutorial_blocked
        StepId.END -> R.string.tutorial_end
    }
}

/** The coach card over the bottom of the screen; the board and the score panel stay visible and playable. */
@Composable
fun TutorialBanner(
    palette: Palette,
    step: Int,
    total: Int,
    text: String,
    showContinue: Boolean,
    last: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(2.dp, palette.accent),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.tutorial_title), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(stringResource(R.string.tutorial_progress, step, total), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text, fontSize = 14.sp, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showContinue) Button(onClick = onContinue) { Text(stringResource(if (last) R.string.tutorial_finish else R.string.tutorial_continue)) }
                if (!last) TextButton(onClick = onSkip) { Text(stringResource(R.string.tutorial_skip)) }
            }
        }
    }
}
