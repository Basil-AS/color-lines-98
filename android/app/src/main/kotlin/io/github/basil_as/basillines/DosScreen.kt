package io.github.basil_as.basillines

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.basil_as.basillines.engine.DosButton
import io.github.basil_as.basillines.engine.DosDraw
import io.github.basil_as.basillines.engine.DosImage
import io.github.basil_as.basillines.engine.DosScene
import io.github.basil_as.basillines.engine.DosSprites
import io.github.basil_as.basillines.engine.DosState
import io.github.basil_as.basillines.engine.HallEntry
import io.github.basil_as.basillines.engine.Point
import io.github.basil_as.basillines.engine.SpriteRect
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class DosWindow { NONE, HELP, TOP_TEN }

/** Clock shared by the screen's animations and the effects started by moves. */
fun clockMs(): Long = System.nanoTime() / 1_000_000

private fun DrawScope.drawSprite(bitmap: ImageBitmap, src: SpriteRect, dx: Int, dy: Int, sx: Float, sy: Float) {
    drawImage(
        bitmap,
        srcOffset = IntOffset(src.x, src.y),
        srcSize = IntSize(src.w, src.h),
        dstOffset = IntOffset((dx * sx).roundToInt(), (dy * sy).roundToInt()),
        dstSize = IntSize((src.w * sx).roundToInt().coerceAtLeast(1), (src.h * sy).roundToInt().coerceAtLeast(1)),
        filterQuality = FilterQuality.None
    )
}

/** Pixels of the original screen are 1.37 times taller than wide. */
private const val PIXEL_ASPECT = 480f / 350f

/** The parts of the 640x350 artwork the reflowed screen is built from (scene pixels). */
private object Regions {
    val TOP = SpriteRect(0, 0, 640, 40)
    val BOARD = SpriteRect(165, 55, 318, 228)
    val BUTTONS = SpriteRect(0, 316, 640, 34)

    // Pieces of the top bar and of the key bar, for the landscape arrangement where they sit beside the board.
    val LED_KING = SpriteRect(44, 0, 96, 40)
    val LED_PLAYER = SpriteRect(496, 0, 96, 40)
    val NEXT_BAR = SpriteRect(200, 0, 250, 40)
    fun key(i: Int) = SpriteRect(96 + i * 133, 316, 124, 34)

    // The king's sprite is 72 pixels wide and the pretender's 50. Each region is cut so that, shown at the same width,
    // both heroes appear equally large (88 / 72 = 61 / 50).
    val KING = SpriteRect(42, 66, 88, 92)
    val PRETENDER = SpriteRect(511, 150, 61, 64)
    val KING_FULL = SpriteRect(30, 62, 116, 204)
    // Same height as the king's block when shown at the same width (116 / 204 = 80 / 141), so both captions share a line.
    val PRETENDER_FULL = SpriteRect(501, 150, 80, 141)
}

private fun SpriteRect.heightPerWidth() = h * PIXEL_ASPECT / w

private class DosBitmaps(val layout: ImageBitmap, val sheet: ImageBitmap, val serif: android.graphics.Typeface, val mono: android.graphics.Typeface)

/** Draws the Russian overlay text in scene pixels; letters stay upright (scaled by the horizontal factor only). */
private fun DrawScope.drawDosText(d: DosDraw.Text, bitmaps: DosBitmaps, sx: Float, sy: Float) {
    drawIntoCanvas { c ->
        val n = c.nativeCanvas
        fun paint(argb: Long) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (d.font == io.github.basil_as.basillines.engine.DosFont.SERIF) bitmaps.serif else bitmaps.mono
            color = argb.toInt()
            textSize = d.size.toFloat()
            textAlign = when (d.align) {
                io.github.basil_as.basillines.engine.DosAlign.LEFT -> Paint.Align.LEFT
                io.github.basil_as.basillines.engine.DosAlign.CENTER -> Paint.Align.CENTER
                io.github.basil_as.basillines.engine.DosAlign.RIGHT -> Paint.Align.RIGHT
            }
        }
        n.save()
        n.scale(sx, sx)
        val y = d.y * sy / sx
        d.shadowArgb?.let { n.drawText(d.text, d.x + 1f, y + 1f, paint(it)) }
        n.drawText(d.text, d.x.toFloat(), y, paint(d.argb))
        n.restore()
    }
}

