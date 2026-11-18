package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class WordContextParserTest {
    @Test
    fun extractsTurkishWordImmediatelyBeforeCursor() {
        assertEquals("çağrı", WordContextParser.trailingWord("Merhaba çağrı"))
    }

    @Test
    fun retainsInternalStraightAndCurlyApostrophes() {
        assertEquals("rock'n'roll", WordContextParser.trailingWord("rock'n'roll"))
        assertEquals("l’esprit", WordContextParser.trailingWord("l’esprit"))
    }

    @Test
    fun returnsEmptyAfterWhitespaceOrPunctuation() {
        assertEquals("", WordContextParser.trailingWord("merhaba "))
        assertEquals("", WordContextParser.trailingWord("merhaba!"))
        assertEquals("", WordContextParser.trailingWord("merhaba—"))
    }

    @Test
    fun rejectsLeadingOrTrailingApostrophes() {
        assertEquals("", WordContextParser.trailingWord("'merhaba"))
        assertEquals("", WordContextParser.trailingWord("merhaba'"))
        assertEquals("", WordContextParser.trailingWord("’merhaba’"))
    }

    @Test
    fun keepsDecomposedUnicodeLettersTogether() {
        val decomposed = "cafe\\u0301"
        assertEquals(decomposed, WordContextParser.trailingWord(decomposed))
        assertEquals(listOf(decomposed), WordContextParser.words(decomposed))
    }

    @Test
    fun handlesEmptyAndPunctuationOnlyInput() {
        assertEquals("", WordContextParser.trailingWord(""))
        assertEquals("", WordContextParser.trailingWord("..."))
    }
}
