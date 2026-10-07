package com.freelibrary.app.domain.model

/** A book as a card in a slider or list shows it. [authorName] is the first credited person. */
data class BookCard(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)

/** One home screen shelf slider: the first books of a shelf and how many it holds in all. */
data class ShelfSlider(
    val shelf: HomeShelf,
    val books: List<BookCard>,
    val totalBooks: Int,
) {
    /** Whether the slider should offer "View more". */
    val hasMore: Boolean get() = totalBooks > books.size
}
