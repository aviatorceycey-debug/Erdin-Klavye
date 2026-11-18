package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmojiCategoryAccessibilityTest {
    @Test
    fun mapsEveryVisibleEmojiCategoryIncludingObjects() {
        for (index in 0 until 7) {
            assertEquals(index, EmojiCategoryAccessibility.indexFor(2100 + index))
        }
    }

    @Test
    fun rejectsIdsOutsideEmojiCategoryRange() {
        assertNull(EmojiCategoryAccessibility.indexFor(2099))
        assertNull(EmojiCategoryAccessibility.indexFor(2107))
        assertNull(EmojiCategoryAccessibility.indexFor(3000))
    }
}
