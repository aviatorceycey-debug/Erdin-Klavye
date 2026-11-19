package com.erdin.klavye

/**
 * Calculates a long-press popup rectangle without depending on Android Views.
 * Coordinates are pixels in the keyboard view.
 */
internal data class PopupBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

internal object AlternatePopupLayout {
    fun optionIndex(x: Float, y: Float, bounds: PopupBounds, optionCount: Int): Int {
        if (optionCount <= 0 || !x.isFinite() || !y.isFinite() ||
            bounds.width <= 0f || bounds.height <= 0f ||
            x < bounds.left || x >= bounds.right ||
            y < bounds.top || y >= bounds.bottom
        ) return -1
        val cellWidth = bounds.width / optionCount
        return ((x - bounds.left) / cellWidth).toInt().coerceIn(0, optionCount - 1)
    }

    fun calculate(
        viewWidth: Float,
        gap: Float,
        density: Float,
        anchorCenterX: Float,
        anchorTop: Float,
        optionCount: Int
    ): PopupBounds {
        if (!viewWidth.isFinite() || viewWidth <= 0f ||
            !density.isFinite() || density <= 0f ||
            optionCount <= 0 || !anchorCenterX.isFinite() || !anchorTop.isFinite()
        ) return PopupBounds(0f, 0f, 0f, 0f)

        val safeGap = if (gap.isFinite()) gap.coerceAtLeast(0f) else 0f
        val safeInset = minOf(safeGap, viewWidth / 2f)
        val availableWidth = (viewWidth - safeInset * 2f).coerceAtLeast(1f)
        val desiredWidth = 46f * density * optionCount
        val popupWidth = minOf(desiredWidth, availableWidth)
        val leftLimit = (viewWidth - popupWidth - safeInset).coerceAtLeast(safeInset)
        val left = (anchorCenterX - popupWidth / 2f).coerceIn(safeInset, leftLimit)
        val popupHeight = 52f * density
        val top = (anchorTop - popupHeight - 7f * density).coerceAtLeast(3f * density)
        return PopupBounds(left, top, left + popupWidth, top + popupHeight)
    }
}
