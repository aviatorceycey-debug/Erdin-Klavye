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
    private fun topTwo(
        candidates: Sequence<SwipeWordCandidate>,
        comparator: Comparator<SwipeWordCandidate>
    ): Pair<SwipeWordCandidate?, SwipeWordCandidate?> {
        var best: SwipeWordCandidate? = null
        var runnerUp: SwipeWordCandidate? = null
        candidates.forEach { candidate ->
            val currentBest = best
            if (currentBest == null || comparator.compare(candidate, currentBest) < 0) {
                runnerUp = currentBest
                best = candidate
            } else {
                val currentRunnerUp = runnerUp
                if (currentRunnerUp == null ||
                    comparator.compare(candidate, currentRunnerUp) < 0
                ) {
                    runnerUp = candidate
                }
            }
        }
        return best to runnerUp
    }

    fun selectTurkish(candidates: Sequence<SwipeWordCandidate>, fallback: String): String {
        val (best, runnerUp) = topTwo(
            candidates,
            compareBy<SwipeWordCandidate> { it.distance }
                .thenByDescending { it.personalCount }
                .thenBy { it.word }
        )
        best ?: return fallback
        val clearlyLearned = runnerUp != null &&
            best.personalCount >= 3 &&
            best.personalCount.toLong() >= runnerUp.personalCount.toLong() * 2L
        val unambiguous = runnerUp == null || runnerUp.distance > best.distance
        return if (best.distance in 0..1 && (unambiguous || clearlyLearned)) best.word else fallback
    }

    fun selectEnglish(candidates: Sequence<SwipeWordCandidate>, fallback: String): String {
        val (best, runnerUp) = topTwo(
            candidates,
            compareBy<SwipeWordCandidate> { it.distance }
                .thenByDescending { it.personalCount }
                .thenBy { it.frequencyRank }
                .thenBy { it.word }
        )
        best ?: return fallback
        val clearlyLearned = runnerUp != null &&
            best.personalCount >= 3 &&
            best.personalCount.toLong() >= runnerUp.personalCount.toLong() * 2L
        return if (best.distance in 0..1 &&
            (runnerUp == null || runnerUp.distance > best.distance || clearlyLearned)
        ) best.word else fallback
    }
}
