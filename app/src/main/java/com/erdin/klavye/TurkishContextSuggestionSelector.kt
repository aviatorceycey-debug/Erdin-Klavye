package com.erdin.klavye

/** Ranks Turkish next-word candidates by phrase context before general word frequency. */
internal object TurkishContextSuggestionSelector {
    fun select(
        contextualHints: List<String>,
        seededHints: List<String>,
        learnedPairCounts: Map<String, Int>,
        learnedTrigramCounts: Map<String, Int>,
        personalWordCounts: Map<String, Int>,
        limit: Int = 3
    ): List<String> {
        val candidates = (contextualHints.asSequence() + seededHints.asSequence() +
            learnedPairCounts.keys.asSequence() + learnedTrigramCounts.keys.asSequence())
            .filter { it.isNotBlank() }
            .distinct()

        fun score(word: String): Long {
            val trigram = learnedTrigramCounts[word]?.coerceAtLeast(0)?.coerceAtMost(100) ?: 0
            val pair = learnedPairCounts[word]?.coerceAtLeast(0)?.coerceAtMost(100) ?: 0
            val contextIndex = contextualHints.indexOf(word)
            val seededTrigram = if (contextIndex < 0) 0 else (contextualHints.size - contextIndex) * 3
            val seedIndex = seededHints.indexOf(word)
            val seededPair = if (seedIndex < 0) 0 else (seededHints.size - seedIndex) * 2
            return trigram.toLong() * 12L + pair.toLong() * 5L +
                seededTrigram.toLong() + seededPair.toLong()
        }

        return SuggestionCandidateSelector.top(
            candidates,
            limit,
            compareByDescending<String> { score(it) }
                .thenByDescending { learnedTrigramCounts[it]?.coerceAtLeast(0) ?: 0 }
                .thenByDescending { learnedPairCounts[it]?.coerceAtLeast(0) ?: 0 }
                .thenByDescending { personalWordCounts[it]?.coerceAtLeast(0) ?: 0 }
                .thenBy { it.length }
                .thenBy { it }
        )
    }
}
