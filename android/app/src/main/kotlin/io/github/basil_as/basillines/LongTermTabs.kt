package io.github.basil_as.basillines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.basil_as.basillines.engine.Careers
import io.github.basil_as.basillines.engine.Bucket
import io.github.basil_as.basillines.engine.Chronotype
import io.github.basil_as.basillines.engine.Finding
import io.github.basil_as.basillines.engine.FindingId
import io.github.basil_as.basillines.engine.GameRecord
import io.github.basil_as.basillines.engine.Mind
import io.github.basil_as.basillines.engine.Ledger
import io.github.basil_as.basillines.engine.ProgressTracker
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private fun num(v: Number): String = NumberFormat.getInstance().format(v)

@Composable
private fun Subtitle(text: String) = Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun Muted(text: String) = Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f).padding(end = 8.dp))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/** "September 2026" for a "YYYY-MM" key, in the language of the app. */
private fun monthName(month: String): String {
    val (y, m) = month.split("-").map { it.toInt() }
    return LocalDate.of(y, m, 1).month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() } + " " + y
}

private fun tierName(t: Careers.Tier): Int = when (t) {
    Careers.Tier.BRONZE -> R.string.season_tier_bronze
    Careers.Tier.SILVER -> R.string.season_tier_silver
    Careers.Tier.GOLD -> R.string.season_tier_gold
    Careers.Tier.PLATINUM -> R.string.season_tier_platinum
    Careers.Tier.LEGEND -> R.string.season_tier_legend
}

@Composable
fun CareerTab(ledger: Ledger, today: LocalDate) {
    val t = Careers.totals(ledger)
    val (current, longest) = Careers.streaks(ledger, today.toString())
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Muted(stringResource(R.string.career_intro))
        KeyValue(stringResource(R.string.stats_gamesPlayed), num(t.games))
        KeyValue(stringResource(R.string.stats_totalScore), num(t.score))
        KeyValue(stringResource(R.string.stats_lines), num(t.lines))
        KeyValue(stringResource(R.string.career_activeDays), num(t.activeDays))
        KeyValue(stringResource(R.string.stats_streak), pluralStringResource(R.plurals.days, current, current))
        KeyValue(stringResource(R.string.stats_bestStreak), pluralStringResource(R.plurals.days, longest, longest))
        KeyValue(stringResource(R.string.stats_playTime), formatDuration(t.playMs))

        Subtitle(stringResource(R.string.career_heatmap))
        Heatmap(ledger, today)

        Subtitle(stringResource(R.string.career_milestones))
        for ((track, value, ladder) in Careers.milestones(t)) {
            val name = stringResource(
                when (track) {
                    Careers.Track.GAMES -> R.string.career_ms_games
                    Careers.Track.SCORE -> R.string.career_ms_score
                    Careers.Track.LINES -> R.string.career_ms_lines
                    Careers.Track.HOURS -> R.string.career_ms_hours
                    Careers.Track.DAYS -> R.string.career_ms_days
                }
            )
            Column(Modifier.semantics(mergeDescendants = true) { contentDescription = name }) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                LinearProgressIndicator(progress = { ladder.fraction.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp))
                Muted(stringResource(R.string.career_ms_progress, num(value), num(ladder.next), ladder.reached.toString()))
            }
        }

        val memories = Careers.memories(ledger, today.toString())
        if (memories.isNotEmpty()) {
            Subtitle(stringResource(R.string.career_memories))
            for (m in memories) {
                KeyValue(
                    m.day,
                    if (m.yearsAgo == 0) stringResource(R.string.career_memory_monthAgo, m.day, m.games.toString(), num(m.best))
                    else stringResource(R.string.career_memory_yearsAgo, m.day.take(4), m.games.toString(), num(m.best))
                )
            }
        }

        val years = Careers.yearly(ledger).reversed()
        if (years.isNotEmpty()) {
            Subtitle(stringResource(R.string.career_years))
            for (y in years) {
                KeyValue(y.year.toString(), stringResource(R.string.career_yearRow, num(y.games), y.activeDays.toString(), num(y.best), y.bestMonth?.let { monthName(it) } ?: "–"))
            }
        }

        Subtitle(stringResource(R.string.career_months))
        val months = Careers.monthly(ledger).takeLast(12).reversed()
        if (months.isEmpty()) Muted(stringResource(R.string.stats_empty))
        for (m in months) {
            KeyValue(monthName(m.month), stringResource(R.string.career_monthRow, m.games.toString(), num(m.best), num(m.average)))
        }
    }
}

