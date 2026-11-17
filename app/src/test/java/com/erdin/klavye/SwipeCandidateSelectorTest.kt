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
        assertEquals("kalam", SwipeCandidateSelector.selectTurkish(candidates.asSequence(), "kalam"))
    }

    @Test
    fun turkishSelectorUsesStrongPersonalLearningToResolveTie() {
        val candidates = listOf(
            SwipeWordCandidate("merhaba", distance = 1, personalCount = 8),
            SwipeWordCandidate("merhuba", distance = 1, personalCount = 3)
        )
        assertEquals("merhaba", SwipeCandidateSelector.selectTurkish(candidates.asSequence(), "merhabe"))
    }

    @Test
    fun turkishSelectorDoesNotOverflowWhenComparingLargeCounts() {
        val candidates = listOf(
            SwipeWordCandidate("öğrenilen", distance = 1, personalCount = Int.MAX_VALUE),
            SwipeWordCandidate("öğrenilen2", distance = 1, personalCount = Int.MAX_VALUE / 2)
        )
        assertEquals("öğrenilen", SwipeCandidateSelector.selectTurkish(candidates.asSequence(), "ogrenilen"))
    }

    @Test
    fun bothSelectorsRejectCandidatesMoreThanOneEditAway() {
        val candidates = listOf(SwipeWordCandidate("telefon", distance = 2, personalCount = 100))
        assertEquals("televon", SwipeCandidateSelector.selectTurkish(candidates.asSequence(), "televon"))
        assertEquals("televon", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "televon"))
    }

    @Test
    fun englishSelectorKeepsTraceWhenBestDistanceIsTied() {
        val candidates = listOf(
            SwipeWordCandidate("there", distance = 1, frequencyRank = 4),
            SwipeWordCandidate("their", distance = 1, frequencyRank = 8)
        )
        assertEquals("thare", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "thare"))
    }

    @Test
    fun englishSelectorUsesFrequencyOnlyAfterDistance() {
        val candidates = listOf(
            SwipeWordCandidate("hello", distance = 1, frequencyRank = 20),
            SwipeWordCandidate("help", distance = 0, frequencyRank = 100)
        )
        assertEquals("help", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "help"))
    }

    @Test
    fun selectorsConsumeCandidateSequenceOnlyOnce() {
        var visited = 0
        val candidates = sequence {
            visited++
            yield(SwipeWordCandidate("hello", distance = 0, frequencyRank = 1))
            visited++
            yield(SwipeWordCandidate("help", distance = 1, frequencyRank = 2))
        }

        assertEquals("hello", SwipeCandidateSelector.selectEnglish(candidates, "helo"))
        assertEquals(2, visited)
    }

    @Test
    fun selectorFindsBestCandidatesRegardlessOfInputOrder() {
        val candidates = sequenceOf(
            SwipeWordCandidate("later", distance = 1, frequencyRank = 5),
            SwipeWordCandidate("hello", distance = 0, frequencyRank = 50),
            SwipeWordCandidate("hero", distance = 1, frequencyRank = 2)
        )
        assertEquals("hello", SwipeCandidateSelector.selectEnglish(candidates, "hello"))
    }

    @Test
    fun emptyCandidateListsPreserveOriginalTrace() {
        assertEquals("trace", SwipeCandidateSelector.selectTurkish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
        assertEquals("trace", SwipeCandidateSelector.selectEnglish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
    }
}
