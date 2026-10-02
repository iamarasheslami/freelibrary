package com.freelibrary.catalogtool

import com.freelibrary.shared.SearchQueryPlanner
import com.freelibrary.shared.SearchScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

private val REAL_SCHEMA =
    listOf(
        "CREATE TABLE IF NOT EXISTS `sources` " +
            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `attribution` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceId` INTEGER NOT NULL, " +
            "`externalId` TEXT NOT NULL, `title` TEXT NOT NULL, `issuedDate` TEXT, `primaryLanguage` TEXT, `locc` TEXT, " +
            "`lastModified` TEXT NOT NULL, `coverUrl` TEXT, " +
            "FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE TABLE IF NOT EXISTS `authors` " +
            "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `birthYear` INTEGER, `deathYear` INTEGER)",
        "CREATE TABLE IF NOT EXISTS `book_authors` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, " +
            "`authorId` INTEGER NOT NULL, `role` TEXT NOT NULL, " +
            "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
            "FOREIGN KEY(`authorId`) REFERENCES `authors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `subjects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `book_subjects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, " +
            "`subjectId` INTEGER NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
            "FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `bookshelves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `book_bookshelves` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, " +
            "`bookshelfId` INTEGER NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
            "FOREIGN KEY(`bookshelfId`) REFERENCES `bookshelves`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `book_formats` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, " +
            "`formatType` TEXT NOT NULL, `downloadUrl` TEXT NOT NULL, " +
            "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `book_summaries` (`bookId` INTEGER NOT NULL, `summary` TEXT NOT NULL, " +
            "PRIMARY KEY(`bookId`), " +
            "FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE VIRTUAL TABLE IF NOT EXISTS `books_fts` USING FTS4(`title` TEXT NOT NULL, `authorNames` TEXT NOT NULL)",
    )

private fun sampleBook(
    externalId: String,
    title: String,
    authorName: String,
) = ParsedBook(
    externalId = externalId,
    title = title,
    issuedDate = "1998-06-01",
    language = "en",
    locc = "PR",
    creators = listOf(ParsedCreator(name = authorName, birthYear = 1775, deathYear = 1817)),
    subjects = listOf("Fiction"),
    bookshelves = listOf("Best Books Ever Listings"),
    formats = listOf(ParsedFormat(url = "https://example.org/$externalId.txt", mimeType = "text/plain; charset=utf-8")),
)

class CatalogWriterTest {
    private lateinit var tempDbPath: Path
    private lateinit var connection: Connection
    private lateinit var writer: CatalogWriter

    @Before
    fun setUp() {
        tempDbPath = Files.createTempFile("catalog-writer-test", ".sqlite")
        Files.delete(tempDbPath)
        connection = createDatabase(tempDbPath, REAL_SCHEMA)
        writer = CatalogWriter(connection)
    }

    @After
    fun tearDown() {
        connection.close()
        Files.deleteIfExists(tempDbPath)
    }

