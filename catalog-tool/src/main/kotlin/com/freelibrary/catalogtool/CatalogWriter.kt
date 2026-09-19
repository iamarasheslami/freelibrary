package com.freelibrary.catalogtool

import java.sql.Connection
import java.sql.Statement

/**
 * Writes parsed catalog data into an already-schema-initialized SQLite
 * database (see [createDatabase]). Deliberately caches author/subject/
 * bookshelf lookups in memory rather than querying the database for every
 * occurrence, since this is a one-time batch tool processing tens of
 * thousands of books that frequently share the same people and topics -
 * not a pattern appropriate for the app's own runtime code.
 *
 * Every credited person is currently inserted with role = "author", since
 * the RDF parser only extracts dcterms:creator, not dcterms:contributor
 * separately. Distinguishing translator/illustrator/editor roles would
 * require extending the parser first - a known, deliberate simplification,
 * not an oversight.
 */
class CatalogWriter(private val connection: Connection) {
    private val authorCache = mutableMapOf<String, Long>()
    private val subjectCache = mutableMapOf<String, Long>()
    private val bookshelfCache = mutableMapOf<String, Long>()

    fun getOrCreateSource(
        name: String,
        attribution: String,
    ): Long {
        connection.prepareStatement("SELECT id FROM sources WHERE name = ?").use { select ->
            select.setString(1, name)
            val resultSet = select.executeQuery()
            if (resultSet.next()) {
                return resultSet.getLong("id")
            }
        }

        connection.prepareStatement(
            "INSERT INTO sources (name, attribution) VALUES (?, ?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { insert ->
            insert.setString(1, name)
            insert.setString(2, attribution)
            insert.executeUpdate()
            val keys = insert.generatedKeys
            keys.next()
            return keys.getLong(1)
        }
    }

    fun insertBook(
        sourceId: Long,
        book: ParsedBook,
        lastModified: String,
    ) {
        val bookId = insertBookRow(sourceId, book, lastModified)

        for (creator in book.creators) {
            val authorId = getOrCreateAuthor(creator)
            insertBookAuthor(bookId, authorId)
        }

        for (subjectLabel in book.subjects) {
            val subjectId = getOrCreateSubject(subjectLabel)
            insertBookSubject(bookId, subjectId)
        }

        for (bookshelfName in book.bookshelves) {
            val bookshelfId = getOrCreateBookshelf(bookshelfName)
            insertBookBookshelf(bookId, bookshelfId)
        }

        for (format in book.formats) {
            insertBookFormat(bookId, format)
        }

        insertBookFts(bookId, book)
    }

    fun commit() {
        connection.commit()
    }

    private fun insertBookRow(
        sourceId: Long,
        book: ParsedBook,
        lastModified: String,
    ): Long {
        connection.prepareStatement(
            "INSERT INTO books (sourceId, externalId, title, issuedDate, primaryLanguage, locc, lastModified) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { insert ->
            insert.setLong(1, sourceId)
            insert.setString(2, book.externalId)
            insert.setString(3, book.title)
            insert.setString(4, book.issuedDate)
            insert.setString(5, book.language)
            insert.setString(6, book.locc)
            insert.setString(7, lastModified)
            insert.executeUpdate()
            val keys = insert.generatedKeys
            keys.next()
            return keys.getLong(1)
        }
    }

    private fun getOrCreateAuthor(creator: ParsedCreator): Long {
        authorCache[creator.name]?.let { return it }

        connection.prepareStatement("SELECT id FROM authors WHERE name = ?").use { select ->
            select.setString(1, creator.name)
            val resultSet = select.executeQuery()
            if (resultSet.next()) {
                val id = resultSet.getLong("id")
                authorCache[creator.name] = id
                return id
            }
        }

        connection.prepareStatement(
            "INSERT INTO authors (name, birthYear, deathYear) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { insert ->
            insert.setString(1, creator.name)
            if (creator.birthYear != null) insert.setInt(2, creator.birthYear) else insert.setNull(2, java.sql.Types.INTEGER)
            if (creator.deathYear != null) insert.setInt(3, creator.deathYear) else insert.setNull(3, java.sql.Types.INTEGER)
            insert.executeUpdate()
            val keys = insert.generatedKeys
            keys.next()
            val id = keys.getLong(1)
            authorCache[creator.name] = id
            return id
        }
    }

