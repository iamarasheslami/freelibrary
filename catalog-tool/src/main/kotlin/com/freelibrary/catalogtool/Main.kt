package com.freelibrary.catalogtool

import com.freelibrary.shared.Manifest
import com.freelibrary.shared.ManifestEntry
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

private const val SCHEMA_JSON_PATH = "../app/schemas/com.freelibrary.app.data.local.FreeLibraryDatabase/9.json"
private const val COMMIT_BATCH_SIZE = 500
private const val BASELINE_VERSION = "2026-09-16"

/**
 * Entry point for the catalog-generation tool. This is a standalone JVM
 * command-line program - it is never shipped inside the Android app. Its job
 * is to download Project Gutenberg's RDF catalog and produce both the
 * bundled SQLite database and the manifest/per-book export files used by
 * the app's ongoing sync mechanism, in a single pass over the archive.
 */
fun main() {
    val archivePath = Path.of("data/rdf-files.tar.bz2")
    val outputPath = Path.of("data/catalog.sqlite")
    val exportBooksDir = Path.of("data/export/books")
    val exportManifestDir = Path.of("data/export")

    try {
        downloadCatalogArchive(archivePath)
    } catch (e: CatalogDownloadException) {
        println("Catalog download failed: ${e.message}")
        return
    }

    val schemaJsonPath = Path.of(SCHEMA_JSON_PATH)
    if (!Files.exists(schemaJsonPath)) {
        println("Schema JSON not found at $schemaJsonPath. Build the app module first so Room exports it.")
        return
    }
    val schemaStatements = loadSchemaStatements(Files.readString(schemaJsonPath))

    Files.deleteIfExists(outputPath)
    val connection = createDatabase(outputPath, schemaStatements)
    val writer = CatalogWriter(connection)
    val sourceId =
        writer.getOrCreateSource(
            "Project Gutenberg",
            "Public domain works via Project Gutenberg (https://www.gutenberg.org)",
        )
    writer.commit()

    val today = LocalDate.now().toString()
    val manifestEntries = mutableListOf<ManifestEntry>()

    var parsedCount = 0
    var skippedCount = 0
    var parseFailedCount = 0
    var writeFailedCount = 0
    val parseFailureSamples = mutableListOf<String>()
    val writeFailureSamples = mutableListOf<String>()

    readRdfEntriesFromArchive(archivePath) { entryName, content ->
        val book: ParsedBook?
        try {
            book = parseRdf(content)
        } catch (e: RdfParseException) {
            parseFailedCount++
            if (parseFailureSamples.size < 5) {
                parseFailureSamples.add("$entryName: ${e.message}")
            }
            return@readRdfEntriesFromArchive
        }

        if (book == null) {
            skippedCount++
            return@readRdfEntriesFromArchive
        }

        try {
            writer.insertBook(sourceId, book)
            writeBookExport(exportBooksDir, book.toExport(lastModified = today))
            manifestEntries.add(ManifestEntry(externalId = book.externalId, lastModified = today))
            parsedCount++
            if (parsedCount % COMMIT_BATCH_SIZE == 0) {
                writer.commit()
                println("Progress: $parsedCount books written...")
            }
        } catch (e: Exception) {
            writeFailedCount++
            if (writeFailureSamples.size < 5) {
                writeFailureSamples.add("${book.externalId}: ${e.message}")
            }
        }
    }

    writer.commit()
    connection.close()

    writeManifest(
        exportManifestDir,
        Manifest(generatedAt = today, baselineVersion = BASELINE_VERSION, books = manifestEntries),
    )

    println(
        "Done. Parsed: $parsedCount, skipped: $skippedCount, " +
            "parse failures: $parseFailedCount, write failures: $writeFailedCount",
    )
    if (parseFailureSamples.isNotEmpty()) {
        println("Sample parse failures:")
        parseFailureSamples.forEach { println("  $it") }
    }
    if (writeFailureSamples.isNotEmpty()) {
        println("Sample write failures:")
        writeFailureSamples.forEach { println("  $it") }
    }
    println("Database written to $outputPath")
    println("Export files written to $exportManifestDir (manifest.json + ${manifestEntries.size} book files)")
}
