package com.freelibrary.app.data.local.dao

/**
 * The format and genre conditions of a filter, shared by every query that takes one. The query
 * needs the books table under the alias b and the parameters :anyFormat, :formats, :anyGenre and
 * :genres. "Any" is true when nothing is chosen, which also makes an empty list harmless.
 */
internal const val BOOK_FILTER_SQL =
    """
    AND (:anyFormat OR EXISTS (
        SELECT 1 FROM book_formats AS bf WHERE bf.bookId = b.id AND bf.formatType IN (:formats)))
    AND (:anyGenre OR EXISTS (
        SELECT 1 FROM book_bookshelves AS gb INNER JOIN bookshelves AS gs ON gs.id = gb.bookshelfId
        WHERE gb.bookId = b.id AND gs.name IN (:genres)))
    """
