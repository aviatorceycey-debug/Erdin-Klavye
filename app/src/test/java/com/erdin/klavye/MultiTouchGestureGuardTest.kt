package com.erdin.klavye

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTouchGestureGuardTest {
    @Test
    fun additionalPointerCancelsGestureUntilFinalPointerLeaves() {
        val guard = MultiTouchGestureGuard()
        guard.beginGesture()
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_MOVE))

        guard.onAdditionalPointerDown()
        assertTrue(guard.shouldIgnore(MotionEvent.ACTION_POINTER_UP))
        assertTrue(guard.shouldIgnore(MotionEvent.ACTION_MOVE))
        assertTrue(guard.shouldIgnore(MotionEvent.ACTION_UP))
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_MOVE))
    }

    @Test
    fun cancellationAlsoReleasesGuard() {
        val guard = MultiTouchGestureGuard()
        guard.onAdditionalPointerDown()
        assertTrue(guard.shouldIgnore(MotionEvent.ACTION_CANCEL))
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_UP))
    }

    @Test
    fun newGestureAlwaysStartsClean() {
        val guard = MultiTouchGestureGuard()
        guard.onAdditionalPointerDown()
        guard.beginGesture()
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_MOVE))
    }

    @Test
    fun ordinarySinglePointerGestureIsUnaffected() {
        val guard = MultiTouchGestureGuard()
        guard.beginGesture()
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_MOVE))
        assertFalse(guard.shouldIgnore(MotionEvent.ACTION_UP))
    }
}
