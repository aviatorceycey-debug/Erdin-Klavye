package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishContextSuggestionSelectorTest {
    @Test
    fun phraseContextOutranksGeneralWordFrequency() {
        val result = TurkishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = emptyList(),
            learnedPairCounts = mapOf("contextual" to 2),
            learnedTrigramCounts = mapOf("contextual" to 2),
            personalWordCounts = mapOf("common" to 100)
        )
        assertEquals(listOf("contextual", "common"), result)
    }

    @Test
    fun personalWordFrequencyBreaksOtherwiseEqualContextScores() {
        val result = TurkishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = emptyList(),
            learnedPairCounts = mapOf("often-used" to 3, "rare" to 3),
            learnedTrigramCounts = emptyMap(),
            personalWordCounts = mapOf("often-used" to 8, "rare" to 1)
        )
        assertEquals(listOf("often-used", "rare"), result)
    }

    @Test
    fun builtInContextHintsKeepTheirDeclaredPriorityWithoutLearning() {
        val result = TurkishContextSuggestionSelector.select(
            contextualHints = listOf("first-context", "second-context"),
            seededHints = listOf("first-seed", "second-seed"),
            learnedPairCounts = emptyMap(),
            learnedTrigramCounts = emptyMap(),
            personalWordCounts = emptyMap()
        )
        assertEquals(listOf("first-context", "second-context", "first-seed"), result)
    }

    @Test
    fun negativeCorruptCountsDoNotIncreaseCandidateScores() {
        val result = TurkishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = emptyList(),
            learnedPairCounts = mapOf("bad" to -100, "good" to 1),
            learnedTrigramCounts = emptyMap(),
            personalWordCounts = emptyMap()
        )
        assertEquals(listOf("good", "bad"), result)
    }

    @Test
    fun nonPositiveLimitReturnsNoSuggestions() {
        assertEquals(
            emptyList<String>(),
            TurkishContextSuggestionSelector.select(
                emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), 0
            )
        )
    }
}
