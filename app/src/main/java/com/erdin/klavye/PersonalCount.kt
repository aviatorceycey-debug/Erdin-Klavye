package com.erdin.klavye

/** Keeps local learning counters non-negative and prevents integer overflow. */
internal object PersonalCount {
    fun increment(current: Int): Int {
        val safeCurrent = current.coerceAtLeast(0)
        return if (safeCurrent < Int.MAX_VALUE) safeCurrent + 1 else Int.MAX_VALUE
    }

    /** Combines counters without allowing corrupted negatives or integer overflow. */
    fun combine(first: Int, second: Int): Int =
        (first.coerceAtLeast(0).toLong() + second.coerceAtLeast(0).toLong())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}
