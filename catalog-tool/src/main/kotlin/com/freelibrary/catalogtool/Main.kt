package com.freelibrary.catalogtool

import java.nio.file.Files
import java.nio.file.Path

private const val SCHEMA_JSON_PATH = "../app/schemas/com.freelibrary.app.data.local.FreeLibraryDatabase/9.json"
private const val COMMIT_BATCH_SIZE = 500

/**
 * Entry point for the catalog-generation tool. This is a standalone JVM
 * command-line program - it is never shipped inside the Android app. Its job
 * is to download Project Gutenberg's RDF catalog and produce a SQLite
 * database matching the app's Room schema.
 */
fun main() {
    val archivePath = Path.of("data/rdf-files.tar.bz2")
    val outputPath = Path.of("data/catalog.sqlite")

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
}
