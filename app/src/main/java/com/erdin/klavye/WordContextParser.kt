package com.erdin.klavye

/** Shared Unicode-aware tokenizer for local phrase context. */
internal object WordContextParser {
    private val separators = Regex("[^\\p{L}'’]+")

    fun words(text: CharSequence): List<String> =
        text.split(separators).filter { it.isNotBlank() }

    /**
     * Returns the word immediately before the cursor, or an empty string when
     * the cursor is after whitespace/punctuation or inside a non-word boundary.
     * Apostrophes are retained only when surrounded by letters.
     */
    fun trailingWord(text: CharSequence): String {
        val candidate = text.takeLastWhile {
            it.isLetter() || it == '\\'' || it == '’'
        }
        if (candidate.firstOrNull()?.isLetter() != true ||
            candidate.lastOrNull()?.isLetter() != true
        ) return ""
        return candidate.toString()
    }
}
