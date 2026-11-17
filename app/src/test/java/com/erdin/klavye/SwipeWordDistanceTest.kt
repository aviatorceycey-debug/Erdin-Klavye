package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeWordDistanceTest {
    @Test
    fun identicalWordsHaveZeroDistance() {
        assertEquals(0, SwipeWordDistance.calculate("klavye", "klavye"))
    }

    @Test
    fun adjacentTranspositionCountsAsOneEdit() {
        assertEquals(1, SwipeWordDistance.calculate("teh", "the"))
    }

    @Test
    fun singleSubstitutionCountsAsOneEdit() {
        assertEquals(1, SwipeWordDistance.calculate("kalem", "kalen"))
    }

    @Test
    fun singleInsertionAndDeletionCountAsOneEdit() {
        assertEquals(0, SwipeWordDistance.calculate("kitap", "kitap"))
        assertEquals(1, SwipeWordDistance.calculate("kitap", "ktap"))
        assertEquals(1, SwipeWordDistance.calculate("ktap", "kitap"))
    }

    @Test
    fun multipleEditsAreCappedAtTwo() {
        assertEquals(2, SwipeWordDistance.calculate("kitap", "kalem"))
        assertEquals(2, SwipeWordDistance.calculate("a", "abcd"))
    }
}
