package com.erdin.klavye

/**
 * Removes every learned phrase that contains a deleted personal word.
 * Tokens are compared exactly so deleting "ev" never removes "evet".
 */
internal object PersonalContextPruner {
    fun removeWord(
        pairCounts: MutableMap<String, Int>,
        trigramCounts: MutableMap<String, Int>,
        word: String
    ) {
        if (word.isBlank()) return
        pairCounts.keys.removeAll { key -> key.split('>').any { it == word } }
        trigramCounts.keys.removeAll { key -> key.split('>').any { it == word } }
    }
}
