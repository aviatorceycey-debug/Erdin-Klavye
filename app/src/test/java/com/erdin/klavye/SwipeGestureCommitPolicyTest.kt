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
    fun failedSwipeFallsBackToTapOnlyOnItsStartingKey() {
        assertTrue(SwipeGestureCommitPolicy.shouldCommitBaseKey(true))
        assertFalse(SwipeGestureCommitPolicy.shouldCommitBaseKey(false))
    }
}
