package com.freelibrary.app.ui.cover

private const val MEDIUM_SUFFIX = ".cover.medium.jpg"

/**
 * The address of a book's cover picture to load: the medium cover the catalog stores
 * (".../pg1342.cover.medium.jpg"). The small cover is too low in resolution and looks pixelated on
 * the screen, and Coil scales the medium one down to the size of the view, so the phone does not
 * hold the full picture in memory.
 *
 * Returns null when there is no stored address or it does not have the expected shape, so the
 * caller shows the generated cover instead of requesting an address we only guessed at.
 */
fun mediumCoverUrl(mediumUrl: String?): String? = mediumUrl?.takeIf { it.startsWith("https://") && it.endsWith(MEDIUM_SUFFIX) }
