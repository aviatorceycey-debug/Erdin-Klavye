package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnicodeWordNormalizerTest {
    @Test
    fun composesDecomposedAccentsAfterLocaleAwareLowercasing() {
        assertEquals("café", UnicodeWordNormalizer.normalize("cafe\u0301", Locale.US))
    }

    @Test
    fun preservesTurkishDottedAndDotlessLetterRules() {
        val turkish = Locale("tr", "TR")
        assertEquals("i", UnicodeWordNormalizer.normalize("İ", turkish))
        assertEquals("ı", UnicodeWordNormalizer.normalize("I", turkish))
    }

    @Test
    fun trimsBeforeCanonicalNormalization() {
        assertEquals("é", UnicodeWordNormalizer.normalize("  e\u0301  ", Locale.US))
    }
}
