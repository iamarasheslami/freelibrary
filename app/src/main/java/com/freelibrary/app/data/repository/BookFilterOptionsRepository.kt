package com.freelibrary.app.data.repository

import com.freelibrary.app.data.local.dao.BookListDao
import com.freelibrary.app.data.local.dao.FilterOptionRow
import com.freelibrary.app.domain.model.BookFilterOptions
import com.freelibrary.app.domain.model.FilterOption
import javax.inject.Inject
import javax.inject.Singleton

/** The languages, formats and genres the catalog actually has, for the filter sheet. */
@Singleton
class BookFilterOptionsRepository
    @Inject
    constructor(
        private val bookListDao: BookListDao,
    ) {
        suspend fun options(): BookFilterOptions =
            BookFilterOptions(
                languages = bookListDao.languageOptions().toOptions(),
                formats = bookListDao.formatOptions().toOptions(),
                genres = bookListDao.genreOptions().toOptions(),
            )

        private fun List<FilterOptionRow>.toOptions() = map { FilterOption(it.value, it.bookCount) }
    }
