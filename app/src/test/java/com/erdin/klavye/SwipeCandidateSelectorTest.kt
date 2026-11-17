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

        assertEquals("hello", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "helo"))
        assertEquals(2, visited)
    }

    @Test
    fun selectorFindsBestCandidatesRegardlessOfInputOrder() {
        val candidates = sequenceOf(
            SwipeWordCandidate("later", distance = 1, frequencyRank = 5),
            SwipeWordCandidate("hello", distance = 0, frequencyRank = 50),
            SwipeWordCandidate("hero", distance = 1, frequencyRank = 2)
        )
        assertEquals("hello", SwipeCandidateSelector.selectEnglish(candidates.asSequence(), "hello"))
    }

    @Test
    fun emptyCandidateListsPreserveOriginalTrace() {
        assertEquals("trace", SwipeCandidateSelector.selectTurkish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
        assertEquals("trace", SwipeCandidateSelector.selectEnglish(emptyList<SwipeWordCandidate>().asSequence(), "trace"))
    }

    @Test
    fun streamingSelectionMatchesSortedReferenceForEveryCandidateOrder() {
        val candidates = listOf(
            SwipeWordCandidate("alpha", distance = 1, personalCount = 8, frequencyRank = 9),
            SwipeWordCandidate("alphi", distance = 1, personalCount = 3, frequencyRank = 2),
            SwipeWordCandidate("aleph", distance = 0, personalCount = 0, frequencyRank = 20),
            SwipeWordCandidate("omega", distance = 2, personalCount = 30, frequencyRank = 1)
        )

        fun permutations(items: List<SwipeWordCandidate>): List<List<SwipeWordCandidate>> {
            if (items.size <= 1) return listOf(items)
            return items.flatMap { item ->
                permutations(items.filterIndexed { index, _ -> items[index] !== item })
                    .map { listOf(item) + it }
            }
        }

        val orders = permutations(candidates)
        orders.forEach { order ->
            val turkishSorted = order.sortedWith(
                compareBy<SwipeWordCandidate> { it.distance }
                    .thenByDescending { it.personalCount }
                    .thenBy { it.word }
            )
            val trBest = turkishSorted.firstOrNull()
            val trRunnerUp = turkishSorted.getOrNull(1)
            val trLearned = trRunnerUp != null && trBest != null &&
                trBest.personalCount >= 3 &&
                trBest.personalCount.toLong() >= trRunnerUp.personalCount.toLong() * 2L
            val trExpected = if (trBest != null && trBest.distance in 0..1 &&
                (trRunnerUp == null || trRunnerUp.distance > trBest.distance || trLearned)
            ) trBest.word else "trace"

            val englishSorted = order.sortedWith(
                compareBy<SwipeWordCandidate> { it.distance }
                    .thenBy { it.frequencyRank }
                    .thenBy { it.word }
            )
            val enBest = englishSorted.firstOrNull()
            val enRunnerUp = englishSorted.getOrNull(1)
            val enExpected = if (enBest != null && enBest.distance in 0..1 &&
                (enRunnerUp == null || enRunnerUp.distance > enBest.distance)
            ) enBest.word else "trace"

            assertEquals(trExpected, SwipeCandidateSelector.selectTurkish(order.asSequence(), "trace"))
            assertEquals(enExpected, SwipeCandidateSelector.selectEnglish(order.asSequence(), "trace"))
        }
    }

}
