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
    fun decode(
        path: List<Pair<Float, Float>>,
        keys: List<SwipeKeyBounds>,
        density: Float,
        english: Boolean
    ): String {
        if (path.isEmpty() || keys.isEmpty() || !density.isFinite() || density <= 0f) return ""
        val validKeys = keys.filter { key ->
            key.value.isNotBlank() &&
                key.left.isFinite() && key.top.isFinite() &&
                key.right.isFinite() && key.bottom.isFinite() &&
                key.right > key.left && key.bottom > key.top
        }
        if (validKeys.isEmpty()) return ""

        val locale = if (english) Locale.US else Locale("tr", "TR")
        // Keep geometry arithmetic in Double: finite Float bounds can still
        // overflow when their centers or deltas are calculated in Float.
        val switchMargin = 12.0 * density.toDouble()
        val maxKeyDistance = 48.0 * density.toDouble()
        val word = StringBuilder()
        var previous = ""
        var activeKey: SwipeKeyBounds? = null

        path.forEach { (x, y) ->
            if (!x.isFinite() || !y.isFinite()) return@forEach
            val pointX = x.toDouble()
            val pointY = y.toDouble()
            fun distanceTo(key: SwipeKeyBounds): Double {
                val centerX = (key.left.toDouble() + key.right.toDouble()) / 2.0
                val centerY = (key.top.toDouble() + key.bottom.toDouble()) / 2.0
                return hypot(centerX - pointX, centerY - pointY)
            }
            val nearest = validKeys.minByOrNull(::distanceTo) ?: return@forEach
            val nearestDistance = distanceTo(nearest)
            if (nearestDistance > maxKeyDistance) return@forEach

            val value = nearest.value.lowercase(locale)
            val currentKey = activeKey
            if (value != previous && currentKey != null) {
                val currentDistance = distanceTo(currentKey)
                // Switch only when the new key is meaningfully closer than the active key.
                if (nearestDistance + switchMargin >= currentDistance) return@forEach
            }
            if (value != previous) word.append(value)
            previous = value
            activeKey = nearest
        }
        return word.toString()
    }
}
