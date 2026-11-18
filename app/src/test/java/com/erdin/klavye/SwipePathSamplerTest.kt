package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePathSamplerTest {
    @Test
    fun ignoresDuplicateAndSubThresholdMotionSamples() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, 10f, 10f, 1f)
        SwipePathSampler.append(samples, 11f, 10f, 1f)
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
    fun repeatedCompactionNeverExceedsTheSampleLimit() {
        val samples = mutableListOf(0f to 0f)
        repeat(2_000) { index ->
            SwipePathSampler.append(samples, (index + 10).toFloat(), 0f, 1f)
            assertTrue(samples.size <= SwipePathSampler.MAX_SAMPLES)
        }
        assertEquals(2_008f to 0f, samples.last())
    }

    @Test
    fun compactionKeepsSamplesInTravelOrder() {
        val samples = mutableListOf(0f to 0f)
        repeat(1_000) { index ->
            SwipePathSampler.append(samples, (index + 10).toFloat(), 0f, 1f)
        }
        assertTrue(samples.zipWithNext().all { (a, b) -> b.first > a.first })
        assertTrue(samples.size <= SwipePathSampler.MAX_SAMPLES)
    }

    @Test
    fun interpolatesLongSegmentsIntoUsefulSamples() {
        val samples = mutableListOf(0f to 0f)
        SwipePathSampler.appendSegment(samples, 30f, 0f, 1f)
        assertEquals(listOf(0f to 0f, 10f to 0f, 20f to 0f, 30f to 0f), samples)
    }

    @Test
    fun roundsInterpolationStepsUpToAvoidOversizedGaps() {
        val samples = mutableListOf(0f to 0f)
        SwipePathSampler.appendSegment(samples, 19f, 0f, 1f)

        assertEquals(19f to 0f, samples.last())
        assertEquals(3, samples.size)
        val largestGap = samples.zipWithNext().maxOf { (a, b) -> b.first - a.first }
        assertTrue("Interpolated gaps should not exceed the target step", largestGap <= 10f)
    }

    @Test
    fun preservesCornersWhenConsecutiveMotionSegmentsTurn() {
        val samples = mutableListOf(0f to 0f)
        SwipePathSampler.appendSegment(samples, 30f, 0f, 1f)
        SwipePathSampler.appendSegment(samples, 30f, 30f, 1f)
        assertTrue(samples.contains(30f to 0f))
        assertTrue(samples.contains(30f to 30f))
        assertTrue(samples.filter { it.second > 0f }.all { it.first == 30f })
    }

    @Test
    fun segmentInterpolationRemainsBoundedForLongGestures() {
        val samples = mutableListOf(0f to 0f)
        repeat(100) { index ->
            SwipePathSampler.appendSegment(samples, (index + 1) * 100f, 0f, 1f)
            assertTrue(samples.size <= SwipePathSampler.MAX_SAMPLES)
        }
    }

    @Test
    fun rejectsNonFiniteCoordinatesAndDensity() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, Float.NaN, 20f, 1f)
        SwipePathSampler.append(samples, 20f, Float.POSITIVE_INFINITY, 1f)
        SwipePathSampler.append(samples, 30f, 30f, Float.NaN)
        assertEquals(listOf(10f to 10f), samples)
    }

    @Test
    fun capsInterpolationWorkAndPreservesTheSegmentEndpoint() {
        val samples = mutableListOf(0f to 0f)
        SwipePathSampler.appendSegment(samples, 10_000f, 0f, 1f)
        assertEquals(10_000f to 0f, samples.last())
        assertTrue(samples.size <= 13)
    }

    @Test
    fun interpolatesBetweenExtremeFiniteFloatCoordinates() {
        val samples = mutableListOf(-Float.MAX_VALUE to 0f)
        SwipePathSampler.appendSegment(samples, Float.MAX_VALUE, 0f, 1f)
        assertEquals(Float.MAX_VALUE to 0f, samples.last())
        assertTrue(samples.size <= SwipePathSampler.MAX_SAMPLES)
        assertTrue(samples.all { it.first.isFinite() && it.second.isFinite() })
    }

    @Test
    fun rejectsNonFiniteInterpolatedEndpoints() {
        val samples = mutableListOf(0f to 0f)
        SwipePathSampler.appendSegment(samples, Float.POSITIVE_INFINITY, 10f, 1f)
        assertEquals(listOf(0f to 0f), samples)
    }

    @Test
    fun rejectsInvalidDensity() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.append(samples, 30f, 30f, 0f)
        assertEquals(listOf(10f to 10f), samples)
    }
}
