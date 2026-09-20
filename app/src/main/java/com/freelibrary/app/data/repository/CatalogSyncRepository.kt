package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.AuthorDao
import com.freelibrary.app.data.local.dao.BookAuthorDao
import com.freelibrary.app.data.local.dao.BookBookshelfDao
import com.freelibrary.app.data.local.dao.BookDao
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.dao.BookFtsDao
import com.freelibrary.app.data.local.dao.BookSubjectDao
import com.freelibrary.app.data.local.dao.BookshelfDao
import com.freelibrary.app.data.local.dao.SourceDao
import com.freelibrary.app.data.local.dao.SubjectDao
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.BookFts
import com.freelibrary.app.data.local.entity.BookSubject
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.local.entity.Subject
import com.freelibrary.app.data.remote.CatalogSyncApi
import com.freelibrary.shared.BookExport
import javax.inject.Inject

private const val SOURCE_NAME = "Project Gutenberg"
private const val SOURCE_ATTRIBUTION = "Public domain works via Project Gutenberg (https://www.gutenberg.org)"
private const val MAX_FAILURE_SAMPLES = 5

/** Thrown when the sync cannot proceed at all, e.g. the manifest itself could not be fetched. */
class CatalogSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class SyncResult(
    val added: Int,
    val updated: Int,
    val failed: Int,
    val failureSamples: List<String>,
)

/**
 * Syncs the local catalog against the manifest published by
 * freelibrary-catalog-data, fetching and merging only books that are
 * missing or whose lastModified has changed - never the whole catalog,
 * regardless of how long since the last successful sync.
 *
 * Updates to existing books happen strictly in place (see [BookDao.update]):
 * the internal book id is never changed, protecting any reading_progress,
 * bookmarks, highlights, or downloaded_books tied to that book. Only the
 * catalog-association tables (authors, subjects, bookshelves, formats) are
 * cleared and rebuilt on an update - those hold catalog metadata, not user
 * data.
 *
 * A single book failing to fetch or parse never aborts the whole sync -
 * failures are collected and reported, matching the same per-item error
 * isolation catalog-tool uses when generating the bundled database.
 */
class CatalogSyncRepository
    @Inject
    constructor(
        private val api: CatalogSyncApi,
        private val sourceDao: SourceDao,
        private val bookDao: BookDao,
        private val authorDao: AuthorDao,
        private val bookAuthorDao: BookAuthorDao,
        private val subjectDao: SubjectDao,
        private val bookSubjectDao: BookSubjectDao,
        private val bookshelfDao: BookshelfDao,
        private val bookBookshelfDao: BookBookshelfDao,
        private val bookFormatDao: BookFormatDao,
        private val bookFtsDao: BookFtsDao,
    ) {
        suspend fun sync(): SyncResult {
            val sourceId = getOrCreateSourceId()

            val manifest =
                try {
                    api.getManifest()
                } catch (e: Exception) {
                    throw CatalogSyncException("Failed to fetch sync manifest", e)
                }

            val localIndex = bookDao.getAllExternalIdsAndLastModified(sourceId).associateBy { it.externalId }

            var added = 0
            var updated = 0
            var failed = 0
            val failureSamples = mutableListOf<String>()

            for (entry in manifest.books) {
                val local = localIndex[entry.externalId]
                val needsSync = local == null || local.lastModified != entry.lastModified
                if (!needsSync) continue

                try {
                    val bookExport = api.getBook(entry.externalId)
                    val wasNew = upsertBook(sourceId, bookExport)
                    if (wasNew) added++ else updated++
                } catch (e: Exception) {
                    failed++
                    if (failureSamples.size < MAX_FAILURE_SAMPLES) {
                        failureSamples.add("${entry.externalId}: ${e.message}")
                    }
                }
            }

            return SyncResult(added = added, updated = updated, failed = failed, failureSamples = failureSamples)
        }

        private suspend fun getOrCreateSourceId(): Long {
            sourceDao.findByName(SOURCE_NAME)?.let { return it.id }
            return sourceDao.insert(Source(name = SOURCE_NAME, attribution = SOURCE_ATTRIBUTION))
        }

        /** Returns true if this was a brand-new book (insert), false if it updated an existing one. */
        private suspend fun upsertBook(
            sourceId: Long,
            bookExport: BookExport,
        ): Boolean {
            val existing = bookDao.getBySourceAndExternalId(sourceId, bookExport.externalId)

            val bookId: Long
            val wasNew: Boolean

            if (existing != null) {
                bookDao.update(
                    Book(
                        id = existing.id,
                        sourceId = sourceId,
                        externalId = bookExport.externalId,
                        title = bookExport.title,
                        issuedDate = bookExport.issuedDate,
                        primaryLanguage = bookExport.language,
                        locc = bookExport.locc,
                        lastModified = bookExport.lastModified,
                    ),
                )
                bookId = existing.id
                wasNew = false

                bookAuthorDao.deleteForBook(bookId)
                bookSubjectDao.deleteForBook(bookId)
                bookBookshelfDao.deleteForBook(bookId)
                bookFormatDao.deleteForBook(bookId)
            } else {
                bookId =
                    bookDao.insert(
                        Book(
                            sourceId = sourceId,
                            externalId = bookExport.externalId,
                            title = bookExport.title,
                            issuedDate = bookExport.issuedDate,
                            primaryLanguage = bookExport.language,
                            locc = bookExport.locc,
                            lastModified = bookExport.lastModified,
                        ),
                    )
                wasNew = true
            }

            for (creator in bookExport.creators) {
                val authorId = getOrCreateAuthorId(creator.name, creator.birthYear, creator.deathYear)
                bookAuthorDao.insert(BookAuthor(bookId = bookId, authorId = authorId, role = "author"))
            }

            for (subjectLabel in bookExport.subjects) {
                val subjectId = getOrCreateSubjectId(subjectLabel)
                bookSubjectDao.insert(BookSubject(bookId = bookId, subjectId = subjectId))
            }

            for (bookshelfName in bookExport.bookshelves) {
                val bookshelfId = getOrCreateBookshelfId(bookshelfName)
                bookBookshelfDao.insert(BookBookshelf(bookId = bookId, bookshelfId = bookshelfId))
            }

            for (format in bookExport.formats) {
                bookFormatDao.insert(BookFormat(bookId = bookId, formatType = format.formatType, downloadUrl = format.url))
            }

            val authorNames = bookExport.creators.joinToString(" ") { it.name }
            bookFtsDao.upsert(BookFts(bookId = bookId, title = bookExport.title, authorNames = authorNames))

            return wasNew
        }

        private suspend fun getOrCreateAuthorId(
            name: String,
            birthYear: Int?,
            deathYear: Int?,
        ): Long {
            authorDao.findByName(name)?.let { return it.id }
            return authorDao.insert(Author(name = name, birthYear = birthYear, deathYear = deathYear))
        }

        private suspend fun getOrCreateSubjectId(label: String): Long {
            subjectDao.findByLabel(label)?.let { return it.id }
            return subjectDao.insert(Subject(label = label))
        }

        private suspend fun getOrCreateBookshelfId(name: String): Long {
            bookshelfDao.findByName(name)?.let { return it.id }
            return bookshelfDao.insert(Bookshelf(name = name))
        }
    }
