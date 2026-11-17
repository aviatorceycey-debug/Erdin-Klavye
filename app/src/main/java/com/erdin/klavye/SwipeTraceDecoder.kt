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
        if (path.isEmpty() || keys.isEmpty() || density <= 0f) return ""

        val locale = if (english) Locale.US else Locale("tr", "TR")
        val switchMarginSquared = (12f * density) * (12f * density)
        val maxKeyDistanceSquared = (48f * density) * (48f * density)
        val word = StringBuilder()
        var previous = ""
        var previousDistance = Float.MAX_VALUE

        path.forEach { (x, y) ->
            val nearest = keys.minByOrNull { key ->
                val dx = key.centerX - x
                val dy = key.centerY - y
                dx * dx + dy * dy
            } ?: return@forEach
            val dx = nearest.centerX - x
            val dy = nearest.centerY - y
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared > maxKeyDistanceSquared) return@forEach

            val value = nearest.value.lowercase(locale)
            // Keep the current key through tiny boundary fluctuations.
            val switchingKey = value != previous && previous.isNotEmpty()
            if (switchingKey && distanceSquared + switchMarginSquared >= previousDistance) {
                return@forEach
            }
            if (value != previous) word.append(value)
            previous = value
            previousDistance = distanceSquared
        }
        return word.toString()
    }
}
