package com.erdin.klavye

/**
 * Bounded Damerau-Levenshtein distance for swipe decoding.
 *
 * Swipe candidates are accepted only at distance zero or one. The result is
 * therefore capped at two. Three reusable rows avoid allocating a full
 * matrix for every dictionary candidate; the shorter word is always used
 * for the row width to reduce temporary memory use.
 */
internal object SwipeWordDistance {
    fun calculate(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        if (a == b) return 0
        if (a.isEmpty() || b.isEmpty()) return minOf(2, maxOf(a.length, b.length))

        val (rowWord, columnWord) = if (a.length >= b.length) a to b else b to a
        val width = columnWord.length + 1
        var twoRowsBack = IntArray(width) { it }
        var previous = IntArray(width) { it }
        var current = IntArray(width)

        for (i in 1..rowWord.length) {
            current[0] = i.coerceAtMost(2)
            for (j in 1..columnWord.length) {
                val substitution = if (rowWord[i - 1] == columnWord[j - 1]) 0 else 1
                var score = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + substitution
                )
                if (i > 1 && j > 1 &&
                    rowWord[i - 1] == columnWord[j - 2] &&
                    rowWord[i - 2] == columnWord[j - 1]
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
        return previous[columnWord.length].coerceAtMost(2)
    }
}
