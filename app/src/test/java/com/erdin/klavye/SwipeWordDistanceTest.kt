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
        assertEquals(1, SwipeWordDistance.calculate("aab", "aba"))
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
    fun emptyWordsAreHandledSafely() {
        assertEquals(0, SwipeWordDistance.calculate("", ""))
        assertEquals(1, SwipeWordDistance.calculate("", "a"))
        assertEquals(1, SwipeWordDistance.calculate("a", ""))
        assertEquals(2, SwipeWordDistance.calculate("", "ab"))
    }

    @Test
    fun TurkishCharactersAreComparedExactly() {
        assertEquals(1, SwipeWordDistance.calculate("s", "ş"))
        assertEquals(1, SwipeWordDistance.calculate("i", "ı"))
        assertEquals(1, SwipeWordDistance.calculate("c", "ç"))
    }

    @Test
    fun multipleEditsAreCappedAtTwo() {
        assertEquals(2, SwipeWordDistance.calculate("kitap", "kalem"))
        assertEquals(2, SwipeWordDistance.calculate("a", "abcd"))
    }
}
