package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionCandidateSelectorTest {
    @Test
    fun topTwoMatchesFullSortForEveryCandidateOrder() {
        val candidates = listOf("alpha", "alpine", "alps", "altar", "also")
        val comparator = compareBy<String> { it.length }.thenBy { it }
        fun permutations(items: List<String>): List<List<String>> {
            if (items.size <= 1) return listOf(items)
            return items.flatMap { item ->
                permutations(items.filter { it != item }).map { listOf(item) + it }
            }
        }

        permutations(candidates).forEach { order ->
            assertEquals(
                order.sortedWith(comparator).take(2),
                SuggestionCandidateSelector.top(order.asSequence(), 2, comparator)
            )
        }
    }

    @Test
    fun topSelectorPreservesStableOrderForEqualCandidates() {
        val candidates = sequenceOf("first", "other", "third")
        val comparator = compareBy<String> { it.length }
        assertEquals(
            listOf("first", "other"),
            SuggestionCandidateSelector.top(candidates, 2, comparator)
        )
    }

    @Test
    fun nonPositiveLimitReturnsNoSuggestions() {
        assertEquals(emptyList<String>(), SuggestionCandidateSelector.top(sequenceOf("one"), 0, compareBy { it }))
        assertEquals(emptyList<String>(), SuggestionCandidateSelector.top(sequenceOf("one"), -1, compareBy { it }))
    }

    @Test
    fun veryLargeLimitDoesNotPreallocateTheRequestedCapacity() {
        assertEquals(
            listOf("one", "two"),
            SuggestionCandidateSelector.top(
                sequenceOf("one", "two"),
                Int.MAX_VALUE,
                compareBy { it }
            )
        )
    }

    @Test
    fun selectorConsumesSequenceOnlyOnce() {
        var visited = 0
        val candidates = sequence {
            listOf("c", "a", "b").forEach {
                visited++
                yield(it)
            }
        }
        assertEquals(listOf("a", "b"), SuggestionCandidateSelector.top(candidates, 2, compareBy { it }))
        assertEquals(3, visited)
    }
}
