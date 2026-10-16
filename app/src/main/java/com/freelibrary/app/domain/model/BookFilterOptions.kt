package com.freelibrary.app.domain.model

/** One choice of the filter sheet: the [value] the filter stores, and how many books have it. */
data class FilterOption(
    val value: String,
    val bookCount: Int,
)

/** Everything the filter sheet can offer, each list most books first. */
data class BookFilterOptions(
    val languages: List<FilterOption> = emptyList(),
    val formats: List<FilterOption> = emptyList(),
    val genres: List<FilterOption> = emptyList(),
)
