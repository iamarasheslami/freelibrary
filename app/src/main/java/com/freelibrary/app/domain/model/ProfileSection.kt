package com.freelibrary.app.domain.model

import androidx.annotation.StringRes

/**
 * One slider of the Profile page. Unlike a Home slider it is kept when it has no books: the
 * page shows [emptyRes] in its place. [id] never changes for a slider.
 */
data class ProfileSection(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val emptyRes: Int,
    val books: List<BookCard>,
    val hasMore: Boolean,
    val viewMore: ViewMoreTarget,
)
