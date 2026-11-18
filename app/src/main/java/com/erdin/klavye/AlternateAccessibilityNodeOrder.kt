package com.erdin.klavye

/** Keeps long-press alternatives adjacent to the key that opened their popup in TalkBack traversal. */
internal object AlternateAccessibilityNodeOrder {
    fun insertAfterKey(keyCount: Int, activeKeyIndex: Int, optionCount: Int): List<Int> {
        val ids = ArrayList<Int>(keyCount + optionCount)
        repeat(keyCount) { index ->
            ids += index
            if (index == activeKeyIndex) {
                repeat(optionCount) { optionIndex -> ids += 3000 + optionIndex }
            }
        }
        if (activeKeyIndex !in 0 until keyCount) {
            repeat(optionCount) { optionIndex -> ids += 3000 + optionIndex }
        }
        return ids
    }
}
