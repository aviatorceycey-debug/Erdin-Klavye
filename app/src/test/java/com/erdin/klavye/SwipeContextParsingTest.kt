package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeContextParsingTest {
    @Test
    fun unicodeLettersAndTurkishWordsRemainIntact() {
        val words = "İyi günler, görüşürüz! Şimdi".split(Regex("[^\\p{L}'’]+"))
            .filter { it.isNotBlank() }
        assertEquals(listOf("İyi", "günler", "görüşürüz", "Şimdi"), words)
    }

    @Test
    fun apostrophesStayInsideWords() {
        val words = "I'm ready; user's choice".split(Regex("[^\\p{L}'’]+"))
            .filter { it.isNotBlank() }
        assertEquals(listOf("I'm", "ready", "user's", "choice"), words)
    }
}
