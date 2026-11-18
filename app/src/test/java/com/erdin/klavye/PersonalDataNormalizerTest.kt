package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PersonalDataNormalizerTest {
    @Test
    fun mergesComposedAndDecomposedWordEntriesWithoutLosingCounts() {
        val result = PersonalDataNormalizer.normalizeCounts(
            mapOf("cafe\u0301" to 2, "café" to 3),
            Locale.US
        )
        assertEquals(mapOf("café" to 5), result)
    }

    @Test
    fun normalizesEveryWordInLearnedPhraseKeys() {
        val result = PersonalDataNormalizer.normalizeCounts(
            mapOf("cafe\u0301>tea" to 4, "café>tea" to 2),
            Locale.US
        )
        assertEquals(mapOf("café>tea" to 6), result)
    }

    @Test
    fun appliesTurkishLocaleToStoredWordsAndContexts() {
        val result = PersonalDataNormalizer.normalizeCounts(
            mapOf("I>İ" to 1),
            Locale("tr", "TR")
        )
        assertEquals(mapOf("ı>i" to 1), result)
    }

    @Test
    fun clampsNegativeCountsAndPreventsOverflowWhenMerging() {
        val result = PersonalDataNormalizer.normalizeCounts(
            mapOf("cafe\u0301" to -8, "café" to Int.MAX_VALUE),
            Locale.US
        )
        assertEquals(Int.MAX_VALUE, result["café"])
    }
}
