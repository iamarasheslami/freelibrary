package com.freelibrary.app.domain.model

/** One row of the Explore page: a section and the first books of its shelf. */
data class ExploreRow(
    val section: ExploreSection,
    val slider: BookSlider,
)
