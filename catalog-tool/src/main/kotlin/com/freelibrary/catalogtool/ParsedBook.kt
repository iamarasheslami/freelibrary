package com.freelibrary.catalogtool

/**
 * A book's metadata as extracted from a single RDF file, independent of how
 * it will later be stored. [subjects] holds LCSH subject headings only;
 * [locc] is the single Library of Congress Classification code, if present.
 *
 * [summary] is the catalog's short description of the book (the RDF's
 * marc520 field), whitespace-normalized; null when the catalog has none.
 * Some summaries are machine-generated and say so in their own text, which
 * is deliberately kept. [coverUrl] is the medium-size cover image URL, or
 * null when the entry lists no cover.
 */
data class ParsedBook(
    val externalId: String,
    val title: String,
    val issuedDate: String?,
    val language: String?,
    val locc: String?,
    val creators: List<ParsedCreator>,
    val subjects: List<String>,
    val bookshelves: List<String>,
    val formats: List<ParsedFormat>,
    val summary: String? = null,
    val coverUrl: String? = null,
)

data class ParsedCreator(
    val name: String,
    val birthYear: Int?,
    val deathYear: Int?,
)

data class ParsedFormat(
    val url: String,
    val mimeType: String,
)
