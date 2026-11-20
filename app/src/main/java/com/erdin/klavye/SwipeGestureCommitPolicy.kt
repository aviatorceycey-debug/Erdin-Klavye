package com.erdin.klavye

/** Keeps swipe commits tied to a valid letter-key release location. */
internal object SwipeGestureCommitPolicy {
    fun shouldCommitWord(word: String, releaseOnLetterKey: Boolean): Boolean =
        word.length >= 2 && releaseOnLetterKey

    fun shouldCommitBaseKey(releaseOnStartKey: Boolean): Boolean = releaseOnStartKey
}
