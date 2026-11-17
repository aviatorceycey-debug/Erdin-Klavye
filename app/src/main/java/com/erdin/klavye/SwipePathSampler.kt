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
}
