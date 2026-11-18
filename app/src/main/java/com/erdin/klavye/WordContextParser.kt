package com.erdin.klavye

/** Shared Unicode-aware tokenizer for local phrase context. */
internal object WordContextParser {
    private val separators = Regex("[^\\\\p{L}'’]+")

    fun words(text: CharSequence): List<String> =
        text.split(separators).filter { it.isNotBlank() }
}
