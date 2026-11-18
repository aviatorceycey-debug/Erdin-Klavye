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
    fun remainingFrequentlyUsedEmojiHaveLocalizedNames() {
        assertEquals("köpek yüzü", EmojiAccessibilityLabeler.label("🐶", false))
        assertEquals("red apple", EmojiAccessibilityLabeler.label("🍎", true))
        assertEquals("araba", EmojiAccessibilityLabeler.label("🚗", false))
        assertEquals("mobile phone", EmojiAccessibilityLabeler.label("📱", true))
        assertEquals("uçak", EmojiAccessibilityLabeler.label("✈️", false))
        assertEquals("globe with meridians", EmojiAccessibilityLabeler.label("🌐", true))
        assertEquals("oje süren el", EmojiAccessibilityLabeler.label("💅", false))
    }

    @Test
    fun commonHeartAndExpressionEmojiHaveLocalizedNames() {
        assertEquals("kırmızı kalp", EmojiAccessibilityLabeler.label("❤️", false))
        assertEquals("blue heart", EmojiAccessibilityLabeler.label("💙", true))
        assertEquals("kırık kalp", EmojiAccessibilityLabeler.label("💔", false))
        assertEquals("winking face", EmojiAccessibilityLabeler.label("😉", true))
        assertEquals("yalvaran yüz", EmojiAccessibilityLabeler.label("🥺", false))
        assertEquals("hugging face", EmojiAccessibilityLabeler.label("🤗", true))
    }

    @Test
    fun variationSelectorsDoNotChangeAccessibleNames() {
        assertEquals("kırmızı kalp", EmojiAccessibilityLabeler.label("❤", false))
        assertEquals("airplane", EmojiAccessibilityLabeler.label("✈", true))
    }

    @Test
    fun mixedSkinToneHandshakeIsDescribedClearly() {
        assertEquals(
            "tokalaşma, farklı ten tonları",
            EmojiAccessibilityLabeler.label("🫱🏽‍🫲🏻", false)
        )
        assertEquals(
            "handshake, mixed skin tones",
            EmojiAccessibilityLabeler.label("🫱🏽‍🫲🏻", true)
        )
    }

    @Test
    fun unknownEmojiKeepsAUsefulFallbackLabel() {
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫥", false))
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫥", true))
    }
}
