package com.erdin.klavye

import java.text.Normalizer
import java.util.Locale

/** Canonicalizes user text so composed and decomposed Unicode spellings match. */
internal object UnicodeWordNormalizer {
    fun normalize(value: String, locale: Locale): String =
        Normalizer.normalize(value.trim().lowercase(locale), Normalizer.Form.NFC)
}
