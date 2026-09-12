package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Links a book to a curated bookshelf collection.
 */
@Entity(
    tableName = "book_bookshelves",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Bookshelf::class,
            parentColumns = ["id"],
            childColumns = ["bookshelfId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookshelfId"]),
        Index(value = ["bookId", "bookshelfId"], unique = true),
    ],
)
data class BookBookshelf(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "bookshelfId")
    val bookshelfId: Long,
)
