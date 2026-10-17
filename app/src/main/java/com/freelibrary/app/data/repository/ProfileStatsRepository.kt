package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookShelfStateDao
import com.freelibrary.app.data.local.dao.BookmarkDao
import com.freelibrary.app.data.local.dao.HighlightDao
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.domain.model.ProfileStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** The live counts on the Profile page. They change at once when the reader changes a shelf. */
@Singleton
class ProfileStatsRepository
    @Inject
    constructor(
        shelfStateDao: BookShelfStateDao,
        bookmarkDao: BookmarkDao,
        highlightDao: HighlightDao,
    ) {
        val stats: Flow<ProfileStats> =
            combine(
                shelfStateDao.observeCount(ShelfState.READING),
                shelfStateDao.observeCount(ShelfState.WANT_TO_READ),
                shelfStateDao.observeCount(ShelfState.FINISHED),
                bookmarkDao.observeCount(),
                highlightDao.observeCount(),
            ) { reading, wantToRead, finished, bookmarks, highlights ->
                ProfileStats(reading, wantToRead, finished, bookmarks, highlights)
            }
    }
