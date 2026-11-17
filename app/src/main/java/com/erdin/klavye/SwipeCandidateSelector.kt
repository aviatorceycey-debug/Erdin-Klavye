package com.erdin.klavye

/** A candidate word and the evidence available to the offline swipe decoder. */
internal data class SwipeWordCandidate(
    val word: String,
    val distance: Int,
    val personalCount: Int = 0,
    val frequencyRank: Int = Int.MAX_VALUE
)

/** Keeps uncertain swipe traces intact instead of silently replacing them. */
internal object SwipeCandidateSelector {
    fun selectTurkish(candidates: List<SwipeWordCandidate>, fallback: String): String {
        val ranked = candidates.sortedWith(
            compareBy<SwipeWordCandidate> { it.distance }
                .thenByDescending { it.personalCount }
                .thenBy { it.word }
        )
        val best = ranked.firstOrNull() ?: return fallback
        val runnerUp = ranked.getOrNull(1)
        val clearlyLearned = runnerUp != null &&
            best.personalCount >= 3 &&
            best.personalCount.toLong() >= runnerUp.personalCount.toLong() * 2L
        val unambiguous = runnerUp == null || runnerUp.distance > best.distance
        return if (best.distance in 0..1 && (unambiguous || clearlyLearned)) best.word else fallback
    }

    fun selectEnglish(candidates: List<SwipeWordCandidate>, fallback: String): String {
        val ranked = candidates.sortedWith(
            compareBy<SwipeWordCandidate> { it.distance }
                .thenBy { it.frequencyRank }
                .thenBy { it.word }
        )
        val best = ranked.firstOrNull() ?: return fallback
        val runnerUp = ranked.getOrNull(1)
        return if (best.distance in 0..1 &&
            (runnerUp == null || runnerUp.distance > best.distance)
        ) best.word else fallback
    }
}