@Composable
private fun Heatmap(ledger: Ledger, today: LocalDate) {
    val grid = Careers.heatmap(ledger, today, 26)
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val label = stringResource(R.string.career_heatmap)
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = label }) {
        val cell = minOf(size.width / grid.size, size.height / 7f)
        for ((w, col) in grid.withIndex()) for ((r, c) in col.withIndex()) {
            if (c == null) continue
            val color = if (c.second == 0) empty else accent.copy(alpha = 0.25f + 0.25f * (c.second - 1))
            drawRoundRect(color, Offset(w * cell + 1f, r * cell + 1f), Size(cell - 2f, cell - 2f), CornerRadius(cell * 0.2f))
        }
    }
}

@Composable
fun SeasonsTab(ledger: Ledger, today: LocalDate) {
    val s = Careers.season(ledger, today)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.season_title, monthName(s.month)), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(stringResource(tierName(s.tier)), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Muted(stringResource(R.string.season_points, num(s.points), s.games.toString()))
        val next = s.nextTier
        val toNext = s.toNext
        Muted(
            if (next != null && toNext != null) stringResource(R.string.season_toNext, num(toNext), stringResource(tierName(next)))
            else stringResource(R.string.season_top)
        )
        Muted(stringResource(R.string.season_daysLeft, s.daysLeft.toString()))
        Muted(stringResource(R.string.season_baseline, num(s.baseline)))
        Subtitle(stringResource(R.string.season_past))
        if (s.past.isEmpty()) Muted(stringResource(R.string.season_none))
        for ((month, points, tier) in s.past) KeyValue(monthName(month), stringResource(tierName(tier)) + " · " + num(points))
        s.bestMonth?.let { Muted(stringResource(R.string.season_best, monthName(it.month), num(it.score))) }
    }
}

@Composable
fun RecordsTab(history: List<GameRecord>, ledger: Ledger) {
    val rows = mutableListOf<Pair<String, String>>()
    history.maxByOrNull { it.score }?.takeIf { it.score > 0 }?.let { rows += stringResource(R.string.rec_bestScore) to num(it.score) }
    history.maxOfOrNull { it.maxLine }?.takeIf { it > 0 }?.let { rows += stringResource(R.string.rec_bestLine) to it.toString() }
    history.maxByOrNull { it.lines }?.takeIf { it.lines > 0 }?.let { rows += stringResource(R.string.rec_mostLines) to num(it.lines) }
    history.filter { it.moves >= 15 }.maxByOrNull { it.score.toDouble() / it.moves }?.let {
        rows += stringResource(R.string.rec_bestEfficiency) to "%.1f".format(Locale.ROOT, it.score.toDouble() / it.moves)
    }
    history.maxByOrNull { it.durationMs }?.takeIf { it.durationMs > 0 }?.let { rows += stringResource(R.string.rec_longestGame) to formatDuration(it.durationMs) }
    ledger.maxByOrNull { it.value.score }?.takeIf { it.value.score > 0 }?.let { rows += stringResource(R.string.rec_bestDay) to "${num(it.value.score)} · ${it.key}" }
    ledger.maxByOrNull { it.value.games }?.let { rows += stringResource(R.string.rec_busiestDay) to "${it.value.games} · ${it.key}" }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Subtitle(stringResource(R.string.rec_title))
        if (rows.isEmpty()) Muted(stringResource(R.string.rec_none))
        for ((label, value) in rows) KeyValue(label, value)
    }
}

@Composable
fun DataTab(message: String?, games: Int, onExportJson: () -> Unit, onExportCsv: () -> Unit, onImport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Subtitle(stringResource(R.string.data_title))
        Muted(stringResource(R.string.data_intro))
        Muted(stringResource(R.string.data_storageApp, num(games)))
        OutlinedButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_exportJson)) }
        OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_exportCsv)) }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.data_import)) }
        if (message != null) Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun findingText(f: Finding): String = when (f.id) {
    FindingId.WINDOW -> stringResource(R.string.mind_f_window, f.a.toInt().toString(), f.b.toInt().toString(), "+" + f.c.toInt())
    FindingId.CHRONOTYPE -> stringResource(
        when (f.chronotype) {
            Chronotype.LARK -> R.string.mind_chrono_lark
            Chronotype.DAY -> R.string.mind_chrono_day
            Chronotype.EVENING -> R.string.mind_chrono_evening
            else -> R.string.mind_chrono_owl
        }
    )
    FindingId.TIRED -> stringResource(R.string.mind_f_tired, f.a.toInt().toString())
    FindingId.FRESH -> stringResource(R.string.mind_f_fresh, f.a.toInt().toString())
    FindingId.TEMPO_FAST -> stringResource(R.string.mind_f_tempoFast, "+" + f.a.toInt())
    FindingId.TEMPO_SLOW -> stringResource(R.string.mind_f_tempoSlow, "+" + f.a.toInt())
    FindingId.TEMPO_MID -> stringResource(R.string.mind_f_tempoMid, "+" + f.a.toInt())
    FindingId.SLOWDOWN -> stringResource(R.string.mind_f_slowdown, f.a.toInt().toString())
    FindingId.STEADY -> stringResource(R.string.mind_f_steady, "%.2f".format(Locale.ROOT, f.a))
    FindingId.ERRATIC -> stringResource(R.string.mind_f_erratic, "%.2f".format(Locale.ROOT, f.a))
    FindingId.IMPULSIVE -> stringResource(R.string.mind_f_impulsive, f.a.toInt().toString())
    FindingId.PLANNER -> stringResource(R.string.mind_f_planner, f.a.toInt().toString())
    FindingId.TIGHT -> stringResource(R.string.mind_f_tight, f.a.toInt().toString())
    FindingId.CALM -> stringResource(R.string.mind_f_calm, f.a.toString())
    FindingId.GROWTH -> stringResource(R.string.mind_f_growth, f.a.toInt().toString(), f.b.toInt().toString(), f.c.toInt().toString())
    FindingId.WEEKDAY -> stringResource(
        R.string.mind_f_weekday,
        java.time.DayOfWeek.of(f.a.toInt() + 1).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() },
        "+" + f.b.toInt()
    )
}

