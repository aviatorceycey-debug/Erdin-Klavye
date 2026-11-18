package com.erdin.klavye

object EmojiSkinTone {
    private val modifiers = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    fun apply(emoji: String, tone: String): String {
        // Preferences can be edited or migrated independently of this code.
        // Never append arbitrary persisted text to an emoji sequence.
        if (tone.isEmpty() || tone !in modifiers) return emoji
        return when (emoji) {
            "🧑‍🍼" -> "🧑${tone}‍🍼"
            "🫱‍🫲" -> "🫱${tone}‍🫲${tone}"
            else -> emoji + tone
        }
    }

    fun modifierFrom(emoji: String): String =
        modifiers.firstOrNull { emoji.contains(it) }.orEmpty()
}
