package com.freelibrary.app.domain.model

/** A book as a card in a slider or list shows it. [authorName] is the first credited person. */
data class BookCard(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)

/**
 * The first books of a list and how many it holds, as a slider shows them. For a catalog shelf
 * [totalBooks] is counted only up to one more than the slider shows, which is enough to tell
 * whether to offer "View more".
 */
data class BookSlider(
    val books: List<BookCard>,
    val totalBooks: Int,
) {
    /** Whether the slider should offer "View more". */
    val hasMore: Boolean get() = totalBooks > books.size
}
