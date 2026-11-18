package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeContextParsingTest {
    @Test
    fun unicodeLettersAndTurkishWordsRemainIntact() {
        val words = WordContextParser.words("İyi günler, görüşürüz! Şimdi")
        assertEquals(listOf("İyi", "günler", "görüşürüz", "Şimdi"), words)
    }

    @Test
    fun apostrophesStayInsideWords() {
        val words = WordContextParser.words("I'm ready; user's choice")
        assertEquals(listOf("I'm", "ready", "user's", "choice"), words)
    }

    @Test
    fun punctuationOnlyInputProducesNoContextWords() {
        assertEquals(emptyList<String>(), WordContextParser.words("— … !!!"))
    }
}
