package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Links a book to a credited person and the role they played (author,
 * translator, illustrator, editor). A single person can be credited on the
 * same book in more than one role, so the uniqueness constraint covers all
 * three columns together, not just (bookId, authorId).
 */
@Entity(
    tableName = "book_authors",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Author::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["authorId"]),
        Index(value = ["bookId", "authorId", "role"], unique = true),
    ],
)
data class BookAuthor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "bookId")
    val bookId: Long,
    @ColumnInfo(name = "authorId")
    val authorId: Long,
    @ColumnInfo(name = "role")
    val role: String,
)
