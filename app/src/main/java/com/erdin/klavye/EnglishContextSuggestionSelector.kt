package com.erdin.klavye

/** Ranks offline English next-word candidates using private phrase counts. */
internal object EnglishContextSuggestionSelector {
    fun select(
        contextualHints: List<String>,
        seededHints: List<String>,
        learnedPairCounts: Map<String, Int>,
        learnedTrigramCounts: Map<String, Int>,
        limit: Int = 3
    ): List<String> {
        val candidates = (contextualHints.asSequence() + seededHints.asSequence() +
            learnedPairCounts.keys.asSequence() + learnedTrigramCounts.keys.asSequence())
            .filter { it.isNotBlank() }
            .distinct()
        return SuggestionCandidateSelector.top(
            candidates,
            limit,
            compareByDescending<String> {
                (learnedTrigramCounts[it]?.coerceAtLeast(0) ?: 0) * 3 +
                    (learnedPairCounts[it]?.coerceAtLeast(0) ?: 0)
            }
        )
    }
}
