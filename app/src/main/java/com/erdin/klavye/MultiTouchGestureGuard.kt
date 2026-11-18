package com.erdin.klavye

import android.view.MotionEvent

/**
 * Suppresses the remainder of a key gesture after a second pointer appears.
 * The keyboard intentionally supports single-finger interaction only.
 */
internal class MultiTouchGestureGuard {
    private var ignoringUntilAllPointersUp = false

    fun beginGesture() {
        ignoringUntilAllPointersUp = false
    }

    fun onAdditionalPointerDown() {
        ignoringUntilAllPointersUp = true
    }

    fun shouldIgnore(action: Int): Boolean {
        if (!ignoringUntilAllPointersUp) return false
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            ignoringUntilAllPointersUp = false
        }
        return true
    }
}
