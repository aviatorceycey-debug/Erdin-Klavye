package com.erdin.klavye

object EmojiSkinTone {
    private val modifiers = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    fun apply(emoji: String, tone: String): String {
        if (tone.isEmpty()) return emoji
        return if (emoji == "🧑‍🍼") "🧑${tone}‍🍼" else emoji + tone
    }

    fun modifierFrom(emoji: String): String =
        modifiers.firstOrNull { emoji.contains(it) }.orEmpty()
}
