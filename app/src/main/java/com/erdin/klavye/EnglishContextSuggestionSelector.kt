package com.erdin.klavye

/** Ranks offline English next-word candidates using private phrase and word counts. */
internal object EnglishContextSuggestionSelector {
    fun select(
        contextualHints: List<String>,
        seededHints: List<String>,
        learnedPairCounts: Map<String, Int>,
        learnedTrigramCounts: Map<String, Int>,
        personalWordCounts: Map<String, Int> = emptyMap(),
        limit: Int = 3
    ): List<String> {
        val candidates = (contextualHints.asSequence() + seededHints.asSequence() +
            learnedPairCounts.keys.asSequence() + learnedTrigramCounts.keys.asSequence() +
            personalWordCounts.asSequence().filter { it.value > 0 }.map { it.key })
            .filter { it.isNotBlank() }
            .distinct()
        return SuggestionCandidateSelector.top(
            candidates,
            limit,
            compareByDescending<String> {
                (learnedTrigramCounts[it]?.coerceAtLeast(0)?.toLong() ?: 0L) * 3L +
                    (learnedPairCounts[it]?.coerceAtLeast(0)?.toLong() ?: 0L)
            }.thenByDescending { personalWordCounts[it]?.coerceAtLeast(0) ?: 0 }
                .thenBy { it.length }
                .thenBy { it }
        )
    }
}
