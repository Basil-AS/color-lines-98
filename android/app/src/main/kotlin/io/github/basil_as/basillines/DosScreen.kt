package io.github.basil_as.basillines

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
    val TOP = SpriteRect(0, 0, 640, 46)
    val BOARD = SpriteRect(165, 55, 318, 228)
    val BUTTONS = SpriteRect(0, 316, 640, 34)
    val KING = SpriteRect(42, 66, 88, 92)
    val KING_FULL = SpriteRect(30, 62, 116, 204)
    val PRETENDER = SpriteRect(500, 150, 80, 74)
    val PRETENDER_FULL = SpriteRect(490, 150, 100, 118)
}

private fun SpriteRect.heightPerWidth() = h * PIXEL_ASPECT / w

private class DosBitmaps(val layout: ImageBitmap, val sheet: ImageBitmap)

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
                        }
                    }
                    overlay(sx, sy)
                }
            }
        }
        content(unitX, unitY)
    }
}

private fun DrawScope.drawWindow(bitmaps: DosBitmaps, window: DosWindow, hall: List<HallEntry>, sx: Float, sy: Float) {
    if (window == DosWindow.NONE) return
    val r = if (window == DosWindow.HELP) DosSprites.HELP_WINDOW else DosSprites.TOP_TEN_WINDOW
    val (wx, wy) = DosSprites.WINDOW_POS
    drawSprite(bitmaps.sheet, r, wx, wy, sx, sy)
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
        fontSize = 20.sp,
        maxLines = 1,
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
        ImageBitmap.imageResource(context.resources, R.drawable.cl92_sheet)
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
        overlay = { sx, sy -> drawWindow(bitmaps, window, hall, sx, sy) }
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

    @Composable
    fun Buttons(m: Modifier) = SceneRegion(Regions.BUTTONS, state, clock, bitmaps, m) { ux, uy ->
        DosButton.entries.forEachIndexed { i, button ->
            val description = "F${i + 1}: ${labels.getValue(button)}"
            Box(
                Modifier
                    .offset(ux * (DosSprites.buttonX(button) - 36 - Regions.BUTTONS.x), uy * (DosSprites.BUTTON_Y - 4 - Regions.BUTTONS.y))
                    .size(ux * 109, uy * 30)
                    .semantics { contentDescription = description; role = Role.Button }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onButton(button) }
            )
        }
    }

    BoxWithConstraints(modifier) {
        val availH = maxHeight
        val portrait = maxHeight >= maxWidth
        if (portrait) {
            val w = maxWidth
            // Board, top bar and key bar take 1.15 widths; what is left of the height goes to the characters.
            val used = w * (Regions.TOP.heightPerWidth() + Regions.BOARD.heightPerWidth() + Regions.BUTTONS.heightPerWidth()) + 28.dp
            val left = maxHeight - used - 56.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                modeBar()
                Top(Modifier.fillMaxWidth())
                Board(Modifier.fillMaxWidth())
                Buttons(Modifier.fillMaxWidth())
                val headW = minOf(w * 0.27f, left / Regions.KING.heightPerWidth() * 0.85f)
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    if (headW >= 64.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SceneRegion(Regions.KING, state, clock, bitmaps, Modifier.width(headW))
                            Caption(kingName)
                        }
                    }
                    tools(true)
                    if (headW >= 64.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SceneRegion(Regions.PRETENDER, state, clock, bitmaps, Modifier.width(headW * 0.85f))
                            Caption(pretenderName)
                        }
                    }
                }
            }
        } else {
            // Landscape: the centre column is as tall as the screen; the characters fill the sides.
            val margin = 4.dp
            val centre = minOf(
                maxWidth * 0.62f,
                (maxHeight - margin * 2 - 26.dp) / (Regions.TOP.heightPerWidth() + Regions.BOARD.heightPerWidth() + Regions.BUTTONS.heightPerWidth())
            )
            val side = (maxWidth - centre - 56.dp - margin * 4) / 2
            Row(Modifier.fillMaxSize().padding(margin), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(margin, Alignment.CenterHorizontally)) {
                Column(Modifier.width(side), horizontalAlignment = Alignment.CenterHorizontally) {
                    SceneRegion(Regions.KING_FULL, state, clock, bitmaps, Modifier.widthIn(max = side).heightIn(max = availH * 0.78f))
                    Caption(kingName)
                }
                Column(Modifier.width(centre), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    modeBar()
                    Top(Modifier.fillMaxWidth())
                    Board(Modifier.fillMaxWidth())
                    Buttons(Modifier.fillMaxWidth())
                }
                Column(Modifier.width(side), horizontalAlignment = Alignment.CenterHorizontally) {
                    SceneRegion(Regions.PRETENDER_FULL, state, clock, bitmaps, Modifier.widthIn(max = side).heightIn(max = availH * 0.6f))
                    Caption(pretenderName)
                }
                tools(false)
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
