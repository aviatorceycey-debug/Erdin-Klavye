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
    fun largePersonalCountsDoNotOverflowAndReverseTheRanking() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = emptyList(),
            learnedPairCounts = mapOf("pair" to Int.MAX_VALUE, "trigram" to Int.MAX_VALUE),
            learnedTrigramCounts = mapOf("trigram" to Int.MAX_VALUE)
        )
        assertEquals(listOf("trigram", "pair"), result)
    }

    @Test
    fun negativeCorruptCountsDoNotBoostCandidates() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("context"),
            seededHints = listOf("seed"),
            learnedPairCounts = mapOf("context" to -100, "seed" to -5),
            learnedTrigramCounts = mapOf("context" to -50)
        )
        assertEquals(listOf("context", "seed"), result)
    }

    @Test
    fun learnedEnglishWordsJoinSuggestionsWithoutOvertakingPhraseContext() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("contextual"),
            seededHints = listOf("seeded"),
            learnedPairCounts = emptyMap(),
            learnedTrigramCounts = emptyMap(),
            personalWordCounts = mapOf("my-word" to 5)
        )
        assertEquals(listOf("contextual", "my-word", "seeded"), result)
    }

    @Test
    fun personalEnglishFrequencyBreaksEqualContextScores() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = emptyList(),
            seededHints = emptyList(),
            learnedPairCounts = mapOf("often" to 2, "rare" to 2),
            learnedTrigramCounts = emptyMap(),
            personalWordCounts = mapOf("often" to 9, "rare" to 1)
        )
        assertEquals(listOf("often", "rare"), result)
    }

    @Test
    fun learnedEnglishPhraseContextOutranksVeryFrequentPersonalWord() {
        val result = EnglishContextSuggestionSelector.select(
            contextualHints = listOf("phrase-word"),
            seededHints = listOf("seeded"),
            learnedPairCounts = emptyMap(),
            learnedTrigramCounts = mapOf("phrase-word" to 1),
            limit = 3,
            personalWordCounts = mapOf("frequent" to Int.MAX_VALUE)
        )
        assertEquals("phrase-word", result.first())
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
