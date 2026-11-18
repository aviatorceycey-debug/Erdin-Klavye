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
    fun saturatesAtMaximumIntegerInsteadOfWrappingNegative() {
        assertEquals(Int.MAX_VALUE, PersonalCount.increment(Int.MAX_VALUE))
    }
}
