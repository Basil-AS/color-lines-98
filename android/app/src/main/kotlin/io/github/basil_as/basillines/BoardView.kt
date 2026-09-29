package io.github.basil_as.basillines

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.basil_as.basillines.engine.BallColor
import io.github.basil_as.basillines.engine.Point

const val BOARD_SIZE = 9

/**
 * An immutable picture of the board. The UI draws from this instead of the live engine, so Compose
 * always sees a change (a new game, an undo, a move) and never shows a stale board.
 */
data class BoardSnapshot(
    val cells: List<BallColor?>,
    val selected: Point?,
    val reachable: Set<Point>,
    /** Cells where the next balls will appear (empty when the preview is switched off). */
    val incoming: Map<Point, BallColor>
) {
    operator fun get(p: Point): BallColor? = cells[p.y * BOARD_SIZE + p.x]
}

@Composable
fun colorLabel(color: BallColor): String = stringResource(
    when (color) {
        BallColor.RED -> R.string.color_red
        BallColor.GREEN -> R.string.color_green
        BallColor.BLUE -> R.string.color_blue
        BallColor.CYAN -> R.string.color_cyan
        BallColor.MAGENTA -> R.string.color_magenta
        BallColor.YELLOW -> R.string.color_yellow
        BallColor.BROWN -> R.string.color_brown
    }
)

@Composable
private fun rememberSprites(palette: Palette): List<ImageBitmap>? {
    if (palette.ballStyle != BallStyle.SPRITE) return null
    val resources = LocalContext.current.resources
    return remember {
        listOf(
            R.drawable.ball_sprite_0, R.drawable.ball_sprite_1, R.drawable.ball_sprite_2, R.drawable.ball_sprite_3,
            R.drawable.ball_sprite_4, R.drawable.ball_sprite_5, R.drawable.ball_sprite_6
        ).map { ImageBitmap.imageResource(resources, it) }
    }
}

@Composable
fun BoardView(
    snapshot: BoardSnapshot,
    palette: Palette,
    onCellTap: (Point) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val transition = rememberInfiniteTransition(label = "selectionPulse")
    val animatedScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulse = if (animationsEnabled) animatedScale else 1f
    val sprites = rememberSprites(palette)
    val boardLabel = stringResource(R.string.board_label)
    val shape = if (palette.square) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp)
    val framePadding = if (palette.square) 3.dp else 4.dp

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.boardBackground)
            .padding(framePadding)
            .semantics { contentDescription = boardLabel }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cellSize = size.width / BOARD_SIZE
            for (row in 0 until BOARD_SIZE) {
                for (col in 0 until BOARD_SIZE) {
                    val point = Point(col, row)
                    val left = col * cellSize
                    val top = row * cellSize
                    drawCell(palette, left, top, cellSize)

                    val center = Offset(left + cellSize / 2f, top + cellSize / 2f)
                    val ball = snapshot[point]
                    if (ball == null) {
                        if (point in snapshot.reachable) {
                            drawCircle(palette.reachableDot, radius = cellSize * 0.09f, center = center)
                        }
                        snapshot.incoming[point]?.let { drawBall(palette, sprites, it, center, cellSize * 0.17f) }
                    } else {
                        val radius = cellSize * 0.38f * if (snapshot.selected == point) pulse else 1f
                        drawBall(palette, sprites, ball, center, radius, shadow = true)
                    }
                }
            }
        }

        // One accessible, focusable target per cell on top of the drawing.
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until BOARD_SIZE) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until BOARD_SIZE) {
                        CellTarget(snapshot, Point(col, row), onCellTap, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCell(palette: Palette, left: Float, top: Float, cell: Float) {
    when (palette.cellStyle) {
        CellStyle.ROUNDED -> {
            val pad = 2f
            drawRoundRect(
                palette.cell, Offset(left + pad, top + pad), Size(cell - pad * 2, cell - pad * 2),
                CornerRadius(cell * 0.16f)
            )
        }
        CellStyle.WIN98, CellStyle.DOS -> {
            val light = Color.White
            val dark = if (palette.cellStyle == CellStyle.WIN98) Color(0xFF808080) else Color(0xFF555555)
            val b = maxOf(2f, cell * 0.06f)
            drawRect(palette.cell, Offset(left, top), Size(cell, cell))
            // Raised bevel: light top and left edges, dark bottom and right edges.
            drawRect(light, Offset(left, top), Size(cell, b))
            drawRect(light, Offset(left, top), Size(b, cell))
            drawRect(dark, Offset(left, top + cell - b), Size(cell, b))
            drawRect(dark, Offset(left + cell - b, top), Size(b, cell))
        }
    }
}

private fun DrawScope.drawBall(
    palette: Palette,
    sprites: List<ImageBitmap>?,
    ball: BallColor,
    center: Offset,
    radius: Float,
    shadow: Boolean = false
) {
    when (palette.ballStyle) {
        BallStyle.SPRITE -> {
            val bitmap = sprites?.get(spriteIndex(ball))
            if (bitmap != null) {
                val d = (radius * 2.4f).toInt().coerceAtLeast(2)
                drawImage(
                    bitmap,
                    dstOffset = IntOffset((center.x - d / 2f).toInt(), (center.y - d / 2f).toInt()),
                    dstSize = IntSize(d, d),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None
                )
            }
        }
        BallStyle.DOS -> {
            val (hi, mid, lo) = dosBands(ball)
            if (shadow) drawCircle(Color.Black.copy(alpha = 0.55f), radius, center + Offset(radius * 0.12f, radius * 0.12f))
            drawCircle(Color.Black, radius + 1.5f, center)
            drawCircle(lo, radius, center)
            drawCircle(mid, radius * 0.86f, center + Offset(-radius * 0.06f, -radius * 0.06f))
            drawCircle(hi, radius * 0.55f, center + Offset(-radius * 0.22f, -radius * 0.24f))
            drawCircle(Color.White, radius * 0.2f, center + Offset(-radius * 0.32f, -radius * 0.36f))
        }
        BallStyle.GLOSSY -> {
            val base = ballColor(ball)
            if (shadow) {
                drawCircle(Color.Black.copy(alpha = 0.3f), radius * 0.95f, Offset(center.x, center.y + radius * 0.15f))
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), base, base.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.4f)),
                    center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = center
            )
        }
    }
}

@Composable
private fun CellTarget(
    snapshot: BoardSnapshot,
    point: Point,
    onTap: (Point) -> Unit,
    modifier: Modifier
) {
    val ball = snapshot[point]
    val coming = if (ball == null) snapshot.incoming[point] else null
    val isSelected = snapshot.selected == point
    val content = if (ball == null) stringResource(R.string.cell_empty)
    else stringResource(R.string.cell_ball, colorLabel(ball))
    val state = (if (isSelected) stringResource(R.string.cell_selected) else "") +
        (if (ball == null && point in snapshot.reachable) stringResource(R.string.cell_reachable) else "") +
        (if (coming != null) stringResource(R.string.cell_incoming, colorLabel(coming)) else "")
    val description = stringResource(R.string.cell_description, point.y + 1, point.x + 1, content, state)

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = description
                role = Role.Button
                selected = isSelected
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap(point) }
    )
}

