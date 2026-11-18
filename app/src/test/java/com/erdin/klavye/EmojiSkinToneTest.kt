package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiSkinToneTest {
    @Test
    fun appliesToneBeforeJoinerInCompoundEmoji() {
        assertEquals("🧑🏽‍🍼", EmojiSkinTone.apply("🧑‍🍼", "🏽"))
    }

    @Test
    fun appliesToneToRegularEmoji() {
        assertEquals("👏🏽", EmojiSkinTone.apply("👏", "🏽"))
    }

    @Test
    fun extractsToneFromCompoundEmoji() {
        assertEquals("🏽", EmojiSkinTone.modifierFrom("🧑🏽‍🍼"))
    }

    @Test
    fun emptyToneKeepsOriginalEmoji() {
        assertEquals("🧑‍🍼", EmojiSkinTone.apply("🧑‍🍼", ""))
    }
}
