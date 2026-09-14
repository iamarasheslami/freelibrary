package com.freelibrary.catalogtool

import java.nio.file.Path

/**
 * Entry point for the catalog-generation tool. This is a standalone JVM
 * command-line program - it is never shipped inside the Android app. Its job
 * is to download Project Gutenberg's RDF catalog and produce a SQLite
 * database matching the app's Room schema.
 */
fun main() {
    val archivePath = Path.of("data/rdf-files.tar.bz2")

    try {
        downloadCatalogArchive(archivePath)
    } catch (e: CatalogDownloadException) {
        println("Catalog download failed: ${e.message}")
        return
    }

    var parsedCount = 0
    var skippedCount = 0
    var failedCount = 0
    var zeroFormatCount = 0
    val failureSamples = mutableListOf<String>()
    val zeroFormatSamples = mutableListOf<String>()

    readRdfEntriesFromArchive(archivePath) { entryName, content ->
        try {
            val book = parseRdf(content)
            if (book != null) {
                parsedCount++
                if (book.formats.isEmpty()) {
                    zeroFormatCount++
                    if (zeroFormatSamples.size < 5) {
                        zeroFormatSamples.add("${book.externalId}: ${book.title}")
                    }
                }
            } else {
                skippedCount++
            }
        } catch (e: RdfParseException) {
            failedCount++
            if (failureSamples.size < 5) {
                failureSamples.add("$entryName: ${e.message}")
            }
        }
    }

    println("Parsed: $parsedCount, skipped (non-Text/no title): $skippedCount, failed: $failedCount")
    println("Parsed books with zero formats: $zeroFormatCount")

    if (failureSamples.isNotEmpty()) {
        println("Sample failures:")
        failureSamples.forEach { println("  $it") }
    }
    if (zeroFormatSamples.isNotEmpty()) {
        println("Sample zero-format books:")
        zeroFormatSamples.forEach { println("  $it") }
    }
}
