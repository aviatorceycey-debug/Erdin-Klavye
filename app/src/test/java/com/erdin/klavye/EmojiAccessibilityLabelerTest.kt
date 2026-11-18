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
    fun unknownEmojiKeepsAUsefulFallbackLabel() {
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫄", false))
        assertEquals("emoji", EmojiAccessibilityLabeler.label("🫄", true))
    }
}
