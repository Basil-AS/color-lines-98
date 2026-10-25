package io.github.basil_as.basillines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.BallSize
import io.github.basil_as.basillines.engine.CellShape
import io.github.basil_as.basillines.engine.Colors
import io.github.basil_as.basillines.engine.CustomTheme
import io.github.basil_as.basillines.engine.GridStyle
import io.github.basil_as.basillines.engine.LookCode
import io.github.basil_as.basillines.engine.LookOverrides
import io.github.basil_as.basillines.engine.LookPalettes
import io.github.basil_as.basillines.engine.LookShare
import io.github.basil_as.basillines.engine.LookSpec
import io.github.basil_as.basillines.engine.PaletteId
import io.github.basil_as.basillines.engine.RingLevel
import io.github.basil_as.basillines.engine.ShapeSet
import io.github.basil_as.basillines.engine.Volume
import io.github.basil_as.basillines.engine.ballWarnings
import io.github.basil_as.basillines.engine.colors

/** A string from the generated resources by name; the look studio's labels follow one naming scheme. */
@Composable
internal fun lookText(name: String): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(name, "string", context.packageName)
    return if (id != 0) androidx.compose.ui.res.stringResource(id) else name
}

/** Colour-vision simulations (Vienot matrices), so a look can be checked for balls that merge. */
private enum class Vision(val id: String) { NORMAL("normal"), GREY("grey"), PROTAN("protan"), DEUTAN("deutan"), TRITAN("tritan") }

private fun visionFilter(vision: Vision): ColorFilter? = when (vision) {
    Vision.NORMAL -> null
    Vision.GREY -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    Vision.PROTAN -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.567f, 0.433f, 0f, 0f, 0f, 0.558f, 0.442f, 0f, 0f, 0f, 0f, 0.242f, 0.758f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
    Vision.DEUTAN -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.625f, 0.375f, 0f, 0f, 0f, 0.7f, 0.3f, 0f, 0f, 0f, 0f, 0.3f, 0.7f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
    Vision.TRITAN -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(0.95f, 0.05f, 0f, 0f, 0f, 0f, 0.433f, 0.567f, 0f, 0f, 0f, 0.475f, 0.525f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
}

/** A small live board drawn by the real styles: seven balls, a marker for the next ball and a reachable cell. */
@Composable
private fun LookPreview(theme: AppTheme, overrides: LookOverrides, custom: CustomTheme, vision: Vision, night: Boolean, modifier: Modifier = Modifier) {
    val palette = resolvePalette(theme, overrides, custom, night)
    val sprites = rememberSprites(palette)
    val measurer = rememberTextMeasurer()
    val filter = visionFilter(vision)
    val label = stringResource(R.string.look_preview)
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(3.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.boardBackground)
            .semantics { contentDescription = label }
            .drawWithContent {
                if (filter == null) {
                    drawContent()
                } else {
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { colorFilter = filter })
                        drawContent()
                        canvas.restore()
                    }
                }
            }
    ) {
        val cell = size.width / 7f
        for (row in 0 until 2) for (col in 0 until 7) drawCell(palette, col * cell, row * cell, cell, col, row)
        BallColor.entries.forEachIndexed { i, ball ->
            drawBall(palette, sprites, ball, Offset((i + 0.5f) * cell, 0.5f * cell), cell * 0.38f, shadow = true, measurer = measurer)
        }
        drawCircle(palette.reachableDot, cell * 0.09f, Offset(1.5f * cell, 1.5f * cell))
        drawBall(palette, sprites, BallColor.GREEN, Offset(2.5f * cell, 1.5f * cell), cell * 0.17f, marker = false)
        drawBall(palette, sprites, BallColor.YELLOW, Offset(3.5f * cell, 1.5f * cell), cell * 0.38f, shadow = true, measurer = measurer)
    }
}

/** One parameter: its name, what the theme does when nothing is chosen, and the options as chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    titleKey: String,
    options: List<T>,
    current: T?,
    effective: T,
    idOf: (T) -> String,
    onPick: (T) -> Unit,
    dots: ((T) -> List<Int>)? = null
) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(lookText(titleKey), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (current == null) {
                Text(
                    "· " + lookText("look_followTheme"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = effective == option,
                    onClick = { onPick(option) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            dots?.invoke(option)?.forEach { c ->
                                Box(Modifier.padding(end = 1.dp).size(6.dp).clip(RoundedCornerShape(50)).background(Color(c)))
                            }
                            Text(lookText("${titleKey}_${idOf(option)}"), fontSize = 13.sp)
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
                )
            }
        }
    }
}

/** The colours of the custom theme the builder edits, by name. */
private enum class Slot(val key: String) {
    PAGE("builder_page"), BOARD("builder_board"), CELL("builder_cell"), ACCENT("builder_accent"),
    RED("color_red"), GREEN("color_green"), BLUE("color_blue"), CYAN("color_cyan"), MAGENTA("color_magenta"), YELLOW("color_yellow"), BROWN("color_brown")
}

