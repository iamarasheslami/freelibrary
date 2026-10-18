package com.freelibrary.app.domain.model

/**
 * The kinds of book file the app can download and open. The order here is the order they are
 * offered in: the first one a book has is the default.
 */
enum class BookFileFormat(
    /** The name the catalog uses for this format. */
    val catalogName: String,
    /** The ending of the file on the phone. */
    val extension: String,
) {
    EPUB("epub", "epub"),
    PDF("pdf", "pdf"),
    TEXT("txt", "txt"),
    ;

    companion object {
        /** The format the catalog calls [name], or null when the app cannot open it. */
        fun fromCatalog(name: String): BookFileFormat? = entries.firstOrNull { it.catalogName.equals(name.trim(), ignoreCase = true) }

        /** The formats of [available] this app can open, in the order they are offered, each once. */
        fun offered(available: Collection<String>): List<BookFileFormat> {
            val known = available.mapNotNull { fromCatalog(it) }.toSet()
            return entries.filter { it in known }
        }
    }
}
