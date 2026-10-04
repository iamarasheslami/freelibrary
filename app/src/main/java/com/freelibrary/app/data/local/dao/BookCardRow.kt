package com.freelibrary.app.data.local.dao

/**
 * A book as a card in a list or slider needs it: title, primary author and cover.
 * [authorName] is the first credited person, or null for a book with no credits.
 */
data class BookCardRow(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)
