package com.erdin.klavye

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

class ErdinImeService : InputMethodService() {
    override fun onCreateInputView(): View = ErdinKeyboardView(this) { value ->
        val connection = currentInputConnection ?: return@ErdinKeyboardView
        when (value) {
            "⌫" -> {
                // Delete a complete Unicode code point so emoji are not cut in half.
                if (!connection.deleteSurroundingTextInCodePoints(1, 0)) {
                    connection.deleteSurroundingText(1, 0)
                }
            }
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE &&
                    action != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    connection.performEditorAction(action)
                } else {
                    connection.commitText("\n", 1)
                }
            }
            "🌐" -> switchToNextInputMethod(false)
            else -> connection.commitText(value, 1)
        }
    }
}
