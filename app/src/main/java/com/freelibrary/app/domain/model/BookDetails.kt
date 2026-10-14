package com.freelibrary.app.domain.model

/** A person credited on a book, with the years the catalog knows for them. */
data class BookAuthorCredit(
    val name: String,
    val birthYear: Int?,
    val deathYear: Int?,
)

/**
 * Everything a book's own page shows. [authors] are in credit order, so the first one is the
 * author shown on the book's card. [summary] is null when the catalog has none.
 */
data class BookDetails(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val coverUrl: String?,
    val language: String?,
    val authors: List<BookAuthorCredit>,
    val summary: String?,
) {
    /** The same book as a card, which is what the cover code draws from. */
    fun toCard(): BookCard =
        BookCard(
            bookId = bookId,
            externalId = externalId,
            title = title,
            authorName = authors.firstOrNull()?.name,
            coverUrl = coverUrl,
        )
}
