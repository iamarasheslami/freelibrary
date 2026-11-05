package com.freelibrary.shared

import kotlinx.serialization.Serializable

/**
 * The JSON shape for an individual book file (e.g. books/1342.json) in the
 * sync-manifest system. This is the shared data contract between
 * catalog-tool (the producer) and the app (the consumer) - a single source
 * of truth so the two sides cannot silently drift apart.
 *
 * [summary] and [coverUrl] are optional: absent in files generated before
 * they were added, and null when the catalog has no value for a book.
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
    val summary: String? = null,
    val coverUrl: String? = null,
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

/**
 * The manifest of one catalog source.
 *
 * [withdrawn] lists the ids of books taken out of the catalog on purpose (a takedown, or a bad batch). They are no
 * longer in [books] and have no book file. An app that still has one of them removes it from its own catalog,
 * but never the reader's own shelves, progress, notes or downloaded files. A withdrawal is permanent:
 * the id stays in the list unless the book is listed again. The field is absent while nothing has been withdrawn.
 */
@Serializable
data class Manifest(
    val generatedAt: String,
    val baselineVersion: String,
    val books: List<ManifestEntry>,
    val withdrawn: List<String> = emptyList(),
)
