package com.erdin.klavye

/**
 * Maps the seven emoji toolbar virtual IDs to their category indices.
 * Keeping the range in one place prevents the final category from being
 * accidentally omitted by accessibility click handling.
 */
internal object EmojiCategoryAccessibility {
    private const val FIRST_VIRTUAL_ID = 2100
    private const val CATEGORY_COUNT = 7

    fun indexFor(virtualViewId: Int): Int? =
        (virtualViewId - FIRST_VIRTUAL_ID).takeIf { it in 0 until CATEGORY_COUNT }
}
