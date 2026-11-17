package com.erdin.klavye

/**
 * Bounded Damerau-Levenshtein distance for swipe decoding.
 *
 * The decoder only accepts candidates at distance 0 or 1, so values above
 * one are deliberately collapsed to 2 and the calculation can stop early.
 */
internal object SwipeWordDistance {
    fun calculate(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        if (a == b) return 0
        if (a.isEmpty() || b.isEmpty()) return 2

        var twoRowsBack = IntArray(b.length + 1) { it }
        var previous = IntArray(b.length + 1) { it }

        for (i in 1..a.length) {
            val current = IntArray(b.length + 1) { 2 }
            current[0] = i
            var rowMinimum = current[0]
            for (j in maxOf(1, i - 1)..minOf(b.length, i + 1)) {
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
                current[j] = score
                rowMinimum = minOf(rowMinimum, score)
            }
            if (rowMinimum > 1) return 2
            twoRowsBack = previous
            previous = current
        }
        return previous[b.length].coerceAtMost(2)
    }
}
