package com.erdin.klavye

import kotlin.math.hypot

/**
 * Keeps swipe samples useful while bounding memory and filtering motion jitter.
 */
internal object SwipePathSampler {
    const val MAX_SAMPLES = 180

    fun append(
        samples: MutableList<Pair<Float, Float>>,
        x: Float,
        y: Float,
        density: Float
    ) {
        if (density <= 0f) return
        val last = samples.lastOrNull()
        if (last != null) {
            val dx = x - last.first
            val dy = y - last.second
            if (hypot(dx.toDouble(), dy.toDouble()) < 2f * density) return
        }
        if (samples.size >= MAX_SAMPLES) {
            val compacted = samples.filterIndexed { index, _ -> index % 2 == 0 }
            samples.clear()
            samples.addAll(compacted)
        }
        samples += x to y
    }
    /**
     * Interpolates a segment so fast MotionEvent updates still leave enough
     * samples for the key-trace decoder.
     */
    fun appendSegment(
        samples: MutableList<Pair<Float, Float>>,
        x: Float,
        y: Float,
        density: Float
    ) {
        if (density <= 0f) return
        val last = samples.lastOrNull()
        if (last == null) {
            append(samples, x, y, density)
            return
        }
        val dx = x - last.first
        val dy = y - last.second
        val distance = hypot(dx.toDouble(), dy.toDouble())
        val steps = (distance / (10f * density)).toInt().coerceIn(1, 12)
        for (step in 1..steps) {
            val fraction = step.toFloat() / steps
            append(
                samples,
                last.first + dx * fraction,
                last.second + dy * fraction,
                density
            )
        }
    }
}
