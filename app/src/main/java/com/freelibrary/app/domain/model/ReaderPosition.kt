package com.freelibrary.app.domain.model

/**
 * Where a reader is in a book. It is stored as text (in the reading progress, bookmarks and
 * highlights) and always starts with the kind of book it belongs to, so a position of one kind is
 * never mistaken for another's. Text that does not decode is simply ignored by the reader.
 */
sealed interface ReaderPosition {
    /** The text stored for this position. */
    fun encode(): String

    /**
     * A place in an EPUB: [chapter] counts the chapters in reading order from 0, and [permille] is
     * how far through that chapter, in thousandths from 0 to 1000.
     */
    data class Epub(
        val chapter: Int,
        val permille: Int,
    ) : ReaderPosition {
        init {
            require(chapter >= 0 && permille in 0..MAX_PERMILLE) { "Not a position in an EPUB: $chapter, $permille" }
        }

        override fun encode(): String = "epub:$chapter:$permille"
    }

    /** A page of a PDF, counted from 0. */
    data class Pdf(
        val page: Int,
    ) : ReaderPosition {
        init {
            require(page >= 0) { "Not a page: $page" }
        }

        override fun encode(): String = "pdf:$page"
    }

    /** A place in a plain text book: the number of characters before it. */
    data class Text(
        val offset: Int,
    ) : ReaderPosition {
        init {
            require(offset >= 0) { "Not an offset: $offset" }
        }

        override fun encode(): String = "text:$offset"
    }

    companion object {
        const val MAX_PERMILLE = 1000

        /** The position stored as [value], or null when it is not a position this version understands. */
        fun decode(value: String): ReaderPosition? {
            val parts = value.split(':')
            val numbers = parts.drop(1).map { it.toIntOrNull() ?: return null }
            if (numbers.any { it < 0 }) return null
            return when (parts[0]) {
                "epub" -> if (numbers.size == 2 && numbers[1] <= MAX_PERMILLE) Epub(numbers[0], numbers[1]) else null
                "pdf" -> numbers.singleOrNull()?.let { Pdf(it) }
                "text" -> numbers.singleOrNull()?.let { Text(it) }
                else -> null
            }
        }
    }
}
