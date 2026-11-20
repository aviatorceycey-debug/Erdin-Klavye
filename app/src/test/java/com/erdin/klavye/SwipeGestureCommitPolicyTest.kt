package com.erdin.klavye

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeGestureCommitPolicyTest {
    @Test
    fun multiLetterTraceCommitsOnlyWhenReleasedOnALetterKey() {
        assertTrue(SwipeGestureCommitPolicy.shouldCommitWord("hello", true))
        assertFalse(SwipeGestureCommitPolicy.shouldCommitWord("hello", false))
    }

    @Test
    fun oneLetterTraceDoesNotCommitAsASwipeWord() {
        assertFalse(SwipeGestureCommitPolicy.shouldCommitWord("h", true))
    }

    @Test
    fun baseKeyCommitRequiresReleaseOnTheStartingKey() {
        // Covers both a failed swipe falling back to a tap and an ordinary tap.
        assertTrue(SwipeGestureCommitPolicy.shouldCommitBaseKey(true))
        assertFalse(SwipeGestureCommitPolicy.shouldCommitBaseKey(false))
    }
}
