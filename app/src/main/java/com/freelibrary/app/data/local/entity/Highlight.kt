package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created text highlight within a book. Local-only, never synced.
 * Only the position range is stored, not the highlighted text itself — the
 * actual text is re-sliced from the book file when displayed, keeping this
 * table small and avoiding redundant storage of book content.
 */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bookId"])],
)
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "startPosition")
    val startPosition: String,
    @ColumnInfo(name = "endPosition")
    val endPosition: String,
    @ColumnInfo(name = "color")
    val color: String?,
    @ColumnInfo(name = "createdAt")
    val createdAt: Long,
)
