package com.erdin.klavye

object EmojiSkinTone {
    private val modifiers = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    fun apply(emoji: String, tone: String): String {
        if (tone.isEmpty()) return emoji
        return when (emoji) {
            "🧑‍🍼" -> "🧑${tone}‍🍼"
            "🫱‍🫲" -> "🫱${tone}‍🫲${tone}"
            else -> emoji + tone
        }
    }

    fun modifierFrom(emoji: String): String =
        modifiers.firstOrNull { emoji.contains(it) }.orEmpty()
}
