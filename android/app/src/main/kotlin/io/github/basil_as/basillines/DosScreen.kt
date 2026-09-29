package io.github.basil_as.basillines

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
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

/**
 * The original Color Lines (1992) screen: 640x350 artwork stretched to 4:3 like DOS pixels. It is only
 * a picture; the accessible controls sit on top of it.
 */
@Composable
fun DosScreen(
    state: DosState,
    kingName: String,
    pretenderName: String,
    window: DosWindow,
    hall: List<HallEntry>,
    boardCells: @Composable (Modifier) -> Unit,
    onButton: (DosButton) -> Unit,
    onCloseWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layout = ImageBitmap.imageResource(context.resources, R.drawable.cl92_layout)
    val sheet = ImageBitmap.imageResource(context.resources, R.drawable.cl92_sheet)
    val typeface = remember { runCatching { context.resources.getFont(R.font.unifraktur_cook) }.getOrDefault(Typeface.SERIF) }
    val paint = remember(typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = android.graphics.Color.rgb(255, 255, 85)
            textAlign = Paint.Align.CENTER
            textSize = 22f
        }
    }

    // The clock is only read while drawing, so a tick redraws the picture without recomposing it.
    // withInfiniteAnimationFrameMillis is the API for endless animations: tests do not wait for it.
    val now = remember { mutableLongStateOf(clockMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now.longValue = it }
            delay(45)
        }
    }

    BoxWithConstraints(modifier) {
        val unitX = maxWidth / DosSprites.SCREEN_W
        val unitY = maxHeight / DosSprites.SCREEN_H
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / DosSprites.SCREEN_W
            val sy = size.height / DosSprites.SCREEN_H
            for (d in DosScene.build(state.copy(nowMs = now.longValue))) {
                when (d) {
                    is DosDraw.Image -> drawSprite(if (d.img == DosImage.LAYOUT) layout else sheet, d.src, d.dx, d.dy, sx, sy)
                    is DosDraw.Fill -> drawRect(
                        Color(d.argb),
                        topLeft = androidx.compose.ui.geometry.Offset(d.x * sx, d.y * sy),
                        size = androidx.compose.ui.geometry.Size(d.w * sx, d.h * sy)
                    )
                }
            }
            drawIntoCanvas { c ->
                // Letters stay upright: scale by the horizontal factor only.
                val n = c.nativeCanvas
                n.save()
                n.scale(sx, sx)
                n.drawText(kingName, 88f, 262f * sy / sx, paint)
                n.drawText(pretenderName, 541f, 262f * sy / sx, paint)
                n.restore()
            }
            if (window != DosWindow.NONE) {
                val r = if (window == DosWindow.HELP) DosSprites.HELP_WINDOW else DosSprites.TOP_TEN_WINDOW
                val (wx, wy) = DosSprites.WINDOW_POS
                drawSprite(sheet, r, wx, wy, sx, sy)
                if (window == DosWindow.TOP_TEN) {
                    hall.take(10).forEachIndexed { i, h ->
                        val y = (wy + 31 + i * 11.6f).roundToInt()
                        h.name.forEachIndexed { k, ch ->
                            DosSprites.glyphRect(ch)?.let { drawSprite(sheet, it, wx + 34 + k * 9, y, sx, sy) }
                        }
                        val score = h.score.toString()
                        score.forEachIndexed { k, ch ->
                            DosSprites.glyphRect(ch)?.let { drawSprite(sheet, it, wx + 210 - score.length * 9 + k * 9, y, sx, sy) }
                        }
                    }
                }
            }
        }

        // One accessible, focusable target per cell on top of the board.
        if (window == DosWindow.NONE) {
            boardCells(
                Modifier
                    .offset(unitX * DosSprites.BOARD_X, unitY * DosSprites.BOARD_Y)
                    .size(unitX * (DosSprites.CELL_W * 9), unitY * (DosSprites.CELL_H * 9))
            )
        }

        val labels = mapOf(
            DosButton.HELP to stringResource(R.string.dos_help),
            DosButton.SOUND to stringResource(R.string.dos_sound),
            DosButton.NEXT to stringResource(R.string.dos_next),
            DosButton.RESTART to stringResource(R.string.dos_restart)
        )
        DosButton.entries.forEachIndexed { i, button ->
            val description = "F${i + 1}: ${labels.getValue(button)}"
            Box(
                Modifier
                    .offset(unitX * (DosSprites.buttonX(button) - 36), unitY * (DosSprites.BUTTON_Y - 4))
                    .size(unitX * 109, unitY * 22)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onButton(button) }
            )
        }
        if (window != DosWindow.NONE) {
            val (wx, wy) = DosSprites.WINDOW_POS
            val closeLabel = stringResource(R.string.btn_close)
            Box(
                Modifier
                    .offset(unitX * wx, unitY * wy)
                    .size(unitX * 238, unitY * 166)
                    .semantics {
                        contentDescription = closeLabel
                        role = Role.Button
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCloseWindow)
            )
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