private fun CustomTheme.colorOf(slot: Slot): Int = when (slot) {
    Slot.PAGE -> page
    Slot.BOARD -> board
    Slot.CELL -> cell
    Slot.ACCENT -> accent
    Slot.RED -> balls.getValue(BallColor.RED)
    Slot.GREEN -> balls.getValue(BallColor.GREEN)
    Slot.BLUE -> balls.getValue(BallColor.BLUE)
    Slot.CYAN -> balls.getValue(BallColor.CYAN)
    Slot.MAGENTA -> balls.getValue(BallColor.MAGENTA)
    Slot.YELLOW -> balls.getValue(BallColor.YELLOW)
    Slot.BROWN -> balls.getValue(BallColor.BROWN)
}

private fun CustomTheme.withColor(slot: Slot, color: Int): CustomTheme = when (slot) {
    Slot.PAGE -> copy(page = color)
    Slot.BOARD -> copy(board = color)
    Slot.CELL -> copy(cell = color)
    Slot.ACCENT -> copy(accent = color)
    Slot.RED -> copy(balls = balls + (BallColor.RED to color))
    Slot.GREEN -> copy(balls = balls + (BallColor.GREEN to color))
    Slot.BLUE -> copy(balls = balls + (BallColor.BLUE to color))
    Slot.CYAN -> copy(balls = balls + (BallColor.CYAN to color))
    Slot.MAGENTA -> copy(balls = balls + (BallColor.MAGENTA to color))
    Slot.YELLOW -> copy(balls = balls + (BallColor.YELLOW to color))
    Slot.BROWN -> copy(balls = balls + (BallColor.BROWN to color))
}

