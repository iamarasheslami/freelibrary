package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.Manifest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

private val jsonFormat =
    Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

/**
 * Writes a single book's export data to booksDir/{externalId}.json,
 * creating the directory if needed. Each book gets its own small file so
 * the app's sync client can fetch exactly the books it's missing, without
 * ever needing to download data for books it already has.
 */
fun writeBookExport(
    booksDir: Path,
    book: BookExport,
) {
    Files.createDirectories(booksDir)
    val filePath = booksDir.resolve("${book.externalId}.json")
    Files.writeString(filePath, jsonFormat.encodeToString(book))
}

/**
 * Writes the sync manifest to outputDir/manifest.json - a small, always-
 * current index of every book's id and last-modified date, small enough for
 * the app to fetch in full on every sync regardless of catalog size.
 */
fun writeManifest(
    outputDir: Path,
    manifest: Manifest,
) {
    Files.createDirectories(outputDir)
    val filePath = outputDir.resolve("manifest.json")
    Files.writeString(filePath, jsonFormat.encodeToString(manifest))
}

/**
 * Reads an existing manifest.json, if present. Used by the nightly
 * incremental sync to update entries in place rather than overwrite the
 * whole manifest blind - returns null if no manifest exists yet at that
 * location (e.g. a fresh checkout before the very first full generation).
 */
fun readManifest(outputDir: Path): Manifest? {
    val filePath = outputDir.resolve("manifest.json")
    if (!Files.exists(filePath)) return null
    return jsonFormat.decodeFromString<Manifest>(Files.readString(filePath))
}
