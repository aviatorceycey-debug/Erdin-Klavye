package com.erdin.klavye

/** Shared Unicode-aware tokenizer for local phrase context. */
internal object WordContextParser {
    // Include combining marks so decomposed Unicode text remains one word.
    private val separators = Regex("[^\\\\p{L}\\\\p{M}'’]+")

    fun words(text: CharSequence): List<String> =
        text.split(separators).filter { it.isNotBlank() }

    /**
     * Returns the word immediately before the cursor, or an empty string when
     * the cursor is after whitespace/punctuation or inside a non-word boundary.
     * Apostrophes are retained only when surrounded by letters or combining marks.
     */
    fun trailingWord(text: CharSequence): String {
        val candidate = text.takeLastWhile(::isWordCharacter)
        if (candidate.firstOrNull()?.isLetter() != true ||
            candidate.lastOrNull()?.let(::isLetterOrMark) != true
        ) return ""
        return candidate.toString()
    }

    private fun isWordCharacter(char: Char): Boolean =
        isLetterOrMark(char) || char == '\\'' || char == '’'

    private fun isLetterOrMark(char: Char): Boolean =
        char.isLetter() || when (Character.getType(char)) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt() -> true
            else -> false
        }
}
