package com.erdin.klavye

import android.text.InputType
import android.view.inputmethod.EditorInfo

/**
 * Centralizes editor-field privacy and convenience rules so input restarts
 * and in-session subtype changes cannot drift apart.
 */
internal data class EditorInputPolicy(
    val suggestionsEnabled: Boolean,
    val clipboardEnabled: Boolean,
    val doubleSpacePeriodEnabled: Boolean,
    val learningEnabled: Boolean
) {
    companion object {
        fun from(
            inputType: Int,
            imeOptions: Int,
            personalLearningPreference: Boolean,
            suggestionsPreference: Boolean
        ): EditorInputPolicy {
            val inputClass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val plainTextField = inputClass == InputType.TYPE_CLASS_TEXT &&
                variation == InputType.TYPE_TEXT_VARIATION_NORMAL
            val passwordField = (inputClass == InputType.TYPE_CLASS_TEXT &&
                variation in setOf(
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                )) || (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            val explicitlyNoSuggestions = inputClass == InputType.TYPE_CLASS_TEXT &&
                (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
            val editorDisablesPersonalization =
                imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

            return EditorInputPolicy(
                suggestionsEnabled = plainTextField && !passwordField &&
                    !explicitlyNoSuggestions && suggestionsPreference,
                clipboardEnabled = inputClass == InputType.TYPE_CLASS_TEXT &&
                    !passwordField && !editorDisablesPersonalization,
                doubleSpacePeriodEnabled = plainTextField && !passwordField &&
                    !explicitlyNoSuggestions,
                learningEnabled = plainTextField && !passwordField &&
                    !explicitlyNoSuggestions && personalLearningPreference &&
                    !editorDisablesPersonalization
            )
        }
    }
}
