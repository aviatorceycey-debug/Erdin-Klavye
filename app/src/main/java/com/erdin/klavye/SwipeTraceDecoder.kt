package com.erdin.klavye

import java.util.Locale
import kotlin.math.hypot

/**
 * Geometry-only key representation used to decode a swipe without depending
 * on Android drawing classes.
 */
internal data class SwipeKeyBounds(
    val value: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/** Converts a sampled finger path into a stable sequence of keyboard letters. */
internal object SwipeTraceDecoder {
    private data class CenteredKey(
        val value: String,
        val centerX: Double,
        val centerY: Double
    )
    fun decode(
        path: List<Pair<Float, Float>>,
        keys: List<SwipeKeyBounds>,
        density: Float,
        english: Boolean
    ): String {
        if (path.isEmpty() || keys.isEmpty() || !density.isFinite() || density <= 0f) return ""
        val validKeys = keys.filter { key ->
            key.value.length == 1 && key.value[0].isLetter() &&
                key.left.isFinite() && key.top.isFinite() &&
                key.right.isFinite() && key.bottom.isFinite() &&
                key.right > key.left && key.bottom > key.top
        }
        if (validKeys.isEmpty()) return ""

        val locale = if (english) Locale.US else Locale("tr", "TR")
        // Precompute centers and normalized labels once per gesture instead
        // of repeating geometry and locale work for every MotionEvent sample.
        val centeredKeys = validKeys.map { key ->
            CenteredKey(
                key.value.lowercase(locale),
                (key.left.toDouble() + key.right.toDouble()) / 2.0,
                (key.top.toDouble() + key.bottom.toDouble()) / 2.0
            )
        }
        // Keep geometry arithmetic in Double: finite Float bounds can still
        // overflow when their centers or deltas are calculated in Float.
        val switchMargin = 12.0 * density.toDouble()
        val maxKeyDistance = 48.0 * density.toDouble()
        val word = StringBuilder()
        var previous = ""
        var activeKey: CenteredKey? = null

        path.forEach { (x, y) ->
            if (!x.isFinite() || !y.isFinite()) return@forEach
            val pointX = x.toDouble()
            val pointY = y.toDouble()
            var nearest: CenteredKey? = null
            var nearestDistance = Double.POSITIVE_INFINITY
            centeredKeys.forEach { candidate ->
                val distance = hypot(candidate.centerX - pointX, candidate.centerY - pointY)
                if (distance < nearestDistance) {
                    nearest = candidate
                    nearestDistance = distance
                }
            }
            val nearestKey = nearest ?: return@forEach
            if (nearestDistance > maxKeyDistance) return@forEach

            val value = nearestKey.value
            val currentKey = activeKey
            if (value != previous && currentKey != null) {
                val currentDistance = hypot(
                    currentKey.centerX - pointX,
                    currentKey.centerY - pointY
                )
                // Switch only when the new key is meaningfully closer than the active key.
                if (nearestDistance + switchMargin >= currentDistance) return@forEach
            }
            if (value != previous) word.append(value)
            previous = value
            activeKey = nearestKey
        }
        return word.toString()
    }
}
