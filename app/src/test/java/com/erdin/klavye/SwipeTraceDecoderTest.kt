package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeTraceDecoderTest {
    private val keys = listOf(
        SwipeKeyBounds("c", 0f, 0f, 20f, 20f),
        SwipeKeyBounds("a", 100f, 0f, 120f, 20f),
        SwipeKeyBounds("t", 200f, 0f, 220f, 20f),
        SwipeKeyBounds("I", 300f, 0f, 320f, 20f)
    )

    @Test
    fun decodesLettersInTheOrderOfTheFingerPath() {
        val path = listOf(10f to 10f, 110f to 10f, 210f to 10f)
        assertEquals("cat", SwipeTraceDecoder.decode(path, keys, 1f, english = true))
    }

    @Test
    fun repeatedSamplesInsideOneKeyDoNotDuplicateLetters() {
        val path = listOf(10f to 10f, 11f to 10f, 12f to 11f)
        assertEquals("c", SwipeTraceDecoder.decode(path, keys, 1f, english = true))
    }

    @Test
    fun keySwitchingIgnoresSmallBoundaryFluctuations() {
        val boundaryPath = listOf(10f to 10f, 65f to 10f)
        val switchedPath = listOf(10f to 10f, 70f to 10f)
        assertEquals("c", SwipeTraceDecoder.decode(boundaryPath, keys, 1f, english = true))
        assertEquals("ca", SwipeTraceDecoder.decode(switchedPath, keys, 1f, english = true))
    }

    @Test
    fun pointsFarFromKeysAreIgnored() {
        val path = listOf(10f to 10f, 1000f to 1000f, 110f to 10f)
        assertEquals("ca", SwipeTraceDecoder.decode(path, keys, 1f, english = true))
    }

    @Test
    fun lowercasesKeysUsingTheActiveLanguage() {
        val path = listOf(310f to 10f)
        assertEquals("i", SwipeTraceDecoder.decode(path, keys, 1f, english = true))
        assertEquals("ı", SwipeTraceDecoder.decode(path, keys, 1f, english = false))
    }

    @Test
    fun ignoresNonFinitePathPoints() {
        val path = listOf(
            10f to 10f,
            Float.NaN to 10f,
            110f to Float.POSITIVE_INFINITY,
            210f to 10f
        )
        assertEquals("ct", SwipeTraceDecoder.decode(path, keys, 1f, english = true))
    }

    @Test
    fun ignoresInvalidKeyGeometry() {
        val invalidKeys = listOf(
            SwipeKeyBounds("x", Float.NaN, 0f, 20f, 20f),
            SwipeKeyBounds("y", 20f, 20f, 10f, 10f)
        )
        assertEquals(
            "c",
            SwipeTraceDecoder.decode(listOf(10f to 10f), keys + invalidKeys, 1f, english = true)
        )
    }

    @Test
    fun invalidDensityProducesNoTrace() {
        assertEquals("",
            SwipeTraceDecoder.decode(listOf(10f to 10f), keys, Float.NaN, english = true)
        )
    }

    @Test
    fun interpolatedFastGestureCanCrossIntermediateKeys() {
        val samples = mutableListOf(10f to 10f)
        SwipePathSampler.appendSegment(samples, 210f, 10f, 1f)
        assertEquals(
            "cat",
            SwipeTraceDecoder.decode(samples, keys, 1f, english = true)
        )
    }

    @Test
    fun extremeFiniteKeyBoundsDoNotOverflowCenterCalculations() {
        val extremeKeys = listOf(
            SwipeKeyBounds(
                "a",
                Float.MAX_VALUE / 2f,
                0f,
                Float.MAX_VALUE,
                40f
            )
        )
        assertEquals(
            "a",
            SwipeTraceDecoder.decode(
                listOf(Float.MAX_VALUE * 0.75f to 20f),
                extremeKeys,
                1f,
                english = true
            )
        )
    }

    @Test
    fun veryLargeFiniteDensityDoesNotCrashDistanceChecks() {
        val twoKeys = listOf(
            SwipeKeyBounds("a", 0f, 0f, 40f, 40f),
            SwipeKeyBounds("b", 40f, 0f, 80f, 40f)
        )
        assertEquals(
            "a",
            SwipeTraceDecoder.decode(
                listOf(10f to 20f, 70f to 20f),
                twoKeys,
                Float.MAX_VALUE,
                english = true
            )
        )
    }

    @Test
    fun emptyPathOrKeysProducesNoTrace() {
        assertEquals("", SwipeTraceDecoder.decode(emptyList(), keys, 1f, english = true))
        assertEquals("", SwipeTraceDecoder.decode(listOf(10f to 10f), emptyList(), 1f, english = true))
    }
}
