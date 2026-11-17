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
        if (!density.isFinite() || density <= 0f || !x.isFinite() || !y.isFinite()) return
        val last = samples.lastOrNull()
        if (last != null) {
            val dx = x.toDouble() - last.first.toDouble()
            val dy = y.toDouble() - last.second.toDouble()
            if (hypot(dx, dy) < 2.0 * density.toDouble()) return
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
        if (!density.isFinite() || density <= 0f || !x.isFinite() || !y.isFinite()) return
        val last = samples.lastOrNull()
        if (last == null) {
            append(samples, x, y, density)
            return
        }
        // Use Double for deltas: subtracting two finite Float coordinates can
        // overflow Float even though the interpolated points are representable.
        val dx = x.toDouble() - last.first.toDouble()
        val dy = y.toDouble() - last.second.toDouble()
        val distance = hypot(dx, dy)
        val stepLength = 10.0 * density.toDouble()
        val steps = (distance / stepLength).toInt().coerceIn(1, 12)
        for (step in 1..steps) {
            val fraction = step.toDouble() / steps
            val sampleX = (last.first.toDouble() + dx * fraction).toFloat()
            val sampleY = (last.second.toDouble() + dy * fraction).toFloat()
            append(samples, sampleX, sampleY, density)
        }
    }
}
