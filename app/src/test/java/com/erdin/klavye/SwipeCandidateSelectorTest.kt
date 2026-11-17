package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeCandidateSelectorTest {
    @Test
    fun turkishSelectorAcceptsAnUnambiguousNearMatch() {
        val candidates = listOf(
            SwipeWordCandidate("kitap", distance = 1, personalCount = 0),
            SwipeWordCandidate("kalem", distance = 2, personalCount = 20)
        )
        assertEquals("kitap", SwipeCandidateSelector.selectTurkish(candidates.asSequence(), "kitapx"))
    }

    @Test
    fun turkishSelectorKeepsTraceWhenCandidatesAreEquallyPlausible() {
        val candidates = listOf(
            SwipeWordCandidate("kalem", distance = 1, personalCount = 2),
            SwipeWordCandidate("kelam", distance = 1, personalCount = 2)
        )
        assertEquals("kalam", SwipeCandidateSelector.selectTurkish(candidates, "kalam"))
    }

    @Test
    fun turkishSelectorUsesStrongPersonalLearningToResolveTie() {
        val candidates = listOf(
            SwipeWordCandidate("merhaba", distance = 1, personalCount = 8),
            SwipeWordCandidate("merhuba", distance = 1, personalCount = 3)
        )
        assertEquals("merhaba", SwipeCandidateSelector.selectTurkish(candidates, "merhabe"))
    }

    @Test
    fun turkishSelectorDoesNotOverflowWhenComparingLargeCounts() {
        val candidates = listOf(
            SwipeWordCandidate("öğrenilen", distance = 1, personalCount = Int.MAX_VALUE),
            SwipeWordCandidate("öğrenilen2", distance = 1, personalCount = Int.MAX_VALUE / 2)
        )
        assertEquals("öğrenilen", SwipeCandidateSelector.selectTurkish(candidates, "ogrenilen"))
    }

    @Test
    fun bothSelectorsRejectCandidatesMoreThanOneEditAway() {
        val candidates = listOf(SwipeWordCandidate("telefon", distance = 2, personalCount = 100))
        assertEquals("televon", SwipeCandidateSelector.selectTurkish(candidates, "televon"))
        assertEquals("televon", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "televon"))
    }

    @Test
    fun englishSelectorKeepsTraceWhenBestDistanceIsTied() {
        val candidates = listOf(
            SwipeWordCandidate("there", distance = 1, frequencyRank = 4),
            SwipeWordCandidate("their", distance = 1, frequencyRank = 8)
        )
        assertEquals("thare", SwipeCandidateSelector.selectEnglish(candidates, "thare"))
    }

    @Test
    fun englishSelectorUsesFrequencyOnlyAfterDistance() {
        val candidates = listOf(
            SwipeWordCandidate("hello", distance = 1, frequencyRank = 20),
            SwipeWordCandidate("help", distance = 0, frequencyRank = 100)
        )
        assertEquals("help", SwipeCandidateSelector.selectEnglish(candidates, "help"))
    }

    @Test
    fun emptyCandidateListsPreserveOriginalTrace() {
        assertEquals("trace", SwipeCandidateSelector.selectTurkish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
        assertEquals("trace", SwipeCandidateSelector.selectEnglish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
    }
}
