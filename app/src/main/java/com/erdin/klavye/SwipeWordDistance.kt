package com.erdin.klavye

/**
 * Bounded Damerau-Levenshtein distance for swipe decoding.
 *
 * Swipe candidates are accepted only at distance zero or one. The result is
 * therefore capped at two; words are short enough that this clear matrix
 * implementation is inexpensive and avoids fragile band-boundary cases.
 */
internal object SwipeWordDistance {
    fun calculate(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        if (a == b) return 0

        val distance = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) distance[i][0] = i
        for (j in 0..b.length) distance[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                var score = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + substitution
                )
                if (i > 1 && j > 1 &&
                    a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
                ) {
                    score = minOf(score, distance[i - 2][j - 2] + 1)
                }
                distance[i][j] = score.coerceAtMost(2)
            }
        }
        return distance[a.length][b.length].coerceAtMost(2)
    }
}