/** How the player thinks and when they play well, from the play data of every game. */
@Composable
fun MindTab(history: List<GameRecord>) {
    val report = androidx.compose.runtime.remember(history) { Mind.report(history) }
    val list = androidx.compose.runtime.remember(report) { Mind.findings(report) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Muted(stringResource(R.string.mind_intro))
        if (report.games < Mind.NEEDED) {
            Text(stringResource(R.string.mind_need, (Mind.NEEDED - report.games).toString()))
            return@Column
        }
        Muted(stringResource(R.string.mind_sample, report.games.toString(), report.decisions.toString()))
        if (list.isNotEmpty()) {
            Subtitle(stringResource(R.string.mind_findings))
            for (f in list) Text("• " + findingText(f), fontSize = 14.sp)
        }
        Subtitle(stringResource(R.string.mind_cards))
        fun pct(v: Double) = "${Math.round(v * 100)}%"
        KeyValue(stringResource(R.string.mind_avgDecision), stringResource(R.string.mind_seconds, "%.1f".format(Locale.ROOT, report.avgDecisionMs / 1000.0)))
        KeyValue(stringResource(R.string.mind_fast), pct(report.fastShare))
        KeyValue(stringResource(R.string.mind_slow), pct(report.slowShare))
        KeyValue(stringResource(R.string.mind_planning), pct(report.planning))
        KeyValue(stringResource(R.string.mind_variation), "%.2f".format(Locale.ROOT, report.variation))
        KeyValue(stringResource(R.string.mind_tightest), report.tightest.toString())
        KeyValue(stringResource(R.string.mind_danger), report.dangerPer100.toString())
        KeyValue(stringResource(R.string.mind_undos), report.undosPer100.toString())
        KeyValue(stringResource(R.string.mind_hints), report.hintsPer100.toString())
        KeyValue(stringResource(R.string.mind_misses), report.missesPer100.toString())
        KeyValue(stringResource(R.string.mind_clearing), pct(report.clearingShare))

        Subtitle(stringResource(R.string.mind_hours))
        Muted(stringResource(R.string.mind_hoursHint))
        BarChart(report.byHour.map { it.index }, report.byHour.map { if (it.hour % 3 == 0) it.hour.toString() else "" }, stringResource(R.string.mind_hours))

        Subtitle(stringResource(R.string.mind_weekdays))
        BarChart(report.byWeekday.map { it.index }, (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }, stringResource(R.string.mind_weekdays))

        Subtitle(stringResource(R.string.mind_sittings))
        Muted(stringResource(R.string.mind_sittingsHint))
        BarChart(
            report.sittings.map { it.index },
            listOf(R.string.mind_sitting1, R.string.mind_sitting2, R.string.mind_sitting3, R.string.mind_sitting4).map { stringResource(it) },
            stringResource(R.string.mind_sittings)
        )

        report.tempo?.let { t ->
            Subtitle(stringResource(R.string.mind_tempo))
            Muted(stringResource(R.string.mind_tempoHint))
            val tb: List<Bucket> = listOf(t.fast, t.mid, t.slow)
            BarChart(tb.map { it.index }, listOf(R.string.mind_tempoFast, R.string.mind_tempoMid, R.string.mind_tempoSlow).map { stringResource(it) }, stringResource(R.string.mind_tempo))
        }

        Subtitle(stringResource(R.string.mind_phases))
        BarChart(listOf(report.phases.early, report.phases.mid, report.phases.late), listOf("1–20", "21–60", "61+"), stringResource(R.string.mind_phases))
    }
}
