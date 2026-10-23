package com.freelibrary.catalogtool

private const val DEFAULT_PAGES = 3

private class PreviewDone : RuntimeException()

/**
 * Reads the first few pages of the OAPEN books feed (the number is the first argument, 3 by default) and
 * prints what the parser made of them, without touching any database or file. Run it with
 * `./gradlew :catalog-tool:oapenPreview -Ppages=5` to check the parser against the live feed.
 */
fun main(args: Array<String>) {
    val limit = args.firstOrNull()?.toIntOrNull() ?: DEFAULT_PAGES
    val books = mutableListOf<OaiBook>()
    var skipped = 0
    var reportedTotal: Int? = null
    var pages = 0

    try {
        harvestOapen { page ->
            pages++
            skipped += page.skipped
            books.addAll(page.books)
            reportedTotal = page.completeListSize ?: reportedTotal
            if (pages >= limit) throw PreviewDone()
        }
    } catch (done: PreviewDone) {
        println("Stopped after $pages page(s) as asked.")
    }

    val languages =
        books.groupingBy { it.book.language ?: "none" }.eachCount().entries
            .sortedByDescending { it.value }
            .joinToString { "${it.key}=${it.value}" }

    println("The server reports $reportedTotal records in the set.")
    println("Books kept: ${books.size}, records skipped: $skipped")
    println("Without a cover: ${books.count { it.book.coverUrl == null }}")
    println("Without authors: ${books.count { it.book.creators.isEmpty() }}")
    println("Without a summary: ${books.count { it.book.summary == null }}")
    println("With an EPUB: ${books.count { book -> book.book.formats.any { it.mimeType.contains("epub") } }}")
    println("Languages: $languages")
    println("Samples:")
    books.take(3).forEach { println("  ${it.lastModified} ${it.book}") }
}
