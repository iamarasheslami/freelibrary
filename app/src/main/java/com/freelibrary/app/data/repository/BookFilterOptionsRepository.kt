package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.data.local.dao.FilterOptionRow
import com.freelibrary.app.domain.model.BookFilterOptions
import com.freelibrary.app.domain.model.FilterOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The languages, formats and genres the catalog actually has, for the filter sheet.
 *
 * Counting them takes seconds on an old phone, so the answer is kept in memory and read from the
 * catalog once per process. It is dropped by [invalidate] when a sync changes the catalog, and the
 * next [options] call counts again. The app asks for it shortly after starting, so the sheet
 * normally opens at once.
 */
@Singleton
class BookFilterOptionsRepository
    @Inject
    constructor(
        private val bookListDao: BookListDao,
    ) {
        private val lock = Mutex()

        @Volatile private var cached: BookFilterOptions? = null

        /** Counts the catalog again whenever [invalidate] ran while a count was under way. */
        @Volatile private var generation = 0

        suspend fun options(): BookFilterOptions = cached ?: lock.withLock { cached ?: load() }

        /** Forgets the kept options because the catalog changed; the next [options] call counts again. */
        fun invalidate() {
            generation++
            cached = null
        }

        private suspend fun load(): BookFilterOptions {
            val startedAt = generation
            val options =
                BookFilterOptions(
                    languages = bookListDao.languageOptions().toOptions(),
                    formats = bookListDao.formatOptions().toOptions(),
                    genres = bookListDao.genreOptions().toOptions(),
                )
            // A sync that changed the catalog while this counted makes the result stale: use it once, do not keep it.
            if (startedAt == generation) cached = options
            return options
        }

        private fun List<FilterOptionRow>.toOptions() = map { FilterOption(it.value, it.bookCount) }
    }
