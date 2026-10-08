package com.freelibrary.app.data.local.dao

/** A catalog shelf and how many books it holds in one language. */
data class ShelfCountRow(
    val name: String,
    val bookCount: Int,
)
