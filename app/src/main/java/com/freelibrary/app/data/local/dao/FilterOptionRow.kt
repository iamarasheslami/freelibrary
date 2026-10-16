package com.freelibrary.app.data.local.dao

/** One choice of the filter sheet as the database reports it: its stored value and how many books have it. */
data class FilterOptionRow(
    val value: String,
    val bookCount: Int,
)