/** One editor for the colour picked from the tiles: its value and three sliders. */
@Composable
private fun ColorEditor(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(value))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            Text(label, Modifier.padding(start = 10.dp).weight(1f), fontWeight = FontWeight.SemiBold)
            Text(Colors.hex(value), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        listOf("R" to Colors.red(value), "G" to Colors.green(value), "B" to Colors.blue(value)).forEachIndexed { index, (name, channel) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.widthIn(min = 18.dp), fontSize = 12.sp)
                Slider(
                    value = channel.toFloat(),
                    onValueChange = { v ->
                        val n = v.toInt()
                        onChange(
                            when (index) {
                                0 -> Colors.rgb(n, Colors.green(value), Colors.blue(value))
                                1 -> Colors.rgb(Colors.red(value), n, Colors.blue(value))
                                else -> Colors.rgb(Colors.red(value), Colors.green(value), n)
                            }
                        )
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "$label $name" }
                )
                Text(channel.toString(), Modifier.widthIn(min = 30.dp), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

/** The builder: surface and colours as tiles with one editor, warnings, the code to share. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuilderTab(
    theme: AppTheme,
    overrides: LookOverrides,
    custom: CustomTheme,
    onCustom: (CustomTheme) -> Unit,
    onApplyShare: (LookShare) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var note by remember { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(Slot.PAGE) }
    val badCode = stringResource(R.string.share_bad)
    val applied = stringResource(R.string.share_applied)
    val copied = stringResource(R.string.share_copied)
    val warnings = ballWarnings(custom.balls, custom.cell)

    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(stringResource(R.string.builder_start), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onCustom(CustomTheme.preset(false)) }) { Text(stringResource(R.string.builder_startLight)) }
            OutlinedButton(onClick = { onCustom(CustomTheme.preset(true)) }) { Text(stringResource(R.string.builder_startDark)) }
            OutlinedButton(onClick = { onCustom(CustomTheme.preset(custom.dark)) }, enabled = custom != CustomTheme.preset(custom.dark)) { Text(stringResource(R.string.builder_reset)) }
        }
        Text(stringResource(R.string.builder_mode), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = !custom.dark, onClick = { onCustom(custom.copy(dark = false)) }, label = { Text(stringResource(R.string.builder_startLight)) }, modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton })
            FilterChip(selected = custom.dark, onClick = { onCustom(custom.copy(dark = true)) }, label = { Text(stringResource(R.string.builder_startDark)) }, modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton })
        }
        Text(stringResource(R.string.builder_colors), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Slot.entries.forEach { slot ->
                FilterChip(
                    selected = picked == slot,
                    onClick = { picked = slot },
                    label = { Text(lookText(slot.key), fontSize = 13.sp) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(custom.colorOf(slot)))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
                )
            }
        }
        ColorEditor(lookText(picked.key), custom.colorOf(picked)) { onCustom(custom.withColor(picked, it)) }

        warnings.similar.forEach { (a, b) ->
            Text(stringResource(R.string.builder_warnSimilar, colorLabel(a), colorLabel(b)), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        warnings.faint.forEach { c ->
            Text(stringResource(R.string.builder_warnCell, colorLabel(c)), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        if (warnings.isClean) Text(stringResource(R.string.builder_good), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Text(stringResource(R.string.share_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(LookCode.encode(LookShare(theme.webId, overrides, custom)))); note = copied }) { Text(stringResource(R.string.share_copy)) }
            // The code is read from the clipboard: nobody types four hundred characters on a phone.
            OutlinedButton(
                onClick = {
                    val decoded = clipboard.getText()?.text?.let { LookCode.decode(it) }
                    if (decoded == null) note = badCode else { onApplyShare(decoded); note = applied }
                }
            ) { Text(stringResource(R.string.share_apply)) }
        }
        if (note.isNotEmpty()) Text(note, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private enum class StudioTab(val key: String) { BALLS("settings_tab_balls"), BOARD("settings_tab_board"), BUILDER("settings_tab_builder") }

/**
 * Depth, colours, rings, markers, size and grid for the current theme, and the builder for a look of your own.
 * The close action stays visible; preview, editing and sharing use the available safe height.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookStudioDialog(
    theme: AppTheme,
    overrides: LookOverrides,
    custom: CustomTheme,
    night: Boolean,
    onOverrides: (LookOverrides) -> Unit,
    onCustom: (CustomTheme) -> Unit,
    onUseCustom: () -> Unit,
    onApplyShare: (LookShare) -> Unit,
    onClose: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(StudioTab.BALLS) }
    var vision by rememberSaveable { mutableStateOf(Vision.NORMAL) }
    val id = theme.webId
    val studio = LookSpec.isStudioTheme(id)
    val effective = LookSpec.effective(id, overrides)

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp)) {
                val compact = maxHeight < 640.dp * LocalDensity.current.fontScale
                Column(Modifier.fillMaxSize()) {
                    // Dismissal stays in the safe header while editing actions can scroll.
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.look_studio), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Button(onClick = onClose) { Text(stringResource(R.string.btn_close), fontSize = 13.sp) }
                    }
                    // The preview joins the editor when the remaining safe height is short.
                    val header: @Composable ColumnScope.() -> Unit = {
                        LookPreview(theme, overrides, custom, vision, night, Modifier.widthIn(max = 320.dp).align(Alignment.CenterHorizontally).padding(top = 6.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Vision.entries.forEach { v ->
                                FilterChip(
                                    selected = vision == v,
                                    onClick = { vision = v },
                                    label = { Text(lookText("look_vision_${v.id}"), fontSize = 12.sp) },
                                    modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
                                )
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StudioTab.entries.forEach { t ->
                                FilterChip(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    label = { Text(lookText(t.key)) },
                                    modifier = Modifier.heightIn(min = 48.dp).semantics { role = Role.RadioButton }
                                )
                            }
                        }
                    }
                    if (!compact) header()
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                        if (compact) header()
                        if (tab == StudioTab.BUILDER) {
                            Button(onClick = onUseCustom, enabled = theme != AppTheme.CUSTOM, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(stringResource(if (theme == AppTheme.CUSTOM) R.string.builder_active else R.string.builder_use))
                            }
                        } else {
                            OutlinedButton(onClick = { onOverrides(LookOverrides()) }, enabled = studio && !overrides.isEmpty, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(stringResource(R.string.look_reset))
                            }
                        }
                        if (tab == StudioTab.BUILDER) {
                            BuilderTab(theme, overrides, custom, onCustom, onApplyShare)
                        } else if (!studio) {
                            Text(stringResource(R.string.look_retroNote), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                        } else if (tab == StudioTab.BALLS) {
                            ChoiceGroup("look_palette", PaletteId.entries.toList(), overrides.palette, effective.palette, { it.id }, { onOverrides(overrides.copy(palette = it)) }) {
                                if (it == PaletteId.CUSTOM) emptyList() else BallColor.entries.map { c -> LookPalettes.SETS.getValue(it).getValue(c) }
                            }
                            ChoiceGroup("look_volume", Volume.entries.toList(), overrides.volume, effective.volume, { it.id }, { onOverrides(overrides.copy(volume = it)) })
                            ChoiceGroup("look_contrast", RingLevel.entries.toList(), overrides.ring, effective.ring, { it.id }, { onOverrides(overrides.copy(ring = it)) })
                            ChoiceGroup("look_shapes", ShapeSet.entries.toList(), overrides.shapes, effective.shapes, { it.id }, { onOverrides(overrides.copy(shapes = it)) })
                            ChoiceGroup("look_ballSize", BallSize.entries.toList(), overrides.ballSize, effective.ballSize, { it.id }, { onOverrides(overrides.copy(ballSize = it)) })
                        } else {
                            ChoiceGroup("look_grid", GridStyle.entries.toList(), overrides.grid, effective.grid, { it.id }, { onOverrides(overrides.copy(grid = it)) })
                            ChoiceGroup("look_cells", CellShape.entries.toList(), overrides.cells, effective.cells, { it.id }, { onOverrides(overrides.copy(cells = it)) })
                        }
                    }
                }
            }
        }
    }
}
