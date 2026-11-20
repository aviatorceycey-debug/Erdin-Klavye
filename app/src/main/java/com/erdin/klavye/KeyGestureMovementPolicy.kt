package com.erdin.klavye

/** Describes which long-press callbacks must stop after leaving a key. */
internal data class KeyGestureMovementEffects(
    val cancelPendingLongPress: Boolean,
    val stopBackspaceRepeat: Boolean
)

internal object KeyGestureMovementPolicy {
    fun evaluate(
        releasedInsideKey: Boolean,
        longPressAlreadyStarted: Boolean,
        isBackspace: Boolean
    ): KeyGestureMovementEffects {
        if (releasedInsideKey) return KeyGestureMovementEffects(false, false)
        return KeyGestureMovementEffects(
            cancelPendingLongPress = !longPressAlreadyStarted,
            stopBackspaceRepeat = longPressAlreadyStarted && isBackspace
        )
    }
}
