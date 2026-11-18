package com.erdin.klavye

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalWordValidatorTest {
    @Test
    fun acceptsOrdinaryTurkishWords() {
        listOf("merhaba", "ışık", "öğrenci", "güzel").forEach {
            assertTrue("$it should be valid", PersonalWordValidator.isValid(it))
        }
    }

    @Test
    fun acceptsTurkishApostropheSuffixes() {
        listOf("Ankara'nın", "İzmir’e", "TBMM'ye").forEach {
            assertTrue("$it should be valid", PersonalWordValidator.isValid(it))
        }
    }

    @Test
    fun acceptsDecomposedUnicodeLetters() {
        assertTrue(PersonalWordValidator.isValid("cafe\u0301"))
        assertTrue(PersonalWordValidator.isValid("cafe\u0301's"))
        assertTrue(PersonalWordValidator.isValid("cafe\u0301’s"))
    }

    @Test
    fun rejectsNumbersSpacesAndPunctuation() {
        listOf("abc1", "iki kelime", "merhaba!", "12345", "").forEach {
            assertFalse("$it should be invalid", PersonalWordValidator.isValid(it))
        }
    }

    @Test
    fun rejectsMalformedApostrophes() {
        listOf("'ankara", "ankara'", "Ankara''ya", "Ankara'’ya", "Ankara’'ya").forEach {
            assertFalse("$it should be invalid", PersonalWordValidator.isValid(it))
        }
    }

    @Test
    fun rejectsSingleCharacterTokens() {
        assertFalse(PersonalWordValidator.isValid("a"))
        assertFalse(PersonalWordValidator.isValid("ı"))
    }
}
