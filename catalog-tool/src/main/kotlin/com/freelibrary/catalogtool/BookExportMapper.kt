package com.freelibrary.catalogtool

import com.freelibrary.shared.BookExport
import com.freelibrary.shared.CreatorExport
import com.freelibrary.shared.FormatExport

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
