package com.erdin.klavye

/** Keeps local learning counters non-negative and prevents integer overflow. */
internal object PersonalCount {
    fun increment(current: Int): Int =
        if (current < Int.MAX_VALUE) current + 1 else Int.MAX_VALUE
}
