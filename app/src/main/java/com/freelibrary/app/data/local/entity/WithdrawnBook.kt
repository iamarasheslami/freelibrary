package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Marks a book the catalog has withdrawn but that the reader still has something of: a reading place,
 * a bookmark, a highlight, a shelf state or a download. The book row stays, so none of that is lost,
 * but browsing and search leave the book out. A withdrawn book nobody has anything of is simply
 * deleted and has no row here.
 */
@Entity(
    tableName = "withdrawn_books",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WithdrawnBook(
    @PrimaryKey
    @ColumnInfo(name = "bookId")
    val bookId: Long,
)
