package com.freelibrary.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A curated collection from the catalog source (e.g. Project Gutenberg's
 * "Bookshelves", like "Science Fiction" or "Children's Literature"). Distinct
 * from [Subject] — bookshelves are editorially curated groupings, subjects are
 * topic labels.
 */
@Entity(tableName = "bookshelves")
data class Bookshelf(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
)
