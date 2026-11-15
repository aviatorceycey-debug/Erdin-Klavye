package com.erdin.klavye

import android.inputmethodservice.InputMethodService
import android.view.View

class ErdinImeService : InputMethodService() {
    override fun onCreateInputView(): View = ErdinKeyboardView(this) { value ->
        val connection = currentInputConnection ?: return@ErdinKeyboardView
        when (value) {
            "⌫" -> connection.deleteSurroundingText(1, 0)
            "↵" -> connection.commitText("\n", 1)
            "⇧" -> { /* shift state is handled by the keyboard view */ }
            "🌐" -> request לה
            else -> connection.commitText(value, 1)
        }
    }
}
