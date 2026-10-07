package com.freelibrary.app.domain.model

/** A book as a card in a slider or list shows it. [authorName] is the first credited person. */
data class BookCard(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)

/** The first books of a list and how many it holds in all, as a slider shows them. */
data class BookSlider(
    val books: List<BookCard>,
    val totalBooks: Int,
) {
    /** Whether the slider should offer "View more". */
    val hasMore: Boolean get() = totalBooks > books.size
}
