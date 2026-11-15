package com.erdin.klavye

import android.inputmethodservice.InputMethodService
import android.view.View

class ErdinImeService : InputMethodService() {
    override fun onCreateInputView(): View = ErdinKeyboardView(this) { value ->
        val connection = currentInputConnection ?: return@ErdinKeyboardView
        when (value) {
            "⌫" -> connection.deleteSurroundingText(1, 0)
            "⌫⌫" -> connection.deleteSurroundingText(5, 0)
            "↵" -> connection.commitText("\n", 1)
            "🌐" -> switchToNextInputMethod(false)
            else -> connection.commitText(value, 1)
        }
    }
}