    @Test
    fun `insertBook creates a book row linked to its source`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-09-16")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT title, sourceId FROM books WHERE externalId = '1342'")
            assertTrue(resultSet.next())
            assertEquals("Pride and Prejudice", resultSet.getString("title"))
            assertEquals(sourceId, resultSet.getLong("sourceId"))
        }
    }

    @Test
    fun `an author shared by two books is inserted only once`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-09-16")
        writer.insertBook(sourceId, sampleBook("158", "Emma", "Jane Austen"), lastModified = "2026-09-16")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM authors WHERE name = 'Jane Austen'")
            resultSet.next()
            assertEquals(1, resultSet.getInt("count"))
        }

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM book_authors")
            resultSet.next()
            assertEquals(2, resultSet.getInt("count"))
        }
    }

    @Test
    fun `a subject shared by two books is inserted only once`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-09-16")
        writer.insertBook(sourceId, sampleBook("158", "Emma", "Jane Austen"), lastModified = "2026-09-16")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM subjects WHERE label = 'Fiction'")
            resultSet.next()
            assertEquals(1, resultSet.getInt("count"))
        }
    }

    @Test
    fun `book formats are inserted with the mapped format type and resolved URL`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-09-16")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT formatType, downloadUrl FROM book_formats")
            assertTrue(resultSet.next())
            assertEquals("txt", resultSet.getString("formatType"))
            assertEquals("https://example.org/1342.txt", resultSet.getString("downloadUrl"))
        }
    }

    @Test
    fun `a book is searchable via the full-text index by title and author`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-09-16")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT rowid FROM books_fts WHERE books_fts MATCH 'Austen'")
            assertTrue(resultSet.next())
        }
    }

    @Test
    fun `insertBook stores the cover URL on the book row`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        val book =
            sampleBook("1342", "Pride and Prejudice", "Jane Austen")
                .copy(coverUrl = "https://example.org/pg1342.cover.medium.jpg")
        writer.insertBook(sourceId, book, lastModified = "2026-10-02")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT coverUrl FROM books WHERE externalId = '1342'")
            assertTrue(resultSet.next())
            assertEquals("https://example.org/pg1342.cover.medium.jpg", resultSet.getString("coverUrl"))
        }
    }

    @Test
    fun `a book with no cover is stored with a null cover URL`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-10-02")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT coverUrl FROM books WHERE externalId = '1342'")
            assertTrue(resultSet.next())
            assertNull(resultSet.getString("coverUrl"))
        }
    }

    @Test
    fun `insertBook stores the summary in book_summaries linked to the right book, including non-ASCII text`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        val summary = "Un roman « fictif » — avec des accents: é, ü, ñ."
        writer.insertBook(sourceId, sampleBook("1", "First", "Author One"), lastModified = "2026-10-02")
        writer.insertBook(
            sourceId,
            sampleBook("2", "Second", "Author Two").copy(summary = summary),
            lastModified = "2026-10-02",
        )
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet =
                statement.executeQuery(
                    "SELECT b.externalId, s.summary FROM book_summaries s JOIN books b ON b.id = s.bookId",
                )
            assertTrue(resultSet.next())
            assertEquals("2", resultSet.getString("externalId"))
            assertEquals(summary, resultSet.getString("summary"))
        }
    }

    @Test
    fun `a book with no summary gets no row in book_summaries`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1342", "Pride and Prejudice", "Jane Austen"), lastModified = "2026-10-02")
        writer.commit()

        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM book_summaries")
            resultSet.next()
            assertEquals(0, resultSet.getInt("count"))
        }
    }

    private fun searchExternalIds(
        scope: SearchScope,
        query: String,
    ): List<String> {
        val matchQuery = SearchQueryPlanner.plan(query, scope)!!.matchQuery
        connection.prepareStatement(
            "SELECT b.externalId FROM books_fts JOIN books b ON b.id = books_fts.rowid " +
                "WHERE books_fts MATCH ? ORDER BY b.externalId",
        ).use { select ->
            select.setString(1, matchQuery)
            val resultSet = select.executeQuery()
            val ids = mutableListOf<String>()
            while (resultSet.next()) ids.add(resultSet.getString("externalId"))
            return ids
        }
    }

    @Test
    fun `the search index is folded so case and accents do not matter`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("135", "Les Misérables", "Hugo, Victor"), lastModified = "2026-10-02")
        writer.insertBook(sourceId, sampleBook("767", "Jane Eyre", "Brontë, Charlotte"), lastModified = "2026-10-02")
        writer.commit()

        assertEquals(listOf("135"), searchExternalIds(SearchScope.TITLE, "MISERABLES"))
        assertEquals(listOf("767"), searchExternalIds(SearchScope.AUTHOR, "bronte charlotte"))
    }

    @Test
    fun `a French elision is searchable by the word after the article`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("12", "L'Homme qui rit", "Hugo, Victor"), lastModified = "2026-10-02")
        writer.commit()

        assertEquals(listOf("12"), searchExternalIds(SearchScope.TITLE, "homme"))
    }

    @Test
    fun `title searches only look in titles and author searches only in authors`() {
        val sourceId = writer.getOrCreateSource("Project Gutenberg", "PG")
        writer.insertBook(sourceId, sampleBook("1", "Austen Country", "Someone Else"), lastModified = "2026-10-02")
        writer.insertBook(sourceId, sampleBook("2", "Emma", "Austen, Jane"), lastModified = "2026-10-02")
        writer.commit()

        assertEquals(listOf("1"), searchExternalIds(SearchScope.TITLE, "austen"))
        assertEquals(listOf("2"), searchExternalIds(SearchScope.AUTHOR, "austen"))
    }
}
