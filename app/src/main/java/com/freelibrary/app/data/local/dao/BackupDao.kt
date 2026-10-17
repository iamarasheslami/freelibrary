package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Reads the reader's own rows for a backup. Each row names its book by the source's name and the
 * book's external id, because the internal book id differs from phone to phone.
 */
@Dao
interface BackupDao {
    @Query(
        """
        SELECT src.name AS source, b.externalId AS externalId, t.state AS state,
               t.updatedAt AS updatedAt, t.finishedAt AS finishedAt
        FROM book_shelf_state AS t
        INNER JOIN books AS b ON b.id = t.bookId
        INNER JOIN sources AS src ON src.id = b.sourceId
        ORDER BY t.updatedAt, b.id
        """,
    )
    suspend fun shelfStates(): List<ShelfStateRow>

    @Query(
        """
        SELECT src.name AS source, b.externalId AS externalId, t.lastPosition AS lastPosition,
               t.lastOpenedAt AS lastOpenedAt
        FROM reading_progress AS t
        INNER JOIN books AS b ON b.id = t.bookId
        INNER JOIN sources AS src ON src.id = b.sourceId
        ORDER BY t.lastOpenedAt, b.id
        """,
    )
    suspend fun readingProgress(): List<ReadingProgressRow>

    @Query(
        """
        SELECT src.name AS source, b.externalId AS externalId, t.position AS position,
               t.note AS note, t.createdAt AS createdAt
        FROM bookmarks AS t
        INNER JOIN books AS b ON b.id = t.bookId
        INNER JOIN sources AS src ON src.id = b.sourceId
        ORDER BY t.createdAt, t.id
        """,
    )
    suspend fun bookmarks(): List<BookmarkRow>

    @Query(
        """
        SELECT src.name AS source, b.externalId AS externalId, t.startPosition AS startPosition,
               t.endPosition AS endPosition, t.color AS color, t.createdAt AS createdAt
        FROM highlights AS t
        INNER JOIN books AS b ON b.id = t.bookId
        INNER JOIN sources AS src ON src.id = b.sourceId
        ORDER BY t.createdAt, t.id
        """,
    )
    suspend fun highlights(): List<HighlightRow>

    @Query(
        """
        SELECT src.name AS source, b.externalId AS externalId, t.localFilePath AS localFilePath,
               t.format AS format, t.downloadedAt AS downloadedAt
        FROM downloaded_books AS t
        INNER JOIN books AS b ON b.id = t.bookId
        INNER JOIN sources AS src ON src.id = b.sourceId
        ORDER BY t.downloadedAt, t.id
        """,
    )
    suspend fun downloads(): List<DownloadRow>

    /** The internal id of the book a backup entry names, or null when this phone's catalog does not have it. */
    @Query(
        """
        SELECT b.id FROM books AS b
        INNER JOIN sources AS src ON src.id = b.sourceId
        WHERE src.name = :source AND b.externalId = :externalId
        """,
    )
    suspend fun bookId(
        source: String,
        externalId: String,
    ): Long?
}
