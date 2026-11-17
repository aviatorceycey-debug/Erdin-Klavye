package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePathSamplerTest {
    @Test
    fun ignoresDuplicateAndSubThresholdMotionSamples() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, 10f, 10f, 1f)
        SwipePathSampler.append(samples, 11f to 10f.first, 10f, 1f)
        assertEquals(listOf(10f to 10f), samples)
    }

    @Test
    fun acceptsMotionAtTheMinimumDistance() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, 12f, 10f, 1f)
        assertEquals(listOf(10f to 10f, 12f to 10f), samples)
    }

    @Test
    fun compactsLongGesturesAndPreservesNewestPoint() {
        val samples = MutableList(SwipePathSampler.MAX_SAMPLES) { it.toFloat() to 0f }
        SwipePathSampler.append(samples, 250f, 0f, 1f)
        assertEquals(91, samples.size)
        assertEquals(250f to 0f, samples.last())
        assertTrue(samples.size < SwipePathSampler.MAX_SAMPLES)
    }

    @Test
    fun rejectsInvalidDensity() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, 30f, 30f, 0f)
        assertEquals(listOf(10f to 10f), samples)
    }
}
