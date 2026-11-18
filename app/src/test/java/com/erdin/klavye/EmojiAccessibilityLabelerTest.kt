package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiAccessibilityLabelerTest {
    @Test
    fun knownEmojiHaveTurkishAndEnglishNames() {
        assertEquals("başparmak yukarı", EmojiAccessibilityLabeler.label("👍", false))
        assertEquals("thumbs up", EmojiAccessibilityLabeler.label("👍", true))
    }

    @Test
    fun selectedSkinToneIsIncludedInAccessibleName() {
        assertEquals(
            "alkış, orta ten rengi",
            EmojiAccessibilityLabeler.label("👏🏽", false)
        )
        assertEquals(
            "clapping hands, medium skin tone",
            EmojiAccessibilityLabeler.label("👏🏽", true)
        )
    }

    @Test
    fun compoundEmojiSkinToneIsIncludedInAccessibleName() {
        assertEquals(
            "bebek besleyen kişi, orta ten rengi",
            EmojiAccessibilityLabeler.label("🧑🏽‍🍼", false)
        )
        assertEquals(
            "person feeding baby, medium skin tone",
            EmojiAccessibilityLabeler.label("🧑🏽‍🍼", true)
        )
    }

    @Test
    fun natureFoodTravelAndObjectEmojiHaveLocalizedNames() {
        assertEquals("kedi yüzü", EmojiAccessibilityLabeler.label("🐱", false))
        assertEquals("butterfly", EmojiAccessibilityLabeler.label("🦋", true))
        assertEquals("pizza", EmojiAccessibilityLabeler.label("🍕", true))
        assertEquals("pusula", EmojiAccessibilityLabeler.label("🧭", false))
        assertEquals("klavye", EmojiAccessibilityLabeler.label("⌨️", false))
        assertEquals("bebek besleyen kişi", EmojiAccessibilityLabeler.label("🧑‍🍼", false))
        assertEquals("star", EmojiAccessibilityLabeler.label("⭐", true))
    }

    @Test
    fun unknownEmojiKeepsAUsefulFallbackLabel() {
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫥", false))
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫥", true))
    }
}
