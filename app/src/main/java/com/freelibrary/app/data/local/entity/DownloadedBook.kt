package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks a single downloaded format of a book, stored on this device.
 * Local-only, never synced. A book can have more than one format downloaded
 * simultaneously (e.g. both EPUB and plain text), so the uniqueness
 * constraint is on (bookId, format) together, not bookId alone.
 */
@Entity(
    tableName = "downloaded_books",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "format"], unique = true),
    ],
)
data class DownloadedBook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "localFilePath")
    val localFilePath: String,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "downloadedAt")
    val downloadedAt: Long,
)
