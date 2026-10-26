package com.freelibrary.app.domain.model

import androidx.annotation.StringRes

/** One slider of the Profile page. [id] never changes for a slider. */
data class ProfileSection(
    val id: String,
    @StringRes val titleRes: Int,
    val books: List<BookCard>,
    val hasMore: Boolean,
    val viewMore: ViewMoreTarget,
)
