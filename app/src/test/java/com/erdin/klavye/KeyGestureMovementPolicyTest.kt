package com.erdin.klavye

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyGestureMovementPolicyTest {
    @Test
    fun leavingBeforeLongPressCancelsPendingTimer() {
        val effects = KeyGestureMovementPolicy.evaluate(
            releasedInsideKey = false,
            longPressAlreadyStarted = false,
            isBackspace = false
        )
        assertTrue(effects.cancelPendingLongPress)
        assertFalse(effects.stopBackspaceRepeat)
    }

    @Test
    fun leavingHeldBackspaceStopsRepeatImmediately() {
        val effects = KeyGestureMovementPolicy.evaluate(
            releasedInsideKey = false,
            longPressAlreadyStarted = true,
            isBackspace = true
        )
        assertFalse(effects.cancelPendingLongPress)
        assertTrue(effects.stopBackspaceRepeat)
    }

    @Test
    fun leavingOtherActiveLongPressDoesNotStopBackspaceRepeat() {
        val effects = KeyGestureMovementPolicy.evaluate(
            releasedInsideKey = false,
            longPressAlreadyStarted = true,
            isBackspace = false
        )
        assertFalse(effects.cancelPendingLongPress)
        assertFalse(effects.stopBackspaceRepeat)
    }

    @Test
    fun movementInsideOriginalKeyKeepsLongPressActive() {
        val effects = KeyGestureMovementPolicy.evaluate(
            releasedInsideKey = true,
            longPressAlreadyStarted = false,
            isBackspace = true
        )
        assertFalse(effects.cancelPendingLongPress)
        assertFalse(effects.stopBackspaceRepeat)
    }
}
