package com.freelibrary.app.data.local.dao

/**
 * One row of a search-candidate query: a matching book plus one of its
 * credited people. A book with several credits appears on several rows, in
 * credit order; [authorName] is null for a book with no credits.
 */
data class SearchRow(
    val bookId: Long,
    val externalId: String,
    val title: String,
    val authorName: String?,
)
