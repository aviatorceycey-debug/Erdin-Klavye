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
        val switchMargin = 12f * density
        val maxKeyDistance = 48f * density
        val word = StringBuilder()
        var previous = ""
        var activeKey: SwipeKeyBounds? = null

        path.forEach { (x, y) ->
            if (!x.isFinite() || !y.isFinite()) return@forEach
            val nearest = validKeys.minByOrNull { key ->
                val dx = key.centerX - x
                val dy = key.centerY - y
                dx * dx + dy * dy
            } ?: return@forEach
            val nearestDistance = hypot(nearest.centerX - x, nearest.centerY - y)
            if (nearestDistance > maxKeyDistance) return@forEach

            val value = nearest.value.lowercase(locale)
            val currentKey = activeKey
            if (value != previous && currentKey != null) {
                val currentDistance = hypot(currentKey.centerX - x, currentKey.centerY - y)
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
