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
    fun emptyPathOrKeysProducesNoTrace() {
        assertEquals("", SwipeTraceDecoder.decode(emptyList(), keys, 1f, english = true))
        assertEquals("", SwipeTraceDecoder.decode(listOf(10f to 10f), emptyList(), 1f, english = true))
    }
}