    private fun insertBookAuthor(
        bookId: Long,
        authorId: Long,
    ) {
        connection.prepareStatement(
            "INSERT OR IGNORE INTO book_authors (bookId, authorId, role) VALUES (?, ?, ?)",
        ).use { insert ->
            insert.setLong(1, bookId)
            insert.setLong(2, authorId)
            insert.setString(3, "author")
            insert.executeUpdate()
        }
    }

    private fun getOrCreateSubject(label: String): Long {
        subjectCache[label]?.let { return it }

        connection.prepareStatement("SELECT id FROM subjects WHERE label = ?").use { select ->
            select.setString(1, label)
            val resultSet = select.executeQuery()
            if (resultSet.next()) {
                val id = resultSet.getLong("id")
                subjectCache[label] = id
                return id
            }
        }

        connection.prepareStatement(
            "INSERT INTO subjects (label) VALUES (?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { insert ->
            insert.setString(1, label)
            insert.executeUpdate()
            val keys = insert.generatedKeys
            keys.next()
            val id = keys.getLong(1)
            subjectCache[label] = id
            return id
        }
    }

    private fun insertBookSubject(
        bookId: Long,
        subjectId: Long,
    ) {
        connection.prepareStatement(
            "INSERT INTO book_subjects (bookId, subjectId) VALUES (?, ?)",
        ).use { insert ->
            insert.setLong(1, bookId)
            insert.setLong(2, subjectId)
            insert.executeUpdate()
        }
    }

    private fun getOrCreateBookshelf(name: String): Long {
        bookshelfCache[name]?.let { return it }

        connection.prepareStatement("SELECT id FROM bookshelves WHERE name = ?").use { select ->
            select.setString(1, name)
            val resultSet = select.executeQuery()
            if (resultSet.next()) {
                val id = resultSet.getLong("id")
                bookshelfCache[name] = id
                return id
            }
        }

        connection.prepareStatement(
            "INSERT INTO bookshelves (name) VALUES (?)",
            Statement.RETURN_GENERATED_KEYS,
        ).use { insert ->
            insert.setString(1, name)
            insert.executeUpdate()
            val keys = insert.generatedKeys
            keys.next()
            val id = keys.getLong(1)
            bookshelfCache[name] = id
            return id
        }
    }

    private fun insertBookBookshelf(
        bookId: Long,
        bookshelfId: Long,
    ) {
        connection.prepareStatement(
            "INSERT INTO book_bookshelves (bookId, bookshelfId) VALUES (?, ?)",
        ).use { insert ->
            insert.setLong(1, bookId)
            insert.setLong(2, bookshelfId)
            insert.executeUpdate()
        }
    }

    private fun insertBookFormat(
        bookId: Long,
        format: ParsedFormat,
    ) {
        connection.prepareStatement(
            "INSERT OR IGNORE INTO book_formats (bookId, formatType, downloadUrl) VALUES (?, ?, ?)",
        ).use { insert ->
            insert.setLong(1, bookId)
            insert.setString(2, mapMimeTypeToFormatType(format.mimeType))
            insert.setString(3, format.url)
            insert.executeUpdate()
        }
    }

    private fun insertBookFts(
        bookId: Long,
        book: ParsedBook,
    ) {
        val authorNames = book.creators.joinToString(" ") { it.name }
        connection.prepareStatement(
            "INSERT INTO books_fts (rowid, title, authorNames) VALUES (?, ?, ?)",
        ).use { insert ->
            insert.setLong(1, bookId)
            insert.setString(2, book.title)
            insert.setString(3, authorNames)
            insert.executeUpdate()
        }
    }
}

/**
 * Maps an RDF-reported MIME type to our schema's short formatType string.
 * Unrecognized MIME types are kept as-is rather than dropped or causing a
 * failure - this lets us observe real-world variety in a later pass rather
 * than silently discarding data we did not anticipate.
 */
fun mapMimeTypeToFormatType(mimeType: String): String =
    when {
        mimeType.contains("epub") -> "epub"
        mimeType.startsWith("text/plain") -> "txt"
        mimeType.contains("application/pdf") -> "pdf"
        else -> mimeType
    }
