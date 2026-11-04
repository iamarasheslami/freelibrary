package com.freelibrary.app.data.download

import com.freelibrary.app.domain.model.BookFileFormat
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Where to try a book file from, in order. The first address is always the catalog's own. For a file
 * of Project Gutenberg, the addresses of its mirrors follow, in the order below; that order is fixed,
 * and a download simply goes down the list until one of them works. A book that is not from Project
 * Gutenberg, a PDF, or a book numbered under 10 (mirrors lay those out differently) has only its own address.
 */
object DownloadSources {
    /** Mirrors that carry the plain text files, tried in this order. */
    private val TEXT_MIRRORS =
        listOf(
            "https://gutenberg.pglaf.org",
            "https://aleph.gutenberg.org",
            "https://www.mirrorservice.org/sites/ftp.ibiblio.org/pub/docs/books/gutenberg",
            "https://mirrors.xmission.com/gutenberg",
        )

    /** Mirrors that carry the generated EPUB files. */
    private val EPUB_MIRRORS = listOf("https://gutenberg.pglaf.org")

    private val OFFICIAL = Regex("^https?://(?:www\\.)?gutenberg\\.org/(?:ebooks|files|cache/epub)/(\\d+)(?:[/.]|$)")

    private const val FIRST_MIRRORED_NUMBER = 10L

    private val MIRROR_HOSTS = (TEXT_MIRRORS + EPUB_MIRRORS).map { it.toHttpUrl().host }.toSet()

    /**
     * Whether a book may be downloaded from [url]: over https, from Project Gutenberg or one of the mirrors above.
     * It is checked for the first address and for every redirect, so a changed catalog cannot send a download elsewhere.
     */
    fun isAllowed(url: HttpUrl): Boolean {
        if (!url.isHttps) return false
        val host = url.host
        return host == "gutenberg.org" || host.endsWith(".gutenberg.org") || host in MIRROR_HOSTS
    }

    /** The addresses to try for the [format] file whose catalog address is [url], the catalog's own first. */
    fun candidates(
        url: String,
        format: BookFileFormat,
    ): List<String> {
        val number = OFFICIAL.find(url)?.groupValues?.get(1)?.toLongOrNull()
        if (number == null || number < FIRST_MIRRORED_NUMBER) return listOf(url)
        val folder = number.toString().dropLast(1).toList().joinToString("/")
        val mirrored =
            when (format) {
                BookFileFormat.TEXT ->
                    TEXT_MIRRORS.flatMap { base ->
                        listOf("$base/$folder/$number/$number-0.txt", "$base/$folder/$number/$number.txt")
                    }
                BookFileFormat.EPUB -> EPUB_MIRRORS.map { base -> "$base/cache/epub/$number/pg$number.epub" }
                BookFileFormat.PDF -> emptyList()
            }
        return (listOf(url) + mirrored).distinct()
    }
}
