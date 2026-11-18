package com.erdin.klavye

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternatePopupLayoutTest {
    @Test
    fun keepsSixEmojiAlternativesInsideCompactView() {
        val bounds = AlternatePopupLayout.calculate(
            viewWidth = 240f, gap = 5f, density = 1f,
            anchorCenterX = 120f, anchorTop = 100f, optionCount = 6
        )

        assertEquals(230f, bounds.width, 0.001f)
        assertTrue(bounds.left >= 0f)
        assertTrue(bounds.right <= 240f)
    }

    @Test
    fun centersPopupAroundAnchorWhenThereIsEnoughRoom() {
        val bounds = AlternatePopupLayout.calculate(
            viewWidth = 400f, gap = 5f, density = 1f,
            anchorCenterX = 200f, anchorTop = 100f, optionCount = 3
        )

        assertEquals(138f, bounds.width, 0.001f)
        assertEquals(200f, (bounds.left + bounds.right) / 2f, 0.001f)
    }

    @Test
    fun clampsPopupToLeftAndRightEdges() {
        val left = AlternatePopupLayout.calculate(400f, 5f, 1f, 0f, 100f, 3)
        val right = AlternatePopupLayout.calculate(400f, 5f, 1f, 400f, 100f, 3)

        assertTrue(left.left >= 5f)
        assertTrue(right.right <= 395f)
    }

    @Test
    fun keepsPopupAboveKeyboardTopEvenWhenAnchorIsNearTop() {
        val bounds = AlternatePopupLayout.calculate(400f, 5f, 2f, 200f, 20f, 3)

        assertEquals(6f, bounds.top, 0.001f)
        assertTrue(bounds.bottom > bounds.top)
    }

    @Test
    fun returnsEmptyBoundsForInvalidGeometry() {
        val bounds = AlternatePopupLayout.calculate(
            viewWidth = Float.NaN, gap = 5f, density = 1f,
            anchorCenterX = 100f, anchorTop = 100f, optionCount = 6
        )

        assertEquals(0f, bounds.width, 0f)
        assertEquals(0f, bounds.height, 0f)
    }
}
