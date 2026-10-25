package io.github.basil_as.basillines

import kotlin.math.min

/** Geometry in dp inside the safe viewport; chrome may scroll, the square board never does. */
internal data class AdaptiveGameLayout(
    val sideBySide: Boolean,
    val boardSize: Float,
    val panelWidth: Float,
    val padding: Float,
    val gap: Float
)

/** Choose the composition that gives the board more room while reserving readable supporting controls. */
internal fun adaptiveGameLayout(
    width: Float,
    height: Float,
    fontScale: Float = 1f,
    headerHeight: Float = 112f * fontScale,
    dockHeight: Float = 56f
): AdaptiveGameLayout {
    fun extent(value: Float) = value.takeIf { it.isFinite() && it > 0f } ?: 0f
    val w = extent(width)
    val h = extent(height)
    val scale = fontScale.takeIf { it.isFinite() }?.coerceIn(1f, 3f) ?: 1f
    val stackPadding = min(12f, min(w, h) / 2f)
    val gap = 12f
    val stackedBoard = min(
        (w - 2 * stackPadding).coerceAtLeast(0f),
        (h - 2 * stackPadding - extent(headerHeight) - extent(dockHeight) - 2 * gap).coerceAtLeast(0f)
    )
    val sidePadding = min(8f, min(w, h) / 2f)
    val minimumPanel = 176f * scale
    val sideBoard = min(
        (h - 2 * sidePadding).coerceAtLeast(0f),
        (w - 2 * sidePadding - gap - minimumPanel).coerceAtLeast(0f)
    )
    if (w > h && sideBoard > stackedBoard && sideBoard >= 96f) {
        val panel = min(232f * scale, w - 2 * sidePadding - gap - sideBoard)
        return AdaptiveGameLayout(true, sideBoard, panel.coerceAtLeast(0f), sidePadding, gap)
    }
    return AdaptiveGameLayout(false, stackedBoard, 0f, stackPadding, gap)
}
