package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class EnglishContextSuggestionSelectorTest {
    @Test
    fun trigramContextHasThreeTimesTheWeightOfPairContext() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = listOf("seeded"),
            learnedPairCounts = mapOf("pair" to 3, "trigram" to 2),
            learnedTrigramCounts = mapOf("trigram" to 2)
        )
        assertEquals(listOf("trigram", "pair", "seeded"), result)
    }

    @Test
    fun learnedPairCanOutrankGenericSeededHints() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("generic-context"),
            seededHints = listOf("generic-seed"),
            learnedPairCounts = mapOf("my-usual-word" to 5),
            learnedTrigramCounts = emptyMap()
        )
        assertEquals("my-usual-word", result.first())
    }

    @Test
    fun contextualAndSeededHintOrderIsStableWhenNoPersonalDataExists() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("to", "again"),
            seededHints = listOf("please", "soon"),
            learnedPairCounts = emptyMap(),
            learnedTrigramCounts = emptyMap()
        )
        assertEquals(listOf("to", "again", "please"), result)
    }

    @Test
    fun removesDuplicatesAndBlankCandidatesAndCapsAtThree() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("to", "", "soon"),
            seededHints = listOf("to", "please"),
            learnedPairCounts = mapOf("often" to 2),
            learnedTrigramCounts = emptyMap(),
            limit = 3
        )
        assertEquals(listOf("often", "to", "soon"), result)
    }

    @Test
    fun nonPositiveLimitReturnsNoCandidates() {
        assertEquals(
            emptyList<String>(),
            EnglishContextSuggestionSelector.select(
                emptyList(), listOf("hello"), emptyMap(), emptyMap(), 0
            )
        )
    }
}