/**
 * One rectangle of the 1992 picture drawn at its own scale, so the board can be as large as the screen allows while the
 * rest of the original screen is arranged around it. [overlay] draws on top, in scene coordinates.
 */
@Composable
private fun SceneRegion(
    region: SpriteRect,
    state: DosState,
    now: androidx.compose.runtime.State<Long>,
    bitmaps: DosBitmaps,
    modifier: Modifier = Modifier,
    overlay: DrawScope.(sx: Float, sy: Float) -> Unit = { _, _ -> },
    content: @Composable BoxScope.(unitX: androidx.compose.ui.unit.Dp, unitY: androidx.compose.ui.unit.Dp) -> Unit = { _, _ -> }
) {
    BoxWithConstraints(modifier.aspectRatio(1f / region.heightPerWidth())) {
        val unitX = maxWidth / region.w
        val unitY = maxHeight / region.h
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / region.w
            val sy = size.height / region.h
            clipRect {
                translate(-region.x * sx, -region.y * sy) {
                    for (d in DosScene.build(state.copy(nowMs = now.value))) {
                        when (d) {
                            is DosDraw.Image -> drawSprite(if (d.img == DosImage.LAYOUT) bitmaps.layout else bitmaps.sheet, d.src, d.dx, d.dy, sx, sy)
                            is DosDraw.Fill -> drawRect(
                                Color(d.argb),
                                topLeft = androidx.compose.ui.geometry.Offset(d.x * sx, d.y * sy),
                                size = androidx.compose.ui.geometry.Size(d.w * sx, d.h * sy)
                            )
                            is DosDraw.Text -> drawDosText(d, bitmaps, sx, sy)
                        }
                    }
                    overlay(sx, sy)
                }
            }
        }
        content(unitX, unitY)
    }
}

