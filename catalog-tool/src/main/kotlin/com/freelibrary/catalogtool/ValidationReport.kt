package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.Manifest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

private val BOOK_ID = Regex("[A-Za-z0-9_-]{1,40}")
private val ISO_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
private const val MAX_REPORTED = 50

private val strictJson = Json { ignoreUnknownKeys = true }

/**
 * What the check of an export folder found. [errors] stop the nightly sync from publishing; [warnings] are
 * only printed. Both are capped to the first few, with [errorCount] and [warningCount] giving the true totals.
 */
data class ValidationReport(
    val bookCount: Int,
    val errors: List<String>,
    val errorCount: Int,
    val warnings: List<String>,
    val warningCount: Int,
) {
    val ok: Boolean get() = errorCount == 0
}

private class Findings {
    val errors = mutableListOf<String>()
    var errorCount = 0
    val warnings = mutableListOf<String>()
    var warningCount = 0

    fun error(message: String) {
        errorCount++
        if (errors.size < MAX_REPORTED) errors += message
    }

    fun warn(message: String) {
        warningCount++
        if (warnings.size < MAX_REPORTED) warnings += message
    }
}

/**
 * Checks an export folder (manifest.json and books/) before it is published, because the app trusts it:
 * the manifest must parse and list every book once under a safe id; every listed book must have a file that
 * decodes, names the same book and carries the same lastModified; no file may be left unlisted. When the
 * [previous] manifest (the one currently published) is given, no book may have disappeared and no
 * lastModified may have gone backwards, since a nightly sync only ever adds and updates.
 */
fun validateExport(
    exportDir: Path,
    previous: Manifest? = null,
): ValidationReport {
    val findings = Findings()
    val manifest =
        try {
            readManifest(exportDir)
        } catch (e: Exception) {
            findings.error("manifest.json cannot be read: ${e.message}")
            null
        }
    if (manifest == null) {
        if (findings.errorCount == 0) findings.error("manifest.json is missing")
        return findings.toReport(0)
    }
    if (manifest.books.isEmpty()) findings.error("The manifest lists no books")

    val booksDir = exportDir.resolve("books")
    val listed = HashSet<String>()
    for (entry in manifest.books) {
        if (!BOOK_ID.matches(entry.externalId)) {
            findings.error("Unsafe book id in the manifest: \"${entry.externalId}\"")
            continue
        }
        if (!listed.add(entry.externalId)) findings.error("Book ${entry.externalId} is listed twice")
        if (!ISO_DATE.matches(entry.lastModified)) findings.error("Book ${entry.externalId} has a bad date: \"${entry.lastModified}\"")
        checkBookFile(booksDir.resolve("${entry.externalId}.json"), entry.externalId, entry.lastModified, findings)
    }

    if (Files.isDirectory(booksDir)) {
        Files.newDirectoryStream(booksDir).use { files ->
            for (file in files) {
                val name = file.fileName.toString()
                if (!name.endsWith(".json") || name.removeSuffix(".json") !in listed) {
                    findings.error("books/$name is not in the manifest")
                }
            }
        }
    }

    if (previous != null) {
        val now = manifest.books.associate { it.externalId to it.lastModified }
        for (old in previous.books) {
            val current = now[old.externalId]
            when {
                current == null -> findings.error("Book ${old.externalId} was in the published manifest and is gone")
                current < old.lastModified -> findings.error("Book ${old.externalId} went back from ${old.lastModified} to $current")
            }
        }
    }
    return findings.toReport(manifest.books.size)
}

private fun Findings.toReport(bookCount: Int) = ValidationReport(bookCount, errors.toList(), errorCount, warnings.toList(), warningCount)

private fun checkBookFile(
    file: Path,
    id: String,
    lastModified: String,
    findings: Findings,
) {
    if (!Files.isRegularFile(file)) {
        findings.error("Book $id has no file")
        return
    }
    val book =
        try {
            strictJson.decodeFromString<BookExport>(Files.readString(file))
        } catch (e: Exception) {
            findings.error("Book $id cannot be read: ${e.message?.lineSequence()?.firstOrNull()}")
            return
        }
    if (book.externalId != id) findings.error("Book $id: its file holds book ${book.externalId}")
    if (book.lastModified != lastModified) findings.error("Book $id: the file says ${book.lastModified}, the manifest says $lastModified")
    if (book.title.isBlank()) findings.error("Book $id has no title")
    if (book.formats.isEmpty()) findings.warn("Book $id lists no formats")
    if (book.formats.any { !it.url.startsWith("https://") }) findings.warn("Book $id has a format address that is not https")
}
