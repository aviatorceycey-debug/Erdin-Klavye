package com.erdin.klavye

/**
 * Bounded Damerau-Levenshtein distance for swipe decoding.
 *
 * Swipe candidates are accepted only at distance zero or one. The result is
 * therefore capped at two. Three reusable rows avoid allocating a full
 * matrix for every dictionary candidate.
 */
internal object SwipeWordDistance {
    fun calculate(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        if (a == b) return 0
        if (a.isEmpty() || b.isEmpty()) return minOf(2, maxOf(a.length, b.length))

        val width = b.length + 1
        var twoRowsBack = IntArray(width) { it }
        var previous = IntArray(width) { it }
        var current = IntArray(width)

        for (i in 1..a.length) {
            current[0] = i.coerceAtMost(2)
            for (j in 1..b.length) {
                val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                var score = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + substitution
                )
                if (i > 1 && j > 1 &&
                    a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
                ) {
                    score = minOf(score, twoRowsBack[j - 2] + 1)
                }
                current[j] = score.coerceAtMost(2)
            }
            val recycled = twoRowsBack
            twoRowsBack = previous
            previous = current
            current = recycled
        }
        return previous[b.length].coerceAtMost(2)
    }
}
