package io.github.basil_as.basillines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
    return if (id != 0) context.getString(id) else name
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
private fun LookPreview(theme: AppTheme, overrides: LookOverrides, custom: CustomTheme, vision: Vision) {
    val palette = resolvePalette(theme, overrides, custom)
    val sprites = rememberSprites(palette)
    val measurer = rememberTextMeasurer()
    val filter = visionFilter(vision)
    val label = stringResource(R.string.look_preview)
    Canvas(
        Modifier
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
    titleKey: String,
    hintKey: String?,
    options: List<T>,
    current: T?,
    effective: T,
    idOf: (T) -> String,
    onPick: (T?) -> Unit,
    dots: ((T) -> List<Int>)? = null
) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(lookText(titleKey), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = current == null,
                onClick = { onPick(null) },
                label = { Text(lookText("look_followTheme") + " · " + lookText("${titleKey}_${idOf(effective)}")) },
                modifier = Modifier.semantics { role = Role.RadioButton }
            )
            options.forEach { option ->
                FilterChip(
                    selected = current == option,
                    onClick = { onPick(option) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            dots?.invoke(option)?.forEach { c ->
                                Box(Modifier.padding(end = 2.dp).size(9.dp).clip(RoundedCornerShape(50)).background(Color(c)))
                            }
                            Text(lookText("${titleKey}_${idOf(option)}"))
                        }
                    },
                    modifier = Modifier.semantics { role = Role.RadioButton }
                )
            }
        }
        if (hintKey != null) Text(lookText(hintKey), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One colour of the builder: a swatch and its hex value; tapping it opens the hex field and the sliders. */
@Composable
private fun ColorRow(label: String, value: Int, onChange: (Int) -> Unit) {
    var open by rememberSaveable(label) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { open = !open }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(value))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            Text(label, Modifier.weight(1f).padding(horizontal = 12.dp))
            Text(Colors.hex(value), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (open) {
            var text by remember(value) { mutableStateOf(Colors.hex(value)) }
            OutlinedTextField(
                value = text,
                onValueChange = { typed ->
                    text = typed
                    Colors.argb(typed)?.let(onChange)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            listOf("R" to Colors.red(value), "G" to Colors.green(value), "B" to Colors.blue(value)).forEachIndexed { index, (name, channel) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.width(20.dp))
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
                        valueRange = 0f..255f
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuilderTab(
    theme: AppTheme,
    overrides: LookOverrides,
    custom: CustomTheme,
    onCustom: (CustomTheme) -> Unit,
    onUseCustom: () -> Unit,
    onApplyShare: (LookShare) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val share = LookCode.encode(LookShare(theme.webId, overrides, custom))
    var pasted by rememberSaveable { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val badCode = stringResource(R.string.share_bad)
    val applied = stringResource(R.string.share_applied)
    val copied = stringResource(R.string.share_copied)
    val warnings = ballWarnings(custom.balls, custom.cell)

    Column(Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.builder_intro), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.builder_start), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onCustom(CustomTheme.preset(false)) }) { Text(stringResource(R.string.builder_startLight)) }
            OutlinedButton(onClick = { onCustom(CustomTheme.preset(true)) }) { Text(stringResource(R.string.builder_startDark)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.builder_startDark), Modifier.weight(1f))
            Switch(checked = custom.dark, onCheckedChange = { onCustom(custom.copy(dark = it)) })
        }
        ColorRow(stringResource(R.string.builder_page), custom.page) { onCustom(custom.copy(page = it)) }
        ColorRow(stringResource(R.string.builder_board), custom.board) { onCustom(custom.copy(board = it)) }
        ColorRow(stringResource(R.string.builder_cell), custom.cell) { onCustom(custom.copy(cell = it)) }
        ColorRow(stringResource(R.string.builder_accent), custom.accent) { onCustom(custom.copy(accent = it)) }
        Text(stringResource(R.string.builder_balls), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        BallColor.entries.forEach { ball ->
            ColorRow(colorLabel(ball), custom.balls.getValue(ball)) { onCustom(custom.copy(balls = custom.balls + (ball to it))) }
        }
        warnings.similar.forEach { (a, b) ->
            Text(stringResource(R.string.builder_warnSimilar, colorLabel(a), colorLabel(b)), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        warnings.faint.forEach { c ->
            Text(stringResource(R.string.builder_warnCell, colorLabel(c)), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        if (warnings.isClean) Text(stringResource(R.string.builder_good), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = onUseCustom, enabled = theme != AppTheme.CUSTOM, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(if (theme == AppTheme.CUSTOM) R.string.builder_active else R.string.builder_use))
        }

        Text(stringResource(R.string.share_title), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        Text(stringResource(R.string.share_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(share, fontSize = 11.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(share)); note = copied }) { Text(stringResource(R.string.share_copy)) }
        OutlinedTextField(
            value = pasted,
            onValueChange = { pasted = it },
            placeholder = { Text(stringResource(R.string.share_placeholder)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            minLines = 2,
            maxLines = 3
        )
        OutlinedButton(
            enabled = pasted.isNotBlank(),
            onClick = {
                val decoded = LookCode.decode(pasted)
                if (decoded == null) note = badCode else { onApplyShare(decoded); pasted = ""; note = applied }
            }
        ) { Text(stringResource(R.string.share_apply)) }
        if (note.isNotEmpty()) Text(note, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

private enum class StudioTab(val key: String) { BALLS("settings_tab_balls"), BOARD("settings_tab_board"), BUILDER("settings_tab_builder") }

/** Depth, colours, rings, markers, size and grid for the current theme, and the builder for a look of your own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookStudioDialog(
    theme: AppTheme,
    overrides: LookOverrides,
    custom: CustomTheme,
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

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.look_studio)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LookPreview(theme, overrides, custom, vision)
                FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Vision.entries.forEach { v ->
                        FilterChip(
                            selected = vision == v,
                            onClick = { vision = v },
                            label = { Text(lookText("look_vision_${v.id}"), fontSize = 12.sp) },
                            modifier = Modifier.semantics { role = Role.RadioButton }
                        )
                    }
                }
                FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioTab.entries.forEach { t ->
                        FilterChip(
                            selected = tab == t,
                            onClick = { tab = t },
                            label = { Text(lookText(t.key)) },
                            modifier = Modifier.semantics { role = Role.RadioButton }
                        )
                    }
                }
                if (tab == StudioTab.BUILDER) {
                    BuilderTab(theme, overrides, custom, onCustom, onUseCustom, onApplyShare)
                } else if (!studio) {
                    Text(stringResource(R.string.look_retroNote), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                } else if (tab == StudioTab.BALLS) {
                    ChoiceGroup("look_palette", "look_paletteHint", PaletteId.entries.toList(), overrides.palette, effective.palette, { it.id }, { onOverrides(overrides.copy(palette = it)) }) {
                        if (it == PaletteId.CUSTOM) emptyList() else BallColor.entries.map { c -> LookPalettes.SETS.getValue(it).getValue(c) }
                    }
                    ChoiceGroup("look_volume", null, Volume.entries.toList(), overrides.volume, effective.volume, { it.id }, { onOverrides(overrides.copy(volume = it)) })
                    ChoiceGroup("look_contrast", "look_contrastHint", RingLevel.entries.toList(), overrides.ring, effective.ring, { it.id }, { onOverrides(overrides.copy(ring = it)) })
                    ChoiceGroup("look_shapes", "look_shapesHint", ShapeSet.entries.toList(), overrides.shapes, effective.shapes, { it.id }, { onOverrides(overrides.copy(shapes = it)) })
                    ChoiceGroup("look_ballSize", null, BallSize.entries.toList(), overrides.ballSize, effective.ballSize, { it.id }, { onOverrides(overrides.copy(ballSize = it)) })
                } else {
                    ChoiceGroup("look_grid", null, GridStyle.entries.toList(), overrides.grid, effective.grid, { it.id }, { onOverrides(overrides.copy(grid = it)) })
                    ChoiceGroup("look_cells", null, CellShape.entries.toList(), overrides.cells, effective.cells, { it.id }, { onOverrides(overrides.copy(cells = it)) })
                }
                if (tab != StudioTab.BUILDER && studio) {
                    TextButton(onClick = { onOverrides(LookOverrides()) }, enabled = !overrides.isEmpty, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.look_reset))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.btn_close)) } }
    )
}
