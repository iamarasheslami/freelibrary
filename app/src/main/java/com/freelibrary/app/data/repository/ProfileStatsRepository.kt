package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookmarkDao
import com.freelibrary.app.data.local.dao.HighlightDao
import com.freelibrary.app.domain.model.ProfileCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** The live totals under the reader's name on the Profile page. */
@Singleton
class ProfileStatsRepository
    @Inject
    constructor(
        bookmarkDao: BookmarkDao,
        highlightDao: HighlightDao,
    ) {
        val counts: Flow<ProfileCounts> =
            combine(bookmarkDao.observeCount(), highlightDao.observeCount()) { bookmarks, highlights ->
                ProfileCounts(bookmarks, highlights)
            }
    }
