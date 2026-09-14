package com.freelibrary.catalogtool

/**
 * A book's metadata as extracted from a single RDF file, independent of how
 * it will later be stored. [subjects] holds LCSH subject headings only;
 * [locc] is the single Library of Congress Classification code, if present.
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
