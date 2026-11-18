package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Test

class AlternateAccessibilityNodeOrderTest {
    @Test
    fun placesLongPressOptionsImmediatelyAfterTheirSourceKey() {
        assertEquals(
            listOf(0, 1, 3000, 3001, 3002, 2, 3),
            AlternateAccessibilityNodeOrder.insertAfterKey(
                keyCount = 4,
                activeKeyIndex = 1,
                optionCount = 3
            )
        )
    }

    @Test
    fun appendsOptionsWhenSourceKeyIsNoLongerVisible() {
        assertEquals(
            listOf(0, 1, 2, 3000, 3001),
            AlternateAccessibilityNodeOrder.insertAfterKey(
                keyCount = 3,
                activeKeyIndex = -1,
                optionCount = 2
            )
        )
    }

    @Test
    fun keepsKeysUnchangedWhenNoPopupIsOpen() {
        assertEquals(
            listOf(0, 1, 2),
            AlternateAccessibilityNodeOrder.insertAfterKey(
                keyCount = 3,
                activeKeyIndex = 1,
                optionCount = 0
            )
        )
    }
}
