package com.erdin.klavye

/** Validation for words stored in the private on-device language model. */
internal object PersonalWordValidator {
    fun isValid(word: String): Boolean {
        if (word.length < 2 || word.first().isLetter().not() || word.last().isLetter().not()) {
            return false
        }
        return word.indices.all { index ->
            val character = word[index]
            when (character) {
                '\'', '’' -> index > 0 && index < word.lastIndex &&
                    word[index - 1].isLetter() && word[index + 1].isLetter()
                else -> character.isLetter() || when (Character.getType(character)) {
                    Character.NON_SPACING_MARK.toInt(),
                    Character.COMBINING_SPACING_MARK.toInt(),
                    Character.ENCLOSING_MARK.toInt() -> index > 0 &&
                        (word[index - 1].isLetter() || Character.getType(word[index - 1]) == Character.NON_SPACING_MARK.toInt())
                    else -> false
                }
            }
        }
    }
}
