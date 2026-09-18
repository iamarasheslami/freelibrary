package com.freelibrary.shared

import kotlinx.serialization.Serializable

/**
 * The JSON shape for an individual book file (e.g. books/1342.json) in the
 * sync-manifest system. This is the shared data contract between
 * catalog-tool (the producer) and the app (the consumer) - a single source
 * of truth so the two sides cannot silently drift apart.
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
