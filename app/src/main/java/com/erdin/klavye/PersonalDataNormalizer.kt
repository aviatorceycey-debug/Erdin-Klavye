package com.erdin.klavye

import java.util.Locale

/** Canonicalizes persisted personal-learning keys and safely merges equivalent entries. */
internal object PersonalDataNormalizer {
    fun normalizeCounts(source: Map<String, Int>, locale: Locale): MutableMap<String, Int> {
        val normalized = mutableMapOf<String, Int>()
        source.forEach { (key, count) ->
            val canonicalKey = key.split('>').joinToString(">") {
                UnicodeWordNormalizer.normalize(it, locale)
            }
            normalized[canonicalKey] = PersonalCount.combine(
                normalized[canonicalKey] ?: 0,
                count
            )
        }
        return normalized
    }
}
