package com.erdin.klavye

/** Selects only the best few suggestions without sorting or retaining the full vocabulary. */
internal object SuggestionCandidateSelector {
    fun <T> top(
        candidates: Sequence<T>,
        limit: Int,
        comparator: Comparator<T>
    ): List<T> {
        if (limit <= 0) return emptyList()
        val best = ArrayList<T>(minOf(limit, 16))
        candidates.forEach { candidate ->
            var insertionIndex = best.indexOfFirst { comparator.compare(candidate, it) < 0 }
            if (insertionIndex < 0) insertionIndex = best.size
            if (insertionIndex < limit) {
                best.add(insertionIndex, candidate)
                if (best.size > limit) best.removeAt(best.lastIndex)
            }
        }
        return best
    }
}
