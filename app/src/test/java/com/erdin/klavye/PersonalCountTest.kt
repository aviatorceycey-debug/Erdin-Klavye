package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalCountTest {
    @Test
    fun incrementsOrdinaryCounts() {
        assertEquals(1, PersonalCount.increment(0))
        assertEquals(42, PersonalCount.increment(41))
    }

    @Test
    fun combinesCountsWithoutOverflowOrNegativeValues() {
        assertEquals(7, PersonalCount.combine(3, 4))
        assertEquals(Int.MAX_VALUE, PersonalCount.combine(Int.MAX_VALUE, 1))
        assertEquals(5, PersonalCount.combine(-10, 5))
        assertEquals(0, PersonalCount.combine(-10, -20))
    }

    @Test
    fun repairsNegativeStoredCountsBeforeIncrementing() {
        assertEquals(1, PersonalCount.increment(-1))
        assertEquals(1, PersonalCount.increment(Int.MIN_VALUE))
    }

    @Test
    fun saturatesAtMaximumIntegerInsteadOfWrappingNegative() {
        assertEquals(Int.MAX_VALUE, PersonalCount.increment(Int.MAX_VALUE))
    }
}
