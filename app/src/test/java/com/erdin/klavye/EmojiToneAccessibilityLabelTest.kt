package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiToneAccessibilityLabelTest {
    @Test
    fun identifiesResetChoiceAsDefaultSkinToneInTurkish() {
        assertEquals(
            "varsayılan ten rengi: 👍",
            EmojiToneAccessibilityLabel.describe(0, "👍", false)
        )
    }

    @Test
    fun identifiesAllFiveModifiedTonesInEnglish() {
        assertEquals("light skin tone: 👍🏻", EmojiToneAccessibilityLabel.describe(1, "👍🏻", true))
        assertEquals("medium-light skin tone: 👍🏼", EmojiToneAccessibilityLabel.describe(2, "👍🏼", true))
        assertEquals("medium skin tone: 👍🏽", EmojiToneAccessibilityLabel.describe(3, "👍🏽", true))
        assertEquals("medium-dark skin tone: 👍🏾", EmojiToneAccessibilityLabel.describe(4, "👍🏾", true))
        assertEquals("dark skin tone: 👍🏿", EmojiToneAccessibilityLabel.describe(5, "👍🏿", true))
    }

    @Test
    fun invalidOptionIndexFallsBackToDefaultLabel() {
        assertEquals(
            "default skin tone: 👍",
            EmojiToneAccessibilityLabel.describe(9, "👍", true)
        )
    }
}
