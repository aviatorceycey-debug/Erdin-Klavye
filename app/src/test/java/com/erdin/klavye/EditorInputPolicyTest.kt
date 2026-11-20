package com.erdin.klavye

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInputPolicyTest {
    private fun policy(
        inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL,
        imeOptions: Int = 0,
        learning: Boolean = true,
        suggestions: Boolean = true,
        swipeTyping: Boolean = true
    ) = EditorInputPolicy.from(inputType, imeOptions, learning, suggestions, swipeTyping)

    @Test
    fun ordinaryTextEnablesExpectedFeatures() {
        val result = policy()
        assertTrue(result.suggestionsEnabled)
        assertTrue(result.clipboardEnabled)
        assertTrue(result.doubleSpacePeriodEnabled)
        assertTrue(result.learningEnabled)
        assertTrue(result.swipeTypingEnabled)
    }

    @Test
    fun passwordFieldsDisableSuggestionsClipboardLearningAndSwipeTyping() {
        val textPasswords = listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )
        textPasswords.forEach { variation ->
            val result = policy(InputType.TYPE_CLASS_TEXT or variation)
            assertFalse(result.suggestionsEnabled)
            assertFalse(result.clipboardEnabled)
            assertFalse(result.learningEnabled)
            assertFalse(result.doubleSpacePeriodEnabled)
            assertFalse(result.swipeTypingEnabled)
        }
        val numericPassword = policy(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )
        assertFalse(numericPassword.clipboardEnabled)
        assertFalse(numericPassword.learningEnabled)
        assertFalse(numericPassword.swipeTypingEnabled)
    }

    @Test
    fun editorPersonalizationFlagDisablesPersonalLearningButKeepsBaseSwipeTyping() {
        val result = policy(imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(result.clipboardEnabled)
        assertFalse(result.learningEnabled)
        assertTrue(result.suggestionsEnabled)
        assertTrue(result.swipeTypingEnabled)
    }

    @Test
    fun explicitNoSuggestionsDisablesLearningAndConvenienceFeatures() {
        val result = policy(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        )
        assertFalse(result.suggestionsEnabled)
        assertFalse(result.learningEnabled)
        assertFalse(result.doubleSpacePeriodEnabled)
        assertFalse(result.swipeTypingEnabled)
    }

    @Test
    fun userPreferencesCanDisableFeaturesWithoutChangingFieldPrivacy() {
        val result = policy(learning = false, suggestions = false)
        assertFalse(result.learningEnabled)
        assertFalse(result.suggestionsEnabled)
        assertTrue(result.clipboardEnabled)
        assertTrue(result.swipeTypingEnabled)
    }

    @Test
    fun nonPlainTextFieldsDoNotEnableSuggestionsLearningOrSwipeTyping() {
        val result = policy(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        assertFalse(result.suggestionsEnabled)
        assertFalse(result.learningEnabled)
        assertTrue(result.clipboardEnabled)
        assertFalse(result.swipeTypingEnabled)
    }

    @Test
    fun userCanDisableSwipeTypingWithoutDisablingOtherFeatures() {
        val result = policy(swipeTyping = false)
        assertFalse(result.swipeTypingEnabled)
        assertTrue(result.suggestionsEnabled)
        assertTrue(result.learningEnabled)
    }
}
