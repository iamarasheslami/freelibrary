package com.freelibrary.app.ui.cover

private const val MEDIUM_SUFFIX = ".cover.medium.jpg"
private const val SMALL_SUFFIX = ".cover.small.jpg"

/**
 * The address of a book's small cover image, worked out from the medium one the catalog stores
 * ".../pg1342.cover.small.jpg"). The small cover is the lightest variant, which suits a slider
 * thumbnail on a low-memory phone.
 *
 * Returns null when there is no stored address or it does not have the expected shape, so the
 * caller shows the generated cover instead of requesting an address we only guessed at.
 */
fun smallCoverUrl(mediumUrl: String?): String? =
    mediumUrl
        ?.takeIf { it.startsWith("https://") && it.endsWith(MEDIUM_SUFFIX) }
        ?.removeSuffix(MEDIUM_SUFFIX)
        ?.plus(SMALL_SUFFIX)
