package com.freelibrary.catalogtool

import kotlinx.serialization.Serializable

/**
 * The JSON shape written for an individual book file (e.g. books/1342.json)
 * in the sync-manifest system. Deliberately separate from [ParsedBook] -
 * this is the public, versioned data contract the app's sync client reads,
 * while ParsedBook is this tool's internal parsing representation. Keeping
 * them distinct means the RDF parsing logic can change freely without
 * silently altering the public export format.
 */
@Serializable
data class BookExport(
    val externalId: String,
    val title: String,
    val issuedDate: String?,
    val language: String?,
    val locc: String?,
    val creators: List<CreatorExport>,
    val subjects: List<String>,
    val bookshelves: List<String>,
    val formats: List<FormatExport>,
    val lastModified: String,
)

@Serializable
data class CreatorExport(
    val name: String,
    val birthYear: Int?,
    val deathYear: Int?,
)

@Serializable
data class FormatExport(
    val url: String,
    val formatType: String,
)

fun ParsedBook.toExport(lastModified: String): BookExport =
    BookExport(
        externalId = externalId,
        title = title,
        issuedDate = issuedDate,
        language = language,
        locc = locc,
        creators = creators.map { CreatorExport(name = it.name, birthYear = it.birthYear, deathYear = it.deathYear) },
        subjects = subjects,
        bookshelves = bookshelves,
        formats = formats.map { FormatExport(url = it.url, formatType = mapMimeTypeToFormatType(it.mimeType)) },
        lastModified = lastModified,
    )

/**
 * A single row in the always-current sync manifest: enough for the app to
 * decide whether it already has this book and whether its copy is current,
 * without needing any book's full data.
 */
@Serializable
data class ManifestEntry(
    val externalId: String,
    val lastModified: String,
)

@Serializable
data class Manifest(
    val generatedAt: String,
    val baselineVersion: String,
    val books: List<ManifestEntry>,
)
