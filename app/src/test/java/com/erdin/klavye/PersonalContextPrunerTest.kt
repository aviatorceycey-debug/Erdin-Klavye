package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalContextPrunerTest {
    @Test
    fun removesWordFromEitherSideOfLearnedPairs() {
        val pairs = mutableMapOf(
            "merhaba>nasılsın" to 2,
            "nasılsın>iyiyim" to 3,
            "bugün>güzel" to 1
        )
        val trigrams = mutableMapOf<String, Int>()

        PersonalContextPruner.removeWord(pairs, trigrams, "nasılsın")

        assertEquals(mapOf("bugün>güzel" to 1), pairs)
    }

    @Test
    fun removesWordFromEveryPositionInLearnedTrigrams() {
        val pairs = mutableMapOf("iyi>gün" to 1)
        val trigrams = mutableMapOf(
            "ben>bugün>iyiyim" to 2,
            "bugün>hava>güzel" to 3,
            "hava>güzel>bugün" to 4,
            "hava>güzel>oldu" to 5
        )

        PersonalContextPruner.removeWord(pairs, trigrams, "bugün")

        assertEquals(mapOf("iyi>gün" to 1), pairs)
        assertEquals(mapOf("hava>güzel>oldu" to 5), trigrams)
    }

    @Test
    fun comparesWholeTokensInsteadOfSubstrings() {
        val pairs = mutableMapOf(
            "ev>güzel" to 1,
            "evet>tamam" to 2,
            "yeni>ev" to 3
        )
        val trigrams = mutableMapOf("evet>çok>iyi" to 4)

        PersonalContextPruner.removeWord(pairs, trigrams, "ev")

        assertEquals(mapOf("evet>tamam" to 2), pairs)
        assertEquals(mapOf("evet>çok>iyi" to 4), trigrams)
    }

    @Test
    fun blankWordDoesNotChangeLearnedContexts() {
        val pairs = mutableMapOf("merhaba>nasılsın" to 2)
        val trigrams = mutableMapOf("bugün>hava>güzel" to 3)

        PersonalContextPruner.removeWord(pairs, trigrams, " ")

        assertEquals(mapOf("merhaba>nasılsın" to 2), pairs)
        assertEquals(mapOf("bugün>hava>güzel" to 3), trigrams)
    }
}
