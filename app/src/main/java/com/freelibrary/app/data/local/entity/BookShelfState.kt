package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Where a book sits on the reader's personal shelf. A book is in exactly one
 * state at a time. Stored by name, so these constants must not be renamed
 * once real user data exists.
 */
enum class ShelfState {
    WANT_TO_READ,
    READING,
    FINISHED,
}

/**
 * The reader's shelf state for a single book. Local-only, never synced.
 * The reading position itself lives in [ReadingProgress]; this table only
 * says which shelf the book is on. [updatedAt] orders the home-screen
 * sliders (most recent first); [finishedAt] is set only for FINISHED books
 * and lets a future stats screen count books by period.
 */
@Entity(
    tableName = "book_shelf_state",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["state", "updatedAt"])],
)
data class BookShelfState(
    @PrimaryKey
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "state")
    val state: ShelfState,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,
    @ColumnInfo(name = "finishedAt")
    val finishedAt: Long? = null,
)
