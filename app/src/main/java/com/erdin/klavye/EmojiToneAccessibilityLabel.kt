package com.erdin.klavye

/** Localized names for the six long-press skin-tone choices, including the reset choice. */
internal object EmojiToneAccessibilityLabel {
    private val turkish = listOf(
        "varsayılan ten rengi",
        "açık ten rengi",
        "açık-orta ten rengi",
        "orta ten rengi",
        "orta-koyu ten rengi",
        "koyu ten rengi"
    )
    private val english = listOf(
        "default skin tone",
        "light skin tone",
        "medium-light skin tone",
        "medium skin tone",
        "medium-dark skin tone",
        "dark skin tone"
    )

    fun describe(optionIndex: Int, emoji: String, isEnglish: Boolean): String {
        val labels = if (isEnglish) english else turkish
        val tone = labels.getOrNull(optionIndex) ?: labels.first()
        return "$tone: $emoji"
    }
}
