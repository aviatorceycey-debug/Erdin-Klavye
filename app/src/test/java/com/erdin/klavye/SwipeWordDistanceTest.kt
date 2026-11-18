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
    fun distanceIsSymmetricAcrossShortWords() {
        val words = listOf("", "a", "b", "ab", "ba", "aa", "bb", "aba", "bab", "aab", "bba")
        words.forEach { first ->
            words.forEach { second ->
                assertEquals(
                    "Distance should be symmetric for '$first' and '$second'",
                    SwipeWordDistance.calculate(first, second),
                    SwipeWordDistance.calculate(second, first)
                )
            }
        }
    }

    @Test
    fun rollingRowsMatchReferenceMatrixForShortWords() {
        fun words(alphabet: String, maxLength: Int): List<String> {
            val result = mutableListOf("")
            var level = listOf("")
            repeat(maxLength) {
                level = level.flatMap { prefix -> alphabet.map { prefix + it } }
                result.addAll(level)
            }
            return result
        }

        fun referenceDistance(first: String, second: String): Int {
            if (kotlin.math.abs(first.length - second.length) > 1) return 2
            val matrix = Array(first.length + 1) { IntArray(second.length + 1) }
            for (i in 0..first.length) matrix[i][0] = i
            for (j in 0..second.length) matrix[0][j] = j
            for (i in 1..first.length) {
                for (j in 1..second.length) {
                    val substitution = if (first[i - 1] == second[j - 1]) 0 else 1
                    var score = minOf(
                        matrix[i - 1][j] + 1,
                        matrix[i][j - 1] + 1,
                        matrix[i - 1][j - 1] + substitution
                    )
                    if (i > 1 && j > 1 &&
                        first[i - 1] == second[j - 2] &&
                        first[i - 2] == second[j - 1]
                    ) {
                        score = minOf(score, matrix[i - 2][j - 2] + 1)
                    }
                    matrix[i][j] = score.coerceAtMost(2)
                }
            }
            return matrix[first.length][second.length].coerceAtMost(2)
        }

        val samples = words("abc", 4)
        samples.forEach { first ->
            samples.forEach { second ->
                assertEquals(
                    "Distance mismatch for '$first' and '$second'",
                    referenceDistance(first, second),
                    SwipeWordDistance.calculate(first, second)
                )
            }
        }
    }

    @Test
    fun longerFirstAndSecondArgumentsProduceTheSameResult() {
        val shortWord = "klavye"
        val longWord = "klavyee"
        assertEquals(1, SwipeWordDistance.calculate(longWord, shortWord))
        assertEquals(1, SwipeWordDistance.calculate(shortWord, longWord))
        assertEquals(1, SwipeWordDistance.calculate("kitap", "ktap"))
        assertEquals(1, SwipeWordDistance.calculate("ktap", "kitap"))
    }

    @Test
    fun multipleEditsAreCappedAtTwo() {
        assertEquals(2, SwipeWordDistance.calculate("kitap", "kalem"))
        assertEquals(2, SwipeWordDistance.calculate("a", "abcd"))
    }
}
