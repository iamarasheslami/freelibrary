package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An author, translator, illustrator, or editor credited on one or more books.
 * A single row here can be linked to many books via [BookAuthor], and a book
 * can have multiple people credited via that same join table.
 */
@Entity(tableName = "authors")
data class Author(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "birthYear")
    val birthYear: Int?,
    @ColumnInfo(name = "deathYear")
    val deathYear: Int?,
)