private fun DrawScope.drawWindow(bitmaps: DosBitmaps, window: DosWindow, hall: List<HallEntry>, russian: Boolean, sx: Float, sy: Float) {
    if (window == DosWindow.NONE) return
    val r = if (window == DosWindow.HELP) DosSprites.HELP_WINDOW else DosSprites.TOP_TEN_WINDOW
    val (wx, wy) = DosSprites.WINDOW_POS
    drawSprite(bitmaps.sheet, r, wx, wy, sx, sy)
    if (russian) {
        for (d in DosScene.windowText(window == DosWindow.TOP_TEN, true, hall)) when (d) {
            is DosDraw.Fill -> drawRect(Color(d.argb), androidx.compose.ui.geometry.Offset(d.x * sx, d.y * sy), androidx.compose.ui.geometry.Size(d.w * sx, d.h * sy))
            is DosDraw.Text -> drawDosText(d, bitmaps, sx, sy)
            else -> Unit
        }
        return
    }
    if (window == DosWindow.TOP_TEN) {
        hall.take(10).forEachIndexed { i, h ->
            val y = (wy + 31 + i * 11.6f).roundToInt()
            h.name.forEachIndexed { k, ch -> DosSprites.glyphRect(ch)?.let { drawSprite(bitmaps.sheet, it, wx + 34 + k * 9, y, sx, sy) } }
            val score = h.score.toString()
            score.forEachIndexed { k, ch ->
                DosSprites.glyphRect(ch)?.let { drawSprite(bitmaps.sheet, it, wx + 210 - score.length * 9 + k * 9, y, sx, sy) }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        color = Color(0xFFFFFF55),
        fontFamily = FontFamily(Font(R.font.unifraktur_cook)),
        fontSize = 16.sp,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center
    )
}

/**
 * The original Color Lines (1992) screen, reflowed: the board takes the whole width in portrait and the whole height in
 * landscape, and the king and the pretender move next to it (below it in portrait, at the sides in landscape).
 * The captions stay in English like the original.
 */
@Composable
fun DosLayout(
    state: DosState,
    kingName: String,
    pretenderName: String,
    window: DosWindow,
    hall: List<HallEntry>,
    boardCells: @Composable (Modifier) -> Unit,
    onButton: (DosButton) -> Unit,
    onCloseWindow: () -> Unit,
    modeBar: @Composable () -> Unit,
    tools: @Composable (horizontal: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmaps = DosBitmaps(
        ImageBitmap.imageResource(context.resources, R.drawable.cl92_layout),
        ImageBitmap.imageResource(context.resources, R.drawable.cl92_sheet),
        remember { runCatching { context.resources.getFont(R.font.ruslan_display) }.getOrDefault(Typeface.SERIF) },
        Typeface.MONOSPACE
    )
    // The clock is only read while drawing, so a tick redraws the picture without recomposing it.
    val now = remember { mutableLongStateOf(clockMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now.longValue = it }
            delay(45)
        }
    }
    val clock = androidx.compose.runtime.remember { object : androidx.compose.runtime.State<Long> { override val value get() = now.longValue } }

    val labels = mapOf(
        DosButton.HELP to stringResource(R.string.dos_help),
        DosButton.SOUND to stringResource(R.string.dos_sound),
        DosButton.NEXT to stringResource(R.string.dos_next),
        DosButton.RESTART to stringResource(R.string.dos_restart)
    )
    val closeLabel = stringResource(R.string.btn_close)

    @Composable
    fun Top(m: Modifier) = SceneRegion(Regions.TOP, state, clock, bitmaps, m)

    @Composable
    fun Board(m: Modifier) = SceneRegion(
        Regions.BOARD, state, clock, bitmaps, m,
        overlay = { sx, sy -> drawWindow(bitmaps, window, hall, state.russian, sx, sy) }
    ) { ux, uy ->
        if (window == DosWindow.NONE) {
            boardCells(
                Modifier
                    .offset(ux * (DosSprites.BOARD_X - Regions.BOARD.x), uy * (DosSprites.BOARD_Y - Regions.BOARD.y))
                    .size(ux * (DosSprites.CELL_W * 9), uy * (DosSprites.CELL_H * 9))
            )
        } else {
            val (wx, wy) = DosSprites.WINDOW_POS
            Box(
                Modifier
                    .offset(ux * (wx - Regions.BOARD.x), uy * (wy - Regions.BOARD.y))
                    .size(ux * 238, uy * 166)
                    .semantics { contentDescription = closeLabel; role = Role.Button }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCloseWindow)
            )
        }
    }

    // F1..F4: a tap lights the label for a moment, gives a haptic tick and runs the action. The touch area is at least
    // 48 dp tall (the drawn bar is only about 26 dp), split into four equal columns like the four keys.
    var pressedKey by remember { mutableStateOf<DosButton?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    /** One of the four keys: a tap lights its label for a moment, ticks and runs the action. */
    @Composable
    fun KeyTap(button: DosButton, m: Modifier) {
        val i = button.ordinal
        val description = "F${i + 1}: ${labels.getValue(button)}"
        Box(
            m
                .fillMaxHeight()
                .semantics { contentDescription = description; role = Role.Button }
                .pointerInput(button) {
                    detectTapGestures(
                        onPress = {
                            pressedKey = button
                            tryAwaitRelease()
                            scope.launch { delay(160); if (pressedKey == button) pressedKey = null }
                        },
                        onTap = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onButton(button)
                        }
                    )
                }
        )
    }

    @Composable
    fun Buttons(m: Modifier) {
        val shown = state.copy(pressed = pressedKey?.let { setOf(it) } ?: emptySet())
        Box(m.fillMaxWidth().defaultMinSize(minHeight = 48.dp), contentAlignment = Alignment.Center) {
            SceneRegion(Regions.BUTTONS, shown, clock, bitmaps, Modifier.fillMaxWidth())
            Row(Modifier.matchParentSize()) {
                DosButton.entries.forEach { button -> KeyTap(button, Modifier.weight(1f)) }
            }
        }
    }

    /** A single key drawn big enough to press with a finger (landscape: the keys stand in a column beside the board). */
    @Composable
    fun Key(button: DosButton, m: Modifier) {
        val shown = state.copy(pressed = pressedKey?.let { setOf(it) } ?: emptySet())
        Box(m.fillMaxWidth().defaultMinSize(minHeight = 48.dp), contentAlignment = Alignment.Center) {
            SceneRegion(Regions.key(button.ordinal), shown, clock, bitmaps, Modifier.fillMaxWidth())
            KeyTap(button, Modifier.matchParentSize())
        }
    }

    BoxWithConstraints(modifier) {
        val availH = maxHeight
        val portrait = maxHeight >= maxWidth
        if (portrait) {
            val w = maxWidth
            // Board, top bar and key bar take about 1.15 widths; what is left of the height goes to the characters, who stand
            // side by side under the keys with the tool buttons beneath them.
            val used = w * (Regions.TOP.heightPerWidth() + Regions.BOARD.heightPerWidth() + Regions.BUTTONS.heightPerWidth()) + 48.dp + 30.dp
            val left = maxHeight - used - 52.dp - 28.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                Box(Modifier.padding(horizontal = 8.dp)) { modeBar() }
                Top(Modifier.fillMaxWidth())
                Board(Modifier.fillMaxWidth())
                Buttons(Modifier.fillMaxWidth())
                // Both heroes get exactly the same width, so neither looks squeezed.
                val headW = minOf(w * 0.34f, left / Regions.KING.heightPerWidth())
                if (headW >= 56.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.width(headW), horizontalAlignment = Alignment.CenterHorizontally) {
                            SceneRegion(Regions.KING, state, clock, bitmaps, Modifier.width(headW))
                            Caption(kingName)
                        }
                        Column(Modifier.width(headW), horizontalAlignment = Alignment.CenterHorizontally) {
                            SceneRegion(Regions.PRETENDER, state, clock, bitmaps, Modifier.width(headW))
                            Caption(pretenderName)
                        }
                    }
                }
                tools(true)
            }
        } else {
            // Landscape: the board takes the whole height. The score displays and the characters stand at the sides, the
            // next colours under the king, the four keys as a column under the pretender.
            val margin = 3.dp
            val gap = 4.dp
            val toolsW = 52.dp
            val boardW = minOf((maxHeight - margin * 2) / Regions.BOARD.heightPerWidth(), maxWidth * 0.62f)
            val side = ((maxWidth - boardW - toolsW - margin * 2 - gap * 3) / 2).coerceAtLeast(96.dp)
            val ledW = minOf(side, 118.dp)
            val ledH = ledW * Regions.LED_KING.heightPerWidth()
            val captionH = 24.dp
            val nextH = side * Regions.NEXT_BAR.heightPerWidth()
            val keysH = 48.dp * 2 + 4.dp
            val leftFixed = ledH + captionH + nextH + 30.dp + gap * 3
            val rightFixed = ledH + captionH + keysH + gap * 3
            val heroW = minOf(side, (maxHeight - margin * 2 - maxOf(leftFixed, rightFixed)) / Regions.KING_FULL.heightPerWidth()).coerceAtLeast(40.dp)
            Row(Modifier.fillMaxSize().padding(margin), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                Column(Modifier.width(side).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
                    SceneRegion(Regions.LED_KING, state, clock, bitmaps, Modifier.width(ledW))
                    SceneRegion(Regions.KING_FULL, state, clock, bitmaps, Modifier.width(heroW))
                    Caption(kingName)
                    SceneRegion(Regions.NEXT_BAR, state, clock, bitmaps, Modifier.width(side))
                    modeBar()
                }
                Board(Modifier.width(boardW))
                Column(Modifier.width(side).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically)) {
                    SceneRegion(Regions.LED_PLAYER, state, clock, bitmaps, Modifier.width(ledW))
                    SceneRegion(Regions.PRETENDER_FULL, state, clock, bitmaps, Modifier.width(heroW))
                    Caption(pretenderName)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Key(DosButton.HELP, Modifier); Key(DosButton.NEXT, Modifier) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Key(DosButton.SOUND, Modifier); Key(DosButton.RESTART, Modifier) }
                    }
                }
                Box(Modifier.width(toolsW), contentAlignment = Alignment.Center) { tools(false) }
            }
        }
    }
}

/** The 9x9 grid of transparent cell targets for the DOS board. */
@Composable
fun DosBoardCells(snapshot: BoardSnapshot, onCellTap: (Point) -> Unit, modifier: Modifier) {
    Column(modifier) {
        for (row in 0 until BOARD_SIZE) {
            Row(Modifier.weight(1f)) {
                for (col in 0 until BOARD_SIZE) {
                    CellTarget(snapshot, Point(col, row), onCellTap, Modifier.weight(1f))
                }
            }
        }
    }
}
