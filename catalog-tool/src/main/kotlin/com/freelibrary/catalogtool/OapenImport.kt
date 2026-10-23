package com.freelibrary.catalogtool

import com.freelibrary.shared.Manifest
import com.freelibrary.shared.ManifestEntry
import java.nio.file.Path

const val OAPEN_SOURCE_NAME = "OAPEN Library"
const val OAPEN_SOURCE_ATTRIBUTION =
    "Open access books via the OAPEN Library (https://www.oapen.org). Each book carries its own open licence, usually CC BY."

data class OapenImportResult(
    val written: Int,
    val skipped: Int,
    val failed: Int,
    val failureSamples: List<String>,
)

/**
 * Writes every harvested book into the catalog database under its own source row and, beside it, the
 * per-book export files and a manifest in [sourceDir] (data/export/sources/oapen). [harvest] is given the
 * function to call with each page, so tests can feed pages without a network.
 */
fun importOapen(
    writer: CatalogWriter,
    sourceDir: Path,
    generatedAt: String,
    harvest: ((OaiPage) -> Unit) -> Unit,
): OapenImportResult {
    val sourceId = writer.getOrCreateSource(OAPEN_SOURCE_NAME, OAPEN_SOURCE_ATTRIBUTION)
    writer.commit()

    val booksDir = sourceDir.resolve("books")
    val entries = LinkedHashMap<String, ManifestEntry>()
    var skipped = 0
    var failed = 0
    var pageCount = 0
    val failureSamples = mutableListOf<String>()

    harvest { page ->
        skipped += page.skipped
        for (item in page.books) {
            try {
                writer.insertBook(sourceId, item.book, lastModified = item.lastModified)
                writeBookExport(booksDir, item.book.toExport(lastModified = item.lastModified))
                entries[item.book.externalId] = ManifestEntry(externalId = item.book.externalId, lastModified = item.lastModified)
            } catch (e: Exception) {
                failed++
                if (failureSamples.size < 5) failureSamples.add("${item.book.externalId}: ${e.message}")
            }
        }
        writer.commit()
        pageCount++
        if (pageCount % 10 == 0) println("OAPEN: $pageCount pages read, ${entries.size} books written...")
    }

    writeManifest(sourceDir, Manifest(generatedAt = generatedAt, baselineVersion = generatedAt, books = entries.values.toList()))
    return OapenImportResult(written = entries.size, skipped = skipped, failed = failed, failureSamples = failureSamples)
}
